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
import com.tencent.devops.environment.api.UserLabelResource
import com.tencent.devops.environment.exception.LabelException
import com.tencent.devops.environment.pojo.label.LabelInfo
import com.tencent.devops.environment.pojo.label.LabelQuery
import com.tencent.devops.environment.service.label.LabelService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserLabelResourceImpl @Autowired constructor(
    private val labelService: LabelService
) : UserLabelResource {
    override fun get(userId: String, projectId: String): Result<List<LabelInfo>> {
        return Result(labelService.get(userId, projectId))
    }

    override fun add(userId: String, projectId: String, labelInfo: LabelInfo): Result<Boolean> {
        // 用户新增标签时拦截系统标签添加
        if (labelService.getSystemLabel().contains(labelInfo.labelKey)) {
            throw LabelException("用户无法添加系统标签!")
        }

        return Result(labelService.add(userId, projectId, labelInfo))
    }

    override fun delete(userId: String, projectId: String, labelId: Long): Result<Boolean> {
        return Result(labelService.delete(userId, projectId, labelId))
    }

    override fun calculateNodes(
        userId: String,
        projectId: String,
        labelQuery: LabelQuery
    ): Result<List<Long>> {
        return Result(labelService.calculateNodes(userId, projectId, labelQuery))
    }
}
