package com.example.basiscodelab.vm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basiscodelab.data.Article
import com.example.basiscodelab.repository.NewsRepository
import com.example.basiscodelab.repository.NewsResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NewsViewModel constructor(val repository: NewsRepository) : ViewModel() {


    private val _newsData: MutableLiveData<NewsResult<List<Article>>> =
        MutableLiveData(NewsResult.Loading)
    val newsData: LiveData<NewsResult<List<Article>>> get() = _newsData

    private val _singleArticle: MutableLiveData<Article> = MutableLiveData(null)
    val singleArticle: LiveData<Article> get() = _singleArticle

    fun getNews(country: String, page: Int) {
        Log.d("BasisCodelab", "getNews called")
        viewModelScope.launch {
            try {
                _newsData.value = NewsResult.Loading
                repository.newsResp.collectLatest {
                    _newsData.value = it
                }
            } catch (e: Exception) {
                _newsData.value = NewsResult.Error(e.message ?: "Unknown error")
            }
        }
        viewModelScope.launch {
            repository.getHeadlines(country, page)

        }
    }
    fun getSingleArticle(title: String) {
        viewModelScope.launch {
            try {
                when (val articleResult = repository.getArticleByTitle(title)) {
                    is NewsResult.Success -> {
                        val newArticle = articleResult.data

                        if (_singleArticle.value != newArticle) {
                            _singleArticle.value = newArticle
                            Log.i("BasisCodelab", "Single article fetched successfully")
                        }

                    }

                    is NewsResult.Error -> {
                        Log.e("BasisCodelab", "Error in fetching single article")
                    }

                    NewsResult.Loading -> {
                        Log.d("BasisCodelab", "Loading in fetching single article")
                    }
                }

            } catch (e: Exception) {
                Log.e("BasisCodelab", "Error in fetching single article:${e.message}")
            }

        }
    }}

