(ns futon2.aif.target-reading-registry-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.target-reading-registry :as sut]))

(def d1 (apply str (repeat 64 "1")))
(def d2 (apply str (repeat 64 "2")))
(def template-head
  "## HEAD\n\n*The mission's live operator-shape, captured before IDENTIFY hardens it into a tractable gap statement.*")

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
   :excerpt-digest digest :source-file-digest d2
   :request {:task {:target_id target :content_sha256 d2 :excerpt_sha256 digest}}
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

(deftest request-excerpt-mismatch-is-refused
  (let [root (temp-root)
        result (sut/publish! root (assoc (publication "M-mismatch" d1)
                                        :excerpt-digest d2))]
    (is (= :refused (:status result)))
    (is (some #{:request-excerpt-digest} (:invalid-fields result)))
    (is (= :no-current-target-reading
           (:kind (sut/current-reading root "M-mismatch" d2))))))

(deftest coverage-classifies-current-stale-and-absent
  (let [root (temp-root)
        _ (sut/publish! root (publication "M-current" d1))
        _ (sut/publish! root (publication "M-stale" d1))
        result (sut/coverage root [{:target-id "M-current" :excerpt-digest d1}
                                   {:target-id "M-stale" :excerpt-digest d2}
                                   {:target-id "M-absent" :excerpt-digest d1}])]
    (is (= {:total 3
            :current 1 :current-ids ["M-current"]
            :stale 1 :stale-ids ["M-stale"]
            :absent 1 :absent-ids ["M-absent"]}
           result))))

(defn text-digest [text]
  (identity/sha256 (.getBytes text "UTF-8")))

(deftest python-and-clojure-excerpt-digests-agree
  (let [dir (temp-root)
        cases {"head.md" "# M\nBefore.\n\n## HEAD\nJoe says this.\n"
               "opening.md" "# M\nJoe opens this mission.\n\n## MAP\nLater.\n"
               "head-sections.md" "# M\n\n## HEAD\nFirst.\nSecond.\n\n## MAP\nChanged elsewhere.\n"}]
    (doseq [[name text] cases]
      (let [file (io/file dir name) out (io/file dir (str name ".request.json"))
            _ (spit file text)
            run (shell/sh "python3" "scripts/wm_task_reading.py" (.getPath file)
                          "--mission-head" "--out" (.getPath out))
            request (json/parse-string (slurp out) true)]
        (is (zero? (:exit run)) (:err run))
        (is (= (get-in request [:task :excerpt_sha256])
               (sut/excerpt-digest file)))))))

(deftest template-only-head-is-not-a-reading-source
  (let [dir (temp-root) file (io/file dir "template.md") out (io/file dir "request.json")]
    (spit file (str "# Mission\n\n" template-head "\n\n## IDENTIFY\nLater.\n"))
    (let [run (shell/sh "python3" "scripts/wm_task_reading.py" (.getPath file)
                        "--mission-head" "--out" (.getPath out))]
      (is (not (zero? (:exit run))))
      (is (re-find #"head-is-template-only" (:err run)))
      (is (not (.exists out))))
    (is (= :target-head-template-only (:kind (sut/excerpt-digest file))))))

(deftest template-line-with-real-head-content-remains-readable
  (let [dir (temp-root) file (io/file dir "substantive.md") out (io/file dir "request.json")
        text (str "# Mission\n\n" template-head "\nJoe wants the actual system repaired.\n")]
    (spit file text)
    (let [run (shell/sh "python3" "scripts/wm_task_reading.py" (.getPath file)
                        "--mission-head" "--out" (.getPath out))
          request (json/parse-string (slurp out) true)]
      (is (zero? (:exit run)) (:err run))
      (is (= (str/trimr (subs text (.indexOf text "## HEAD"))) (:source_text request)))
      (is (string? (sut/excerpt-digest file))))))

(deftest publish-refuses-template-only-source-text
  (let [root (temp-root)
        result (sut/publish! root (assoc (publication "M-template" d1)
                                         :request {:source_text template-head
                                                   :task {:target_id "M-template"
                                                          :excerpt_sha256 d1}}))]
    (is (= :refused (:status result)))
    (is (= :invalid-target-reading (:kind result)))
    (is (some #{:source-text-template-only} (:invalid-fields result)))))

(deftest edits-outside-head-do-not-stale-the-reading
  (let [root (temp-root) file (io/file root "M-head.md")
        before "# M\nBefore metadata.\n\n## HEAD\nJoe's unchanged words.\n\n## MAP\nOld map.\n"
        after "# M\nChanged metadata.\n\n## HEAD\nJoe's unchanged words.\n\n## MAP\nA completely new map.\n"]
    (spit file before)
    (let [digest (sut/excerpt-digest file)
          record (assoc (publication "M-head" digest)
                        :source-path (.getPath file)
                        :source-file-digest (sut/source-file-digest file))]
      (is (= :current-candidate (:status (sut/publish! root record))))
      (spit file after)
      (is (= digest (sut/excerpt-digest file)))
      (is (= :current-candidate
             (:status (sut/current-reading root "M-head" (sut/excerpt-digest file))))))))

(deftest edit-inside-head-stales-the-reading
  (let [root (temp-root) file (io/file root "M-head.md")]
    (spit file "# M\n\n## HEAD\nJoe's words.\n\n## MAP\nMap.\n")
    (let [digest (sut/excerpt-digest file)
          record (assoc (publication "M-head" digest) :source-path (.getPath file))]
      (sut/publish! root record)
      (spit file "# M\n\n## HEAD\nJoe's wordX.\n\n## MAP\nMap.\n")
      (is (= :stale-target-reading
             (:kind (sut/current-reading root "M-head" (sut/excerpt-digest file))))))))

(def lab-root "holes/labs/wm-contract/mission-head-cascades-2026-09-30")

(deftest import-six-current-lab-readings-and-refuse-template-head
  ;; S7 measured that analyses 02..08 correspond to current substrate targets;
  ;; 01-M-xiang-2000 is not in that set. This imports only into TEMP-ROOT.
  (let [root (temp-root)
        stems ["02-M-metric-harness" "03-M-distributed-proofreaders"
               "04-M-web-arxana-ui-improvements" "05-M-self-documenting-stack"
               "06-M-war-machine-aif-completion" "07-M-essays-diachronic-model"
               "08-M-value-creation-loop"]
        rows
        (mapv (fn [stem]
                (let [request (json/parse-string
                               (slurp (io/file lab-root (str stem ".request.json"))) true)
                      analysis (json/parse-string
                                (slurp (io/file lab-root
                                                (str stem ".request.json.analysis.json"))) true)
                      target (get-in request [:task :target_id])
                      digest (text-digest (:source_text request))
                      current-digest (sut/excerpt-digest (get-in request [:task :file_path]))
                      result (sut/publish! root
                                           {:target-id target
                                            :source-path (get-in request [:task :file_path])
                                            :excerpt-digest digest
                                            :source-file-digest (get-in request [:task :content_sha256])
                                            :request request
                                            :analysis analysis :validator-version 1})]
                  {:stem stem :result result :target-id target
                   :excerpt-digest current-digest}))
              stems)
        accepted (filterv #(= :current-candidate (get-in % [:result :status])) rows)
        refused (filterv #(= :refused (get-in % [:result :status])) rows)
        report (sut/coverage root (mapv #(select-keys % [:target-id :excerpt-digest]) accepted))]
    (println "TARGET-READING-LAB-COVERAGE" (pr-str report))
    (is (= 6 (:current report)))
    (is (= ["04-M-web-arxana-ui-improvements"] (mapv :stem refused)))
    (is (some #{:source-text-template-only}
              (get-in refused [0 :result :invalid-fields])))))
