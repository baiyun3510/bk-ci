package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.RemoteDevFileDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevSettingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val remoteDevFileDao: RemoteDevFileDao,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevSettingService::class.java)
    }

    fun getRemoteDevSettings(userId: String): RemoteDevSettings {
        logger.info("$userId get remote dev setting")
        val setting = remoteDevSettingDao.fetchAnySetting(dslContext, userId) ?: return RemoteDevSettings()

        return setting.copy(envsForFile = remoteDevFileDao.fetchFile(dslContext, userId))
    }

    fun updateRemoteDevSettings(userId: String, setting: RemoteDevSettings): Boolean {
        logger.info("$userId get remote dev setting")
        remoteDevSettingDao.createOrUpdateSetting(dslContext, setting, userId)
        // 删除用户已去掉的文件
        remoteDevFileDao.batchDeleteFile(dslContext, setting.envsForFile.map { it.id ?: -1 }.toSet(), userId)
        // 添加or更新存在的文件
        setting.envsForFile.forEach {
            val computeMd5 = DigestUtils.md5Hex(it.content)
            when {
                it.id == null -> remoteDevFileDao.createFile(
                    dslContext = dslContext,
                    path = it.path,
                    content = it.content,
                    userId = userId,
                    md5 = computeMd5
                )
                it.md5 != computeMd5 -> remoteDevFileDao.updateFile(
                    dslContext = dslContext, file = it, md5 = computeMd5, userId = userId
                )
            }
        }
        return true
    }
}
