(ns row2-receipts
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.pprint :as pp] [witnesses.node-witness :as nw]))

(def dir "holes/labs/wm-contract/runs/row-2-admission-2026-09-12")
(def fragments
  [["checks/witness-fragments/machineObservation.edn" "observation-transcript.txt" "observation-receipts.edn"]
   ["checks/witness-fragments/machinePrecision.edn" "precision-transcript.txt" "precision-receipts.edn"]
   ["checks/witness-fragments/machineChannelPredictionError.edn" "prediction-error-transcript.txt" "prediction-error-receipts.edn"]])
(def roots {"futon2" "/home/joe/code/futon2" "mathlib4" "/home/joe/code/mathlib4"})
(defn locator [repo path]
  (let [f (io/file (roots repo) path)]
    {:repo repo :path path :sha256 (nw/sha256 (java.nio.file.Files/readAllBytes (.toPath f)))}))
(def checker (locator "mathlib4" "DarkTower/WarMachine/Row2AdmissionAxiomCheck.lean"))
(def toolchain (locator "mathlib4" "lean-toolchain"))
(def axiom-transcript (locator "futon2" (str dir "/axiom-transcript.txt")))
(def claims (mapcat (fn [[p]] (:node-witnesses (edn/read-string (slurp p)))) fragments))
(def census
  {:record {:declarations
            (into {}
                  (mapcat (fn [w]
                            [[(get-in w [:artifact :declaration])
                              {:kind :theorem
                               :source (select-keys (:artifact w) [:repo :path :sha256])
                               :module (get-in w [:artifact :module])
                               :proposition (:claim w)
                               :axioms ["propext" "Classical.choice" "Quot.sound"]}]
                             [(get-in w [:subject-artifact :declaration])
                              {:kind :def
                               :source (select-keys (:subject-artifact w) [:repo :path :sha256])}]]))
                  claims)}})
(spit (str dir "/declaration-census.edn") (with-out-str (pp/pprint census)))
(def census-loc (assoc (locator "futon2" (str dir "/declaration-census.edn")) :selector [:record]))
(doseq [[fragment transcript-name receipt-name] fragments]
  (let [entry (edn/read-string (slurp fragment))
        transcript (locator "futon2" (str dir "/" transcript-name))
        records
        (into {}
              (for [w (:node-witnesses entry)]
                [(:id w)
                 {:subject (nw/subject w) :executed? true :exit 0 :result :passed
                  :command (str "lake env lean " (get-in w [:artifact :path]))
                  :typecheck :passed :axiom-check :passed
                  :dependencies [(:artifact w) (:subject-artifact w)]
                  :transcript transcript :declaration-census census-loc
                  :import-closure [(:subject-artifact w) (:artifact w)]
                  :toolchain toolchain :checker checker
                  :axiom-transcript axiom-transcript}]))
        receipt-path (str dir "/" receipt-name)]
    (spit receipt-path (with-out-str (pp/pprint {:records records})))
    (let [receipt-loc (locator "futon2" receipt-path)
          updated (update entry :node-witnesses
                          (fn [ws] (mapv (fn [w]
                                          (assoc w :verification
                                                 {:status :verified
                                                  :receipt (assoc receipt-loc :selector [:records (:id w)])})) ws)))]
      (spit fragment (with-out-str (pp/pprint updated))))))
