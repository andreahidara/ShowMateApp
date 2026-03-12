package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import javax.inject.Inject

class UpdateUserInterestsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * @param media La serie con la que el usuario ha interactuado.
     * @param isPositive True si es un Like/Favorito, False si es un Dislike/Penalización.
     */
    suspend fun execute(media: MediaContent, isPositive: Boolean) {
        val genreIds = media.safeGenreIds.map { it.toString() }
        val keywordIds = media.keywords?.results?.map { it.id.toString() } ?: emptyList()
        val actorIds = media.credits?.cast?.map { it.id } ?: emptyList()

        val interactionType = if (isPositive) UserRepository.InteractionType.Like else UserRepository.InteractionType.Dislike

        userRepository.trackMediaInteraction(
            mediaId = media.id,
            genres = genreIds,
            keywords = keywordIds,
            actors = actorIds,
            interactionType = interactionType
        )
    }
}
