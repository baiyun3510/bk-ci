package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.user.UserPipelineOutputResource
import com.tencent.devops.artifactory.pojo.PipelineOutput
import com.tencent.devops.artifactory.pojo.PipelineOutputSearchOption
import com.tencent.devops.artifactory.service.BkRepoPipelineOutputService
import com.tencent.devops.common.api.pojo.Result
import org.springframework.web.bind.annotation.RestController

@RestController
class UserPipelineOutputResourceImpl(
    private val bkRepoPipelineOutputService: BkRepoPipelineOutputService
) : UserPipelineOutputResource {
    override fun searchByBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        option: PipelineOutputSearchOption
    ): Result<PipelineOutput> {
        return Result(bkRepoPipelineOutputService.search(userId, projectId, pipelineId, buildId, option))
    }
}