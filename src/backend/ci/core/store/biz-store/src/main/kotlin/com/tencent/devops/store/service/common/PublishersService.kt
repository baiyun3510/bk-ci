package com.tencent.devops.store.service.common

import com.tencent.devops.store.pojo.common.Platforms
import com.tencent.devops.store.pojo.common.Publishers

interface PublishersService {

    fun createPublisherData(userId: String, publishers: List<Publishers>)

    fun deletePublisherData(userId: String, publishers: List<Publishers>)

    fun updatePublisherData(puserId: String, ublishers: List<Publishers>)

    fun createPlatformsData(userId: String, platforms: List<Platforms>)

    fun deletePlatformsData(userId: String, platforms: List<Platforms>)

    fun updatePlatformsData(userId: String, platforms: List<Platforms>)
}