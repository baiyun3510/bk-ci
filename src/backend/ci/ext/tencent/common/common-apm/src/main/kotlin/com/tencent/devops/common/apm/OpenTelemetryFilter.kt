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

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.Enumeration
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

class OpenTelemetryFilter @Autowired constructor(
    val opentelemetryConfiguration: OpenTelemetryConfiguration,
) : Filter{

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain) {
        val httpServletRequest = request as HttpServletRequest
        // 屏蔽掉spring management相关接口
        if (httpServletRequest.requestURL.contains("/management/")) {
            return chain.doFilter(request, response)
        }
        val trace = opentelemetryConfiguration.trace

        val textMapPropagator: TextMapPropagator = opentelemetryConfiguration.openTelemetry.propagators.textMapPropagator

        val context: Context = textMapPropagator.extract(Context.current(), httpServletRequest, object : TextMapGetter<HttpServletRequest> {
            override fun keys(request: HttpServletRequest): MutableList<String?> {
                val headers: MutableList<String?> = mutableListOf()
                val names: Enumeration<*> = request.headerNames
                while (names.hasMoreElements()) {
                    val name = names.nextElement() as String
                    headers.add(name)
                }
                return headers
            }

            override fun get(carrier: HttpServletRequest?, key: String): String? {
                return request.getHeader(key)
            }
        })

        val span = trace.spanBuilder(httpServletRequest.requestURL.toString())
            .setParent(context)
            .setAttribute("method", httpServletRequest.getMethod())
            .setSpanKind(SpanKind.SERVER).startSpan()

        try {
            chain.doFilter(request, response)
        } catch (ex: Exception) {
            span.setStatus(StatusCode.ERROR)
            throw ex
        }finally {
            span.end()
        }
    }
}
