# Row 14 measured-A categorical authority proposal — 2026-09-13

Status: **proposal for lead review; not adopted, implemented, measured, or
admitted.** This packet reads existing files only. It creates no observation,
pair count, kernel row, production record, or live-system result.

## 1. The variable A actually requires

The machine contract's A is an observation/emission kernel from `Status` to
the ruled organization-outcome domain. `MachineModelSpec.Contract` carries the
model, `aAuthority`, the outcome support, and
`observationSupport : ∀ s, model.observation.support s = outcomes`
(`mathlib4/DarkTower/WarMachine/MachineModelSpec.lean:43-75`). The fundamentals
spec says the missing machine A is from hidden machine states to that outcome
domain and explicitly forbids relabelling the old seven-by-seven lifecycle
event matrix as a disposition likelihood
(`SPEC-fundamentals-build-2026-09-12.md:9-11`). The production contract likewise
requires observed-estimate A rows to be backed by an exact pinned
`:wm/kernel-measurement-v1` record; the pointer check binds bytes but does not
authenticate the measurer or the experiment
(`CONTRACT-machine-model-v1.md:65-85`, `machine_model.clj:71-99`).

Consequently the counted datum is:

```
(adjudicated Status of entity e at close t,
 actual ruled disposition of that same close)
```

It is not any of these three nearby values:

| Value | What it says | Why it cannot silently fill `Status` |
| --- | --- | --- |
| interest `:event/type` | the transition asserted at a checkpoint | The vocabulary separates event type from prior/posterior standing; `state/reopened` is an event that resets standing to `:live` (`interest-event-vocabulary.flexiarg:70-110`). |
| interest `:posterior-state` | standing after replay of an authored interest event | The WM carrier is exactly `#{:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened}` and contains no `:live` (`belief.clj:37-42`). No `:live -> :reopened` coercion is licensed. |
| WM `:belief-row` / its argmax | the machine's latent posterior and a derived point summary | The close code labels the summary `:derived-unique-argmax-of-mu-post`, retains ties as refused, and says it is not the admissible categorical estimator (`full_loop_runner.clj:2404-2442`). Training A from that value would make the model supervise itself. |

The required semantic repair is therefore a declaration, before an estimator:
**a qualifying state value is an independently authored, evidence-bearing,
reviewed categorical annotation over the exact WM seven-state carrier at the
exact entity/run/close point.** It is a measurement label for a latent state,
not a claim of direct physical ground truth. Treating the adjudicated label as
the conditioning state is an explicit supervised-estimation assumption. If
that assumption is not adopted in review, the honest result remains
`:categorical-state-authority-absent`; the current contract must not be filled
from a posterior or an interest-network standing.

## 2. Proposed versioned observation record

The smallest record that can support that assumption is a separate sibling to
the existing belief snapshot, provisionally:

```clojure
{:schema :wm/categorical-state-observation-v1
 :observation/id <globally unique named id>
 :subject {:entity/id <exact selection target>}
 :point {:run/id <WM run id>
         :cohort/id <cohort id>
         :attempt/id <attempt id>
         :checkpoint/ref <exact close/checkpoint ref>
         :observed-at <timestamp>}
 :categorical-status {:domain :wm/status-v1
                      :value <one of the exact seven keywords>}
 :observation/method :reviewed-categorical-annotation
 :observer {:kind :operator|:agent
            :id <named identity>}
 :evidence {:refs [<nonempty exact references>]
            :source/path <local retained single-record artifact>
            :source/sha256 <64 lowercase hex>}
 :review {:reviewer/id <different named identity>
          :verdict :accepted
          :reviewed-at <timestamp>}}
```

This record preserves rather than collapses entity, checkpoint, time, evidence
source, and run identity. The corresponding close keeps
`:entity-state-at-close` unchanged as latent-model evidence and carries the
categorical observation separately. The close's ruled disposition remains the
other arm of the pair. No count is eligible until all three records—the
categorical observation, exact close, and pinned source—join literally.

Who observed what is explicit:

* `:operator` means a named human operator asserted the seven-state label about
  this entity at this close based on the cited evidence. It is still an
  annotation, not unqualified ground truth.
* `:agent` means a named agent authored that same categorical judgment. It does
  not qualify without review by a distinct named human/operator or authorized
  reviewer. A model-generated argmax cannot be renamed an agent observation.
* Validation establishes schema, domain, identity, provenance, and independent
  acceptance. It does not establish metaphysical truth. Disagreement is
  retained as a typed conflict and produces no training pair.

The existing interest vocabulary supplies a bounded authoring pattern, not a
ready-made status oracle: checkpoint events are operator-authored, append-only,
evidence-bearing, and validation is required to block
(`interest-event-vocabulary.flexiarg:89-136`). A procedural adapter may accept
an explicitly authored `:wm/categorical-state-observation-v1` sibling at that
boundary. It must not reinterpret `:event/type` or `:posterior-state`. The
existing push script currently extracts markdown EDN and posts it directly
without invoking its replay validator (`interest-event-push-v1.bb:24-58`),
while the replay validator depends on a missing procedural vocabulary file
(`interest-event-replay-v1.bb:19-25`). Therefore neither existing path is a
qualifying gate today.

