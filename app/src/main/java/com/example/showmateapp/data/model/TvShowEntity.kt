package com.example.showmateapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.showmateapp.data.network.TvShow

@Entity(tableName = "tv_shows")
data class TvShowEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String,
    val category: String // "popular", "trending", "daily_pick"
)

// Extension function to map network model to entity
fun TvShow.toEntity(category: String): TvShowEntity {
    return TvShowEntity(
        id = this.id,
        name = this.name,
        overview = this.overview,
        posterPath = this.posterPath ?: "",
        category = category
    )
}

// Extension function to map entity to network model (used internally in our app)
fun TvShowEntity.toDomain(): TvShow {
    return TvShow(
        id = this.id,
        name = this.name,
        overview = this.overview,
        posterPath = this.posterPath
    )
}