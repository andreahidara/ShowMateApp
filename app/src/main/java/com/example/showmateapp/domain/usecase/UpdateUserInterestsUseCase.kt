package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.network.MediaContent
import com.example.showmateapp.data.repository.UserRepository
import com.example.showmateapp.util.NarrativeStyleMapper
import javax.inject.Inject

class UpdateUserInterestsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend fun execute(media: MediaContent, isPositive: Boolean) {
        val interactionType = if (isPositive) UserRepository.InteractionType.Like else UserRepository.InteractionType.Dislike
        userRepository.trackMediaInteraction(
            mediaId = media.id,
            genres = media.safeGenreIds.map { it.toString() },
            keywords = media.keywordNames,
            actors = media.credits?.cast?.map { it.id } ?: emptyList(),
            creators = media.creatorIds,
            narrativeStyles = NarrativeStyleMapper.extractStyles(media.keywordNames, media.episodeRunTime?.firstOrNull()),
            interactionType = interactionType
        )
    }
}
