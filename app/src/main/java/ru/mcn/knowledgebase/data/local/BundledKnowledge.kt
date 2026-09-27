package ru.mcn.knowledgebase.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.mcn.knowledgebase.AppContextProvider
import ru.mcn.knowledgebase.data.remote.dto.ArticleDto

object BundledKnowledge {
    suspend fun articles(): List<ArticleDto> = read("articles.json", object : TypeToken<List<ArticleDto>>() {})
    suspend fun titles(): Map<String, String> = read("catalog_titles.json", object : TypeToken<Map<String, String>>() {})

    private suspend fun <T> read(name: String, type: TypeToken<T>): T = withContext(Dispatchers.IO) {
        AppContextProvider.context.assets.open(name).bufferedReader().use { Gson().fromJson(it, type.type) }
    }
}
