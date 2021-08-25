package com.tencent.devops.gitci.common.exception

import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.GitCICommitCheckState
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.v2.GitCIBasicSetting

open class TriggerBaseException(
    val requestEvent: GitRequestEvent,
    val reasonParams: List<String>? = null,
    val gitEvent: GitEvent? = null,
    val commitCheck: CommitCheck? = null,
    val basicSetting: GitCIBasicSetting? = null,
    val pipeline: GitProjectPipeline? = null,
    val yamls: Yamls? = null,
    val version: String? = null,
    val filePath: String? = null
) : Exception()

data class CommitCheck(
    val isNoPipelineCheck: Boolean = false,
//    val push: Boolean,
    val block: Boolean,
    val state: GitCICommitCheckState
)

data class Yamls(
    val originYaml: String?,
    val parsedYaml: String?,
    val normalYaml: String?
)
