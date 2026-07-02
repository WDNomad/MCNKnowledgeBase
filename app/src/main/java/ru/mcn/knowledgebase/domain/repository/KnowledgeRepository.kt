package ru.mcn.knowledgebase.domain.repository

import ru.mcn.knowledgebase.domain.model.Article
import ru.mcn.knowledgebase.domain.model.Category

interface KnowledgeRepository {

    suspend fun getCategories(): List<Category>

    suspend fun getArticles(
        categoryId: String
    ): List<Article>
    suspend fun getAllArticles(): List<Article>
    suspend fun getArticle(
        articleId: String
    ): Article?
}