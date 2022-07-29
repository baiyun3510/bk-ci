package com.tencent.devops.store.service.common.impl

import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.auth.pojo.DeptInfo
import com.tencent.devops.store.pojo.common.Platforms
import com.tencent.devops.store.pojo.common.Publishers
import com.tencent.devops.store.service.common.PublishersService
import com.tencent.devops.common.client.Client
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublishersServiceImpl @Autowired constructor(
    private val client: Client
) : PublishersService {
    override fun createPublisherData(userId: String, publishers: List<Publishers>) {
        TODO("Not yet implemented")
    }

    override fun deletePublisherData(userId: String, publishers: List<Publishers>) {
        TODO("Not yet implemented")
    }

    override fun updatePublisherData(puserId: String, ublishers: List<Publishers>) {
        TODO("Not yet implemented")
    }

    override fun createPlatformsData(userId: String, platforms: List<Platforms>) {
        TODO("Not yet implemented")
    }

    override fun deletePlatformsData(userId: String, platforms: List<Platforms>) {
        TODO("Not yet implemented")
    }

    override fun updatePlatformsData(userId: String, platforms: List<Platforms>) {
        TODO("Not yet implemented")
    }

    fun analysisDept(userId: String, organization: String) {
        val deptNames = organization.split("/")
        val deptInfos = mutableListOf<DeptInfo>()
        deptNames.forEachIndexed(){ index, deptName ->
            val result = client.get(ServiceDeptResource::class).getDeptByName(userId, deptName).data
            result?.let {
                if (result.count > 1) {
                    result.results.forEach {
                        if (it.id == deptInfos[index - 1].id)
                    }

                }
            }
        }

    }
}