(ns futon2.report.war-machine-horizon-test
  "E-cascade-real D14/D16: the tick's common horizon is declared or
  computed, never the fallback literal T=2; the judgement records it, and the
  construction budget and move cost, each with its authority."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-construction :as ic]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def target "M-chain")

;; a 4-step chain: s0 -> t1 -> t2 -> t3 -> t4, one pattern per step
(def patterns
  {:chain/one {:guard {:needs #{:s0} :forbids #{:t1}} :produces #{:t1}}
   :chain/two {:guard {:needs #{:t1} :forbids #{:t2}} :produces #{:t2}}
   :chain/three {:guard {:needs #{:t2} :forbids #{:t3}} :produces #{:t3}}
   :chain/four {:guard {:needs #{:t3} :forbids #{:t4}} :produces #{:t4}}})

(def tokens [:s0 :t1 :t2 :t3 :t4])

(defn- sources [& [extra]]
  (merge
   {:universes {target {:s0 true :t1 false :t2 false :t3 false :t4 false}}
    :wants {target [:t4]}
    :locators {target (into {} (for [t tokens]
                                 [t {:class :C4 :repo "futon2" :sha "HEAD" :path "x.md" :decl (str "- [x] " (name t))}]))}
    :interpretations {target {:patterns patterns
                              :receipts (into {} (for [k (keys patterns)] [k {:source :test}]))}}
    :beta-by-context {:WM {:beta 1}} :context-of (constantly :WM)
    :construction {:construct ic/construct :budget {:max-moves 4 :max-expansions 20000}
                   ;; a stand-in G (empty cascade worst, then shorter
                   ;; better): this test is about the horizon. With the lane's
                   ;; real G the 4-step chain was DECLINED even at horizon 4
                   ;; before H-VALUE-G-D: G(plan) 15.34 vs G(empty) 10.80
                   ;; compared two different token universes; in one
                   ;; universe the pair is 15.34 vs 16.34 and the chain is
                   ;; taken (futon2.report.war-machine-universe-test).
                   :move-cost 0 :evaluate-g (fn [_ c] (if (empty? (:precedence c)) 1.0e9
                                                          (double (count (:precedence c)))))}}
   extra))

(defn- empty-store []
  (.getCanonicalPath (.toFile (Files/createTempDirectory "horizon-store" (make-array FileAttribute 0)))))

(deftest declared-horizon-wins
  (is (= {:value 3 :authority {:source :cascade-sources :declarations nil}}
         (wm/resolve-cascade-horizon {:horizon-steps 3} [target]))))

(deftest computed-horizon-is-the-largest-interpretation-count
  (let [h (wm/resolve-cascade-horizon (sources) [target "M-none"])]
    (is (= 4 (:value h)))
    (is (= :computed (get-in h [:authority :source])))
    (is (= {target 4} (get-in h [:authority :per-target])))))

(deftest nothing-admitted-is-horizon-1-not-2
  (is (= 1 (:value (wm/resolve-cascade-horizon {} ["M-none"])))))

(deftest a-four-step-chain-constructs-at-the-computed-horizon
  (let [r (wm/assemble-cascade-problems-with-published (empty-store) {:targets [target] :sources (sources)})]
    (is (= 4 (get-in r [:cascade-horizon :value])))
    (is (empty? (:refusals r)) (pr-str (:refusals r)))
    (is (= [[:chain/one :chain/two :chain/three :chain/four]]
           (get-in (first (:problems r)) [:cascade-problem :precedences])))))

(deftest the-old-literal-t2-cuts-the-chain
  ;; bad case: the horizon the fallback literal gave. At T=2 the want is
  ;; beyond the horizon, so no plan reaches it
  (let [r (wm/assemble-cascade-problems-with-published
           (empty-store) {:targets [target] :sources (sources {:horizon-steps 2})})
        c (first (mapcat :constructed-candidates (:problems r)))]
    (is (= 2 (get-in r [:cascade-horizon :value])))
    (is (or (seq (:refusals r))
            (some #(= :beyond-horizon (:reason %)) (get-in c [:construction-receipt :unreached-wants]))))))

(deftest construction-parameters-carry-their-authority
  (is (= :none-found (get-in wm/construction-move-cost [:authority :ruling])))
  (is (= {:max-moves 4 :max-expansions 20000} (:value (wm/construction-budget {}))))
  (testing "a declared budget wins"
    (is (= {:value {:max-moves 9} :authority {:source :cascade-sources}}
           (wm/construction-budget {:construction-budget {:max-moves 9}})))))
