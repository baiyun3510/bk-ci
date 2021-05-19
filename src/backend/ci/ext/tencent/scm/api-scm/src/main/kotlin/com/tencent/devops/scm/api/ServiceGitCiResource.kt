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

package com.tencent.devops.scm.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.repository.pojo.oauth.GitToken
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SCM_GIT_CI"], description = "Service Code GIT CI resource")
@Path("/service/gitci/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGitCiResource {

    @ApiOperation("获取项目的token")
    @GET
    @Path("/getToken")
    fun getToken(
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<GitToken>

    @ApiOperation("校验用户git项目权限")
    @GET
    @Path("/checkUserGitAuth")
    fun checkUserGitAuth(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<Boolean>

    @ApiOperation("获取项目的token")
    @DELETE
    @Path("/clearToken")
    fun clearToken(
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String
    ): Result<Boolean>

    @ApiOperation("获取GitCode项目成员信息")
    @GET
    @Path("/getMembers")
    fun getMembers(
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "项目ID或者全路径", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam(value = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20,
        @ApiParam(value = "搜索用户关键字", required = true)
        @QueryParam("search")
        search: String?
    ): Result<List<GitMember>>

    @ApiOperation("校验用户git项目权限")
    @GET
    @Path("/getUserId")
    fun getGitUserId(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        rtxUserId: String,
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<String?>
}
