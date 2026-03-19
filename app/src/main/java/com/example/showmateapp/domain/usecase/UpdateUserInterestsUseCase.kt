package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import javax.inject.Inject

class UpdateUserInterestsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend fun execute(media: MediaContent, isPositive: Boolean) {
        val genreIds = media.safeGenreIds.map { it.toString() }
        val keywordNames = media.keywords?.results?.map { it.name } ?: emptyList()
        val actorIds = media.credits?.cast?.map { it.id } ?: emptyList()

        val interactionType = if (isPositive) UserRepository.InteractionType.Like else UserRepository.InteractionType.Dislike

        userRepository.trackMediaInteraction(
            mediaId = media.id,
            genres = genreIds,
            keywords = keywordNames,
            actors = actorIds,
            interactionType = interactionType
        )
    }
}
