package com.example.basiscodelab.network
import com.example.basiscodelab.data.News
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// https://newsapi.org/v2/top-headlines?country=in&apiKey=0f27c9a3dfc844d2940d42b111bbe9f1
// https://newsapi.org/v2/everything?q=apple&from=2024-06-13&to=2024-06-13&sortBy=popularity&apiKey=0f27c9a3dfc844d2940d42b111bbe9f1

const val BASE_URL="https://newsapi.org/"
const val API_KEY="0f27c9a3dfc844d2940d42b111bbe9f1"
interface NewsInterface {

    @GET("v2/top-headlines?apiKey=$API_KEY")
    fun getHeadLines(@Query("country")country :String,@Query("page")page:Int) : Call<News>

    @GET("v2/everything?apiKey=$API_KEY")
    fun searchNews(@Query("q")searchQuery:String,@Query("page")page:Int) : Call<News>

    // https://newsapi.org/v2/top-headlines?apiKey=$API_KEY&country=in&page=1

}
object NewsService{
    val newsInstance: NewsInterface
    init{
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        newsInstance =retrofit.create(NewsInterface:: class.java)
    }
}