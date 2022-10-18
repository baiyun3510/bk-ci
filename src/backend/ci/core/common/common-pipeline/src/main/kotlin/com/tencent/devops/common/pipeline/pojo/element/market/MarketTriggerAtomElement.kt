package com.tencent.devops.common.pipeline.pojo.element.market

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-插件市场第三方触发类插件", description = MarketBuildAtomElement.classType)
data class MarketTriggerAtomElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "任务名称由用户自己填写",
    @ApiModelProperty("id将由后台生成", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null
) : Element(name, id, status) {

    companion object {
        const val classType = "marketTrigger"
    }

    override fun getClassType() = classType
}
