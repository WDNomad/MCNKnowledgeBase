package ru.mcn.knowledgebase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mcn.knowledgebase.data.remote.OfflineArticleDownloader
import ru.mcn.knowledgebase.domain.model.*
import java.io.IOException

class OfflineDownloadTest {
    private val articles = (1..5).map { OfflineArticle("$it", "https://handbook.mcn.ru/ru/$it/", "Статья $it") }

    @Test fun `resume starts at three of six and never downloads ready articles`() = runBlocking {
        val six = articles + OfflineArticle("6", "https://handbook.mcn.ru/ru/6/", "Статья 6")
        val plan = OfflineResumePlan.create(six.drop(3) + six.take(3)) { it.id.toInt() <= 3 }
        assertEquals(OfflineProgress(3, 3, 0), plan.progress)
        val downloaded = mutableListOf<String>()
        val counts = mutableListOf(plan.progress.saved)
        val result = OfflineBatch().run(plan.articles, plan.progress, { downloaded += it.id }, { counts += it.saved })
        assertEquals(listOf("4", "5", "6"), downloaded)
        assertEquals(listOf(3, 4, 5, 6), counts)
        assertEquals(OfflineProgress(6, 6, 0), result)
    }

    @Test fun `resume retries incomplete articles without carrying old error count`() = runBlocking {
        val plan = OfflineResumePlan.create(articles) { it.id in setOf("1", "3", "5") }
        val attempted = mutableListOf<String>()
        assertEquals(OfflineProgress(3, 3, 0), plan.progress)
        val result = OfflineBatch().run(plan.articles, plan.progress, { attempted += it.id }, {})
        assertEquals(listOf("2", "4"), attempted)
        assertEquals(OfflineProgress(5, 5, 0), result)
    }

    @Test fun `second cancellation preserves cumulative ready count on next resume`() = runBlocking {
        val ready = mutableSetOf("1", "2")
        val first = OfflineResumePlan.create(articles) { it.id in ready }
        try {
            OfflineBatch().run(first.articles, first.progress, {
                if (it.id == "4") throw CancellationException()
                ready += it.id
            }, {})
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        val next = OfflineResumePlan.create(first.articles) { it.id in ready }
        assertEquals(OfflineProgress(3, 3, 0), next.progress)
    }

    @Test fun `article is complete only after all distinct inline images are cached`() = runBlocking {
        var html: String? = null
        var htmlRequests = 0
        val cachedImages = mutableSetOf<String>()
        val requestedImages = mutableListOf<String>()
        var failSecondImage = true
        val downloader = OfflineArticleDownloader({ html }, {
            htmlRequests++
            "<p>Шаг</p><img src='https://mcn.ru/1.png'><p>Далее</p><img src='https://mcn.ru/2.png'><img src='https://mcn.ru/1.png'>".also { html = it }
        }, { url ->
            if (url !in cachedImages) {
                requestedImages += url
                if (url.endsWith("2.png") && failSecondImage) throw IOException("offline")
                cachedImages += url
            }
        })
        var progress = OfflineProgress()
        OfflineBatch().run(articles.take(1), progress, downloader::download, { progress = it })
        assertEquals(OfflineProgress(1, 0, 1), progress)
        failSecondImage = false
        progress = OfflineBatch().run(articles.take(1), OfflineProgress(), downloader::download, {})
        assertEquals(OfflineProgress(1, 1, 0), progress)
        assertEquals(1, htmlRequests)
        assertEquals(listOf("https://mcn.ru/1.png", "https://mcn.ru/2.png", "https://mcn.ru/2.png"), requestedImages)
    }

    @Test fun `interruption resumes at first unfinished article`() = runBlocking {
        var savedProgress = OfflineProgress()
        val visited = mutableListOf<String>()
        try {
            OfflineBatch().run(articles, savedProgress, {
                if (it.id == "3") throw CancellationException()
                visited += it.id
            }, { savedProgress = it })
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        assertEquals(OfflineProgress(2, 2, 0), savedProgress)
        val final = OfflineBatch().run(articles, savedProgress, { visited += it.id }, {})
        assertEquals(articles.map { it.id }, visited)
        assertEquals(OfflineProgress(5, 5, 0), final)
    }

    @Test fun `broken article does not mark its images saved or block remaining articles`() = runBlocking {
        val final = OfflineBatch().run(articles, OfflineProgress(), {
            if (it.id == "2") throw IOException("404")
        }, {})
        assertEquals(OfflineProgress(5, 4, 1), final)
    }

    @Test fun `repeated failures stop batch rather than hammer unavailable server`() = runBlocking {
        var attempts = 0
        val final = OfflineBatch().run(articles, OfflineProgress(), { attempts++; throw IOException() }, {})
        assertEquals(3, attempts)
        assertEquals(OfflineProgress(3, 0, 3), final)
    }

    @Test fun `bundled HTML does not require downloading the page`() = runBlocking {
        var fetched = false
        OfflineArticleDownloader({ "<p>Статья без изображений</p>" }, { fetched = true; "" }, {
            fail("No images expected")
        }).download(articles.first())
        assertFalse(fetched)
    }

    @Test fun `several missing documents still allow later articles to download`() = runBlocking {
        val final = OfflineBatch().run(articles, OfflineProgress(), {
            if (it.id.toInt() <= 3) throw IllegalStateException("HTTP 404")
        }, {})
        assertEquals(OfflineProgress(5, 2, 3), final)
    }
}
