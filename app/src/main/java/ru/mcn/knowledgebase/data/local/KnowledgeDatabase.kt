package ru.mcn.knowledgebase.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.mcn.knowledgebase.data.local.dao.ArticleDao
import ru.mcn.knowledgebase.data.local.dao.CategoryDao
import ru.mcn.knowledgebase.data.local.entity.ArticleEntity
import ru.mcn.knowledgebase.data.local.entity.CategoryEntity

@Database(
    entities = [
        CategoryEntity::class,
        ArticleEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KnowledgeDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun articleDao(): ArticleDao
}