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

package com.tencent.devops.store.resources.common

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.ServicePublishersResource
import com.tencent.devops.store.pojo.common.PublishersRequest
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest
import com.tencent.devops.store.service.common.PublishersDataService
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePublishersResourceImpl @Autowired constructor(
    private val publishersDataService: PublishersDataService
) : ServicePublishersResource {
    override fun synAddPublisherData(userId: String, publishers: List<PublishersRequest>): Result<Int> {
        return Result(publishersDataService.createPublisherData(userId, publishers))
    }

    override fun synDeletePublisherData(userId: String, publishers: List<PublishersRequest>): Result<Int> {
        return Result(publishersDataService.deletePublisherData(userId, publishers))
    }

    override fun synUpdatePublisherData(userId: String, publishers: List<PublishersRequest>): Result<Int> {
        return Result(publishersDataService.updatePublisherData(userId, publishers))
    }

    override fun synAddPlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(publishersDataService.createPlatformsData(userId, storeDockingPlatformRequests))
    }

    override fun synDeletePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(publishersDataService.deletePlatformsData(userId, storeDockingPlatformRequests))
    }

    override fun synUpdatePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(publishersDataService.updatePlatformsData(userId, storeDockingPlatformRequests))
    }

    override fun synUpdatePlatformsLogoInfo(userId: String, platformCode: String, logoUrl: String): Result<Boolean> {
        return Result(publishersDataService.updatePlatformsLogoInfo(userId, platformCode, logoUrl))
    }
}
