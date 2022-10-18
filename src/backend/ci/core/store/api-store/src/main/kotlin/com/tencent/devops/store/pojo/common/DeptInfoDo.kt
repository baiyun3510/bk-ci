package com.tencent.devops.store.pojo.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
data class DeptInfoDo(
    @ApiModelProperty("组织ID")
    val id: Int,
    @ApiModelProperty("组织名称")
    val name: String,
    @ApiModelProperty("父级组织")
    val parent: Int,
    @ApiModelProperty("是否有子级", name = "has_children")
    @JsonProperty("has_children")
    val hasChildren: Boolean,
    @ApiModelProperty("是否启用")
    val enabled: Boolean
)
