# The War Machine, in plain terms — what it recommends and why

*A public-audience explainer. No in-house jargon; terms are defined as they appear.
It evolves the earlier FuLab governor pseudocode (p4ng `appendix.tex`) into what the
machine actually does today. Companion to the wiring diagram (`aif-wiring-explainer.html`)
and the definitive `wm-end-to-end-contract.md`.*

---

## 1. What it is (one paragraph)

The **War Machine** is an *active-inference agent* that looks at a body of research
work — a stack of missions, code, and open questions — and, each cycle, **recommends
what to work on next**. "Active inference" means it doesn't just pattern-match a
to-do list: it scores each candidate move by a single quantity, **Expected Free Energy
(G)**, that balances *getting closer to what you want* against *resolving what you're
uncertain about*, and it prefers the move that minimises G. The recommendation you see
in the UI is the lowest-G move among many — so it always has a reason you can inspect.

---

## 2. The loop, in pseudocode

The old FuLab governor was already this shape: *believe → score by `softmax(−G/τ)` →
act → observe → update → repeat*. Today's machine keeps that spine and fills in how
each step actually happens.

```
every cycle ("tick"):

  READ    ── perceive the world ───────────────────────────────────
    • scan the stack: missions, holes, code, recent activity
    • form a BELIEF (μ): a per-thing posterior — how healthy / stuck / ready is each
    • refresh the BELLY: ~455 live PREFERENCES (what a good state looks like) — the
      yardstick every candidate is measured against
    • drift-fence: drop candidates that target already-superseded work

  EVAL    ── SELECT the task: score candidate ACTIONS by G(a) ──────
    propose candidate actions a (advance a mission, address a hole, open one, abstain)
    for each candidate action a:
        G_efe(a)   = RISK(a) + AMBIGUITY(a) − EIG(a)        ← the faithful core   [G #1]
        G_total(a) = G_efe(a) + augmentation(a)             ← the controller's blend
              augmentation = extra objectives (survival pressure, structural pressure, …)
    rank by G_total; SELECT via a γ-tempered softmax that abstains on a flat field

  PRINT   ── recommend the selected task, then (if armed) act ──────
    recommend = the SELECTED task; surface its G_efe and G_total + why (the breakdown)
    if armed:  build a cascade of PATTERNS for that task; fold it → candidate wiring;
               price the wiring with a SECOND, different G — a rollout G(π) over the
               cascade (R13)                                                    [G #2]
               act-gate on (ΔF > 0  ∧  ΔG < 0); enact the first pass into the
               substrate, re-observing the WORLD (not the plan)

  LOOP    ── learn, then tick again ───────────────────────────────
    fold the realized outcome into precision γ (confidence in the policy)
    persist the trace; belief μ is re-derived next tick, NOT carried (the R8 gap)
```

Two things are worth naming against the FuLab original:
- There are **two G's**, at two stages: `G(a)` over candidate *actions* selects the task
  (G #1, in EVAL); a separate rollout `G(π)` over the chosen task's *pattern-cascade*
  act-gates it (G #2, in PRINT). FuLab had only the selection G; the act-gating G(π) is new.
- FuLab's "belief update" tuned a single precision `τ` from an error proxy. The WM's
  learning is **γ (precision over policies)**, fed by a *grounded* realized outcome (see §4).

---

## 3. What "recommended next task" means — and its AIF backing

The recommendation is **not a heuristic pick**. It is the γ-tempered `softmax(−G/τ)` choice
over ranked candidate **actions** (scored by `G(a)`), with a no-op abstain guard rather than a
naive top-pick. (Whether it then *acts* on that task is a separate decision, priced by the
*other* G — the rollout `G(π)` over the task's pattern-cascade; see §2.) Because the score decomposes,
the recommendation carries its own justification, and you can read it straight off the trace:

- **`:G-efe`** — the faithful Expected Free Energy (risk + ambiguity − EIG). This is the
  real active-inference value.
- **`:G-total`** — the multi-objective controller's blend (`:G-efe` + augmentation), which
  is what the ranking actually sorts by.

*Example, from a live tick:* the top recommendation was *"advance `M-recommendation-bindings`
— make the machine's next move land in operational surfaces,"* with **`:G-efe` = 19.05** and
**`:G-total` = 17.28**. That the two differ is the honest part: the augmentation is *pulling*
the pick, so the machine is a **multi-objective controller with a real EFE core inside** — not
a pure EFE minimiser, and not a bare heuristic either. The AIF backing is present *and
measurable per candidate*.

---

## 4. What the machine remembers between runs

An agent that "learns" must carry something between episodes. The WM genuinely remembers
**two things it learned itself**, plus the world it acts on:

- **Precision** — the per-channel error precision and **γ** (how sharply to commit to the
  best policy). γ accumulates across ticks (with a burn-in before it moves), so the machine's
  *confidence* compounds.
- **The world** — the substrate it writes into (built capabilities, discharges) and re-reads
  next tick. When it acts, the world is the memory.

What it does **not** yet carry across runs is its **belief posterior** — that is re-derived
fresh each tick from the stack. So today's cross-episode learning is *precision/confidence
learning, not belief-model learning*. (Making belief persist across runs is a named, separate
next step — the "R8 seam" — a decision, not a bug: either wire it, or state the scope.)

---

## 5. The following step: deeper policies

The machine **already** scores G over **policies** — short action *sequences*, via rollout —
and recommends the head of the best-scoring plan. What remains is *depth*: today's rollout is
flat and shallow-horizon, so the next development is a longer horizon and a two-timescale
(strategic/tactical) hierarchy, plus **surfacing the whole ranked plan** — not just its head —
so the recommendation reads as *"work on this next; it's step 1 of this trajectory, chosen
because the whole trajectory minimises G."* That plan view is where a more developed, more
self-referential AIF process will live.

---

## 6. Honest scope

- The loop is **built and armed** — every stage exists, the arms (real KL risk, Gaussian-entropy
  ambiguity, EIG, live-wire, grounded-γ) default on. "Armed" is not "proven optimal"; it means
  the faithful machinery is in the path, latent until a run exercises it.
- The recommendation is **AIF-backed** (§3) but rides inside a **multi-objective controller**;
  the augmentation terms are mostly analogical (documented separately in
  `aif-wiring-ambiguity-explainer.html`). We don't claim the blend *is* Expected Free Energy —
  we show the EFE core beside it.
- Self-reference is **mild and non-blocking**: the core loop runs without intersecting the
  missions that are *about* the machine; as the process develops it will reason about itself
  more, which is intended, not a leak.

*In one line: the War Machine recommends the next task by minimising Expected Free Energy over
many candidates, shows you the EFE reason for its pick, learns its confidence (not yet its
beliefs) across runs, and is about to start recommending plans rather than single moves.*
