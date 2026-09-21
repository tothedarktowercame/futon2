# improve-7 — discovered focus, outcome domain, and two timescales

Discovery only, codex-12, 2026-09-21. Branch `fix/narrative-improve-7`,
base `40762056`. No production changes, clicks, serving-JVM evaluation, or
source-store writes. Reproduction files: [improve-7-evidence](improve-7-evidence/).

**Finding:** recorded work strongly identifies the WM implementation programme
on both days. The old structural embedding does not identify WM and APM as one
region. Joe's explicit semantic connection supplies information that this graph
lacks. Redistributing preference probability over the existing token powerset
is computable but is not yet his preference over attested increments: it can
strongly prefer a policy that predicts no new wanted token. Focus discovery,
outcome semantics and long-run occupancy must be recorded separately.

## 0. Answers changed during this discovery

Read Q2, the claude-5 relay and Q3 before investigating. Subsequently read and
pinned the full [answers at `94230d57`](improve-7-evidence/answers-at-94230d57.md):
`2181ac4d` confirms **55% focus, 35% associated, 5% useful elsewhere, 5% known
non-delivery**; `4526e927` distinguishes global occupancy from persistent local
focus; `94230d57` adds repair/incremental/greenfield occupancy. These supersede
the initial commission's 60/40/0 and the two unanswered questions.

“Known non-delivery” requires an observed typed failure/stop-the-line. It is not
an unchanged prediction, an unobserved result, or a scheduled allocation of time
to fail. “Elsewhere” includes useful interruptions; an arbitrary teleport is not
automatically entitled to that 5%. Class odds are 11:7:1:1; per-token odds and a
transition rate between classes do not follow from those four numbers.

The replay below keeps the originally requested 60/40/0 and epsilon cases as
**counterfactual controls**, and labels the 55/35/5/5 token-powerset arm as a
counterexample, not an implementation of the clarified ruling.

## 1. Available focus evidence and a declared discovery rule

### What is recorded

| Evidence | Fresh inspection | What it can establish / missing part |
|---|---|---|
| Git across futon0, futon2, futon3c, futon4, futon5a, futon6, mathlib4, apm-lean | Immutable head pins and every counted commit in `discovery.json` | Changed mission files, implementation paths and commit time; not active attention or uncommitted proof work. No merges counted. |
| `mission-structure-embed/{mission-embed.json,structure-embeddings.npy}` | **198 stems × 161 structural features**; files have June 25 mtimes | Scope-structure similarity, not a fresh semantic model of today's missions. Updater and G-wm-wiring absent. Mtime is not authenticated build provenance. |
| `mission-carpet-pos-embed.json` | **347** 2-D positions, Sept 21 mtime | Display coordinates; neither authoritative cosine distance nor a time series. |
| `fold-embed/manifest.json`, nodes/edges/pairs | **78,278 nodes**, **263,761 edges**, 309 mission nodes, 23 paired examples; July 1 mtime | Mission→pattern/var substrate: 1,783 uses-pattern and 99,013 touches edges; 89,321 calls, 73,644 contains. It is not a retained day-by-day work graph or a fitted focus process. Old/fork-qualified identities require joins. |
| `mission-wholeness.edn` | **602** rows, Sept 21 snapshot | Current mission-scope L/T/H, not time spent. Full source digest and selected rows retained. Historical snapshot for Sept 20 was not established. |
| Frozen WM runs/checkpoints | Exact selected action, modeled tokens, implementation/review evidence and outcome | Work on a specific attempt; prediction/selection is not completion or operator attention. |
| Agency jobs and bell traffic | Existing Sept 21 accounting survey: 2,199 seven-day jobs, 1,320 bells, 756 auto-bellbacks, 38 whistles | Rich potential activity evidence, but topic/mission join is not canonical. Survey found only 20 volatile mesh rows and **0 durable mesh-edge rows**, with session/edge identity gaps. Do not infer silence from that graph. |
| Emacs/session surfaces | `futon4/dev/codex-session-log.org`, `arxana-window-constraints.el`; Agency job/session envelope fields | A session log and layout-validation code exist. Filename searches of futon4/futon3c data did not identify a dated buffer/window residence series. No claim to have observed today's or yesterday's complete layout. |

Sources for the Agency figures and limitations:
`futon3c/holes/NOTE-agency-accounting-gaps-2026-09-21.md:54–69,198–250`.
This discovery did not query live endpoints. `NOTE-joes-view-of-C.md §3` names
L, mission closure and capability stars as the starter sources. Its Sept 18
“zero production requirers” statement is historical: live C is now consumed.
`live_c.clj:1–39,81ff` describes the existing normalized source budget and pins.

### Proposed rule `commit-facets-v1` (record-only, not ratified)

1. At a UTC cutoff, read a pinned 24-hour commit window on declared repository
   heads. Attribute changed paths to named facets using the exact regexes in
   `discover.py`: `aif`, `wm-contract`, `DarkTower/WarMachine`, WM mission/runner
   names → WM; APM implementation/problem/mission paths → APM. Split one unit
   of credit across matched facets; retain unmatched credit. This discovers
   **which declared topic features are active**, not an unconstrained ontology.