## 3. Exact implementation seam after adoption

This is a two-packet implementation, deliberately before any estimator or
counts:

1. **Authority reader/validator (new futon2 source module).** Strictly read one
   pinned UTF-8 EDN form; validate the schema above, exact seven-value domain,
   distinct author/reviewer identities, evidence pin, and nonempty provenance.
   Its successful result is a typed validated envelope; it does not calculate
   a label. The interest checkpoint authoring path may produce that envelope,
   but only explicitly and without changing the interest event fields.
2. **Exact close attachment.** Thread an already validated envelope from the
   full-loop attempt context to the close assembly at
   `full_loop_runner.clj:2951-2973`. Before append, require exact equality with
   `:outcome-entity`, run id, cohort id, attempt id, checkpoint ref, and close
   time policy. Absent input is retained as typed absence, not fatal to closing;
   malformed or present-but-mismatched input refuses before
   `close-attempt!` (`full_loop_cohort.clj:442-460`). The existing latent belief
   capture and identity mismatch check (`full_loop_runner.clj:2404-2471`) stay
   unchanged.

A later estimator may count only validated joins and emit the contract's
`:wm/kernel-measurement-v1` artifact with counts, zero cells, exact source
hashes, and typed-absent rows. That later packet must not smooth zero counts,
invent a row for a status with no data, or claim this proposal supplied data.

## 4. Required refusal controls

The validator/join packets are not acceptable unless commissioned controls
produce these typed outcomes before any close/measurement write:

| Induced violation | Required typed outcome |
| --- | --- |
| `:derived-unique-argmax-of-mu-post` offered as annotation | `:derived-state-not-observation` |
| `:event/type :state/strengthened` offered as status | `:event-category-not-standing` |
| `:posterior-state :live`, or silent `:live -> :reopened` | `:state-domain-mismatch` |
| unknown/eighth status or namespaced event keyword | `:state-domain-mismatch` |
| subject differs from `:outcome-entity` | `:observation-entity-mismatch` |
| run/cohort/attempt/checkpoint missing or unequal | the corresponding `:observation-*-mismatch` |
| missing source, bad digest, multi-form/malformed record | `:observation-evidence-*` refusal |
| unnamed observer, same author/reviewer, or unreviewed agent label | `:categorical-observation-unreviewed` |
| two accepted labels for the same exact point disagree | `:categorical-observation-conflict` (no vote or average) |
| close disposition absent/outside ruled support | existing typed outcome-support refusal |
| mutation after bytes are read but before pin/use | `:observation-source-mutated` |

Positive controls must use a production-shaped envelope with literal
entity/run/cohort/attempt/checkpoint equality and must show that the latent
belief row can disagree with the annotation without either being overwritten.

## 5. Existing production-shaped evidence and its exact absence

The closest existing annotation-shaped record is
`M-interim-director/CP-close`, timestamp `2026-05-29T13:00:00Z`. For example,
`evt-close-1` asserts event type `:state/strengthened`, target
`vsat-poc-2026-q2-q3-scenario-c-vsatlatarium`, posterior standing
`:strengthened`, three evidence references, and a rationale
(`futon7/holes/M-interim-director.md:257-276`). The document explicitly says
the rationales paraphrase Joe's reasoning and were to be adjusted before
formal close (`:257-258`). It has no WM `:run/id`, cohort/attempt identity,
full-loop close reference, separate WM categorical-observation schema, or
independent review envelope. The push path does not enforce the authoritative
validator. It is therefore a concrete candidate annotation shape and **zero
qualifying measured-A pairs**.

The current September 12 F11 close is the complementary exact absence. Its
pinned bytes record cohort `:run4-f11-production-successor-20260912-v3`,
attempt `attempt-001`, and disposition `:incomplete`, but contain neither
`:outcome-entity` nor `:entity-state-at-close`, much less an independently
adjudicated categorical state. Its SHA-256 is
`6100141ed696df07769c704dfb8d6f70eef0d0a72bb04f75adfaac270b140a26`.
It establishes no cell.

## 6. Disposition

The seven-state node obligation is unchanged. The interest event vocabulary is
also unchanged. The proposal repairs the measurement specification by adding a
separate, reviewable annotation authority at the exact close point, rather
than equating transition category, projected standing, or latent posterior.
Lead review must adopt (or reject) the supervised-label assumption and record
schema before implementation. Until then Row 14 measured A remains blocked on
categorical-state authority. This packet itself yields zero qualifying pairs
and does not alter the earlier historical coverage census.

## Source-byte pins

Mechanical SHA-256 output is retained in
`runs/row-14-measured-a-authority-spec-2026-09-13/source-pins.sha256`.
The prior read-only discovery artifacts remain separately pinned in
`runs/row-14-current-capture-discovery-2026-09-13/`.
