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

package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.dto.SubmitDTO
import com.tencent.devops.store.pojo.vo.ExtServiceRespItem
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXTENSION_SERVICE_DESK"], description = "服务扩展--工作台")
@Path("/user/market/desk/service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceDeskResource {

    @POST
    @ApiOperation(value = "工作台--初始化微扩展")
    @Path("/")
    fun initExtensionService(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("微扩展信息")
        @Valid
        extensionInfo: InitExtServiceDTO
    ): Result<Boolean>

    @PUT
    @ApiOperation(value = "工作台-升级扩展")
    @Path("/")
    fun submitExtensionService(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("微扩展信息")
        extensionInfo: SubmitDTO
    ): Result<String>

    @GET
    @ApiOperation(value = "根据扩展ID获取扩展版本进度")
    @Path("/release/process/{serviceId}")
    fun getExtensionServiceProcessInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("微扩展Id")
        @PathParam("serviceId")
        serviceId: String
    ): Result<StoreProcessInfo>

    @ApiOperation("工作台--下架微扩展")
    @PUT
    @Path("/{serviceCode}/offline/")
    fun offlineService(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceCode", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("下架请求报文")
        serviceOffline: ServiceOfflineDTO
    ): Result<Boolean>

    @GET
    @ApiOperation(value = "工作台--根据用户获取服务扩展列表")
    @Path("/list")
    fun listDeskExtService(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("微扩展name", required = false)
        @QueryParam("serviceName")
        serviceName: String?,
        @ApiParam("页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam("每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        pageSize: Int = 10
    ): Result<Page<ExtServiceRespItem>>

    @ApiOperation("工作台--删除微扩展")
    @DELETE
    @Path("/{serviceCode}/delete")
    fun deleteExtensionService(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("微扩展Code", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<Boolean>

    @ApiOperation("根据插件版本ID获取插件详情")
    @GET
    @Path("/{serviceId}")
    fun getServiceDetails(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<ServiceVersionVO?>

    @ApiOperation("获取微扩展支持的语言列表")
    @GET
    @Path("/desk/service/language")
    fun listLanguage(): Result<List<String?>>

    @ApiOperation("微扩展取消发布")
    @PathParam("serviceId")
    @PUT
    @Path("/release/cancel/{serviceId}")
    fun cancelRelease(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<Boolean>

    @ApiOperation("微扩展确认通过测试")
    @PathParam("serviceId")
    @PUT
    @Path("/release/passTest/{serviceId}")
    fun passTest(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<Boolean>
}
