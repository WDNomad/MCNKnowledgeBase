package ru.mcn.knowledgebase.core.di

import ru.mcn.knowledgebase.data.remote.GithubKnowledgeRepository
import ru.mcn.knowledgebase.data.remote.RemoteDataSource
import ru.mcn.knowledgebase.domain.repository.KnowledgeRepository

object AppModule {

    private val remoteDataSource = RemoteDataSource()

    val knowledgeRepository: KnowledgeRepository by lazy {
        GithubKnowledgeRepository(remoteDataSource)
    }
}