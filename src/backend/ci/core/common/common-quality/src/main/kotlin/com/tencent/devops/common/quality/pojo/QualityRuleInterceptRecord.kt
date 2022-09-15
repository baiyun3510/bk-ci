/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.quality.pojo

import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-拦截规则拦截记录")
data class QualityRuleInterceptRecord(
    @ApiModelProperty("指标ID", required = true)
    val indicatorId: String,
    @ApiModelProperty("指标名称", required = true)
    val indicatorName: String,
    @ApiModelProperty("指标插件类型", required = false)
    val indicatorType: String?,
    @ApiModelProperty("关系", required = true)
    val operation: QualityOperation,
    @ApiModelProperty("阈值值大小", required = true)
    val value: String?,
    @ApiModelProperty("实际值", required = true)
    val actualValue: String?,
    @ApiModelProperty("控制点", required = true)
    val controlPoint: String,
    @ApiModelProperty("是否通过", required = true)
    val pass: Boolean,
    @ApiModelProperty("指标详情", required = true)
    val detail: String?,
    @ApiModelProperty("指标日志输出详情", required = false)
    var logPrompt: String?,
    @ApiModelProperty("指标权重", required = false)
    var weight: Int? = null
)
