(ns futon2.aif.meta-injury-store-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-injury-observation :as injury]
            [futon2.aif.meta-injury-store :as sut]
            [futon2.aif.meta-live-outer-selector :as selector])
  (:import [java.nio.file Files]))

(defn- failed-record []
  {:run/id "2026-10-19-real-shape"
   :click/id "wm-click-real-shape"
   :failure {:kind :abstained :stage :selection
             :detail {:kind :wm/selection-terminal-abstention}}
   :terminal-receipt {:kind :failure :failure-kind :abstained}
   :outer-task-selection {:schema :wm/outer-task-selection-v1
                          :status :absent :reason :receipt-not-retained}
   :loop-node-exercise {:schema :wm/loop-node-exercise-v1
                        :status :incomplete
                        :counts {:present 0 :bypassed 0 :refused 4}}
   :run-output {:schema :wm/run-output-v1 :status :absent
                :reason :run-not-grounded :outcome :abstained}
   :traceWritten false})

(defn- click-authority []
  {:schema :wm/ordinary-click-availability-v1 :unit :ordinary-click
   :allocated 5 :consumed 4 :available 1
   :ledger-source {:path "/tmp/click-ledger"
                   :sha256 (apply str (repeat 64 "a"))}})

(deftest terminal-injury-becomes-next-meta-algorithm
  (let [tmp (.toFile (Files/createTempDirectory "wm-injury" (make-array java.nio.file.attribute.FileAttribute 0)))
        record-path (str tmp "/run.edn")
        _ (spit record-path (str (pr-str (failed-record)) "\n"))
        deposit (sut/deposit! (str tmp "/injuries") record-path)
        authority (sut/load-authority (str tmp "/injuries"))
        observation (field/observe {:registry-snapshot {:missions {:missions []}
                                                        :tickets {:tickets []}
                                                        :excursions {:excursions []}}
                                    :catalog-path field/default-algorithm-catalog
                                    :click-availability (click-authority)
                                    :injury-authority authority})
        selected (selector/selector {:tasks []
                                     :meta-field-observation observation
                                     :meta-field-source-pin (:source-pin observation)})]
    (testing "the terminal close retained independent bytes and pins"
      (is (= :active (:status (:observation deposit))))
      (is (= :verified (:status (injury/verify (:observation deposit) authority)))))
    (testing "the next META field admits and selects the approved algorithm"
      (is (= ["A-self-heal"]
             (mapv :id (filter #(= :algorithm (:kind %)) (:rows observation)))))
      (is (= {:type :run-algorithm :target "A-self-heal"} (:action selected))))))

(deftest tampered-run-record-cannot-rearm
  (let [tmp (.toFile (Files/createTempDirectory "wm-injury-tamper" (make-array java.nio.file.attribute.FileAttribute 0)))
        record-path (str tmp "/run.edn")
        root (str tmp "/injuries")]
    (spit record-path (str (pr-str (failed-record)) "\n"))
    (sut/deposit! root record-path)
    (spit record-path (str (pr-str (assoc (failed-record) :traceWritten true)) "\n"))
    (is (= :injury-run-record-drift
           (:reason (injury/produce (sut/load-authority root)))))))

(deftest failed-meta-algorithm-remains-an-active-injury
  (let [tmp (.toFile (Files/createTempDirectory "wm-algorithm-injury" (make-array java.nio.file.attribute.FileAttribute 0)))
        record (-> (failed-record)
                   (assoc :failure {:kind :fold-output-invalid :stage :construction})
                   (assoc :terminal-receipt {:kind :failure
                                             :failure-kind :fold-output-invalid
                                             :target-kind :algorithm})
                   (assoc :loop-node-exercise {:schema :wm/loop-node-exercise-v1
                                               :status :complete
                                               :counts {:present 0 :bypassed 4 :refused 0}})
                   (assoc :run-output {:schema :wm/run-output-v1 :status :absent
                                       :reason :run-not-grounded :outcome :incomplete}))
        record-path (str tmp "/run.edn")
        _ (spit record-path (str (pr-str record) "\n"))
        deposited (sut/deposit! (str tmp "/injuries") record-path)]
    (is (= :active (:status deposited)))
    (is (= injury/capability (:capability deposited)))))

(deftest failed-meta-algorithm-author-dispatch-remains-an-active-injury
  (let [tmp (.toFile (Files/createTempDirectory "wm-algorithm-dispatch-injury" (make-array java.nio.file.attribute.FileAttribute 0)))
        record (-> (failed-record)
                   (assoc :failure {:kind :carry-identity-unavailable :stage :author-dispatch})
                   (assoc :terminal-receipt {:kind :failure
                                             :failure-kind :carry-identity-unavailable
                                             :target-kind :algorithm})
                   (assoc :loop-node-exercise {:schema :wm/loop-node-exercise-v1
                                               :status :complete
                                               :counts {:present 0 :bypassed 4 :refused 0}})
                   (assoc :run-output {:schema :wm/run-output-v1 :status :absent
                                       :reason :run-not-grounded :outcome :incomplete}))
        record-path (str tmp "/run.edn")
        _ (spit record-path (str (pr-str record) "\n"))
        deposited (sut/deposit! (str tmp "/injuries") record-path)]
    (is (= :active (:status deposited)))
    (is (= injury/capability (:capability deposited)))))
