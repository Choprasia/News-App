package com.example.basiscodelab.vm

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basiscodelab.MainActivity
import com.example.basiscodelab.data.Article
import com.example.basiscodelab.phi.GenAIException
import com.example.basiscodelab.phi.GenAIWrapper
import com.example.basiscodelab.repository.NewsRepository
import com.example.basiscodelab.repository.NewsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NewsViewModel constructor(val repository: NewsRepository, context: Context) : ViewModel(),
    GenAIWrapper.TokenUpdateListener {


    private val _newsData: MutableLiveData<NewsResult<List<Article>>> =
        MutableLiveData(NewsResult.Loading)
    val newsData: LiveData<NewsResult<List<Article>>> get() = _newsData

    private val _singleArticle: MutableLiveData<Article> = MutableLiveData(null)
    val singleArticle: LiveData<Article> get() = _singleArticle

    private val _summarizedArticle = MutableLiveData<String>()
    val summarizedArticle: LiveData<String> get() = _summarizedArticle

    private var genAIWrapper: GenAIWrapper? = null

    init {
        viewModelScope.launch {
            try {
                downloadModels(context)
                //  genAIWrapper = createGenAIWrapper(context)
            } catch (e: GenAIException) {
                throw RuntimeException(e)
            }
        }
    }

    fun triggersummaryofarticles(articles: List<Article>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                genAIWrapper?.let { wrapper ->
                    val titles= articles.take(1).joinToString("\n") { article ->
                        " - ${article.title}"
                    }
                  /*  val descriptions = articles.take(1).joinToString("\n") { article ->
                        " - ${article.description}"

                    }*/
                    val prompt = """
               Categorize the following news article titles and provide a summary for that category.: 
               Summarize the main points for that category in a sentences:
                   ``` News Article Descriptions:$titles```
                    """.trimIndent()


                    Log.d(TAG, "Prompt length:${prompt.length}")

                    wrapper.run(prompt)
                    Log.d(TAG, "Prompt sent to GenAIWrapper : $prompt")

                }
            }
        }
    }

    @Throws(GenAIException::class)
    fun createGenAIWrapper(context: Context): GenAIWrapper {
        val wrapper = GenAIWrapper(context.filesDir.path)
        wrapper.setTokenUpdateListener(this)
        return wrapper
    }

    override fun onTokenUpdate(token: String) {
        Log.d(MainActivity.TAG, "Received token update: $token")
        val currentsummary = _summarizedArticle.value ?: ""
        val updatedSummary = currentsummary + token
         _summarizedArticle.postValue(updatedSummary)

    }

    @Throws(GenAIException::class)
    private fun downloadModels(context: Context) {
        val urlFilePairs = listOf(
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/added_tokens.json?download=true",
                "added_tokens.json"
            ), Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/config.json?download=true",
                "config.json"
            ), Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/configuration_phi3.py?download=true",
                "configuration_phi3.py"
            ), Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/genai_config.json?download=true",
                "genai_config.json"
            ), Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx?download=true",
                "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx"
            ), Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data?download=true",
                "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data"
            ), Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/special_tokens_map.json?download=true",
                "special_tokens_map.json"
            ), Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/tokenizer.json?download=true",
                "tokenizer.json"
            ), Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/tokenizer.model?download=true",
                "tokenizer.model"
            ), Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/tokenizer_config.json?download=true",
                "tokenizer_config.json"
            )
        )
        Toast.makeText(
            context,
            "Downloading model for the app... Model Size greater than 2GB, please allow a few minutes to download.",
            Toast.LENGTH_SHORT
        ).show()
        for (i in urlFilePairs.indices) {
            val index = i
            val (url, fileName) = urlFilePairs[index]
            if (fileExists(context, fileName)) {
                Toast.makeText(
                    context,
                    "File already exists. Skipping Download.",
                    Toast.LENGTH_SHORT
                )
                    .show()

                Log.d(MainActivity.TAG, "File $fileName already exists. Skipping download.")
                genAIWrapper = createGenAIWrapper(context)
                break
            }
        }
    }

    private fun fileExists(context: Context, fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists()
    }

    override fun onCleared() {
        try {
            genAIWrapper?.close()
        } catch (e: Exception) {
            Log.e(MainActivity.TAG, "exception from closing genAIWrapper", e)
        }
        genAIWrapper = null
        super.onCleared()
    }


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
                        _singleArticle.value = newArticle
                        Log.i(
                            "BasisCodelab",
                            "Single article fetched successfully:${newArticle.title}"
                        )
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
    }
}




