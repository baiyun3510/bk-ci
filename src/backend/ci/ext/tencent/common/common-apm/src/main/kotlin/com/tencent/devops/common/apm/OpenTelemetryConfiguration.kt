/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.apm

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

class OpenTelemetryConfiguration {

    @Value("\${apm.endpoint:#{null}}")
    val apmEndPoint = "http://bk-otlp-report-bkop-01.woa.com:4317"

    @Value("\${apm.bkdata:#{null}}")
    val bkDataId = "Ymtia2JrYmtia2JrYmtia/MJPFhRw5Ia+LuYODqXATRBECih2cVL3ypPq4atuOhD"

    @Value("\${spring.application.name:#{null}}")
    val applicationName: String? = null

    lateinit var trace: Tracer

    lateinit var openTelemetry: OpenTelemetry

    @PostConstruct
    fun initOpenTelemetry() : OpenTelemetry {
        val grpcSpanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(apmEndPoint)
            .build()

        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(grpcSpanExporter).build())
            .setResource(Resource.create(Attributes.builder()
                                             .put("service.name", applicationName)
                                             .put("bk.data.token", bkDataId)
                                             .build()))
            .build()

        val openTelemetryInstance: OpenTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build()

        Runtime.getRuntime().addShutdownHook(Thread { tracerProvider.close() })
        trace = openTelemetryInstance.getTracer("instrumentation-library-name", "1.0.0")
        openTelemetry = openTelemetryInstance
        return openTelemetryInstance
    }
}
