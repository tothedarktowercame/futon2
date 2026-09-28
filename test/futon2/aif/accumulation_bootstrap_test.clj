(ns futon2.aif.accumulation-bootstrap-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.accumulation-bootstrap :as bootstrap]
            [futon2.aif.belief :as belief]
            [futon2.aif.lane-futility :as lane-futility]
            [futon2.aif.observation :as observation]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as wm])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def ^:dynamic *dir* nil)

(use-fixtures
  :each
  (fn [f]
    (let [dir (.toFile (Files/createTempDirectory
                        "accumulation-bootstrap-"
                        (make-array FileAttribute 0)))]
      (try
        (binding [*dir* dir] (f))
        (finally
          (doseq [x (reverse (file-seq dir))]
            (io/delete-file x true)))))))

(def config
  {:schema :wm/accumulation-live-config-v1
   :accumulation-entity-id "entity"
   :accumulation-initialization
   {:authority :declared :prior 1.0 :model/revision "test-v1"}})

(def authority
  {:by "reviewer"
   :authority {:decision :DIRICHLET-BOOTSTRAP-I}
   :supports-authority
   {:observation "futon2.aif.observation/observation-channels"
    :state "futon2.aif.belief/status-set"}})

(defn opts [& [extra]]
  (merge {:trace-dir (str *dir*)
          :config config
          :observation-support observation/observation-channels
          :state-support belief/status-set
          :authority authority}
         extra))

(defn publish! [record]
  (trace/write-trace! record :dir (str *dir*) :date-str "2026-09-28"
                      :return-record? true))

(defn file-bytes [file]
  (when (.isFile (io/file file))
    (Files/readAllBytes (.toPath (io/file file)))))

(deftest stateless-tail-bootstraps-and-next-tick-accumulates
  (publish! {:run/id "tail" :record/kind :fixture})
  (let [path (io/file *dir* "wm-trace-2026-09-28.edn")
        before (file-bytes path)
        result (bootstrap/bootstrap! (opts))
        after (file-bytes path)
        record (:record result)
        history (trace/read-history-strict 2 :dir (str *dir*))
        observation (zipmap observation/observation-channels (repeat 0.5))
        belief-row (zipmap belief/status-set (repeat (/ 1.0 (count belief/status-set))))
        outcome (wm/accumulation-outcome-for-tick
                 {:enabled? true :trace-dir (str *dir*) :tick-id "next"
                  :entity-id "entity" :observation observation
                  :observation-envelope
                  {:channels (update-vals observation
                                          #(hash-map :variant :observed :value %))}
                  :belief-pre {"entity" belief-row}
                  :belief-post {"entity" belief-row}
                  :initialization (:accumulation-initialization config)})]
    (is (= :bootstrapped (:status result)))
    (is (= :accumulation-bootstrap (:record/kind record)))
    (is (= "tail" (get-in record [:bootstrap :predecessor-id])))
    (is (= (:run/id record) (get-in record [:accumulation-state :last-tick])))
    (is (apply distinct? (map :run/id (:records history)))
        "the bootstrap must not share the predecessor's run id")
    (is (java.util.Arrays/equals
         before (java.util.Arrays/copyOfRange after 0 (alength before))))
    (is (= 2 (count (:records history))))
    (is (= 2 (:record-count (lane-futility/indexed-futility-summary (str *dir*)))))
    (is (= :accumulated (get-in outcome [:receipt :status])))
    (is (= (:run/id record) (get-in outcome [:receipt :previous-id])))
    (is (= 1.0 (get-in record [:accumulation-state :concentrations
                               (first (sort observation/observation-channels))
                               (first (sort belief/status-set))])))
    (is (> (get-in outcome [:state :concentrations
                            (first (sort observation/observation-channels))
                            (first (sort belief/status-set))])
           1.0))))

(deftest state-bearing-tail-refuses-without-writing
  (publish! {:run/id "stateful" :accumulation-state {:ok true}})
  (let [path (io/file *dir* "wm-trace-2026-09-28.edn") before (file-bytes path)
        result (bootstrap/bootstrap! (opts))]
    (is (= :bootstrap-not-needed (:reason result)))
    (is (java.util.Arrays/equals before (file-bytes path)))))

(deftest dry-run-returns-record-without-writing
  (publish! {:run/id "tail"})
  (let [path (io/file *dir* "wm-trace-2026-09-28.edn") before (file-bytes path)
        result (bootstrap/bootstrap! (opts {:dry-run? true}))]
    (is (= :dry-run (:status result)))
    (is (= :accumulation-bootstrap (get-in result [:record :record/kind])))
    (is (java.util.Arrays/equals before (file-bytes path)))
    (is (= 1 (:record-count (lane-futility/indexed-futility-summary (str *dir*)))))))

(deftest changed-tail-refuses-under-lock
  (publish! {:run/id "tail"})
  (with-redefs [bootstrap/*before-locked-append*
                #(publish! {:run/id "interloper"})]
    (let [result (bootstrap/bootstrap! (opts))
          records (:records (trace/read-history-strict 10 :dir (str *dir*)))]
      (is (= :bootstrap-stale-predecessor (:reason result)))
      (is (= ["tail" "interloper"] (mapv :run/id records)))
      (is (not-any? #(= :accumulation-bootstrap (:record/kind %)) records))
      (is (= 2 (:record-count (lane-futility/indexed-futility-summary (str *dir*))))))))

(deftest invalid-inputs-refuse
  (publish! {:run/id "tail"})
  (is (= :bootstrap-configuration-invalid
         (:reason (bootstrap/bootstrap! (opts {:config {}})))))
  (is (= :bootstrap-support-empty
         (:reason (bootstrap/bootstrap! (opts {:observation-support []})))))
  (is (= :bootstrap-authority-invalid
         (:reason (bootstrap/bootstrap! (opts {:authority {}}))))))
