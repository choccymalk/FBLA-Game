from PIL import Image, ImageDraw, ImageFont
import argparse
import os
import unicodedata
import sys

PRINTABLE_ASCII_START = 0x00
PRINTABLE_ASCII_END = 0xFF

def sane_filename_for_char(ch: str) -> str:
    cp = ord(ch)
    try:
        name = unicodedata.name(ch)
    except ValueError:
        name = f"U+{cp:04X}"
    # make name filesystem-friendly
    name = name.lower().replace(' ', '_')
    return f"{name}.png"

def glyph_fits(font_path: str, ch: str, size: int, target_w: int, target_h: int) -> bool:
    # Create temporary canvas to measure
    font = ImageFont.truetype(font_path, size=size)
    # PIL's getbbox via a draw instance gives precise bounding box
    img = Image.new("L", (max(1, target_w), max(1, target_h)), 0)
    draw = ImageDraw.Draw(img)
    bbox = draw.textbbox((0,0), ch, font=font)
    if bbox is None:
        return False
    x0,y0,x1,y1 = bbox
    w = x1 - x0
    h = y1 - y0
    return (w <= target_w) and (h <= target_h)

def find_max_font_size(font_path: str, ch: str, target_w: int, target_h: int, max_search_size: int = 2048) -> int:
    # Binary search for largest integer size that fits
    lo, hi = 1, max_search_size
    best = 1
    while lo <= hi:
        mid = (lo + hi) // 2
        try:
            fits = glyph_fits(font_path, ch, mid, target_w, target_h)
        except Exception:
            fits = False
        if fits:
            best = mid
            lo = mid + 1
        else:
            hi = mid - 1
    return best

def render_char_to_png(font_path: str, ch: str, target_w: int, target_h: int, outpath: str) -> bool:
    # Determine font size that best fits
    size = find_max_font_size(font_path, ch, target_w, target_h)
    font = ImageFont.truetype(font_path, size=size)
    # Create transparent RGBA image
    img = Image.new("RGBA", (target_w, target_h), (0,0,0,0))
    draw = ImageDraw.Draw(img)

    # compute text bounding box at (0,0)
    bbox = draw.textbbox((0,0), ch, font=font)
    if bbox is None:
        return False
    x0,y0,x1,y1 = bbox
    glyph_w = x1 - x0
    glyph_h = y1 - y0

    # center glyph in the image, account for bbox offset
    x = (target_w - glyph_w) / 2 - x0
    y = (target_h - glyph_h) / 2 - y0

    # draw in solid black (you can change color if desired)
    draw.text((x, y), ch, font=font, fill=(0,0,0,255))

    # If nothing was drawn (font lacks glyph), skip
    if img.getbbox() is None:
        return False

    # Save PNG
    os.makedirs(os.path.dirname(outpath), exist_ok=True)
    img.save(outpath, format="PNG")
    return True

def main():
    parser = argparse.ArgumentParser(description="Render printable ASCII characters from a TTF font into transparent PNGs.")
    parser.add_argument("--font", "-f", required=True, help="Path to .ttf font file")
    parser.add_argument("--outdir", "-o", required=True, help="Directory to write PNG files into")
    parser.add_argument("--width", "-W", type=int, required=True, help="Width of each output image in pixels")
    parser.add_argument("--height", "-H", type=int, required=True, help="Height of each output image in pixels")
    parser.add_argument("--range", "-r", nargs=2, type=lambda x: int(x,0), metavar=("START","END"),
                        help="Optional inclusive Unicode range (codepoints) to render, e.g. 0x20 0x7E. Defaults to printable ASCII.")
    args = parser.parse_args()

    font_path = args.font
    outdir = args.outdir
    w = args.width
    h = args.height

    if not os.path.isfile(font_path):
        print(f"ERROR: font file not found: {font_path}", file=sys.stderr)
        sys.exit(2)

    if args.range:
        start, end = args.range
    else:
        start, end = PRINTABLE_ASCII_START, PRINTABLE_ASCII_END

    total = 0
    skipped = 0
    for cp in range(start, end+1):
        ch = chr(cp)
        filename = sane_filename_for_char(ch)
        outpath = os.path.join(outdir, filename)
        ok = False
        try:
            ok = render_char_to_png(font_path, ch, w, h, outpath)
        except Exception as e:
            print(f"Warning: failed to render U+{cp:04X} ({ch!r}): {e}", file=sys.stderr)
            ok = False
        if ok:
            total += 1
            print(f"Wrote {outpath}")
        else:
            skipped += 1
            print(f"Skipped U+{cp:04X} ({ch!r}) -- glyph missing or too complex for size", file=sys.stderr)

    print(f"Done. Written: {total}. Skipped: {skipped}.")

if __name__ == "__main__":
    main()