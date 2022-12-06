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

package com.tencent.devops.dispatch.kubernetes.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.kubernetes.pojo.AppIngress
import com.tencent.devops.dispatch.kubernetes.pojo.DeployApp
import com.tencent.devops.dispatch.kubernetes.pojo.StopApp
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesApiUtils
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Probe
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.api.model.TCPSocketAction
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeployment
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue
import io.fabric8.kubernetes.api.model.extensions.Ingress
import io.fabric8.kubernetes.api.model.extensions.IngressBackend
import io.fabric8.kubernetes.api.model.extensions.IngressBackendBuilder
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder
import io.fabric8.kubernetes.api.model.extensions.IngressRule
import io.fabric8.kubernetes.client.KubernetesClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Collections
import java.text.MessageFormat

@Service
class KubernetesDeployService @Autowired constructor(private val redisOperation: RedisOperation) {

    private val logger = LoggerFactory.getLogger(KubernetesDeployService::class.java)

    private final val defaultLabelKey = "app"

    private final val ingressRedisPrefixKey = "ext:service:ingress"

    private final val dateConfigName = "date-config"

    private final val dateConfigPath = "/etc/localtime"

    fun deployApp(
        userId: String,
        deployApp: DeployApp
    ): Result<Boolean> {
        logger.info("deployApp userId is: $userId,app is: $deployApp")
        val apiUrl = deployApp.apiUrl
        val token = deployApp.token
        val namespaceName = deployApp.namespaceName
        val serviceCode = deployApp.appCode
        val appDeployment = deployApp.appDeployment
        val appService = deployApp.appService
        val appIngress = deployApp.appIngress
        val containerPort = appDeployment.containerPort
        // 创建deployment的pod部署策略为滚动更新，maxUnavailable 为0，maxSurge为1
        val deploymentStrategy = DeploymentStrategy(
            RollingUpdateDeployment(IntOrString(1), IntOrString(0)), "RollingUpdate"
        )
        // 创建deployment无状态部署
        val deployment = DeploymentBuilder()
            .withNewMetadata()
            .withName(serviceCode)
            .endMetadata()
            .withNewSpec()
            .withReplicas(appDeployment.replicas)
            .withStrategy(deploymentStrategy)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels(defaultLabelKey, serviceCode)
            .addToAnnotations("dummy", "du_" + System.currentTimeMillis()) // 保证滚动更新时每次从仓库拉取最新镜像
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName(serviceCode)
            .withImage(appDeployment.image)
            .withImagePullPolicy("Always") // 更新为总是从仓库拉取镜像
            .addNewPort()
            .withContainerPort(containerPort)
            .endPort()
            .addNewVolumeMount()
            .withName(dateConfigName)
            .withMountPath(dateConfigPath)
            .endVolumeMount()
            .withEnv(appDeployment.envVarList)
            // 存活探针来确定何时重启容器
            .withLivenessProbe(getTcpSocketProbe(containerPort, 10, 20))
            // 就绪探针来确定容器是否已经就绪可以接受流量
            .withReadinessProbe(getTcpSocketProbe(containerPort, 60, 10))
            .endContainer()
            .addNewImagePullSecret()
            .withName(appDeployment.pullImageSecretName)
            .endImagePullSecret()
            .addNewVolume()
            .withName(dateConfigName)
            .withNewHostPath()
            .withPath(dateConfigPath)
            .endHostPath()
            .endVolume()
            .endSpec()
            .endTemplate()
            .withNewSelector()
            .addToMatchLabels(defaultLabelKey, serviceCode)
            .endSelector()
            .endSpec()
            .build()
        KubernetesApiUtils.createDeployment(apiUrl, token, namespaceName, deployment)
        logger.info("created deployment:$deployment")
        val servicePort = appService.servicePort
        val service = ServiceBuilder()
            .withNewMetadata()
            .withName(getServiceName(serviceCode))
            .endMetadata()
            .withNewSpec()
            .addToSelector(Collections.singletonMap(defaultLabelKey, serviceCode))
            .addNewPort()
            .withName("$serviceCode-port")
            .withProtocol("TCP")
            .withPort(servicePort)
            .withTargetPort(IntOrString(containerPort))
            .endPort()
            .withType("NodePort")
            .endSpec()
            .build()
        KubernetesApiUtils.createService(apiUrl, token, namespaceName, service)
        // 创建ingress
        // generate ingress backend
        val ingressBackend: IngressBackend = IngressBackendBuilder()
            .withServiceName(getServiceName(serviceCode))
            .withNewServicePort(servicePort)
            .build()
        // generate ingress path
        val ingressPath = HTTPIngressPathBuilder()
            .withBackend(ingressBackend)
            .withPath(appIngress.contextPath).build()
        val ingressRule = IngressRule(
            appIngress.host,
            HTTPIngressRuleValue(
                listOf(ingressPath)
            )
        )
        val ingressRedisKey = "$ingressRedisPrefixKey:$namespaceName"
        val ingressName = redisOperation.get(ingressRedisKey)
        logger.info("deployApp ingressName is: $ingressName")
        if (ingressName.isNullOrBlank()) {
            createIngress(
                apiUrl = apiUrl,
                token = token,
                namespaceName = namespaceName,
                serviceCode = serviceCode,
                appIngress = appIngress,
                ingressRule = ingressRule,
                ingressRedisKey = ingressRedisKey
            )
        } else {
            val bcsKubernetesClient = KubernetesApiUtils.getKubernetesClient(apiUrl, token)
            val ingress =
                bcsKubernetesClient.extensions().ingresses().inNamespace(namespaceName).withName(ingressName).get()
            if (ingress == null) {
                createIngress(
                    apiUrl = apiUrl,
                    token = token,
                    namespaceName = namespaceName,
                    serviceCode = serviceCode,
                    appIngress = appIngress,
                    ingressRule = ingressRule,
                    ingressRedisKey = ingressRedisKey
                )
            } else {
                when {
                    ingress.spec.rules.contains(ingressRule) -> return Result(true)
                    else -> {
                        ingress.spec.rules.add(ingressRule)
                        KubernetesApiUtils.createIngress(apiUrl, token, namespaceName, ingress)
                        logger.info("update ingress:$ingressName success")
                    }
                }
            }
        }
        return Result(true)
    }

