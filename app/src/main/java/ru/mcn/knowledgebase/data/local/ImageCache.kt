package ru.mcn.knowledgebase.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.mcn.knowledgebase.AppContextProvider
import java.io.File
import java.util.concurrent.TimeUnit

object ImageCache {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun cacheImages(urls: List<String>): List<String> {
        return urls.map { cacheImage(it) }
    }

    suspend fun cacheImage(url: String): String {
        if (isLocalPath(url)) {
            return url
        }

        return withContext(Dispatchers.IO) {
            try {
                val file = fileForUrl(url)
                if (file.exists() && file.length() > 0L) {
                    return@withContext file.absolutePath
                }

                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext url
                    }

                    response.body?.byteStream()?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                file.absolutePath
            } catch (e: Exception) {
                url
            }
        }
    }

    fun resolve(pathOrUrl: String): String {
        if (isLocalPath(pathOrUrl) && File(pathOrUrl).exists()) {
            return pathOrUrl
        }

        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            val cached = fileForUrl(pathOrUrl)
            if (cached.exists() && cached.length() > 0L) {
                return cached.absolutePath
            }
        }

        return pathOrUrl
    }

    fun resolveAll(pathsOrUrls: List<String>): List<String> {
        return pathsOrUrls.map { resolve(it) }
    }

    fun imageModel(pathOrUrl: String): Any {
        val resolved = resolve(pathOrUrl)
        return if (isLocalPath(resolved) && File(resolved).exists()) {
            File(resolved)
        } else {
            pathOrUrl
        }
    }

    private fun isLocalPath(value: String): Boolean {
        return value.startsWith("/")
    }

    private fun cacheDir(): File {
        return File(AppContextProvider.context.filesDir, "article-images").apply {
            mkdirs()
        }
    }

    private fun fileForUrl(url: String): File {
        val fileName = url
            .substringAfterLast('/')
            .substringBefore('?')
            .takeIf { it.isNotBlank() }
            ?: url.hashCode().toUInt().toString(16)

        return File(cacheDir(), fileName)
    }
}
