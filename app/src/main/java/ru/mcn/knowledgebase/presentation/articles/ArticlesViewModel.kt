package ru.mcn.knowledgebase.presentation.articles

import ru.mcn.knowledgebase.domain.repository.KnowledgeRepository
import ru.mcn.knowledgebase.core.di.AppModule
class ArticlesViewModel {

    private val repository: KnowledgeRepository =
        AppModule.knowledgeRepository

    suspend fun loadArticles(
        categoryId: String
    ): List<ArticleUiModel> {

        return repository
            .getArticles(categoryId)
            .map {

                ArticleUiModel(
                    id = it.id,
                    title = it.title
                )
            }
    }
}
