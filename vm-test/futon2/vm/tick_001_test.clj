(ns futon2.vm.tick-001-test
  "Virtual War Machine tick 1, step 1 (R2 structured observation).

  Real-machine REQUIREMENT test (VM-PROTOCOL.md, p4ng 46f7ea7): it asserts
  what the model requires of the real tick's R2 function on this tick's
  problem input — a summary missing :total-repos — and FAILS until the real
  code meets it.

  Requirement source: the absence envelope (observation.clj channel-statuses)
  promises {:variant :absent :reason :source-field-missing :coerced-to 0.0}
  for every absent channel, and observe's docstring says 'an absent source is
  represented as 0.0'. :active-repo-ratio reads (:total-repos summary) at
  observation.clj:129 with no default, so the promise is broken there today
  (NullPointerException); the neighbour :coupling-density at :134 keeps it
  with default 0. See 01-R2.edn and the R2 row of aif-equations.edn.

  Model side: R2 has no aligned counterpart in cascade_model_manifest.clj —
  observation there is token-likelihood (P5 token adjudication), not this
  14-channel vector. No model assertion; recorded in :real-vs-model."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.observation :as obs]))

(defn- observe**
  "Call observe, converting a throw into a test FAILURE carrying the exception
  message rather than an error that aborts the namespace."
  [data]
  (try
    {:ok (obs/observe data)}
    (catch Throwable t
      {:throw t})))

(deftest s01-r2
  (testing "REQUIRED: summary without :total-repos must not throw"
    (let [{:keys [ok throw]} (observe** {:graph {:summary {:active-repos 3}}})]
      (is (nil? throw)
          (str "observe threw: "
               (when throw (class throw)) " "
               (when throw (.getMessage throw))))
      (is (some? ok))))

  (testing "REQUIRED: :active-repo-ratio coerces to 0.0 on the partial summary"
    (let [{:keys [ok throw]} (observe** {:graph {:summary {:active-repos 3}}})]
      (is (nil? throw))
      (when (and (nil? throw) ok)
        (is (= 0.0 (:active-repo-ratio ok))))))

  (testing "REQUIRED: envelope marks the channel absent with :coerced-to 0.0"
    (let [{:keys [ok throw]} (observe** {:graph {:summary {:active-repos 3}}})]
      (is (nil? throw))
      (when (and (nil? throw) ok)
        (let [envelope (obs/observation-envelope ok)
              channel (get-in envelope [:channels :active-repo-ratio])]
          (is (= :futon2.aif/tagged-observation (:type envelope)))
          (is (= :absent (:variant channel)))
          (is (= :source-field-missing (:reason channel)))
          (is (= 0.0 (:coerced-to channel)))))))

  (testing "control: empty scan passes today, :active-repo-ratio is 0.0"
    (let [observation (obs/observe {})]
      (is (map? observation))
      (is (= 0.0 (:active-repo-ratio observation)))))

  (testing "the real tick route-tags observe as :R2"
    ;; war_machine.clj lives on the classpath via the scripts path (deps.edn).
    (let [source (slurp (io/resource "futon2/report/war_machine.clj"))]
      (is (.contains
           source
           "(route-tag route0 :R2 \"futon2.aif.observation/observe\")")))))
