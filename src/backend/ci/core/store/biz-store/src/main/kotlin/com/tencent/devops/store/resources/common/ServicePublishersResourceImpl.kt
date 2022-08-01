package com.tencent.devops.store.resources.common

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.ServicePublishersResource
import com.tencent.devops.store.pojo.common.Publishers
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest
import com.tencent.devops.store.service.common.PublishersService
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePublishersResourceImpl @Autowired constructor(
    private val publishersService: PublishersService
): ServicePublishersResource {
    override fun synAddPublisherData(userId: String, publishers: List<Publishers>): Result<Int> {
        return Result(publishersService.createPublisherData(userId, publishers))
    }

    override fun synDeletePublisherData(userId: String, publishers: List<Publishers>): Result<Int> {
        return Result(publishersService.deletePublisherData(userId, publishers))
    }

    override fun synUpdatePublisherData(userId: String, publishers: List<Publishers>): Result<Int> {
        return Result(publishersService.updatePublisherData(userId, publishers))
    }

    override fun synAddPlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(publishersService.createPlatformsData(userId, storeDockingPlatformRequests))
    }

    override fun synDeletePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(publishersService.deletePlatformsData(userId, storeDockingPlatformRequests))
    }

    override fun synUpdatePlatformsData(
        userId: String,
        storeDockingPlatformRequests: List<StoreDockingPlatformRequest>
    ): Result<Int> {
        return Result(publishersService.updatePlatformsData(userId, storeDockingPlatformRequests))
    }

}