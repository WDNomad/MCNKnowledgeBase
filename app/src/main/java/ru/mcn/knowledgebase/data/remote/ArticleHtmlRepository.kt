package ru.mcn.knowledgebase.data.remote

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ArticleHtmlRepository(private val context: Context) {
    private val client = OkHttpClient.Builder().callTimeout(35, TimeUnit.SECONDS).build()

    suspend fun cached(id: String, url: String): String? = withContext(Dispatchers.IO) {
        val file = htmlFile(url)
        val html = if (file.exists()) AtomicFile(file).openRead().bufferedReader().use { it.readText() }
        else runCatching { context.assets.open("article-html/$id.html").bufferedReader().use { it.readText() } }.getOrNull()
        html?.let { ArticleHtmlParser.sanitize(it, url) }
    }

    suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        require(url.startsWith("https://handbook.mcn.ru/ru/"))
        val html = client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            ArticleHtmlParser.extract(requireNotNull(response.body).string(), url)
        }
        val file = AtomicFile(htmlFile(url))
        val output = file.startWrite()
        try {
            output.write(html.toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
        html
    }

    private fun htmlFile(url: String): File = File(context.filesDir, "article-html").apply { mkdirs() }
        .resolve("${contentKey(url)}.html")
}

internal fun contentKey(url: String): String = MessageDigest.getInstance("SHA-256")
    .digest(url.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
