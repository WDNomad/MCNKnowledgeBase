package ru.mcn.knowledgebase.domain.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException

data class OfflineArticle(val id: String, val url: String, val title: String)
data class OfflineProgress(val processed: Int = 0, val saved: Int = 0, val failed: Int = 0)

/** Checkpoint only complete articles; interrupted downloads reuse individual cached files. */
class OfflineBatch {
    suspend fun run(
        articles: List<OfflineArticle>,
        initial: OfflineProgress,
        download: suspend (OfflineArticle) -> Unit,
        checkpoint: suspend (OfflineProgress) -> Unit,
        onArticle: suspend (String) -> Unit = {}
    ): OfflineProgress {
        var progress = initial
        var consecutiveErrors = 0
        for (article in articles.drop(initial.processed)) {
            currentCoroutineContext().ensureActive()
            onArticle(article.title)
            val saved = try {
                download(article)
                currentCoroutineContext().ensureActive()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A missing/bad individual document must not block the rest of the catalog.
                consecutiveErrors = if (e is IOException) consecutiveErrors + 1 else 0
                false
            }
            progress = OfflineProgress(progress.processed + 1,
                progress.saved + if (saved) 1 else 0,
                progress.failed + if (saved) 0 else 1)
            checkpoint(progress)
            if (saved) consecutiveErrors = 0
            // Avoid hundreds of requests when the source is unavailable or the disk is full.
            if (consecutiveErrors >= 3) break
        }
        return progress
    }
}
