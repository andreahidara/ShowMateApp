package com.andrea.showmateapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbUtilsTest {

    // region buildImageUrl

    @Test
    fun `buildImageUrl returns null when path is null`() {
        assertNull(TmdbUtils.buildImageUrl(null))
    }

    @Test
    fun `buildImageUrl builds correct url with leading slash`() {
        val result = TmdbUtils.buildImageUrl("/abc123.jpg", TmdbUtils.ImageSize.W500)
        assertEquals("https://image.tmdb.org/t/p/w500/abc123.jpg", result)
    }

    @Test
    fun `buildImageUrl adds leading slash when path has no slash`() {
        val result = TmdbUtils.buildImageUrl("abc123.jpg", TmdbUtils.ImageSize.W500)
        assertEquals("https://image.tmdb.org/t/p/w500/abc123.jpg", result)
    }

    @Test
    fun `buildImageUrl uses W500 as default size`() {
        val result = TmdbUtils.buildImageUrl("/poster.jpg")
        assertTrue(result?.contains("w500") == true)
    }

    @Test
    fun `buildImageUrl respects specified image size`() {
        val result = TmdbUtils.buildImageUrl("/poster.jpg", TmdbUtils.ImageSize.W185)
        assertEquals("https://image.tmdb.org/t/p/w185/poster.jpg", result)
    }

    @Test
    fun `buildImageUrl with ORIGINAL size`() {
        val result = TmdbUtils.buildImageUrl("/poster.jpg", TmdbUtils.ImageSize.ORIGINAL)
        assertEquals("https://image.tmdb.org/t/p/original/poster.jpg", result)
    }

    // endregion

    // region getPosterUrl

    @Test
    fun `getPosterUrl returns null for null path`() {
        assertNull(TmdbUtils.getPosterUrl(null))
    }

    @Test
    fun `getPosterUrl uses W500 by default`() {
        val result = TmdbUtils.getPosterUrl("/poster.jpg")
        assertTrue(result?.contains("w500") == true)
    }

    @Test
    fun `getPosterUrl respects custom size`() {
        val result = TmdbUtils.getPosterUrl("/poster.jpg", TmdbUtils.ImageSize.W342)
        assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg", result)
    }

    // endregion

    // region getBackdropUrl

    @Test
    fun `getBackdropUrl returns null for null path`() {
        assertNull(TmdbUtils.getBackdropUrl(null))
    }

    @Test
    fun `getBackdropUrl uses W1280 by default`() {
        val result = TmdbUtils.getBackdropUrl("/backdrop.jpg")
        assertTrue(result?.contains("w1280") == true)
    }

    @Test
    fun `getBackdropUrl respects custom size`() {
        val result = TmdbUtils.getBackdropUrl("/backdrop.jpg", TmdbUtils.ImageSize.W780)
        assertEquals("https://image.tmdb.org/t/p/w780/backdrop.jpg", result)
    }

    // endregion
}
