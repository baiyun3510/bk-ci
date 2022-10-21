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

package com.tencent.devops.auth

import IamBkActionServiceImpl
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.IamActionService
import com.tencent.bk.sdk.iam.service.IamResourceService
import com.tencent.bk.sdk.iam.service.SystemService
import com.tencent.bk.sdk.iam.service.impl.ActionServiceImpl
import com.tencent.bk.sdk.iam.service.impl.ApigwHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.ResourceServiceImpl
import com.tencent.bk.sdk.iam.service.impl.SystemServiceImpl
import com.tencent.devops.auth.dao.ActionDao
import com.tencent.devops.auth.dao.ResourceDao
import com.tencent.devops.auth.service.iam.BkResourceService
import com.tencent.devops.auth.service.iam.impl.IamBkResourceServiceImpl
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
//@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
class IamV3AuthConfiguration {

    @Bean
    fun iamSystemService(
        apigwHttpClientServiceImpl: ApigwHttpClientServiceImpl,
        iamConfiguration: IamConfiguration
    ) = SystemServiceImpl(apigwHttpClientServiceImpl, iamConfiguration)

    @Bean
    fun iamActionService(
        iamConfiguration: IamConfiguration,
        apigwHttpClientServiceImpl: ApigwHttpClientServiceImpl,
        systemService: SystemService
    ) = ActionServiceImpl(iamConfiguration, apigwHttpClientServiceImpl, systemService)

    @Bean
    fun iamResourceService(
        iamConfiguration: IamConfiguration,
        apigwHttpClientServiceImpl: ApigwHttpClientServiceImpl,
        systemService: SystemService
    ) = ResourceServiceImpl(iamConfiguration, apigwHttpClientServiceImpl, systemService)

    @Bean
    fun ciIamActionService(
        dslContext: DSLContext,
        actionDao: ActionDao,
        resourceService: BkResourceService,
        iamConfiguration: IamConfiguration,
        systemService: SystemService,
        iamActionService: IamActionService,
        iamResourceService: IamResourceService
    ) = IamBkActionServiceImpl(
        dslContext = dslContext,
        actionDao = actionDao,
        resourceService = resourceService,
        iamConfiguration = iamConfiguration,
        systemService = systemService,
        iamActionService = iamActionService,
        iamResourceService = iamResourceService
    )

    @Bean
    fun ciIamResourceService(
        dslContext: DSLContext,
        resourceDao: ResourceDao,
        iamConfiguration: IamConfiguration,
        iamResourceService: IamResourceService,
        iamSystemService: SystemService
    ) = IamBkResourceServiceImpl(
        dslContext = dslContext,
        resourceDao = resourceDao,
        iamConfiguration = iamConfiguration,
        iamResourceService = iamResourceService,
        iamSystemService = iamSystemService
    )
}
