package com.tencent.devops.store.pojo.service

data class OpEditInfoDTO(
    val baseInfo: OpExtBaseInfo?,
    val mediaInfo: List<OpMediaInfo>?,
    val settingInfo: OpSettingInfo?
)