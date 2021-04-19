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

package com.tencent.devops.common.ci.yaml.v2.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.LogLevel
import com.github.fge.jsonschema.core.report.ProcessingMessage
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.service.AbstractService
import com.tencent.devops.common.ci.task.AbstractTask
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.ci.yaml.Trigger
import com.tencent.devops.common.ci.yaml.MatchRule
import com.tencent.devops.common.ci.yaml.MergeRequest
import com.tencent.devops.common.ci.yaml.v2.Container
import com.tencent.devops.common.ci.yaml.v2.Credentials
import com.tencent.devops.common.ci.yaml.v2.Job
import com.tencent.devops.common.ci.yaml.v2.MrRule
import com.tencent.devops.common.ci.yaml.v2.PreScriptBuildYaml
import com.tencent.devops.common.ci.yaml.v2.PushRule
import com.tencent.devops.common.ci.yaml.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.yaml.v2.Stage
import com.tencent.devops.common.ci.yaml.v2.TagRule
import com.tencent.devops.common.ci.yaml.v2.TriggerOn
import com.tencent.devops.common.ci.yaml.v2.YmlVersion
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.StringReader
import java.util.Random
import javax.ws.rs.core.Response

object ScriptYmlUtils {

    private val logger = LoggerFactory.getLogger(ScriptYmlUtils::class.java)

    //    private const val dockerHubUrl = "https://index.docker.io/v1/"
    private const val dockerHubUrl = ""

    private const val secretSeed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun formatYaml(yamlStr: String): String {
        // replace custom tag
        val yamlNormal = formatYamlCustom(yamlStr)
        println("=====" + yamlNormal)
        // replace anchor tag
        val yaml = Yaml()
        val obj = yaml.load(yamlNormal) as Any
        return YamlUtil.toYaml(obj)
    }

    fun parseVersion(yamlStr: String?): YmlVersion? {
        if (yamlStr == null) {
            return null
        }

        val yaml = Yaml()
        val obj = YamlUtil.toYaml(yaml.load(yamlStr) as Any)
        return YamlUtil.getObjectMapper().readValue(obj, YmlVersion::class.java)
    }

