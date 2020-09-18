package com.tencent.devops.environment.websocket.page

import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class NodePath : IPath {
    override fun buildPage(buildPageInfo: BuildPageInfo): String {
        return "/console/environment/${buildPageInfo.projectId}/nodeList"
    }
}