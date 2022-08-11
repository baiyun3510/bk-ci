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

package com.tencent.devops.environment.dao

import com.tencent.devops.environment.pojo.label.LabelInfo
import com.tencent.devops.model.environment.tables.TLabel
import com.tencent.devops.model.environment.tables.records.TLabelRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class LabelDao {
    fun listLabels(dslContext: DSLContext, projectId: String): List<TLabelRecord>? {
        with(TLabel.T_LABEL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun getLabelId(
        dslContext: DSLContext,
        projectId: String,
        labelKey: String,
        labelValue: String = ""
    ): Long? {
        with(TLabel.T_LABEL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(LABEL_KEY.eq(labelKey))
                .and(LABEL_VALUE.eq(labelValue))
                .fetchOne()?.id
        }
    }

    fun getLabelInfo(
        dslContext: DSLContext,
        labelId: Long
    ): String? {
        with(TLabel.T_LABEL) {
            return dslContext.selectFrom(this)
                .where(ID.eq(labelId))
                .fetchOne()?.labelKey
        }
    }

    fun addLabel(
        projectId: String,
        labelInfo: LabelInfo,
        dslContext: DSLContext
    ): Long {
        with(TLabel.T_LABEL) {
            // 先插入单key标签记录
            dslContext.insertInto(
                this,
                PROJECT_ID,
                LABEL_KEY,
                LABEL_VALUE,
                DESCRIPTION,
                GMT_CREATE,
                GMT_MODIFIED
            )
                .values(
                    projectId,
                    labelInfo.labelKey,
                    "",
                    "",
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .onDuplicateKeyIgnore()
                .execute()


            // 再插入标签key value数据，存在则更新
            val labelId = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(LABEL_KEY.eq(labelInfo.labelKey))
                .and(LABEL_VALUE.eq(labelInfo.labelValue))
                .fetchOne()?.id

            if (labelId != null) {
                dslContext.update(this)
                    .set(LABEL_KEY, labelInfo.labelKey)
                    .set(LABEL_VALUE, labelInfo.labelValue)
                    .set(GMT_MODIFIED, LocalDateTime.now())
                    .where(ID.eq(labelId))
                    .execute()

                return labelId
            } else {
                return dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    LABEL_KEY,
                    LABEL_VALUE,
                    DESCRIPTION,
                    GMT_CREATE,
                    GMT_MODIFIED
                )
                    .values(
                        projectId,
                        labelInfo.labelKey,
                        labelInfo.labelValue,
                        labelInfo.description,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                    )
                    .returning(ID)
                    .fetchOne()!!.id
            }
        }
    }

    fun batchDeleteLabel(dslContext: DSLContext, labelIds: List<Long>) {
        if (labelIds.isEmpty()) {
            return
        }

        with(TLabel.T_LABEL) {
            dslContext.deleteFrom(this)
                .where(ID.`in`(labelIds))
                .execute()
        }
    }
}
