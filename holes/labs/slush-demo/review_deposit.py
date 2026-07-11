#!/usr/bin/env python3
"""review_deposit.py — the reviewer's gate battery, as code.

This is the CONTROL LAYER's executable half (see CONTROL-LAYER.md). Every
check here was learned the hard way during batches 1-4 (2026-07-10/11); the
comment on each names the incident that minted it. A future session (or a
different reviewer agent) runs THIS instead of re-deriving the battery.

Usage:
  python3 review_deposit.py <deposit.edn> [--worklist <batch-worklist.json>]
  python3 review_deposit.py --sweep          # box-subset check on ALL deposits

Exit 0 = all checks pass. Any FAIL prints the incident it guards against.
NOT covered here (needs a human/agent reviewer, see CONTROL-LAYER.md):
re-running the runner's claimed test gates from a different cwd; :via prose
independence across same-mission folds; satiety-call quality; wiring judgment.
"""
import hashlib, json, re, subprocess, sys, glob
from pathlib import Path

CHECK_PARENS = "/home/joe/code/futon4/dev/check-parens.el"
PROPOSALS = Path("/home/joe/code/futon2/holes/labs/slush-demo/findings/proposals")
DEPOSITS = Path("/home/joe/code/futon6/data/fold-turns")


def decode_edn_string(raw):
    # psi-sha is over the DECODED value (Fold Run 10: runner right, reviewer
    # checker wrong — escapes resolve to real characters before hashing)
    return raw.replace('\\"', '"').replace('\\n', '\n').replace('\\\\', '\\')


def all_proposals():
    props = {}
    for f in PROPOSALS.glob("proposals-*.json"):
        if "SEALED" in f.name:
            continue
        for r in json.load(open(f)):
            props[r["proposal"]] = set(r["patterns"])
    return props


def parse(path):
    t = Path(path).read_text()
    d = {"text": t}
    d["hash"] = (re.search(r':proposal/hash\s+"([0-9a-f]+)"', t) or [None, None])[1]
    d["pins"] = (re.search(r':pins \[([^\]]*)\]', t) or [None, ""])[1]
    m = re.search(r':psi\s+"((?:[^"\\]|\\.)*)"', t, re.S)
    d["psi"] = decode_edn_string(m.group(1)) if m else None
    m = re.search(r':psi-sha256\s+"([0-9a-f]+)"', t)
    d["psi_sha_claimed"] = m.group(1) if m else None
    ids = re.search(r':pattern-ids\s+\[([^\]]*)\]', t, re.S)
    d["pattern_ids"] = set(re.findall(r'"([\w./-]+/[\w.-]+)"', ids.group(1))) if ids else set()
    d["boxes"] = {}
    for chunk in re.split(r'\{:id :', t)[1:]:
        bid = re.match(r'(b\d+)', chunk)
        fp = re.search(r':fits-pattern\s+"([^"]+)"', chunk[:3000])
        if bid and fp:
            d["boxes"].setdefault(bid.group(1), fp.group(1))
    return d


