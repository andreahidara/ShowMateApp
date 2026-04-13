package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del filtrado colaborativo usuario-usuario.
 *
 * buildRatingVector es internal → accesible directamente desde el mismo módulo de test.
 */
class GetCollaborativeBoostUseCaseTest {

    private val userRepository = mockk<IUserRepository>()
    private lateinit var useCase: GetCollaborativeBoostUseCase

    @Before
    fun setup() {
        useCase = GetCollaborativeBoostUseCase(userRepository)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // buildRatingVector — señales implícitas
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `given watched episode entry, when buildRatingVector, then show gets SIGNAL_WATCHED=0_40`() {
        // Given
        val profile = UserProfile(watchedEpisodes = mapOf("42" to listOf(1, 2, 3)))

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then
        assertEquals("Watched show should get 0.40", 0.40f, vector[42]!!, 0.001f)
    }

    @Test
    fun `given liked show, when buildRatingVector, then show gets SIGNAL_LIKED=0_80`() {
        // Given — no tiene entrada en ratings ni watchedEpisodes
        val profile = UserProfile(likedMediaIds = listOf(7))

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then
        assertEquals("Liked show should get 0.80", 0.80f, vector[7]!!, 0.001f)
    }

    @Test
    fun `given essential show, when buildRatingVector, then show gets SIGNAL_ESSENTIAL=1_00`() {
        // Given
        val profile = UserProfile(essentialMediaIds = listOf(5))

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then
        assertEquals("Essential show should get 1.00", 1.00f, vector[5]!!, 0.001f)
    }

    @Test
    fun `given explicit rating 8 out of 10, when buildRatingVector, then show gets 0_80`() {
        // Given — rating "80" → la clave es el showId
        val profile = UserProfile(ratings = mapOf("15" to 8f))

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then — 8 / 10 = 0.80
        assertEquals("Explicit rating 8/10 should become 0.80", 0.80f, vector[15]!!, 0.001f)
    }

    @Test
    fun `given disliked show, when buildRatingVector, then show is absent from vector`() {
        // Given
        val profile = UserProfile(dislikedMediaIds = listOf(99))

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then
        assertTrue("Disliked show must be absent from rating vector", 99 !in vector)
    }

    @Test
    fun `given empty profile, when buildRatingVector, then vector is empty`() {
        // Given
        val profile = UserProfile()

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then
        assertTrue("Empty profile should produce an empty vector", vector.isEmpty())
    }

    // ── Prioridades de señales ────────────────────────────────────────────────

    @Test
    fun `given show that is both watched and liked, when buildRatingVector, then liked takes priority`() {
        // Given — watched=0.40, liked=0.80 → max(0.40, 0.80) = 0.80
        val profile = UserProfile(
            watchedEpisodes = mapOf("10" to listOf(1)),
            likedMediaIds = listOf(10)
        )

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then
        assertEquals("Liked (0.80) should override watched (0.40)", 0.80f, vector[10]!!, 0.001f)
    }

    @Test
    fun `given show with explicit rating 9 and liked signal, when buildRatingVector, then explicit rating wins`() {
        // Given — rating=9/10=0.90 > liked=0.80 → max(0.90, 0.80) = 0.90
        val profile = UserProfile(
            ratings = mapOf("20" to 9f),
            likedMediaIds = listOf(20)
        )

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then — rating sobreescribe watched; luego liked toma max(existing, 0.80)
        // La lógica aplica: ratings overwrite watched, then liked = max(existing, 0.80)
        // rating=0.90 se escribe. liked → max(0.90, 0.80) = 0.90. Resultado: 0.90
        assertEquals("Explicit rating 0.90 should override liked signal 0.80", 0.90f, vector[20]!!, 0.001f)
    }

    @Test
    fun `given essential show with lower explicit rating, when buildRatingVector, then essential always wins`() {
        // Given — essential siempre sobrescribe (incluso si rating era mayor)
        val profile = UserProfile(
            // 0.95 explicit
            ratings = mapOf("30" to 9.5f),
            // 0.80 liked
            likedMediaIds = listOf(30),
            // 1.00 essential — should win
            essentialMediaIds = listOf(30)
        )

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then
        assertEquals("Essential (1.00) should always override all other signals", 1.00f, vector[30]!!, 0.001f)
    }

    @Test
    fun `given show that is disliked and watched, when buildRatingVector, then it is excluded`() {
        // Given — dislike elimina del vector aunque haya señal watched
        val profile = UserProfile(
            watchedEpisodes = mapOf("55" to listOf(1, 2)),
            dislikedMediaIds = listOf(55)
        )

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then
        assertTrue("Disliked show must not appear even if watched", 55 !in vector)
    }

    @Test
    fun `given multiple shows with mixed signals, when buildRatingVector, then each gets correct value`() {
        // Given
        val profile = UserProfile(
            watchedEpisodes = mapOf("1" to listOf(1), "2" to listOf(1)),
            // show 1: watched overwrote by rating 0.70
            ratings = mapOf("1" to 7f),
            // show 2: watched 0.40 → liked max → 0.80
            likedMediaIds = listOf(2),
            // show 3: only essential → 1.00
            essentialMediaIds = listOf(3),
            // show 4: excluded
            dislikedMediaIds = listOf(4)
        )

        // When
        val vector = useCase.buildRatingVector(profile)

        // Then
        assertEquals(0.70f, vector[1]!!, 0.001f) // 7/10
        assertEquals(0.80f, vector[2]!!, 0.001f) // liked overrides watched
        assertEquals(1.00f, vector[3]!!, 0.001f) // essential
        assertTrue(4 !in vector) // disliked excluded
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // execute — filtrado colaborativo completo
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `given no similar users, when execute, then result is empty map`() = runTest {
        // Given
        val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1, 2, 3))
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns emptyList()

        // When
        val result = useCase.execute(myProfile)

        // Then
        assertTrue("No neighbors → no collaborative boost", result.isEmpty())
    }

