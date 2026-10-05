(ns futon2.aif.interpretation-construction-order-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-construction :as construction]))

(defn pattern [needs produces]
  {:guard {:needs (set needs) :forbids #{}}
   :produces (set produces)})

(defn compile-plan [patterns wants horizon order]
  (#'construction/compile-plan patterns #{} wants horizon order 0))

(deftest independent-producers-coapply-at-one-step
  (let [patterns {:p (pattern [] [:w1])
                  :q (pattern [] [:w2])
                  :r (pattern [] [:w3])}
        candidate (:candidate (compile-plan patterns [:w1 :w2 :w3] 1 [:p :q :r]))]
    (is (= {:order :co-application} (:order-use candidate)))
    (is (= [:w1 :w2 :w3] (:reached-wants candidate)))
    (is (= [] (:unreached-wants candidate)))
    (is (= [] (get-in candidate [:order :descent])))))

(deftest chain-keeps-list-horizon-semantics
  (let [patterns {:p (pattern [] [:w1])
                  :q (pattern [:w1] [:w2])
                  :r (pattern [:w2] [:w3])}
        one (:candidate (compile-plan patterns [:w1 :w2 :w3] 1 [:p :q :r]))
        three (:candidate (compile-plan patterns [:w1 :w2 :w3] 3 [:p :q :r]))]
    (is (= {:order :chain} (:order-use one)))
    (is (= [:w1] (:reached-wants one)))
    (is (= [{:token :w2 :reason :beyond-horizon}
            {:token :w3 :reason :beyond-horizon}]
           (:unreached-wants one)))
    (is (= {:order :chain} (:order-use three)))
    (is (= [:w1 :w2 :w3] (:reached-wants three)))
    (is (= [] (:unreached-wants three)))))

(deftest mixed-order-matches-the-recorded-run-shape
  ;; Authored after run 2026-10-05-c9d25d6a's order: three independent
  ;; producers and a fourth whose guard needs one frontier product.
  (let [patterns {:reader (pattern [] [:reader-want])
                  :capture (pattern [] [:capture-want])
                  :handoff (pattern [] [:handoff-want])
                  :warrant (pattern [:handoff-want] [:warrant-want])}
        wants [:reader-want :capture-want :handoff-want :warrant-want]
        order [:reader :capture :handoff :warrant]
        one (:candidate (compile-plan patterns wants 1 order))
        two (:candidate (compile-plan patterns wants 2 order))]
    (is (= {:order :co-application} (:order-use one)))
    (is (= [:capture-want :handoff-want :reader-want] (:reached-wants one)))
    (is (= [{:token :warrant-want :reason :beyond-horizon}]
           (:unreached-wants one)))
    (is (= (sort-by pr-str wants) (:reached-wants two)))
    (is (= [] (:unreached-wants two)))))

(deftest independent-later-want-is-now-admitted
  (let [patterns {:first (pattern [] [:not-wanted])
                  :second (pattern [] [:wanted])}
        result (compile-plan patterns [:wanted] 1 [:first :second])]
    ;; The old list rollout fired :first and returned
    ;; :want-unreachable-within-horizon. The frontier transition fires both.
    (is (nil? (:finding result)))
    (is (= [:wanted] (get-in result [:candidate :reached-wants])))
    (is (= {:order :co-application}
           (get-in result [:candidate :order-use])))))
