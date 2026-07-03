#!/usr/bin/env python3
"""exhibit_cascade_argue.py — M-evaluate-policies ARGUE exhibit (§9).

Three REAL pattern cascades, every valuation the stack knows how to attach to
them, and the gaps between the valuations left visible. The exhibit IS the
ARGUE artifact (operator decision, 2026-07-03): the complexity under the
bonnet is shown, not summarized.

Real computations: each cascade is (re)constructed by the PRODUCTION
constructor (futon3a cascade_serve.py — phylogeny-greedy, coverage-saturated,
budget 6), using the PRODUCTION psi recipe (cascade_lane.clj mission->psi:
"<stem> — want: <title[:160]>. have: <status[:160]>"). Arena facts (placeholder
G-total 0.0, 0 wins, tick winners) come from the wm-trace corpus census
(q1/q2 artifacts, 674 ticks).

Outputs (holes/labs/M-evaluate-policies/exhibit/):
  cascade-<n>-<target>.dot/.png/.pdf  argue-exhibit.md  argue-exhibit.pdf

Usage: python3 scripts/exhibit_cascade_argue.py
"""
import json, re, subprocess, sys
from pathlib import Path

HOME = Path.home()
CODE = HOME / "code"
F2 = CODE / "futon2"
OUT = F2 / "holes/labs/M-evaluate-policies/exhibit"
SERVE_PY = CODE / "futon3a/.venv/bin/python"
SERVE = CODE / "futon3a/holes/labs/M-memes-arrows/cascade_serve.py"
PHYLO = CODE / "futon6/data/pattern-phylogeny-edges.json"
XELATEX = "/usr/local/texlive/2025/bin/x86_64-linux/xelatex"
LAMBDA = 0.25  # cascade_construct DEFAULT_LAMBDA (F = accuracy - lambda*complexity)

MISSION_DOC_ROOTS = [CODE / r / s
                     for r in ["futon0", "futon1", "futon2", "futon3", "futon3a", "futon3b",
                               "futon3c", "futon4", "futon5", "futon5a", "futon6", "futon7"]
                     for s in ["holes/missions", "holes"]]

def mission_psi(target):
    """Replicate cascade_lane.clj mission->psi (Q-C have->want recipe)."""
    stem = re.sub(r"^M-", "", str(target)).replace("-", " ")
    for root in MISSION_DOC_ROOTS:
        f = root / f"{target}.md"
        if f.exists():
            lines = f.read_text().splitlines()
            title = next((m.group(1) for l in lines
                          if (m := re.match(r"^#\s*(?:Mission:)?\s*(.*)", l))), None)
            status = next((m.group(1) for l in lines
                           if (m := re.match(r"(?i)^\*\*Status:?\*\*:?\s*(.*)", l))), "")
            if title:
                psi = f"{stem} — want: {title.strip()[:160]}"
                if status.strip():
                    psi += f". have: {status.strip()[:160]}"
                return psi, str(f.relative_to(CODE))
    return stem, None

def run_serve(psi, budget=6):
    r = subprocess.run([str(SERVE_PY), str(SERVE), psi, str(budget)],
                       cwd=SERVE.parent, capture_output=True, text=True, timeout=300)
    if r.returncode != 0:
        sys.exit(f"cascade_serve failed: {r.stderr[-500:]}")
    return json.loads(r.stdout)

# The three subjects. Arena context from the 674-tick census (q1/q2, 2026-07-03).
ARENA_TICK = ("2026-07-03T09:03", ":address-sorry sorry/pudding-g1-arrow-witness-binding",
              -4.496, 0.241)
SUBJECTS = [
    {"target": "M-bayesian-structure-learning",
     "kind": "production (in the WM arena since 2026-06; 146 ticks)",
     "persisted_wholeness": 4.141, "arena": ARENA_TICK},
    {"target": "M-canon-fingerprint-store",
     "kind": "production (in the WM arena since 2026-06; 147 ticks)",
     "persisted_wholeness": 2.842, "arena": ARENA_TICK},
    {"target": "M-evaluate-policies",
     "kind": "FRESH — constructed for this exhibit; has never entered the arena",
     "persisted_wholeness": None, "arena": None},
]

