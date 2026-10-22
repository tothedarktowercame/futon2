(ns futon2.aif.occurrence-declared-effect-binding-test
  "Focused tests for the pure binding receipt v1. The declared-effect record
  and its verification are TEST FIXTURES modeled on the C2 producer's frozen
  shape (declared-effect-record-result.json sample): the accepted corrected
  producer (zai-2 job2384) was NOT available when this was written, so the
  real-producer integration positive remains an explicit open dependency —
  these tests prove the binding's own joins, digests, and refusals only."
  (:require [clojure.test :refer [are deftest is testing]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.occurrence-declared-effect-binding :as binding]))

(def target "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a19c2ade9b1c281d999c567cf22a2ade")
(def recorded-at "2026-10-22T08:00:00Z")
(def bound-at "2026-10-22T08:00:01Z")

(defn mint-occurrence [action]
  (retention/mint-occurrence
   {:run-id "run-1" :cohort-id "cohort-1" :attempt-id "attempt-1"
    :selected-action action
    :now (constantly (java.time.Instant/parse recorded-at))
    :uuid-fn (fn [] (java.util.UUID/fromString
                     "00000000-0000-0000-0000-000000000001"))}))

(def selected-action {:kind :cascade-candidate :id :C2 :target target
                      :precedence [{:id :p :guard {:status :interpreted}
                                    :produces #{[target :restoration-accepted]}}]})
(def occurrence (mint-occurrence selected-action))

(defn digest-record [record]
  (identity/sha256
   (identity/printed false
                     [:wm/selected-effect-declaration-v1
                      (identity/canonical (dissoc record :record/digest))])))

(defn record-fixture
  "Faithful to the C2 frozen shape; digest computed with the documented
  scheme. This is a fixture, not the producer."
  [& [overrides]]
  (let [base {:schema :wm/selected-effect-declaration-v1
              :target target
              :declaration {:status :pinned
                            :path "resources/wm/cascade-sources/T-repair-occ-444fb018.edn"
                            :sha256 (apply str (repeat 64 "a"))}
              :base-revision {:repo "futon2"
                              :sha "0798f96ad2082a8f80c8a850c8e1138c036213f9"
                              :resolved-sha "0798f96ad2082a8f80c8a850c8e1138c036213f9"}
              :horizon {:status :declared :value 4 :authority :declaration/horizon-steps}
              :admission {:eligible true :refusals []}}
        record (merge base overrides)]
    (assoc record :record/digest (digest-record record))))

(defn verification [record]
  (fn [_] {:verified true :digest (:record/digest record)}))

(defn inputs
  ([record] (inputs record occurrence))
  ([record occ]
   {:occurrence occ :selected-mission target :record record
    :record-verifier (verification record)
    :recorded-at recorded-at :bound-at bound-at}))

(defn rebuild [record]
  (binding/build-binding (inputs record)))

(defn refuse-reason [step-fn]
  (try
    (step-fn)
    ::no-refusal
    (catch clojure.lang.ExceptionInfo e
      (or (get-in (ex-data e) [:declared-effect-binding/refusal])
          (get-in (ex-data e) [:close-retention/refusal])))))

(deftest positive-build-and-verify
  (let [record (record-fixture)
        b (binding/build-binding (inputs record))
        v (binding/verify-binding
           {:selected-mission target :occurrence occurrence :binding b :record record
            :record-verifier (verification record)})]
    (is (= binding/binding-keys (set (keys b))))
    (is (= :verified (:status v)))
    (is (= (:binding/digest b) (:binding/digest v)))
    ;; domain separation: occurrence digest is not the action identity digest
    (is (not= (binding/occurrence-digest occurrence)
              (identity/digest (:action/value occurrence))))
    (is (= (binding/occurrence-key occurrence)
           {:action/id (:action/id occurrence)
            :action/value-sha256 (:action/value-sha256 occurrence)
            :transition/id (:transition/id occurrence)}))
    (is (= target (:target b)))
    (is (= (select-keys (:declaration record) [:path :sha256])
           (:declared-effect/source b)))
    (is (= (select-keys (:base-revision record) [:repo :sha :resolved-sha])
           (:base-revision b)))))

(deftest input-and-shape-refusals
  (are [expected tampered] (= expected (refuse-reason #(binding/build-binding tampered)))
    :shape-invalid (assoc (inputs (record-fixture)) :extra 1)
    :shape-invalid (dissoc (inputs (record-fixture)) :bound-at)
    :occurrence-v2-required (assoc-in (inputs (record-fixture))
                                      [:occurrence :schema]
                                      retention/legacy-occurrence-schema)
    :occurrence-action-drift
    (assoc (inputs (record-fixture))
           :occurrence (assoc occurrence :action/value-sha256
                              (apply str (repeat 64 "0"))))
    :record-declaration-not-pinned
    (inputs (record-fixture {:declaration {:status :absent}})))
  (is (= :timestamp-invalid
         (refuse-reason #(binding/build-binding
                          (assoc (inputs (record-fixture)) :bound-at "not-a-time")))))
  (is (= :record-schema-mismatch
         (refuse-reason #(binding/build-binding
                          (inputs (assoc (record-fixture) :schema :wm/forged-v1)))))))

(deftest no-status-only-trust-record-authority-is-injected
  (let [eligible (record-fixture)]
    (testing "eligible record with NO verification refuses"
      (is (= :record-verification-missing
             (refuse-reason #(binding/build-binding
                              (assoc (inputs eligible) :record-verifier nil))))))
    (testing "producer refusal propagates (f90-style forged record rejected)"
      (is (= :record-verification-refused
             (refuse-reason #(binding/build-binding
                              (assoc (inputs (record-fixture))
                                     :record-verifier
                                     (fn [_] {:verified false :reason :forged-record})))))))
    (testing "verification digest for a DIFFERENT record refuses"
      (let [other (record-fixture {:base-revision {:sha "97e17e10f2695c481c55ddaebe9026f2d245663f"
                                                   :resolved-sha "97e17e10f2695c481c55ddaebe9026f2d245663f"}})]
        (is (= :record-verification-digest-mismatch
               (refuse-reason #(binding/build-binding
                                (assoc (inputs eligible)
                                       :record-verifier (verification other))))))))
    (testing "ineligible admission never binds"
      (is (= :record-not-eligible
             (refuse-reason #(binding/build-binding
                              (inputs (record-fixture
                                       {:admission {:eligible false
                                                    :refusals [:no-open-declared-want]}})))))))))

