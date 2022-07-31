package com.tencent.devops.store.dao.common

import com.tencent.devops.model.store.tables.TStorePublisherInfo
import com.tencent.devops.model.store.tables.records.TStorePublisherInfoRecord
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
                        .and(PUBLISHER_NAME.eq(it.name))
                        .and(PUBLISHER_TYPE.eq(it.publishersType.name))
                        .and(OWNERS.eq(it.owners[0]))
                        .and(STORE_TYPE.eq(it.storeType.type.toByte())))
                }
            ).execute().size
        }
    }
}