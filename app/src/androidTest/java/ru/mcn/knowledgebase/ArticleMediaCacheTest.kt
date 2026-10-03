package ru.mcn.knowledgebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.mcn.knowledgebase.data.local.ArticleMediaCache
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ArticleMediaCacheTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun savedImageSurvivesCacheRecreationWithoutAnotherRequest() = runBlocking {
        val url = "https://example.invalid/${UUID.randomUUID()}.png"
        val bytes = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10, 1)
        var requests = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(bytes.toResponseBody("image/png".toMediaType())).build()
        }.build()
        ArticleMediaCache(context, client).ensureCached(url)
        val restored = ArticleMediaCache(context, client).response(url)
        assertEquals("image/png", restored.mimeType)
        assertArrayEquals(bytes, restored.data.use { it.readBytes() })
        assertEquals(1, requests)
    }

    @Test fun failedImageIsNotSavedAndPlaceholderDoesNotCountAsDownloadSuccess() = runBlocking {
        val url = "https://example.invalid/${UUID.randomUUID()}.png"
        var requests = 0
        val client = OkHttpClient.Builder().addInterceptor {
            requests++
            throw IOException("No connection")
        }.build()
        val cache = ArticleMediaCache(context, client)
        assertFalse(cache.isCached(url))
        assertEquals(0, requests)
        try {
            cache.ensureCached(url)
            fail("Expected failure")
        } catch (_: IOException) { }
        val fallback = cache.response(url)
        assertEquals("image/svg+xml", fallback.mimeType)
        fallback.data.close()
        assertEquals(2, requests)
    }
}
