package com.tencent.devops.store.pojo.common

data class Platforms(
    val platforms: String,
    val name: String,
    val labels: List<String>,
    val website: String,
    val description: String,
    val owner: List<String>,
    val ownerDeptName: String
)
