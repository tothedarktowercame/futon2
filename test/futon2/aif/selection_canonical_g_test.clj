(ns futon2.aif.selection-canonical-g-test
  "Selection G over cascades (Joe, 2026-11-13; Lean SelectionGOverCascades).
  The field is the Lucy ladder case that abstained :insufficient-comparison:
  tickets 08 and 09 ready with equal Giraffe facts, 09 alone placed under a way."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.selection-canonical-g :as sg]))

(def facts
  {"T-08" {:status "ready" :unblocks 0 :cone 0 :prerequisites []
           :placement {:status "open" :action "work" :ways []}}
   "T-09" {:status "ready" :unblocks 0 :cone 0 :prerequisites []
           :placement {:status "open" :action "work" :ways ["one-queryable-self-account"]}}})

(defn- g [entries id] (get-in (sg/score entries facts) [:by-id id :G-cascade]))

(def only ["only-what-can-start-now"])
(def giraffe ["only-what-can-start-now" "prepare-the-ground" "what-others-wait-on-first"])
(def serves ["only-what-can-start-now" "serves-a-way" "what-others-wait-on-first"])

(deftest same-pick-same-patterns-same-g
  (let [es [{:id :a :patterns only :pick "T-08" :relation-class :useful-elsewhere}
            {:id :b :patterns only :pick "T-08" :relation-class :useful-elsewhere}]]
    (is (number? (g es :a)))
    (is (= (g es :a) (g es :b)))))

(deftest same-pick-different-firing-patterns-different-g
  (testing "Joe's ruling: G is over the cascade, not its pick. The pick-only
            scorer (outer-score) gives these two one G by construction."
    (let [es [{:id :plain :patterns only :pick "T-09" :relation-class :useful-elsewhere}
              {:id :way :patterns serves :pick "T-09" :relation-class :useful-elsewhere}]]
      (is (number? (g es :plain)))
      (is (not= (g es :plain) (g es :way)))
      (is (< (g es :way) (g es :plain))
          "serves-a-way fires on T-09's placement: one more pattern's novelty"))))

(deftest ladder-field-gives-two-classes-below-stopping
  (let [es [{:id :only-what-can-start-now :patterns only :pick "T-08" :relation-class :useful-elsewhere}
            {:id :find-out-before-building :patterns ["find-out-before-building"] :pick :no-pick}
            {:id :giraffe-order :patterns giraffe :pick "T-08" :relation-class :useful-elsewhere}
            {:id :serves-a-way :patterns serves :pick "T-09" :relation-class :useful-elsewhere}]
        r (sg/score es facts)
        G #(get-in r [:by-id % :G-cascade])
        stop (get-in r [:stopping :G-cascade])]
    (is (nil? (:refusal r)))
    (testing "patterns that cannot fire on the pick add nothing"
      (is (= (G :only-what-can-start-now) (G :giraffe-order))))
    (is (< (G :serves-a-way) (G :only-what-can-start-now) stop))
    (is (= stop (G :find-out-before-building)) "no pick scores as stopping")))

(deftest patternless-cascade-is-the-empty-policy
  (let [es [{:id :empty :patterns [] :pick "T-08" :relation-class :useful-elsewhere}]
        r (sg/score es facts)]
    (is (= (get-in r [:stopping :G-cascade]) (get-in r [:by-id :empty :G-cascade])))))

(deftest uninterpreted-pattern-refuses
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"no interpretation"
                        (sg/score [{:id :x :patterns ["take-the-cheap-win"] :pick "T-08"
                                    :relation-class :useful-elsewhere}] facts))))

(deftest fired-patterns-are-recorded-for-requirements-q12
  (let [es [{:id :only-what-can-start-now :patterns only :pick "T-08" :relation-class :useful-elsewhere}
            {:id :giraffe-order :patterns giraffe :pick "T-08" :relation-class :useful-elsewhere}
            {:id :serves-a-way :patterns serves :pick "T-09" :relation-class :useful-elsewhere}]
        fired (:fired (sg/score es facts))]
    (is (= ["only-what-can-start-now"] (fired :only-what-can-start-now)))
    (is (= ["only-what-can-start-now"] (fired :giraffe-order))
        "no cone, no unblocks on T-08: prepare-the-ground and what-others-wait-on-first cannot fire")
    (is (= ["only-what-can-start-now" "serves-a-way"] (fired :serves-a-way)))
    (is (= [] (fired :futon2.aif.selection-canonical-g/stopping)))))

