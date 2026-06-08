(ns futon2.aif2.tension
  "M-aif2 slice-1: the **tension-proposer** as a credited + admissibility-gated
   S1 registry entry.

   This is the first instance of the `aif2` extensible-registry primitive
   (M-aif2 DERIVE §3a–§3e) at stratum **S1 (proposers)**. It is purely additive:
   `futon2.aif.*` is unchanged (Bayesian-model-reduction: `aif` = `aif2` with this
   entry absent), and the candidates it emits use the **existing** S2 action-classes
   (`:open-mission` / `:address-sorry` / `:fire-pattern`) — it does NOT introduce a
   new action-class (that is Gap-2 / S2 work, out of slice-1 scope).

   It consumes the **E1 curvature signal** from the substrate-2 metric
   (Campaign C-substrate-completion §4 O3, keystone `M-substrate-metric` §3):
   a seq of per-node maps under `(:curvature-signal state)`, each carrying
   `:curvature/*` + `:resolution-state/*` fields. The signal is *injected* into
   wm-state by the metric (the boundary-growth seam) — this namespace is the
   consumer behind that seam, so it is testable against a fixture with no
   cross-repo dependency on futon3c.

   Two trust quantities keep this (B)-not-(A) (ARGUE hard criterion):
   - **credit** — a Beta posterior keyed by the entry id `:s1/tension`, read live
     via `futon2.aif.intrinsic-values/credit-for` (generalised from per-class to
     per-entry by passing the entry id). New entry ⇒ Beta(1,1) mode 0.5.
   - **admissibility** — a consent gate (M-aif2 T3): the proposer emits *only*
     when its entry is admissible/active, so it is NOT a hardcoded always-on
     proposer."
  (:require [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.intrinsic-values :as iv]
            [cheshire.core :as json]))

;; ---------------------------------------------------------------------------
;; κ — node-type → action-class (over the EXISTING S2 classes)
;; ---------------------------------------------------------------------------

(def kappa
  "κ: the action-class to propose at a high-tension node of a given type.
   Maps onto today's S2 inventory only (no new class). `:file` routes to
   `:open-mission` (a file's tension is discharged via the mission it feeds,
   per keystone §3.1) — but files are non-actionable directly (resolution-state
   `:unknown`), so the gate filters them before κ is consulted in practice."
  {:mission :open-mission
   :sorry   :address-sorry
   :pattern :fire-pattern
   :file    :open-mission})

;; ---------------------------------------------------------------------------
;; The O3 firing rule + intensity (keystone M-substrate-metric §3.1 / §3.4)
;; ---------------------------------------------------------------------------

(defn- numeric? [x] (number? x))

(defn propose-here?
  "O3 firing rule: a node fires propose-here iff it is geometrically strained
   AND unresolved AND actionable. Curvature alone never fires — a complete node
   (resolvedness 1.0) can be a sharp bridge yet stay non-actionable (the Stage-B
   guard). Mirrors keystone `metric/e1/propose-here?`."
  [node]
  (let [r (:resolution-state/resolvedness node)]
    (boolean
     (and (:curvature/strain? node)
          (numeric? r)
          (< (double r) 1.0)
          (:resolution-state/actionable? node)))))

(defn action-intensity
  "Ranking magnitude: |min-incident-κ| × (1 − resolvedness). Keeps the bridge
   signal selective (most κ ≥ 0; only the negative tail fires), and weights by
   how unresolved the node is. Mirrors keystone `metric/e1/action-intensity`."
  [node]
  (let [k (double (or (:curvature/min-incident-kappa node) 0.0))
        r (double (or (:resolution-state/resolvedness node) 0.0))]
    (* (Math/abs k) (- 1.0 r))))

;; ---------------------------------------------------------------------------
;; The S1 registry entry + its admissibility (consent) gate
;; ---------------------------------------------------------------------------

(def tension-entry
  "The slice-1 S1 registry entry. `:credit` is not stored here — it is read live
   from the intrinsic-values atom keyed by `:id` (per-entry credit). `:producer`
   is the κ-routing function below; `:status`/`:admissibility` gate activation."
  {:id          :s1/tension
   :stratum     :s1-proposer
   :status      :active
   :provenance  {:source "M-aif2 slice-1" :version 1
                 :consumes "C-substrate-completion §4 O3 (E1 curvature cut)"}})

(def default-consent
  "v0 consent context: operator-ratified (Joe ratified SV / installed slice-1).
   The supervised→autonomous migration (project_consent_gate) would swap this
   for an autopen policy at the boundary-redraw locus (T3)."
  {:operator-ratified? true})

(defn admissible?
  "Admissibility gate (M-aif2 T3 / INV-consent): may this entry act? True only
   when the entry is not pruned AND consent grants it. This is what makes the
   tension-proposer gated rather than a hardcoded always-on proposer — deny
   consent and it emits nothing."
  [entry consent]
  (boolean
   (and (not= :pruned (:status entry))
        (:operator-ratified? consent))))

