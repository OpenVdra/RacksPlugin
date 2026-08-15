#!/usr/bin/env python3
"""
Generates the Racks plugin artwork from KawaMood's data pack icon.

The source icon is 96x96 and already has a transparent background, so the work here is to keep it
looking like pixel art while making it big enough to use: it is scaled with nearest-neighbour so the
blocks stay crisp instead of turning into mush, any leftover semi-transparent fringe is cleaned up,
and the lettering is drawn a pixel at a time from the bitmap font below rather than set in a real
typeface, so it matches the icon instead of sitting on top of it.

Outputs, all with transparent backgrounds:
    docs/public/logo.png     512x512   icon with PLUGIN under it, for the docs hero and the README
    docs/public/favicon.png  128x128   icon on its own
    docs/public/banner.png   1280x448  icon beside RACKS / PLUGIN, for the README header

Usage:
    python docs/scripts/generate-logo.py [path/to/icon96.webp]

With no argument the icon is downloaded from Modrinth. Requires Pillow.
"""

import os
import sys
import urllib.request

from PIL import Image, ImageDraw

ICON_URL = "https://cdn.modrinth.com/data/Ws2vw51R/9197f1533caa55781a49aa6d14a3b5332e18e06c_96.webp"

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "public")

# Sampled off the rack's oak planks, with a shadow dark enough to read on both a light and a dark
# page. Minecraft draws text shadow one font-pixel down and right, which is what SHADOW_OFFSET is.
GOLD = (216, 177, 115, 255)
GOLD_DIM = (186, 149, 92, 255)
SHADOW = (58, 41, 20, 255)
SHADOW_OFFSET = 1

# 5x7 bitmap font. Only the glyphs this artwork needs.
FONT = {
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "C": ["01111", "10000", "10000", "10000", "10000", "10000", "01111"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01111"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
}

GLYPH_W = 5
GLYPH_H = 7
TRACKING = 1  # blank columns between glyphs, in font pixels


def text_width(text: str) -> int:
    """Width of `text` in font pixels, without a trailing gap."""
    if not text:
        return 0
    return len(text) * (GLYPH_W + TRACKING) - TRACKING


def draw_text(draw: ImageDraw.ImageDraw, text: str, x: int, y: int, scale: int,
              colour, shadow=SHADOW) -> None:
    """Draws `text` with its top-left corner at (x, y), each font pixel a `scale`-sized square."""
    for pass_colour, dx, dy in ((shadow, SHADOW_OFFSET, SHADOW_OFFSET), (colour, 0, 0)):
        if pass_colour is None:
            continue
        pen_x = x + dx * scale
        for char in text:
            glyph = FONT[char]
            for row, bits in enumerate(glyph):
                for col, bit in enumerate(bits):
                    if bit == "1":
                        px = pen_x + col * scale
                        py = y + (row + dy) * scale
                        draw.rectangle([px, py, px + scale - 1, py + scale - 1], fill=pass_colour)
            pen_x += (GLYPH_W + TRACKING) * scale


def load_icon(source: str) -> Image.Image:
    """Loads the icon and hardens its transparency."""
    if source.startswith("http"):
        with urllib.request.urlopen(source) as response:
            data = response.read()
        import io
        icon = Image.open(io.BytesIO(data))
    else:
        icon = Image.open(source)
    icon = icon.convert("RGBA")

    # The webp carries a soft edge from its own resampling. Nearest-neighbour scaling would smear
    # that into visible grey confetti around the rack, so every pixel is forced fully on or fully
    # off first. The cut sits low because the icon's own outline is dark and nearly opaque.
    pixels = icon.load()
    width, height = icon.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            pixels[x, y] = (r, g, b, 255) if a >= 128 else (0, 0, 0, 0)
    return icon


def scaled(icon: Image.Image, factor: int) -> Image.Image:
    return icon.resize((icon.width * factor, icon.height * factor), Image.NEAREST)


def make_logo(icon: Image.Image) -> Image.Image:
    """512x512: the rack, with PLUGIN set underneath it."""
    size = 512
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    art = scaled(icon, 4)  # 384x384
    art_y = 14
    canvas.alpha_composite(art, ((size - art.width) // 2, art_y))

    scale = 11
    label = "PLUGIN"
    width = (text_width(label) + SHADOW_OFFSET) * scale
    draw = ImageDraw.Draw(canvas)
    draw_text(draw, label, (size - width) // 2, art_y + art.height + 8, scale, GOLD)
    return canvas


def make_favicon(icon: Image.Image) -> Image.Image:
    """128x128: the rack on its own, so it stays legible at tab size."""
    return scaled(icon, 4).resize((128, 128), Image.NEAREST)


def make_banner(icon: Image.Image) -> Image.Image:
    """The rack on the left, RACKS over PLUGIN on the right. Sized to fit its contents."""
    art = scaled(icon, 4)  # 384x384
    margin = 96
    gap = 72

    title, title_scale = "RACKS", 18
    subtitle, subtitle_scale = "PLUGIN", 9
    gutter = 26  # between the two lines

    title_w = (text_width(title) + SHADOW_OFFSET) * title_scale
    title_h = (GLYPH_H + SHADOW_OFFSET) * title_scale
    subtitle_w = (text_width(subtitle) + SHADOW_OFFSET) * subtitle_scale
    subtitle_h = (GLYPH_H + SHADOW_OFFSET) * subtitle_scale

    text_w = max(title_w, subtitle_w)
    text_h = title_h + gutter + subtitle_h

    width = margin + art.width + gap + text_w + margin
    height = art.height + margin
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)

    canvas.alpha_composite(art, (margin, (height - art.height) // 2))

    text_x = margin + art.width + gap
    text_y = (height - text_h) // 2
    draw_text(draw, title, text_x, text_y, title_scale, GOLD)
    draw_text(draw, subtitle, text_x, text_y + title_h + gutter, subtitle_scale, GOLD_DIM)
    return canvas


def main() -> None:
    source = sys.argv[1] if len(sys.argv) > 1 else ICON_URL
    icon = load_icon(source)

    os.makedirs(OUT_DIR, exist_ok=True)
    outputs = {
        "logo.png": make_logo(icon),
        "favicon.png": make_favicon(icon),
        "banner.png": make_banner(icon),
    }
    for name, image in outputs.items():
        path = os.path.normpath(os.path.join(OUT_DIR, name))
        image.save(path)
        print(f"{path}  {image.width}x{image.height}")


if __name__ == "__main__":
    main()
