package com.tencent.devops.process.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.annotations.ApiModelProperty

data class ReportArtifactoryInfo(
    @ApiModelProperty("构件名称", required = true)
    val name: String,
    @ApiModelProperty("构件tag", required = true)
    val tag: String?,
    @ApiModelProperty("构件仓库路径", required = true)
    val path: String,
    @ApiModelProperty("构件大小", required = true)
    val size: Long,
    @ApiModelProperty("仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @ApiModelProperty("是否文件夹", required = true)
    val folder: Boolean,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String
)
