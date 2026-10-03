package ru.mcn.knowledgebase.data.local

import android.content.Context
import android.util.AtomicFile
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.mcn.knowledgebase.data.remote.contentKey
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.URLConnection
import java.util.concurrent.TimeUnit

/** WebView calls this on a worker thread. Successful image responses persist across restarts. */
class ArticleMediaCache(context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
) {
    private val directory = File(context.filesDir, "article-media").apply { mkdirs() }
    companion object {
        // Shared by WebViews and bulk downloads, including separate cache instances.
        private val locks = Array(32) { Any() }
    }

    suspend fun ensureCached(url: String) = withContext(Dispatchers.IO) {
        resource(url).data.close()
    }

    suspend fun isCached(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            resource(url, downloadMissing = false).data.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun resource(url: String, downloadMissing: Boolean = true): WebResourceResponse = synchronized(locks[(url.hashCode() and Int.MAX_VALUE) % locks.size]) {
        require(url.startsWith("https://"))
        val file = AtomicFile(File(directory, contentKey(url)))
        if (!file.baseFile.exists()) {
            check(downloadMissing) { "Изображение ещё не сохранено" }
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (response.code == 429 || response.code >= 500) throw IOException("HTTP ${response.code}")
                check(response.isSuccessful)
                val body = requireNotNull(response.body)
                body.byteStream().buffered().use { input ->
                    val declared = body.contentType()?.toString()?.substringBefore(';').orEmpty()
                    val mime = declared.takeIf { it.startsWith("image/") }
                        ?: URLConnection.guessContentTypeFromStream(input).orEmpty()
                    require(mime.startsWith("image/"))
                    val output = file.startWrite()
                    try {
                        // First line stores MIME; the rest is the exact image binary.
                        output.write((mime + "\n").toByteArray())
                        check(input.copyTo(output) > 0) { "Пустое изображение" }
                        file.finishWrite(output)
                    } catch (error: Exception) {
                        file.failWrite(output)
                        throw error
                    }
                }
            }
        }
        val input = file.openRead().buffered()
        try {
            val mime = StringBuilder()
            while (true) {
                val byte = input.read()
                if (byte == -1 || byte == 10) break
                require(mime.length < 100)
                mime.append(byte.toChar())
            }
            require(mime.startsWith("image/")) { "Некорректный файл изображения" }
            input.mark(1)
            check(input.read() != -1) { "Пустое изображение" }
            input.reset()
            WebResourceResponse(mime.toString(), null, input)
        } catch (error: Exception) {
            input.close()
            file.delete()
            throw error
        }
    }

    fun response(url: String): WebResourceResponse = try {
        resource(url)
    } catch (error: Exception) {
        WebResourceResponse("image/svg+xml", "UTF-8", ByteArrayInputStream(
            """<svg xmlns="http://www.w3.org/2000/svg" width="480" height="90"><rect width="100%" height="100%" fill="#eee"/><text x="20" y="40" font-size="16" fill="#555">Изображение пока недоступно.</text><text x="20" y="65" font-size="14" fill="#555">Подключитесь к сети и обновите статью.</text></svg>""".toByteArray(Charsets.UTF_8)
        ))
    }
}
