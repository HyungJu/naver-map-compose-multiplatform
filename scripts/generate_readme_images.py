#!/usr/bin/env python3

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = ROOT / "docs" / "images"
FONT_PATH = Path("/System/Library/Fonts/AppleSDGothicNeo.ttc")
SCREENSHOTS = {
    "android_overlay": ROOT / "artifacts" / "mobile" / "phase4-android-overlays.png",
    "ios_overlay": ROOT / "artifacts" / "mobile" / "phase4-ios-overlays.png",
    "android_marker": ROOT / "artifacts" / "mobile" / "marker-composable-android-final.png",
    "ios_marker": ROOT / "artifacts" / "mobile" / "marker-composable-ios-final.png",
}


def font(size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT_PATH), size=size)


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, size[0], size[1]), radius=radius, fill=255)
    return mask


def fit_crop(image: Image.Image, target: tuple[int, int], anchor_y: float = 0.5) -> Image.Image:
    src_ratio = image.width / image.height
    dst_ratio = target[0] / target[1]
    if src_ratio > dst_ratio:
        crop_h = image.height
        crop_w = int(crop_h * dst_ratio)
        left = max(0, (image.width - crop_w) // 2)
        box = (left, 0, left + crop_w, crop_h)
    else:
        crop_w = image.width
        crop_h = int(crop_w / dst_ratio)
        top = int((image.height - crop_h) * anchor_y)
        top = max(0, min(top, image.height - crop_h))
        box = (0, top, crop_w, top + crop_h)
    return image.crop(box).resize(target, Image.Resampling.LANCZOS)


def create_card(
    image_path: Path,
    frame_size: tuple[int, int],
    inner_padding: int = 16,
    radius: int = 40,
    anchor_y: float = 0.5,
    background: tuple[int, int, int, int] = (255, 255, 255, 235),
) -> Image.Image:
    source = Image.open(image_path).convert("RGBA")
    card = Image.new("RGBA", frame_size, (0, 0, 0, 0))

    shadow = Image.new("RGBA", frame_size, (0, 0, 0, 0))
    shadow_mask = rounded_mask(frame_size, radius)
    shadow.putalpha(shadow_mask)
    shadow = ImageChops.multiply(shadow, Image.new("RGBA", frame_size, (0, 0, 0, 180)))
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))
    card.alpha_composite(shadow, (0, 18))

    panel = Image.new("RGBA", frame_size, background)
    panel.putalpha(rounded_mask(frame_size, radius))
    card.alpha_composite(panel)

    inner_size = (frame_size[0] - inner_padding * 2, frame_size[1] - inner_padding * 2)
    fitted = fit_crop(source, inner_size, anchor_y=anchor_y)
    fitted.putalpha(rounded_mask(inner_size, radius - 10))
    card.alpha_composite(fitted, (inner_padding, inner_padding))
    return card


def draw_gradient_background(canvas: Image.Image, top: tuple[int, int, int], bottom: tuple[int, int, int]) -> None:
    draw = ImageDraw.Draw(canvas)
    for y in range(canvas.height):
        t = y / max(1, canvas.height - 1)
        color = tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3))
        draw.line((0, y, canvas.width, y), fill=color)


def draw_soft_blob(base: Image.Image, box: tuple[int, int, int, int], color: tuple[int, int, int, int], blur: int) -> None:
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    draw.ellipse(box, fill=color)
    overlay = overlay.filter(ImageFilter.GaussianBlur(blur))
    base.alpha_composite(overlay)


