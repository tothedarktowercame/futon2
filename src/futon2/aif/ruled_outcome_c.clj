(ns futon2.aif.ruled-outcome-c
  "The ruled outcome preference surface and its EFE-fold declaration."
  (:require [clojure.set :as set]
            [futon2.aif.full-loop-cohort :as cohort]))

(def seeded-positive-masses
  "Exact-ratio transcription of `:C-seeded` at
   `futon2:holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:11`.
   That record says this illustrates a registry seed and is not a ruling at
   `futon2:holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:15`.
   Since `futon2:holes/labs/wm-contract/RULINGS-walkthrough-2026-09-08.md`
   Item 6, this seed enters the EFE fold as a live preference layer (\"the
   machine acts on C\"), and Item 10 requires canonical Q(o|pi) scoring; it is
   the terminal member of the Cτ family in `Holes.C`."
  {:grounded-change 1/2
   :agent-unavailable 1/8
   :build-failed 1/8
   :incomplete 1/8
   :no-selection 1/8})

(def observed-dispositions
  "Observed dispositions, derived once from the positive seed keys recorded at
   `futon2:holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:11`."
  (set (keys seeded-positive-masses)))

(def non-disposition-outcomes
  #{:historical-verification-awaiting-validation
    :historical-verification-refused})

(def disposition-outcomes
  (set/difference cohort/outcome-kinds non-disposition-outcomes))

(def named-zero-dispositions
  "The seven named-zero obligations, derived from the gated authority at
   `futon2:src/futon2/aif/full_loop_cohort.clj:31-33` rather than retyped."
  (set/difference disposition-outcomes observed-dispositions))

