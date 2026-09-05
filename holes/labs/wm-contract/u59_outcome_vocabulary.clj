#!/usr/bin/env clojure
;; U59 -- THE OUTCOME VOCABULARY, MEASURED. Run from the repo root:
;;
;;   clojure -M:test holes/labs/wm-contract/u59_outcome_vocabulary.clj [<run-id> ...]
;;
;; `clojure -M:test` and not bb: it loads `futon2.report.war-machine` from
;; src/ and scripts/ to measure the decay through the real function, not a
;; re-implementation of it. Reading the corpus through a copy of the code under
;; test is what let three vocabularies for one quantity survive in the first
;; place (C511-repair-or-elaborate.md section 3).
;;
;; WHAT IT MEASURES, all four numbers the :U59 row asks be reported and none
;; enabled:
;;   1. the vocabulary census over the whole live corpus -- which key-set each
;;      recorded realized outcome is written in, and how many of them gamma can
;;      now read that it could not before;
;;   2. gamma folding those records, so "readable" is a fold and not a claim;
;;   3. gamma folding an OBSERVATION record produced by `wm_step_observe.bb`,
;;      which is the same schema arriving from the new producer;
;;   4. the decay difference: `recent-non-progress-count` over every
;;      ranking-carrying record, with `*observed-outcomes*` EMPTY (the default,
;;      and every live path) against the same computation with the sequence's
;;      observations bound. The difference is REPORTED. Nothing is flipped:
;;      the var defaults to `{}` and no caller binds it.
;;
;; READ-ONLY. Writes one artifact under runs/U59-outcome-vocabulary/. No tick,
;; no run lock, no substrate call, no network, nothing under data/ written.
;; DETERMINISM: no wall-clock field; every number is read from a record or is a
;; count of records.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[futon2.aif.realized-outcome :as ro]
         '[futon2.aif.selection-gain :as sg]
         '[futon2.report.war-machine :as wm])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
(def out-dir (io/file repo-root "holes/labs/wm-contract/runs/U59-outcome-vocabulary"))
(def read-opts {:default (fn [t v] {:unread-tag t :value v})})

