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
package com.tencent.devops.lambda.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildCommitFinishEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildTaskFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.lambda.listener.LambdaBuildCommitFinishListener
import com.tencent.devops.lambda.listener.LambdaBuildTaskFinishListener
import com.tencent.devops.lambda.listener.LambdaBuildFinishListener
import com.tencent.devops.lambda.listener.LambdaPipelineModelListener
import com.tencent.devops.lambda.service.project.LambdaProjectService
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import java.util.function.Consumer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message

@Configuration
class LambdaMQConfiguration {

    companion object {
        const val STREAM_CONSUMER_GROUP = "lambda-service"
    }

    /**
     * 构建结束广播交换机
     */
    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, STREAM_CONSUMER_GROUP)
    fun pipelineBuildFinishListener(
        @Autowired lambdaBuildFinishListener: LambdaBuildFinishListener
    ): Consumer<Message<PipelineBuildFinishBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildFinishBroadCastEvent> ->
            lambdaBuildFinishListener.execute(event.payload)
        }
    }

    /**
     * 任务结束广播交换机
     */
    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_ELEMENT_FINISH_FANOUT, STREAM_CONSUMER_GROUP)
    fun pipelineBuildElementFinishListener(
        @Autowired lambdaBuildTaskFinishListener: LambdaBuildTaskFinishListener
    ): Consumer<Message<PipelineBuildTaskFinishBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildTaskFinishBroadCastEvent> ->
            lambdaBuildTaskFinishListener.execute(event.payload)
        }
    }

    /**
     * 构建project创建广播交换机
     */
    @EventConsumer(StreamBinding.EXCHANGE_PROJECT_CREATE_FANOUT, STREAM_CONSUMER_GROUP)
    fun projectCreateListener(
        @Autowired lambdaProjectService: LambdaProjectService
    ): Consumer<Message<ProjectCreateBroadCastEvent>> {
        return Consumer { event: Message<ProjectCreateBroadCastEvent> ->
            lambdaProjectService.onReceiveProjectCreate(event.payload)
        }
    }

    /**
     * 构建project更新广播交换机
     */
    @EventConsumer(StreamBinding.EXCHANGE_PROJECT_UPDATE_FANOUT, STREAM_CONSUMER_GROUP)
    fun projectUpdateListener(
        @Autowired lambdaProjectService: LambdaProjectService
    ): Consumer<Message<ProjectUpdateBroadCastEvent>> {
        return Consumer { event: Message<ProjectUpdateBroadCastEvent> ->
            lambdaProjectService.onReceiveProjectUpdate(event.payload)
        }
    }

    /**
     * 构建model更新广播交换机
     */
    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_EXTENDS_FANOUT, STREAM_CONSUMER_GROUP)
    fun pipelineModelAnalysisListener(
        @Autowired lambdaPipelineModelListener: LambdaPipelineModelListener
    ): Consumer<Message<PipelineModelAnalysisEvent>> {
        return Consumer { event: Message<PipelineModelAnalysisEvent> ->
            lambdaPipelineModelListener.run(event.payload)
        }
    }

    /**
     * webhook commits完成事件交换机
     */
    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_COMMIT_FINISH_FANOUT, STREAM_CONSUMER_GROUP)
    fun pipelineBuildCommitsFinishListener(
        @Autowired lambdaBuildCommitFinishListener: LambdaBuildCommitFinishListener
    ): Consumer<Message<PipelineBuildCommitFinishEvent>> {
        return Consumer { event: Message<PipelineBuildCommitFinishEvent> ->
            lambdaBuildCommitFinishListener.run(event.payload)
        }
    }
}
