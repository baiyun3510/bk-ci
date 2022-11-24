package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import io.swagger.annotations.ApiModelProperty

data class WorkspaceReq(
    @ApiModelProperty("工作空间ID")
    val workspaceId: Long,
    @ApiModelProperty("工作空间名称")
    val name: String,
    @ApiModelProperty("远程开发仓库地址")
    val repositoryUrl: String,
    @ApiModelProperty("仓库分支")
    val branch: String,
    @ApiModelProperty("devfile配置路径")
    val devFilePath: String?
)
