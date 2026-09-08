# Rulings walkthrough, 2026-09-08 (morning)

Joe, in session with claude-1 (emacs-repl), answering the five-item
outstanding-decisions sheet presented after the codex/zai build loop drained
the board (181 done, 5 blocked, bulletin 745bdfa1). Joe's words are quoted
verbatim, typos preserved; each item records the transcription taken.

## Item 1 — accepted-red entries REFUSED; tickets in a dependency-first DAG instead

> "only really 'accepted' if they appear in a live workplan, otherwise they
> are just part of an ambiguous workplan. What we could do if not doe
> alreadyvis write Tickets for outstanding items, and organise them as a
> dependency first DAG, like I have done with the topology loop. That way we
> wiuld have a full picture of what to build when and would not need invented
> categories."

The "accepted-red" classification proposed for RED-COMPONENTS items 6-7
(sorry-category count, workspace mid-work dirt) is NOT taken. Instead: every
outstanding register item gets a ticket in the live workplan, and the workplan
is organised as a dependency-first DAG (the topology-contract loop at
apm-lean/holes/labs/topology-contract is the exemplar). No invented
categories; the full picture of what to build when. Transcription: worklist
row :U70 commissions the DAG organisation.

## Item 2 — receipt re-attestation ruling ACCEPTED

> "ok, and if receipts camed with a pinned sha, that would prevent staleness
> of this sort (though it wouldn't prevent drift if the lean changes, but at
> least that would become obvious)."

The proposed design is ruled: a lane drains the existing backlog of
positive-proof receipts whose pinned declaration text has drifted (37 strict
stale at last count), and the FLOW is fixed at source -- any slice that edits
a Lean declaration carrying a positive receipt re-emits that receipt in the
same slice, gated. Joe's note confirms the pin's purpose: the pinned sha does
not prevent drift, it makes drift obvious. Transcription: rows :U71 (the
at-source gate) and :U72 (the backlog drain).

## Item 3 — F11: refusal-becomes-pattern, a warrant to proceed

> "this sounds like a case where refusal-becomes-pattern would be ideal, and
> the pattern would act as a warrant to proceed with this implementation and
> gather evidence as NEXT STEPS that will support or modify it. That
> experience will be more useful than a ruling from me. (And this itself is
> looking like a meta-pattern we could use, as many needs joe situations are
> really needs evidence situations, and we can't get that as efficiently if
> we wait gated on me to say so.)"

The deliberate implementation refusal at
mathlib4/DarkTower/WarMachine/Holes.lean:264 (`find`) is not discharged by an
operator ruling and not left standing as a block: it is converted into a
PATTERN (refusal-becomes-pattern), and the written pattern acts as a warrant
to proceed with an implementation, with the evidence gathered as NEXT STEPS
that will support or modify the pattern. The dependent choices (:find-sorry
ruled to this disposition; :find-f2-receipt-carrier and :find-f4-reading stay
:observed-not-decided) are to be DECIDED BY THE EVIDENCE the implementation
produces, not guessed first.

THE META-PATTERN, recorded for the pattern library and the APM
design-principles list (claude-9's redline thread): many needs-joe situations
are really needs-evidence situations; gating them on the operator serialises
what evidence-gathering could parallelise. A ruling-shaped block should first
be asked: what evidence would decide this, and can a warranted slice gather
it?

## Item 4 — F12 exemplar proviso: ruled to a NEW disposition (f)

> "(e) is the best of the given options, but I would propose instead (f) to
> use these 3 existing examples as as a source of inspiration to find a real
> working example not on the list. Otherwise tge inly way to pass is
> saticficing, rather that building a real reusable tool. This is a great
> example fir tge 'What problems are we solving' paper and it should be
> written up there. Making snatch reproducible is a good warm up and bridge
> to the new naturalistic example."

The proviso on the ARM 6 carrier ruling is discharged by NONE of the five
C582 arms as they stand. Ruled disposition (f): use ants, mining and snatch
as inspiration to FIND a real working example not on the list -- a
naturalistic exemplar -- and exercise the choice on that. Of the listed arms,
(e) (discharge conditionally, divergence carried open) is the best, and (f)
subsumes its honesty without the satisficing: the aim is a reusable tool, not
a passed gate. Sequence: (1) make snatch reproduce its own run's organised
edge set -- the warm-up and bridge; (2) find the naturalistic exemplar; the
proviso discharges when the choice is exercised on it. The episode is to be
written up in the paper (p4ng/futon-2026.tex, "What Problems Are We
Solving?") -- transcription: row :U69.

## Item 5 — F10: the C580 §6 sequencing recommendation ACCEPTED

> "I accept tge recommendation"

C580 §6 (holes/labs/wm-contract/C580-F10-rider-gap.md): choose the seed's
computation boundary (C580 §5 decision i: EFE fold vs terminal close) BEFORE
deciding whether to restart the dormant cohort (decision ii). Accepting the
recommendation fixes the ORDER; it does not choose the boundary. F10 stays
:blocked, its blocker narrowed to decision (i) (with (iii), whether the rider
requires canonical Q(o|pi) scoring, alongside), and per the Item-3
meta-pattern the boundary question should arrive back at Joe with the
evidence that decides it, not as a bare choice.
