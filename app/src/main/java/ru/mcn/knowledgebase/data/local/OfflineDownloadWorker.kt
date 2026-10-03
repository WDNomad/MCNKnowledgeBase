package ru.mcn.knowledgebase.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.mcn.knowledgebase.data.remote.ArticleHtmlRepository
import ru.mcn.knowledgebase.data.remote.OfflineArticleDownloader
import ru.mcn.knowledgebase.domain.model.OfflineBatch

class OfflineDownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val store = OfflineDownloads(applicationContext)
        try {
            val request = store.readRequest(id)
            val html = ArticleHtmlRepository(applicationContext)
            val media = ArticleMediaCache(applicationContext)
            val downloader = OfflineArticleDownloader(
                cachedHtml = { html.cached(it.id, it.url) }, downloadHtml = html::download, cacheImage = media::ensureCached
            )
            // Keep each run below Android's regular worker deadline. Checkpoints let the next run continue.
            val progress = withTimeoutOrNull(7 * 60 * 1000L) {
                OfflineBatch().run(request.articles, store.readProgress(id), download = downloader::download, checkpoint = { progress ->
                    store.saveProgress(id, progress)
                    setProgress(workDataOf("processed" to progress.processed))
                }, onArticle = { title ->
                    setProgress(workDataOf("article" to title.take(500)))
                })
            } ?: return@withContext Result.retry()
            if (progress.processed == request.articles.size && progress.failed == 0) Result.success()
            else Result.failure()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
