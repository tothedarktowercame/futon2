# C411 — unmarked declared-not-derived boundary census

Date: 2026-09-01

## Basis and scope

This census was read against Futon2 commit
`ca152b89c3a7cfd784bffec47a44f2549c170b05`.  The machine-readable basis is in
`checks/repository-census-bases.edn`; it names every source subject cited below.

Selection rule: inspect active verification code that (a) enumerates a
population, inventory, coverage surface, or required interface and (b) can
report acceptance or a completeness-shaped result.  The search covered the
top-level checks, their registries, the preemptive lint corpus, and the quiet-run
state machine.  Generated outputs and historical audit prose were not treated
as new boundary owners.

This census records its own limit:

```clojure
{:boundary/type :declared-not-derived
 :subject :C411-boundary-census-population
 :pinned :named-active-completeness-sites-at-basis-commit
 :not-pinned :all-semantic-boundaries-in-the-repository
 :derivation-status :not-exactly-derivable
 :reason :semantic-completeness-claims-are-not-a-syntactic-population}
```

The repository basis makes the reported observations reproducible.  It cannot
turn “all semantic boundary claims” into a mechanically enumerable population.
Registration is self-referential here: adding this entry necessarily moves
`repository-census-bases.edn` after the observation basis, so the basis checker
immediately and correctly reports that one subject `:possibly-stale`.  The
substantive checker/code subjects are unchanged from the recorded basis.  This
is awkward but preferable to pretending a self-modifying registry can equal
its own prior basis commit.

## Unmarked boundaries found

Eight active sites use an authored scope without the shared C405 marker.
Discovery only: none was changed here.

| subject | source | classification | authored pin and excluded stronger claim | worth closing? |
|---|---|---|---|---|
| post-tested state-machine attestations | `scripts/wm_quiet_run_state.py` | `:not-exactly-derivable` | Pins the transition's named evidence and says `attestation-coverage: not-claimed`; does not identify runtime producer/event authority after the tested phase. Injection and caller-authored evidence prevent inference from artifact shape. | **Yes, highest priority.** Keep the marker even after producer receipts land; runtime seam non-replacement remains an observation. Owned by wm-organization; not edited here. |
| Q-interface population | `checks/q_interface_completeness_check.clj` | `:not-exactly-derivable` | Pins seven authored definition IDs and six interface IDs; does not prove that these are every semantic Q producer, consumer, or topology edge across Lean and Clojure. Notation and grain determine membership, not syntax alone. | **Yes.** Mark the boundary now; closing it would require an authoritative typed interface inventory, not a larger hand list. |
| preference-stack layer population | `checks/preference_stack_witness_shape_check.clj` | `:derivable-not-adopted` | Pins five expected layer IDs and checks the fixture against them; does not derive those five from the live EFE producer's complete folded and unfolded inputs. | **Yes.** A producer-emitted layer manifest or identity-preserving adapter can close it. |
| empty-subject acceptance catalogue | `checks/empty_subject_acceptance_lint.py` | `:not-exactly-derivable` | Pins seven registered acceptance boundaries and historical controls; explicitly does not cover helper-hidden or novel acceptance shapes. Arbitrary data-flow subjects are not exactly inferable by its regex analysis. | **Mark, do not pretend to close.** Continue explicit registration when new acceptance boundaries appear. |
| live-artifact format catalogue | `checks/live_artifact_format_boundary_lint.py` | `:not-exactly-derivable` | Pins eight named generators and seven source/proof shapes; does not establish that every publication generator or lossy aggregation boundary is represented. Interprocedural validation is intentionally outside the lint. | **Mark.** Build discovery can help add generators, but semantic format boundaries remain declared. |
| absent-is-loud repository scope | `checks/absent_is_loud_lint.clj` | `:derivable-not-adopted` | Recursively derives files within six authored roots; does not derive that those roots are the complete implementation surface where silent absence matters. | **Yes.** Move the root inventory to an authoritative repository/source manifest, then retain the static-analysis limitation separately. |
| preemptive-repair corpus scope | `checks/preemptive_repair_lint.clj` | `:derivable-not-adopted` | Derives all eligible tracked text in three authored repositories; does not derive that those repositories are the complete campaign surface. | **Yes, cheaply.** Reuse an authoritative workspace repository manifest rather than adding repositories by hand. |
| repository-census registry population | `checks/repository_census_basis_check.clj` | `:not-exactly-derivable` | Derives and reconciles cited subjects for each registered census, but the population of documents classified as repository-state audits/censuses is itself registration-only. An unregistered census is invisible. | **Yes.** A naming/front-matter convention could make discovery exact for opted-in artifacts; semantic identification of every prose census would remain unclaimed. |

## Authored sets that are not findings

Three prominent authored sets were checked and are not being marked merely for
being lists:

- `checks/wm_workspace_gate.clj` derives the full top-level `checks/*.clj`
  filename population and reconciles it with `known-check-files`.  The comment
  explicitly says this is classification, not execution policy.  Its file
  inventory is a complete fact at that grain, not a claim of complete semantic
  test coverage.
- `scripts/mutable_verdict_population.bb` derives its members from a recorded
  lexical criterion, and the mutable-verdict registry exhaustively partitions
  exactly that derived population.  It explicitly calls the result discovery,
  not a verdict about every possible mutable dependency.
- the holder registry is joined against every current contract declaration by
  the holder checker.  Extra historical registry rows are harmless; a missing
  owner for a declaration is rejected.  Its resolution claim is derived at the
  stated contract grain.

## Split

Within the declared census scope, the eight active product/checker sites split
**3 `:derivable-not-adopted` / 5 `:not-exactly-derivable`**.  This census's own
population boundary adds one further `:not-exactly-derivable` self-description;
it is not counted as a ninth repository finding.

The best closure investments are the preference-layer producer manifest, an
authoritative workspace-repository manifest for the two source-corpus scopes,
and the state-machine producer/runtime observation already owned by
wm-organization.  The two regex catalogues should be marked rather than sold as
closable inference.
