# Triage of the nine glossary-only rows (Box 2 / variable-situation table)

claude-1, 2026-09-02 evening; typing and plan approved by Joe ("I am ok with
your typing & plan"). Source rows: variable-situation-accounting.edn,
content-status :named-only (glossary paragraph exists, pointers resolve, no
declaration in the formal contract corresponds). :named-only does NOT mean
"not built" — the per-row dispositions below say which it is. Each re-typing
requires its own witness pointer; no bulk flip. U14 (minted after U13's
review completes) is the row that executes this.

## Dispositions

Class A — built and witnessed, promote to :closed-by-record-with-witness,
one row at a time, witness pointers named in the registry row:

1. **Observation vector o** — 14 live channels, 8 with R3a likelihood,
   counts derived in tests (glossary's own authorities:
   futon2/src/futon2/aif/observation.clj observation-channels,
   belief.clj channels-with-likelihood, their tests, trace.clj guards).
   ALSO carries a formal residue: the Lean contract's `Obs v` appears only
   inside C's signature; when P-validated-R5 §2a splits C per
   DESIGN-c-vector.md §5, o's mission-grain half is the criteria reader's
   :observable fields (U11). The record closure should say both.
2. **No self-certification** — L1/L2 calibration rule; birth-tagging in
   futon0 rollout_ledger.clj / reward_red_team.clj, tag-discipline tests in
   futon3c flight_record_test.clj; enforced operationally (worklist_check
   rejects :done without :reviewed-by — witnessed live 2026-09-02).
3. **Substrate and Drawbridge** — live infra (:7071/:7073, drawbridge);
   actuator_a3.clj / a4a_substrate.clj per the glossary footnote.
4. **A shared experimental substrate** — the evidence store today's U8a
   probes executed against (runs/U8a-report-sources.md shows live queries).
5. **Revision boundary** — valid-time retract+put and db-as-of-now queries
   (clock-lineage protocol; S4 review confirmed server-side exclusion).
6. **EDN** — the record format every artifact above is written in; witness =
   any typed record + reader.

Class B — framing vocabulary, re-type so it stops counting as uncovered:

7. **Active Inference Framework** — the framework's name; covered by the
   paper as a whole, nothing to build. New content-status (or axis note):
   :framing.

Class C — genuinely unbuilt machinery; PROMOTE to :open-hole with an owner,
so it is counted the way C is counted:

8. **Strategic mission selection** — the glossary paragraph itself calls the
   current three-factor additive value "surrogates for outer-loop structure
   that the implementation has not yet represented" and specs the principled
   layer (reason-bearing policy support; predicted mission outcomes in a
   forward model and G_S; proposal potentials; separate habit E_S;
   hierarchical Q(pi_S), Q(pi_T|pi_S)). Owner:
   futon2/holes/missions/M-wm-strategic-mission-selection.md (exists per
   the glossary footnote). Cross-links: risk term of G_S =
   KL(Q(o|pi) || C_mis) — DESIGN-c-vector.md §3 / U11-U13 build a
   component; zaif board S5 (epistemic mission value) is another.
9. **Embedding space** — split verdict: exists as MiniLM infrastructure
   (cascade-lane constructor); the strategic-selection paragraph demotes it
   to "neither foundational nor required". Close the infrastructure fact
   by-record; do NOT mint a hole for the exploratory proposal mechanism.

## U14 acceptance (to be minted verbatim on the worklist)

Re-type the nine rows in variable-situation-accounting.edn per this note,
each with its witness pointer (Class A/B) or owner (Class C #8);
gen_model_coverage.py re-run; the generated Box-2 total row shows
glossary-only count 0 (or the honest residual, named); paper rebuilds clean;
any contested typing comes back as :needs-joe instead of being forced.

## Execution (U14, 2026-09-02) — two divergences from the plan above

Executed by the wm-edge loop. Seven of the nine rows were re-typed exactly as
dispositioned. Two were not, and both reasons are findings rather than
preferences.

**1. "Observation vector o" is not an uncovered paragraph.** It already has an
owning contract declaration: `ObservationVector` in
`mathlib4/DarkTower/WarMachine/holes-contract.json`, owner
`"sec-glossary.tex paragraph:Observation vector o · P-glossary-mathematics"`,
decided 2026-08-31, already `:closed-by-record-with-witness` in
`variable-situation-accounting.edn`. The drift note's uncovered list was
compiled the same day and does not reflect it. The glossary row is therefore a
*duplicate* of a declaration, and adding it to the coverage table's closed
column would count one paragraph twice. This is why the registry now carries
`:row-source` and the coverage generator counts the two populations apart. The
formal residue in disposition 1 holds and is now pinned: the literal `Obs v`
occurs in the Lean corpus only at `Holes.lean:153`, C's signature (`:6556` and
`:6577` are prose quoting it).

**2. "Revision boundary" was NOT re-typed — it is contested on its referent,
and U14's acceptance sends a contested typing to Joe rather than forcing it.**
Disposition 5 witnesses the row with valid-time retract+put and db-as-of-now
queries — bitemporal store revision. The row's owner resolves to
`sec-glossary.tex:84`, `\paragraph{Revision (2026-08-31; cancellation
boundary)}`, which is about cohort 46's outcome taxonomy: a cancelled attempt
stays durably visible but begins a new semantic stratum unless a cohort
preregisters it. Nothing in that paragraph is about valid time. Closing it on
the note's witness would assert a witness for a claim the paragraph does not
make — the referent-drift defect class this registry exists to catch. A witness
that *does* match the paragraph as written exists:
`futon2/src/futon2/aif/full_loop_cohort.clj:397` emits
`:stratum/id :post-preregistration/cancelled` and `:173` excludes `:cancelled`
from the preregistered population, against the preregistration at
`futon2/holes/labs/M-aif-full-loop-46/cohort.edn`. Joe's call: (a) close on the
cohort witness, (b) name the paragraph disposition 5 meant, or (c) leave it
`:named-only`.
