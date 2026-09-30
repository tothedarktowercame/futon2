#!/usr/bin/env python3
"""Build the unchanged session_turn_analysis.py request schema for one task.

The source is the first unchecked item (or --item-line) together with its
containing Markdown section. This command only writes a request; the existing
象 seat and session_turn_analysis.py template/complete perform the reading.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re

HEADING = re.compile(r"^(#{1,6})\s+(.+?)\s*$", re.M)
OPEN_ITEM = re.compile(r"^\s*[-*]\s+\[\s\]\s+\S.*$", re.M)


def choose_item(text, line=None):
    matches = list(OPEN_ITEM.finditer(text))
    if not matches:
        raise ValueError("task file has no unchecked item")
    if line is None:
        return matches[0]
    for match in matches:
        if text.count("\n", 0, match.start()) + 1 == line:
            return match
    raise ValueError(f"line {line} is not an unchecked item")


def containing_section(text, item):
    headings = list(HEADING.finditer(text))
    prior = [h for h in headings if h.start() < item.start()]
    start_heading = prior[-1] if prior else None
    start = start_heading.start() if start_heading else 0
    level = len(start_heading.group(1)) if start_heading else 0
    end = len(text)
    for heading in headings:
        if heading.start() > item.start() and (level == 0 or len(heading.group(1)) <= level):
            end = heading.start()
            break
    return start, end


def sentence_rows(source):
    """Exact nonblank lines are bounded sentences; offsets are code points."""
    rows = []
    for index, match in enumerate(re.finditer(r"(?m)^\s*\S.*$", source), 1):
        start, end = match.span()
        while end > start and source[end - 1].isspace():
            end -= 1
        rows.append({"id": f"s{index}", "start": start, "end": end,
                     "text": source[start:end]})
    return rows


def build_request(path, target=None, item_line=None):
    path = Path(path).resolve()
    raw = path.read_bytes()
    text = raw.decode()
    item = choose_item(text, item_line)
    section_start, section_end = containing_section(text, item)
    source = text[section_start:section_end].rstrip()
    item_start = item.start() - section_start
    item_end = item.end() - section_start
    return {
        "source_text": source,
        "offset_unit": "unicode-codepoint",
        "sentences": sentence_rows(source),
        "analysis_status": "requested",
        "interpretation_version": 1,
        "vocabulary_version": 1,
        "task": {
            "target_id": target or path.stem,
            "file_path": str(path),
            "content_sha256": hashlib.sha256(raw).hexdigest(),
            "item": {"start": item_start, "end": item_end,
                     "line": text.count("\n", 0, item.start()) + 1,
                     "text": text[item.start():item.end()]},
            "section": {"source_start": section_start, "source_end": section_end},
        },
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("file", type=Path)
    parser.add_argument("--target")
    parser.add_argument("--item-line", type=int)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    try:
        request = build_request(args.file, args.target, args.item_line)
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(request, ensure_ascii=False, indent=2) + "\n")
        print(args.out)
    except (OSError, UnicodeError, ValueError) as error:
        parser.exit(1, f"wm_task_reading: {error}\n")


if __name__ == "__main__":
    main()
