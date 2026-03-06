package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.repository.FirestoreRepository
import javax.inject.Inject

class UpdateUserInterestsUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {
    /**
     * @param tvShow La serie con la que el usuario ha interactuado.
     * @param isPositive True si es un Like/Favorito, False si es un Dislike/Penalización.
     */
    suspend fun execute(tvShow: TvShow, isPositive: Boolean) {
        val genreIds = tvShow.safeGenreIds.map { it.toString() }
        val keywordIds = tvShow.keywords?.results?.map { it.id.toString() } ?: emptyList()
        val actorIds = tvShow.credits?.cast?.map { it.id } ?: emptyList()

        firestoreRepository.updateUserInterests(
            genres = genreIds,
            keywords = keywordIds,
            actors = actorIds,
            isPositive = isPositive
        )
    }
}
