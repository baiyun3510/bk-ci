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

import com.tencent.devops.model.environment.tables.TLabel
import com.tencent.devops.model.environment.tables.TNodeLabel
import com.tencent.devops.model.environment.tables.records.TNodeLabelRecord
import org.jooq.DSLContext
import org.jooq.Record4
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class NodeLabelDao {

    /**
     * 获取节点关联的标签
     */
    fun getNodeLabels(dslContext: DSLContext, nodeId: Long): Result<Record4<Long, String, String, String>>? {
        val t1 = TNodeLabel.T_NODE_LABEL.`as`("t1")
        val t2 = TLabel.T_LABEL.`as`("t2")
        return dslContext.select(t1.LABEL_ID, t2.LABEL_KEY, t2.LABEL_VALUE, t2.DESCRIPTION)
            .from(t1).leftJoin(t2).on(t1.LABEL_ID.eq(t2.ID))
            .where(t1.NODE_ID.eq(nodeId))
            .fetch()
    }

    /**
     * 获取标签关联的节点
     */
    fun getLabelNodes(dslContext: DSLContext, labelId: Long): List<TNodeLabelRecord>? {
        with(TNodeLabel.T_NODE_LABEL) {
            return dslContext.selectFrom(this)
                .where(LABEL_ID.eq(labelId))
                .fetch()
        }
    }

    fun addNodeLabel(
        projectId: String,
        nodeId: Long,
        labelId: Long,
        dslContext: DSLContext
    ): Long {
        with(TNodeLabel.T_NODE_LABEL) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                NODE_ID,
                LABEL_ID,
                GMT_CREATE,
                GMT_MODIFIED
            )
                .values(
                    projectId,
                    nodeId,
                    labelId,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun deleteNodeLabel(
        dslContext: DSLContext,
        labelId: Long,
        nodeId: Long
    ) {
        with(TNodeLabel.T_NODE_LABEL) {
            dslContext.deleteFrom(this)
                .where(NODE_ID.eq(nodeId))
                .and(LABEL_ID.eq(labelId))
                .execute()
        }
    }

    fun deleteLabel(
        dslContext: DSLContext,
        labelId: Long
    ) {
        with(TNodeLabel.T_NODE_LABEL) {
            dslContext.deleteFrom(this)
                .where(LABEL_ID.eq(labelId))
                .execute()
        }
    }
}