(deftest occurrence-tamper-negatives
  (let [record (record-fixture)
        b (rebuild record)
        v (fn [occ]
            (binding/verify-binding
             {:selected-mission target :occurrence occ :binding b :record record
              :record-verifier (verification record)}))]
    (testing "one occurrence key changed (:action-at) -> digest mismatch"
      (is (= :occurrence-digest-mismatch
             (:reason (v (assoc occurrence :action-at "2026-10-22T09:00:00Z"))))))
    (testing "key triple changed AND self-hashed -> key mismatch"
      (let [forged (assoc b :occurrence/key
                          {:action/id "action-other"
                           :action/value-sha256 (:action/value-sha256 occurrence)
                           :transition/id (:transition/id occurrence)})
            self-hashed (assoc forged :binding/digest (binding/binding-digest forged))]
        (is (= :occurrence-key-mismatch
               (:reason (binding/verify-binding
                         {:selected-mission target :occurrence occurrence :binding self-hashed :record record
                          :record-verifier (verification record)}))))))))

(deftest target-source-base-mismatch-negatives
  (let [record (record-fixture)
        b (rebuild record)
        other-target (str target "0")
        v (fn [binding*]
            (binding/verify-binding
             {:selected-mission target :occurrence occurrence :binding binding* :record record
              :record-verifier (verification record)}))
        rehash (fn [tampered]
                 (assoc tampered :binding/digest (binding/binding-digest tampered)))]
    (testing "binding retargeted to a DIFFERENT target's record (self-hashed)"
      (let [forged (assoc b :target other-target)
            self-hashed (assoc forged :binding/digest (binding/binding-digest forged))]
        (is (= :target-mismatch (:reason (v self-hashed))))))
    (testing "source pin changed, digest not recomputed"
      (is (= :binding-digest-mismatch
             (:reason (v (assoc-in b [:declared-effect/source :sha256]
                                   (apply str (repeat 64 "b"))))))))
    (testing "source pin changed AND self-hashed -> declaration-source-mismatch"
      (is (= :declaration-source-mismatch
             (:reason (v (rehash (assoc-in b [:declared-effect/source :sha256]
                                           (apply str (repeat 64 "b")))))))))
    (testing "base revision changed AND self-hashed -> base-revision-mismatch"
      (is (= :base-revision-mismatch
             (:reason (v (rehash (assoc-in b [:base-revision :resolved-sha]
                                           "97e17e10f2695c481c55ddaebe9026f2d245663f"))))))))
  (testing "record digest swapped AND self-hashed -> record-digest-mismatch"
    (let [record (record-fixture)
          b (rebuild record)
          other (record-fixture {:horizon {:status :declared :value 2
                                           :authority :declaration/horizon-steps}})
          forged (assoc b :declared-effect/digest (:record/digest other))
          self-hashed (assoc forged :binding/digest (binding/binding-digest forged))]
      (is (= :record-digest-mismatch
             (:reason (binding/verify-binding
                       {:selected-mission target :occurrence occurrence :binding self-hashed :record record
                        :record-verifier (verification record)})))))))