2. With no previous focus receipt, bootstrap from the largest credit facet,
   lexicographic tie-break. Evidence validity ends at the next UTC day or an
   earlier declared source/meaning revision. A new receipt refreshes evidence;
   expiry does **not** authorize a random new focus.
3. Once bootstrapped, retain the unfinished focus until an authenticated
   programme-completion receipt or an explicit warranted move changes it.
   Missing completion means “not established,” not “finished.” Losing activity
   for a day does not finish months of work. A paused focus remains identified
   but is not an instruction to execute through a pause. Today's replay carries
   yesterday's discovered focus; both independently bootstrap to the same one.
4. Expand a facet only along declared semantic-facet edges or accepted
   mission/dependency edges. Embedding neighbors suggest edges for review,
   never make the work applicable. Each edge needs endpoints, source span,
   content hashes, author/authority, validity interval and relation type.

Applied committed-work evidence (the script records every contributing SHA):

| Cutoff/window | Commits | WM credit | Other/unattributed | APM credit | Discovered focus |
|---|---:|---:|---:|---:|---|
| Sept 20 UTC, through Sept 21 00:00 | 165 | 151 | 14 | 0 | WM implementation programme |
| Sept 21 UTC, through 17:31:44 | 166 | 157 | 9 | 0 | WM implementation programme, retained |

The lack of counted APM commits does not say no agent was working on APM.
Only committed work on these pinned heads was measured. Yesterday's most-touched
mission document was the WMC spinoff (4 commits); this **does not make WMC the
focus**: the larger implementation-path region is WM, and WMC's own declaration
(`holes/M-a-wmc-scaling.md:4–16`) explicitly parks activation behind triggers.
The old `futon3c/holes/missions/M-war-machine.md:2–5` says both “parked” and
“IDENTIFY,” last modified May 4. Do not equate that stale root document with a
completion of the presently active contract programme.

**WM+APM under this rule:** not from embedding proximity alone. Cosine
WM/APM-solutions = **−0.011879916**. WM's top neighbors are prelim-practice
(.421292), sorry-enterprise (.324365), recommendation-bindings (.307555),
learning-loop (.296460), web-arxana-missions (.241556). APM's closest are
symbol-grounding (.920086) and differentiable-code (.904730). These are
structural features and fail Joe's broader semantic grouping.

At the current cutoff, Q3 in `40762056` supplies a source-attributed facet edge
WM↔APM: they can now be recorded as one overarching focus with a WM-active
facet and an APM-background facet. For Sept 20, that Sept 21 statement cannot be
claimed as evidence available then. Strict as-of discovery finds WM, with APM
separate/unestablished; applying Joe's newly supplied grouping retrospectively
is an explicitly marked retrospective interpretation. The numerical experiment
uses this current grouping, **not a claim that the old selectors knew it**.

### Salingaros over the focus region

Current rows: WM L=53.1 (T=9,H=5.9), WM-wiring 72 (9,8), APM-solutions 57.6
(8,7.2), APM-ratchet 44.8 (8,5.6), updater 44.8 (8,5.6), F11 15 (3,5),
WMC 14.1 (3,4.7). These are document-structure measurements, not focus shares.
Do not add their L values and call the result attention or a region's L.

`futon6/scripts/mission_wholeness.py:35–95` defines centres from scopes, size
from leaf concepts, T from articulation/differentiation/multi-depth branching/
kind diversity/complementarity, H from distinctive concepts threading centres,
and L=T·H. To measure a focus spanning windows/sessions/missions, retain dated
window→buffer→file/mission identities, selected-window residence intervals,
agent-session→job→mission/occurrence joins and parent/child/handoff edges, with
missing and concurrent intervals explicit. Build a versioned multiscale centre
graph and concept-reinforcement edges; then declare its T/H interpretation and
calibration. Today's mission trees supply one scale. The cross-scale window and
session observations are missing, so **region L and attention shares are held**.

## 2. Adjacency and warranted moves

A reproducible proposed neighbor generator: top-five cosine neighbors in the
pinned 161-dimensional structural space, cosine ≥ .25, stable stem tie-break;
record missing nodes. Treat this as **candidate adjacency**, not authorization.
An explicit mission→excursion, prerequisite, successor or accepted connection
edge can establish adjacency independently of embedding distance. Require its
source/version and named purpose (needed to finish the focus, or a specified
background thread), activation conditions and expiry. No transitive closure
through arbitrary “see also” links: that would connect almost the whole stack.

Examples: WMC is a successor with explicit activation gates, not immediately
active work; action-cost-modelling's title and §HEAD expressly name the WM and
an excursion responding to its prioritization gap; APM-ratchet cross-references
APM-solutions, the learning loop and `E-capability-graph-ui-polish`. An EOI
bibliographic mention of M-war-machine at line 1153 is **not** a prerequisite
edge. Joe's Q1 IAD answer establishes a situational connection to the process
preference work; it does not activate every institution everywhere.

