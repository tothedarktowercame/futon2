(ns futon2.report.observation-labels-consume-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.observation-labels :as labels]
            [futon2.aif.observation-label-store :as store]
            [futon2.aif.observation-label-reader :as reader]
            [futon2.aif.observation-label-reader-test :as population]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.aif.wm.cascade-decision :as wm-cd])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- observed-check [class locator]
  (let [r (checks/observe {:subject (assoc locator :class class)})]
    (or (get-in r [:results :subject]) (get-in r [:refused :subject]))))

(def ^:dynamic *dir* nil)
(use-fixtures :each
  (fn [f]
    (let [dir (.toFile (Files/createTempDirectory "label-consume-" (make-array FileAttribute 0)))]
      (try (binding [*dir* dir] (f))
           (finally (doseq [file (reverse (file-seq dir))] (io/delete-file file true)))))))
(defn- path [] (str (io/file *dir* "labels.edn")))
(defn- check-path [p]
  (observed-check :C3 {:repo "futon2" :sha population/pin :path p}))
(defn- fill! [n]
  (store/init! (path))
  (store/record! (path) (mapv check-path (concat (take n population/present-paths) population/absent-paths))
                 (labels/loaded-identities) {}))
(defn- assembled []
  (cp/assemble {:targets [fixture/tick-1-target]
                :sources (locfix/locate-all fixture/tick-1-sources)}))
(defn- decision [opts]
  (wm-cd/cascade-decision (assembled) (merge fixture/live-c-opts opts)))
(defn- scoring [lane] (:cascade-scoring (meta (:ranked lane))))
(defn- score [r]
  (pr-str (select-keys (:decision r) [:action :softmax-weights :selection-law])))
(defn- captured [opts after-lane]
  (let [lanes (atom []) real wm-cd/cascade-lane
        result (with-redefs [wm-cd/cascade-lane
                            (fn [p o] (let [lane (real p o)]
                                        (swap! lanes conj lane) (after-lane) lane))]
                 (decision opts))]
    {:result result :lane (first @lanes)}))

(deftest one-real-population-reaches-lanes-measured-a-and-disk
  (fill! 5)
  (let [snapshot (store/snapshot (path))
        {:keys [result lane]} (captured {:observation-labels-path (path)} (fn []))
        d (:decision result) ma (:measured-a d)
        certificate (get-in d [:selection-certificate :observation-labels])
        sc (scoring lane) rates (get-in sc [:precision-model :rates])
        token (first (keys rates))
        saved (#'runner/persist-run-record!
               {:run-record-dir (str (io/file *dir* "records"))
                :scan-render-fn (fn [& _] nil)}
               "offline-label-consume" "2026-09-26T00:00:00Z"
               {:outcome :offline-no-selection
                :checkpoints {:selection {:judgment {:controller-decision d}}}})
        record (edn/read-string (slurp (:run-record saved)))]
    (is (= {:false-neg 1/12 :false-pos 1/12} (get rates token)))
    (is (= {:false-neg {:numerator 0 :denominator 5} :false-pos {:numerator 0 :denominator 5}}
           (get-in sc [:rates-provenance :measurement token])))
    (is (= (pr-str (get rates token)) (pr-str (get-in ma [:rates [fixture/tick-1-target token]]))))
    (is (= :sourced (:status certificate)))
    (is (= (:sha256 snapshot) (:snapshot-sha256 certificate)))
    (is (= {:C3 10} (:subjects certificate)))
    (is (= [] (:excluded certificate)))
    (is (= reader/prior (:prior certificate)))
    (is (= 10 (:labels-count certificate)))
    (is (not (contains? certificate :labels)))
    (is (= (:sha256 snapshot) (get-in record [:decision :selection-certificate :observation-labels :snapshot-sha256])))
    (is (= ma (get-in record [:decision :measured-a])))))

