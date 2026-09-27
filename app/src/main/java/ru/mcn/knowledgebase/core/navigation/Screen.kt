package ru.mcn.knowledgebase.core.navigation

sealed class Screen(
    val route: String
) {

    data object Categories :
        Screen("categories")

    data object Articles :
        Screen("articles/{sectionId}/{subsectionId}")

    data object Subsections :
        Screen("subsections/{sectionId}")

    data object Article :
        Screen("article/{articleId}")
}
