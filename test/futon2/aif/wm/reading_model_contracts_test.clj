(ns futon2.aif.wm.reading-model-contracts-test
  "The Q5 one-selector route consumes the certified precision and preference
  contracts: reading-policy-preference certifies every scored record's one C
  and reading-policy-precision decides beta continuity over the identity of
  what was scored, both before the one selector call. A refusal from either
  abstains typed with the selector never called."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.policy-precision-carry :as old-carry]
            [futon2.aif.reading-policy-precision :as precision]
            [futon2.aif.wm.family-selection :as family-selection]
            [futon2.aif.wm.reading-family-decision-test :as rfd])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)
(use-fixtures :each
  (fn [run]
    (with-redefs [ledger/default-root
                  (.getPath (.toFile (Files/createTempDirectory
                                      "reading-contracts-ledger-"
                                      (make-array FileAttribute 0))))]
      (run))))

(defn- run-decision
  "One production decision for M-current, optionally after PREDECESSOR (a
  previous decision), counting selector calls."
  ([fx] (run-decision fx nil))
  ([fx predecessor]
   (#'rfd/decide fx [(get-in fx [:rows :current])] {"M-current" 1}
                 (when predecessor
                   {:token-belief-predecessor-trace {:decision predecessor}}))))

(defn- cert [result] (get-in result [:decision :selection-certificate]))

(defn- refused-with [result]
  (let [r (get-in result [:decision :refusals 0])]
    [(:kind r) (:certification-kind r) (get-in r [:carry :reason])]))

