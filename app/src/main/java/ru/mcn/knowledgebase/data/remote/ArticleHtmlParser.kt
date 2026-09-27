package ru.mcn.knowledgebase.data.remote

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist

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
        body { margin:0; padding:20px; font:17px/1.6 sans-serif; overflow-wrap:anywhere; background:${palette.background}; color:${palette.text}; }
        h1 { font-size:25px; line-height:1.3; } h2 { font-size:22px; } h3 { font-size:20px; }
        img { max-width:100%; height:auto; display:block; margin:16px auto; }
        figure { margin:16px 0; } figcaption { font-size:14px; color:${palette.secondaryText}; }
        a, a:visited { color:${palette.link}; text-decoration:underline; } p { margin:0 0 16px; } li { margin:8px 0; }
        table { display:block; max-width:100%; overflow-x:auto; border-collapse:collapse; }
        td,th { border:1px solid ${palette.border}; padding:8px; min-width:80px; color:${palette.text}; }
        th { background:${palette.panel}; }
        pre { overflow-x:auto; padding:12px; background:${palette.panel}; color:${palette.text}; white-space:pre; }
        code { color:inherit; } hr { border:0; border-top:1px solid ${palette.border}; }
        blockquote { border-left:3px solid ${palette.link}; padding-left:16px; margin-left:0; }
        .path { font-size:13px; color:${palette.secondaryText}; } footer { margin-top:28px; }
        </style></head><body><div class="path">${escape(breadcrumb)}</div>
        <h1>${escape(title)}</h1>${if (date.isBlank()) "" else "<p class=\"path\">Обновлено ${escape(date)}</p>"}$body
        <footer><a href="${escape(originalUrl)}">Открыть оригинал статьи</a></footer></body></html>
    """.trimIndent()
}
