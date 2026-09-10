(ns futon2.aif.c-fold-config
  "Materialize the explicitly enabled, pinned RUN4 C fold. No fitting at runtime."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.disposition-risk :as risk]
            [futon2.aif.ruled-outcome-c :as ruled])
  (:import [java.security MessageDigest]))

(defn sha256 [text]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256")
                                         (.getBytes text "UTF-8")))))

(defn- refuse! [reason]
  (throw (ex-info "C fold configuration refused" {:refused? true :reason reason})))

(defn- parse-one [text]
  (try
    (with-open [r (java.io.PushbackReader. (java.io.StringReader. text))]
      (let [value (edn/read r)]
        (when-not (= ::end (edn/read {:eof ::end} r)) (refuse! :trailing-config-form))
        value))
    (catch Exception e
      (if (:refused? (ex-data e)) (throw e) (refuse! :invalid-config-edn)))))

(defn- pinned [path digest read-text]
  (when-not (and (string? path) (string? digest)) (refuse! :missing-source-pin))
  (let [text (try (read-text path) (catch Exception _ (refuse! :unreadable-source)))]
    (when-not (= digest (sha256 text)) (refuse! :source-pin-mismatch))
    (parse-one text)))

(defn resolve-opts
  "Explicit fold opts win, including false. Otherwise consume :c-fold from
   FUTON_WM_RUN_CONFIG. Relative artifact paths resolve beside that sheet.
   Provenance describes the exact loaded seed and kernel, not a live fit."
  ([opts] (resolve-opts opts (System/getenv "FUTON_WM_RUN_CONFIG") slurp))
  ([opts config-path read-text]
   (if (contains? opts :ruled-outcome-c-enabled?)
     (do (when-not (boolean? (:ruled-outcome-c-enabled? opts))
           (refuse! :invalid-enabled-flag))
         opts)
     (if-not config-path opts
       (let [sheet (try (parse-one (read-text config-path))
                        (catch Exception _ (refuse! :unreadable-run-config)))
             config (:c-fold sheet)]
         (if-not config
           (if (get-in sheet [:flags :ruled-outcome-c-enabled?])
             (refuse! :unmaterialized-legacy-fold-config) opts)
           (let [{:keys [enabled? seed kernel]} config]
             (when-not (boolean? enabled?) (refuse! :invalid-enabled-flag))
             (if-not enabled? (assoc opts :ruled-outcome-c-enabled? false)
               (let [resolve-path (fn [p]
                                    (when-not (string? p) (refuse! :missing-source-pin))
                                    (let [f (io/file p)]
                                      (str (if (.isAbsolute f) f
                                               (io/file (.getParentFile (io/file config-path)) p)))))
                     _ (when-not (and (= :ruled-outcome-c-v1 (:id seed))
                                      (= :constant-checkpoint-kernel/v1 (:adapter kernel)))
                         (refuse! :unknown-c-selector))
                     seed-value (pinned (resolve-path (:path seed)) (:sha256 seed) read-text)
                     _ (when-not (= ruled/seeded-c seed-value) (refuse! :seed-carrier-mismatch))
                     artifact (pinned (resolve-path (:path kernel)) (:sha256 kernel) read-text)
                     adapter (risk/constant-checkpoint-kernel artifact)]
                 (assoc opts :ruled-outcome-c-enabled? true
                        :seeded-c seed-value :disposition-kernel adapter
                        :c-fold-provenance
                        {:enabled? true :boundary :efe-disposition-risk
                         :seed seed :kernel kernel :source (:source artifact)
                         :constant-across-policies? true
                         :observation-model-bridge :open}))))))))))
