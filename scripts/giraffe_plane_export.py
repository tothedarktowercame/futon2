#!/usr/bin/env python3
"""One-way export of Giraffe items into Plane (items + labels only).

Giraffe's run files are the source of truth; Plane is rebuilt from them.
Idempotent: work items carry external_source="giraffe" and
external_id=<item id>, so reruns update in place and never duplicate.
Nothing is ever read back from Plane into Giraffe or the WM.

Inputs:
  - a Giraffe run directory (default: newest under the storage root,
    /home/joe/code/storage/giraffe) -- input.edn (item roster) and
    pass.edn (readiness per item).  Optional --run <dir-or-stamp>.
  - optional --census <tsv> (~/notes/wm-next/giraffe-quality-census-*.tsv)
    adds one Plane label per failed census check.

Endpoints used (Plane 1.4.2, apps/api/plane/api/):
  urls/project.py:11   GET/POST  workspaces/<slug>/projects/
  urls/state.py:11     GET/POST  workspaces/<slug>/projects/<id>/states/
  urls/label.py:11     GET/POST  workspaces/<slug>/projects/<id>/labels/
  urls/work_item.py:110 GET/POST workspaces/<slug>/projects/<id>/work-items/
  urls/work_item.py:118 PATCH/GET .../work-items/<pk>/
IssueSerializer (serializers/issue.py:46) accepts name, description_html,
state, labels (write-only list of label ids, line 60), external_source,
external_id.  POST replies 409 on duplicate external id
(views/issue.py:553-565).  GET list supports ?external_id=&external_source=
(views/issue.py:406-411).

stdlib only: urllib, json, csv, argparse, html.
"""

import argparse
import csv
import html
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

DEFAULT_STORAGE_ROOT = "/home/joe/code/storage/giraffe"
DEFAULT_CENSUS = os.path.expanduser(
    "~/notes/wm-next/giraffe-quality-census-2026-11-07.tsv"
)
API_BASE = os.environ.get("PLANE_API_BASE", "http://127.0.0.1:8094/api/v1")
TOKEN_PATH = os.environ.get("PLANE_TOKEN_PATH", os.path.expanduser("~/plane/.api-token"))
WORKSPACE = "giraffe"
EXTERNAL_SOURCE = "giraffe"

KINDS = {"M": "mission", "T": "ticket", "E": "excursion"}

# Wing names (notes/commons-map/map.edn), now carried as labels.
WINGS = {
    "A": ("Records carry warrant", "WARRANT"),
    "B": ("One queryable self account", "SELFACC"),
    "C": ("Feedback reaches every participant", "FEEDBACK"),
    "D": ("Accountable next action choice / AIF outer loop", "OUTERLOOP"),
    "V": ("Viability / body and energy", "VIABLE"),
    "none": ("Outside the wings", "NOWING"),
    "unplaced": ("Not yet placed", "UNPLACED"),
}
# Projects are benefit trunks (Joe, 2026-11-08: benefit lines are "trunks",
# projects "like the branches"; wings become labels).  key -> (project name,
# identifier); names avoid the characters Plane refuses (PLANE_FORBIDDEN_NAME).
TRUNKS = {
    "L": ("Learning", "LEARNING"),
    "P": ("Patterns and the Alexandrian way", "PATTERNS"),
    "C": ("Clients and income", "CLIENTS"),
    "K": ("Collaborators", "COLLAB"),
    "G": ("Community and governance", "COMMUNITY"),
    "N": ("Newcomers and users", "NEWCOMERS"),
    "commons": ("Commons and public", "COMMONS"),
    "harm": ("Harm avoided", "HARMS"),
    "nostated": ("No stated benefit or harm", "NOSTATED"),
    "notrunk": ("Not on a trunk yet", "NOTRUNK"),
}
DEFAULT_TRUNKS = "/home/joe/notes/cascade-live-outline/trunks-2026-11-08.tsv"
REPAIRS = ("WM repairs", "REPAIRS")
PLANE_FORBIDDEN_NAME = r"^.*[&+,:;$^}{*=?@#|'<>.()%!-].*$"
# Superseded projects, deleted after sync: the kind-named projects of the
# first export, then the wing projects (wings are labels from 2026-11-08).
RETIRED_PROJECTS = ("Missions", "Tickets", "Excursions") + tuple(
    name for name, _ in WINGS.values())
