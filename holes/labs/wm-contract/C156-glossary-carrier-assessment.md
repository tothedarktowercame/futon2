# C156 — reader-facing glossary carrier assessment

Date: 2026-08-31

## Scope and rule

This assessment checks the seven Lean carriers identified by C151 against the
text a reader of `science-2026` actually receives.  A carrier needs its own
reader-facing definition when the paper asks the reader to interpret it as an
operand in a displayed claim and does not explain it locally.  A Lean carrier
does not, by itself, create an editorial obligation.

Authority checked:

- `p4ng/science-2026.tex:11` includes `sec-glossary.tex`;
- `p4ng/sec-glossary.tex:1-80` is the complete 33-entry glossary;
- `DarkTower/WarMachine/Holes.lean:6118-6139` supplies the seven typed
  carriers; and
- C151 and C155 supply the prior formal-coverage censuses.  This assessment
  narrows their editorial conclusion; it does not change their Lean census.

No paper source is changed by this delivery.

## The seven omitted Lean carriers

| carrier | reader exposure and need | one-sentence entry, if the editor elects to add one | disposition |
|---|---|---|---|
| `Outcome` | The reader meets outcomes throughout the EFE discussion, but `o` is already introduced as an observation vector at `sec-glossary.tex:12` and “predicted outcomes” is ordinary language at lines 21–25.  A separate entry is optional. | **Outcome.** An outcome is a possible future observation, indexed by the observation channel that gives it meaning. | **scope decision**, not a defect by itself |
| predictive `Q(o\mid\pi)` / `PredictiveOutcomeKernel` | The displayed EFE equation at `sec-glossary.tex:21` uses `Q(o\mid a)` as an operand but never says that it is the distribution of outcomes predicted under the candidate action/policy.  The risk paragraph at line 23 repeats the idea without binding it to `Q`. | **Predictive outcome distribution `Q(o\mid\pi)`.** `Q(o\mid\pi)` is the probability distribution over outcomes the model predicts if policy `\pi` is followed. | **reader-facing defect** |
| transition `B` / `TransitionKernel` | `B` is used at lines 27 and 35, but line 27 explicitly calls it the transition model and explains both its identity setting and its role in `q^- = Bq`; line 35 explains action-conditioned extensions.  A separate entry would aid lookup but is not needed for comprehension. | **Transition model `B`.** `B` gives, for each action, the probability of each next hidden state from each current hidden state. | **scope decision; already locally explained** |
| policy prior `E` / `PolicyPriorKernel` | This is not actually unexplained: `sec-glossary.tex:37-46` has a dedicated “Policy prior `E` and habit” entry, defines the normalized prior, and shows where it enters selection. | **Policy prior `E`.** `E(\pi)` is the probability assigned to an already allowable policy before current observations are used to evaluate it. | **already covered; no omission defect** |
| preferences `C` / `PreferenceDistribution` | The displayed EFE equation at line 21 uses `C`; “preferences `C`” names it but does not say that it is a normalized preferred-outcome distribution.  Risk at line 23 still leaves that carrier implicit. | **Preference distribution `C`.** `C` is the probability distribution over outcomes the agent prefers, against which its predicted outcome distribution is compared. | **reader-facing defect** |
| parameter prior `Q(\theta\mid\pi)` / `ParameterPriorKernel` | The paper never displays this notation.  The EIG entry at line 29 says in prose that policy-conditioned EIG requires predicted prior entropy; BMR later defines its count-vector priors locally, which are a different construction. | **Parameter prior `Q(\theta\mid\pi)`.** `Q(\theta\mid\pi)` is the distribution over model-parameter values predicted before a future observation under policy `\pi`. | **internal formal carrier; no current reader need** |
| parameter posterior `Q(\theta\mid o,\pi)` / `ParameterPosteriorKernel` | The paper never displays this notation.  Line 29 explains posterior-uncertainty reduction at the level needed for its explicit statement that production does not compute EIG. | **Parameter posterior `Q(\theta\mid o,\pi)`.** `Q(\theta\mid o,\pi)` is the distribution over model-parameter values after conditioning on outcome `o` under policy `\pi`. | **internal formal carrier; no current reader need** |

### Editorial verdict

The omission is a **localized defect for two symbols**, predictive `Q(o|a)`
and preference distribution `C`, not a seven-entry glossary defect.  Those two
are standard AIF notation for an expert, but this glossary declares at
`sec-glossary.tex:3` that it glosses the symbols in each substantive formula
for a reader who needs plain language.  Standardness therefore does not close
the gap under the paper's own stated register.

`B` and `E` are already explained in the reader surface.  `Outcome` is
recoverable from the observation entry and ordinary prose.  The two parameter
kernels are formal dependencies of the full EIG definition, but the paper
neither prints their notation nor claims to compute that EIG; adding them now
would expose internal formal machinery rather than repair a reader-visible
gap.

## Reverse-direction audit: the fourteen entries without Lean definitions

“No named Lean definition” is not the same as “undefined for the reader.”  The
fourteen C151 entries all receive an actual prose explanation:

| glossary entry | prose definition present? | reader evidence |
|---|---:|---|
| Active Inference Framework | yes | line 5 defines the theory and this paper's operational use |
| Ambiguity | yes | line 25 defines state-to-outcome uncertainty and expected entropy |
| Control states `U` and policy vocabulary | yes | line 35 defines controls, policies, and vocabulary extension |
| Aliveness `L=T.H` | yes | line 50 defines both factors, their product, and its role |
| Embedding space | yes | line 52 defines the representation and limits nearness to hypothesis generation |
| GFlowNet “slush” | yes | line 62 defines the sampler, distribution, temperature, and intended role |
| Fold | yes | line 64 defines the checked construction plan and its parts |
| Act-gate | yes | line 66 states the conjunction and missing-input abstention |
| EDN | yes | line 68 defines the serialization notation and why it is used |
| Substrate and Drawbridge | yes | line 70 distinguishes both substrate layers and the guarded access layer |
| Demonstration Foundry / have–want arrows | yes | line 74 defines the arrow and the Foundry extension |
| Strategic mission selection | yes | line 76 explains the current surrogate, its limits, and the proposed two-grain role |
| Clicks, attempts, and cohorts | yes | line 78 separately defines all three measurement units |
| Shared experimental substrate | yes | line 80 defines what is shared and what evidence does not transfer |

Result: **0 of 14 are also undefined in the prose**.  Their lack of a named
Lean counterpart remains the formal-scope partition recorded by C155 (six
definable, five blocked, three expository); it is not evidence that the
glossary names unexplained terms.

## Decision left to the editor

An edit, if authorized, need only repair the reader-visible operands `Q(o|a)`
and `C` (either with short entries or an equally explicit local definition at
the EFE equation).  Whether to add lookup entries for `Outcome` and `B` is an
editorial completeness choice.  The policy prior already has an entry, and the
parameter kernels should remain out unless the paper begins to display or rely
on the full parameter-conditioned EIG construction.

## Verification

- `cd /home/joe/code/p4ng && python3 detect_drift.py` — exit 0; 72 referents,
  `RELOCATED (1)`, no semantic drift.
- `cd /home/joe/code/futon2 && bb checks/wm_workspace_gate.clj` — exit 0;
  33 checks, 32 executable, zero failures.
- `cd /home/joe/code/futon2 && bb -cp . checks/preemptive_absence_coercion_lint.clj`
  — expected exit 1 with the unchanged live population
  `{:futon2 7, :futon3 0, :p4ng 0}`.
- `git status --short -- sec-glossary.tex` in `p4ng` was empty: no paper source
  changed.
