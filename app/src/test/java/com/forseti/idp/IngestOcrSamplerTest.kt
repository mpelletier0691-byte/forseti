package com.forseti.idp

import org.junit.Assert.assertEquals
import org.junit.Test

class IngestOcrSamplerTest {

    @Test
    fun pageIndices_singlePage() {
        assertEquals(listOf(0), IngestOcrSampler.pageIndicesForSampling(1))
    }

    @Test
    fun pageIndices_twoPages() {
        assertEquals(listOf(0, 1), IngestOcrSampler.pageIndicesForSampling(2))
    }

    @Test
    fun pageIndices_sixPages() {
        assertEquals(listOf(0, 1, 5), IngestOcrSampler.pageIndicesForSampling(6))
    }

    @Test
    fun pageIndices_longDocument_includesMiddle() {
        assertEquals(listOf(0, 1, 5, 9), IngestOcrSampler.pageIndicesForSampling(10))
    }
}
