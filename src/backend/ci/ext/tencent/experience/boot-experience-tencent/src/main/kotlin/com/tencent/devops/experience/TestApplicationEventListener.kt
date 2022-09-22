package com.tencent.devops.experience

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class TestApplicationEventListener : ApplicationListener<ApplicationPreparedEvent> {
    override fun onApplicationEvent(event: ApplicationPreparedEvent) {
        val applicationContext = event.applicationContext
        logger.info("######## env : ${applicationContext.environment.systemEnvironment}")
        logger.info("######## property : ${applicationContext.environment.systemProperties}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TestApplicationEventListener::class.java)
    }
}
