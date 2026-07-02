package ru.mcn.knowledgebase.presentation.article

import ru.mcn.knowledgebase.data.remote.GithubKnowledgeRepository
import ru.mcn.knowledgebase.domain.repository.KnowledgeRepository

class ArticleViewModel {

    private val repository: KnowledgeRepository =
        GithubKnowledgeRepository()

    suspend fun loadArticle(
        articleId: String
    ): ArticleUiModel? {

        return repository
            .getArticle(articleId)
            ?.let {

                ArticleUiModel(
                    title = it.title,
                    content = it.content ?: "",
                    originalUrl = it.originalUrl,
                    updatedAt = it.updatedAt,
                    images = it.images
                )
            }
    }
}