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

package com.tencent.devops.gitci.v2.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.common.ci.OBJECT_KIND_PUSH
import com.tencent.devops.common.ci.OBJECT_KIND_TAG_PUSH
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.task.DockerRunDevCloudTask
import com.tencent.devops.common.ci.task.GitCiCodeRepoInput
import com.tencent.devops.common.ci.task.GitCiCodeRepoTask
import com.tencent.devops.common.ci.task.ServiceJobDevCloudTask
import com.tencent.devops.common.ci.v2.Job
import com.tencent.devops.common.ci.v2.JobRunsOnType
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.type.gitci.GitCIDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventNotBuildDao
import com.tencent.devops.gitci.pojo.BuildConfig
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.git.GitPushEvent
import com.tencent.devops.gitci.pojo.git.GitTagPushEvent
import com.tencent.devops.gitci.service.BaseBuildService
import com.tencent.devops.gitci.utils.GitCIParameterUtils
import com.tencent.devops.gitci.utils.GitCIPipelineUtils
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.BK_CI_REF
import com.tencent.devops.scm.pojo.BK_CI_REPOSITORY
import com.tencent.devops.scm.pojo.BK_CI_REPO_OWNER
import com.tencent.devops.scm.pojo.BK_CI_RUN
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_ID_SHORT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_NAME
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_URL
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class TriggerBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val buildConfig: BuildConfig,
    private val objectMapper: ObjectMapper,
    private val gitCISettingDao: GitCISettingDao,
    private val gitCIParameterUtils: GitCIParameterUtils,
    redisOperation: RedisOperation,
    scmClient: ScmClient,
    gitPipelineResourceDao: GitPipelineResourceDao,
    gitRequestEventBuildDao: GitRequestEventBuildDao,
    gitRequestEventNotBuildDao: GitRequestEventNotBuildDao
) : BaseBuildService<ScriptBuildYaml>(client, scmClient, dslContext, redisOperation, gitPipelineResourceDao, gitRequestEventBuildDao, gitRequestEventNotBuildDao) {
    private val channelCode = ChannelCode.GIT


    companion object {
        private val logger = LoggerFactory.getLogger(TriggerBuildService::class.java)

        const val BK_REPO_GIT_WEBHOOK_MR_IID = "BK_CI_REPO_GIT_WEBHOOK_MR_IID"
        const val VARIABLE_PREFIX = "variables."
    }

    override fun gitStartBuild(
        pipeline: GitProjectPipeline,
        event: GitRequestEvent,
        yaml: ScriptBuildYaml,
        gitBuildId: Long
    ): BuildId? {
        logger.info("Git request gitBuildId:$gitBuildId, pipeline:$pipeline, event: $event, yaml: $yaml")

        // create or refresh pipeline
        val gitProjectConf = gitCISettingDao.getSetting(dslContext, event.gitProjectId) ?: throw OperationException("git ci projectCode not exist")

        val model = createPipelineModel(event, gitProjectConf, yaml)
        logger.info("Git request gitBuildId:$gitBuildId, pipeline:$pipeline, model: $model")

        return startBuild(pipeline, event, gitProjectConf, model, gitBuildId)
    }

    private fun createPipelineModel(
        event: GitRequestEvent,
        gitProjectConf: GitRepositoryConf,
        yaml: ScriptBuildYaml
    ): Model {
        // 预安装插件市场的插件
        installMarketAtom(gitProjectConf, event.userId, GitCiCodeRepoTask.atomCode)
        installMarketAtom(gitProjectConf, event.userId, DockerRunDevCloudTask.atomCode)
        installMarketAtom(gitProjectConf, event.userId, ServiceJobDevCloudTask.atomCode)

        val stageList = mutableListOf<Stage>()

        // 第一个stage，触发类
        val manualTriggerElement = ManualTriggerElement("手动触发", "T-1-1-1")
        val params = createPipelineParams(yaml, gitProjectConf, event)
        val triggerContainer =
            TriggerContainer("0", "构建触发", listOf(manualTriggerElement), null, null, null, null, params)
        val stage1 = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(stage1)

        // 其他的stage
        yaml.stages.forEachIndexed { stageIndex, stage ->
            val containerList = mutableListOf<Container>()
            stage.jobs.forEachIndexed { jobIndex, job ->
                var elementList = mutableListOf<Element>()

                if (job.runsOn[0] == JobRunsOnType.DOCKER_ON_VM.type) {
                    // 构建环境容器每个job的第一个插件都是拉代码
                    elementList.add(createGitCodeElement(event, gitProjectConf))
                    elementList = makeElementList(job, gitProjectConf, event.userId)
                    addVmBuildContainer(job, elementList, containerList, jobIndex)
                } else {
                    elementList = makeElementList(job, gitProjectConf, event.userId)
                    addNormalContainer(job, elementList, containerList, jobIndex)
                }
            }

            // 根据if设置stageController
            var stageControlOption = StageControlOption()
            if (stage.ifField != null) {
                stageControlOption = StageControlOption(
                    runCondition = StageRunCondition.CUSTOM_CONDITION_MATCH,
                    customCondition = stage.ifField.toString()
                )
            }

            stageList.add(Stage(
                id = stage.id,
                tag = listOf(stage.label),
                fastKill = stage.fastKill,
                stageControlOption = stageControlOption,
                containers = containerList
            ))
        }

        return Model(
            name = GitCIPipelineUtils.genBKPipelineName(gitProjectConf.gitProjectId),
            desc = "",
            stages = stageList,
            labels = emptyList(),
            instanceFromTemplate = false,
            pipelineCreator = event.userId
        )
    }

    private fun addVmBuildContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int
    ) {
        val osType = VMBaseOS.LINUX
        val containerPool =
            when {
                // 有container配置时优先使用
                job.container != null -> {
                    Pool(
                        container = job.container!!.image,
                        credential = Credential(
                            user = job.container!!.credentials?.username ?: "",
                            password = job.container!!.credentials?.password ?: ""
                        ),
                        macOS = null,
                        third = null
                    )
                }

                // 假设都没有配置，使用默认镜像
                else -> {
                    Pool(buildConfig.registryImage, Credential("", ""), null, null)
                }
            }

        val vmContainer = VMBuildContainer(
            id = job.id,
            name = "Job_${jobIndex + 1} ${job.name ?: ""}",
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = osType,
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = job.timeoutMinutes ?: 900,
            buildEnv = null,
            customBuildEnv = job.env,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            jobControlOption = getJobControlOption(job),
            dispatchType = if (containerPool.macOS != null) {
                MacOSDispatchType(
                    macOSEvn = containerPool.macOS!!.systemVersion!! + ":" + containerPool.macOS!!.xcodeVersion!!,
                    systemVersion = containerPool.macOS!!.systemVersion!!,
                    xcodeVersion = containerPool.macOS!!.xcodeVersion!!
                )
            } else {
                GitCIDispatchType(objectMapper.writeValueAsString(containerPool))
            }
        )
        containerList.add(vmContainer)
    }

    private fun addNormalContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int
    ) {

        containerList.add(
            NormalContainer(
                containerId = null,
                id = job.id,
                name = "Job_${jobIndex + 1} ${job.name ?: ""}",
                elements = elementList,
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                enableSkip = false,
                conditions = null,
                canRetry = false,
                jobControlOption = getJobControlOption(job),
                mutexGroup = null
            )
        )
    }

    private fun getJobControlOption(job: Job): JobControlOption {
        return if (job.ifField != null) {
            JobControlOption(
                timeout = job.timeoutMinutes,
                runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = job.ifField.toString(),
                dependOnType = DependOnType.ID,
                dependOnId = job.dependOn
            )
        } else {
            JobControlOption(
                timeout = job.timeoutMinutes,
                dependOnType = DependOnType.ID,
                dependOnId = job.dependOn
            )
        }
    }

    private fun makeElementList(
        job: Job,
        gitProjectConf: GitRepositoryConf,
        userId: String
    ): MutableList<Element> {
        val elementList = mutableListOf<Element>()
        job.steps!!.forEach { step ->
            // bash
            val element: Element = if (step.run != null) {
                LinuxScriptElement(
                    name = step.name ?: "执行Linux脚本",
                    id = step.id,
                    // todo: 如何判断类型
                    scriptType = BuildScriptType.SHELL,
                    script = step.run!!,
                    continueNoneZero = step.continueOnError
                )
            } else {
                val data = mutableMapOf<String, Any>()
                data["input"] = step.with.toString()
                MarketBuildAtomElement(
                    name = step.name ?: "插件市场第三方构建环境类插件",
                    id = step.id,
                    atomCode = step.uses!!.split('@')[0],
                    version = step.uses!!.split('@')[1],
                    data = data
                )
            }

            elementList.add(element)

            if (element is MarketBuildAtomElement) {
                logger.info("install market atom: ${element.getAtomCode()}")
                installMarketAtom(gitProjectConf, userId, element.getAtomCode())
            }
        }

        return elementList
    }

    private fun createGitCodeElement(event: GitRequestEvent, gitProjectConf: GitRepositoryConf): Element {
        val gitToken = client.getScm(ServiceGitResource::class).getToken(gitProjectConf.gitProjectId).data!!
        logger.info("get token from scm success, gitToken: $gitToken")
        val gitCiCodeRepoInput = when (event.objectKind) {
            OBJECT_KIND_PUSH -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.COMMIT_ID,
                    refName = event.commitId
                )
            }
            OBJECT_KIND_TAG_PUSH -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.TAG,
                    refName = event.branch.removePrefix("refs/tags/")
                )
            }
            OBJECT_KIND_MERGE_REQUEST -> {
                // MR时fork库的源仓库URL会不同，需要单独拿出来处理
                val gitEvent = objectMapper.readValue<GitEvent>(event.event) as GitMergeRequestEvent
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.BRANCH,
                    refName = "",
                    pipelineStartType = StartType.WEB_HOOK,
                    hookEventType = CodeEventType.MERGE_REQUEST.name,
                    hookSourceBranch = event.branch,
                    hookTargetBranch = event.targetBranch,
                    hookSourceUrl = if (event.sourceGitProjectId != null && event.sourceGitProjectId != event.gitProjectId) {
                        gitEvent.object_attributes.source.http_url
                    } else {
                        gitProjectConf.gitHttpUrl
                    },
                    hookTargetUrl = gitProjectConf.gitHttpUrl
                )
            }
            OBJECT_KIND_MANUAL -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.BRANCH,
                    refName = event.branch.removePrefix("refs/heads/")
                )
            }
            else -> {
                logger.error("event.objectKind invalid")
                null
            }
        }

        return MarketBuildAtomElement(
            name = "拉代码",
            id = null,
            status = null,
            atomCode = GitCiCodeRepoTask.atomCode,
            version = "1.*",
            data = mapOf("input" to gitCiCodeRepoInput!!)
        )
    }

    private fun createPipelineParams(
        yaml: ScriptBuildYaml,
        gitProjectConf: GitRepositoryConf,
        event: GitRequestEvent
    ): MutableList<BuildFormProperty> {
        val result = mutableListOf<BuildFormProperty>()
        gitProjectConf.env?.forEach {
            val value = gitCIParameterUtils.encrypt(it.value)
            result.add(
                BuildFormProperty(
                    id = it.name,
                    required = false,
                    type = BuildFormPropertyType.PASSWORD,
                    defaultValue = value,
                    options = null,
                    desc = null,
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            )
        }

        val startParams = mutableMapOf<String, String>()

        // 通用参数
        startParams[BK_CI_RUN] = "true"
        startParams[BK_CI_REPO_OWNER] = GitCommonUtils.getRepoOwner(gitProjectConf.gitHttpUrl)
        startParams[BK_CI_REPOSITORY] =
            GitCommonUtils.getRepoOwner(gitProjectConf.gitHttpUrl) + "/" + gitProjectConf.name
        startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = event.objectKind
        startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH] = event.branch
        startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = event.commitId
        startParams[BK_REPO_WEBHOOK_REPO_NAME] = gitProjectConf.name
        startParams[BK_REPO_WEBHOOK_REPO_URL] = gitProjectConf.url
        startParams[BK_REPO_GIT_WEBHOOK_COMMIT_MESSAGE] = event.commitMsg.toString()
        if (!event.commitId.isBlank() && event.commitId.length >= 8)
            startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID_SHORT] = event.commitId.substring(0, 8)

        // 写入WEBHOOK触发环境变量
        val originEvent = try {
            startParams["BK_CI_EVENT_CONTENT"] = event.event
            objectMapper.readValue<GitEvent>(event.event)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${e.message}")
        }

        when (originEvent) {
            is GitPushEvent -> {
                startParams[BK_CI_REF] = originEvent.ref
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT] = originEvent.before
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT] = originEvent.after
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = originEvent.total_commits_count.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND] = originEvent.operation_kind
            }
            is GitTagPushEvent -> {
                startParams[BK_CI_REF] = originEvent.ref
//                startParams[BK_REPO_GIT_WEBHOOK_TAG_NAME] = event.branch
//                startParams[BK_REPO_GIT_WEBHOOK_TAG_OPERATION] = originEvent.operation_kind ?: ""
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = originEvent.total_commits_count.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_TAG_USERNAME] = event.userId
//                startParams[BK_REPO_GIT_WEBHOOK_TAG_CREATE_FROM] = originEvent.create_from.toString()
            }
            is GitMergeRequestEvent -> {
//                startParams[BK_REPO_GIT_WEBHOOK_MR_ACTION] = originEvent.object_attributes.action
//                startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = originEvent.user.username
                startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = originEvent.object_attributes.target_branch
                startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = originEvent.object_attributes.source_branch
                startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_URL] = originEvent.object_attributes.target.http_url
                startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL] = originEvent.object_attributes.source.http_url
