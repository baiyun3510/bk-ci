package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.ApiModelProperty

data class Publishers(
    val publishersCode: String,
    val name: String,
    val publishersType: PublisherType,
    val owners: List<String>,
    val members: List<String>,
    val helper: String,
    val certificationFlag: Boolean,
    val storeType: StoreTypeEnum,
    val organization: String,
    val ownerDeptName: String
)
