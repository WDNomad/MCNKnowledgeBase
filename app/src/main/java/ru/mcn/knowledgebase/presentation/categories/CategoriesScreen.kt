package ru.mcn.knowledgebase.presentation.categories

import ru.mcn.knowledgebase.core.ui.ListItemCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.mcn.knowledgebase.core.ui.AppTopBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import ru.mcn.knowledgebase.presentation.articles.ArticleUiModel

@Composable
fun CategoriesScreen(
    onCategoryClick: (String) -> Unit,
    onArticleClick: (String) -> Unit
) {

    val viewModel = remember {
        CategoriesViewModel()
    }

    var categories by remember {
        mutableStateOf<List<CategoryUiModel>>(emptyList())
    }
    var articles by remember {
        mutableStateOf<List<ArticleUiModel>>(emptyList())
    }
    var searchText by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        categories = viewModel.loadCategories()

        articles = viewModel.loadAllArticles()
    }
    val filteredArticles = articles.filter {

        it.title.contains(
            searchText,
            ignoreCase = true
        )
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        AppTopBar(
            title = "MCN Knowledge Base"
        )
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            label = {
                Text("Поиск")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            if (searchText.isBlank()) {

                items(categories) { category ->

                    ListItemCard(
                        title = category.title,
                        onClick = {
                            onCategoryClick(category.id)
                        }
                    )
                }

            } else {

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
}