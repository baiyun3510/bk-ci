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

import com.tencent.devops.common.api.constant.KEY_BRANCH
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_HASH_ID
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_PATH
import com.tencent.devops.common.api.constant.KEY_SCRIPT
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.MASTER
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineInitResource
import com.tencent.devops.process.pojo.ExtServiceBuildInitPipelineReq
import com.tencent.devops.process.utils.KEY_PIPELINE_NAME
import com.tencent.devops.store.config.ExtServiceImageSecretConfig
import com.tencent.devops.store.config.ExtServiceKubernetesNameSpaceConfig
import com.tencent.devops.store.dao.ExtServiceBuildInfoDao
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.KEY_EXT_SERVICE_DEPLOY_INFO
import com.tencent.devops.store.pojo.common.KEY_EXT_SERVICE_IMAGE_INFO
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.KEY_SERVICE_CODE
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ExtServiceBaseInfoDTO
import com.tencent.devops.store.pojo.dto.ExtServiceImageInfoDTO
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtServicePipelineService @Autowired constructor(
    private val client: Client,
    private val extServiceDao: ExtServiceDao,
    private val extServiceFeatureDao: ExtServiceFeatureDao,
    private val extServiceBuildInfoDao: ExtServiceBuildInfoDao,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storePipelineBuildRelDao: StorePipelineBuildRelDao,
    private val businessConfigDao: BusinessConfigDao,
    private val extServiceKubernetesService: ExtServiceKubernetesService,
    private val extServiceKubernetesNameSpaceConfig: ExtServiceKubernetesNameSpaceConfig,
    private val extServiceImageSecretConfig: ExtServiceImageSecretConfig
) {

    private val logger = LoggerFactory.getLogger(ExtServicePipelineService::class.java)

    fun runPipeline(context: DSLContext, serviceId: String, userId: String): Boolean {
        val serviceRecord = extServiceDao.getServiceById(context, serviceId) ?: return false
        val serviceCode = serviceRecord.serviceCode
        val version = serviceRecord.version
        val servicePipelineRelRecord = storePipelineRelDao.getStorePipelineRel(
            dslContext = context,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE
        )
        val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = context,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE.type.toByte()
        ) // 查找新增微扩展时关联的项目
        val buildInfo = extServiceBuildInfoDao.getServiceBuildInfo(context, serviceId)
        logger.info("service[$serviceCode] buildInfo is:$buildInfo")
        val script = buildInfo.value1()
        val language = buildInfo.value3()
        val repoAddr = extServiceImageSecretConfig.repoRegistryUrl
        val imageName = "${extServiceImageSecretConfig.imageNamePrefix}$serviceCode"
        val extServiceImageInfo = ExtServiceImageInfoDTO(
            imageName = imageName,
            imageTag = version,
            repoAddr = repoAddr,
            username = extServiceImageSecretConfig.repoUsername,
            password = extServiceImageSecretConfig.repoPassword
        )
        // 未正式发布的微扩展先部署到bcs灰度环境
        val deployApp = extServiceKubernetesService.generateDeployApp(
            userId = userId,
            namespaceName = extServiceKubernetesNameSpaceConfig.grayNamespaceName,
            serviceCode = serviceCode,
            version = version
        )
        if (null == servicePipelineRelRecord) {
            // 为用户初始化构建流水线并触发执行
            val serviceBaseInfo = ExtServiceBaseInfoDTO(
                serviceId = serviceId,
                serviceCode = serviceCode,
                version = serviceRecord.version,
                extServiceImageInfo = extServiceImageInfo,
                extServiceDeployInfo = deployApp
            )
            val pipelineModelConfig = businessConfigDao.get(
                dslContext = context,
                business = StoreTypeEnum.SERVICE.name,
                feature = "initBuildPipeline",
                businessValue = "PIPELINE_MODEL"
            )
            var pipelineModel = pipelineModelConfig!!.configValue
            val pipelineName = "am-$serviceCode-${UUIDUtil.generate()}"
            val extServiceFeature = extServiceFeatureDao.getServiceByCode(context, serviceCode)!!
            val repositoryHashId = extServiceFeature.repositoryHashId
            val repositoryPath = buildInfo.value2()
            val paramMap = mapOf(
                KEY_PIPELINE_NAME to pipelineName,
                KEY_STORE_CODE to serviceCode,
                KEY_VERSION to version,
                KEY_LANGUAGE to language,
                KEY_REPOSITORY_HASH_ID to repositoryHashId,
                KEY_REPOSITORY_PATH to (repositoryPath ?: ""),
                KEY_BRANCH to MASTER
            )
            // 将流水线模型中的变量替换成具体的值
            paramMap.forEach { (key, value) ->
                pipelineModel = pipelineModel.replace("#{$key}", value)
            }
            val extServiceBuildInitPipelineReq = ExtServiceBuildInitPipelineReq(
                pipelineModel = pipelineModel,
                repositoryHashId = repositoryHashId,
                repositoryPath = repositoryPath,
                script = script,
                extServiceBaseInfo = serviceBaseInfo
            )
            val serviceMarketInitPipelineResp = client.get(ServicePipelineInitResource::class)
                .initExtServiceBuildPipeline(userId, projectCode!!, extServiceBuildInitPipelineReq).data
            logger.info("the serviceMarketInitPipelineResp is:$serviceMarketInitPipelineResp")
            if (null != serviceMarketInitPipelineResp) {
                storePipelineRelDao.add(
                    dslContext = context,
                    storeCode = serviceCode,
                    storeType = StoreTypeEnum.SERVICE,
                    pipelineId = serviceMarketInitPipelineResp.pipelineId
                )
                extServiceDao.setServiceStatusById(
                    dslContext = context,
                    serviceId = serviceId,
                    serviceStatus = serviceMarketInitPipelineResp.extServiceStatus.status.toByte(),
                    userId = userId,
                    msg = null
                )
                val buildId = serviceMarketInitPipelineResp.buildId
                if (null != buildId) {
                    storePipelineBuildRelDao.add(
                        dslContext = context,
                        storeId = serviceId,
                        pipelineId = serviceMarketInitPipelineResp.pipelineId,
                        buildId = buildId
                    )
                }
            }
        } else {
            // 触发执行流水线
            val startParams = mutableMapOf<String, String>() // 启动参数
            startParams[KEY_SERVICE_CODE] = serviceCode
            startParams[KEY_VERSION] = serviceRecord.version
            startParams[KEY_EXT_SERVICE_IMAGE_INFO] = JsonUtil.toJson(extServiceImageInfo)
            startParams[KEY_EXT_SERVICE_DEPLOY_INFO] = JsonUtil.toJson(deployApp)
            startParams[KEY_SCRIPT] = script
            val buildIdObj = client.get(ServiceBuildResource::class).manualStartup(
                userId, projectCode!!, servicePipelineRelRecord.pipelineId, startParams,
                ChannelCode.AM
            ).data
            logger.info("the buildIdObj is:$buildIdObj")
            if (null != buildIdObj) {
                storePipelineBuildRelDao.add(
                    dslContext = context,
                    storeId = serviceId,
                    pipelineId = servicePipelineRelRecord.pipelineId,
                    buildId = buildIdObj.id
                )
                extServiceDao.setServiceStatusById(
                    dslContext = context,
                    serviceId = serviceId,
                    serviceStatus = ExtServiceStatusEnum.BUILDING.status.toByte(),
                    userId = userId,
                    msg = null
                ) // 构建中
            } else {
                extServiceDao.setServiceStatusById(
                    dslContext = context,
                    serviceId = serviceId,
                    serviceStatus = ExtServiceStatusEnum.BUILD_FAIL.status.toByte(),
                    userId = userId,
                    msg = null
                ) // 构建失败
            }
        }
        return true
    }
}
