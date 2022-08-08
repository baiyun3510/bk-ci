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

import com.tencent.devops.environment.dao.LabelDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeLabelDao
import com.tencent.devops.environment.exception.LabelException
import com.tencent.devops.environment.pojo.label.CalculateExpression
import com.tencent.devops.environment.pojo.label.LabelInfo
import com.tencent.devops.environment.pojo.label.Operator
import com.tencent.devops.environment.utils.LabelRedisUtils
import org.jooq.DSLContext
import org.roaringbitmap.longlong.Roaring64Bitmap
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.streams.toList

@Service
class LabelService @Autowired constructor(
    private val nodeDao: NodeDao,
    private val labelDao: LabelDao,
    private val nodeLabelDao: NodeLabelDao,
    private val labelRedisUtils: LabelRedisUtils,
    private val dslContext: DSLContext
) {
    fun get(userId: String, projectId: String): List<LabelInfo> {
        val labelList = mutableListOf<LabelInfo>()
        val labelRecords = labelDao.listLabels(dslContext, projectId)
        labelRecords.let {
            labelRecords!!.forEach {
                labelList.add(
                    LabelInfo(
                        labelId = it.id,
                        labelKey = it.labelKey,
                        labelValue = it.labelValue,
                        description = it.description
                    )
                )
            }
        }

        return labelList
    }

    fun add(userId: String, projectId: String, labelInfo: LabelInfo): Boolean {
        labelDao.addLabel(
            projectId = projectId,
            labelInfo = labelInfo,
            dslContext = dslContext
        )

        return true
    }

    fun delete(userId: String, projectId: String, labelId: Long): Boolean {
        labelDao.batchDeleteLabel(
            dslContext = dslContext,
            labelIds = listOf(labelId)
        )

        return true
    }

    fun calculateNodes(userId: String, projectId: String, calculateExpression: CalculateExpression): List<Long> {
        if (calculateExpression.labelExpression.isEmpty()) {
            return emptyList()
        }

        calculateExpression.labelExpression.forEach { labelExpression ->
            val labelIds = getLabelIds(
                projectId = projectId,
                labelKey = labelExpression.key,
                labelValues = labelExpression.value
            )

            return when(labelExpression.operator) {
                Operator.IN -> {
                    getInOrExistBitMap(projectId, labelIds).toArray().toList()
                }
                Operator.NOT_IN, Operator.DOES_NOT_EXIST -> {
                    val inNodesBitMap = getInOrExistBitMap(projectId, labelIds)
                    val allProjectNodesBitmap = strToBitMap(getProjectNodes(projectId))
                    allProjectNodesBitmap.andNot(inNodesBitMap)
                    allProjectNodesBitmap.toArray().toList()
                }
                Operator.EXIST -> {
                    getInOrExistBitMap(projectId, labelIds).toArray().toList()
                }
                else -> {
                    throw LabelException("Label expression operator not exist. ${labelExpression.operator}")
                }
            }
        }

        return emptyList()
    }

    private fun getInOrExistBitMap(
        projectId: String,
        labelIds: List<Long>
    ): Roaring64Bitmap {
        var inRoaringBitmap = Roaring64Bitmap()
        for (index in labelIds.indices) {
            val bitMapNodes = getLabelBindingNodes(projectId, labelIds[index])
            if (index == 0) {
                inRoaringBitmap = strToBitMap(bitMapNodes)
            } else {
                inRoaringBitmap.and(strToBitMap(bitMapNodes))
            }
        }

        return inRoaringBitmap
    }

    /**
     * 获取标签绑定节点，先读取缓存，缓存不存在则从DB取数据并刷新缓存
     */
    private fun getLabelBindingNodes(
        projectId: String,
        labelId: Long
    ): String {
        val bitMapNodes = labelRedisUtils.getLabelBindingNodes(projectId, labelId)
        if (bitMapNodes == null) {
            val labelBindingNodes = nodeLabelDao.getLabelNodes(dslContext, labelId)
            return if (labelBindingNodes == null) {
                ""
            } else {
                // 刷新缓存并返回
                labelRedisUtils.refreshLabelBindingNodes(
                    projectId = projectId,
                    labelId = labelId,
                    nodeIds = labelBindingNodes.stream().map { it.nodeId }.toList()
                )
            }
        }

        return bitMapNodes
    }

    /**
     * 获取项目下的所有节点，先读取缓存，缓存不存在则从DB取数据并刷新缓存
     */
    private fun getProjectNodes(projectId: String): String {
        val bitMapProjectNodes = labelRedisUtils.getProjectNodes(projectId)
        if (bitMapProjectNodes == null) {
            val projectNodes = nodeDao.listNodes(dslContext, projectId)
            return labelRedisUtils.refreshProjectNodes(
                projectId = projectId,
                nodeIds = projectNodes.stream().map { it.nodeId }.toList()
            )
        }

        return bitMapProjectNodes
    }

    private fun strToBitMap(str: String?): Roaring64Bitmap {
        val longArr = str?.split(",")?.map { it.trim().toLong() }?.toLongArray() ?: LongArray(0)
        return Roaring64Bitmap.bitmapOf(*longArr)
    }

    private fun getLabelIds(projectId: String, labelKey: String, labelValues: List<String>?): List<Long> {
        val labelIds = mutableListOf<Long>()
        if (labelValues == null) {
            val labelId = labelDao.getLabelId(dslContext, projectId, labelKey)
            if (labelId == null) {
                logger.error("No label exist, $labelKey")
                throw LabelException("No label exist, $labelKey")
            }  else {
                labelIds.add(labelId)
            }
        } else {
            labelValues.forEach {
                val labelId = labelDao.getLabelId(dslContext, projectId, labelKey, it)
                if (labelId == null) {
                    logger.error("No label exist, $labelKey:$it")
                    throw LabelException("No label exist, $labelKey:$it")
                }  else {
                    labelIds.add(labelId)
                }
            }
        }

        return labelIds
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LabelService::class.java)
    }
}
