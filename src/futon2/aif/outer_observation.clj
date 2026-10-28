(ns futon2.aif.outer-observation
  "Build the task-level `R20Step.Observation` from verified Giraffe evidence.

  The serving JVM supplies futon3c's verifier dynamically.  This namespace
  does not duplicate its manifest law and refuses when that authority is not
  available."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.aif.outer-state :as outer-state])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def ^:dynamic *resolve-var* requiring-resolve)

(defn- refuse! [kind data]
  (throw (ex-info "Outer observation refused" (assoc data :kind kind))))

(defn- sha256 [^bytes bytes]
  (let [digest (.digest (doto (MessageDigest/getInstance "SHA-256")
                          (.update bytes)))]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(defn- read-edn-file [file kind]
  (try
    (edn/read-string (slurp file))
    (catch Exception e
      (refuse! kind {:path (str file) :message (.getMessage e)}))))

(defn- verifier []
  (try
    (or (*resolve-var* 'futon3c.giraffe.run/verify-run)
        (refuse! :giraffe-verifier-unavailable {}))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Exception e
      (refuse! :giraffe-verifier-unavailable {:message (.getMessage e)}))))

(defn giraffe-field
  "Build Lean `Observation.field` and `Observation.facts` from a verified run.

  Returns `{:field ... :facts ... :giraffe/run-id ...
  :giraffe/manifest-sha256 ...}`.  Candidate reach is the makeup's explicit
  `:default-reach`, because task reach is not yet produced by the runtime."
  ([run-dir] (giraffe-field run-dir (outer-state/load-makeup)))
  ([run-dir makeup]
   (let [dir (io/file run-dir)
         verification ((verifier) dir)]
     (when-not (:ok? verification)
       (refuse! :giraffe-run-unverified
                {:run-dir (.getPath dir) :verification verification}))
     (let [manifest-file (io/file dir "manifest.edn")
           pass-file (io/file dir "pass.edn")]
       (when-not (and (.isFile manifest-file) (.isFile pass-file))
         (refuse! :giraffe-run-incomplete {:run-dir (.getPath dir)}))
       (let [manifest-bytes (Files/readAllBytes (.toPath manifest-file))
             manifest (read-edn-file manifest-file :giraffe-manifest-malformed)
             pass (read-edn-file pass-file :giraffe-pass-malformed)
             rows (:order pass)]
         (when-not (and (vector? rows)
                        (every? #(and (:id %) (contains? % :status)
                                      (nat-int? (:unblocks %)) (nat-int? (:cone %))) rows))
           (refuse! :giraffe-pass-invalid {:run-dir (.getPath dir)}))
         (let [entries (mapv (fn [{:keys [id status unblocks cone]}]
                               (let [target (name id)]
                                 {:candidate {:target target
                                              :reach (:default-reach makeup)}
                                  :target target
                                  :fact {:status status
                                         :unblocks unblocks
                                         :cone cone}}))
                             rows)]
           {:field (mapv :candidate entries)
            :facts (into {} (map (juxt :target :fact)) entries)
            :giraffe/run-id (:run-id manifest)
            :giraffe/manifest-sha256 (sha256 manifest-bytes)}))))))

(defn outer-observation
  "Build the input read by `outer-score`, aligned with Lean `R20Step.Observation`.

  Required keys are `:run-dir`, `:makeup`, `:focus-inputs`, and
  `:decision-as-of`.  Optional `:previous-focus` and `:relation-context` are
  passed to the shared focus producer.  Unknown relations remain `:unknown`."
  [{:keys [run-dir makeup focus-inputs decision-as-of previous-focus relation-context]}]
  (let [giraffe (giraffe-field run-dir makeup)
        focus-result (focus-receipt/decision-focus focus-inputs decision-as-of previous-focus)
        focus-info (:info focus-result)
        relation (fn [focus target]
                   (:class (focus-receipt/classify-target
                            focus-inputs
                            (assoc focus-info :focus focus)
                            decision-as-of target relation-context)))]
    (merge giraffe
           {:focus (:focus focus-info)
            :relation relation
            :focus/source (select-keys focus-info
                                       [:status :focus-origin :as-of
                                        :retained-evidence-as-of])})))
