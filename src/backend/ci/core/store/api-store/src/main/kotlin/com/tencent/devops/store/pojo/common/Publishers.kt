package com.tencent.devops.store.pojo.common

import io.swagger.annotations.ApiModelProperty

data class Publishers(
    val publishersCode: String,
    val name: String,
    val owners: List<String>,
    val members: List<String>,
    val helper: String,
    val organization: String,
    val ownerDeptName: String
)
