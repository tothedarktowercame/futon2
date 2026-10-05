(ns meta-slot-census
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as sh]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.meta-adapter-discovery :as discovery]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-policy-constructor :as constructor]))

(def run-path
  "data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn")
(def report-path
  "holes/labs/wm-contract/REPORT-meta-slot-census-2026-10-05.md")
(def slots [:task-kind :target :next-move :resource-envelope
            :evidence-channel :stopping-rule])
(def resource-envelope
  {:time-budget-ms 5000 :token-budget 800
   :author-seat "census-author" :reviewer-seat "census-reviewer"})

(defn git-sha [dir]
  (let [{:keys [exit out err]} (sh/sh "git" "-C" dir "rev-parse" "HEAD")]
    (if (zero? exit) (str/trim out)
        (throw (ex-info "git rev-parse refused" {:dir dir :stderr err})))))

(defn refusal! [stage receipt]
  (when (= :refused (:status receipt))
    (throw (ex-info (str (name stage) " refused")
                    {:stage stage :receipt receipt}))))

(defn locator? [x]
  (and (map? x)
       (or (contains? constructor/checkable-locator-classes (:class x))
           (and (keyword? (:kind x))
                (or (pos-int? (:line x)) (some? (:id x)))))))

(defn permitted-move? [kind move]
  (contains? (if (= :algorithm kind)
               #{:advance :unblock :close :repair :run-algorithm}
               #{:advance :unblock :close})
             move))

(defn slot-analysis [ranked row adapter discovery-exclusion construction]
  (if-not row
    {:id (:id ranked) :kind (:kind ranked) :fillable []
     :absences (zipmap slots (repeat :not-in-field-observation))
     :first-refusal :not-in-field-observation}
    (let [template (some #(when (= (:id ranked) (get-in % [:slots :target])) %) 
                         (:templates construction))
          constructor-exclusion (some #(when (= (:id ranked) (:id %)) %) 
                                      (:exclusions construction))
          move (or (:next-move adapter)
                   (when (and (constructor/ordinary-kinds (:kind row))
                              (= :ready (:next-step adapter)))
                     :advance)
                   (when (= :algorithm (:kind row)) :run-algorithm))
          stop (or (:stopping-rule adapter) :grounded-progress)
          values {:task-kind (:kind row)
                  :target (:id row)
                  :next-move (when (permitted-move? (:kind row) move) move)
                  :resource-envelope resource-envelope
                  :evidence-channel (when (locator? (:locator adapter))
                                      {:source (:source row) :locator (:locator adapter)})
                  :stopping-rule (when (constructor/stopping-rules stop) stop)}
          fillable (if template slots (filterv #(some? (get values %)) slots))
          absent (remove (set fillable) slots)
          base-reason (or (:reason discovery-exclusion)
                          (:reason constructor-exclusion)
                          :slot-value-unavailable)]
      {:id (:id ranked) :kind (:kind ranked) :fillable fillable
       :absences (into {} (map (fn [slot] [slot base-reason])) absent)
       :first-refusal (when (seq absent) base-reason)})))

(defn kw [x] (if x (str "`" x "`") "—"))
(defn ids-cell [xs] (if (seq xs) (str/join ", " (map #(str "`" % "`") xs)) "none"))

(defn render [{:keys [futon2-sha futon3-sha ranked field-observation discovery
                      construction analyses ranked-only field-only]}]
  (let [n (count ranked)
        slot-rows
        (for [slot slots
              :let [absent-reasons (frequencies (keep #(get-in % [:absences slot]) analyses))
                    absent (reduce + 0 (vals absent-reasons))]]
          (str "| `" slot "` | " (- n absent) " | " absent " | "
               (if (seq absent-reasons)
                 (str/join ", " (map (fn [[reason count]] (str "`" reason "` (" count ")"))
                                      (sort-by (comp str key) absent-reasons)))
                 "—") " |"))
        ordered (sort-by (juxt (comp - count :fillable) :id) analyses)
        all-six (count (filter #(= 6 (count (:fillable %))) analyses))
        zero (count (filter #(zero? (count (:fillable %))) analyses))
        absences (frequencies (mapcat (comp vals :absences) analyses))
        dominant (first (sort-by (juxt (comp - val) (comp str key)) absences))
        slot-absence-counts (into {} (map (fn [slot]
                                            [slot (count (filter #(contains? (:absences %) slot)
                                                                 analyses))])
                                          slots))
        max-slot-absence (apply max (vals slot-absence-counts))
        most-absent-slots (->> slot-absence-counts
                               (keep (fn [[slot count]]
                                       (when (= count max-slot-absence) slot)))
                               (sort-by str))]
    (str "# META slot census at HEAD — 2026-10-05\n\n"
         "The census used the persisted ranking, not a fresh selection. Reproduce from `/home/joe/code/futon2`:\n\n"
         "```sh\nclojure -M scripts/meta_slot_census.clj\n```\n\n"
         "| repository | HEAD |\n|---|---|\n"
         "| futon2 | `" futon2-sha "` |\n| futon3 | `" futon3-sha "` |\n\n"
         "The resource envelope was `" (pr-str resource-envelope) "`. The field observation, adapter discovery, and construction all completed with statuses `"
         (:status field-observation) "`, `" (:status discovery) "`, and `" (:status construction) "`.\n\n"
         "## Coverage and totals\n\n"
         "The run ranking contains **" n "** items. Of those, **" (- n (count ranked-only))
         "** are rows in the HEAD field observation; **" (count ranked-only) "** are absent from it. The field contains **"
         (count (:rows field-observation)) "** rows total, of which **" (count field-only)
         "** are not in the persisted ranking.\n\n"
         "- Ranking minus field: " (ids-cell ranked-only) "\n"
         "- Field minus ranking: " (ids-cell field-only) "\n\n"
         "Items with all six slots fillable: **" all-six "**. Items with zero slots fillable: **" zero
         "**. The most frequently absent slots are "
         (str/join " and " (map #(str "`" % "`") most-absent-slots))
         " (**" max-slot-absence "** items each). The dominant absence-reason keyword across slot cells is "
         (if dominant (str "`" (key dominant) "` (**" (val dominant) "** slot absences)") "none") ".\n\n"
         "## Slot counts\n\n| slot | fillable | absent | absence reason keywords |\n|---|---:|---:|---|\n"
         (str/join "\n" slot-rows) "\n\n"
         "`construct` admits or excludes a whole row. For excluded rows this report counts independently available slot values using the same predicates and defaults as the constructor; the first refusal remains discovery's typed reason (or construction's typed reason if discovery supplied none).\n\n"
         "## Per ranked item\n\n| id | kind | fillable slots | first refusal reason |\n|---|---|---|---|\n"
         (str/join "\n"
                   (for [{:keys [id kind fillable first-refusal]} ordered]
                     (str "| `" id "` | `" kind "` | "
                          (if (seq fillable) (str/join ", " (map kw fillable)) "—")
                          " | " (kw first-refusal) " |")))
         "\n\n## What the numbers say\n\n"
         "The tables above state the observed coverage and typed absences only. They do not infer policy quality or recommend a change to the outer cascade.\n")))

(defn -main [& _]
  (let [run (edn/read-string (slurp run-path))
        ranked (get-in run [:outer-task-selection :policy :meta-selection :ranking])
        _ (when-not (= 107 (count ranked))
            (throw (ex-info "persisted ranking does not contain 107 items"
                            {:count (count ranked)})))
        fo (field/observe {})
        _ (refusal! :observe fo)
        pin (:source-pin fo)
        discovered (discovery/discover {:field-observation fo :expected-field-pin pin})
        _ (refusal! :discover discovered)
        constructed (constructor/construct {:field-observation fo
                                            :expected-field-pin pin
                                            :adapters (:adapters discovered)
                                            :resource-envelope resource-envelope})
        _ (refusal! :construct constructed)
        rows (into {} (map (juxt :id identity)) (:rows fo))
        adapters (into {} (map (juxt :id identity)) (:adapters discovered))
        exclusions (into {} (map (juxt :id identity)) (:exclusions discovered))
        ranked-ids (set (map :id ranked))
        field-ids (set (keys rows))
        result {:futon2-sha (git-sha ".") :futon3-sha (git-sha "../futon3")
                :ranked ranked :field-observation fo :discovery discovered
                :construction constructed
                :analyses (mapv #(slot-analysis % (rows (:id %)) (adapters (:id %))
                                                 (exclusions (:id %)) constructed)
                                ranked)
                :ranked-only (sort (set/difference ranked-ids field-ids))
                :field-only (sort (set/difference field-ids ranked-ids))}]
    (spit report-path (render result))
    (println report-path)))

(apply -main *command-line-args*)
