package ru.mcn.knowledgebase.data.remote

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import ru.mcn.knowledgebase.domain.model.OfflineArticle

class OfflineArticleDownloader(
    private val cachedHtml: suspend (OfflineArticle) -> String?,
    private val downloadHtml: suspend (String) -> String,
    private val cacheImage: suspend (String) -> Unit
) {
    suspend fun download(article: OfflineArticle) {
        val body = cachedHtml(article) ?: downloadHtml(article.url)
        for (url in ArticleHtmlParser.images(body)) {
            currentCoroutineContext().ensureActive()
            cacheImage(url)
        }
    }
}
