(ns futon2.aif.wm.recorded-flight-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [futon2.aif.selected-target-reading :as target-reading]
            [futon2.aif.wm.recorded-flight :as sut]))

(def checkpoint
  "/home/joe/code/futon2/data/wm-full-loop-machinery-128/wm-contract-machinery-128-v1/attempt-002/002-selection.edn")
(def answer
  "/home/joe/code/storage/wm-target-reading-requests/b13efa403ece5a5deb42cba77f48ed305fe18f6c356eb7e169a70b07886c4e94/analysis-input-attempt-1.json")

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "recorded-flight-" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn click5-manifest []
  (sut/manifest
   {:source-run-id "2026-10-17-173c5a65-fed9-4bc1-8aa2-b3b4df08dba5"
    :source-click-id "wm-click-0b708330-60fa-4b40-b00d-014340ea471d"
    :author "zai-2"
    :answer-job-id "recorded-click-5-answer"
    :selection-checkpoint checkpoint
    :recorded-answer answer}))

(deftest click5-reaches-honest-recorded-evidence-frontier
  (let [budget (atom {:available 0 :consumed 5})
        before @budget
        result (sut/run-recorded! {:manifest (click5-manifest)
                          :output-root (.getPath (temp-dir))
                          :click-budget budget})]
    (is (= :corrected-answer-missing (:frontier result)))
    (is (= 0 (:selector-calls result)))
    (is (= 0 (:clicks-spent result)))
    (is (= 0 (:ticks-carried result)))
    (is (= before @budget))
    (is (= [:selected-target-reading :selected-target-reading-correction]
           (mapv :kind (:agent-calls result))))
    (is (= :selected-target-reading-publication-refused
           (get-in result [:boundary-record :kind])))
    (is (.isFile (io/file (:ledger-path result))))))

(deftest source-drift-and-manifest-tampering-refuse
  (let [m (click5-manifest)
        root (.getPath (temp-dir))]
    (is (= :recorded-flight-manifest-invalid
           (try
             (sut/run-recorded! {:manifest (assoc m :source-run-id "forged")
                                 :output-root root})
             nil
             (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))
    (is (= :recorded-flight-source-drift
           (try
             (sut/run-recorded!
              {:manifest (sut/seal-manifest
                          (assoc-in m [:recorded-answer :sha256]
                                    (apply str (repeat 64 "0"))))
               :output-root root})
             nil
             (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))

(deftest canonical-cascade-uses-the-bound-selection-certificate
  (let [source (slurp "src/futon2/aif/wm/recorded_flight.clj")]
    (is (re-find #":selected-canonical-cascade" source))
    ;; A tree walk could accept an unrelated nested cascade that was never
    ;; bound by the selector's certificate.
    (is (not (re-find #"tree-seq coll\? seq final-judgement" source)))))

(deftest recorded-correction-continues-without-a-selector
  (let [m (sut/manifest
           {:source-run-id "run" :source-click-id "click" :author "zai-2"
            :selection-checkpoint checkpoint
            :recorded-answers [answer answer]})
        contacted (atom [])
        result
        (with-redefs-fn
          {#'target-reading/repair!
           (fn [opts _]
             (swap! contacted conj ((:answer-fn opts) {:kind :selected-target-reading}))
             (swap! contacted conj ((:answer-fn opts)
                                    {:kind :selected-target-reading-correction}))
             {:status :published :published true :analysis-digest "analysis"})
           #'sut/continue-inner-construction!
           (fn [& _] {:frontier :canonical-cascade-not-retained
                      :outer-receipt-restorations 1})}
          #(sut/run-recorded! {:manifest m :output-root (.getPath (temp-dir))}))]
    (is (= 2 (count @contacted)))
    (is (= 0 (:selector-calls result)))
    (is (= 0 (:clicks-spent result)))
    (is (= :canonical-cascade-not-retained (:frontier result)))))
