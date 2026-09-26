# C6-D — channel identity is not yet enforced by construction

2026-09-26. codex-1 for claude-8 review. Read futon2
`383d98ddfe7c5d819ea519420a01599f1c9240f3`. No src/scripts changes,
flight, click, map edit or live load. Paths below are repository-relative;
source basenames denote `src/futon2/aif/` unless stated otherwise.

## 1. Definition row and placement

Added `:channel-identity` adjacent to `:adjudication-rates` and
`:token-likelihood`, after reading those and `:co-application-kernel`.
`:defines :rates`, `:imports [:o :ref-label]`, `:node :R7`,
`:class :stack-defined`, `:model :cascade-token-model`, `:realised false`.
The formal line is:

> For each observation class c, A_c (the class's false-negative and
> false-positive rates) is measured only from labels whose :check-mechanism
> names the check function the flight runs as class c at the loaded code
> identity; a label produced by any other mechanism, or by the same function
> at another identity, is not evidence for A_c. A class with no such labels
> is unmeasured (:measurement :absent), never measured from a neighbour.

The pre-edit registry at :641–658 defines adjudication `:rates` at **R7**
from `[:o :ref-label]`. `:token-likelihood` (:661–678) defines `:A-tok` at R4
from `[:rates]`. `:likelihood-precision` (:539–559) defines `:zeta` at R7,
importing `[:A :o :s]`. Therefore C6 restricts the evidence used by the
existing rates producer at R7; it neither invents `:A-c` nor relocates the
consumer to R17 or R3a. A_c is prose notation for those two class rate cells,
not a new registry variable. Repeating :defines :rates records a constraint
on that existing definition, not a competing estimator.

The LaTeX defines the admitted class population by BOTH class equality and
full loaded-check identity equality, then applies adjudication rates to that
population. The minimum and authorised prior remain A-S Revision 3 policy;
C6 does not override them. The row's :code first sentence is:

> NOT realised: loaded-identity suffix filtering exists, but class and full
> check-mechanism identity are not derived from the flight dispatch's single
> authority.

`:code-at` names the read pin above. `:lean` is explicitly
`{:status :absent :reason :not-yet-stated}`. Census of existing equation rows
found no typed-map Lean-absence precedent: 29 :closed, one :diverges, and
`:class-preference-schedule` simply omits the binding. This uses the packet's
explicitly permitted typed form rather than copying that omission or adding
bare nil. No Lean name or proof is claimed; claude-8 owns that statement.

## 2. The five identity sites

### (a) Flight dispatch

`observation_checks.clj:551–556` defines:

```clojure
{:C3 check-path-exists :C4 check-decl-in-file
 :C5 check-registry-entry :C6 check-witness-reference :C8 check-registered-run}
```

`observe*` (:558–568) selects `(get checks class)` then calls `(f locator)`;
`observe` (:570–574) delegates to it. `flight_runner.clj:77–86` defaults to
`checks/observe`, sends the flight locators to it, and maps refused checks to
`:unknown`. C3 resolves the reference and calls git cat-file -e
(`observation_checks.clj:58–67`); C4 resolves it, git-shows the file and calls
`decl-present?` (:79–87), whose mechanism is an anchored regex (:69–77).

The dispatch table stores **function values**, not Vars or structured
mechanism descriptors. Rebinding `check-decl-in-file` alone in a test does
not automatically change the already-built checks table. A replacement test
must reload/rebuild the dispatch or replace its entry explicitly.

### (b) Subject and loaded identity

`observation_labels.clj:44–58` sets token-class from `(:check check-result)`
but separately constructs `(str "C4/decl-present?@" mechanism-sha)`.
`:88–100` sets token-class to literal :C3 and separately constructs
`(str "C3/cat-file-e@" mechanism-sha)`. Neither name is derived from the
function selected by the flight table. Indeed C4 names an internal regex
helper, not the dispatched check-decl-in-file wrapper; C3 names the git
operation, not the dispatched check-path-exists Var. These descriptive names
currently agree with implementation by convention.

Independent recomputation does not fix this correspondence. C4 uses the
line tokenizer at :60–86 and names its observer C4/reader-form; C3 uses
ls-tree at :102–124 and names C3/ls-tree. Those are the reference mechanisms;
C6 concerns which *recorded check* the rate estimates, not equating the
reference observer with that check.

`loaded-identities` (:16–29) dereferences load-identity/registry once, requires
captured SHA256 entries, and returns `sha256:<hex>` for observation-checks
and observation-labels. Missing registration refuses, with no HEAD fallback.
Both namespaces register on load (`observation_checks.clj:27`,
`observation_labels.clj:14`). `load_identity.clj:95–104,106–125` hashes source
resource bytes at registration, not the identity of a runtime function object.
It captures the entire namespace file: editing an unrelated check in that
same file changes the shared mechanism population too. An unrelated repository
commit alone does not. It does not attest an arbitrary later root-var mutation.

### (c) Reader

`observation_label_reader.clj:9–13` reads key slot 5 and applies
`(str/ends-with? mechanism (str "@" mechanism-sha))`. This correctly rejects
another digest and avoids partial-digest suffix collisions. It does **not**
compare the prefix, the function, or the class against the flight dispatch.
`:32–40` groups active/obsolete records by :token-class and records
`{:class c :excluded :obsolete-mechanism :counts {:labels n}}` for old ones.
`:44–54` enforces both cells >=5 and supplies only included classes' seen
subject counts. A suffix is necessary identity evidence, not the full C6 law.

