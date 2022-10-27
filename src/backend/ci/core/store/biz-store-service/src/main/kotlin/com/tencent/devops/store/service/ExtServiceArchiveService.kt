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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtServiceArchiveService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val extServiceDao: ExtServiceDao,
    private val extServiceCommonService: ExtServiceCommonService
) {

    fun verifyExtServicePackageByUserId(
        userId: String,
        serviceCode: String,
        version: String,
        releaseType: ReleaseTypeEnum? = null
    ): Boolean {
        // 校验用户是否是该微扩展的开发成员
        val flag = storeMemberDao.isStoreMember(
            dslContext = dslContext,
            userId = userId,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE.type.toByte()
        )
        if (!flag) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PERMISSION_DENIED,
                params = arrayOf(serviceCode)
            )
        }
        val serviceCount = extServiceDao.countByCode(dslContext, serviceCode)
        if (serviceCount < 0) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(serviceCode)
            )
        }
        val serviceRecord = extServiceDao.getNewestServiceByCode(dslContext, serviceCode)!!
        // 不是重新上传的包才需要校验版本号
        if (null != releaseType) {
            extServiceCommonService.validateServiceVersion(
                releaseType = releaseType,
                serviceCode = serviceCode,
                serviceName = serviceRecord.serviceName,
                serviceStatus = serviceRecord.serviceStatus,
                dbVersion = serviceRecord.version,
                requestVersion = version
            )
        }
        return true
    }
}
