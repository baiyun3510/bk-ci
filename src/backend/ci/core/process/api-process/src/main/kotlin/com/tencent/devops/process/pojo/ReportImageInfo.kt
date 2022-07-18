package com.tencent.devops.process.pojo

import com.fasterxml.jackson.annotation.JsonAlias
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.annotations.ApiModelProperty

data class ReportImageInfo(
    @ApiModelProperty("镜像名称", required = true)
    val name: String,
    @ApiModelProperty("镜像tag", required = false)
    val tag: String,
    @ApiModelProperty("镜像路径", required = true)
    val path: String,
    @ApiModelProperty("文件类型", required = true)
    @JsonAlias("type")
    val fileType: String,
    @ApiModelProperty("镜像大小", required = true)
    val size: Long,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String
)
