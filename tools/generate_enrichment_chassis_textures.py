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
    "heat0": "#3b1b16", "heat1": "#8f2b18", "heat2": "#ed651c", "heat3": "#ffe18a",
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


def enrichment_front(online, working, phase=0):
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


def machine_front_base(online):
    im = new(); chassis(im); lamp(im, online)
    rect(im, (6, 8, 25, 25), COLORS["panel_hi"])
    rect(im, (8, 10, 23, 23), COLORS["frame0"])
    rect(im, (9, 11, 22, 22), COLORS["black"])
    return im


def crusher_front(online, working, phase=0):
    im = machine_front_base(online)
    active = COLORS["cyan2"] if online else COLORS["cyan0"]
    highlight = COLORS["cyan3"] if online else COLORS["edge"]
    travel = (0, 1, 2, 1)[phase] if working else 0
    # Opposing stepped jaws close around a fractured mineral.
    for y, inset in ((12, 0), (13, 1), (14, 2), (18, 2), (19, 1), (20, 0)):
        rect(im, (9 + travel, y, 12 + travel + inset, y), COLORS["frame2"])
        rect(im, (19 - travel - inset, y, 22 - travel, y), COLORS["frame2"])
    rect(im, (10 + travel, 15, 13 + travel, 17), active)
    rect(im, (18 - travel, 15, 21 - travel, 17), active)
    rect(im, (14, 14, 17, 19), COLORS["panel_mid"])
    px(im, 15, 15, COLORS["panel_hi"]); px(im, 16, 17, COLORS["frame1"])
    px(im, 15, 18, highlight); px(im, 17, 16, highlight)
    if working:
        px(im, 13 - phase % 2, 11 + phase, highlight)
        px(im, 19 + phase % 2, 21 - phase, highlight)
    return im


def energized_smelter_front(online, working, phase=0):
    im = machine_front_base(online)
    cold = COLORS["frame2"] if not online else COLORS["heat1"]
    warm = COLORS["edge"] if not online else COLORS["heat2"]
    hot = COLORS["bolt"] if not online else COLORS["heat3"]

    # Refractory hearth and three heating elements around the central billet.
    rect(im, (10, 12, 21, 21), COLORS["frame1"])
    rect(im, (12, 14, 19, 20), COLORS["heat0"] if online else COLORS["black"])
    for x in (10, 15, 20):
        rect(im, (x, 11, x + 1, 13), cold)
        rect(im, (x, 20, x + 1, 22), cold)

    # Four animation phases make heat circulate without changing the silhouette.
    flame_rise = (2, 1, 0, 1)[phase] if working else 2
    rect(im, (14, 16 - flame_rise, 17, 19), warm)
    rect(im, (15, 15 - flame_rise, 16, 17), hot)
    px(im, 13, 18 - flame_rise, cold)
    px(im, 18, 17 - flame_rise, cold)
    rect(im, (13, 19, 18, 20), COLORS["panel_mid"])
    rect(im, (14, 18, 17, 19), COLORS["panel_hi"])

    if working:
        for index, (x, y) in enumerate(((11, 15), (20, 15), (20, 18), (11, 18))):
            px(im, x, y, hot if index == phase else warm)
    return im


def metallurgic_front(online, working, phase=0):
    im = machine_front_base(online)
    chemical = COLORS["purple1"] if online else COLORS["purple0"]
    signal = COLORS["cyan2"] if online else COLORS["cyan0"]
    # Two isolated feed manifolds converge only inside the central reaction cell.
    rect(im, (9, 11, 11, 20), chemical); rect(im, (20, 11, 22, 20), signal)
    rect(im, (12, 14, 19, 18), COLORS["frame1"])
    rect(im, (14, 13, 17, 19), COLORS["panel_hi"])
    rect(im, (15, 15, 17, 17), signal if online else COLORS["edge"])
    for y in (12, 15, 18):
        px(im, 12 + (phase + y) % 2, y, chemical)
        px(im, 19 - (phase + y) % 2, y, signal)
    if working:
        pulse = COLORS["cyan3"] if online else COLORS["cyan2"]
        px(im, 13 + phase, 12 + phase % 2, pulse)
        px(im, 18 - phase, 19 - phase % 2, pulse)
    return im


