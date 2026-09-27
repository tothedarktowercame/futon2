# E-kimi-task-111 — Can the ledger tell a stale wire test from its warrant's load closure without rerunning it (read-only)

**Requisition:** completed — 2026-09-27T18:16:42Z, job invoke-1790532795588-25533-047ee9f6, state done

Clocked in by claude-8 for kimi-7 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# WARRANT-CLOSURE-D: can the ledger tell a stale wire test from its warrant, without rerunning it? (read-only discovery; ⟨2⟩3 of PROOF-2a-PLAN)

WARRANT-CLOSURE-D (claude-8 → kimi-7). READ-ONLY: change no file, make no commit, register no warrant, run no flight or click. Bell claude-8 back with the report.

Why. Joe, 2026-09-27: "I hate slow tests, that's why I created the Test Registry, but if there's a way to avoid *rerunning* slow tests by running big sweeping tests in the background and then producing warrants for them, we can do that." Today five first-layer wire tests were red for fourteen hours (a futon2 commit, 5217cb619, changed a value they pinned) while the wire ledger read "0 unverified": the ledger test (`futon3c.diagramprover.wm-wire-ledger-test`) evaluates each wire's `:check` and looks up warrants for second-layer witnesses; it judges a warrant stale by the TEST FILE's last commit (`second-layer-context`, `:last-commit`, `:ancestor?`, about :470-500), not by the futon2 code the test loads. A registry record does carry more: its payload has `:code-files`, `:test-files`, `:code-sha`, `:load-closure` (a list of {:ns :path :sha256} for what the run loaded) and `:results`.

Questions, each with file:line in futon3c `src/futon3c/test_registry.clj` (and `src/futon3c/test_registry/ledger.clj`), and a real read where marked (your own process; read the evidence store through `GET http://localhost:7073/api/alpha/evidence/<id>`, no writes):

1. WHAT A WARRANT PINS. For one real warrant — take `test-registry-467e1fe2…` from futon3c `data/test-registry/namespace-ledger.edn` (the next-step wire's first-layer namespace, registered today at futon3c 97364b6d) — print the payload's keys and the size and a sample of `:load-closure`: does it list the futon2 source files the test loaded (e.g. `futon2/src/futon2/aif/outer_cascade.clj`) with their sha256? Does it list futon3c support namespaces?
2. WHAT ALREADY COMPARES A CLOSURE. `closure-diff`, `uncommitted-scope` and the function around test_registry.clj:500-515 that classifies changed paths `:committed` / `:uncommitted` "when a recorded warrant has been superseded": what calls them, what do they return, and is there already a function that answers "is this warrant current against the files on disk now" without running the test? Name it and its cost (file hashes only?).
3. WOULD IT HAVE CAUGHT TODAY'S CASE. Take the warrant the next-step namespace had BEFORE today's fix (the ledger file's history: `git log -p -S'r1-target-field-r1-outer-cascade-next-step' -- data/test-registry/namespace-ledger.edn`, or the newest entry for that namespace dated before 2026-09-27T03:26Z) and compare its load-closure's sha256 for outer_cascade.clj with the file's sha256 at futon2 5217cb619 and at the commit before it: different or not? If the closure does not list that file, say so: then the closure cannot detect it.
4. COST. How long does a closure comparison take for one warrant, and for all the wire namespaces (about 194), by your measurement on a sample of ten?

Report: answers 1–4 with printed values; then ONE proposed design in at most fifteen lines, with two parts: (i) a background sweep that re-registers only the wire namespaces whose warrant is not current by closure (never run in the foreground, one JVM at a time), and (ii) a ledger-side check, cheap, that every wire namespace's newest warrant is current by closure and passed, and that every declared second-layer entry's evidence resolved — failing with the list of namespaces otherwise. Say which existing functions each part would call and what is missing. Do not write code.
