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

package com.tencent.devops.auth.service.action.impl

import com.tencent.bk.sdk.iam.service.IamResourceService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.ResourceDao
import com.tencent.devops.auth.pojo.resource.CreateResourceDTO
import com.tencent.devops.auth.pojo.resource.UpdateResourceDTO
import com.tencent.devops.auth.service.iam.BkResourceService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class BkResourceServiceImpl @Autowired constructor(
    open val dslContext: DSLContext,
    open val resourceDao: ResourceDao,
    open val iamResourceService: IamResourceService,
) : BkResourceService {
    override fun createResource(userId: String, resource: CreateResourceDTO): Boolean {
        // 判断此资源是否存在, 存在直接报错
        val isExistResource = iamResourceService.resourceCheck(resource.resourceId)
        if (isExistResource) {
            logger.warn("createResource $resource exist")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.RESOURCE_EXSIT,
                defaultMessage = MessageCodeUtil.getCodeMessage(
                    messageCode = AuthMessageCode.RESOURCE_EXSIT,
                    params = arrayOf(resource.resourceId)
                )
            )
        }
        // 校验父类资源是否合法
        if (resource.parent != null && resource.parent != AuthResourceType.PROJECT.value) {
            logger.warn("createResource: parents illegal")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.PERMISSION_MODEL_CHECK_FAIL,
                defaultMessage = MessageCodeUtil.getCodeLanMessage(
                    messageCode = AuthMessageCode.PERMISSION_MODEL_CHECK_FAIL
                )
            )
        }
        createExtSystem(resource)
        return true
    }

    override fun updateResource(
        userId: String,
        resourceId: String,
        resource: UpdateResourceDTO
    ): Boolean {
        // 判断此资源是否存在, 不存在直接报错
        val isExistResource = iamResourceService.resourceCheck(resourceId)
        if (!isExistResource) {
            logger.warn("createResource $resource not exist")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.RESOURCE_NOT_EXSIT
            )
        }
        // 修改资源类数据
        updateExtSystem(resource, resourceId)
        return true
    }

    abstract fun createExtSystem(resource: CreateResourceDTO)

    abstract fun updateExtSystem(resource: UpdateResourceDTO, resourceType: String)

    companion object {
        private val logger = LoggerFactory.getLogger(BkResourceServiceImpl::class.java)
    }
}
