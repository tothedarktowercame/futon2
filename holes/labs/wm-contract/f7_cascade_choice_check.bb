#!/usr/bin/env bb
;; F7 -- the cascade-policy-choice check.
;;
;;   bb f7_cascade_choice_check.bb <decision-record.edn>
;;
;; Answers ONE question about a decision record: does it exhibit the falsifier
;; FUNDAMENTALS.edn:272 names for :fundamental/machine-policy-carrier -- "a
;; runtime decision in which two or more cascades for one selected mission are
;; constructed and scored"? Exit 0 and print ACCEPT, or exit 1 and print
;; REFUSE with a typed :error key.
;;
;; WHY THE CHECK IS NOT "count the candidates". Every refusal below is a way a
;; record could carry two entries and still not be a policy choice: the same
;; policy twice, a policy for another mission, a policy with no score, a
;; truncated construction whose score describes patterns it does not show, a
;; menu that was never ranked. The census that motivated this row
;; (runs/F1-machine-q/02-policy-family-census.edn) measured
;; :most-distinct-cascades-for-one-target-in-one-record 1 across 889 decisions,
;; so the interesting failure is not absence but a false two.
;;
;; READ-ONLY. Reads the record and the trace file it cites; writes nothing.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.walk :as walk])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
(def reader-opts {:default (fn [_tag value] value)})

(defn refuse [error msg]
  (println (format "f7_cascade_choice_check: REFUSE %s -- %s" error msg))
  (flush)
  (System/exit 1))

;; ---------------------------------------------------------------------------
;; The policy identity, TRANSCRIBED from futon2/src/futon2/aif/cascade_prior.clj
;; :19-66 (canonical-form, canonical-semilattice, policy-key). It is a copy on
;; purpose: this checker is a reader of records and stays runnable under bb
;; without the futon2 classpath, exactly as f1_policy_family_census.bb keeps its
;; copy of habit-prior/policy-key. A drift between the two is a finding, and the
;; pointer above is where to check -- negative_controls.sh 12i runs the real
;; namespace over the committed record and compares, so the drift is measured
;; rather than trusted.
;; ---------------------------------------------------------------------------

(defn canonical-form [value]
  (walk/postwalk
   (fn [x]
     (cond
       (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2))) x)
       (set? x) (vec (sort-by pr-str x))
       :else x))
   value))

(defn canonical-semilattice [semilattice]
  (cond
    (map? semilattice)
    (canonical-form
     (into {}
           (map (fn [[edge-type edges]]
                  [edge-type (if (sequential? edges)
                               (vec (sort-by pr-str (map canonical-form edges)))
                               (canonical-form edges))]))
           semilattice))

    (sequential? semilattice)
    (vec (sort-by pr-str (map canonical-form semilattice)))

    :else nil))

(defn policy-key [cascade]
  (let [mission (:mission cascade)
        shown (:shown cascade)
        semilattice (:semilattice cascade)]
    (when (and (map? cascade)
               (some? mission)
               (vector? shown)
               (seq shown)
               (every? string? shown)
               (or (map? semilattice) (sequential? semilattice)))
      [:pattern-cascade
       (str mission)
       shown
       (canonical-semilattice semilattice)])))

;; ---------------------------------------------------------------------------

(defn finite-number? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn read-records [path]
  (edn/read-string reader-opts (str "[" (slurp path) "]")))