def osmium_compressor_front(online, working, phase=0):
    im = machine_front_base(online)
    power = COLORS["cyan2"] if online else COLORS["cyan0"]
    travel = (0, 1, 2, 1)[phase] if working else 0
    # Heavy opposed rams compress a bright osmium billet in the center.
    rect(im, (11, 11, 20, 12 + travel), COLORS["frame2"])
    rect(im, (13, 13 + travel, 18, 14 + travel), COLORS["edge"])
    rect(im, (13, 18 - travel, 18, 19 - travel), COLORS["edge"])
    rect(im, (11, 20 - travel, 20, 21), COLORS["frame2"])
    rect(im, (13, 15, 18, 17), COLORS["panel_hi"])
    rect(im, (14, 15, 17, 16), power)
    px(im, 14, 15, COLORS["cyan3"] if online else COLORS["bolt"])
    rect(im, (9, 14, 10, 18), power); rect(im, (21, 14, 22, 18), power)
    if working:
        pulse = COLORS["cyan3"] if online else COLORS["cyan2"]
        px(im, 12 + phase * 2, 22, pulse)
    return im


def purification_front(online, working, phase=0):
    im = machine_front_base(online)
    oxygen = COLORS["cyan2"] if online else COLORS["cyan0"]
    # Three filter membranes with a clear rising oxygen path.
    for y in (13, 16, 19):
        rect(im, (11, y, 20, y + 1), COLORS["frame2"])
        rect(im, (13, y, 18, y), COLORS["edge"])
    rect(im, (9, 11, 10, 21), oxygen); rect(im, (21, 11, 22, 21), oxygen)
    bubbles = ((14, 20), (17, 17), (15, 14), (18, 12))
    offset = phase if working else 0
    for index, (x, y) in enumerate(bubbles):
        yy = 11 + ((y - 11 - offset - index) % 11)
        px(im, x, yy, COLORS["cyan3"] if online and index == phase else oxygen)
    return im


def chemical_injection_front(online, working, phase=0):
    im = machine_front_base(online)
    chemical = COLORS["purple1"] if online else COLORS["purple0"]
    spray = COLORS["cyan2"] if online else COLORS["cyan0"]
    # A top injector sprays a controlled chemical cone into the item chamber.
    rect(im, (13, 10, 18, 12), COLORS["frame2"])
    rect(im, (15, 13, 16, 14), chemical)
    rect(im, (11, 19, 20, 21), COLORS["frame1"])
    rect(im, (13, 17, 18, 20), COLORS["panel_hi"])
    rect(im, (14, 18, 17, 19), spray)
    spread = phase if working else 1
    for index, x in enumerate((13, 15, 17, 19)):
        y = 15 + ((index + spread) % 3)
        px(im, x, y, COLORS["cyan3"] if online and index == phase else chemical)
    if working:
        pulse = COLORS["cyan3"] if online else COLORS["cyan2"]
        px(im, 12 + phase * 2, 22, pulse)
    rect(im, (9, 13, 10, 18), spray); rect(im, (21, 13, 22, 18), spray)
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
    (TEXTURE_DIR / f"{name}.png.mcmeta").write_text(
        '{\n  "animation": {\n    "frametime": 3,\n    "interpolate": false\n  }\n}\n',
        encoding="utf-8")
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
        save(f"me_machine_chassis_front_{suffix}", enrichment_front(online, False))
        save_animation(f"me_machine_chassis_front_{suffix}_working",
                       [enrichment_front(online, True, phase) for phase in range(4)])
        for machine, renderer in (
                ("me_crusher", crusher_front),
                ("me_energized_smelter", energized_smelter_front),
                ("me_metallurgic_infuser", metallurgic_front),
                ("me_osmium_compressor", osmium_compressor_front),
                ("me_purification_chamber", purification_front),
                ("me_chemical_injection_chamber", chemical_injection_front)):
            save(f"{machine}_front_{suffix}", renderer(online, False))
            save_animation(f"{machine}_front_{suffix}_working",
                           [renderer(online, True, phase) for phase in range(4)])


if __name__ == "__main__":
    main()
