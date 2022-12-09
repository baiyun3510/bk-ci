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

package com.tencent.devops.process.dao.record

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.model.process.tables.TPipelineBuildRecordModel
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordModelRecord
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@Repository
class BuildRecordModelDao {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun createRecord(dslContext: DSLContext, record: BuildRecordModel) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            dslContext.insertInto(this)
                .set(BUILD_ID, record.buildId)
                .set(PROJECT_ID, record.projectId)
                .set(PIPELINE_ID, record.pipelineId)
                .set(RESOURCE_VERSION, record.resourceVersion)
                .set(BUILD_NUM, record.buildNum)
                .set(EXECUTE_COUNT, record.executeCount)
                .set(START_USER, record.startUser)
                .set(START_TYPE, record.startType)
                .set(MODEL_VAR, JsonUtil.toJson(record.modelVar, false))
                .set(STATUS, record.status)
                .set(CANCEL_USER, record.cancelUser)
                .set(TIMESTAMPS, JsonUtil.toJson(record.timestamps, false))
                .execute()
        }
    }

    fun updateRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?,
        buildStatus: BuildStatus,
        modelVar: Map<String, Any>,
        cancelUser: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        timestamps: List<BuildRecordTimeStamp>?
    ) {
        logger.info(
            "RECORD|updateModel|$projectId|$pipelineId|$buildId|$executeCount" +
                "|buildStatus=$buildStatus" +
                "|timestamps=$timestamps|cancelUser=$cancelUser"
        )
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            val update = dslContext.update(this)
                .set(STATUS, buildStatus.name)
                .set(MODEL_VAR, JsonUtil.toJson(modelVar, false))
            cancelUser?.let { update.set(CANCEL_USER, cancelUser) }
            timestamps?.let { update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false)) }
            val exeCount = executeCount ?: dslContext.select(DSL.max(EXECUTE_COUNT)).where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
            ).fetchAny()?.value1()!!
            update.where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(EXECUTE_COUNT.eq(exeCount))
            ).execute()
        }
    }

    fun getRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ): BuildRecordModel? {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            return dslContext.selectFrom(this)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).fetchAny(mapper)
        }
    }

    class BuildRecordPipelineJooqMapper : RecordMapper<TPipelineBuildRecordModelRecord, BuildRecordModel> {
        override fun map(record: TPipelineBuildRecordModelRecord?): BuildRecordModel? {
            return record?.run {
                BuildRecordModel(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    resourceVersion = resourceVersion,
                    executeCount = executeCount,
                    buildNum = buildNum,
                    modelVar = JsonUtil.getObjectMapper().readValue(modelVar) as MutableMap<String, Any>,
                    startUser = startUser,
                    startType = startType,
                    status = status,
                    cancelUser = cancelUser,
                    timestamps = timestamps?.let {
                        JsonUtil.getObjectMapper().readValue(it) as List<BuildRecordTimeStamp>
                    } ?: emptyList()
                )
            }
        }
    }

    fun updateBuildCancelUser(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        cancelUser: String
    ) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            dslContext.update(this)
                .set(CANCEL_USER, cancelUser)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    companion object {
        private val mapper = BuildRecordPipelineJooqMapper()
    }
}
