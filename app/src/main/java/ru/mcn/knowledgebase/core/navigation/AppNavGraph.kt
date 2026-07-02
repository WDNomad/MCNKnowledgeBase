package ru.mcn.knowledgebase.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.mcn.knowledgebase.presentation.article.ArticleScreen
import ru.mcn.knowledgebase.presentation.articles.ArticlesScreen
import ru.mcn.knowledgebase.presentation.categories.CategoriesScreen

@Composable
    fun AppNavGraph() {

        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Screen.Categories.route
        ) {

            composable(Screen.Categories.route) {

                CategoriesScreen(

                    onCategoryClick = { categoryId ->

                        navController.navigate(
                            "articles/$categoryId"
                        )
                    },

                    onArticleClick = { articleId ->

                        navController.navigate(
                            "article/$articleId"
                        )
                    }
                )
            }

            composable(
                route = Screen.Articles.route,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val categoryId =
                    backStackEntry.arguments
                        ?.getString("categoryId")
                        ?: ""

                ArticlesScreen(
                    categoryId = categoryId,
                    onArticleClick = { articleId ->

                        navController.navigate(
                            "article/$articleId"
                        )
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Article.route,
                arguments = listOf(
                    navArgument("articleId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val articleId =
                    backStackEntry.arguments
                        ?.getString("articleId")
                        ?: ""

                ArticleScreen(
                    articleId = articleId,
                ) {
                    navController.popBackStack()
                }
            }
        }
    }