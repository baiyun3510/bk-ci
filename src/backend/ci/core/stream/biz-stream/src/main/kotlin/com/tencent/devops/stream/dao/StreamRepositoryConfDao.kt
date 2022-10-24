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

package com.tencent.devops.stream.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.stream.tables.TGitBasicSetting
import com.tencent.devops.model.stream.tables.TRepositoryConf
import com.tencent.devops.model.stream.tables.records.TGitBasicSettingRecord
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.StreamCIInfo
import com.tencent.devops.stream.pojo.TriggerReviewSetting
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StreamRepositoryConfDao {

    fun getRepoByGitDomain(
        dslContext: DSLContext,
        gitDomain: String,
        limit: Int
    ): Result<Record1<Long>> {
        with(TRepositoryConf.T_REPOSITORY_CONF) {
            return dslContext.select(ID).from(this)
                .where(HOME_PAGE.like("%$gitDomain%"))
                .limit(limit)
                .fetch()
        }
    }

    fun updateGitDomainByIds(
        dslContext: DSLContext,
        oldGitDomain: String,
        newGitDomain: String,
        idList: List<Long>
    ): Int {
        with(TRepositoryConf.T_REPOSITORY_CONF) {
            return dslContext.update(this)
                .set(URL, URL.replace(oldGitDomain, newGitDomain))
                .set(HOME_PAGE, HOME_PAGE.replace(oldGitDomain, newGitDomain))
                .set(GIT_HTTP_URL, GIT_HTTP_URL.replace(oldGitDomain, newGitDomain))
                .set(GIT_SSH_URL, GIT_SSH_URL.replace(oldGitDomain, newGitDomain))
                .where(ID.`in`(idList)).execute()
        }
    }
}
