package com.tencent.devops.experience.resources.open

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.open.OpenExperienceResource
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import com.tencent.devops.experience.service.ExperienceAppService
import com.tencent.devops.experience.service.ExperienceOuterService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@RestResource
class OpenExperienceResourceImpl @Autowired constructor(
    private val experienceOuterService: ExperienceOuterService,
    private val experienceAppService: ExperienceAppService,
    private val redisOperation: RedisOperation
) : OpenExperienceResource {
    private val fixThreadPool = Executors.newFixedThreadPool(3)
    private val threadPoolExecutor = ThreadPoolExecutor(
        8, 8, 60, TimeUnit.SECONDS, LinkedBlockingQueue(50)
    )

    override fun outerLogin(
        platform: Int,
        appVersion: String?,
        realIp: String,
        params: OuterLoginParam
    ): Result<String> {
        return Result(experienceOuterService.outerLogin(platform, appVersion, realIp, params))
    }

    override fun outerAuth(token: String): Result<OuterProfileVO> {
        return Result(experienceOuterService.outerAuth(token))
    }

    override fun appStoreRedirect(id: String, userId: String): Response {
        return experienceAppService.appStoreRedirect(id, userId)
    }

    override fun testOt(): Result<String> {
        logger.info("###### test ot")
        fixThreadPool.submit { redisOperation.get("test1") }
        threadPoolExecutor.submit { redisOperation.get("test2") }
        redisOperation.get("test2")
        return Result("true")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenExperienceResourceImpl::class.java)
    }
}
