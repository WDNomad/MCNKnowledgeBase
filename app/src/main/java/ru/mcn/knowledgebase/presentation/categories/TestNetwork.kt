package ru.mcn.knowledgebase.presentation.categories

import android.util.Log
import ru.mcn.knowledgebase.core.network.RetrofitProvider

class TestNetwork {

    suspend fun test() {

        val categories =
            RetrofitProvider.api.getCategories()

        Log.d(
            "MCN_TEST",
            categories.toString()
        )
    }
}