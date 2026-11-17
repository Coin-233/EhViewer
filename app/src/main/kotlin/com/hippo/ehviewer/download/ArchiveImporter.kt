package com.hippo.ehviewer.download

import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.FileSystem
import okio.Path

internal fun FileSystem.publishArchivePage(pending: Path, target: Path) {
    require(pending.parent == target.parent)
    val backup = target.parent!! / "${target.name}.archive.old.tmp"
    if (exists(target)) {
        if (exists(backup)) delete(target) else atomicMove(target, backup)
        check(!exists(target) && exists(backup)) { "Could not preserve previous archive page: ${target.name}" }
    }
    try {
        atomicMove(pending, target)
        check(exists(target) && !exists(pending)) { "Archive page import failed: ${target.name}" }
    } catch (e: Throwable) {
        if (!exists(target) && exists(backup)) {
            runCatching { atomicMove(backup, target) }.onFailure { e.addSuppressed(it) }
        }
        throw e
    }
    delete(backup)
}

private val imageExtensions = setOf("jpeg", "jpg", "png", "gif", "webp", "bmp", "ico", "wbmp", "heic", "heif", "avif")
private val filenameParts = Regex("\\d+|\\D+")

private val archiveNameComparator = Comparator<String> { first, second ->
    val left = filenameParts.findAll(first).map { it.value }.iterator()
    val right = filenameParts.findAll(second).map { it.value }.iterator()
    var result = 0
    while (left.hasNext() && right.hasNext() && result == 0) {
        val a = left.next()
        val b = right.next()
        result = if (a[0].isDigit() && b[0].isDigit()) {
            val x = a.trimStart('0')
            val y = b.trimStart('0')
            compareValues(x.length, y.length).takeIf { it != 0 } ?: x.compareTo(y)
        } else {
            a.compareTo(b, ignoreCase = true)
        }
    }
    result.takeIf { it != 0 } ?: compareValues(left.hasNext(), right.hasNext()).takeIf { it != 0 } ?: first.compareTo(second)
}

internal suspend fun extractGalleryArchive(archive: File, directory: File, expectedPages: Int): List<File> = ZipFile(archive).use { zip ->
    val entries = zip.entries().asSequence().filter {
        !it.isDirectory && !it.name.startsWith("__MACOSX/") && it.name.substringAfterLast('/').substringAfterLast('.').lowercase() in imageExtensions
    }.sortedWith { a, b -> archiveNameComparator.compare(a.name, b.name) }.toList()
    check(entries.isNotEmpty()) { "Archive contains no images" }
    check(expectedPages <= 0 || entries.size == expectedPages) { "Archive page count mismatch: ${entries.size}/$expectedPages" }
    check(directory.isDirectory || directory.mkdirs()) { "Cannot create archive import directory" }
    entries.mapIndexed { index, entry ->
        currentCoroutineContext().ensureActive()
        // Generate page names ourselves; ZIP entry paths must never become output paths.
        val output = File(directory, "%08d.%s".format(index + 1, entry.name.substringAfterLast('.').lowercase()))
        val crc = CRC32()
        var written = 0L
        zip.getInputStream(entry).use { input ->
            output.outputStream().use { sink ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count == -1) break
                    check(written + count <= entry.size) { "Archive page exceeds declared size" }
                    sink.write(buffer, 0, count)
                    crc.update(buffer, 0, count)
                    written += count
                }
            }
        }
        check(written == entry.size && crc.value == entry.crc) { "Corrupt archive page: ${entry.name}" }
        output
    }
}
