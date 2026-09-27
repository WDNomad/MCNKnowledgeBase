package ru.mcn.knowledgebase.core.di

import androidx.room.Room
import ru.mcn.knowledgebase.AppContextProvider
import ru.mcn.knowledgebase.data.local.KnowledgeDatabase
import ru.mcn.knowledgebase.data.remote.GithubKnowledgeRepository
import ru.mcn.knowledgebase.data.remote.RemoteDataSource
import ru.mcn.knowledgebase.domain.repository.KnowledgeRepository
import ru.mcn.knowledgebase.data.local.LocalDataSource


object AppModule {

    private val remoteDataSource = RemoteDataSource()
    private val localDataSource by lazy {
        LocalDataSource(database)
    }
    val database: KnowledgeDatabase by lazy {
        Room.databaseBuilder(
            AppContextProvider.context,
            KnowledgeDatabase::class.java,
            "knowledge.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    val knowledgeRepository: KnowledgeRepository by lazy {
        GithubKnowledgeRepository(
            remoteDataSource,
            localDataSource
        )
    }
}