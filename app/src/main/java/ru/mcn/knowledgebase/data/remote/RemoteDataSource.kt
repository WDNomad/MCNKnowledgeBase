package ru.mcn.knowledgebase.data.remote

import ru.mcn.knowledgebase.core.network.RetrofitProvider
import ru.mcn.knowledgebase.data.remote.dto.ArticleDto
import ru.mcn.knowledgebase.data.remote.dto.CategoryDto

class RemoteDataSource : KnowledgeRemoteDataSource {

    private val api = RetrofitProvider.api

    override suspend fun getCategories(): List<CategoryDto> {
        return api.getCategories()
    }
    override suspend fun getArticle(articleId: String): ArticleDto? {

        return api
            .getArticles()
            .firstOrNull {
                it.id == articleId
            }
    }
    override suspend fun getArticles(): List<ArticleDto> {
        return api.getArticles()
    }
}
