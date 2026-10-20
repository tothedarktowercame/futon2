#!/usr/bin/env python3
"""Align the self-heal Lean model with the Clojure code as a hypergraph, and count coverage.

Inputs:
  * code sites: JSON from code_sites.clj (one row per outcome site: refusal, held,
    status map, absence term, call), each with file, line, function, reason, sha, and one
    :defn row per definition with its source text;
  * Lean entities: every constructor of every inductive in the Lean files, with the
    text of its docstring or trailing comment.

A hyperedge `lean-code/realizes` joins one Lean entity to the code sites it names.
Edges are derived only from what the Lean text says, by these rules, in order:
  call     an anchor `(P fn ns/callee)` names a call inside fn;
  keyword  the entity's text contains the site's reason keyword, scoped by anchors,
           or an anchor names a status map's status keyword;
  name     the constructor name, camelCase to kebab, is the site's reason name.
Nothing is inferred beyond that. A site no rule reaches is uncovered; an entity no
rule reaches has no code endpoint. Both lists are the to-do list.

The Lean text cites code by anchor, never by line number: `(P fn)` names a function,
`(P fn :keyword)` an outcome or keyword inside it, `(P fn ns/callee)` a call inside it, and
`(P fn "text")` a fragment of its source. Every anchor anywhere in a Lean file must
resolve against the code read: the function exists in that file, and each keyword,
callee and quoted fragment occurs in its source. An anchor that does not resolve is
an error to fix, because a code change that moves lines leaves anchors alone. A line
citation (`P:460`, `C line 232`) is reported so it can be replaced (Joe, 2026-10-20).

Usage: python3 -I align.py SITES.json OUT_DIR LEAN_FILE...
Writes OUT_DIR/hypergraph.json (Arxana hyperedge shape: hx/type, hx/endpoints, props)
and OUT_DIR/coverage.md."""
import json, re, sys
from pathlib import Path

TAG_OF_FILE = {"meta_self_heal_production.clj": "P", "meta_repair_coordinator.clj": "C",
               "full_loop_runtime.clj": "R", "meta_self_heal_adapters.clj": "A",
               "runner_service.clj": "S", "meta_terminal_injury.clj": "T",
               "meta_injury_ledger.clj": "L", "meta_injury_store.clj": "I", "full_loop_runner.clj": "U"}

def kebab(name):
  return re.sub(r"(?<!^)(?=[A-Z])", "-", name).lower()

def lean_entities(path):
  """Constructors of each inductive, with doc text (preceding /-- -/ or trailing --)."""
  text = Path(path).read_text().splitlines()
  out, ind, pending = [], None, None
  i = 0
  while i < len(text):
    line = text[i]
    m = re.match(r"^(inductive|structure) (\w+)", line)
    if m:
      ind = m.group(2) if m.group(1) == "inductive" else None; pending = None; i += 1; continue
    if ind and re.match(r"^\S", line) and not line.startswith("inductive"):
      ind = None
    if ind:
      s = line.strip()
      if s.startswith("/--"):
        buf = [s]
        while "-/" not in text[i] and i + 1 < len(text): i += 1; buf.append(text[i].strip())
        pending = " ".join(buf)
      elif s.startswith("|"):
        trailing = s.split("--", 1)[1] if "--" in s else ""
        names = re.findall(r"\|\s*(\w+)", s.split("--", 1)[0])
        for n in names:
          out.append({"id": f"lean:{Path(path).stem}.{ind}.{n}", "inductive": ind, "name": n,
                      "file": str(path), "line": i + 1,
                      "text": ((pending or "") + " " + trailing).strip() if len(names) == 1 else trailing})
        pending = None
    i += 1
  return out

def cited_keywords(text):
  return {k.lstrip(":") for k in re.findall(r"`(:[A-Za-z0-9./!?-]+)`", text)} | \
         {k.lstrip(":") for k in re.findall(r"(?<![\w`])(:[a-z][A-Za-z0-9./!?-]+)", text)}

# `P:460`, `C line 232`, `full_loop_runtime.clj:475`, and the older `L36-49` (two or
# more digits, since `L1` and `L2` name units of the logic model, not lines)
LINE_CITE = re.compile(r"\b[PCRASTLIU](?::| lines? )\d+(?:-\d+)?|\.clj:\d+|\bL\d\d+(?:-\d+)?\b")

