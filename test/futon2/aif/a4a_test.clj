(ns futon2.aif.a4a-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.a4a :as a4a]
            [futon2.aif.a4a-substrate :as substrate]
            [futon2.aif.actuator-a3 :as a3]
            [futon2.aif.bmr :as bmr]))

(defn- repeat-edge
  [n edge]
  (vec (repeat n edge)))

(def synthetic-merge-corpus
  {:capabilities ["A" "B" "C"]
   :edges (vec (concat
                (repeat-edge 10 ["A" "m1"])
                (repeat-edge 10 ["A" "m2"])
                (repeat-edge 1 ["A" "m3"])
                (repeat-edge 10 ["B" "m1"])
                (repeat-edge 10 ["B" "m2"])
                (repeat-edge 1 ["B" "m3"])
                (repeat-edge 1 ["C" "m1"])
                (repeat-edge 1 ["C" "m2"])
                (repeat-edge 20 ["C" "m3"])))
   :discharges []})

(defn- score-by-pair
  [reduced]
  (into {} (map (juxt :pair identity) (:merge-scores reduced))))

(defn- aggregate-stddev
  [row]
  (Math/sqrt (/ (reduce + (map #(* % %) (map :stddev (bmr/dirichlet-moments row))))
                (count row))))

(deftest corpus-to-concentration-counts
  (let [result (a4a/corpus->concentration
                {:capabilities ["A" "B"]
                 :edges [["A" "m1"]
                         ["A" "m1"]
                         ["B" "m2"]]
                 :discharges [{:mission "m1" :endpoint "B" :type :capability}
                              {:mission "m2" :endpoint "A" :type :other}]})]
    (is (= ["A" "B"] (:capabilities result)))
    (is (= ["m1" "m2"] (:outcomes result)))
    (is (= {"A" [2.1 0.1]
            "B" [1.1 1.1]}
           (:concentrations result)))))

(deftest concept-merge-sign-pin
  (let [reduced (a4a/reduce-concepts synthetic-merge-corpus)
        scores (score-by-pair reduced)
        ab (get scores ["A" "B"])
        ac (get scores ["A" "C"])
        bc (get scores ["B" "C"])]
    (println "A4a synthetic delta-F values"
             {:A-B (:delta-F ab)
              :A-C (:delta-F ac)
              :B-C (:delta-F bc)})
    (testing "identical A/B mission profiles merge"
      (is (<= (:delta-F ab) -3.0))
      (is (true? (:accept? ab)))
      (is (= {"A" ["A" "B"]
              "C" ["C"]}
             (:equivalence-classes reduced))))
    (testing "distinct C remains separate"
      (is (> (:delta-F ac) -3.0))
      (is (> (:delta-F bc) -3.0))
      (is (false? (:accept? ac)))
      (is (false? (:accept? bc)))
      (is (= ["A" "C"] (:concepts reduced))))))

(deftest concept-stddev-is-endpoint-count
  (let [reduced (a4a/reduce-concepts synthetic-merge-corpus)
        stddevs (a4a/concept->stddev reduced)
        concept-rows (:concept-concentrations reduced)]
    (is (= (set (:concepts reduced)) (set (keys stddevs))))
    (is (= (aggregate-stddev (get concept-rows "A"))
           (get stddevs "A")))
    (is (every? number? (vals stddevs)))
    (is (not-any? #(and (map? %) (contains? % :variance)) (vals stddevs)))))

(deftest star-doc-shape-is-deterministic
  (let [doc-1 (a4a/concept->star-doc "A")
        doc-2 (a4a/concept->star-doc "A")]
    (is (= doc-1 doc-2))
    (is (= {:xt/id "a4a-bmr/capability-star/A"
            :entity/type :capability-star
            :star/capability "A"
            :star/updated-by :a4a-bmr}
           doc-1))))

(deftest pure-pipeline-is-deterministic-and-order-stable
  (let [shuffled (-> synthetic-merge-corpus
                     (assoc :capabilities ["C" "B" "A"])
                     (update :edges #(vec (reverse %))))
        reduced-1 (a4a/reduce-concepts synthetic-merge-corpus)
        reduced-2 (a4a/reduce-concepts synthetic-merge-corpus)
        reduced-3 (a4a/reduce-concepts shuffled)]
    (is (= (select-keys reduced-1 [:concepts :equivalence-classes :concept-concentrations])
           (select-keys reduced-2 [:concepts :equivalence-classes :concept-concentrations])))
    (is (= (select-keys reduced-1 [:concepts :equivalence-classes :concept-concentrations])
           (select-keys reduced-3 [:concepts :equivalence-classes :concept-concentrations])))))

(deftest eig-for-produces-identity-no-merge
  (let [reduced {:equivalence-classes {"c1" ["c1"]
                                       "c2" ["c2"]
                                       "c3" ["c3"]}
                 :concept-concentrations {"c1" [3.1 1.1 0.1]
                                          "c2" [0.1 2.1 2.1]
                                          "c3" [1.1 0.1 4.1]}}
        stddevs (a4a/concept->stddev reduced)]
    (is (= {"c1" "c1" "c2" "c2" "c3" "c3"}
           (a4a/capability->concept reduced)))
    (is (= (+ (get stddevs "c1") (get stddevs "c2"))
           (a4a/eig-for-produces reduced ["c1" "c2"])))))

(deftest eig-for-produces-counts-merged-concept-once
  (let [reduced (a4a/reduce-concepts synthetic-merge-corpus)
        stddevs (a4a/concept->stddev reduced)]
    (is (= {"A" "A" "B" "A" "C" "C"}
           (a4a/capability->concept reduced)))
    (is (= (get stddevs "A")
           (a4a/eig-for-produces reduced ["A" "B"])))
    (is (= (+ (get stddevs "A") (get stddevs "C"))
           (a4a/eig-for-produces reduced ["A" "B" "C"])))))

(deftest eig-for-produces-absent-and-empty
  (let [reduced (a4a/reduce-concepts synthetic-merge-corpus)]
    (is (= 0.0 (a4a/eig-for-produces reduced ["unknown"])))
    (is (= 0.0 (a4a/eig-for-produces reduced [])))
    (is (= 0.0 (a4a/eig-for-produces reduced nil)))))

(deftest make-eig-fn-returns-deterministic-closure
  (let [mission->eig {"M-a" 0.25 "M-b" 0.5}
        eig-fn (a4a/make-eig-fn mission->eig)]
    (is (= 0.25 (eig-fn "M-a" {:produces ["ignored"]})))
    (is (= 0.25 (eig-fn "M-a@futon0" {}))
        "mission IDs resolve across @repo suffixes")
    (is (= 0.0 (eig-fn "M-absent" {})))
    (is (= (eig-fn "M-b" {})
           (eig-fn "M-b" {})))))

(deftest constellation-eig-and-mission-eig
  (let [pattern->constellation {"ns/p1" 1
                                "ns/p2" 1
                                "other/p3" 2}
        edges [["p1" "M-a"]
               ["p2" "M-a"]
               ["p2" "M-b"]
               ["p3" "M-b"]]
        c->eig (a4a/constellation->eig pattern->constellation edges)
        m->patterns {"M-a@futon0" ["p1" "p2" "unknown"]
                     "M-b" ["p2" "p3"]
                     "M-empty" []}
        m->eig (a4a/mission->eig m->patterns pattern->constellation c->eig)]
    (is (= #{1 2} (set (keys c->eig))))
    (is (every? pos? (vals c->eig)))
    (is (= (get c->eig 1) (get m->eig "M-a"))
        "p1 and p2 share a constellation, so M-a counts it once")
    (is (= (+ (get c->eig 1) (get c->eig 2))
           (get m->eig "M-b")))
    (is (= 0.0 (get m->eig "M-empty")))))

(deftest mint-stars-dry-run-does-not-write
  (let [called? (atom false)]
    (with-redefs [a3/drawbridge-submit-tx! (fn [& _]
                                             (reset! called? true)
                                             (throw (ex-info "should not write in dry-run" {})))]
      (is (= [(a4a/concept->star-doc "A")]
             (substrate/mint-stars! ["A"])))
      (is (false? @called?)))))

(deftest mint-stars-exposes-constellation-eig
  (with-redefs [substrate/load-pattern-grain-eig
                (fn [_] {:constellation->eig {2 0.0125}})]
    (is (= [(assoc (a4a/concept->star-doc 2)
                   :star/constellation-eig 0.0125
                   :star/eig-unit :endpoint-count-stddev
                   :star/eig-source :a4a-constellation-stddev)]
           (substrate/mint-stars! [2])))
    (is (= [(assoc (a4a/concept->star-doc "P2")
                   :star/constellation-eig 0.0125
                   :star/eig-unit :endpoint-count-stddev
                   :star/eig-source :a4a-constellation-stddev)]
           (substrate/mint-stars! ["P2"])))))
