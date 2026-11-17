package com.hippo.ehviewer.client

import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.util.ensureSuccess
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.takeFrom
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readBuffer
import java.net.URI
import java.nio.ByteBuffer
import kotlinx.io.readByteArray

internal suspend fun HttpClient.resolveArchiveDownload(
    initialUrl: String,
    initialReferer: String,
    parseDownloadLink: (ByteBuffer) -> String?,
): Pair<String, String> {
    var url = initialUrl
    var referer = initialReferer
    val visited = hashSetOf<String>()
    repeat(5) {
        check(visited.add(url)) { "Archive download page redirects to itself" }
        val (next, isArchive) = prepareGet(url) {
            header(HttpHeaders.Referrer, referer)
            header(HttpHeaders.AcceptEncoding, "identity")
        }.executeSafely { response ->
            response.status.ensureSuccess()
            val body = response.bodyAsChannel()
            val prefix = body.readBuffer(4L).readByteArray()
            if (prefix.contentEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04)) || prefix.contentEquals(byteArrayOf(0x50, 0x4b, 0x05, 0x06))) {
                return@executeSafely response.request.url.toString() to true
            }
            val buffer = ByteBuffer.allocateDirect(512 * 1024).put(prefix)
            while (buffer.hasRemaining() && !body.isClosedForRead) {
                if (body.readAvailable(buffer) == -1) break
            }
            check(body.readBuffer(1L).readByteArray().isEmpty()) { "Archive download page is too large" }
            buffer.flip()
            val link = parseDownloadLink(buffer) ?: throw EhException("Archive download page does not contain a download link")
            referer = response.request.url.toString()
            val nextUrl = URLBuilder(response.request.url).takeFrom(link).build().also {
                check(it.protocol == URLProtocol.HTTP || it.protocol == URLProtocol.HTTPS) { "Invalid archive download URL" }
                check(it.user.isNullOrEmpty() && it.password.isNullOrEmpty()) { "Invalid archive download URL" }
            }
            URI(nextUrl.toString()).normalize().toString() to false
        }
        if (isArchive) return next to referer
        url = next
    }
    throw EhException("Too many archive download pages")
}
