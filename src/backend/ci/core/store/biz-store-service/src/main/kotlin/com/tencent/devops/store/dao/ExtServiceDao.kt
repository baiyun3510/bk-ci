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

import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TExtensionServiceEnvInfo
import com.tencent.devops.model.store.tables.TExtensionServiceFeature
import com.tencent.devops.model.store.tables.TExtensionServiceItemRel
import com.tencent.devops.model.store.tables.TExtensionServiceLabelRel
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.records.TExtensionServiceRecord
import com.tencent.devops.store.pojo.ExtServiceCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_SERVICE_CODE
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record2
import org.jooq.Result
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class ExtServiceDao {

    fun createExtService(
        dslContext: DSLContext,
        userId: String,
        id: String,
        extServiceCreateInfo: ExtServiceCreateInfo
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_NAME,
                SERVICE_CODE,
                CLASSIFY_ID,
                VERSION,
                SERVICE_STATUS,
                SERVICE_STATUS_MSG,
                LOGO_URL,
                ICON,
                SUMMARY,
                DESCRIPTION,
                PUBLISHER,
                PUB_TIME,
                LATEST_FLAG,
                DELETE_FLAG,
                OWNER,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    id,
                    extServiceCreateInfo.serviceName,
                    extServiceCreateInfo.serviceCode,
                    extServiceCreateInfo.classify,
                    extServiceCreateInfo.version,
                    extServiceCreateInfo.status.toByte(),
                    extServiceCreateInfo.statusMsg,
                    extServiceCreateInfo.logoUrl,
                    extServiceCreateInfo.iconData,
                    extServiceCreateInfo.summary,
                    extServiceCreateInfo.description,
                    extServiceCreateInfo.publisher,
                    LocalDateTime.now(),
                    extServiceCreateInfo.latestFlag,
                    extServiceCreateInfo.deleteFlag,
                    userId,
                    extServiceCreateInfo.creatorUser,
                    userId,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun deleteServiceById(
        dslContext: DSLContext,
        serviceId: String
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.deleteFrom(this).where(ID.eq(serviceId)).execute()
        }
    }

    /**
     * 清空LATEST_FLAG
     */
    fun cleanLatestFlag(dslContext: DSLContext, serviceCode: String) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.update(this)
                .set(LATEST_FLAG, false)
                .where(SERVICE_CODE.eq(serviceCode))
                .execute()
        }
    }

    fun deleteExtServiceData(
        dslContext: DSLContext,
        serviceCode: String
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.deleteFrom(this).where(SERVICE_CODE.eq(serviceCode)).execute()
        }
    }

    fun getExtServiceIds(dslContext: DSLContext, serviceCode: String): Result<Record2<String, Boolean>> {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            return dslContext.select(ID, LATEST_FLAG).from(this)
                .where(SERVICE_CODE.eq(serviceCode))
                .and(LATEST_FLAG.eq(true))
                .fetch()
        }
    }

    fun updateExtServiceBaseInfo(
        dslContext: DSLContext,
        userId: String,
        serviceId: String,
        extServiceUpdateInfo: ExtServiceUpdateInfo
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val baseStep = dslContext.update(this)
            val serviceName = extServiceUpdateInfo.serviceName
            if (null != serviceName) {
                baseStep.set(SERVICE_NAME, serviceName)
            }
            val summary = extServiceUpdateInfo.summary
            if (null != summary) {
                baseStep.set(SUMMARY, summary)
            }
            val status = extServiceUpdateInfo.status
            if (null != status) {
                baseStep.set(SERVICE_STATUS, status.toByte())
            }
            val statusMsg = extServiceUpdateInfo.statusMsg
            if (null != statusMsg) {
                baseStep.set(SERVICE_STATUS_MSG, statusMsg)
            }
            val version = extServiceUpdateInfo.version
            if (null != version) {
                baseStep.set(VERSION, version)
            }
            val description = extServiceUpdateInfo.description
            if (null != description) {
                baseStep.set(DESCRIPTION, description)
            }
            val logoUrl = extServiceUpdateInfo.logoUrl
            if (null != logoUrl) {
                baseStep.set(LOGO_URL, logoUrl)
            }
            val iconData = extServiceUpdateInfo.iconData
            if (null != iconData) {
                baseStep.set(ICON, iconData)
            }

            val latest = extServiceUpdateInfo.latestFlag
            if (null != latest) {
                baseStep.set(LATEST_FLAG, latest)
            }

            val publisher = extServiceUpdateInfo.publisher
            if (null != publisher) {
                baseStep.set(PUBLISHER, publisher)
                baseStep.set(PUB_TIME, LocalDateTime.now())
            }

            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId))
                .execute()
        }
    }

    fun countByUser(
        dslContext: DSLContext,
        userId: String,
        serviceName: String?
    ): Long {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val conditions = generateGetMemberConditions(tExtensionService, userId, tStoreMember, serviceName)
        return dslContext.select(DSL.countDistinct(tExtensionService.SERVICE_CODE))
            .from(tExtensionService)
            .leftJoin(tStoreMember)
            .on(tExtensionService.SERVICE_CODE.eq(tStoreMember.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Long::class.java)!!
    }

    fun countReleaseServiceByCode(dslContext: DSLContext, serviceCode: String): Int {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            return dslContext.selectCount().from(this).where(
                SERVICE_CODE.eq(serviceCode).and(DELETE_FLAG.eq(false)).and(
                    SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte())
                )
            ).fetchOne(0, Int::class.java)!!
        }
    }

    fun getMyService(
        dslContext: DSLContext,
        userId: String,
        serviceName: String?,
        page: Int,
        pageSize: Int
    ): Result<out Record>? {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val tExtensionServiceEnvInfo = TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO
        // 查找每组serviceCode最新的记录
        val t = dslContext.select(
            tExtensionService.SERVICE_CODE.`as`(KEY_SERVICE_CODE),
            DSL.max(tExtensionService.CREATE_TIME).`as`(KEY_CREATE_TIME)
        ).from(tExtensionService)
            .groupBy(tExtensionService.SERVICE_CODE)
        val conditions = generateGetMemberConditions(tExtensionService, userId, tStoreMember, serviceName)
        val baseStep = dslContext.select(
            tExtensionService.ID,
            tExtensionService.SERVICE_CODE,
            tExtensionService.SERVICE_NAME,
            tExtensionService.CLASSIFY_ID,
            tExtensionService.LOGO_URL,
            tExtensionService.VERSION,
            tExtensionService.SERVICE_STATUS,
            tExtensionService.PUBLISHER,
            tExtensionService.PUB_TIME,
            tExtensionService.CREATOR,
            tExtensionService.CREATE_TIME,
            tExtensionService.MODIFIER,
            tExtensionService.UPDATE_TIME,
            tExtensionServiceEnvInfo.LANGUAGE
        )
            .from(tExtensionService)
            .join(t)
            .on(
                tExtensionService.SERVICE_CODE.eq(t.field(KEY_SERVICE_CODE, String::class.java)).and(
                    tExtensionService.CREATE_TIME.eq(
                        t.field(
                            KEY_CREATE_TIME,
                            LocalDateTime::class.java
                        )
                    )
                )
            )
            .join(tStoreMember)
            .on(tExtensionService.SERVICE_CODE.eq(tStoreMember.STORE_CODE))
            .leftJoin(tExtensionServiceEnvInfo)
            .on(tExtensionService.ID.eq(tExtensionServiceEnvInfo.SERVICE_ID))
            .where(conditions)
            .groupBy(tExtensionService.SERVICE_CODE)
            .orderBy(tExtensionService.UPDATE_TIME.desc())
        return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
    }

    fun getServiceById(dslContext: DSLContext, serviceId: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(ID.eq(serviceId)).fetchOne()
        }
    }

    fun getServiceByCode(
        dslContext: DSLContext,
        serviceCode: String,
        page: Int,
        pageSize: Int
    ): Result<TExtensionServiceRecord>? {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val conditions = mutableListOf<Condition>()
        conditions.add(tExtensionService.SERVICE_CODE.eq(serviceCode))
        conditions.add(tExtensionService.DELETE_FLAG.eq(false))
        return dslContext.selectFrom(tExtensionService)
            .where(conditions)
            .orderBy(tExtensionService.CREATE_TIME.desc()).limit((page - 1) * pageSize, pageSize)
            .fetch()
    }

    fun countByCode(dslContext: DSLContext, serviceCode: String): Int {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectCount().from(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByName(dslContext: DSLContext, serviceName: String, serviceCode: String? = null): Int {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(SERVICE_NAME.eq(serviceName))
            conditions.add(DELETE_FLAG.eq(false))
            if (serviceCode != null) {
                conditions.add(SERVICE_CODE.eq(serviceCode))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
        }
    }

    fun getServiceLatestByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode))
                .and(LATEST_FLAG.eq(true)).fetchOne()
        }
    }

    fun getNewestServiceByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode))
                .orderBy(CREATE_TIME.desc()).limit(1).fetchOne()
        }
    }

    fun listServiceByCode(dslContext: DSLContext, serviceCode: String): Result<TExtensionServiceRecord>? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode))
                .orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    fun getServiceByName(dslContext: DSLContext, serviceName: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(SERVICE_NAME.eq(serviceName)).fetchOne()
        }
    }

    fun getExtService(dslContext: DSLContext, serviceCode: String, version: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this)
                .where(SERVICE_CODE.eq(serviceCode).and(VERSION.like(VersionUtils.generateQueryVersion(version))))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun listServiceByStatus(
        dslContext: DSLContext,
        serviceStatus: ExtServiceStatusEnum,
        page: Int?,
        pageSize: Int?,
        timeDescFlag: Boolean? = null
    ): Result<TExtensionServiceRecord>? {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val baseStep = dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_STATUS.eq(serviceStatus.status.toByte()))
            if (timeDescFlag != null && timeDescFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    /**
     * 审核原子时，更新状态、类型等信息
     */
    fun approveServiceFromOp(
        dslContext: DSLContext,
        userId: String,
        serviceId: String,
        serviceStatus: Byte,
        approveReq: ServiceApproveReq,
        pubTime: LocalDateTime? = null
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.update(this)
                .set(SERVICE_STATUS, serviceStatus)
                .set(SERVICE_STATUS_MSG, approveReq.message)
                .set(PUB_TIME, pubTime)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId))
                .execute()
        }
    }

    /**
     * 设置可用的微扩展版本状态为下架中、已下架
     */
    fun setServiceStatusByCode(
        dslContext: DSLContext,
        serviceCode: String,
        serviceOldStatus: Byte,
        serviceNewStatus: Byte,
        userId: String,
        msg: String?
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val baseStep = dslContext.update(this)
                .set(SERVICE_STATUS, serviceNewStatus)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(SERVICE_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_CODE.eq(serviceCode))
                .and(SERVICE_STATUS.eq(serviceOldStatus))
                .execute()
        }
    }

    /**
     * 微扩展商店搜索结果，总数
     */
    fun count(
        dslContext: DSLContext,
        keyword: String?,
        classifyCode: String?,
        bkService: Long?,
        rdType: ServiceTypeEnum? = null,
        labelCodeList: List<String>?,
        score: Int?
    ): Int {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val tExtensionServiceFeature = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE
        val baseStep = dslContext.select(DSL.countDistinct(tExtensionService.ID)).from(tExtensionService)
            .join(tExtensionServiceFeature).on(tExtensionService.SERVICE_CODE.eq(tExtensionServiceFeature.SERVICE_CODE))
        val conditions = handleMainListBaseStep(
            tExtensionService = tExtensionService,
            dslContext = dslContext,
            baseStep = baseStep,
            keyword = keyword,
            classifyCode = classifyCode,
            rdType = rdType,
            labelCodeList = labelCodeList,
            bkService = bkService,
            score = score
        )
        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
    }

    private fun handleMainListBaseStep(
        tExtensionService: TExtensionService,
        dslContext: DSLContext,
        baseStep: SelectOnConditionStep<out Record>,
        keyword: String?,
        classifyCode: String?,
        rdType: ServiceTypeEnum?,
        labelCodeList: List<String>?,
        bkService: Long?,
        score: Int?
    ): MutableList<Condition> {
        val storeType = StoreTypeEnum.SERVICE.type.toByte()
        val conditions = setExtServiceVisibleCondition(tExtensionService)
        if (!keyword.isNullOrEmpty()) {
            conditions.add(
                tExtensionService.SERVICE_NAME.contains(keyword)
                    .or(tExtensionService.SUMMARY.contains(keyword))
            )
        }
        if (!classifyCode.isNullOrEmpty()) {
            val tClassify = TClassify.T_CLASSIFY
            val classifyId = dslContext.select(tClassify.ID)
                .from(tClassify)
                .where(tClassify.CLASSIFY_CODE.eq(classifyCode).and(tClassify.TYPE.eq(storeType)))
                .fetchOne(0, String::class.java)
            conditions.add(tExtensionService.CLASSIFY_ID.eq(classifyId))
        }

        if (rdType != null) {
            val tExtensionServiceFeature = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE
            baseStep.leftJoin(tExtensionServiceFeature)
                .on(tExtensionService.SERVICE_CODE.eq(tExtensionServiceFeature.SERVICE_CODE))
            conditions.add(tExtensionServiceFeature.SERVICE_TYPE.eq(rdType.type.toByte()))
        }
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val tLabel = TLabel.T_LABEL
            val labelIdList = dslContext.select(tLabel.ID)
                .from(tLabel)
                .where(tLabel.LABEL_CODE.`in`(labelCodeList)).and(tLabel.TYPE.eq(storeType))
                .fetch().map { it.value1() }
            val tExtensionServiceLabelRel = TExtensionServiceLabelRel.T_EXTENSION_SERVICE_LABEL_REL
            baseStep.leftJoin(tExtensionServiceLabelRel)
                .on(tExtensionService.ID.eq(tExtensionServiceLabelRel.SERVICE_ID))
            conditions.add(tExtensionServiceLabelRel.LABEL_ID.`in`(labelIdList))
        }
        if (bkService != null) {
            val tExtensionServiceItemRel = TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL
            val serviceIdList = dslContext.select(tExtensionServiceItemRel.SERVICE_ID).from(tExtensionServiceItemRel)
                .where(tExtensionServiceItemRel.BK_SERVICE_ID.eq(bkService)).fetch().map { it.value1() }
            conditions.add(tExtensionService.ID.`in`(serviceIdList))
        }

        if (score != null) {
            val tStoreStatisticsTotal = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
            val t = dslContext.select(
                tStoreStatisticsTotal.STORE_CODE,
                tStoreStatisticsTotal.STORE_TYPE,
                tStoreStatisticsTotal.SCORE_AVERAGE
            ).from(tStoreStatisticsTotal).asTable("t")
            baseStep.leftJoin(t).on(tExtensionService.SERVICE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            val convertScore = BigDecimal.valueOf(score.toLong())
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java)!!.ge(convertScore))
            conditions.add(t.field("STORE_TYPE", Byte::class.java)!!.eq(storeType))
        }
        return conditions
    }

    /**
     * 设置微扩展状态（单个版本）
     */
    fun setServiceStatusById(
        dslContext: DSLContext,
        serviceId: String,
        serviceStatus: Byte,
        userId: String,
        msg: String?
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val baseStep = dslContext.update(this)
                .set(SERVICE_STATUS, serviceStatus)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(SERVICE_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId))
                .execute()
        }
    }

    fun queryServicesFromOp(
        dslContext: DSLContext,
        serviceName: String?,
        itemId: String?,
        isRecommend: Boolean?,
        isPublic: Boolean?,
        labelId: String?,
        isApprove: Boolean?,
        sortType: String?,
        desc: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<out Record> {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val tExtensionServiceFeature = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE
        val tExtensionServiceItemRel = TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        // 查找每组serviceCode最新的记录
        val tmp = dslContext.select(
            tExtensionService.SERVICE_CODE.`as`(KEY_SERVICE_CODE),
            DSL.max(tExtensionService.CREATE_TIME).`as`(KEY_CREATE_TIME)
        ).from(tExtensionService).groupBy(tExtensionService.SERVICE_CODE)
        val baseStep = dslContext.select(
            tExtensionService.ID,
            tExtensionService.SERVICE_STATUS,
            tExtensionService.SERVICE_NAME,
            tExtensionService.SERVICE_CODE,
            tExtensionService.VERSION,
            tExtensionService.PUB_TIME,
            tExtensionService.PUBLISHER,
            tExtensionService.UPDATE_TIME,
            tStoreProjectRel.PROJECT_CODE
        ).from(tExtensionService)
            .join(tExtensionServiceFeature)
            .on(tExtensionService.SERVICE_CODE.eq(tExtensionServiceFeature.SERVICE_CODE))
            .join(tStoreProjectRel)
            .on(tExtensionService.SERVICE_CODE.eq(tStoreProjectRel.STORE_CODE))
            .join(tmp)
            .on(tExtensionService.SERVICE_CODE.eq(tmp.field(KEY_SERVICE_CODE, String::class.java)).and(
                tExtensionService.CREATE_TIME.eq(tmp.field(KEY_CREATE_TIME, LocalDateTime::class.java))
            ))
        val conditions = getOpServiceCondition(
            tStoreProjectRel = tStoreProjectRel,
            tExtensionService = tExtensionService,
            tExtensionServiceItemRel = tExtensionServiceItemRel,
            tExtensionServiceFeature = tExtensionServiceFeature,
            baseStep = baseStep,
            serviceName = serviceName,
            itemId = itemId,
            isPublic = isPublic,
            isRecommend = isRecommend,
            isApprove = isApprove
        )
        val realSortType = tExtensionService.field(sortType)
        val orderByStep = if (desc != null && desc) {
            realSortType!!.desc()
        } else {
            realSortType!!.asc()
        }
        return baseStep.where(conditions).orderBy(orderByStep).limit((page - 1) * pageSize, pageSize).fetch()
    }

    private fun getOpServiceCondition(
        tStoreProjectRel: TStoreProjectRel,
        tExtensionService: TExtensionService,
        tExtensionServiceItemRel: TExtensionServiceItemRel,
        tExtensionServiceFeature: TExtensionServiceFeature,
        baseStep: SelectOnConditionStep<out Record>,
        serviceName: String?,
        itemId: String?,
        isPublic: Boolean?,
        isRecommend: Boolean?,
        isApprove: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte()))
        conditions.add(tExtensionService.DELETE_FLAG.eq(false))
        conditions.add(tExtensionService.LATEST_FLAG.eq(true))
        if (null != serviceName) {
            conditions.add(tExtensionService.SERVICE_NAME.like("%$serviceName%"))
        }
        if (null != itemId) {
            conditions.add(tExtensionServiceItemRel.ITEM_ID.eq(itemId))
            baseStep.join(tExtensionServiceItemRel).on(tExtensionService.ID.eq(tExtensionServiceItemRel.SERVICE_ID))
        }

        if (null != isPublic) {
            conditions.add(tExtensionServiceFeature.PUBLIC_FLAG.eq(isPublic))
        }

        if (null != isRecommend) {
            conditions.add(tExtensionServiceFeature.RECOMMEND_FLAG.eq(isRecommend))
        }

        if (null != isApprove) {
            if (isApprove) {
                conditions.add(tExtensionService.SERVICE_STATUS.eq(ExtServiceStatusEnum.AUDITING.status.toByte()))
            } else {
                conditions.add(tExtensionService.SERVICE_STATUS.notEqual(ExtServiceStatusEnum.AUDITING.status.toByte()))
            }
        }
        return conditions
    }

    fun queryCountFromOp(
        dslContext: DSLContext,
        serviceName: String?,
        itemId: String?,
        isRecommend: Boolean?,
        isPublic: Boolean?,
        isApprove: Boolean?
    ): Int {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val tExtensionServiceFeature = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE
        val tExtensionServiceItemRel = TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL

        val baseStep = dslContext.select(
           DSL.countDistinct(tExtensionService.ID)
        ).from(tExtensionService)
            .join(tExtensionServiceFeature)
            .on(tExtensionService.SERVICE_CODE.eq(tExtensionServiceFeature.SERVICE_CODE))
            .join(tStoreProjectRel)
            .on(tExtensionService.SERVICE_CODE.eq(tStoreProjectRel.STORE_CODE))
        val conditions = getOpServiceCondition(
            tStoreProjectRel = tStoreProjectRel,
            tExtensionService = tExtensionService,
            tExtensionServiceItemRel = tExtensionServiceItemRel,
            tExtensionServiceFeature = tExtensionServiceFeature,
            baseStep = baseStep,
            serviceName = serviceName,
            itemId = itemId,
            isPublic = isPublic,
            isRecommend = isRecommend,
            isApprove = isApprove
        )
        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
    }

    fun getLatestFlag(dslContext: DSLContext, serviceCode: String): Boolean {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            return dslContext.selectCount().from(this)
                .where(SERVICE_CODE.eq(serviceCode)
                    .and(SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte()))
                )
                .fetchOne(0, Int::class.java)!! < 1
        }
    }

    /**
     * 研发商店搜索结果列表
     */
    fun list(
        dslContext: DSLContext,
        keyword: String?,
        classifyCode: String?,
        bkService: Long?,
        labelCodeList: List<String>?,
        score: Int?,
        rdType: ServiceTypeEnum?,
        sortType: ExtServiceSortTypeEnum?,
        desc: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<out Record>? {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val tExtensionServiceFeature = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE
        val baseStep = dslContext.select(
            tExtensionService.ID,
            tExtensionService.SERVICE_NAME,
            tExtensionService.CLASSIFY_ID,
            tExtensionService.SERVICE_CODE,
            tExtensionService.LOGO_URL,
            tExtensionService.PUBLISHER,
            tExtensionService.SUMMARY,
            tExtensionService.MODIFIER,
            tExtensionService.UPDATE_TIME,
            tExtensionServiceFeature.RECOMMEND_FLAG,
            tExtensionServiceFeature.PUBLIC_FLAG
        ).from(tExtensionService)
            .leftJoin(tExtensionServiceFeature)
            .on(tExtensionService.SERVICE_CODE.eq(tExtensionServiceFeature.SERVICE_CODE))

        val conditions = handleMainListBaseStep(
            tExtensionService = tExtensionService,
            dslContext = dslContext,
            baseStep = baseStep,
            keyword = keyword,
            classifyCode = classifyCode,
            rdType = rdType,
            labelCodeList = labelCodeList,
            bkService = bkService,
            score = score
        )

        if (null != sortType) {
            if (sortType == ExtServiceSortTypeEnum.DOWNLOAD_COUNT && score == null) {
                val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
                val t =
                    dslContext.select(tas.STORE_CODE, tas.DOWNLOADS.`as`(ExtServiceSortTypeEnum.DOWNLOAD_COUNT.name))
                        .from(tas).where(tas.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte())).asTable("t")
                baseStep.leftJoin(t)
                    .on(tExtensionService.SERVICE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }
            val sortTypeField = ExtServiceSortTypeEnum.getSortType(sortType.name)
            val realSortType = if (sortType == ExtServiceSortTypeEnum.DOWNLOAD_COUNT) {
                DSL.field(sortTypeField)
            } else {
                tExtensionService.field(sortTypeField)
            }

            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(realSortType!!.desc())
            } else {
                baseStep.where(conditions).orderBy(realSortType!!.asc())
            }
        } else {
            baseStep.where(conditions)
        }
        return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
    }

    fun getServiceByProjectCode(dslContext: DSLContext, projectCode: String): Result<out Record>? {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val tExtensionServiceFeature = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE
        val baseStep = dslContext.select(
            tExtensionService.ID,
            tExtensionService.SERVICE_NAME,
            tExtensionService.SERVICE_CODE,
            tExtensionService.LOGO_URL,
            tExtensionService.PUBLISHER,
            tExtensionService.PUB_TIME,
            tExtensionService.VERSION,
            tExtensionService.CREATOR,
            tExtensionService.CREATE_TIME,
            tExtensionService.MODIFIER,
            tExtensionService.UPDATE_TIME,
            tExtensionService.SERVICE_STATUS,
            tExtensionServiceFeature.PUBLIC_FLAG,
            tStoreProjectRel.CREATE_TIME,
            tStoreProjectRel.CREATOR,
            tStoreProjectRel.TYPE
        ).from(tExtensionService)
            .leftOuterJoin(tStoreProjectRel)
            .on(tExtensionService.SERVICE_CODE.eq(tStoreProjectRel.STORE_CODE))
            .leftJoin(tExtensionServiceFeature)
            .on(tExtensionService.SERVICE_CODE.eq(tExtensionServiceFeature.SERVICE_CODE))
        val condition = mutableListOf<Condition>()
        condition.add(tExtensionService.LATEST_FLAG.eq(true))
        condition.add(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
        condition.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte()))
        condition.add(tExtensionService.DELETE_FLAG.eq(false))
        condition.add(tStoreProjectRel.TYPE.notEqual(StoreProjectTypeEnum.TEST.type.toByte()))
        return baseStep.where(condition).groupBy(tExtensionService.SERVICE_CODE).orderBy(tExtensionService.UPDATE_TIME.desc()).fetch()
    }

    /**
     * 设置见扩展查询条件
     */
    protected fun setExtServiceVisibleCondition(a: TExtensionService): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte())) // 已发布的
        conditions.add(a.LATEST_FLAG.eq(true)) // 最新版本
        conditions.add(a.DELETE_FLAG.eq(false)) // 只查没有被删除的扩展
        return conditions
    }

    private fun generateGetMemberConditions(
        a: TExtensionService,
        userId: String,
        b: TStoreMember,
        serviceName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.DELETE_FLAG.eq(false))
        conditions.add(b.USERNAME.eq(userId))
        conditions.add(b.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte()))
        if (null != serviceName) {
            conditions.add(a.SERVICE_NAME.contains(serviceName))
        }
        return conditions
    }

    fun batchUpdateService(dslContext: DSLContext, serviceRecords: List<TExtensionServiceRecord>) {
        if (serviceRecords.isEmpty()) {
            return
        }
        dslContext.batchUpdate(serviceRecords).execute()
    }
}
