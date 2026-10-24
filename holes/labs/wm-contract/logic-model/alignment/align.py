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
import argparse, hashlib, json, re, subprocess, sys
from pathlib import Path

TAG_OF_FILE = {"meta_self_heal_production.clj": "P", "meta_repair_coordinator.clj": "C",
               "full_loop_runtime.clj": "R", "meta_self_heal_adapters.clj": "A",
               "runner_service.clj": "S", "meta_terminal_injury.clj": "T",
               "meta_injury_ledger.clj": "L", "meta_injury_store.clj": "I", "full_loop_runner.clj": "U",
               "checked_problem_map.clj": "K"}

class AlignmentError(ValueError):
  """A checked named-declaration invocation cannot produce an artifact."""

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

def pinned_lean_source(spec):
  """Read REPO@REV:PATH through git, with no shell interpolation.

  The returned module is repository-relative and intentionally distinct from a
  Lean namespace.  Named declaration mode is pinned-only so a required symbol
  cannot quietly describe whatever happens to be in a worktree.
  """
  try:
    repo_rev, rel = spec.rsplit(":", 1)
    repo, revision = repo_rev.rsplit("@", 1)
  except ValueError as exc:
    raise AlignmentError("pinned Lean source must be REPO@REV:PATH") from exc
  if not repo or not revision or not rel.endswith(".lean"):
    raise AlignmentError("pinned Lean source needs nonempty repo, revision, and .lean path")
  resolved = subprocess.run(["git", "-C", repo, "rev-parse", "--verify", f"{revision}^{{commit}}"],
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
  if resolved.returncode:
    raise AlignmentError(f"cannot resolve immutable Lean commit {repo}@{revision}: " +
                         resolved.stderr.decode("utf-8", "replace").strip())
  revision = resolved.stdout.decode("ascii", "strict").strip()
  if not re.fullmatch(r"[0-9a-f]{40}", revision):
    raise AlignmentError(f"resolved Lean revision is not a full commit: {revision!r}")
  result = subprocess.run(["git", "-C", repo, "show", f"{revision}:{rel}"],
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
  if result.returncode:
    raise AlignmentError(f"cannot read pinned Lean source {spec}: " +
                         result.stderr.decode("utf-8", "replace").strip())
  try:
    text = result.stdout.decode("utf-8")
  except UnicodeDecodeError as exc:
    raise AlignmentError(f"pinned Lean source is not UTF-8: {spec}") from exc
  return {"repo": repo, "revision": revision, "path": rel,
          "module": rel[:-5], "text": text,
          "sha256": hashlib.sha256(result.stdout).hexdigest()}

def _masked_lean_line(line, block_depth, in_string):
  """Mask strings and nested comments for the deliberately small declaration parser."""
  out, i = [], 0
  while i < len(line):
    pair = line[i:i + 2]
    if block_depth:
      if pair == "/-": block_depth += 1; i += 2; out.extend("  "); continue
      if pair == "-/": block_depth -= 1; i += 2; out.extend("  "); continue
      out.append(" "); i += 1; continue
    if not in_string and pair == "/-":
      block_depth += 1; i += 2; out.extend("  "); continue
    if not in_string and pair == "--":
      out.extend(" " * (len(line) - i)); break
    ch = line[i]
    if in_string:
      out.append(" ")
      if ch == "\\" and i + 1 < len(line):
        i += 1; out.append(" ")
      elif ch == '"':
        in_string = False
    elif ch == '"':
      in_string = True; out.append(" ")
    else:
      out.append(ch)
    i += 1
  return "".join(out), block_depth, in_string

def _doc_comment(lines, start):
  """Return (doc-text, final-line-index, trailing-source) for nested `/--` comments."""
  depth, i, column, chunks = 1, start, lines[start].find("/--") + 3, [lines[start]]
  while i < len(lines):
    line = lines[i]
    while column < len(line):
      pair = line[column:column + 2]
      if pair == "/-":
        depth += 1; column += 2; continue
      if pair == "-/":
        depth -= 1; column += 2
        if depth == 0:
          return "\n".join(chunks), i, line[column:]
        continue
      column += 1
    i += 1
    if i < len(lines): chunks.append(lines[i])
    column = 0
  raise AlignmentError(f"unterminated documentation comment at line {start + 1}")

DECLARATION = re.compile(r"^(?P<prefix>(?:(?:private|protected|noncomputable|unsafe|partial)\s+)*)"
                         r"(?P<kind>def|theorem)\s+(?P<name>[A-Za-z_][A-Za-z0-9_'.]*)\b")
NAMESPACE = re.compile(r"^namespace\s+([A-Za-z_][A-Za-z0-9_'.]*)\b")
SECTION = re.compile(r"^section(?:\s+[A-Za-z_][A-Za-z0-9_'.]*)?\s*$")
END = re.compile(r"^end(?:\s+[A-Za-z_][A-Za-z0-9_'.]*)?\s*(?:--.*)?$")

def named_lean_entities(source):
  """Extract the reviewed fragment: named top-level def/theorem declarations.

  This is intentionally not a Lean parser or elaborator.  It tracks namespace
  and section/end scopes, accepts dotted declaration names and `noncomputable
  def`, masks comments/strings, and refuses to export private declarations as
  public identities.  Unsupported private declarations are returned as issues
  so a required identity fails rather than becoming an invented public symbol.
  """
  out, issues, scopes, pending_doc, block_depth, in_string = [], [], [], None, 0, False
  lines = source["text"].splitlines()
  i = 0
  while i < len(lines):
    raw, stripped = lines[i], lines[i].strip()
    if not in_string and not block_depth and stripped.startswith("/--"):
      doc, end_line, trailing = _doc_comment(lines, i)
      if trailing.strip():
        issues.append({"line": end_line + 1, "kind": "unsupported",
                       "reason": "documentation comment with trailing source"})
      pending_doc = doc
      i = end_line + 1; continue
    line, block_depth, in_string = _masked_lean_line(raw, block_depth, in_string)
    code = line.strip()
    ns = NAMESPACE.match(code)
    if ns:
      scopes.append(("namespace", ns.group(1))); pending_doc = None; i += 1; continue
    if SECTION.match(code):
      scopes.append(("section", None)); pending_doc = None; i += 1; continue
    end = END.match(code)
    if end:
      requested = code[3:].strip().split()[0] if code[3:].strip() else None
      if not scopes:
        issues.append({"line": i + 1, "kind": "unsupported", "reason": "unmatched end"})
      elif requested and scopes[-1][0] == "namespace" and scopes[-1][1] != requested:
        issues.append({"line": i + 1, "kind": "unsupported", "reason": "named end does not match scope",
                       "name": requested})
      else:
        scopes.pop()
      pending_doc = None; i += 1; continue
    decl = DECLARATION.match(code)
    if decl:
      kind, name, prefix = decl.group("kind"), decl.group("name"), decl.group("prefix")
      namespace = ".".join(value for scope, value in scopes if scope == "namespace")
      if not namespace:
        issues.append({"line": i + 1, "kind": "unsupported", "reason": "declaration outside namespace",
                       "name": name})
      elif "private" in prefix.split():
        issues.append({"line": i + 1, "kind": "private", "reason": "private declaration has no public symbol",
                       "symbol": namespace + "." + name})
      else:
        symbol = namespace + "." + name
        out.append({"id": f"lean:{symbol}:{kind}", "module": source["module"],
                    "namespace": namespace, "symbol": symbol, "kind": kind, "name": name,
                    "file": source["path"], "line": i + 1, "text": pending_doc or "",
                    "revision": source["revision"], "sha256": source["sha256"]})
      pending_doc = None
    elif code:
      pending_doc = None
    i += 1
  return out, issues

def require_named_entities(entities, issues, requirements):
  """Reject unpinned missing, ambiguous, or unsupported required symbols."""
  indexed = {}
  for entity in entities:
    indexed.setdefault((entity["kind"], entity["symbol"]), []).append(entity)
  for kind, symbol in requirements:
    if kind not in ("def", "theorem"):
      raise AlignmentError(f"unsupported required declaration kind `{kind}`")
    matches = indexed.get((kind, symbol), [])
    if len(matches) == 1:
      continue
    issue = next((x for x in issues if x.get("symbol") == symbol), None)
    if issue:
      raise AlignmentError(f"required {kind} `{symbol}` is unsupported: {issue['reason']}")
    if not matches:
      raise AlignmentError(f"required {kind} `{symbol}` is missing")
    raise AlignmentError(f"required {kind} `{symbol}` is ambiguous ({len(matches)} declarations)")

def cited_keywords(text):
  return {k.lstrip(":") for k in re.findall(r"`(:[A-Za-z0-9./!?-]+)`", text)} | \
         {k.lstrip(":") for k in re.findall(r"(?<![\w`])(:[a-z][A-Za-z0-9./!?-]+)", text)}

# `P:460`, `C line 232`, `full_loop_runtime.clj:475`, and the older `L36-49` (two or
# more digits, since `L1` and `L2` name units of the logic model, not lines)
LINE_CITE = re.compile(r"\b[PCRASTLIUK](?::| lines? )\d+(?:-\d+)?|\.clj:\d+|\bL\d\d+(?:-\d+)?\b")

def cited_callees(text):
  return set(re.findall(r"(?<![:\w.-])([A-Za-z][A-Za-z0-9_.!?-]*/[A-Za-z][A-Za-z0-9_.!?-]*)", text))

# the body of an anchor has no parentheses outside a quoted fragment; an opening that
# ANCHOR_FORM cannot finish (`(P route! (when (seq ...)))`) is reported, not ignored
ANCHOR_FORM = re.compile(r"\((?:(not)\s+)?(P|C|R|A|S|T|L|I|U|K)\s+([a-z*][^\s()]*)\s*((?:\"[^\"]*\"|[^()\"])*)\)")
ANCHOR_OPEN = re.compile(r"\((?:not\s+)?(?:P|C|R|A|S|T|L|I|U|K)\s+[a-z*][^\s()]*")
NEEDLE = re.compile(r'"([^"]*)"')

def anchor_forms(text):
  """Return (tag, function, keywords, callees, span, positive?, needles) for simple anchors.

  These are deliberately documentation anchors, not a parser for Lean.  Their
  contents have no nested parens in the contract grammar, so retaining the
  enclosing form lets a cited outcome keyword keep the function it names.
  `(not P function ...)` is an exclusion: it consumes its keywords as prose
  without claiming a code endpoint. A quoted fragment is matched against the
  function's source only; keywords and callees inside it are not cited, and it
  may contain parentheses.
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
    # Named declarations use only declaration_edges below.  Keeping them out
    # of the constructor keyword/name rules prevents a heuristic realization.
    if "inductive" not in e:
      continue
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
    # `(not TAG fn ...)` is an exclusion: a constructor whose text carries one does not
    # reach that function by name either (rev 24 review: `CoordinationPhase.retry`
    # with `(not R run-opportunity! :retry)` reached R's `:reason :retry` absence).
    negated_fns = {(form[0], form[1]) for form in forms if not form[5]}
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
      elif r and short == nm and (s["tag"], s["fn"]) not in negated_fns and (s["fn"] in anchor_name_scope if anchor_name_scope else in_scope) and (s["kind"] != "absent-term") == (e["inductive"] not in TERMS):
        basis = "name"
      if basis: hits.setdefault(basis, []).append(s["id"])
    for basis, ids in hits.items():
      edges.append({"hx/type": "lean-code/realizes", "hx/endpoints": [e["id"], *ids],
                    "props": {"basis": basis, "lean-line": e["line"]}})
  return edges

def declaration_edges(entities, rows):
  """Connect a named Lean declaration only through an explicit positive anchor.

  The existing `:defn` rows are the resolver's pinned source inventory.  They
  become graph nodes only when an anchored declaration actually names one.
  The edge says source correspondence, not semantic proof or runtime authority.
  """
  definitions = {(row["tag"], row["fn"]): row for row in rows if row["kind"] == "defn"}
  edges, nodes = [], []
  emitted = set()
  for entity in entities:
    if entity.get("kind") not in ("def", "theorem"):
      continue
    for tag, fn, kws, callees, _span, positive, needles in anchor_forms(entity["text"]):
      if not positive or kws or callees or needles:
        continue
      row = definitions.get((tag, fn))
      if not row:
        continue  # unresolved_anchors reports the typo; no inferred endpoint
      node_id = f"code:{tag}:{fn}"
      if node_id not in emitted:
        emitted.add(node_id)
        nodes.append({"id": node_id, "kind": "code-definition", "tag": tag, "fn": fn,
                      "file": row["file"], "line": row["line"], "end-line": row["end-line"],
                      "sha": row["sha"]})
      edges.append({"hx/type": "lean-code/realizes", "hx/endpoints": [entity["id"], node_id],
                    "props": {"basis": "definition-anchor", "lean-line": entity["line"],
                              "lean-symbol": entity["symbol"], "lean-revision": entity["revision"],
                              "source-correspondence": True}})
  return edges, nodes

def unresolved_anchors(rows, lean_files, extra_texts=()):
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
  def has_callee(tag, fn, callee):
    # whole symbol: `ledger/append` is not a citation of `ledger/append-retry-result`
    return re.search(r"(?<![\w./!?*+<>=-])" + re.escape(callee) + r"(?![\w./!?*+<>=-])", source[(tag, fn)]) is not None
  def has_keyword(tag, fn, kw):
    if kw in outcome.get((tag, fn), set()): return True
    src = source[(tag, fn)]
    if re.search(r":" + re.escape(kw) + r"(?![\w./!?*+<>=-])", src): return True
    # an auto-resolved keyword is written `::name` in the source
    return "/" in kw and re.search(r"::" + re.escape(kw.split("/")[-1]) + r"(?![\w./!?*+<>=-])", src) is not None
  out = []
  inputs = [(str(path), Path(path).read_text()) for path in lean_files] + list(extra_texts)
  for display, text in inputs:
    stem = Path(display).stem
    parsed = {m.start() for m in ANCHOR_FORM.finditer(text)}
    for m in ANCHOR_OPEN.finditer(text):
      if m.start() not in parsed:
        line = text.count("\n", 0, m.start()) + 1
        out.append((stem, line, text[m.start():text.find("\n", m.start())].strip(),
                    "does not parse: parentheses belong inside a quoted fragment"))
    for tag, fn, kws, callees, (start, end), _positive, needles in anchor_forms(text):
      line, anchor = text.count("\n", 0, start) + 1, " ".join(text[start:end].split())
      if (tag, fn) not in source:
        out.append((stem, line, anchor, f"no function `{fn}` in {tag}")); continue
      src = source[(tag, fn)]
      missing = ([f":{k}" for k in sorted(kws) if not has_keyword(tag, fn, k)]
                 + [c for c in sorted(callees) if not has_callee(tag, fn, c)]
                 + [f'"{n}"' for n in needles if n not in src])
      if missing:
        out.append((stem, line, anchor, f"not in `{fn}`: " + ", ".join(missing)))
  return out

def line_citations(lean_files):
  """Line-number citations left in the Lean files: (file stem, Lean line, citation)."""
  out = []
  for path in lean_files:
    for i, line in enumerate(Path(path).read_text().splitlines(), 1):
      out += [(Path(path).stem, i, m.group(0)) for m in LINE_CITE.finditer(line)]
  return out

def main(sites_path, out_dir, *lean_files, named_sources=(), requirements=()):
  rows = json.loads(Path(sites_path).read_text())
  # :defn rows carry each function's source for anchor resolution; they are not outcome sites.
  sites = [r for r in rows if r["kind"] != "defn"]
  # :call rows are evidence for an anchor that names the callee, not outcome
  # sites the model owes an endpoint: they stay out of the totals, the
  # uncovered table and the stale-citation check (review fix, M17T).
  owed = [s for s in sites if s["kind"] != "call"]
  calls = [s for s in sites if s["kind"] == "call"]
  entities = [e for f in lean_files for e in lean_entities(f)]
  named_entities, named_issues = [], []
  for source in named_sources:
    parsed, issues = named_lean_entities(source)
    named_entities.extend(parsed); named_issues.extend(issues)
  # Required named mode is deliberately checked before `out.mkdir`/writes.
  if requirements:
    if not named_sources:
      raise AlignmentError("required named declarations need pinned --lean-source input")
    require_named_entities(named_entities, named_issues, requirements)
  entities += named_entities
  edges = align(sites, entities)
  named_edges, definition_nodes = declaration_edges(named_entities, rows)
  edges += named_edges
  covered_sites = {i for ed in edges for i in ed["hx/endpoints"][1:]}
  covered_ents = {ed["hx/endpoints"][0] for ed in edges}
  unresolved = unresolved_anchors(rows, lean_files,
                                  [(source["path"], source["text"]) for source in named_sources])
  if requirements and unresolved:
    rendered = "; ".join(f"{file} line {line}: {why}" for file, line, _anchor, why in unresolved)
    raise AlignmentError("required named declaration anchors do not resolve: " + rendered)
  line_cites = line_citations(lean_files)
  out = Path(out_dir); out.mkdir(parents=True, exist_ok=True)
  artifact = {"sites": sites, "entities": entities, "hyperedges": edges}
  if definition_nodes:
    artifact["definition-sites"] = definition_nodes
  (out/"hypergraph.json").write_text(json.dumps(artifact, indent=1))
  def frac(a, b): return f"{a}/{b} ({100*a/b:.0f}%)" if b else "0/0"
  by_kind = {}
  for s in owed: by_kind.setdefault(s["kind"], [0, 0]); by_kind[s["kind"]][1] += 1; by_kind[s["kind"]][0] += s["id"] in covered_sites
  by_ind = {}
  for e in (e for e in entities if "inductive" in e): by_ind.setdefault(e["inductive"], [0, 0]); by_ind[e["inductive"]][1] += 1; by_ind[e["inductive"]][0] += e["id"] in covered_ents
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
  if named_entities:
    by_decl = {}
    for e in named_entities:
      key = (e["module"], e["kind"])
      by_decl.setdefault(key, [0, 0]); by_decl[key][1] += 1; by_decl[key][0] += e["id"] in covered_ents
    L += ["", "| Lean module / declaration kind | declarations with explicit source correspondence |", "|---|---|"]
    L += [f"| {module} / {kind} | {frac(*count)} |" for (module, kind), count in sorted(by_decl.items())]
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

def parse_requirement(value):
  try:
    kind, symbol = value.split("=", 1)
  except ValueError as exc:
    raise argparse.ArgumentTypeError("requirement must be KIND=fully.qualified.symbol") from exc
  if not symbol or not kind:
    raise argparse.ArgumentTypeError("requirement must name kind and full symbol")
  return kind, symbol

def cli(argv):
  parser = argparse.ArgumentParser(description="Generate Lean/Clojure alignment artifacts")
  parser.add_argument("sites")
  parser.add_argument("out")
  parser.add_argument("lean_files", nargs="*", help="legacy raw Lean paths (constructor mode)")
  parser.add_argument("--lean-source", action="append", default=[], metavar="REPO@REV:PATH",
                      help="pinned Lean source for opt-in named def/theorem extraction")
  parser.add_argument("--require-lean", action="append", type=parse_requirement, default=[],
                      metavar="KIND=FULL.SYMBOL", help="pinned named declaration required before output")
  args = parser.parse_args(argv)
  sources = [pinned_lean_source(spec) for spec in args.lean_source]
  main(args.sites, args.out, *args.lean_files, named_sources=sources, requirements=args.require_lean)

if __name__ == "__main__":
  try:
    cli(sys.argv[1:])
  except AlignmentError as exc:
    print(f"alignment error: {exc}", file=sys.stderr)
    sys.exit(2)
