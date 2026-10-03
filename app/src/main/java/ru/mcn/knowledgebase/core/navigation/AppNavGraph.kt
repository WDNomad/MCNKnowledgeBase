package ru.mcn.knowledgebase.core.navigation

import android.net.Uri
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.mcn.knowledgebase.presentation.article.ArticleScreen
import ru.mcn.knowledgebase.presentation.catalog.CatalogScreen
import ru.mcn.knowledgebase.presentation.audience.AudienceScreen
import ru.mcn.knowledgebase.data.local.AudiencePreferences

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()
    val context = LocalContext.current.applicationContext
    val preferences = remember { AudiencePreferences(context.getSharedPreferences(AudiencePreferences.FILE_NAME, Context.MODE_PRIVATE)) }
    val startRoute = rememberSaveable { preferences.remembered()?.startRoute ?: "audience" }
    val changeAudience: () -> Unit = { nav.navigate("audience") { launchSingleTop = true } }
    val backToCatalog: () -> Unit = {
        if (!nav.popBackStack()) nav.navigate(Screen.Categories.route) { launchSingleTop = true }
    }
    val home: () -> Unit = {
        nav.navigate(preferences.remembered()?.startRoute ?: "audience") {
            popUpTo(nav.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
    val openLinkedArticle: (String, String?) -> Unit = { id, fragment ->
        nav.openArticle(id, fragment)
    }
    val openArticle: (String) -> Unit = { openLinkedArticle(it, null) }
    NavHost(navController = nav, startDestination = startRoute) {
        composable("audience") {
            AudienceScreen(
                preferences = preferences,
                onChoose = { audience ->
                    nav.navigate(audience.startRoute) {
                        popUpTo(nav.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = if (nav.previousBackStackEntry != null) { { nav.popBackStack(); Unit } } else null
            )
        }
        composable(Screen.Categories.route) {
            CatalogScreen(
                onFolderClick = { nav.navigate("subsections/${Uri.encode(it)}") },
                onArticleClick = openArticle,
                onHomeClick = home,
                onChangeAudience = changeAudience
            )
        }
        composable("subsections/{sectionId}", arguments = listOf(navArgument("sectionId") { type = NavType.StringType })) { entry ->
            val sectionId = requireNotNull(entry.arguments?.getString("sectionId"))
            CatalogScreen(
                sectionId = sectionId,
                onFolderClick = { nav.navigate("articles/${Uri.encode(sectionId)}/${Uri.encode(it)}") },
                onArticleClick = openArticle,
                onHomeClick = home,
                onBackClick = backToCatalog,
                onChangeAudience = changeAudience
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
                onHomeClick = home,
                onBackClick = backToCatalog,
                onChangeAudience = changeAudience
            )
        }
        composable(Screen.Article.route + "?anchor={anchor}", arguments = listOf(
            navArgument("articleId") { type = NavType.StringType },
            navArgument("anchor") { type = NavType.StringType; nullable = true; defaultValue = null }
        )) { entry ->
            ArticleScreen(requireNotNull(entry.arguments?.getString("articleId")),
                initialAnchor = entry.arguments?.getString("anchor"),
                onOpenArticle = openLinkedArticle, onHomeClick = home,
                onBackClick = { nav.popBackStack() })
        }
    }
}
