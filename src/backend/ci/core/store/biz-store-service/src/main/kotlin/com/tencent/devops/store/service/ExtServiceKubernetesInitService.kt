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

import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceKubernetesResource
import com.tencent.devops.dispatch.kubernetes.pojo.CreateImagePullSecretRequest
import com.tencent.devops.dispatch.kubernetes.pojo.CreateKubernetesNameSpaceRequest
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesLabel
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesLimitRange
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesRepo
import com.tencent.devops.store.config.ExtServiceKubernetesConfig
import com.tencent.devops.store.config.ExtServiceKubernetesLimitRangeConfig
import com.tencent.devops.store.config.ExtServiceKubernetesNameSpaceConfig
import com.tencent.devops.store.config.ExtServiceImageSecretConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
@DependsOn("springContextUtil")
class ExtServiceKubernetesInitService @Autowired constructor(
    private val client: Client,
    private val extServiceKubernetesConfig: ExtServiceKubernetesConfig,
    private val extServiceKubernetesNameSpaceConfig: ExtServiceKubernetesNameSpaceConfig,
    private val extServiceKubernetesLimitRangeConfig: ExtServiceKubernetesLimitRangeConfig,
    private val extServiceImageSecretConfig: ExtServiceImageSecretConfig
) {

    private val logger = LoggerFactory.getLogger(ExtServiceKubernetesInitService::class.java)

    @PostConstruct
    fun initKubernetesEnv() {
        // 异步初始化微扩展k8s环境信息
        Thread {
            initKubernetesNamespace()
            initKubernetesImagePullSecret()
        }.start()
    }

    fun initKubernetesNamespace() {
        logger.info("begin execute initKubernetesNamespace")
        // 初始化微扩展命名空间（包括已发布微扩展版本的命名空间和处于测试中微扩展版本的命名空间）
        val namespaceName = extServiceKubernetesNameSpaceConfig.namespaceName
        val createKubernetesNameSpaceRequest = CreateKubernetesNameSpaceRequest(
            apiUrl = extServiceKubernetesConfig.masterUrl,
            token = extServiceKubernetesConfig.token,
            kubernetesLabel = KubernetesLabel(
                labelKey = extServiceKubernetesNameSpaceConfig.labelKey,
                labelValue = extServiceKubernetesNameSpaceConfig.labelValue
            ),
            limitRangeInfo = KubernetesLimitRange(
                defaultCpu = extServiceKubernetesLimitRangeConfig.defaultCpu,
                defaultMemory = extServiceKubernetesLimitRangeConfig.defaultMemory,
                defaultRequestCpu = extServiceKubernetesLimitRangeConfig.defaultRequestCpu,
                defaultRequestMemory = extServiceKubernetesLimitRangeConfig.defaultRequestMemory,
                limitType = extServiceKubernetesLimitRangeConfig.limitType
            )
        )
        // 创建已发布微扩展版本的命名空间
        val releaseNamespaceResult = client.get(ServiceKubernetesResource::class).createNamespace(
            namespaceName = namespaceName,
            createKubernetesNameSpaceRequest = createKubernetesNameSpaceRequest
        )
        logger.info("createReleaseNamespaceResult result:[$namespaceName|$releaseNamespaceResult]")
        val grayNamespaceName = extServiceKubernetesNameSpaceConfig.grayNamespaceName
        // 创建测试中微扩展版本的命名空间
        val grayNamespaceResult = client.get(ServiceKubernetesResource::class).createNamespace(
            namespaceName = grayNamespaceName,
            createKubernetesNameSpaceRequest = createKubernetesNameSpaceRequest
        )
        logger.info("createGrayNamespaceResult result:[$grayNamespaceName|$grayNamespaceResult]")
        logger.info("end execute initKubernetesNamespace")
    }

    fun initKubernetesImagePullSecret() {
        logger.info("begin execute initKubernetesImagePullSecret")
        val secretName = extServiceImageSecretConfig.secretName
        val createImagePullSecretRequest = CreateImagePullSecretRequest(
            apiUrl = extServiceKubernetesConfig.masterUrl,
            token = extServiceKubernetesConfig.token,
            kubernetesRepo = KubernetesRepo(
                registryUrl = extServiceImageSecretConfig.repoRegistryUrl,
                username = extServiceImageSecretConfig.repoUsername,
                password = extServiceImageSecretConfig.repoPassword,
                email = extServiceImageSecretConfig.repoEmail
            )
        )
        // 创建已发布微扩展版本的命名空间拉取镜像secret
        val createReleaseNsImagePullSecretResult = client.get(ServiceKubernetesResource::class)
            .createImagePullSecretTest(
                namespaceName = extServiceKubernetesNameSpaceConfig.namespaceName,
                secretName = secretName,
                createImagePullSecretRequest = createImagePullSecretRequest
            )
        logger.info("createReleaseNsImagePullSecret result:[$secretName|$createReleaseNsImagePullSecretResult]")
        // 创建已发布微扩展版本的命名空间拉取镜像secret
        val graySecretName = extServiceImageSecretConfig.graySecretName
        val createGrayNsImagePullSecretResult = client.get(ServiceKubernetesResource::class)
            .createImagePullSecretTest(
                namespaceName = extServiceKubernetesNameSpaceConfig.grayNamespaceName,
                secretName = graySecretName,
                createImagePullSecretRequest = createImagePullSecretRequest
            )
        logger.info("createGrayNsImagePullSecretResult result:[$graySecretName|$createGrayNsImagePullSecretResult]")
        logger.info("end execute initKubernetesImagePullSecret")
    }
}
