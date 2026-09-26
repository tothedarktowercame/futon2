(ns futon2.aif.observation-label-reader-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.observation-labels :as labels]
            [futon2.aif.observation-label-store :as store]
            [futon2.aif.observation-label-reader :as reader]
            [futon2.aif.observation-rates :as rates])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def pin "3fabf0260c056c5bd09755a288cfe179b349e83b")
(def present-paths ["README.md" "deps.edn" "src/futon2/aif/observation_admission.clj"
                    "src/futon2/aif/observation_checks.clj" "src/futon2/aif/observation_rates.clj"])
(def absent-paths (mapv #(str "absent-label-reader-subject-" %) (range 5)))
;; The shipped S-1 contract, also used by observation-rates-test/prod-contract.
(def contract (edn/read-string (slurp (io/resource "wm/observation-contract.edn"))))
(def ^:dynamic *dir* nil)
(use-fixtures :each
  (fn [f]
    (let [dir (.toFile (Files/createTempDirectory "label-reader-" (make-array FileAttribute 0)))]
      (try (binding [*dir* dir] (f))
           (finally (doseq [file (reverse (file-seq dir))] (io/delete-file file true)))))))
(defn- path [] (io/file *dir* "labels.edn"))
(defn- check-path [p] (checks/check-path-exists {:repo "futon2" :sha pin :path p}))
(defn- population [present absent]
  (mapv check-path (concat (take present present-paths) (take absent absent-paths))))
(defn- fill! [present absent]
  (let [ids (labels/loaded-identities)]
    (store/init! (path))
    (let [r (store/record! (path) (population present absent) ids {})]
      (is (= (+ present absent) (:written r)))
      (is (zero? (:refused r))))
    ids))
(defn- source [inputs]
  (rates/sourced-rates (:labels inputs) (:subjects inputs) (:prior inputs)
                      {:target {:class :C3}} contract))

(deftest ten-real-subjects-produce-jeffreys-rates
  (let [ids (fill! 5 5) snapshot (store/snapshot (path))
        inputs (reader/read-rates-inputs (path) ids) sourced (source inputs)]
    (is (= 10 (count (:labels inputs))))
    (is (= {:C3 10} (:subjects inputs)))
    (is (= {:alpha 1/2 :beta 1/2 :authority "A-S §2 (Jeffreys), Revision 3"} (:prior inputs)))
    (is (nil? (#'rates/check-prior (:prior inputs))))
    (is (= [] (:excluded inputs)))
    (is (= 5 (:minimum inputs)))
    (is (= ids (:identities inputs)))
    (is (= (:sha256 snapshot) (:snapshot-sha256 inputs)))
    (is (= (set (vals (get-in snapshot [:envelope :labels]))) (set (:labels inputs))))
    (is (= :sourced (:status sourced)))
    (is (= {:false-neg 1/12 :false-pos 1/12} (get-in sourced [:rates :target])))
    (is (= {:false-neg {:numerator 0 :denominator 5}
            :false-pos {:numerator 0 :denominator 5}} (get-in sourced [:measurement :target])))
    (println "READER-POSITIVE" (pr-str sourced))))

(deftest below-minimum-is-wholly-unmeasured
  (let [ids (fill! 4 5) inputs (reader/read-rates-inputs (path) ids)]
    (is (= [{:class :C3 :excluded :below-minimum :counts {:present 4 :absent 5}}] (:excluded inputs)))
    (is (= [] (:labels inputs)))
    (is (= {} (:subjects inputs)))
    (is (= :sourced (:status (source inputs))))
    (is (= :absent (get-in (source inputs) [:measurement :target])))))

(deftest repeated-subjects-do-not-satisfy-a-missing-cell
  (let [ids (fill! 5 0) receipt (store/record! (path) (population 5 0) ids {})
        inputs (reader/read-rates-inputs (path) ids)]
    (is (= 5 (:skipped receipt) (:labels-total receipt) (:seen-total receipt)))
    (is (= [{:class :C3 :excluded :one-cell-unobserved :counts {:present 5 :absent 0}}] (:excluded inputs)))
    (is (= [] (:labels inputs)))
    (is (= {} (:subjects inputs)))
    (is (every? #(= 2 (:times %)) (vals (get-in (store/snapshot (path)) [:envelope :seen]))))))

(deftest mechanisms-are-separate-populations
  (let [ids (fill! 5 5)
        ;; A suffix collision must not mix distinct identities.
        other (assoc ids :mechanism-sha (str "previous-" (:mechanism-sha ids)))]
    (is (= 10 (:written (store/record! (path) (population 5 5) other {}))))
    (doseq [active [ids other]]
      (let [inputs (reader/read-rates-inputs (path) active)]
        (is (= 10 (count (:labels inputs))))
        (is (= {:C3 10} (:subjects inputs)))
        (is (= [{:class :C3 :excluded :obsolete-mechanism :counts {:labels 10}}] (:excluded inputs)))
        (is (every? #(= (str "C3/cat-file-e@" (:mechanism-sha active)) (nth (:label-key %) 5))
                    (:labels inputs)))))))

(deftest refused-subjects-count-towards-coverage
  (let [ids (fill! 5 5) recompute labels/c3-recompute]
    (with-redefs [labels/c3-recompute (fn [s] (assoc (recompute s) :observer (:check-mechanism s)))]
      (let [receipt (store/record! (path) [(check-path "src")] ids {})]
        (is (= 1 (:refused receipt)))
        (is (= :review-not-concur (get-in receipt [:refusals 0 :kind])))
        (is (= :self-truthed (get-in receipt [:refusals 0 :reason])))))
    (let [inputs (reader/read-rates-inputs (path) ids)]
      (is (= {:C3 11} (:subjects inputs)))
      (is (= 10 (count (:labels inputs))))
      (is (= 10/11 (get-in (rates/rates-by-class (:labels inputs) (:subjects inputs) (:prior inputs)) [:C3 :coverage]))))))

(deftest duplicate-record-keys-refuse-the-snapshot
  (let [ids (fill! 4 5) snapshot (store/snapshot (path))
        label (first (filter #(= :present (:admitted %)) (vals (get-in snapshot [:envelope :labels]))))
        broken (assoc-in snapshot [:envelope :labels :duplicate] label)
        bad (reader/rates-inputs broken ids)]
    (is (= :duplicate-label-key (:kind bad)))
    (is (= [(:label-key label)] (:keys bad)))
    (is (not (contains? bad :labels)))))

(deftest store-failures-do-not-invent-populations
  (let [ids (labels/loaded-identities)]
    (let [bad (reader/read-rates-inputs (path) ids)]
      (is (= :store-uninitialized (:kind bad)))
      (is (not (contains? bad :labels))))
    (spit (path) "{} {}")
    (let [bad (reader/read-rates-inputs (path) ids)]
      (is (= :invalid-label-store (:kind bad)))
      (is (not (contains? bad :labels))))
    (is (= "{} {}" (slurp (path))))))

(deftest a-refused-only-class-is-not-silently-omitted
  (let [ids (fill! 0 0) recompute labels/c3-recompute]
    (with-redefs [labels/c3-recompute (fn [s] (assoc (recompute s) :observer (:check-mechanism s)))]
      (store/record! (path) [(check-path "src")] ids {}))
    (let [snapshot (store/snapshot (path))]
      ;; An already captured snapshot remains usable with its file removed.
      (io/delete-file (path))
      (let [inputs (reader/rates-inputs snapshot ids)]
        (is (= [{:class :C3 :excluded :one-cell-unobserved :counts {:present 0 :absent 0}}]
               (:excluded inputs)))
        (is (= [] (:labels inputs)))
        (is (= {} (:subjects inputs)))))))
