package ru.mcn.knowledgebase.core.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {

    private const val BASE_URL =
        "https://wdnomad.github.io/knowledge-base-data/"

    val api: KnowledgeApi by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(KnowledgeApi::class.java)
    }
}