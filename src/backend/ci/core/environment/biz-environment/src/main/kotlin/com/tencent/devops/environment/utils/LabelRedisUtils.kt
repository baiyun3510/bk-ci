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

import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class LabelRedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation
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

        val labelBindingNodesStr = labelBindingNodes.removePrefix(NODE_SEPARATOR).toString()

        redisOperation.hset(
            key = LABEL_BIT_MAP,
            hashKey = labelBitMapHashKey(projectId, labelId),
            values = labelBindingNodesStr
        )

        return labelBindingNodesStr
    }

    /**
     * 获取标签下的节点列表
     */
    fun getLabelBindingNodes(
        projectId: String,
        labelId: Long
    ): String? {
        return redisOperation.hget(
            key = LABEL_BIT_MAP,
            hashKey = labelBitMapHashKey(projectId, labelId)
        )
    }

    /**
     * 删除标签关联节点列表缓存
     */
    fun deleteLabelBitMapHashKey(
        projectId: String,
        labelId: Long
    ) {
        redisOperation.hdelete(
            key = LABEL_BIT_MAP,
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

        val projectNodesStr = projectNodes.removePrefix(NODE_SEPARATOR).toString()

        redisOperation.hset(
            key = NODE_BIT_MAP,
            hashKey = nodeBitMapHashKey(projectId),
            values = projectNodesStr
        )

        return projectNodesStr
    }

    /**
     * 获取单项目下关联的节点列表
     */
    fun getProjectNodes(
        projectId: String
    ): String? {
        return redisOperation.hget(
            key = NODE_BIT_MAP,
            hashKey = nodeBitMapHashKey(projectId)
        )
    }

    /**
     * 删除项目关联节点列表
     */
    fun deleteProjectNodes(projectId: String) {
        redisOperation.hdelete(
            key = NODE_BIT_MAP,
            hashKey = nodeBitMapHashKey(projectId)
        )
    }

    /**
     * 获取系统标签key列表
     */
    fun getSystemLabelKey(): List<String> {
        return redisOperation.getSetMembers(SYSTEM_LABEL_KEY)?.toList() ?: emptyList()
    }

    /**
     * 添加系统标签key
     */
    fun addSystemLabelKeys(labelKeys: List<String>) {
        labelKeys.forEach {
            redisOperation.addSetValue(SYSTEM_LABEL_KEY, it)
        }
    }

    /**
     * 删除系统标签缓存
     */
    fun deleteSystemLabelKey() {
        redisOperation.delete(SYSTEM_LABEL_KEY)
    }

    private fun labelBitMapHashKey(projectId: String, labelId: Long): String {
        return "$LABEL_BIT_MAP:$projectId:$labelId"

    }

    private fun nodeBitMapHashKey(projectId: String): String {
        return "$NODE_BIT_MAP:$projectId"
    }

    companion object {
        private const val LABEL_BIT_MAP = "environment:roaringbitmap:label"
        private const val NODE_BIT_MAP = "environment:roaringbitmap:node"
        private const val SYSTEM_LABEL_KEY = "environment:systemlabel"

        private const val NODE_SEPARATOR = ","
    }
}
