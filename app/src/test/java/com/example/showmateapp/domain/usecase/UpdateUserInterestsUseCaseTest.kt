package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.network.CastMember
import com.example.showmateapp.data.network.CreditsResponse
import com.example.showmateapp.data.network.Keyword
import com.example.showmateapp.data.network.KeywordsResponse
import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class UpdateUserInterestsUseCaseTest {

    private val userRepository: UserRepository = mock()
    private lateinit var useCase: UpdateUserInterestsUseCase

    private val mediaWithAll = MediaContent(
        id = 1,
        name = "Breaking Bad",
        genreIds = listOf(18, 80),
        keywords = KeywordsResponse(listOf(Keyword(1, "drugs"), Keyword(2, "crime"))),
        credits = CreditsResponse(listOf(CastMember(id = 17419, name = "Bryan Cranston")))
    )

    @Before
    fun setup() {
        useCase = UpdateUserInterestsUseCase(userRepository)
    }

    @Test
    fun `isPositive true calls trackMediaInteraction with Like`() = runTest {
        useCase.execute(mediaWithAll, isPositive = true)

        verify(userRepository).trackMediaInteraction(
            mediaId = 1,
            genres = listOf("18", "80"),
            keywords = listOf("drugs", "crime"),
            actors = listOf(17419),
            interactionType = UserRepository.InteractionType.Like
        )
    }

    @Test
    fun `isPositive false calls trackMediaInteraction with Dislike`() = runTest {
        useCase.execute(mediaWithAll, isPositive = false)

        verify(userRepository).trackMediaInteraction(
            mediaId = 1,
            genres = listOf("18", "80"),
            keywords = listOf("drugs", "crime"),
            actors = listOf(17419),
            interactionType = UserRepository.InteractionType.Dislike
        )
    }

    @Test
    fun `media with no keywords sends empty keyword list`() = runTest {
        val noKeywords = MediaContent(id = 2, name = "Test", genreIds = listOf(28))

        useCase.execute(noKeywords, isPositive = true)

        verify(userRepository).trackMediaInteraction(
            mediaId = 2,
            genres = listOf("28"),
            keywords = emptyList(),
            actors = emptyList(),
            interactionType = UserRepository.InteractionType.Like
        )
    }

    @Test
    fun `media with null genreIds sends empty genre list`() = runTest {
        val noGenres = MediaContent(id = 3, name = "Test", genreIds = null)

        useCase.execute(noGenres, isPositive = true)

        verify(userRepository).trackMediaInteraction(
            mediaId = 3,
            genres = emptyList(),
            keywords = emptyList(),
            actors = emptyList(),
            interactionType = UserRepository.InteractionType.Like
        )
    }

    @Test
    fun `multiple actors are all passed to trackMediaInteraction`() = runTest {
        val multiActor = MediaContent(
            id = 4,
            name = "Ensemble",
            genreIds = listOf(35),
            credits = CreditsResponse(listOf(
                CastMember(id = 1, name = "Actor A"),
                CastMember(id = 2, name = "Actor B"),
                CastMember(id = 3, name = "Actor C")
            ))
        )

        useCase.execute(multiActor, isPositive = true)

        verify(userRepository).trackMediaInteraction(
            mediaId = 4,
            genres = listOf("35"),
            keywords = emptyList(),
            actors = listOf(1, 2, 3),
            interactionType = UserRepository.InteractionType.Like
        )
    }
}
