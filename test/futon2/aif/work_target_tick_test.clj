(ns futon2.aif.work-target-tick-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.aif.work-target-belief :as belief]
            [futon2.aif.work-target-belief-test :as fixtures]
            [futon2.aif.work-target-store :as store]
            [futon2.aif.work-target-store-test :as store-fixtures]
            [futon2.aif.work-target-tick :as tick])
  (:import (java.io IOException)))

;; Production-shaped pinned registry and wrapped candidates come from P1a's
;; captured loader fixtures. No live registry reads. Only test declaration I/O.
(def declaration
  (delay (belief/read-declaration (.getAbsolutePath (io/file belief/declaration-path)))))
(def activated {:status :activated :evidence {:commission "isolated-test-only"}})
(def empty-head
  {:store/id #uuid "0135d955-f662-49a1-8346-a057dc0c6473"
   :genesis-sha256 (apply str (repeat 64 "a")) :status :established-no-snapshots
   :seq 0 :snapshot-sha256 nil :operation/id nil})
(defn inputs []
  {:declaration @declaration :activation activated
   :store-read {:status :established-no-snapshots :head empty-head}
   :registry-snapshot fixtures/snapshot :candidates fixtures/candidates
   :caller {:kind :full-loop-attempt :run/id "run-1" :attempt/id "attempt-1"}
   :tick-context fixtures/t0})
(defn proposal [] (tick/build-proposal (inputs)))
(defn verdict-kind [result] (get-in result [:refusal :kind]))
(defn committed-read [payload]
  {:status :committed
   :head (assoc empty-head :status :committed :seq 1
                :snapshot-sha256 (apply str (repeat 64 "b")) :operation/id {:test "previous"})
   :snapshot {:payload payload}})
(defn encode [x]
  ;; Test the actual store encoding, not a second test-side approximation.
  ((ns-resolve 'futon2.aif.work-target-store 'encode) x))
(defn byte-equal? [a b] (java.util.Arrays/equals ^bytes a ^bytes b))

