package ru.mcn.knowledgebase.presentation.articles

import ru.mcn.knowledgebase.data.remote.GithubKnowledgeRepository
import ru.mcn.knowledgebase.domain.repository.KnowledgeRepository

class ArticlesViewModel {

    private val repository: KnowledgeRepository =
        GithubKnowledgeRepository()

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
