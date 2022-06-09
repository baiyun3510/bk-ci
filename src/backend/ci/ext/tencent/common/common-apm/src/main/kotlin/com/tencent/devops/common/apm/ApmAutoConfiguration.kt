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

import com.tencent.devops.common.apm.prometheus.BkPushGateway
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.cloud.consul.ConsulAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered

@Configuration
@PropertySource("classpath:/common-service.properties")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(ConsulAutoConfiguration::class)
class ApmAutoConfiguration {

    @Value("\${spring.application.name:#{null}}")
    val applicationName: String? = null

    @Bean
    fun opentelemertry() = OpenTelemetryConfiguration()

//    @Bean
//    fun opentelemertryFilter(
//        opentelemetryConfiguration: OpentelemetryConfiguration
//    ) = OpenTelemetryFilter(opentelemetryConfiguration)


    @Bean
    fun getPushGateway(): BkPushGateway? {
        return BkPushGateway("bkmonitor-http-report-paasee.woa.com:4318", "Ymtia2JrYmtia2JrYmtia4FtQWLNkSKtNp77jBh0s/TYzOtqKq7oFyDDmnP5jtxD\n")
    }

    @Bean
    fun getCounter(): Counter? {
        return Counter.build()
            .name("$applicationName:counter") //
            .labelNames("$applicationName:counter") //
            .help("fitz test") //这个名字随便起
            .register() //注：通常只能注册1次，1个实例中重复注册会报错
    }

    @Bean
    fun getGauge(): Gauge? {
        return Gauge.build()
            .name("$applicationName:gauge") //
            .help("$applicationName:gauge")
            .register()
    }
}
