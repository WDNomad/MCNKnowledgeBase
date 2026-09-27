package ru.mcn.knowledgebase

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test
import ru.mcn.knowledgebase.data.remote.ArticleHtmlParser
import ru.mcn.knowledgebase.data.remote.ArticlePalette
import java.io.File

class ArticleHtmlParserTest {
    private val url = "https://handbook.mcn.ru/ru/virtualnaya-ats/nastroyka/example--9914d70a-acd4-43a6-8c05-cf868e1a68e3/"

    @Test fun `light and dark pages have explicit readable colors without transforming images`() {
        val palettes = listOf(ArticlePalette(), ArticlePalette(
            dark = true, background = "#121212", text = "#E6E1E5",
            secondaryText = "#CAC4D0", link = "#D0BCFF", panel = "#49454F", border = "#938F99"
        ))
        val body = "<p>Текст</p><img src=\"https://kb.mcn.ru/test.png\"><table><tr><td>Ячейка</td></tr></table>"
        palettes.forEach { palette ->
            val page = Jsoup.parse(ArticleHtmlParser.page("Заголовок", "Раздел", body, url, palette = palette))
            val css = page.selectFirst("style")!!.data()
            assertEquals(if (palette.dark) "dark" else "light", page.selectFirst("meta[name=color-scheme]")!!.attr("content"))
            assertTrue(css.contains("background:${palette.background}; color:${palette.text};"))
            assertFalse(css.contains("filter:"))
            assertFalse(css.contains("opacity:"))
            assertEquals("https://kb.mcn.ru/test.png", page.selectFirst("img")!!.attr("src"))
            assertTrue(contrast(palette.text, palette.background) >= 4.5)
            assertTrue(contrast(palette.secondaryText, palette.background) >= 4.5)
            assertTrue(contrast(palette.link, palette.background) >= 4.5)
            assertTrue(contrast(palette.text, palette.panel) >= 4.5)
        }
    }

    private fun contrast(first: String, second: String): Double {
        fun luminance(hex: String): Double {
            val channels = hex.removePrefix("#").chunked(2).map {
                val value = it.toInt(16) / 255.0
                if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
            }
            return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722
        }
        val a = luminance(first)
        val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    @Test fun `pictures stay between their original paragraphs including nested list images`() {
        val html = ArticleHtmlParser.extract("""
            <main><aside>Меню сайта</aside><div><nav>Навигация</nav><h1>Название</h1>
            <div class="not-prose">Счётчик просмотров</div>
            <p>Первый шаг</p><p><img src="/one.png" alt="Первое фото"></p>
            <ol><li>Второй шаг<img src="https://kb.mcn.ru/two.png"><strong>После фото</strong></li></ol>
            <table><tr><td>Таблица</td></tr></table><p>Последний шаг</p>
            <section>Рекомендованные статьи</section></div></main>
        """.trimIndent(), url)
        assertTrue(html.indexOf("Первый шаг") < html.indexOf("one.png"))
        assertTrue(html.indexOf("one.png") < html.indexOf("Второй шаг"))
        assertTrue(html.indexOf("Второй шаг") < html.indexOf("two.png"))
        assertTrue(html.indexOf("two.png") < html.indexOf("После фото"))
        assertTrue(html.indexOf("После фото") < html.indexOf("Последний шаг"))
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("https://handbook.mcn.ru/one.png"))
        assertFalse(html.contains("Счётчик"))
        assertFalse(html.contains("Рекомендованные"))
        assertFalse(html.contains("Меню сайта"))
    }

    @Test fun `active content and local resources are removed but document links remain`() {
        val html = ArticleHtmlParser.sanitize("""
            <script>alert(1)</script><iframe src="https://bad.example"></iframe>
            <p onclick="alert(1)">Инструкция<img src="file:///secret" onerror="alert(1)"></p>
            <a href="javascript:alert(1)">Опасная ссылка</a>
            <h2 id="step">Шаг</h2><a href="#step">Перейти к шагу</a>
            <a href="/ru/">База знаний</a><img src="https://kb.mcn.ru/test.png">
        """.trimIndent(), url)
        listOf("<script", "<iframe", "onclick", "onerror", "javascript:", "file:").forEach { assertFalse(it, html.contains(it)) }
        assertTrue(html.contains("id=\"step\""))
        assertTrue(html.contains("href=\"https://handbook.mcn.ru/ru/\""))
        assertEquals(setOf("https://kb.mcn.ru/test.png"), ArticleHtmlParser.images(html))
    }

    @Test fun `bundled real article preserves exact image sequence after sanitizing`() {
        val source = File("src/main/assets/article-html/9914d70a-acd4-43a6-8c05-cf868e1a68e3.html").readText()
        val original = Jsoup.parseBodyFragment(source)
        val cleaned = Jsoup.parseBodyFragment(ArticleHtmlParser.sanitize(source, url))
        assertEquals(original.select("img").map { it.attr("src") }, cleaned.select("img").map { it.attr("src") })
        assertTrue(cleaned.select("img").size > 1)
        // Text and images must remain interleaved, not flattened into two separate lists.
        val firstImage = cleaned.selectFirst("img")!!
        assertTrue(firstImage.parents().any { it.tagName() == "li" })
        assertTrue(cleaned.text().contains("максимального времени ожидания"))
    }

    @Test fun `invalid responses are not saved as articles`() {
        assertThrows(IllegalArgumentException::class.java) { ArticleHtmlParser.extract("<html>Service unavailable</html>", url) }
        assertThrows(IllegalArgumentException::class.java) { ArticleHtmlParser.extract("<main><h1>Ошибка</h1></main>", url) }
    }
}
