package com.example.basiscodelab.repository

import android.util.Log
import com.example.basiscodelab.data.Article
import com.example.basiscodelab.data.News
import com.example.basiscodelab.network.NewsService
import com.example.basiscodelab.db.ArticleDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext


class NewsRepository(private val db: ArticleDatabase) {
    private val _newsResp = MutableSharedFlow<NewsResult<List<Article>>>()
    val newsResp: SharedFlow<NewsResult<List<Article>>> = _newsResp


    suspend fun getHeadlines(country: String, page: Int) {
        _newsResp.emit(NewsResult.Loading)
        withContext(Dispatchers.IO) {

            val cachedArticles = db.getArticleDAO().getAllArticles()
            if (!cachedArticles.isNullOrEmpty()) {
                _newsResp.emit(NewsResult.Success(cachedArticles))
            }

            try {
                val call = NewsService.newsInstance.getHeadLines(country, page)
                val response = call.execute()
                if (response.isSuccessful) {
                    Log.d("BasisCodelab", "success in fetching news")
                    val news: News? = response.body()
                    news?.let {
                        for (article in it.articles) {
                            Log.d("BasisCodelab", "article $article")
                            db.getArticleDAO().upsert(article)
                        }
                        _newsResp.emit(NewsResult.Success(it.articles))

                    }
                    if (news == null) {
                        _newsResp.emit(NewsResult.Error("Exception: News is null"))
                    }
                    if (news?.articles?.isEmpty() == true) {
                        _newsResp.emit(NewsResult.Error("Exception: News articles is empty"))
                    }
                    //  _newsData.value = news?.articles ?: emptyList()
                } else {
                    Log.d("BasisCodelab", "error in fetching news")
                    _newsResp.emit(NewsResult.Error("Exception:${response.message()}"))
                }
            } catch (e: Exception) {
                Log.d("BasisCodelab", "error in fetching news")
                _newsResp.emit(NewsResult.Error("Exception:${e.message}"))
            }

        }
    }
    suspend fun searchNews(searchQuery: String, page: Int) =
        NewsService.newsInstance.searchNews(searchQuery, page)

    suspend fun upsert(article: Article) = db.getArticleDAO().upsert(article)

    fun getFavoriteNews() = db.getArticleDAO().getAllArticles()

    fun getAllArticles() = db.getArticleDAO().getAllArticles()

    suspend fun getArticleByTitle(title: String) : NewsResult<Article> {
        val article = db.getArticleDAO().getArticleByTitle(title)
        return article?.let {
            NewsResult.Success(article)
        } ?: NewsResult.Error("Article not found")
    }
    suspend fun deleteArticle(article: Article) =
        db.getArticleDAO().deleteArticle(article)
}
