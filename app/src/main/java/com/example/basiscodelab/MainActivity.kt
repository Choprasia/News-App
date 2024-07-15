package com.example.basiscodelab

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.example.basiscodelab.data.Article
import com.example.basiscodelab.db.ArticleDatabase
import com.example.basiscodelab.phi.GenAIException
import com.example.basiscodelab.phi.GenAIWrapper
import com.example.basiscodelab.repository.NewsRepository
import com.example.basiscodelab.repository.NewsResult
import com.example.basiscodelab.ui.theme.BasisCodelabTheme
import com.example.basiscodelab.vm.NewsViewModel
import com.example.basiscodelab.vm.NewsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity(), GenAIWrapper.TokenUpdateListener {

    private lateinit var newsViewModel: NewsViewModel
    private var genAIWrapper: GenAIWrapper? = null

    companion object {
        private const val TAG = "basiscodelab.MainActivity"
        private fun fileExists(context: Context, fileName: String): Boolean {
            val file = File(context.filesDir, fileName)
            return file.exists()
        }
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = ArticleDatabase(this)
        val repository = NewsRepository(db)
        val newsViewModelFactory = NewsViewModelFactory(repository)
        newsViewModel = ViewModelProvider(this, newsViewModelFactory).get(NewsViewModel::class.java)
        Log.d("BasisCodelab", "onCreate")
        enableEdgeToEdge()
        setContent {
            BasisCodelabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp(modifier = Modifier.fillMaxSize())
                }
                LaunchedEffect(Unit) {
                    try {
                        downloadModels(applicationContext)
                    } catch (e: GenAIException) {
                        throw RuntimeException(e)
                    }
                }
            }

        }
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun MyApp(
        modifier: Modifier = Modifier
    ) {
        var fetchedNewsState: List<Article> by rememberSaveable { mutableStateOf(emptyList()) }
        Surface(modifier) {
            if (fetchedNewsState.isEmpty()) {
                OnboardingScreen(onContinueClicked = {
                    Log.d("DBG", "")
                    fetchNewsData { news ->
                        fetchedNewsState = news
                    }
                })
            } else {
                Nav(fetchedNewsState)
            }
        }
    }
    override fun onDestroy() {
        try {
            genAIWrapper?.close()
        } catch (e: Exception) {
            Log.e(TAG, "exception from closing genAIWrapper", e)
        }
        genAIWrapper = null
        super.onDestroy()
    }
    @Throws(GenAIException::class)
    private fun downloadModels(context: Context) {
        val urlFilePairs = listOf(
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/added_tokens.json?download=true",
                "added_tokens.json"
            ),
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/config.json?download=true",
                "config.json"
            ),
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/configuration_phi3.py?download=true",
                "configuration_phi3.py"
            ),
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/genai_config.json?download=true",
                "genai_config.json"
            ),
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx?download=true",
                "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx"
            ),
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data?download=true",
                "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data"
            ),
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/special_tokens_map.json?download=true",
                "special_tokens_map.json"
            ),
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/tokenizer.json?download=true",
                "tokenizer.json"
            ),
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/tokenizer.model?download=true",
                "tokenizer.model"
            ),
            Pair(
                "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/tokenizer_config.json?download=true",
                "tokenizer_config.json"
            )
        )
        Toast.makeText(
            this,
            "Downloading model for the app... Model Size greater than 2GB, please allow a few minutes to download.",
            Toast.LENGTH_SHORT
        ).show() // val executor: ExecutorService = Executors.newSingleThreadExecutor()
        for (i in urlFilePairs.indices) {
            val index = i
            val (url, fileName) = urlFilePairs[index]
            if (fileExists(context, fileName)) {
                // Display a message using Toast
                Toast.makeText(this, "File already exists. Skipping Download.", Toast.LENGTH_SHORT)
                    .show()

                Log.d(TAG, "File $fileName already exists. Skipping download.")
                genAIWrapper = createGenAIWrapper()
                break
            }
        }
    }
    @Throws(GenAIException::class)
    private fun createGenAIWrapper(): GenAIWrapper {
        val wrapper = GenAIWrapper(filesDir.path)
        wrapper.setTokenUpdateListener(this)
        return wrapper
    }
    override fun onTokenUpdate(token: String) {
        Log.d(TAG, "Received token update: $token")
    }
    fun categorizeAndSummarizeNews(newsText: String) {
        genAIWrapper?.let { wrapper ->
            val prompt = "Categorize the following news article and provide a summary:\n\n" +
                    "News Article:\n$newsText\n\n" +
                    "Categories:\n" +
                    "1. Politics\n" +
                    "2. Technology\n" +
                    "3. Business\n" +
                    "4. Sports\n" +
                    "5. Health\n\n" +
                    "6. Others\n\n" +
                    "Summarize the main points in a sentences:".trimIndent()

            wrapper.run(prompt)
            Log.d(TAG, "Prompt sent to GenAIWrapper: $prompt")
        }
    }
    fun articleSummarization(article: Article) {
        article.description?.let { description ->
            Log.i(TAG, "This is the description: $description")
            genAIWrapper?.let {
                Log.i(TAG, "GenAIWrapper is not null")
                categorizeAndSummarizeNews(description)
                Log.i(TAG, "GenAIWrapper called")
            }
        }
    }
    private fun fetchNewsData(fetchedNews: (List<Article>) -> Unit) {
        newsViewModel.getNews("in", 1)
        newsViewModel.newsData.observe(this) {
            when (it) {
                is NewsResult.Loading -> {
                    Toast.makeText(this, "Loading", Toast.LENGTH_SHORT).show()
                }

                is NewsResult.Success -> {
                    fetchedNews(it.data)

                }

                is NewsResult.Error -> {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
            }

        }
    }
    @Composable
    fun Nav(list: List<Article>) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "newslist") {
            composable(route = "newslist") {

                NewsList(list = list,
                    onClick = { article ->
                        navController.navigate("articleview/${article.title}")
                    },
                    onSummarizeClick = {
                        navController.navigate("summarizeView")
                    })
            }

            composable(route = "articleview/{articleTitle}") { backStackEntry ->
                val articleTitle = backStackEntry.arguments?.getString("articleTitle")


                ArticleDetailedView(
                    navController = navController,
                    articleTitle = articleTitle,
                    newsViewModel = newsViewModel
                )
            }
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ArticleDetailedView(
        modifier: Modifier = Modifier,
        navController: NavController,
        articleTitle: String?,
        newsViewModel: NewsViewModel

    ) {
        val articleState by newsViewModel.singleArticle.observeAsState()
        if (articleState == null)
            newsViewModel.getSingleArticle(articleTitle ?: "")
        LaunchedEffect(articleState) {
            Log.i("BasisCodelab", "Article state updated: $articleState")
            if (articleState != null) {
                withContext(Dispatchers.IO) {
                    articleSummarization(articleState!!)

                    Log.i("BasisCodelab", "Article processed:${articleState?.title}")
                }
            }
        }
        articleState?.let { article ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopAppBar(
                    title = { Text(text = "Back") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")

                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                article.urlToImage?.let {
                    Image(
                        painter = rememberImagePainter(data = it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(256.dp)
                            .padding(8.dp)
                    )
                }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.description ?: "",
                    style = MaterialTheme.typography.bodyLarge.copy(

                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
                article.url?.let {
                    Text(

                        text = "URL:$it ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            startActivity(intent, null)
                        }
                    )
                }
            }
        } ?: run {
            Text(text = "Loading")
        }
    }
    @Composable
    fun OnboardingScreen(
        onContinueClicked: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to Daily News!")
            Button(
                modifier = Modifier.padding(vertical = 24.dp),
                onClick = onContinueClicked
            ) {
                Text("Continue")
            }
        }
    }
    @Composable
     fun NewsList(
        list: List<Article>,
        onClick: (Article) -> Unit,
        onSummarizeClick:()-> Unit,
        modifier: Modifier = Modifier,

    ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp))
            Text(

                text = "NEWS HEADLINES",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold),
                modifier=Modifier.padding(horizontal = 12.dp,vertical=8.dp)
            )
                Row(
                    modifier=Modifier
                     //   .fillMaxWidth()
                        .padding(horizontal=12.dp,vertical = 4.dp),
                    //horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ){
                    Button(
                        onClick= { onSummarizeClick() },
                        modifier = Modifier.padding(8.dp)
                    ){
                        Text("Summarize")
                    }
                }
            LazyColumn(modifier = modifier.padding(vertical = 4.dp)) {
                items(items = list) { article ->
                    ArticleView(article.title,
                        article.publishedAt,
                        article.description ?: "",
                        imageUrl = article.urlToImage,
                        modifier = Modifier.clickable { onClick(article) })
                }
            }
        }
    }
    fun convertTimestampToDate(publisedAt: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val date = sdf.parse(publisedAt)
        val outputsdf = SimpleDateFormat("EEEE MMMM dd,yyyy", Locale.getDefault())
        return outputsdf.format(date)
    }
    fun getDaysDifference(publisedAt: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val date = sdf.parse(publisedAt)
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - date.time

        return when {
            diff < 86400000 -> "Today"
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days day(s) ago"
            }
        }
    }
    @Composable
    private fun ArticleView(

        title: String,
        publisedAt: String,
        description: String,
        imageUrl: String?,
        modifier: Modifier = Modifier
    ) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {


                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp

                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = convertTimestampToDate(publisedAt),
                                style = MaterialTheme.typography.bodySmall.copy(

                                )
                            )

                            Text(
                                text = getDaysDifference(publisedAt),
                                style = MaterialTheme.typography.bodySmall.copy(

                                )
                            )
                        }

                    }

                }
                imageUrl?.let {
                    Image(
                        painter = rememberImagePainter(data = it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(128.dp)
                    )
                }

            }
        }
    }
}








