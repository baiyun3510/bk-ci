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
import com.tencent.devops.model.process.tables.TPipelineBuildRecordStage
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordStageRecord
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.time.BuildRecordTimeCost
import com.tencent.devops.process.pojo.pipeline.record.time.BuildRecordTimeStamp
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@Repository
class BuildRecordStageDao {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun batchSave(dslContext: DSLContext, records: List<BuildRecordStage>) {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            records.forEach { record ->
                dslContext.insertInto(this)
                    .set(BUILD_ID, record.buildId)
                    .set(PROJECT_ID, record.projectId)
                    .set(PIPELINE_ID, record.pipelineId)
                    .set(RESOURCE_VERSION, record.resourceVersion)
                    .set(STAGE_ID, record.stageId)
                    .set(EXECUTE_COUNT, record.executeCount)
                    .set(SEQ, record.stageSeq)
                    .set(STAGE_VAR, JsonUtil.toJson(record.stageVar, false))
                    .set(START_TIME, record.startTime)
                    .set(END_TIME, record.endTime)
                    .set(TIMESTAMPS, JsonUtil.toJson(record.timestamps, false))
                    .execute()
            }
        }
    }

    fun updateRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        stageVar: Map<String, Any>,
        buildStatus: BuildStatus?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        timestamps: List<BuildRecordTimeStamp>?,
        timeCost: BuildRecordTimeCost?
    ) {
        logger.info(
            "RECORD|updateStage|$projectId|$pipelineId|$buildId|$stageId|$executeCount" +
                "|stageVar=$stageVar|buildStatus=$buildStatus" +
                "|timestamps=$timestamps|timeCost=$timeCost"
        )
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            val update = dslContext.update(this)
                .set(STAGE_VAR, JsonUtil.toJson(stageVar, false))
            buildStatus?.let { update.set(STATUS, buildStatus.name) }
            startTime?.let { update.set(START_TIME, startTime) }
            endTime?.let { update.set(END_TIME, endTime) }
            timestamps?.let { update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false)) }
            timeCost?.let { update.set(TIME_COST, JsonUtil.toJson(timeCost, false)) }
            update.where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(STAGE_ID.eq(stageId))
            ).execute()
        }
    }

    fun getRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ): List<BuildRecordStage> {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            return dslContext.selectFrom(this)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).orderBy(SEQ.asc()).fetch(mapper)
        }
    }

    fun getRecordContainerVar(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int
    ): Map<String, Any>? {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            return dslContext.select(STAGE_VAR)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(STAGE_VAR.eq(stageId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).fetchOne(0, String::class.java)?.let {
                    JsonUtil.getObjectMapper().readValue(it) as Map<String, Any>
                }
        }
    }

    class BuildRecordStageJooqMapper : RecordMapper<TPipelineBuildRecordStageRecord, BuildRecordStage> {
        override fun map(record: TPipelineBuildRecordStageRecord?): BuildRecordStage? {
            return record?.run {
                BuildRecordStage(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    resourceVersion = resourceVersion,
                    executeCount = executeCount,
                    stageId = stageId,
                    stageVar = JsonUtil.getObjectMapper().readValue(stageVar) as MutableMap<String, Any>,
                    stageSeq = seq,
                    status = status,
                    startTime = startTime,
                    endTime = endTime,
                    timestamps = timestamps?.let {
                        JsonUtil.getObjectMapper().readValue(it) as List<BuildRecordTimeStamp>
                    } ?: emptyList(),
                    timeCost = timeCost?.let {
                        JsonUtil.getObjectMapper().readValue(it, BuildRecordTimeCost::class.java)
                    }
                )
            }
        }
    }

    companion object {
        private val mapper = BuildRecordStageJooqMapper()
    }
}
