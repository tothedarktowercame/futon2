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


# Records the harness writes into the transcript with type "user" but which are
# not operator turns: they were never sent, so they cannot have been stored and
# they must not sit in C1's denominator.  Typed rather than lumped, because
# "the summary injection" and "a slash command" are different things.
HARNESS_KINDS = [
    ("compaction-summary",
     "This session is being continued from a previous conversation"),
    ("command-caveat", "<local-command-caveat>"),
    ("command-output", "<local-command-stdout>"),
    ("command-output", "<local-command-stderr>"),
    ("slash-command", "<command-name>"),
]


def harness_kind(text):
    """The kind of harness record this is, or None if it is an operator turn."""
    head = text.lstrip()[:400]
    for kind, marker in HARNESS_KINDS:
        if head.startswith(marker) or marker in head:
            return kind
    return None


def normalised(text):
    """Both sides of the text join, reduced to what they have in common.

    The transcript carries the emacs transport wrapper and the store carries
    the payload, with whitespace differing between them, so exact equality
    reports losses that are joins failing rather than turns missing.
    """
    marker = "\nUser message:\n"
    if marker in text:
        text = text.split(marker, 1)[1]
    return " ".join(text.split())


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
        # NOT anchored on "}: :text is not always the last field in the body,
        # and anchoring dropped every entry where it is not, which showed up as
        # turns "missing" from the store that were in it.
        text = re.search(r':text "((?:\\.|[^"\\])*)"', chunk, re.S)
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


def retrieval_queries(raw):
    """The `query` strings of context-retrieval events, which the store keeps
    TRUNCATED.  A turn appearing only here was processed downstream without ever
    being stored as a turn — a different loss from not being there at all."""
    return [q for q in re.findall(r'\\"query\\" \\"((?:[^\\\\"]|\\\\.)*)\\"', raw)]


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
    return (evidence_entries(raw, session_id), count >= LIMIT, since, count,
            retrieval_queries(raw))


def assess(session_id):
    path, turns = transcript(session_id)
    evidence, truncated, since, returned, queries = fetch_evidence(session_id, turns)
    norm_queries = [" ".join(q.split()) for q in queries]
    by_text = collections.defaultdict(list)
    for entry in evidence:
        by_text[entry["text"]].append(entry)

    norm_index = [(normalised(entry["text"]), entry) for entry in evidence]

    matched = []
    losses = []
    excluded = []
    ordinal = 0
    for turn in turns:
        kind = harness_kind(turn["text"])
        if kind:
            excluded.append({"transcript-line": turn["line"],
                             "transcript-uuid": turn["uuid"], "kind": kind})
            continue
        ordinal += 1
        target = normalised(turn["text"])
        candidates = by_text.get(turn["text"], [])
        if candidates:
            matched.append((turn, candidates.pop(0), "exact"))
            continue
        hit = next((e for n, e in norm_index if n == target), None)
        rule = "normalised"
        if hit is None and target:
            hit = next((e for n, e in norm_index
                        if target in n or n in target), None)
            rule = "normalised-containment"
        if hit is not None:
            matched.append((turn, hit, rule))
        else:
            # The store keeps the retrieval query truncated, so compare on the
            # shorter of the two rather than requiring containment one way.
            def seen_as_query(target=target):
                for q in norm_queries:
                    n = min(len(q), len(target))
                    if n >= 40 and q[:n] == target[:n]:
                        return True
                return False
            reason = ("present-only-as-retrieval-query" if seen_as_query()
                      else "not-stored")
            losses.append({"operator-turn": ordinal,
                           "transcript-line": turn["line"],
                           "transcript-uuid": turn["uuid"],
                           "reason": reason})

    uuid_hits = sum(bool(turn["uuid"]) and
                    any(turn["uuid"] in str(value) for value in entry.values())
                    for turn in turns for entry in evidence)
    prompt_hits = sum(bool(turn["prompt-id"]) and
                      any(turn["prompt-id"] in str(value) for value in entry.values())
                      for turn in turns for entry in evidence)
    return {"session": session_id, "transcript": path, "turns": turns,
            "excluded": excluded, "operator-turns": ordinal,
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
    print(f"  transcript-user-records: {len(result['turns'])}")
    kinds = collections.Counter(x["kind"] for x in result["excluded"])
    print("  excluded-as-not-operator-turns: "
          f"{len(result['excluded'])}"
          + (" (" + ", ".join(f"{k}={n}" for k, n in sorted(kinds.items())) + ")"
             if kinds else ""))
    print(f"  fallback-denominator-operator-turns: {result['operator-turns']}")
    rules = collections.Counter(rule for _, _, rule in result["matched"])
    print(f"  fallback-numerator-text-matches: {len(result['matched'])}"
          + (" (" + ", ".join(f"{r}={n}" for r, n in sorted(rules.items())) + ")"
             if rules else ""))
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
    print(f"  excluded-as-not-operator-turns: "
          f"{sum(len(r['excluded']) for r in results)}")
    print(f"  fallback-denominator-operator-turns: "
          f"{sum(r['operator-turns'] for r in results)}")
    print(f"  fallback-numerator-text-matches: "
          f"{sum(len(r['matched']) for r in results)}")
    reasons = collections.Counter(loss["reason"] for result in results
                                  for loss in result["losses"])
    print("  typed-loss-counts: " +
          (", ".join(f"{reason}={count}" for reason, count in sorted(reasons.items()))
           or "none"))


if __name__ == "__main__":
    main(sys.argv[1:])
