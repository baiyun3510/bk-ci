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

package com.tencent.devops.worker.common.api.store

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.constant.BK_CI_SERVICE_DIR
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.artifactory.constant.REALM_LOCAL
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.dto.UpdateExtServiceEnvInfoDTO
import com.tencent.devops.store.pojo.vo.ServiceEnvVO
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.archive.ArtifactoryBuildResourceApi
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.net.URLEncoder

class ExtServiceResourceApi : AbstractBuildResourceApi(), ExtServiceSDKApi {

    private val realm = ArtifactoryBuildResourceApi().getRealm()

    override fun updateExtServiceEnv(
        projectId: String,
        serviceCode: String,
        version: String,
        updateExtServiceEnvInfo: UpdateExtServiceEnvInfoDTO
    ): Result<Boolean> {
        val path = "/ms/store/api/build/ext/services/env/projects/$projectId/services/$serviceCode/versions/$version"
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            objectMapper.writeValueAsString(updateExtServiceEnvInfo)
        )
        val request = buildPut(path, body)
        val responseContent = request(request, "updateExtServiceEnv fail")
        return objectMapper.readValue(responseContent)
    }

    override fun downloadServicePkg(
        serviceFilePath: String,
        file: File,
        isVmBuildEnv: Boolean
    ) {
        val filePath = when (realm) {
            REALM_LOCAL -> "$BK_CI_SERVICE_DIR/$serviceFilePath"
            REALM_BK_REPO -> "/bk-store/service/$serviceFilePath"
            else -> throw IllegalArgumentException("unknown artifactory realm")
        }
        val path = "/ms/artifactory/api/build/artifactories/file/download?filePath=${
            URLEncoder.encode(
                filePath,
                "UTF-8"
            )
        }"
        val request = buildGet(path = path, useFileDevnetGateway = isVmBuildEnv)
        download(request, file)
    }

    override fun getExtServiceEnv(serviceCode: String, version: String): Result<ServiceEnvVO> {
        val path = "/ms/store/api/build/ext/services/env/services/$serviceCode/versions/$version"
        val request = buildGet(path)
        val responseContent = request(request, "get service env fail")
        return objectMapper.readValue(responseContent)
    }
}
