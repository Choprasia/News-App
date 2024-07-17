package com.example.basiscodelab.vm
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.basiscodelab.repository.NewsRepository

class NewsViewModelFactory(private val context: Context,private val repository: NewsRepository) : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(repository,context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }}

