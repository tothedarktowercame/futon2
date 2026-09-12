#!/usr/bin/env bb
(require '[babashka.classpath :as cp] '[clojure.edn :as edn])
(cp/add-classpath "scripts")
(require '[witnesses.node-witness :as nw])

(def w (first (:node-witnesses
               (edn/read-string (slurp "checks/witness-fragments/PredictiveOutcomeKernel.edn")))))
(def receipts (:records (edn/read-string
                         (slurp "holes/labs/wm-contract/runs/row-16-forward-model-2026-09-12/verification-receipts.edn"))))
(def r (get receipts (:id w)))
(def ctx (nw/context {"futon2" "/home/joe/code/futon2"
                      "mathlib4" "/home/joe/code/mathlib4"
                      "p4ng" "/home/joe/code/p4ng"}))
(nw/need! (= (nw/subject w) (:subject r)) (:id w)
          :node-witness-subject-mismatch :verification)
(nw/verify-kind! ctx w r)
(prn {:pass? true :tests 1 :assertions 1})
