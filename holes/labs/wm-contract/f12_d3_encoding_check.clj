(ns f12-d3-encoding-check
  "`:F12` slice 15. Measures D3's three arms without choosing one.

   Run from futon3c with the same classpath as f12-o4-reachability. Temporary
   copies are used for every record and rule-table plant. Exit 0 means every
   premise and control still holds; exit 1 means the measurement is stale."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [f12-o4-reachability :as reach]
            [find-organise :as fo]
            [zaif-cascade-gate :as g]))

(def home (System/getProperty "user.home"))
(def futon3c-root (str home "/code/futon3c"))
(def checks-root (str home "/code/futon3/checks"))
(def out-path (str home "/code/futon2/holes/labs/wm-contract/runs/F12-organise/10-d3-encoding.edn"))
(def gate-path (str futon3c-root "/scripts/zaif_cascade_gate.clj"))
(def committed-reachability
  (str home "/code/futon2/holes/labs/wm-contract/runs/F12-organise/02-o4-reachability.edn"))

(def fifth-rule
  "\n   {:id :aif/status-gated-belief-update\n    :d3-plant :member-authored-after-the-law\n    :then-source \"aif/status-gated-belief-update.flexiarg:26-30\"\n    :encodes \"Apply update-mu using the post-step state when observation remains available.\"\n    :if (fn [s] (and (= :play (:grain s)) (:has-transcript? s)))\n    :however (fn [_] true)\n    :encoding-limit \"The zaif arm set has no belief-update term; :act records only that the authored THEN calls for an update.\"\n    :then (fn [_] :act)}")

(def close-rule-table ":then (fn [_] :ask)}])")

(defn add-fifth [text]
  (str/replace text close-rule-table
               (str ":then (fn [_] :ask)}" fifth-rule "])")))

