package com.andrea.showmateapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrativeStyleMapperTest {

    // region extractStyles

    @Test
    fun `extractStyles returns empty map when no keywords match`() {
        val result = NarrativeStyleMapper.extractStyles(listOf("nothing", "here"), null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractStyles detects detective style from keyword`() {
        val result = NarrativeStyleMapper.extractStyles(listOf("detective mystery", "crime"), null)
        assertTrue(result.containsKey("protagonista_detective"))
        assertTrue(result["protagonista_detective"]!! > 0f)
    }

    @Test
    fun `extractStyles detects dark tone from keyword`() {
        val result = NarrativeStyleMapper.extractStyles(listOf("dark humor", "violence"), null)
        assertTrue(result.containsKey("tono_oscuro"))
    }

    @Test
    fun `extractStyles detects complex narrative`() {
        val result = NarrativeStyleMapper.extractStyles(listOf("plot twist", "psychological"), null)
        assertTrue(result.containsKey("narrativa_compleja"))
    }

    @Test
    fun `extractStyles detects short episode rhythm for runtime under 30`() {
        val result = NarrativeStyleMapper.extractStyles(emptyList(), 25)
        assertTrue(result.containsKey("ritmo_episodico"))
        assertEquals(1f, result["ritmo_episodico"]!!, 0.001f)
    }

    @Test
    fun `extractStyles detects long episode rhythm for runtime over 50`() {
        val result = NarrativeStyleMapper.extractStyles(emptyList(), 60)
        assertTrue(result.containsKey("ritmo_largo"))
        assertEquals(1f, result["ritmo_largo"]!!, 0.001f)
    }

    @Test
    fun `extractStyles does not add episode rhythm when runtime is null`() {
        val result = NarrativeStyleMapper.extractStyles(emptyList(), null)
        assertFalse(result.containsKey("ritmo_episodico"))
        assertFalse(result.containsKey("ritmo_largo"))
    }

    @Test
    fun `extractStyles adds ritmo_episodico for runtime of exactly 30`() {
        val result = NarrativeStyleMapper.extractStyles(emptyList(), 30)
        assertTrue(result.containsKey("ritmo_episodico"))
        assertFalse(result.containsKey("ritmo_largo"))
    }

    @Test
    fun `extractStyles scores are between 0 and 1`() {
        val result = NarrativeStyleMapper.extractStyles(
            listOf("detective", "crime solving", "police", "fbi", "investigator"),
            null
        )
        result.forEach { (_, score) ->
            assertTrue("Score $score out of [0,1]", score in 0f..1f)
        }
    }

    @Test
    fun `extractStyles keyword matching is case-insensitive`() {
        val lower = NarrativeStyleMapper.extractStyles(listOf("detective"), null)
        val upper = NarrativeStyleMapper.extractStyles(listOf("DETECTIVE"), null)
        assertEquals(lower["protagonista_detective"], upper["protagonista_detective"])
    }

    @Test
    fun `extractStyles can detect multiple styles at once`() {
        val result = NarrativeStyleMapper.extractStyles(
            listOf("detective", "dark", "emotional"),
            null
        )
        assertTrue(result.size >= 2)
    }

    // endregion

    // region getStyleLabel

    @Test
    fun `getStyleLabel returns correct label for narrativa_compleja`() {
        assertEquals("Narrativa compleja", NarrativeStyleMapper.getStyleLabel("narrativa_compleja"))
    }

    @Test
    fun `getStyleLabel returns correct label for tono_oscuro`() {
        assertEquals("Tono oscuro", NarrativeStyleMapper.getStyleLabel("tono_oscuro"))
    }

    @Test
    fun `getStyleLabel returns correct label for ritmo_episodico`() {
        assertEquals("Episodios cortos", NarrativeStyleMapper.getStyleLabel("ritmo_episodico"))
    }

    @Test
    fun `getStyleLabel returns correct label for ritmo_largo`() {
        assertEquals("Episodios largos", NarrativeStyleMapper.getStyleLabel("ritmo_largo"))
    }

    @Test
    fun `getStyleLabel returns key itself for unknown style`() {
        assertEquals("unknown_style", NarrativeStyleMapper.getStyleLabel("unknown_style"))
    }

    @Test
    fun `getStyleLabel covers all known clusters`() {
        val knownKeys = listOf(
            "narrativa_compleja", "protagonista_detective", "protagonista_antihero",
            "protagonista_genio", "tono_oscuro", "tono_emocional", "tono_ligero",
            "ritmo_intenso", "ritmo_lento", "ritmo_episodico", "ritmo_largo"
        )
        knownKeys.forEach { key ->
            val label = NarrativeStyleMapper.getStyleLabel(key)
            assertFalse("Label for $key should not be the key itself", label == key)
        }
    }

    // endregion
}
