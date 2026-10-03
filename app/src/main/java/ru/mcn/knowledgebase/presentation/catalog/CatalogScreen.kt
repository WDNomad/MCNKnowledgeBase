package ru.mcn.knowledgebase.presentation.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mcn.knowledgebase.core.di.AppModule
import ru.mcn.knowledgebase.data.local.BundledKnowledge
import ru.mcn.knowledgebase.data.local.OfflineDownloads
import ru.mcn.knowledgebase.domain.model.Article
import ru.mcn.knowledgebase.domain.model.KnowledgeCatalog
import ru.mcn.knowledgebase.ui.theme.BrandHero
import ru.mcn.knowledgebase.ui.theme.BrandTitle
import ru.mcn.knowledgebase.ui.theme.CategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    sectionId: String? = null,
    subsectionId: String? = null,
    onFolderClick: (String) -> Unit,
    onArticleClick: (String) -> Unit,
    onBackClick: () -> Unit = {},
    onChangeAudience: (() -> Unit)? = null,
    onHomeClick: (() -> Unit)? = null
) {
    var catalog by remember { mutableStateOf<KnowledgeCatalog?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current.applicationContext
    val downloads = remember(context) { OfflineDownloads(context) }
    val downloadStatus by downloads.status.collectAsState(initial = null)
    var startingDownload by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var hiddenDownload by rememberSaveable { mutableStateOf<String?>(null) }
    fun downloadAction(action: suspend () -> Unit) {
        if (startingDownload) return
        startingDownload = true
        downloadError = null
        hiddenDownload = null
        scope.launch {
            try { action(); listState.animateScrollToItem(0) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { downloadError = "Не удалось запустить загрузку. Проверьте свободное место и повторите попытку." }
            finally { startingDownload = false }
        }
    }
    val canDownload = !startingDownload && downloadStatus?.active != true
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
                title = {
                    if (sectionId == null) BrandTitle()
                    else Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    if (sectionId != null) IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (onHomeClick != null) IconButton(onClick = onHomeClick) { Icon(Icons.Default.Home, "Главная страница") }
                    if (sectionId == null) IconButton(onClick = { scope.launch { load(true) } }, enabled = !loading) {
                        Icon(Icons.Default.Refresh, "Обновить базу знаний")
                    }
                    if (onChangeAudience != null) {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Меню") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Изменить выбор аудитории") }, onClick = {
                                menuOpen = false
                                onChangeAudience()
                            })
                        }
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
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
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
                LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item(key = "offline-download") {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            OutlinedButton(onClick = {
                                val current = catalog ?: return@OutlinedButton
                                downloadAction {
                                    val selected = withContext(Dispatchers.Default) { current.articles(sectionId, subsectionId) }
                                    downloads.enqueue(selected, title)
                                }
                            }, enabled = canDownload && !loading, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(8.dp))
                                Text(when {
                                    subsectionId != null -> "Загрузить подраздел"
                                    sectionId != null -> "Загрузить раздел"
                                    else -> "Загрузить все статьи"
                                })
                            }
                            Text("Текст и изображения для чтения без интернета. Загрузка через текущее подключение.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            if (startingDownload) {
                                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                                Text("Подготовка загрузки…", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    downloadStatus?.let { status ->
                        if (hiddenDownload != status.id.toString()) item(key = "offline-progress") {
                            OfflineDownloadPanel(status,
                                onCancel = { downloadAction { downloads.cancel(status.id) } },
                                onRetry = { downloadAction { downloads.retry(status.id) } },
                                onDismiss = { hiddenDownload = status.id.toString() },
                                enabled = !startingDownload)
                        }
                    }
                    if (count == 0) item(key = "empty") {
                        Text(if (query.isNotBlank()) "Ничего не найдено. Попробуйте другое слово." else "В этом разделе пока нет статей.", Modifier.padding(24.dp))
                    }
                    if (sectionId == null && query.isBlank()) {
                        item(key = "catalog-intro") {
                            BrandHero("Поможем разобраться", "Выберите услугу или найдите ответ через поиск.", Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp))
                        }
                    }
                    if (showArticles) {
                        items(articles.orEmpty(), key = { it.id }) { article ->
                            CatalogCard(article.title, catalog!!.breadcrumb(article), false) { onArticleClick(article.id) }
                        }
                    } else {
                        items(folders, key = { it.id }) { folder ->
                            CatalogCard(folder.title, "Статей: ${folder.articleCount}", true,
                                downloadEnabled = canDownload,
                                onDownload = {
                                    val current = catalog ?: return@CatalogCard
                                    downloadAction {
                                        val selected = withContext(Dispatchers.Default) {
                                            if (sectionId == null) current.articles(sectionId = folder.id)
                                            else current.articles(sectionId = sectionId, subsectionId = folder.id)
                                        }
                                        downloads.enqueue(selected, folder.title)
                                    }
                                }) { onFolderClick(folder.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(title: String, subtitle: String, folder: Boolean,
    downloadEnabled: Boolean = true, onDownload: (() -> Unit)? = null, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CategoryIcon(if (folder) Icons.Default.FolderOpen else Icons.AutoMirrored.Outlined.Article, secondary = !folder)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onDownload != null) IconButton(onClick = onDownload, enabled = downloadEnabled) {
                Icon(Icons.Default.Download, "Загрузить: $title", tint = if (downloadEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            } else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
