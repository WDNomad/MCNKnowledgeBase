package ru.mcn.knowledgebase.data.remote.dto

data class ArticleDto(
    val id: String,
    val categoryId: String,
    val title: String,
    val content: String,
    val originalUrl: String,
    val updatedAt: String? = null,
    val images: List<String> = emptyList()
)