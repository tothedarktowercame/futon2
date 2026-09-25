(ns futon2.aif.flight-grain-gate-test
  "M-wm-wiring row 5: the enactment step calls grain-gate before the grain
  attempt's commit is asked for, and records the pass or the typed refusal
  on the attempt. The grain is click-001's (futon3c roles.clj seat-for, the
  role grain); a fixture dispatch stands in for the seat."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight-runner :as fr]))

(def role-grain
  (:grain (edn/read-string (slurp "/home/joe/code/futon3c/holes/labs/M-futon-seams/exemplar/click-001-enactment.edn"))))

(def provider-grain
  (:grain (edn/read-string (slurp "/home/joe/code/futon3c/holes/labs/M-futon-seams/exemplar/click-001-outcome.edn"))))

(def interps {:p/grain {:produces #{:t/roles} :grain role-grain} :p/after {:produces #{:t/after}}})
(def click {:click-id "run-g" :chosen {:candidate :cand/g :precedence [:p/grain :p/after]}})

(defn- enact [calls planned]
  (fr/enact-fn {:dispatch-step! (fn [step]
                                  (swap! calls conj (select-keys step [:pattern :phase]))
                                  (if (= :plan (:phase step))
                                    {:grain planned}
                                    {:commit (str "c-" (name (:pattern step))) :produced :t/x
                                     :check {:class :fixture}}))
                :check-fn (constantly {:observed true})
                :interpretations (constantly interps)}))

(deftest the-gate-runs-before-the-grain-commit-and-its-pass-is-recorded
  (let [calls (atom [])
        {:keys [enactment]} ((enact calls role-grain) {:flight/id "f" :target "M-t"} click)
        a (first (:attempts enactment))]
    (is (= {:pattern :p/grain} (:grain-attempt enactment)))
    (is (= [{:pattern :p/grain :phase :plan} {:pattern :p/grain :phase :commit} {:pattern :p/after :phase :commit}]
           @calls) "plan, gate, then commit")
    (is (= {:status :pass} (:grain-gate a)))
    (is (= "c-grain" (:commit a)))
    (is (true? (:success a)))
    (is (= role-grain (:grain enactment)))
    (is (not (contains? enactment :grain-gate)) "the gate's result is on the attempt")))

(deftest a-grain-mismatch-is-recorded-and-not-committed
  (let [calls (atom [])
        {:keys [enactment]} ((enact calls provider-grain) {:flight/id "f" :target "M-t"} click)
        a (first (:attempts enactment))]
    (is (= :grain-mismatch (get-in a [:grain-gate :reason])))
    (is (= :grain-gate-refused (:not-committed a)))
    (is (false? (:success a)))
    (is (not-any? #(= {:pattern :p/grain :phase :commit} %) @calls) "no commit asked after the refusal")
    (is (= [{:kind :grain-gate-refused :pattern :p/grain :reason :grain-mismatch}]
           (get-in enactment [:conformance :deviations])))
    (is (= 2 (count (:attempts enactment))) "the flight's enactment continues past the refusal")))
