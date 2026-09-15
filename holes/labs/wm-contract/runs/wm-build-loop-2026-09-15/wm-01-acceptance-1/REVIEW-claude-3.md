# WM-01-acceptance-1: claude-3 review of codex-3's acceptance receipt

**Reviewer verdict: CONCUR — ACCEPT**, with the same scope as `RECEIPT.md`: the carrier laws and constructor changes in mathlib4 `480a666ad27c18477b4b9df86e11862be3930d50` (historical subject). Not WM-01 completion; not acceptance of any descendant.

- Author of acceptance: codex-3 (job `invoke-1789507948054-21249-18a18e5d`). Independent of the repair author codex-27.
- Reviewer: claude-3. Governing dispatch: codex-28 `invoke-1789507763981-21248-b8bddfbb`.
- Handoff packet: `../handoffs/WM-01-acceptance-1-to-codex-3.md` (sha256 `ac63d67bdd732e65dfe12ec4235a47b827ebbe5c0241e956d08d36bae248c8b2`).
- Reviewed commits: futon2 `3fc00fe1` (receipt and controls) and `1f373e4b` (raw logs).

## What I checked

1. **Commit scope.** `git show --name-only 3fc00fe1 1f373e4b`: every file is under `wm-01-acceptance-1/`. Nothing else was committed.
2. **Subject identity.** For all 38 files in the receipt's hash table, `git show 480a666ad2:<file> | sha256sum` matches: 38/38, 0 mismatches. The set equals `git show --name-only 480a666ad2` (38 files, none missing or extra).
3. **Control design, by reading.**
   - Each independent negative (`InvalidKernel-*`, `InvalidExactTable-*`, `InvalidPrecision`) supplies every field.
   - The field under test is a named `?field` hole, so Lean's error carries that field's case tag.
   - The other fields carry real proofs.
   - Invalid data: duplicate `[(), ()]` at mass 1/2; support `[false]` with hidden mass 100 at `true`; masses (-1, 2); singleton mass 1/2; precision -2.
   - Each leaves the non-tested laws true.
   - `Valid.lean` accepts a sparse kernel with a real off-support outcome, exact thirds, a sparse exact table, and zero and two precision. `zeroPrecisionAccepted` rules out strict positivity.
   - The committed `ProbabilityKernelRepairNegative.lean` at the subject expects exactly one error per `#guard_msgs` block, so the other fields in each definition are discharged. Its `duplicateKernel` diagnostic has no case tag; the named-case controls above settle the attribution to `support_nodup`.
4. **Independent re-run.** Run from `/home/joe/code/mathlib4` with `LEAN_NUM_THREADS=1 lake env lean <file>` (no `-o`, nothing written).
   - Files: `Valid.lean`, the 9 `Invalid*.lean`, `RejectedAxioms.lean`, `Axioms-Holes.lean` and `Axioms-MachineQ.lean`.
   - Exit statuses: Valid 0; all 9 Invalid 1; RejectedAxioms 0; both Axioms probes 0.
   - All 13 stdouts are byte-identical to codex-3's `logs/*.stdout`.
   - Each rejecting diagnostic names the intended field: `support_nodup`, `mass_eq_zero_of_not_mem(.true)`, `nonnegative(.false)`, `normalised`, `precision_nonnegative`.
5. **Axioms.** I parsed every `logs/Axioms-*.stdout` and `logs/Valid.stdout`, including multi-line lists.
   - 73 declarations: the 65 in `axiom-targets.json` plus the 8 valid controls.
   - Each depends on exactly `propext`, `Classical.choice` and `Quot.sound`. None is outside that set.
   - `sorryAx` occurs only in `RejectedAxioms.stdout`, for the three deliberately rejected definitions `duplicateKernel`, `hiddenMassKernel` and `negativePrecisionCall`, as the receipt flags.
6. **Canonical checkout untouched.**
   - `/home/joe/code/mathlib4` is still `darktower` at `2b22eeef3102b3b48c8c12df66aaded459c36819`, with 0 porcelain lines.
   - `find .lake -newermt '2026-09-15 21:32:00 UTC'` (dispatch time) returns 0 files, both after codex-3's run and after my re-run.
   - No `/tmp/wm01-acc*` workspace remains. `/tmp/wm01-tools.py` (19:25) is an earlier cascade-rendering script, not from this job.

## Findings

- **Correction to my own packet.** The packet said no retained codex-27 logs were found in /tmp. That was wrong. My `ls /tmp | grep … | head -20` was filled with `.wm-repair-*` lock files before any `codex27-kernel*` entry could show. The files exist (build passes 1–3, final build, source audit, before/after axioms). codex-3 found them and correctly classified them as author-only evidence (`existing-evidence.json`). They are in volatile /tmp; only their sha256s are retained here.
- **No defect found in the receipt.** Its claims matched every check above.

## Limits of this review

- My re-run compiled the control and probe files against the canonical checkout's prebuilt oleans (`Holes.olean` mtime 2026-09-15 06:25:31Z), read-only. It is not a fresh build.
  - The fresh compilation of the 38 modules is codex-3's evidence, in an isolated copy since removed.
  - `Valid.lean` elaborates with the repaired fields, and the outputs match codex-3's fresh-build outputs byte for byte. That shows the oleans carry the repaired structure; it does not independently re-establish the fresh build.
- I did not re-read all 38 file diffs. I relied on the receipt's constructor-group account for items beyond the kernel structure, the ExactTable forwarding and the precision type.
- I did not check the nine pre-existing `sorry` warnings in `Holes.lean` line by line. The relevant claim, that no accepted declaration depends on `sorryAx`, is covered by check 5.
- Everything `RECEIPT.md` lists under Limits stays open: numeric/runtime correspondence, admission bindings, production callers, descendant/serving behaviour. WM-01 stays unchecked.
