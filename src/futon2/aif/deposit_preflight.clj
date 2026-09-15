(ns futon2.aif.deposit-preflight
  "Offline preflight for one complete attempt evidence directory.

  This copies the candidate bytes into an isolated attempt layout and invokes
  the runner's real checkpoint-evidence-manifest admission seam.  It confers
  no authority and writes neither the live evidence directory nor a close."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.full-loop-runner :as runner])
  (:import [java.nio.file Files StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(def ^:private record-schemas
  #{:wm/limb-receipt-v1 :wm/target-standing-decision-v1
    :wm/entity-revision-pair-v1})

(defn- candidate-files [dir]
  (let [files (seq (.listFiles (io/file dir)))]
    (when (some #(.isDirectory ^java.io.File %) files)
      (throw (ex-info "Deposit directory must be flat"
                      {:limb-evidence/refusal :evidence-directory-not-flat})))
    (vec (sort-by #(.getName ^java.io.File %) files))))

(defn- form [file]
  (try (edn/read-string (slurp file)) (catch Throwable _ nil)))

(defn- classifications [files]
  (let [records (filter #(contains? record-schemas (:schema (form %))) files)
        companions (into #{}
                         (mapcat (fn [file]
                                   (let [v (form file)]
                                     (concat (keep v [:stdout-file :stderr-file])
                                             (keep #(get-in v [% :file])
                                                   [:before :after])))))
                         records)]
    (into {} (map (fn [^java.io.File file]
                    [(.getName file)
                     (if (contains? companions (.getName file))
                       :admitted-as-companion
                       :admitted-as-record)]) files))))

(defn preflight-dir
  "Run the production admission seam over DIR for REPAIR-ID.
  Returns {:ok? ... :verdicts ...}; each verdict is also printed as EDN."
  [dir repair-id]
  (let [files (candidate-files dir)
        attrs (make-array FileAttribute 0)
        root (.toFile (Files/createTempDirectory "wm-deposit-preflight-" attrs))
        evidence (io/file root "preflight" "attempt-001" "evidence")]
    (.mkdirs evidence)
    (doseq [^java.io.File file files]
      (Files/copy (.toPath file)
                  (.toPath (io/file evidence (.getName file)))
                  (into-array StandardCopyOption
                              [StandardCopyOption/REPLACE_EXISTING])))
    (try
      (#'runner/checkpoint-evidence-manifest
       {} (.getAbsolutePath root) :preflight "attempt-001" repair-id)
      (let [verdicts (classifications files)]
        (doseq [[file verdict] verdicts]
          (prn {:file file :verdict verdict}))
        {:ok? true :verdicts verdicts})
      (catch clojure.lang.ExceptionInfo e
        (let [data (ex-data e)
              refusal (or (:limb-evidence/refusal data)
                          (:evidence-manifest/refusal data)
                          :untyped-refusal)
              culprit (or (:filename data)
                          (some-> (:source-path data) io/file .getName))
              verdicts (into {} (map (fn [^java.io.File file]
                                       [(.getName file)
                                        {:verdict :WOULD-REFUSE
                                         :refusal refusal
                                         :culprit? (= culprit (.getName file))}])
                                     files))]
          (doseq [[file verdict] verdicts] (prn (assoc verdict :file file)))
          {:ok? false :refusal refusal :data data :verdicts verdicts})))))

(defn -main [& [dir repair-id]]
  (when-not (and dir repair-id)
    (binding [*out* *err*]
      (println "usage: clojure -M -m futon2.aif.deposit-preflight DIR REPAIR-ID"))
    (System/exit 2))
  (when-not (:ok? (preflight-dir dir repair-id))
    (System/exit 1)))
