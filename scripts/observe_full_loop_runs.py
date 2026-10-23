#!/usr/bin/env python3
"""Observe the running wm-full-loop stack and emit a deterministic digest
of its attempt outcomes — the observation feed of
M-wm-eat-your-own-tail (holes/missions/M-wm-eat-your-own-tail.md).

Scans data/wm-full-loop/wm-outer-loop-*-v1/attempt-*/007-closed.edn and
counts, per cohort, the adjudicated outcomes and the not-reached kinds
recorded at the earlier checkpoints.  Output is a fixed point of the run
records: same records in, byte-identical EDN out, so a committed digest
can be checked with --check.

Usage:
  python3 scripts/observe_full_loop_runs.py            # write digest
  python3 scripts/observe_full_loop_runs.py --check    # fixed-point check
"""

from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "wm-full-loop"
DIGEST = ROOT / "data" / "wm-full-loop" / "observed-outcomes.edn"

CLOSED_RE = re.compile(r':checkpoint/type :closed')
OUTCOME_RE = re.compile(r':outcome :([a-z0-9-]+)')
NOT_REACHED_RE = re.compile(r':kind :not-reached-([a-z-]+)')


def edn_string(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def cohort_outcomes(cohort_dir: Path):
    """attempt-id -> (closed outcome, sorted not-reached kinds)."""
    out = {}
    for attempt in sorted(cohort_dir.glob("attempt-*")):
        closed = attempt / "007-closed.edn"
        outcome, not_reached = None, set()
        if closed.exists():
            text = closed.read_text(encoding="utf-8")
            for line in text.splitlines():
                if CLOSED_RE.search(line):
                    m = OUTCOME_RE.search(line)
                    if m:
                        outcome = m.group(1)
                m = NOT_REACHED_RE.search(line)
                if m and m.group(1) != "closed":
                    not_reached.add(m.group(1))
        out[attempt.name] = (outcome, tuple(sorted(not_reached)))
    return out


def build_digest() -> str:
    cohorts = sorted(d for d in DATA.glob("wm-outer-loop-*-v1") if d.is_dir())
    lines = ["{:schema :wm/full-loop-observed-outcomes-v1",
             ' :observed "attempt outcomes of every recorded outer-loop cohort, '
             'per data/wm-full-loop (see --check for fixed-point semantics)"',
             " :cohorts {"]
    total = {}
    for c in cohorts:
        att = cohort_outcomes(c)
        counts = {}
        for outcome, _ in att.values():
            key = outcome or "no-closed-record"
            counts[key] = counts.get(key, 0) + 1
            total[key] = total.get(key, 0) + 1
        entry = ", ".join(f":{k} {v}" for k, v in sorted(counts.items()))
        lines.append(f"  {edn_string(c.name)} {{:attempts {len(att)} {entry}}}")
    lines.append("}")
    entry = ", ".join(f":{k} {v}" for k, v in sorted(total.items()))
    lines.append(f" :total {{:cohorts {len(cohorts)} :attempts "
                 f"{sum(len(cohort_outcomes(c)) for c in cohorts)} {entry}}}")
    return "\n".join(lines) + "\n"


def main(argv):
    if "--check" in argv:
        if not DIGEST.exists():
            print("FAIL digest absent", file=sys.stderr)
            return 1
        if build_digest() == DIGEST.read_text(encoding="utf-8"):
            print(f"fixed point: {DIGEST.relative_to(ROOT)} matches the run records")
            return 0
        print("FAIL committed digest is not a fixed point of the run records",
              file=sys.stderr)
        return 1
    DIGEST.write_text(build_digest(), encoding="utf-8")
    print(f"wrote {DIGEST.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
