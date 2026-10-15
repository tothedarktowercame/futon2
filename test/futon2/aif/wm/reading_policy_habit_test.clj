(ns futon2.aif.wm.reading-policy-habit-test
  "Habit E for reading policies (Q5 habit packet): the enactment fold counts
  history under each policy's structural reading-policy key, the same key
  Prefix-F, the history census, the flight writer and the gate use, and the
  one G selector consumes exactly that fold. Zero history is neutral; history
  of another policy, arrangement or target contributes nothing; refused,
  unattributed or outside-the-policy enactments count nothing."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.enactment-habit :as eh]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.test-support.reading-route :as route]))

(use-fixtures :once hermetic/with-hermetic-stores)
(use-fixtures :each route/with-empty-learning-ledger)

(defn- receipt [click key & [verdict attempts]]
  (eh/increment {:click click :candidate :cas/a :attempts (or attempts [{:pattern "p/a"}])}
                key (or verdict [])))

(defn- rows-by-key [decision]
  (into {} (map (fn [c] [(admission/candidate-key (:id c)) c])
                (get-in decision [:selection-certificate :candidates]))))

(defn- posterior-by-key [decision]
  (into {} (map (fn [[c p]] [(admission/candidate-key c) p])
                (get-in decision [:selection-law :posterior]))))

(defn- run-with [fx fold]
  (route/decide fx 1 (if fold {:enactment-fold fold} {})))

(deftest same-structural-key-accumulates-e
  (let [fx (route/fixture)
        [k other] (vals (route/policy-keys (:decision (route/decide fx))))
        fold (eh/fold nil [(receipt "click-1" k) (receipt "click-2" k)])
        none (run-with fx nil)
        with (run-with fx fold)
        rows (rows-by-key (:decision with))]
    (is (= 1 (:selector-calls with)))
    (is (= {:source :enactment-fold :policy-key k :count 2 :samples 2}
           (select-keys (get-in rows [k :habit-provenance]) [:source :policy-key :count :samples])))
    (is (= 0 (get-in rows [other :habit-provenance :count])))
    (testing "the E the selector consumed is the fold's Dirichlet mass, alpha 1"
      ;; masses (n + alpha) normalised over the menu: k 3/4, other 1/4
      (is (< (Math/abs (- 0.75 (get-in rows [k :habit]))) 1e-12))
      (is (< (Math/abs (- 0.25 (get-in rows [other :habit]))) 1e-12))
      (let [before (posterior-by-key (:decision none))
            after (posterior-by-key (:decision with))]
        (is (< (Math/abs (- 3.0 (/ (/ (get after k) (get after other))
                                   (/ (get before k) (get before other)))))
               1e-9)
            "the posterior odds move by exactly the E ratio, (2+1)/(0+1)")))))

(deftest zero-history-is-neutral
  (let [fx (route/fixture)
        rows (rows-by-key (:decision (run-with fx nil)))]
    (is (apply = (map :habit (vals rows))))
    (is (every? #(= 0 (get-in % [:habit-provenance :count])) (vals rows)))))

(deftest theta-only-learning-does-not-split-the-key
  (let [fx (route/fixture)
        [k] (vals (route/policy-keys (:decision (route/decide fx))))
        fold (eh/fold nil [(receipt "click-1" k)])
        learned (with-redefs [ledger/pattern-theta
                              (fn [_] {:status :recorded-trials :theta 13/16 :trials-count 7
                                       :successes 6 :identities [] :targets [route/target]})]
                  (run-with fx fold))
        rows (rows-by-key (:decision learned))]
    (is (contains? rows k) "the same structural key after a theta change")
    (is (= 1 (get-in rows [k :habit-provenance :count])))))

(deftest history-of-another-identity-does-not-count
  (let [fx (route/fixture)
        [k] (vals (route/policy-keys (:decision (route/decide fx))))
        [tag target id structure] k
        other-arrangement [tag target id (update structure :edges conj ["p/a" "p/a" :overlap])]
        other-target [tag "M-elsewhere" (str "M-elsewhere/policy-" (subs id (count (str target "/policy-")))) structure]
        other-policy [tag target (str target "/policy-0000000000000000ffff") structure]]
    (doseq [[label key] {:arrangement other-arrangement :target other-target :policy other-policy}]
      (testing (name label)
        (let [fold (eh/fold nil [(receipt "click-1" key)])
              rows (rows-by-key (:decision (run-with fx fold)))]
          (is (= 1 (:samples fold)) "the fold counted it, under its own key")
          (is (= 0 (get-in rows [k :habit-provenance :count]))
              "it contributes nothing to this policy"))))))

(deftest refused-unattributed-and-outside-history-count-nothing
  (let [fx (route/fixture)
        [k] (vals (route/policy-keys (:decision (route/decide fx))))
        refused (receipt "click-1" k ["W_c: no G_c pass"])
        ;; the flight writer's refusal when an enactment has no reading key
        unattributed (receipt "click-2" {:status :refused :reason :reading-policy-key-refused})
        outside (receipt "click-3" k [] [{:pattern "p/not-in-policy"}])
        fold (eh/fold nil [refused unattributed outside])
        rows (rows-by-key (:decision (run-with fx fold)))]
    (is (= 0 (:delta refused)))
    (is (= :attempt-outside-candidate (:reason outside)))
    (is (= :refused (:status unattributed)))
    (is (= 0 (:samples fold)))
    (is (= 0 (get-in rows [k :habit-provenance :count])))))
