package ru.mcn.knowledgebase.core.navigation

import android.net.Uri
import androidx.navigation.NavController

internal fun NavController.openArticle(id: String, fragment: String? = null) {
    if (currentBackStackEntry?.arguments?.getString("articleId") == id) return
    val route = "article/${Uri.encode(id)}" + (fragment?.let { "?anchor=${Uri.encode(it)}" } ?: "")
    // All articles share one destination. SingleTop replaces its arguments and loses the previous article.
    navigate(route)
}
