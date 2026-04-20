package com.andrea.showmateapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreMapperTest {

    // region jaccardSimilarity

    @Test
    fun `jaccardSimilarity returns 0 when both maps are empty`() {
        val result = GenreMapper.jaccardSimilarity(emptyMap(), emptyMap())
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `jaccardSimilarity returns 1 when maps are identical`() {
        val a = mapOf("18" to 5f, "35" to 3f)
        val result = GenreMapper.jaccardSimilarity(a, a)
        assertEquals(1f, result, 0.001f)
    }

    @Test
    fun `jaccardSimilarity returns 0 when maps have no overlap`() {
        val a = mapOf("18" to 5f)
        val b = mapOf("35" to 3f)
        val result = GenreMapper.jaccardSimilarity(a, b)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `jaccardSimilarity returns value between 0 and 1 for partial overlap`() {
        val a = mapOf("18" to 4f, "35" to 2f)
        val b = mapOf("18" to 4f, "80" to 3f)
        val result = GenreMapper.jaccardSimilarity(a, b)
        assertTrue(result > 0f && result < 1f)
    }

    @Test
    fun `jaccardSimilarity is symmetric`() {
        val a = mapOf("18" to 4f, "35" to 2f)
        val b = mapOf("18" to 4f, "80" to 3f)
        val ab = GenreMapper.jaccardSimilarity(a, b)
        val ba = GenreMapper.jaccardSimilarity(b, a)
        assertEquals(ab, ba, 0.001f)
    }

    @Test
    fun `jaccardSimilarity ignores negative scores`() {
        val a = mapOf("18" to -1f, "35" to 3f)
        val b = mapOf("18" to -2f, "35" to 3f)
        val result = GenreMapper.jaccardSimilarity(a, b)
        assertEquals(1f, result, 0.001f)
    }

    // endregion

    // region getGenreName(String)

    @Test
    fun `getGenreName returns correct name for drama`() {
        assertEquals("Drama", GenreMapper.getGenreName("18"))
    }

    @Test
    fun `getGenreName returns correct name for comedy`() {
        assertEquals("Comedia", GenreMapper.getGenreName("35"))
    }

    @Test
    fun `getGenreName returns correct name for action and adventure`() {
        assertEquals("Acción y Aventura", GenreMapper.getGenreName("10759"))
    }

    @Test
    fun `getGenreName returns correct name for scifi and fantasy`() {
        assertEquals("Sci-Fi y Fantasía", GenreMapper.getGenreName("10765"))
    }

    @Test
    fun `getGenreName returns fallback for unknown id`() {
        assertEquals("Series Variadas", GenreMapper.getGenreName("99999"))
    }

    // endregion

    // region getGenreName(Int)

    @Test
    fun `getGenreName Int delegates to String version`() {
        assertEquals(GenreMapper.getGenreName("18"), GenreMapper.getGenreName(18))
    }

    @Test
    fun `getGenreName Int returns fallback for unknown id`() {
        assertEquals("Series Variadas", GenreMapper.getGenreName(99999))
    }

    // endregion
}
