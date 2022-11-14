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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_HASH_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.service.service.ServiceItemResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.KEY_CODE_SRC
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.constants.KEY_EXT_SERVICE_ITEMS_PREFIX
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.enums.ExtServicePackageSourceTypeEnum
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TxExtServiceReleaseService : ExtServiceReleaseService() {

    private val logger = LoggerFactory.getLogger(TxExtServiceReleaseService::class.java)

    @Value("\${git.service.nameSpaceId}")
    private lateinit var serviceNameSpaceId: String

    @Autowired
    private lateinit var storeVisibleDeptService: StoreVisibleDeptService

    override fun doServiceVisibleBus(userId: String, serviceCode: String, deptList: List<DeptInfo>) {
        storeVisibleDeptService.addVisibleDept(
            userId = userId,
            storeType = StoreTypeEnum.SERVICE,
            storeCode = serviceCode,
            deptInfos = deptList
        )
    }

    override fun handleServicePackage(
        extensionInfo: InitExtServiceDTO,
        userId: String,
        serviceCode: String
    ): Result<Map<String, String>?> {
        logger.info("handleServicePackage params:[$extensionInfo|$serviceCode|$userId]")
        extensionInfo.authType ?: return MessageCodeUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("authType"),
            data = null
        )
        extensionInfo.visibilityLevel ?: return MessageCodeUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("visibilityLevel"),
            data = null
        )
        val repositoryInfo: RepositoryInfo?
        if (extensionInfo.visibilityLevel == VisibilityLevelEnum.PRIVATE) {
            if (extensionInfo.privateReason.isNullOrBlank()) {
                return MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("privateReason"),
                    data = null
                )
            }
        }
        // 把扩展服务对应的扩展点放入redis中（初始化扩展服务代码库的时候需将extension.json改成用户对应的模板）
        val itemInfoList = client.get(ServiceItemResource::class).getItemInfoByIds(extensionInfo.extensionItemList).data
        val itemCodeSet = mutableSetOf<String>()
        itemInfoList?.forEach {
            itemCodeSet.add(it.itemCode)
        }
        redisOperation.set(
            key = "$KEY_EXT_SERVICE_ITEMS_PREFIX:$serviceCode",
            value = JsonUtil.toJson(itemCodeSet),
            expiredInSecond = TimeUnit.DAYS.toSeconds(1)
        )
        // 远程调工蜂接口创建代码库
        try {
            val createGitRepositoryResult = client.get(ServiceGitRepositoryResource::class).createGitCodeRepository(
                userId = userId,
                projectCode = extensionInfo.projectCode,
                repositoryName = serviceCode,
                sampleProjectPath = storeBuildInfoDao.getStoreBuildInfoByLanguage(
                    dslContext = dslContext,
                    language = extensionInfo.language!!,
                    storeType = StoreTypeEnum.SERVICE
                ).sampleProjectPath,
                namespaceId = serviceNameSpaceId.toInt(),
                visibilityLevel = extensionInfo.visibilityLevel,
                tokenType = TokenTypeEnum.PRIVATE_KEY
            )
            logger.info("the createGitRepositoryResult is :$createGitRepositoryResult")
            if (createGitRepositoryResult.isOk()) {
                repositoryInfo = createGitRepositoryResult.data
            } else {
                return Result(createGitRepositoryResult.status, createGitRepositoryResult.message, null)
            }
        } catch (ignored: Throwable) {
            logger.error("service[$serviceCode] createGitCodeRepository fail!", ignored)
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
        }
        if (null == repositoryInfo) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
        }
        val repositoryHashId = repositoryInfo.repositoryHashId ?: ""
        return Result(mapOf(KEY_REPOSITORY_HASH_ID to repositoryHashId, KEY_CODE_SRC to repositoryInfo.url))
    }

    override fun getExtServicePackageSourceType(serviceCode: String): ExtServicePackageSourceTypeEnum {
        return ExtServicePackageSourceTypeEnum.REPO
    }
}
