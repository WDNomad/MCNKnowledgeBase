package ru.mcn.knowledgebase.data.local

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.mcn.knowledgebase.domain.model.Audience

class AudiencePreferences(private val preferences: SharedPreferences) {
    fun remembered(): Audience? = Audience.fromStoredValue(preferences.getString(KEY, null))

    suspend fun choose(audience: Audience, remember: Boolean) = withContext(Dispatchers.IO) {
        val editor = preferences.edit()
        if (remember) editor.putString(KEY, audience.storedValue) else editor.remove(KEY)
        // Finish the disk write before navigating, including when clearing an earlier choice.
        check(editor.commit()) { "Не удалось сохранить выбор. Повторите попытку." }
    }

    companion object {
        const val FILE_NAME = "audience_preferences"
        private const val KEY = "remembered_audience"
    }
}
