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

(defn guarded [needs forbids produces]
  {:guard {:needs (set needs) :forbids (set forbids)} :produces (set produces)})

(deftest conflicting-frontier-is-flagged-on-the-candidate
  ;; The shape of Lean Proof2.CoApplicationKernel.ConflictFixture: p produces
  ;; :a and forbids :b; q produces :b and forbids :a. Co-application fires
  ;; both and reaches a state each of them forbids; the candidate says so.
  (let [patterns {:p (guarded [] [:b] [:a])
                  :q (guarded [] [:a] [:b])}
        candidate (:candidate (compile-plan patterns [:a :b] 1 [:p :q]))]
    (is (= [:a :b] (:reached-wants candidate)))
    (is (= [{:tau 1 :state #{} :frontier [:p :q]}]
           (:frontier-conflicts candidate)))))

(deftest a-frontier-without-conflict-carries-no-flag
  (let [patterns {:p (pattern [] [:w1])
                  :q (pattern [] [:w2])}
        candidate (:candidate (compile-plan patterns [:w1 :w2] 1 [:p :q]))]
    (is (= [] (:frontier-conflicts candidate)))))

(deftest a-need-cycle-is-a-typed-finding
  (let [patterns {:p (pattern [:b] [:a])
                  :q (pattern [:a] [:b])}
        result (compile-plan patterns [:a :b] 2 [:p :q])]
    (is (= :need-cycle (get-in result [:finding :kind])))
    (is (nil? (:candidate result)))))

(deftest an-overlapping-pair-without-a-meet-is-recorded-on-the-order
  ;; p and q both stand above A and B, which are incomparable: the pair has
  ;; two maximal common units and no meet (Lean hasRestrictedMeets fails).
  ;; The plan is still compiled; its order names the pair.
  (let [patterns {:p (pattern [] [:x])
                  :q (pattern [] [:y])
                  :A (pattern [:x :y] [:wa])
                  :B (pattern [:x :y] [:wb])}
        candidate (:candidate (compile-plan patterns [:wa :wb] 2 [:p :q :A :B]))]
    (is (= [{:pair [:p :q] :common-maximal [:A :B]}]
           (get-in candidate [:order :missing-meets])))
    (is (= [:wa :wb] (:reached-wants candidate)))))
