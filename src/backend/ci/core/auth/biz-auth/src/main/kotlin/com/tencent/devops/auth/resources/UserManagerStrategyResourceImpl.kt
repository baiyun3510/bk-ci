package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.UserManagerStrategyResource
import com.tencent.devops.auth.pojo.StrategyEntity
import com.tencent.devops.auth.pojo.dto.ManageStrategyDTO
import com.tencent.devops.auth.service.StrategyService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

@RestResource
class UserManagerStrategyResourceImpl @Autowired constructor(
    val strategyService: StrategyService
): UserManagerStrategyResource {

    override fun createMangerStrategy(userId: String, name: String, strategy: ManageStrategyDTO): Result<Int> {
        return Result(strategyService.createStrategy(
            userId = userId,
            strategy = strategy,
            name = name
        ))
    }

    override fun updateMangerStrategy(strategyId: Int, userId: String, strategy: ManageStrategyDTO): Result<Boolean> {
        return Result(strategyService.updateStrategy(
            userId = userId,
            strategy = strategy,
            strategyId = strategyId
        ))
    }

    override fun getMangerStrategy(strategyId: Int): Result<StrategyEntity?> {
        return Result(strategyService.getStrategy(strategyId))
    }

    override fun listMangerStrategy(): Result<List<StrategyEntity>?> {
        return Result(strategyService.listStrategy())
    }
}
