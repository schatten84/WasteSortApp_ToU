#!/usr/bin/env python3
"""
Generiert die 20 QR-Codes fuer die WasteSort-Studien-Stimulus-Karten.

QR-Payload = nur die stimulus_id (z.B. "BG-WASTE-09"), NICHTS weiter
(keine Klassifikation, keine Teilnehmerdaten - siehe Spezifikation Punkt 4/21).

Ausgabe:
  qr_codes/BG-WASTE-01.png ... BG-WASTE-20.png   (je 600x600px, Fehlerkorrektur H)
  qr_codes/overview_sheet.png                     (druckfertiges Uebersichtsblatt, 4 Spalten)

Verwendung:
  pip install qrcode[pil]
  python3 generate_qr_codes.py
"""

import os
import qrcode
from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "qr_codes")
ITEM_COUNT = 20
ID_FORMAT = "BG-WASTE-{:02d}"

# Item-Namen nur fuer die Beschriftung des Uebersichtsblatts (keine Studienlogik hier -
# die eigentliche Zuordnung lebt ausschliesslich in StimulusRepository.kt)
ITEM_LABELS = {
    1: "Zerbrochene Keramiktasse", 2: "Gebrauchtes Klebeband", 3: "Windel",
    4: "Zerkratzte CD/DVD", 5: "Zeitung", 6: "Flachgefaltete Kartonage",
    7: "Papierumschlag", 8: "Papiertragetasche", 9: "Apfelrest",
    10: "Kaffeesatz mit Papierfilter", 11: "Gemueseschale", 12: "Eierschale",
    13: "Joghurtbecher", 14: "Alufolie", 15: "Getraenkekarton (Tetra Pak)",
    16: "Shampooflasche (Kunststoff)", 17: "Gruene Weinflasche",
    18: "Braune Arzneimittelflasche (Glas)",
    19: "Klare Aperini-Flasche", 20: "Blaue Mineralwasserflasche",
}


def generate_single_qr(stimulus_id: str) -> Image.Image:
    qr = qrcode.QRCode(
        version=None,
        error_correction=qrcode.constants.ERROR_CORRECT_H,  # robust gegen Druck-/Lichtartefakte
        box_size=10,
        border=4,
    )
    qr.add_data(stimulus_id)
    qr.make(fit=True)
    return qr.make_image(fill_color="black", back_color="white").convert("RGB")


def try_load_font(size: int):
    for path in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    tiles = []

    for n in range(1, ITEM_COUNT + 1):
        stimulus_id = ID_FORMAT.format(n)
        qr_img = generate_single_qr(stimulus_id)

        # Einzeldatei mit Beschriftung darunter (fuer direkten Druck auf Setcards)
        label = ITEM_LABELS.get(n, "")
        canvas = Image.new("RGB", (qr_img.width, qr_img.height + 70), "white")
        canvas.paste(qr_img, (0, 0))
        draw = ImageDraw.Draw(canvas)
        font_id = try_load_font(28)
        font_label = try_load_font(18)
        id_text = stimulus_id
        draw.text((canvas.width / 2, qr_img.height + 8), id_text, fill="black",
                   font=font_id, anchor="ma")
        draw.text((canvas.width / 2, qr_img.height + 42), label, fill="#555555",
                   font=font_label, anchor="ma")

        out_path = os.path.join(OUTPUT_DIR, f"{stimulus_id}.png")
        canvas.save(out_path)
        print(f"OK  {out_path}")
        tiles.append(canvas)

    # ── Druckfertiges Uebersichtsblatt (4 Spalten x 5 Reihen) ────────────────
    cols, rows = 4, 5
    pad = 24
    tile_w, tile_h = tiles[0].width, tiles[0].height
    sheet_w = cols * tile_w + (cols + 1) * pad
    sheet_h = rows * tile_h + (rows + 1) * pad + 60
    sheet = Image.new("RGB", (sheet_w, sheet_h), "white")
    sheet_draw = ImageDraw.Draw(sheet)
    sheet_draw.text((sheet_w / 2, 16), "WasteSort – Stimulus-Karten QR-Codes (BG-WASTE-01 bis 20)",
                     fill="black", font=try_load_font(24), anchor="ma")

    for idx, tile in enumerate(tiles):
        col = idx % cols
        row = idx // cols
        x = pad + col * (tile_w + pad)
        y = 60 + pad + row * (tile_h + pad)
        sheet.paste(tile, (x, y))

    sheet_path = os.path.join(OUTPUT_DIR, "overview_sheet.png")
    sheet.save(sheet_path)
    print(f"OK  {sheet_path}  (Uebersichtsblatt, druckfertig)")


if __name__ == "__main__":
    main()