A move warrant is a retained connection/transition receipt: from/to focus,
reason and source span, needed artifact or background objective, expected return
or completion condition, issuer/authority, time and evidence hashes, plus the
actual next occurrence referencing it. An attested execution/test-registry
warrant can substantiate an artifact in that reason; a passing test alone does
not establish why a topic switch was needed. Retrieval rank, spatial distance,
a bell saying “do this,” and a successful commit have distinct authority roles.
Record unclassified separately from positively irrelevant; **absence of an
adjacency receipt must not become a hard-zero ban**.

## 3. Global (unconditioned) C sensitivity and local conditioned replay

### Candidate classification used in the experiment

This small table is a declared, hand-reviewed diagnostic interpretation of the
current focus, not an automatically admitted applicability witness. All full
candidate rows (including C0s) and all wanted-token odds are in `replay.edn`.

| Class | Targets in the two frozen menus |
|---|---|
| Focus | M-aif-policy-conditioned-eig; M-f11-find-production-successor; M-wm-08-external-f2; M-G-wm-wiring; M-wm-aif-policy-grain-compliance; M-apm-capability-ratchet (current Joe facet edge) |
| Adjacent | M-action-cost-modelling (WM excursion); M-futonzero-generative (WM learner); M-aif4iad (situational process-preference connection) |
| Off-focus/unwarranted, simulated as “irrelevant” only for sensitivity | M-expressions-of-interest; M-dionysus-winddown; M-daily-scan; M-distributed-frontiermath; M-kangaroo; M-chipwitz-corps; M-canon-fingerprint-store; M-self-documenting-stack; M-usage-hacking; M-futonzero-mvp; M-federated-agency-hardening; M-futon-forward-model |

The last row is not a finding that these missions are worthless. For example,
Agency could be required by a particular WM problem; no particular requirement
is manufactured here. Classifying new wants by these target labels gives latest
5 focus new wants; earlier 40 focus, 18 adjacent, 61 off-focus. F2's want is
already true, so its cascade's terminal state belongs to **no new want**, even
though its target is focus. Earlier C0s also produce no new wants.

### Probability construction — a diagnostic, not merely utility scaling

Use the frozen global qualified-token universe U, initial state s0, original
terminal log weights w (including explicitly retained fallback weights), and
new-want coordinates W minus s0. Partition full states as follows: any new
irrelevant token → I; otherwise any focus token → F; otherwise any adjacent
token → A; otherwise N (no new want). Mixed irrelevant delivery remains I in
this diagnostic, so a hard zero cannot be escaped by adding one focus token.
This priority rule is an explicit modeling choice.

For class k set **C(s)=m_k exp(Σ[t∈s]w_t)/Z_k**, with Z_k the sum over *every*
state in class k. Thus within a class, the retained live log-weight law is
unchanged; class probabilities are genuine masses, not .6/.4 log utilities.
This preserves source weighting through the existing exponential law. A direct
categorical distribution proportional to raw source weights is a different
outcome domain; it must not be silently substituted.

Analytic full normalizers, writing B_X=∏[t∈X](1+exp(w_t)) for fresh-want class
X and B_R for remaining coordinates:

```
Z_I = B_R (B_I−1) B_F B_A
Z_F = B_R (B_F−1) B_A
Z_A = B_R (B_A−1)
Z_N = B_R
```

The latest menu has **no adjacent outcome coordinates**. Assigning .4 to that
class on its unchanged token powerset is impossible. The diagnostic explicitly
extends the carrier by an unrepresented-class atom with zero Q mass; it reserves
that .4 rather than donating it to the available candidates. This is a support
extension for arithmetic, not an invented observed mission or a live model.
With epsilon it also reserves an irrelevant atom. Earlier all classes exist.
A production receipt should hold an unrepresented class until its outcome-domain
mapping is declared. Conditioning only on the observed menu would silently
change Joe's budget when enumeration changes.

Earlier C stays uniform; B, q0, A, horizon, E, F, β=1, action marginalization and
tie-break remain frozen. The probe runs the actual `horizon-g-sparse` with a
step-indexed pointwise C, `selection-posterior`, Bayes action projection and
fix-7 `selection-comparisons`. It reproduces all 27 baseline Gs and posterior
entries within 1e-8 first. Latest powerset normalization is also checked by
explicit enumeration of all 2^11 states plus reserved mass.

### Latest run 1789964661

A=updater C1, F=F11 C1, W=F2 C1. Triples are in A/F/W order.

| Terminal class masses F/A/I/N | G | Policy posterior | Selected action |
|---|---|---|---|
| Original C | 15.170069 / 15.171332 / 15.171375 | .333619 / .333198 / .333183 | A |
| .60/.40/0/0 | 15.649211 / 15.650474 / ∞ | .500316 / .499684 / 0 | A |
| .54/.36/0/.10 | 15.754571 / 15.755834 / 14.006222 | .129135 / .128972 / .741893 | **W** |
| .5999994/.3999996/.000001/0 | 15.649212 / 15.650475 / ∞ | .500316 / .499684 / 0 | A |
| .5399994/.3599996/.000001/.10 | 15.754572 / 15.755836 / 14.006222 | .129135 / .128972 / .741894 | **W** |
| .55/.35/.05/.05 imposed on token states | 15.736222 / 15.737485 / 14.699369 | .207509 / .207247 / .585244 | **W** |

