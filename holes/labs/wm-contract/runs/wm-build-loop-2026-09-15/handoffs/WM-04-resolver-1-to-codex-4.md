# WM-04-resolver-1: the strict authority resolver for the existing observation validator

From claude-3 to codex-4. **Author: codex-4. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28, under Joe's September 16 breadth-first direction (`p4ng/wm-walkthroughs/build-loop/closure/DEADLINE-PLAN.md`, sha256 `ee2e2128…`). Dispatch: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-04-resolver-1.md` (sha256 `a9c685fd06707ab596f63d6e517ac55a944c2853784a41137e188b9ae4925ad7`). **Read it first; it governs.** TODO WM-04, cascade O2/O4/O6, DAG node `WM-04-machinery`.

This runs **concurrently** with codex-3 (WM-08/09 review, writing only `runs/.../wm-08-09-review-1/`) and alongside codex-2's finished receipt campaign. Your write scope is disjoint from both; keep it that way.

## What this is, and what it is not

It is one acquisition **interface**: a resolver that lets the unchanged validator check an observation against independently supplied authority and admitted evidence.

It is **not** an acquired observation, a real label, measured A, coverage, serving activation, an observation commission, or WM-04 closure. Fixture labels in your tests are test data; never describe them as observations.

## The seam, as I verified it (canonical futon2 `4fec3894`)

`src/futon2/aif/categorical_state_observation.clj` (343 lines, sha256 prefix `8d48ecf867e2b6df`):
- `validate-observation!` (line 223) takes `[observation {:keys [resolver io-opts expected]}]`.
- `authority-record!` (line 217) demands `(fn? resolver)` and calls **`(resolver kind ref)`** for **three** kinds:
  - `:evidence` (from `validate-evidence!`, line 177, once per evidence ref),
  - `:observer` (`[:authority :observer/ref]`),
  - `:review` (`[:authority :review/ref]`).
- `read-pinned-form!` (line 76) does the file and form digest verification. **Keep that there**; your resolver returns locators, it does not re-verify bytes itself.
- Read lines 177–222 yourself for the exact return shape `authority-record!` expects before designing anything. The dispatch describes it as `{:path :sha256}`; confirm it from the code, and if the code expects more (or different) keys, the code wins — report the difference.

`src/futon2/aif/evidence_manifest.clj` (160 lines) exposes `validate-manifest`, `build-manifest` and `verify-retention-agreement`. Entries are `{:evidence/id :source-path :sha256 :admitted-at}`.

**No resolver exists yet.** The only `:resolver` use elsewhere is `actuator_a3.clj`, unrelated. `observation_authority_resolver.clj` and its test do not exist. So the new namespace does not duplicate anything — but re-check before you create it, as the dispatch asks.

## The rules the resolver must enforce

- **Inputs are external.** A pinned authority index and a validated admitted-evidence manifest for one exact entity, occurrence and cutoff. The candidate observation can neither supply nor replace either.
- **Exact binding only.** An index entry binds a reference kind and identity to an existing pinned record. Unknown refuses; ambiguous (more than one match) refuses.
- **Evidence must match admission.** Identity, path and hash must equal the manifest entry, and the entry must be admitted by the evidence cutoff.
- **A late review is allowed.** Do not require the observer review's own creation time to predate the evidence cutoff; reviewing frozen eligible bytes later is legitimate.
- **Never infer authority** from path names, registry status, posteriors, author claims, or the candidate's own fields. No default authority, no live search fallback.
- **Retain identities** of the index, manifest and subject the resolver was built from.
- **An index does not prove its own authorization.** Externally commissioned authority stays a separately named input. Do not fabricate one for production.

## Tests

New `src/futon2/aif/observation_authority_resolver.clj` and `test/futon2/aif/observation_authority_resolver_test.clj`, plus minimal integration tests in `test/futon2/aif/categorical_state_observation_test.clj`.

**Positive:** an actual call to `validate-observation!` using your resolver, with realistic existing observation and authority record shapes, independently supplied records, literal admitted evidence and an exact review subject.

**Negatives**, each asserting the specific refusal rather than just "throws":
wrong kind or ref · absent authority · ambiguous binding · missing manifest entry · altered bytes, hash or path · evidence admitted after cutoff · wrong entity or occurrence · candidate-owned authority or review · review of a different subject.

Also include one **positive** late-review case (review created after the evidence cutoff, over eligible frozen bytes), so the permitted case is tested and not just the refusals.

Run in isolated processes, retaining command, cwd and exit:
- `clojure -X:test :nses '[futon2.aif.observation-authority-resolver-test]'`
- `clojure -X:test :nses '[futon2.aif.categorical-state-observation-test]'`
- `clojure -X:test :nses '[futon2.aif.evidence-manifest-test]'`
- clj-kondo on the new source and test files
- check-parens with files after `--`: `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- <files>`

## Scope

Write only:
- `src/futon2/aif/observation_authority_resolver.clj`
- `test/futon2/aif/observation_authority_resolver_test.clj`
- `test/futon2/aif/categorical_state_observation_test.clj` (minimal additions)
- `holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-04-resolver-1/`

**Existing source contracts stay unchanged.** If `categorical_state_observation.clj` or `evidence_manifest.clj` cannot support a correct resolver without edits, stop and return the precise conflict instead of editing them. Not allowed: WM-01 receipts, model semantics, manifest schema, the runner, storage, the live service, reloads into the shared JVM, checkbox edits, pushes, choosing successor work.

futon2 is shared (~190 unrelated dirty paths, and codex-3 is committing concurrently): stage explicit paths only, never `commit -a`, never amend, and check `git log -1` immediately before committing.

## Receipt: `wm-04-resolver-1/RECEIPT.md`

Record: the exact source pins of the validator and manifest files; the resolver's API and which rule each branch enforces; every command with cwd and exit; the positive and each negative with its asserted refusal; and the limits stated plainly (interface only; no acquired observation, label, measured A or commission).

## Stop and bellback

Stop once the scoped files and receipt are committed. **Bell claude-3 back** with the commit sha, the resolver API, the test results with exits, and any conflict with the existing contracts. A discovery beyond scope is a question in the bellback, not a task.
