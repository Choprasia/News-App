package com.example.basiscodelab

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
import com.example.basiscodelab.repository.NewsRepository
import com.example.basiscodelab.repository.NewsResult
import com.example.basiscodelab.ui.theme.BasisCodelabTheme
import com.example.basiscodelab.vm.NewsViewModel
import com.example.basiscodelab.vm.NewsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    private lateinit var newsViewModel: NewsViewModel

    companion object {
        const val TAG = "basiscodelab.MainActivity"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = ArticleDatabase(this)
        val repository = NewsRepository(db)
        val newsViewModelFactory = NewsViewModelFactory(this, repository)
        newsViewModel = ViewModelProvider(this, newsViewModelFactory).get(NewsViewModel::class.java)
        Log.d("BasisCodelab", "onCreate")
        enableEdgeToEdge()
        setContent {

            BasisCodelabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    MyApp(modifier = Modifier.fillMaxSize())
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SummarizeViewScreen(navController: NavController, viewModel: NewsViewModel,articles: List<Article>) {
        val summarizedArticle by viewModel.summarizedArticle.observeAsState()
        Log.d("BasisCodelab", "SummarizeViewScreen: $summarizedArticle")


        LaunchedEffect(Unit) {
            viewModel.triggersummaryofarticles(articles)
            Log.d("BasisCodelab", "triggersummaryofarticles: $articles")
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(title = { Text(text = "Back") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")

                }
            }, modifier = Modifier.fillMaxWidth()
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(

                    text = "Summary of the day", style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ))
                Spacer(modifier = Modifier.height(16.dp))
                if (summarizedArticle != null) {
                    Text(
                        text = summarizedArticle!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Summarizing...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
    fun Nav(list: List<Article>, viewModel: NewsViewModel = newsViewModel) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "newslist") {
            composable(route = "newslist") {
                // val newsData=viewModel.newsData.value
                //  val articles=if(newsData is NewsResult.Success)newsData.data else emptyList()

                NewsList(list = list, onClick = { article ->
                    navController.navigate("articleview/${article.title}")
                }, onSummarizeClick = {
                    // viewModel.summarizeArticles(articles)
                    navController.navigate("summarizeView")
                })
            }

            composable(route = "articleview/{articleTitle}") { backStackEntry ->
                val articleTitle = backStackEntry.arguments?.getString("articleTitle")
                ArticleDetailedView(
                    navController = navController,
                    articleTitle = articleTitle,

                    )
            }
            composable(route = "summarizeView") {
                SummarizeViewScreen(navController, viewModel,articles = list)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ArticleDetailedView(
        modifier: Modifier = Modifier,
        navController: NavController,
        articleTitle: String?,

        ) {
        val articleState by newsViewModel.singleArticle.observeAsState()
        LaunchedEffect(Unit) {
            newsViewModel.getSingleArticle(articleTitle ?: "")
        }

        LaunchedEffect(articleState) {
            Log.i("BasisCodelab", "Article state updated: $articleState")
            if (articleState != null) {
                withContext(Dispatchers.IO) {
                    //  articleSummarization(articleState!!)

                    Log.i("BasisCodelab", "Article processed:${articleState?.title}")
                }
            }
        }
        articleState?.let { article ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopAppBar(title = { Text(text = "Back") }, navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")

                    }
                }, modifier = Modifier.fillMaxWidth()
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
                    text = article.title, style = MaterialTheme.typography.headlineSmall.copy(
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
                        })
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
                modifier = Modifier.padding(vertical = 24.dp), onClick = onContinueClicked
            ) {
                Text("Continue")
            }
        }
    }

    @Composable
    fun NewsList(
        list: List<Article>,
        onClick: (Article) -> Unit,
        onSummarizeClick: () -> Unit,
        modifier: Modifier = Modifier,

        ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp))
            Text(

                text = "NEWS HEADLINES", style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Button(
                    onClick = { onSummarizeClick() },
                    modifier = Modifier.padding(8.dp)
                ) {
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
                        text = title, style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold, fontSize = 16.sp

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
                        modifier = Modifier.size(128.dp)
                    )
                }

            }
        }
    }
}










