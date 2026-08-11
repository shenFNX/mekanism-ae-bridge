from pathlib import Path

from PIL import Image, ImageColor


SIZE = 32
ROOT = Path(__file__).resolve().parents[1]
TEXTURE_DIR = ROOT / "src/main/resources/assets/mekanismae/textures/block"
PREVIEW_DIR = ROOT / "build/texture-previews"

COLORS = {name: ImageColor.getcolor(value, "RGBA") for name, value in {
    "black": "#12171c", "frame0": "#20272e", "frame1": "#303a43", "frame2": "#4a565f",
    "edge": "#738088", "bolt": "#a5afb2", "shadow": "#929da1", "panel": "#ced5d4",
    "panel_hi": "#eef1ef", "panel_mid": "#b4bebf", "cyan0": "#064e59", "cyan1": "#087f8b",
    "cyan2": "#11c5ce", "cyan3": "#8bffff", "purple0": "#34223f", "purple1": "#654178",
}.items()}


def new():
    return Image.new("RGBA", (SIZE, SIZE), COLORS["black"])


def rect(im, box, color):
    x0, y0, x1, y1 = box
    pix = im.load()
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            pix[x, y] = color


def px(im, x, y, color):
    im.putpixel((x, y), color)


def chassis(im):
    # Three-pixel rails: visually substantial but leave a 22x22 ceramic field.
    rect(im, (0, 0, 31, 31), COLORS["frame0"])
    rect(im, (2, 2, 29, 29), COLORS["frame1"])
    rect(im, (3, 3, 28, 28), COLORS["shadow"])
    rect(im, (5, 5, 26, 26), COLORS["panel"])
    rect(im, (5, 5, 26, 6), COLORS["panel_hi"])
    rect(im, (5, 25, 26, 26), COLORS["panel_mid"])

    # Six-pixel reinforced caps with small bolts and bevels.
    for x0, y0 in ((0, 0), (26, 0), (0, 26), (26, 26)):
        rect(im, (x0, y0, x0 + 5, y0 + 5), COLORS["frame1"])
        rect(im, (x0 + 1, y0 + 1, x0 + 4, y0 + 4), COLORS["frame2"])
        rect(im, (x0 + 2, y0 + 2, x0 + 3, y0 + 3), COLORS["bolt"])
        px(im, x0 + 2, y0 + 2, COLORS["edge"])


def lamp(im, online):
    rect(im, (12, 3, 19, 6), COLORS["frame0"])
    color = COLORS["cyan2"] if online else COLORS["cyan0"]
    rect(im, (13, 4, 18, 5), color)
    if online:
        rect(im, (14, 4, 16, 4), COLORS["cyan3"])


def front(online, working, phase=0):
    im = new(); chassis(im); lamp(im, online)
    rect(im, (6, 8, 25, 25), COLORS["frame0"])
    rect(im, (8, 10, 23, 23), COLORS["black"])
    rect(im, (10, 12, 21, 21), COLORS["cyan0"] if online else COLORS["frame1"])

    # A larger enrichment crystal with shaded facets and converging particles.
    glow = COLORS["cyan2"] if online else COLORS["cyan1"]
    hi = COLORS["cyan3"] if working else (COLORS["cyan2"] if online else COLORS["edge"])
    for x, y in ((10, 12), (21, 12), (10, 21), (21, 21), (12, 14), (19, 14), (12, 19), (19, 19)):
        px(im, x, y, glow)
    rect(im, (14, 14, 17, 19), COLORS["panel_hi"])
    rect(im, (13, 16, 18, 17), COLORS["panel_hi"])
    rect(im, (14, 15, 16, 17), hi)
    px(im, 15, 14, COLORS["panel_hi"])
    px(im, 17, 18, COLORS["cyan1"])

    # Work-only animated pulse ring is supplied by a vertical frame strip.
    if working:
        rect(im, (8, 8, 23, 8), glow)
        rect(im, (8, 23, 23, 23), glow)
        rect(im, (8, 9, 8, 22), glow)
        rect(im, (23, 9, 23, 22), glow)
        # One bright segment moves clockwise around the core over four frames.
        pulse = COLORS["cyan3"] if online else COLORS["cyan2"]
        if phase == 0:
            rect(im, (12, 8, 18, 8), pulse)
        elif phase == 1:
            rect(im, (23, 12, 23, 18), pulse)
        elif phase == 2:
            rect(im, (12, 23, 18, 23), pulse)
        else:
            rect(im, (8, 12, 8, 18), pulse)

    contact = COLORS["cyan2"] if online else COLORS["cyan0"]
    for x in (12, 15, 18):
        rect(im, (x, 27, x + 1, 28), contact)
    rect(im, (15, 25, 16, 26), COLORS["purple1"] if online else COLORS["purple0"])
    return im