Every finite-choice arm has policy decided-by **G**, action flip set **#{G}**.
The last arm is a counterexample to identifying “no newly true wanted token”
with Joe's known-failure class. Its 5% is concentrated on far fewer states than
the 55% focus mass; F2 wins without promising any new work.

### Earlier run 1789952479

Only EOI C1/C2 and F2 C1 enact actions; 21 other candidates are empty C0s.
Both EOI cascades map to the same first action. Table gives EOI1/EOI2/F2.

| Class masses F/A/I/N | G EOI / F2 | Policy posterior EOI1/EOI2/F2 | Choice; fix-7 policy / action |
|---|---|---|---|
| Original C | 170.558868 / 170.561999 | .167085 / .033417 / .033312 | EOI; habit / robust |
| .60/.40/0/0 | ∞ / ∞ | no posterior | **:no-admissible-candidate**, all 24 infinite |
| .54/.36/0/.10 | ∞ / 90.328119 | 0 / 0 / .041667 | **F2**; no-competing-policy / robust |
| .5999994/.3999996/.000001/0 | 184.374379 / ∞ | .833333 / .166667 / 0 | EOI; habit / robust |
| .5399994/.3599996/.000001/.10 | 184.374379 / 90.328119 | 2.985e−42 / 5.971e−43 / .041667 | **F2**; G / #{G} |
| .55/.35/.05/.05 imposed on token states | 173.554600 / 91.021266 | 2.985e−37 / 5.971e−38 / .041667 | **F2**; G / #{G} |

Remaining mass sits on non-enactable C0s; F2 action mass .041667 is not a
conditional-on-enactment normalization. The hard-zero arm's “robust” means
fix-7 neutralization preserves excluded support, not that C had no effect.
With rho=0 and epsilon>0 EOI wins despite epsilon: there is no alternative
finite acting outcome. A preference cannot construct a missing focus cascade.

### Wanted-token odds depend on the comparison context

At s0, toggle one wanted token, fixing all others. Latest updater's three wants
(h0e270aa090bc, h42fceb4ad48b, h6378c65a4012) each had **1.001306835:1**.
They become ∞:1 when N is ruled zero, **.174061:1** under .54/.36/0/.10, and
**.354569:1** under the 55/35/5/5 token counterexample. Both F11 wants become
.354121:1 in that last arm; F2's already-present route-report coordinate remains
1.181360:1 because toggling it does not constitute a *new* want under this
partition. Within the same nonzero class, a toggle still has odds exp(w).

Earlier EOI's three wants each had 1.001566693:1. They are undefined (both
outcomes ruled zero) in the literal zero arm, 0:1 against allowed N under hard
zero, ∞:1 when epsilon is positive but N forbidden, and 1.430700e−36:1 in the
55/35/5/5 token counterexample. Full per-token lists are retained.

These numbers demonstrate the outcome-domain problem; they are not advocated
strengths. For contrast, a **categorical single-attested-increment** carrier
splitting the latest focus budget directly by raw source weights would give
updater share 458/1404 of focus, hence focus-increment versus known-failure odds
11×458/1404 = **3.588319:1**, rather than .354569:1. That carrier needs a declared
observation/kernel for multiple simultaneous increments and their attestations.
It is not today's B and cannot honestly be used to recompute the old G as if it
were already fitted. The same global class odds therefore do not identify a
unique per-want C.

For the **actual clarified attested-outcome C**, G and posterior are not
identifiable from these frozen token models: they have no predictive kernel
for the required attestation/known-failure classes or slow local-focus state.
The computed scores above are explicit diagnostic alternatives, not estimates
of that missing model.

### Partition-conditioned per-tick C: distinguish inference from retained state

The pinned answers' “Global estimate vs local focus” section and follow-up
`invoke-1790012276836` make the hierarchy explicit. The preceding mixtures
are **global, unconditioned C diagnostics**, not Joe's requested per-tick C.
The work-history rule in §1 discovers WM focus for both frozen runs' date.
Thus the replay assigns both **inferred local partition F = WM focus**, not
“elsewhere” merely because the earlier selector chose EOI. Neither frozen
record retains an authenticated slow partition or entry/exit receipt: their
actual local state is unavailable. This retrospective assignment must be
recorded as an inference, not inserted into the historical record as fact.

For a sharp conditioning diagnostic, let
`C_terminal(s | z=F) = C_global(s) 1[class(s)=F] / .55`.
Equivalently replace the proxy's masses by 1/0/0/0, preserving within-focus
weights and the earlier horizon-step C. This is the limiting case of local
concentration, not evidence that Joe specified exact local zeros. A softened
local preference or an anticipated warranted exit requires a declared kernel;
the global split cannot identify that softness or exit probability.

