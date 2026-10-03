package ru.mcn.knowledgebase.domain.model

import java.net.URI
import java.util.Locale

sealed interface ArticleLink {
    data class Internal(val id: String, val fragment: String?) : ArticleLink
    data class Current(val fragment: String?) : ArticleLink
    data class External(val url: String) : ArticleLink
    data class Missing(val url: String) : ArticleLink
    data object Blocked : ArticleLink
}

class ArticleLinkResolver(articles: List<Article>) {
    private val byId = articles.associateBy { it.id.lowercase() }
    private val byPath = articles.associateBy { path(it.originalUrl) }
    private val byTitle = articles.groupBy { normalizeTitle(it.title) }

    fun originalUrl(id: String): String? = byId[id.lowercase(Locale.ROOT)]?.originalUrl

    fun resolve(link: String, currentId: String, currentUrl: String, linkText: String? = null): ArticleLink {
        val uri = runCatching { URI(currentUrl).resolve(link) }.getOrNull() ?: return ArticleLink.Blocked
        if (uri.scheme?.lowercase() !in setOf("https", "http", "mailto", "tel")) return ArticleLink.Blocked
        val host = uri.host?.lowercase(Locale.ROOT)
        if (host !in knowledgeHosts) return ArticleLink.External(uri.toString())
        val id = articleId.find(uri.path.orEmpty().trimEnd('/'))?.groupValues?.get(1)
        val documentPath = uri.path.orEmpty()
        val legacyArticle = host in legacyHosts && (documentPath.startsWith("/ru/") ||
            (documentPath.startsWith("/knowledge-bases/") && "/articles/" in documentPath))
        val target = byPath[documentPath.trimEnd('/')] ?: id?.let { byId[it.lowercase()] }
            ?: if (legacyArticle && id == null && !linkText.isNullOrBlank()) byTitle[normalizeTitle(linkText)]?.singleOrNull() else null
        if (target?.id.equals(currentId, ignoreCase = true) || id.equals(currentId, ignoreCase = true) || path(currentUrl) == uri.path.orEmpty().trimEnd('/')) {
            return ArticleLink.Current(uri.fragment)
        }
        if (target != null) return ArticleLink.Internal(target.id, uri.fragment)
        return if (id != null || legacyArticle) ArticleLink.Missing(uri.toString()) else ArticleLink.External(uri.toString())
    }

    private fun path(url: String) = runCatching { URI(url).path.orEmpty().trimEnd('/') }.getOrDefault("")
    private fun normalizeTitle(title: String) = title.trim().trim('«', '»', '"').lowercase(Locale.ROOT)
        .replace('ё', 'е').replace(Regex("\\s+"), " ")

    companion object {
        private val legacyHosts = setOf("kb.mcn.ru", "mcntelecom.userecho.ru", "mcntelecom.userecho.com")
        private val knowledgeHosts = legacyHosts + setOf("handbook.mcn.ru", "www.handbook.mcn.ru")
        private val articleId = Regex("--([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$")
    }
}