# ---- dot rendering (style after ~/julia-chat pattern-cascade.dot exemplar) --
def stem_of(pid): return pid.split("/")[-1]

def make_dot(name, shown, phylo):
    stems = {stem_of(p["pattern"]): p for p in shown}
    order = [stem_of(p["pattern"]) for p in shown]
    co = {}
    for a, b, w in phylo["co_app"]:
        if a in stems and b in stems and a != b:
            co[(a, b)] = max(co.get((a, b), 0), w)
    desc = [(a, b) for a, b in phylo["descent"] if a in stems and b in stems]
    lines = ['digraph cascade {',
             '  rankdir=TB; bgcolor="transparent"; ranksep=0.5; nodesep=0.35;',
             '  node [shape=box, style="filled,rounded", fontname="DejaVu Sans", fontsize=13,'
             ' color="#3F5B54", fontcolor="#2A3E39", penwidth=1.1, margin="0.12,0.06"];',
             '  edge [color="#9CB2AA", arrowsize=0.65];']
    fills = ["#EAF1EE", "#F3EEDA", "#F4EEF2"]
    for i, s in enumerate(order):
        rel = stems[s]["rel"]
        label = s if len(s) <= 26 else s[:24] + "…"
        lines.append(f'  "{s}" [label="{i+1}. {label}\\nrel {rel:.2f}", fillcolor="{fills[min(i//2,2)]}"];')
    drawn = set()
    for (a, b) in desc:
        lines.append(f'  "{a}" -> "{b}" [color="#5E7E73", penwidth=1.8];')
        drawn.add(frozenset((a, b)))
    for (a, b), w in sorted(co.items(), key=lambda kv: -kv[1]):
        if frozenset((a, b)) in drawn:
            continue
        drawn.add(frozenset((a, b)))
        pw = 0.8 + 0.45 * min(w, 4)
        lines.append(f'  "{a}" -> "{b}" [dir=none, penwidth={pw:.2f}];')
    # keep greedy order legible when phylogeny leaves a node unlinked
    linked = {x for e in drawn for x in e}
    for i in range(len(order) - 1):
        if order[i+1] not in linked:
            lines.append(f'  "{order[i]}" -> "{order[i+1]}" [style=dashed, color="#C7B9A0", penwidth=0.8];')
    lines.append('}')
    dot = OUT / f"{name}.dot"
    dot.write_text("\n".join(lines))
    for fmt in ["png", "pdf"]:
        subprocess.run(["dot", f"-T{fmt}", str(dot), "-o", str(OUT / f"{name}.{fmt}")],
                       check=True, env={"PATH": "/usr/bin:/bin"} ,)
    return dot

def fmt(x, nd=3):
    return f"{x:.{nd}f}" if isinstance(x, (int, float)) else str(x)

