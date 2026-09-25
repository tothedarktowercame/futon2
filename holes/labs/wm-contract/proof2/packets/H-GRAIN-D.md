# H-GRAIN-D — grain as data, and the check that would have caught click-001's first enactment

Author: kimi-4 (E-kimi-task-16), 2026-09-24. Read-only discovery packet for
PROOF-2a hole **H-grain** ("choose the grain, with a check before enactment;
click-001's first enactment built the wrong grain"). No code edits. Sources
cited inline; all offsets in the click record are its declared unit
(`:offset-unit :unicode-codepoints-zero-based-end-exclusive`).

Sources:
- futon3c `holes/labs/M-futon-seams/exemplar/click-001.edn` (the decision; claude-1)
- futon3c `holes/labs/M-futon-seams/exemplar/click-001-enactment.edn` (the accepted enactment; claude-10)
- futon3c `holes/labs/M-futon-seams/exemplar/click-001-outcome.edn` (the first attempt, kept as the falsifier; claude-10)
- futon3c `holes/missions/M-futon-seams.md` (the mission)
- futon2 `holes/labs/wm-contract/PROOF-2a-THEOREM-draft-2026-09-24.md` (Clause T, Clause C)

---

## (1) First attempt: the grain built vs the grain the candidate named

**The grain the candidate named.** The chosen candidate
`:cand/a-registry-first` carries the pattern
`cascade-construction/choose-the-grain-where-state-lives`, whose receipt
reading in click-001.edn is:

> "The state one dispatch changes lives at the role, not the seat and not the provider."

The accepted enactment restates the chosen grain at `:grain`:

> `:keyed-by :role` — "The resolver takes a ROLE and answers which seat plays
> it. That is the grain instance-4.edn chose; the first attempt built a lookup
> keyed by an agent id, which is the provider grain."
> evidence: `futon3c.agency.roles/seat-for`, arglist `[role]`.

**The grain the first attempt built.** click-001-outcome.edn `:grain`:

> `:keyed-by :agent-id` — "THE FIRST ATTEMPT, kept as the falsifier for
> grain_check.py. futon3c.agency.roles/provider takes an AGENT ID and answers
> which provider it is -- a provider lookup, not a role resolver. The cascade
> chose :role. This record is what the check must refuse."
> evidence: `futon3c.agency.roles/provider`, arglist `[agent-id]`, commit `8e5c431e`.

The mission records the same mismatch (§Working out of order):

> "the enactment at futon3c `8e5c431e` built the **provider** grain where the
> chosen cascade had specified the **role** grain, because the implementer
> worked from this mission's defect description rather than from a DERIVE that
> had met its exit. All three of instance 4's wants came back `:partial`."

And §DERIVE step 3 states why nothing caught it:

> "the first enactment read the *defect description* in §4 instead … and built
> a provider lookup. Nothing compared the two until all three wants came back
> `:partial`."

So: candidate grain = **role** (`seat-for [role]`); first-attempt grain =
**agent-id → provider** (`provider [agent-id]`). Same defect (id-derived
routing), different unit of change: the first attempt still answers "which
provider is this agent", which is the provider keyed by the agent id.

**The mission's own sentence choosing its grain** — the IDENTIFY exit
(M-futon-seams.md, line 161):

> "Pick one instance and declare its interface — not all eight."

This is the *scope* half of the grain (one instance, not all eight); the
*unit* half was chosen in DERIVE step 3 ("For instance 4: **`:role`**, not
the seat and not the provider").

---

## (2) Grain as data

The worked example already converged on a record shape; both the outcome
(falsifier) and the enactment (accepted) carry a `:grain` map of the same
schema, and `scripts/grain_check.py` consumes it. Generalising, a candidate's
chosen grain and an enactment's declared grain are both values of one type:

```
Grain :=
  {:keyed-by    keyword            ; the unit of change: what the resolver/
                                   ;  lookup/dispatch is keyed by.
                                   ;  Observed values: :role :agent-id;
                                   ;  mission also declares :room :fragment :turn.
                                   ;  This is THE comparable field.
   :statement   string             ; one sentence: what a lookup at this grain
                                   ;  answers ("takes a ROLE and answers which
                                   ;  seat plays it")
   :evidence    {:fn       string  ; fully-qualified resolver that embodies it
                 :path     string  ; file, repo-relative
                 :arglist  string  ; e.g. "[role]" — the key is visible here
                 :sha256   string} ; pins the file's bytes (absent in the
                                   ;  falsifier's evidence — see refusals)
   :checked-by  string             ; the check that adjudicates this record
   :source      {:path ... :sha256 ... :span [start end]} ; where the choice
                                   ;  was declared (cascade file + span), so a
                                   ;  later edit shows as drift
   :scope       {:instances #{:inst/i4}  ; one instance vs all eight
                 :source-cue [4399 4421] ; the mission sentence above
                 }}
```

Notes on the fields, each grounded in the record:

- **`:keyed-by` is the mechanical comparator.** The mission's own finding:
  "a resolver's grain is *what its lookup is keyed by*, and that this is
  visible in its argument list: `(defn seat-for [role])` / `(defn provider
  [agent-id])`". Two grain records are compared on `:keyed-by` first; the
  arglist of `:evidence :fn` is the ground-truth witness for it.
- **`:evidence` is what stops the check being "two agents agreeing with each
  other"** (mission §DERIVE step 3): the resolver must exist, with the
  recorded arglist, in the file at the recorded sha256. The falsifier's
  evidence carries no `:sha256` — itself a typed gap the check can refuse on.
- **`:scope` separates the two choices the mission makes.** "Pick one
  instance … not all eight" is scope; "at the role" is the unit. click-001's
  failure was in the unit, not the scope. Keeping them distinct fields is
  what lets the check say *which* half mismatched.
- **Unit of change vocabulary** is larger than `:keyed-by` covers in general
  (file / declaration / instance / prompt text are the mission's cost
  ordering kinds). For H-grain the operative axis is the resolver key;
  `:kind` (code / prompt-text / declaration-first) belongs to Clause T's
  cost-ordering entry, not to the grain record. Recorded here so the two are
  not conflated later.

---

## (3) The pre-enactment check

The mission already has the post-hoc form: `scripts/grain_check.py`, "three
comparisons rather than one" — cascade declares its grain; enactment declares
the grain it was built at with a resolver as evidence; the resolver must
exist with the recorded arglist at the recorded sha256. The H-grain hole asks
for the same predicate evaluated **before** the attempt is committed, over
the planned attempt rather than the realised one.

Signature:

```clojure
(defn grain-gate
  "Pre-enactment check for the grain step of a selected candidate.
   candidate       — the selected candidate derivation (its
                     choose-the-grain interpretation's declared Grain)
   planned-attempt — {:grain Grain :files [..] :commit-plan ..} as the
                     implementer intends to enact it
   repo            — {:root path :pinned-sha sha} the check reads evidence against
   => {:status :pass}
   |  {:status :refuse
       :reason :grain-mismatch        ; candidate :keyed-by ≠ attempt :keyed-by
              :grain-not-declared     ; attempt (or candidate) carries no :grain
              :scope-mismatch         ; attempt touches instances outside :scope
              :resolver-missing       ; :evidence :fn not found at :path
              :arglist-mismatch       ; resolver's arglist ≠ recorded :arglist
                                      ;  (the key it claims is not the key it has)
              :sha-mismatch           ; file bytes at :path ≠ :evidence :sha256
       :detail {...}}"
  [candidate planned-attempt repo] ...)
```

Each refusal is typed; no refusal substitutes a value (a missing grain is
`:grain-not-declared`, never an assumed grain). On the worked example:

- **First attempt** (`click-001-outcome.edn :grain` as the planned attempt,
  `:keyed-by :agent-id` vs candidate's `:role`): refuses `:grain-mismatch`.
  This is exactly grain_check.py's recorded failure line:
  "cascade grain role, enacted grain agent-id / FAIL GRAIN MISMATCH".
- **Accepted enactment** (`click-001-enactment.edn :grain`, `:keyed-by :role`,
  `seat-for [role]` at pinned sha256 `617f7fe8…`): passes all three
  comparisons.

Order of evaluation matters for attribution: `:grain-not-declared` first
(absence), then `:grain-mismatch` (the comparable), then the evidence
refusals (existence, arglist, sha) which adjudicate that the declared grain
is the built grain rather than an agreed fiction.

---

## (4) W_c or a new condition?

**A new condition — a pre-enactment gate G_c — feeding W_c, not a clause of
W_c itself.** The argument, from the definitions:

1. **W_c is defined over the record of attempts that exist.** Its text:
   "the enactment record lists every attempt at a pattern step … Every
   pattern of the selected candidate has a successful attempt; no attempt
   names a pattern outside it; an attempt claims only a token its pattern
   produces." Every condition in W_c quantifies over `:attempts` — entries
   written *after* the work. W_c can *detect* the grain failure (X_c(a):
   "The first attempt alone (a change at another grain than the one chosen);
   W_c must fail" — and it does, futon3c `401469fd`), but detection lands
   where click-001's detection landed: after the wrong-grain commit
   `8e5c431e` existed and all three wants read `:partial`. Clause T is
   explicit about this asymmetry for its own condition ("Wₜ is a witness
   condition, not a gate: a click that fails it still runs; it is not
   credited"); W_c has the same witness shape.
2. **The hole asks for the failure to not happen.** H-grain's wording is
   "a check *before enactment*". A predicate evaluated before the attempt is
   committed has no attempts to quantify over, so it cannot be W_c as
   defined. Its input (candidate × planned attempt) is not W_c's input
   (candidate × enactment record).
3. **But it is not a new clause either.** Its data is exactly the `:grain`
   maps W_c's record already carries (P_c builds `[:enactment]` beside the
   click record), and its post-hoc shadow is the success-adjudication of the
   `choose-the-grain` attempt inside W_c — grain_check.py is what made
   attempt 1's `:success false` a finding rather than an opinion. So the
   right shape is: **G_c, a gate with the signature of §(3), whose pass is a
   precondition for the grain step's attempt being committed, and whose
   refusal is recorded on the enactment as a typed absence**; W_c is amended
   by one sentence — for the candidate's grain pattern, a successful attempt
   must name G_c's pass as its check. X_c gains a fourth mode: an enactment
   whose grain attempt carries no G_c pass must fail W_c.

   This mirrors what the mission already concluded operationally ("the
   mistake had to be made before the check existed; nothing in the loop
   checked the enactment against the cascade before it was committed" —
   outcome `:deviation :why`) and what the outcome file records as the
   finding: "PROOF-2a has no clause that binds the enacted change to the
   chosen candidate." W_c (added as Clause C) binds them after the fact;
   G_c binds them before.

### Proposed amendment text (for PROOF-2a, Clause C)

> **G_c (gate).** Before the first attempt at the selected candidate's grain
> pattern is committed, `grain-gate` (candidate, planned attempt, repo) must
> return `{:status :pass}`. A refusal is one of `:grain-mismatch`,
> `:grain-not-declared`, `:scope-mismatch`, `:resolver-missing`,
> `:arglist-mismatch`, `:sha-mismatch`, recorded on the enactment with its
> `:detail`. An enactment whose grain attempt names no G_c pass fails W_c.
> Witnessed by: `click-001-outcome.edn` refuses `:grain-mismatch`;
> `click-001-enactment.edn` passes.

---

## Findings and open edges

- The `:grain` record schema exists in two conforming instances (outcome and
  enactment) and one consumer (`scripts/grain_check.py`); §(2) is a
  generalisation of observed practice, not an invention. The falsifier's
  evidence lacking `:sha256` is a real typed gap the gate should refuse on.
- `:scope` (one instance vs all eight) has no check today; the mission chose
  it by sentence and nothing adjudicates an enactment that strays. Recorded
  as a smaller sibling hole; G_c's `:scope-mismatch` refusal is specified but
  unwitnessed.
- G_c presupposes the candidate *declares* a grain. 4b-style candidates
  ("does not name a grain", mission line 602) refuse `:grain-not-declared` —
  which is the desired behaviour per the mission's own comparison of 4a vs 4b.
- This packet is discovery only: no amendment applied, no code written.