DEFAULT_A1 = "/home/joe/code/futon3c/holes/excursions/ant-reads/A1-placements.tsv"
DEFAULT_EARLY = "/home/joe/notes/commons-map/placements-[12]*.tsv"
DEFAULT_REPAIRS = "/home/joe/code/futon2/records/repair"
TERMINAL_STATE_GROUPS = {"completed", "cancelled"}

# Plane State.groups (db/models/state.py StateGroup): we use "started" for
# Ready and "unstarted" for Not ready.
STATE_SPECS = {
    "Ready": {"group": "started", "color": "#00a550"},
    "Not ready": {"group": "unstarted", "color": "#60646C"},
    "Open": {"group": "unstarted", "color": "#60646C"},
    "Done": {"group": "completed", "color": "#46a758"},
}

# Census check -> label name (one label per failed check).
CENSUS_LABELS = {
    "gated-on-joe": "gated",
    "prose-only-wait": "prose_only",
    "close-unstated": "close_cond",
    "header-unread": "header",
    "timebox-absent": "timebox_in_file",
}
LABEL_COLORS = {
    "close-unstated": "#e0b309",
    "gated-on-joe": "#d46767",
    "prose-only-wait": "#a155b3",
    "header-unread": "#3a7bd5",
    "timebox-absent": "#7f8da0",
    "giraffe-ready": "#00a550",
}


# ---------------------------------------------------------------- EDN reader

_TOKEN = re.compile(
    r'''[()\[\]{}]|\s+|"(?:[^"\\]|\\.)*"|;[^\n]*|:[^,\s()\[\]{}]+|[^\s()\[\]{}]+'''
)


def parse_edn(text):
    """Minimal EDN parser: maps, vectors, lists, sets, keywords, strings,
    numbers, booleans, nil.  Keywords stay as ':foo' strings."""
    toks = [
        t
        for t in _TOKEN.findall(text)
        if not t.isspace() and t != "," and not t.startswith(";")
    ]
    pos = 0

    def rd():
        nonlocal pos
        t = toks[pos]
        pos += 1
        if t in "([":
            end = ")" if t == "(" else "]"
            out = []
            while toks[pos] != end:
                out.append(rd())
            pos += 1
            return out
        if t == "{":
            out = {}
            while toks[pos] != "}":
                k = rd()
                if toks[pos] == "}":
                    raise ValueError("odd map around %d" % pos)
                out[k] = rd()
            pos += 1
            return out
        if t == "#{":
            out = set()
            while toks[pos] != "}":
                out.add(rd())
            pos += 1
            return out
        if t.startswith('"'):
            return t[1:-1].encode().decode("unicode_escape")
        if t.startswith(":"):
            return t
        if t == "true":
            return True
        if t == "false":
            return False
        if t == "nil":
            return None
        try:
            return int(t)
        except ValueError:
            pass
        try:
            return float(t)
        except ValueError:
            return t

    v = rd()
    if pos != len(toks):
        raise ValueError("trailing EDN tokens at %d/%d" % (pos, len(toks)))
    return v


# ------------------------------------------------------------ run loading

def find_run(storage_root, run_arg=None):
    if run_arg:
        if os.path.isdir(run_arg):
            return run_arg
        cand = os.path.join(storage_root, run_arg)
        if os.path.isdir(cand):
            return cand
        raise SystemExit("no such run: %s" % run_arg)
    runs = sorted(
        d
        for d in os.listdir(storage_root)
        if re.match(r"^\d{8}T\d{6}\.\d+Z-[0-9a-f]+$", d)
    )
    if not runs:
        raise SystemExit("no giraffe runs under %s" % storage_root)
    return os.path.join(storage_root, runs[-1])


def item_path(item):
    pin = item.get(":source-pin") or {}
    if pin.get(":path"):
        return pin[":path"]
    return os.path.join("/home/joe/code", item.get(":repo", ""), item.get(":file", ""))


