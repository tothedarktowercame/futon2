# FLIGHT-TARGET-D — which targets have an open want the machine can produce now; D13 withdrawals as data

Discovery packet, read-only. Author: kimi-6, 2026-09-24. Anchors: futon2
`610498a5` (packet-time HEAD; sources and evidence read at it), the 09-24 scan
report `data/wm-runs/tick-run-record-2026-09-24-1790225596.scan.md`, and
`holes/E-cascade-real.md` findings D6/D9/D11/D13. "Open want" and "completion
criterion" are used per `proof2/packets/H-EXITS-D.md`; "constructible" means
`interpretation_construction/construct` (`src/futon2/aif/interpretation_construction.clj:101`)
run offline on the target's declared sources, the `test/fixtures/click2-replay/`
method (manifest `:method`: `load-declared` → `problem-tokens` → `:unknown` read as
not-established, declared horizon, fixture budget, injected `:evaluate-g`).

## 1. The admitted set

The scan report's decision section lists 276 `universe-not-admitted` targets and
abstains. The four targets **not** in the not-admitted list are exactly the four
declared-source targets with locators: `M-aif-policy-conditioned-eig`,
`M-f11-find-production-successor`, `M-wm-08-external-f2`, `T-repair-occ-444fb018…`.
The fifth declared source file, `M-expressions-of-interest`, **is** in the
not-admitted list — consistent with its `:locators {}` (§2.3): with no locators its
universe cannot be observed, and `construct` refuses an unobserved token set with
`:observation-required` rather than reading absence as false. Of the 248 substrate
targets I2 surveyed, 245 have no declared interpretations at all (D11), so nothing is
constructible over them regardless of their wants; the three that have any are
covered by D6's finding that the declarations do not aim at the remaining false
wants. The substrate therefore contributes no survivor; the census below is the four
admitted targets plus the fifth declared file.

## 2. Census: open want × producer × blocker

### 2.1 T-repair-occ-444fb018… — no open want (excluded)

`:want [:restoration-accepted]`, locator `{:class :C4 … :path "holes/tickets/T-repair-occ-444fb018….md" :decl "**Status:** DONE"}`.
The ticket's L3 today reads `**Status:** DONE` (changed by the 09-23 click's own
build commit `97e17e10`, E-cascade-real D10). Its only want is already true; all
intermediate locators' files (`resources/wm/rechecks/repair-occ-444fb018-dated-recheck.edn`,
`resources/wm/eig/held-out-{split,observations,calibration}.edn`) exist at HEAD. A
flight here is the CLICK2-D bad case: wants already true, nothing to construct.

### 2.2 M-wm-08-external-f2 — no open want (excluded)

`:want [:route-a-rehearsal-reported]`, locator `{:class :C3 :sha "HEAD" :path
"holes/labs/wm-contract/runs/wm-08-external-f2-2026-09-16/ROUTE-A-REHEARSAL.md"}`.
That file exists at HEAD. The want is satisfied; the source file's own header
anticipated this ("The machine's own success exhausts its work supply", D5/D6
context). Excluded.

### 2.3 M-expressions-of-interest — open wants, producers declared, no locators (blocked on observability)

`:want [:change-authored-and-bound :premise-refused-before-work
:obligation-resolved-through-the-account]`, three declared patterns producing all
three, two hand-admitted candidates — but `:locators {}` and the header states the
artifacts are prospective: "This declaration does not create them." So (a) open
wants: yes in intent, unverifiable; (b) producer: declared; (c) blocker: no locator
can witness any want (typed absence `:locators-not-declared`), and the underlying
artifacts may not exist either — the file does not say. Needs, in order: locators
(one per want, C6-style presence checks the header already gestures at), then a check
of whether the artifacts exist. Rank below M-f11 because the missing piece is
observability of the whole universe, not one producer.

### 2.4 M-f11-find-production-successor — one open want, no producer (the smallest gap)

