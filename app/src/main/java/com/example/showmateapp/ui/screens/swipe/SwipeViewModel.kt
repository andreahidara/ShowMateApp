package com.example.showmateapp.ui.screens.swipe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.showmateapp.data.network.Movie
import com.example.showmateapp.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SwipeViewModel : ViewModel() {
    private val _shows = MutableStateFlow<List<Movie>>(emptyList())
    val shows: StateFlow<List<Movie>> = _shows

    private val token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJjYzFhY2NmNTU5Mzk4YTNmNDdiMWZhMzYyNTIwY2UyYiIsIm5iZiI6MTc3MTk1MDk1OC4xMjYwMDAyLCJzdWIiOiI2OTlkZDM2ZWJjM2YzZDFkNjUwNjYwMjYiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.Deix25uQqXJ623nkHJJWhLBz2Ga4ouCv1iK9PT5iSM8"

    fun removeTopShow() {
        val currentList = _shows.value.toMutableList()
        if (currentList.isNotEmpty()) {
            currentList.removeAt(0)
            _shows.value = currentList
        }
    }

    fun loadShows(genreIds: String) {
        viewModelScope.launch {
            try {
                // Llamamos a la API usando los IDs numéricos directamente
                val response = RetrofitClient.apiService.getShowsByGenres(token, genreIds)
                _shows.value = response.results
            } catch (e: Exception) {
                Log.e("API_ERROR", "Fallo al cargar series: ${e.message}")
            }
        }
    }
}