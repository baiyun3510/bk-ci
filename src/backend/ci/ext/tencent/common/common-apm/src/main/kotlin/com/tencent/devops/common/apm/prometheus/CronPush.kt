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

package com.tencent.devops.common.apm.prometheus

import com.tencent.devops.common.service.utils.SpringContextUtil
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.annotation.PostConstruct

@Service
class CronPush @Autowired constructor(
    val pushGateway: BkPushGateway
) {
    @Value("\${spring.application.name:#{null}}")
    val applicationName: String? = null

    private val executorService = Executors.newSingleThreadExecutor()

    @PostConstruct
    fun startPusth() {
        executorService.submit(pushThread())
    }

    fun pushThread(): Callable<Int> {
        while (true) {
            logger.info("start push")
            val counter = SpringContextUtil.getBean(Counter::class.java)
            val gauge = SpringContextUtil.getBean(Gauge::class.java)
            pushGateway.push(counter, "$applicationName-counter")
            pushGateway.push(gauge, "$applicationName-gauge")
            logger.info("end push")

            // 每10s上报一次数据
            Thread.sleep(SLEEP_TIME)
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(CronPush::class.java)
        const val SLEEP_TIME = 10 * 1000L
    }
}