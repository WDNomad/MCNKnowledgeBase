package ru.mcn.knowledgebase.core.navigation

sealed class Screen(
    val route: String
) {

    data object Categories :
        Screen("categories")

    data object Articles :
        Screen("articles/{categoryId}")

    data object Article :
        Screen("article/{articleId}")
}