def item_title(path):
    try:
        with open(path, "r", errors="replace") as f:
            for line in f:
                if line.startswith("# "):
                    return line[2:].strip()
                if line.strip() and not line.startswith("#"):
                    break
    except OSError:
        pass
    return ""


def status_line_from_file(path):
    try:
        with open(path, "r", errors="replace") as f:
            m = re.search(r"^\s*[-*]?\s*\**Status\**\s*:?\s*(.+)$", f.read(), re.M)
            if m:
                return m.group(1).strip()
    except OSError:
        pass
    return ""


def _int(v):
    """The embedded EDN reader leaves numbers as '12,' strings; None stays None."""
    if v is None:
        return None
    try:
        return int(str(v).rstrip(",").strip())
    except ValueError:
        return None


def primary_checkout(path, code_root="/home/joe/code"):
    """True when PATH lives in a primary checkout directly under CODE_ROOT
    (its .git is a directory). A git worktree (.git is a file), e.g.
    ~/code/futon2-retraction-p1, holds stale copies of items: on 2026-11-07
    Giraffe's input still listed 132 T-repair-occ tickets from that worktree
    after REPAIR-MOVE-1 (0b1eb9bd4) had moved them out of futon2's tickets.
    The WM's own loader reads primary checkouts only."""
    rel = os.path.relpath(path, code_root)
    if rel.startswith(".."):
        return False
    repo = rel.split(os.sep)[0]
    return os.path.isdir(os.path.join(code_root, repo, ".git"))


def load_run(run_dir):
    """Return (items, readiness): items is a list of dicts with id, kind,
    path, own_status, live; readiness maps id -> {status, reason}."""
    with open(os.path.join(run_dir, "input.edn")) as f:
        data = parse_edn(f.read())
    readiness = {}
    pass_path = os.path.join(run_dir, "pass.edn")
    if os.path.exists(pass_path):
        with open(pass_path) as f:
            p = parse_edn(f.read())
        for k, v in (p.get(":readiness") or {}).items():
            readiness[k[1:]] = {
                "status": (v.get(":status") or "")[1:],
                "reason": (v.get(":reason") or "")[1:],
            }
    for row in (p.get(":order") or []) if os.path.exists(pass_path) else []:
        rid = (row.get(":id") or "").lstrip(":")
        if rid in readiness:
            readiness[rid].update({
                "rank": _int(row.get(":rank")),
                "unblocks": _int(row.get(":unblocks")),
                "cone": _int(row.get(":cone")),
                "order_reason": (row.get(":reason") or "").lstrip(":"),
            })
    items = []
    for raw in data[":items"]:
        iid = raw[":id"][1:]
        prefix = iid[0]
        if prefix not in KINDS:
            continue
        path = item_path(raw)
        if not primary_checkout(path) or not os.path.exists(path):
            continue
        own = raw.get(":declared-status") or status_line_from_file(path)
        items.append(
            {
                "id": iid,
                "kind": KINDS[prefix],
                "path": path,
                "own_status": own,
                "state_group": (raw.get(":state-group") or "")[1:],
                "readiness": readiness.get(iid, {"status": "unknown", "reason": ""}),
            }
        )
    return items


# --------------------------------------------------------------- mapping

def item_labels(item, census_row):
    """Labels for one item.  Pure: item dict + census row (or None)."""
    labels = {item["kind"]} if item.get("kind") else set()
    for way in re.split(r"[,;|\s]+", item.get("way") or ""):
        if way and way not in ("none", "way"):
            labels.add("way: " + way)
    if item["readiness"]["status"] == "ready":
        labels.add("giraffe-ready")
    row = census_row or {}
    if row:
        if row.get("gated") == "1":
            labels.add("gated-on-joe")
        if row.get("prose_only") == "1" and row.get("gate_line") == "0":
            labels.add("prose-only-wait")
        # Missions close via checkboxes in their own file, so an absent
        # close condition section is only a defect for tickets/excursions.
        if (
            row.get("close_cond") == "no-section"
            and item["kind"] in ("ticket", "excursion")
        ):
            labels.add("close-unstated")
        # "Unread" means the file HAS the field and the WM's summary reader
        # did not get it (census C3, a reader defect) -- not merely absent.
        # own_status catches the parenthetical "**Status (triaged ...):**"
        # form that the census's status_in_file detector also misses.
        status_present = row.get("status_in_file") == "1" or bool(
            (row.get("own_status") or "").strip())
        if (row.get("hdr_status") == "" and status_present) or (
                row.get("hdr_date") == "" and row.get("date_in_file") == "1"):
            labels.add("header-unread")
        # timebox-absent is not exported: 360 of 368 live items lacked a
        # timebox on 2026-11-08, so the label told the reader nothing.
    return labels


