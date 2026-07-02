package ru.mcn.knowledgebase.presentation.article

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import ru.mcn.knowledgebase.core.ui.AppTopBar
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@Composable
fun ArticleScreen(
    articleId: String,
    onBackClick: () -> Unit,
) {

    val viewModel = remember {
        ArticleViewModel()
    }

    val context = LocalContext.current

    var article by remember {
        mutableStateOf<ArticleUiModel?>(null)
    }
    var isLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(articleId) {

        isLoading = true
        article = viewModel.loadArticle(articleId)
        isLoading = false
    }

    if (isLoading) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator()

        }

    } else {

        article?.let {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                AppTopBar(
                    title = it.title,
                    showBackButton = true,
                    onBackClick = onBackClick
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(16.dp)
                ) {

                    Text(
                        text = it.title
                    )
                    it.updatedAt?.let { date ->

                        Text(
                            text = "📅 Последнее обновление",
                            style = MaterialTheme.typography.labelMedium
                        )

                        Text(
                            text = formatDate(it.updatedAt),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }
                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    it.images.forEach { imageUrl ->

                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )
                    }

                    Text(
                        text = it.content
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Button(
                        onClick = {

                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(it.originalUrl)
                            )

                            context.startActivity(intent)
                        }
                    ) {
                        Text("Открыть оригинал статьи")
                    }

                }
            }
        }
    }
}

private fun formatDate(date: String): String {

    return try {

        OffsetDateTime
            .parse(date)
            .format(
                DateTimeFormatter.ofPattern(
                    "dd.MM.yyyy"
                )
            )

    } catch (e: Exception) {

        date
    }
}