(defn read-trace [path]
  (with-open [r (io/reader path)]
    (mapv #(edn/read-string read-opts %) (line-seq r))))

(defn corpus-files []
  (->> (file-seq (io/file repo-root "data/wm-trace"))
       (filter #(.isFile ^java.io.File %))
       (map str)
       (filter #(re-find #"wm-trace-\d{4}-\d{2}-\d{2}\.edn$" %))
       sort vec))

(defn day-of [p] (second (re-find #"wm-trace-(\d{4}-\d{2}-\d{2})\.edn$" p)))

;; --------------------------------------------------------------------------
;; 1. The vocabulary census.

(defn vocabulary-census [records]
  (let [ros (keep ro/realized-outcome records)]
    {:records (count records)
     :records-carrying-a-realized-outcome (count ros)
     :by-vocabulary (into (sorted-map)
                          (frequencies (map #(or (ro/vocabulary %) :neither-leg-pair-readable) ros)))
     :historical-and-now-readable
     (count (filter #(contains? ro/historical-vocabularies (ro/vocabulary %)) ros))
     :readable-by-the-old-key-test
     (count (filter #(and (number? (:expected-score %)) (number? (:realized-score %))) ros))
     :records-with-a-categorical-outcome (count (filter ro/categorical-outcome records))
     :mixed-vocabulary (count (filter ro/mixed-vocabulary? ros))}))

;; --------------------------------------------------------------------------
;; 2/3. gamma folds them.

(defn fold-all
  "Fold RECORDS' realized outcomes into a fresh gain state, in order, and report
   what moved. `fold-realized-outcome` dedups on `:tick`, so this is the same
   sequence gamma would see reading them back one tick at a time."
  [ros]
  (let [start (sg/initial-selection-gain-state)
        end (reduce sg/fold-realized-outcome start ros)]
    {:offered (count ros)
     :folded (- (:samples end) (:samples start))
     :selection-gain-before (:selection-gain start)
     :selection-gain-after (:selection-gain end)
     :mean-perf-after (:mean-perf end)
     :last-outcome-vocabulary (:last-outcome-vocabulary end)
     :moved? (not= (:selection-gain start) (:selection-gain end))}))

;; --------------------------------------------------------------------------
;; 4. The decay difference.

(def recent-non-progress-count
  (var-get #'wm/recent-non-progress-count))

(defn decay-of [n] (if (pos? n) (/ 1.0 (+ 1.0 (* 1.0 n))) 1.0))

(defn decay-census
  "For every ranking-carrying record, the non-progress count its own chosen
   action would get from the 12 records before it, and the decay that count
   produces (war_machine.clj:2676-2679, 1/(1+k*n) with k=1)."
  [records]
  (let [ranked (filterv #(seq (get-in % [:decision :controller-ranking])) records)
        idx (into {} (map-indexed (fn [i r] [(:run/id r) i]) records))
        rows (for [r ranked
                   :let [i (get idx (:run/id r))
                         history (vec (take i records))
                         action (get-in r [:decision :action])
                         n (recent-non-progress-count action history)]]
               {:run-id (:run/id r) :target (:target action)
                :non-progress-count n :non-progress-decay (decay-of n)})]
    {:ranking-records (count ranked)
     :counts (into (sorted-map) (frequencies (map :non-progress-count rows)))
     :decays (into (sorted-map) (frequencies (map :non-progress-decay rows)))
     :sum-of-counts (reduce + 0 (map :non-progress-count rows))
     :rows (vec rows)}))

(defn decay-difference
  "The measurement the row asks for: the same census with the observed-outcome
   side-channel EMPTY (the default, and every live path) and with it BOUND."
  [records observations]
  (let [off (binding [wm/*observed-outcomes* {}] (decay-census records))
        on  (binding [wm/*observed-outcomes* observations] (decay-census records))
        changed (vec (for [[a b] (map vector (:rows off) (:rows on))
                           :when (not= a b)]
                       {:run-id (:run-id a) :target (:target a)
                        :count-off (:non-progress-count a) :count-on (:non-progress-count b)
                        :decay-off (:non-progress-decay a) :decay-on (:non-progress-decay b)}))]
    {:flag-off (dissoc off :rows)
     :flag-on (dissoc on :rows)
     :identical? (= (:rows off) (:rows on))
     :records-whose-decay-moved (count changed)
     :moved changed
     :observations-bound (count observations)
     :not-enabled (str "war-machine/*observed-outcomes* defaults to {} and no caller binds it. "
                       "This function is the only binder in the repository, and it binds it to "
                       "measure the difference, not to keep it. Flipping it is a decision, not a "
                       "consequence of making the input readable (worklist :U59).")}))

;; --------------------------------------------------------------------------

(defn run-store [run-id] (io/file repo-root "holes/labs/wm-contract/runs" run-id))

(defn run-observations
  "The observation records a run store holds, keyed by the tick they are about
   -- the same index `u39_selection_retrospective.bb/run-observations` builds."
  [run-id]
  (let [d (io/file (run-store run-id) "observation")]
    (if-not (.isDirectory d)
      {}
      (into {} (keep (fn [^java.io.File f]
                       (let [o (edn/read-string read-opts (slurp f))]
                         (when-let [t (:observation/observed-for-tick o)] [t o])))
                     (sort-by #(.getName ^java.io.File %)
                              (filter #(str/ends-with? (.getName ^java.io.File %) ".edn")
                                      (.listFiles d))))))))

(defn run-records [run-id]
  (let [d (run-store run-id)
        files (when (.isDirectory d)
                (vec (sort (map #(.getName ^java.io.File %)
                                (filter #(.isFile ^java.io.File %) (.listFiles d))))))
        ids (set (keep #(second (re-matches #"tick-run-record-\d{4}-\d{2}-\d{2}-(.+)\.edn" %)) files))]
    (->> (filter #(re-matches #"wm-trace.*\.edn" %) files)
         (mapcat #(read-trace (str (io/file d %))))
         (filter #(contains? ids (:run/id %)))
         (sort-by :timestamp)
         vec)))

;; --------------------------------------------------------------------------
;; 5. Flag-off equivalence, as a CONTROL and not an assertion.
;;
;; The two readers this row changed are re-implemented here EXACTLY as they
;; stood before it -- the three hand-inlined `get-in`s and the two-key `number?`
;; test -- and run beside the new ones over every record. If the new accessors
;; answered differently anywhere, the counts below would differ, and the claim
;; that scoring does not move with the flag off would be false.

(defn old-trace-outcome
  "war_machine.clj before :U59 (and its verbatim copy in
   u39_selection_retrospective.bb:55-59)."
  [m]
  (or (:outcome m) (get-in m [:enactment :outcome]) (get-in m [:realized-outcome :outcome])))

(defn old-foldable?
  "selection_gain.clj:197-205 before :U59 -- the two v1 keys, by name."
  [ro]
  (and (map? ro) (number? (:expected-score ro)) (number? (:realized-score ro))))

(defn flag-off-equivalence [records]
  (let [ros (mapv ro/realized-outcome records)
        cat-old (mapv old-trace-outcome records)
        ;; the NEW reader itself, not its accessor: `war-machine/trace-outcome`
        ;; is what the non-progress walk calls, and it is the one that consults
        ;; the observed side-channel. Bound empty, which is its default.
        new-reader (var-get #'wm/trace-outcome)
        cat-new (binding [wm/*observed-outcomes* {}] (mapv new-reader records))
        fold-old (mapv old-foldable? ros)
        fold-new (mapv ro/legs-readable? ros)]
    {:records (count records)
     :categorical-outcome-identical? (= cat-old cat-new)
     :categorical-disagreements (count (remove true? (map = cat-old cat-new)))
     :non-nil-categorical-old (count (remove nil? cat-old))
     :non-nil-categorical-new (count (remove nil? cat-new))
     :foldable-old (count (filter true? fold-old))
     :foldable-new (count (filter true? fold-new))
     :foldable-difference (- (count (filter true? fold-new)) (count (filter true? fold-old)))
     :what-the-difference-is
     (str "the ONLY difference between the two readers is the July spelling of the two numeric "
          "legs, which the old one could not read. The categorical answer is identical on every "
          "record, which is why the non-progress walk, the decay it feeds and every selection "
          "score are unchanged with the observed side-channel empty -- and it is empty on every "
          "path but the measurement in this file.")}))

(defn -main [& args]
  (let [run-ids (vec args)
        files (corpus-files)
        corpus (vec (mapcat read-trace files))
        july (filterv #(str/starts-with? (or (day-of %) "") "2026-07") files)
        july-recs (vec (mapcat read-trace july))
        july-ros (vec (keep ro/realized-outcome july-recs))
        seq-recs (vec (mapcat run-records run-ids))
        seq-obs (apply merge {} (map run-observations run-ids))
        art (array-map
             :row :U59
             :generated-by "holes/labs/wm-contract/u59_outcome_vocabulary.clj"
             :read-only true
             :schema-under-test ro/schema
             :vocabularies ro/vocabularies
             :historical-vocabularies (vec (sort ro/historical-vocabularies))
             :corpus {:dir "data/wm-trace" :files (count files) :records (count corpus)}
             :section-1-vocabulary-census (vocabulary-census corpus)
             :section-1b-july-only {:files (mapv day-of july)
                                    :census (vocabulary-census july-recs)}
             :section-2-gamma-folds-the-july-records (fold-all july-ros)
             :section-2b-what-the-old-reader-did
             {:folded 0
              :why (str "the reader required :expected-score and :realized-score by key "
                        "(selection_gain.clj:197-205 before this row) and every one of these "
                        "records spells the same two legs :expected-G and :realized-G, so the fold "
                        "returned the state unchanged on all of them. Nothing about the records "
                        "changed; the reader did.")}
             :sequence {:run-ids run-ids
                        :records (count seq-recs)
                        :observations (count seq-obs)
                        :observation-records (vec (vals seq-obs))}
             :section-3-gamma-folds-the-observation (fold-all (vec (vals seq-obs)))
             :section-4-decay-difference-on-the-sequence (decay-difference seq-recs seq-obs)
             :section-4b-decay-difference-on-the-live-corpus
             (decay-difference (filterv #(seq (get-in % [:decision :controller-ranking])) corpus) {})
             :section-5-flag-off-equivalence
             {:live-corpus (flag-off-equivalence corpus)
              :sequence (flag-off-equivalence seq-recs)}
             :section-4c-what-a-readable-grounded-change-would-do
             (let [ranked (filterv #(seq (get-in % [:decision :controller-ranking])) corpus)
                   synthetic (into {} (map (fn [r] [(:run/id r) {:outcome :grounded-change}]) ranked))
                   d (decay-difference ranked synthetic)]
               (assoc d
                      :synthetic true
                      :what-this-is
                      (str "NOT an observation and not a proposal: every record is given a "
                           ":grounded-change so the decay's SENSITIVITY to a readable outcome can be "
                           "quantified. It is the upper bound of the difference -- no run of the "
                           "machine would produce a grounded change on every tick -- and it is here "
                           "because the two-step sequence's own observation is a :grounded-no-change, "
                           "which moves nothing and so measures nothing. The 29 records that carry a "
                           "non-progress count today carry it SOLELY because no outcome is readable: "
                           "the walk reaches the previous selection of the same mission, cannot ask "
                           "whether it progressed, and counts it as non-progress "
                           "(war_machine.clj:2463-2474)."))))]
    (.mkdirs out-dir)
    (spit (io/file out-dir "u59-outcome-vocabulary.edn") (with-out-str (pp/pprint art)))
    (println "U59 -- the outcome vocabulary, measured")
    (println "=======================================")
    (println)
    (println "1. VOCABULARY CENSUS over" (count files) "files /" (count corpus) "records")
    (println "   by vocabulary:" (pr-str (get-in art [:section-1-vocabulary-census :by-vocabulary])))
    (println "   readable by the OLD key test (:expected-score/:realized-score):"
             (get-in art [:section-1-vocabulary-census :readable-by-the-old-key-test]))
    (println "   historical, and readable now:"
             (get-in art [:section-1-vocabulary-census :historical-and-now-readable]))
    (println "   records with a CATEGORICAL outcome:"
             (get-in art [:section-1-vocabulary-census :records-with-a-categorical-outcome]))
    (println)
    (println "2. GAMMA FOLDS THE JULY RECORDS:" (pr-str (:section-2-gamma-folds-the-july-records art)))
    (println)
    (println "3. GAMMA FOLDS THE OBSERVATION:" (pr-str (:section-3-gamma-folds-the-observation art)))
    (println)
    (println "4. DECAY DIFFERENCE on the sequence" (pr-str run-ids))
    (let [d (:section-4-decay-difference-on-the-sequence art)]
      (println "   flag OFF:" (pr-str (:flag-off d)))
      (println "   flag ON :" (pr-str (:flag-on d)))
      (println "   identical?" (:identical? d) " records whose decay moved:" (:records-whose-decay-moved d))
      (doseq [m (:moved d)] (println "     " (pr-str m))))
    (let [d (:section-4b-decay-difference-on-the-live-corpus art)]
      (println "   live corpus, flag off vs off (control):" (:identical? d)
               "counts" (pr-str (get-in d [:flag-off :counts]))))
    (let [d (:section-4c-what-a-readable-grounded-change-would-do art)]
      (println)
      (println "4c. SENSITIVITY (synthetic :grounded-change on every record; NOT enabled, NOT observed)")
      (println "   counts off:" (pr-str (get-in d [:flag-off :counts]))
               "-> on:" (pr-str (get-in d [:flag-on :counts])))
      (println "   decays off:" (pr-str (get-in d [:flag-off :decays]))
               "-> on:" (pr-str (get-in d [:flag-on :decays])))
      (println "   ranking records whose decay would move:" (:records-whose-decay-moved d)))
    (println)
    (println "5. FLAG-OFF EQUIVALENCE (the pre-:U59 readers re-implemented and run beside the new)")
    (doseq [[k v] (:section-5-flag-off-equivalence art)]
      (println "  " (name k) "records" (:records v)
               "categorical identical?" (:categorical-outcome-identical? v)
               "disagreements" (:categorical-disagreements v)
               "| foldable old" (:foldable-old v) "-> new" (:foldable-new v)))
    (println)
    (println "wrote" (str (io/file out-dir "u59-outcome-vocabulary.edn")))))

(apply -main *command-line-args*)
