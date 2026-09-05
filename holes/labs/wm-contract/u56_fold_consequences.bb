#!/usr/bin/env bb
;; U56 §4 -- the fold consequences of each reading of :tensions-cashed, computed
;; by applying run_era_ledger.bb's own run-fold rule to the ledger's rows with
;; verdicts substituted IN MEMORY.
;;
;;   bb u56_fold_consequences.bb
;;
;; READ-ONLY AND NO ROW IS WRITTEN. The run-era ledger is append-only and refuses
;; a divergent row for an existing (run-id, check-id) (run_era_ledger.bb:241-243),
;; so a scenario cannot be deposited even in principle; it is computed instead.
;; The fold rule replicated below is run_era_ledger.bb:432-451 -- missing check
;; => :incomplete, any :red => :red, any :typed-absence => :incomplete, else
;; :green.
(require '[clojure.edn :as edn])

(def lab (str (System/getProperty "user.home") "/code/futon2/holes/labs/wm-contract"))
(def ledger (edn/read-string (slurp (str lab "/run-era-ledger.edn"))))
(def catalogue (vec (sort (map :check/id (:ledger/check-catalogue ledger)))))

(defn fold-by-run [rows]
  (vec (for [[run-id rs] (sort-by key (group-by :row/run-id rows))]
         (let [by-check (into (sorted-map) (map (juxt :row/check-id :row/verdict)) rs)
               missing (vec (sort (remove (set (keys by-check)) catalogue)))
               verdicts (set (vals by-check))
               absent (vec (sort (map key (filter #(= :typed-absence (val %)) by-check))))
               red (vec (sort (map key (filter #(= :red (val %)) by-check))))]
           {:run-id run-id
            :status (cond (seq missing) :incomplete
                          (contains? verdicts :red) :red
                          (contains? verdicts :typed-absence) :incomplete
                          :else :green)
            :because (cond (seq missing) {:checks-not-deposited missing}
                           (contains? verdicts :red) {:red-verdict red}
                           (contains? verdicts :typed-absence) {:typed-absences absent}
                           :else {:every-catalogued-check-green true})}))))

(defn substitute
  "Set CHECK's verdict to VERDICT for the named RUNS. In memory only."
  [rows check runs verdict]
  (mapv (fn [r] (if (and (= check (:row/check-id r)) (contains? runs (:row/run-id r)))
                  (assoc r :row/verdict verdict) r))
        rows))

(def rows (:rows ledger))
(def all-runs #{"2026-09-01-s5" "2026-09-04-re5" "2026-09-04-010-accepted"})
(def named-runs #{"2026-09-01-s5" "2026-09-04-re5"})

(defn show [label rs]
  (println label)
  (doseq [r (fold-by-run rs)]
    (println (format "  %-28s %-11s %s" (:run-id r) (:status r) (pr-str (:because r))))))

(show "S0 -- as deposited:" rows)
(show "S1 -- readings (A)/(C): :tensions-cashed green on all three:"
      (substitute rows :tensions-cashed all-runs :green))
(show "S2 -- reading (B): green only where the ledger names the run:"
      (substitute rows :tensions-cashed named-runs :green))
(show "S3 -- all four standing absences green on the accepted run:"
      (reduce (fn [rs c] (substitute rs c #{"2026-09-04-010-accepted"} :green))
              rows
              [:tensions-cashed :flip-readiness :per-node-runtime-validation :rationale-regret]))
