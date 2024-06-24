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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


class MainActivity : ComponentActivity() {

    private lateinit var newsViewModel: NewsViewModel

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
                    })
            }

            composable(route = "articleview/{articleTitle}") { backStackEntry ->
                val articleTitle = backStackEntry.arguments?.getString("articleTitle")


                ArticleDetailedView(navController = navController, articleTitle = articleTitle)
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ArticleDetailedView(
        modifier: Modifier = Modifier,
        navController: NavController,
        articleTitle: String?
    ) {
        var articleState: Article? by rememberSaveable { mutableStateOf(null) }

        newsViewModel.getSingleArticle(articleTitle ?: "")
        newsViewModel.singleArticle.observe(this@MainActivity) { article ->
            articleState = article
        }
        articleState?.let { article ->

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
               TopAppBar(
                    title = { Text(text = "Back") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription ="Back" )

                        }
                    },
                    modifier = Modifier.fillMaxWidth())
                article.urlToImage?.let {
                    Image(
                        painter = rememberImagePainter(data = it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(256.dp)
                            .padding(8.dp)
                    )
                }

                Text(text = article.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold)
                )
                        Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Published at: ${article.publishedAt}",
                    style=MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = article.description ?: "",
                    style = MaterialTheme.typography.bodyLarge.copy(

                    ))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = article.source.name ?: "",
                    style = MaterialTheme.typography.bodyMedium
                    )
                Spacer(modifier = Modifier.height(8.dp))
                article.url?.let{
                    Text(
                        text=it,
                        style=MaterialTheme.typography.bodyMedium,
                        modifier=Modifier.clickable {
                           val intent =Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            startActivity(intent,null)
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
    private fun NewsList(
        list: List<Article>,
        onClick: (Article) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column {
            Spacer(modifier = Modifier.height(18.dp))
            Text(

                text = "NEWS HEADLINES",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold

                ),

                modifier = Modifier.padding(24.dp)
            )
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


    @Composable
    private fun ArticleView(

        title: String,
        publisedAt: String,
        description: String,
        imageUrl:String?,
        modifier: Modifier = Modifier
    ) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        val formattedPublishedAt=try{
            val parsedDate= LocalDateTime.parse(publisedAt, DateTimeFormatter.ISO_DATE_TIME)
            parsedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()))
        }
        catch(e:Exception){
            publisedAt
        }
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Row(modifier = Modifier.padding(24.dp)) {
                imageUrl?.let {
                    Image(
                        painter = rememberImagePainter(data = it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(128.dp)
                            .padding(end = 8.dp)
                    )
                }

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
                    Text(text = publisedAt,
                        )
                }

            }
        }}}