`:want [:hole/h9ab212b3281d :hole/h2045faa0e7cc]` with HEAD checkbox locators into
`holes/missions/M-f11-find-production-successor.md`. At HEAD: L101 `- [x] Complete
F11's ordinary acceptance…` (h9ab212b3281d, satisfied) and L102 `- [ ] Publish the
strict successful successor link for repair-024, or retain the typed failure without
resolution.` (h2045faa0e7cc, **open**). (a) Open want: yes, observable, faithful —
the checkbox *is* the mission's stated form and the item's own text conditions
closure on an either/or the box tracks. (b) Producer: **none** — the only declared
pattern, `:apparatus/done-is-observed-running`, produces the already-true
h9ab212b3281d; D6 records the constructor reporting the false want as a typed
`:no-producer` finding, and no existing interpretation names h2045faa0e7cc, so
`construct` on the current sources cannot reach it. (c) Blockers: no withdrawal, no
evidence dependency; the want's disjunctive text ("publish the link, **or** retain
the typed failure") means even the produce side has a stop-shaped success. **Gap to
first constructible click: exactly one interpretation with receipt whose `:produces`
contains `:hole/h2045faa0e7cc`.** This is the same shape as the P1 probe's
agent-authored interpretations; nothing in the target needs wall-clock evidence.

### 2.5 M-aif-policy-conditioned-eig — one open want, producer exists but its route is withdrawn (not a fair first flight)

