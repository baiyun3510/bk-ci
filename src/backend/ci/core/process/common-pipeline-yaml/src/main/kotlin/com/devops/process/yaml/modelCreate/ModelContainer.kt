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

package com.devops.process.yaml.modelCreate

import com.devops.process.yaml.modelCreate.inner.InnerModelCreator
import com.devops.process.yaml.pojo.StreamDispatchInfo
import com.devops.process.yaml.utils.StreamDispatchUtils
import com.devops.process.yaml.v2.models.IfType
import com.devops.process.yaml.v2.models.Resources
import com.devops.process.yaml.v2.models.job.Job
import com.devops.process.yaml.v2.models.job.ResourceExclusiveDeclaration
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.matrix.MatrixConfig.Companion.MATRIX_CONTEXT_KEY_PREFIX
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import org.springframework.stereotype.Component

@Component
class ModelContainer constructor(
    val client: Client,
    val objectMapper: ObjectMapper,
    val inner: InnerModelCreator
) {

    fun addVmBuildContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        projectCode: String,
        finalStage: Boolean = false,
        jobEnable: Boolean = true,
        resources: Resources? = null,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ) {
        val defaultImage = inner.defaultImage
        val dispatchInfo = if (JsonUtil.toJson(job.runsOn).contains("\${{ $MATRIX_CONTEXT_KEY_PREFIX")) {
            StreamDispatchInfo(
                name = "dispatchInfo_${job.id}",
                job = job,
                projectCode = projectCode,
                defaultImage = defaultImage,
                resources = resources
            )
        } else null
        val vmContainer = VMBuildContainer(
            jobId = job.id,
            name = job.name ?: "Job-${jobIndex + 1}",
            elements = elementList,
            mutexGroup = getMutexGroup(job.resourceExclusiveDeclaration),
            baseOS = StreamDispatchUtils.getBaseOs(job),
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = job.timeoutMinutes ?: 900,
            buildEnv = StreamDispatchUtils.getBuildEnv(job),
            customBuildEnv = job.env,
            jobControlOption = getJobControlOption(
                job = job, jobEnable = jobEnable, finalStage = finalStage
            ),
            dispatchType = StreamDispatchUtils.getDispatchType(
                client = client,
                objectMapper = objectMapper,
                job = job,
                projectCode = projectCode,
                defaultImage = defaultImage,
                resources = resources,
                containsMatrix = dispatchInfo != null,
                buildTemplateAcrossInfo = buildTemplateAcrossInfo
            ),
            matrixGroupFlag = job.strategy != null,
            matrixControlOption = getMatrixControlOption(job, dispatchInfo)
        )
        containerList.add(vmContainer)
    }

    protected fun getMatrixControlOption(
        job: Job,
        dispatchInfo: StreamDispatchInfo?
    ): MatrixControlOption? {

        val strategy = job.strategy ?: return null

        with(strategy) {
            if (matrix is Map<*, *>) {
                val yaml = matrix as MutableMap<String, Any>
                val include = if ("include" in yaml.keys && yaml["include"] != null) {
                    YamlUtil.toYaml(yaml["include"]!!)
                } else {
                    null
                }
                val exclude = if ("exclude" in yaml.keys && yaml["exclude"] != null) {
                    YamlUtil.toYaml(yaml["exclude"]!!)
                } else {
                    null
                }
                val json = matrix as MutableMap<String, Any>
                json.remove("include")
                json.remove("exclude")

                return MatrixControlOption(
                    strategyStr = YamlUtil.toYaml(json),
                    includeCaseStr = include,
                    excludeCaseStr = exclude,
                    fastKill = fastKill,
                    maxConcurrency = maxParallel,
                    customDispatchInfo = dispatchInfo
                )
            } else {
                return MatrixControlOption(
                    strategyStr = matrix.toString(),
                    fastKill = fastKill,
                    maxConcurrency = maxParallel,
                    customDispatchInfo = dispatchInfo
                )
            }
        }
    }

    fun addNormalContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        jobEnable: Boolean = true,
        finalStage: Boolean = false
    ) {

        containerList.add(
            NormalContainer(
                jobId = job.id,
                containerId = null,
                id = job.id,
                name = job.name ?: "Job-${jobIndex + 1}",
                elements = elementList,
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                enableSkip = false,
                conditions = null,
                canRetry = false,
                jobControlOption = getJobControlOption(
                    job = job, jobEnable = jobEnable, finalStage = finalStage
                ),
                mutexGroup = getMutexGroup(job.resourceExclusiveDeclaration)
            )
        )
    }

    protected fun getJobControlOption(
        job: Job,
        jobEnable: Boolean = true,
        finalStage: Boolean = false
    ): JobControlOption {
        return if (!job.ifField.isNullOrBlank()) {
            if (finalStage) {
                JobControlOption(
                    timeout = job.timeoutMinutes,
                    runCondition = when (job.ifField) {
                        IfType.SUCCESS.name -> JobRunCondition.PREVIOUS_STAGE_SUCCESS
                        IfType.FAILURE.name -> JobRunCondition.PREVIOUS_STAGE_FAILED
                        IfType.CANCELLED.name, IfType.CANCELED.name -> JobRunCondition.PREVIOUS_STAGE_CANCEL
                        else -> JobRunCondition.STAGE_RUNNING
                    },
                    dependOnType = DependOnType.ID,
                    dependOnId = job.dependOn,
                    prepareTimeout = job.runsOn.queueTimeoutMinutes,
                    continueWhenFailed = job.continueOnError
                )
            } else {
                JobControlOption(
                    enable = jobEnable,
                    timeout = job.timeoutMinutes,
                    runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                    customCondition = job.ifField.toString(),
                    dependOnType = DependOnType.ID,
                    dependOnId = job.dependOn,
                    prepareTimeout = job.runsOn.queueTimeoutMinutes,
                    continueWhenFailed = job.continueOnError
                )
            }
        } else {
            JobControlOption(
                enable = jobEnable,
                timeout = job.timeoutMinutes,
                dependOnType = DependOnType.ID,
                dependOnId = job.dependOn,
                prepareTimeout = job.runsOn.queueTimeoutMinutes,
                continueWhenFailed = job.continueOnError
            )
        }
    }

    protected fun getMutexGroup(resource: ResourceExclusiveDeclaration?): MutexGroup? {
        if (resource == null) {
            return null
        }
        return MutexGroup(
            enable = true,
            mutexGroupName = resource.label,
            queueEnable = true,
            queue = resource.queueLength ?: 0,
            timeout = resource.timeoutMinutes ?: 10
        )
    }
}
