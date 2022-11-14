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

package com.tencent.devops.lambda.pojo.bkdata

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

/**
 * 数据平台查询接口请求参数对象
 */
data class BkDataQueryRequest(
    @JsonProperty("bkdata_authentication_method")
    @ApiModelProperty("校验方法", required = true, name = "bkdata_authentication_method")
    override val authenticationMethod: String,
    @JsonProperty("bkdata_data_token")
    @ApiModelProperty("token", required = true, name = "bkdata_data_token")
    override val dataToken: String,
    @JsonProperty("bk_app_code")
    @ApiModelProperty("蓝鲸应用编码", required = true, name = "bk_app_code")
    override val bkAppCode: String,
    @JsonProperty("bk_app_secret")
    @ApiModelProperty("蓝鲸应用私密key", required = true, name = "bk_app_secret")
    override val bkAppSecret: String,
    @JsonProperty("sql")
    @ApiModelProperty("查询SQL", required = true, name = "sql")
    val sql: String,
    @JsonProperty("prefer_storage")
    @ApiModelProperty("查询引擎", required = false, name = "prefer_storage")
    var preferStorage: String? = null
) : BkDataBaseRequest(authenticationMethod, dataToken, bkAppCode, bkAppSecret)
