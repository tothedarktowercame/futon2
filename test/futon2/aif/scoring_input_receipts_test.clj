(ns futon2.aif.scoring-input-receipts-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.declaration-reads-test :as runner-fixture]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.cascade-habit-read-test :as store-fixture]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn run-record [path]
  (runner-fixture/with-dir
   (fn [dir]
     (let [reads (atom [])]
       (binding [receipts/*habit-reads* reads]
         (let [assembled (#'fixture/assemble*
                          {:targets [fixture/tick-1-target]
                           :sources fixture/tick-1-sources})
               decision (:decision (#'fixture/decision
                                    assembled (assoc fixture/live-c-opts :cascade-habit-path path)))
               saved (#'runner/persist-run-record!
                      {:run-record-dir (str dir) :habit-reads/state reads
                       :scan-render-fn (fn [& _] nil)}
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
       ;; M-wm-wiring step 8: the read names what selection consumed, the
       ;; enactment fold, here its typed absence (the tick passes none); the
       ;; store written above (record-selection!, 1 sample) is not read
       (is (= {:status :absent :source :enactment-fold :reason :no-enactment-fold}
              (select-keys receipt [:status :source :reason])))
       (is (= 0 (get-in receipt [:state :samples])))
       (is (= 0 (get-in next-receipt [:state :samples])))
       (is (= :enactment-fold (:history (receipts/validate-record second-record))))
       (is (every? #(= :enactment-fold (get-in % [:habit-provenance :source])) candidates))
       (testing "a pre-drop record (E from the legacy store) is history, not invalid"
         (let [pre-drop (update-in second-record [:decision :selection-certificate :candidates]
                                   (fn [cs] (mapv #(assoc-in % [:habit-provenance :source] :cascade-prior) cs)))]
           (is (= {:status :valid :history :pre-drop-store-read}
                  (select-keys (receipts/validate-record pre-drop) [:status :history])))))
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

(defn- snapshot-run-record [fold reads decision]
  (runner-fixture/with-dir
   (fn [dir]
     (let [saved (#'runner/persist-run-record!
                  {:run-record-dir (str dir) :habit-reads/state reads
                   :enactment-fold fold :scan-render-fn (fn [& _] nil)}
                  "habit-snapshot-fixture" "2026-10-22"
                  {:outcome :incomplete
                   :checkpoints {:selection {:judgment {:controller-decision decision}}}})]
       (edn/read-string (slurp (:run-record saved)))))))

(deftest retained-fold-does-not-claim-consumption
  (let [candidate (first (store-fixture/menu))
        fold (assoc ((requiring-resolve 'futon2.aif.cascade-prior/observe-policy)
                     nil (habit/policy-view candidate))
                    :folded-from {:dir "fixture/flights" :read [] :unread []})
        decision {:selection-certificate
                  {:selection-route :wm/selected-outer-cascade-policy-v1}}
        reads (atom [])
        record (snapshot-run-record fold reads decision)
        snapshot (:habit-fold-snapshot record)]
    (is (seq (:counts snapshot)))
    (is (= 1 (:samples snapshot)))
    (is (= fold (edn/read-string (:snapshot-edn snapshot))))
    (is (= {:status :present :consumed? false :consumed-by []
            :ln-e-source :outer-controller-inputs}
           (select-keys snapshot [:status :consumed? :consumed-by :ln-e-source])))
    (is (= [] @reads) "retention does not append a consumption occurrence")
    (is (= :valid (:status (receipts/validate-record record))))
    (doseq [[bad error]
            [[(assoc-in record [:habit-fold-snapshot :consumed?] true)
              :habit-fold-consumption-unwitnessed]
             [(update-in record [:habit-fold-snapshot :counts]
                         #(into {} (map (fn [[k n]] [k (inc n)])) %))
              :habit-fold-snapshot-hash-mismatch]
             [(assoc-in record [:habit-fold-snapshot :sha256] "changed")
              :habit-fold-snapshot-hash-mismatch]
             [(dissoc record :habit-fold-snapshot)
              :habit-fold-snapshot-not-observed]
             [(assoc-in record [:habit-fold-snapshot :ln-e-source] :habit-fold)
              :habit-fold-ln-e-source-not-route]
             [(update record :habit-fold-snapshot dissoc :ln-e-source)
              :habit-fold-ln-e-source-not-route]]]
      (is (= :invalid (:status (receipts/validate-record bad))))
      (is (some #{error} (:errors (receipts/validate-record bad)))))
    (binding [receipts/*habit-reads* reads]
      (habit/attach-state fold [{:action candidate}] :enactment-fold))
    (let [consumed (snapshot-run-record fold reads {:selection-certificate {}})]
      (is (= {:consumed? true :consumed-by [0] :ln-e-source :habit-fold}
             (select-keys (:habit-fold-snapshot consumed)
                          [:consumed? :consumed-by :ln-e-source])))
      (is (= :valid (:status (receipts/validate-record consumed))))
      (is (some #{:habit-fold-consumption-unwitnessed}
                (:errors (receipts/validate-record
                          (assoc-in consumed [:habit-fold-snapshot :consumed?] false)))))
      (is (some #{:habit-fold-consumption-unwitnessed}
                (:errors (receipts/validate-record
                          (assoc-in consumed [:habit-fold-snapshot :consumed-by] [1])))))))
  (let [record (snapshot-run-record nil (atom []) {})]
    (is (= {:schema :wm/habit-fold-snapshot-v1 :status :absent
            :reason :fold-not-passed-to-runner}
           (:habit-fold-snapshot record)))
    (is (= :valid (:status (receipts/validate-record record))))))
