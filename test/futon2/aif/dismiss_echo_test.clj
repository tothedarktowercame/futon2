(ns futon2.aif.dismiss-echo-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.tripwire :as tripwire]))

(defn- temp-root []
  (str (.toFile (java.nio.file.Files/createTempDirectory
                 "dismiss-echo-"
                 (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn- write-record! [root child record]
  (let [file (io/file root child (str (:repair/id record) ".edn"))]
    (io/make-parents file)
    (spit file (pr-str record))
    record))

(defn- refusal-data [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(deftest echo-dismissal-is-proof-carrying-and-closing
  (let [root (temp-root)
        source-a (write-record! root "findings"
                                {:repair/id "source-a" :repair/status :open
                                 :attempt-id "source-a-attempt"})
        source-b (write-record! root "findings"
                                {:repair/id "source-b" :repair/status :open
                                 :attempt-id "source-b-attempt"})
        _echo (write-record! root "findings"
                            {:repair/id "echo" :repair/status :open
                             :attempt-id "echo-attempt"
                             :failure-data
                             {:tripwire/witness
                              {:repair-ids [(:repair/id source-a)
                                            (:repair/id source-b)]}}})
        _no-witness (write-record! root "findings"
                                  {:repair/id "executed-repair"
                                   :repair/status :open
                                   :attempt-id "executed-attempt"
                                   :failure-data {:author-job
                                                  {:execution
                                                   {:executed true
                                                    :tool-events 1
                                                    :command-events 1}}}})
        disposition {:authority "Joe/repair-queue/2026-09-19"
                     :reason :disposed-source-echo
                     :actor "codex-24"}
        echo-file (io/file root "findings" "echo.edn")
        before (java.nio.file.Files/readAllBytes (.toPath echo-file))]
    (write-record! root "dismissals"
                   {:repair/id "source-a"
                    :repair/status :dismissed-unexecuted})
    (let [data (refusal-data #(repair/dismiss-echo! root "echo" disposition))]
      (is (= :sources-not-disposed (:repair-dismissal/refusal data)))
      (is (= ["source-b"] (:live-source-ids data))))
    (is (= :no-witness-retained
           (:repair-dismissal/refusal
            (refusal-data #(repair/dismiss-echo! root "executed-repair"
                                                 disposition)))))
    (write-record! root "resolutions"
                   {:repair/id "source-b" :repair/status :resolved})
    (let [dismissal (repair/dismiss-echo! root "echo" disposition)]
      (is (= :dismissed-echo (:repair/status dismissal)))
      (is (= [{:repair/id "source-a"
               :status-at-dismissal :dismissed-unexecuted}
              {:repair/id "source-b" :status-at-dismissal :resolved}]
             (:witness-sources dismissal))))
    (is (empty? (filter #(= "echo" (:repair/id %))
                        (repair/open-obligations root))))
    (is (java.util.Arrays/equals
         before (java.nio.file.Files/readAllBytes (.toPath echo-file))))
    (is (= :already-dismissed
           (:repair-dismissal/refusal
            (refusal-data #(repair/dismiss-echo! root "echo" disposition)))))
    (let [observation (#'tripwire/cross-run-observation
                       {:repair-root root :cohort? true
                        :tripwire/cohort-history []
                        :tripwire/a-matrix-events []
                        :tripwire/grounding-witnesses []}
                       {:phase :opportunity :transition :start})]
      (is (contains? (:closed-repair-ids observation) "echo")))))
