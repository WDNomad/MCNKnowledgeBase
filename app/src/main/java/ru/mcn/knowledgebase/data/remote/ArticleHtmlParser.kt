package ru.mcn.knowledgebase.data.remote

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import ru.mcn.knowledgebase.domain.model.ArticleLink
import ru.mcn.knowledgebase.domain.model.ArticleLinkResolver

data class ArticlePalette(
    val dark: Boolean = false,
    val background: String = "#FFFFFF",
    val text: String = "#1C1B1F",
    val secondaryText: String = "#49454F",
    val link: String = "#006493",
    val panel: String = "#F0EEF3",
    val border: String = "#79747E"
)

/** Keep the document tree intact: splitting text and images destroys their reading order. */
object ArticleHtmlParser {
    const val ORIGINAL_LINK = "mcnkb://open-original"

    /** Resolve aliases while the link text is available; this also works for already cached HTML. */
    fun canonicalLinks(html: String, url: String, currentId: String, resolver: ArticleLinkResolver): String {
        val body = Jsoup.parseBodyFragment(html, url).body()
        for (anchor in body.select("a[href]")) {
            when (val link = resolver.resolve(anchor.attr("href"), currentId, url, anchor.text())) {
                is ArticleLink.Internal -> {
                    val fragment = link.fragment?.let { "#" + java.net.URI(null, null, it).rawFragment }.orEmpty()
                    resolver.originalUrl(link.id)?.let { anchor.attr("href", it.substringBefore('#') + fragment) }
                }
                is ArticleLink.Current -> {
                    val fragment = link.fragment?.let { "#" + java.net.URI(null, null, it).rawFragment }.orEmpty()
                    anchor.attr("href", url.substringBefore('#') + fragment)
                }
                else -> Unit
            }
        }
        return body.html()
    }
    fun extract(page: String, url: String): String {
        val document = Jsoup.parse(page, url)
        val heading = requireNotNull(document.selectFirst("main h1")) { "Не найден текст статьи" }
        val body = Element("div").apply { setBaseUri(url) }
        for (sibling in heading.nextElementSiblings()) {
            // MCN places related articles after the body in a separate section.
            if (sibling.tagName() == "section") break
            if (sibling.tagName() in listOf("nav", "script", "aside") || sibling.hasClass("not-prose")) continue
            body.appendChild(sibling.clone())
        }
        require(body.text().isNotBlank() || body.select("img").isNotEmpty()) { "Пустой текст статьи" }
        return sanitize(body.html(), url)
    }

    fun sanitize(html: String, url: String): String {
        val body = Jsoup.parseBodyFragment(html, url).body()
        body.select("script,style,iframe,form,object,embed,nav,aside").remove()
        body.select("img").forEach { image ->
            val absolute = image.absUrl("src")
            if (!absolute.startsWith("https://")) image.remove()
            else image.attr("src", absolute)
        }
        val safe = Safelist.relaxed()
            .addTags("div", "span", "figure", "figcaption", "hr", "details", "summary")
            .addAttributes(":all", "id")
            .addAttributes("a", "href")
            .addAttributes("ol", "start")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan")
            .addProtocols("a", "href", "#")
            .removeProtocols("img", "src", "http")
        return Jsoup.clean(body.html(), url, safe)
    }

    fun images(html: String): Set<String> = Jsoup.parseBodyFragment(html).select("img[src]")
        .map { it.attr("src") }.toSet()

    fun escape(text: String): String = Element("span").text(text).html().replace("\"", "&quot;")

    fun page(title: String, breadcrumb: String, body: String, originalUrl: String, date: String = "", palette: ArticlePalette = ArticlePalette()): String = """
        <!doctype html><html lang="ru"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="color-scheme" content="${if (palette.dark) "dark" else "light"}">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src https:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
        <style>
        :root { color-scheme: only ${if (palette.dark) "dark" else "light"}; background:${palette.background}; color:${palette.text}; }
        * { box-sizing: border-box; }
        body { margin:0 auto; max-width:840px; padding:20px 24px 40px; font:17px/1.65 sans-serif; overflow-wrap:anywhere; background:${palette.background}; color:${palette.text}; }
        h1 { font-size:28px; line-height:1.25; margin:20px 0; letter-spacing:-.4px; } h2 { font-size:22px; margin-top:28px; } h3 { font-size:20px; }
        img { max-width:100%; height:auto; display:block; margin:20px auto; border-radius:8px; }
        figure { margin:16px 0; } figcaption { font-size:14px; color:${palette.secondaryText}; }
        a, a:visited { color:${palette.link}; text-decoration:underline; } p { margin:0 0 16px; } li { margin:8px 0; }
        table { display:block; max-width:100%; overflow-x:auto; border-collapse:collapse; }
        td,th { border:1px solid ${palette.border}; padding:8px; min-width:80px; color:${palette.text}; }
        th { background:${palette.panel}; }
        pre { overflow-x:auto; padding:16px; border-radius:12px; background:${palette.panel}; color:${palette.text}; white-space:pre; }
        code { color:inherit; } hr { border:0; border-top:1px solid ${palette.border}; }
        blockquote { border-left:3px solid ${palette.link}; background:${palette.panel}; padding:16px; margin:20px 0; border-radius:0 12px 12px 0; }
        blockquote p:last-child { margin-bottom:0; }
        .path { font-size:13px; color:${palette.secondaryText}; } footer { margin-top:32px; padding-top:20px; border-top:1px solid ${palette.border}; font-size:15px; }
        </style></head><body><div class="path">${escape(breadcrumb)}</div>
        <h1>${escape(title)}</h1>${if (date.isBlank()) "" else "<p class=\"path\">Обновлено ${escape(date)}</p>"}$body
        <footer><a href="$ORIGINAL_LINK">Открыть оригинал статьи</a></footer></body></html>
    """.trimIndent()
}
