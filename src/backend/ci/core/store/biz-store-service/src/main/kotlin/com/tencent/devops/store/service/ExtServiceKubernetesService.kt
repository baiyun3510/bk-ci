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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceKubernetesResource
import com.tencent.devops.dispatch.kubernetes.pojo.AppDeployment
import com.tencent.devops.dispatch.kubernetes.pojo.AppIngress
import com.tencent.devops.dispatch.kubernetes.pojo.AppService
import com.tencent.devops.dispatch.kubernetes.pojo.DeployApp
import com.tencent.devops.dispatch.kubernetes.pojo.StopApp
import com.tencent.devops.store.config.ExtServiceKubernetesConfig
import com.tencent.devops.store.config.ExtServiceKubernetesNameSpaceConfig
import com.tencent.devops.store.config.ExtServiceDeploymentConfig
import com.tencent.devops.store.config.ExtServiceImageSecretConfig
import com.tencent.devops.store.config.ExtServiceIngressConfig
import com.tencent.devops.store.config.ExtServiceServiceConfig
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.ExtServiceFeatureUpdateInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreEnvVarService
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.time.LocalDateTime

@Service
class ExtServiceKubernetesService {

    private val logger = LoggerFactory.getLogger(ExtServiceKubernetesService::class.java)

    @Autowired
    private lateinit var client: Client

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var extServiceKubernetesConfig: ExtServiceKubernetesConfig

    @Autowired
    private lateinit var extServiceKubernetesNameSpaceConfig: ExtServiceKubernetesNameSpaceConfig

    @Autowired
    private lateinit var extServiceImageSecretConfig: ExtServiceImageSecretConfig

    @Autowired
    private lateinit var extServiceDeploymentConfig: ExtServiceDeploymentConfig

    @Autowired
    private lateinit var extServiceServiceConfig: ExtServiceServiceConfig

    @Autowired
    private lateinit var extServiceIngressConfig: ExtServiceIngressConfig

    @Autowired
    private lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    private lateinit var extServiceFeatureDao: ExtServiceFeatureDao

    @Autowired
    private lateinit var storeEnvVarService: StoreEnvVarService

    fun generateDeployApp(
        userId: String,
        namespaceName: String,
        serviceCode: String,
        version: String,
        checkPermissionFlag: Boolean = true
    ): DeployApp {
        val imageName = "${extServiceImageSecretConfig.imageNamePrefix}$serviceCode"
        val grayFlag = namespaceName == extServiceKubernetesNameSpaceConfig.grayNamespaceName
        val host = if (grayFlag) extServiceIngressConfig.grayHost else extServiceIngressConfig.host
        val scopes = "ALL," + if (grayFlag) "TEST" else "PRD"
        val storeEnvVarInfoListResult = storeEnvVarService.getLatestEnvVarList(
            userId = userId,
            storeType = StoreTypeEnum.SERVICE.name,
            storeCode = serviceCode,
            scopes = scopes,
            isDecrypt = true,
            checkPermissionFlag = checkPermissionFlag
        )
        if (storeEnvVarInfoListResult.isNotOk()) {
            throw ErrorCodeException(errorCode = storeEnvVarInfoListResult.status.toString())
        }
        val storeEnvVarInfoList = storeEnvVarInfoListResult.data
        var envVarList: List<EnvVar>? = null
        if (storeEnvVarInfoList != null) {
            envVarList = mutableListOf()
            storeEnvVarInfoList.forEach {
                envVarList.add(EnvVar(it.varName, it.varValue, null))
            }
        }
        return DeployApp(
            apiUrl = extServiceKubernetesConfig.masterUrl,
            token = extServiceKubernetesConfig.token,
            namespaceName = namespaceName,
            appCode = serviceCode,
            appDeployment = AppDeployment(
                replicas = if (grayFlag) {
                    extServiceDeploymentConfig.grayReplicas.toInt()
                } else {
                    extServiceDeploymentConfig.replicas.toInt()
                },
                image = "${extServiceImageSecretConfig.repoRegistryUrl}/$imageName:$version",
                pullImageSecretName = if (grayFlag) {
                    extServiceDeploymentConfig.grayPullImageSecretName
                } else {
                    extServiceDeploymentConfig.pullImageSecretName
                },
                containerPort = extServiceDeploymentConfig.containerPort.toInt(),
                envVarList = envVarList
            ),
            appService = AppService(
                servicePort = extServiceServiceConfig.servicePort.toInt()
            ),
            appIngress = AppIngress(
                host = MessageFormat(host).format(arrayOf(serviceCode)),
                contextPath = extServiceIngressConfig.contextPath,
                ingressAnnotationMap = mapOf(
                    "kubernetes.io/ingress.class" to extServiceIngressConfig.annotationClass,
                    "kubernetes.io/ingress.existLbId" to if (grayFlag) {
                        extServiceIngressConfig.annotationGrayExistLbId
                    } else {
                        extServiceIngressConfig.annotationExistLbId
                    }
                )
            ),
            deployTimeOut = extServiceKubernetesConfig.deployTimeOut.toInt()
        )
    }

