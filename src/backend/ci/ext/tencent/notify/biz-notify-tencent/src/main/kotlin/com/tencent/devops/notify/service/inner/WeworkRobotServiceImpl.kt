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

package com.tencent.devops.notify.service.inner

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.apm.OpentelemetryConfiguration
import com.tencent.devops.common.notify.enums.WeworkReceiverType
import com.tencent.devops.common.notify.enums.WeworkTextType
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_WEWORK
import com.tencent.devops.notify.dao.WeworkNotifyDao
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.WeweokRobotBaseMessage
import com.tencent.devops.notify.pojo.WeworkNotifyMediaMessage
import com.tencent.devops.notify.pojo.WeworkNotifyTextMessage
import com.tencent.devops.notify.pojo.WeworkRobotMarkdownMessage
import com.tencent.devops.notify.pojo.WeworkRobotSingleTextMessage
import com.tencent.devops.notify.pojo.WeworkSendMessageResp
import com.tencent.devops.notify.pojo.WeworkRobotContentMessage
import com.tencent.devops.notify.service.WeworkService
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.exporter.PushGateway
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import java.util.Optional

@Configuration
@ConditionalOnProperty(prefix = "notify", name = ["weworkChannel"], havingValue = "weworkRobot")
class WeworkRobotServiceImpl @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val weworkNotifyDao: WeworkNotifyDao,
    private val opentelemetryConfiguration: OpentelemetryConfiguration,
    private val pushGateway: PushGateway,
    private val counter: Counter,
    private val gauge: Gauge
) : WeworkService {
    override fun sendMqMsg(message: WeworkNotifyMessageWithOperation) {
        val trace = opentelemetryConfiguration.trace
        logger.info("$pushGateway $counter $gauge")
        val buildCount = Counter.build().name("build_count_test").create()
        buildCount.inc()
        logger.info("build_count: ${buildCount.get()}")

        val beansCount = SpringContextUtil.getBean(Counter::class.java)
        beansCount.inc()
        logger.info("build_count: ${beansCount.get()}")

//        val textMapPropagator: TextMapPropagator = opentelemetryConfiguration.openTelemetry.propagators.textMapPropagator
        val span = trace.spanBuilder("sendRtx_PRO").setParent(Context.current()).setSpanKind(SpanKind.PRODUCER).startSpan()
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_WEWORK, message)
        if (counter != null && gauge != null) {
            counter.inc()
            gauge.inc()
            logger.info("count: ${counter.get()}")
            logger.info("gauge: ${gauge.get()}")
            logger.info("pg: $pushGateway")
            pushGateway.push(counter, "notify_count_test")
            pushGateway.push(gauge, "notify_gauge_test")
        }
        span.end()
        pushGateway.push(buildCount, "notify_build_count_test")
        pushGateway.push(beansCount, "notify_beans_count_test")
    }

    @Value("\${wework.apiUrl:https://qyapi.weixin.qq.com}")
    lateinit var weworkHost: String

    @Value("\${wework.robotKey}")
    lateinit var robotKey: String

    override fun sendMediaMessage(weworkNotifyMediaMessage: WeworkNotifyMediaMessage) {
        return
    }

    override fun sendTextMessage(weworkNotifyTextMessage: WeworkNotifyTextMessage) {
        val trace = opentelemetryConfiguration.trace
        val span = trace.spanBuilder("sendRtx_CUS").setParent(Context.current()).setSpanKind(SpanKind.CONSUMER).startSpan()
        try {
            val sendRequest = mutableListOf<WeweokRobotBaseMessage>()
            val content = if (checkMessageSize(weworkNotifyTextMessage.message)) {
                weworkNotifyTextMessage.message.replace("\\n", "\n")
            } else {
                weworkNotifyTextMessage.message.replace("\\n", "\n").substring(0, WEWORK_MAX_SIZE - 1) +
                    "...(消息长度超$WEWORK_MAX_SIZE 已截断,请控制消息长度)"
            }
            weworkNotifyTextMessage.message = content
            when (weworkNotifyTextMessage.receiverType) {
                WeworkReceiverType.group -> {
                    return
                }
                WeworkReceiverType.single -> {
                    weworkNotifyTextMessage.receivers.forEach {
                        if (weworkNotifyTextMessage.textType == WeworkTextType.text) {
                            sendRequest.add(
                                WeworkRobotSingleTextMessage(
                                    chatid = it,
                                    text = WeworkRobotContentMessage(
                                        content = content,
                                        mentionedList = null,
                                        mentionedMobileList = null
                                    ),
                                    visibleToUser = null,
                                    postId = null
                                )
                            )
                        } else if (weworkNotifyTextMessage.textType == WeworkTextType.markdown) {
                            sendRequest.add(
                                WeworkRobotMarkdownMessage(
                                    chatid = it,
                                    markdown = WeworkRobotContentMessage(
                                        content = content,
                                        mentionedList = null,
                                        mentionedMobileList = null
                                    ),
                                    postId = null
                                )
                            )
                        }
                    }
                }
            }
            try {
                doSendRequest(sendRequest)
                logger.info("send message success, $weworkNotifyTextMessage")
                saveResult(weworkNotifyTextMessage.receivers, "type:${weworkNotifyTextMessage.message}\n", true, null)
            } catch (e: Exception) {
                logger.warn("send message fail, $weworkNotifyTextMessage")
                saveResult(weworkNotifyTextMessage.receivers, "type:${weworkNotifyTextMessage.message}\n", false, e.message)
            }
        } finally {
            span.end()
        }
    }

    private fun doSendRequest(requestBodies: List<WeweokRobotBaseMessage>) {
        if (requestBodies.isEmpty()) {
            throw OperationException("no message to send")
        }
        val errMsg = requestBodies.asSequence().map {
            send(it)
        }.joinToString(", ")
        if (errMsg.isNotBlank())
            throw RemoteServiceException(errMsg)
    }

    private fun send(weworkMessage: WeweokRobotBaseMessage): Optional<Throwable> {
        val url = buildUrl("$weworkHost/cgi-bin/webhook/send?key=$robotKey")
        val requestBody = JsonUtil.toJson(weworkMessage)
        return OkhttpUtils.doPost(url, requestBody).use {
            val responseBody = it.body()?.string() ?: ""
            kotlin.runCatching {
                val sendMessageResp = JsonUtil.to(responseBody, jacksonTypeRef<WeworkSendMessageResp>())
                if (!it.isSuccessful || 0 != sendMessageResp.errCode) {
                    throw RemoteServiceException(
                        httpStatus = it.code(),
                        responseContent = responseBody,
                        errorMessage = "send wework robot message failed：${sendMessageResp.errMsg}",
                        errorCode = sendMessageResp.errCode
                    )
                }
            }.fold({ Optional.empty() }, { e ->
                logger.warn("${it.request()}|send wework robot message failed, $responseBody")
                Optional.of(e)
            })
        }
    }

    private fun buildUrl(url: String): String {
        return if (url.startsWith("http")) url else "https://$url"
    }

    private fun saveResult(receivers: Collection<String>, body: String, success: Boolean, errMsg: String?) {
        weworkNotifyDao.insertOrUpdateWeworkNotifyRecord(
            success = success,
            lastErrorMessage = errMsg,
            receivers = receivers.joinToString(","),
            body = body
        )
    }

    private fun checkMessageSize(message: String): Boolean {
        if (message.length < WEWORK_MAX_SIZE) {
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(WeworkRobotServiceImpl::class.java)
        const val WEWORK_MAX_SIZE = 4000
    }
}
