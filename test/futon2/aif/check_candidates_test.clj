(ns futon2.aif.check-candidates-test
  "H7b requirement tests (SPEC-flat-removal-and-cascade-decision, section
  H7; D3 amendments p4ng eec2c9f): an unknown fact is a belief, checks are
  candidates, an unknown fact is never a terminal refusal. Tick 1's
  interpretations come from p4ng vm/tick-001/03-R6.edn :computed
  :interpretations."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.check-candidates :as cc]))

(def tick-1-steps
  "The three interpreted steps of 03-R6.edn, token guard shape."
  [{:id :aif/structured-observation-vector
    :guard {:needs #{:summary-without-total-repos-throws}
            :forbids #{:summary-without-total-repos-observes-cleanly}}
    :produces #{:summary-without-total-repos-observes-cleanly}}
   {:id :aif/placeholder-is-load-bearing
    :guard {:needs #{:coupling-density-reads-same-key-with-default
                     :summary-without-total-repos-throws}
            :forbids #{:summary-without-total-repos-observes-cleanly}}
    :produces #{:summary-without-total-repos-observes-cleanly
                :active-repo-ratio-absent-default-is-0}}
   {:id :test-step-covering-missing-total-repos
    :guard {:needs #{:summary-without-total-repos-throws}
            :forbids #{:test-covers-missing-total-repos}}
    :produces #{:test-covers-missing-total-repos}}])

(def tick-1-want
  [:summary-without-total-repos-observes-cleanly
   :active-repo-ratio-absent-default-is-0
   :test-covers-missing-total-repos])

(deftest h7b-check-patterns
  (testing "tick 1: the unknown crash fact gates the fix patterns, so it yields a check"
    (let [{:keys [checks not-gating basis]}
          (cc/check-patterns
           {:facts {:summary-without-total-repos-throws :unknown
                    :active-repo-ratio-absent-default-is-0 true
                    :coupling-density-reads-same-key-with-default true
                    :observe-empty-does-not-throw :unknown}
            :want tick-1-want
            :patterns tick-1-steps
            :check-theta 1})]
      (is (= 1 (count checks))
          "exactly one check: the crash fact gates all three relevant patterns")
      (is (= :check/summary-without-total-repos-throws (:id (first checks))))
      (is (= :check (:kind (first checks))))
      (is (= :summary-without-total-repos-throws (:fact (first checks))))
      (is (= {:unknown #{:summary-without-total-repos-throws}}
             (:guard (first checks))))
      (is (= :summary-without-total-repos-throws (:opens (first checks))))
      (is (= #{} (:produces (first checks)))
          "a check establishes an observation, not an effect")
      (is (= 1 (:theta (first checks))) "declared theta is carried, not defaulted")
      (is (= [:observe-empty-does-not-throw] not-gating)
          "the unknown fact no guard tests is recorded under :not-gating")
      (is (= :observation-channel-H7a (:value-source basis)))))

  (testing "an unknown fact used by no guard yields no check"
    (let [{:keys [checks not-gating]}
          (cc/check-patterns
           {:facts {:orphan-unknown :unknown
                    :summary-without-total-repos-throws true}
            :want tick-1-want
            :patterns tick-1-steps
            :check-theta 1/2})]
      (is (= [] checks))
      (is (= [:orphan-unknown] not-gating))))

  (testing "a known fact never yields a check"
    (let [{:keys [checks]}
          (cc/check-patterns
           {:facts {:summary-without-total-repos-throws false}
            :want tick-1-want
            :patterns tick-1-steps
            :check-theta 1})]
      (is (= [] checks))))

  (testing "missing :check-theta is a typed refusal, never a silent default"
    (let [r (try (cc/check-patterns {:facts {:f :unknown}
                                     :want [:t]
                                     :patterns [{:id :p :guard {:needs #{:f}}
                                                 :produces #{:t}}]})
                 (catch Exception e e))]
      (is (map? (ex-data r)) "refusal carries ex-data")
      (is (= :check-candidates/refusal (:finding (ex-data r))))
      (is (= :check-theta-required (:law (ex-data r))))))

  (testing "chained gating: an unknown fact gating a pattern that only feeds another pattern's guard still yields a check"
    ;; :mid gates the want-producing pattern only through another pattern's
    ;; produces: p1 produces :tok, p2 needs :tok and produces the want token.
    (let [{:keys [checks]}
          (cc/check-patterns
           {:facts {:mid :unknown :root :unknown}
            :want [:want-token]
            :patterns [{:id :p1 :guard {:needs #{:root}} :produces #{:tok}}
                       {:id :p2 :guard {:needs #{:mid :tok}} :produces #{:want-token}}]
            :check-theta 1})]
      (is (= #{:check/mid} (set (map :id checks)))
          ":mid gates p2 (want-relevant through p1's produces); :root gates
              only p1, which gates nothing toward the want")))
)
