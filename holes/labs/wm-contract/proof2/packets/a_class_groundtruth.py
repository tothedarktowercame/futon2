#!/usr/bin/env python3
"""A-CLASS ground truth: re-derive the truth of every case in a-class-cases.edn
by means INDEPENDENT of the checks in futon2.aif.observation-checks.

Never calls the check code. Uses git ls-tree / git show / line scanning /
json parsing / sha256sum / urllib against the evidence API.

Env: A_CLASS_ROOT (default /home/joe/code) — repository root; the scratch dry
run points it at a twin of the checkout holding this packet's fixtures.
     A_CLASS_AGENCY (default http://127.0.0.1:7070) — evidence API base.

Writes a-class-truth.json beside the cases file and exits nonzero if any
derived truth disagrees with the case's intended :truth (a construction bug,
to be fixed before the check run — not a check error)."""
import json, os, re, subprocess, sys, hashlib, urllib.request

ROOT = os.environ.get("A_CLASS_ROOT", "/home/joe/code")
AGENCY = os.environ.get("A_CLASS_AGENCY", "http://127.0.0.1:7070")
HERE = os.path.dirname(os.path.abspath(__file__))

def git(repo, *args):
    return subprocess.run(["git", "-C", os.path.join(ROOT, repo), *args],
                          capture_output=True, text=True)

def resolve(repo, sha):
    r = git(repo, "rev-parse", "--verify", "--end-of-options", sha + "^{commit}")
    return r.stdout.strip() if r.returncode == 0 else None

def show(repo, sha, path):
    r = git(repo, "show", f"{sha}:{path}")
    return r.stdout if r.returncode == 0 else None

def decl_present(text, decl):
    """Independent line scan: a line whose lstrip starts with decl, the next
    char being whitespace, :, (, {, [ or end of line."""
    for line in text.splitlines():
        s = line.lstrip()
        if s.startswith(decl):
            rest = s[len(decl):]
            if rest == "" or rest[0] in " \t:({[":
                return True
    return False

def truth_c3(loc):
    rs = resolve(loc["repo"], loc["sha"])
    if rs is None: return None, f"locator sha unresolvable"
    r = git(loc["repo"], "ls-tree", rs, "--", loc["path"])
    return bool(r.stdout.strip()), f"git ls-tree {rs[:10]} -- {loc['path']}: {'nonempty' if r.stdout.strip() else 'empty'}"

def truth_c4(loc):
    rs = resolve(loc["repo"], loc["sha"])
    if rs is None: return None, "locator sha unresolvable"
    text = show(loc["repo"], rs, loc["path"])
    if text is None: return False, f"file absent at {rs[:10]}"
    return decl_present(text, loc["decl"]), "line scan over git show output"

def truth_c5(loc):
    rs = resolve(loc["repo"], loc["sha"])
    if rs is None: return None, "locator sha unresolvable"
    text = show(loc["repo"], rs, loc["bundle-path"])
    if text is None: return None, "bundle absent"
    bundle = json.loads(text)
    contract = next((c for c in bundle.get("contracts", [])
                     if c.get("contract-id") == loc["entry"]), None)
    if contract is None: return False, "contract-id absent from bundle"
    loci = [d["clojure-locus"] for d in contract.get("declarations", []) if d.get("clojure-locus")]
    if not loci: return False, "contract has no clojure-loci"
    for l in loci:
        repo, rest = l.split("/", 1); path, line = rest.rsplit(":", 1)
        text = show(repo, "HEAD", path)
        n = len(text.splitlines()) if text is not None else -1
        if n < int(line):
            return False, f"locus {l} does not resolve at {repo} HEAD (lines={n})"
    return True, f"{len(loci)} loci resolve"

WITNESS_RE = dict((k, re.compile(r":" + k + r'\s+"([^"]+)"')) for k in ("repo", "sha", "entry"))

def truth_c6(loc):
    rs = resolve(loc["repo"], loc["sha"])
    if rs is None: return None, "locator sha unresolvable"
    text = show(loc["repo"], rs, loc["path"])
    if text is None: return False, "witness file absent"
    m = {k: (p.search(text).group(1) if p.search(text) else None)
         for k, p in WITNESS_RE.items()}
    if not m["repo"] or not m["sha"]:
        return None, "witness malformed (no repo/sha)"
    wrs = resolve(m["repo"], m["sha"])
    if wrs is None:
        return False, f"witness sha unresolvable in {m['repo']} (refusal expected)"
    if m["entry"]:
        r = git(m["repo"], "ls-tree", wrs, "--", m["entry"])
        if not r.stdout.strip():
            return False, f"witness entry absent at {wrs[:10]}"
    elif loc.get("require-entry"):
        return False, "entry mandatory but absent (refusal expected)"
    return True, f"witness commit {wrs[:10]} resolves" + (", entry present" if m["entry"] else "")

def section(txt, key):
    i = txt.find(key)
    if i < 0: return {}
    j = txt.index("{", i); depth = 0
    for k in range(j, len(txt)):
        if txt[k] == "{": depth += 1
        elif txt[k] == "}":
            depth -= 1
            if depth == 0: break
    return dict(re.findall(r'"([^"]+)"\s+"([0-9a-f]{64})"', txt[j:k]))