def review(path, worklist=None):
    d = parse(path)
    checks = []

    def check(name, ok, incident):
        checks.append((name, ok, incident))

    # 1. paren/EDN balance — Batch 2: two artifacts one brace short behind
    #    claimed passes; one claim was a NON-RUN of the checker.
    r = subprocess.run(["emacs", "-Q", "--batch", "-l", CHECK_PARENS,
                        "--eval", "(arxana-check-parens-cli)", "--", str(path)],
                       capture_output=True, text=True)
    check("check-parens prints OK", r.stdout.strip().endswith("OK"),
          "B2-F4/F5: claimed pass on unbalanced file; 'no output' read as pass")

    # 1b. real EDN parse, EXACTLY ONE form then EOF — Fold Run 22 (zai-5):
    #     check-parens tracks AGGREGATE balance, so compensating braces pass it
    #     while nesting is wrong; the EDN reader is stricter. Refined during
    #     Run 25 (zai-5 again): read-string reads only the FIRST form and
    #     ignores trailing content — mismatched brackets AFTER a valid form are
    #     invisible to it. So: first read must yield a form, second read must
    #     hit clean EOF (a trailing bracket makes it throw; a second form makes
    #     it non-EOF; both FAIL).
    r = subprocess.run(["bb", "-e",
                        '(with-open [rd (java.io.PushbackReader. '
                        f'(clojure.java.io/reader "{path}"))] '
                        '(let [a (clojure.edn/read {:eof ::eof} rd) '
                        '      b (clojure.edn/read {:eof ::eof} rd)] '
                        '(when (and (not= a ::eof) (= b ::eof)) (println "EDN-OK"))))'],
                       capture_output=True, text=True)
    check("EDN reads as exactly one form", "EDN-OK" in r.stdout,
          "Run 22/25: mis-nesting or trailing garbage invisible to weaker gates")

    # 2. psi-sha over DECODED value — Fold Run 10.
    ok = (d["psi"] is not None and d["psi_sha_claimed"] is not None and
          hashlib.sha256(d["psi"].encode()).hexdigest() == d["psi_sha_claimed"])
    check("psi-sha256 (decoded value)", ok, "Run 10: hash the value, not the source text")

    # 3. proposal hash at BOTH sites — the S1/S2 attribution seam.
    ok = d["hash"] is not None and d["text"].count(d["hash"]) >= 2
    check("proposal/hash at top level AND constructor", ok,
          "attribution seam: scoreboard joins deposits to sealed proposers by this")

    # 4. pins truthful — Fold Run 1: pins claimed that were never computed.
    check("pins = [:psi-sha256] only", d["pins"].strip() == ":psi-sha256",
          "Run 1: blinded folds have no prompt replay; pins that don't pin = laundering")

    # 5. declared cascade matches the blinded proposal.
    props = all_proposals()
    if d["hash"] in props:
        check("pattern-ids match blinded proposal", d["pattern_ids"] == props[d["hash"]],
              "the fold grounds THIS cascade, verbatim")
        # 6. box set ⊆ proposal set — Fold Run 19: two patterns imported from
        #    the runner's OWN earlier fold of the same mission.
        extra = set(d["boxes"].values()) - props[d["hash"]]
        check("box-set subset of proposal-set", not extra,
              f"Run 19 cross-fold contamination{': foreign ' + str(sorted(extra)) if extra else ''}")
    else:
        check("proposal hash known", False, "hash not found in any blinded batch file")

    # 7. psi freshness vs same-mission siblings — first-contact discipline.
    stem = re.sub(r'-\d+\.edn$', '', Path(path).name)
    stale = []
    for sib in DEPOSITS.glob(f"{stem}-*.edn"):
        if sib.name == Path(path).name:
            continue
        sd = parse(sib)
        if sd["psi"] and sd["psi"] == d["psi"]:
            stale.append(sib.name)
    check("psi fresh vs same-mission siblings", not stale,
          f"first-contact discipline{': identical to ' + str(stale) if stale else ''}")

    width = max(len(c[0]) for c in checks) + 2
    failed = 0
    for name, ok, incident in checks:
        print(f"  [{'PASS' if ok else 'FAIL'}] {name:{width}}" +
              ("" if ok else f"<- {incident}"))
        failed += not ok
    return failed


def sweep():
    props = all_proposals()
    bad = 0
    n = 0
    for dep in sorted(DEPOSITS.glob("ft-*.edn")):
        d = parse(dep)
        if d["hash"] not in props:
            continue
        n += 1
        extra = set(d["boxes"].values()) - props[d["hash"]]
        if extra:
            bad += 1
            print(f"  VIOLATION {dep.name}: foreign {sorted(extra)}")
    print(f"  sweep: {n - bad}/{n} proposal-linked deposits clean")
    return bad


if __name__ == "__main__":
    if "--sweep" in sys.argv:
        sys.exit(1 if sweep() else 0)
    path = sys.argv[1]
    print(f"review: {path}")
    sys.exit(1 if review(path) else 0)
