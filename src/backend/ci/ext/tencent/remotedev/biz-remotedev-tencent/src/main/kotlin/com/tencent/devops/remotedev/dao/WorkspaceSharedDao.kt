package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WorkspaceSharedDao {

    // 新增工作空间共享记录
    fun createWorkspaceSharedInfo(
        userId: String,
        workspaceShared: WorkspaceShared,
        dslContext: DSLContext
    ) {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            dslContext.insertInto(
                this,
                WORKSPACE_ID,
                OPERATOR,
                SHARED_USER
            )
                .values(
                    workspaceShared.workspaceId,
                    userId,
                    workspaceShared.sharedUser
                ).execute()
        }
    }

    // 删除工作空间共享记录
    fun deleteWorkspaceSharedInfo(
        workspaceId: Long,
        sharedUser: String,
        dslContext: DSLContext
    ) {
        with(TWorkspaceShared.T_WORKSPACE_SHARED) {
            dslContext.delete(this)
                .where(WORKSPACE_ID.eq(workspaceId))
                .and(SHARED_USER.equals(sharedUser))
                .limit(1)
        }
    }
}