    fun parseImage(imageNameInput: String): Triple<String, String, String> {
        val imageNameStr = imageNameInput.removePrefix("http://").removePrefix("https://")
        val arry = imageNameStr.split(":")
        if (arry.size == 1) {
            val str = imageNameStr.split("/")
            return if (str.size == 1) {
                Triple(dockerHubUrl, imageNameStr, "latest")
            } else {
                Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
            }
        } else if (arry.size == 2) {
            val str = imageNameStr.split("/")
            when {
                str.size == 1 -> return Triple(dockerHubUrl, arry[0], arry[1])
                str.size >= 2 -> return if (str[0].contains(":")) {
                    Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
                } else {
                    if (str.last().contains(":")) {
                        val nameTag = str.last().split(":")
                        Triple(
                            str[0],
                            imageNameStr.substringAfter(str[0] + "/").substringBefore(":" + nameTag[1]),
                            nameTag[1]
                        )
                    } else {
                        Triple(str[0], str.last(), "latest")
                    }
                }
                else -> {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
            }
        } else if (arry.size == 3) {
            val str = imageNameStr.split("/")
            if (str.size >= 2) {
                val tail = imageNameStr.removePrefix(str[0] + "/")
                val nameAndTag = tail.split(":")
                if (nameAndTag.size != 2) {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
                return Triple(str[0], nameAndTag[0], nameAndTag[1])
            } else {
                logger.error("image name invalid: $imageNameStr")
                throw Exception("image name invalid.")
            }
        } else {
            logger.error("image name invalid: $imageNameStr")
            throw Exception("image name invalid.")
        }
    }

    private fun formatYamlCustom(yamlStr: String): String {
        val sb = StringBuilder()
        val br = BufferedReader(StringReader(yamlStr))
        var line: String? = br.readLine()
        while (line != null) {
            if (line == "on:") {
                sb.append("triggerOn:").append("\n")
            } else {
                sb.append(line).append("\n")
            }

            line = br.readLine()
        }
        return sb.toString()
    }

    private fun checkYaml(preScriptBuildYaml: PreScriptBuildYaml): List<Stage> {
        if ((preScriptBuildYaml.stages != null && preScriptBuildYaml.jobs != null) ||
            (preScriptBuildYaml.stages != null && preScriptBuildYaml.steps != null) ||
            (preScriptBuildYaml.jobs != null && preScriptBuildYaml.steps != null)) {
            logger.error("Invalid yaml: steps or jobs or stages conflict") // 不能并列存在steps和stages
            throw CustomException(Response.Status.BAD_REQUEST, "stages, jobs, steps不能并列存在，只能存在其一!")
        }

        when {
            preScriptBuildYaml.steps != null -> {
                return listOf(
                    Stage(
                        name = "stage_1",
                        id = randomString("s-"),
                        jobs = mapOf(
                            randomString("j-") to
                                    Job(
                                        name = "job1",
                                        runsOn = listOf("devcloud-linux"),
                                        container = Container("", Credentials("", "")),
                                        steps = preScriptBuildYaml.steps
                                    )
                        )
                    )
                )
            }
            preScriptBuildYaml.jobs != null -> {
                return listOf(
                    Stage(
                        name = "stage_1",
                        id = randomString("s-"),
                        jobs = preScriptBuildYaml.jobs
                    )
                )
            }
            else -> {
                preScriptBuildYaml.stages
            }
        }

/*        // 校验job类型
        stages.forEach {
            it.stage.forEach { job ->
                run {
                    val type = job.job.type
                    if (type != null && type != "" && type != VM_JOB && type != NORMAL_JOB) {
                        throw CustomException(Response.Status.BAD_REQUEST, "非法的job类型")
                    }
                }
            }
        }*/

        return emptyList()
    }

    fun normalizeGitCiYaml(preScriptBuildYaml: PreScriptBuildYaml): ScriptBuildYaml {
        val stages = checkYaml(preScriptBuildYaml)

        var thisTriggerOn = TriggerOn(
            push = PushRule(
                branches = listOf("*")
            ),
            tag = TagRule(
                tags = listOf("*")
            ),
            mr = MrRule(
                targetBranches = listOf("*")
            )
        )

        if (preScriptBuildYaml.triggerOn != null) {
            thisTriggerOn = preScriptBuildYaml.trigger ?: Trigger(
                disable = true,
                branches = MatchRule(listOf("**"), null),
                tags = null,
                paths = null
            )
            thisMr = preScriptBuildYaml.mr ?: MergeRequest(
                disable = true,
                autoCancel = true,
                branches = MatchRule(listOf("**"), null),
                paths = null
            )
        }

        return ScriptBuildYaml(
            name = preScriptBuildYaml.name,
            version = preScriptBuildYaml.version,
            triggerOn = null,
            variables = preScriptBuildYaml.variables,
            onFail = preScriptBuildYaml.onFail,
            extends = preScriptBuildYaml.extends,
            resource = preScriptBuildYaml.resource,
            notices = preScriptBuildYaml.notices,
            stages = stages
        )
    }

    fun normalizePrebuildYaml(originYaml: CIBuildYaml): CIBuildYaml {
        return CIBuildYaml(originYaml.name, null, null, originYaml.variables, null, checkYaml(originYaml), null)
    }

    fun validateYaml(yamlStr: String): Pair<Boolean, String> {
        val yamlJsonStr = try {
            convertYamlToJson(yamlStr)
        } catch (e: Throwable) {
            logger.error("", e)
            throw CustomException(Response.Status.BAD_REQUEST, "${e.cause}")
        }

        try {
            val schema = getCIBuildYamlSchema()
            return validate(schema, yamlJsonStr)
        } catch (e: Throwable) {
            logger.error("", e)
            throw CustomException(Response.Status.BAD_REQUEST, "${e.message}")
        }
    }

    fun validate(schema: String, json: String): Pair<Boolean, String> {
        val schemaNode = jsonNodeFromString(schema)
        val jsonNode = jsonNodeFromString(json)
        val report = JsonSchemaFactory.byDefault().validator.validate(schemaNode, jsonNode)
        val itr = report.iterator()
        val sb = java.lang.StringBuilder()
        while (itr.hasNext()) {
            val message = itr.next() as ProcessingMessage
            if (message.logLevel == LogLevel.ERROR || message.logLevel == LogLevel.FATAL) {
                sb.append(message).append("\r\n")
            }
        }
        return Pair(report.isSuccess, sb.toString())
    }

    fun jsonNodeFromString(json: String): JsonNode = JsonLoader.fromString(json)

    fun validateSchema(schema: String): Boolean = validateJson(schema)

    fun validateJson(json: String): Boolean {
        try {
            jsonNodeFromString(json)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    fun convertYamlToJson(yaml: String): String {
        val yamlReader = ObjectMapper(YAMLFactory())
        val obj = yamlReader.readValue(yaml, Any::class.java)

        val jsonWriter = ObjectMapper()
        return jsonWriter.writeValueAsString(obj)
    }

    fun getCIBuildYamlSchema(): String {
        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        val schema = mapper.generateJsonSchema(CIBuildYaml::class.java)
        schema.schemaNode.with("properties").with("steps").put("item", getAbstractTaskSchema())
        schema.schemaNode.with("properties").with("services").put("item", getAbstractServiceSchema())
        schema.schemaNode.with("properties")
            .with("stages")
            .with("items")
            .with("properties")
            .with("stage")
            .with("items")
            .with("properties")
            .with("job")
            .with("properties")
            .with("steps")
            .put("item", getAbstractTaskSchema())
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    }

    fun getAbstractTaskSchema(): ObjectNode {
        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        return mapper.generateJsonSchema(AbstractTask::class.java).schemaNode
    }

    fun getAbstractServiceSchema(): ObjectNode {
        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        return mapper.generateJsonSchema(AbstractService::class.java).schemaNode
    }

    fun randomString(flag: String): String {
        val random = Random()
        val buf = StringBuffer(flag)
        for (i in 0 until 7) {
            val num = random.nextInt(secretSeed.length)
            buf.append(secretSeed[num])
        }
        return buf.toString()
    }
}
