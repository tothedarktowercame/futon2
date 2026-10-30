(ns futon2.aif.r20-reading-beta-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.edn :as edn] [clojure.java.io :as io]
            [futon2.aif.affective-level :as affect]
            [futon2.aif.cascade-selection :as cs]
            [futon2.aif.policy :as policy]
            [futon2.aif.decision-gate :as gate]
            [futon2.aif.reading-policy-precision :as precision]
            [futon2.aif.target-policy-family :as family]
            [futon2.aif.wm.family-selection :as selection]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.reading-route :as route]))
(use-fixtures :once hermetic/with-hermetic-stores)
(use-fixtures :each route/with-empty-learning-ledger)
(defn close? [a b] (< (Math/abs (- a b)) 1e-12))
(deftest lean-precision-examples
  ;; R20Run.hesp_start; R20Step.iteratePrecision_constant_F
  (let [prior (affect/carried-valence-prior {:status :absent :reason :no-earlier-started-run})
        cs [{:id :a :habit 1 :g 0 :f 3} {:id :b :habit 2 :g 2 :f 3}]]
    (is (= 1.25 (:beta-prior prior)))
    (doseq [n [0 1 2 16]] (is (close? 1.25 (:beta-posterior (cs/iterate-precision 1.25 n cs))))))
  ;; R20Step.moving_click_zero_betaPosterior: 5/4 + 5/16 * log 3.
  (let [c [{:id :a :habit 1 :g 0 :f 0}
           {:id :b :habit 1 :g (* 1.25 (Math/log 3)) :f (- (Math/log 3))}]]
    (is (close? (+ 1.25 (* 0.3125 (Math/log 3)))
                (:beta-posterior (cs/iterate-precision 1.25 1 c))))))
(deftest two-fixed-iterations
  ;; R20Step.iteratePrecision definition applied twice to movingPrecisionObservation.
  ;; Closed-form second step, independently expanded from the two-policy softmax.
  (let [l (Math/log 3) b1 (+ 1.25 (* 0.3125 l))
        prior-weight (Math/exp (- (/ (* 1.25 l) b1)))
        post-weight (* 3 prior-weight)
        expected (+ 1.25 (* (- (/ post-weight (+ 1 post-weight))
                                (/ prior-weight (+ 1 prior-weight))) (* 1.25 l)))
        candidates [{:id :a :habit 1 :g 0 :f 0}
                    {:id :b :habit 1 :g (* 1.25 l) :f (- l)}]]
    (is (close? expected (:beta-posterior (cs/iterate-precision 1.25 2 candidates))))))

(defn frozen [] (edn/read-string (slurp (io/resource "fixtures/r20-reading-beta/held.edn"))))
(defn families [r]
  (let [reading (:reading r) squares (:squares r)
        a (#'family/without-void-citations (:analysis reading) (set (keys (:voids squares))))
        ps (#'family/deduplicate (#'family/reading-policies "M-typed-memories" a
                                  (into {} (map (fn [[k v]] [k (:square v)])) (:entries squares))))]
    [{:status :computed :target-id "M-typed-memories" :policies ps
      :provenance (select-keys reading [:analysis-digest :excerpt-digest])}]))
(deftest held-input-certification-and-consumer
  (let [r (frozen) fs (families r) predecessor (:predecessor r)
        opts {:q6/predecessor predecessor :token-belief-predecessor-trace {:decision (:prior-decision r)}
              :outer/makeup-path "data/wm-learning/outer-makeup.edn"}
        actual cs/selection-posterior select policy/select-action-cascades seen (atom [])
        result (with-redefs [precision/declaration-for (fn [& _] (throw (ex-info "campaign called" {})))
                             policy/select-action-cascades
                             (fn [menu options]
                               (with-redefs [cs/selection-posterior
                                             (fn [x] (swap! seen conj x) (actual x))]
                                 (select menu options)))]
                 (selection/select-over-families fs
                   {
                    :bind-cascade-identity {:readings #(decision/occurrence-readings % (:reading-pin %))}
                    :certify (#'decision/certify-reading-family (:field r) fs nil opts)}))
        c (:certification result) carry (get-in c [:receipt :reading-precision-carry])]
    (is (= :certified (:status c)) (pr-str (select-keys result [:status :refusal])))
    (is (= :selected (:status result)))
    (is (= (:refused-identity r) (:identity carry)))
    (is (:identity-changed carry))
    (is (= (:beta-prior (affect/valence-carry predecessor nil)) (:beta-prior carry)))
    (is (= (:beta-posterior carry) (get-in result [:decision :beta :value])))
    (is (some #(and (= (:beta-posterior carry) (:beta %))
                    (= (:candidates (:prepared-selection c)) (:candidates %))) @seen))
    (is (= :r20-beta-from-valence (:authority carry)))
    (let [d (update (:decision result) :selection-certificate merge (:receipt c))]
      (is (nil? (#'gate/check-beta! d)))
      (is (thrown? clojure.lang.ExceptionInfo (#'gate/check-beta! (assoc-in d [:beta :value] 99))))
      (is (thrown? clojure.lang.ExceptionInfo (#'gate/check-beta! (update d :selection-certificate dissoc :reading-precision-carry)))))
    (is (= (:beta-posterior carry)
           (:beta-posterior (affect/valence-carry predecessor (get-in c [:receipt :precision-row])))))))
(deftest invalid-q-refuses
  (let [r (frozen) fs (families r)
        result (selection/select-over-families fs
                 {
                    :bind-cascade-identity {:readings #(decision/occurrence-readings % (:reading-pin %))}
                  :certify (#'decision/certify-reading-family (:field r) fs nil
                             {:q6/predecessor {:status :present :previous {:valence-carry {:q-after {:status :absent :reason :retained-test-q-failure}}}}
                              :outer/makeup-path "data/wm-learning/outer-makeup.edn"})})]
    (is (= :refused (:status result)))
    (is (= :valence-beta-unavailable (get-in result [:refusal :carry :reason])) (pr-str (:refusal result)))
    (is (nil? (get-in result [:refusal :beta])))))
