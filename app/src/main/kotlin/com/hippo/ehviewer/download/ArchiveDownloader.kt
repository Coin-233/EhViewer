package com.hippo.ehviewer.download

import arrow.fx.coroutines.parMap
import com.hippo.ehviewer.client.ehRequest
import com.hippo.ehviewer.spider.timeoutBySpeed
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun archiveRanges(size: Long, threads: Int): List<LongRange> {
    require(size > 0)
    val count = minOf(threads.coerceIn(1, 32).toLong(), size).toInt()
    val chunk = size / count
    return List(count) { index ->
        val start = index * chunk
        start..if (index == count - 1) size - 1 else start + chunk - 1
    }
}

internal fun checkArchiveRange(value: String?, range: LongRange, size: Long) {
    check(value == "bytes ${range.first}-${range.last}/$size") { "Invalid archive Content-Range: $value" }
}

private class RangeUnsupported : IOException()

internal suspend fun downloadArchive(
    url: String,
    referer: String,
    file: File,
    threads: Int,
    onProgress: suspend (total: Long, received: Long, bytesRead: Int) -> Unit,
) {
    suspend fun save(response: HttpResponse, offset: Long = 0, expected: Long? = null) {
        RandomAccessFile(file, "rw").use { output ->
            output.seek(offset)
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(64 * 1024)
            var written = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = channel.readAvailable(buffer)
                if (count == -1) break
                check(expected == null || written + count <= expected) { "Archive response exceeds requested range" }
                output.write(buffer, 0, count)
                written += count
            }
            check(expected == null || written == expected) { "Incomplete archive response" }
        }
    }

    suspend fun singleConnection() {
        RandomAccessFile(file, "rw").use { it.setLength(0) }
        timeoutBySpeed(
            url,
            {
                ehRequest(url, referer) {
                    header(HttpHeaders.AcceptEncoding, "identity")
                    it()
                }
            },
            onProgress,
            { response ->
                check(response.status == HttpStatusCode.OK) { "Unexpected archive response: ${response.status}" }
                save(response, expected = response.headers[HttpHeaders.ContentLength]?.toLongOrNull())
            },
        )
    }

    if (threads.coerceIn(1, 32) == 1) return singleConnection()
    var size = 0L
    var validator: String? = null
    timeoutBySpeed(
        url,
        {
            ehRequest(url, referer) {
                header(HttpHeaders.AcceptEncoding, "identity")
                header(HttpHeaders.Range, "bytes=0-0")
                it()
            }
        },
        { _, _, _ -> },
        { response ->
            if (response.status == HttpStatusCode.PartialContent) {
                size = response.headers[HttpHeaders.ContentRange]?.substringAfterLast('/')?.toLongOrNull() ?: 0
                check(size > 0) { "Missing archive size" }
                checkArchiveRange(response.headers[HttpHeaders.ContentRange], 0L..0L, size)
                validator = response.headers[HttpHeaders.ETag]?.takeUnless { it.startsWith("W/") }
                    ?: response.headers[HttpHeaders.LastModified]
            } else {
                check(response.status == HttpStatusCode.OK) { "Unexpected archive response: ${response.status}" }
            }
        },
    )
    // A stable validator prevents combining different versions of the archive.
    if (size == 0L || validator == null) return singleConnection()
    val ranges = archiveRanges(size, threads)
    val progress = LongArray(ranges.size)
    val progressMutex = Mutex()
    RandomAccessFile(file, "rw").use { it.setLength(size) }
    try {
        ranges.withIndex().toList().parMap(concurrency = ranges.size) { (index, range) ->
            timeoutBySpeed(
                url,
                {
                    ehRequest(url, referer) {
                        header(HttpHeaders.AcceptEncoding, "identity")
                        header(HttpHeaders.Range, "bytes=${range.first}-${range.last}")
                        header(HttpHeaders.IfRange, validator)
                        it()
                    }
                },
                { _, done, bytesRead ->
                    progressMutex.withLock {
                        progress[index] = done
                        onProgress(size, progress.sum(), bytesRead)
                    }
                },
                { response ->
                    if (response.status == HttpStatusCode.OK) throw RangeUnsupported()
                    check(response.status == HttpStatusCode.PartialContent) { "Unexpected archive response: ${response.status}" }
                    checkArchiveRange(response.headers[HttpHeaders.ContentRange], range, size)
                    save(response, range.first, range.last - range.first + 1)
                },
            )
        }
    } catch (_: RangeUnsupported) {
        onProgress(-1, 0, 0)
        singleConnection()
    }
}