`:want [:hole/h6378c65a4012 :hole/h0e270aa090bc :hole/h42fceb4ad48b]`. At HEAD the
first two read `- [x]` (mission L195, L214 — satisfied); the open want is
`:hole/h42fceb4ad48b`, the `- [ ]` shadow/calibration item at mission L245. Its only
declared producer is `:aif/two-layer-calibration`; the candidate
`[:aif/two-layer-calibration]` was withdrawn by claude-3 on 2026-09-22 in a **source
comment** (`resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn` L91–96,
after run 2026-09-22-1790053967: reviewer REQUEST_CHANGES, author REFUSE
`:prospective-held-out-evidence-unavailable`; "It won every G tie by name order, so
it would be selected and refused on every click. Restore when that evidence has a
locator."). I2 confirmed the constructor rebuilds exactly this route from the
surviving interpretation and that the judge's admission check passes it (D6) — so at
891b4af6 a click selects it and the author refuses again.

**What evidence the withdrawn route needs, exactly.** From the withdrawal comment,
the mission item's text (L245–253), and its 2026-09-22 implementation-advance note:
(i) a **preregistered** held-out split for the mission's EIG shadow — declaration
before outcomes; (ii) **post-split** held-out outcomes persisted as the empirical
packet; (iii) the packet under `holes/labs/M-aif-policy-conditioned-eig/` — the
artifact directory §4 of the mission names, which **does not exist at HEAD**
(measured 2026-09-05 in the item text; still absent today). The reviewer-shaped
question D9 left open — "whether `resources/wm/eig/held-out-calibration.edn` is the
preregistered split and post-split outcomes the reviewer asked for" — is settled by
reading the files: `held-out-split.edn` carries `:ticket/id
"T-repair-occ-444fb018…"` and `:model {:policy-family :wm/pattern-cascade}`, and
`held-out-calibration.edn` likewise carries the ticket id. They are the **ticket's**
evidence (zai-1, for `:repair/calibration-evidence-present`), scoped to the repair
target's policy family, not the mission's shadow packet; the mission item's own note
names a different artifact location. Typed answer to D9's open check: the restore
condition's evidence exists **for another target**; for this mission it is
`:evidence-scoped-to-other-target`, and the withdrawal stands.

**Can any of it be produced within a click?** (i) yes in principle — a split
declaration is a document a click can author, and it is progress under flight rule 2
(a token a later click consumes). (ii) and (iii) no: post-split outcomes require
held-out runs that occur after the declaration, wall-clock by construction, and the
restore condition ("that evidence has a locator") cannot be observed met until they
land. So on this target the flight's first click would be the preregistration, not
the want — correct work, but it does not exercise closure, and every click until the
evidence exists re-selects a refused route unless D13 lands.

## 3. Ranking (machine needs beyond what exists today)

1. **M-f11-find-production-successor** — one interpretation + receipt producing
   `:hole/h2045faa0e7cc`. No withdrawal, no evidence dependency, observable and
   faithful want, constructor already verified to report the gap cleanly. Smallest
   gap: one agent-authored interpretation.
2. **M-expressions-of-interest** — needs three locators (and admission into the
   universe) before anything is observable; possibly the artifacts too. Producers
   already declared.
3. **M-aif-policy-conditioned-eig** — needs D13 (else the withdrawn route is
   re-selected every click), then a preregistration click, then wall-clock held-out
   evidence. Not a fair first flight for closure; fair for a progress-token flight
   once D13 lands.
4. **M-wm-08-external-f2**, **T-repair-occ-444fb018…** — no open want; excluded.

Recommendation for the first flight target: **M-f11**, with the one missing
interpretation authored under the P1 method (two seats, loader + constructor +
admission scoring) before the flight starts; M-aif-policy-conditioned-eig becomes
fair once D13 is data and its preregistration click has run.

## 4. D13 as an amendment: withdrawals as data

Today a withdrawal is a comment; the constructor's guard vocabulary
(`:needs`/`:forbids`/`:produces`) cannot say "not until this evidence exists" (D13,
I2 §5 gap 6), and D9 shows the failure both ways: a met restore condition went
unnoticed (for the ticket) and an unmet one is unreadable (for the mission).
Proposed shape, additive to `:wm/cascade-source-v1` (a reader that knows it, uses
it; one that doesn't, behaves as today — which is precisely the defect being
repaired, so adoption must be gated on the reader, see rule 4):

```clojure
:withdrawals
[{:withdrawal/id :w/2026-09-22-two-layer-calibration
  :applies-to {:pattern :aif/two-layer-calibration
               :produces :hole/h42fceb4ad48b}    ; grain: the production edge,
                                                 ; not the pattern everywhere
  :by "claude-3" :at "2026-09-22"
  :reason :prospective-held-out-evidence-unavailable
  :evidence {:run "2026-09-22-1790053967" :review :request-changes}
  :restore-when [{:class :C3 :repo "futon2" :sha "HEAD"
                  :path "holes/labs/M-aif-policy-conditioned-eig/<split>.edn"}
                 {:class :C3 :repo "futon2" :sha "HEAD"
                  :path "holes/labs/M-aif-policy-conditioned-eig/<packet>.edn"}]
  :restore-observed nil}]                        ; filled by observation, never by hand
```

Rules:

1. `:restore-when` is a vector of locators in the **existing** check classes (C3/C4
   at HEAD, as M-wm-08's want locator already is) — so the restore condition is
   observable by `observation-checks` with no new machinery. A withdrawal whose
   restore condition cannot be expressed this way is refused at load with
   `:restore-condition-not-observable`; that refusal is the amendment's answer to
   "restore when Joe feels differently" — not expressible, therefore not data.
2. The constructor excludes any plan using a withdrawn production edge while any of
   its `:restore-when` locators observes false, and reports the exclusion as a typed
   finding naming the unmet locators — so the flight sees *what evidence would open
   the route* instead of re-selecting a refused route (the D6/D13 defect) or losing
   the route silently.
3. When every `:restore-when` locator observes true, the edge is eligible again and
   `:restore-observed` records the observation (locator + resolved sha + date). A
   restore is an observation, never an edit — D9's "nothing noticed" is the bug this
   field exists to kill.
4. Admission treats an unexpired withdrawal as `universe-not-admitted` for the
   affected edge, with the withdrawal id as the reason, so a source file read by an
   old reader (which ignores the key and would re-admit the route) fails closed at
   admission rather than silently: the withdrawal is declared in the source, the
   reader version is declared in the receipt, and a `:wm/cascade-source-v1` reader
   loading a file carrying `:withdrawals` must refuse `:schema-requires-v2-reader`.
   Without rule 4 the amendment re-introduces the defect for every stale reader.

Worked instance: the claude-3 withdrawal above, expressed, would have (a) prevented
I2's constructor from returning `[:aif/two-layer-calibration]` as a live candidate
(it would instead report `:edge-withdrawn :restore-when […unmet…]`), and (b) made
the 2026-09-23 publication of the ticket-scoped held-out files a *non-event* for
this route — observable as scoped to another target, leaving the withdrawal standing,
which is the correct current state.
