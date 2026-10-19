(ns futon2.aif.meta-injury-store-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def pin {:path "fixtures/terminal.edn"
          :sha256 "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"})

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