//                startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = originEvent.object_attributes.created_at
//                startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP] =
//                    DateTimeUtil.zoneDateToTimestamp(originEvent.object_attributes.created_at).toString()
//                startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = originEvent.object_attributes.updated_at
//                startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP] =
//                    DateTimeUtil.zoneDateToTimestamp(originEvent.object_attributes.updated_at).toString()
                startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = originEvent.object_attributes.id.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = originEvent.object_attributes.title
                startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = originEvent.object_attributes.url
//                startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = originEvent.object_attributes.id.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = originEvent.object_attributes.description
//                startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = originEvent.object_attributes.assignee_id.toString()
                startParams[BK_REPO_GIT_WEBHOOK_MR_IID] = originEvent.object_attributes.iid.toString()
            }
        }

        // 用户自定义变量
        // startParams.putAll(yaml.variables ?: mapOf())
        putVariables2StartParams(yaml, gitProjectConf, startParams)

        startParams.forEach {
            result.add(
                BuildFormProperty(
                    id = it.key,
                    required = false,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = it.value,
                    options = null,
                    desc = null,
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            )
        }

        return result
    }

    private fun putVariables2StartParams(
        yaml: ScriptBuildYaml,
        gitProjectConf: GitRepositoryConf,
        startParams: MutableMap<String, String>
    ) {
        if (yaml.variables == null) {
            return
        }

        yaml.variables!!.forEach { (key, variable) ->
            startParams[VARIABLE_PREFIX + key] =
                variable.copy(value = formatVariablesValue(variable.value, gitProjectConf)).toString()
        }
    }

    private fun formatVariablesValue(value: String?, gitProjectConf: GitRepositoryConf): String? {
        if (value == null || value.isEmpty()) {
            return ""
        }

        val settingMap = mutableMapOf<String, String>()
        gitProjectConf.env?.forEach {
            settingMap[it.name] = it.value
        }

        var newValue = value
        val pattern = Pattern.compile("\\$\\{\\{([^{}]+?)}}")
        val matcher = pattern.matcher(value)
        while (matcher.find()) {
            val realValue = settingMap[matcher.group(1).trim()]
            newValue = newValue!!.replace(matcher.group(), realValue ?: "")
        }
        return newValue
    }

    private fun getCiBuildConf(buildConf: BuildConfig): CiBuildConfig {
        return CiBuildConfig(
            buildConf.codeCCSofwareClientImage,
            buildConf.codeCCSofwarePath,
            buildConf.registryHost,
            buildConf.registryUserName,
            buildConf.registryPassword,
            buildConf.registryImage,
            buildConf.cpu,
            buildConf.memory,
            buildConf.disk,
            buildConf.volume,
            buildConf.activeDeadlineSeconds,
            buildConf.devCloudAppId,
            buildConf.devCloudToken,
            buildConf.devCloudUrl
        )
    }
}
