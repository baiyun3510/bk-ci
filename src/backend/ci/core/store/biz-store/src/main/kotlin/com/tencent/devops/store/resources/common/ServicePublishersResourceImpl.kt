package com.tencent.devops.store.resources.common

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.ServicePublishersResource
import com.tencent.devops.store.pojo.common.Platforms
import com.tencent.devops.store.pojo.common.Publishers

@RestResource
class ServicePublishersResourceImpl: ServicePublishersResource {
    override fun synAddPublisherData(userId: String, publishers: List<Publishers>) {
        TODO("Not yet implemented")
    }

    override fun synDeletePublisherData(userId: String, publishers: List<Publishers>) {
        TODO("Not yet implemented")
    }

    override fun synUpdatePublisherData(userId: String, publishers: List<Publishers>) {
        TODO("Not yet implemented")
    }

    override fun synAddPlatformsData(userId: String, platforms: List<Platforms>) {
        TODO("Not yet implemented")
    }

    override fun synDeletePlatformsData(userId: String, platforms: List<Platforms>) {
        TODO("Not yet implemented")
    }

    override fun synUpdatePlatformsData(userId: String, platforms: List<Platforms>) {
        TODO("Not yet implemented")
    }
}