package com.tencent.devops.store.service.atom.action.impl

import com.tencent.devops.store.service.common.impl.TxOpMigrateStoreDescriptionServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TxOpMigrateStoreDescriptionServiceImplTest {

    @Test
    fun checkLogoUrlConditionTest() {
        val t = TxOpMigrateStoreDescriptionServiceImpl()
        val pathList = t.checkLogoUrlCondition(description)
        Assertions.assertEquals(
            "http://radosgw.open.oa.com/xxx/xx/xx/file/png/xxxxx.png?v=xxxx",
            pathList?.get(0) ?: ""
        )
        Assertions.assertEquals(
            "https://radosgw.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas",
            pathList?.get(1) ?: ""
        )
    }

    @Test
    fun replaceDescriptionTest() {
        val t = TxOpMigrateStoreDescriptionServiceImpl()
        val pathMap = mapOf(
            "http://radosgw.open.oa.com/xxx/xx/xx/file/png/xxxxx.png?v=xxxx".replace("?", "\\?")
                    to "https://test.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas",
            "https://radosgw.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas".replace("?", "\\?")
        to "https://test.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas"
        )
        println(t.replaceDescription(description, pathMap))
    }

    companion object {
        private const val BK_CI_PATH_REGEX = "(!\\[(.*?)]\\()(http[s]?://radosgw.open.oa.com(.*?))(\\))"
        private const val description = "合质量标准。\\n\\n![image1.png](http://radosgw.open.oa.com/xxx/xx/" +
                "xx/file/png/xxxxx.png?v=xxxx)\\n\\n[了解更多Code合质量标准。\\n\\n![image3.png](https://rados" +
                "gw.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas)\\n\\n[了解更多Code更多Code合质量标准。" +
                "\\n\\n![image2.png](http://radosgw.open.oa.com/xxx/xx/xx/file/png/xxxsx.png?v=xxxxx)\\n\\n[了解更多Code"
    }
}