    private fun getTcpSocketProbe(containerPort: Int, initialDelaySeconds: Int, periodSeconds: Int): Probe {
        val tcpSocket = TCPSocketAction()
        tcpSocket.port = IntOrString(containerPort)
        val probe = Probe()
        probe.initialDelaySeconds = initialDelaySeconds
        probe.tcpSocket = tcpSocket
        probe.periodSeconds = periodSeconds
        return probe
    }

    private fun getServiceName(serviceCode: String) = "$serviceCode-service"

    private fun createIngress(
        apiUrl: String,
        token: String,
        namespaceName: String,
        serviceCode: String,
        appIngress: AppIngress,
        ingressRule: IngressRule,
        ingressRedisKey: String
    ): Ingress {
        val ingress = IngressBuilder()
            .withNewMetadata()
            .withName(getIngressName(namespaceName))
            .addToLabels(defaultLabelKey, serviceCode)
            .addToAnnotations(appIngress.ingressAnnotationMap)
            .endMetadata()
            .withNewSpec()
            .withRules(ingressRule)
            .endSpec()
            .build()
        KubernetesApiUtils.createIngress(apiUrl, token, namespaceName, ingress)
        redisOperation.set(
            key = ingressRedisKey,
            value = getIngressName(namespaceName),
            expiredInSecond = null,
            expired = false
        )
        return ingress
    }

    private fun getIngressName(namespaceName: String) = "$namespaceName-ingress"

    fun stopApp(
        userId: String,
        stopApp: StopApp
    ): Result<Boolean> {
        logger.info("bcsStopApp userId is: $userId,stopApp is: $stopApp")
        val apiUrl = stopApp.apiUrl
        val token = stopApp.token
        val deploymentName = stopApp.deploymentName
        val serviceName = stopApp.serviceName
        // 停止灰度命名空间的应用
        val grayNamespaceName = stopApp.grayNamespaceName
        if (grayNamespaceName.isNotEmpty()) {
            doStopAppBus(
                namespaceName = grayNamespaceName,
                deploymentName = deploymentName,
                serviceName = serviceName,
                host = stopApp.grayHost,
                apiUrl = apiUrl,
                token = token
            )
        }
        // 停止正式命名空间的应用
        val namespaceName = stopApp.namespaceName
        if (namespaceName.isNotEmpty()) {
            doStopAppBus(
                namespaceName = namespaceName,
                deploymentName = deploymentName,
                serviceName = serviceName,
                host = stopApp.host,
                apiUrl = apiUrl,
                token = token
            )
        }
        return Result(true)
    }

    private fun doStopAppBus(
        namespaceName: String,
        deploymentName: String,
        serviceName: String,
        host: String,
        apiUrl: String,
        token: String
    ) {
        val bcsKubernetesClient = KubernetesApiUtils.getKubernetesClient(apiUrl, token)
        val deployment = bcsKubernetesClient.apps().deployments().inNamespace(namespaceName)
            .withName(deploymentName).get()
        if (deployment != null) {
            // 删除deployment
            bcsKubernetesClient.apps().deployments().inNamespace(namespaceName)
                .withName(deploymentName).delete()
            // 删除service
            bcsKubernetesClient.services().inNamespace(namespaceName).withName(serviceName).delete()
            // 更新ingress规则
            deleteIngressRule(
                bcsKubernetesClient = bcsKubernetesClient,
                namespaceName = namespaceName,
                host = host,
                deploymentName = deploymentName,
                apiUrl = apiUrl,
                token = token
            )
        }
    }

    private fun deleteIngressRule(
        bcsKubernetesClient: KubernetesClient,
        namespaceName: String,
        host: String,
        deploymentName: String,
        apiUrl: String,
        token: String
    ) {
        val ingressRedisKey = "$ingressRedisPrefixKey:$namespaceName"
        val ingressName = redisOperation.get(ingressRedisKey)
        val ingress =
            bcsKubernetesClient.extensions().ingresses().inNamespace(namespaceName).withName(ingressName).get()
        logger.info("deleteIngressRule namespaceName[$namespaceName]|rules[${ingress.spec.rules}]|" +
                "host[${MessageFormat(host).format(arrayOf(deploymentName))}]")
        ingress.spec.rules.removeIf { rule -> rule.host == MessageFormat(host).format(arrayOf(deploymentName)) }
        logger.debug("deleteIngressRule newIngress is $ingress")
        KubernetesApiUtils.createIngress(apiUrl, token, namespaceName, ingress)
    }
}
