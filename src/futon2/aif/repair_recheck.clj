(ns futon2.aif.repair-recheck
  "Dated, source-bound observation of the held-out calibration precondition."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.observation-checks :as checks])
  (:import [java.nio.file Files StandardCopyOption]
           [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(load-identity/register! *ns* *file*)

(def schema :wm/repair-recheck-v1)
(def cleared-disposition 'RECHECK-DISPOSITION-CLEARED)
(def present-disposition 'RECHECK-DISPOSITION-PRESENT)
(def calibration-disposition 'CALIBRATION-EVIDENCE-PASSING)

(defn- sha256 [bytes]
  (apply str (map #(format "%02x" %)
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn observe
  "Observe CALIBRATION-BYTES at CHECKED-AT. Malformed or non-passing evidence
  produces :present; only the complete passing contract produces :cleared."
  [source-path calibration-bytes checked-at]
  (let [text (String. ^bytes calibration-bytes java.nio.charset.StandardCharsets/UTF_8)
        record (try (edn/read-string text) (catch Exception _ nil))
        checks {:parseable? (map? record)
                :schema? (= :wm/eig-held-out-calibration-v1 (:schema record))
                :status-passing? (= :passing (:status record))
                :claim-present? (true? (get-in record [:claims :calibration-evidence-present?]))
                :disposition? (= calibration-disposition (:disposition record))
                :c4-head? (checks/decl-present? text (str calibration-disposition))}
        cleared? (every? true? (vals checks))]
    {:schema schema
     :ticket/id "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade"
     :checked-at checked-at
     :condition :held-out-calibration-evidence-present
     :source {:path source-path :sha256 (sha256 calibration-bytes)}
     :checks checks
     :observed (if cleared? :cleared :present)
     :disposition (if cleared? cleared-disposition present-disposition)}))

(defn render [record]
  (let [disposition (:disposition record)
        body (str/trimr (with-out-str (pprint/pprint (dissoc record :disposition))))
        inner (str/trimr (subs body 0 (str/last-index-of body "}")))]
    (str inner "\n :disposition\n" disposition "\n}\n")))

(defn write-recheck!
  [source-path output-path]
  (let [bytes (Files/readAllBytes (.toPath (io/file source-path)))
        record (observe source-path bytes (str (Instant/now)))
        rendered (render record)
        target (io/file output-path)
        tmp (io/file (.getParentFile target)
                     (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
    (when (and (= :cleared (:observed record))
               (not (checks/decl-present? rendered (str cleared-disposition))))
      (throw (ex-info "Cleared recheck disposition is not C4-observable"
                      {:repair-recheck/refusal :disposition-head-not-observable})))
    (io/make-parents target)
    (spit tmp rendered)
    (Files/move (.toPath tmp) (.toPath target)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))
    record))

(defn -main [& _]
  (println (pr-str (select-keys
                    (write-recheck! "resources/wm/eig/held-out-calibration.edn"
                                    "resources/wm/rechecks/repair-occ-444fb018-dated-recheck.edn")
                    [:observed :disposition :checked-at]))))
