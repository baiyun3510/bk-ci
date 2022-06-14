package com.tencent.devops.common.apm.prometheus

import io.prometheus.client.exporter.HttpConnectionFactory
import java.net.HttpURLConnection
import java.net.URL

class BkConnectionFactory(
    private val token: String
) : HttpConnectionFactory {
    override fun create(url: String?): HttpURLConnection {
//        val httpURLConnection = URL(url).openConnection() as HttpURLConnection
//        httpURLConnection.setRequestProperty("X-BK-TOKEN", token)
//        return httpURLConnection
        return URL("$url?X-BK-TOKEN=$token").openConnection() as HttpURLConnection
    }
}
