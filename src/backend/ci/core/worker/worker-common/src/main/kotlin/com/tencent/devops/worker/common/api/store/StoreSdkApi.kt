package com.tencent.devops.worker.common.api.store

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.WorkerRestApiSDK

interface StoreSdkApi : WorkerRestApiSDK {

    fun isPlatformCodeRegistered(platformCode: String): Result<Boolean>
}
