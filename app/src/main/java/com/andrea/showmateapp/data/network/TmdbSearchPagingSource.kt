package com.andrea.showmateapp.data.network

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.andrea.showmateapp.domain.usecase.GetRecommendationsUseCase

/**
 * PagingSource para búsqueda paginada de series en TMDB.
 * Aplica el scoring de recomendaciones en cada página cargada.
 */
class TmdbSearchPagingSource(
    private val apiService: TmdbApiService,
    private val query: String,
    private val getRecommendationsUseCase: GetRecommendationsUseCase
) : PagingSource<Int, MediaContent>() {

    override fun getRefreshKey(state: PagingState<Int, MediaContent>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaContent> {
        val page = params.key ?: 1
        return try {
            val response = apiService.searchMedia(query = query.trim(), page = page)
            val scored = getRecommendationsUseCase.scoreShows(response.results)
            LoadResult.Page(
                data = scored,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page >= response.total_pages) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