(deftest production-shaped-proposal-and-determinism
  (let [a (proposal) b (proposal) p (:payload a)
        id (:id fixtures/mission)]
    (is (= :proposal (:mode a)) (pr-str a))
    (is (= a b))
    (is (byte-equal? (encode p) (encode (:payload b))))
    (is (= :ok ((tick/payload-validator @declaration) p)))
    (is (= #{:model-context :belief :lineage :information-cutoff :admissions
             :not-admitted :candidate-population :registry-context} (set (keys p))))
    (is (= 2 (get-in p [:candidate-population :count])))
    (is (= [(:id fixtures/mission) (:id fixtures/ticket)]
           (mapv :id (get-in p [:registry-context :pins]))))
    (is (= (:timestamp fixtures/t0) (:information-cutoff p)))
    (is (= (:read-at fixtures/snapshot) (get-in p [:registry-context :read-at])))
    (is (= #{(:id fixtures/mission) (:id fixtures/ticket)} (set (keys (:row-7-inputs a)))))
    (is (every? :ok (vals (:row-7-inputs a))))
    (is (identical? (get-in p [:belief id])
                    (get-in a [:row-7-inputs id :belief-input :posteriors id])))
    (is (= (get-in p [:lineage id]) (get-in a [:row-7-inputs id :lineage])))
    (is (= 1/7 (get-in p [:belief id :spawned])))))

(deftest validator-rejects-each-invalid-payload
  (let [p (:payload (proposal)) id (:id fixtures/mission)
        validate (tick/payload-validator @declaration)]
    (doseq [[label kind mutate]
            [[:support :posterior-support-mismatch #(update-in % [:belief id] dissoc :spawned)]
             [:negative :invalid-mass #(assoc-in % [:belief id :spawned] -1/7)]
             [:nonfinite :invalid-mass #(assoc-in % [:belief id :spawned] ##Inf)]
             [:nan :invalid-mass #(assoc-in % [:belief id :spawned] ##NaN)]
             [:nonnumeric :invalid-mass #(assoc-in % [:belief id :spawned] "1/7")]
             [:sum :invalid-mass #(assoc-in % [:belief id :spawned] 2/7)]
             [:overflow :invalid-mass #(assoc-in % [:belief id]
                                                (zipmap (keys (get-in p [:belief id]))
                                                        (repeat Long/MAX_VALUE)))]
             [:D :lineage-D-mismatch #(assoc-in % [:lineage id :D :name] "another-D")]
             [:hash :lineage-D-mismatch #(assoc-in % [:lineage id :D :declaration-sha256] "wrong")]
             [:revision :interpretation-revision-mismatch
              #(assoc-in % [:lineage id :interpretation-revision] "v2")]
             [:decision :decision-ref-mismatch #(assoc-in % [:lineage id :decision-ref] "another-bell")]
             [:update :unsupported-update-lineage #(assoc-in % [:lineage id :updates] :observed)]
             [:missing-row :carry-missing #(update % :belief dissoc id)]
             [:missing-lineage :carry-missing #(update % :lineage dissoc id)]
             [:future-cutoff :lineage-cutoff-mismatch
              #(assoc-in % [:lineage id :information-cutoff] (:timestamp fixtures/t1))]
             [:intro-after-cutoff :lineage-cutoff-mismatch
              #(assoc-in % [:lineage id :introduced-at] (:timestamp fixtures/t1))]
             [:invalid-time :lineage-cutoff-mismatch #(assoc-in % [:lineage id :introduced-at] "not-a-time")]
             [:payload-time :invalid-cutoff #(assoc % :information-cutoff "today")]
             [:context :model-context-mismatch #(assoc-in % [:model-context :model :id] "strategic")]
             [:population :candidate-population-pin-missing #(dissoc % :candidate-population)]
             [:population-hash :candidate-population-pin-missing #(update % :candidate-population dissoc :sha256)]
             [:population-count :candidate-population-pin-missing #(assoc-in % [:candidate-population :count] -1)]
             [:registry :registry-pin-missing #(dissoc % :registry-context)]
             [:pin :registry-pin-missing #(update-in % [:registry-context :pins 1] dissoc :sha256)]
             [:read-at :registry-pin-missing #(update % :registry-context dissoc :read-at)]
             [:duplicate-pin :registry-pin-missing #(update-in % [:registry-context :pins] conj
                                                                             (get-in p [:registry-context :pins 0]))]]]
      (testing (name label)
        (is (= kind (verdict-kind (validate (mutate p)))))))
    (testing "instant order, not lexical order: 18:20+01:00 equals 17:20Z"
      (is (= :ok (validate (assoc-in p [:lineage id :introduced-at] "2026-09-15T18:20:00+01:00")))))
    (is (= :declaration-hash-mismatch
           (verdict-kind ((tick/payload-validator (update @declaration :text str "\n")) p))))))

(deftest validator-checks-carried-target-not-read-by-row-seven
  (let [first-payload (:payload (proposal))
        next-proposal (tick/build-proposal
                       (assoc (inputs) :store-read (committed-read first-payload)
                              :candidates [(second fixtures/candidates)] :tick-context fixtures/t1))
        id (:id fixtures/mission)]
    (is (= #{(:id fixtures/ticket)} (set (keys (:row-7-inputs next-proposal)))))
    (is (= :posterior-support-mismatch
           (verdict-kind ((tick/payload-validator @declaration)
                          (update-in (:payload next-proposal) [:belief id] dissoc :spawned)))))))

(deftest predecessor-adapter-complete-table
  (let [p (:payload (proposal))
        statuses [:model-not-established :established-no-snapshots :committed
                  :initialization-incomplete :damaged :pending-recovery :unknown nil]]
    (doseq [status statuses]
      (let [read (assoc (case status
                         :established-no-snapshots {:head empty-head}
                         :committed (committed-read p)
                         {}) :status status :reason {:store "retained-reason"})
            off (tick/predecessor-from-store {:status :not-activated} read)
            on (tick/predecessor-from-store activated read)]
        (if (= :model-not-established status)
          (do (is (= {:mode :inert :reason :model-not-established} off))
              (is (= :stop (:mode on)))
              (is (= :activated-store-missing (get-in on [:failure :kind]))))
          (do (is (= :stop (:mode off)))
              (is (= :activation-mismatch (get-in off [:failure :kind])))))
        (if (#{:committed :established-no-snapshots} status)
          (do (is (= :state-writing (:mode on)))
              (is (= (:head read) (:expected-head on)))
              (is (= (if (= :committed status)
                       {:status :present :state (select-keys p [:belief :lineage :model-context])}
                       {:status :established-no-snapshots}) (:predecessor on))))
          (when-not (= :model-not-established status)
            (is (= :store-not-usable (get-in on [:failure :kind])))))
        (doseq [result [off on] :when (= :stop (:mode result))]
          (is (= (:reason read) (get-in result [:failure :reason]))))))
    (is (= :invalid-activation (get-in (tick/predecessor-from-store {:status :activated}
                                                                 {:status :model-not-established})
                                     [:failure :kind])))
    (doseq [read [{:status :committed :head (:head (committed-read p)) :snapshot {:payload {}}}
                 {:status :established-no-snapshots :head nil}]]
      (is (= :stop (:mode (tick/predecessor-from-store activated read)))))))

(deftest structured-stable-operation-identities
  (let [cutoff (:timestamp fixtures/t0)
        caller {:kind :full-loop-attempt :run/id "a/b" :attempt/id "c"}
        op (tick/operation-identity caller empty-head cutoff)]
    (is (= op (tick/operation-identity caller empty-head cutoff)))
    (is (= (:id op) (:id (tick/operation-identity caller (assoc empty-head :seq 99)
                                                 (:timestamp fixtures/t1)))))
    (is (= {:purpose :work-target :caller :full-loop-attempt
            :occurrence {:run/id "a/b" :attempt/id "c"}
            :store/id (:store/id empty-head) :genesis-sha256 (:genesis-sha256 empty-head)} (:id op)))
    (doseq [other [(assoc caller :run/id "a" :attempt/id "b/c")
                   (assoc caller :attempt/id "d")]]
      (is (not= (:id op) (:id (tick/operation-identity other empty-head cutoff)))))
    (doseq [field [:run/id :attempt/id]]
      (is (= :missing-caller-identity
             (verdict-kind (tick/operation-identity (dissoc caller field) empty-head cutoff)))))
    (doseq [field [:store/id :genesis-sha256]]
      (is (= :missing-store-scope
             (verdict-kind (tick/operation-identity caller (dissoc empty-head field) cutoff)))))
    (is (not= (:id op) (:id (tick/operation-identity caller
                                                   (assoc empty-head :genesis-sha256 (apply str (repeat 64 "c"))) cutoff))))
    (is (= :unknown-caller-kind (verdict-kind (tick/operation-identity {:kind :preview} empty-head cutoff))))
    (is (= :invalid-cutoff (verdict-kind (tick/operation-identity caller empty-head "today"))))
    (let [scheduled (tick/operation-identity {:kind :scheduled-tick :tick-run/id "tick-1"} empty-head cutoff)]
      (is (= {:tick-run/id "tick-1"} (get-in scheduled [:id :occurrence])))
      (is (= :scheduled-tick (:caller-identity-type scheduled)))
      (is (= :missing-caller-identity
             (verdict-kind (tick/operation-identity {:kind :scheduled-tick} empty-head cutoff)))))))

(deftest builder-stops-without-rows
  (let [base (inputs) p (:payload (proposal)) id (:id fixtures/mission)]
    (is (= {:mode :inert :reason :model-not-established}
           (tick/build-proposal {:activation {:status :not-activated}
                                 :store-read {:status :model-not-established}})))
    (doseq [[kind args]
            [[:activated-store-missing (assoc base :store-read {:status :model-not-established})]
             [:activation-mismatch (assoc base :activation {:status :not-activated})]
             [:store-not-usable (assoc base :store-read {:status :pending-recovery :reason :prepared-artifacts})]
             [:registry-unreadable (assoc base :registry-snapshot {:status :unreadable :reason :read-failed})]
             [:registry-pin-missing (update-in base [:registry-snapshot :entries 0] dissoc :sha256)]
             [:declaration-hash-mismatch (update-in base [:declaration :text] str "\n")]
             [:model-context-mismatch (assoc base :store-read
                                            (committed-read (assoc-in p [:model-context :model :id] "wrong")))]
             [:carry-missing (assoc base :store-read (committed-read (update p :lineage dissoc id)))]
             [:invalid-mass (assoc base :store-read (committed-read (assoc-in p [:belief id :spawned] -1)))]
             [:lineage-cutoff-mismatch (assoc base :tick-context {:timestamp "2026-09-15T17:19:00Z"}
                                              :store-read (committed-read p))]
             [:missing-caller-identity (update base :caller dissoc :attempt/id)]
             [:non-edn-candidate (assoc base :candidates [{:type :fire-pattern :object (Object.)}])]]]
      (testing (name kind)
        (let [r (tick/build-proposal args)]
          (is (= :stop (:mode r)) (pr-str r))
          (is (= kind (get-in r [:failure :kind])) (pr-str r))
          (is (not-any? #(contains? r %) [:payload :belief :lineage :row-7-inputs]))
          (when (= kind :carry-missing) (is (= [id] (get-in r [:failure :targets])))))))))

(deftest candidate-coverage-and-canonical-population-pin
  (let [open {:action {:type :open-mission :target "futon4-d/mission/G-wm-wiring"} :G 8.5}
        base (update (inputs) :candidates conj open open)
        result (tick/build-proposal base)
        pin #(get-in (tick/build-proposal %) [:payload :candidate-population])]
    (is (= :proposal (:mode result)))
    (is (= [{:candidate open :reason :not-a-work-target-type :coverage :unresolved-full-policy-coverage}
            {:candidate open :reason :not-a-work-target-type :coverage :unresolved-full-policy-coverage}]
           (get-in result [:payload :not-admitted])))
    (is (= 4 (:count (pin base))))
    (is (not= (pin base) (pin (assoc-in base [:candidates 0 :action :target] "M-other"))))
    (is (not= (pin base) (pin (update base :candidates #(vec (reverse %))))))
    (is (= (pin base) (pin (update base :candidates #(mapv :action %)))))))

(deftest real-store-two-ticks-retry-and-stale-proposal
  (store-fixtures/fixture
   (fn [unvalidated-store]
     (let [validator (tick/payload-validator @declaration)
           s (assoc unvalidated-store :payload-validator validator)
           genesis (assoc (store-fixtures/genesis) :declaration
                          {:path (get-in @declaration [:source :path])
                           :sha256 (get-in @declaration [:source :sha256])
                           :interpretation-revision (get-in @declaration [:declaration :revision])})]
       (is (= :model-not-established (:status (store/read-store s))))
       (is (= :established-no-snapshots (:status (store/initialize! s genesis))))
       (let [initial (store/read-store s)
             first-input (assoc (inputs) :store-read initial :candidates [(first fixtures/candidates)])
             events (atom [])
             first-proposal (binding [store/*event* #(swap! events conj %)]
                              (tick/build-proposal first-input))
             first-commit (store/commit! s (:expected-head first-proposal)
                                         (:operation first-proposal) (:payload first-proposal))
             first-read (store/read-store s)
             second-input (assoc (inputs) :store-read first-read
                                 :caller {:kind :scheduled-tick :tick-run/id "tick-two"}
                                 :candidates [(second fixtures/candidates)] :tick-context fixtures/t1)
             second-proposal (tick/build-proposal second-input)]
         (is (empty? @events) "pure builder performs no store IO")
         (is (= :committed (:status first-commit)))
         (is (= (:payload first-proposal) (get-in first-read [:snapshot :payload])))
         (is (= :proposal (:mode second-proposal)))
         (is (= (get-in first-proposal [:payload :lineage (:id fixtures/mission)])
                (get-in second-proposal [:payload :lineage (:id fixtures/mission)])))
         (is (= (:timestamp fixtures/t1)
                (get-in second-proposal [:payload :lineage (:id fixtures/ticket) :introduced-at])))
         (let [lost (binding [store/*failpoint* #(when (= :response %)
                                                 (throw (IOException. "test lost response")))]
                      (store/commit! s (:expected-head second-proposal)
                                     (:operation second-proposal) (:payload second-proposal)))
               second-read (store/read-store s)
               retry (store/commit! s (:expected-head second-proposal)
                                    (:operation second-proposal) (:payload second-proposal))
               stale (tick/build-proposal (assoc-in first-input [:caller :attempt/id] "stale-attempt"))]
           (is (= :persistence-failed (:status lost)))
           (is (true? (:commit-point-reached? lost)))
           (is (= :committed (:status second-read)))
           (is (= 2 (get-in second-read [:head :seq])))
           (is (= :committed (:status retry)))
           (is (true? (:idempotent? retry)))
           (is (= (:ref second-read) (:ref retry)))
           (is (= :stale-predecessor
                  (:status (store/commit! s (:expected-head stale) (:operation stale) (:payload stale)))))
           (is (= (:ref second-read) (:ref (store/read-store s))))
           (is (= :operation-id-reused
                  (:status (store/commit! s (:head second-read)
                                         (:operation second-proposal) (:payload second-proposal)))))
           (is (= :resolved (:status (store/resolve-reference s (:ref first-commit)))))
           (testing "validator runs over every committed ancestor, not just the tip"
             (let [seen (atom [])
                   observed-store (assoc s :payload-validator
                                         (fn [payload] (swap! seen conj (:information-cutoff payload))
                                           (validator payload)))]
               (is (= :committed (:status (store/read-store observed-store))))
               (is (= [(:timestamp fixtures/t0) (:timestamp fixtures/t1)] @seen))))))))))
