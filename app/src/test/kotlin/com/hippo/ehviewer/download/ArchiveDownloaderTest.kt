package com.hippo.ehviewer.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArchiveDownloaderTest {
    @Test
    fun rangesCoverEveryByteExactlyOnce() {
        for (size in listOf(1L, 3L, 32L, 101L, 5_000_000_000L, Long.MAX_VALUE)) {
            for (threads in listOf(-1, 1, 8, 32, 100)) {
                val ranges = archiveRanges(size, threads)
                assertEquals(0L, ranges.first().first)
                assertEquals(size - 1, ranges.last().last)
                assertTrue(ranges.size in 1..32)
                assertTrue(ranges.all { !it.isEmpty() })
                ranges.zipWithNext().forEach { (left, right) -> assertEquals(left.last + 1, right.first) }
                assertEquals(size, ranges.sumOf { it.last - it.first + 1 })
            }
        }
    }

    @Test
    fun rejectsWrongRangesAndSizes() {
        checkArchiveRange("bytes 10-19/100", 10L..19L, 100)
        for (value in listOf(null, "bytes 0-9/100", "bytes 10-19/101", "bytes 10-19/*")) {
            assertFailsWith<IllegalStateException> { checkArchiveRange(value, 10L..19L, 100) }
        }
        assertFailsWith<IllegalArgumentException> { archiveRanges(0, 8) }
    }
}