| Run | Global .55/.35/.05/.05 token proxy | Local F-conditioned token proxy |
|---|---|---|
| 1789964661 | G A/F/F2 = 15.736222 / 15.737485 / 14.699369; posterior .207509 / .207247 / .585244; F2 selected | G = **15.138385 / 15.139648 / ∞**; posterior **.500316 / .499684 / 0**; **updater selected**, policy G / action #{G} |
| 1789952479 | EOI G 173.554600, F2 G 91.021266; F2 selected | **All 24 G = ∞**; `:no-admissible-candidate`, no posterior or decided-by |

On F-supported terminal predictions, G_local − G_global = log(.55) =
−.597837001; relative odds among those policies are unchanged. In the latest
run F2 is excluded because it predicts no **new** focus token, despite its
focus target. The earlier menu has no policy producing a new focus token:
conditioning preferences cannot supply one. Compared with the old global
60/40/0/0 arm, local conditioning shifts finite latest G by log(.6), keeping
the updater and its posterior; both arms refuse the earlier menu.

At the initial token context each updater present/absent comparison is now
infinite, since absence lies outside F; F2's comparison has both states ruled
zero and is undefined. These are support effects of this proxy, not warranted
local attestation odds. Within F, odds still use the original exp(weight).
The actual attested-outcome conditioned C remains held for the same missing
kernel as the global case. Keep missing observations separate as below.
The new `:local-focus-token-proxy` arm in `replay.edn` retains every candidate
and odds row, and the research test checks the G shift and support refusals.

### Supplement: fixed 5% elsewhere, varying no-increment mass

Follow-up `invoke-1790012034279` requested the controlled consequence table.
Hold elsewhere at .05 and focus:adjacent at 11:7; let rho=N be 0, .05 or .10.
Then F=(.95−rho)×11/18 and A=(.95−rho)×7/18. This avoids changing two independent
budgets while varying the no-increment assumption. These remain **token-domain
counterexamples**, not the attested/known-failure semantics Joe confirmed.

| rho | F / A / I / N | Focus : N odds; log odds | Latest A/F/F2 posterior; choice | Earlier EOI1/EOI2/F2 posterior; choice |
|---|---|---|---|---|
| 0 | .580556/.369444/.05/0 | ∞; ∞ | .500316/.499684/0; A | .833333/.166667/0; EOI |
| .05 | .55/.35/.05/.05 | 11:1; **2.397895 nats** | .207509/.207247/.585244; **F2** | 2.985e−37/5.971e−38/.041667; **F2** |
| .10 | .519444/.330556/.05/.10 | 5.194444:1; **1.647590 nats** | .125452/.125293/.749255; **F2** | 1.493e−37/2.985e−38/.041667; **F2** |

At rho=0 latest G=(15.682155,15.683418,∞); at rho=.10 it is
(15.793380,15.794644,14.006222). Earlier EOI G=173.554600 for either rho;
F2 G=∞ at zero, 91.021266 at .05, 90.328119 at .10. Latest policy/action
decided-by remain G/#{G}; earlier zero-rho is habit/robust, positive-rho G/#{G}.
Complete rows are in the expanded replay artifact.

**Comparison to the entropy experiments:** “above about 1 nat” is a scale
heuristic, not a universal completion threshold. For improve-5's Bernoulli
attempt endpoint, changing p=.9 to q=9/11 gives

```
ΔG = (p−q) log(C_success/C_failure) − [h(q)−h(p)]
critical log odds = [h(q)−h(p)]/(p−q) = 1.821799707 nats.
```

Thus class odds 11:1 exceed this threshold: on that binary class carrier the
failure update increases G by **.047135092 nats**, instead of rewarding the
extra entropy. At rho=.10 the class log odds 1.647590 fall below it and ΔG is
**−.014253548 nats**. Associated : N is 7:1 (1.945910 nats) at rho=.05;
elsewhere : N is 1:1 (0 nats). At literal 60/40/0/0, positive probability of a
failure outcome incurs infinite risk, not merely a large finite preference.

Improve-1 uses a different grain: p1=theta and p2=1−(1−theta)^2, entropy at
**both** steps, terminal utility only. Its displayed formula gives a threshold
**10.333605607 nats** for the same .9→9/11 change. Even ln(11) leaves
ΔG=**−.182980428 nats** there. The research test checks both thresholds.
Do not claim that 11:1 automatically fixes both models, transfer an attempt
likelihood to firings, or add parameter novelty twice.

Finally, class odds are not every token's odds. The powerset experiment's
cardinality factors turn class-level 11:1 into updater .354569:1 at the initial
context; the categorical increment example gives 3.588319:1 (about 1.278 nats),
which is also below the endpoint threshold. This is why the same declared
55/35/5/5 cannot settle entropy behavior until the observation carrier and
within-class allocation are specified.

### Confirmed split: known failure is not an unobserved outcome

