"""Export launcher previews and legacy WebP resources from the Android vectors.

Requires Pillow. The source artwork lives in app/src/main/res/drawable.
Only absolute M/L/C/Z paths and linear gradients used by these assets are supported.
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
ART = ROOT / "artwork"
ANDROID = "{http://schemas.android.com/apk/res/android}"
SIZE = 864
SCALE = SIZE / 108


def contours(data):
    tokens = re.findall(r"[A-Za-z]|-?\d+(?:\.\d+)?", data)
    position = 0
    points = []
    cursor = (0, 0)
    while position < len(tokens):
        command = tokens[position]
        position += 1
        count = {"M": 2, "L": 2, "C": 6, "Z": 0}[command]
        numbers = list(map(float, tokens[position:position + count]))
        position += count
        if command in ("M", "L"):
            cursor = tuple(numbers)
            points.append(cursor)
        elif command == "C":
            x0, y0 = cursor
            x1, y1, x2, y2, x3, y3 = numbers
            for step in range(1, 33):
                t = step / 32
                u = 1 - t
                points.append((u**3*x0 + 3*u*u*t*x1 + 3*u*t*t*x2 + t**3*x3,
                               u**3*y0 + 3*u*u*t*y1 + 3*u*t*t*y2 + t**3*y3))
            cursor = (x3, y3)
        elif command == "Z":
            yield [(x*SCALE, y*SCALE) for x, y in points]
            points = []
    if points:
        raise ValueError("Launcher paths must be closed")


def rgb(color):
    return tuple(int(color[i:i+2], 16) for i in (1, 3, 5))


def vector(name, tint=None):
    canvas = Image.new("RGBA", (SIZE, SIZE))
    for path in ET.parse(RES / "drawable" / name).getroot().findall("path"):
        mask = Image.new("L", (SIZE, SIZE))
        pen = ImageDraw.Draw(mask)
        for polygon in contours(path.attrib[ANDROID + "pathData"]):
            pen.polygon(polygon, fill=255)
        gradient = path.find(".//gradient")
        if gradient is None:
            color = tint or path.attrib[ANDROID + "fillColor"]
            layer = Image.new("RGBA", (SIZE, SIZE), color)
        else:
            start = rgb(gradient.findall("item")[0].attrib[ANDROID + "color"])
            end = rgb(gradient.findall("item")[-1].attrib[ANDROID + "color"])
            layer = Image.new("RGBA", (SIZE, SIZE))
            pixels = layer.load()
            for y in range(SIZE):
                for x in range(SIZE):
                    t = max(0, min(1, (x/SCALE + y/SCALE - 36)/144))
                    pixels[x, y] = tuple(round(a + (b-a)*t) for a, b in zip(start, end)) + (255,)
        layer.putalpha(mask)
        canvas = Image.alpha_composite(canvas, layer)
    return canvas


def masked(canvas, size, shape="square"):
    # Android's visible adaptive viewport is the central 72 dp of the 108 dp layers.
    canvas = canvas.crop((144, 144, 720, 720))
    mask = Image.new("L", canvas.size)
    draw = ImageDraw.Draw(mask)
    if shape == "circle":
        draw.ellipse((0, 0, 575, 575), fill=255)
    else:
        draw.rounded_rectangle((0, 0, 575, 575), radius=128, fill=255)
    canvas.putalpha(mask)
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def main():
    ART.mkdir(exist_ok=True)
    foreground = vector("ic_launcher_foreground.xml")
    normal = Image.alpha_composite(vector("ic_launcher_background.xml"), foreground)
    masked(normal, 512).save(ART / "launcher-icon.png")
    for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        for name, shape in (("ic_launcher", "square"), ("ic_launcher_round", "circle")):
            masked(normal, size, shape).save(RES / f"mipmap-{density}" / f"{name}.webp", lossless=True)
    preview = Image.new("RGB", (800, 260), "#F0F5FA")
    preview.paste(masked(normal, 176), (20, 25), masked(normal, 176))
    preview.paste(masked(normal, 176, "circle"), (215, 25), masked(normal, 176, "circle"))
    for x, bg, fg in ((410, "#CFE5F4", "#124A84"), (605, "#17354A", "#C6EAF4")):
        themed = Image.alpha_composite(Image.new("RGBA", (SIZE, SIZE), bg), vector("ic_launcher_monochrome.xml", fg))
        icon = masked(themed, 176, "circle")
        preview.paste(icon, (x, 25), icon)
    for x, shape in ((84, "square"), (279, "circle")):
        icon = masked(normal, 48, shape)
        preview.paste(icon, (x, 208), icon)
    preview.save(ART / "launcher-icon-preview.png")
    print("Exported adaptive icon previews and all 10 legacy WebP assets")


if __name__ == "__main__":
    main()