;; ---------------------------------------------------------------------------
;; Producer: gated read of the curvature signal → credited candidate-actions
;; ---------------------------------------------------------------------------

(defn- route-candidate
  "Turn a fired node into a candidate-action over an existing S2 class via κ.
   Carries full provenance (INV-provenance) and the entry's live Beta credit."
  [entry node]
  (let [node-type (:node/type node)
        action    (kappa node-type)]
    {:type           action
     :target         (:node/id node)
     :weight         (action-intensity node)
     :intrinsic-value (iv/credit-for (:id entry))
     :rationale      (format "tension at %s %s: min-incident-κ %.4f, resolvedness %.2f — strain bridge %s"
                             (name (or node-type :node))
                             (:node/id node)
                             (double (or (:curvature/min-incident-kappa node) 0.0))
                             (double (or (:resolution-state/resolvedness node) 0.0))
                             (str (:curvature/strain-edge node)))
     :provenance     {:proposer-id   (:id entry)
                      :min-kappa     (:curvature/min-incident-kappa node)
                      :strain-edge   (:curvature/strain-edge node)
                      :resolvedness  (:resolution-state/resolvedness node)
                      :inclusion-reason "curvature-strain ∧ unresolved ∧ actionable"}}))

(defn propose-from-signal
  "Pure core: given the entry, consent, and the injected curvature signal
   (a seq of node maps), return the gated, κ-routed, credited candidate-actions.
   Returns [] (never nil) when the entry is inadmissible — proving the gate."
  [entry consent curvature-signal]
  (if (admissible? entry consent)
    (->> curvature-signal
         (filter propose-here?)
         (map #(route-candidate entry %))
         (vec))
    []))

(defn tension-proposer
  "The S1 entry exposed as an `ActionProposer`, so it slots into the existing
   `compose-proposers` vector. Reads `(:curvature-signal state)` (the seam),
   gates via admissibility, routes via κ, emits credited + provenanced
   candidate-actions over existing S2 classes.

   NOTE (slice-1 scope): this builds and tests the proposer entry; *installing*
   it into the live `war_machine/judge` proposer vector is a separate,
   consent-gated activation step (a boundary-redraw, T3) — deliberately not done
   here so the live WM is untouched until that step is taken explicitly."
  ([] (tension-proposer tension-entry default-consent))
  ([entry consent]
   (reify ap/ActionProposer
     (propose [_ state]
       (propose-from-signal entry consent (:curvature-signal state)))
     (proposer-id [_] (:id entry)))))

;; ---------------------------------------------------------------------------
;; Reduction tripwire (INV-reduction, GF1): with no curvature signal present,
;; the proposer is silent — `aif` behaviour is preserved exactly (additive).
;; ---------------------------------------------------------------------------

(defn emits-nothing-without-signal?
  "True when an absent/empty curvature signal yields zero candidates — the
   additive guarantee (frozen `aif` reproduced)."
  []
  (empty? (propose-from-signal tension-entry default-consent nil)))

;; ---------------------------------------------------------------------------
;; SEAM: read the keystone's delivered E1 curvature output into a curvature
;; signal, for injection into wm-state by the live judge. Fail-safe: any
;; error/absence ⇒ [] ⇒ the proposer is silent ⇒ the WM is unchanged. So this
;; install CANNOT break the live WM (INV-reduction holds under failure).
;; ---------------------------------------------------------------------------

(def default-curvature-artifact
  "The delivered E1 curvature output (keystone M-substrate-metric R2). The metric
   refreshes this; the WM consumes it. (v1: read the delivered artifact;
   recompute-per-scan is a later upgrade.)"
  "/home/joe/code/futon3c/holes/missions/M-substrate-metric.R2-curvature-full.json")

(defn candidates->signal
  "Pure: map the artifact's `top_propose_candidates` (already the actionable
   strain set) into the node-map shape the tension-proposer consumes."
  [candidates]
  (mapv (fn [c]
          {:node/id                       (:node c)
           :node/type                     (keyword (:node_type c))
           :curvature/strain?             true
           :curvature/min-incident-kappa  (:min_incident_kappa c)
           :curvature/strain-edge         (:strain_edge c)
           :resolution-state/resolvedness (:resolvedness c)
           :resolution-state/actionable?  true})
        candidates))

(defn read-curvature-signal
  "Read the delivered E1 curvature output into the curvature-signal. Returns []
   on ANY error/absence — so an absent or malformed metric leaves the WM exactly
   as it was (additive/fail-safe)."
  ([] (read-curvature-signal default-curvature-artifact))
  ([path]
   (try
     (-> (slurp path)
         (json/parse-string true)
         :top_propose_candidates
         candidates->signal)
     (catch Throwable _ []))))