def make_fold_dot(fold):
    """Wiring diagram for the recorded fold turn (fold-summary.json)."""
    lines = ['digraph fold {',
             '  rankdir=TB; bgcolor="transparent"; ranksep=0.45; nodesep=0.3;',
             '  node [shape=box, style="filled,rounded", fontname="DejaVu Sans", fontsize=12,'
             ' color="#3F5B54", fontcolor="#2A3E39", penwidth=1.1, margin="0.14,0.08"];',
             '  edge [color="#5E7E73", fontname="DejaVu Sans", fontsize=10, fontcolor="#6B7F78", arrowsize=0.7];']
    boxes = fold["boxes"]
    for b in boxes:
        holed = b["hole"] is not None
        fill = "#F6E3DF" if holed else "#EAF1EE"
        mark = "  ⌀ sorry" if holed else ""
        lines.append(f'  "{b["id"]}" [label="{b["id"]}: {b["method"]}\\n{b["gloss"]}{mark}",'
                     f' fillcolor="{fill}"{", penwidth=2.0" if holed else ""}];')
    for a, b in zip(boxes, boxes[1:]):
        lines.append(f'  "{a["id"]}" -> "{b["id"]}" [label=" {a["produces"]}"];')
    for i, h in enumerate(fold["policy_holes"], 1):
        lines.append(f'  "h{i}" [label="policy-hole {i}\\n{h["free"]}", shape=note,'
                     ' fillcolor="#F9F3E0", color="#B08D3E", fontcolor="#7A6228"];')
        lines.append(f'  "h{i}" -> "{boxes[-1]["id"]}" [style=dotted, color="#B08D3E", dir=none];')
    lines.append('}')
    dot = OUT / "fold-wiring.dot"
    dot.write_text("\n".join(lines))
    for f in ["png", "pdf"]:
        subprocess.run(["dot", f"-T{f}", str(dot), "-o", str(OUT / f"fold-wiring.{f}")],
                       check=True, env={"PATH": "/usr/bin:/bin"})

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    phylo = json.loads(PHYLO.read_text())
    results = []
    for i, sub in enumerate(SUBJECTS, 1):
        psi, doc = mission_psi(sub["target"])
        print(f"[{i}/3] {sub['target']}\n  psi: {psi}")
        r = run_serve(psi)
        name = f"cascade-{i}-{sub['target']}"
        make_dot(name, r["shown"], phylo)
        results.append({**sub, "psi": psi, "doc": doc, "serve": r, "img": f"{name}.png"})
        print(f"  size {r['size']} wholeness {r['wholeness']} F {r['F-free-energy']}")

    md = [
        "---",
        "title: \"What do we talk about when we talk about EFE?\"",
        "subtitle: \"M-evaluate-policies ARGUE exhibit — three real cascades, every valuation the stack can attach to them\"",
        "date: 2026-07-03",
        "geometry: margin=2.2cm",
        "mainfont: DejaVu Serif",
        "monofont: DejaVu Sans Mono",
        "---",
        "",
        "## The argument, in plain language",
        "",
        "Every hour the War Machine chooses its next action by comparing numbers. This exhibit",
        "shows three real pattern cascades — the structures the system is supposed to be",
        "choosing between — and **every number the stack currently knows how to attach to",
        "them**. Three facts become visible. First, cascades are valued in one currency",
        "(Alexander wholeness and a free-energy score F) by the lane that builds them, and in a",
        "different currency (the 8-term blend \"G\") by the arena that ranks actions — and the",
        "two currencies never meet: every cascade enters the arena at a constant placeholder",
        "0.0 and has never won a tick (0 of 674). Second, the placeholder is load-bearing:",
        "deleting apparently-inert blend terms would silently hand wins to the unscored rows.",
        "Third, the blend's own epistemic terms have measured zero influence on any decision.",
        "The gaps in the tables below are not missing data — they are the finding. \"EFE\" is",
        "currently a word used across four senses; the mission's work is to make each surface",
        "earn, or honestly relabel, its sense.",
        "",
        "**The four senses** — (i) the canonical quantity, risk + ambiguity in nats;",
        "(ii) the design tradition (pragmatic/epistemic decomposition as a generative shape);",
        "(iii) the deployed 8-term blend `G-total`; (iv) the per-step cost field g(s) of the",
        "futon6 diagrams (the integrand, not the path quantity).",
        "",
        "*Method: each cascade below is (re)constructed by the production constructor",
        "(phylogeny-greedy, coverage-saturated, budget 6) from the production ψ recipe",
        "(want = mission title, have = status line). Arena facts come from the 674-tick",
        "census of `data/wm-trace/` (2026-05-18 → 2026-07-03). Solid green edges = descent;",
        "plain edges = co-application (weight = thickness); dashed = greedy pick order where",
        "the phylogeny is silent. The \"conditions on\" column makes each valuation's",
        "situation-slice explicit — value is fit-to-situation, never form alone (mission",
        "§9.7): the atomic lane eats the live observation vector, the cascade lane eats a",
        "prose scrap, and two of the six native components consume no situation at all.*",
        "",
    ]
    for i, r in enumerate(results, 1):
        s = r["serve"]
        F = s["F-free-energy"]
        md += [
            f"\\newpage",
            f"## Cascade {i}: `{r['target']}`",
            "",
            f"*{r['kind']}.*",
            "",
            f"ψ = `{r['psi']}`",
            "",
            f"![]({r['img']}){{width=88%}}",
            "",
            "| valuation (its own currency) | value | conditions on (§9.7) | provenance |",
            "|---|---|---|---|",
            f"| T-intensity (Σ relevance) | {fmt(s['T-intensity'])} | ψ — the 160-char have→want scrap | constructor, real |",
            f"| H-coherence | {fmt(s['H-coherence'])} | nothing — intrinsic to the pattern set | constructor, real |",
            f"| **Wholeness T·H** (Alexander life) | **{fmt(s['wholeness'])}** | mixed: situated T × intrinsic H | constructor, real |",
            f"| accuracy (ψ-coverage) | {fmt(s['accuracy'])} | ψ | constructor, real |",
            f"| complexity (−log prior mass) | {fmt(s['complexity'])} | corpus base-rates — unsituated | constructor, real |",
            f"| **F = accuracy − {LAMBDA}·complexity** | **{fmt(F)}** | mixed: situated − unsituated | constructor, real |",
            f"| blend G-total (sense iii) | **not computed** — arena rows carry placeholder 0.0 | (would: live observation vector) | `war_machine.clj:3797` `(or dG 0.0)` |",
            f"| rollout ΔG(π) | **abstained** — mission outside the v2 move-set | v2 move-set + capability overlay — not the tick's observation | cascade-lane seam 2 |",
            f"| canonical core G = risk + ambiguity (sense i) | **not defined for cascades** | (would: live observation vector) | `compute-efe` |",
        ]
        if r["arena"]:
            ts, wname, wg, wcore = r["arena"]
            md += [
                "",
                f"**Selection consequence (real tick {ts}):** this cascade sat in the arena at",
                f"0.0 and lost to `{wname}` (blend G-total {wg}; its canonical core {wcore}).",
                f"It has never won in {'146' if i==1 else '147'} appearances. Under the blend it loses on a",
                "placeholder; under wholeness/F it isn't even ranked against actions; under the",
                "canonical core it has no score at all. **Three currencies, no exchange rate.**",
            ]
        else:
            md += [
                "",
                "**Selection consequence:** none — this cascade exists only in this exhibit.",
                "If it entered the arena tomorrow it would be a 0.0 placeholder like the others,",
                "regardless of its F or wholeness shown above.",
            ]
        if r.get("persisted_wholeness") is not None:
            md += ["",
                   f"*Production cross-check: persisted arena wholeness {r['persisted_wholeness']} vs"
                   f" {fmt(s['wholeness'])} recomputed here (ψ recipe and library evolve; both are real runs).*"]
        md += [""]

    # ---- fold section (recorded LLM-turn fold over cascade 3) ------------
    fold_file = OUT / "fold-summary.json"
    if fold_file.exists():
        fold = json.loads(fold_file.read_text())
        make_fold_dot(fold)
        md += [
            "\\newpage",
            "## The fold test — does the best-of-class cascade CONSTRUCT?",
            "",
            "A cascade is only a *policy* if it folds into a construction: a wiring of concrete",
            "steps with typed holes for what it cannot ground (`futon2.aif.fold`:",
            "`fold : (cascade, circumstance) → {:wiring :delta-g :policy-holes}`). We ran the",
            "**LLM-turn fold (impl #2)** over Cascade 3 — the recorded agent turn is",
            "`fold-turn.edn`; the shared coverage→rollout evaluation gives **ΔG = −0.750**",
            f"(coverage 6/8 = {fold['coverage']}). The act-gate's two legs are now both real for",
            "this cascade: ΔF > 0 (F = 4.861, page 5) ∧ ΔG < 0 — the conjunction the production",
            "arena has never once evaluated (every arena ΔG abstained).",
            "",
            "![]({}){{width=92%}}".format("fold-wiring.png"),
            "",
            "**DarkTower structural check:** the wiring renders to CLean",
            "(`m-evaluate-policies.clean.edn`) and compiles against the DarkTower Lean theory —",
            f"**{fold['lean_check']}**. That certifies *structure, not semantics*: the boxes",
            "compose, the spine is a valid `BV.seq`, the two in-box sorries are well-typed",
            "obligations — a soundness floor that rules out hallucinated wiring, nothing more.",
            "",
            "**The complementary sorries** (what the fold honestly could not ground):",
            "",
            "| where | typed hole |",
            "|---|---|",
        ]
        for b in fold["boxes"]:
            if b["hole"]:
                md.append(f"| box {b['id']} ({b['method']}) | {b['hole']} |")
        for i, h in enumerate(fold["policy_holes"], 1):
            md.append(f"| policy-hole {i} | **{h['free']}** — {h['why']} |")
        md += [
            "",
            "Read against the mission: the fold *independently re-derives the mission's own",
            "gap list*. Its six boxes are the DERIVE stages (typed scores, provenance-marked",
            "arena, admission gate, bounded experiments, durable evidence, PAR close); its",
            "policy-holes are D3's commensuration question and D5's probabilistic builds —",
            "the two places DERIVE also marked as the real work. A best-of-class cascade does",
            "fold; what it cannot reach is exactly what the library cannot yet say.",
            "",
        ]

    md += [
        "\\newpage",
        "## The formal reconciliation (D8) — standard notation vs what we compute",
        "",
        "*Canonical formulations per the R18 audit's reference frame (discrete-state-space",
        "AIF; Friston et al., Da Costa 2020), the same forms the badges were judged against",
        "(`E-r18-faithfulness-audit.md`, futon2 `dcbe021`).*",
        "",
        "**Variational free energy** (perception; the cascade lane's F is this, sign-flipped):",
        "$$F \\;=\\; \\mathbb{E}_{Q(s)}\\!\\left[\\ln Q(s) - \\ln P(o,s)\\right]"
        " \\;=\\; \\underbrace{D_{KL}\\!\\left[Q(s)\\,\\|\\,P(s)\\right]}_{\\text{complexity}}"
        " \\;-\\; \\underbrace{\\mathbb{E}_{Q(s)}\\!\\left[\\ln P(o\\mid s)\\right]}_{\\text{accuracy}}$$",
        "",
        "**Expected free energy** (policy evaluation; sense (i) of the four):",
        "$$G(\\pi) = \\sum_{\\tau} G(\\pi,\\tau), \\qquad",
        "G(\\pi,\\tau) \\;=\\; \\underbrace{D_{KL}\\!\\left[Q(o_\\tau\\mid\\pi)\\,\\|\\,C\\right]}_{\\text{risk}}"
        " \\;+\\; \\underbrace{\\mathbb{E}_{Q(s_\\tau\\mid\\pi)}\\!\\left[H\\!\\left[P(o_\\tau\\mid s_\\tau)\\right]\\right]}_{\\text{ambiguity}}$$",
        "",
        "equivalently $G = -\\underbrace{\\mathbb{E}_{Q(o\\mid\\pi)}\\!\\left[D_{KL}[Q(s\\mid o,\\pi)\\,\\|\\,Q(s\\mid\\pi)]\\right]}_{\\text{epistemic value (EIG)}}"
        " - \\underbrace{\\mathbb{E}_{Q(o\\mid\\pi)}\\!\\left[\\ln C(o)\\right]}_{\\text{pragmatic value}}$;",
        "Gaussian ambiguity $H[P(o\\mid s)] = \\tfrac{1}{2}\\ln(2\\pi e\\,\\sigma^2)$.",
        "",
        "**Policy selection:** $P(\\pi) = \\sigma\\!\\big(\\ln E(\\pi) - \\gamma\\, G(\\pi)\\big)$ —",
        "softmax over a **given finite menu** $\\Pi$, under habit prior $E$ and precision $\\gamma = 1/\\beta$.",
        "",
        "### Term-by-term: canonical object vs computed object",
        "",
        "| ours (badge) | canonical object | what the code computes | relation |",
        "|---|---|---|---|",
        "| G-risk (analogical) | $D_{KL}[Q(o\\mid\\pi)\\,\\|\\,C]$ | $\\sum_{ch} w_{ch}\\,\\mathrm{hinge}(\\hat\\mu_{ch})\\cdot u\\,-\\,\\mathrm{intr.}$ | L1 hinge on a point estimate; agrees near the zero-set, no distribution/log/tails |",
        "| G-ambiguity (principled-approx) | $\\mathbb{E}_{Q(s\\mid\\pi)}[H[P(o\\mid s)]]$ | $\\sum_{ch}\\sigma^2_{ch}$ | monotone Gaussian-entropy proxy; one repair ($\\tfrac12\\ln 2\\pi e\\sigma^2$) from canonical; measured influence 0% |",
        "| G-info (analogical) | $\\mathbb{E}_{Q(o\\mid\\pi)}[D_{KL}[Q(s\\mid o,\\pi)\\|Q(s\\mid\\pi)]]$ | $\\sum_{ch}\\max(0,1-\\sigma^2_{ch}) = N - \\sum\\sigma^2$ | affine complement of ambiguity, not an information gain; measured influence 0% |",
        "| G-survival (analogical) | — (no canonical G-term: set-points live **inside** $C$) | second hinge over 4 channels $\\times\\,1.2\\,\\times u$ | a fragment of risk under a $C$ the code doesn't have; flips 47.9% |",
        "| G-structural-pressure (analogical) | — (closest seat: the habit/prior term $\\ln E(\\pi)$) | injected scalar $\\times\\,0.35$ inside $G$ | exogenous weight projected into the wrong slot; flips 44.5% |",
        "| G-graph-pragmatic (analogical) | pragmatic value $-\\mathbb{E}_Q[\\ln C(o)]$; feasibility = the menu $\\Pi_{feasible}$ | $1000\\cdot[\\lnot applicable] + 3\\cdot holes - 20\\cdot ascent$ | a domain restriction smuggled in as a value; stripped from traces |",
        "| G-gap (analogical) | expected uncertainty reduction (EIG fragment) | $6.0 \\times$ gap-score lookup | scaled lookup, no expectation; stripped from traces |",
        "| G-goal-outcome (analogical, R19) | fragment of $D_{KL}[Q(o\\mid\\pi)\\|C]$ | weighted deterministic flip ($p=1$) | belongs inside distributional risk once D5a exists |",
        "| G-total (analogical) | $G(\\pi)$ — one functional, nats | $\\sum$ 9 incommensurate terms, hand weights | multi-objective blend with an EFE-shaped core |",
        "| $\\gamma$, softmax (principled-approx) | $P(\\pi)=\\sigma(-\\gamma G)$, $\\gamma$ from $\\mathbb{E}[G]$ | faithful softmax; $\\gamma$ learns from realized performance; $\\tau$-spread layer non-canonical | the honest corner (R14) |",
        "| cascade F (principled-approx) | $-F$ (ELBO): $\\mathbb{E}_Q[\\ln P(o\\mid s)] - D_{KL}[Q\\|P]$ | $\\mathrm{accuracy} - 0.25\\cdot\\mathrm{complexity}$ | right shape, fitted $\\lambda$, not nats; never enters $G$ |",
        "| fold $\\Delta G$ | rollout $G(\\pi)$ of a discharge policy | $\\gamma^0\\cdot(-\\mathrm{coverage})$ | path-integral in shape; coverage is not in nats |",
        "",
        "### The one-paragraph technical statement",
        "",
        "**The blend is a flattened generative model.** Terms that canonically live in three",
        "different slots — preferences $C$ (survival, goal-outcome), the habit prior $E(\\pi)$",
        "(structural-pressure), and the feasible menu $\\Pi$ (the 1000-mask) — have been",
        "projected into a single sum alongside the two genuine $G$-summands (risk, ambiguity)",
        "and two would-be-epistemic proxies (info, gap). The measured behaviour follows: the",
        "epistemic pole is inert, and the flattened $C$/$E$/$\\Pi$ terms do the steering",
        "(flip-rates 47.9/44.5/35.9%), compensating for a weak explicit $C$. The selection",
        "machinery itself ($\\sigma(-\\gamma G)$, $\\gamma$-calibration) is the faithful part —",
        "applied to a *generated menu*, whose generator (the cascade constructor) plays",
        "$E(\\pi)$'s role unacknowledged. **The repair path in canonical terms:** (i) build a",
        "real preference density $C$ and compute risk as a KL (D5a) — survival and",
        "goal-outcome fold into it; (ii) real EIG over beliefs replaces info/gap (D5b);",
        "(iii) ambiguity in nats (D5c); (iv) move the mask into $\\Pi_{feasible}$ and",
        "structural-pressure into $\\ln E(\\pi)$; (v) declare the selection semantics (sampled",
        "menu, not extremum) and the conditioning (mission §9.6–9.7). Endpoint: $G$ in nats",
        "$=$ risk $+$ ambiguity $(-\\,\\mathrm{EIG})$, everything else living where the theory",
        "puts it — the state of affairs that would earn back the sentence \"EFE-scored.\"",
        "",
        "\\newpage",
        "## Cross-cascade comparison — do the currencies even agree with each other?",
        "",
        "| cascade | size | wholeness T·H | F = acc − λ·cplx | blend G | core G |",
        "|---|---|---|---|---|---|",
    ]
    for i, r in enumerate(results, 1):
        s = r["serve"]
        md.append(f"| {i}. {r['target']} | {s['size']} | {fmt(s['wholeness'])} | "
                  f"{fmt(s['F-free-energy'])} | 0.0 (placeholder) | — |")
    wh_order = sorted(range(3), key=lambda k: -results[k]["serve"]["wholeness"])
    f_order = sorted(range(3), key=lambda k: -results[k]["serve"]["F-free-energy"])
    agree = wh_order == f_order
    md += [
        "",
        f"Ranking by wholeness: {' > '.join(str(k+1) for k in wh_order)}. "
        f"Ranking by F: {' > '.join(str(k+1) for k in f_order)}. "
        + ("The two native currencies **agree** on this sample — "
           "the disagreement that matters is between both of them and the arena's constant."
           if agree else
           "The two native currencies **disagree on ordering even between themselves** — "
           "combining-methods-as-diagnostic, live: the disagreement is the signal."),
        "",
        "## What this argues for (the DERIVE design, §8)",
        "",
        "1. **Observability before semantics** (D1/D2): persist the stripped terms, mark the",
        "   placeholders, expose the canonical core per candidate — so this exhibit's empty",
        "   cells become measured cells.",
        "2. **No term deletions before cascade-row handling** (IHTB-2): the 0.0 constant is",
        "   load-bearing; \"cleanup\" would hand wins to unscored rows.",
        "3. **The exchange-rate problem is the mission** (D3): hand-set weights already fail",
        "   *within* one currency (ambiguity: 0% influence); adding cascades' F to the same",
        "   sum without commensuration would repeat the mistake at larger scale.",
        "4. **Scoring cascades for real is M-G-over-cascades** (C10): ΔG abstains because the",
        "   rollout move-set doesn't cover these missions — a coverage gap, not a wiring bug.",
        "",
        "*Generated by `futon2/scripts/exhibit_cascade_argue.py`; constructor and ψ recipe are",
        "the production code paths; census artifacts in `holes/labs/M-evaluate-policies/`.*",
    ]
    md_file = OUT / "argue-exhibit.md"
    md_file.write_text("\n".join(md))
    subprocess.run(["pandoc", str(md_file), "-o", str(OUT / "argue-exhibit.pdf"),
                    f"--pdf-engine={XELATEX}", "--resource-path", str(OUT)],
                   check=True, cwd=OUT)
    print(f"\nWrote {OUT}/argue-exhibit.pdf")

if __name__ == "__main__":
    main()