(deftest predicted-parameters-are-per-pattern-for-requirements-q11
  (let [es [{:id :only-what-can-start-now :patterns only :pick "T-08" :relation-class :useful-elsewhere}
            {:id :serves-a-way :patterns serves :pick "T-09" :relation-class :useful-elsewhere}]
        fired (:fired (sg/score es facts))
        p #(sg/predicted-parameters (fired %))]
    (testing "the Lucy 08/09 pair: one outcome class, different parameters"
      (is (= {"only-what-can-start-now" [2 1]} (p :only-what-can-start-now)))
      (is (= {"only-what-can-start-now" [2 1] "serves-a-way" [2 1]} (p :serves-a-way))))
    (testing "the click-12 facade: different picks, same patterns, same posterior"
      (is (= (sg/predicted-parameters ["only-what-can-start-now"])
             (p :only-what-can-start-now))))
    (is (= {} (sg/predicted-parameters [])) "the empty policy predicts no change")))

;; Lucy 2026-11-13, clicks 2-3: with ladder 09 closed the field was 08 and 10,
;; every pick-making cascade picked 08 and one class was compared.  A chain
;; (Joe: "it might select 8 first b/c it anticipates selecting 10 next") is a
;; second policy with the same first pick.
(def facts-08-10
  {"T-08" {:status "ready" :unblocks 0 :cone 0 :prerequisites [] :rank 1
           :placement {:status "open" :action "work" :ways []}}
   "T-10" {:status "ready" :unblocks 0 :cone 0 :prerequisites [] :rank 3
           :placement {:status "open" :action "work" :ways []}}})

(deftest chain-anticipating-the-next-pick-is-a-distinct-lower-g-policy
  (let [es [{:id :alone :patterns only :pick "T-08" :relation-class :useful-elsewhere}
            {:id :chain :patterns only :pick "T-08" :picks ["T-08" "T-10"]
             :relation-class :useful-elsewhere}]
        r (sg/score es facts-08-10)
        G #(get-in r [:by-id % :G-cascade])]
    (is (nil? (:refusal r)))
    (is (< (G :chain) (G :alone) (get-in r [:stopping :G-cascade]))
        "novelty-only ranking: the chain's second pick fires one more unit")
    (is (= [["only-what-can-start-now" "T-08"] ["only-what-can-start-now" "T-10"]]
           (get-in r [:fired-units :chain])))
    (is (= {"only-what-can-start-now" [3 1]}
           (sg/predicted-parameters (map first (get-in r [:fired-units :chain])))))
    (is (= {"only-what-can-start-now" [2 1]}
           (sg/predicted-parameters (map first (get-in r [:fired-units :alone])))))))

(deftest chain-later-pick-waits-for-the-earlier-pick
  (testing "find-out-before-building never produces can-begin, so a chain under it
            fires only on its first pick"
    (let [facts {"T-a" {:status "undetermined" :unblocks 0 :cone 0 :prerequisites []}
                 "T-b" {:status "undetermined" :unblocks 0 :cone 0 :prerequisites []}}
          r (sg/score [{:id :c :patterns ["find-out-before-building"] :pick "T-a"
                        :picks ["T-a" "T-b"] :relation-class :useful-elsewhere}] facts)]
      (is (= [["find-out-before-building" "T-a"]] (get-in r [:fired-units :c]))))))

(deftest a-repeated-pattern-is-less-novel-than-a-second-pattern
  ;; Lucy 2026-11-13, wm-click-3e072695 (field 09 and 10): serves-a-way on 09
  ;; and the chain 09 then 10 under only-what-can-start-now each fired two
  ;; units at Beta(1,1) and tied at the minimum.  Parameters are per pattern,
  ;; so the chain's second only-what-can-start-now starts from Beta(2,1).
  (let [facts {"T-09" {:status "ready" :unblocks 0 :cone 0 :prerequisites [] :rank 2
                       :placement {:status "open" :action "work" :ways ["one-queryable-self-account"]}}
               "T-10" {:status "ready" :unblocks 0 :cone 0 :prerequisites [] :rank 3
                       :placement {:status "open" :action "work" :ways []}}}
        es [{:id :way :patterns serves :pick "T-09" :relation-class :useful-elsewhere}
            {:id :chain :patterns only :pick "T-09" :picks ["T-09" "T-10"]
             :relation-class :useful-elsewhere}
            {:id :alone :patterns only :pick "T-09" :relation-class :useful-elsewhere}]
        r (sg/score es facts)
        G #(get-in r [:by-id % :G-cascade])]
    (is (nil? (:refusal r)))
    (is (= 2 (count (get-in r [:fired-units :way]))) (= 2 (count (get-in r [:fired-units :chain]))))
    (is (< (G :way) (G :chain) (G :alone))
        "two patterns once each < one pattern twice < one pattern once")))

