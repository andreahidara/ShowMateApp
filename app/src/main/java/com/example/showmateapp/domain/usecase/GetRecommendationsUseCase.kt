package com.example.showmateapp.domain.usecase

import com.example.showmateapp.data.network.TvShow
import com.example.showmateapp.data.repository.TvShowRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GetRecommendationsUseCase @Inject constructor(
        private val tvShowRepository: TvShowRepository
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun execute(): List<TvShow> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            val genreScores = userDoc.get("genreScores") as? Map<String, Long> ?: emptyMap()

            if (genreScores.isEmpty()) {
                return tvShowRepository.getPopularTvShows()
            }

            val favoriteGenreId = genreScores.maxByOrNull { it.value }?.key ?: return tvShowRepository.getPopularTvShows()

            tvShowRepository.getTvShowsByGenres(favoriteGenreId)

        } catch (e: Exception) {
            tvShowRepository.getPopularTvShows()
        }
    }
}