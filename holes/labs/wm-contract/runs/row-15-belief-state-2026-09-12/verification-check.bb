#!/usr/bin/env bb
(require '[babashka.classpath :as cp]
         '[clojure.edn :as edn])

(cp/add-classpath "scripts")
(require '[witnesses.node-witness :as nw])

(def roots {"futon2" "/home/joe/code/futon2"
            "mathlib4" "/home/joe/code/mathlib4"
            "p4ng" "/home/joe/code/p4ng"})
(def fragment (edn/read-string (slurp "checks/witness-fragments/machineBeliefState.edn")))
(def receipts (:records (edn/read-string
                         (slurp "holes/labs/wm-contract/runs/row-15-belief-state-2026-09-12/verification-receipts.edn"))))
(def ctx (nw/context roots))

(doseq [w (:node-witnesses fragment)]
  (let [r (get receipts (:id w))]
    (nw/need! (= (nw/subject w) (:subject r)) (:id w)
              :node-witness-subject-mismatch :verification)
    (nw/verify-kind! ctx w r)))

(prn {:pass? true :tests (count (:node-witnesses fragment)) :assertions 2})
