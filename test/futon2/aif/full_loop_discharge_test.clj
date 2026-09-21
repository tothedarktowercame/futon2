(ns futon2.aif.full-loop-discharge-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(defn store-fixture []
  (let [docs (atom {})]
    {:docs docs
     :opts {:entity-by-id-fn (fn [id] (when-let [doc (get @docs id)]
                                      {:id id :props doc}))
            :put-doc-fn (fn [doc] (swap! docs assoc (:xt/id doc) doc) {:ok true})}}))

(deftest grounding-two-cohorts-does-not-replace-the-first-discharge
  (let [{:keys [docs opts]} (store-fixture)
        ground (fn [cohort run commit]
                 (runner/ground-commit!
                  "attempt-001" "M-target" "author" "reviewer" "/repo" commit
                  ["src/change.clj"] {} {:job-id "review"}
                  (assoc opts :cohort-id cohort :run-id run)))
        first-result (ground :cohort-69 "run-69" "commit-69")
        first-doc (get @docs (:discharge-id first-result))
        second-result (ground :cohort-70 "run-70" "commit-70")]
    (is (not= (:discharge-id first-result) (:discharge-id second-result)))
    (is (= 2 (count (filter #(= :discharge (:entity/type %)) (vals @docs)))))
    (is (= first-doc (get @docs (:discharge-id first-result))))
    (is (= "full-loop/discharge/cohort/cohort-69/run/run-69/attempt/attempt-001"
           (:xt/id first-doc)))
    (is (= (:discharge-id second-result)
           (:xt/id (get @docs (:discharge-id second-result)))))))


(deftest discharge-identity-is-scoped-and-idempotent
  (let [id #'runner/discharge-id]
    (is (not= (id :cohort-69 "same-run" "attempt-001")
              (id :cohort-70 "same-run" "attempt-001")))
    (is (= (id :cohort-69 "run-69" "attempt-001")
           (id :cohort-69 "run-69" "attempt-001")))
    (is (not= (id :cohort-69 "run-69" "attempt-001")
              (id :cohort-69 "rerun-69" "attempt-001")))
    (is (not= (id :cohort-69 "run-69" "attempt-001")
              (id :cohort-69 "run-69" "attempt-002")))
    (is (not= (id :a/b "c" "attempt-001")
              (id :a "b/c" "attempt-001")))
    (is (not= (id :a/b "c" "attempt-001")
              (id "a%2Fb" "c" "attempt-001")))
    (is (not= (id nil "run" "attempt-001")
              (id :non-cohort "run" "attempt-001")))
    (is (= "full-loop/discharge/run/run/attempt/canary-1"
           (id nil "run" "canary-1")))))

(deftest missing-run-identity-refuses-before-writing
  (let [{:keys [docs opts]} (store-fixture)
        failure (try
                  (runner/ground-commit! "attempt-001" "M-target" "author" "reviewer"
                                        "/repo" "commit" [] {} {} opts)
                  nil
                  (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :discharge-identity-invalid (:failure-kind failure)))
    (is (empty? @docs))))
