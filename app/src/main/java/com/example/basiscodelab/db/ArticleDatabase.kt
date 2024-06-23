package com.example.basiscodelab.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.basiscodelab.data.Article


@Database(
    entities = [Article::class],
    version = 4)
@TypeConverters( Converters:: class)
 abstract class ArticleDatabase: RoomDatabase() {
     abstract fun getArticleDAO(): ArticleDAO



     companion object{
         @Volatile
         private var instance:ArticleDatabase?= null
         private val LOCK= Any()

         operator fun invoke(context:Context)= instance ?: synchronized(LOCK){
         instance ?: createDatabase(context).also {
             instance = it



         }

     }
    private fun createDatabase(context: Context) = androidx.room.Room.databaseBuilder(
        context.applicationContext,
        ArticleDatabase::class.java,
        "article_db.db"
        ).fallbackToDestructiveMigration()
            .build()

         private val MIGRATION_2_4 = object : androidx.room.migration.Migration(2, 4) {
             override fun migrate(database: SupportSQLiteDatabase) {
                 database.execSQL("ALTER TABLE articles ADD COLUMN content TEXT")
             }
         }

     }
}
