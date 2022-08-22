package com.tencent.devops.store.pojo.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
data class SearchUserAndDeptEntity(
    @ApiModelProperty("查找字段, 默认值为 'id'", name = "lookup_field")
    @JsonProperty("lookup_field")
    val lookupField: String,
    @ApiModelProperty("返回值字段")
    val fields: String?,
    @ApiModelProperty("精确查找内容列表", name = "exact_lookups")
    @JsonProperty("exact_lookups")
    val exactLookups: Any? = null,
    @ApiModelProperty("模糊查找内容列表", name = "fuzzy_lookups")
    @JsonProperty("fuzzy_lookups")
    val fuzzyLookups: Any? = null,
    @ApiModelProperty("用户登录态信息", name = "access_token")
    @JsonProperty("access_token")
    val accessToken: String? = null,
    @ApiModelProperty("分页大小", name = "page_size")
    @JsonProperty("page_size")
    val pageSize: Int? = 200,
    var bk_app_code: String,
    var bk_app_secret: String,
    var bk_username: String,
    val bk_token: String = ""
)