(deftest binding-shape-and-schema-refusals
  (let [record (record-fixture)
        b (rebuild record)
        v (fn [binding*]
            (binding/verify-binding
             {:selected-mission target :occurrence occurrence :binding binding* :record record
              :record-verifier (verification record)}))]
    (is (= :verify-input-invalid
           (:reason (binding/verify-binding
                     {:selected-mission target :occurrence occurrence :binding b :record record}))))
    (is (= :binding-shape-invalid (:reason (v (dissoc b :bound-at)))))
    (is (= :binding-schema-mismatch
           (:reason (v (assoc b :schema :wm/forged-binding-v1)))))
    (is (= :binding-digest-mismatch
           (:reason (v (assoc b :binding/digest (apply str (repeat 64 "0")))))))
    (is (= :timestamp-invalid
           (:reason (v (let [forged (assoc b :bound-at "yesterday")]
                         (assoc forged :binding/digest
                                (binding/binding-digest forged)))))))))

(deftest occurrence-schema-untouched
  ;; the v2 occurrence minted by close-retention passes through unchanged
  (let [record (record-fixture)
        b (rebuild record)]
    (is (= retention/occurrence-schema (:schema occurrence)))
    (is (= 9 (count (keys occurrence))))
    (is (= (:action/value-sha256 occurrence)
           (identity/digest selected-action)))
    (is (= {:action/id (:action/id occurrence)
            :action/value-sha256 (:action/value-sha256 occurrence)
            :transition/id (:transition/id occurrence)}
           (:occurrence/key b)))))

(deftest current-body-and-selection-target-are-rechecked
  (let [r (record-fixture)
        b (rebuild r)
        calls (atom [])
        verifier (fn [current]
                   (swap! calls conj current)
                   {:verified true :digest (:record/digest current)})
        input {:occurrence occurrence :binding b :record r
               :selected-mission target :record-verifier verifier}]
    (is (= :verified (:status (binding/verify-binding input))))
    (is (= [r] @calls))
    (is (= :target-mismatch
           (:reason (binding/verify-binding (assoc input :selected-mission "other")))))
    (is (= :record-verification-refused
           (:reason (binding/verify-binding
                     (assoc input :record-verifier {:verified true :digest (:record/digest r)})))))
    (is (= :record-verification-refused
           (:reason (binding/verify-binding
                     (assoc input :record (assoc r :horizon {:status :absent}))))))
    (is (= 2 (count @calls)) "stale body and verdict map never invoke authority")
    (is (= :record-body-digest-mismatch
           (refuse-reason #(binding/build-binding
                            (assoc (inputs r) :record (assoc r :horizon {:status :absent}))))))))
