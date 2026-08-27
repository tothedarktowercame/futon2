#!/usr/bin/env python3
"""Report C1 turn survival from an external transcript to Evidence Landscape.

The transcript supplies the denominator.  Evidence is joined by a shared key
when one exists; today it does not, so exact operator-message text is used as a
reported fallback.  A fallback miss is never asserted to mean "not stored".
"""

import collections
import datetime
import glob
import json
import os
import re
import sys
import urllib.parse
import urllib.request


DEFAULT_SESSION = "66f62b84-6002-47d4-9a52-5577415ad163"
TRANSCRIPT_DIR = "/home/joe/.claude/projects/-home-joe-code"
EVIDENCE_URL = "http://localhost:7073/api/alpha/evidence"
LIMIT = 1000


def operator_text(record):
    """Return operator payload text, or None for non-turn/tool-result records."""
    if record.get("type") != "user" or record.get("toolUseResult"):
        return None
    content = record.get("message", {}).get("content")
    if isinstance(content, list):
        if any(isinstance(block, dict) and block.get("type") == "tool_result"
               for block in content):
            return None
        text = "".join(block.get("text", "") for block in content
                       if isinstance(block, dict) and block.get("type") == "text")
    elif isinstance(content, str):
        text = content
    else:
        text = ""
    marker = "\nUser message:\n"
    return text.split(marker, 1)[1] if marker in text else text


def transcript(session_id):
    path = os.path.join(TRANSCRIPT_DIR, session_id + ".jsonl")
    if not os.path.exists(path):
        matches = []
        for candidate in glob.glob(os.path.join(TRANSCRIPT_DIR, "*.jsonl")):
            with open(candidate, encoding="utf-8", errors="replace") as stream:
                if any(session_id in line for line in stream):
                    matches.append(candidate)
        if len(matches) != 1:
            raise SystemExit(
                f"c1_turn_survival: transcript for {session_id}: "
                f"expected one file, found {len(matches)}")
        path = matches[0]

    turns = []
    with open(path, encoding="utf-8", errors="replace") as stream:
        for line_number, line in enumerate(stream, 1):
            try:
                record = json.loads(line)
            except json.JSONDecodeError:
                continue
            if record.get("sessionId") != session_id:
                continue
            text = operator_text(record)
            if text is not None:
                turns.append({"line": line_number,
                              "uuid": record.get("uuid"),
                              "prompt-id": record.get("promptId"),
                              "timestamp": record.get("timestamp"),
                              "text": text})
    return path, turns


def edn_string(raw):
    """Decode the JSON-compatible escapes used in an EDN string body."""
    return json.loads('"' + raw + '"')


def evidence_entries(raw, session_id):
    """Extract only fields needed for the join from the Evidence API's EDN."""
    chunks = [chunk for chunk in re.split(r"(?=\{:evidence/body)", raw)
              if ":evidence/author" in chunk]
    entries = []
    for chunk in chunks:
        session = re.search(r':evidence/session-id "([^"]+)"', chunk)
        if session and session.group(1) != session_id:
            raise SystemExit(
                "c1_turn_survival: session-id filter returned another session; "
                "refusing an unverified API filter")
        if ':event "chat-turn"' not in chunk or ':role "user"' not in chunk:
            continue
        text = re.search(r':text "((?:\\.|[^"\\])*)"\}', chunk, re.S)
        if not text:
            continue
        turn_id = re.search(r':turn-id "([^"]+)"', chunk)
        in_reply_to = re.search(r':evidence/in-reply-to "([^"]+)"', chunk)
        evidence_id = re.search(r':evidence/id "([^"]+)"', chunk)
        entries.append({"text": edn_string(text.group(1)),
                        "turn-id": turn_id.group(1) if turn_id else None,
                        "in-reply-to": in_reply_to.group(1) if in_reply_to else None,
                        "evidence-id": evidence_id.group(1) if evidence_id else None})
    return entries


