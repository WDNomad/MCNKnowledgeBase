package ru.mcn.knowledgebase.data.local

import androidx.room.withTransaction

import ru.mcn.knowledgebase.data.local.entity.ArticleEntity
import ru.mcn.knowledgebase.data.local.entity.CategoryEntity

class LocalDataSource(
    private val database: KnowledgeDatabase
) {

    suspend fun getCategories(): List<CategoryEntity> =
        database.categoryDao().getCategories()

    suspend fun saveCategories(
        categories: List<CategoryEntity>
    ) {
        database.categoryDao().clear()
        database.categoryDao().insertCategories(categories)
    }

    suspend fun getArticles(): List<ArticleEntity> =
        database.articleDao().getArticles()

    suspend fun getArticles(
        categoryId: String
    ): List<ArticleEntity> =
        database.articleDao().getArticles(categoryId)

    suspend fun getArticle(
        articleId: String
    ): ArticleEntity? =
        database.articleDao().getArticle(articleId)

    suspend fun saveArticles(
        articles: List<ArticleEntity>
    ) {
        database.withTransaction {
            database.articleDao().clear()
            database.articleDao().insertArticles(articles)
        }
    }
}
