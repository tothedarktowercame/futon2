(ns f12-d2-denominator-check
  "`:F12` slice 16. Measure D2's denominator arms without choosing one."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [f12-d3-encoding-check :as d3]
            [f12-o4-reachability :as reach]
            [find-organise :as fo]
            [zaif-cascade-gate :as g]))

(def home (System/getProperty "user.home"))
(def futon3c-root (str home "/code/futon3c"))
(def out-path (str home "/code/futon2/holes/labs/wm-contract/runs/F12-organise/11-d2-denominator.edn"))
(def pair-ids [:math-strategy/missing-dependency-protocol
               :aif/status-gated-belief-update])

(defn denominator-specs []
  [[:primary reach/primary?]
   [:paired g/paired?]
   [:with-a-transcript :has-transcript?]])

(defn row-for
  [rules sits keep? before after]
  (let [plays-before (g/play rules before sits)
        plays-after (g/play rules after sits)
        kept (fn [plays]
               (keep-indexed (fn [i play]
                               (when (keep? (nth sits i)) play))
                             plays))
        agree (fn [plays] (:agree (#'g/agreement plays sits keep?)))]
    {:row (sorted-map
           :precedence-before (mapv before pair-ids)
           :precedence-after (mapv after pair-ids)
           :acting-order-before (reach/acting-order (kept plays-before))
           :acting-order-after (reach/acting-order (kept plays-after))
           :score-before (agree plays-before)
           :score-after (agree plays-after))
     :plays-before plays-before
     :plays-after plays-after}))

(defn arm
  [rules sits label keep?]
  (let [rounds (vec (filter keep? sits))]
    (if (empty? rounds)
      (sorted-map :denominator label :refused? true :reason :empty-denominator)
      (let [before {(first pair-ids) 1 (second pair-ids) 2}
            after {(first pair-ids) 2 (second pair-ids) 1}
            {:keys [row plays-before plays-after]} (row-for rules sits keep? before after)
            live? (fn [rule s] (reach/live? rule s))
            contenders (vec (for [s sits
                                  :when (and (keep? s)
                                             (every? #(live? % s) rules))]
                              (:round s)))
            fired-diff (vec (for [[pb pa s] (map vector plays-before plays-after sits)
                                  :when (and (keep? s) (not= (:fired pb) (:fired pa)))]
                              (:round s)))]
        (sorted-map
         :denominator label
         :rounds (count rounds)
         :round-ids (mapv :round rounds)
         :rule-ids pair-ids
         :contending-rounds contenders
         :contending-round-count (count contenders)
         :rounds-on-which-fired-rule-differs fired-diff
         :row row
         :acting-order-changed? (not= (:acting-order-before row) (:acting-order-after row))
         :score-changed? (not= (:score-before row) (:score-after row))
         :o4-holds? (fo/o4-precedence-governance row))))))

;; `contention-key` is passed rather than assumed: the worker's arms carry
;; `:contending-rounds` (both rules live) and the reachability report's arms
;; carry `:rounds-on-which-the-fired-rule-differs` (the outcome differs). They
;; are different measures, so the key that was read is recorded beside the
;; result and a missing key is reported instead of reading as an empty set.
(defn nested-floor [arms contention-key]
  (let [sets (into {} (map (juxt :denominator #(set (:round-ids %))) arms))
        p (:primary sets) q (:paired sets) t (:with-a-transcript sets)
        missing (vec (keep #(when-not (contains? % contention-key) (:denominator %)) arms))]
    (sorted-map
     :primary-subset-of-paired? (set/subset? p q)
     :paired-subset-of-with-a-transcript? (set/subset? q t)
     :paired-minus-primary (vec (sort (set/difference q p)))
     :transcript-minus-paired (vec (sort (set/difference t q)))
     :contention-measure contention-key
     :arms-missing-the-contention-key missing
     :contentions-in-paired-minus-primary
     (vec (sort (set/intersection (set/difference q p)
                                  (set (contention-key (second arms))))))
     :contentions-in-transcript-minus-paired
     (vec (sort (set/intersection (set/difference t q)
                                  (set (contention-key (nth arms 2)))))))))

(defn worker-report []
  (let [cohort (edn/read-string (slurp g/cohort-path))
        sits (g/situations cohort)
        rules (mapv (into {} (map (juxt :id identity) g/rule-table)) pair-ids)
        pair-found? (every? some? rules)
        arms (if pair-found?
               (mapv (fn [[label keep?]] (arm rules sits label keep?)) (denominator-specs))
               (mapv (fn [[label _]] (sorted-map :denominator label
                                                 :o4-exercised? false
                                                 :reason :rule-pair-not-found))
                     (denominator-specs)))
        full (when pair-found? (arm rules sits :full-play (constantly true)))
        identity-before {(first pair-ids) 1 (second pair-ids) 2}
        identity-row (when pair-found?
                       (:row (row-for rules sits reach/primary? identity-before identity-before)))
        empty-arm (when pair-found? (arm rules sits :forced-empty (constantly false)))
        report (g/report g/cohort-path)
        pass (try (g/require-pass! report) {:passed? true}
                  (catch clojure.lang.ExceptionInfo e
                    {:passed? false
                     :finding (:finding (ex-data e))
                     :failures (:failures (ex-data e))}))]
    (sorted-map
     :pair-found? pair-found?
     :arms arms
     :full-play full
     :nested-round-set-floor (when pair-found? (nested-floor arms :contending-rounds))
     :identity-control (when pair-found?
                         (sorted-map :row identity-row
                                     :o4-holds? (fo/o4-precedence-governance identity-row)))
     :empty-denominator-control empty-arm
     :gate-controls (select-keys (:controls report)
                                [:rules-in-the-cascade :rules-not-in-the-cascade
                                 :then-correspondence])
     :require-pass pass)))

(defn worker-run [transform expected]
  (let [dir (d3/temp-dir)
        plant (d3/write-gate! dir transform expected)
        cp (pr-str {:paths (into [(.getPath dir)] d3/classpath-tail)})
        p (shell/sh "clojure" "-Sdeps" cp "-M" "-m" "f12-d2-denominator-check" "--worker"
                    :dir futon3c-root)]
    (sorted-map :plant (select-keys plant [:verified?]) :process-exit (:exit p)
                :worker (when (zero? (:exit p)) (edn/read-string (:out p))))))

(defn baseline []
  (let [r (reach/report)
        arms (get-in r [:probe :arms])]
    (sorted-map
     :arms arms
     :nested-round-set-floor
     (nested-floor (mapv #(assoc % :round-ids
                                (case (:denominator %)
                                  :primary (get-in r [:contention :primary-rounds])
                                  :paired (mapv :round (filter g/paired?
                                                              (g/situations (edn/read-string (slurp reach/cohort-path)))))
                                  :with-a-transcript (mapv :round (filter :has-transcript?
                                                                         (g/situations (edn/read-string (slurp reach/cohort-path)))))))
                         arms)
                   :rounds-on-which-the-fired-rule-differs)
     :recorded-carriage (sorted-map
                         :records-checked (count reach/record-files)
                         :runs-carrying-both
                         (vec (for [[file v] (:carriage r)
                                    :when (map? (:runs v))
                                    [run row] (:runs v)
                                    :when (>= (:rule-carrying-count row) 2)]
                                {:record file :run run
                                 :members (:rule-carrying-members row)}))))))

(defn correspondence-summary []
  (let [rows (g/then-correspondence)
        conditions [:file-exists? :pattern-id-matches-path?
                    :span-inside-the-then-block? :span-is-non-empty?]
        failed (fn [row] (vec (remove #(true? (get row %)) conditions)))
        pass (try (g/require-pass! (g/report g/cohort-path)) {:passed? true}
                  (catch clojure.lang.ExceptionInfo e
                    {:passed? false :finding (:finding (ex-data e))
                     :failures (:failures (ex-data e))}))]
    (sorted-map :rules (mapv #(sorted-map :rule (:rule %)
                                          :then-source (:then-source %)
                                          :conditions (into (sorted-map) (map (juxt identity %) conditions))
                                          :failed-conditions (failed %)) rows)
                :failure-count (count (filter (comp seq failed) rows))
                :require-pass pass)))

(defn fitted-transform [text]
  (-> text d3/add-fifth
      (str/replace ":id :war-machine/ambient-pattern-retrieval" ":id :aif/no-self-certification")
      (str/replace ":id :agent/budget-bounds-exploration" ":id :aif/hierarchical-budget-aware-action-selection")
      (str/replace ":id :agent/pause-is-not-failure" ":id :budgeted-action-selection/mana-gated-work")))

(defn report []
  (let [base (baseline)
        treatment (worker-run d3/add-fifth
                              [":d3-plant :member-authored-after-the-law"
                               ":id :aif/status-gated-belief-update"])
        rename (worker-run #(-> % d3/add-fifth
                                (str/replace ":id :aif/status-gated-belief-update"
                                             ":id :problems/r3-belief-update"))
                           [":id :problems/r3-belief-update"])
        dead (worker-run #(-> % d3/add-fifth
                              (str/replace ":if (fn [s] (and (= :play (:grain s)) (:has-transcript? s)))"
                                           ":if (fn [_] false)"))
                         [":d3-plant :member-authored-after-the-law" ":if (fn [_] false)"])
        fitted (worker-run fitted-transform
                           [":id :aif/no-self-certification"
                            ":id :aif/hierarchical-budget-aware-action-selection"
                            ":id :budgeted-action-selection/mana-gated-work"
                            ":d3-plant :member-authored-after-the-law"])
        b (:worker treatment)
        controls (sorted-map
                  :rename-to-nonmember (select-keys rename [:plant :process-exit :worker])
                  :false-antecedent (select-keys dead [:plant :process-exit :worker])
                  :identity-precedence-exchange (:identity-control b)
                  :empty-denominator (:empty-denominator-control b)
                  :all-rule-ids-fit-cascade (select-keys fitted [:plant :process-exit :worker]))
        base-arms (:arms base)
        b-arms (:arms b)
        problems
        (vec (concat
              (when-not (= [[29 false] [49 true] [102 true]]
                           (mapv (juxt :rounds :o4-holds?) base-arms))
                [:head-baseline-moved])
              (when-not (empty? (get-in base [:recorded-carriage :runs-carrying-both]))
                [:recorded-carriage-blocker-moved])
              (when-not (and (= [29 49 102] (mapv :rounds b-arms))
                             (= [false true true] (mapv :o4-holds? b-arms)))
                [:arm-b-moved])
              (when-not (= (set pair-ids)
                           (set (get-in b [:gate-controls :rules-in-the-cascade])))
                [:arm-b-does-not-carry-the-rule-pair])
              (when-not (and (get-in b [:nested-round-set-floor :primary-subset-of-paired?])
                             (get-in b [:nested-round-set-floor :paired-subset-of-with-a-transcript?]))
                [:denominators-not-nested])
              (for [[k floor] [[:head-baseline (:nested-round-set-floor base)]
                               [:arm-b (get-in b [:nested-round-set-floor])]]
                    :when (seq (:arms-missing-the-contention-key floor))]
                [:floor-contention-key-absent k])
              (for [[k run] [[:treatment treatment] [:rename rename] [:dead dead] [:fitted fitted]]
                    :when (or (not (zero? (:process-exit run)))
                              (not (get-in run [:plant :verified?])))]
                [:worker-or-plant-failed k])
              (when-not (false? (get-in rename [:worker :pair-found?])) [:rename-control-did-not-move])
              (when-not (every? empty? (map :contending-rounds (get-in dead [:worker :arms])))
                [:dead-control-did-not-move])
              (when-not (true? (get-in b [:identity-control :o4-holds?]))
                [:identity-control-did-not-move])
              (when-not (true? (get-in b [:empty-denominator-control :refused?]))
                [:empty-denominator-was-not-refused])
              (when-not (some #(= :rule-table-is-fitted-to-the-cascade (:finding %))
                              (get-in fitted [:worker :require-pass :failures]))
                [:unfitting-guard-did-not-fire])))]
    (sorted-map
     :head-baseline base
     :arm-b treatment
     :head-gate-blocker (correspondence-summary)
     :controls controls
     :problems problems)))

(defn -main [& args]
  (if (= ["--worker"] (vec args))
    (do (pprint/pprint (worker-report)) (shutdown-agents))
    (let [r (report)]
      (io/make-parents out-path)
      (spit out-path (with-out-str (pprint/pprint r)))
      (println "F12 D2 denominator check:" (if (seq (:problems r)) "FAIL" "PASS")
               (pr-str {:problems (:problems r)}))
      (println " wrote" out-path)
      (shutdown-agents)
      (System/exit (if (seq (:problems r)) 1 0)))))
