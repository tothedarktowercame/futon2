(ns futon2.aif.interoceptive-activation-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.interoceptive-activation :as activation]
            [futon2.aif.interoceptive-store-lock :as store-lock]))

(def lock-record
  {:path store-lock/default-lock-path :file-key "(dev=1,ino=2)"
   :owner "joe" :parent-owner "root" :parent-writable-by-service? false})

(def process-record
  {:process/id "jvm-1" :pid 101 :start-ticks "202"
   :exe "/usr/bin/java" :cmdline-sha256 (apply str (repeat 64 "a"))
   :coordination/status :participating
   :deployment/id "operator-deployment-1" :loaded-at-ms 900
   :loaded-source-pins activation/required-source-pins
   :writer-entrypoints (vec activation/required-writers)})

(def valid-record
  {:schema :wm/interoceptive-writer-participation-v1
   :receipt-sha256 (apply str (repeat 64 "b"))
   :writer-entrypoints (vec activation/required-writers)
   :source-pins activation/required-source-pins
   :observed-at-ms 1000 :valid-until-ms 2000
   :host {:census-complete? true :boot-id "boot-1"
          :writer-census-sha256 activation/required-writer-census-sha256
          :process-census-sha256 (apply str (repeat 64 "c"))}
   :processes [process-record]
   :lock lock-record})

(defn observable [process]
  (select-keys process [:process/id :pid :start-ticks :exe :cmdline-sha256]))

(defn opts []
  {:now-ms 1500 :source-pins activation/required-source-pins
   :lock-probe identity :process-probe observable})

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest candidate-record-never-authorizes-production
  (is (= :test (:authority-class
                (activation/validate-participation
                 valid-record (assoc (opts) :authority-class :production)))))
  (with-redefs [activation/production-receipt-path "/definitely/missing/receipt.edn"]
    (is (= :interoceptive/activation-receipt-unavailable
           (refusal activation/resolve-production-participation!)))))

(deftest exact-coverage-and-freshness-refusals
  (testing "missing writer and nonparticipating process"
    (is (= :interoceptive/activation-writer-coverage
           (refusal #(activation/validate-participation
                      (update valid-record :writer-entrypoints pop) (opts)))))
    (is (= :interoceptive/activation-nonparticipating-writer
           (refusal #(activation/validate-participation
                      (assoc-in valid-record [:processes 0 :coordination/status]
                                :not-loaded)
                      (opts)))))
    (is (= :interoceptive/activation-process-source-unverified
           (refusal #(activation/validate-participation
                      (update-in valid-record [:processes 0] dissoc :loaded-source-pins)
                      (opts))))))
  (testing "stale, source mismatch, process replacement and lock replacement"
    (is (= :interoceptive/activation-stale
           (refusal #(activation/validate-participation valid-record
                                                        (assoc (opts) :now-ms 3000)))))
    (is (= :interoceptive/activation-source-mismatch
           (refusal #(activation/validate-participation
                      valid-record (assoc (opts) :source-pins {})))))
    (is (= :interoceptive/activation-process-changed
           (refusal #(activation/validate-participation
                      valid-record (assoc (opts) :process-probe
                                          (fn [p] (assoc (observable p)
                                                         :start-ticks "changed")))))))
    (is (= :interoceptive/activation-lock-mismatch
           (refusal #(activation/validate-participation
                      valid-record (assoc (opts) :lock-probe
                                          (fn [lock] (assoc lock :file-key "replaced")))))))))

(deftest process-census-and-lock-contract
  (is (= :interoceptive/activation-process-census-invalid
         (refusal #(activation/validate-participation
                    (assoc valid-record :processes []) (opts)))))
  (is (= :interoceptive/activation-process-writer-coverage
         (refusal #(activation/validate-participation
                    (assoc-in valid-record [:processes 0 :writer-entrypoints] [])
                    (opts)))))
  (is (= :interoceptive/activation-lock-mismatch
         (refusal #(activation/validate-participation
                    (assoc-in valid-record [:lock :parent-writable-by-service?] true)
                    (opts))))))
