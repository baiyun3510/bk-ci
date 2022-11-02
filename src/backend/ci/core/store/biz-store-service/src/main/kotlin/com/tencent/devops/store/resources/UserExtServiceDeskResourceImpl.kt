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

package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceDeskResource
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.dto.SubmitDTO
import com.tencent.devops.store.pojo.vo.ExtServiceRespItem
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import com.tencent.devops.store.service.ExtServiceReleaseService
import com.tencent.devops.store.service.ExtServiceManageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceDeskResourceImpl @Autowired constructor(
    private val extServiceReleaseService: ExtServiceReleaseService,
    private val extServiceManageService: ExtServiceManageService
) : UserExtServiceDeskResource {

    override fun initExtensionService(
        userId: String,
        extensionInfo: InitExtServiceDTO
    ): Result<Boolean> {
        return extServiceReleaseService.addExtService(
            userId = userId,
            extensionInfo = extensionInfo
        )
    }

    override fun submitExtensionService(
        userId: String,
        extensionInfo: SubmitDTO
    ): Result<String> {
        return extServiceReleaseService.submitExtService(
            userId = userId,
            submitDTO = extensionInfo
        )
    }

    override fun getExtensionServiceProcessInfo(userId: String, serviceId: String): Result<StoreProcessInfo> {
        return extServiceReleaseService.getExtensionServiceProcessInfo(userId, serviceId)
    }

    override fun offlineService(
        userId: String,
        serviceCode: String,
        serviceOffline: ServiceOfflineDTO
    ): Result<Boolean> {
        return extServiceReleaseService.offlineService(
            userId = userId,
            serviceCode = serviceCode,
            serviceOfflineDTO = serviceOffline
        )
    }

    override fun listDeskExtService(
        accessToken: String,
        userId: String,
        serviceName: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<ExtServiceRespItem>> {
        return extServiceManageService.getMyService(
            userId = userId,
            serviceName = serviceName,
            page = page,
            pageSize = pageSize
        )
    }

    override fun deleteExtensionService(userId: String, serviceCode: String): Result<Boolean> {
        return extServiceManageService.deleteExtensionService(userId, serviceCode)
    }

    override fun getServiceDetails(userId: String, serviceId: String): Result<ServiceVersionVO?> {
        return extServiceManageService.getServiceById(serviceId, userId)
    }

    override fun listLanguage(): Result<List<String?>> {
        return Result(extServiceReleaseService.listLanguage())
    }

    override fun cancelRelease(userId: String, serviceId: String): Result<Boolean> {
        return extServiceReleaseService.cancelRelease(userId, serviceId)
    }

    override fun passTest(userId: String, serviceId: String): Result<Boolean> {
        return extServiceReleaseService.passTest(userId, serviceId)
    }

    override fun rebuild(userId: String, projectCode: String, serviceId: String): Result<Boolean> {
        return extServiceReleaseService.rebuild(projectCode, userId, serviceId)
    }
}
