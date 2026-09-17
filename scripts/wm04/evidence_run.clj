(ns wm04.evidence-run
  "WM-04 evidence run (claude-4, 2026-09-17). WM-04's own build tokens are
  observed mechanically at pinned shas (futon2.aif.observation-checks). The
  S-1 contract gives each token its class. S-3 token-likelihood-rates builds
  the rate map, which is checkable, so it is exact. cascade-model-manifest's
  token-likelihood and predict-observations then turn the observed state into
  the observation distribution. No judgement rate has been admitted (the pilot was not reviewed, so there is no
  RESULT.edn), so the run shows an observed absence as well as presences. Writes holes/labs/wm-contract/wm04-evidence/RUN.edn."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.observation-checks :as oc]
            [futon2.aif.observation-rates :as rates]))

(def tokens
  {:admission-records    {:class :C3 :repo "futon2" :sha "3f601f60" :path "src/futon2/aif/observation_admission.clj"}
   :raw-rate-counts      {:class :C4 :repo "futon2" :sha "6bb94fee" :path "src/futon2/aif/observation_rates.clj" :decl "(defn rates-by-class"}
   :mechanical-checks    {:class :C3 :repo "futon2" :sha "b81e5996" :path "src/futon2/aif/observation_checks.clj"}
   :locator-requirement  {:class :C4 :repo "futon2" :sha "705adb39" :path "src/futon2/aif/cascade_problems.clj" :decl "(def checkable-classes"}
   :checkable-kernel-law {:class :C4 :repo "mathlib4" :sha "889429e6bf" :path "DarkTower/WarMachine/TokenObservation.lean" :decl "theorem tokenLikelihood_checkable"}
   :observation-contract-entry {:class :C5 :repo "mathlib4" :sha "3726659d84" :bundle-path "DarkTower/WarMachine/machine-contracts-2026-09-17-r14/machine-contracts.json" :entry "wm-token-observation"}
   :construction-receipt-locators {:class :C4 :repo "futon2" :sha "9f7eeb46" :path "src/futon2/aif/construction.clj" :decl ":unlocated-tokens"}
   :test-warrant-check   {:class :C4 :repo "futon2" :sha "75708b9e" :path "src/futon2/aif/observation_checks.clj" :decl "(defn check-test-warrant"}
   :raw-rate-tests-pass  {:class :C2 :repo "futon2" :entry-id "test-registry-0b4a2378bb224daa499a8012209eff3a35208871e529c7b5c1eb578364496978" :ns "futon2.aif.observation-rates-test"}
   :contract-modules-build {:class :C1 :repo "mathlib4" :entry-id "test-registry-0a3802b77bfcbbd3db483605f6f532bdab60e53c9b27739d8e2c2b82860b4c40" :module "DarkTower.WarMachine.MachineContracts" :path "DarkTower/WarMachine/TokenObservation.lean" :decl "theorem tokenLikelihood_checkable"}
   :judgement-rate-admitted {:class :C4 :repo "futon2" :sha "75708b9e" :path "holes/labs/wm-contract/wm04-pilot/RESULT.edn" :decl ":rates"}})

(let [contract (edn/read-string (slurp "resources/wm/observation-contract.edn"))
      observed (oc/observe tokens)
      universe (set (keys tokens))
      rate-map (rates/token-likelihood-rates nil contract (into {} (map (fn [[t l]] [t (:class l)])) tokens))
      kernel-rates (into {} (map (fn [[t r]] [t (select-keys r [:false-neg :false-pos])])) rate-map)
      state (:observed observed)
      dist (m/observation-distribution kernel-rates state)
      q {state 1}
      predicted (m/predict-observations kernel-rates q)
      run {:schema :wm04-evidence-run-v1
           :tokens tokens
           :observed-state (vec (sort state))
           :not-observed (vec (sort (remove state universe)))
           :refused (:refused observed)
           :results (:results observed)
           :rate-map rate-map
           :observation-distribution dist
           :predicted-observations predicted
           :exact? (= dist {state 1})}]
  (io/make-parents "holes/labs/wm-contract/wm04-evidence/RUN.edn")
  (spit "holes/labs/wm-contract/wm04-evidence/RUN.edn" (with-out-str (pp/pprint run)))
  (prn (select-keys run [:observed-state :not-observed :refused :observation-distribution :exact?])))
