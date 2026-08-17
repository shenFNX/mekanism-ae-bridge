from __future__ import annotations

import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/mekanismae/textures/block"
OUTPUT = ROOT / "docs/images/mekanism-ae-bridge-cover.png"

WIDTH = 1600
HEIGHT = 900

CHARCOAL = (10, 15, 18, 255)
PANEL = (18, 26, 31, 255)
PANEL_LIGHT = (30, 40, 45, 255)
WHITE = (236, 241, 239, 255)
MUTED = (155, 169, 171, 255)
CYAN = (22, 200, 211, 255)
CYAN_HI = (133, 255, 255, 255)
PURPLE = (132, 82, 164, 255)
AMBER = (235, 98, 40, 255)
GREEN = (123, 190, 75, 255)


def load_font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(Path("C:/Windows/Fonts") / name), size)


def load_face(name: str, frame: int | None = None) -> Image.Image:
    image = Image.open(TEXTURES / name).convert("RGBA")
    if image.height > image.width:
        frame_count = image.height // image.width
        selected = min(frame_count - 1, max(0, frame or 0))
        image = image.crop((0, selected * image.width, image.width, (selected + 1) * image.width))
    return image


def map_top(source: Image.Image, front_size: int, side_width: int, depth: int) -> Image.Image:
    source = source.resize((32, 32), Image.Resampling.NEAREST)
    output = Image.new("RGBA", (front_size + side_width + 2, depth + 2))
    draw = ImageDraw.Draw(output)

    def point(x: float, y: float) -> tuple[float, float]:
        return (
            side_width + front_size * x / 32 - side_width * y / 32,
            depth * y / 32,
        )

    pixels = source.load()
    for y in range(32):
        for x in range(32):
            color = pixels[x, y]
            draw.polygon(
                [point(x, y), point(x + 1, y), point(x + 1, y + 1), point(x, y + 1)],
                fill=color,
            )
    return output


def map_side(source: Image.Image, front_size: int, side_width: int, depth: int) -> Image.Image:
    source = source.resize((32, 32), Image.Resampling.NEAREST)
    output = Image.new("RGBA", (side_width + 2, front_size + depth + 2))
    draw = ImageDraw.Draw(output)

    def point(x: float, y: float) -> tuple[float, float]:
        return (
            side_width * x / 32,
            depth - depth * x / 32 + front_size * y / 32,
        )

    pixels = source.load()
    for y in range(32):
        for x in range(32):
            color = pixels[x, y]
            draw.polygon(
                [point(x, y), point(x + 1, y), point(x + 1, y + 1), point(x, y + 1)],
                fill=color,
            )
    return output


def tint(image: Image.Image, factor: float) -> Image.Image:
    result = image.copy()
    pixels = result.load()
    for y in range(result.height):
        for x in range(result.width):
            red, green, blue, alpha = pixels[x, y]
            pixels[x, y] = (
                min(255, int(red * factor)),
                min(255, int(green * factor)),
                min(255, int(blue * factor)),
                alpha,
            )
    return result