def draw_chip(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, fill: tuple[int, int, int, int]) -> None:
    chip_font = font(26)
    bbox = draw.textbbox((0, 0), text, font=chip_font)
    width = bbox[2] - bbox[0] + 34
    height = bbox[3] - bbox[1] + 20
    x, y = xy
    draw.rounded_rectangle((x, y, x + width, y + height), radius=height // 2, fill=fill)
    draw.text((x + 17, y + 7), text, font=chip_font, fill=(24, 30, 44))


def wrap_text(text: str, text_font: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    dummy = ImageDraw.Draw(Image.new("RGB", (10, 10)))
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = word if not current else f"{current} {word}"
        if dummy.textbbox((0, 0), candidate, font=text_font)[2] <= max_width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def create_banner() -> Path:
    canvas = Image.new("RGBA", (1600, 900), (247, 250, 255, 255))
    draw_gradient_background(canvas, (244, 250, 248), (234, 238, 255))
    draw_soft_blob(canvas, (-120, 420, 560, 1080), (101, 190, 143, 85), 90)
    draw_soft_blob(canvas, (920, -140, 1560, 420), (89, 119, 255, 70), 85)
    draw_soft_blob(canvas, (740, 540, 1600, 1100), (122, 89, 214, 65), 100)

    overlay = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    line_draw = ImageDraw.Draw(overlay)
    line_draw.arc((980, 180, 1450, 700), 180, 288, fill=(61, 122, 255, 120), width=7)
    line_draw.arc((860, 260, 1530, 840), 200, 328, fill=(27, 185, 115, 92), width=5)
    line_draw.arc((1010, 210, 1510, 810), 195, 310, fill=(121, 76, 214, 92), width=6)
    canvas.alpha_composite(overlay)

    draw = ImageDraw.Draw(canvas)
    title_font = font(86)
    subtitle_font = font(34)
    body_font = font(28)

    draw.text((110, 120), "NAVER Map Compose", font=title_font, fill=(18, 24, 38))
    draw.text((110, 212), "Multiplatform", font=title_font, fill=(18, 24, 38))
    draw.text(
        (110, 338),
        "Android와 iOS에서 같은 Compose API로\n지도를 그리고, 카메라 상태와 오버레이를 공유합니다.",
        font=subtitle_font,
        fill=(56, 65, 84),
        spacing=12,
    )
    draw.text(
        (110, 486),
        "Kotlin Multiplatform · Compose Multiplatform · NAVER Map SDK",
        font=body_font,
        fill=(70, 83, 108),
    )

    chip_y = 560
    chips = [
        ("Android", (203, 236, 216, 220)),
        ("iOS", (219, 229, 255, 220)),
        ("CameraState", (238, 232, 255, 230)),
        ("Overlays", (224, 244, 242, 230)),
    ]
    chip_x = 110
    for label, fill in chips:
        draw_chip(draw, (chip_x, chip_y), label, fill)
        chip_font = font(26)
        bbox = draw.textbbox((0, 0), label, font=chip_font)
        chip_x += (bbox[2] - bbox[0]) + 60

    android_card = create_card(
        SCREENSHOTS["android_marker"],
        frame_size=(310, 620),
        anchor_y=0.44,
        background=(255, 255, 255, 238),
    )
    ios_card = create_card(
        SCREENSHOTS["ios_overlay"],
        frame_size=(330, 660),
        anchor_y=0.40,
        background=(255, 255, 255, 242),
    )
    small_card = create_card(
        SCREENSHOTS["ios_marker"],
        frame_size=(250, 250),
        anchor_y=0.58,
        radius=32,
        background=(255, 255, 255, 228),
    )

    ios_card = ios_card.rotate(-6, resample=Image.Resampling.BICUBIC, expand=True)
    android_card = android_card.rotate(8, resample=Image.Resampling.BICUBIC, expand=True)
    small_card = small_card.rotate(-4, resample=Image.Resampling.BICUBIC, expand=True)

    canvas.alpha_composite(ios_card, (940, 110))
    canvas.alpha_composite(android_card, (1170, 220))
    canvas.alpha_composite(small_card, (1030, 580))

    badge = Image.new("RGBA", (320, 92), (255, 255, 255, 210))
    badge.putalpha(rounded_mask((320, 92), 28))
    badge_draw = ImageDraw.Draw(badge)
    badge_draw.text((28, 18), "Shared API Preview", font=font(28), fill=(24, 30, 44))
    badge_draw.text((28, 50), "실제 모바일 캡처 기반", font=font(20), fill=(91, 101, 124))
    badge = badge.filter(ImageFilter.GaussianBlur(0.2))
    canvas.alpha_composite(badge, (950, 86))

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    output = OUTPUT_DIR / "readme-banner.png"
    canvas.convert("RGB").save(output, quality=96)
    return output


def create_showcase() -> Path:
    canvas = Image.new("RGBA", (1600, 1180), (247, 249, 252, 255))
    draw_gradient_background(canvas, (248, 250, 252), (241, 245, 251))
    draw_soft_blob(canvas, (1050, 720, 1700, 1360), (86, 173, 128, 48), 90)
    draw_soft_blob(canvas, (-180, 740, 540, 1380), (124, 98, 226, 42), 100)

    draw = ImageDraw.Draw(canvas)
    draw.text((120, 90), "Preview", font=font(64), fill=(20, 25, 40))
    draw.text(
        (120, 172),
        "Android와 iOS에서 확인한 실제 샘플 화면입니다.",
        font=font(28),
        fill=(85, 94, 115),
    )

    entries = [
        ("Android Overlay", "오버레이와 줌 컨트롤", SCREENSHOTS["android_overlay"], 0.44),
        ("iOS Overlay", "동일한 공용 UI 흐름", SCREENSHOTS["ios_overlay"], 0.42),
        ("Android Marker", "Compose 마커 렌더링", SCREENSHOTS["android_marker"], 0.48),
        ("iOS Marker", "플랫폼 간 시각 일관성", SCREENSHOTS["ios_marker"], 0.47),
    ]
    positions = [(120, 260), (840, 260), (120, 710), (840, 710)]

    for (title, subtitle, path, anchor_y), (x, y) in zip(entries, positions):
        card = Image.new("RGBA", (640, 380), (255, 255, 255, 0))
        shadow = Image.new("RGBA", (640, 380), (0, 0, 0, 0))
        shadow.putalpha(rounded_mask((640, 380), 34))
        shadow = ImageChops.multiply(shadow, Image.new("RGBA", (640, 380), (0, 0, 0, 110)))
        shadow = shadow.filter(ImageFilter.GaussianBlur(16))
        card.alpha_composite(shadow, (0, 12))

        panel = Image.new("RGBA", (640, 380), (255, 255, 255, 235))
        panel.putalpha(rounded_mask((640, 380), 34))
        card.alpha_composite(panel)

        thumb = fit_crop(Image.open(path).convert("RGBA"), (260, 330), anchor_y=anchor_y)
        thumb.putalpha(rounded_mask((260, 330), 26))
        card.alpha_composite(thumb, (24, 24))

        card_draw = ImageDraw.Draw(card)
        title_font = font(28)
        subtitle_font = font(24)
        title_lines = wrap_text(title, title_font, 280)
        title_y = 42
        for line in title_lines:
            card_draw.text((314, title_y), line, font=title_font, fill=(20, 25, 40))
            title_y += 38
        subtitle_y = title_y + 8
        card_draw.text((314, subtitle_y), subtitle, font=subtitle_font, fill=(89, 99, 119))
        chip_start_y = subtitle_y + 56
        card_draw.rounded_rectangle((314, chip_start_y, 594, chip_start_y + 56), radius=20, fill=(240, 244, 250))
        card_draw.text((338, chip_start_y + 16), "Shared Compose API", font=font(22), fill=(55, 66, 84))
        card_draw.rounded_rectangle((314, chip_start_y + 72, 502, chip_start_y + 122), radius=18, fill=(230, 242, 236))
        card_draw.text((336, chip_start_y + 87), "Android / iOS", font=font(20), fill=(41, 86, 62))
        card_draw.rounded_rectangle((314, chip_start_y + 136, 552, chip_start_y + 186), radius=18, fill=(236, 232, 250))
        card_draw.text((336, chip_start_y + 151), "Map · Camera · Overlay", font=font(20), fill=(71, 56, 113))

        canvas.alpha_composite(card, (x, y))

    output = OUTPUT_DIR / "readme-showcase.png"
    canvas.convert("RGB").save(output, quality=96)
    return output


def main() -> None:
    banner = create_banner()
    showcase = create_showcase()
    print(banner)
    print(showcase)


if __name__ == "__main__":
    main()
