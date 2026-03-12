package com.example.showmateapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.showmateapp.data.network.MediaContent

@Entity(tableName = "media_content", primaryKeys = ["id", "category"])
data class MediaEntity(
    val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String,
    val category: String // "popular", "trending", "recommended"
)

// Extension function to map network model to entity
fun MediaContent.toEntity(category: String): MediaEntity {
    return MediaEntity(
        id = this.id,
        name = this.name,
        overview = this.overview,
        posterPath = this.posterPath ?: "",
        category = category
    )
}

// Extension function to map entity to network model (used internally in our app)
fun MediaEntity.toDomain(): MediaContent {
    return MediaContent(
        id = this.id,
        name = this.name,
        overview = this.overview,
        posterPath = this.posterPath
    )
}