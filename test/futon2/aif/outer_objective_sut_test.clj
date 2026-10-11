(ns futon2.aif.outer-objective-sut-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.meta-live-outer-selector :as live]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.outer-objective-certificate :as objective]
            [futon2.aif.outer-objective-sut :as sut]))

(defonce fixture-root (atom nil))

(defn- write! [path text]
  (.mkdirs (.getParentFile (io/file path)))
  (spit path text))

(defn isolated-store [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "outer-objective-sut"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (io/file root "fixture-repo")]
    (.mkdirs (io/file repo ".git"))
    (write! (io/file repo "holes/missions/M-a.md")
            "# Mission A\nStatus: ACTIVE\n- [ ] one\n")
    (write! (io/file repo "holes/excursions/E-b.md")
            "# Excursion B\nStatus: ACTIVE\n")
    (write! (io/file repo "holes/tickets/T-c.md")
            "# Ticket C\nStatus: OPEN\n- [ ] one\n")
    (reset! fixture-root (.getAbsolutePath root))
    (try (f) (finally (reset! fixture-root nil)))))

(use-fixtures :once isolated-store)

;; Independent SUT authority fixture; never loaded by production configuration.
(def calls (atom []))
(defn fixture-objective [channels]
  (swap! calls conj (mapv :channel/id channels))
  0)
(defn substituted-objective [_] 9)
(def registration
  {:schema :wm/outer-objective-registration-v1
   :objective/identity :sut/stable-tie :objective/version 1
   :registration/token :sut/outer-registration
   :registration/authority 501
   :implementation/pin "test-only:outer-objective-sut:fixture-objective:v1"
   :evaluate fixture-objective})
(def sealed (objective/registry [registration]))
(def public-registration (objective/public-registration registration))

(defn snapshot [a-time]
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source {:path "sut-summary" :sha256 (apply str (repeat 64 "a"))}
   :graph-source {:path "sut-graph" :sha256 (apply str (repeat 64 "b"))}
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:registry {:status :ok}}
           :lineage []
           :clusters [{:mission "M-a"} {:mission "E-b"} {:mission "T-c"}]
           :arrows [] :held [] :patterns {:edges []}
           :tickets {:items [{:stem "M-a" :mtime-ms a-time}
                             {:stem "E-b" :mtime-ms 20}
                             {:stem "T-c" :mtime-ms 30}]}}})

(defn enumerate-isolated []
  (let [root @fixture-root
        load-missions mission-registry/load-missions
        load-excursions mission-registry/load-excursions
        load-tickets mission-registry/load-tickets]
    (with-redefs [mission-registry/load-missions #(load-missions root)
                  mission-registry/load-excursions #(load-excursions root)
                  mission-registry/load-tickets #(load-tickets root)]
      (vec (live/live-registry-tasks)))))

(defn run-sut! [opts]
  (sut/run-controlled
   (merge {:sealed-registry sealed :registration public-registration
           :selector-authority 502 :snapshot (snapshot 10)
           :enumerate-fn enumerate-isolated}
          opts)))

(deftest default-production-shape-refuses-before-enumeration
  (let [called (atom false)
        result (sut/run-controlled {:enumerate-fn #(reset! called true)})]
    (is (= :production-outer-objective-registration-absent (:reason result)))
    (is (false? @called))))

(deftest controlled-entrypoint-preserves-complete-real-enumeration
  (reset! calls [])
  (let [result (run-sut! {})]
    (is (= :selected (:status result)))
    (is (= #{"M-a" "E-b" "T-c"} (:enumerated-open-tasks result)))
    (is (= (:enumerated-open-tasks result)
           (get-in result [:population-correspondence :current])))
    (is (= "E-b" (:selected-task result)) "stable lexical task id breaks tie")
    (is (= 3 (count @calls)) "each complete key is evaluated exactly once")))

(deftest one-changed-dependency-recomputes-only-that-task
  (let [first-run (run-sut! {})
        prior (get-in first-run [:frontier :evaluations])
        old-a (first (filter #(= "M-a" (get-in % [:entry :task/id])) prior))
        changed-id (->> (get-in first-run [:normalized-task-inputs "M-a"
                                           :task/channels])
                        (filter #(= :pipeline-freshness-cost (:channel/id %)))
                        first :channel/value-digest)
        second-run (run-sut! {:snapshot (snapshot 99) :freshness-epoch 1
                              :prior prior
                              :delta {:changed-observations #{changed-id}
                                      :added-tasks #{} :removed-tasks #{}}})
        refresh (get-in second-run [:frontier :refresh])]
    (is (= :selected (:status second-run)))
    (is (= ["M-a"] (mapv #(get-in % [:entry :task/id]) (:invalidated refresh))))
    (is (= ["M-a"] (mapv #(get-in % [:entry :task/id]) (:recomputed refresh))))
    (is (= #{"E-b" "T-c"}
           (set (map #(get-in % [:entry :task/id]) (:retained refresh)))))
    (doseq [retained (:retained refresh)]
      (is (= retained (first (filter #(= (get-in retained [:entry :task/id])
                                         (get-in % [:entry :task/id])) prior)))))
    (is (not= old-a (first (:recomputed refresh))))))

(deftest controlled-integration-falsifiers
  (let [result (run-sut! {})
        frontier (:frontier result)
        evaluations (:evaluations frontier)]
    (testing "omitted enumerated task is a population mismatch"
      (is (= :outer-frontier-certificate-invalid
             (:reason (objective/select-minimum
                       sealed 502 public-registration
                       (assoc frontier :evaluations (pop evaluations)))))))
    (testing "hidden added task has no input and refuses"
      (is (= :outer-refresh-input-invalid
             (:reason (run-sut! {:prior evaluations
                                 :delta {:changed-observations #{}
                                         :added-tasks #{"M-hidden"}
                                         :removed-tasks #{}}})))))
    (testing "stale affected receipt cannot be retained"
      (let [stale (first evaluations)
            changed (first (get-in stale [:entry :dependencies]))
            refresh {:prior evaluations :current evaluations
                     :retained evaluations :invalidated [] :recomputed []
                     :removed #{}
                     :delta {:changed-observations #{changed}
                             :added-tasks #{} :removed-tasks #{}}
                     :current-open-tasks
                     (set (map #(get-in % [:entry :task/id]) evaluations))}]
        (is (some #{:retained-partition-mismatch}
                  (objective/refresh-errors refresh)))))
    (testing "evaluator substitution under the same labels refuses receipt"
      (let [forged (objective/registry [(assoc registration
                                              :evaluate substituted-objective)])]
        (is (not (objective/evaluation-valid? forged 502 public-registration
                                              (first evaluations))))))))

(deftest tie-selection-is-stable-under-enumeration-order
  (let [forward (run-sut! {})
        reversed (sut/run-controlled
                  {:sealed-registry sealed :registration public-registration
                   :selector-authority 502 :snapshot (snapshot 10)
                   :enumerate-fn #(vec (reverse (enumerate-isolated)))})]
    (is (= "E-b" (:selected-task forward)))
    (is (= (:selected-task forward) (:selected-task reversed)))))
