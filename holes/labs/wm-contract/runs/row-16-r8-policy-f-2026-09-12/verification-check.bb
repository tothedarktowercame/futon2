#!/usr/bin/env bb
(require '[babashka.classpath :as cp])
(cp/add-classpath "scripts")
(require '[clojure.edn :as edn]
         '[witnesses.node-witness :as nw])

(def roots {"futon2" "/home/joe/code/futon2" "mathlib4" "/home/joe/code/mathlib4"})
(def fragment (edn/read-string (slurp "checks/witness-fragments/machinePolicyFreeEnergy.edn")))
(def witness (first (:node-witnesses fragment)))
(def receipts (edn/read-string (slurp "holes/labs/wm-contract/runs/row-16-r8-policy-f-2026-09-12/verification-receipts.edn")))
(nw/verify-kind! (nw/context roots) witness (get-in receipts [:records (:id witness)]))
(prn {:status :passed :claim (:id witness)})