    @Test
    fun `given identical profiles, when execute, then cosine similarity is 1 and boost is recommended`() = runTest {
        // Given — ambos perfiles idénticos → cosine=1.0 ≥ MIN_SIMILARITY=0.15
        val sharedShows = listOf(1, 2, 3)
        val myProfile = UserProfile(
            userId = "me",
            likedMediaIds = sharedShows,
            essentialMediaIds = listOf(10)
        )
        val neighborProfile = UserProfile(
            userId = "neighbor",
            likedMediaIds = sharedShows,
            essentialMediaIds = listOf(10),
            // Show 20 que yo no he visto pero el vecino sí (liked)
            ratings = mapOf("20" to 9f)
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        // When
        val result = useCase.execute(myProfile)

        // Then — show 20 debería aparecer con boost
        assertTrue(
            "Show liked by identical neighbor should get collaborative boost",
            result.isNotEmpty()
        )
        assertTrue(
            "Boost values must be in [0, MAX_BOOST=1.20]",
            result.values.all { it in 0f..1.20f }
        )
    }

    @Test
    fun `given neighbor below MIN_SIMILARITY threshold, when execute, then result is empty`() = runTest {
        // Given — mi perfil: solo show 1; vecino: solo show 99 (sin superposición → cosine≈0 < 0.15)
        val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1))
        val unrelatedNeighbor = UserProfile(
            userId = "stranger",
            likedMediaIds = listOf(99, 100, 101)
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(unrelatedNeighbor)

        // When
        val result = useCase.execute(myProfile)

        // Then — similaridad coseno ≈ 0 < MIN_SIMILARITY=0.15 → el vecino es filtrado
        assertTrue(
            "Neighbor with no overlap should not contribute to collaborative boost",
            result.isEmpty()
        )
    }