### (d) Consumer and the exact class seam

`observation_admission.clj:168–189` copies class and mechanism independently
into `[class repo sha path decl mechanism]`; label-record copies :token-class
again. `observation_labels.clj:141–159` dispatches subjects by result :check,
so the ordinary C3/C4 writer currently constructs consistent pairs, but it
never obtains that pair from the flight's actual function descriptor.

The store DOES check something important: `observation_label_store.clj:29–44`
requires key class to equal record :token-class, and key class must be C3/C4;
C3's declaration slot must be nil and C4's a string. It does **not** check key
class against key slot 5's mechanism. The reader groups by record class
(:33–34); `observation_rates.clj:90–106` also `(group-by :token-class labels)`
and never inspects the mechanism. `sourced-rates` (:245–260) maps token locator
classes to those class rates. This is the precise unenforced seam: a
class-consistent key/record pair can carry the other class's mechanism string
at the active suffix and become measurement evidence for the wrong class.

I ran a pure, synthetic counterexample in futon2's own tooling JVM, without
git reads, admission, store writes or a flight. It deliberately constructs
records rather than claiming the production writer emitted them:

```edn
;; Ten distinct keys; five present/recorded true, five absent/recorded false.
[:C4 "futon2" "pin" "path-i" "(defn example"
 "C3/cat-file-e@sha256:active"]
;; Each label's :token-class is :C4; :admission {:status :admitted}.
```

With consistent seen metadata, the real store envelope validator returns true.
The real reader includes 10 labels, subjects {:C4 10}, excluded []. The real
sourced-rates returns C4 rates 1/12 and 1/12 under the authorised Jeffreys prior,
with both measurements {:numerator 0 :denominator 5}. Changing only the active
identity to sha256:new returns labels [], subjects {}, and exactly
`{:class :C4 :excluded :obsolete-mechanism :counts {:labels 10}}`.
This shows the suffix control works and the full identity control is missing.
It is not evidence that malformed labels already exist in the live store.

### (e) Classes without a producer

The flight implements C5/C6/C8 (:551–556 above). The writer supports only C3/C4
(`observation_labels.clj:141–148`), and the post-decision wire drops all other
results (`observation_label_wire.clj:7–13,15–28`). The store's supported key
classes are also C3/C4 (:29–33). Therefore this producer supplies no C5/C6/C8
labels. The contract marks all three checkable
(`resources/wm/observation-contract.edn:26–43`).

`observation_rates.clj:188–208` looks up exactly the token's class and, for
an unmeasured checkable class, returns zero rates with :measurement :absent;
`:245–260` preserves that measurement provenance per token. In the same pure
probe, locators for C5/C6/C8 yielded :measurement :absent for each, while the
synthetic C4 labels yielded measurement counts only for C4. There is no mapping
to C3/C4 neighbours. Zero here is an explicit unmeasured-kernel default, not
an empirical assertion that the checks have zero error. The pure reader API
can be handed arbitrary synthetic snapshots, but the ordinary store/writer
path has no producer for these three classes.

## 3. Verdict and the C6-I test

**:realised false — by name, not by construction.** The identity suffix and
minimum are implemented; full mechanism-to-class-to-flight-function agreement
is not. No new guards or runtime changes are made in C6-D.

One-line structural repair for C6-I: **make one class-to-loaded-check descriptor
own the dispatched Var, canonical mechanism identity and class, and use it in
flight dispatch, subject construction and exact reader matching.** This is a
single authority change, not a claim that a physical one-line string edit
would repair all three consumers. Merely replacing one literal with another,
or validating only the C3/C4 prefix, leaves the function-binding gap.

Proposed test `channel-identity-follows-loaded-flight-check`: use real C4
subjects/admission with five labels per cell under old identity; load the
replacement check and its new registration, rebuild the actual dispatch,
and read with that captured identity. All old C4 labels must be explicitly
obsolete and C4 measurement absent. Four new labels per cell remain below
minimum; five per cell admit only the new population. The bad assertion is a
C4 measured rate obtained from old-function/old-identity records. Existing
`observation_label_reader_test.clj:74–87` covers changed mechanism-sha
populations but not their connection to a replaced flight function.

That requested test alone would already exercise the working suffix rule;
it cannot establish the missing prefix/class law. Add
`mismatched-class-mechanism-is-not-measured`: ten otherwise valid C4-classed
records whose mechanism names C3 (or an unrelated function) at the *active*
identity must never measure C4. Pin the descriptor/function actually invoked
by observe as well. This is the counterexample above, made a refusal/exclusion
regression test against the real consumer, not a fixture kind-to-class table.

Unfixed findings: observation_checks.clj:12–14 still describes zero adjudication
rates as by construction, whereas observation_rates.clj:176–180 explicitly
calls them an unmeasured default. Namespace-wide rather than per-function
digests invalidate all check classes on any source-file change; runtime Var
mutation is not covered by registration. Store validation checks admitted
status and shape, not a reconstruction of admission evidence. None of these
is changed here. No claim is made about the contents of the live label store.
