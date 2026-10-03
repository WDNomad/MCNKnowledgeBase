package ru.mcn.knowledgebase

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.mcn.knowledgebase.data.local.AudiencePreferences
import ru.mcn.knowledgebase.domain.model.Audience

@RunWith(AndroidJUnit4::class)
class AudiencePreferencesTest {
    private val preferences = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("audience_test_${System.nanoTime()}", Context.MODE_PRIVATE)

    @After fun clearTestPreferences() { preferences.edit().clear().commit() }

    @Test fun rememberedChoiceSurvivesStoreRecreation() = runBlocking {
        assertNull(AudiencePreferences(preferences).remembered())
        for (audience in Audience.entries) {
            AudiencePreferences(preferences).choose(audience, remember = true)
            assertEquals(audience, AudiencePreferences(preferences).remembered())
        }
    }

    @Test fun uncheckedChoiceClearsPreviouslyRememberedChoice() = runBlocking {
        AudiencePreferences(preferences).choose(Audience.PRIVATE, remember = true)
        AudiencePreferences(preferences).choose(Audience.BUSINESS, remember = false)
        assertNull(AudiencePreferences(preferences).remembered())
        AudiencePreferences(preferences).choose(Audience.PRIVATE, remember = false)
        assertNull(AudiencePreferences(preferences).remembered())
    }
}