(defn check-basis
  "The record must be about a decision the machine actually recorded. A record
   whose cited run-id is absent, or whose named target is not the target that
   run committed to, is a claim about a tick that did not happen."
  [record]
  (let [{:keys [trace-file run-id decision-target]} (:basis record)
        f (io/file (if (.isAbsolute (io/file (str trace-file)))
                     (str trace-file)
                     (str repo-root "/" trace-file)))]
    (when-not (and (string? trace-file) (string? run-id) (string? decision-target))
      (refuse ":error/basis-incomplete"
              (format "the record names trace-file %s run-id %s decision-target %s"
                      (pr-str trace-file) (pr-str run-id) (pr-str decision-target))))
    (when-not (.exists f)
      (refuse ":error/basis-trace-not-found" (str "no trace file at " (.getPath f))))
    (let [recorded (first (filter #(= run-id (:run/id %)) (read-records f)))]
      (when-not recorded
        (refuse ":error/basis-run-not-found"
                (format "%s carries no record with :run/id %s" trace-file (pr-str run-id))))
      (let [actual (get-in recorded [:decision :action :target])]
        (when-not (= actual decision-target)
          (refuse ":error/basis-does-not-match-record"
                  (format "run %s committed to %s, the record says %s"
                          run-id (pr-str actual) (pr-str decision-target)))))
      decision-target)))

(defn check-candidates [record target]
  (let [candidates (:candidates record)]
    (when-not (and (sequential? candidates) (every? map? candidates))
      (refuse ":error/no-candidate-list" "the record carries no :candidates vector"))
    (when (< (count candidates) 2)
      (refuse ":error/single-cascade"
              (format "%d cascade(s) for %s -- one construction is not a choice between policies"
                      (count candidates) target)))
    (doseq [c candidates]
      (when-not (= target (:mission c))
        (refuse ":error/mixed-mission"
                (format "a candidate is for mission %s, the decision is for %s -- the strategic and tactical levels are separate"
                        (pr-str (:mission c)) (pr-str target))))
      (when (true? (:truncated c))
        (refuse ":error/truncated-candidate"
                (format "a candidate is :truncated, so its score describes patterns it does not show (shown %d of size %s)"
                        (count (:shown c)) (pr-str (:size c)))))
      (when-not (finite-number? (:cascade-score c))
        (refuse ":error/unscored-candidate"
                (format "a candidate carries :cascade-score %s -- constructed is not scored"
                        (pr-str (:cascade-score c))))))
    (let [recomputed (mapv policy-key candidates)]
      (when (some nil? recomputed)
        (refuse ":error/no-policy-identity"
                "a candidate has no stable cascade identity (mission, :shown or :semilattice malformed)"))
      (doseq [[c k] (map vector candidates recomputed)]
        (when-not (= (:policy-key c) k)
          (refuse ":error/forged-policy-key"
                  (format "a candidate's recorded :policy-key is not the identity of its own fields (recorded %s)"
                          (pr-str (take 3 (:policy-key c)))))))
      (when-not (= (count (distinct recomputed)) (count recomputed))
        (refuse ":error/duplicate-policy-identity"
                (format "%d candidates carry %d distinct identities -- the same policy listed twice is not a choice"
                        (count recomputed) (count (distinct recomputed)))))
      recomputed)))

(defn check-selection [record identities]
  (let [selection (:selection record)
        ranked (:ranked-candidates selection)]
    (when-not (map? selection)
      (refuse ":error/unranked" "the record carries no :selection -- constructed but not scored as a menu"))
    (when-not (= :dark-cascade-ranking (:status selection))
      (refuse ":error/unranked"
              (format ":selection :status is %s, not :dark-cascade-ranking" (pr-str (:status selection)))))
    (when-not (= (count ranked) (count identities))
      (refuse ":error/ranking-population"
              (format "%d candidates constructed, %d ranked" (count identities) (count ranked))))
    (when-not (= (set (map :policy-key ranked)) (set identities))
      (refuse ":error/ranking-population"
              "the ranked policies are not the constructed policies"))
    (let [weights (map :cascade-selection-weight ranked)]
      (when-not (every? finite-number? weights)
        (refuse ":error/unscored-candidate"
                (format "a ranked candidate has weight %s" (pr-str (remove finite-number? weights)))))
      (when (> (Math/abs (- 1.0 (reduce + 0.0 (map double weights)))) 1.0e-9)
        (refuse ":error/unnormalized-weights"
                (format "the selection weights sum to %s, not 1" (reduce + 0.0 (map double weights))))))
    selection))

(let [args *command-line-args*
      path (first args)]
  (when-not path
    (binding [*out* *err*] (println "usage: f7_cascade_choice_check.bb <decision-record.edn>"))
    (System/exit 2))
  (when-not (.exists (io/file path))
    (refuse ":error/no-record" (str "no file at " path)))
  (let [record (try (edn/read-string reader-opts (slurp path))
                    (catch Exception e
                      (refuse ":error/unreadable-record" (.getMessage e))))]
    (when-not (and (map? record) (= :wm/cascade-policy-decision-v1 (:schema record)))
      (refuse ":error/not-a-cascade-policy-decision"
              (format "schema is %s" (pr-str (and (map? record) (:schema record))))))
    (when-not (false? (:enacted? record))
      (refuse ":error/claims-enactment"
              (format ":enacted? is %s -- this record's producer selects and enacts nothing"
                      (pr-str (:enacted? record)))))
    (let [target (check-basis record)
          identities (check-candidates record target)
          selection (check-selection record identities)]
      (printf "f7_cascade_choice_check: ACCEPT %d distinct constructed-and-scored cascades for %s (run %s, governed-by %s)%n"
              (count identities) target
              (get-in record [:basis :run-id])
              (pr-str (:governed-by selection)))
      (flush)
      (System/exit 0))))
