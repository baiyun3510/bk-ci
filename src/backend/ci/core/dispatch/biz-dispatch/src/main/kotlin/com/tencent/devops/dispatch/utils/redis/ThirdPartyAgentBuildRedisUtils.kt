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
package com.tencent.devops.dispatch.utils.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ThirdPartyAgentBuildRedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {

    fun setThirdPartyBuild(secretKey: String, redisBuild: ThirdPartyRedisBuild) {
        redisOperation.set(
            thirdPartyBuildKey(
                secretKey = secretKey,
                agentId = redisBuild.agentId,
                buildId = redisBuild.buildId,
                vmSeqId = redisBuild.vmSeqId
            ),
            objectMapper.writeValueAsString(redisBuild)
        )
    }

    fun deleteThirdPartyBuild(secretKey: String, agentId: String, buildId: String, vmSeqId: String) =
        redisOperation.delete(
            key = thirdPartyBuildKey(
                secretKey = secretKey,
                agentId = agentId,
                buildId = buildId,
                vmSeqId = vmSeqId
            )
        )

    fun getRunningJobWithAllAgent(containerHashId: String): Int {
        val runningCount = redisOperation.hget(jobRunningHashKey(), allAgentHashField(containerHashId))
        return runningCount?.toInt() ?: 0
    }

    fun getRunningJobWithSingleAgent(containerHashId: String, agentId: String): Int {
        val runningCount = redisOperation.hget(jobRunningHashKey(), singleAgentHashField(containerHashId, agentId))
        return runningCount?.toInt() ?: 0
    }

    /**
     * 原子加/减当前job在同一agent下的运行数量以及在所有agent下的运行数量
     */
    fun increRunningJobWithAgentId(containerHashId: String, agentId: String, delta: Long) {
        redisOperation.hIncrBy(jobRunningHashKey(), singleAgentHashField(containerHashId, agentId), delta)
        redisOperation.hIncrBy(jobRunningHashKey(), allAgentHashField(containerHashId), delta)
    }

    fun isThirdPartyAgentUpgrading(projectId: String, agentId: String): Boolean {
        return try {
            redisOperation.get(thirdPartyUpgradeKey(projectId, agentId)) == "true"
        } catch (ignored: Exception) {
            false
        }
    }

    fun setThirdPartyAgentUpgrading(projectId: String, agentId: String) {
        redisOperation.set(
            key = thirdPartyUpgradeKey(projectId = projectId, agentId = agentId),
            value = "true",
            expiredInSecond = 60L
        )
    }

    fun thirdPartyAgentUpgradingDone(projectId: String, agentId: String) {
        redisOperation.delete(key = thirdPartyUpgradeKey(projectId = projectId, agentId = agentId))
    }

    private fun thirdPartyBuildKey(secretKey: String, agentId: String, buildId: String, vmSeqId: String) =
        "third_party_agent_${secretKey}_${agentId}_${buildId}_$vmSeqId"

    private fun thirdPartyUpgradeKey(projectId: String, agentId: String) =
        "third_party_agent_upgrade_${projectId}_$agentId"

    /**
     * 统计job正在运行中数据
     */
    private fun jobRunningHashKey() =
        "dispatch:job_running"

    /**
     * 单 job 单 agent hash field
     */
    private fun singleAgentHashField(containerHashId: String, agentId: String) =
        "${containerHashId}_$agentId"

    /**
     * 单 job 全量 agent hash field
     */
    private fun allAgentHashField(containerHashId: String) =
        containerHashId
}
