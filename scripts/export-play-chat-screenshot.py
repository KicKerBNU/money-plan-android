#!/usr/bin/env python3
"""Build Play Store chat screenshot from emulator capture (1440x2560 RGB PNG, no alpha)."""

from __future__ import annotations

import textwrap
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SRC = Path("/tmp/mp-chat1.png")
NAV_REF = ROOT / "play-store" / "phone-screenshot-01-expenses.png"
OUT = ROOT / "play-store" / "phone-screenshot-03-chat.png"
NAV_H = 210

TARGET_W, TARGET_H = 1440, 2560
USER_BG = (37, 99, 235)  # #2563EB
USER_FG = (255, 255, 255)
ASSIST_BG = (226, 232, 240)  # surfaceVariant-ish
ASSIST_FG = (15, 23, 42)
MUTED = (100, 116, 139)

USER_TEXT = "How much did I spend on food this month?"
ASSIST_TEXT = (
    "This month you spent $48.50 on Food across 4 expenses. "
    "Your largest food entry was $18.00 on Jul 12."
)


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/System/Library/Fonts/SFNS.ttf",
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/Library/Fonts/Arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size=size)
            except OSError:
                continue
    return ImageFont.load_default()


def crop_to_9_16(img: Image.Image) -> Image.Image:
    w, h = img.size
    target_h = round(w * 16 / 9)
    if target_h <= h:
        top = (h - target_h) // 2
        return img.crop((0, top, w, top + target_h))
    target_w = round(h * 9 / 16)
    left = (w - target_w) // 2
    return img.crop((left, 0, left + target_w, h))


def rounded_rect(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    radius: int,
    fill: tuple[int, int, int],
) -> None:
    draw.rounded_rectangle(xy, radius=radius, fill=fill)


def draw_wrapped_bubble(
    base: Image.Image,
    text: str,
    *,
    right: bool,
    top_y: int,
    max_width: int,
    pad_x: int = 28,
    pad_y: int = 22,
    font_size: int = 34,
) -> int:
    font = load_font(font_size)
    draw = ImageDraw.Draw(base)
    w, _ = base.size

    lines: list[str] = []
    for paragraph in text.split("\n"):
        lines.extend(textwrap.wrap(paragraph, width=42) or [""])

    line_heights = []
    line_widths = []
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=font)
        line_widths.append(bbox[2] - bbox[0])
        line_heights.append(bbox[3] - bbox[1])

    text_w = max(line_widths) if line_widths else 0
    text_h = sum(line_heights) + max(0, len(lines) - 1) * 8
    bubble_w = min(max_width, text_w + pad_x * 2)
    bubble_h = text_h + pad_y * 2

    margin = 48
    if right:
        x1 = w - margin - bubble_w
    else:
        x1 = margin
    x2 = x1 + bubble_w
    y1 = top_y
    y2 = y1 + bubble_h

    bg = USER_BG if right else ASSIST_BG
    fg = USER_FG if right else ASSIST_FG
    rounded_rect(draw, (x1, y1, x2, y2), 28, bg)

    y = y1 + pad_y
    for i, line in enumerate(lines):
        draw.text((x1 + pad_x, y), line, font=font, fill=fg)
        y += line_heights[i] + 8

    return y2


def restore_input_bar(img: Image.Image, bg: tuple[int, int, int], nav_strip: Image.Image) -> None:
    """Remove Gboard overlay; redraw input row; restore bottom nav from source capture."""
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 2090, TARGET_W, TARGET_H - NAV_H), fill=bg)

    field = (54, 2204, 1280, 2348)
    draw.rounded_rectangle(field, radius=18, fill=(255, 255, 255), outline=(203, 213, 225), width=3)
    draw.text((96, 2244), "Ask about your expenses", fill=MUTED, font=load_font(30))

    cx, cy = 1348, 2276
    draw.polygon(
        [(cx - 14, cy - 16), (cx + 18, cy), (cx - 14, cy + 16), (cx - 6, cy)],
        fill=MUTED,
    )

    img.paste(nav_strip, (0, TARGET_H - NAV_H))


def main() -> None:
    src = Path(__import__("os").environ.get("PLAY_CHAT_SRC", str(SRC)))
    if not src.exists():
        raise SystemExit(f"Missing source capture: {src}. Run emulator chat screen capture first.")

    img = Image.open(src).convert("RGBA")
    img = crop_to_9_16(img)
    img = img.resize((TARGET_W, TARGET_H), Image.Resampling.LANCZOS)
    nav_strip = img.crop((0, TARGET_H - NAV_H, TARGET_W, TARGET_H)).copy()

    # Cover empty-state card + hint area with sampled background
    px = img.load()
    bg = px[80, 900][:3]
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 500, TARGET_W, 1320), fill=bg)

    draw.text((60, 520), "Ask questions about your spending.", fill=MUTED, font=load_font(30))

    after_user = draw_wrapped_bubble(
        img,
        USER_TEXT,
        right=True,
        top_y=600,
        max_width=920,
        font_size=32,
    )
    draw_wrapped_bubble(
        img,
        ASSIST_TEXT,
        right=False,
        top_y=after_user + 36,
        max_width=980,
        font_size=32,
    )

    restore_input_bar(img, bg, nav_strip)

    # Play: 24-bit PNG, no alpha
    rgb = Image.new("RGB", img.size, bg)
    rgb.paste(img, mask=img.split()[3])
    OUT.parent.mkdir(parents=True, exist_ok=True)
    rgb.save(OUT, format="PNG", optimize=True)

    size_kb = OUT.stat().st_size / 1024
    print(f"Wrote {OUT} ({TARGET_W}x{TARGET_H}, {size_kb:.0f} KB)")


if __name__ == "__main__":
    main()
