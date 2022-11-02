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

package com.tencent.devops.store.service

import com.tencent.devops.artifactory.api.ServiceArchiveStoreFileResource
import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.config.BkRepoConfig
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxExtServiceManageService : ExtServiceManageService() {

    private val logger = LoggerFactory.getLogger(TxExtServiceManageService::class.java)

    @Autowired
    private lateinit var bkRepoConfig: BkRepoConfig

    override fun doServiceDeleteBus(
        userId: String,
        serviceId: String,
        serviceCode: String,
        initProjectCode: String
    ) {
        //  删除仓库镜像
        try {
            val serviceEnvRecord = extServiceEnvDao.getMarketServiceEnvInfoByServiceId(dslContext, serviceId)
            if (serviceEnvRecord != null && serviceEnvRecord.imagePath.isNotEmpty()) {
                deleteNode(serviceCode)
            }
        } catch (ignored: Throwable) {
            logger.warn("delete service[$serviceCode] repository image fail!", ignored)
        }
        // 删除代码库
        val extServiceRecord = extServiceFeatureDao.getServiceByCode(dslContext, serviceCode)
        deleteExtServiceRepository(
            userId = userId,
            projectCode = initProjectCode,
            repositoryHashId = extServiceRecord!!.repositoryHashId
        )
    }

    override fun getFileStr(
        serviceCode: String,
        version: String,
        fileName: String,
        repositoryHashId: String?
    ): String? {
        // 从工蜂拉取文件
        try {
            return client.get(ServiceGitRepositoryResource::class).getFileContent(
                repoId = repositoryHashId!!,
                filePath = fileName,
                reversion = null,
                branch = null,
                repositoryType = null
            ).data
        } catch (ignored: RemoteServiceException) {
            logger.warn("getFileContent fileName:$fileName error", ignored)
            if (ignored.httpStatus == HTTP_404 || ignored.errorCode == HTTP_404) {
                return ""
            } else {
                throw ignored
            }
        }
    }

    fun deleteNode(serviceCode: String) {
        val serviceUrlPrefix = client.getServiceUrl(ServiceArchiveStoreFileResource::class)
        val serviceUrl = "$serviceUrlPrefix/service/artifactories/store/file/repos/" +
            "${bkRepoConfig.bkrepoDockerRepoName}/$serviceCode/delete?type=${StoreTypeEnum.SERVICE.name}"
        val request = Request.Builder()
            .url(serviceUrl)
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body()!!.string()
                throw RemoteServiceException("delete node file failed: $responseContent", response.code())
            }
        }
    }

    private fun deleteExtServiceRepository(
        userId: String,
        projectCode: String?,
        repositoryHashId: String
    ) {
        // 删除代码库信息
        if (!projectCode.isNullOrEmpty() && repositoryHashId.isNotBlank()) {
            try {
                val delGitRepositoryResult =
                    client.get(ServiceGitRepositoryResource::class)
                        .delete(
                            userId = userId,
                            projectId = projectCode,
                            repositoryHashId = repositoryHashId,
                            tokenType = TokenTypeEnum.PRIVATE_KEY
                        )
                logger.info("the delGitRepositoryResult is :$delGitRepositoryResult")
            } catch (ignored: Throwable) {
                logger.warn("delete service git repository fail!", ignored)
            }
        }
    }
}
