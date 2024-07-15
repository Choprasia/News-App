package com.example.basiscodelab.data

data class News(
    val status: String,
    val totalResult: Int,
    val articles: List<Article>
)