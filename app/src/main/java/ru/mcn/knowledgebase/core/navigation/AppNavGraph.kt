package ru.mcn.knowledgebase.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.mcn.knowledgebase.presentation.article.ArticleScreen
import ru.mcn.knowledgebase.presentation.catalog.CatalogScreen

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()
    val openArticle: (String) -> Unit = { nav.navigate("article/${Uri.encode(it)}") }
    NavHost(navController = nav, startDestination = Screen.Categories.route) {
        composable(Screen.Categories.route) {
            CatalogScreen(
                onFolderClick = { nav.navigate("subsections/${Uri.encode(it)}") },
                onArticleClick = openArticle
            )
        }
        composable("subsections/{sectionId}", arguments = listOf(navArgument("sectionId") { type = NavType.StringType })) { entry ->
            val sectionId = requireNotNull(entry.arguments?.getString("sectionId"))
            CatalogScreen(
                sectionId = sectionId,
                onFolderClick = { nav.navigate("articles/${Uri.encode(sectionId)}/${Uri.encode(it)}") },
                onArticleClick = openArticle,
                onBackClick = { nav.popBackStack() }
            )
        }
        composable("articles/{sectionId}/{subsectionId}", arguments = listOf(
            navArgument("sectionId") { type = NavType.StringType },
            navArgument("subsectionId") { type = NavType.StringType }
        )) { entry ->
            CatalogScreen(
                sectionId = requireNotNull(entry.arguments?.getString("sectionId")),
                subsectionId = requireNotNull(entry.arguments?.getString("subsectionId")),
                onFolderClick = {}, onArticleClick = openArticle,
                onBackClick = { nav.popBackStack() }
            )
        }
        composable(Screen.Article.route, arguments = listOf(navArgument("articleId") { type = NavType.StringType })) { entry ->
            ArticleScreen(requireNotNull(entry.arguments?.getString("articleId"))) { nav.popBackStack() }
        }
    }
}
