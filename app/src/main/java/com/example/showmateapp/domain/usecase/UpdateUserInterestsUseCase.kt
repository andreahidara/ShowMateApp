package com.example.showmateapp.domain.usecase;

import com.example.showmateapp.data.repository.FirestoreRepository;

// Esta clase es el "puente" entre la pantalla y la base de datos
class UpdateUserInterestsUseCase (
        private val firestoreRepository: FirestoreRepository
) {
    // El operador invoke permite llamar a la clase como si fuera una función
    // Ejemplo: updateUserInterestsUseCase(userId, genreId)
    operator fun invoke(userId: String, genreId: String) {
        if (userId.isNotEmpty() && genreId.isNotEmpty()) {
            // Asegúrate de que en FirestoreRepository la función se llame exactamente así:
            firestoreRepository.incrementGenreScore(userId, genreId)
        }
    }
}