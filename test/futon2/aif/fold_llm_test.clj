(ns futon2.aif.fold-llm-test
  "Impl #2 (the LLM-turn fold) satisfies the SAME `futon2.aif.fold` interface as
   impl #1, with an INJECTED turn (incident-safe: no LLM in the JVM) and the
   SHARED coverage→rollout evaluation — E-close-the-loop §2/§6b."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.fold :as fold]
            [futon2.aif.fold-eval :as fe]
            [futon2.aif.fold-llm :as llm]))

(def ^:private a-construction
  {:boxes [{:id :a :role "r1" :fits-pattern "p/x"}
           {:id :b :role "r2" :fits-pattern "p/y"}]
   :wires [[:a :b]]
   :terminals [:b]
   :policy-holes [{:unfolded-pattern nil :free "the metric" :why "undefined"}]})

(deftest llm-fold-satisfies-interface
  (testing "an injected turn → a valid fold output with the three ports + a closing ΔG"
    (let [out (llm/llm-fold ["p/x" "p/y"] {:mission "M-t"}
                            {:turn-fn (constantly (pr-str a-construction))})]
      (is (fold/valid-fold-output? out))
      (is (= 2 (count (get-in out [:wiring :boxes]))))
      (is (seq (:policy-holes out)))
      (is (fold/closes? out) "2 boxes / 1 hole ⇒ coverage>0 ⇒ ΔG<0"))))

(deftest evaluation-axis-is-shared-with-impl-1
  (testing "ΔG is exactly the shared coverage→rollout over the built wiring"
    (let [out (llm/llm-fold ["p/x" "p/y"] {} {:turn-fn (constantly (pr-str a-construction))})]
      (is (= (fe/coverage-delta-g (:wiring out)) (:delta-g out))))))

(deftest incident-safe-defaults
  (testing "no turn-fn ⇒ no construction ⇒ ΔG nil ⇒ the gate abstains (never blocks, spawns nothing)"
    (let [out (llm/llm-fold ["p/x"] {})]
      (is (fold/valid-fold-output? out))
      (is (empty? (get-in out [:wiring :boxes])) "no fabricated construction")
      (is (nil? (:delta-g out)))
      (is (not (fold/closes? out)))))
  (testing "an unreadable turn ⇒ no construction ⇒ ΔG nil (honest, not a crash)"
    (let [out (llm/llm-fold ["p/x"] {} {:turn-fn (constantly "}{ not edn")})]
      (is (nil? (:delta-g out)))
      (is (empty? (get-in out [:wiring :boxes]))))))

(deftest turn-may-return-a-map-or-a-string
  (testing "a turn that returns the construction map directly is accepted too"
    (let [out (llm/llm-fold ["p/x" "p/y"] {} {:turn-fn (constantly a-construction)})]
      (is (fold/closes? out))
      (is (= 2 (count (get-in out [:wiring :boxes])))))))

(deftest prompt-is-pure-and-carries-prose-and-shape
  (testing "the fold prompt names the circumstance, the pattern prose, and the required EDN shape"
    (let [p (llm/fold-prompt ["p/x"] {:mission "M-t"} {"p/x" "IF foo HOWEVER bar THEN baz"})]
      (is (string? p))
      (is (re-find #"M-t" p))
      (is (re-find #"IF foo HOWEVER bar THEN baz" p))
      (is (re-find #":policy-holes" p) "asks for explicit holes")
      (is (re-find #"never fabricate|Do not fabricate" p) "coverage-discipline instruction"))))

(deftest parse-construction-is-defensive
  (is (= a-construction (llm/parse-construction a-construction)) "map passthrough")
  (is (map? (llm/parse-construction (pr-str a-construction))) "edn string parses")
  (is (nil? (llm/parse-construction "}{")) "garbage ⇒ nil, no throw")
  (is (nil? (llm/parse-construction 42)) "non-string/non-map ⇒ nil"))
