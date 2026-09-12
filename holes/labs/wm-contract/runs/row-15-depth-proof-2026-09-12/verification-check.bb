#!/usr/bin/env bb
(require '[babashka.classpath :as cp] '[clojure.edn :as edn])
(cp/add-classpath "scripts")
(require '[witnesses.node-witness :as nw])

(def witness
  (first (:node-witnesses
          (edn/read-string (slurp "checks/witness-fragments/machineDepth.edn")))))
(def receipt
  (get-in (edn/read-string
           (slurp "holes/labs/wm-contract/runs/row-15-depth-proof-2026-09-12/verification-receipts.edn"))
          [:records (:id witness)]))
(def ctx
  (nw/context {"futon2" "/home/joe/code/futon2"
               "mathlib4" "/home/joe/code/mathlib4"
               "p4ng" "/home/joe/code/p4ng"}))

(nw/need! (= (nw/subject witness) (:subject receipt)) (:id witness)
          :node-witness-subject-mismatch :verification)
(nw/verify-kind! ctx witness receipt)
(prn {:pass? true :tests 1 :assertions 1})