def item_state_name(item):
    if item.get("state_name"):
        return item["state_name"]
    return "Ready" if item["readiness"]["status"] == "ready" else "Not ready"


def item_payload(item, labels):
    """Pure: item -> Plane work item payload (sans state/label ids)."""
    bits = [
        "<p><b>Path:</b> <code>%s</code></p>" % html.escape(item["path"]),
        "<p><b>Giraffe status:</b> :%s" % html.escape(item["readiness"]["status"]),
    ]
    if item["readiness"]["reason"]:
        bits.append(" <i>(reason: :%s)</i>" % html.escape(item["readiness"]["reason"]))
    bits.append("</p>")
    bits.append(
        "<p><b>Status line (verbatim):</b><br/><pre>%s</pre></p>"
        % html.escape(item["own_status"])
    )
    if item.get("way"):
        bits.append("<p><b>Way of working:</b> %s</p>" % html.escape(item["way"]))
    r = item["readiness"]
    if r.get("rank") is not None:
        bits.append(
            "<p><b>Giraffe order:</b> rank %d of the ready set "
            "(unblocks %s, cone %s; :%s)</p>"
            % (r["rank"], r.get("unblocks"), r.get("cone"),
               html.escape(r.get("order_reason") or ""))
        )
    title = item_title(item["path"])
    # Giraffe headings usually repeat the id; never render it twice.
    if title.startswith(item["id"]):
        name = title.strip()
    else:
        name = (item["id"] + " " + title).strip()
    # Giraffe's own order, visible in every Plane view: "#12 M-foo ...".
    # Plane's priority field (urgent/high/medium/low/none) is left at none:
    # Giraffe has no coarse priority, and inventing buckets would misstate it.
    # sort_order makes Plane's "Manual" ordering follow the rank.
    rank = r.get("rank")
    if rank is not None:
        name = "#%d %s" % (rank, name)
    return {
        "name": name,
        "sort_order": float(rank * 1000) if rank is not None else 1e9,
        "description_html": "".join(bits),
        "external_source": EXTERNAL_SOURCE,
        "external_id": item["id"],
        "state_name": item_state_name(item),
        "label_names": sorted(labels),
    }


def load_placements(a1_path=DEFAULT_A1, early_glob=DEFAULT_EARLY):
    """id -> {"wing", "way"} from the 2026-11-02 placement readings: the
    re-read A1 rows first, the earlier placement sheets for the rest.  A row
    whose wing is blank or 'unsure' counts as unplaced."""
    import glob
    out = {}
    for f in sorted(glob.glob(early_glob)):
        with open(f) as fh:
            for r in csv.DictReader(fh, delimiter="\t"):
                if r.get("id"):
                    out[r["id"]] = {"wing": (r.get("wing") or "").strip(), "way": ""}
    if a1_path and os.path.exists(a1_path):
        with open(a1_path) as fh:
            for r in csv.DictReader(fh, delimiter="\t"):
                if not r.get("id"):
                    continue
                prev = out.get(r["id"], {})
                wing = (r.get("wing") or "").strip() or prev.get("wing", "")
                way = (r.get("way") or "").strip()
                out[r["id"]] = {"wing": wing,
                                "way": "" if way in ("", "none", "way") else way}
    return out