def left(online):
    im = new(); chassis(im); lamp(im, online)
    rect(im, (7, 8, 24, 24), COLORS["panel_hi"])
    rect(im, (9, 10, 13, 22), COLORS["frame0"])
    rect(im, (18, 10, 22, 22), COLORS["frame0"])
    rail = COLORS["cyan2"] if online else COLORS["cyan0"]
    rect(im, (11, 12, 13, 20), rail); rect(im, (18, 12, 20, 20), rail)
    if online:
        rect(im, (11, 12, 12, 14), COLORS["cyan3"]); rect(im, (19, 12, 20, 14), COLORS["cyan3"])
    rect(im, (14, 10, 17, 22), COLORS["frame1"])
    for y in (12, 16, 20):
        rect(im, (15, y, 16, y + 1), rail)
    rect(im, (14, 15, 17, 16), COLORS["purple1"] if online else COLORS["purple0"])
    return im


def right(online):
    im = new(); chassis(im); lamp(im, online)
    rect(im, (7, 8, 24, 12), COLORS["frame0"])
    diag = COLORS["cyan2"] if online else COLORS["cyan0"]
    for x in (9, 13, 17, 21): rect(im, (x, 9, x + 1, 10), diag)
    rect(im, (7, 14, 24, 22), COLORS["frame0"])
    for y in (15, 18, 21):
        rect(im, (9, y, 22, y + 1), COLORS["frame2"])
        rect(im, (10, y, 21, y), COLORS["edge"])
    rect(im, (8, 24, 23, 26), COLORS["frame2"])
    px(im, 10, 25, COLORS["black"]); px(im, 21, 25, COLORS["black"])
    return im


def back(online):
    im = new(); chassis(im); lamp(im, online)
    rect(im, (7, 8, 24, 24), COLORS["frame0"])
    ring0 = COLORS["cyan1"] if online else COLORS["frame2"]
    ring1 = COLORS["cyan2"] if online else COLORS["cyan0"]
    rect(im, (9, 10, 22, 22), ring0); rect(im, (11, 12, 20, 20), ring1)
    rect(im, (13, 14, 18, 18), COLORS["black"])
    rect(im, (14, 15, 17, 17), COLORS["frame2"])
    px(im, 14, 15, COLORS["edge"])
    channel = COLORS["purple1"] if online else COLORS["purple0"]
    rect(im, (14, 7, 17, 9), channel); rect(im, (14, 23, 17, 25), channel)
    rect(im, (6, 14, 8, 17), channel); rect(im, (23, 14, 25, 17), channel)
    for box in ((14, 5, 17, 6), (14, 26, 17, 27), (5, 14, 6, 17), (25, 14, 26, 17)):
        rect(im, box, ring1)
    return im


def top(online):
    im = new(); chassis(im)
    rect(im, (7, 7, 24, 24), COLORS["panel_hi"])
    trace = COLORS["purple1"] if online else COLORS["purple0"]
    for box in ((8, 8, 14, 9), (17, 8, 23, 9), (8, 22, 14, 23), (17, 22, 23, 23),
                (8, 10, 9, 14), (8, 17, 9, 21), (22, 10, 23, 14), (22, 17, 23, 21)):
        rect(im, box, trace)
    rect(im, (12, 12, 19, 19), COLORS["frame0"])
    rect(im, (14, 14, 17, 17), COLORS["frame2"])
    px(im, 14, 14, COLORS["bolt"])
    signal = COLORS["cyan2"] if online else COLORS["cyan0"]
    for box in ((14, 5, 17, 6), (14, 25, 17, 26), (5, 14, 6, 17), (25, 14, 26, 17)):
        rect(im, box, signal)
    return im


def bottom():
    im = new()
    rect(im, (0, 0, 31, 31), COLORS["frame0"])
    rect(im, (3, 3, 28, 28), COLORS["frame1"])
    rect(im, (8, 8, 23, 23), COLORS["black"])
    rect(im, (10, 10, 21, 21), COLORS["frame0"])
    rect(im, (14, 14, 17, 17), COLORS["bolt"])
    return im


def save(name, image):
    image.save(TEXTURE_DIR / f"{name}.png", optimize=False)
    image.resize((256, 256), Image.Resampling.NEAREST).save(PREVIEW_DIR / f"{name}_preview.png")


def save_animation(name, frames):
    strip = Image.new("RGBA", (SIZE, SIZE * len(frames)))
    for index, frame in enumerate(frames):
        strip.paste(frame, (0, index * SIZE))
    strip.save(TEXTURE_DIR / f"{name}.png", optimize=False)
    frames[0].resize((256, 256), Image.Resampling.NEAREST).save(PREVIEW_DIR / f"{name}_preview.png")


def main():
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True); PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
    # Common bottom plus two online states for every visible static face.
    save("me_machine_chassis_bottom", bottom())
    for online in (False, True):
        suffix = "online" if online else "offline"
        save(f"me_machine_chassis_left_{suffix}", left(online))
        save(f"me_machine_chassis_right_{suffix}", right(online))
        save(f"me_machine_chassis_back_{suffix}", back(online))
        save(f"me_machine_chassis_top_{suffix}", top(online))
        save(f"me_machine_chassis_front_{suffix}", front(online, False))
        save_animation(f"me_machine_chassis_front_{suffix}_working",
                       [front(online, True, phase) for phase in range(4)])


if __name__ == "__main__":
    main()
