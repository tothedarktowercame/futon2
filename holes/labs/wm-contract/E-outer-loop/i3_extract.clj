#!/usr/bin/env clojure -M
;; I3 extraction: what the pre-H5b outer loop proposed and chose.
;; Reads wm-trace judge tick EDN files (read-only), prints an EDN report.
;; Usage: clojure -M i3_extract.clj   (run from /home/joe/code/futon2)
;; Every number in I3-records.md comes from this script's output.
(require '[clojure.pprint :as pprint]
         '[clojure.java.io :as io]
         '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[clojure.set :as set])
(import 'java.security.MessageDigest)

(def cutoff "2026-09-17") ; H5b, 5d55e7a0

(defn sha8 [f]
  (let [md (MessageDigest/getInstance "SHA-256")
        b (java.nio.file.Files/readAllBytes (.toPath (io/file f)))]
    (subs (.substring (format "%064x" (BigInteger. 1 (.digest md b))) 0) 0 8)))

(defn read-forms [f]
  (with-open [rd (java.io.PushbackReader. (io/reader f))]
    (loop [a []]
      (let [x (edn/read {:default tagged-literal :eof ::eof} rd)]
        (if (= x ::eof) a (recur (conj a x)))))))

;; Proposer attribution: the records carry no :proposer field, so a proposer
;; is inferred from the action :type and :rationale prefix, using the mapping
;; below (derived from the proposer docstrings cited in holes/E-outer-loop.md).
;; Unmatched actions are counted as :unattributed, never guessed.
(defn infer-proposer [a]
  (let [t (:type a) r (str (:rationale a))]
    (cond
      (contains? #{:no-op :learn-action-class} t) :bootstrap
      (= t :fire-pattern) :pattern
      (contains? #{:open-mission :advance-mission} t)
      (if (str/starts-with? r "tension at mission") :tension :mission)
      (contains? #{:open-ticket :advance-ticket :address-ticket} t) :ticket
      (= t :address-sorry) :sorry
      (str/starts-with? r "tension") :tension
      :else :unattributed)))

(defn tick-report [file t]
  (let [ra (:ranked-actions t)
        pse (:policy-support-exclusions t)
        dec (:decision t)
        act (:action dec)
        ranked-by-rank (sort-by :rank ra)
        top (first ranked-by-rank)
        top-score (:controller-score top)
        chosen-score (:controller-score dec)
        gaps (when (and (number? top-score) (number? chosen-score))
               (- top-score chosen-score))
        tie-threshold 0.01
        near-ties (when (number? top-score)
                    (count (filter #(and (number? (:controller-score %))
                                         (< (Math/abs (- top-score (:controller-score %)))
                                            tie-threshold))
                                   ra)))]
    {:file file
     :timestamp (:timestamp t)
     :mode (:mode t)
     :n-ranked (count ra)
     :ranked-type-counts (frequencies (map #(get-in % [:action :type]) ra))
     :ranked-proposer-counts (frequencies (map #(infer-proposer (:action %)) ra))
     :n-pse (count pse)
     :pse-reason-counts (frequencies (map :reason pse))
     :pse-type-counts (frequencies (map #(get-in % [:action :type]) pse))
     :decision-type (:type act)
     :decision-target (:target act)
     :decision-rank (:rank dec)
     :decision-present (some? act)
     :decision-score chosen-score
     :top-score top-score
     :top-action-type (get-in top [:action :type])
     :top-action-target (get-in top [:action :target])
     :gap-top-minus-chosen gaps
     :n-within-0.01-of-top near-ties
     :tie-break-field-present (contains? dec :tie-break-rule)
     :selection-law (:selection-law dec)}))

(def all-files
  (->> (.listFiles (io/file "data/wm-trace"))
       (map str)
       (filter #(re-find #"wm-trace-\d{4}-\d{2}-\d{2}\.edn$" %))
       (filter #(neg? (compare (second (re-find #"wm-trace-(\d{4}-\d{2}-\d{2})\.edn$" %))
                               cutoff)))
       sort vec))

(def report
  {:cutoff cutoff
   :files (mapv (fn [f] {:file f :sha256-8 (sha8 f)}) all-files)
   :ticks
   (into []
         (mapcat (fn [f]
                   (try (mapv #(tick-report f %) (read-forms f))
                        (catch Exception e
                          [{:file f :read-error (str (.getMessage e))}]))))
         all-files)})

(pprint/pprint report)
