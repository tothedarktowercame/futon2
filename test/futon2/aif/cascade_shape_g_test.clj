(ns futon2.aif.cascade-shape-g-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-shape-g :as shape-g]))

(def artifacts "holes/labs/wm-contract/mission-head-cascades-2026-09-30")

(def same-order-a
  {:nodes [{:pattern "p/a"} {:pattern "p/b"} {:pattern "p/c"}]
   :precedence ["p/a" "p/b" "p/c"]
   :edges [{:from "p/a" :to "p/b" :kind :precedes}
           {:from "p/b" :to "p/c" :kind :precedes}]})

(def same-order-b
  {:nodes (:nodes same-order-a)
   :precedence (:precedence same-order-a)
   :edges [{:from "p/a" :to "p/c" :kind :precedes}]})

(deftest arrangement-reaches-the-real-scorer
  (let [a (shape-g/score-arranged "target" "chain" same-order-a)
        b (shape-g/score-arranged "target" "join" same-order-b)]
    (is (= [:computed :computed] [(:status a) (:status b)]))
    (is (not= (:g a) (:g b))
        "same firing list, different edges: compiling only the list makes this fail")
    (doseq [r [a b]]
      (is (every? #(Double/isFinite (double %))
                  ((juxt :g :risk :ambiguity :information-gain) r)))
      (is (= (:horizon r) (count (:preference-at-each-step r)))))))

(deftest overlap-is-a-shared-advance-token
  (let [c (assoc same-order-a :edges [{:from "p/a" :to "p/b" :kind :overlap}])
        candidate (shape-g/arranged->candidate "t" "overlap" c)
        pa (first (:precedence candidate))
        pb (second (:precedence candidate))]
    (is (= 1 (count (set/intersection (:produces pa) (:produces pb)))))
    (is (empty? (get-in pb [:guard :clauses 0 :present]))
        "overlap co-advances shared state; it is not an enabling edge")))

(deftest repeated-pattern-citations-remain-separate-nodes
  (let [cascade {:nodes [{:pattern "p/a" :fragment-index 1}
                         {:pattern "p/a" :fragment-index 4}]
                 :edges [{:from "p/a" :to "p/a" :kind :precedes
                          :from-fragment 1 :to-fragment 4}]}
        candidate (shape-g/arranged->candidate "t" "repeated" cascade)
        [first-occurrence second-occurrence] (:precedence candidate)]
    (is (= 2 (count (:precedence candidate))))
    (is (not= (:occurrence-id first-occurrence) (:occurrence-id second-occurrence)))
    (is (contains? (get-in second-occurrence [:guard :clauses 0 :present])
                   ["t" :pattern-done (:occurrence-id first-occurrence)]))))

(defn- fit-analysis [accepted-patterns]
  {:sentences [{:fragments (mapv (fn [i p]
                                   {:start i :end (inc i) :text p :relations ["action"]
                                    :pattern_refs [{:id p :status "candidate"
                                                    :rationale "fixture fit"
                                                    :source_sha256 (apply str (repeat 64 "a"))}]})
                                 (range) accepted-patterns)}]})

(deftest unknown-connector-has-higher-f-and-g
  (let [cascade {:nodes [{:pattern "p/a"} {:pattern "p/b"}]
                 :edges [{:from "p/a" :to "p/b" :kind :precedes}]}
        policy {:target "t" :policy-id "same-shape" :cascade cascade}
        fitted (shape-g/score-policy (assoc policy :analysis (fit-analysis ["p/a" "p/b"])))
        unknown (shape-g/score-policy (assoc policy :analysis (fit-analysis ["p/a"])))]
    (is (< (:f fitted) (:f unknown)))
    (is (< (:g fitted) (:g unknown)))
    (is (= :computed (:f-status fitted) (:f-status unknown)))
    (is (= :sigma-log-E-minus-F-minus-gamma-G
           (get-in unknown [:selection-law :law])))
    (is (= ["p/b"] (get-in unknown [:fit :interpretation-owed])))))

(deftest ^:slow all-recorded-head-policies-have-g
  (let [policies (shape-g/materialize-policies artifacts)
        results (mapv shape-g/score-policy policies)]
    (is (= {:reported-count 81 :distinct-count 33} (meta policies)))
    (is (= 33 (count policies)))
    (is (= 33 (count (filter #(= :computed (:status %)) results))))
    (is (zero? (count (remove #(Double/isFinite (double (:g %))) results))))
    (is (every? #(and (= :computed (:f-status %))
                      (Double/isFinite (double (:f %)))) results))
    (is (every? #(= (:horizon %) (count (:preference-at-each-step %))) results))))
