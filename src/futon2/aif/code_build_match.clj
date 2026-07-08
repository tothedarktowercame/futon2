(ns futon2.aif.code-build-match
  "Automated build-match for CODE-pipeline CLean proofs.

   Unlike substrate build-match, these boxes represent implementation stages.
   A box is ready when its reviewed code binding resolves and its reviewed
   gating sorries have no open entries in the live sorry registry."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.sorry-registry :as sorry])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def ^:private clean-argcheck-script
  "/home/joe/code/futon6/scripts/clean_argcheck.bb")

(def aif-grounded-loop-clean-path
  "/home/joe/code/futon6/holes/clean/aif-grounded-loop.clean.edn")

(def reviewed-code-bindings
  "Reviewed code bindings for the aif-grounded-loop CLean.

   The bindings are authored by the reviewer/owner lane, never supplied by the
   builder. Open sorries gate incomplete stages; `:known-not-ready?` marks a
   stage that would otherwise read green and therefore surfaces a missing-sorry
   faithfulness gap instead of passing silently."
  {:b1 {:code-ns ['futon2.aif.belief]
        :code-var 'futon2.aif.belief/update-belief
        :gating-sorries []
        :known-not-ready? true
        :faithfulness-note "R1 belief covers only ~4/14 channels; no open registry sorry records that incompleteness."}
   :b2 {:code-ns ['futon2.aif.forward-model]
        :code-var 'futon2.aif.forward-model/predict
        :gating-sorries []
        :known-not-ready? true
        :faithfulness-note "R4 forward model is still shape-only; no open registry sorry records that incompleteness."}
   :b3 {:code-ns ['futon2.aif.efe]
        :code-var 'futon2.aif.efe/compute-efe
        :gating-sorries []}
   :b4 {:code-ns ['futon2.aif.policy]
        :code-var 'futon2.aif.policy/select-action
        :gating-sorries []
        :known-not-ready? true
        :faithfulness-note "R13 policy has the tie-floor and acting is HELD; no open registry sorry records that incompleteness."}
   :b5 {:code-ns ['futon2.aif.actuator-a3]
        :code-var 'futon2.aif.actuator-a3/build-match
        :gating-sorries []}
   :b6 {:code-ns ['futon2.aif.fold-realized]
        :code-var 'futon2.aif.fold-realized/realized-outcome-of
        :gating-sorries [:sorry/wm-realised-on-merge-binding]}
   :b7 {:code-ns ['futon2.aif.c-vector]
        :code-var 'futon2.aif.c-vector/grounded-aif-loop-accumulate
        :gating-sorries [:sorry/wm-realised-on-merge-binding]}})

(defn load-clean
  ([] (load-clean aif-grounded-loop-clean-path))
  ([path]
   (edn/read-string (slurp path))))

(defn- temp-clean-file [clean]
  (let [path (Files/createTempFile "code-build-match-clean" ".clean.edn"
                                   (into-array FileAttribute []))]
    (spit (.toFile path) (pr-str clean))
    (.toFile path)))

(defn clean-validation
  "Validate a CLean map by shelling out to futon6's canonical clean_argcheck.
   This deliberately reuses the same eight gates instead of reimplementing them."
  [clean]
  (let [^File tmp (temp-clean-file clean)
        pb (doto (ProcessBuilder. ["bb" clean-argcheck-script (.getAbsolutePath tmp)])
             (.directory (io/file "/home/joe/code/futon6"))
             (.redirectErrorStream true))
        proc (.start pb)
        output (slurp (.getInputStream proc))
        exit (.waitFor proc)]
    (.delete tmp)
    {:clean-pass? (zero? exit)
     :exit exit
     :output output
     :errors (->> (str/split-lines output)
                  (filter #(str/includes? % "G"))
                  vec)}))

(defn- require-ns? [ns-sym]
  (try
    (require ns-sym)
    true
    (catch Throwable _
      false)))

(defn- resolving-var? [var-sym]
  (try
    (boolean (requiring-resolve var-sym))
    (catch Throwable _
      false)))

(defn code-present?
  [{:keys [code-ns code-var code-vars]}]
  (let [namespaces (or (seq code-ns) [])
        vars (cond-> []
               code-var (conj code-var)
               (seq code-vars) (into code-vars))]
    (and (every? require-ns? namespaces)
         (seq vars)
         (every? resolving-var? vars))))

(defn- open-sorry-id-set [opts]
  (let [loaded (or (:sorry-doc opts) (sorry/load-sorrys))]
    (set (map :id (sorry/open-sorrys loaded)))))

(defn- row-for-box [open-ids {:keys [id]} binding]
  (let [present? (boolean (and binding (code-present? binding)))
        open-sorries (->> (:gating-sorries binding)
                          (filter open-ids)
                          vec)
        sorry-clear? (empty? open-sorries)
        faithfulness-gap? (boolean (and present?
                                        sorry-clear?
                                        (:known-not-ready? binding)))
        ready? (boolean (and present? sorry-clear? (not faithfulness-gap?)))]
    (cond-> {:box id
             :code-present? present?
             :sorry-clear? sorry-clear?
             :open-sorries open-sorries
             :ready? ready?}
      faithfulness-gap?
      (assoc :faithfulness-gap true
             :faithfulness-note (:faithfulness-note binding)))))

(defn code-box-match
  "Return a derived readiness dashboard for a CODE-pipeline CLean."
  ([clean code-bindings] (code-box-match clean code-bindings {}))
  ([clean code-bindings opts]
   (let [validation (clean-validation clean)
         boxes (:clean/boxes clean)
         total (count boxes)]
     (if-not (:clean-pass? validation)
       {:clean-pass? false
        :refused? true
        :reason :clean-invalid
        :clean-errors (:errors validation)
        :boxes []
        :dial {:ready 0 :total total}
        :faithfulness-gaps []}
       (let [open-ids (open-sorry-id-set opts)
             rows (mapv #(row-for-box open-ids % (get code-bindings (:id %))) boxes)
             ready (count (filter :ready? rows))
             gaps (mapv :box (filter :faithfulness-gap rows))]
         {:clean-pass? true
          :refused? false
          :boxes rows
          :dial {:ready ready :total total}
          :faithfulness-gaps gaps})))))

(defn aif-grounded-loop-match
  ([] (aif-grounded-loop-match {}))
  ([opts]
   (code-box-match (load-clean) reviewed-code-bindings opts)))
