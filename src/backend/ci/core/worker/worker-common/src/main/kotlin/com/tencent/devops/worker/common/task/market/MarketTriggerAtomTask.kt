package com.tencent.devops.worker.common.task.market

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
                type = "linuxScript",
                params = mapOf(
                    "id" to "e-02653654700a494680e1c21cfefb5d9d",
                    "scriptType" to "SHELL",
                    "script" to "echo hello"
                )
            )
        }
        val task = TaskFactory.create(LinuxScriptElement.classType)
        val taskDaemon = TaskDaemon(task, triggerBuildTask, buildVariables, workspace)
        taskDaemon.runWithTimeout()
    }
}
