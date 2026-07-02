package ru.mcn.knowledgebase.core.network

import retrofit2.http.GET
import ru.mcn.knowledgebase.data.remote.dto.ArticleDto
import ru.mcn.knowledgebase.data.remote.dto.CategoryDto

interface KnowledgeApi {

    @GET("categories.json")
    suspend fun getCategories(): List<CategoryDto>

    @GET("articles.json")
    suspend fun getArticles(): List<ArticleDto>
}