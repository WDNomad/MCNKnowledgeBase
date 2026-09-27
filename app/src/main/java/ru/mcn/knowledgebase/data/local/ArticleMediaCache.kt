package ru.mcn.knowledgebase.data.local

import android.content.Context
import android.util.AtomicFile
import android.webkit.WebResourceResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.mcn.knowledgebase.data.remote.contentKey
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLConnection
import java.util.concurrent.TimeUnit

/** WebView calls this on a worker thread. Successful image responses persist across restarts. */
class ArticleMediaCache(context: Context) {
    private val directory = File(context.filesDir, "article-media").apply { mkdirs() }
    private val client = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
    private val locks = Array(16) { Any() }

    fun response(url: String): WebResourceResponse = synchronized(locks[(url.hashCode() and Int.MAX_VALUE) % locks.size]) {
        val file = AtomicFile(File(directory, contentKey(url)))
        try {
            if (!file.baseFile.exists()) {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
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
                            input.copyTo(output)
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
                WebResourceResponse(mime.toString(), null, input)
            } catch (error: Exception) {
                input.close()
                throw error
            }
        } catch (error: Exception) {
            WebResourceResponse("image/svg+xml", "UTF-8", ByteArrayInputStream(
                """<svg xmlns="http://www.w3.org/2000/svg" width="480" height="90"><rect width="100%" height="100%" fill="#eee"/><text x="20" y="40" font-size="16" fill="#555">Изображение пока недоступно.</text><text x="20" y="65" font-size="14" fill="#555">Подключитесь к сети и обновите статью.</text></svg>""".toByteArray(Charsets.UTF_8)
            ))
        }
    }
}
