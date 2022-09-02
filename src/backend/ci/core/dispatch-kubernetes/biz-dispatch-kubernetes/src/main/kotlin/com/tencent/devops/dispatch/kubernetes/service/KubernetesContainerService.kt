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

package com.tencent.devops.dispatch.kubernetes.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.dispatch.kubernetes.client.KubernetesBuilderClient
import com.tencent.devops.dispatch.kubernetes.client.KubernetesTaskClient
import com.tencent.devops.dispatch.kubernetes.common.ConstantsMessage
import com.tencent.devops.dispatch.kubernetes.common.ENV_JOB_BUILD_TYPE
import com.tencent.devops.dispatch.kubernetes.common.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.kubernetes.common.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.kubernetes.common.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.kubernetes.common.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.common.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.kubernetes.components.LogsPrinter
import com.tencent.devops.dispatch.kubernetes.interfaces.ContainerService
import com.tencent.devops.dispatch.kubernetes.pojo.Builder
import com.tencent.devops.dispatch.kubernetes.pojo.DeleteBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.DispatchBuildLog
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesBuilderStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesDockerRegistry
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesResource
import com.tencent.devops.dispatch.kubernetes.pojo.Pool
import com.tencent.devops.dispatch.kubernetes.pojo.StartBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.StopBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.TaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildBuilderStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildOperateBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildOperateBuilderType
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.canReStart
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchBuilderDebugStatus
import com.tencent.devops.dispatch.kubernetes.pojo.getCodeMessage
import com.tencent.devops.dispatch.kubernetes.pojo.hasException
import com.tencent.devops.dispatch.kubernetes.pojo.isFailed
import com.tencent.devops.dispatch.kubernetes.pojo.isRunning
import com.tencent.devops.dispatch.kubernetes.pojo.isStarting
import com.tencent.devops.dispatch.kubernetes.pojo.isSuccess
import com.tencent.devops.dispatch.kubernetes.pojo.readyToStart
import com.tencent.devops.dispatch.kubernetes.utils.CommonUtils
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("kubernetesContainerService")
class KubernetesContainerService @Autowired constructor(
    private val logsPrinter: LogsPrinter,
    private val kubernetesTaskClient: KubernetesTaskClient,
    private val kubernetesBuilderClient: KubernetesBuilderClient
) : ContainerService {

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesContainerService::class.java)
    }

    override val shutdownLockBaseKey = "dispatch_kubernetes_shutdown_lock_"

    override val log = DispatchBuildLog(
        readyStartLog = "准备创建kubernetes构建机...",
        startContainerError = "启动kubernetes构建容器失败，请联系蓝盾助手反馈处理.\n容器构建异常请参考：",
        troubleShooting = "Kubernetes构建异常，请联系蓝盾助手排查，异常信息 - "
    )

    @Value("\${kubernetes.resources.builder.cpu}")
    override var cpu: Double = 32.0

    @Value("\${kubernetes.resources.builder.memory}")
    override var memory: String = "65535"

    @Value("\${kubernetes.resources.builder.disk}")
    override var disk: String = "500"

    @Value("\${kubernetes.entrypoint}")
    override val entrypoint: String = "kubernetes_init.sh"

    override val helpUrl: String? = ""

    override fun getBuilderStatus(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        retryTime: Int
    ): Result<DispatchBuildBuilderStatus> {
        val result = kubernetesBuilderClient.getBuilderDetail(
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            name = builderName,
            retryTime = retryTime
        )
        if (result.isNotOk()) {
            return Result(result.status, result.message)
        }

        val status = when {
            result.data!!.readyToStart() -> DispatchBuildBuilderStatus.READY_START
            result.data.hasException() -> DispatchBuildBuilderStatus.HAS_EXCEPTION
            result.data.canReStart() -> DispatchBuildBuilderStatus.CAN_RESTART
            result.data.isRunning() -> DispatchBuildBuilderStatus.RUNNING
            result.data.isStarting() -> DispatchBuildBuilderStatus.STARTING
            else -> DispatchBuildBuilderStatus.UNKNOWN
        }

        return Result(status)
    }

    override fun operateBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        param: DispatchBuildOperateBuilderParams
    ): String {
        return kubernetesBuilderClient.operateBuilder(
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            name = builderName,
            param = when (param.type) {
                DispatchBuildOperateBuilderType.DELETE -> DeleteBuilderParams()
                DispatchBuildOperateBuilderType.STOP -> StopBuilderParams()
                DispatchBuildOperateBuilderType.START_SLEEP -> StartBuilderParams(
                    env = param.env,
                    command = listOf("/bin/sh", entrypoint)
                )
            }
        )
    }

    override fun createAndStartBuilder(
        dispatchMessages: DispatchMessage,
        containerPool: Pool,
        poolNo: Int,
        cpu: Double,
        mem: String,
        disk: String
    ): Pair<String, String> {
        with(dispatchMessages) {
            val (host, name, tag) = CommonUtils.parseImage(containerPool.container!!)
            val userName = containerPool.credential?.user
            val password = containerPool.credential?.password
            val registry = if (host.isBlank() || userName.isNullOrBlank() || password.isNullOrBlank()) {
                null
            } else {
                KubernetesDockerRegistry(host, userName, password)
            }

            val builderName = getOnlyName(userId)
            val taskId = kubernetesBuilderClient.createBuilder(
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                builder = Builder(
                    name = builderName,
                    image = "$host/$name:$tag",
                    registry = registry,
                    resource = KubernetesResource(
                        requestCPU = cpu.toString(),
                        requestDisk = "${disk}G",
                        requestDiskIO = "1",
                        requestMem = "${memory}Mi",
                        limitCpu = cpu.toString(),
                        limitDisk = "${disk}G",
                        limitDiskIO = "1",
                        limitMem = "${memory}Mi"
                    ),
                    env = mapOf(
                        ENV_KEY_PROJECT_ID to projectId,
                        ENV_KEY_AGENT_ID to id,
                        ENV_KEY_AGENT_SECRET_KEY to secretKey,
                        ENV_KEY_GATEWAY to gateway,
                        "TERM" to "xterm-256color",
                        SLAVE_ENVIRONMENT to "Kubernetes",
                        ENV_JOB_BUILD_TYPE to (dispatchType?.buildType()?.name ?: BuildType.KUBERNETES.name)
                    ),
                    command = listOf("/bin/sh", entrypoint),
                    nfs = null,
                    privateBuilder = null,
                    specialBuilder = null
                )
            )
            logger.info(
                "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo createBuilder, " +
                    "taskId:($taskId)"
            )
            logsPrinter.printLogs(
                this, "下发创建构建机请求成功，builderName: $builderName 等待机器创建..."
            )

            val (taskStatus, failedMsg) = kubernetesTaskClient.waitTaskFinish(userId, taskId)

            if (taskStatus == TaskStatusEnum.SUCCEEDED) {
                // 启动成功
                logger.info(
                    "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo " +
                        "create kubernetes vm success, wait vm start..."
                )
                logsPrinter.printLogs(this, "构建机创建成功，等待机器启动...")
            } else {
                // 清除构建异常容器，并重新置构建池为空闲
                clearExceptionBuilder(builderName)
                throw BuildFailureException(
                    ErrorCodeEnum.CREATE_VM_ERROR.errorType,
                    ErrorCodeEnum.CREATE_VM_ERROR.errorCode,
                    ErrorCodeEnum.CREATE_VM_ERROR.formatErrorMessage,
                    "${ConstantsMessage.TROUBLE_SHOOTING}构建机创建失败:${failedMsg ?: taskStatus.message}"
                )
            }
            return Pair(startBuilder(dispatchMessages, builderName, poolNo, cpu, mem, disk), builderName)
        }
    }

    override fun startBuilder(
        dispatchMessages: DispatchMessage,
        builderName: String,
        poolNo: Int,
        cpu: Double,
        mem: String,
        disk: String
    ): String {
        with(dispatchMessages) {
            return kubernetesBuilderClient.operateBuilder(
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                name = builderName,
                param = StartBuilderParams(
                    env = mapOf(
                        ENV_KEY_PROJECT_ID to projectId,
                        ENV_KEY_AGENT_ID to id,
                        ENV_KEY_AGENT_SECRET_KEY to secretKey,
                        ENV_KEY_GATEWAY to gateway,
                        "TERM" to "xterm-256color",
                        SLAVE_ENVIRONMENT to "Kubernetes",
                        ENV_JOB_BUILD_TYPE to (dispatchType?.buildType()?.name ?: BuildType.KUBERNETES.name)
                    ),
                    command = listOf("/bin/sh", entrypoint)
                )
            )
        }
    }

    private fun DispatchMessage.clearExceptionBuilder(builderName: String) {
        try {
            // 下发删除，不管成功失败
            logger.info("[$buildId]|[$vmSeqId] Delete builder, userId: $userId, builderName: $builderName")
            kubernetesBuilderClient.operateBuilder(
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                name = builderName,
                param = DeleteBuilderParams()
            )
        } catch (e: Exception) {
            logger.error("[$buildId]|[$vmSeqId] delete builder failed", e)
        }
    }

    override fun waitTaskFinish(userId: String, taskId: String): DispatchBuildTaskStatus {
        val startResult = kubernetesTaskClient.waitTaskFinish(userId, taskId)
        return if (startResult.first == TaskStatusEnum.SUCCEEDED) {
            DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.SUCCEEDED, null)
        } else {
            DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.FAILED, startResult.second)
        }
    }

    override fun getTaskStatus(userId: String, taskId: String): DispatchBuildStatusResp {
        val taskResponse = kubernetesTaskClient.getTasksStatus(userId, taskId)
        val status = TaskStatusEnum.realNameOf(taskResponse.data?.status)
        if (taskResponse.isNotOk() || taskResponse.data == null) {
            // 创建失败
            val msg = "${taskResponse.message ?: taskResponse.getCodeMessage()}"
            logger.error("Execute task: $taskId failed, actionCode is ${taskResponse.status}, msg: $msg")
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, msg)
        }
        // 请求成功但是任务失败
        if (status != null && status.isFailed()) {
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, taskResponse.data.detail)
        }
        return when {
            status!!.isRunning() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.running.name)
            status.isSuccess() -> {
                DispatchBuildStatusResp(DispatchBuildStatusEnum.succeeded.name)
            }

            else -> DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status.message)
        }
    }

    override fun waitDebugBuilderRunning(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String
    ): DispatchBuilderDebugStatus {
        val status = kubernetesBuilderClient.waitContainerRunning(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            containerName = builderName
        )
        return when (status) {
            KubernetesBuilderStatusEnum.READY_TO_RUN, KubernetesBuilderStatusEnum.SUCCEEDED ->
                DispatchBuilderDebugStatus.CAN_RESTART

            KubernetesBuilderStatusEnum.RUNNING -> DispatchBuilderDebugStatus.RUNNING
            KubernetesBuilderStatusEnum.PENDING -> DispatchBuilderDebugStatus.STARTING
            else -> DispatchBuilderDebugStatus.UNKNOWN
        }
    }

    override fun getDebugWebsocketUrl(
        projectId: String,
        pipelineId: String,
        staffName: String,
        builderName: String
    ): String {
        return kubernetesBuilderClient.getWebsocketUrl(projectId, pipelineId, staffName, builderName).data!!
    }

    override fun buildAndPushImage(
        userId: String,
        projectId: String,
        buildId: String,
        dispatchBuildImageReq: DispatchBuildImageReq
    ): DispatchTaskResp {
        logger.info(
            "projectId: $projectId, buildId: $buildId build and push image. " +
                JsonUtil.toJson(dispatchBuildImageReq)
        )

        return DispatchTaskResp(
            kubernetesBuilderClient.buildAndPushImage(
                userId, dispatchBuildImageReq
            )
        )
    }

    private fun getOnlyName(userId: String): String {
        val subUserId = if (userId.length > 14) {
            userId.substring(0 until 14)
        } else {
            userId
        }
        return "${subUserId}${System.currentTimeMillis()}-" +
                RandomStringUtils.randomAlphabetic(8).toLowerCase()
    }
}
