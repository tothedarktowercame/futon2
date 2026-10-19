(ns futon2.aif.meta-injury-store-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-injury-observation :as injury]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-live-outer-selector :as selector]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def pin {:path "fixtures/terminal.edn"
          :sha256 "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"})

(defn- failed-record []
  {:run/id "2026-10-19-real-shape" :click/id "wm-click-real-shape"
   :failure {:kind :abstained :stage :selection
             :detail {:kind :wm/selection-terminal-abstention}}
   :terminal-receipt {:kind :failure :failure-kind :abstained}
   :outer-task-selection {:schema :wm/outer-task-selection-v1
                          :status :absent :reason :receipt-not-retained}
   :loop-node-exercise {:schema :wm/loop-node-exercise-v1 :status :incomplete
                        :counts {:present 0 :bypassed 0 :refused 4}}
   :run-output {:schema :wm/run-output-v1 :status :absent
                :reason :run-not-grounded :outcome :abstained}
   :traceWritten false})
(defn- click-authority []
  {:schema :wm/ordinary-click-availability-v1 :unit :ordinary-click
   :allocated 5 :consumed 4 :available 1
   :ledger-source {:path "/tmp/click-ledger"
                   :sha256 (apply str (repeat 64 "a"))}})

(deftest legacy-terminal-observation-route-remains-compatible
  (let [tmp (.toFile (java.nio.file.Files/createTempDirectory
                      "wm-injury" (make-array java.nio.file.attribute.FileAttribute 0)))
        record-path (str tmp "/run.edn")
        _ (spit record-path (str (pr-str (failed-record)) "\n"))
        deposit (store/deposit! (str tmp "/injuries") record-path)
        authority (store/load-authority (str tmp "/injuries") )
        observation (field/observe
                     {:registry-snapshot {:missions {:missions []}
                                          :tickets {:tickets []}
                                          :excursions {:excursions []}}
                      :catalog-path field/default-algorithm-catalog
                      :click-availability (click-authority)
                      :injury-authority authority})
        selected (selector/selector {:tasks [] :meta-field-observation observation
                                     :meta-field-source-pin (:source-pin observation)})]
    (testing "legacy pins remain valid"
      (is (= :active (:status (:observation deposit))))
      (is (= :verified (:status (injury/verify (:observation deposit) authority)))))
    (is (= ["A-self-heal"]
           (mapv :id (filter #(= :algorithm (:kind %)) (:rows observation)))))
    (is (= {:type :run-algorithm :target "A-self-heal"} (:action selected)))))

(deftest legacy-run-record-drift-still-refuses
  (let [tmp (.toFile (java.nio.file.Files/createTempDirectory
                      "wm-injury-tamper" (make-array java.nio.file.attribute.FileAttribute 0)))
        record-path (str tmp "/run.edn") root (str tmp "/injuries")]
    (spit record-path (str (pr-str (failed-record)) "\n"))
    (store/deposit! root record-path)
    (spit record-path (str (pr-str (assoc (failed-record) :traceWritten true)) "\n"))
    (is (= :injury-run-record-drift
           (:reason (injury/produce (store/load-authority root)))))))

(deftest legacy-algorithm-failure-shapes-remain-active
  (doseq [[kind stage] [[:fold-output-invalid :construction]
                        [:carry-identity-unavailable :author-dispatch]]]
    (let [tmp (.toFile (java.nio.file.Files/createTempDirectory
                        "wm-algorithm-injury" (make-array java.nio.file.attribute.FileAttribute 0)))
          record (-> (failed-record)
                     (assoc :failure {:kind kind :stage stage})
                     (assoc :terminal-receipt {:kind :failure :failure-kind kind
                                               :target-kind :algorithm})
                     (assoc :loop-node-exercise
                            {:schema :wm/loop-node-exercise-v1 :status :complete
                             :counts {:present 0 :bypassed 4 :refused 0}}))
          record-path (str tmp "/run.edn")]
      (spit record-path (str (pr-str record) "\n"))
      (let [deposited (store/deposit! (str tmp "/injuries") record-path)]
        (is (= :active (:status deposited)))
        (is (= injury/capability (:capability deposited)))))))

(deftest durable-store-is-atomic-one-form-tamper-evident-and-replay-stable
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "meta-injury-store" (make-array java.nio.file.attribute.FileAttribute 0)))
        path (str (.getPath dir) "/ledger.edn")
        classification
        (terminal/classify {:run/id "run-1" :click/id "click-1"
                            :failure {:kind :phase-failure :stage :reload}}
                           pin)
        first (store/transact-classification! path classification)
        bytes-before (slurp path)
        duplicate (store/transact-classification! path classification)
        bytes-after (slurp path)]
    (is (= :appended (:status first)))
    (is (= :atomic-locked-one-form (:durability first)))
    (is (= :loaded (:status (store/load-ledger path))))
    (is (= :duplicate (:status duplicate)))
    (is (= bytes-before bytes-after))
    (is (= :read (:status (store/read-one-form bytes-after))))
    (is (= :store/multiple-forms
           (:reason (store/read-one-form (str bytes-after "{}")))))
    (spit path (str bytes-after " " "{:tampered true}"))
    (is (= :store/multiple-forms (:reason (store/load-ledger path))))))

(deftest resealed-content-is-required-on-read
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "meta-injury-tamper" (make-array java.nio.file.attribute.FileAttribute 0)))
        path (str (.getPath dir) "/ledger.edn")]
    (spit path "{:schema :wm/meta-injury-ledger-v1 :attempt-limit 3 :entries [] :ledger-pin {:path \"meta-injury-ledger/ledger.edn\" :sha256 \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}}")
    (is (= :store/tampered-ledger (:reason (store/load-ledger path))))))

(deftest record-file-ingestion-pins-exact-one-form-bytes
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "meta-injury-record" (make-array java.nio.file.attribute.FileAttribute 0)))
        ledger-path (str (.getPath dir) "/ledger.edn")
        record-path (str (.getPath dir) "/run.edn")
        record {:run/id "run-file" :click/id "click-file"
                :failure {:kind :author-dispatch-failed :stage :author-dispatch}}]
    (spit record-path (pr-str record))
    (let [result (store/ingest-record-file! ledger-path record-path)]
      (is (= :appended (:status result)))
      (is (= record-path (get-in result [:classification :record-pin :path])))
      (is (= 64 (count (get-in result [:classification :record-pin :sha256])))))
    (spit record-path (str (pr-str record) "\n{}"))
    (is (= :store/multiple-forms
           (:reason (store/ingest-record-file! ledger-path record-path))))))
