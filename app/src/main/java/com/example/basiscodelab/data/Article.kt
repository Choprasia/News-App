package com.example.basiscodelab.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "articles"
)

 data class Article(
    val author: String?,
    val source: Source,
    val description: String?,
    val url: String?,
    val urlToImage: String?,
    val publishedAt: String,
    @PrimaryKey(autoGenerate = false)
    val title: String,
    val content: String?
) : Serializable

