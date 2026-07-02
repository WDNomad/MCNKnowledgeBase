package ru.mcn.knowledgebase.presentation.articles

import ru.mcn.knowledgebase.core.ui.AppTopBar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import ru.mcn.knowledgebase.core.ui.ListItemCard


@Composable
fun ArticlesScreen(
    categoryId: String,
    onArticleClick: (String) -> Unit,
    onBackClick: () -> Unit
) {

    val viewModel = remember {
        ArticlesViewModel()
    }

    var articles by remember {
        mutableStateOf<List<ArticleUiModel>>(emptyList())
    }
    var searchQuery by remember {
        mutableStateOf("")
    }
    LaunchedEffect(categoryId) {

        articles =
            viewModel.loadArticles(categoryId)
    }
    val filteredArticles = articles.filter {

        it.title.contains(
            searchQuery,
            ignoreCase = true
        )
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        AppTopBar(
            title = "Статьи",
            showBackButton = true,
            onBackClick = onBackClick
        )
        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            label = {
                Text("Поиск статьи")
            },
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            items(filteredArticles) { article ->

                ListItemCard(
                    title = article.title,
                    onClick = {
                        onArticleClick(article.id)
                    }
                )
            }
        }
    }
}