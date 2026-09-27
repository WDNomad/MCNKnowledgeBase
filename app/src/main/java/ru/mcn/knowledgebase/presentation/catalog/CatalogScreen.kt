package ru.mcn.knowledgebase.presentation.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mcn.knowledgebase.core.di.AppModule
import ru.mcn.knowledgebase.data.local.BundledKnowledge
import ru.mcn.knowledgebase.domain.model.Article
import ru.mcn.knowledgebase.domain.model.KnowledgeCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    sectionId: String? = null,
    subsectionId: String? = null,
    onFolderClick: (String) -> Unit,
    onArticleClick: (String) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var catalog by remember { mutableStateOf<KnowledgeCatalog?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    suspend fun load(refresh: Boolean) {
        loading = true
        error = null
        try {
            if (refresh) AppModule.knowledgeRepository.refresh()
            val articles = AppModule.knowledgeRepository.getAllArticles()
            val titles = BundledKnowledge.titles()
            catalog = withContext(Dispatchers.Default) { KnowledgeCatalog(articles, titles) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = if (catalog == null) "Не удалось загрузить базу знаний. Повторите попытку."
                else "Не удалось обновить базу. Доступна сохранённая копия."
        } finally {
            loading = false
        }
    }
    LaunchedEffect(Unit) { load(false) }
    val title = when {
        subsectionId != null -> catalog?.title(subsectionId) ?: "Статьи"
        sectionId != null -> catalog?.title(sectionId) ?: "Подразделы"
        else -> "База знаний MCN"
    }
    val searchLabel = when {
        subsectionId != null -> "Поиск в подразделе"
        sectionId != null -> "Поиск в разделе"
        else -> "Поиск по всей базе знаний"
    }
    val showArticles = query.isNotBlank() || subsectionId != null
    val folders = remember(catalog, sectionId) {
        if (sectionId == null) catalog?.sections().orEmpty() else catalog?.subsections(sectionId).orEmpty()
    }
    val articles by produceState<List<Article>?>(null, catalog, sectionId, subsectionId, query) {
        value = null
        value = withContext(Dispatchers.Default) {
            catalog?.articles(sectionId, subsectionId, query).orEmpty()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    if (sectionId != null) IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (sectionId == null) TextButton(onClick = { scope.launch { load(true) } }, enabled = !loading) {
                        Text("Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (sectionId != null) Text(
                if (subsectionId == null) "База знаний / Раздел" else "${catalog?.title(sectionId).orEmpty()} / Подраздел",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text(searchLabel) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Очистить поиск") } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large
            )
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { message ->
                Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { scope.launch { load(catalog != null) } }, enabled = !loading) { Text("Повторить") }
            }
            if (catalog != null) {
                val count = if (showArticles) articles?.size else folders.size
                Text(
                    when {
                        query.isNotBlank() -> "Найдено статей: ${count ?: "…"}"
                        subsectionId != null -> "Статьи · ${count ?: "…"}"
                        sectionId != null -> "Подразделы · $count"
                        else -> "Все разделы · $count"
                    },
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleSmall
                )
                if (count == 0) {
                    Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(if (query.isNotBlank()) "Ничего не найдено. Попробуйте другое слово." else "В этом разделе пока нет статей.")
                    }
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showArticles) {
                        items(articles.orEmpty(), key = { it.id }) { article ->
                            CatalogCard(article.title, catalog!!.breadcrumb(article), false) { onArticleClick(article.id) }
                        }
                    } else {
                        items(folders, key = { it.id }) { folder ->
                            CatalogCard(folder.title, "Статей: ${folder.articleCount}", true) { onFolderClick(folder.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(title: String, subtitle: String, folder: Boolean, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (folder) Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
