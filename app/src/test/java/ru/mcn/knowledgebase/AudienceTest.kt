package ru.mcn.knowledgebase

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import ru.mcn.knowledgebase.domain.model.Audience
import ru.mcn.knowledgebase.domain.model.Article
import ru.mcn.knowledgebase.domain.model.KnowledgeCatalog
import ru.mcn.knowledgebase.domain.model.PRIVATE_SECTION_ID
import java.io.File

class AudienceTest {
    @Test fun `missing or unknown preference requests a new choice`() {
        listOf(null, "", "invalid").forEach { assertNull(Audience.fromStoredValue(it)) }
    }

    @Test fun `both remembered choices restore the intended start route`() {
        assertEquals("categories", Audience.fromStoredValue("business")?.startRoute)
        assertEquals("subsections/$PRIVATE_SECTION_ID", Audience.fromStoredValue("private")?.startRoute)
        Audience.entries.forEach { assertEquals(it, Audience.fromStoredValue(it.storedValue)) }
    }

    @Test fun `private start section exists in the shipped database and has articles`() {
        val articles: List<Article> = Gson().fromJson(File("src/main/assets/articles.json").readText(), object : TypeToken<List<Article>>() {}.type)
        val catalog = KnowledgeCatalog(articles, emptyMap())
        assertTrue(catalog.sections().any { it.id == PRIVATE_SECTION_ID })
        assertTrue(catalog.subsections(PRIVATE_SECTION_ID).isNotEmpty())
        assertTrue(catalog.articles(sectionId = PRIVATE_SECTION_ID).isNotEmpty())
    }
}
