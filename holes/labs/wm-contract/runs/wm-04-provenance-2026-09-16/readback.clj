(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[cheshire.core :as json])

(def output-dir "holes/labs/wm-contract/runs/wm-04-provenance-2026-09-16/")
(def target "M-shared-memory-control-build-test")
(def trace-path "data/wm-trace/wm-trace-2026-09-12.edn")
(def brief-path
  "data/wm-morning-brief/items/ea1-418bb56e1f0d7ad8986839e45f5268a98e62dcbf9c8ad3634bd998f421a293ae--attempt-001.edn")
(def occurrence-keys
  [:run/id :cohort/id :attempt/id :checkpoint/ref
   :action/started-at :action/completed-at :evidence/cutoff-at
   :disposition/recorded-at])

(defn read-all [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [xs []]
      (let [x (edn/read {:eof ::eof :default tagged-literal} r)]
        (if (= ::eof x) xs (recur (conj xs x)))))))

(defn fields [x ks]
  (into {} (map (fn [k] [k {:present? (contains? x k) :value (get x k)}])) ks))

(let [records (read-all trace-path)
      selected (nth records 3)
      summary {:source trace-path :record-count (count records) :record-index 3
               :timestamp (:timestamp selected)
               :record-keys (vec (sort (keys selected)))
               :producer-contract (:producer-contract selected)
               :observation-envelope-keys (vec (keys (:observation-envelope selected)))
               :decision (select-keys (:decision selected)
                                      [:action :selected-policy-id :actuation-authorized?
                                       :actuation-status :reason])
               :top-level-observation-context (fields selected occurrence-keys)
               :point-observation-context (fields (:point selected) occurrence-keys)
               :target-belief {:pre (get-in selected [:mu-pre target])
                               :post (get-in selected [:mu-post target])}}]
  (assert (= 4 (count records)))
  (assert (= "2026-09-12T17:28:09.498448087Z" (:timestamp selected)))
  (assert (= target (get-in selected [:decision :action :target])))
  (spit (str output-dir "trace-readback.edn") (str (pr-str summary) "\n")))

(let [search (json/parse-string (slurp (str output-dir "correlation-search.json")) true)
      files (get-in search [:policy :files])
      summaries
      (mapv (fn [path]
              {:path path
               :records (mapv (fn [x]
                                {:keys (vec (sort (keys x)))
                                 :context (select-keys x
                                                       [:schema :recorded-at :checkpoint/type
                                                        :event/sequence :run/id :cohort/id :attempt/id
                                                        :checkpoint/ref :timestamp :at :phase
                                                        :run-id :cohort-id :attempt-id
                                                        :checkpoint :created-at])})
                              (read-all path))})
            files)]
  (spit (str output-dir "policy-occurrences.edn") (str (pr-str summaries) "\n"))
  (println "Read four trace records and" (count summaries) "policy-matching files."))

(let [trace (nth (read-all trace-path) 3)
      backup (nth (read-all (str trace-path ".pre-migration-backup")) 3)
      item (first (read-all brief-path))
      reasons (get-in item [:selection-review :selection-reasons])
      ordinary (:ordinary-selector-decision reasons)
      decision (:decision trace)
      decision-equal? (= (dissoc ordinary :softmax-weights)
                         (dissoc decision :softmax-weights-by-candidate-id))
      weights-equal? (= (sort (vals (:softmax-weights ordinary)))
                        (sort (vals (:softmax-weights-by-candidate-id decision))))
      summary {:brief-source brief-path
               :opportunity-id (:opportunity-id item)
               :ordinary-target (get-in ordinary [:action :target])
               :enacted-target (get-in reasons [:enacted-candidate-action :target])
               :brief-selected-target (:selected-target item)
               :selection-source (:source reasons)
               :operator (:operator reasons)
               :decision-equal-except-weight-encoding? decision-equal?
               :weight-multiset-equal? weights-equal?
               :weight-counts [(count (:softmax-weights ordinary))
                               (count (:softmax-weights-by-candidate-id decision))]
               :backup-decision-identical? (= (:decision backup) decision)
               :belief-key-counts [(count (:mu-pre trace)) (count (:mu-post trace))]
               :belief-key-types (frequencies (map #(if (keyword? %) :keyword :string)
                                                  (keys (:mu-pre trace))))}]
  (assert decision-equal?)
  (assert weights-equal?)
  (assert (= (:decision backup) decision))
  (assert (= target (:ordinary-target summary)))
  (assert (= "M-f11-find-production-successor"
             (:enacted-target summary) (:brief-selected-target summary)))
  (spit (str output-dir "execution-join.edn") (str (pr-str summary) "\n"))
  (println "Verified displaced selector head, enacted F11 target, and backup decision."))
