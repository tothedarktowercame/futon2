(ns futon2.aif.surprise
  "Record-only mismatches to selection-frozen positive-support expectations.
   Incidents without this expectation/comparison carrier are not surprises."
  (:require [futon2.aif.action-identity :as identity]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.time Instant]))

(load-identity/register! *ns* *file*)

(defn- instant [x]
  (try (when (string? x) (Instant/parse x)) (catch Exception _ nil)))

(defn records
  "Historical observations can lack an exact timestamp; retain that absence.
   The caller supplies the durable selection time and post-build observation.
   A known reversed clock, missing declaration time or missing occurrence cannot
   establish D1. This records no revision, parameter update or causal proof."
  [{:keys [comparison occurrence declared-at observed-at]}]
  (let [p (:prediction comparison)
        declared (instant declared-at)
        observed (instant observed-at)]
    (if-not (and (= :wm/token-outcome-comparison-v1 (:schema comparison))
                 (= :compared (:status comparison)) (= :frozen (:status p))
                 (= :positive-marginal-support (:prediction-rule p))
                 (:action/id occurrence) declared
                 (or (nil? observed-at) (and observed (.isBefore declared observed))))
      []
      (vec
       (for [{:keys [token predicted observed verdict measurement]} (:tokens comparison)
             :when (and (number? predicted) (boolean? observed)
                        (or (and (= :predicted-not-observed verdict) (pos? predicted) (false? observed))
                            (and (= :not-predicted-observed verdict) (zero? predicted) (true? observed))))
             :let [expectation-digest (identity/digest {:prediction p :token token})
                   produced? (contains? (:intended-outputs p) token)
                   model-part (cond (= :not-predicted-observed verdict) :D-or-external
                                    produced? :B-effect :else :D-prediction)]]
         {:schema :wm/surprise-v1
          :surprise/id (str "surprise-" (identity/digest
                                       {:occurrence occurrence :token token
                                        :expectation-digest expectation-digest}))
          :occurrence occurrence :token token :verdict verdict
          :model-part model-part
          :model-part-reason (case model-part
                               :B-effect :declared-produced-token-not-observed
                               :D-or-external :observed-outside-predicted-support-cause-unattributed
                               :D-prediction :predicted-state-token-not-a-declared-effect)
          :expectation {:id (str "expectation-" expectation-digest)
                        :digest expectation-digest :declared-at declared-at
                        :scope {:target (:target p) :token token :horizon (:horizon p)}
                        :rule :positive-marginal-support :predicted predicted
                        :tolerance {:expected-observed (pos? predicted)}}
          :observation {:value observed :evidence-digest (identity/digest measurement)
                        :artifact-sha (:artifact-sha comparison)
                        :observed-at (or observed-at {:status :not-recorded})
                        :placement :post-build-artifact-observation}
          :causal-attribution :not-established
          :revision {:status :none-yet}})))))

(defn records-for-action
  "Read retained surprise records and return only exact selected-action tokens.

  This is a read-only prompt lookup over the same retained/surprises.edn
  records consumed by revision-scanner. An unreadable record is explicit; it
  never prevents author dispatch."
  [root action]
  (try
    (let [root-file (io/file root)
          _ (when-not (.isDirectory root-file)
              (throw (ex-info "Surprise store is not a directory"
                              {:path (.getAbsolutePath root-file)})))
          tokens (into #{} (mapcat :produces) (:precedence action))
          files (filter #(and (.isFile ^java.io.File %)
                              (str/ends-with? (.getPath ^java.io.File %)
                                              (str java.io.File/separator
                                                   "retained"
                                                   java.io.File/separator
                                                   "surprises.edn")))
                        (file-seq root-file))
          rows (mapcat (fn [file]
                         (let [value (edn/read-string (slurp file))]
                           (when-not (vector? value)
                             (throw (ex-info "Retained surprises must be a vector"
                                             {:path (.getAbsolutePath ^java.io.File file)})))
                           value))
                       files)
          matches (->> rows
                       (filter #(and (= :wm/surprise-v1 (:schema %))
                                     (contains? tokens (:token %))))
                       (reduce (fn [by-id row]
                                 (assoc by-id (:surprise/id row) row)) {})
                       vals
                       (sort-by :surprise/id)
                       vec)]
      {:status :ok :records matches})
    (catch Throwable e
      {:status :unavailable
       :kind :surprise-store-unreadable
       :path (some-> root io/file .getAbsolutePath)
       :exception-class (.getName (class e))})))
