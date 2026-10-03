package ru.mcn.knowledgebase.presentation.article

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mcn.knowledgebase.core.di.AppModule
import ru.mcn.knowledgebase.domain.model.ArticleLink
import ru.mcn.knowledgebase.domain.model.ArticleLinkResolver
import ru.mcn.knowledgebase.data.local.ArticleMediaCache
import ru.mcn.knowledgebase.data.remote.ArticleHtmlParser
import ru.mcn.knowledgebase.data.remote.ArticleHtmlRepository
import ru.mcn.knowledgebase.data.remote.ArticlePalette
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(articleId: String, initialAnchor: String? = null,
    onOpenArticle: (String, String?) -> Unit, onHomeClick: () -> Unit, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var links by remember { mutableStateOf<ArticleLinkResolver?>(null) }
    val colors = MaterialTheme.colorScheme
    val palette = ArticlePalette(
        dark = colors.background.luminance() < 0.5f,
        background = colors.surface.cssHex(),
        text = colors.onSurface.cssHex(),
        secondaryText = colors.onSurfaceVariant.cssHex(),
        link = colors.primary.cssHex(),
        panel = colors.surfaceVariant.cssHex(),
        border = colors.outline.cssHex()
    )
    val viewModel = remember { ArticleViewModel() }
    val repository = remember { ArticleHtmlRepository(context.applicationContext) }
    val media = remember { ArticleMediaCache(context.applicationContext) }
    var article by remember(articleId) { mutableStateOf<ArticleUiModel?>(null) }
    var html by remember(articleId) { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var attempt by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(articleId, attempt) {
        loading = true
        message = null
        try {
            if (links == null) links = withContext(Dispatchers.Default) {
                ArticleLinkResolver(AppModule.knowledgeRepository.getAllArticles())
            }
            val item = viewModel.loadArticle(articleId)
            article = item
            if (item != null) {
                html = repository.cached(articleId, item.originalUrl)
                if (html == null || attempt > 0) html = repository.download(item.originalUrl)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message = if (html == null) "Не удалось загрузить оформление. Доступен сохранённый текст. Подключитесь к сети и нажмите «Обновить»."
                else "Обновление недоступно. Показана сохранённая статья."
        } finally {
            loading = false
        }
    }
    Scaffold(containerColor = colors.surface, snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(title = { Text("База знаний", style = MaterialTheme.typography.titleMedium) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface), navigationIcon = {
            IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
        }, actions = {
            IconButton(onClick = onHomeClick) { Icon(Icons.Default.Home, "Главная страница") }
            IconButton(onClick = { attempt++ }, enabled = !loading) { Icon(Icons.Default.Refresh, "Обновить статью") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
            val item = article
            if (item == null && !loading) {
                Text("Не удалось открыть статью.", Modifier.padding(24.dp))
                TextButton(onClick = { attempt++ }) { Text("Повторить") }
            }
            if (item != null) {
                val rawBody = html ?: "<p>" + ArticleHtmlParser.escape(item.content).replace("\n", "<br>") + "</p>"
                val body = remember(rawBody, links, item.originalUrl, articleId) {
                    links?.let { ArticleHtmlParser.canonicalLinks(rawBody, item.originalUrl, articleId, it) } ?: rawBody
                }
                val document = remember(body, item, palette) {
                    val date = item.updatedAt?.let { value ->
                        runCatching { OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrDefault(value)
                    }.orEmpty()
                    ArticleHtmlParser.page(item.title, item.breadcrumb, body, item.originalUrl, date, palette)
                }
                ArticleWebView(document, item.originalUrl, if (html != null || !loading) initialAnchor else null, attempt, media,
                    resolveLink = { url -> links?.resolve(url, articleId, item.originalUrl) ?: ArticleLink.Blocked },
                    onOpenArticle = onOpenArticle,
                    onMissingArticle = { url ->
                        scope.launch {
                            if (snackbar.showSnackbar("Этой статьи пока нет в каталоге приложения.", "Открыть сайт") == SnackbarResult.ActionPerformed) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            }
                        }
                    }, modifier = Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ArticleWebView(document: String, originalUrl: String, initialAnchor: String?, attempt: Int, media: ArticleMediaCache,
    resolveLink: (String) -> ArticleLink, onOpenArticle: (String, String?) -> Unit,
    onMissingArticle: (String) -> Unit, modifier: Modifier) {
    val background = MaterialTheme.colorScheme.surface.toArgb()
    val images = remember(document) { ArticleHtmlParser.images(document) }
    val currentImages by rememberUpdatedState(images)
    val currentResolver by rememberUpdatedState(resolveLink)
    val openArticle by rememberUpdatedState(onOpenArticle)
    val missingArticle by rememberUpdatedState(onMissingArticle)
    var savedScroll by rememberSaveable(originalUrl) { mutableIntStateOf(0) }
    val currentAnchor by rememberUpdatedState(initialAnchor)
    var anchorApplied by rememberSaveable(originalUrl) { mutableStateOf(false) }
    val restoration = remember(originalUrl) { PageRestoration() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(background)
                // The article supplies its own palette. Do not invert text or screenshots a second time.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    isForceDarkAllowed = false
                    @Suppress("DEPRECATION")
                    settings.forceDark = WebSettings.FORCE_DARK_OFF
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    settings.isAlgorithmicDarkeningAllowed = false
                }
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                        val url = request.url.toString()
                        return if (url in currentImages) media.response(url)
                        else WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(byteArrayOf()))
                    }
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return true
                        val uri = request.url
                        fun external(url: String) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        }
                        if (uri.toString() == ArticleHtmlParser.ORIGINAL_LINK) {
                            external(originalUrl)
                            return true
                        }
                        when (val link = currentResolver(uri.toString())) {
                            is ArticleLink.Internal -> openArticle(link.id, link.fragment)
                            is ArticleLink.Current -> if (link.fragment != null) {
                                val target = originalUrl.substringBefore('#') + "#" + Uri.encode(link.fragment)
                                if (uri.toString() == target) return false
                                view.loadUrl(target)
                            }
                            is ArticleLink.External -> external(link.url)
                            is ArticleLink.Missing -> missingArticle(link.url)
                            ArticleLink.Blocked -> Unit
                        }
                        return true
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        if (restoration.pending) {
                            restoration.pending = false
                            val anchor = currentAnchor
                            if (!anchorApplied && !anchor.isNullOrEmpty()) {
                                anchorApplied = true
                                view.loadUrl(originalUrl.substringBefore('#') + "#" + Uri.encode(anchor))
                            } else view.scrollTo(0, savedScroll)
                        }
                    }
                }
                setOnScrollChangeListener { _, _, y, _, _ -> if (progress == 100 && !restoration.pending) savedScroll = y }
            }
        },
        update = { view ->
            view.setBackgroundColor(background)
            val key = Triple(document, attempt, initialAnchor)
            if (view.tag != key) {
                view.tag = key
                restoration.pending = true
                view.loadDataWithBaseURL(originalUrl, document, "text/html", "UTF-8", null)
            }
        },
        onRelease = { view -> view.stopLoading(); view.destroy() }
    )
}

private class PageRestoration(var pending: Boolean = true)

private fun androidx.compose.ui.graphics.Color.cssHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)