def draw_cube(canvas: Image.Image, front_name: str, x: int, y: int, size: int,
              frame: int = 0, glow: tuple[int, int, int, int] = CYAN) -> None:
    side_width = max(28, round(size * 0.24))
    depth = max(24, round(size * 0.19))
    front = load_face(front_name, frame).resize((size, size), Image.Resampling.NEAREST)
    top = map_top(load_face("me_machine_chassis_top_online.png"), size, side_width, depth)
    side = tint(map_side(load_face("me_machine_chassis_right_online.png"), size, side_width, depth), 0.84)

    shadow = Image.new("RGBA", canvas.size)
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.ellipse(
        (x - 30, y + size - 14, x + size + side_width + 38, y + size + 58),
        fill=(0, 0, 0, 170),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(max(10, size // 14)))
    canvas.alpha_composite(shadow)

    glow_layer = Image.new("RGBA", canvas.size)
    glow_draw = ImageDraw.Draw(glow_layer)
    glow_draw.rectangle(
        (x - 8, y - depth - 8, x + size + side_width + 8, y + size + 8),
        outline=glow[:3] + (95,),
        width=max(3, size // 42),
    )
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(max(8, size // 18)))
    canvas.alpha_composite(glow_layer)

    canvas.alpha_composite(top, (x, y - depth))
    canvas.alpha_composite(side, (x + size, y - depth))
    canvas.alpha_composite(front, (x, y))

    draw = ImageDraw.Draw(canvas)
    line_width = max(2, size // 70)
    draw.line([(x, y), (x + size, y), (x + size, y + size), (x, y + size), (x, y)],
              fill=(8, 12, 15, 230), width=line_width)
    draw.line([(x, y), (x + side_width, y - depth), (x + size + side_width, y - depth),
               (x + size, y)], fill=(73, 88, 96, 230), width=line_width)
    draw.line([(x + size, y), (x + size + side_width, y - depth),
               (x + size + side_width, y + size - depth), (x + size, y + size)],
              fill=(7, 11, 14, 230), width=line_width)


def draw_circuit(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], color: tuple[int, int, int, int],
                 width: int = 3) -> None:
    draw.line(points, fill=color, width=width, joint="curve")
    for x, y in (points[0], points[-1]):
        draw.rectangle((x - 4, y - 4, x + 4, y + 4), fill=color)
        draw.rectangle((x - 1, y - 1, x + 1, y + 1), fill=WHITE)


def draw_background(canvas: Image.Image) -> None:
    draw = ImageDraw.Draw(canvas)
    for y in range(HEIGHT):
        t = y / HEIGHT
        red = round(10 + 6 * t)
        green = round(15 + 8 * t)
        blue = round(18 + 9 * t)
        draw.line((0, y, WIDTH, y), fill=(red, green, blue, 255))

    draw.polygon([(0, 0), (735, 0), (620, HEIGHT), (0, HEIGHT)], fill=(14, 21, 25, 255))
    draw.polygon([(700, 0), (WIDTH, 0), (WIDTH, HEIGHT), (1180, HEIGHT)], fill=(11, 17, 21, 255))
    draw.polygon([(0, 720), (WIDTH, 610), (WIDTH, 900), (0, 900)], fill=(20, 28, 32, 255))

    grid = (50, 66, 72, 42)
    for x in range(0, WIDTH, 40):
        draw.line((x, 0, x, HEIGHT), fill=grid, width=1)
    for y in range(0, HEIGHT, 40):
        draw.line((0, y, WIDTH, y), fill=grid, width=1)

    random.seed(2101)
    for _ in range(120):
        x = random.randrange(WIDTH)
        y = random.randrange(HEIGHT)
        color = random.choice((CYAN, PURPLE, AMBER, GREEN, MUTED))
        alpha = random.randrange(35, 90)
        radius = random.choice((1, 1, 2))
        draw.rectangle((x, y, x + radius, y + radius), fill=color[:3] + (alpha,))

    draw_circuit(draw, [(620, 148), (704, 148), (704, 226), (820, 226)], CYAN[:3] + (120,), 3)
    draw_circuit(draw, [(536, 690), (664, 690), (664, 622), (790, 622)], PURPLE[:3] + (130,), 3)
    draw_circuit(draw, [(1130, 130), (1240, 130), (1240, 188), (1390, 188)], AMBER[:3] + (110,), 3)
    draw_circuit(draw, [(1110, 714), (1254, 714), (1254, 655), (1450, 655)], GREEN[:3] + (110,), 3)


def draw_text(canvas: Image.Image) -> None:
    draw = ImageDraw.Draw(canvas)
    title = load_font("impact.ttf", 106)
    title_secondary = load_font("impact.ttf", 98)
    label = load_font("consolab.ttf", 21)
    subtitle = load_font("segoeuib.ttf", 29)
    body = load_font("segoeui.ttf", 23)
    stat = load_font("bahnschrift.ttf", 24)

    draw.rounded_rectangle((96, 91, 392, 133), radius=4, fill=(25, 37, 42, 245),
                           outline=(72, 92, 99, 255), width=2)
    draw.rectangle((96, 91, 104, 133), fill=CYAN)
    draw.text((120, 99), "MINECRAFT 1.21.1  /  NEOFORGE", font=label, fill=WHITE)

    draw.text((94, 170), "MEKANISM", font=title, fill=WHITE,
              stroke_width=2, stroke_fill=(6, 9, 12, 255))
    draw.text((94, 275), "AE BRIDGE", font=title_secondary, fill=CYAN_HI,
              stroke_width=2, stroke_fill=(6, 9, 12, 255))

    draw.rectangle((98, 394, 554, 400), fill=PURPLE)
    draw.rectangle((98, 394, 252, 400), fill=AMBER)
    draw.text((96, 432), "ME NETWORK-NATIVE PROCESSING", font=subtitle, fill=WHITE)
    draw.text((96, 482), "Mekanism recipes, buffered and isolated", font=body, fill=MUTED)
    draw.text((96, 516), "inside one automation network.", font=body, fill=MUTED)

    stats = [
        ("19", "MACHINES", CYAN),
        ("3", "RESOURCE TYPES", PURPLE),
        ("9", "PATTERN SLOTS", AMBER),
    ]
    x = 96
    for value, name, color in stats:
        draw.rectangle((x, 602, x + 6, 670), fill=color)
        draw.text((x + 19, 595), value, font=load_font("bahnschrift.ttf", 45), fill=WHITE)
        draw.text((x + 19, 649), name, font=label, fill=MUTED)
        x += 183

    draw.text((96, 796), "AE2 PATTERNS   /   ITEMS + FLUIDS + CHEMICALS   /   GTNH-STYLE BUFFERING",
              font=stat, fill=(199, 210, 210, 255))
    draw.rectangle((96, 842, 584, 846), fill=CYAN)


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (WIDTH, HEIGHT), CHARCOAL)
    draw_background(canvas)

    # Rear machines first, then the central reaction chamber.
    draw_cube(canvas, "me_electrolytic_separator_front_online_working.png",
              735, 205, 184, frame=2, glow=CYAN)
    draw_cube(canvas, "me_rotary_condensentrator_front_online_working.png",
              1260, 188, 166, frame=1, glow=PURPLE)
    draw_cube(canvas, "me_machine_chassis_front_online_working.png",
              752, 553, 154, frame=3, glow=CYAN)
    draw_cube(canvas, "me_metallurgic_infuser_front_online_working.png",
              1264, 548, 166, frame=2, glow=PURPLE)
    draw_cube(canvas, "me_pressurized_reaction_chamber_front_online_working.png",
              985, 338, 264, frame=3, glow=AMBER)

    draw_text(canvas)

    # Crisp outer frame and small colored registration marks.
    draw = ImageDraw.Draw(canvas)
    draw.rectangle((26, 26, WIDTH - 27, HEIGHT - 27), outline=(71, 86, 91, 190), width=2)
    for x, y, color in ((26, 26, CYAN), (WIDTH - 42, 26, PURPLE),
                        (26, HEIGHT - 42, AMBER), (WIDTH - 42, HEIGHT - 42, GREEN)):
        draw.rectangle((x, y, x + 16, y + 4), fill=color)
        draw.rectangle((x, y, x + 4, y + 16), fill=color)

    canvas.convert("RGB").save(OUTPUT, quality=95, optimize=True)
    print(OUTPUT)


if __name__ == "__main__":
    main()