def cited_callees(text):
  return set(re.findall(r"(?<![:\w.-])([A-Za-z][A-Za-z0-9_.!?-]*/[A-Za-z][A-Za-z0-9_.!?-]*)", text))

ANCHOR_FORM = re.compile(r"\((?:(not)\s+)?(P|C|R|A|S|T|L|I|U)\s+([a-z*][^\s()]*)\s*([^()]*)\)")
NEEDLE = re.compile(r'"([^"]*)"')

def anchor_forms(text):
  """Return (tag, function, keywords, callees, span, positive?, needles) for simple anchors.

  These are deliberately documentation anchors, not a parser for Lean.  Their
  contents have no nested parens in the contract grammar, so retaining the
  enclosing form lets a cited outcome keyword keep the function it names.
  `(not P function ...)` is an exclusion: it consumes its keywords as prose
  without claiming a code endpoint. A quoted fragment is matched against the
  function's source only; keywords and callees inside it are not cited.
  """
  out = []
  for m in ANCHOR_FORM.finditer(text):
    rest = NEEDLE.sub(" ", m.group(4))
    out.append((m.group(2), m.group(3), cited_keywords(rest), cited_callees(rest),
                m.span(), not m.group(1), NEEDLE.findall(m.group(4))))
  return out

def outside_anchors(text, forms):
  """The text with every anchor form cut out: keywords here keep the old, unscoped reading."""
  ends = [0, *(form[4][1] for form in forms)]
  starts = [*(form[4][0] for form in forms), len(text)]
  return "".join(text[last:start] for last, start in zip(ends, starts))

# The code functions each Lean inductive models.  The name rule links a constructor
# to a site only inside this scope, so `CoordinatorRefusal.malformed` cannot reach
# `:work-order/malformed`.  This table is itself part of the alignment: it says which
# Lean vocabulary describes which code, and it is read, not guessed.
SCOPE = {
  "ReadinessTerm": {"production-readiness"},
  "RouteTerm": {"route!"},
  "Field": {"verify-work-order"},
  "HeldReason": {"coordinate", "held!"},
  "CoordinatorRefusal": {"coordinate", "invoke-once", "append-transition!"},
  "ClosingStatus": {"coordinate"},
}

# Lean vocabularies of absence terms; only these match `absent-term` sites, and they
# match nothing else.
TERMS = {"ReadinessTerm", "RouteTerm", "Field"}

