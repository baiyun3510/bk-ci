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

package com.tencent.devops.store.dao

import com.tencent.devops.common.api.constant.JS
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_HASH_ID
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TExtensionServiceEnvInfo
import com.tencent.devops.model.store.tables.TExtensionServiceFeature
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.pojo.common.KEY_CODE_SRC
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository(value = "SERVICE_COMMON_DAO")
class ServiceCommonDao : AbstractStoreCommonDao() {

    override fun getNewestStoreNameByCode(dslContext: DSLContext, storeCode: String): String? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.select(SERVICE_NAME).from(this)
                .where(SERVICE_CODE.eq(storeCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne(0, String::class.java)
        }
    }

    override fun getStorePublicFlagByCode(dslContext: DSLContext, storeCode: String): Boolean {
        return with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.select(PUBLIC_FLAG).from(this)
                .where(SERVICE_CODE.eq(storeCode))
                .fetchOne(0, Boolean::class.java)!!
        }
    }

    override fun getStoreCodeListByName(dslContext: DSLContext, storeName: String): Result<out Record>? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.select(SERVICE_CODE.`as`(KEY_STORE_CODE)).from(this)
                .where(SERVICE_NAME.contains(storeName))
                .groupBy(SERVICE_CODE)
                .fetch()
        }
    }

    override fun getStoreNameById(dslContext: DSLContext, storeId: String): String? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.select(SERVICE_NAME).from(this)
                .where(ID.eq(storeId))
                .fetchOne(0, String::class.java)
        }
    }

    override fun getLatestStoreInfoListByCodes(
        dslContext: DSLContext,
        storeCodeList: List<String>
    ): Result<out Record>? {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val tExtensionServiceFeature = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE
        return dslContext.select(
            tExtensionService.SERVICE_CODE.`as`(KEY_STORE_CODE),
            tExtensionService.VERSION.`as`(KEY_VERSION),
            tExtensionServiceFeature.REPOSITORY_HASH_ID.`as`(KEY_REPOSITORY_HASH_ID),
            tExtensionServiceFeature.CODE_SRC.`as`(KEY_CODE_SRC)
        ).from(tExtensionService)
            .join(tExtensionServiceFeature)
            .on(tExtensionService.SERVICE_CODE.eq(tExtensionServiceFeature.SERVICE_CODE))
            .where(tExtensionService.SERVICE_CODE.`in`(storeCodeList))
            .and(tExtensionService.LATEST_FLAG.eq(true))
            .fetch()
    }

    override fun getStoreDevLanguages(dslContext: DSLContext, storeCode: String): List<String>? {
        val tes = TExtensionService.T_EXTENSION_SERVICE
        val tExtensionServiceEnvInfo = TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO
        val language = dslContext.select(tExtensionServiceEnvInfo.LANGUAGE).from(tes)
            .join(tExtensionServiceEnvInfo)
            .on(tes.ID.eq(tExtensionServiceEnvInfo.SERVICE_ID))
            .where(tes.SERVICE_CODE.eq(storeCode).and(tes.LATEST_FLAG.eq(true)))
            .fetchOne(0, String()::class.java)!!
        return arrayListOf(language, JS)
    }

    override fun getNewestStoreBaseInfoByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeStatus: Byte?
    ): StoreBaseInfo? {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val tExtensionServiceFeature = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE
        val conditions = mutableListOf<Condition>()
        conditions.add(tExtensionService.SERVICE_CODE.eq(storeCode))
        if (storeStatus != null) {
            conditions.add(tExtensionService.SERVICE_STATUS.eq(storeStatus))
        }
        val serviceRecord = dslContext.selectFrom(tExtensionService)
            .where(conditions)
            .orderBy(tExtensionService.CREATE_TIME.desc())
            .limit(1)
            .fetchOne()
        return if (serviceRecord != null) {
            val publicFlag = dslContext.select(tExtensionServiceFeature.PUBLIC_FLAG).from(tExtensionServiceFeature)
                .where(tExtensionServiceFeature.SERVICE_CODE.eq(storeCode))
                .fetchOne(0, Boolean::class.java)!!
            StoreBaseInfo(
                storeId = serviceRecord.id,
                storeCode = serviceRecord.serviceCode,
                storeName = serviceRecord.serviceName,
                version = serviceRecord.version,
                publicFlag = publicFlag
            )
        } else {
            null
        }
    }
}
