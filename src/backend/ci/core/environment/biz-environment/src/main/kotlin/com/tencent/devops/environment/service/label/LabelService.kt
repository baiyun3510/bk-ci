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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.environment.dao.LabelDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeLabelDao
import com.tencent.devops.environment.exception.LabelException
import com.tencent.devops.environment.pojo.label.CalculateExpression
import com.tencent.devops.environment.pojo.label.LabelExpression
import com.tencent.devops.environment.pojo.label.LabelInfo
import com.tencent.devops.environment.pojo.label.Operator
import com.tencent.devops.environment.utils.LabelRedisUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
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
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            // 删除标签
            labelDao.batchDeleteLabel(
                dslContext = context,
                labelIds = listOf(labelId)
            )

            // 删除标签绑定
            nodeLabelDao.deleteLabel(context, labelId)

            // 删除标签bitmap缓存
            labelRedisUtils.deleteLabelBitMapHashKey(projectId, labelId)
        }

        return true
    }

    fun calculateNodes(userId: String, projectId: String, calculateExpression: CalculateExpression): List<Long> {
        logger.info("$userId calculateNodes projectId: $projectId " +
                        "calculateExpression: ${JsonUtil.toJson(calculateExpression)}")
        if (calculateExpression.labelExpression.isEmpty()) {
            return emptyList()
        }

        var finalRoaring64Bitmap = Roaring64Bitmap()
        for (index in calculateExpression.labelExpression.indices) {
            val expressionBitMap = getExpressionBitMap(projectId, calculateExpression.labelExpression[index])
            if (index == 0) {
                finalRoaring64Bitmap = expressionBitMap
            } else {
                finalRoaring64Bitmap.and(expressionBitMap)
            }
        }

        return finalRoaring64Bitmap.toArray().toList()
    }

    private fun getExpressionBitMap(
        projectId: String,
        labelExpression: LabelExpression
    ): Roaring64Bitmap {
        val labelIds = getLabelIds(
            projectId = projectId,
            labelKey = labelExpression.key,
            labelValues = labelExpression.value
        )

        return when(labelExpression.operator) {
            Operator.IN -> {
                getInOrExistBitMap(projectId, labelIds)
            }
            Operator.NOT_IN, Operator.DOES_NOT_EXIST -> {
                getNotInOrNotExistBitMap(projectId, labelIds)
            }
            Operator.EXIST -> {
                getInOrExistBitMap(projectId, labelIds)
            }
            else -> {
                throw LabelException("Label expression operator not exist. ${labelExpression.operator}")
            }
        }
    }

    private fun getInOrExistBitMap(
        projectId: String,
        labelIds: List<Long>
    ): Roaring64Bitmap {
        try {
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
        } catch (e: Exception) {
            logger.error("$projectId Get In or Exist bitmap get error.", e)
            throw LabelException("Get In or Exist bitmap get error.")
        }
    }

    private fun getNotInOrNotExistBitMap(
        projectId: String,
        labelIds: List<Long>
    ): Roaring64Bitmap {
        try {
            val inNodesBitMap = getInOrExistBitMap(projectId, labelIds)
            val allProjectNodesBitmap = strToBitMap(getProjectNodes(projectId))
            allProjectNodesBitmap.andNot(inNodesBitMap)
            return allProjectNodesBitmap
        } catch (e: Exception) {
            logger.error("$projectId Get NotIn or NotExist bitmap get error.", e)
            throw LabelException("Get NotIn or NotExist bitmap get error.")
        }
    }

    /**
     * 获取标签绑定节点，先读取缓存，缓存不存在则从DB取数据并刷新缓存
     */
    private fun getLabelBindingNodes(
        projectId: String,
        labelId: Long
    ): String {
        val bitMapNodes = labelRedisUtils.getLabelBindingNodes(projectId, labelId)
        if (bitMapNodes.isNullOrBlank()) {
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
        if (str.isNullOrBlank()) {
            return Roaring64Bitmap()
        }

        val longArr = str.split(",").map { it.trim().toLong() }.toLongArray()
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
