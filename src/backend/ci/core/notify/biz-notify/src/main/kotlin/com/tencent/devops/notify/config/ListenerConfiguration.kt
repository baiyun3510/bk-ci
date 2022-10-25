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

package com.tencent.devops.notify.config

import com.tencent.devops.common.event.annotation.StreamEventConsumer
import com.tencent.devops.notify.QUEUE_NOTIFY_EMAIL
import com.tencent.devops.notify.QUEUE_NOTIFY_RTX
import com.tencent.devops.notify.QUEUE_NOTIFY_SMS
import com.tencent.devops.notify.QUEUE_NOTIFY_WECHAT
import com.tencent.devops.notify.QUEUE_NOTIFY_WEWORK
import com.tencent.devops.notify.consumer.NotifyMessageConsumer
import com.tencent.devops.notify.model.EmailNotifyMessageWithOperation
import com.tencent.devops.notify.model.RtxNotifyMessageWithOperation
import com.tencent.devops.notify.model.SmsNotifyMessageWithOperation
import com.tencent.devops.notify.model.WechatNotifyMessageWithOperation
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import java.util.function.Consumer

@Configuration
class ListenerConfiguration {

    companion object {
        const val STREAM_CONSUMER_GROUP = "notify-service"
    }

    @StreamEventConsumer(QUEUE_NOTIFY_RTX, STREAM_CONSUMER_GROUP)
    fun rtxNotifyListener(
        @Autowired listener: NotifyMessageConsumer
    ): Consumer<Message<RtxNotifyMessageWithOperation>> {
        return Consumer { event: Message<RtxNotifyMessageWithOperation> ->
            listener.onReceiveRtxMessage(event.payload)
        }
    }

    @StreamEventConsumer(QUEUE_NOTIFY_EMAIL, STREAM_CONSUMER_GROUP)
    fun emailNotifyListener(
        @Autowired listener: NotifyMessageConsumer
    ): Consumer<Message<EmailNotifyMessageWithOperation>> {
        return Consumer { event: Message<EmailNotifyMessageWithOperation> ->
            listener.onReceiveEmailMessage(event.payload)
        }
    }

    @StreamEventConsumer(QUEUE_NOTIFY_SMS, STREAM_CONSUMER_GROUP)
    fun smsNotifyListener(
        @Autowired listener: NotifyMessageConsumer
    ): Consumer<Message<SmsNotifyMessageWithOperation>> {
        return Consumer { event: Message<SmsNotifyMessageWithOperation> ->
            listener.onReceiveSmsMessage(event.payload)
        }
    }

    @StreamEventConsumer(QUEUE_NOTIFY_WECHAT, STREAM_CONSUMER_GROUP)
    fun wechatNotifyListener(
        @Autowired listener: NotifyMessageConsumer
    ): Consumer<Message<WechatNotifyMessageWithOperation>> {
        return Consumer { event: Message<WechatNotifyMessageWithOperation> ->
            listener.onReceiveWechatMessage(event.payload)
        }
    }

    @StreamEventConsumer(QUEUE_NOTIFY_WEWORK, STREAM_CONSUMER_GROUP)
    fun weworkNotifyListener(
        @Autowired listener: NotifyMessageConsumer
    ): Consumer<Message<WeworkNotifyMessageWithOperation>> {
        return Consumer { event: Message<WeworkNotifyMessageWithOperation> ->
            listener.onReceiveWeworkMessage(event.payload)
        }
    }
}
