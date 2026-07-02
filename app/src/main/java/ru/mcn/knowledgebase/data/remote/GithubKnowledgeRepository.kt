package ru.mcn.knowledgebase.data.remote

import ru.mcn.knowledgebase.core.network.RetrofitProvider
import ru.mcn.knowledgebase.domain.model.Article
import ru.mcn.knowledgebase.domain.model.Category
import ru.mcn.knowledgebase.domain.repository.KnowledgeRepository

class GithubKnowledgeRepository : KnowledgeRepository {

    override suspend fun getCategories(): List<Category> {

        return RetrofitProvider.api
            .getCategories()
            .map {

                Category(
                    id = it.id,
                    title = it.title
                )
            }
    }

    override suspend fun getArticles(
        categoryId: String
    ): List<Article> {

        return RetrofitProvider.api
            .getArticles()
            .filter {
                it.categoryId == categoryId
            }
            .map {

                Article(
                    id = it.id,
                    categoryId = it.categoryId,
                    title = it.title,
                    content = it.content,
                    originalUrl = it.originalUrl,
                    updatedAt = it.updatedAt,
                    images = it.images

                )
            }
    }
    override suspend fun getAllArticles(): List<Article> {

        return RetrofitProvider.api
            .getArticles()
            .map {

                Article(
                    id = it.id,
                    categoryId = it.categoryId,
                    title = it.title,
                    content = it.content,
                    originalUrl = it.originalUrl,
                    updatedAt = it.updatedAt,
                    images = it.images
                )
            }
    }

    override suspend fun getArticle(
        articleId: String
    ): Article? {

        return RetrofitProvider.api
            .getArticles()
            .firstOrNull {
                it.id == articleId
            }
            ?.let {

                Article(
                    id = it.id,
                    categoryId = it.categoryId,
                    title = it.title,
                    content = it.content,
                    originalUrl = it.originalUrl,
                    updatedAt = it.updatedAt,
                    images = it.images
                )
            }
    }
}