Follow-up `invoke-1790012152117` confirms .55/.35/.05/.05 for focus,
associated, useful elsewhere, and **nothing delivered and known**, respectively.
The last class requires an observed typed failure (including stop-the-line),
not simply an absent increment. The records already preserve distinctions that
an outcome classifier must retain:

| Recorded evidence | Meaning and permitted classification |
|---|---|
| Token comparison `:predicted-not-observed`, Boolean false with matching artifact | The predicted token was observed absent. The updater example establishes a checkbox mismatch; it does not establish that no attested work increment occurred. |
| Token comparison `:observation-missing` with `:measurement-unavailable`, `:ambiguous-measurement`, or `:artifact-revision-mismatch` | No admissible answer to that token question. Neither false nor known non-delivery. |
| Admitted D-task v2 artifact observation, Boolean false | Revision-bound negative evidence with declaration/locator, schedule, artifact and evidence identities. Its authority explicitly does not establish mission completion or causation. |
| D-task v2 typed refusal, or a token's `{:status :missing :kind ...}` | A failed evidence check or unavailable measurement. A typed **evidence refusal** is not automatically an observed **work failure**. |
| Occurrence-bound stop-line finding and its repair/lifecycle evidence | Evidence of a known process failure, eligible for the declared failure classifier. Delivery measurements may still be missing; preserve both facts rather than overwrite one with the other. |

Source anchors at this discovery's pinned checkout:
`src/futon2/aif/token_outcome.clj:48` (`compare-outcomes`),
`src/futon2/aif/d_predecessor_task_authority.clj:325` (`observation-value`),
`:330` (`signed-observations`), and `:372` (`verify-observations-v2`).
`src/futon2/aif/full_loop_runner.clj:212` explicitly distinguishes a stop-line
record from the durable parked lifecycle transition: recording the former
alone cannot attest the latter. An outcome classifier must bind the finding
to the occurrence and state which proposition it establishes.

**Consequence of an explicit fifth outcome called “unobserved”:** the four
confirmed masses sum to one, leaving C(unobserved)=0. Under mathematical KL,
any policy with Q(unobserved)>0 has infinite risk: even Q(unobserved)=.01
contributes .01 log(.01/0)=∞. If every policy can lose its measurement, every
policy is excluded. This is not a small penalty for poor observability, and
Joe's 5% for useful interruptions does not supply missing-observation mass.
These are mathematical consequences of that proposed carrier, not a claim
that the present scorer has a fifth outcome or implements this extension.

**Alternative requiring an explicit observation model:** keep unobserved as
an observation mask/epistemic state, with the latent work outcome still in the
four valued classes. Missing evidence then leaves a distribution over those
classes; it does not become a fifth valued outcome or a known failure.
Marginalization needs a declared missingness likelihood. Treating a missing
measurement as neutral is justified only under an appropriate ignorable
missingness assumption; selective loss of failure evidence needs its own
model. Do not drop missing rows and renormalize the remaining outcomes as a
substitute. Entropy/ambiguity depends on that model, not on assigning every
missing row the same uncertainty score.

This alternative is **not implemented by the existing observation evaluator**:
`src/futon2/aif/observation_model.clj:150` (`observation!`) refuses a non-observed
status or an empty event as `:missing-observation`. Slice 1 must record the
unresolved classification/model requirement, preserving this refusal. Slice 2
needs the declared observation semantics before any switch-on.

Accordingly, the numerical .55/.35/.05/.05 tables above remain explicitly
labelled token-domain counterexamples. In particular, F2's unchanged token
state cannot be promoted to the confirmed 5% known-failure class. The frozen
runs do not identify the required predictive attestation/failure kernel, so
actual confirmed-carrier G, posterior and selection changes remain held;
reporting the proxy winners as the confirmed policy would fabricate that join.
The 60/40/0 comparison and 0/.05/.10 sensitivity table quantify the alternative
carrier assumptions, rather than resolve missing evidence by fiat.

## 4. Questions, now answered and narrowed

The original concrete questions were:

1. If the earlier menu offers only off-focus EOI or a policy producing no new
   want, should zero forbid all 24 candidates (stop), or allow a tiny-probability
   interruption? The replay shows both consequences. **Joe has now answered:
   5% useful elsewhere, not a universal hard zero.** This does not authorize
   arbitrary unclassified work; the “useful elsewhere” witness is still needed.
2. If F2 changes no wanted fact, is that the 5% failure class, even absent a
   failed check or stop-the-line receipt? **Joe has now answered: no, known
   non-delivery is an observed typed failure/stop-the-line.** It is not inferred
   from a null model delta or missing evidence.

Do not ask those again as unanswered. Two narrower decisions remain before a
switch-on, phrased for the specific ambiguity rather than re-asking for strength:

- Does the global 55/35/5/5 account count closed attempts, independently attested
  increments, or elapsed work time? A one-minute stop and a day-long attempt
  have equal weight under the first and very different weight under the last.
- When an otherwise focused WM attempt needs an Agency repair or an APM
  excursion, what event authorizes exiting/resuming the local partition: a
  accepted dependency/connection receipt, an operator instruction, or both?
  Which completion receipt ends the overarching focus? The rule can support
  both authorities, but must record which one actually fired.

