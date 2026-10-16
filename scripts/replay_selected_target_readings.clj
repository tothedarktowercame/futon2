(ns replay-selected-target-readings
  "Offline reliability gate for saved selected-target reading answers."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.selected-target-reading :as selected]))

(defn- replay-one [dir]
  (let [request-file (io/file dir "request.json")
        analysis-file (io/file dir "analysis-input.json")]
    (when (and (.isFile request-file) (.isFile analysis-file))
      (let [request (json/parse-string (slurp request-file) true)
            analysis (json/parse-string (slurp analysis-file) true)
            resolution (selected/resolve-analysis-offsets request analysis)]
        (if-not (= :resolved (:status resolution))
          {:request (.getName dir) :status :refused
           :stage :offset-resolution
           :reason (dissoc resolution :analysis)}
          (let [tmp (.toFile
                     (java.nio.file.Files/createTempDirectory
                      "selected-reading-replay-"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
                request-copy (io/file tmp "request.json")
                analysis-copy (io/file tmp "analysis.json")
                _ (spit request-copy (json/generate-string request))
                _ (spit analysis-copy
                        (json/generate-string (:analysis resolution)))
                result (shell/sh "python3" selected/default-analysis-validator
                                 "complete" (.getPath request-copy)
                                 (.getPath analysis-copy))]
            {:request (.getName dir)
             :target (get-in request [:task :target_id])
             :status (if (zero? (:exit result)) :validated :refused)
             :stage :validator
             :reason (when-not (zero? (:exit result))
                       (str/trim (:err result)))}))))))

(defn -main [& [root]]
  (let [root (io/file (or root selected/default-request-root))
        results (->> (.listFiles root)
                     (filter #(.isDirectory %))
                     (keep replay-one)
                     (sort-by :request)
                     vec)
        validated (count (filter #(= :validated (:status %)) results))
        report {:schema :wm/selected-target-reading-replay-v1
                :root (.getCanonicalPath root)
                :saved-answers (count results)
                :validated validated
                :refused (- (count results) validated)
                :gate {:required "6/6, or 5/6 with the remaining non-offset failure explained"
                       :status (if (= validated (count results) 6)
                                 :pass :fail)}
                :results results}]
    (prn report)
    (shutdown-agents)
    (when (= :fail (get-in report [:gate :status]))
      (System/exit 1))))

(apply -main *command-line-args*)
