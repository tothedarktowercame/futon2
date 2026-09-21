(ns futon2.aif.scoring-input-receipts-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.declaration-reads-test :as runner-fixture]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.cascade-habit-read-test :as store-fixture]
            [futon2.report.war-machine :as wm]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn run-record [path]
  (runner-fixture/with-dir
   (fn [dir]
     (let [reads (atom [])]
       (binding [receipts/*habit-reads* reads]
         (let [assembled (problems/assemble
                          {:targets [fixture/tick-1-target]
                           :sources (locators/locate-all fixture/tick-1-sources)})
               decision (:decision (wm/select-and-record-cascade!
                                    assembled (assoc fixture/live-c-opts :cascade-habit-path path)))
               saved (#'runner/persist-run-record!
                      {:run-record-dir (str dir) :habit-reads/state reads}
                      "scoring-fixture" "2026-09-21"
                      {:outcome :incomplete
                       :checkpoints {:selection {:judgment {:controller-decision decision}}}})]
           (edn/read-string (slurp (:run-record saved)))))))))

(deftest actual-scoring-snapshots-and-initial-origin
  (store-fixture/with-store
   (fn [path]
     (let [_ (habit/record-selection! path {:action (first (store-fixture/menu))})
           first-record (run-record path)
           second-record (run-record path)
           third-record (run-record path)
           candidates (get-in second-record [:decision :selection-certificate :candidates])
           receipt (get-in second-record [:habit-reads :occurrences 1 :receipt])
           next-receipt (get-in third-record [:habit-reads :occurrences 1 :receipt])
           initial (get-in second-record [:decision :initial-belief-receipt])]
       (is (= :valid (:status (receipts/validate-record first-record))))
       (is (= :valid (:status (receipts/validate-record second-record))))
       (is (= [:selection-scoring :joint-selection] (mapv :purpose (get-in second-record [:habit-reads :occurrences]))))
       (is (= 1 (:occurrence-index receipt)))
       (is (= 1 (get-in receipt [:state :samples])))
       (is (= 1 (get-in next-receipt [:state :samples])))
       (is (= (:sha256 receipt) (:sha256 next-receipt)))
       (is (= (:sha256 receipt) (receipts/sha (:snapshot-edn receipt))))
       (doseq [c candidates]
         (is (= (get-in receipt [:state :counts (get-in c [:habit-provenance :policy-key])] 0)
                (get-in c [:habit-provenance :count]))))
       (is (= :assembled-target-facts (:origin initial)))
       (is (= fixture/tick-1-universe (:facts (first (:inputs initial)))))
       (is (= (:value initial) (get-in second-record [:decision :selection-certificate :node-evaluation-traces 0 :evaluations 0 :incoming-belief])))
       (let [stage (get-in second-record [:decision :selection-certificate :token-belief-stage])]
         (is (= :not-wired (:conditioning-status stage)))
         (is (= [] (:observation-updates stage)))
         (is (= (:value initial) (:continuation-belief stage)))
         (is (= (:value initial) (get-in stage [:prospective-carry :belief])))
         (is (= {:token-count 6 :state-count 64 :support-count 1} (:carrier stage))))
       (doseq [bad [(dissoc second-record :habit-reads)
                    (update second-record :decision dissoc :initial-belief-receipt)
                    (assoc-in second-record [:decision :initial-belief-receipt :origin] :unknown)
                    (assoc-in second-record [:decision :selection-certificate :token-belief-stage
                                             :continuation-belief] {#{} 1})
                    (assoc-in second-record [:decision :selection-certificate :token-belief-stage
                                             :prospective-carry :belief] {#{} 1})
                    (assoc-in second-record [:decision :selection-certificate :token-belief-stage
                                             :observation-updates] [{:status :value}])
                    (update-in second-record [:habit-reads] dissoc :occurrences)
                    (assoc-in second-record [:habit-reads :occurrences]
                              [(assoc (get-in second-record [:habit-reads :occurrences 1]) :receipt (assoc next-receipt :sha256 "wrong-snapshot"))])]]
         (is (not= second-record bad))
         (is (= :invalid (:status (receipts/validate-record bad)))))
       (println "SCORING-INPUT-RECEIPTS-RECORD" (pr-str second-record))))))

(deftest never-read-versus-not-retained
  (let [record (runner-fixture/record-run (constantly nil))
        removed (dissoc record :habit-reads)]
    (is (= {:status :absent :reason :never-read :occurrences []} (:habit-reads record)))
    (is (not= record removed))
    (is (= :valid (:status (receipts/validate-record record))))
    (is (= :invalid (:status (receipts/validate-record removed)))))
  (is (= :not-observed (:status (receipts/habit-log nil)))))

(deftest read-used-for-scoring-is-not-the-later-update-read
  (store-fixture/with-store
   (fn [path]
     (let [log (atom [])
           [candidate] (store-fixture/menu)]
       (binding [receipts/*habit-reads* log]
         (habit/attach-habits path [{:action candidate :controller-score 0}])
         ;; An intervening legitimate store update, not a hand edit.
         (habit/record-selection! path {:action candidate})
         (habit/record-selection! path {:action candidate}))
       (is (= [0 0 1] (mapv #(get-in % [:receipt :state :samples]) @log)))
       (is (= [:selection-scoring :selection-update :selection-update] (mapv :purpose @log)))))))
