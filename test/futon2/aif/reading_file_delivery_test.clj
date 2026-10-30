(ns futon2.aif.reading-file-delivery-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [futon2.aif.selected-target-reading :as reading]
            [futon2.aif.selected-target-reading-test :as existing]
            [futon2.aif.target-reading-registry :as registry]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.pinned-file-delivery :as delivery]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))
(use-fixtures :once fixture/with-hermetic-traces hermetic/with-hermetic-stores)
(def analysis (slurp (io/resource "fixtures/reading-file-delivery/analysis.json")))
(defn run-reading [reply]
  (let [root (#'existing/temp-dir "file-reading-")
        source (io/file root "M-typed-memories.md")
        _ (io/copy (io/file (io/resource "fixtures/reading-file-delivery/M-typed-memories.md")) source)
        calls (atom [])
        result (reading/repair!
                {:run-id "frozen-file" :author "wm-author" :request-root (str root)
                 :target-reading-root (str (io/file root "readings"))
                 :answer-fn (fn [request] (swap! calls conj request)
                              (reply request (count @calls)))}
                (#'existing/judgement "M-typed-memories" (str source)
                                      (registry/excerpt-digest source)))]
    {:result result :calls @calls}))
(defn delivered [request]
  (let [path (:analysis-output-path request)]
    (spit path analysis)
    {:state "done" :job-id "file-reader"
     :result (json/generate-string {:path path :sha256 (identity/sha256 (.getBytes analysis "UTF-8"))})}))
(deftest real-analysis-file-reaches-publication
  (let [{:keys [result calls]} (run-reading (fn [r _] (delivered r)))]
    (is (= :published (:status result)))
    (is (= :valid (get-in result [:validation :status])))
    (is (= 1 (count calls)))
    (is (= "18a083be1e3522768c20ae258fcc2316d42eadaf01ec971d25a8fdd975c99ad2"
           (get-in result [:analysis-source-pin :sha256])))
    (is (re-find #"Reply only with" (reading/prompt (first calls))))))
(deftest missing-and-mismatched-files-get-one-correction
  (doseq [mode [:missing :sha]]
    (let [{:keys [result calls]}
          (run-reading (fn [r n]
                         (if (= 2 n) (delivered r)
                             (do (when (= mode :sha) (spit (:analysis-output-path r) analysis))
                                 {:state "done" :job-id "bad-file"
                                  :result (json/generate-string {:path (:analysis-output-path r)
                                                                 :sha256 "wrong"})}))))]
      (is (= :published (:status result)))
      (is (= 2 (count calls)))
      (is (re-find (if (= mode :sha) #"sha-mismatch" #"file-absent")
                   (get-in calls [1 :format-correction :parser-error]))))))
(deftest exhaustion-is-typed-and-containment-is-enforced
  (let [{:keys [result calls]}
        (run-reading (fn [r _] {:state "done" :job-id "missing"
                                :result (json/generate-string {:path (:analysis-output-path r)
                                                               :sha256 "wrong"})}))]
    (is (= 2 (count calls)))
    (is (= :selected-target-reading-unavailable (:kind result)))
    (is (= :file-absent (get-in result [:detail :file-delivery-error :reason]))))
  (is (nil? (delivery/contained-file "/tmp/request" "../elsewhere.json"))))
(deftest inline-analysis-remains-valid
  (let [{:keys [result calls]} (run-reading (fn [_ _] {:state "done" :job-id "inline" :result analysis}))]
    (is (= :published (:status result)))
    (is (= 1 (count calls)))
    (is (nil? (:analysis-source-pin result)))))
