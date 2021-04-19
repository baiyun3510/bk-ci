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

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.yaml.v2.PreScriptBuildYaml
import com.tencent.devops.common.ci.yaml.v2.PushRule
import com.tencent.devops.common.ci.yaml.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.yaml.v2.YmlVersion
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ScriptYmlUtilsTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun formatYaml() {
        val classPathResource = ClassPathResource("Sample1.yml")
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }

        // println(sb.toString())

        val obj = YamlUtil.getObjectMapper().readValue(
            ScriptYmlUtils.formatYaml(sb.toString()),
            PreScriptBuildYaml::class.java
        )

        if (obj.triggerOn != null && obj.triggerOn!!.push != null) {
            val push = obj.triggerOn!!.push

            var pushRule: PushRule?
            try {
                pushRule = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(push!!),
                    PushRule::class.java
                )

                println(JsonUtil.toJson(pushRule))
            } catch (e: MismatchedInputException) {
                try {
                    println("push: " + JsonUtil.toJson(push!!))
                    val pushObj = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(push!!),
                        List::class.java
                    ) as ArrayList<String>

                    pushRule = PushRule(
                        branches = pushObj,
                        branchesIgnore = null,
                        paths = null,
                        pathsIgnore = null,
                        users = null,
                        usersIgnore = null
                    )
                    println("array: " + JsonUtil.toJson(pushObj))
                } catch (e: Exception) {
                    println(e)
                    pushRule = null
                }

            }

            println(JsonUtil.toJson(pushRule!!))
        }


    }
}
