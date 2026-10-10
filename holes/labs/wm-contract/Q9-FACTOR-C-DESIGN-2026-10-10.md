# Q9: factor completion from relevance before tuning C

Status: design recommendation; no runtime or Lean semantics changed by this
record.

## Finding

The current terminal C table combines two different judgements in one flat
distribution:

1. whether the selected policy reached its own acceptance criterion; and
2. if it did, whether its target was focused, related, or unrelated to the
   current focus.

In the runtime, all three relation classes are completions, while
`:stop-the-line` is non-completion.  The fixed table is `55/35/5/5`, so the
least-relevant completion ties non-completion.  Changing either 5 to 4 or 6
would make the present Q9 counter pass, but it would not explain the model or
create a defensible tuning rule.

The checked implementations are:

- `futon2.aif.wm.cascade-decision/class-preference-weights` and
  `class-observation-model`;
- `DarkTower.WarMachine.CTauClassPreference`, whose `terminal_order` theorem
  proves the tie and whose module comment explicitly does not establish that
  the fixed distribution is right; and
- `DarkTower.WarMachine.Requirements.Q9`, which currently asks that every
  represented completion-class/non-completion pair be strictly ordered.

## Proposed model

Represent a terminal outcome by two coordinates:

```text
closure    = closed | not-closed
relevance  = focused | related | unrelated    (defined only when closed)
```

Keep two distinct preference carriers:

- `C-closure` expresses the normative preference for closing the selected
  policy's acceptance criterion rather than failing to close it.
- `C-relevance-given-closure` expresses the relative value of the three kinds
  of successful result.

Their product may still be compiled to the four probabilities required by
the current scorer:

```text
C(stop-the-line) = C-closure(not-closed)
C(r)             = C-closure(closed) * C-relevance-given-closure(r)
```

That product alone does **not** imply that every individual relevance class
has more mass than `stop-the-line`: a conditional relevance probability can
be arbitrarily small.  Therefore the present Q9 pair counter should not be
repaired with a smaller arbitrary number.  The recommended Q9 requirement is
the meaningful aggregate law:

```text
sum_r C(r) > C(stop-the-line)
```

with the relevance ordering checked separately.  If the intended policy is
instead that *every* successful relevance class must individually dominate
non-completion, that must be stated as an additional admissibility constraint
on the two carriers:

```text
C-closure(closed) * min_r C-relevance-given-closure(r)
  > C-closure(not-closed)
```

That is a real constraint, not an epsilon chosen to make one report green.

## What may be learned

Observed frequency is not preference.  Updating C from raw ending counts
would teach the machine to prefer whatever it already happens to produce.
The existing Dirichlet machinery in `DirichletLearning.lean` updates an
observation/state likelihood, and `R15HabitPrior.lean` updates the action
habit E; neither authorizes learning desired outcomes C.

Runtime tuning of these carriers therefore requires registered evaluative
evidence:

- closure utility or cost for `C-closure`;
- evaluated usefulness conditioned on closure for
  `C-relevance-given-closure`;
- the authority, prior, update rule, evidence window, and resulting posterior
  recorded in the run certificate; and
- a stable fallback that is named as a declared prior, never described as
  empirical calibration.

Outcome counts may calibrate the observation model used to predict endings.
They cannot, by themselves, determine which endings ought to be preferred.

## Falsifiers

This proposal is false or incompletely implemented if any of these occur:

1. changing only the relevance evidence can reverse the aggregate preference
   for closure over non-closure;
2. raw outcome frequency changes C without registered utility or authority;
3. a run certificate cannot reconstruct both component carriers, their
   sources, and their compilation into terminal C;
4. absent or invalid tuning evidence silently becomes a supposedly calibrated
   fixed table; or
5. Q9 passes by changing the census or the meaning of `:unrelated` rather than
   checking the declared closure law.

## Staged implementation

1. Add write-only receipts for both component carriers and the compiled C;
   retain current scoring while correspondence is tested.
2. Add a Lean definition of the factorization and prove normalization and the
   selected closure law.  Preserve the existing fixed-table module as the
   historical baseline until the new requirement is accepted.
3. Recompute Q9 from the component receipt, with closure dominance and
   relevance claims reported separately.
4. Only then cut scoring over to the factorized carrier under an explicit
   authority record.  Parameter learning is a later feature and requires
   evaluative labels, not merely observed terminal counts.

