package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TIdeAtom
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.store.dao.TxOpMigrateStoreDescriptionDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors
import java.util.regex.Matcher
import java.util.regex.Pattern

class TxOpMigrateStoreDescriptionServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val txOpMigrateStoreDescriptionDao: TxOpMigrateStoreDescriptionDao,
    client: Client
) : TxOpMigrateStoreFileServiceImpl(client) {

    companion object {
        private val logger = LoggerFactory.getLogger(TxOpMigrateStoreDescriptionServiceImpl::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
        private const val BK_CI_PATH_REGEX = "!\\[(.*)]\\((http://radosgw.open.oa.com(.*))\\)"
    }

    override fun migrateStoreResources(): Boolean {
        // 迁移插件描述信息引用资源
        migrateAtomDescription()
        // 迁移模板描述信息引用资源
        migrateTemplateDescription()
        // 迁移IDE插件描述信息引用资源
        migrateIdeAtomDescription()
        // 迁移镜像描述信息引用资源
        migrateImageDescription()
        // 迁移微扩展描述信息引用资源
        migrateExtServiceDescription()
        return true
    }

    private fun migrateAtomDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateAtomDescription!!")
            var offset = 0
            do {
                // 查询插件描述信息记录
                val atomDescriptionRecords = txOpMigrateStoreDescriptionDao.getAtomDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tAtom = TAtom.T_ATOM
                atomDescriptionRecords?.forEach { atomDescriptionRecord ->
                    val description = atomDescriptionRecord[tAtom.DESCRIPTION]
                    val userId = atomDescriptionRecord[tAtom.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it] = bkRepoFileUrl
                    }
                    if (pathMap.isNotEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新插件的描述
                    val id = atomDescriptionRecord[tAtom.ID]
                    txOpMigrateStoreDescriptionDao.updateAtomDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (atomDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateAtomDescription!!")
        }
    }

    private fun migrateTemplateDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateTemplateDescription!!")
            var offset = 0
            do {
                // 查询模板描述信息记录
                val templateDescriptionRecords = txOpMigrateStoreDescriptionDao.getTemplateDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tTemplate = TTemplate.T_TEMPLATE
                templateDescriptionRecords?.forEach { atomDescriptionRecord ->
                    val description = atomDescriptionRecord[tTemplate.DESCRIPTION]
                    val userId = atomDescriptionRecord[tTemplate.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it] = bkRepoFileUrl
                    }
                    if (pathMap.isNotEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新模板的描述
                    val id = atomDescriptionRecord[tTemplate.ID]
                    txOpMigrateStoreDescriptionDao.updateTemplateDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (templateDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateTemplateDescription!!")
        }
    }

    private fun migrateIdeAtomDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateIdeAtomDescription!!")
            var offset = 0
            do {
                // 查询IDE插件描述信息记录
                val atomDescriptionRecords = txOpMigrateStoreDescriptionDao.getIdeAtomDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tAtom = TIdeAtom.T_IDE_ATOM
                atomDescriptionRecords?.forEach { atomDescriptionRecord ->
                    val description = atomDescriptionRecord[tAtom.DESCRIPTION]
                    val userId = atomDescriptionRecord[tAtom.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it] = bkRepoFileUrl
                    }
                    if (pathMap.isNotEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新插件的描述
                    val id = atomDescriptionRecord[tAtom.ID]
                    txOpMigrateStoreDescriptionDao.updateIdeAtomDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (atomDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateIdeAtomDescription!!")
        }
    }

    private fun migrateImageDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateImageDescription!!")
            var offset = 0
            do {
                // 查询镜像描述信息记录
                val imageDescriptionRecords = txOpMigrateStoreDescriptionDao.getImageDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tImage = TImage.T_IMAGE
                imageDescriptionRecords?.forEach { imageDescriptionRecord ->
                    val description = imageDescriptionRecord[tImage.DESCRIPTION]
                    val userId = imageDescriptionRecord[tImage.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it] = bkRepoFileUrl
                    }
                    if (pathMap.isNotEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新镜像的描述
                    val id = imageDescriptionRecord[tImage.ID]
                    txOpMigrateStoreDescriptionDao.updateImageDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (imageDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateImageDescription!!")
        }
    }

    private fun migrateExtServiceDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateExtServiceDescription!!")
            var offset = 0
            do {
                // 查询微扩展描述信息记录
                val imageDescriptionRecords = txOpMigrateStoreDescriptionDao.getExtServiceDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
                imageDescriptionRecords?.forEach { imageDescriptionRecord ->
                    val description = imageDescriptionRecord[tExtensionService.DESCRIPTION]
                    val userId = imageDescriptionRecord[tExtensionService.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it] = bkRepoFileUrl
                    }
                    if (pathMap.isNotEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新微扩展的描述
                    val id = imageDescriptionRecord[tExtensionService.ID]
                    txOpMigrateStoreDescriptionDao.updateExtServiceDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (imageDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateExtServDescription!!")
        }
    }

    private fun checkLogoUrlCondition(description: String?): List<String>? {
        if (description.isNullOrBlank()) {
            return null
        }
        val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
        val matcher: Matcher = pattern.matcher(description)
        val pathList = mutableListOf<String>()
        while (matcher.find()) {
            pathList.add(matcher.group(3))
        }
        return pathList
    }

    private fun replaceDescription(description: String, pathMap: Map<String, String>): String {
        var newDescription = ""
        pathMap.forEach {
            val bkCiPathRegex = "(!\\[.*]\\()(${it.key})(\\))"
            val pattern: Pattern = Pattern.compile(bkCiPathRegex)
            val matcher: Matcher = pattern.matcher(description)
            newDescription = matcher.replaceAll("$1${it.value}$3")
        }
        return newDescription
    }
}