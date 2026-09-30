(ns futon2.aif.target-reading-registry-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.target-reading-registry :as sut]))

(def d1 (apply str (repeat 64 "1")))
(def d2 (apply str (repeat 64 "2")))

(defn temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "target-reading-registry"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(def valid-analysis
  {:status "analyzed"
   :sentences [{:id "s1"
                :fragments [{:start 0 :end 4 :text "work" :intent "act"
                             :target "mission" :rationale "the stated work"
                             :relations ["action"]
                             :pattern_refs [{:status "candidate"
                                             :id "test/example"
                                             :rationale "fits the stated work"
                                             :source_sha256 (apply str (repeat 64 "a"))}]
                             :pattern_rejections []}]}]})

(defn publication [target digest]
  {:target-id target :source-path (str "/tmp/" target ".md")
   :source-digest digest
   :request {:task {:target_id target :content_sha256 digest}}
   :analysis valid-analysis :validator-version 1})

(deftest publish-and-read-current
  (let [root (temp-root)
        written (sut/publish! root (publication "M-one" d1))
        read-back (sut/current-reading root "M-one" d1)]
    (is (= :current-candidate (:status written)))
    (is (= written read-back))
    (is (re-matches #"[0-9a-f]{64}" (:analysis-digest read-back)))))

(deftest stale-reading-is-never-returned-as-current
  (let [root (temp-root)
        published (sut/publish! root (publication "M-stale" d1))
        result (sut/current-reading root "M-stale" d2)]
    (is (= :current-candidate (:status published)))
    (is (= {:status :absent :kind :stale-target-reading
            :target-id "M-stale" :expected-digest d2 :found-digests [d1]}
           result))
    (is (nil? (:analysis result)))))

(deftest invalid-analysis-is-refused-without-a-write
  (let [root (temp-root)
        invalid (assoc (publication "M-invalid" d1)
                       :analysis {:status "analyzed" :sentences []})
        result (sut/publish! root invalid)]
    (is (= :refused (:status result)))
    (is (= :invalid-target-reading (:kind result)))
    (is (= :no-current-target-reading
           (:kind (sut/current-reading root "M-invalid" d1))))))

(deftest coverage-classifies-current-stale-and-absent
  (let [root (temp-root)
        _ (sut/publish! root (publication "M-current" d1))
        _ (sut/publish! root (publication "M-stale" d1))
        result (sut/coverage root [{:target-id "M-current" :source-digest d1}
                                   {:target-id "M-stale" :source-digest d2}
                                   {:target-id "M-absent" :source-digest d1}])]
    (is (= {:total 3
            :current 1 :current-ids ["M-current"]
            :stale 1 :stale-ids ["M-stale"]
            :absent 1 :absent-ids ["M-absent"]}
           result))))

(def lab-root "holes/labs/wm-contract/mission-head-cascades-2026-09-30")

(deftest import-seven-current-lab-readings-into-temp-registry
  ;; S7 measured that analyses 02..08 correspond to current substrate targets;
  ;; 01-M-xiang-2000 is not in that set. This imports only into TEMP-ROOT.
  (let [root (temp-root)
        stems ["02-M-metric-harness" "03-M-distributed-proofreaders"
               "04-M-web-arxana-ui-improvements" "05-M-self-documenting-stack"
               "06-M-war-machine-aif-completion" "07-M-essays-diachronic-model"
               "08-M-value-creation-loop"]
        targets
        (mapv (fn [stem]
                (let [request (json/parse-string
                               (slurp (io/file lab-root (str stem ".request.json"))) true)
                      analysis (json/parse-string
                                (slurp (io/file lab-root
                                                (str stem ".request.json.analysis.json"))) true)
                      target (get-in request [:task :target_id])
                      digest (get-in request [:task :content_sha256])
                      result (sut/publish! root
                                           {:target-id target
                                            :source-path (get-in request [:task :file_path])
                                            :source-digest digest :request request
                                            :analysis analysis :validator-version 1})]
                  (is (= :current-candidate (:status result)) stem)
                  {:target-id target :source-digest digest}))
              stems)
        report (sut/coverage root targets)]
    (println "TARGET-READING-LAB-COVERAGE" (pr-str report))
    (is (= 7 (:current report)))
    (is (zero? (:stale report)))
    (is (zero? (:absent report)))))
