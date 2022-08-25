package com.tencent.devops.openapi.service.apigw

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.constant.OpenAPIMessageCode
import com.tencent.devops.openapi.service.op.OpAppUserService
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("opAppUserService")
class TxOpUserService @Autowired constructor(
    val client: Client
) : OpAppUserService {

    override fun checkUser(userId: String): Boolean {
        return try {
            val userResult =
                client.get(ServiceTxUserResource::class).get(userId)
            if (userResult.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = OpenAPIMessageCode.USER_CHECK_FAIL,
                    defaultMessage = "checkUser fail",
                    params = arrayOf(userId)
                )
            }
            true
        } catch (e: Exception) {
            logger.warn("checkUser failed : $e")
            false
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxOpUserService::class.java)
    }
}
