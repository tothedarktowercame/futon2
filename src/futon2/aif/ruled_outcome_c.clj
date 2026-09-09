(ns futon2.aif.ruled-outcome-c
  "The ruled outcome preference surface and its EFE-fold declaration."
  (:require [clojure.set :as set]
            [futon2.aif.full-loop-cohort :as cohort]))

(def seeded-positive-masses
  "Exact-ratio transcription of `:C-seeded` at
   `futon2:holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:11`.
   That record says this illustrates a registry seed and is not a ruling at
   `futon2:holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:15`."
  {:grounded-change 1/2
   :agent-unavailable 1/8
   :build-failed 1/8
   :incomplete 1/8
   :no-selection 1/8})

(def observed-dispositions
  "Observed dispositions, derived once from the positive seed keys recorded at
   `futon2:holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:11`."
  (set (keys seeded-positive-masses)))

(def named-zero-dispositions
  "The seven named-zero obligations, derived from the gated authority at
   `futon2:src/futon2/aif/full_loop_cohort.clj:31-33` rather than retyped."
  (set/difference cohort/outcome-kinds observed-dispositions))

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
  {:organization {:status :ruled :carrier cohort/outcome-kinds}
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
  {:support cohort/outcome-kinds
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
