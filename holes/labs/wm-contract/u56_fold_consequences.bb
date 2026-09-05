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
;;
;; THE READINGS ARE NOT INTERCHANGEABLE, so each gets its own scenario. (A) "the
;; run's tensions are all cashed" can only speak about a run that HAS attributed
;; tensions, and where it speaks today it says NOT-all-cashed; (C) "the run
;; minted none, vacuously satisfied" can only speak about a run with NONE. They
;; partition the three runs rather than agreeing on them, and an earlier version
;; of this script gave both a single ":tensions-cashed green everywhere" line,
;; which asserted a green under (A) that §4 of C511 rules out in the same breath.
;;
;; The per-run inputs each scenario rests on -- how many tensions the committed
;; ledger attributes to the run, and how many of those are cashed -- are MEASURED
;; here from tension-ledger.edn, not asserted, and printed before the folds.
;;
;; S1a IS NOW THE SHIPPED READING (:AD1, 2026-09-05, aif-equations.edn :choices
;; :tensions-cashed-reading). This script still prints all six scenarios and is
;; still a discovery record: the deposited rows it folds are NOT repaired, so S0
;; remains what the ledger holds and S1a remains what the check would deposit if
;; those runs were deposited today. The gap between the two is the divergence the
;; adoption reports rather than repairs.
(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str] '[clojure.set :as set])

(def lab (str (System/getProperty "user.home") "/code/futon2/holes/labs/wm-contract"))
(def ledger (edn/read-string (slurp (str lab "/run-era-ledger.edn"))))
(def catalogue (vec (sort (map :check/id (:ledger/check-catalogue ledger)))))
(def tensions (edn/read-string (slurp (str lab "/tension-ledger.edn"))))

(def all-runs ["2026-09-01-s5" "2026-09-04-re5" "2026-09-04-010-accepted"])

(defn run-tick-ids
  "The run's tick ids, off its own store's receipt filenames (u41:456-465)."
  [run-id]
  (let [d (io/file lab "runs" run-id)]
    (if (.isDirectory d)
      (->> (.listFiles d)
           (map #(.getName ^java.io.File %))
           (keep #(second (re-matches #"tick-run-record-\d{4}-\d{2}-\d{2}-(.+)\.edn" %)))
           sort vec)
      [])))

(defn attributed
  "The committed tensions this run can be read off, by the same substring test
   u41_tension_ledger.bb:615-616 uses -- but scoped to ONE tension's own form
   rather than to the whole ledger text, so the result is per-run rather than
   per-ledger. Returns [{:id :status}].

   :U60 gave u41 that per-tension form as an API -- `attribute-tension:589-621`,
   which prefers :tension/provenance :records and falls back to this substring
   test -- so this copy is now a second implementation of the FALLBACK half. It
   is left in place because this script is a U56 discovery record, not a
   consumer of the deposit path; a reader comparing the two should expect them
   to agree only on tensions that declare no runs."
  [run-id]
  (let [needles (cons run-id (run-tick-ids run-id))]
    (vec (for [t (:tensions tensions)
               :let [text (pr-str t)]
               :when (some #(str/includes? text %) needles)]
           {:id (:tension/id t) :status (:tension/status t)}))))

(def inputs
  (into {} (for [r all-runs]
             (let [ts (attributed r)]
               [r {:attributed ts
                   :n-attributed (count ts)
                   :n-cashed (count (filter #(= :cashed (:status %)) ts))}]))))

(def cashed-events (count (filter #(= :cashed (:event/type %)) (:events tensions))))

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

;; Which runs each reading can speak about, DERIVED from the measurement above.
;; (A) speaks where the ledger attributes at least one tension to the run; today
;; every such run has an uncashed one, so (A) never yields a green on this
;; ledger. (C) speaks where it attributes none.
(def a-runs (set (for [[r m] inputs :when (pos? (:n-attributed m))] r)))
(def a-all-cashed (set (for [[r m] inputs
                             :when (and (pos? (:n-attributed m))
                                        (= (:n-attributed m) (:n-cashed m)))] r)))
(def c-runs (set (for [[r m] inputs :when (zero? (:n-attributed m))] r)))
(def b-runs (set (for [r all-runs
                       :let [text (slurp (str lab "/tension-ledger.edn"))]
                       :when (or (str/includes? text r)
                                 (some #(str/includes? text %) (run-tick-ids r)))] r)))

(defn show [label rs]
  (println label)
  (doseq [r (fold-by-run rs)]
    (println (format "  %-28s %-11s %s" (:run-id r) (:status r) (pr-str (:because r))))))

(println "MEASURED INPUTS (tension-ledger.edn, current commit)")
(doseq [r all-runs]
  (let [m (inputs r)]
    (println (format "  %-28s attributed %d, cashed %d %s"
                     r (:n-attributed m) (:n-cashed m)
                     (pr-str (mapv :id (:attributed m)))))))
(println (format "  :cashed events in the whole ledger: %d" cashed-events))
(println (format "  (A) can speak about: %s ; of those, all-cashed: %s"
                 (pr-str (vec (sort a-runs))) (pr-str (vec (sort a-all-cashed)))))
(println (format "  (B) can speak about: %s" (pr-str (vec (sort b-runs)))))
(println (format "  (C) can speak about: %s" (pr-str (vec (sort c-runs)))))
(println)

(show "S0 -- as deposited:" rows)

(println (str "S1a -- reading (A) 'the run's tensions are all cashed', uncashed => :red.\n"
              "       Speaks only where a tension is attributed; " (pr-str (vec (sort c-runs)))
              " is out of its scope and stays as deposited."))
(doseq [r (fold-by-run (substitute rows :tensions-cashed (set/difference a-runs a-all-cashed) :red))]
  (println (format "  %-28s %-11s %s" (:run-id r) (:status r) (pr-str (:because r)))))

(show (str "S1b -- reading (A), uncashed => :typed-absence (no :cashed event has ever been\n"
           "       written, so the cashing signal is unobservable rather than failed):")
      (substitute rows :tensions-cashed (set/difference a-runs a-all-cashed) :typed-absence))

(show "S2 -- reading (B): green where the ledger names the run:"
      (substitute rows :tensions-cashed b-runs :green))

(show (str "S3 -- reading (C): vacuous green only where the ledger attributes no tension\n"
           "       to the run (and see C511 §4: that emptiness is not establishable on the\n"
           "       present schema, :event/at being a date -- this is (C)'s value IF GRANTED):")
      (substitute rows :tensions-cashed c-runs :green))

(show "S4 -- (A) with a red, composed with (C): every run in scope of some reading:"
      (-> rows
          (substitute :tensions-cashed (set/difference a-runs a-all-cashed) :red)
          (substitute :tensions-cashed c-runs :green)))

(show "S5 -- all four standing absences green on the accepted run:"
      (reduce (fn [rs c] (substitute rs c #{"2026-09-04-010-accepted"} :green))
              rows
              [:tensions-cashed :flip-readiness :per-node-runtime-validation :rationale-regret]))
