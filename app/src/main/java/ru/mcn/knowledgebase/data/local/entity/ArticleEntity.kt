package ru.mcn.knowledgebase.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(

    @PrimaryKey
    val id: String,

    val categoryId: String,

    val title: String,

    val content: String?,

    val originalUrl: String,

    val updatedAt: String,

    val images: List<String>
)