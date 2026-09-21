(ns futon2.report.c-process-discovery-test
  "Read-only census of the pinned Agency snapshot; not a warrant freshness check."
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]])
  (:import [java.util.zip GZIPInputStream]))

(def evidence "holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-4-evidence/")
(defn read-gzip [name]
  (with-open [in (GZIPInputStream. (io/input-stream (str evidence name)))]
    (slurp in)))
(defn semantic-keys [v]
  (into #{} (filter #(and (keyword? %) (re-find #"(?i)(mission|want|token)" (str %))))
        (tree-seq coll? seq v)))
(defn mission-mentions [v]
  (into #{} (mapcat #(re-seq #"(?<![A-Za-z0-9])M-[A-Za-z0-9][A-Za-z0-9_-]*" (str %)))
        (filter #(or (string? %) (keyword? %)) (tree-seq coll? seq v))))

(deftest census-has-a-complete-dated-snapshot
  (let [snapshot (json/read-str (read-gzip "registry-today.json.gz") :key-fn keyword)
        entries (:entries snapshot)
        payloads (mapv #(edn/read-string (get-in % [:evidence/body :payload-edn])) entries)
        warrants (filter #(and (= :run (:kind %)) (true? (:warrant? %))) payloads)
        bindings (mapv edn/read-string (str/split-lines (read-gzip "subjects.ednlog.gz")))
        live (vals (reduce (fn [m x] (if (= :subject-binding (:entry/type x))
                                      (assoc m (:subject-id x) x) m)) {} bindings))]
    (is (= (:count snapshot) (count entries) (count (set (map :evidence/id entries)))))
    (is (every? #(str/starts-with? (:evidence/at %) "2026-09-21") entries))
    (is (= #{:intent :run} (set (map :kind payloads))))
    (is (= (count payloads) (* 2 (count (filter #(= :run (:kind %)) payloads)))))
    (prn {:entries (count entries) :kinds (frequencies (map :kind payloads))
          :registered-warrants (count warrants)
          :semantic-key-warrants (count (filter #(seq (semantic-keys %)) warrants))
          :mission-mention-warrants (count (filter #(seq (mission-mentions %)) warrants))
          :binding-records (count bindings) :live-subjects (count live)
          :mission-subjects (vec (filter #(seq (mission-mentions (:subject-id %))) live))
          :want-subjects (vec (filter #(re-find #"(?i)(want|hole/|closed/|star/)" (:subject-id %)) live))})))

(deftest mentions-are-not-semantic-bindings
  (is (= #{} (semantic-keys {:code-files {"holes/M-example.md" "sha"}})))
  (is (= #{"M-example"} (mission-mentions {:code-files {"holes/M-example.md" "sha"}})))
  (is (= #{:mission :wanted-token} (semantic-keys {:mission "M-example" :wanted-token :closed/example}))))