def align(sites, entities):
  edges = []
  reasons_of = {}
  for s in sites:
    if s.get("reason") and s["kind"] != "call":
      reasons_of.setdefault((s["tag"], s["fn"]), set()).update({s["reason"], s["reason"].split("/")[-1]})
  for e in entities:
    text = e["text"]
    forms = anchor_forms(text)
    outside_kws, nm = cited_keywords(outside_anchors(text, forms)), kebab(e["name"])
    negative_kws = set().union(*(form[2] for form in forms if not form[5])) if forms else set()
    outside_kws -= negative_kws
    scoped_kws = {}
    scoped_callees = {}
    scoped_statuses = {}
    anchored_fns = {}
    positive_forms = [form for form in forms if form[5]]
    for tag, fn, form_kws, form_callees, _span, _positive, _needles in positive_forms:
      anchored_fns.setdefault(tag, set()).add(fn)
      for kw in form_kws:
        scoped_kws.setdefault(kw, set()).add((tag, fn))
      # an anchor that names one of fn's reasons is about that reasoned site, so its
      # `:status` keyword does not also reach fn's reasonless maps
      if not form_kws & reasons_of.get((tag, fn), set()):
        for kw in form_kws:
          scoped_statuses.setdefault(kw, set()).add((tag, fn))
      for callee in form_callees:
        scoped_callees.setdefault(callee, set()).add((tag, fn))
    hits = {}
    for s in sites:
      r = s.get("reason") or ""
      full = r                      # cheshire writes ns/name
      short = r.split("/")[-1]
      basis = None
      status = s.get("status") or ""
      in_scope = e["inductive"] not in SCOPE or s["fn"] in SCOPE[e["inductive"]]
      cited = {full} if "/" in full else {full, short}
      scoped = set().union(*(scoped_kws.get(k, set()) for k in cited)) if cited else set()
      # A status map with no :reason is reached by its status keyword, and only inside an
      # anchor naming its function and none of that function's reasons:
      # `(C coordinate-queued-retry :awaiting-retry)`. A bare `:discharged` in prose does
      # not reach every map with that status, a map with a reason is reached by its reason
      # only, and `(R run-opportunity! {:status :not-required :reason X})` does not reach
      # the function's reasonless `{:status :not-required}`. This replaces the line rule,
      # which was the only way a reasonless status map got an edge (rev 20).
      if s["kind"] == "status-map" and not r and status and status != "computed":
        scoped = scoped | scoped_statuses.get(status, set())
      # Once a docstring names a positive anchor, unanchored keywords are
      # explanatory prose.  A docstring with no anchor preserves the legacy
      # bare-keyword reading; negative forms have already been removed above.
      # A bare keyword in a docstring that has a positive anchor reaches only the
      # anchored functions of its tag (rev 12 lists a function's reasons after a
      # bare `(P fn)` anchor); with no anchor it keeps the legacy whole-file reading.
      unscoped = (any(k in outside_kws for k in cited)
                  and (not positive_forms or s["fn"] in anchored_fns.get(s["tag"], set())))
      anchor_name_scope = anchored_fns.get(s["tag"])
      if s["kind"] == "call":
        # a call site is evidence for an anchor that names its callee; the keyword
        # and name rules do not reach it (review fix, M17T)
        if (s["tag"], s["fn"]) in scoped_callees.get(r, set()):
          basis = "call"
      elif s["kind"] != "absent-term" and ((r and unscoped) or (s["tag"], s["fn"]) in scoped):
        basis = "keyword"
      elif r and short == nm and (s["fn"] in anchor_name_scope if anchor_name_scope else in_scope) and (s["kind"] != "absent-term") == (e["inductive"] not in TERMS):
        basis = "name"
      if basis: hits.setdefault(basis, []).append(s["id"])
    for basis, ids in hits.items():
      edges.append({"hx/type": "lean-code/realizes", "hx/endpoints": [e["id"], *ids],
                    "props": {"basis": basis, "lean-line": e["line"]}})
  return edges

def unresolved_anchors(rows, lean_files):
  """Every anchor in the Lean files, positive or `(not ...)`, that names no function in
  its file, or a keyword, callee or quoted fragment that function does not contain.
  Returns (file stem, Lean line, anchor text, what failed)."""
  source = {}
  for r in rows:
    if r["kind"] == "defn":
      source[(r["tag"], r["fn"])] = source.get((r["tag"], r["fn"]), "") + "\n" + r.get("source", "")
  outcome = {}
  for r in rows:
    if r["kind"] not in ("defn", "call"):
      for v in (r.get("reason"), r.get("status")):
        if v: outcome.setdefault((r["tag"], r["fn"]), set()).update({v, v.split("/")[-1]})
  def has_keyword(tag, fn, kw):
    if kw in outcome.get((tag, fn), set()): return True
    src = source[(tag, fn)]
    if re.search(r":" + re.escape(kw) + r"(?![\w./!?*+<>=-])", src): return True
    # an auto-resolved keyword is written `::name` in the source
    return "/" in kw and re.search(r"::" + re.escape(kw.split("/")[-1]) + r"(?![\w./!?*+<>=-])", src) is not None
  out = []
  for path in lean_files:
    text = Path(path).read_text()
    for tag, fn, kws, callees, (start, end), _positive, needles in anchor_forms(text):
      line, anchor = text.count("\n", 0, start) + 1, " ".join(text[start:end].split())
      if (tag, fn) not in source:
        out.append((Path(path).stem, line, anchor, f"no function `{fn}` in {tag}")); continue
      src = source[(tag, fn)]
      missing = ([f":{k}" for k in sorted(kws) if not has_keyword(tag, fn, k)]
                 + [c for c in sorted(callees) if c not in src]
                 + [f'"{n}"' for n in needles if n not in src])
      if missing:
        out.append((Path(path).stem, line, anchor, f"not in `{fn}`: " + ", ".join(missing)))
  return out

def line_citations(lean_files):
  """Line-number citations left in the Lean files: (file stem, Lean line, citation)."""
  out = []
  for path in lean_files:
    for i, line in enumerate(Path(path).read_text().splitlines(), 1):
      out += [(Path(path).stem, i, m.group(0)) for m in LINE_CITE.finditer(line)]
  return out