def fetch_evidence(session_id, turns):
    timestamps = [turn["timestamp"] for turn in turns if turn.get("timestamp")]
    if not timestamps:
        raise SystemExit(f"c1_turn_survival: {session_id}: no transcript timestamps")
    first = min(timestamps)
    day = datetime.datetime.fromisoformat(first.replace("Z", "+00:00")).date()
    since = day.isoformat() + "T00:00:00Z"
    query = urllib.parse.urlencode({"session-id": session_id,
                                    "since": since,
                                    "limit": LIMIT})
    url = EVIDENCE_URL + "?" + query
    with urllib.request.urlopen(url, timeout=180) as response:
        if response.status != 200:
            raise SystemExit(f"c1_turn_survival: HTTP {response.status} for {url}")
        raw = response.read().decode("utf-8")
    counts = re.findall(r':count (\d+)', raw)
    count = int(counts[-1]) if counts else 0
    return evidence_entries(raw, session_id), count >= LIMIT, since, count


def assess(session_id):
    path, turns = transcript(session_id)
    evidence, truncated, since, returned = fetch_evidence(session_id, turns)
    by_text = collections.defaultdict(list)
    for entry in evidence:
        by_text[entry["text"]].append(entry)

    matched = []
    losses = []
    for ordinal, turn in enumerate(turns, 1):
        candidates = by_text.get(turn["text"], [])
        if candidates:
            matched.append((turn, candidates.pop(0)))
        else:
            losses.append({"operator-turn": ordinal,
                           "transcript-line": turn["line"],
                           "transcript-uuid": turn["uuid"],
                           "reason": "unmatchable-by-text"})

    uuid_hits = sum(bool(turn["uuid"]) and
                    any(turn["uuid"] in str(value) for value in entry.values())
                    for turn in turns for entry in evidence)
    prompt_hits = sum(bool(turn["prompt-id"]) and
                      any(turn["prompt-id"] in str(value) for value in entry.values())
                      for turn in turns for entry in evidence)
    return {"session": session_id, "transcript": path, "turns": turns,
            "matched": matched, "losses": losses, "evidence-count": returned,
            "evidence-user-turns": len(evidence), "truncated": truncated,
            "since": since, "uuid-hits": uuid_hits, "prompt-hits": prompt_hits}


def print_report(result):
    print(f"SESSION {result['session']}")
    print(f"  transcript: {result['transcript']}")
    print(f"  evidence-query: session-id={result['session']} "
          f"since={result['since']} limit={LIMIT}")
    print(f"  evidence-query-returned: {result['evidence-count']}; "
          f"could-be-truncated: {'yes' if result['truncated'] else 'no'}")
    print("  mapping: text-fallback (exact operator payload text)")
    print(f"  real-key-check: transcript uuid hits={result['uuid-hits']}; "
          f"promptId hits={result['prompt-hits']}; "
          ":evidence/in-reply-to is an Emacs transport id absent from transcript")
    print("  authoritative-C1: not-computable")
    print("  required-field: evidence must carry :evidence/source-turn-id equal "
          "to the transcript user record's uuid")
    print(f"  fallback-denominator-operator-turns: {len(result['turns'])}")
    print(f"  fallback-numerator-exact-text-matches: {len(result['matched'])}")
    print("  fallback-bound: lower-bound only; a text miss is not proof of non-storage")
    print(f"  typed-losses: {len(result['losses'])}")
    for loss in result["losses"]:
        print("    operator-turn={operator-turn} transcript-line={transcript-line} "
              "transcript-uuid={transcript-uuid} reason={reason}".format(**loss))


def main(argv):
    sessions = argv or [DEFAULT_SESSION]
    results = [assess(session) for session in sessions]
    for index, result in enumerate(results):
        if index:
            print()
        print_report(result)
    print("\nTOTAL")
    print(f"  sessions: {len(results)} ({', '.join(sessions)})")
    print(f"  any-query-could-be-truncated: "
          f"{'yes' if any(r['truncated'] for r in results) else 'no'}")
    print("  mapping: text-fallback; authoritative-C1: not-computable")
    print(f"  fallback-denominator-operator-turns: "
          f"{sum(len(r['turns']) for r in results)}")
    print(f"  fallback-numerator-exact-text-matches: "
          f"{sum(len(r['matched']) for r in results)}")
    reasons = collections.Counter(loss["reason"] for result in results
                                  for loss in result["losses"])
    print("  typed-loss-counts: " +
          (", ".join(f"{reason}={count}" for reason, count in sorted(reasons.items()))
           or "none"))


if __name__ == "__main__":
    main(sys.argv[1:])