These are timescale/transition and witness declarations, not a request for
another arbitrary k. The clarified global split is not a mandated per-tick
mixture. Stationary occupancy does not determine residence times or a transition
kernel. The requested hierarchy includes a known-failure partition: represent
that as a diagnosed/stop-line state entered on evidence, not a task the agent
should manufacture to spend 5% of its time. Keep delivery outcome and slow
process state separately typed when formalizing R15.

## 5. Handoff-sized slices

### Slow state, exit observations and stationary occupancy

Proposed carried state (separate from token q0, seven-status entity belief and
habit E): `{:schema :wm/focus-state-v1 :partition ... :focus-id ...
:entered-at ... :previous-state-digest ... :entry-receipt ...
:rule-digest ... :as-of ... :valid-through ...}`. The four partition tags are
focus, associated, useful-elsewhere and known-failure; focus-id names the
project/facet region, not just the broad tag. Unestablished state is typed
missing, not automatically known-failure. A continuation carries the frozen
state and evidence hashes; expiry requests re-observation without granting an
exit. Preserve the parent focus and return condition on an excursion stack.

The slow transition consumes an occurrence-bound completion attestation for
the active work, an authorized from/to move warrant with purpose and return
condition, or a diagnosed failure/stop-line finding. A failure state exits on
its repair/discharge evidence. A commit, unchecked box, retrieval neighbor or
quiet day alone cannot fire an exit. The fast scorer receives a frozen
`C_tau(outcome | partition, focus-id, rule)` plus the prediction/observation
kernel. If planning includes exits, its predictive slow transition must model
the future **exit evidence**, not silently change partitions at every fast step.

A concrete stationary construction illustrates what the global numbers fix.
For pi=(.55,.35,.05,.05), an ungated transition family is

```
T_alpha(i,j) = (1-alpha) 1[i=j] + alpha pi_j,  0 < alpha <= 1.
pi T_alpha = pi.
E[dwell ticks in i] = 1 / (alpha (1-pi_i)).
```

Alpha sets the slow timescale once, independently of candidate G and the tick's
winner. Pi does not set alpha. At illustrative alpha=.01 (not a proposed
operating parameter), dwell means are 222.22 / 153.85 / 105.26 / 105.26 ticks.
Different desired dwell ratios require a richer transition family. More
generally declare stationary flows f_ij with equal total incoming/outgoing
flow at every i, sum[j!=i] f_ij <= pi_i, and T_ij=f_ij/pi_i off diagonal.
Self-loops fill each row. Balanced flows guarantee pi T=pi; dwell times and
allowed edges constrain feasible flows. This provides a checkable construction,
not an arbitrary per-tick strength chosen to get a winner.

**The evidence-gated invariant is additional:** blocking T_alpha's proposed
exits until evidence arrives generally destroys its claimed stationary law.
The test includes the exact bad case: focus has no exit evidence and its row
becomes absorbing; pi T != pi. Never bypass the exit check to recover the
numbers. To claim both requirements, model exit-evidence arrival and durations
on an extended (partition, active work, age, evidence) state. For a semi-Markov
model with embedded-chain stationary weights nu_i and mean dwell d_i, the
partition occupancy is proportional to nu_i d_i. Fit/declare those quantities
and verify the resulting marginal against pi; if completion never arrives,
finite dwell/stationarity may not exist. Until such a model and observations
exist, 55/35/5/5 is a desired long-run occupancy, not a proved property of the
runtime. Deviations are reportable evidence, not permission to force a move.

R15 in `MAP-rnode-to-lean-2026-09-21.md:45` has declared edges to R13/R16,
but no hierarchy/timescale dynamics theorem. The fast planning horizon is not
this slow transition. Future formal obligations are normalization and support
of each conditional C, evidence-authorized transitions, stationary marginal
under stated recurrence/duration assumptions, and preservation of missingness.
No current Lean result supplies these laws.


1. **Record only, ~200–350 lines plus tests:** freeze discovery rule/version,
   commit/source/time window, active/background facet graph, previous focus and
   completion/transition status, distinguishing inferred from retained partition.
   Record global and partition-conditioned C separately, the carried-state
   proposal, exit-evidence availability and held stationary-kernel status.
   Classify each candidate with source-attributed
   relation or typed unknown. Record class-level C masses and the explicit
   outcome-domain map; record any computable diagnostic C, unrepresented mass,
   normalizers, odds and held attestation/kernel fields. G/E/posterior bytes
   remain unchanged. First test: both pinned dates recover WM; Q3 bridge is
   ineligible on Sept 20 but eligible on Sept 21; missing embedding node stays
   missing; no known-failure class from F2's unchanged prediction. Replay the
   two frozen selections byte-identically with receipt attachment only.
