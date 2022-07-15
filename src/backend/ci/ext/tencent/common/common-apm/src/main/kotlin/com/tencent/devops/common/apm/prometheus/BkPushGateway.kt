package com.tencent.devops.common.apm.prometheus

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

class BkPushGateway(
    private val address: String
) : PushGateway(address) {

    private var connectionFactory: HttpConnectionFactory = DefaultHttpConnectionFactory()
    private val MILLISECONDS_PER_SECOND = 1000

    override fun setConnectionFactory(connectionFactory: HttpConnectionFactory) {
        this.connectionFactory = connectionFactory
    }

    override fun push(collector: Collector?, job: String?) {
        val registry = CollectorRegistry.defaultRegistry
        val method = "PUT"

        var url = gatewayBaseURL
        url += if (job!!.contains("/")) {
            "job@base64/" + base64url(job)
        } else {
            "job/" + URLEncoder.encode(job, "UTF-8")
        }


        val connection = connectionFactory.create(url)
        connection.setRequestProperty("Content-Type", TextFormat.CONTENT_TYPE_OPENMETRICS_100)
        connection.doOutput = true
        connection.requestMethod = method

        connection.connectTimeout = 10 * MILLISECONDS_PER_SECOND
        connection.readTimeout = 10 * MILLISECONDS_PER_SECOND
        connection.connect()

        try {
            val writer = BufferedWriter(OutputStreamWriter(connection.outputStream, "UTF-8"))
            TextFormat.writeOpenMetrics100(writer, registry.metricFamilySamples())
            writer.flush()
            writer.close()
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

    private fun readFromStream(inputStream: InputStream): String? {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } != -1) {
            result.write(buffer, 0, length)
        }
        return result.toString("UTF-8")
    }
}
