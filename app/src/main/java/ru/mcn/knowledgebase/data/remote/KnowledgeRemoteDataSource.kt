package ru.mcn.knowledgebase.data.remote

import ru.mcn.knowledgebase.data.remote.dto.ArticleDto
import ru.mcn.knowledgebase.data.remote.dto.CategoryDto

interface KnowledgeRemoteDataSource {

    suspend fun getCategories(): List<CategoryDto>

    suspend fun getArticles(): List<ArticleDto>

    suspend fun getArticle(articleId: String): ArticleDto?
}
