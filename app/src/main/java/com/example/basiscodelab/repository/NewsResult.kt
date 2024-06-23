package com.example.basiscodelab.repository

sealed class NewsResult<out T> {
    data class Success<out T>(val data: T) : NewsResult<T>()
    data class Error(val message: String) : NewsResult<Nothing>()
    data object Loading : NewsResult<Nothing>()
}