(ns futon2.aif.cross-ledger-identity-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cross-ledger-identity :as identity]))

(def close
  {:cohort/id :cohort/a :attempt/id "attempt-001"
   :payload {:judgment
             {:entity-state-at-close
              {:belief-source {:run/id "run/right"}}}}})

(defn refusal [close-record traces]
  (try (identity/join-close-to-trace close-record traces) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest joins-only-by-literal-run-identity
  (let [trace {:run/id "run/right"
               :cohort-attempt {:cohort/id :cohort/a :attempt/id "attempt-001"}}
        historical {:run/id "run/old"
                    :cohort-attempt {:cohort/id :cohort/a :attempt/id "attempt-001"}}]
    (is (= trace (:trace (identity/join-close-to-trace close [historical trace]))))))

(deftest refuses-missing-and-nonunique-run-identities
  (is (= :missing-close-run-identity (refusal {} [])))
  (is (= :missing-trace-run-identity (refusal close [])))
  (is (= :multiple-trace-run-matches
         (refusal close [{:run/id "run/right"} {:run/id "run/right"}]))))

(deftest refuses-reused-attempt-id-as-a-fallback-join
  (let [unidentified (dissoc close :payload)
        duplicate [{:run/id "run/one"
                    :cohort-attempt {:cohort/id :cohort/a :attempt/id "attempt-001"}}
                   {:run/id "run/two"
                    :cohort-attempt {:cohort/id :cohort/a :attempt/id "attempt-001"}}]]
    (is (= :historical-attempt-id-collision
           (refusal unidentified duplicate)))))
