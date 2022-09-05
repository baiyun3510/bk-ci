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

package com.tencent.devops.quality.service

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.quality.dao.v2.QualityControlPointDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
class OPQualityService @Autowired constructor(
    private val dslContext: DSLContext,
    private val controlPointDao: QualityControlPointDao,
    private val qualityRuleDao: QualityRuleDao,
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao
) {
    fun addHashId() {
        val threadPoolExecutor = ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS, LinkedBlockingQueue(50))
        threadPoolExecutor.submit {
            var offset = 0
            val limit = 100
            try {
                do {
                    val controlPointRecords = controlPointDao.getAllControlPoint(dslContext, limit, offset)
                    val controlPointSize = controlPointRecords?.size
                    controlPointRecords?.map {
                        val id = it.value1()
                        val hashId = HashUtil.encodeLongId(it.value1())
                        controlPointDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (controlPointSize == 100)
                offset = 0
                do {
                    val ruleRecords = qualityRuleDao.getAllRule(dslContext, limit, offset)
                    val ruleSize = ruleRecords?.size
                    ruleRecords?.map {
                        val id = it.value1()
                        val hashId = HashUtil.encodeLongId(it.value1())
                        qualityRuleDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (ruleSize == 100)
                offset = 0
                do {
                    val ruleBuildHisRecords = qualityRuleBuildHisDao.getAllRuleBuildHis(dslContext, limit, offset)
                    val ruleBuildHisSize = ruleBuildHisRecords?.size
                    ruleBuildHisRecords?.map {
                        val id = it.value1()
                        val hashId = HashUtil.encodeLongId(it.value1())
                        qualityRuleBuildHisDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (ruleBuildHisSize == 100)
            } catch (e: Exception) {
                logger.warn("OPQualityService：addHashId failed | $e ")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OPQualityService::class.java)
    }
}
