#!/usr/bin/env python3
"""Generate launcher icons from the wide original of the in-app avatar.

The 256px in-app avatar is a tight crop whose maid headdress is cut off at the
top edge.  The 768px original of the same artwork is used here so the head can
be kept complete.  The face midline (x≈455 in the 768px art) is placed on the
canvas centre, and the portrait is scaled so the whole head fits inside the
visible 72dp adapter area even under a circular mask.
"""
from PIL import Image, ImageDraw

SRC = "tools/avatar-768.png"
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
ART = Image.open(SRC).convert("RGB")
EDGE = ART.getpixel((10, 10))
FACE_X, FACE_Y = 455, 202      # face midline / head centre in the 768px art
SCALE_108 = 0.135              # dp per source pixel on the 108dp canvas

def composed(canvas_px, canvas_dp):
    dp = canvas_px / canvas_dp
    k = SCALE_108 * canvas_dp / 108.0
    art = ART.resize((round(ART.width * k * dp), round(ART.height * k * dp)), Image.LANCZOS)
    canvas = Image.new("RGB", (canvas_px, canvas_px), EDGE)
    left = round((canvas_dp / 2 - FACE_X * k) * dp)
    top = round((canvas_dp / 2 - FACE_Y * k) * dp)
    canvas.paste(art, (left, top))
    return canvas

# Adaptive icon: portrait layer bleeds under every mask; foreground is empty.
for name, scale in DENSITIES.items():
    px = round(108 * scale)
    composed(px, 108).save(f"app/src/main/res/drawable-{name}/ic_launcher_background.png")
    Image.new("RGBA", (px, px), (0, 0, 0, 0)).save(
        f"app/src/main/res/drawable-{name}/ic_launcher_foreground.png")

# Legacy square + round icons (Android <8).
for name, scale in DENSITIES.items():
    px = round(48 * scale)
    square = composed(px, 48)
    square.save(f"app/src/main/res/mipmap-{name}/ic_launcher.png")
    mask = Image.new("L", (px, px), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, px - 1, px - 1), fill=255)
    roundi = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    roundi.paste(square.convert("RGBA"), (0, 0), mask)
    roundi.save(f"app/src/main/res/mipmap-{name}/ic_launcher_round.png")

# Play-store 512 icon.
composed(512, 108).save("app/src/main/res/drawable/ic_launcher_512.png")
print("icons generated from", SRC)
