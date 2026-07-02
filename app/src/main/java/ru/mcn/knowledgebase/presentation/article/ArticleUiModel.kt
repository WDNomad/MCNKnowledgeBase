package ru.mcn.knowledgebase.presentation.article

data class ArticleUiModel(
    val title: String,
    val content: String,
    val originalUrl: String,
    val updatedAt: String? = null,
    val images: List<String> = emptyList()
)