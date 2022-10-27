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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TExtensionServiceEnvInfo
import com.tencent.devops.project.api.service.ServiceItemResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectConstant
import com.tencent.devops.project.pojo.ServiceItem
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtItemServiceDao
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceEnvDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.ExtServiceItemRelDao
import com.tencent.devops.store.dao.ExtServiceLabelRelDao
import com.tencent.devops.store.dao.ExtServiceVersionLogDao
import com.tencent.devops.store.dao.common.StoreMediaInfoDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.EditInfoDTO
import com.tencent.devops.store.pojo.ExtServiceFeatureUpdateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.ExtensionJson
import com.tencent.devops.store.pojo.ItemPropCreateInfo
import com.tencent.devops.store.pojo.common.EXTENSION_JSON_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_CODE
import com.tencent.devops.store.pojo.common.KEY_LABEL_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_TYPE
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.README
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.DescInputTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.vo.ExtServiceRespItem
import com.tencent.devops.store.pojo.vo.ServiceVersionListItem
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreMediaService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
abstract class ExtServiceManageService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var redisOperation: RedisOperation

    @Autowired
    lateinit var extServiceDao: ExtServiceDao

    @Autowired
    lateinit var extServiceFeatureDao: ExtServiceFeatureDao

    @Autowired
    lateinit var extServiceEnvDao: ExtServiceEnvDao

    @Autowired
    lateinit var extServiceVersionLogDao: ExtServiceVersionLogDao

    @Autowired
    lateinit var extItemServiceDao: ExtItemServiceDao

    @Autowired
    lateinit var extServiceItemRelDao: ExtServiceItemRelDao

    @Autowired
    lateinit var extServiceLabelRelDao: ExtServiceLabelRelDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao

    @Autowired
    lateinit var storeMediaInfoDao: StoreMediaInfoDao

    @Autowired
    lateinit var storeUserService: StoreUserService

    @Autowired
    lateinit var storeMediaService: StoreMediaService

    @Autowired
    lateinit var storeCommonService: StoreCommonService

    @Autowired
    lateinit var storeCommentService: StoreCommentService

    @Autowired
    lateinit var extServiceKubernetesService: ExtServiceKubernetesService

    @Autowired
    lateinit var extServiceCommonService: ExtServiceCommonService

    companion object {
        private val logger = LoggerFactory.getLogger(ExtServiceManageService::class.java)
    }

    fun getMyService(
        userId: String,
        serviceName: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<ExtServiceRespItem>> {
        logger.info("getMyService params:[$userId|$serviceName|$page|$pageSize]")
        // 获取有权限的微扩展列表
        val records = extServiceDao.getMyService(dslContext, userId, serviceName, page, pageSize)
        val count = extServiceDao.countByUser(dslContext, userId, serviceName)
        // 获取项目ID对应的名称
        val projectCodeList = mutableListOf<String>()
        val serviceItemIdMap = mutableMapOf<String, Set<String>>()
        val itemIdList = mutableSetOf<String>()
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
        records?.forEach {
            val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = userId,
                storeCode = it[tExtensionService.SERVICE_CODE] as String,
                storeType = StoreTypeEnum.SERVICE
            )
            if (null != testProjectCode) projectCodeList.add(testProjectCode)
            val serviceId = it[tExtensionService.ID] as String
            val serviceItemRecords = extServiceItemRelDao.getItemByServiceId(dslContext, serviceId)
            val itemIds = mutableSetOf<String>()
            serviceItemRecords?.forEach { itemInfo ->
                itemIds.add(itemInfo.itemId)
                itemIdList.add(itemInfo.itemId)
            }
            serviceItemIdMap[serviceId] = itemIds
        }
        val projectMap = client.get(ServiceProjectResource::class)
            .getNameByCode(projectCodeList.joinToString(",")).data

        val itemRecordList = client.get(ServiceItemResource::class).getItemInfoByIds(itemIdList).data
        val itemInfoMap = mutableMapOf<String, ServiceItem>()
        itemRecordList?.forEach {
            itemInfoMap[it.itemId] = it
        }

        val myService = mutableListOf<ExtServiceRespItem>()
        val tExtensionServiceEnvInfo = TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO
        records?.forEach {
            val serviceCode = it[tExtensionService.SERVICE_CODE] as String
            val serviceId = it[tExtensionService.ID] as String
            var releaseFlag = false // 是否有处于上架状态的微扩展版本
            val releaseServiceNum = extServiceDao.countReleaseServiceByCode(dslContext, serviceCode)
            if (releaseServiceNum > 0) {
                releaseFlag = true
            }
            val serviceItemList = serviceItemIdMap[serviceId]
            val itemNameList = mutableListOf<String>()
            serviceItemList?.forEach { itId ->
                val itemInfo = itemInfoMap[itId]
                if (itemInfo != null) {
                    itemNameList.add("${itemInfo.parentName}-${itemInfo.itemName}")
                }
            }
            myService.add(
                ExtServiceRespItem(
                    serviceId = serviceId,
                    serviceName = it[tExtensionService.SERVICE_NAME],
                    serviceCode = serviceCode,
                    version = it[tExtensionService.VERSION],
                    logoUrl = it[tExtensionService.LOGO_URL],
                    serviceStatus = ExtServiceStatusEnum.getServiceStatus(it[tExtensionService.SERVICE_STATUS].toInt()),
                    publisher = it[tExtensionService.PUBLISHER],
                    publishTime = DateTimeUtil.toDateTime(it[tExtensionService.PUB_TIME]),
                    creator = it[tExtensionService.CREATOR],
                    createTime = DateTimeUtil.toDateTime(it[tExtensionService.CREATE_TIME]),
                    modifier = it[tExtensionService.MODIFIER],
                    updateTime = DateTimeUtil.toDateTime(it[tExtensionService.UPDATE_TIME]),
                    projectName = projectMap?.get(
                        storeProjectRelDao.getUserStoreTestProjectCode(
                            dslContext = dslContext,
                            userId = userId,
                            storeCode = serviceCode,
                            storeType = StoreTypeEnum.SERVICE
                        )
                    ) ?: "",
                    language = it[tExtensionServiceEnvInfo.LANGUAGE],
                    itemName = itemNameList,
                    itemIds = serviceItemList ?: emptySet(),
                    releaseFlag = releaseFlag
                )
            )
        }
        return Result(Page(page, pageSize, count, myService))
    }

    fun deleteExtensionService(userId: String, serviceCode: String): Result<Boolean> {
        logger.info("deleteService userId: $userId , serviceCode: $serviceCode")
        val result = extServiceDao.getExtServiceIds(dslContext, serviceCode)
        var extServiceId: String? = null
        val extServiceIdList = result.map {
            val serviceId = it.value1()
            val latestFlag = it.value2()
            if (latestFlag) {
                extServiceId = serviceId
            }
            serviceId
        }
        if (extServiceId.isNullOrBlank()) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_SERVICE_NOT_EXIST,
                arrayOf(serviceCode)
            )
        }
        val type = StoreTypeEnum.SERVICE.type.toByte()
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, serviceCode, type)) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val releasedCount = extServiceDao.countReleaseServiceByCode(dslContext, serviceCode)
        logger.info("releasedCount: $releasedCount")
        if (releasedCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_SERVICE_RELEASED_IS_NOT_ALLOW_DELETE,
                arrayOf(serviceCode)
            )
        }
        // 如果已经被安装到其他项目下使用，不能删除
        val installedCount = storeProjectRelDao.countInstalledProject(dslContext, serviceCode, type)
        logger.info("installedCount: $releasedCount")
        if (installedCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_SERVICE_USED_IS_NOT_ALLOW_DELETE,
                arrayOf(serviceCode)
            )
        }
        val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = dslContext,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE.type.toByte()
        )
        // 停止微扩展灰度命名空间和正式命名空间的应用
        val serviceStopAppResult = extServiceKubernetesService.stopExtService(
            userId = userId,
            serviceCode = serviceCode,
            deploymentName = serviceCode,
            serviceName = "$serviceCode-service"
        )
        logger.info("serviceStopAppResult is :$serviceStopAppResult")
        doServiceDeleteBus(
            userId = userId,
            serviceId = extServiceId!!,
            serviceCode = serviceCode,
            initProjectCode = initProjectCode!!
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeCommonService.deleteStoreInfo(context, serviceCode, StoreTypeEnum.SERVICE.type.toByte())
            extServiceEnvDao.deleteEnvInfo(context, extServiceIdList)
            extServiceFeatureDao.deleteExtFeatureServiceData(context, serviceCode)
            extServiceVersionLogDao.deleteByServiceId(context, extServiceIdList)
            extItemServiceDao.deleteByServiceId(context, extServiceIdList)
            extServiceDao.deleteExtServiceData(context, serviceCode)
        }
        return Result(true)
    }

    abstract fun doServiceDeleteBus(
        userId: String,
        serviceId: String,
        serviceCode: String,
        initProjectCode: String
    )

    fun getServiceById(serviceId: String, userId: String): Result<ServiceVersionVO?> {
        return getServiceVersion(serviceId, userId)
    }

    fun getServiceByCode(userId: String, serviceCode: String): Result<ServiceVersionVO?> {
        val record = extServiceDao.getServiceLatestByCode(dslContext, serviceCode)
        return (if (null == record) {
            Result(data = null)
        } else {
            getServiceVersion(record.id, userId)
        })
    }


    @Suppress("UNCHECKED_CAST")
    private fun getServiceVersion(serviceId: String, userId: String): Result<ServiceVersionVO?> {
        logger.info("getServiceVersion serviceId[$serviceId], userId[$userId]")
        val record = extServiceDao.getServiceById(dslContext, serviceId)
        return if (null == record) {
            Result(data = null)
        } else {
            val serviceCode = record.serviceCode
            val defaultFlag = record.deleteFlag
            val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext = dslContext,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            logger.info("getServiceVersion projectCode: $projectCode")
            val featureInfoRecord = extServiceFeatureDao.getLatestServiceByCode(dslContext, serviceCode)

            val flag =
                storeUserService.isCanInstallStoreComponent(defaultFlag, userId, serviceCode, StoreTypeEnum.SERVICE)
            val userCommentInfo =
                storeCommentService.getStoreUserCommentInfo(userId, serviceCode, StoreTypeEnum.SERVICE)
            val serviceEnv = extServiceEnvDao.getMarketServiceEnvInfoByServiceId(dslContext, serviceId)
            val itemList = getItemsByServiceId(serviceId)
            val mediaList = this.storeMediaService.getByCode(serviceCode, StoreTypeEnum.SERVICE).data
            val labelRecords = extServiceLabelRelDao.getLabelsByServiceId(dslContext, serviceId)
            val labelList = mutableListOf<Label>()
            labelRecords?.forEach {
                labelList.add(
                    Label(
                        id = it[KEY_LABEL_ID] as String,
                        labelCode = it[KEY_LABEL_CODE] as String,
                        labelName = it[KEY_LABEL_NAME] as String,
                        labelType = StoreTypeEnum.getStoreType((it[KEY_LABEL_TYPE] as Byte).toInt())
                    )
                )
            }
            val extensionName = getAllItemName(itemList.toSet())
            val serviceVersion = extServiceVersionLogDao.getVersionLogByServiceId(dslContext, serviceId)

            Result(
                ServiceVersionVO(
                    serviceId = serviceId,
                    serviceCode = serviceCode,
                    serviceName = record.serviceName,
                    logoUrl = record.logoUrl,
                    summary = record.summary ?: "",
                    description = record.description ?: "",
                    version = record.version,
                    serviceStatus = ExtServiceStatusEnum.getServiceStatus((record.serviceStatus).toInt()),
                    language = serviceEnv!!.language,
                    codeSrc = featureInfoRecord!!.codeSrc ?: "",
                    publisher = record.publisher,
                    modifier = record.modifier,
                    creator = record.creator,
                    createTime = DateTimeUtil.toDateTime(record.createTime as LocalDateTime),
                    updateTime = DateTimeUtil.toDateTime(record.updateTime as LocalDateTime),
                    flag = flag,
                    repositoryAuthorizer = record.publisher,
                    defaultFlag = defaultFlag,
                    projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                        dslContext = dslContext,
                        userId = userId,
                        storeCode = serviceCode,
                        storeType = StoreTypeEnum.SERVICE
                    ),
                    labelList = labelList,
                    labelIdList = labelList.map { it.id },
                    userCommentInfo = userCommentInfo,
                    visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(featureInfoRecord.visibilityLevel),
                    recommendFlag = featureInfoRecord.recommendFlag,
                    publicFlag = featureInfoRecord.publicFlag,
                    certificationFlag = featureInfoRecord.certificationFlag,
                    descInputType = featureInfoRecord.descInputType,
                    weight = featureInfoRecord.weight,
                    serviceType = featureInfoRecord.serviceType.toInt(),
                    extensionItemList = itemList,
                    mediaList = mediaList,
                    itemName = extensionName,
                    bkServiceId = getBkServicesByServiceId(serviceId),
                    versionContent = serviceVersion?.content ?: "",
                    releaseType = ReleaseTypeEnum.getReleaseType(
                        serviceVersion?.releaseType?.toInt() ?: ReleaseTypeEnum.NEW.releaseType
                    ),
                    editFlag = extServiceCommonService.checkEditCondition(serviceCode)
                )
            )
        }
    }

    private fun getItemsByServiceId(serviceId: String): List<String> {
        val serviceItems = extServiceItemRelDao.getItemByServiceId(dslContext, serviceId)
        val itemIds = mutableListOf<String>()
        serviceItems?.forEach { it ->
            itemIds.add(it.itemId)
        }
        return itemIds
    }

    private fun getBkServicesByServiceId(serviceId: String): Set<Long> {
        val serviceItems = extServiceItemRelDao.getItemByServiceId(dslContext, serviceId)
        val bkServiceIds = mutableSetOf<Long>()
        serviceItems?.forEach { it ->
            bkServiceIds.add(it.bkServiceId)
        }
        return bkServiceIds
    }

    private fun getAllItemName(itemList: Set<String>): String {
        val itemRecords = client.get(ServiceItemResource::class).getItemInfoByIds(itemList).data
        var itemNames = ""
        itemRecords?.forEach {
            itemNames = itemNames + it.parentName + "-" + it.itemName + ","
        }
        itemNames = itemNames.substringBeforeLast(",")
        return itemNames
    }

    fun getServiceVersionListByCode(
        userId: String,
        serviceCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<ServiceVersionListItem>> {
        logger.info("getServiceVersionListByCode params[$userId|$serviceCode|$page|$pageSize]")
        val totalCount = extServiceDao.countByCode(dslContext, serviceCode)
        val records = extServiceDao.listServiceByCode(dslContext, serviceCode)
        val atomVersions = mutableListOf<ServiceVersionListItem>()
        if (records != null) {
            val serviceIds = records.map { it.id }
            // 批量获取版本内容
            val versionRecords = extServiceVersionLogDao.getVersionLogsByServiceIds(
                dslContext = dslContext,
                serviceIds = serviceIds
            )
            val versionMap = mutableMapOf<String, String>()
            versionRecords?.forEach { versionRecord ->
                versionMap[versionRecord.serviceId] = versionRecord.content
            }
            records.forEach {
                atomVersions.add(
                    ServiceVersionListItem(
                        serviceId = it.id,
                        serviceCode = it.serviceCode,
                        serviceName = it.serviceName,
                        version = it.version,
                        versionContent = versionMap[it.id].toString(),
                        serviceStatus = ExtServiceStatusEnum.getServiceStatus((it.serviceStatus as Byte).toInt()),
                        creator = it.creator,
                        createTime = DateTimeUtil.toDateTime(it.createTime)
                    )
                )
            }
        }
        return Result(Page(page, pageSize, totalCount.toLong(), atomVersions))
    }

    fun updateExtInfo(
        userId: String,
        serviceId: String,
        serviceCode: String,
        infoResp: EditInfoDTO,
        checkPermissionFlag: Boolean = true
    ): Result<Boolean> {
        logger.info("updateExtInfo params[$userId|$serviceId|$serviceCode|$infoResp|$checkPermissionFlag]")
        // 判断当前用户是否是该扩展的成员
        if (checkPermissionFlag && !storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        // 查询扩展的最新记录
        val newestServiceRecord = extServiceDao.getNewestServiceByCode(dslContext, serviceCode)
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(serviceCode)
            )
        val editFlag = extServiceCommonService.checkEditCondition(serviceCode)
        val version = newestServiceRecord.version
        if (!editFlag) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_VERSION_IS_NOT_FINISH,
                params = arrayOf(newestServiceRecord.serviceName, version)
            )
        }
        val baseInfo = infoResp.baseInfo
        val settingInfo = infoResp.settingInfo
        if (baseInfo != null) {
            extServiceDao.updateExtServiceBaseInfo(
                dslContext = dslContext,
                userId = userId,
                serviceId = serviceId,
                extServiceUpdateInfo = ExtServiceUpdateInfo(
                    serviceName = baseInfo.serviceName,
                    logoUrl = baseInfo.logoUrl,
                    iconData = baseInfo.iconData,
                    summary = baseInfo.summary,
                    description = baseInfo.description,
                    modifierUser = userId,
                    status = null,
                    latestFlag = null
                )
            )

            // 更新标签信息
            val labelIdList = baseInfo.labels
            if (null != labelIdList) {
                extServiceLabelRelDao.deleteByServiceId(dslContext, serviceId)
                if (labelIdList.isNotEmpty())
                    extServiceLabelRelDao.batchAdd(dslContext, userId, serviceId, labelIdList)
            }
            val itemIds = baseInfo.itemIds
            if (itemIds != null) {
                addServiceItem(
                    userId = userId,
                    serviceId = serviceId,
                    serviceCode = serviceCode,
                    version = version,
                    itemIds = itemIds
                )
            }
        }

        this.storeMediaService.deleteByStoreCode(userId, serviceCode, StoreTypeEnum.SERVICE)
        infoResp.mediaInfo?.forEach {
            storeMediaInfoDao.add(
                dslContext = dslContext,
                userId = userId,
                type = StoreTypeEnum.SERVICE.type.toByte(),
                id = UUIDUtil.generate(),
                storeMediaInfoReq = StoreMediaInfoRequest(
                    storeCode = serviceCode,
                    mediaUrl = it.mediaUrl,
                    mediaType = it.mediaType.name,
                    modifier = userId
                )
            )
        }

        if (settingInfo != null) {
            extServiceFeatureDao.updateExtServiceFeatureBaseInfo(
                dslContext = dslContext,
                userId = userId,
                serviceCode = serviceCode,
                extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                    publicFlag = settingInfo.publicFlag,
                    recommentFlag = settingInfo.recommendFlag,
                    certificationFlag = settingInfo.certificationFlag,
                    weight = settingInfo.weight,
                    modifierUser = userId,
                    serviceTypeEnum = settingInfo.type,
                    descInputType = baseInfo?.descInputType ?: DescInputTypeEnum.MANUAL
                )
            )
        }
        return Result(true)
    }

   fun addServiceItem(
        userId: String,
        serviceId: String,
        serviceCode: String,
        version: String,
        itemIds: Set<String>
    ) {
        val featureInfoRecord = extServiceFeatureDao.getLatestServiceByCode(dslContext, serviceCode)
        val itemCreateInfoList = getFileServiceProps(
            serviceCode = serviceCode,
            version = version,
            repositoryHashId = featureInfoRecord!!.repositoryHashId,
            inputItemList = itemIds
        )
        extServiceItemRelDao.deleteByServiceId(dslContext, serviceId)
        extServiceItemRelDao.batchAdd(
            dslContext = dslContext,
            userId = userId,
            serviceId = serviceId,
            itemPropList = itemCreateInfoList
        )
        // 添加扩展点使用记录
        client.get(ServiceItemResource::class).addServiceNum(itemIds)
    }

    private fun getFileServiceProps(
        serviceCode: String,
        version: String,
        repositoryHashId: String,
        inputItemList: Set<String>
    ): List<ItemPropCreateInfo> {
        val itemCreateList = mutableListOf<ItemPropCreateInfo>()
        val inputItemMap = mutableMapOf<String, ItemPropCreateInfo>()
        // 匹配页面输入的itemId，是否在json文件内有对应的props，若没有则为空。有则替换。  json文件内多余的itemCode无视
        inputItemList.forEach {
            inputItemMap[it] = ItemPropCreateInfo(
                itemId = it,
                props = "",
                bkServiceId = getItemBkServiceId(it)
            )
        }

        val fileStr = getFileStr(serviceCode, version, EXTENSION_JSON_NAME, repositoryHashId)
        if (fileStr.isNullOrEmpty()) {
            // 文件数据为空，直接返回输入数据
            return getInputData(inputItemList)
        }
        val taskDataMap = JsonUtil.to(fileStr, ExtensionJson::class.java)
        logger.info("getServiceProps taskDataMap[$taskDataMap]")
        val fileServiceCode = taskDataMap.serviceCode
        val fileItemList = taskDataMap.itemList
        if (fileServiceCode != serviceCode) {
            logger.warn("getServiceProps input serviceCode[$serviceCode], extension.json serviceCode[$fileServiceCode]")
            throw ErrorCodeException(errorCode = StoreMessageCode.USER_SERVICE_CODE_DIFF)
        }

        if (fileItemList == null) {
            // 文件数据为空，直接返回输入数据
            return getInputData(inputItemList)
        }

        // 文件与输入itemCode取交集，若文件内有props，以文件props为准
        val filePropMap = mutableMapOf<String, String>()
        fileItemList.forEach {
            filePropMap[it.itemCode] = JsonUtil.toJson(it.props ?: "")
        }
        val itemRecords = client.get(ServiceItemResource::class).getItemInfoByIds(inputItemList).data
        itemRecords?.forEach {
            val itemCode = it.itemCode
            val itemId = it.itemId
            if (filePropMap.keys.contains(itemCode)) {
                val propsInfo = filePropMap[itemCode]
                inputItemMap[itemId] = ItemPropCreateInfo(
                    itemId = itemId,
                    props = propsInfo ?: "",
                    bkServiceId = getItemBkServiceId(itemId)
                )
            }
        }

        // 返回取完交集后的数据
        inputItemMap.forEach { (_, u) ->
            itemCreateList.add(u)
        }
        return itemCreateList
    }

    fun getItemBkServiceId(itemId: String): Long {
        var bkServiceId = redisOperation.hget(ProjectConstant.ITEM_BK_SERVICE_REDIS_KEY, itemId)
        // redis中没有取到蓝盾服务id，则去数据库中取
        if (null == bkServiceId) {
            val serviceItem = client.get(ServiceItemResource::class).getItemById(itemId).data
            if (null == serviceItem) {
                throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(itemId))
            } else {
                bkServiceId = serviceItem.parentId
            }
        }
        return bkServiceId.toLong()
    }

    fun getReadMeFile(userId: String, serviceCode: String): String? {
        val serviceRecord = extServiceDao.getNewestServiceByCode(dslContext, serviceCode)
            ?: throw RuntimeException(MessageCodeUtil.getCodeMessage(
                messageCode = StoreMessageCode.USER_SERVICE_NOT_EXIST,
                params = arrayOf(serviceCode))
            )
        val featureRecord = extServiceFeatureDao.getLatestServiceByCode(dslContext, serviceCode)
            ?: throw RuntimeException(MessageCodeUtil.getCodeMessage(
                messageCode = StoreMessageCode.USER_SERVICE_NOT_EXIST,
                params = arrayOf(serviceCode))
            )
        return getFileStr(
            serviceCode = serviceCode,
            version = serviceRecord.version,
            fileName = README,
            repositoryHashId = featureRecord.repositoryHashId
        )
    }

    /**
     * 获取文件信息
     */
    abstract fun getFileStr(
        serviceCode: String,
        version: String,
        fileName: String,
        repositoryHashId: String? = null
    ): String?

    private fun getInputData(inputItem: Set<String>): List<ItemPropCreateInfo> {
        // 默认添加页面选中的扩展点, props给空
        val itemCreateList = mutableListOf<ItemPropCreateInfo>()
        inputItem.forEach {
            itemCreateList.add(
                ItemPropCreateInfo(
                    itemId = it,
                    props = "",
                    bkServiceId = getItemBkServiceId(it)
                )
            )
        }
        return itemCreateList
    }
}
