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

package com.tencent.devops.dispatch.kubernetes.resource.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceKubernetesResource
import com.tencent.devops.dispatch.kubernetes.pojo.CreateKubernetesNameSpaceRequest
import com.tencent.devops.dispatch.kubernetes.pojo.CreateImagePullSecretRequest
import com.tencent.devops.dispatch.kubernetes.pojo.DeployApp
import com.tencent.devops.dispatch.kubernetes.pojo.StopApp
import com.tencent.devops.dispatch.kubernetes.service.KubernetesDeployService
import com.tencent.devops.dispatch.kubernetes.service.KubernetesQueryService
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesApiUtils
import io.fabric8.kubernetes.api.model.apps.Deployment
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceBcsResourceImpl @Autowired constructor(
    private val kubernetesDeployService: KubernetesDeployService,
    private val kubernetesQueryService: KubernetesQueryService
) : ServiceKubernetesResource {

    override fun createNamespace(
        namespaceName: String,
        createKubernetesNameSpaceRequest: CreateKubernetesNameSpaceRequest
    ): Result<Boolean> {
        KubernetesApiUtils.createNamespace(
            apiUrl = createKubernetesNameSpaceRequest.apiUrl,
            token = createKubernetesNameSpaceRequest.token,
            namespaceName = namespaceName,
            labelInfo = createKubernetesNameSpaceRequest.kubernetesLabel,
            limitRangeInfo = createKubernetesNameSpaceRequest.limitRangeInfo
        )
        return Result(true)
    }

    override fun createImagePullSecretTest(
        namespaceName: String,
        secretName: String,
        createImagePullSecretRequest: CreateImagePullSecretRequest
    ): Result<Boolean> {
        KubernetesApiUtils.createImagePullSecret(
            apiUrl = createImagePullSecretRequest.apiUrl,
            token = createImagePullSecretRequest.token,
            secretName = secretName,
            namespaceName = namespaceName,
            kubernetesRepoInfo = createImagePullSecretRequest.kubernetesRepo
        )
        return Result(true)
    }

    override fun deployApp(userId: String, deployApp: DeployApp): Result<Boolean> {
        return kubernetesDeployService.deployApp(userId, deployApp)
    }

    override fun stopApp(userId: String, stopApp: StopApp): Result<Boolean> {
        return kubernetesDeployService.stopApp(userId, stopApp)
    }

    override fun getDeploymentInfo(
        namespaceName: String,
        deploymentName: String,
        apiUrl: String,
        token: String
    ): Result<Deployment> {
        return kubernetesQueryService.getBcsDeploymentInfo("", namespaceName, deploymentName, apiUrl, token)
    }

    override fun getDeploymentInfos(
        namespaceName: String,
        deploymentNames: String,
        apiUrl: String,
        token: String
    ): Result<Map<String, Deployment>> {
        return kubernetesQueryService.getBcsDeploymentInfos("", namespaceName, deploymentNames, apiUrl, token)
    }
}