;; 2026-09-09, walkthrough Item 18b: canonical tetrahedron names; the earlier
;; people/money/organisations reading is a specialization, not new carriers.
(def ruled-vertices
  "The tagged-sum carrier declaration uses nouns, verbs, organization, evidence.
   The specialization at `futon2:holes/problems/P-validated-R5.md:116-120`
   placed people at nouns and money at verbs; both remain named-empty,
   with money belonging to VSAT at
   `futon2:holes/problems/P-validated-R5.md:129-132`.  Evidence remains
   deliberately unruled because certification/update records and the named
   epistemic-validity region have no attested source enumeration
   (`futon2:holes/labs/wm-contract/aif-equations.edn:211`)."
  {:organization {:status :ruled :carrier disposition-outcomes}
   :nouns {:status :named-empty :carrier #{}}
   :verbs {:status :named-empty :carrier #{} :reason :vsat-vertex}
   :evidence {:status :unruled
              :carrier :owed
              :reason :no-attested-certification-update-vocabulary}})

(def seeded-c
  "The full twelve-wide support required by the corrected ruling at
   `futon2:holes/labs/wm-contract/aif-equations.edn:214`; named zeros remain in
   support so the positivity premise at
   `mathlib4:DarkTower/WarMachine/Holes.lean:6993-6997` can reach them."
  {:support disposition-outcomes
   :mass (merge (zipmap named-zero-dispositions (repeat 0))
                seeded-positive-masses)})

(def fold-declaration
  "Runtime counterpart of the ordered-layer declaration shape at
   `futon2:src/futon2/aif/efe.clj:117-150` and Lean fold boundary at
   `mathlib4:DarkTower/WarMachine/Holes.lean:7186-7189`.  Declaration only."
  [{:layer/id :ruled-outcome-c
    :composition-axis :risk-contribution
    :source :owner-ruling
    :author "Joseph Corneli"
    :basis "futon2:holes/labs/wm-contract/aif-equations.edn:211-214"
    :folded? true
    :in-ruled-sum :yes
    :site "futon2:src/futon2/aif/efe.clj"
    :ruling "futon2:holes/labs/wm-contract/RULINGS-walkthrough-2026-09-08.md:101-112"}
   {:layer/id :c-int
    :source :runtime
    :author "futon2.aif.preferences"
    :basis "futon2:src/futon2/aif/preferences.clj:9-24"
    :folded? true
    :in-ruled-sum :no
    :site "futon2:src/futon2/aif/preferences.clj:9-24"}
   {:layer/id :c-ser
    :source :owner-ruling
    :author "Joseph Corneli"
    :basis "futon2:holes/labs/wm-contract/C537-serendipity-shapes-C.md:49-59"
    :folded? false
    :in-ruled-sum :yes
    :site "named-empty organization-support region"}
   {:layer/id :c-mis
    :source :runtime-dark
    :author "futon2.aif.mission-c"
    :basis "futon2:src/futon2/aif/mission_c.clj:377,422,500,558"
    :folded? false
    :in-ruled-sum :undeclared
    :site "futon2:src/futon2/aif/mission_c.clj"
    :owed "A ruling must decide whether mission-grain C is a region of the tagged sum or a second C outside it."}])

(def machine-preference-schema :wm/machine-preference-v1)

(defn- refuse!
  [kind path]
  {:ok false :refusal {:kind kind :path path}})

(defn- tagged-organization
  [outcome]
  [:organization outcome])

(defn construct-machine-preference
  "Construct the one ruled C from an admitted MachineModelSpec context and the
   seed materialized by the production C loader. Support is read from the model
   authority, never from this namespace's constants. Named empty vertices and
   named zero masses are retained. The result is deliberately not consumable
   while the model contract says that the evidence vocabulary is owed."
  [model-context {:keys [seed provenance layers]}]
  ;; Runtime resolution avoids a namespace cycle: MachineModelSpec obtains its
  ;; outcome authority from this namespace. The actual validator remains the
  ;; sole admission authority.
  (let [validate-model (requiring-resolve 'futon2.aif.machine-model/validate)
        require-vocabulary (requiring-resolve 'futon2.aif.machine-model/require-outcome-vocabulary)
        model-verdict (validate-model model-context)]
    (cond
      (not (:ok model-verdict)) model-verdict
      (not= seeded-c seed) (refuse! :preference-seed-mismatch [:seed])
      (not= fold-declaration layers) (refuse! :preference-layer-revision-mismatch [:layers])
      (not (map? provenance)) (refuse! :preference-provenance-missing [:provenance])
      :else
      (let [support-verdict (require-vocabulary model-context :organization)]
        (if-not (:ok support-verdict)
          support-verdict
          (let [organization-support (set (:support support-verdict))]
            (cond
              (not= organization-support (:support seed))
              (refuse! :preference-support-authority-mismatch [:seed :support])

              (not= organization-support (set (keys (:mass seed))))
              (refuse! :preference-mass-support-mismatch [:seed :mass])

              (not (every? #(and (number? %) (<= 0 %)) (vals (:mass seed))))
              (refuse! :preference-negative-mass [:seed :mass])

              (not= 1 (reduce + (vals (:mass seed))))
              (refuse! :preference-not-normalized [:seed :mass])

              :else
              {:ok true
               :schema machine-preference-schema
               :model (select-keys (:model model-context) [:id :revision])
               :unconditional-on :unit
               :vertices
               {:nouns {:status :named-empty :support []}
                :verbs {:status :named-empty :support []}
                :organization
                {:status :ruled
                 :support (mapv tagged-organization (sort organization-support))}
                :evidence {:status :unruled :support :owed}}
               :distribution
               {:support (mapv tagged-organization (sort organization-support))
                :mass (into (sorted-map)
                            (map (fn [[outcome mass]]
                                   [(tagged-organization outcome) mass]))
                            (:mass seed))}
               :named-zero-outcomes
               (mapv tagged-organization
                     (sort (for [[outcome mass] (:mass seed) :when (zero? mass)] outcome)))
               :ordered-layers (mapv :layer/id layers)
               :provenance provenance
               :open-obligations #{:evidence-vocabulary}})))))))

(defn require-machine-preference
  "Return the constructed C only when every outcome vertex is consumable.
   V1 always refuses at evidence rather than filling the owed vocabulary."
  [model-context loader-input]
  (let [constructed (construct-machine-preference model-context loader-input)]
    (if-not (:ok constructed)
      constructed
      (let [require-vocabulary (requiring-resolve 'futon2.aif.machine-model/require-outcome-vocabulary)
            evidence (require-vocabulary model-context :evidence)]
        (if (:ok evidence) constructed evidence)))))

(defn preference-mass
  "Read one tagged mass. Unknown outcomes are typed refusals, not zero."
  [constructed outcome]
  (if-not (:ok constructed)
    constructed
    (if (contains? (get-in constructed [:distribution :mass]) outcome)
      {:ok true :mass (get-in constructed [:distribution :mass outcome])}
      (refuse! :unknown-preference-outcome [:distribution :mass outcome]))))

(defn unsupported-risk
  "Expose Q-positive/C-zero as a typed infinite-KL boundary. No smoothing."
  [constructed outcome q-mass]
  (let [c (preference-mass constructed outcome)]
    (cond
      (not (:ok c)) c
      (or (not (number? q-mass)) (neg? q-mass))
      (refuse! :invalid-predictive-mass [:q outcome])
      (and (pos? q-mass) (zero? (:mass c)))
      {:ok false
       :refusal {:kind :positive-prediction-at-zero-preference
                 :path [:distribution :mass outcome]
                 :risk :infinite}}
      :else {:ok true :risk-boundary :finite})))
