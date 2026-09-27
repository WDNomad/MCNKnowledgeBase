"""Build an offline snapshot from MCN sitemap + the sibling knowledge-base-data export.

Run with Python 3: python tools/build_catalog.py --sitemap sitemap-source.xml
Missing documents are fetched once with curl; failures abort publication.
Existing articles are reused to avoid crawling the whole handbook on every build.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from html.parser import HTMLParser
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
from urllib.parse import urlparse
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
UUID = re.compile(r"--([0-9a-f-]{36})/$")


@dataclass
class Node:
    tag: str
    attrs: dict = field(default_factory=dict)
    children: list = field(default_factory=list)
    parent: object = None

    def walk(self, tag=None):
        if tag is None or self.tag == tag:
            yield self
        for child in self.children:
            if isinstance(child, Node):
                yield from child.walk(tag)

    def text(self):
        return "".join(c.text() if isinstance(c, Node) else c for c in self.children)

    def html(self):
        attrs = "".join(f' {key}="{escape(value or "", quote=True)}"' for key, value in self.attrs.items())
        start = f"<{self.tag}{attrs}>"
        if self.tag in {"br", "hr", "img", "input", "source", "wbr"}:
            return start
        return start + "".join(c.html() if isinstance(c, Node) else escape(c) for c in self.children) + f"</{self.tag}>"


class Document(HTMLParser):
    def __init__(self, html):
        super().__init__(convert_charrefs=True)
        self.root = Node("root")
        self.current = self.root
        self.feed(html)

    def handle_starttag(self, tag, attrs):
        node = Node(tag, dict(attrs), parent=self.current)
        self.current.children.append(node)
        if tag not in {"area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"}:
            self.current = node

    def handle_startendtag(self, tag, attrs):
        self.handle_starttag(tag, attrs)
        self.handle_endtag(tag)

    def handle_endtag(self, tag):
        node = self.current
        while node.parent is not None:
            if node.tag == tag:
                self.current = node.parent
                return
            node = node.parent

    def handle_data(self, data):
        self.current.children.append(data)


def extract(html, url):
    doc = Document(html).root
    h1 = next(doc.walk("h1"))
    metadata = next(json.loads(n.text()) for n in doc.walk("script") if n.attrs.get("type") == "application/ld+json")
    body = []
    body_html = []
    images = []
    started = False
    for child in h1.parent.children:
        if child is h1:
            started = True
            continue
        if not started or not isinstance(child, Node):
            continue
        if child.tag == "section":
            break
        if child.tag in {"nav", "script"} or "not-prose" in child.attrs.get("class", ""):
            continue
        body_html.append(child.html())
        text = child.text().strip()
        if text:
            body.append(text)
        images.extend(n.attrs["src"] for n in child.walk("img") if n.attrs.get("src", "").startswith("https://kb.mcn.ru/"))
    assert body, f"No content extracted: {url}"
    parts = urlparse(url).path.strip("/").split("/")
    return dict(id=UUID.search(url).group(1), categoryId=parts[1], title=h1.text().strip(),
                content="\n\n".join(body), contentHtml="\n".join(body_html), originalUrl=url, updatedAt=metadata.get("dateModified"), images=list(dict.fromkeys(images)))


def fetch_article(url):
    cache = ROOT / "build" / "catalog-source" / (UUID.search(url).group(1) + ".html")
    cache.parent.mkdir(parents=True, exist_ok=True)
    article = None
    if cache.exists():
        try:
            article = extract(cache.read_text(encoding="utf-8"), url)
        except (ValueError, StopIteration, AssertionError):
            pass
    if article is None:
        temporary = cache.with_suffix(".part")
        curl = shutil.which("curl.exe") or shutil.which("curl")
        if not curl:
            raise RuntimeError("curl is required to download missing articles")
        subprocess.run([curl, "--ipv4", "--fail", "--silent", "--show-error", "--location", "--connect-timeout", "30", "--max-time", "60", "--retry", "2", url, "-o", str(temporary)], check=True)
        article = extract(temporary.read_text(encoding="utf-8"), url)
        temporary.replace(cache)
    print("Loaded:", article["title"], flush=True)
    return article


def build(sitemap):
    urls = [n.text for n in ET.parse(sitemap).getroot().iter("{http://www.sitemaps.org/schemas/sitemap/0.9}loc")
            if n.text.startswith("https://handbook.mcn.ru/ru/") and UUID.search(n.text)]
    urls = list(dict.fromkeys(urls))
    previous = json.loads((ROOT.parent / "knowledge-base-data" / "articles.json").read_text(encoding="utf-8-sig"))
    existing = {a["id"]: a for a in previous}
    missing = [url for url in urls if UUID.search(url).group(1) not in existing]
    failures = []
    with ThreadPoolExecutor(max_workers=4) as pool:
        futures = {pool.submit(fetch_article, url): url for url in missing}
        for future in as_completed(futures):
            try:
                article = future.result()
                existing[article["id"]] = article
            except Exception as error:
                failures.append(futures[future])
                print(f"Failed: {futures[future]}: {error}", flush=True)
    if failures:
        raise RuntimeError(f"Missing {len(failures)} articles; run again to retry. Successfully fetched documents are cached.")
    articles = []
    for url in urls:
        article = dict(existing[UUID.search(url).group(1)])
        article["originalUrl"] = url
        article["categoryId"] = urlparse(url).path.split("/")[2]
        articles.append(article)
    assert len(articles) == len(urls) and all(a["content"] and a["title"] for a in articles)
    output = ROOT / "app" / "src" / "main" / "assets"
    output.mkdir(parents=True, exist_ok=True)
    (output / "articles.json").write_text(json.dumps(articles, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    titles = json.loads((ROOT / "tools" / "subsection_titles.json").read_text(encoding="utf-8"))
    titles.update({c["id"]: c["title"] for c in json.loads((ROOT.parent / "knowledge-base-data" / "categories.json").read_text(encoding="utf-8-sig"))})
    (output / "catalog_titles.json").write_text(json.dumps(titles, ensure_ascii=False, indent=2), encoding="utf-8")
    export_html()
    print(f"Saved {len(articles)} articles from sitemap; fetched {len(missing)} new articles.")


def export_html():
    """Reuse downloaded source pages without re-crawling the site for every build."""
    articles = json.loads((ROOT / "app/src/main/assets/articles.json").read_text(encoding="utf-8"))
    output = ROOT / "app/src/main/assets/article-html"
    output.mkdir(parents=True, exist_ok=True)
    count = 0
    for article in articles:
        source = ROOT / "build/catalog-source" / (article["id"] + ".html")
        html = article.get("contentHtml")
        if source.exists():
            html = extract(source.read_text(encoding="utf-8"), article["originalUrl"])["contentHtml"]
        if html:
            (output / (article["id"] + ".html")).write_text(html, encoding="utf-8")
            count += 1
    print(f"Bundled HTML documents: {count}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--sitemap", type=Path, required=True)
    build(parser.parse_args().sitemap)
