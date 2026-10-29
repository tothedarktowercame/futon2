(ns futon2.aif.effective-run-history-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.effective-run-history :as history]
            [futon2.aif.meta-terminal-injury :as terminal-injury]
            [futon2.aif.outer-refusal-history :as outer-history]
            [futon2.aif.wm.authored.req-f0ba279a :as retained-checks])
  (:import [java.math BigInteger]
           [java.security MessageDigest]))

(def prior-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-29-b1a049d7-d4f8-4f63-be69-6872d86e8827.edn")

(def incomplete-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-29-9e2103c1-c48f-496f-b8d5-2421fb7f68f0.edn")

(defn- sha256 [x]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (pr-str x) "UTF-8"))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(def ordered-checks
  (->> retained-checks/clauses
       (map (fn [[occurrence clause]] [occurrence (:completion-check clause)]))
       (sort-by pr-str)
       vec))

(defn- effective-identity [record]
  {:schema :wm/effective-run-identity-v1
   :target (get-in record [:selection-event :target])
   :giraffe-manifest-sha256
   (get-in record [:outer/selection :giraffe/manifest-sha256])
   :policy-id (get-in record [:selection-event :policy-key 2])
   :compiled-check-set-sha256 (sha256 ordered-checks)
   :mission-source-sha256
   "e1b01eb3e0c1cef716174978e63a6b9ac100e968032bb59adbdf31929d805cf0"
   :base-head "cfb1408629826c6d825e2ddc06aad84ee5ac6108"})

(defn- row [record id failure-kind finished-work?]
  {:schema :wm/outer-refusal-index-entry-v1 :status :indexed
   :run-id (:run/id record) :record-path prior-path
   :record-sha256 (apply str (repeat 64 "a"))
   :failure {:kind failure-kind}
   :finished-work? finished-work?
   :effective-run-identity id})

(deftest retained-qualified-close-blocks-equal-next-run
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)
        comparison (history/compare-rows id [(row record id :already-satisfied true)])
        refusal (try (history/require-novel! comparison) nil
                     (catch clojure.lang.ExceptionInfo e e))]
    (is (= :duplicate-effective-run (:status comparison)))
    (is (= (:run/id record) (get-in comparison [:matched-run :run-id])))
    (is (= :outer/duplicate-effective-run (:failure-kind (ex-data refusal))))
    (is (= :effective-run-comparison (:failure-stage (ex-data refusal))))))

(deftest every-identity-component-is-material
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)
        prior (row record id :already-satisfied true)]
    (doseq [k history/required-keys]
      (testing (name k)
        (is (= :no-duplicate
               (:status (history/compare-rows (update id k str "-changed") [prior]))))))
    (is (= :no-duplicate
           (:status (history/compare-rows (assoc id :base-head "changed") [prior]))))))

(deftest irrelevant-world-changes-do-not-authorize-a-repeat
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)
        prior (assoc (row record id :already-satisfied true)
                     :seat-roster :changed :open-tasks :changed :cascade-scores [99])]
    (is (= :duplicate-effective-run
           (:status (history/compare-rows id [prior]))))))

(deftest missing-identity-is-incomparable
  (let [record (edn/read-string (slurp prior-path))
        id (dissoc (effective-identity record) :mission-source-sha256)]
    (is (= :incomparable (:status (history/compare-rows id []))))
    (is (= [:mission-source-sha256]
           (:missing (history/compare-rows id []))))))

(deftest non-finished-priors-do-not-block
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)]
    (doseq [kind [:agent-unavailable :incomplete :guardrail-refusal
                  :already-satisfied]]
      (is (= :no-duplicate
             (:status (history/compare-rows id [(row record id kind false)])))))))

(deftest changed-prior-blocks
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)]
    (is (= :duplicate-effective-run
           (:status (history/compare-rows id [(row record id nil true)]))))))

(deftest existing-terminal-semantics-decide-finished-work
  (let [qualified (edn/read-string (slurp prior-path))
        incomplete (edn/read-string (slurp incomplete-path))]
    (is (true? (terminal-injury/finished-work? qualified)))
    (is (false? (terminal-injury/finished-work? incomplete)))
    (is (true? (:finished-work?
                (outer-history/index-entry qualified prior-path (apply str (repeat 64 "a"))))))
    (is (false? (:finished-work?
                 (outer-history/index-entry incomplete incomplete-path
                                            (apply str (repeat 64 "b"))))))
    (is (true? (terminal-injury/finished-work?
                {:terminal-receipt {:outcome :grounded-change}})))
    (is (false? (terminal-injury/finished-work?
                 {:terminal-receipt {:outcome :already-satisfied}})))))