(defn temp-dir [] (.toFile (java.nio.file.Files/createTempDirectory "f12-d3-" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn write-gate! [dir transform expected]
  (let [target (io/file dir "zaif_cascade_gate.clj")
        planted (transform (slurp gate-path))]
    (spit target planted)
    {:path (.getPath target)
     :verified? (every? #(str/includes? (slurp target) %) expected)}))

(def classpath-tail
  ["src" "resources" "library" "scripts" "." "../futon3/checks" "../futon3"
   "../futon2/holes/labs/wm-contract"])

(defn worker-run [transform expected]
  (let [dir (temp-dir)
        planted (write-gate! dir transform expected)
        cp (pr-str {:paths (into [(.getPath dir)] classpath-tail)})
        p (shell/sh "clojure" "-Sdeps" cp "-M" "-m" "f12-d3-encoding-check" "--worker"
                    :dir futon3c-root)
        value (when (zero? (:exit p)) (edn/read-string (:out p)))]
    {:plant planted :process-exit (:exit p) :worker value :stderr (str/trim (:err p))}))

(defn primary? [s] (and (g/paired? s) (not (g/oracle-uncertain? s))))

(defn worker-report []
  (let [r (g/report g/cohort-path)
        cohort (edn/read-string (slurp g/cohort-path))
        sits (g/situations cohort)
        planted (first (filter :d3-plant g/rule-table))
        live (vec (map :round (filter #(and (fo/fires? planted %)
                                            (some? ((:then planted) %))) sits)))
        primary-live (vec (map :round (filter #(and (primary? %)
                                                    (fo/fires? planted %)
                                                    (some? ((:then planted) %))) sits)))
        coverage (g/coverage g/cohort-path)
        o4 (:o4 r)
        row (select-keys o4 [:precedence-before :precedence-after :acting-order-before
                             :acting-order-after :score-before :score-after])]
    (sorted-map
     :fifth-rule-id (:id planted)
     :fifth-rule-found? (some? planted)
     :live-rounds live
     :live-primary-rounds primary-live
     :contentions (get-in coverage [:contention :rounds-where-more-than-one-cascade-rule-fires])
     :contentions-in-primary
     (vec (filter #(contains? (set (map :round (filter primary? sits))) (:round %))
                  (get-in coverage [:contention :rounds-where-more-than-one-cascade-rule-fires])))
     :primary-rounds (count (filter primary? sits))
     :rules-not-in-the-cascade (get-in r [:controls :rules-not-in-the-cascade])
     :then-correspondence-failure-count
     (count (remove #(and (:file-exists? %) (:pattern-id-matches-path? %)
                          (:span-inside-the-then-block? %) (:span-is-non-empty? %))
                    (get-in r [:controls :then-correspondence])))
     :o4 o4
     :o4-holds? (when (:exercised? o4) (fo/o4-precedence-governance row)))))

(defn copy-checks-and-plant-a! []
  (let [dir (temp-dir)]
    (doseq [f reach/record-files]
      (io/copy (io/file checks-root f) (io/file dir f)))
    (let [p (io/file dir "zaif-cascade.edn")
          before (edn/read-string (slurp p))
          arm :widen-to-the-marginal-gain-floor
          member :war-machine/ambient-pattern-retrieval
          after (-> before
                    (update-in [:runs arm :cascade :members] #(vec (sort (conj (set %) member))))
                    (update-in [:runs arm :members] inc))]
      (spit p (with-out-str (pprint/pprint after)))
      {:dir (.getPath dir)
       :verified? (and (some #{member} (get-in after [:runs arm :cascade :members]))
                       (= (get-in after [:runs arm :members])
                          (count (get-in after [:runs arm :cascade :members]))))})))

(defn arm-a []
  (let [{:keys [dir verified?]} (copy-checks-and-plant-a!)
        planted (with-redefs [reach/checks-dir dir] (reach/report))
        primary (first (filter #(= :primary (:denominator %)) (get-in planted [:probe :arms])))]
    (sorted-map :plant-verified? verified?
                :max-rule-carrying-members
                (:max-rule-carrying-members-on-any-recorded-run planted)
                :primary-rounds (:rounds primary)
                :primary-o4-holds? (:o4-holds? primary)
                :primary-acting-order-changed? (:acting-order-changed? primary)
                :primary-score-before (get-in primary [:row :score-before])
                :primary-score-after (get-in primary [:row :score-after])
                :stayed-the-same {:cohort-rounds (get-in planted [:contention :rounds])
                                  :primary-rounds (:rounds primary)
                                  :contending-primary-rounds
                                  (get-in planted [:contention :contending-rounds-in-primary])})))

(defn o4-records []
  (letfn [(walk [x]
            (cond (map? x) (concat (when (and (contains? x :o4) (map? (:o4 x))) [(:o4 x)])
                                   (mapcat walk (vals x)))
                  (sequential? x) (mapcat walk x)
                  :else []))]
    (vec (for [f reach/record-files
               o4 (walk (edn/read-string (slurp (io/file checks-root f))))
               :when (:exercised? o4)]
           {:record f :o4 o4}))))

(defn baseline-premises []
  (let [reach-result (reach/report)
        rendered (with-out-str (pprint/pprint reach-result))
        gate (:gate-status reach-result)]
    (sorted-map
     :slice-3-byte-identical? (= rendered (slurp committed-reachability))
     :max-rule-carrying-members (:max-rule-carrying-members-on-any-recorded-run reach-result)
     :then-correspondence-failures (:then-correspondence-failure-count gate)
     :require-pass-verdict (:require-pass-verdict gate)
     :unfitting-pointer-check
     {:stated-lines [53 57] :computed-lines [479 480] :enforced-line 585})))

(defn treatment []
  (worker-run add-fifth [":d3-plant :member-authored-after-the-law"
                         ":id :aif/status-gated-belief-update"]))

(defn controls []
  (let [nonmember (worker-run #(-> % add-fifth
                                   (str/replace ":id :aif/status-gated-belief-update"
                                                ":id :problems/r3-belief-update"))
                              [":d3-plant :member-authored-after-the-law"
                               ":id :problems/r3-belief-update"])
        dead (worker-run #(-> % add-fifth
                              (str/replace ":if (fn [s] (and (= :play (:grain s)) (:has-transcript? s)))"
                                           ":if (fn [_] false)"))
                         [":d3-plant :member-authored-after-the-law" ":if (fn [_] false)"])
        contend (worker-run #(-> % add-fifth
                                 (str/replace ":however (fn [s] (or (:md-once-more? s) (:md-record-gap? s) (:md-stop-researching? s)))"
                                              ":however (fn [_] true)")
                                 (str/replace ":else nil))}" ":else :retrieve))}"))
                            [":d3-plant :member-authored-after-the-law" ":however (fn [_] true)"
                             ":else :retrieve))}"])
        fitted (worker-run #(-> % add-fifth
                                (str/replace ":id :war-machine/ambient-pattern-retrieval"
                                             ":id :aif/no-self-certification")
                                (str/replace ":id :agent/budget-bounds-exploration"
                                             ":id :aif/hierarchical-budget-aware-action-selection")
                                (str/replace ":id :agent/pause-is-not-failure"
                                             ":id :budgeted-action-selection/mana-gated-work"))
                           [":id :aif/no-self-certification"
                            ":id :aif/hierarchical-budget-aware-action-selection"
                            ":id :budgeted-action-selection/mana-gated-work"
                            ":d3-plant :member-authored-after-the-law"])]
    [(sorted-map :control :planted-rule-is-not-a-cascade-member
                 :plant-verified? (get-in nonmember [:plant :verified?])
                 :o4-exercised? (get-in nonmember [:worker :o4 :exercised?])
                 :verdict-moved-to :o4-not-exercised)
     (sorted-map :control :planted-rule-dead :plant-verified? (get-in dead [:plant :verified?])
                 :observed-live-rounds (get-in dead [:worker :live-rounds])
                 :verdict-moved-to :planted-rule-is-live-on-no-round)
     (sorted-map :control :existing-rule-made-primary-live :plant-verified? (get-in contend [:plant :verified?])
                 :o4-holds? (get-in contend [:worker :o4-holds?])
                 :verdict-moved-to :primary-o4-true)
     (sorted-map :control :all-rules-fitted-to-members :plant-verified? (get-in fitted [:plant :verified?])
                 :rules-not-in-the-cascade (get-in fitted [:worker :rules-not-in-the-cascade])
                 :verdict-moved-to :unfitting-guard-fires)]))

(defn report []
  (let [premises (baseline-premises)
        a (arm-a)
        b-run (treatment)
        b (:worker b-run)
        c (o4-records)
        cs (controls)
        problems (vec (concat
                       (when-not (:slice-3-byte-identical? premises) [:slice-3-artifact-drift])
                       (when-not (= 1 (:max-rule-carrying-members premises)) [:carriage-premise-moved])
                       (when-not (= 3 (:then-correspondence-failures premises)) [:gate-failure-count-moved])
                       (when-not (= :rule-does-not-encode-an-authored-then (:require-pass-verdict premises)) [:gate-verdict-moved])
                       (when-not (and (:plant-verified? a) (= 2 (:max-rule-carrying-members a))
                                      (false? (:primary-o4-holds? a))) [:arm-a-moved])
                       (when-not (and (zero? (:process-exit b-run)) (get-in b-run [:plant :verified?])
                                      (seq (:rules-not-in-the-cascade b)) (= 29 (:primary-rounds b))
                                      (seq (:live-primary-rounds b)) (:exercised? (:o4 b))
                                      (false? (:o4-holds? b))) [:arm-b-moved])
                       (when (seq c) [:arm-c-moved])
                       (for [x cs :when (not (:plant-verified? x))]
                         [:control-plant-missing (:control x)])
                       (when-not (and (false? (:o4-exercised? (nth cs 0)))
                                      (empty? (:observed-live-rounds (nth cs 1)))
                                      (true? (:o4-holds? (nth cs 2)))
                                      (empty? (:rules-not-in-the-cascade (nth cs 3))))
                         [:negative-control-did-not-move])))]
    (sorted-map
     :premises premises
     :arm-a-new-construction a
     :arm-b-new-play-grain-encoding
     (assoc b :plant-verified? (get-in b-run [:plant :verified?])
              :primary-o4-moved-from :not-exercised
              :primary-o4-moved-to :exercised-false
              :stayed-the-same {:primary-rounds (:primary-rounds b)
                                :then-correspondence-failure-count
                                (:then-correspondence-failure-count b)
                                :rules-not-in-the-cascade-remains-nonempty?
                                (boolean (seq (:rules-not-in-the-cascade b)))})
     :arm-c-refuse-both {:record-files (count reach/record-files)
                         :recorded-runs-exercising-o4 (count c)
                         :rows c}
     :negative-controls cs
     :problems problems)))

(defn -main [& args]
  (if (= ["--worker"] (vec args))
    (do (pprint/pprint (worker-report)) (shutdown-agents))
    (let [r (report)]
      (io/make-parents out-path)
      (spit out-path (with-out-str (pprint/pprint r)))
      (println "F12 D3 encoding check:" (if (seq (:problems r)) "FAIL" "PASS"))
      (println " wrote" out-path)
      (shutdown-agents)
      (System/exit (if (seq (:problems r)) 1 0)))))
