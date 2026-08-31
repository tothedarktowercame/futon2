#!/usr/bin/env bb
(require '[babashka.classpath :as cp])
(cp/add-classpath "src")

(ns scripts.emit-preference-stack-witness
  (:require [clojure.pprint :as pprint]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]))

;; This is deliberately an emitter, not a checker.  It invokes the production
;; scoring boundary and serializes the value that boundary records.  The
;; independent shape check never invokes compute-efe.
(def fixture
  {:state {:observation {} :belief (belief/initial-belief-state [:m1])}
   :action {:type :no-op}
   :opts {:goal-outcome-entries []}})

(defn -main [& _]
  (let [{:keys [state action opts]} fixture
        result (efe/compute-efe state action opts)]
    (pprint/pprint
     {:witness/type :PreferenceStackWitness
      :witness/version 1
      :recorded-at "2026-08-31T15:38:00Z"
      :producer {:function 'futon2.aif.efe/compute-efe
                 :field :preference-stack
                 :source "src/futon2/aif/efe.clj:92-124,781"}
      :input {:kind :runtime-dry-run
              :action (:action fixture)
              :goal-outcome-entry-count 0}
      :consumer {:check "checks/preference_stack_witness_shape_check.clj"
                 :binding :preferenceStackLiveRecorded}
      :preference-stack (:preference-stack result)})))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
