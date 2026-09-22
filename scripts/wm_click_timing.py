#!/usr/bin/env python3
"""wm_click_timing.py <attempt-dir> -- read-only timing report for one WM click.

PROOF-wm-works <1>1 <2>5: timing is REPORTING ONLY. No budget, no threshold,
no gate; exit 0 on success, 2 on bad arguments or an unreadable attempt.

Wall time is decomposed over the intervals between consecutive checkpoint
events (00N-*.edn, ordered by :event/sequence, timed by :recorded-at).
Agent-turn intervals are exactly these checkpoint pairs:
  dispatch -> build        the author's turn AND the build-resolution /
                           build-cure machine work that follows it in the same
                           interval (full_loop_runner.clj ~4621); the attempt
                           checkpoints do not record a phase split inside it,
                           so it is reported as NOT SEPARABLE and the totals
                           are given as bounds rather than a false split.
  build    -> adjudication the reviewer's turn (in recorded clicks the
                           reviewer's turn also falls inside the dispatch->build
                           interval, so this pair reads near zero).
Machine intervals are all the others, measured exactly.
Checkpoints are written strictly in sequence, so intervals never overlap:
  wall = (agent-turn intervals) + (machine intervals)          [overlap 0]
and since the dispatch->build interval mixes agent and machine time:
  machine >= measured machine intervals
  agent wait <= agent-turn intervals  (upper bound; overcounts, never
  undercounts -- the build-resolution tail is inside)
No sub-interval split is invented (codex-20 review, 2026-09-22).
"""
import re
import sys
from datetime import datetime
from pathlib import Path

TURN_PAIRS = {
    ("dispatch", "build"):
        "author turn + build resolution, not separable from the records",
    ("build", "adjudication"):
        "reviewer's turn (reviewer-wait)",
}


def parse_edn_event(path: Path):
    text = path.read_text(encoding="utf-8", errors="replace")
    seq = re.search(r":event/sequence (\d+)", text)
    ctype = re.search(r":checkpoint/type :([a-z-]+)", text)
    recorded = re.search(r':recorded-at "([^"]+)"', text)
    if not (seq and ctype and recorded):
        return None
    ts = datetime.fromisoformat(recorded.group(1).replace("Z", "+00:00"))
    return {"seq": int(seq.group(1)), "type": ctype.group(1), "at": ts, "file": path.name}


def main(argv):
    if len(argv) != 2:
        print("usage: wm_click_timing.py <attempt-dir>", file=sys.stderr)
        return 2
    d = Path(argv[1])
    if not d.is_dir():
        print(f"not a directory: {d}", file=sys.stderr)
        return 2
    events = [e for e in (parse_edn_event(p) for p in sorted(d.glob("0*.edn"))) if e]
    if len(events) < 2:
        print("fewer than two timed checkpoints", file=sys.stderr)
        return 2
    events.sort(key=lambda e: e["seq"])

    wall = (events[-1]["at"] - events[0]["at"]).total_seconds()
    turn = 0.0
    print(f"attempt {d.name}: {len(events)} checkpoints "
          f"{events[0]['at'].isoformat()} -> {events[-1]['at'].isoformat()}")
    print(f"{'interval':<62}{'seconds':>10}  class")
    for a, b in zip(events, events[1:]):
        seconds = (b["at"] - a["at"]).total_seconds()
        label = TURN_PAIRS.get((a["type"], b["type"]))
        if label:
            turn += seconds
            row = f"{a['type']}->{b['type']} ({label})"
            print(f"{row:<62}{seconds:>10.1f}  agent turn (mixed)")
        else:
            print(f"{a['type']}->{b['type']}".ljust(62)
                  + f"{seconds:>10.1f}  machine (exact)")
    machine_exact = wall - turn
    overlap = 0.0  # sequential :recorded-at; see docstring
    print()
    print(f"wall time      {wall:>10.1f} s   (exact: first -> last :recorded-at)")
    print(f"agent-turn     {turn:>10.1f} s   (mixed intervals above; "
          f"agent wait <= this)")
    print(f"machine        {machine_exact:>10.1f} s   (exact intervals only; "
          f"machine >= this -- the build-resolution tail is inside the "
          f"agent-turn interval)")
    print(f"overlap        {overlap:>10.1f} s   (checkpoints are sequential)")
    print(f"check: wall = agent-turn + machine-exact - overlap -> "
          f"{turn:.1f} + {machine_exact:.1f} - {overlap:.1f} = "
          f"{turn + machine_exact - overlap:.1f} (wall {wall:.1f})")
    print("bounds: agent wait <= "
          f"{turn:.1f} s; machine >= {machine_exact:.1f} s; "
          "no split inside dispatch->build is invented.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
