package ru.mcn.knowledgebase.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.mcn.knowledgebase.data.local.entity.ArticleEntity

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles")
    suspend fun getArticles(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE categoryId = :categoryId")
    suspend fun getArticles(
        categoryId: String
    ): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE id = :articleId LIMIT 1")
    suspend fun getArticle(
        articleId: String
    ): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(
        articles: List<ArticleEntity>
    )

    @Query("DELETE FROM articles")
    suspend fun clear()
}