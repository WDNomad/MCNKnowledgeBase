package ru.mcn.knowledgebase.domain.model

data class Article(
    val id: String,
    val categoryId: String,
    val title: String,
    val content: String? = null,
    val originalUrl: String,
    val updatedAt: String? = null,
    val images: List<String> = emptyList()
)