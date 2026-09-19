#!/usr/bin/env python3
"""Ordinary-click budget: how much of Joe's grant is left.

Joe, 2026-09-19 (holes/labs/wm-contract/AUTH-ordinary-click-budget-2026-09-19.md):
"I'll authorize them 5 at a time because I don't want unbounded execution...
when that is exhausted come back to me for more. This is also contingent on
proper record keeping not only for runs but bugs and repairs."

Consumption accounting is claude-4's call as OPS node owner. THIS SCRIPT IS THE
INTERIM MECHANISM, and it is deliberately the weaker of the two halves:

  INTERIM (here): count ordinary-click run records with :startedAt after the
  grant. An ordinary click is a run on the gated on-demand click path, which is
  what carries :click/id.

  DURABLE (not built): the click seam writes an append-only consumption entry
  AT ISSUE TIME, before the run can fail. Then this enumeration becomes a
  CHECK against that ledger rather than the source of truth, and issued-minus-
  recorded is a finding instead of an invisible loss.

THE LIMITATION, stated rather than smoothed, because Joe's grant turns on it:
there is no independent index of issued click ids anywhere in this repo — the
run record is the only durable trace a click leaves. So a click that dies
before its run record is written is INVISIBLE TO THIS COUNT, and it consumed
budget without honoring the contingency. This script cannot detect that. The
durable mechanism above is exactly what would.

    python3 scripts/ordinary_click_budget.py
"""
import re, sys, pathlib, datetime

HERE = pathlib.Path(__file__).resolve().parent.parent
RUNS = HERE / "data" / "wm-runs"
GRANT = "2026-09-19T15:21:34+00:00"   # commit time of futon2 d18e4f9c
ALLOCATED = 5

grant = datetime.datetime.fromisoformat(GRANT)
click_re = re.compile(rb':click/id\s+"([^"]+)"')
start_re = re.compile(rb':startedAt\s+"([^"]+)"')

rows = []
for f in sorted(RUNS.glob("*.edn")):
    # Read the WHOLE file, not a head slice. First cut of this script read
    # 4096 bytes and found 6 of the 10 click records: in a grounded-change run
    # the record opens with :runner-execution/identity and a large :terminal,
    # so :click/id lands well past 4 KB. An enumerator that silently misses
    # records is the same defect it exists to prevent, so the cheap correct
    # read wins over the clever one (ten files, ~1.7 MB each).
    blob = f.read_bytes()
    c, s = click_re.search(blob), start_re.search(blob)
    if not c:
        continue                      # not a click-path run
    when = None
    if s:
        raw = s.group(1).decode()
        try:
            when = datetime.datetime.fromisoformat(raw.replace("Z", "+00:00"))
        except ValueError:
            pass
    rows.append((when, c.group(1).decode(), f.name))

after = [r for r in rows if r[0] and r[0] > grant]
undated = [r for r in rows if not r[0]]

print(f"grant      : {GRANT}  (futon2 d18e4f9c)")
print(f"allocated  : {ALLOCATED}")
print(f"consumed   : {len(after)}")
print(f"remaining  : {ALLOCATED - len(after)}")
print(f"click-path run records total: {len(rows)}  (before grant: {len(rows)-len(after)-len(undated)})")
for when, cid, name in after:
    print(f"  CONSUMED {when.isoformat()}  {cid}  {name}")
if undated:
    # fail closed: a run record with no readable :startedAt cannot be shown to
    # predate the grant, so it is reported, never silently excluded.
    print(f"  !! {len(undated)} click record(s) with no readable :startedAt — "
          "cannot be shown to predate the grant:")
    for _, cid, name in undated:
        print(f"     {cid}  {name}")
print()
print("NOT COUNTED HERE: any click that died before writing a run record. "
      "There is no independent index of issued click ids to compare against; "
      "building one is the durable mechanism and it is not built.")
if len(after) >= ALLOCATED:
    print()
    print("BUDGET EXHAUSTED — return to Joe for the next allocation. This "
          "refusal is Joe-commissioned by the AUTH record; it is not a "
          "liability-guard.")
    sys.exit(1)
