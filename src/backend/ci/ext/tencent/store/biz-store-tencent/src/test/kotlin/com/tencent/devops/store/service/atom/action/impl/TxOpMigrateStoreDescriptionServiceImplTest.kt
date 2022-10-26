package com.tencent.devops.store.service.atom.action.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.regex.Matcher
import java.util.regex.Pattern

class TxOpMigrateStoreDescriptionServiceImplTest {

    @Test
    fun checkLogoUrlConditionTest() {
        val pathList = checkLogoUrlCondition(description)
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
    fun replaceDescription() {
        val pathMap = mapOf(
            "http://radosgw.open.oa.com/xxx/xx/xx/file/png/xxxxx.png\\?v=xxxx"
                    to "https://test.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas",
            "https://radosgw.open.oa.com/xxx/xx/xx/file/png/xxsxax.png\\?v=aas"
        to "https://test.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas"
        )
        println(replaceDescription(description, pathMap))
    }

    private fun checkLogoUrlCondition(description: String?): List<String>? {
        if (description.isNullOrBlank()) {
            return null
        }
        val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
        val matcher: Matcher = pattern.matcher(description)
        val pathList = mutableListOf<String>()

        return pathList
    }

    private fun replaceDescription(description: String, pathMap: Map<String, String>): String {
        var newDescription = description
        pathMap.forEach {
            val pattern: Pattern = Pattern.compile("(!\\[(.*)]\\()(${it.key})(\\))")
            val matcher: Matcher = pattern.matcher(newDescription)
            newDescription = matcher.replaceAll("$1${it.value}$4")
        }
        return newDescription
    }


    companion object {
        private const val BK_CI_PATH_REGEX = "(!\\[(.*?)]\\()(http[s]?://radosgw.open.oa.com(.*?))(\\))"
        private const val description = "合质量标准。\\n\\n![image1.png](http://radosgw.open.oa.com/xxx/xx/" +
                "xx/file/png/xxxxx.png?v=xxxx)\\n\\n[了解" +
                "更多Code合质量标准。\\n\\n![image3.png](https://radosgw.open.oa.com/xxx/xx/xx/file/png/xxsx" +
                "ax.png?v=aas)\\n\\n[了解更多Code" +
                "更多Code合质量标准。\\n\\n![image2.png](http://radosgw.open.oa.com/xxx/xx/xx/file/png/xxxs" +
                "x.png?v=xxxxx)\\n\\n[了解更多Code"
    }
}