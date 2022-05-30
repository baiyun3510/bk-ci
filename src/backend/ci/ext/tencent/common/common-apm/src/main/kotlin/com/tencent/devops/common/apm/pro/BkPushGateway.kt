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

package com.tencent.devops.common.apm.pro

import io.prometheus.client.Collector
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.Base64
import io.prometheus.client.exporter.DefaultHttpConnectionFactory
import io.prometheus.client.exporter.HttpConnectionFactory
import io.prometheus.client.exporter.PushGateway
import io.prometheus.client.exporter.common.TextFormat
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class BkPushGateway(address: String?, token: String) : PushGateway(address) {

    private val connectionFactory: HttpConnectionFactory = DefaultHttpConnectionFactory()

    private val MILLISECONDS_PER_SECOND = 1000

    private val token = token

    override fun push(registry: CollectorRegistry?, job: String?) {
        doRequest(registry!!, job!!, null, "PUT")
    }

    override fun push(collector: Collector?, job: String?) {
        val registry = CollectorRegistry()
        collector!!.register<Collector>(registry)
        push(registry, job)
    }

    private fun doRequest(
        registry: CollectorRegistry?,
        job: String?,
        groupingKey: MutableMap<String, String>?,
        method: String?,
    ) {
        val bkToken = token
        var url = gatewayBaseURL
        url += if (job!!.contains("/")) {
            "job@base64/" + base64url(job)
        } else {
            "job/" + URLEncoder.encode(job, "UTF-8")
        }

        groupingKey?.forEach { key, value ->
            url += if (value.isEmpty()) {
                "/$key@base64/="
            } else if (value.contains("/")) {
                "/" + key + "@base64/" + base64url(value)
            } else {
                "/" + key + "/" + URLEncoder.encode(value, "UTF-8")
            }
        }

        val connection = connectionFactory.create(url)
        connection.setRequestProperty("Content-Type", TextFormat.CONTENT_TYPE_004)
        connection.setRequestProperty("X-BK-TOKEN", bkToken)
        if (method != "DELETE") {
            connection.doOutput = true
        }
        connection.requestMethod = method

        connection.connectTimeout = 10 * MILLISECONDS_PER_SECOND
        connection.readTimeout = 10 * MILLISECONDS_PER_SECOND
        connection.connect()

        try {
            if (method != "DELETE") {
                val writer = BufferedWriter(OutputStreamWriter(connection.outputStream, "UTF-8"))
                TextFormat.write004(writer, registry!!.metricFamilySamples())
                writer.flush()
                writer.close()
            }
            val response = connection.responseCode
            if (response / 100 != 2) {
                val errorMessage: String
                val errorStream = connection.errorStream
                errorMessage = if (errorStream != null) {
                    val errBody = readFromStream(errorStream)
                    "Response code from $url was $response, response body: $errBody"
                } else {
                    "Response code from $url was $response"
                }
                throw IOException(errorMessage)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun base64url(v: String): String? {
        // Per RFC4648 table 2. We support Java 6, and java.util.Base64 was only added in Java 8,
        return try {
            Base64.encodeToString(v.toByteArray(charset("UTF-8"))).replace("+", "-").replace("/", "_")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e) // Unreachable.
        }
    }

    @Throws(IOException::class)
    private fun readFromStream(`is`: InputStream): String? {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int
        while (`is`.read(buffer).also { length = it } != -1) {
            result.write(buffer, 0, length)
        }
        return result.toString("UTF-8")
    }
}