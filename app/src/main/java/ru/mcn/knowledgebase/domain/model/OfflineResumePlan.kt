package ru.mcn.knowledgebase.domain.model

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class OfflineResumePlan(val articles: List<OfflineArticle>, val progress: OfflineProgress) {
    companion object {
        /** Check local files before enqueueing: the first visible count includes complete articles. */
        suspend fun create(articles: List<OfflineArticle>, isSaved: suspend (OfflineArticle) -> Boolean): OfflineResumePlan {
            val ready = mutableListOf<OfflineArticle>()
            val pending = mutableListOf<OfflineArticle>()
            for (article in articles) {
                currentCoroutineContext().ensureActive()
                if (isSaved(article)) ready += article else pending += article
            }
            return OfflineResumePlan(ready + pending, OfflineProgress(processed = ready.size, saved = ready.size))
        }
    }
}
