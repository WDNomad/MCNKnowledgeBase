package ru.mcn.knowledgebase

import org.junit.Assert.*
import org.junit.Test
import org.jsoup.Jsoup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.mcn.knowledgebase.domain.model.*
import ru.mcn.knowledgebase.data.remote.ArticleHtmlParser
import java.io.File

class ArticleLinkResolverTest {
    private val sourceId = "6f1d705d-0e53-4f54-b57b-e7943b0dca59"
    private val targetId = "6349f9f0-d838-43b7-8e30-3072ace26818"
    private val source = Article(sourceId, "", "Сценарии звонков", originalUrl = "https://handbook.mcn.ru/ru/virtualnaya-ats/naznachenie-stsenariya-zvonka/stsenarii-zvonkov--$sourceId/")
    private val target = Article(targetId, "", "Элементы сценария звонков", originalUrl = "https://handbook.mcn.ru/ru/virtualnaya-ats/naznachenie-stsenariya-zvonka/elementy-stsenariya-zvonkov--$targetId/")
    private val resolver = ArticleLinkResolver(listOf(source, target))

    @Test fun `example articles resolve using the real bundled catalog`() {
        val articles: List<Article> = Gson().fromJson(File("src/main/assets/articles.json").readText(), object : TypeToken<List<Article>>() {}.type)
        val actual = ArticleLinkResolver(articles)
        assertEquals(ArticleLink.Internal(targetId, null), actual.resolve(target.originalUrl, sourceId, source.originalUrl))
        assertEquals(ArticleLink.Current(null), actual.resolve(source.originalUrl, sourceId, source.originalUrl))
    }

    @Test fun `FAQ link with old vats section resolves to queue settings in another section`() {
        val articles: List<Article> = Gson().fromJson(File("src/main/assets/articles.json").readText(), object : TypeToken<List<Article>>() {}.type)
        val faq = articles.single { it.id == "7039f7ca-36cb-4ed5-980b-39178fb8c20b" }
        val queue = "6d0124d5-2107-462a-8931-42c2d5294de6"
        assertEquals(ArticleLink.Internal(queue, null), ArticleLinkResolver(articles).resolve(
            "https://handbook.mcn.ru/ru/vats/nastroyki-marshrutizatsii/nastroyki-ocheredi--$queue/", faq.id, faq.originalUrl))
    }

    @Test fun `self links including query trailing slash and changed slug do not navigate`() {
        listOf(source.originalUrl, source.originalUrl.trimEnd('/'), source.originalUrl + "?from=article",
            "https://handbook.mcn.ru/ru/old-slug--$sourceId/").forEach {
            repeat(5) { _ -> assertEquals(ArticleLink.Current(null), resolver.resolve(it, sourceId, source.originalUrl)) }
        }
    }

    @Test fun `cached FAQ links using kb domain resolve before any browser redirect`() {
        val articles: List<Article> = Gson().fromJson(File("src/main/assets/articles.json").readText(), object : TypeToken<List<Article>>() {}.type)
        val faq = articles.single { it.id == "7039f7ca-36cb-4ed5-980b-39178fb8c20b" }
        val queue = articles.single { it.id == "6d0124d5-2107-462a-8931-42c2d5294de6" }
        val actual = ArticleLinkResolver(articles)
        val legacy = "https://kb.mcn.ru/ru/vats/nastroyki-marshrutizatsii/nastroyki-ocheredi--${queue.id}/"
        assertEquals(ArticleLink.Internal(queue.id, null), actual.resolve(legacy, faq.id, faq.originalUrl))
        val body = ArticleHtmlParser.canonicalLinks("<p><a href='$legacy'>Настройки очереди</a></p>", faq.originalUrl, faq.id, actual)
        assertEquals(queue.originalUrl, Jsoup.parseBodyFragment(body).selectFirst("a")!!.attr("href"))
    }

    @Test fun `legacy numeric links resolve only by an unambiguous title on a known knowledge host`() {
        val legacy = "https://mcntelecom.userecho.ru/knowledge-bases/13/articles/123-elements"
        assertEquals(ArticleLink.Internal(targetId, null), resolver.resolve(legacy, sourceId, source.originalUrl, target.title))
        assertEquals(ArticleLink.Missing(legacy), resolver.resolve(legacy, sourceId, source.originalUrl, "Несуществующая статья"))
        val ambiguous = ArticleLinkResolver(listOf(source, target, target.copy(id = "duplicate", originalUrl = "https://handbook.mcn.ru/ru/another--duplicate/")))
        assertEquals(ArticleLink.Missing(legacy), ambiguous.resolve(legacy, sourceId, source.originalUrl, target.title))
        val other = legacy.replace("mcntelecom.userecho.ru", "other.example")
        assertEquals(ArticleLink.External(other), resolver.resolve(other, sourceId, source.originalUrl, target.title))
    }

    @Test fun `canonical links keep fragments and leave external links and images intact`() {
        val image = "https://kb.mcn.ru/api/public/v1/s3-file/image"
        val body = ArticleHtmlParser.canonicalLinks("<p>Шаг<img src='$image'></p><a href='${target.originalUrl}#step%202'>Элементы</a><a href='https://mcn.ru/'>Сайт</a>",
            source.originalUrl, sourceId, resolver)
        val parsed = Jsoup.parseBodyFragment(body)
        assertEquals(image, parsed.selectFirst("img")!!.attr("src"))
        assertEquals(target.originalUrl + "#step%202", parsed.select("a")[0].attr("href"))
        assertEquals("https://mcn.ru/", parsed.select("a")[1].attr("href"))
    }

    @Test fun `relative links and fragments preserve destination`() {
        val relative = "../elementy-stsenariya-zvonkov--$targetId/#step-2"
        assertEquals(ArticleLink.Internal(targetId, "step-2"), resolver.resolve(relative, sourceId, source.originalUrl))
        assertEquals(ArticleLink.Current("шаг"), resolver.resolve("#%D1%88%D0%B0%D0%B3", sourceId, source.originalUrl))
    }

    @Test fun `untrusted host and non web schemes cannot become internal articles`() {
        val external = "https://other.example/ru/example--$targetId/"
        assertEquals(ArticleLink.External(external), resolver.resolve(external, sourceId, source.originalUrl))
        assertEquals(ArticleLink.Blocked, resolver.resolve("javascript:alert(1)", sourceId, source.originalUrl))
        assertEquals(ArticleLink.Blocked, resolver.resolve("file:///tmp/test", sourceId, source.originalUrl))
        assertEquals(ArticleLink.External("tel:+74950000000"), resolver.resolve("tel:+74950000000", sourceId, source.originalUrl))
    }

    @Test fun `unknown handbook article offers explicit fallback`() {
        val url = "https://handbook.mcn.ru/ru/new--11111111-1111-1111-1111-111111111111/"
        assertEquals(ArticleLink.Missing(url), resolver.resolve(url, sourceId, source.originalUrl))
    }

    @Test fun `original action is distinct from a self link in article body`() {
        val page = Jsoup.parse(ArticleHtmlParser.page(source.title, "", "<a href='${source.originalUrl}'>Сценарии звонков</a>", source.originalUrl))
        assertEquals(source.originalUrl, page.selectFirst("body > a")!!.attr("href"))
        assertEquals(ArticleHtmlParser.ORIGINAL_LINK, page.selectFirst("footer a")!!.attr("href"))
        assertEquals("Открыть оригинал статьи", page.selectFirst("footer a")!!.text())
    }
}
