package com.hippo.ehviewer.download

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

class ArchiveImporterTest {
    private fun withArchive(vararg entries: Pair<String, ByteArray>, block: suspend (File, File) -> Unit) = runBlocking {
        val root = Files.createTempDirectory("archive-import-test").toFile()
        try {
            val zip = File(root, "gallery.zip")
            ZipOutputStream(zip.outputStream()).use { output ->
                entries.forEach { (name, bytes) ->
                    output.putNextEntry(
                        ZipEntry(name).apply {
                            method = ZipEntry.STORED
                            size = bytes.size.toLong()
                            crc = CRC32().apply { update(bytes) }.value
                        },
                    )
                    output.write(bytes)
                    output.closeEntry()
                }
            }
            block(zip, File(root, "pages"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun importsImagesInPageOrderWithoutTrustingEntryPaths() = withArchive(
        "gallery/10.jpg" to byteArrayOf(10),
        "gallery/2.PNG" to byteArrayOf(2),
        "../1.jpg" to byteArrayOf(1),
        "ComicInfo.xml" to byteArrayOf(0),
        "__MACOSX/1.jpg" to byteArrayOf(0),
    ) { zip, directory ->
        val pages = extractGalleryArchive(zip, directory, 3)
        assertEquals(listOf("00000001.jpg", "00000002.png", "00000003.jpg"), pages.map { it.name })
        pages.forEachIndexed { index, file -> assertContentEquals(byteArrayOf(listOf(1, 2, 10)[index].toByte()), file.readBytes()) }
        assertFalse(File(directory.parentFile, "1.jpg").exists())
    }

    @Test
    fun rejectsMissingPagesBeforeWritingImages() = withArchive("1.jpg" to byteArrayOf(1)) { zip, directory ->
        assertFailsWith<IllegalStateException> { extractGalleryArchive(zip, directory, 2) }
        assertFalse(directory.exists())
    }

    @Test
    fun rejectsArchivesWithoutImages() = withArchive("readme.txt" to byteArrayOf(1)) { zip, directory ->
        assertFailsWith<IllegalStateException> { extractGalleryArchive(zip, directory, 0) }
        assertFalse(directory.exists())
    }

    @Test
    fun verifiesImageChecksums() = withArchive("1.jpg" to byteArrayOf(1, 2, 3, 4)) { zip, directory ->
        val bytes = zip.readBytes()
        val dataOffset = 30 + "1.jpg".length
        bytes[dataOffset] = 99
        zip.writeBytes(bytes)
        assertFailsWith<IllegalStateException> { extractGalleryArchive(zip, directory, 1) }
    }

    @Test
    fun honoursCancellation() = withArchive("1.jpg" to byteArrayOf(1)) { zip, directory ->
        coroutineScope {
            val job = launch {
                cancel()
                assertFailsWith<CancellationException> { extractGalleryArchive(zip, directory, 1) }
            }
            job.join()
        }
        assertFalse(directory.listFiles().orEmpty().any())
    }

    @Test
    fun publishesPagesUsingOnlyRenamesWithinTheDownloadDirectory() = withArchive { _, directory ->
        directory.mkdirs()
        val target = File(directory, "00000001.jpg").apply { writeText("old") }
        val pending = File(directory, "00000001.jpg.archive.tmp").apply { writeText("new") }
        FileSystem.SYSTEM.publishArchivePage(pending.toOkioPath(), target.toOkioPath())
        assertEquals("new", target.readText())
        assertEquals(listOf(target.name), directory.listFiles()!!.map { it.name })
    }

    @Test
    fun restoresOldPageWhenPublishingFails() = withArchive { _, directory ->
        directory.mkdirs()
        val target = File(directory, "00000001.jpg").apply { writeText("old") }
        val pending = File(directory, "00000001.jpg.archive.tmp").apply { writeText("new") }
        val fileSystem = object : ForwardingFileSystem(FileSystem.SYSTEM) {
            override fun atomicMove(source: Path, target: Path) {
                if (source == pending.toOkioPath()) throw IOException("Storage unavailable")
                super.atomicMove(source, target)
            }
        }
        assertFailsWith<IOException> { fileSystem.publishArchivePage(pending.toOkioPath(), target.toOkioPath()) }
        assertEquals("old", target.readText())
        assertEquals("new", pending.readText())
    }

    @Test
    fun keepsBackupIfStorageAlsoRejectsRollback() = withArchive { _, directory ->
        directory.mkdirs()
        val target = File(directory, "00000001.jpg").apply { writeText("old") }
        val pending = File(directory, "00000001.jpg.archive.tmp").apply { writeText("new") }
        val fileSystem = object : ForwardingFileSystem(FileSystem.SYSTEM) {
            override fun atomicMove(source: Path, target: Path) {
                if (source != File(directory, "00000001.jpg").toOkioPath()) throw IOException("Storage unavailable")
                super.atomicMove(source, target)
            }
        }
        assertFailsWith<IOException> { fileSystem.publishArchivePage(pending.toOkioPath(), target.toOkioPath()) }
        assertEquals("old", File(directory, "00000001.jpg.archive.old.tmp").readText())
        assertEquals("new", pending.readText())
    }
}
