package ru.mcn.knowledgebase.domain.model

import java.net.URI
import java.util.Locale

/** Folder IDs are full paths: identical subsection slugs in different sections stay distinct. */
data class ArticlePath(val sectionId: String, val subsectionId: String) {
    companion object {
        fun fromUrl(url: String): ArticlePath? = runCatching {
            val uri = URI(url)
            if (uri.host != "handbook.mcn.ru" || uri.scheme !in listOf("https", "http")) return null
            val parts = uri.path.trim('/').split('/')
            if (parts.size < 3 || parts.first() != "ru" || !parts.last().matches(
                    Regex(".+--[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                )) return null
            val section = parts[1]
            val folders = parts.drop(2).dropLast(1)
            ArticlePath(section, "$section/${folders.joinToString("/").ifEmpty { "@general" }}")
        }.getOrNull()
    }
}

data class CatalogFolder(val id: String, val title: String, val articleCount: Int)

class KnowledgeCatalog(articles: List<Article>, private val titles: Map<String, String>) {
    private val entries = articles.distinctBy { it.id }.mapNotNull { article ->
        ArticlePath.fromUrl(article.originalUrl)?.let { article to it }
    }

    fun title(id: String): String = when {
        id.endsWith("/@general") -> "Общие статьи"
        else -> titles[id] ?: titles[id.substringAfterLast('/')]
            ?: id.substringAfterLast('/').replace('-', ' ').replaceFirstChar { it.titlecase() }
    }

    fun sections(): List<CatalogFolder> = entries.groupBy { it.second.sectionId }
        .map { (id, items) -> CatalogFolder(id, title(id), items.size) }.sortedBy { it.title }

    fun subsections(sectionId: String): List<CatalogFolder> = entries
        .filter { it.second.sectionId == sectionId }.groupBy { it.second.subsectionId }
        .map { (id, items) -> CatalogFolder(id, title(id), items.size) }.sortedBy { it.title }

    fun articles(sectionId: String? = null, subsectionId: String? = null, query: String = ""): List<Article> {
        val words = normalize(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        return entries.asSequence().filter { (_, path) ->
            (sectionId == null || path.sectionId == sectionId) &&
                (subsectionId == null || path.subsectionId == subsectionId)
        }.map { it.first }.filter { article ->
            val text = normalize(article.title + " " + article.content.orEmpty())
            words.all { it in text }
        }.sortedBy { it.title }.toList()
    }

    fun breadcrumb(article: Article): String = ArticlePath.fromUrl(article.originalUrl)?.let {
        "${title(it.sectionId)} · ${title(it.subsectionId)}"
    }.orEmpty()

    private fun normalize(text: String) = text.lowercase(Locale.ROOT).replace('ё', 'е')
}
