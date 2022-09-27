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

package com.tencent.devops.store.service.common.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.service.ServiceBkRepoResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.store.service.common.TxOpMigrateStoreFileService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Files

abstract class TxOpMigrateStoreFileServiceImpl @Autowired constructor(
    private val client: Client
) : TxOpMigrateStoreFileService {

    companion object {
        private val logger = LoggerFactory.getLogger(TxOpMigrateStoreFileServiceImpl::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
    }

    protected fun getBkRepoFileUrl(fileUrl: String, userId: String): String? {
        val fileType = getFileType(fileUrl)
        val tmpFile = Files.createTempFile(UUIDUtil.generate(), ".$fileType").toFile()
        val fileName = tmpFile.name
        try {
            // 从s3下载logo
            OkhttpUtils.downloadFile(fileUrl, tmpFile)
            // 把logo上传至bkrepo
            val serviceUrlPrefix = client.getServiceUrl(ServiceBkRepoResource::class)
            val destPath = "file/$fileType/$fileName"
            val serviceUrl =
                "$serviceUrlPrefix/service/bkrepo/statics/file/upload?userId=$userId&destPath=$destPath"
            OkhttpUtils.uploadFile(serviceUrl, tmpFile).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.warn("$userId upload file:$fileName fail,responseContent:$responseContent")
                }
                val result = JsonUtil.to(responseContent, object : TypeReference<Result<String?>>() {})
                logger.info("requestUrl:$serviceUrl,result:$result")
                return result.data
            }
        } catch (ignore: Throwable) {
            logger.warn("$userId upload file:$fileName fail, error is:", ignore)
        } finally {
            // 删除临时文件
            tmpFile.delete()
        }
        return null
    }

    private fun getFileType(fileUrl: String): String {
        val paramIndex = fileUrl.lastIndexOf("?")
        val url = if (paramIndex > 0) fileUrl.substring(0, paramIndex) else fileUrl
        val index = url.lastIndexOf(".")
        return url.substring(index + 1).toLowerCase()
    }
}