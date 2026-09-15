# Buffer cleaner: checkable costs, no manufactured A/B winner

Preregistered in **a508aa24**, before executing `compute.py`. All probabilities,
revisit flags and costs are **constructed**, not measurements of Joe's Emacs.
The supplied real log is motivation only. No live operation was performed.

The aggressive wiring scans 24 buffers, admits every cleaner category, and
uses a six-hour file threshold. Conservative admits streams, completed HTTP
outputs and files only, with a 24-hour file threshold. The latter category mask
is a proposed wiring, not an existing configuration variable. Both must leave
at most 16 buffers. The two thresholds are choices for this demonstration,
not empirically calibrated priors.

| Constructed group | Count | Needed within 24h probability each | Recovery cost each | Actual revisits | Aggressive kills | Conservative kills |
|---|---:|---:|---:|---:|---:|---:|
| Disposable streams | 4 | 0 | 1 | 0 | 4 | 4 |
| Disposable completed HTTP | 2 | 0 | 1 | 0 | 2 | 2 |
| Regenerable render | 1 | .10 | 1 | 1 | 1 | 0 |
| Inactive invoke output | 1 | .20 | 2 | 0 | 1 | 0 |
| Dired navigation | 1 | .25 | .5 | 0 | 1 | 0 |
| Other unmodified temp | 2 | .05 | 1 | 0 | 2 | 0 |
| Files last displayed 30h ago | 2 | .05 | 5 | 1 | 2 | 2 |
| Files last displayed 8h ago | 3 | .40 | 5 | 2 | 3 | 0 |
| Fresh files, 2h | 4 | .60 | 5 | 0 | 0 | 0 |
| Protected buffers | 4 | 0 (fixture) | 5 | 0 | 0 | 0 |

Every killed buffer is assumed successfully killed. All have no process,
visibility, unsaved file edits or waiting clients. Protected entries are never
eligible. Sharp eligibility checks do not imply sharp needed-later predictions:
the zero probabilities for disposable streams/HTTP are fixture premises only.
File reloadability prevents loss of saved bytes; it does not eliminate revisit
work. Render, invoke, Dired and temp categories likewise do not establish
zero recovery cost. Actual predicates: pinned `futon-buffer-cleaner.el:130-179`.

## Equations and arithmetic

One cycle is one scored endpoint, **T=1 for both policies**. This does not test
longer temporal cascades. Each of the same 24 outcome coordinates records a
false kill. For killed buffer i, Q_i(1)=p_i; for kept buffers Q_i(1)=0.
Independence is a declared construction. The current hidden state is known,
so the likelihood A equals the predicted outcome distribution Q, and I=0.
One cost unit is mapped to one nat. Preferences encode recovery cost c_i:

    C_i(1) = exp(-c_i)/(1+exp(-c_i))
    C_i(0) = 1/(1+exp(-c_i))
    KL_i = p_i*c_i + log(1+exp(-c_i)) - H(Bernoulli(p_i))
    G_B = KL(Q || C) + H(A) = expected recovery cost + sum_i log(1+exp(-c_i))

This is the book risk/ambiguity equation used by CascadeEFE. No extra entropy
penalty is appended. The full product-joint calculation independently checks
the sums of coordinate KL and entropy. Probabilities/costs use exact rational
input arithmetic; logarithms and displayed scores use Python double precision,
with numerical identity checks, not a new Lean proof.

**A illustration** projects away only deterministic bookkeeping observations.
Its genuine revisit outcomes remain, so its projected likelihood/preferences
are identical to B's. It is **not the unresolved full-A formula**. This field
cannot distinguish these models. Removing actual revisit uncertainty, or
replacing KL with expected loss, would change the model or objective; neither
is silently performed. Expected loss is reported separately below.

| Wiring | Remaining | Expected recovery | KL | Ambiguity | A illustration G | Book B G | Fuel | A/B predicted total | Constructed realized total | Ground-truth verdict |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Aggressive | 8 | 7.225 | 6.531743 | 4.200917 | 10.732660 | 10.732660 | 1.84 | 12.572660 | 17.84 | Eligible, loses |
| Conservative | 16 | .500 | 3.610629 | .397030 | 4.007660 | 4.007660 | 1.04 | 5.047660 | 6.04 | Eligible, wins |

Fuel = .01 per scanned buffer + .1 per kill. Realized oracle costs are
16+1.84 and 5+1.04, computed from the preregistered revisit identities. The
oracle selects minimum realized recovery+fuel subject to the count threshold;
it does not select by either horn's own score. Both predicted choices match
this one constructed outcome. That does not establish calibration or expected
optimality on Joe's workload. The constant preference baseline is 3.507659713246
for BOTH policies, including preserved coordinates. Dropping preserved
coordinates would introduce a policy-dependent normalization change.

## What this settles and what remains missing

The example prices real revisit exposure, and it shows why counting ambiguity
alone is insufficient to predict what book G chooses. It does **not** settle
A/B: there is no stated alternative formula assigning different scores to this
same genuine uncertainty. Full-A comparison remains blocked on that specific
mathematical definition, not on further arithmetic or a larger example.
Nor is this evidence for any live cleaner setting. Measuring that needs a
retained buffer-identity/kill/reopen history and a declared revisit window.
The cleaner's report (`:182-211`) supplies neither. Category counters count
candidate attempts while total is the net count difference; the supplied
"16" line therefore cannot be used as a per-category successful-kill census.

`parameters.json` exposes the age-to-needed-later prior. Changing the stale-age
threshold does not itself estimate that prior. Revisit effort, desirability
exchange rate and the clean-enough threshold also remain declared inputs.

Reproduce with `python3 compute.py` in this directory. `results.json` includes
all coordinate distributions, preferences, costs and independent joint checks;
`execution.log` retains the successful run. `receipt.json` pins inputs, source,
script and outputs. Registry work remains deferred at b9b3047c/f50f8de9.
