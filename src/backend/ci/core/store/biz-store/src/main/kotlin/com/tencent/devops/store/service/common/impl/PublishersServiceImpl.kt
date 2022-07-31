package com.tencent.devops.store.service.common.impl

import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.auth.pojo.DeptInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.store.tables.records.TStorePublisherInfoRecord
import com.tencent.devops.store.dao.common.PublishersDao
import com.tencent.devops.store.pojo.common.Platforms
import com.tencent.devops.store.pojo.common.Publishers
import com.tencent.devops.store.service.common.PublishersService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PublishersServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val publishersDao: PublishersDao,
    private val client: Client
) : PublishersService {
    override fun createPublisherData(userId: String, publishers: List<Publishers>) {
        val storePublisherInfoRecords = mutableListOf<TStorePublisherInfoRecord>()
        publishers.forEach {
            val deptInfos = analysisDept(userId, it.organization)
            val record = TStorePublisherInfoRecord()
            record.id = UUIDUtil.generate()
            record.publisherCode = it.publishersCode
            record.publisherName = it.name
            record.publisherType = it.publishersType.name
            record.owners = JsonUtil.toJson(it.owners)
            record.helper = it.helper
            record.firstLevelDeptId = deptInfos[0].id.toLong()
            record.firstLevelDeptName = deptInfos[0].name
            record.secondLevelDeptId = deptInfos[1].id.toLong()
            record.secondLevelDeptName = deptInfos[1].name
            record.thirdLevelDeptId = deptInfos[2].id.toLong()
            record.thirdLevelDeptName = deptInfos[2].name
            record.fourthLevelDeptId = deptInfos[3].id.toLong()
            record.fourthLevelDeptName = deptInfos[3].name
            record.organizationName = it.organization
            record.ownerDeptName = it.ownerDeptName
            record.certificationFlag = it.certificationFlag
            record.storeType = it.storeType.type.toByte()
            record.creator = userId
            record.modifier = userId
            record.createTime = LocalDateTime.now()
            record.updateTime = LocalDateTime.now()
            storePublisherInfoRecords.add(record)
        }
        // 发布者关联表尚有疑问
        publishersDao.batchCreate(dslContext, storePublisherInfoRecords)
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

    fun analysisDept(userId: String, organization: String): List<DeptInfo> {
        val deptNames = organization.split("/")
        val deptInfos = mutableListOf<DeptInfo>()
        deptNames.forEachIndexed(){ index, deptName ->
            val result = client.get(ServiceDeptResource::class).getDeptByName(userId, deptName).data
            result?.let { it ->
                if (result.count == 1) {
                    deptInfos[index] = it.results[0]
                } else {
                    if (result.count > 1) {
                        result.results.forEach {
                            if (it.parent == deptInfos[index - 1].id) {
                                deptInfos[index] = it
                            }
                        }
                    }
                }
            }
        }
        return deptInfos
    }
}