def main(sites_path, out_dir, *lean_files):
  rows = json.loads(Path(sites_path).read_text())
  # :defn rows carry each function's source for anchor resolution; they are not outcome sites.
  sites = [r for r in rows if r["kind"] != "defn"]
  # :call rows are evidence for an anchor that names the callee, not outcome
  # sites the model owes an endpoint: they stay out of the totals, the
  # uncovered table and the stale-citation check (review fix, M17T).
  owed = [s for s in sites if s["kind"] != "call"]
  calls = [s for s in sites if s["kind"] == "call"]
  entities = [e for f in lean_files for e in lean_entities(f)]
  edges = align(sites, entities)
  covered_sites = {i for ed in edges for i in ed["hx/endpoints"][1:]}
  covered_ents = {ed["hx/endpoints"][0] for ed in edges}
  unresolved = unresolved_anchors(rows, lean_files)
  line_cites = line_citations(lean_files)
  out = Path(out_dir); out.mkdir(parents=True, exist_ok=True)
  (out/"hypergraph.json").write_text(json.dumps({"sites": sites, "entities": entities, "hyperedges": edges}, indent=1))
  def frac(a, b): return f"{a}/{b} ({100*a/b:.0f}%)" if b else "0/0"
  by_kind = {}
  for s in owed: by_kind.setdefault(s["kind"], [0, 0]); by_kind[s["kind"]][1] += 1; by_kind[s["kind"]][0] += s["id"] in covered_sites
  by_ind = {}
  for e in entities: by_ind.setdefault(e["inductive"], [0, 0]); by_ind[e["inductive"]][1] += 1; by_ind[e["inductive"]][0] += e["id"] in covered_ents
  shas = sorted({(r["tag"], r["file"], r["sha"]) for r in rows})
  L = ["# Lean / Clojure alignment: self-heal route", "",
       "Generated by `align.py` from `code_sites.clj`. Do not edit by hand.", "",
       "Code read at: " + "; ".join(f"{t} = `{f}` @ `{h}`" for t, f, h in shas), "",
       "Lean: " + ", ".join(f"`{f}`" for f in lean_files), "",
       f"**Code sites realised by a Lean entity: {frac(sum(s['id'] in covered_sites for s in owed), len(owed))}.**", "",
       f"**Lean entities with a code endpoint: {frac(len(covered_ents), len(entities))}.**", "",
       "| site kind | covered |", "|---|---|"]
  L += [f"| {k} | {frac(*v)} |" for k, v in sorted(by_kind.items())]
  L += ["", f"Call sites named by an anchor: {sum(s['id'] in covered_sites for s in calls)} of {len(calls)} emitted (evidence, not owed an endpoint)."]
  L += ["", "| Lean inductive | constructors with a code endpoint |", "|---|---|"]
  L += [f"| {k} | {frac(*v)} |" for k, v in sorted(by_ind.items())]
  L += ["", f"Edges by basis: " + ", ".join(f"{b} {sum(1 for e in edges if e['props']['basis']==b)}" for b in ("keyword", "call", "name")), ""]
  L += ["## Uncovered code sites", "", "| site | function | kind | status | reason |", "|---|---|---|---|---|"]
  L += [f"| {s['id']} | `{s['fn']}` | {s['kind']} | {s.get('status') or ''} | {s.get('reason') or ''} |"
        for s in owed if s["id"] not in covered_sites]
  L += ["", "## Lean entities with no code endpoint", ""]
  L += [f"- `{e['id'].split(':',1)[1]}` (line {e['line']})" for e in entities if e["id"] not in covered_ents]
  L += ["", "## Anchors that do not resolve against the code above", ""]
  L += [f"- `{f}` line {n}: `{a}`: {why}" for f, n, a, why in unresolved] or ["- none"]
  L += ["", "## Line-number citations left in the Lean files", ""]
  L += [f"- `{f}` line {n}: {c}" for f, n, c in line_cites] or ["- none"]
  (out/"coverage.md").write_text("\n".join(L) + "\n")
  print(f"sites {frac(sum(s['id'] in covered_sites for s in owed), len(owed))}; calls {sum(s['id'] in covered_sites for s in calls)}/{len(calls)}; entities {frac(len(covered_ents), len(entities))}; edges {len(edges)}; unresolved anchors {len(unresolved)}; line citations {len(line_cites)}")

if __name__ == "__main__":
  main(*sys.argv[1:])