def load_trunks(path=DEFAULT_TRUNKS):
    """id -> {"trunk", "harm_line"} from the trunk sheet
    (notes/cascade-live-outline/build_trunks.py).  Missing file -> {}."""
    out = {}
    if path and os.path.exists(path):
        with open(path) as fh:
            for r in csv.DictReader(fh, delimiter="\t"):
                if r.get("id"):
                    out[r["id"]] = {"trunk": (r.get("trunk") or "").strip(),
                                    "harm_line": (r.get("harm_line") or "").strip()}
    return out


def trunk_key(row):
    t = (row or {}).get("trunk", "")
    return t if t in TRUNKS else "notrunk"


def wing_label(placement):
    k = wing_key(placement)
    if k in ("A", "B", "C", "D", "V"):
        return "wing %s: %s" % (k, WINGS[k][0])
    return "wing: " + k


def wing_key(placement):
    w = (placement or {}).get("wing", "")
    return w if w in WINGS else "unplaced"


def load_repairs(root=DEFAULT_REPAIRS):
    """The WM's repair records (REPAIR-MOVE-1, 0b1eb9bd4: records, not
    tickets, never selectable).  Shown in Plane as one project."""
    items = []
    if not os.path.isdir(root):
        return items
    for name in sorted(os.listdir(root)):
        if not (name.startswith("T-repair") and name.endswith(".md")):
            continue
        path = os.path.join(root, name)
        own = status_line_from_file(path)
        done = bool(re.match(r"\W*(DONE|CLOSED|COMPLETE)", own, re.I))
        items.append({"id": name[:-3], "kind": "repair", "path": path,
                      "own_status": own, "state_group": "",
                      "state_name": "Done" if done else "Open",
                      "readiness": {"status": "not-selectable",
                                    "reason": "repair record"}})
    return items


def load_census(path):
    if not path:
        return {}
    with open(path) as f:
        return {r["id"]: r for r in csv.DictReader(f, delimiter="\t")}


# ----------------------------------------------------------- Plane client

class Plane:
    # API_KEY_RATE_LIMIT is 60/minute (~/plane/variables.env:89): pace every
    # request at just over one per second, and on 429 sleep until the window
    # resets rather than burning retries.
    MIN_INTERVAL = 1.05

    def __init__(self, base, token, dry=False):
        self.base = base.rstrip("/")
        self.token = token
        self.dry = dry
        self._last = 0.0

    def req(self, method, path, body=None):
        url = self.base + path
        data = json.dumps(body).encode() if body is not None else None
        for attempt in range(10):
            wait = self._last + self.MIN_INTERVAL - time.monotonic()
            if wait > 0:
                time.sleep(wait)
            r = urllib.request.Request(url, data=data, method=method)
            r.add_header("X-API-Key", self.token)
            if data:
                r.add_header("Content-Type", "application/json")
            try:
                with urllib.request.urlopen(r, timeout=60) as resp:
                    self._last = time.monotonic()
                    return resp.status, json.loads(resp.read() or b"null")
            except urllib.error.HTTPError as e:
                self._last = time.monotonic()
                if e.code == 429:
                    time.sleep(30)  # window is a minute; 30s is safely past half
                    continue
                body_txt = e.read()
                try:
                    return e.code, json.loads(body_txt or b"null")
                except ValueError:
                    return e.code, {"raw": body_txt.decode("utf-8", "replace")}
            except urllib.error.URLError:
                self._last = time.monotonic()
                if attempt == 9:
                    raise
                time.sleep(2 ** attempt)
        raise RuntimeError("rate-limited after retries: %s %s" % (method, path))

    def get_all(self, path):
        out = []
        cursor = ""
        while True:
            q = "?per_page=100" + (("&cursor=" + urllib.parse.quote(cursor)) if cursor else "")
            st, body = self.req("GET", path + q)
            if st != 200:
                raise RuntimeError("GET %s -> %s %s" % (path, st, body))
            out.extend(body.get("results", []))
            if not body.get("next_page_results"):
                return out
            cursor = body["next_cursor"]


# ------------------------------------------------------------------ export

