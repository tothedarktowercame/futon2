#!/usr/bin/env python3
"""Align the self-heal Lean model with the Clojure code as a hypergraph, and count coverage.

Inputs:
  * code sites: JSON from code_sites.clj (one row per outcome site: refusal, held,
    status map, absence term), each with file, line, function, reason, sha;
  * Lean entities: every constructor of every inductive in the Lean files, with the
    text of its docstring or trailing comment.

A hyperedge `lean-code/realizes` joins one Lean entity to the code sites it names.
Edges are derived only from what the Lean text says, by three rules, in order:
  keyword  the entity's text contains the site's reason keyword, backticked;
  line     the entity's text cites the site's line (`P:460`, `C:237`, `A:233`);
  name     the constructor name, camelCase to kebab, is the site's reason name.
Nothing is inferred beyond that. A site no rule reaches is uncovered; an entity no
rule reaches has no code endpoint. Both lists are the to-do list.

Usage: python3 -I align.py SITES.json OUT_DIR LEAN_FILE...
Writes OUT_DIR/hypergraph.json (Arxana hyperedge shape: hx/type, hx/endpoints, props)
and OUT_DIR/coverage.md."""
import json, re, sys
from pathlib import Path

TAG_OF_FILE = {"meta_self_heal_production.clj": "P", "meta_repair_coordinator.clj": "C",
               "full_loop_runtime.clj": "R", "meta_self_heal_adapters.clj": "A",
               "runner_service.clj": "S"}

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

def cited_lines(text):
  return {(t, int(n)) for t, n in re.findall(r"\b([PCRAS]):(\d+)", text)}

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
  for e in entities:
    kws, lines, nm = cited_keywords(e["text"]), cited_lines(e["text"]), kebab(e["name"])
    hits = {}
    for s in sites:
      r = s.get("reason") or ""
      full = r                      # cheshire writes ns/name
      short = r.split("/")[-1]
      basis = None
      status = s.get("status") or ""
      in_scope = e["inductive"] not in SCOPE or s["fn"] in SCOPE[e["inductive"]]
      if r and s["kind"] != "absent-term" and (full in kws or ("/" not in full and short in kws)):
        basis = "keyword"
      elif any(t == s["tag"] and s["line"] <= n <= (s.get("end-line") or s["line"]) for t, n in lines):
        # a line citation counts only when the cited site says what the text says
        basis = "line" if (full in kws or short in kws or status in kws) else "line-unconfirmed"
      elif r and short == nm and in_scope and (s["kind"] != "absent-term") == (e["inductive"] not in TERMS):
        basis = "name"
      if basis: hits.setdefault(basis, []).append(s["id"])
    for basis, ids in hits.items():
      edges.append({"hx/type": "lean-code/realizes", "hx/endpoints": [e["id"], *ids],
                    "props": {"basis": basis, "lean-line": e["line"]}})
  return edges

def main(sites_path, out_dir, *lean_files):
  rows = json.loads(Path(sites_path).read_text())
  # :defn rows anchor citations of a function's first line; they are not outcome sites.
  defn_lines = {(r["tag"], r["line"]) for r in rows if r["kind"] == "defn"}
  sites = [r for r in rows if r["kind"] != "defn"]
  entities = [e for f in lean_files for e in lean_entities(f)]
  edges = align(sites, entities)
  real = [ed for ed in edges if ed["props"]["basis"] != "line-unconfirmed"]
  covered_sites = {i for ed in real for i in ed["hx/endpoints"][1:]}
  covered_ents = {ed["hx/endpoints"][0] for ed in real}
  # line citations that name no site at HEAD: stale or wrong
  # a citation may name any line of a multi-line site (the :reason line of a map)
  site_lines = {(s["tag"], n) for s in sites for n in range(s["line"], (s.get("end-line") or s["line"]) + 1)} | defn_lines
  stale = sorted({(e["id"], f"{t}:{n}") for e in entities for t, n in cited_lines(e["text"])
                  if (t, n) not in site_lines})
  out = Path(out_dir); out.mkdir(parents=True, exist_ok=True)
  (out/"hypergraph.json").write_text(json.dumps({"sites": sites, "entities": entities, "hyperedges": edges}, indent=1))
  def frac(a, b): return f"{a}/{b} ({100*a/b:.0f}%)" if b else "0/0"
  by_kind = {}
  for s in sites: by_kind.setdefault(s["kind"], [0, 0]); by_kind[s["kind"]][1] += 1; by_kind[s["kind"]][0] += s["id"] in covered_sites
  by_ind = {}
  for e in entities: by_ind.setdefault(e["inductive"], [0, 0]); by_ind[e["inductive"]][1] += 1; by_ind[e["inductive"]][0] += e["id"] in covered_ents
  shas = sorted({(s["tag"], s["file"], s["sha"]) for s in sites})
  L = ["# Lean / Clojure alignment: self-heal route", "",
       "Generated by `align.py` from `code_sites.clj`. Do not edit by hand.", "",
       "Code read at: " + "; ".join(f"{t} = `{f}` @ `{h}`" for t, f, h in shas), "",
       "Lean: " + ", ".join(f"`{f}`" for f in lean_files), "",
       f"**Code sites realised by a Lean entity: {frac(len(covered_sites), len(sites))}.**", "",
       f"**Lean entities with a code endpoint: {frac(len(covered_ents), len(entities))}.**", "",
       "| site kind | covered |", "|---|---|"]
  L += [f"| {k} | {frac(*v)} |" for k, v in sorted(by_kind.items())]
  L += ["", "| Lean inductive | constructors with a code endpoint |", "|---|---|"]
  L += [f"| {k} | {frac(*v)} |" for k, v in sorted(by_ind.items())]
  L += ["", f"Edges by basis: " + ", ".join(f"{b} {sum(1 for e in edges if e['props']['basis']==b)}" for b in ("keyword", "line", "name", "line-unconfirmed")), ""]
  L += ["## Uncovered code sites", "", "| site | function | kind | status | reason |", "|---|---|---|---|---|"]
  L += [f"| {s['id']} | `{s['fn']}` | {s['kind']} | {s.get('status') or ''} | {s.get('reason') or ''} |"
        for s in sites if s["id"] not in covered_sites]
  L += ["", "## Lean entities with no code endpoint", ""]
  L += [f"- `{e['id'].split(':',1)[1]}` (line {e['line']})" for e in entities if e["id"] not in covered_ents]
  L += ["", "## Line citations in Lean that name no site at the code HEAD above", ""]
  L += [f"- `{i.split(':',1)[1]}` cites {c}" for i, c in stale] or ["- none"]
  (out/"coverage.md").write_text("\n".join(L) + "\n")
  print(f"sites {frac(len(covered_sites), len(sites))}; entities {frac(len(covered_ents), len(entities))}; edges {len(edges)}; stale citations {len(stale)}")

if __name__ == "__main__":
  main(*sys.argv[1:])
