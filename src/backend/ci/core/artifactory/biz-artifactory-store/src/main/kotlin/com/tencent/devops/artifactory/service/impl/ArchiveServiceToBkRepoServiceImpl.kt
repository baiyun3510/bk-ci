package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.constant.BK_CI_SERVICE_DIR
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.artifactory.util.BkRepoUtils.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.util.BkRepoUtils.BKREPO_STORE_PROJECT_ID
import com.tencent.devops.artifactory.util.BkRepoUtils.REPO_NAME_PLUGIN
import com.tencent.devops.artifactory.util.BkRepoUtils.REPO_NAME_SERVICE
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.archive.client.BkRepoClient
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import javax.ws.rs.NotFoundException

@Service
@ConditionalOnProperty(prefix = "artifactory", name = ["realm"], havingValue = REALM_BK_REPO)
class ArchiveServiceToBkRepoServiceImpl(
    private val bkRepoClient: BkRepoClient
) : ArchiveServicePkgServiceImpl() {

    override fun getServiceArchiveBasePath(): String {
        return System.getProperty("java.io.tmpdir")
    }

    override fun handleArchiveFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        serviceCode: String,
        version: String
    ) {
        unzipFile(
            disposition = disposition,
            inputStream = inputStream,
            serviceCode = serviceCode,
            version = version
        )
        val serviceArchivePath = buildServiceArchivePath(serviceCode, version)
        File(serviceArchivePath).walk().filter { it.path != serviceArchivePath }.forEach {
            val path = it.path.removePrefix("${getServiceArchiveBasePath()}/$BK_CI_SERVICE_DIR")
            bkRepoClient.uploadLocalFile(
                userId = BKREPO_DEFAULT_USER,
                projectId = BKREPO_STORE_PROJECT_ID,
                repoName = REPO_NAME_SERVICE,
                path = path,
                file = it
            )
        }
    }

    override fun getServiceFileContent(filePath: String): String {
        val tmpFile = DefaultPathUtils.randomFile()
        return try {
            bkRepoClient.downloadFile(
                userId = BKREPO_DEFAULT_USER,
                projectId = BKREPO_STORE_PROJECT_ID,
                repoName = REPO_NAME_SERVICE,
                fullPath = filePath,
                destFile = tmpFile
            )
            tmpFile.readText(Charsets.UTF_8)
        } catch (ignored: NotFoundException) {
            logger.warn("file[$filePath] not exists")
            ""
        } catch (ignored: RemoteServiceException) {
            logger.warn("download file[$filePath] error: $ignored")
            ""
        } finally {
            tmpFile.delete()
        }
    }

    override fun deleteServicePkg(userId: String, serviceCode: String) {
        bkRepoClient.delete(userId, BKREPO_STORE_PROJECT_ID, REPO_NAME_PLUGIN, serviceCode)
    }

    override fun clearServerTmpFile(serviceCode: String, version: String) {
        val serviceArchivePath = buildServiceArchivePath(serviceCode, version)
        File(serviceArchivePath).deleteRecursively()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveServiceToBkRepoServiceImpl::class.java)
    }
}
