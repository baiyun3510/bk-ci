package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.constant.NAME
import com.tencent.devops.model.store.tables.TStorePublisherInfo
import com.tencent.devops.model.store.tables.TStorePublisherMemberRel
import com.tencent.devops.model.store.tables.records.TStorePublisherInfoRecord
import com.tencent.devops.model.store.tables.records.TStorePublisherMemberRelRecord
import com.tencent.devops.store.pojo.common.Publishers
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublishersDao {

    fun batchCreate(dslContext: DSLContext, storePublisherInfos: List<TStorePublisherInfoRecord>): Int {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.batchInsert(storePublisherInfos).execute().size
        }
    }

    fun batchDelete(dslContext: DSLContext, publishers: List<Publishers>): Int {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.batch(publishers.map {
                dslContext.deleteFrom(this)
                    .where(PUBLISHER_CODE.eq(it.publishersCode)
                        .and(PUBLISHER_TYPE.eq(it.publishersType.name))
                        .and(STORE_TYPE.eq(it.storeType.type.toByte())))
                }
            ).execute().size
        }
    }

    fun batchUpdate(dslContext: DSLContext, storePublisherInfos: List<TStorePublisherInfoRecord>): Int {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.batch(storePublisherInfos.map {
                dslContext.update(this)
                    .set(PUBLISHER_NAME, it.publisherName)
                    .set(FIRST_LEVEL_DEPT_ID, it.firstLevelDeptId)
                    .set(FIRST_LEVEL_DEPT_NAME, it.firstLevelDeptName)
                    .set(SECOND_LEVEL_DEPT_ID, it.secondLevelDeptId)
                    .set(SECOND_LEVEL_DEPT_NAME, it.secondLevelDeptName)
                    .set(THIRD_LEVEL_DEPT_ID, it.thirdLevelDeptId)
                    .set(THIRD_LEVEL_DEPT_NAME, it.thirdLevelDeptName)
                    .set(PUBLISHER_TYPE, it.publisherType)
                    .set(OWNERS, it.owners)
                    .set(CERTIFICATION_FLAG, it.certificationFlag)
                    .set(ORGANIZATION_NAME, it.organizationName)
                    .set(OWNER_DEPT_NAME, it.ownerDeptName)
                    .set(HELPER, it.helper)
                    .set(UPDATE_TIME, it.updateTime)
                    .set(MODIFIER, it.modifier)
                    .where(PUBLISHER_CODE.eq(it.publisherCode))
                    .and(STORE_TYPE.eq(it.storeType))
            }
            ).execute().size
        }
    }

    fun batchCreatePublisherMemberRel(
        dslContext: DSLContext,
        storePublisherMemberRelInfos: List<TStorePublisherMemberRelRecord>
    ): Int {
        return dslContext.batchInsert(storePublisherMemberRelInfos).execute().size
    }

    fun batchDeletePublisherMemberRelById(
        dslContext: DSLContext,
        organizePublishersIds: List<String>
    ) {
        with(TStorePublisherMemberRel.T_STORE_PUBLISHER_MEMBER_REL) {
            dslContext.deleteFrom(this).where(PUBLISHER_ID.`in`(organizePublishersIds)).execute()
        }
    }

    fun getPublisherIdByCode(dslContext: DSLContext, publisherCodes: List<String>): List<String> {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.select(ID).from(this)
                .where(PUBLISHER_CODE.`in`(publisherCodes))
                .fetchInto(String::class.java)
        }
    }
}