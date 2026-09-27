package ru.mcn.knowledgebase

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import ru.mcn.knowledgebase.domain.model.Article
import ru.mcn.knowledgebase.domain.model.ArticlePath
import ru.mcn.knowledgebase.domain.model.KnowledgeCatalog

class BundledCatalogTest {
    @Test fun `bundled articles are reachable through all four levels`() {
        val gson = Gson()
        val articles: List<Article> = gson.fromJson(File("src/main/assets/articles.json").readText(), object : TypeToken<List<Article>>() {}.type)
        val titles: Map<String, String> = gson.fromJson(File("src/main/assets/catalog_titles.json").readText(), object : TypeToken<Map<String, String>>() {}.type)
        assertTrue("The full Russian knowledge base must be included", articles.size >= 776)
        assertEquals(articles.size, articles.distinctBy { it.id }.size)
        val catalog = KnowledgeCatalog(articles, titles)
        val reached = catalog.sections().flatMap { section ->
            catalog.subsections(section.id).flatMap { folder ->
                catalog.articles(section.id, folder.id)
            }
        }
        assertEquals(articles.map { it.id }.toSet(), reached.map { it.id }.toSet())
        articles.forEach {
            assertTrue("Missing title: ${it.id}", it.title.isNotBlank())
            assertFalse("Missing content: ${it.id}", it.content.isNullOrBlank())
            val path = requireNotNull(ArticlePath.fromUrl(it.originalUrl))
            assertTrue("Missing section title: ${path.sectionId}", titles.containsKey(path.sectionId))
            if (!path.subsectionId.endsWith("/@general")) {
                assertTrue("Missing subsection title: ${path.subsectionId}", titles.containsKey(path.subsectionId) || titles.containsKey(path.subsectionId.substringAfterLast('/')))
            }
        }
        val example = catalog.articles("virtualnaya-ats", "virtualnaya-ats/nastroyka", "выход очереди")
        assertTrue(example.any { it.id == "9914d70a-acd4-43a6-8c05-cf868e1a68e3" })
    }
}
