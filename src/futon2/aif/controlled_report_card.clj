(ns futon2.aif.controlled-report-card
  "Controlled evidence wrapper around the authoritative RunFacts/Lean checker."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]
            [wm-check-run-facts :as checker]
            [wm-run-facts :as run-facts]))

(def schema :wm/controlled-report-card-v1)
(def checker-request-schema :wm/requirements-checker-request-v1)
(def terminal-schema :wm/requirements-checker-terminal-v1)
(def result-schema :wm/lean-requirements-result-v1)
(def requirements (set (map #(keyword (str "q" %)) (range 1 11))))
(def outcomes #{:holds :violated :unverifiable})

(defn- descriptor-valid? [x]
  (and (map? x) (some? (:identity x)) (pos-int? (:version x))
       (pos-int? (:authority x)) (seq (:implementation-pin x))))

(defn- parse-output [output]
  (try
    (let [x (edn/read-string output)]
      (when (and (= result-schema (:schema x))
                 (= requirements (set (keys (:outcomes x))))
                 (every? outcomes (vals (:outcomes x))))
        (:outcomes x)))
    (catch Exception _ nil)))

(defn produce
  [hydrated-record {:keys [checker executor snapshot record-path previous previous-path]}]
  (if-not (and (descriptor-valid? checker) (ifn? executor) (map? snapshot))
    {:status :absent :reason :controlled-checker-authority-unconfigured}
    (let [export (run-facts/facts-for-record hydrated-record
                                             (or record-path "controlled-run-record")
                                             snapshot previous previous-path)
          facts (:facts export)
          source (checker/lean-source {"facts" facts} "controlledReportCard")
          request {:schema checker-request-schema
                   :run-record-digest (evidence/value-digest hydrated-record)
                   :facts-digest (evidence/value-digest facts)
                   :source-digest (evidence/value-digest source)
                   :checker checker
                   :lean-source source}
          request-digest (evidence/value-digest request)
          terminal (executor (assoc request :request-digest request-digest))
          output (:output terminal)
          parsed (when (string? output) (parse-output output))
          valid? (and (= terminal-schema (:schema terminal))
                      (= checker (:checker terminal))
                      (= request-digest (:request-digest terminal))
                      (= (:source-digest request) (:source-digest terminal))
                      (= 0 (:exit terminal))
                      (= (evidence/value-digest output) (:output-digest terminal))
                      (= (evidence/value-digest (dissoc terminal :terminal-digest))
                         (:terminal-digest terminal))
                      (= #{:schema :checker :request-digest :source-digest :exit
                           :output :output-digest :terminal-digest}
                         (set (keys terminal)))
                      parsed)]
      (if-not valid?
        {:schema schema :status :refused :reason :checker-terminal-invalid
         :request-digest request-digest}
        {:schema schema :status :verified
         :run-record-digest (:run-record-digest request)
         :exported-facts-digest (:facts-digest request)
         :lean-source-digest (:source-digest request)
         :checker checker
         :terminal terminal
         :terminal-output-digest (:output-digest terminal)
         :outcomes parsed}))))

(defn write! [sut-root card]
  (let [path (io/file sut-root "report-card.edn")]
    (spit path (pr-str card))
    {:path (.getCanonicalPath path)
     :value (edn/read-string (slurp path))}))
