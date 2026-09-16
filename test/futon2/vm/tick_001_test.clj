(ns futon2.vm.tick-001-test
  "Virtual War Machine tick 1, step 1 (R2 structured observation).

  VM-PROTOCOL.md \"Real-machine test per step\": this deftest exercises the
  real tick's R2 function on this tick's problem input — the :active-repo-ratio
  nil crash at futon2.aif.observation.clj:129 — and asserts what the code
  actually does today, including the throw.

  Model side: R2 has NO aligned model counterpart in cascade_model_manifest.clj
  — observation there is token-likelihood, P5's token adjudication, not this
  14-channel vector. No model assertion is made (see 01-R2.edn :real-vs-model)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.observation :as obs]))

(deftest s01-r2
  (testing "real R2 observe on a summary without :total-repos throws today"
    ;; observation.clj:129 reads (:total-repos summary) with no default; the
    ;; neighbouring :coupling-density read at :134 uses default 0.
    (is (thrown? NullPointerException
                 (obs/observe {:graph {:summary {:active-repos 3}}}))))

  (testing "empty scan does not throw and coerces :active-repo-ratio to 0.0"
    (let [observation (obs/observe {})]
      (is (map? observation))
      (is (= 0.0 (:active-repo-ratio observation)))))

  (testing "absence envelope for :active-repo-ratio promises :coerced-to 0.0"
    (let [envelope (obs/observation-envelope (obs/observe {}))
          channel (get-in envelope [:channels :active-repo-ratio])]
      (is (= :futon2.aif/tagged-observation (:type envelope)))
      (is (= :absent (:variant channel)))
      (is (= :source-field-missing (:reason channel)))
      (is (= 0.0 (:coerced-to channel)))))

  (testing "the real tick route-tags observe as :R2"
    ;; war_machine.clj lives on the classpath via the scripts path (deps.edn).
    (let [source (slurp (io/resource "futon2/report/war_machine.clj"))]
      (is (.contains
           source
           "(route-tag route0 :R2 \"futon2.aif.observation/observe\")")))))
