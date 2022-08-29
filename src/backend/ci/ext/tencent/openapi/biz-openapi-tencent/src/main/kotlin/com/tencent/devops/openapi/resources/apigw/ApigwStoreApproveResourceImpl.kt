package com.tencent.devops.openapi.resources.apigw

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.ApigwStoreApproveResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwStoreApproveResourceImpl @Autowired constructor(
    private val client: Client
) : ApigwStoreApproveResource {
    override fun moaApproveCallBack(
        verifier: String,
        result: Int,
        taskId: String,
        message: String,
        token: String
    ): Result<Boolean> {
        return client.get()
    }
}