2. **Declared switch, default OFF, ~200–350 lines once domain witnesses exist:**
   consume a declared local-focus C family and authenticated classification
   kernel over attested increments/known failures. Preserve the current scorer
   off-switch exactly. Refuse absent support/meaning/attestation or missing
   local-state binding; do not fill with the token proxy above. Test an actual
   attested focus increment, warranted associated move, useful interruption,
   known typed failure and unknown observation separately. Do not call this
   production-ready until the prediction→attestation observation model is
   declared; improve-4 and aligned kernel examples supply relevant receipts.
3. **Separate slow state/occupancy slice:** version a sticky focus transition
   rule and its completion/warrant inputs using the state and duration contract
   above. Test the stationary law and the evidence-gating counterexample; never
   claim ungated stationarity for a gated runtime. Record occupancy separately from E
   (avoid rewarding the same recent commit twice as preference and habit).
   Test unfinished focus surviving quiet windows, explicit completion exit,
   warranted excursion/return, missing evidence, and repair-only lock-in.
   Fit/calibrate transition/duration parameters only after the unit in §4 is
   declared. No numerical transition kernel is inferred from 55/35/5/5 alone.
4. **Cross-scale observability slice:** retain the window/session/mission joins
   and validity intervals needed for focus-region T/H/L; report coverage before
   deriving an attention measure. Existing mission L is usable as source weight,
   never a substitute for missing window/session observations.

## Reproduction and limits

From this worktree:

```
python3 holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-7-evidence/discover.py
clojure -M:test -e '(load-file "holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-7-evidence/improve_7_replay.clj")'
```

`heads.json` fixes commit histories; `discovery.json` pins inspected embedding/
wholeness bytes and retains every counted commit. Those generated data files are
mutable and not all version-controlled: hashes/mtimes are observations, not
proof of their Sept 20 availability. Historical focus activity is reproducible
from pinned Git; historical embedding/attention state is **not established**.
`replay.edn` retains every candidate, score, posterior, class and odds comparison;
frozen model inputs are the existing improve-5 compressed evidence fixtures.
The full 2^123 domain is normalized analytically, never enumerated or restricted
to Q's support. Script assertions reproduce baseline G/posteriors and check
latest full-space normalization; source/parameter choices are explicit above.

Discovery-script gates: clj-kondo 0 errors/0 warnings; check-parens OK. The
research-only `futon2.report.focus-discovery-test` replays the probe and checks
the hard-zero refusal, reserved mass, epsilon behavior, F2 counterexample and
fix-7 diagnosis. This is not a claim that a production focus mechanism passed
tests. No baseline-failure claim is appropriate for this discovery. Validation
and the scoped registry warrant are recorded below.

Validation at `c18b982a`, Java 21.0.11 / Clojure 1.11.1:

- `clojure -M:test -m cognitect.test-runner -n futon2.report.focus-discovery-test`:
  **1 test / 17 assertions / 0 failures / 0 errors**, plus the probe's explicit
  baseline/normalization assertions during load.
- Fresh standalone replay exit 0; `cmp` against committed `replay.edn` exit 0
  (byte-identical). Python AST syntax check passed; discovery command exit 0.
- `clj-kondo --lint` on the probe and test: 0 errors / 0 warnings. Both passed
  `futon4/dev/check-parens.el`; `git diff --check` clean.
- Scoped warrant
  `test-registry-2a10a80a5e01775ff2a3a438a3a810ab31816c5ec108912a13ba1d5466fd8115`:
  registered replay **1/17, exit 0**; fresh check `:warrant? true`,
  `:outside-closure []`. It pins the Clojure probe, both frozen model fixtures
  and the research test; it does not certify the proposed discovery rule,
  semantic classifications, or live external embedding snapshots. Commands:
  `clojure -M -m futon3c.test-registry run /tmp/improve-7-registry.edn` and
  `check /tmp/improve-7-registry-check.edn` from futon3c, own CLI process.

Follow-up validation at `537f39f4`: the fixed-elsewhere/no-increment arms and
observation-grain entropy controls pass **2 tests / 26 assertions / 0 failures /
0 errors** under the same namespace command. Probe/test lint 0/0 and parens OK.
Updated scoped warrant:
`test-registry-39f0b15d78a099f5ab76f2f0bfe3f14cbcbf578e602fe39cb1c211675333771f`,
fresh check `:warrant? true`, `:outside-closure []`. This supersedes the earlier
warrant for the expanded replay script/test, with the same scope limitations.

Hierarchy follow-up at `a4646c41`: **4 tests / 38 assertions / 0 failures /
0 errors** under the same namespace command. Probe/test clj-kondo 0/0,
check-parens OK, diff whitespace check clean. The added controls check local
conditioning's exact log(.55) G shift, the earlier menu's empty focus support,
and stationarity before/after an exit gate. New scoped warrant:
`test-registry-b8adc9b604bc275d38952256036c52f07925c41bbc5b5801fba6dbeeef824d89`,
fresh check `:warrant? true`, `:outside-closure []`; same research-only scope.
Commands: `run /tmp/improve-7-registry.edn` and
`check /tmp/improve-7-hierarchy-registry-check.edn` via the registry CLI above.
