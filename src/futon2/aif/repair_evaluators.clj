(ns futon2.aif.repair-evaluators
  "Registered evaluator implementations. Admissions are separately authored
   and independently reviewed; merely registering code grants no authority."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.repair-discharge-evidence :as evidence])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.util.concurrent TimeUnit]))

(def driver-path "src/futon2/aif/repair_history_replay.clj")
(def launcher-path "src/futon2/aif/repair_evaluators.clj")
(defn- source-text [path] (slurp (io/resource (subs path 4))))
(def ^:private loaded-source
  (into {} (map (fn [path] [path (evidence/sha256 (evidence/text-bytes (source-text path)))]))
        [driver-path launcher-path]))

(defn- command! [dir args]
  (let [stdout (java.io.File/createTempFile "evaluator-stdout-" ".log" (io/file dir))
        stderr (java.io.File/createTempFile "evaluator-stderr-" ".log" (io/file dir))
        process (.start (doto (ProcessBuilder. ^java.util.List (vec args))
                          (.directory (io/file dir))
                          (.redirectOutput stdout) (.redirectError stderr)))
        finished? (.waitFor process 60 TimeUnit/SECONDS)]
    (when-not finished?
      (with-open [descendants (.descendants (.toHandle process))]
        (doseq [child (iterator-seq (.iterator descendants))] (.destroyForcibly ^java.lang.ProcessHandle child)))
      (.destroyForcibly process)
      (.waitFor process 5 TimeUnit/SECONDS))
    (evidence/require! finished? :evaluator-timeout {:timeout-ms 60000})
    (evidence/require! (zero? (.exitValue process)) :evaluator-process-failed
                      {:exit (.exitValue process) :stdout (slurp stdout) :stderr (slurp stderr)})))

(defn history-artifact-read!
  [{:keys [admission finding finding-pin artifact] :as request}]
  (let [input (get-in admission [:inputs (:repair/id finding)])
        repo (:repo artifact)
        parent (.getParentFile (.getCanonicalFile (io/file repo)))]
    (evidence/require! (and (= :initialization-failed (:failure-kind finding))
                            (= (:sha256 finding-pin) (:finding-sha256 input))
                            (= (:recorded-locator input) (get-in finding [:failure-data :poisoned-artifact]))
                            (:path input) (:sha256 input))
                      :recorded-failure-input-unavailable {:repair/id (:repair/id finding)})
    ;; A sibling checkout preserves the repo's declared relative local deps.
    ;; Archive is read-only with respect to canonical refs/index/worktrees.
    (let [scratch (.toFile (Files/createTempDirectory (.toPath parent) "futon2-discharge-replay-"
                                                    (make-array FileAttribute 0)))
          tool (.toFile (Files/createTempDirectory "repair-evaluator-" (make-array FileAttribute 0)))
          archive (io/file tool "repair.tar")
          driver (io/file tool driver-path)
          in (io/file tool "request.edn") out (io/file tool "observation.edn")]
      (try
        (evidence/git! repo "archive" "--format=tar" (str "--output=" archive) (:commit artifact))
        (command! (.getPath tool) ["tar" "-xf" (.getPath archive) "-C" (.getPath scratch)])
        (io/make-parents driver)
        (spit driver (source-text driver-path))
        (evidence/require! (= (get loaded-source driver-path) (evidence/sha256 (evidence/bytes-at driver)))
                          :evaluator-source-changed {})
        (spit in (evidence/canonical-text
                  (assoc request :input input :scratch (.getPath (io/file tool "cohort"))
                         :prereg (.getPath (io/file scratch "holes/labs/M-aif-full-loop-40/cohort.edn")))))
        (command! (.getPath tool)
                  ["clojure" "-Sdeps"
                   (pr-str {:paths [(.getPath (io/file tool "src"))]
                            :deps {'repaired/repo {:local/root (.getPath scratch)}}})
                   "-M" "-m" "futon2.aif.repair-history-replay" (.getPath in) (.getPath out)])
        (evidence/read-one (slurp out))
        (finally
          (doseq [root [scratch tool] file (reverse (file-seq root))]
            (io/delete-file file true)))))))

(def registry
  {:history-artifact-read {:loaded-source loaded-source :evaluate history-artifact-read!}})

(defn admissions
  "Committed admission locators, read anew for each attempted discharge.
   Empty means no kind is admitted; evaluator code presence is not admission."
  []
  (let [record (edn/read-string (slurp (io/resource "wm/repair-evaluator-admissions.edn")))]
    (evidence/require! (and (= :wm/repair-evaluator-index-v1 (:schema record))
                            (map? (:evaluators record))) :evaluator-index-invalid {})
    (:evaluators record)))
