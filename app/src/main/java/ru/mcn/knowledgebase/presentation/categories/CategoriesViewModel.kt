package ru.mcn.knowledgebase.presentation.categories

import ru.mcn.knowledgebase.domain.repository.KnowledgeRepository
import ru.mcn.knowledgebase.presentation.articles.ArticleUiModel
import ru.mcn.knowledgebase.core.di.AppModule
class CategoriesViewModel {

    private val repository: KnowledgeRepository =
        AppModule.knowledgeRepository

    suspend fun loadCategories(): List<CategoryUiModel> {

        return repository
            .getCategories()
            .map {

                CategoryUiModel(
                    id = it.id,
                    title = it.title
                )
            }
    }

    suspend fun loadAllArticles(): List<ArticleUiModel> {

        return repository
            .getAllArticles()
            .map {

                ArticleUiModel(
                    id = it.id,
                    title = it.title
                )
            }
    }
}