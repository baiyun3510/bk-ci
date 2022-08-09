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
package com.tencent.devops.environment.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class LabelRedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {

    /**
     * 全量刷新标签关联节点信息
     */
    fun refreshLabelBindingNodes(
        projectId: String,
        labelId: Long,
        nodeIds: List<Long>
    ): String {
        val labelBindingNodes = StringBuilder()
        nodeIds.forEach {
            labelBindingNodes.append(NODE_SEPARATOR).append(it)
        }

        labelBindingNodes.removePrefix(NODE_SEPARATOR)

        redisOperation.hset(
            key = labelBitMapKey(),
            hashKey = labelBitMapHashKey(projectId, labelId),
            values = labelBindingNodes.toString()
        )

        return labelBindingNodes.toString()
    }

    fun getLabelBindingNodes(
        projectId: String,
        labelId: Long
    ): String? {
        return redisOperation.hget(
            key = labelBitMapKey(),
            hashKey = labelBitMapHashKey(projectId, labelId)
        )
    }

    fun deleteLabelBitMapHashKey(
        projectId: String,
        labelId: Long
    ) {
        redisOperation.hdelete(
            key = labelBitMapKey(),
            hashKey = labelBitMapHashKey(projectId, labelId)
        )
    }

    /**
     * 全量刷新项目下的所有节点信息
     */
    fun refreshProjectNodes(
        projectId: String,
        nodeIds: List<Long>
    ): String {
        val projectNodes = StringBuilder()
        nodeIds.forEach {
            projectNodes.append(NODE_SEPARATOR).append(it)
        }

        projectNodes.removePrefix(NODE_SEPARATOR)

        redisOperation.hset(
            key = nodeBitMapKey(),
            hashKey = nodeBitMapHashKey(projectId),
            values = projectNodes.toString()
        )

        return projectNodes.toString()
    }

    fun getProjectNodes(
        projectId: String
    ): String? {
        return redisOperation.hget(
            key = nodeBitMapKey(),
            hashKey = nodeBitMapHashKey(projectId)
        )
    }

    fun deleteProjectNodes(projectId: String) {
        redisOperation.hdelete(
            key = nodeBitMapKey(),
            hashKey = nodeBitMapHashKey(projectId)
        )
    }

    private fun labelBitMapKey() = LABEL_BIT_MAP

    private fun labelBitMapHashKey(projectId: String, labelId: Long): String {
        return "$LABEL_BIT_MAP:$projectId:$labelId"

    }

    private fun nodeBitMapKey() = NODE_BIT_MAP

    private fun nodeBitMapHashKey(projectId: String): String {
        return "$NODE_BIT_MAP:$projectId"
    }

    companion object {
        private const val LABEL_BIT_MAP = "label:roaringbitmap"
        private const val NODE_BIT_MAP = "node:roaringbitmap"

        private const val NODE_SEPARATOR = ","
    }
}
