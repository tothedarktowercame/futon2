(ns futon2.aif.sorry-registry
  "Hand-curated open-sorry registry for the WM AIF apparatus.

   This namespace IS the v1 substrate adapter that flips
   `forward-model/can-propose? :address-sorry` from false to true. It
   loads sorrys from `~/code/futon2/data/sorrys.edn` (or a path passed
   in), exposes `open-sorrys`, and provides a `sorry-enumerator-proposer`
   that emits one `:address-sorry` candidate per open sorry.

   The act of loading this namespace registers the `can-propose?
   :address-sorry` arm — so callers (e.g. `futon2.report.war-machine/judge`)
   that require this ns will see the gated action class become available
   when the state map carries `:sorrys` (populated by `open-sorrys`).

   Contract: contributes to closing the v0.5 'all concrete actions
   non-proposable' empirical state. The first sorry in `sorrys.edn` is
   the *meta-sorry* — see `futon2/docs/futon-aif-completeness.md`
   §'Capability-gap modeling as endogenous action' for the framing.

   Honest scope: hand-curated, not ingested. New sorrys join by operator
   hand-edit. The interface (`load-sorrys` / `open-sorrys` /
   `can-propose? :address-sorry` / the enumerator proposer) is the swap
   target when a richer substrate (substrate-2, stack-annotations.edn,
   M-INC events post step (b)) becomes canonical.

   Schema v2 (2026-05-18) added optional `:kind` field on each sorry,
   distinguishing `:meta` / `:prototyping-forward` / `:technical-debt` /
   `:decision-debt` / `:external-dependency`. Per Joe 2026-05-18:
   `:prototyping-forward` is the explicit category for work knowingly
   shipped incomplete as a v1 step where the refinement IS the next
   work, not debt to pay back — see `feedback_prototyping_forward_vs_debt`
   memory entry for the discipline."
  (:require [clojure.edn :as edn]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]))

(def ^:private default-sorrys-path
  ;; R-A.1 (M-war-machine-first-outing): relocated data/ → resources/ (tracked;
  ;; git history is the closure ledger + git-level reversibility of WM operation).
  (str (System/getProperty "user.home") "/code/futon2/resources/sorrys.edn"))

(defn load-sorrys
  "Read the sorry-registry EDN file. Returns the full document map
   `{:schema-version :sorrys [...]}`. Default path:
   `~/code/futon2/data/sorrys.edn`."
  ([] (load-sorrys default-sorrys-path))
  ([path]
   (edn/read-string (slurp path))))

(defn open-sorrys
  "Filter loaded sorrys to those with `:status :open`. Returns a vector
   of sorry maps. Zero-arg variant calls `load-sorrys` with the default
   path; one-arg variant accepts a pre-loaded document."
  ([] (open-sorrys (load-sorrys)))
  ([loaded]
   (vec (filter #(= :open (:status %)) (:sorrys loaded)))))

(defmethod fm/can-propose? :address-sorry
  [state _action-type]
  (boolean (seq (:sorrys state))))

(def default-kind-intrinsic-value
  "Per-`:kind` intrinsic-value defaults for `:address-sorry` candidates.

   These are hyperparameters in R12's sense — they could/should be learned
   from operator follow-through data, but at v1 they are hardcoded defaults
   derived from the operator-edited `:kind` field on each sorry. See
   `~/code/futon0/holes/missions/M-the-futon-stack-Q6-r12-design-choices.md`
   §11 for the open question of whether to extend the per-class Beta
   apparatus to per-(class, kind).

   Rationale per `feedback_prototyping_forward_vs_debt`:
   - `:meta`                 — apparatus surfaces about itself; reflexive,
                               often load-bearing for downstream work; highest.
   - `:technical-debt`       — genuine debt; should-have-been-done-right.
   - `:decision-debt`        — open decision waiting on operator judgement.
   - `:external-dependency`  — waiting on external work; operator can't act
                               unilaterally, so smaller credit.
   - `:prototyping-forward`  — refinement IS the work, not debt; smallest
                               credit (the system shouldn't push to 'close'
                               prototyping-forward sorries on its own
                               schedule)."
  {:meta                 0.4
   :technical-debt       0.25
   :decision-debt        0.25
   :external-dependency  0.15
   :prototyping-forward  0.1})

(def ^:const default-no-kind-intrinsic-value
  "Neutral default for sorries with no `:kind` field (legacy v1 schema or
   newly raised sorries before classification). Falls between
   `:external-dependency` and `:technical-debt`."
  0.15)

(defn intrinsic-value-for-sorry
  "Compute the `:intrinsic-value` credit for a sorry candidate.

   Lookup table: `default-kind-intrinsic-value`. Sorries without a `:kind`
   field get `default-no-kind-intrinsic-value`. The breaking-the-tie
   discipline is per `~/code/futon7/holes/M-war-machine-aif-last-mile.md`
   §2.E.1.a — VSATARCS v0.5.13's Q2 chrome surfaced that all `:address-sorry`
   candidates had identical G=-4.208 because per-target intrinsic-value
   wasn't set; this fixes that by using the existing `:kind` schema."
  [sorry]
  (let [k (:kind sorry)]
    (if k
      (get default-kind-intrinsic-value k default-no-kind-intrinsic-value)
      default-no-kind-intrinsic-value)))

(def sorry-enumerator-proposer
  "Proposer that emits one `:address-sorry` candidate per open sorry in
   the state map. State must carry `:sorrys` (populated by `open-sorrys`).

   Each candidate carries an `:intrinsic-value` derived from the sorry's
   `:kind` field via `intrinsic-value-for-sorry`, which breaks the G-tie
   among `:address-sorry` candidates surfaced by VSATARCS v0.5.13's Q2
   chrome (per M-war-machine-aif-last-mile §2.E.1.a)."
  (reify ap/ActionProposer
    (propose [_ state]
      (for [s (:sorrys state)]
        {:type :address-sorry
         :target (:id s)
         :weight 1.0
         :intrinsic-value (intrinsic-value-for-sorry s)
         :rationale (str "open sorry: " (:title s))}))
    (proposer-id [_] :sorry-enumerator)))