    /**
     * 部署微扩展应用
     * @param userId 用户ID
     * @param grayFlag 是否是灰度标识
     * @param serviceCode 微扩展代码
     * @param version 微扩展版本号
     * @param checkPermissionFlag 是否需要校验权限（op系统操作不需要校验）
     */
    fun deployExtService(
        userId: String,
        grayFlag: Boolean,
        serviceCode: String,
        version: String,
        checkPermissionFlag: Boolean = true
    ): Result<Boolean> {
        logger.info("deployExtService params:[$userId|$grayFlag|$serviceCode|$version|$checkPermissionFlag]")
        if (checkPermissionFlag && !storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE.type.toByte())
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val namespaceName = if (!grayFlag) {
            extServiceKubernetesNameSpaceConfig.namespaceName
        } else {
            extServiceKubernetesNameSpaceConfig.grayNamespaceName
        }
        val deployApp = generateDeployApp(
            userId = userId,
            namespaceName = namespaceName,
            serviceCode = serviceCode,
            version = version,
            checkPermissionFlag = checkPermissionFlag
        )
        val kubernetesDeployAppResult = client.get(ServiceKubernetesResource::class).deployApp(
            userId = userId,
            deployApp = deployApp
        )
        logger.info("kubernetesDeployAppResult is :$kubernetesDeployAppResult")
        if (kubernetesDeployAppResult.isOk() && !grayFlag) {
            // 当微扩展正式发布后，需针对灰度环境部署的扩展应用标记停止部署，如果一段时间内还没有处于测试或审核中的版本则停掉灰度环境的应用
            extServiceFeatureDao.updateExtServiceFeatureBaseInfo(
                dslContext = dslContext,
                serviceCode = serviceCode,
                userId = userId,
                extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                    killGrayAppFlag = true,
                    killGrayAppMarkTime = LocalDateTime.now()
                )
            )
        }
        return kubernetesDeployAppResult
    }

    /**
     * 停止微扩展应用
     * @param userId 用户ID
     * @param serviceCode 微扩展代码
     * @param deploymentName deployment名称
     * @param serviceName service名称
     * @param checkPermissionFlag 是否需要校验权限（op系统操作不需要校验）
     * @param grayFlag 是否是灰度标识
     */
    fun stopExtService(
        userId: String,
        serviceCode: String,
        deploymentName: String,
        serviceName: String,
        checkPermissionFlag: Boolean = true,
        grayFlag: Boolean? = null
    ): Result<Boolean> {
        logger.info("stopExtService params:[$userId|$serviceCode|$deploymentName|$serviceName|" +
            "$checkPermissionFlag|$grayFlag")
        if (checkPermissionFlag && !storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE.type.toByte())
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        var grayNamespaceName = ""
        var grayHost = ""
        var namespaceName = ""
        var host = ""
        when {
            grayFlag == null -> {
                grayNamespaceName = extServiceKubernetesNameSpaceConfig.grayNamespaceName
                grayHost = extServiceIngressConfig.grayHost
                namespaceName = extServiceKubernetesNameSpaceConfig.namespaceName
                host = extServiceIngressConfig.host
            }
            grayFlag -> {
                grayNamespaceName = extServiceKubernetesNameSpaceConfig.grayNamespaceName
                grayHost = extServiceIngressConfig.grayHost
            }
            else -> {
                namespaceName = extServiceKubernetesNameSpaceConfig.namespaceName
                host = extServiceIngressConfig.host
            }
        }
        // 停止微扩展部署
        val kubernetesStopAppResult = client.get(ServiceKubernetesResource::class).stopApp(
            userId = userId,
            stopApp = StopApp(
                apiUrl = extServiceKubernetesConfig.masterUrl,
                token = extServiceKubernetesConfig.token,
                grayNamespaceName = grayNamespaceName,
                grayHost = grayHost,
                namespaceName = namespaceName,
                host = host,
                deploymentName = serviceCode,
                serviceName = "$serviceCode-service"
            )
        )
        logger.info("kubernetesStopAppResult is :$kubernetesStopAppResult")
        return kubernetesStopAppResult
    }

    fun getExtServiceDeployStatus(
        userId: String,
        serviceCode: String,
        grayFlag: Boolean?
    ): Result<DeploymentStatus?> {
        logger.info("getExtServiceDeployStatus userId is:$userId,serviceCode is:$serviceCode,grayFlag is:$grayFlag")
        // 判断用户是否有权限查询部署状态
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE.type.toByte())
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val deployment = client.get(ServiceKubernetesResource::class).getDeploymentInfo(
            namespaceName = if (grayFlag == null || !grayFlag) {
                extServiceKubernetesNameSpaceConfig.namespaceName
            } else {
                extServiceKubernetesNameSpaceConfig.grayNamespaceName
            },
            deploymentName = serviceCode,
            apiUrl = extServiceKubernetesConfig.masterUrl,
            token = extServiceKubernetesConfig.token
        ).data
        logger.info("getExtServiceDeployStatus deployment is:$deployment")
        return Result(deployment?.status)
    }
}