def ensure_project(plane, name, identifier, projects):
    for p in projects:
        if p.get("identifier") == identifier or p.get("name") == name:
            return p["id"]
    st, body = plane.req(
        "POST",
        "/workspaces/%s/projects/" % WORKSPACE,
        {"name": name, "identifier": identifier},
    )
    if st not in (200, 201):
        raise RuntimeError("create project %s -> %s %s" % (name, st, body))
    return body["id"]


def label_description(name):
    if name in KINDS.values() or name == "repair":
        return "item kind"
    if name.startswith("way: "):
        return "way of working (A1 placement reading, 2026-11-02)"
    if name == "giraffe-ready":
        return "Giraffe readiness :ready"
    return "Giraffe census check"


def ensure_by_name(plane, path, name, spec, existing):
    for s in existing:
        if s.get("name") == name:
            return s["id"]
    st, body = plane.req("POST", path, dict(spec, name=name))
    if st not in (200, 201):
        raise RuntimeError("create %s %s -> %s %s" % (path, name, st, body))
    return body["id"]


def sync_project(plane, project_id, payloads):
    items_path = "/workspaces/%s/projects/%s/work-items/" % (WORKSPACE, project_id)
    existing = plane.get_all(items_path)
    by_ext = {i.get("external_id"): i for i in existing if i.get("external_source") == EXTERNAL_SOURCE}
    stats = {"created": 0, "updated": 0, "unchanged": 0}
    for n, p in enumerate(payloads, 1):
        if n % 100 == 0:
            print("  ...%d/%d" % (n, len(payloads)), flush=True)
        cur = by_ext.get(p["external_id"])
        if cur is None:
            st, body = plane.req("POST", items_path, p["body"])
            if st == 409:  # raced/duplicate: fall through to update by id
                cur = by_ext.get(p["external_id"]) or {"id": body.get("id")}
            elif st != 201:
                raise RuntimeError("create %s -> %s %s" % (p["external_id"], st, body))
            else:
                stats["created"] += 1
                continue
        if (
            cur.get("name") == p["body"]["name"]
            and cur.get("state") == p["body"]["state"]
            and set(cur.get("labels") or []) == set(p["body"]["labels"])
        ):
            stats["unchanged"] += 1
            continue
        st, body = plane.req(
            "PATCH", items_path + str(cur["id"]) + "/", p["body"]
        )
        if st not in (200, 201):
            raise RuntimeError("update %s -> %s %s" % (p["external_id"], st, body))
        stats["updated"] += 1
    # One-way mirror: an item Giraffe no longer exports (finished, moved, or
    # read from a worktree) is deleted from Plane, so Plane never shows more
    # than the source does.
    wanted = {p["external_id"] for p in payloads}
    stats["deleted"] = 0
    for ext, cur in by_ext.items():
        if ext not in wanted:
            st, body = plane.req("DELETE", items_path + str(cur["id"]) + "/", None)
            if st not in (200, 204):
                raise RuntimeError("delete %s -> %s %s" % (ext, st, body))
            stats["deleted"] += 1
    return stats


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--storage-root", default=DEFAULT_STORAGE_ROOT)
    ap.add_argument("--run", help="run dir or stamp dir-name under the storage root")
    ap.add_argument("--census", nargs="?", const=DEFAULT_CENSUS, default=None)
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--a1", default=DEFAULT_A1)
    ap.add_argument("--repairs", default=DEFAULT_REPAIRS)
    ap.add_argument("--trunks", default=DEFAULT_TRUNKS)
    ap.add_argument("--api-base", default=API_BASE)
    args = ap.parse_args(argv)

    run_dir = find_run(args.storage_root, args.run)
    print("run:", run_dir)
    items = load_run(run_dir)
    census = load_census(args.census)
    if args.census:
        print("census:", args.census, "(%d rows)" % len(census))

    live = [i for i in items if i["state_group"] not in TERMINAL_STATE_GROUPS]
    print(
        "items: %d total, %d live (exported), %d terminal (skipped)"
        % (len(items), len(live), len(items) - len(live))
    )

    # Build payloads grouped by project: one project per benefit trunk, plus
    # the repair records.  Wing and harm line are labels.
    placements = load_placements(args.a1)
    trunks = load_trunks(args.trunks)
    by_project = {}
    for it in live:
        pl_ = placements.get(it["id"])
        it["way"] = (pl_ or {}).get("way", "")
        row = census.get(it["id"])
        labels = item_labels(it, row)
        labels.add(wing_label(pl_))
        tr = trunks.get(it["id"])
        if (tr or {}).get("harm_line"):
            labels.add("harm " + tr["harm_line"])
        pl = item_payload(it, labels)
        by_project.setdefault(TRUNKS[trunk_key(tr)][0], []).append(pl)
    repairs = load_repairs(args.repairs)
    by_project[REPAIRS[0]] = [item_payload(it, item_labels(it, None))
                              for it in repairs]
    project_specs = list(TRUNKS.values()) + [REPAIRS]

    # Summaries for dry-run and final report.
    print("counts per project/state/label:")
    for pname, pls in sorted(by_project.items()):
        per_state = {}
        per_label = {}
        for p in pls:
            per_state[p["state_name"]] = per_state.get(p["state_name"], 0) + 1
            for ln in p["label_names"]:
                per_label[ln] = per_label.get(ln, 0) + 1
        print(
            "  %s: %d items; states %s; labels %s"
            % (pname, len(pls), per_state, per_label or "{}")
        )

    if args.dry_run:
        print("first 5 payloads:")
        shown = 0
        for pname, pls in sorted(by_project.items()):
            for p in pls:
                if shown >= 5:
                    return 0
                print(json.dumps(p, indent=2))
                shown += 1
        return 0

    with open(TOKEN_PATH) as f:
        token = f.read().strip()
    plane = Plane(args.api_base, token)

    projects = plane.get_all("/workspaces/%s/projects/" % WORKSPACE)
    totals = {}
    for pname, ident in project_specs:
        pls = by_project.get(pname, [])
        pid = ensure_project(plane, pname, ident, projects)
        states_path = "/workspaces/%s/projects/%s/states/" % (WORKSPACE, pid)
        existing_states = plane.get_all(states_path)
        used_states = {p["state_name"] for p in pls} or {"Ready"}
        state_ids = {
            n: ensure_by_name(plane, states_path, n, spec, existing_states)
            for n, spec in STATE_SPECS.items() if n in used_states
        }
        labels_path = "/workspaces/%s/projects/%s/labels/" % (WORKSPACE, pid)
        existing_labels = plane.get_all(labels_path)
        all_label_names = sorted({ln for p in pls for ln in p["label_names"]})
        label_ids = {
            n: ensure_by_name(
                plane, labels_path, n,
                {"color": LABEL_COLORS.get(n, "#60646C"),
                 "description": label_description(n)},
                existing_labels,
            )
            for n in all_label_names
        }
        payloads = [
            {
                "external_id": p["external_id"],
                "body": {
                    "name": p["name"],
                    "sort_order": p["sort_order"],
                    "description_html": p["description_html"],
                    "external_source": p["external_source"],
                    "external_id": p["external_id"],
                    "state": state_ids[p["state_name"]],
                    "labels": [label_ids[ln] for ln in p["label_names"]],
                },
            }
            for p in pls
        ]
        stats = sync_project(plane, pid, payloads)
        totals[pname] = stats
        print("%s: created %d, updated %d, unchanged %d, deleted %d"
              % (pname, stats["created"], stats["updated"], stats["unchanged"],
                 stats["deleted"]))
    # Superseded projects (RETIRED_PROJECTS) are removed.  Plane is
    # a one-way mirror, so removing them loses nothing the files do not hold.
    for proj in plane.get_all("/workspaces/%s/projects/" % WORKSPACE):
        if proj.get("name") in RETIRED_PROJECTS:
            st, body = plane.req(
                "DELETE", "/workspaces/%s/projects/%s/" % (WORKSPACE, proj["id"]), None)
            print("retired project %s -> %s" % (proj["name"], st))
    tot = {k: sum(s[k] for s in totals.values()) for k in ("created", "updated", "unchanged")}
    print("TOTAL: created %d, updated %d, unchanged %d" % (tot["created"], tot["updated"], tot["unchanged"]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
