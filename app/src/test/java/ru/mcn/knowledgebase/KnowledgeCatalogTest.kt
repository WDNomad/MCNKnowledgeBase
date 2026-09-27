package ru.mcn.knowledgebase

import org.junit.Assert.*
import org.junit.Test
import ru.mcn.knowledgebase.domain.model.Article
import ru.mcn.knowledgebase.domain.model.ArticlePath
import ru.mcn.knowledgebase.domain.model.KnowledgeCatalog

class KnowledgeCatalogTest {
    private val uuid = "9914d70a-acd4-43a6-8c05-cf868e1a68e3"
    private fun article(id: String, folders: String, title: String = "Выход из очереди", content: String = "Настройка звонка") =
        Article(id, "ignored", title, content, "https://handbook.mcn.ru/ru/$folders/article--$uuid/")

    @Test fun `example URL maps to section and scoped subsection`() {
        val path = ArticlePath.fromUrl("https://handbook.mcn.ru/ru/virtualnaya-ats/nastroyka/vykhod-iz-ocheredi-v-stsenarii-zvonka--$uuid/")!!
        assertEquals("virtualnaya-ats", path.sectionId)
        assertEquals("virtualnaya-ats/nastroyka", path.subsectionId)
    }

    @Test fun `direct articles receive a general subsection`() {
        assertEquals("faq/@general", ArticlePath.fromUrl(article("a", "faq").originalUrl)?.subsectionId)
    }

    @Test fun `invalid foreign and non article URLs are excluded`() {
        listOf("not a url", "https://evil.example/ru/a/article--$uuid/", "https://handbook.mcn.ru/en/a/article--$uuid/", "https://handbook.mcn.ru/ru/a/nastroyka/").forEach {
            assertNull(ArticlePath.fromUrl(it))
        }
    }

    @Test fun `same subsection names do not leak between sections`() {
        val catalog = KnowledgeCatalog(listOf(article("a", "ats/nastroyka"), article("b", "mobile/nastroyka")), emptyMap())
        assertEquals(listOf("a"), catalog.articles(subsectionId = "ats/nastroyka").map { it.id })
        assertEquals(listOf("b"), catalog.articles(sectionId = "mobile").map { it.id })
        assertEquals(2, catalog.articles(query = "ЗВОНКА").size)
    }

    @Test fun `search normalizes Russian and matches title and body with whitespace`() {
        val catalog = KnowledgeCatalog(listOf(article("a", "ats/nastroyka", "Счёт", "Оплата услуг"), article("b", "ats/other", "Очередь", "Звонки")), emptyMap())
        assertEquals(listOf("a"), catalog.articles(query = "  СЧЕТ  оплата  ").map { it.id })
        assertTrue(catalog.articles(query = "несуществующее").isEmpty())
        assertEquals(2, catalog.articles(query = "   ").size)
    }

    @Test fun `folder counts deduplicate articles and include direct articles`() {
        val a = article("a", "ats/nastroyka")
        val catalog = KnowledgeCatalog(listOf(a, a, article("b", "ats")), mapOf("ats" to "Виртуальная АТС", "nastroyka" to "Настройка"))
        assertEquals(2, catalog.sections().single().articleCount)
        assertEquals(2, catalog.subsections("ats").size)
        assertEquals("Виртуальная АТС · Настройка", catalog.breadcrumb(a))
    }

    @Test fun `deeper paths remain separate within the four screen hierarchy`() {
        assertEquals("ats/settings/sip", ArticlePath.fromUrl(article("a", "ats/settings/sip").originalUrl)?.subsectionId)
    }
}