(deftest a-write-after-lane-scoring-does-not-change-the-entry-snapshot
  (fill! 5)
  (let [entry (store/snapshot (path)) reads (atom 0) read-inputs reader/read-rates-inputs
        {:keys [result lane]}
        (with-redefs [reader/read-rates-inputs (fn [p ids] (swap! reads inc) (read-inputs p ids))]
          (captured {:observation-labels-path (path)}
                    #(store/record! (path) [(check-path "src")] (labels/loaded-identities) {})))
        later (reader/read-rates-inputs (path) (labels/loaded-identities))
        ma (get-in result [:decision :measured-a])]
    (is (= 1 @reads))
    (is (not= (:sha256 entry) (:sha256 (store/snapshot (path)))))
    (is (= {:C3 11} (:subjects later)))
    (is (= 6 (count (filter #(= :present (:admitted %)) (:labels later)))))
    (is (= (:sha256 entry) (get-in result [:decision :selection-certificate :observation-labels :snapshot-sha256])))
    (doseq [[token rate] (get-in (scoring lane) [:precision-model :rates])]
      (is (= {:false-neg 1/12 :false-pos 1/12} rate))
      (is (= (pr-str rate) (pr-str (get-in ma [:rates [fixture/tick-1-target token]]))))
      (is (= 5 (get-in ma [:measurement [fixture/tick-1-target token] :false-neg :denominator]))))))

(deftest absent-and-uninitialized-stores-keep-the-baseline-score
  (let [none (decision {}) missing (decision {:observation-labels-path (path)})]
    ;; Captured before this change at 243d3740, over this same real fixture.
    (is (= "0519bdf876aa2c44dab9be8825a5bfb5182f1e59d0fc8a8f3eda75710fefccfc"
           (wm-cd/sha256-hex (score none))))
    (is (= (score none) (score missing)))
    (is (= {:status :absent :reason :no-label-store-configured}
           (select-keys (get-in none [:decision :selection-certificate :observation-labels]) [:status :reason])))
    (is (= :store-uninitialized (get-in missing [:decision :selection-certificate :observation-labels :status])))
    (is (= {:status :absent :reason :no-measured-rates} (get-in missing [:decision :measured-a])))
    (is (not (.exists (io/file (path)))))))

(deftest below-minimum-remains-unmeasured-at-the-lane
  (fill! 4)
  (let [{:keys [result lane]} (captured {:observation-labels-path (path)} (fn []))]
    (is (= [{:class :C3 :excluded :below-minimum :counts {:present 4 :absent 5}}]
           (get-in result [:decision :selection-certificate :observation-labels :excluded])))
    (is (every? #{:absent} (vals (get-in (scoring lane) [:rates-provenance :measurement]))))
    (is (= :absent (get-in result [:decision :measured-a :status])))))

(deftest constructor-and-decision-can-share-an-already-captured-view
  (fill! 5)
  (let [view (reader/read-rates-inputs (path) (labels/loaded-identities))
        p (get-in (assembled) [:problems 0 :cascade-problem])
        lanes (atom []) real wm-cd/cascade-lane
        opts {:observation-labels-view view :constructor-scored-with :same-observation-labels-snapshot}]
    (io/delete-file (path))
    (with-redefs [wm-cd/cascade-lane (fn [p o] (let [r (real p o)] (swap! lanes conj r) r))]
      (is (number? (:value (wm-cd/constructed-candidate-g p {:precedence []} opts))))
      (let [d (:decision (decision opts))]
        (is (= :same-observation-labels-snapshot (get-in d [:selection-certificate :observation-labels :constructor-scored-with])))
        (is (= (:snapshot-sha256 view) (get-in d [:selection-certificate :observation-labels :snapshot-sha256])))))
    (is (= 2 (count @lanes)))
    (is (apply = (map #(get-in (scoring %) [:precision-model :rates]) @lanes)))
    (is (every? #(= {:false-neg 1/12 :false-pos 1/12} %)
                (vals (get-in (scoring (first @lanes)) [:precision-model :rates]))))))
