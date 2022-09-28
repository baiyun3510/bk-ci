package com.tencent.devops.worker.common.task.market

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskDaemon
import com.tencent.devops.worker.common.task.TaskFactory
import java.io.File

class MarketTriggerAtomTask : ITask() {

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val triggerBuildTask = with(buildTask) {
            BuildTask(
                buildId = buildId,
                vmSeqId = vmSeqId,
                status = BuildTaskStatus.DO,
                taskId = taskId,
                type = "marketBuildLess",
                params = mapOf(
                    "id" to "e-9170c3938ea24055b0a41499092f1404",
                    "name" to "git事件触发-插件",
                    "atomCode" to "gitTrigger",
                    "data" to JsonUtil.toJson(mapOf("desc" to "ddfdf"))
                )
            )
        }
        val triggerBuildVariables = buildVariables.copy(
            variables = buildVariables.variables.plus(
                "BK_CI_EVENT_ACTION" to "REGISTER"
            )
        )
        val task = TaskFactory.create(LinuxScriptElement.classType)
        val taskDaemon = TaskDaemon(task, triggerBuildTask, triggerBuildVariables, workspace)
        taskDaemon.runWithTimeout()
    }
}