(deftest certified-contracts-are-consumed
  (let [fx (#'rfd/fixture ["p/a" "p/c"])
        result (run-decision fx)
        c (cert result)
        carry (:reading-precision-carry c)]
    (is (= 1 (:futon2.aif.wm.reading-family-decision-test/selector-calls result)))
    (testing "precision: a sealed module receipt, consumed as the selector's beta"
      (is (= precision/carry-schema (:schema carry)))
      (is (precision/intact? carry))
      (is (= :initialized (:status carry)))
      (is (= (:beta carry) (get-in result [:decision :beta :value])))
      (is (= (:identity carry) (:reading-precision-identity c))))
    (testing "preference: one certified receipt per scored record, live C diagnostic only"
      (is (= (count (:candidates c)) (count (:preference-receipts c))))
      (is (every? #(= :certified (:status %)) (:preference-receipts c)))
      (is (every? #(= {:role :diagnostic-only :selected false} (:live-c %))
                  (:preference-receipts c)))
      (is (= (set (map (comp :id :id) (:candidates c)))
             (set (map :policy-id (:preference-receipts c))))))))

(deftest beta-carries-only-across-the-same-identity
  (let [fx (#'rfd/fixture ["p/a" "p/c"])
        first-run (:decision (run-decision fx))]
    (testing "the same scored family carries"
      (let [r (run-decision fx first-run)]
        (is (= :carried (:status (:reading-precision-carry (cert r)))))
        (is (= 1 (:futon2.aif.wm.reading-family-decision-test/selector-calls r)))))
    (testing "theta-only learning leaves the identity whole"
      (let [r (with-redefs [ledger/pattern-theta
                            (fn [_] {:status :recorded-trials :theta 13/16 :trials-count 7
                                     :successes 6 :identities [] :targets ["M-current"]})]
                (run-decision fx first-run))]
        (is (not= (map :g (get-in first-run [:selection-certificate :candidates]))
                  (map :g (:candidates (cert r))))
            "the theta change reached the scored G")
        (is (= :carried (:status (:reading-precision-carry (cert r)))))))
    (testing "a scorer version change divides it, before selection"
      (let [r (with-redefs [shape-g/scorer-version :wm/cascade-shape-g-score-policy-v2]
                (run-decision fx first-run))]
        (is (= 0 (:futon2.aif.wm.reading-family-decision-test/selector-calls r)))
        (is (= [:model-contract-refused :reading-precision-refused
                :reading-precision-identity-changed]
               (refused-with r)))))
    (testing "a kernel version change divides it, before selection"
      (let [r (with-redefs [family-selection/kernel-version :co-application-frontier-theta-v2]
                (run-decision fx first-run))]
        (is (= 0 (:futon2.aif.wm.reading-family-decision-test/selector-calls r)))
        (is (= :reading-precision-identity-changed (last (refused-with r))))))))

(deftest refused-precision-authority-never-selects
  (let [fx (#'rfd/fixture ["p/a" "p/c"])
        first-run (:decision (run-decision fx))
        carry (get-in first-run [:selection-certificate :reading-precision-carry])]
    (testing "a tampered receipt"
      (let [r (run-decision fx (assoc-in first-run [:selection-certificate :reading-precision-carry]
                                         (assoc carry :beta 9.0)))]
        (is (= 0 (:futon2.aif.wm.reading-family-decision-test/selector-calls r)))
        (is (= :previous-receipt-tampered (last (refused-with r))))))
    (testing "an old q0/class precision receipt"
      (let [old (old-carry/seal {:schema :wm/precision-carry-v1 :beta 1 :model-id "old"
                                 :beta-status :declared :consumed #{}})
            r (run-decision fx {:selection-certificate {:policy-precision-state old}})]
        (is (= 0 (:futon2.aif.wm.reading-family-decision-test/selector-calls r)))
        (is (= :old-class-q0-model-identity (last (refused-with r))))))))

(deftest refused-preference-authority-never-selects
  (let [fx (#'rfd/fixture ["p/a" "p/c"])
        score shape-g/score-policy]
    (testing "an absent preference authority"
      (let [r (with-redefs [shape-g/score-policy
                            (fn [p] (update (score p) :scorer-result
                                            #(vary-meta % dissoc :cascade-scoring)))]
                (run-decision fx))]
        (is (= 0 (:futon2.aif.wm.reading-family-decision-test/selector-calls r)))
        (is (= [:model-contract-refused :preference-not-certified]
               (take 2 (refused-with r))))
        (is (= :preference-authority-absent
               (get-in r [:decision :refusals 0 :refusals 0 :kind])))))
    (testing "two C schedules"
      (let [r (with-redefs [shape-g/score-policy
                            (fn [p] (let [s (score p)]
                                      (update s :preference-at-each-step
                                              #(update % 1 (fn [d] (update-vals d (constantly (/ 1.0 (count d)))))))))]
                (run-decision fx))]
        (is (= 0 (:futon2.aif.wm.reading-family-decision-test/selector-calls r)))
        (is (= :competing-preferences
               (get-in r [:decision :refusals 0 :refusals 0 :kind])))))
    (testing "a G that is not the scorer's terms plus the stated fit"
      (let [r (with-redefs [shape-g/score-policy (fn [p] (update (score p) :g + 0.5))]
                (run-decision fx))]
        (is (= 0 (:futon2.aif.wm.reading-family-decision-test/selector-calls r)))
        (is (= :g-composition-mismatch
               (get-in r [:decision :refusals 0 :refusals 0 :kind])))))))

(deftest identity-change-stays-refused-until-an-explicit-declaration
  ;; codex-68 ruling (b): a changed identity re-arms only on a sealed beta
  ;; declaration for exactly the new identity; an abstained tick does not
  ;; let the next one reinitialize from beta-by-context.
  (let [fx (#'rfd/fixture ["p/a" "p/c"])
        first-run (:decision (run-decision fx))
        changed (fn [predecessor & [declaration]]
                  (with-redefs [shape-g/scorer-version :wm/cascade-shape-g-score-policy-v2]
                    (#'rfd/decide fx [(get-in fx [:rows :current])] {"M-current" 1}
                                  (cond-> {:token-belief-predecessor-trace {:decision predecessor}}
                                    declaration (assoc :reading-precision-declaration declaration)))))
        calls :futon2.aif.wm.reading-family-decision-test/selector-calls
        tick-1 (changed first-run)
        refused-carry (:reading-precision-carry (cert tick-1))
        new-identity (get-in refused-carry [:rearm :identity])
        authority {:status :declared :version "operator declaration v1"}]
    (testing "tick 1 refuses and records what re-arms it"
      (is (= 0 (calls tick-1)))
      (is (= :refused (:status refused-carry)))
      (is (precision/intact? refused-carry))
      (is (= :beta-declaration-for-identity (get-in refused-carry [:rearm :condition])))
      (is (string? new-identity)))
    (testing "tick 2 without a declaration still refuses"
      (let [tick-2 (changed (:decision tick-1))]
        (is (= 0 (calls tick-2)))
        (is (= :abstained (get-in tick-2 [:decision :status])))
        (is (= :refused (get-in (cert tick-2) [:reading-precision-carry :status])))))
    (testing "declarations that do not fit refuse"
      (doseq [[label d] {:another-identity (precision/declare-beta (apply str (repeat 64 "e")) 1 authority)
                         :tampered (assoc (precision/declare-beta new-identity 1 authority) :beta 7)}]
        (testing (name label)
          (let [t (changed (:decision tick-1) d)]
            (is (= 0 (calls t)))
            (is (= :refused (get-in (cert t) [:reading-precision-carry :status])))))))
    (testing "a sealed declaration for the new identity re-arms selection"
      (let [declaration (precision/declare-beta new-identity 2 authority)
            tick-2 (changed (:decision tick-1) declaration)
            carry (:reading-precision-carry (cert tick-2))]
        (is (= 1 (calls tick-2)))
        (is (= :redeclared (:status carry)))
        (is (= 2 (:beta carry)))
        (is (= (:sha256 declaration) (:declaration-sha256 carry)))
        (is (= 2 (get-in tick-2 [:decision :beta :value])))
        (testing "and the tick after carries it"
          (let [tick-3 (changed (:decision tick-2))]
            (is (= 1 (calls tick-3)))
            (is (= :carried (:status (:reading-precision-carry (cert tick-3)))))))))))
