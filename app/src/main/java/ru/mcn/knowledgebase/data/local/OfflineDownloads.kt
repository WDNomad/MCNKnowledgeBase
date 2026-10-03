package ru.mcn.knowledgebase.data.local

import android.content.Context
import android.util.AtomicFile
import androidx.work.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.mcn.knowledgebase.domain.model.Article
import ru.mcn.knowledgebase.domain.model.OfflineArticle
import ru.mcn.knowledgebase.domain.model.OfflineProgress
import ru.mcn.knowledgebase.domain.model.OfflineResumePlan
import ru.mcn.knowledgebase.data.remote.ArticleHtmlRepository
import ru.mcn.knowledgebase.data.remote.ArticleHtmlParser
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

data class OfflineRequest(val title: String, val articles: List<OfflineArticle>)

data class OfflineStatus(
    val id: UUID, val title: String, val total: Int, val progress: OfflineProgress,
    val state: WorkInfo.State, val currentArticle: String
) {
    val active get() = !state.isFinished
    val complete get() = state == WorkInfo.State.SUCCEEDED && progress.saved == total
}

/** Durable job input and checkpoints are independent of the activity's lifecycle. */
class OfflineDownloads(context: Context) {
    private val context = context.applicationContext
    private val work = WorkManager.getInstance(this.context)
    private val gson = Gson()
    private val directory = File(this.context.filesDir, "offline-downloads").apply { mkdirs() }
    private val preferences = this.context.getSharedPreferences("offline-download-progress", Context.MODE_PRIVATE)

    val status = work.getWorkInfosForUniqueWorkFlow(WORK_NAME).map { jobs ->
        val info = jobs.maxByOrNull { job -> job.tags.firstOrNull { it.startsWith("created:") }?.substringAfter(':')?.toLongOrNull() ?: 0L }
            ?: return@map null
        val data = if (info.state.isFinished) info.outputData else info.progress
        OfflineStatus(info.id,
            info.tags.firstOrNull { it.startsWith("title:") }?.substringAfter(':') ?: "База знаний",
            info.tags.firstOrNull { it.startsWith("total:") }?.substringAfter(':')?.toIntOrNull() ?: 0,
            readProgress(info.id), info.state, data.getString("article").orEmpty())
    }.flowOn(Dispatchers.IO)

    suspend fun enqueue(articles: List<Article>, title: String) = start(
        OfflineRequest(title, articles.distinctBy { it.id }.map { OfflineArticle(it.id, it.originalUrl, it.title) })
    )

    suspend fun retry(id: UUID) = withContext(Dispatchers.IO) {
        val batch = readRequest(id)
        val processed = readProgress(id).processed.coerceIn(0, batch.articles.size)
        val html = ArticleHtmlRepository(context)
        val media = ArticleMediaCache(context)
        val plan = OfflineResumePlan.create(batch.articles.drop(processed) + batch.articles.take(processed)) { article ->
            try {
                val body = html.cached(article.id, article.url)
                body != null && ArticleHtmlParser.images(body).all { media.isCached(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        }
        start(batch.copy(articles = plan.articles), plan.progress)
    }

    private suspend fun start(batch: OfflineRequest, initial: OfflineProgress = OfflineProgress()) = withContext(Dispatchers.IO) {
        enqueueLock.withLock {
            require(batch.articles.isNotEmpty()) { "Нет статей для загрузки" }
            check(work.getWorkInfosForUniqueWork(WORK_NAME).get().none { !it.state.isFinished }) {
                "Загрузка уже выполняется"
            }
            val request = OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .addTag("created:${System.currentTimeMillis()}")
                .addTag("title:${batch.title}").addTag("total:${batch.articles.size}").build()
            val file = AtomicFile(requestFile(request.id))
            val stream = file.startWrite()
            try {
                stream.write(gson.toJson(batch).toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (e: Exception) {
                file.failWrite(stream)
                throw e
            }
            saveProgress(request.id, initial)
            work.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request).result.get()
        }
    }

    suspend fun cancel(id: UUID) = withContext(Dispatchers.IO) { work.cancelWorkById(id).result.get(); Unit }

    internal fun readRequest(id: UUID): OfflineRequest = AtomicFile(requestFile(id)).openRead().bufferedReader().use {
        gson.fromJson(it, OfflineRequest::class.java)
    }

    internal fun readProgress(id: UUID) = OfflineProgress(
        preferences.getInt("$id.processed", 0), preferences.getInt("$id.saved", 0), preferences.getInt("$id.failed", 0)
    )

    internal fun saveProgress(id: UUID, progress: OfflineProgress) {
        check(preferences.edit().putInt("$id.processed", progress.processed)
            .putInt("$id.saved", progress.saved).putInt("$id.failed", progress.failed).commit()) {
            "Не удалось сохранить прогресс загрузки"
        }
    }

    private fun requestFile(id: UUID) = File(directory, "$id.json")

    companion object {
        private const val WORK_NAME = "offline-articles"
        private val enqueueLock = Mutex()
    }
}
