package ru.mcn.knowledgebase.presentation.article

import ru.mcn.knowledgebase.domain.repository.KnowledgeRepository
import ru.mcn.knowledgebase.core.di.AppModule
import ru.mcn.knowledgebase.data.local.BundledKnowledge
import ru.mcn.knowledgebase.domain.model.KnowledgeCatalog
class ArticleViewModel {

    private val repository: KnowledgeRepository =
        AppModule.knowledgeRepository

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
                    images = it.images,
                    breadcrumb = KnowledgeCatalog(emptyList(), BundledKnowledge.titles()).breadcrumb(it)
                )
            }
    }
}
