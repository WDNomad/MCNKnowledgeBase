package ru.mcn.knowledgebase.domain.model

data class Category(
    val id: String,
    val title: String,

    val sourceUrl: String? = null,

    val articlesCount: Int = 0
)