def truth_c8(loc):
    eid = loc["config"]
    try:
        r = urllib.request.urlopen(f"{AGENCY}/api/alpha/evidence/{eid}", timeout=10)
        body = json.load(r)
    except urllib.error.HTTPError as e:
        if e.code == 404: return False, "registry holds no such record (:absent)"
        return None, f"registry unreadable: HTTP {e.code}"
    except Exception as e:
        return None, f"registry unreadable: {e}"
    entry = body.get("entry")
    if entry is None: return False, "registry holds no such record (:absent)"
    txt = entry.get("evidence/body", {}).get("payload-edn", "")
    digest = hashlib.sha256(txt.encode("utf-8")).hexdigest()
    if eid != "test-registry-" + digest:
        return None, "record digest mismatch (registry unreadable)"
    ns_m = re.search(r'"-n"\s+"([^"]+)"', txt)
    kind = re.search(r":kind\s+:(\w+)", txt)
    post = re.search(r":postcheck\s*\{[^}]*?:status\s+:(\w+)", txt)
    res = re.search(r":results\s*\{(.*?)\}", txt)
    fail = re.search(r":failures\s+(\d+)", res.group(1)) if res else None
    err = re.search(r":errors\s+(\d+)", res.group(1)) if res else None
    warrant = re.search(r":warrant\?\s+(true|false)", txt)
    files = {}; files.update(section(txt, ":code-files")); files.update(section(txt, ":test-files"))
    ran = ns_m.group(1) if ns_m else None
    if not kind or kind.group(1) != "run": return False, "not a run record"
    if ran != loc["namespace"]:
        return False, f"namespace-mismatch: record ran {ran}"
    if not warrant or warrant.group(1) != "true": return False, "not a warrant"
    if not post or post.group(1) != "matched": return False, "postcheck not matched"
    if not fail or int(fail.group(1)) != 0 or not err or int(err.group(1)) != 0:
        return False, f"run recorded failures={fail.group(1) if fail else '?'} errors={err.group(1) if err else '?'}"
    root = os.path.join(ROOT, loc["repo"])
    moved = []
    for p, s in files.items():
        fp = os.path.join(root, p)
        cur = hashlib.sha256(open(fp, "rb").read()).hexdigest() if os.path.isfile(fp) else None
        if cur != s: moved.append(p)
    if moved: return False, f"content moved: {', '.join(sorted(moved))}"
    return True, f"warrant {eid[:24]}… current, matched, 0 failures over {len(files)} pinned paths"

TRUTH = {"C3": truth_c3, "C4": truth_c4, "C5": truth_c5, "C6": truth_c6, "C8": truth_c8}

def read_cases():
    """Minimal EDN read of the cases file via babashka-free means: the file is
    generated, so a small parser over its known shape suffices — but rather
    than parse EDN in Python, shell out to clojure? No: keep independence
    cheap; the cases file is machine-generated with a stable shape, so parse
    with regex over case blocks."""
    txt = open(os.path.join(HERE, "a-class-cases.edn")).read()
    blocks = re.findall(r'\{:id\s+"([^"]+)"\s+:class\s+"(\w+)"\s+:truth\s+(true|false)\s+:near-miss\?\s+(true|false)\s+:locator\s+(\{[^{}]*)\}([^}]*)\}', txt, re.S)
    cases = []
    for cid, cls, truth, nm, locs, tail in blocks:
        loc = dict(re.findall(r':([?\w-]+)\s+"((?:[^"\\]|\\.)*)"', locs))
        if ":require-entry true" in locs: loc["require-entry"] = True
        note = re.search(r':note\s+"((?:[^"\\]|\\.)*)"', tail)
        expect = re.search(r":expect\s+:(\w+)", tail)
        cases.append({"id": cid, "class": cls, "truth": truth == "true",
                      "near-miss?": nm == "true", "locator": loc,
                      "note": note.group(1) if note else None,
                      "expect": expect.group(1) if expect else None})
    return cases

def main():
    cases = read_cases()
    assert len(cases) == 100, f"expected 100 cases, parsed {len(cases)}"
    out, mismatches = [], []
    for cs in cases:
        truth, how = TRUTH[cs["class"]](cs["locator"])
        row = dict(cs); row["derived-truth"] = truth; row["truth-how"] = how
        out.append(row)
        if truth is not None and truth != cs["truth"]:
            mismatches.append((cs["id"], cs["truth"], truth, how))
        print(f"{cs['id']:8s} intended={cs['truth']!s:5s} derived={truth!s:5s} {how}")
    json.dump(out, open(os.path.join(HERE, "a-class-truth.json"), "w"), indent=1)
    if mismatches:
        print("\nCONSTRUCTION MISMATCHES (fix cases, not checks):")
        for m in mismatches: print(" ", m)
        sys.exit(1)
    print("\nall derived truths agree with construction")

if __name__ == "__main__":
    main()