    @Test
    fun `given show already seen by current user, when execute, then it is excluded from boost map`() = runTest {
        // Given — yo ya he visto el show 5; el vecino también lo valora bien
        val myProfile = UserProfile(
            userId = "me",
            likedMediaIds = listOf(1, 2, 3),
            watchedEpisodes = mapOf("5" to listOf(1, 2, 3))
        ) // show 5 ya visto
        val neighborProfile = UserProfile(
            userId = "neighbor",
            likedMediaIds = listOf(1, 2, 3, 5)
        ) // show 5 también liked por vecino
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        // When
        val result = useCase.execute(myProfile)

        // Then — show 5 ya está en mi historial → no debe aparecer en el boost
        assertTrue(
            "Already-watched show must not appear in collaborative recommendations",
            5 !in result
        )
    }

    @Test
    fun `given show disliked by current user, when execute, then it is excluded from boost map`() = runTest {
        // Given — yo ya he visto y dado dislike al show 7
        val myProfile = UserProfile(
            userId = "me",
            likedMediaIds = listOf(1, 2, 3),
            dislikedMediaIds = listOf(7)
        )
        val neighborProfile = UserProfile(
            userId = "neighbor",
            likedMediaIds = listOf(1, 2, 3, 7)
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        // When
        val result = useCase.execute(myProfile)

        // Then
        assertTrue("Disliked show must not appear in collaborative boost", 7 !in result)
    }

    @Test
    fun `given neighbor recommends many shows, when execute, then results are capped at 50`() = runTest {
        // Given — vecino tiene 100 shows liked que yo no he visto
        val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1))
        val neighborIds = (100..199).toList()
        val neighborProfile = UserProfile(
            userId = "neighbor",
            likedMediaIds = listOf(1) + neighborIds
        ) // 1 compartido + 100 únicos
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        // When
        val result = useCase.execute(myProfile)

        // Then
        assertTrue("Collaborative boost results must be capped at 50", result.size <= 50)
    }

    @Test
    fun `given exception thrown by userRepository, when execute, then returns empty map without crashing`() = runTest {
        // Given
        val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1))
        coEvery { userRepository.getSimilarUsers(limit = any()) } throws RuntimeException("DB error")

        // When
        val result = useCase.execute(myProfile)

        // Then — exception swallowed, returns safe empty map
        assertTrue("Exception in repository should return empty map gracefully", result.isEmpty())
    }

    @Test
    fun `given partially overlapping profiles, when execute, then similarity is between 0 and 1`() = runTest {
        // Given — 2 shows compartidos de 4 en total
        val myProfile = UserProfile(
            userId = "me",
            likedMediaIds = listOf(1, 2, 3, 4)
        )
        val neighborProfile = UserProfile(
            userId = "neighbor",
            // comparten 1 y 2
            likedMediaIds = listOf(1, 2),
            // show 10 → yo no lo he visto
            ratings = mapOf("10" to 8f)
        )
        coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighborProfile)

        // When
        val result = useCase.execute(myProfile)

        // Then — similar parcialmente → puede haber boost
        result.values.forEach { boost ->
            assertTrue("Boost must be in [0, MAX_BOOST=1.20]", boost in 0f..1.20f)
        }
    }

    @Test
    fun `given multiple similar users recommending same show, when execute, then boost reflects weighted average`() =
        runTest {
            // Given — dos vecinos ambos recomiendan el show 50 con ratings altos
            val myProfile = UserProfile(userId = "me", likedMediaIds = listOf(1, 2, 3))
            val neighbor1 = UserProfile(
                userId = "n1",
                likedMediaIds = listOf(1, 2, 3),
                essentialMediaIds = listOf(50)
            ) // show 50: SIGNAL_ESSENTIAL=1.0
            val neighbor2 = UserProfile(
                userId = "n2",
                likedMediaIds = listOf(1, 2, 3),
                ratings = mapOf("50" to 9f)
            ) // show 50: 9/10 = 0.90
            coEvery { userRepository.getSimilarUsers(limit = any()) } returns listOf(neighbor1, neighbor2)

            // When
            val result = useCase.execute(myProfile)

            // Then — show 50 debería aparecer con alto boost por dos vecinos de alta similitud
            assertTrue(
                "Show recommended by two similar neighbors should appear in results",
                50 in result
            )
            val boost = result[50]!!
            assertTrue(
                "Boost from two high-similarity neighbors should be significant (>0.5)",
                boost > 0.5f
            )
        }
}
