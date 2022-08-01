package com.tencent.devops.store.service.common

import com.tencent.devops.store.pojo.common.Publishers
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest

interface PublishersService {

    fun createPublisherData(userId: String, publishers: List<Publishers>): Int

    fun deletePublisherData(userId: String, publishers: List<Publishers>): Int

    fun updatePublisherData(userId: String, publishers: List<Publishers>): Int

    fun createPlatformsData(userId: String, storeDockingPlatformRequests: List<StoreDockingPlatformRequest>): Int

    fun deletePlatformsData(userId: String, storeDockingPlatformRequests: List<StoreDockingPlatformRequest>): Int

    fun updatePlatformsData(userId: String, storeDockingPlatformRequests: List<StoreDockingPlatformRequest>): Int
}