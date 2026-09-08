# C591 — C as a calculation: discovery to evaluation (PROPOSAL for Joe)

2026-09-08 evening, claude-1, from Joe's direction (RULINGS-walkthrough Item 12):
not another formal definition — a proposal for how the C-vector "turns into a
real evaluation," focused on one specific part of the system, with unit tests.
Discovery, not invention: the preferences are to be READ OFF the recorded
evidence, with ruling as the fixing act.

## 1. What already computes, and what cannot

Two C carriers exist. They are not in the same state:

- **C_int (channel floor)** — per-channel Gaussian preference densities,
  consumed LIVE at the risk site: `:risk-mode :kl` scores
  `Σ_ch w_ch · KL(N(μ_ch,σ²_ch) ‖ C_ch)` in nats (efe.clj:643-645 region,
  D5a, contract E-C-vector-live.md:230). This is a real calculation, in the
  fold, today.
- **seeded-C (the ruled 12-wide outcome-kind masses)** —
  ruled_outcome_c.clj:43-50, `:folded? false`. Declared, ruled (Items 6/10),
  and UNEVALUABLE as it stands — not because a wire is missing but because a
  TYPE bridge is: seeded-C is a distribution over terminal outcome-kinds
  (dispositions); Q(o|π) predicts channel observations. `KL[Q(o|π) ‖ C]`
  over outcome-kinds cannot be formed from what the forward model emits.

The missing computational object is the bridge:

    Q(d|π) := Σ_o  P(d | o) · Q(o|π)        — predicted DISPOSITION distribution

`P(d|o)` — the disposition kernel: probability of terminal outcome-kind d
given a predicted observation state. Nothing invents preferences here; the
kernel is a MODEL-side object, and it is DISCOVERABLE FROM RECORDS: the
cohort ledger (holes/labs/M-aif-full-loop-46/ledger.edn) holds
(observation trajectory → terminal disposition) pairs — attempt closes with
`:grounded-change` etc. — exactly the conditional's empirical support. The
dark C_mis goal-outcome half (C533: ensure-belly-fresh! built, no caller) is
a partial hand-built version of the same map and gets a caller or gets
retired by this work.

## 2. The evaluation, end to end

    disposition-risk(π) := KL[ Q(d|π) ‖ seeded-C ]
    G(π) += w_d · disposition-risk(π)     — as the :ruled-outcome-c ordered
                                            layer (efe.clj:117-150 shape),
                                            flipping :folded? true, cited to
                                            Item 6; canonical Q(o|π) per
                                            Item 10 (F8 depth flags inherited)

Named zeros stay in support (positivity premise, Holes.lean:6993-6997), so
the KL is well-defined against them by construction, not by epsilon-fudging.

## 3. The discovery loop (preferences read, never invented)

- **Pins**: the ruled seeded-C masses are ground truth. Discovery cannot move
  them; only a ruling can.
- **Extractor**: a checker-shaped script reads the evidence landscape — the
  RULINGS-walkthrough files, aif-equations :choices, mission documents,
  design-pattern promotions, months of logged operator turns — and emits
  proposed-C: same 12-kind support, every mass carrying PROVENANCE (the
  quotes/rulings it derives from), like the design-patterns→production-rules
  conversion.
- **The diff is the product**: proposed-C vs ruled seeded-C renders as a
  decision sheet. Agreement strengthens a pin (citations accumulate);
  disagreement surfaces for ruling. The extractor NEVER writes preferences.
- **The AlphaZero dual, made operative**: the habit prior E is revealed
  preference (what the system did); C is stated preference (what Joe ruled).
  Discovery adds a third: evidence-derived proposed-C. The E-vs-C gap inside
  G is the machine's own value-policy dual; the proposed-C-vs-ruled-C gap is
  the OPERATOR-FACING one — where behavior-as-logged and preference-as-ruled
  disagree, and precisely the thing worth a decision sheet.

## 4. Unit tests (the one specific part: the disposition-risk path)

- **t1 selection-moves**: fixture Q(o|π) pairs constructed to differ only in
  predicted disposition; disposition-risk finite and ordering flips the
  selected policy. (The seed demonstrably ACTS.)
- **t2 refusal**: absent seeded-C ⇒ AC7-style refusal record — no choice, no
  invented masses — matching the aif_sampling contract (U75).
- **t3 zeros**: named-zero dispositions in support; KL well-defined; removing
  one from support is the planted control and must refuse by name.
- **t4 kernel-from-records**: P(d|o) fitted from a PINNED cohort-ledger
  fixture reproduces a hand-computed conditional (live-pin rule: pins
  captured verbatim from ledger.edn, one per test).
- **t5 fold-layer**: :ruled-outcome-c enters the ruled sum in declared order;
  :folded? flips true citing Item 6; layer removable by config and its
  absence changes G (no silent no-op layer).
- **t6 extractor-provenance**: over a pinned rulings-file fragment, proposed-C
  components carry the expected citations; a seeded disagreement emits a
  decision-sheet row and writes nothing.

## 5. Slices (one file / one behavior each)

1. Disposition kernel from cohort records + t4 (discovery-from-records first:
   it derisk everything downstream).
2. disposition-risk + fold layer + t1/t3/t5 (the F10 fold slice; discharges
   the rider's computation half).
3. AC7 refusal path + t2.
4. Preference extractor + t6 (the discovery loop's first turn; its decision
   sheet is the next C conversation with Joe — held to route-by-standing-
   principle discipline: only genuine preference questions surface).

Slice 2 depends on 1; slices 3 and 4 are orthogonal to each other and to 2.
After slice 2, `preferenceStackLiveRecorded` has its live path and the F10
rider run becomes schedulable.
