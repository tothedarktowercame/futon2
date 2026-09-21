(ns futon2.aif.close-retention-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.close-retention :as close]))

(defn- refusal [f]
  (try (f) nil
       (catch clojure.lang.ExceptionInfo e (:close-retention/refusal (ex-data e)))))

(defn- occurrence []
  (let [ids (atom ["00000000-0000-4000-8000-000000000001"
                   "00000000-0000-4000-8000-000000000002"])]
    (close/mint-occurrence
     {:run-id "run-1" :cohort-id "cohort-1" :attempt-id "attempt-1"
      :selected-action {:type :repair :target "x"}
      :now (constantly "2026-09-14T10:00:00Z")
      :uuid-fn #(let [id (first @ids)] (swap! ids subvec 1) id)})))

(def absent-state {:status :absent :reason :independent-observation-unavailable})
(def absent-model {:status :absent :reason :declared-model-identity-unthreaded})

(defn- block [overrides]
  (close/build-retention-block
   (merge {:occurrence (occurrence) :state absent-state :model absent-model
           :closed-at "2026-09-14T10:03:00Z"
           :evidence-cutoff "2026-09-14T10:03:00Z"
           :admitted-evidence []}
          overrides)))

(deftest mint-and-typed-absence-happy-path
  (let [o (occurrence) b (block {:occurrence o})]
    (is (= "transition-00000000-0000-4000-8000-000000000001" (:transition/id o)))
    (is (= "action-00000000-0000-4000-8000-000000000002" (:action/id o)))
    (is (= absent-state (:state b)))
    (is (= absent-model (:model b)))
    (is (= (:closed-at b) (:evidence-cutoff b)))))

(deftest state-and-model-coercions-refuse
  (doseq [[method expected]
          [[:selection-belief :selection-belief-as-state]
           [:derived-unique-argmax-of-mu-post :argmax-as-state]
           [:close-disposition :disposition-as-state]]]
    (is (= expected
           (refusal #(block {:state {:status :observed :method method
                                     :state :refined :state-at "2026-09-14T10:02:00Z"
                                     :observed-at "2026-09-14T10:01:00Z"
                                     :evidence/id "e-1"}})))))
  (is (= :model-revision-coercion
         (refusal #(block {:model {:status :present :source :code-sha
                                   :model/id "wm" :model/revision "abc"}})))))

(deftest identifier-and-action-drift-controls
  (let [o (occurrence)]
    (is (= :action-id-coercion
           (refusal #(close/validate-occurrence (assoc o :action/id "wm-click-123")))))
    (is (= :occurrence-action-drift
           (refusal #(close/validate-occurrence
                      (assoc o :action/value {:type :repair :target "y"})))))))

(deftest temporal-and-freeze-controls
  (let [observed {:status :observed :method :independent-categorical-observation
                  :state :refined :state-at "2026-09-14T10:02:00Z"
                  :observed-at "2026-09-14T10:01:00Z" :evidence/id "e-1"}]
    (testing "an independently observed earlier freeze is accepted"
      (is (= :observed (:status (:state (block {:state observed
                                                :admitted-evidence ["e-1"]}))))))
    (is (= :collapsed-evidence-freezes
           (refusal #(block {:state (assoc observed
                                      :observed-at "2026-09-14T10:03:00Z")}))))
    (is (= :evidence-after-cutoff
           (refusal #(block {:state (assoc observed
                                      :observed-at "2026-09-14T10:04:00Z")}))))
    (is (= :cutoff-not-closed-at
           (refusal #(block {:evidence-cutoff "2026-09-14T10:04:00Z"}))))
    (is (= :temporal-order-invalid
           (refusal #(block {:state (assoc observed
                                      :state-at "2026-09-14T09:59:00Z"
                                      :observed-at "2026-09-14T09:58:00Z")}))))))


(deftest occurrence-identity-does-not-inherit-reader-printer
  (let [o (binding [*print-namespace-maps* false]
            (close/mint-occurrence
             {:run-id "test" :cohort-id "test" :attempt-id "test"
              :selected-action {:interpretation-receipts {:apparatus/one-authority {:reading "one"}}}
              :now (constantly "2026-09-21T04:24:00Z")
              :uuid-fn (constantly "00000000-0000-0000-0000-000000000001")}))
        reread (edn/read-string (pr-str o))]
    (is (nil? (binding [*print-namespace-maps* true]
                (refusal #(close/validate-occurrence reread)))))
    (is (= :wm/action-transition-occurrence-v2 (:schema o)))))
