import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def repair_mojibake(value: str) -> str:
    try:
        return value.encode("cp1251").decode("utf-8")
    except UnicodeError:
        return value


def read_streams(path: Path, source_part: str) -> list[tuple[str, str]]:
    root = ET.parse(path).getroot()
    streams: list[tuple[str, str]] = []
    for node in root.iter():
        if not node.tag.endswith("TextStream"):
            continue
        chunks: list[str] = []
        for child in node:
            if child.text:
                chunks.append(repair_mojibake(child.text).strip())
        text = re.sub(r"\s+", " ", " ".join(chunk for chunk in chunks if chunk)).strip()
        if text:
            streams.append((source_part, text))
    return streams


def collect_entries() -> list[dict]:
    sources = [
        ("assets/source/extracted/part-1-META-INF-textinfo.xml", "part-1"),
        ("assets/source/extracted/part-2-META-INF-textinfo.xml", "part-2"),
    ]
    entries: list[dict] = []
    for source, part in sources:
        part_text = "\n\n".join(text for _, text in read_streams(ROOT / source, part))
        chunks = re.split(r"(?=\[Дата:)", part_text)
        for chunk in chunks:
            chunk = chunk.strip()
            if chunk.startswith("[Дата:"):
                entries.append({"part": part, "chunks": [chunk]})
    return entries


ORDINALS = {
    "первая": 1,
    "вторая": 2,
    "третья": 3,
    "четвертая": 4,
    "четвёртая": 4,
    "пятая": 5,
    "шестая": 6,
    "седьмая": 7,
    "восьмая": 8,
    "девятая": 9,
    "десятая": 10,
    "одиннадцатая": 11,
    "двенадцатая": 12,
    "тринадцатая": 13,
    "четырнадцатая": 14,
}

CLOSINGS = [
    "На сегодня, пожалуй, достаточно. Чернила ещё блестят, а значит, страница пока не стала прошлым.",
    "Я оставлю эту мысль здесь. Пусть полежит до следующего вечера и посмотрит, во что превратится.",
    "Свеча почти догорела. Продолжу, когда Таверна снова подбросит мне повод.",
    "Перо просит отдыха. А спор, кажется, всё равно ещё не сказал последнего слова.",
    "На этом я закрою страницу. Не историю — только сегодняшний шум в голове.",
    "Таверна скрипнула в ответ. Думаю, она согласна: остальное запишем позже.",
    "Пусть этот вечер закончится так: дом найден, чай не остыл, а тайна осталась при мне.",
    "Я остановлюсь здесь, пока путники ещё не поняли, что их уже ждёт история.",
    "На сегодня всё. Цветы в комнате тихо шевелятся, будто тоже читают через плечо.",
    "Запись пора оставить. Дом дышит ровно, и это лучший знак, который я знаю.",
    "Я закрою страницу до следующего шума за дверью. Боги подождут. Им полезно.",
    "На сегодня хватит. Старые имена не исчезают — они просто садятся ближе к огню.",
    "Пока мы ждём, страница тоже ждёт. Иногда это почти одно и то же.",
    "Чернила начинают сохнуть. Я не ставлю точку навсегда — только закрываю глаза до следующей строки.",
]


def normalize_entry(raw: dict, fallback_number: int) -> dict:
    full = "\n\n".join(raw["chunks"])
    date_match = re.search(r"^\[Дата:\s*(.*?)\]\s*", full, re.S)
    date = date_match.group(1).strip() if date_match else ""
    rest = full[date_match.end():].strip() if date_match else full

    title_match = re.search(r"^(Запись\s+([^.]+)\.)\s*(.*)", rest, re.S)
    title = title_match.group(1).strip() if title_match else f"Запись {fallback_number}"
    ordinal = title_match.group(2).strip().lower() if title_match else ""
    number = ORDINALS.get(ordinal, fallback_number)
    after_title = title_match.group(3).strip() if title_match else rest

    paragraphs = [paragraph.strip() for paragraph in after_title.split("\n\n") if paragraph.strip()]
    subtitle = ""
    if paragraphs:
        first = paragraphs[0]
        match = re.match(r"((?:О том|В которой|Когда|Заметки)[^.?!]*[.?!])\s*(.*)", first)
        if match:
            subtitle = match.group(1).strip()
            paragraphs[0] = match.group(2).strip() or first

    part = raw["part"]
    page_index = number if part == "part-1" else max(1, number - 11)
    if part == "part-1":
        page_index = min(page_index, 14)
    else:
        page_index = min(page_index, 6)

    return {
        "id": f"entry-{number:02d}",
        "date": date,
        "title": title,
        "subtitle": subtitle,
        "paragraphs": paragraphs,
        "closing": CLOSINGS[(number - 1) % len(CLOSINGS)],
        "mood": "Запись из CDR",
        "pageNumber": number,
        "sourcePart": "hatter-diary-part-1.cdr" if part == "part-1" else "hatter-diary-part-2.cdr",
        "previewImage": f"assets/images/diary/cdr/{part}-page{page_index}.webp",
        "sourcePreview": f"assets/images/diary/cdr/{part}-page{page_index}.png",
    }


meta = {
    "icon": "assets/images/diary/hatter-diary-icon.webp",
    "iconSource": "assets/images/diary/hatter-diary-icon.png",
    "sourceFiles": [
        "assets/source/hatter-diary-part-1.cdr",
        "assets/source/hatter-diary-part-2.cdr",
    ],
    "extractedFrom": [
        "assets/source/extracted/part-1-META-INF-textinfo.xml",
        "assets/source/extracted/part-2-META-INF-textinfo.xml",
    ],
    "fontFamily": "Segoe Script",
    "fontNote": "Font table in CDR references Segoe Script Cyrillic; no separate webfont file was embedded into the project.",
}

entries = [normalize_entry(entry, index) for index, entry in enumerate(collect_entries(), 1)]
output = (
    "window.TAVERNA_HATTER_DIARY_META = "
    + json.dumps(meta, ensure_ascii=False, indent=2)
    + ";\n\nwindow.TAVERNA_HATTER_DIARY = "
    + json.dumps(entries, ensure_ascii=False, indent=2)
    + ";\n"
)

(ROOT / "data/hatter-diary.js").write_text(output, encoding="utf-8")
print(f"Wrote {len(entries)} diary entries")
