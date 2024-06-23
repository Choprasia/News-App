package com.example.basiscodelab.data

import com.example.basiscodelab.data.Article

data class News(
    val status: String,
    val totalResult: Int,
    val articles: List<Article>
)