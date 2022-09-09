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

package com.tencent.devops.environment.service.label

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.LabelDao
import com.tencent.devops.environment.dao.NodeLabelDao
import com.tencent.devops.environment.exception.LabelException
import com.tencent.devops.environment.pojo.label.LabelInfo
import com.tencent.devops.environment.utils.LabelRedisUtils
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NodeLabelService @Autowired constructor(
    private val envDao: EnvDao,
    private val envNodeDao: EnvNodeDao,
    private val labelDao: LabelDao,
    private val nodeLabelDao: NodeLabelDao,
    private val labelRedisUtils: LabelRedisUtils,
    private val dslContext: DSLContext
) {
    fun getByHashId(userId: String, nodeHashId: String): List<LabelInfo> {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        return getNodeLabels(userId, listOf(nodeId))
    }

    fun getNodeLabels(userId: String, nodeIds: List<Long>): List<LabelInfo> {
        val nodeLabelRecords = nodeLabelDao.getNodeLabels(dslContext, nodeIds.toSet())
        val labelList = mutableListOf<LabelInfo>()
        nodeLabelRecords.let { result ->
            result!!.forEach {
                labelList.add(
                    LabelInfo(
                        labelId = it["LABEL_ID"] as Long,
                        labelKey = it["LABEL_KEY"] as String,
                        labelValue = it["LABEL_VALUE"] as String,
                        description = it["DESCRIPTION"] as String
                    )
                )
            }
        }

        return labelList
    }

    fun getLabelsByEnvId(userId: String, projectId: String, envHashId: String): List<LabelInfo> {
        val envNodes = envNodeDao.list(dslContext, projectId, listOf(HashUtil.decodeIdToLong(envHashId)))
        return getNodeLabels(userId, envNodes.map {
            it.nodeId
        }.toList())
    }

    fun add(userId: String, projectId: String, nodeId: Long, labelId: Long): Boolean {
        logger.info("$userId add nodeLabel nodeId: $nodeId, labelId: $labelId")

        val labelKey = labelDao.getLabelInfo(dslContext, labelId)
        if (labelKey.isNullOrBlank()) {
            throw LabelException("LabelId: $labelId labelKey is null or blank.")
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            // 绑定单独key标签
            nodeLabelDao.addNodeLabel(
                projectId = projectId,
                nodeId = nodeId,
                labelId = labelDao.getLabelId(dslContext, projectId, labelKey) ?:
                throw LabelException("Labelkey: $labelKey does not exist."),
                dslContext = context
            )

            // 先更新数据库
            nodeLabelDao.addNodeLabel(
                projectId = projectId,
                nodeId = nodeId,
                labelId = labelId,
                dslContext = context
            )

            // 删除标签节点缓存
            labelRedisUtils.deleteLabelBitMapHashKey(projectId, labelId)
        }

        return true
    }

    fun addByHashId(userId: String, projectId: String, nodeHashId: String, labelId: Long): Boolean {
        return add(userId, projectId, HashUtil.decodeIdToLong(nodeHashId), labelId)
    }

    fun delete(userId: String, projectId: String, nodeId: Long, labelId: Long): Boolean {
        logger.info("$userId delete nodeLabel nodeId: $nodeId, labelId: $labelId")
        nodeLabelDao.deleteNodeLabel(
            labelId = labelId,
            nodeId = nodeId,
            dslContext = dslContext
        )

        // 删除标签节点缓存
        labelRedisUtils.deleteLabelBitMapHashKey(projectId, labelId)

        return true
    }

    fun deleteByHashId(userId: String, projectId: String, nodeHashId: String, labelId: Long): Boolean {
        return delete(userId, projectId, HashUtil.decodeIdToLong(nodeHashId), labelId)
    }

    fun batchDeleteEnvLabel(userId: String, projectId: String, envName: String, nodeIdList: List<Long>) {
        logger.info("$userId delete nodeLabel nodeId: $nodeIdList, labelValue: $envName")

        val envLabelId = labelDao.getLabelId(
            dslContext = dslContext,
            projectId = projectId,
            labelKey = ENV_LABEL_KEY,
            labelValue = envName
        ) ?: return

        nodeLabelDao.batchDeleteNodeLabel(
            labelId = envLabelId,
            nodeIdList = nodeIdList,
            dslContext = dslContext
        )

        // 删除标签节点缓存
        labelRedisUtils.deleteLabelBitMapHashKey(projectId, envLabelId)
    }

    fun getLabelNodes(userId: String, projectId: String, labelId: Long): List<Long> {
        val labelBindingNodes = labelRedisUtils.getLabelBindingNodes(projectId, labelId)
        if (labelBindingNodes.isNullOrBlank()) {
            return emptyList()
        }

        return labelBindingNodes.split(",").map { it.trim().toLong() }.toList()
    }

    fun refreshEnvironmentLabel(
        minEnvId: Long?,
        maxEnvId: Long?,
        projectId: String?,
        environmentId: Long?
    ): Boolean {
        var envIdList = listOf<TEnvRecord>()
        // 全量刷新
        if (projectId == null && environmentId == null && minEnvId != null && maxEnvId != null) {
            envIdList = envDao.listAll(minEnvId, maxEnvId, dslContext)
        }

        // 单项目刷新
        if (projectId != null && environmentId == null) {
            envIdList = envDao.list(dslContext, projectId)
        }

        // 单环境刷新
        if (projectId != null && environmentId != null) {
            envIdList = listOf(envDao.get(dslContext, projectId, environmentId))
        }

        envIdList.stream().forEach {
            transferEnv2Label(it.projectId, it.envId, it.envName)
        }

        return true
    }

    /**
     * 新增环境变量标签
     */
    fun setEnvLabel(projectId: String, envName: String, nodeIds: List<Long>) {
        val envLabelId = labelDao.addLabel(
            projectId = projectId,
            labelInfo = LabelInfo(
                labelId = 0,
                labelKey = ENV_LABEL_KEY,
                labelValue = envName,
                description = ""
            ),
            dslContext = dslContext
        )

        nodeIds.forEach {
            add("", projectId, it, envLabelId)
        }
    }

    /**
     * 删除环境变量标签
     */
    fun removeEnvLabel(projectId: String, envName: String) {
        val envLabelId = labelDao.getLabelId(
            dslContext = dslContext,
            projectId = projectId,
            labelKey = ENV_LABEL_KEY,
            labelValue = envName
        ) ?: return

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            labelDao.batchDeleteLabel(context, listOf(envLabelId))
            nodeLabelDao.deleteLabel(context, envLabelId)
        }

        // 删除标签节点缓存
        labelRedisUtils.deleteLabelBitMapHashKey(projectId, envLabelId)
    }

    /**
     * 变更环境变量标签value
     */
    fun resetEnvLabel(projectId: String, oldEnvName: String, newEnvName: String, desc: String) {
        val envLabelId = labelDao.getLabelId(
            dslContext = dslContext,
            projectId = projectId,
            labelKey = ENV_LABEL_KEY,
            labelValue = oldEnvName
        ) ?: return

        // 更新标签值value
        labelDao.updateLabelValueWithId(envLabelId, newEnvName, desc, dslContext)

        // 删除标签节点缓存
        labelRedisUtils.deleteLabelBitMapHashKey(projectId, envLabelId)
    }

    private fun transferEnv2Label(projectId: String, envId: Long, envName: String) {
        val envNodes = envNodeDao.list(dslContext, projectId, listOf(envId))
        val envLabelId = labelDao.addLabel(
            projectId = projectId,
            labelInfo = LabelInfo(
                labelId = 0,
                labelKey = ENV_LABEL_KEY,
                labelValue = envName,
                description = ""
            ),
            dslContext = dslContext
        )

        envNodes.forEach {
            add("", projectId, it.nodeId, envLabelId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeLabelService::class.java)
        private const val ENV_LABEL_KEY = "environment"
    }
}
