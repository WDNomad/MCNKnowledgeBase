package ru.mcn.knowledgebase.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.mcn.knowledgebase.data.local.BundledKnowledge
import ru.mcn.knowledgebase.data.local.LocalDataSource
import ru.mcn.knowledgebase.data.local.entity.ArticleEntity
import ru.mcn.knowledgebase.data.remote.dto.ArticleDto
import ru.mcn.knowledgebase.domain.model.Article
import ru.mcn.knowledgebase.domain.model.Category
import ru.mcn.knowledgebase.domain.model.KnowledgeCatalog
import ru.mcn.knowledgebase.domain.repository.KnowledgeRepository

class GithubKnowledgeRepository(
    private val remoteDataSource: KnowledgeRemoteDataSource,
    private val localDataSource: LocalDataSource
) : KnowledgeRepository {
    private val mutex = Mutex()
    private var snapshot: List<Article>? = null

    override suspend fun getAllArticles(): List<Article> = mutex.withLock {
        snapshot ?: run {
            // The bundled sitemap snapshot is also the offline seed for existing installations.
            val bundled = BundledKnowledge.articles().map { it.toDomain() }
            val cached = try {
                localDataSource.getArticles().map {
                    Article(it.id, it.categoryId, it.title, it.content, it.originalUrl, it.updatedAt, it.images)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A broken optional cache must not prevent reading the bundled snapshot.
                emptyList()
            }
            merge(bundled, cached).also { snapshot = it }
        }
    }

    override suspend fun refresh() {
        mutex.withLock {
            val remote = remoteDataSource.getArticles().map { it.toDomain() }
            require(remote.isNotEmpty()) { "Сервер вернул пустую базу" }
            // Older GitHub exports must not hide articles shipped in the application.
            val baseline = merge(BundledKnowledge.articles().map { it.toDomain() }, snapshot.orEmpty())
            val updated = merge(baseline, remote)
            localDataSource.saveArticles(updated.map {
                ArticleEntity(it.id, it.categoryId, it.title, it.content, it.originalUrl, it.updatedAt.orEmpty(), it.images)
            })
            snapshot = updated
        }
    }

    override suspend fun getCategories(): List<Category> =
        KnowledgeCatalog(getAllArticles(), BundledKnowledge.titles()).sections().map {
            Category(it.id, it.title, articlesCount = it.articleCount)
        }

    override suspend fun getArticles(categoryId: String): List<Article> =
        KnowledgeCatalog(getAllArticles(), emptyMap()).articles(sectionId = categoryId)

    override suspend fun getArticle(articleId: String): Article? = getAllArticles().firstOrNull { it.id == articleId }

    private suspend fun merge(bundled: List<Article>, other: List<Article>): List<Article> = withContext(Dispatchers.Default) {
        (bundled + other).groupBy { it.id }.values.map { versions ->
            versions.maxBy { it.updatedAt.orEmpty() }
        }
    }

    private fun ArticleDto.toDomain() = Article(id, categoryId, title, content, originalUrl, updatedAt, images)
}
