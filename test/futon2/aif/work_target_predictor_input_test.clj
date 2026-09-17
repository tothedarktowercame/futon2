(ns futon2.aif.work-target-predictor-input-test
  "WM-02 acceptance controls for the predictor-input seam. Every clause of the
   acceptance text is exercised: declared domain, lineage, model identity at
   the input; no invented updates; candidate admission; predecessor lineage;
   cutoff; common model identity; and the no-backdating clause (a store
   established with no committed row refuses, it is never seeded with D)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.aif.work-target-belief :as belief]
            [futon2.aif.work-target-belief-test :as fixtures]
            [futon2.aif.work-target-predictor-input :as pinput]
            [futon2.aif.work-target-tick :as tick]))

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

(defn committed-read [payload]
  {:status :committed
   :head (assoc empty-head :status :committed :seq 1
                :snapshot-sha256 (apply str (repeat 64 "b"))
                :operation/id {:test "previous"})
   :snapshot {:payload payload}})

(defn committed-inputs [target]
  (let [payload (:payload (tick/build-proposal (inputs)))]
    {:activation activated
     :store-read (committed-read payload)
     :registry-ids [(:id fixtures/mission) (:id fixtures/ticket)]
     :target target}))

(defn kind [r] (get-in r [:refusal :kind]))

(deftest committed-row-reaches-input-with-domain-lineage-and-model-identity
  (let [id (:id fixtures/mission)
        r (pinput/predictor-input (committed-inputs id))]
    (is (:ok r) (pr-str r))
    ;; declared domain: the row-7 single-entity belief input over the
    ;; declared state support, with float admission already performed
    (is (= :single-entity (get-in r [:belief-input :mode])))
    (is (= #{id} (set (keys (get-in r [:belief-input :posteriors])))))
    (is (contains? (:numeric-admission r) :ok))
    ;; lineage: D identity, admission, cutoff — carried verbatim, and the
    ;; update state is the store's own :no-admitted-observations, never
    ;; an invented update
    (is (= :no-admitted-observations (get-in r [:lineage :updates])))
    (is (contains? (:lineage r) :D))
    (is (contains? (:lineage r) :admission))
    (is (contains? (:lineage r) :information-cutoff))
    ;; model identity and cutoff: the common model context and the payload
    ;; cutoff, plus the store head the row was carried from
    (is (contains? (:model r) :id))
    (is (= (:timestamp fixtures/t0) (:information-cutoff r)))
    (is (= 1 (:seq (:store-head r))))))

(deftest no-committed-row-refuses-and-never-backdates
  ;; A store with no committed snapshot has NO historical target belief row.
  ;; The seam refuses; it must not seed the row with the declared D.
  (let [r (pinput/predictor-input {:activation activated
                                   :store-read {:status :established-no-snapshots
                                                :head empty-head}
                                   :registry-ids [(:id fixtures/mission)]
                                   :target (:id fixtures/mission)})]
    (is (false? (:ok r)))
    (is (= :no-committed-belief-row (kind r)))))

(deftest unestablished-and-damaged-stores-refuse-typed
  (let [not-established (pinput/predictor-input
                         {:activation {:status :not-activated}
                          :store-read {:status :model-not-established}
                          :registry-ids [] :target "M-x"})
        damaged (pinput/predictor-input
                 {:activation activated
                  :store-read {:status :damaged :reason :missing-head}
                  :registry-ids [] :target "M-x"})]
    (is (= :store-not-established (kind not-established)) (pr-str not-established))
    (is (= :store-not-usable (kind damaged)) (pr-str damaged))))

(deftest target-belief-input-refusals-pass-through-verbatim
  (let [base (committed-inputs (:id fixtures/mission))
        registered [(:id fixtures/mission) (:id fixtures/ticket)]]
    (testing "target outside the registry"
      (is (= :entity-outside-registry
             (kind (pinput/predictor-input (assoc base :target "M-not-there"
                                                  :registry-ids registered))))))
    (testing "registered but never admitted (no belief row)"
      (let [payload (:payload (tick/build-proposal (inputs)))
            tid (:id fixtures/ticket)
            never-admitted (committed-read
                            (-> payload
                                (update :belief dissoc tid)
                                (update :lineage dissoc tid)))]
        (is (= :registered-not-admitted
               (kind (pinput/predictor-input
                      (assoc base :store-read never-admitted
                             :target tid)))))))
    (testing "half-present row (carry damage)"
      (let [payload (:payload (tick/build-proposal (inputs)))
            id (:id fixtures/mission)
            broken (committed-read (update payload :belief dissoc id))]
        (is (= :carry-missing
               (kind (pinput/predictor-input (assoc base :store-read broken)))))))))

(deftest missing-payload-cutoff-refuses-typed
  (let [payload (dissoc (:payload (tick/build-proposal (inputs))) :information-cutoff)
        base (committed-inputs (:id fixtures/mission))]
    (is (= :cutoff-not-declared
           (kind (pinput/predictor-input (assoc base
                                                :store-read (committed-read payload))))))))
