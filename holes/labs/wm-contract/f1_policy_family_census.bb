#!/usr/bin/env bb
;; F1 slice 2 -- the POLICY-CARRIER census over the live wm-trace corpus.
;;
;;   bb f1_policy_family_census.bb
;;
;; READ-ONLY. Reads data/wm-trace only; writes nothing, takes no run lock,
;; deposits nothing. No wall-clock field in the output, so two runs over an
;; unchanged corpus print identically.
;;
;; WHAT IT MEASURES AND WHY. Slice 2 of :F1 is the machine-grain Q(o|pi)
;; instance, and it needs a machine policy carrier -- pi. The corpus can say
;; which of the two candidate grains the machine actually produced.
;;
;;   LEG 1, the SCORED grain. `futon2.aif.habit-prior/policy-key`
;;   (src/futon2/aif/habit_prior.clj:27-38) is the machine's own stable
;;   categorical identity for a selected action, folded into a Dirichlet-
;;   multinomial by `fold-record` (:81-84). This leg re-derives that identity
;;   over every recorded decision and counts the alphabet it spans.
;;
;;   LEG 2, the DECLARED grain, run as the FUNDAMENTAL'S OWN FALSIFIER.
;;   FUNDAMENTALS.edn:243-272 records :fundamental/machine-policy-carrier
;;   :uninhabited on both sides, and names the falsifier: "A runtime decision
;;   in which two or more cascades for one selected mission are constructed
;;   and scored". That verdict's runtime basis is a code read
;;   (cascade_prior.clj:1-12, forward_model.clj:26-33), not a corpus
;;   measurement. This leg runs the falsifier against the records: for each
;;   decision, the :apply-cascade candidates are grouped by :target and the
;;   DISTINCT :cascade payloads per target are counted. Two or more for one
;;   target in one decision would falsify the census entry.
;;
;; The outcome side of Q(o|pi) is NOT re-measured here: u56_outcome_census.bb
;; already covers it and is committed.
(require '[clojure.edn :as edn] '[clojure.java.io :as io])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
(def trace-dir (io/file repo-root "data/wm-trace"))

;; Old records carry tagged literals; keep the tag visible rather than throw,
;; so a reader change cannot silently drop records from the denominator.
(def reader-opts {:default (fn [_tag value] value)})

(defn trace-files []
  (->> (file-seq trace-dir)
       (filter #(re-find #"wm-trace-\d{4}-\d{2}-\d{2}\.edn$" (.getName %)))
       (sort-by #(.getName %))))

(defn records [f]
  (edn/read-string reader-opts (str "[" (slurp f) "]")))

;; Transcribed from src/futon2/aif/habit_prior.clj:27-38. Kept as a copy on
;; purpose: this script is a measurement of the corpus, not a load of the
;; namespace, so it stays runnable without the futon2 classpath. A drift
;; between the two is a finding, and the pointer above is where to check.
(defn policy-key [action]
  (when (and (map? action) (keyword? (:type action)))
    [(:type action)
     (cond
       (some? (:target action)) [:target (:target action)]
       (some? (:target-class action)) [:target-class (:target-class action)]
       :else [:unscoped nil])]))

(defn cascade-candidates [record]
  (->> (:ranked-actions record)
       (map #(or (:action %) %))
       (filter #(and (map? %) (= :apply-cascade (:type %))))))

(defn distinct-cascades-per-target [record]
  (into {}
        (map (fn [[target group]]
               [target (count (distinct (map :cascade group)))]))
        (group-by :target (cascade-candidates record))))

(let [files (trace-files)
      recs (mapcat records files)
      keys-seen (keep #(policy-key (get-in % [:decision :action])) recs)
      histogram (sort-by (juxt (comp - val) (comp str key)) (frequencies keys-seen))
      per-target (map distinct-cascades-per-target recs)
      widths (mapcat vals per-target)]
  (printf "files %d | records %d%n" (count files) (count recs))
  (println)
  (println "LEG 1 -- scored grain (habit-prior/policy-key)")
  (printf "  records with a stable policy identity   %d%n" (count keys-seen))
  (printf "  records with none (abstention/malformed) %d%n"
          (- (count recs) (count keys-seen)))
  (printf "  distinct policy identities              %d%n" (count histogram))
  (doseq [[k n] histogram]
    (printf "    %5d  %s%n" n (pr-str k)))
  (println)
  (println "LEG 2 -- declared grain (FUNDAMENTALS.edn:272 falsifier)")
  (printf "  records carrying an :apply-cascade candidate  %d%n"
          (count (filter seq (map cascade-candidates recs))))
  (printf "  most :apply-cascade candidates in one record  %d%n"
          (reduce max 0 (map (comp count cascade-candidates) recs)))
  (printf "  most DISTINCT cascades for one target, one record %d%n"
          (reduce max 0 widths))
  (printf "  records with >= 2 distinct cascades for one target %d%n"
          (count (filter (fn [m] (some #(>= % 2) (vals m))) per-target)))
  (println)
  (println (if (some #(>= % 2) widths)
             "  FALSIFIER FIRES: the census entry :uninhabited is refuted."
             "  falsifier does not fire: no decision offers two admissible cascades for one mission.")))
