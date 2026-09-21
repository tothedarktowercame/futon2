#!/usr/bin/env bb
(ns inventory
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.string :as str] [clojure.pprint :as pp]))
;; Read-only inventory. No production namespace loads, network calls, or EDN eval.
(def roots ["/home/joe/code/futon2/data" "/home/joe/code/futon3c/data"])
(def selected-dirs
  #"wm-(runs|full-loop.*|d-task-enactment|learning-trials|repair-obligations)")
(def files
  (sort-by str
           (for [root roots dir (.listFiles (io/file root))
                 :when (and (.isDirectory dir) (re-matches selected-dirs (.getName dir)))
                 f (file-seq dir)
                 :when (and (.isFile f) (str/ends-with? (str f) ".edn"))] f)))
(def out (atom []))
(def errors (atom []))
(def forms (atom 0))
(defn date-of [m]
  (first (sort (keep (fn [k] (let [v (get m k)]
                             (when (and (string? v) (re-find #"^2026-\d\d-\d\d" v))
                               (subs v 0 10))))
                    [:opened-at :resolved-at :recorded-at :created-at :timestamp :startedAt :at :completed-at :dismissed-at :started-at :finished-at]))))
(defn emit [category file form path m context]
  (swap! out conj
         {:category category :file file :form form :path path
          :date (or (date-of m) (:date context))
          :identity (merge (select-keys context [:run/id :attempt-id :action-id :runner-execution/identity :cohort/id :attempt/id :action/id :transition/id])
                           (select-keys m [:repair/id :occurrence-id :occurrence :deduplication-id :token :target :candidate :artifact-sha]))
          :fields (select-keys m [:schema :status :reason :kind :stage :verdict :repair/class :failure-kind
                                 :failure-stage :review-verdict :failed-commit :replacement-commit
                                 :resolved-at :opened-at :repository :revision-pair :review-job :reviewer :repair/status :dismissal/reason :dismissal/kind])}))
(defn walk [file form path x context]
  (cond
    (map? x)
    (let [context (merge context (select-keys x [:run/id :attempt-id :action-id :runner-execution/identity :cohort/id :attempt/id :action/id :transition/id])
                         (when-let [d (date-of x)] {:date d}))
          schema (:schema x)]
      (when (#{:predicted-not-observed :not-predicted-observed} (:verdict x))
        (emit :token-surprise file form path x context))
      (when (#{:wm/d-task-token-observations-v2 :wm/learning-trial-receipt-v1 :wm/learning-trial-receipt-v2} schema)
        (emit schema file form path x context))
      (when (and (= :refusal (last path)) (:kind x))
        (emit :typed-refusal file form path x context))
      (when (and (integer? (last path)) (#{:dropped-candidates :declines :refusals} (last (butlast path))))
        (emit :candidate-drop file form path x context))
      (when (or (#{:refused :declined :held :admitted-at-attempt-grain} (:status x))
                (#{:candidate-admission :target-admission} (:stage x)))
        (emit :typed-status file form path x context))
      (doseq [[k v] x] (when (coll? v) (walk file form (conj path k) v context))))
    (sequential? x) (doseq [[i v] (map-indexed vector x)] (when (coll? v) (walk file form (conj path i) v context)))))
(doseq [f files]
  (try
    (with-open [r (java.io.PushbackReader. (io/reader f))]
      (loop [n 0]
        (let [x (edn/read {:eof ::eof :default (fn [tag value] {:unread-tag tag :value value})} r)]
          (when-not (= ::eof x)
            (swap! forms inc)
            (let [file (str f)]
              (if (str/includes? file "/wm-repair-obligations/")
                (emit (keyword (str "repair-" (first (str/split (second (str/split file #"/wm-repair-obligations/")) #"/")))) file n [] x {})
                (do
                  (when (= :wm/d-task-enactment-v1 (:schema x))
                    (emit :d-task-artifact file n [] x (merge (select-keys (get-in x [:dispatch :occurrence]) [:run/id :action/id :transition/id :cohort/id :attempt/id]) {:date (date-of (:review-job x))}))
                    (doseq [row (:after-token-evidence x)]
                      (emit :raw-signed-observation file n [:after-token-evidence] (assoc row :verdict (get-in row [:result :observed]))
                            (merge (select-keys (get-in x [:dispatch :occurrence]) [:run/id :action/id :transition/id :cohort/id :attempt/id]) {:date (date-of (:review-job x))}))))
                  (walk file n [] x {}))))
            (recur (inc n))))))
    (catch Exception e (swap! errors conj {:file (str f) :error (.getMessage e)}))))
(defn summary [xs]
  (let [dates (sort (keep :date xs))]
    {:appearances (count xs) :files (count (set (map :file xs)))
     :first-date (first dates) :last-date (last dates) :undated (count (remove :date xs))}))
(defn fingerprint [f]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (java.security.DigestInputStream. (io/input-stream f) md)]
      (let [buffer (byte-array 65536)]
        (loop [] (when (pos? (.read in buffer)) (recur)))))
    {:file (str f) :bytes (.length f)
     :sha256 (format "%064x" (java.math.BigInteger. 1 (.digest md)))}))
(assert (= (count files) (count (set (map str files)))))
(assert (empty? @errors) (pr-str @errors))
(pp/pprint {:sources (mapv fingerprint files) :scope {:roots roots :directory-regex (str selected-dirs) :extension ".edn"
                   :files (count files) :forms @forms :bytes (reduce + (map #(.length %) files))
                   :excluded "trace, step, tripwire aggregates, morning-brief, proposals, arbitrary Agency jobs; copies are appearances, not independent trials"}
            :summary (into (sorted-map) (map (fn [[k v]] [k (summary v)]) (group-by :category @out)))
            :typed-statuses (into (sorted-map) (map (fn [[k v]] [(pr-str k) (summary v)])
                            (group-by #(select-keys (:fields %) [:status :reason :kind :stage])
                                      (filter #(= :typed-status (:category %)) @out))))
            :errors @errors :events @out})
