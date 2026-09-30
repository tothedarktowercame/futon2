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


def request_base(path, source, source_start, target):
    raw = path.read_bytes()
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
            "excerpt_sha256": hashlib.sha256(source.encode("utf-8")).hexdigest(),
            "section": {"source_start": source_start,
                        "source_end": source_start + len(source)},
        },
    }


def build_request(path, target=None, item_line=None):
    path = Path(path).resolve()
    text = path.read_text()
    item = choose_item(text, item_line)
    section_start, section_end = containing_section(text, item)
    source = text[section_start:section_end].rstrip()
    item_start = item.start() - section_start
    item_end = item.end() - section_start
    request = request_base(path, source, section_start, target)
    request["task"]["item"] = {
        "start": item_start, "end": item_end,
        "line": text.count("\n", 0, item.start()) + 1,
        "text": text[item.start():item.end()],
    }
    request["task"]["source_kind"] = "open-item-section"
    return request


def build_mission_request(path, target=None):
    """Read a mission's operator-voice HEAD, or its opening when absent."""
    path = Path(path).resolve()
    text = path.read_text()
    heads = [h for h in HEADING.finditer(text)
             if len(h.group(1)) == 2 and h.group(2).strip().lower() == "head"]
    if heads:
        heading = heads[0]
        start = heading.start()
        end = next((h.start() for h in HEADING.finditer(text)
                    if h.start() > start and len(h.group(1)) <= 2), len(text))
        source_kind = "head-section"
    else:
        start = 0
        end = next((h.start() for h in HEADING.finditer(text)
                    if len(h.group(1)) == 2), len(text))
        source_kind = "opening-before-first-section"
    source = text[start:end].rstrip()
    if not source:
        raise ValueError("mission reading source is empty")
    request = request_base(path, source, start, target)
    request["task"]["source_kind"] = source_kind
    return request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("file", type=Path)
    parser.add_argument("--target")
    parser.add_argument("--item-line", type=int)
    parser.add_argument("--mission-head", action="store_true",
                        help="read HEAD, or the opening before the first ## heading")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    try:
        if args.mission_head and args.item_line is not None:
            raise ValueError("--item-line cannot be combined with --mission-head")
        request = (build_mission_request(args.file, args.target)
                   if args.mission_head
                   else build_request(args.file, args.target, args.item_line))
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(request, ensure_ascii=False, indent=2) + "\n")
        print(args.out)
    except (OSError, UnicodeError, ValueError) as error:
        parser.exit(1, f"wm_task_reading: {error}\n")


if __name__ == "__main__":
    main()
