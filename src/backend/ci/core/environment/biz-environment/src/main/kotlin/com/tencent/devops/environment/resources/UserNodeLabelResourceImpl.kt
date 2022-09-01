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

package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserNodeLabelResource
import com.tencent.devops.environment.pojo.label.LabelInfo
import com.tencent.devops.environment.service.label.LabelService
import com.tencent.devops.environment.service.label.NodeLabelService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserNodeLabelResourceImpl @Autowired constructor(
    private val labelService: LabelService,
    private val nodeLabelService: NodeLabelService
) : UserNodeLabelResource {
    override fun get(userId: String, projectId: String, nodeHashId: String): Result<List<LabelInfo>> {
        return Result(nodeLabelService.getByHashId(userId, nodeHashId))
    }

    override fun add(userId: String, projectId: String, nodeId: Long, labelId: Long): Result<Boolean> {
        return Result(nodeLabelService.add(userId, projectId, nodeId, labelId))
    }

    override fun batchAdd(userId: String, projectId: String, nodeId: Long, labelInfoList: List<LabelInfo>): Result<Boolean> {
        logger.info("$userId batch add nodeLabel nodeId: $nodeId, labelInfoList: $labelInfoList")

        val oldLabelInfoList = nodeLabelService.get(userId, nodeId)

        // 获取此次批量绑定新增标签列表
        val addLabelInfoList = labelInfoList.toMutableList()
        labelInfoList.forEach { labelInfo ->
            oldLabelInfoList.forEach { oldLabelInfo ->
                if (labelInfo.labelKey == oldLabelInfo.labelKey && labelInfo.labelValue == oldLabelInfo.labelValue) {
                    addLabelInfoList.remove(labelInfo)
                }
            }
        }

        // 批量新增标签并批量绑定
        val labelIdList = labelService.batchAdd(userId, projectId, addLabelInfoList)
        labelIdList.forEach {
            nodeLabelService.add(userId, projectId, nodeId, it)
        }

        // 获取解绑标签列表
        val deleteLabelInfoList = oldLabelInfoList.toMutableList()
        oldLabelInfoList.forEach { oldLabelInfo ->
            labelInfoList.forEach { labelInfo ->
                if (labelInfo.labelKey == oldLabelInfo.labelKey && labelInfo.labelValue == oldLabelInfo.labelValue) {
                    deleteLabelInfoList.remove(labelInfo)
                }
            }
        }

        // 解绑标签
        deleteLabelInfoList.forEach {
            nodeLabelService.delete(userId, projectId, nodeId, it.labelId)
        }

        return Result(true)
    }

    override fun delete(userId: String, projectId: String, nodeId: Long, labelId: Long): Result<Boolean> {
        return Result(nodeLabelService.delete(userId, projectId, nodeId, labelId))
    }

    override fun getLabelNodes(userId: String, projectId: String, labelId: Long): Result<List<Long>> {
        return Result(nodeLabelService.getLabelNodes(userId, projectId, labelId))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserNodeLabelResourceImpl::class.java)
    }
}
