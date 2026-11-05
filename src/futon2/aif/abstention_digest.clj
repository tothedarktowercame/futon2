(ns futon2.aif.abstention-digest
  "Read-only owner feedback derived from the outer-refusal index."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- run-day [run-id]
  (when (and (string? run-id) (<= 10 (count run-id)))
    (subs run-id 0 10)))

(defn- missing-values [missing]
  (cond
    (nil? missing) []
    (sequential? missing) missing
    (set? missing) missing
    :else [missing]))

(defn by-target
  "Group indexed abstentions by target, newest occurrence retained.

  Rows that are neither abstentions nor carry an :abstention are ignored.
  A missing target is explicit :no-target; :not-retained remains a counted
  reason rather than disappearing.  An abstained/no-selection row indexed
  before ABSTAIN-REASON-1 has no carrier; it counts as :not-indexed (its
  record may hold a reason, but reading 60 records, ~230 MB, per page render
  is not done here), so legacy abstentions are visible, not dropped."
  [index-rows]
  (->> index-rows
       (keep (fn [row]
               (when-let [abstention (or (:abstention row)
                                         (when (contains? #{:abstained :no-selection}
                                                          (get-in row [:failure :kind]))
                                           {:reason :not-indexed}))]
                 {:target (or (:target abstention) (:target row) :no-target)
                  :reason (or (:reason abstention) :not-retained)
                  :missing (missing-values (:missing abstention))
                  :run-id (:run-id row)
                  :at (run-day (:run-id row))})))
       (group-by :target)
       (map (fn [[target rows]]
              (let [ordered (sort-by (juxt :at :run-id) rows)
                    latest (last ordered)]
                {:target target
                 :count (count rows)
                 :first-at (:at (first ordered))
                 :last-at (:at latest)
                 :reasons (frequencies (map :reason rows))
                 :missing (into #{} (mapcat :missing) rows)
                 :latest-run-id (:run-id latest)})))
       (sort-by (juxt (comp - :count) (comp str :target)))
       vec))

(defn read-index
  "Read an EDN-lines outer-refusal index without modifying it."
  [path]
  (with-open [reader (io/reader path)]
    (->> (line-seq reader) (remove str/blank?) (mapv edn/read-string))))

(defn read-by-target [path]
  (by-target (read-index path)))

(defn- shown [x]
  (if (keyword? x) (name x) (str x)))

(defn- counts-text [counts]
  (->> counts
       (sort-by (comp str key))
       (map (fn [[reason n]] (str (shown reason) "=" n)))
       (str/join ", ")))

(defn render-text
  "Render digest entries as a compact plain-text/Markdown table."
  [entries]
  (str "| target | count | first | last | reasons | missing | latest run |\n"
       "|---|---:|---|---|---|---|---|\n"
       (->> entries
            (map (fn [{:keys [target count first-at last-at reasons missing
                              latest-run-id]}]
                   (str "| " (shown target) " | " count " | " first-at " | "
                        last-at " | " (counts-text reasons) " | "
                        (str/join ", " (sort (map shown missing))) " | "
                        latest-run-id " |")))
            (str/join "\n"))
       (when (seq entries) "\n")))
