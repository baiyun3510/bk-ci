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

import com.tencent.devops.environment.dao.NodeLabelDao
import com.tencent.devops.environment.pojo.label.LabelInfo
import com.tencent.devops.environment.utils.LabelRedisUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NodeLabelService @Autowired constructor(
    private val nodeLabelDao: NodeLabelDao,
    private val labelRedisUtils: LabelRedisUtils,
    private val dslContext: DSLContext
) {
    fun get(userId: String, nodeId: Long): List<LabelInfo> {
        val nodeLabelRecords = nodeLabelDao.getNodeLabels(dslContext, nodeId)
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

    fun add(userId: String, projectId: String, nodeId: Long, labelId: Long): Boolean {
        logger.info("$userId add nodeLabel nodeId: $nodeId, labelId: $labelId")
        // 先更新数据库
        nodeLabelDao.addNodeLabel(
            projectId = projectId,
            nodeId = nodeId,
            labelId = labelId,
            dslContext = dslContext
        )

        // 删除标签节点缓存
        labelRedisUtils.deleteLabelBitMapHashKey(projectId, labelId)

        return true
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

    companion object {
        private val logger = LoggerFactory.getLogger(NodeLabelService::class.java)
    }
}
