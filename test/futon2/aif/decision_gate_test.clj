(ns futon2.aif.decision-gate-test
  "E1 decision-gate fixtures (SPEC-flat-removal-and-cascade-decision, 2026-09-17).

  The admissible cascade case is built with the REAL
  policy/select-action-cascades over tick 1's four candidates and G
  (p4ng wm-walkthroughs/build-loop/vm/tick-001/06-R5.edn :computed :G,
  C0 13.101074797244184, C1 = C2 11.434408130577516,
  C3 12.101074797244184, common T 3), with construction and interpretation
  receipts added to every candidate. Every rejection asserts the thrown
  :reason, not just that something threw."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.decision-gate :as gate]
            [futon2.aif.policy :as policy]))

;; --- tick 1 (06-R5.edn) candidates, with receipts --------------------------

(def ^:private tick1-G
  "G at the declared common T = 3, transcribed from 06-R5.edn :computed :G."
  {:C0-empty 13.101074797244184
   :C1-test-first 11.434408130577516
   :C2-fix-first 11.434408130577516
   :C3-fix-only 12.101074797244184})

(defn- cascade-action
  [cascade-id precedence]
  (cond-> {:kind :cascade-candidate
           :cascade-id cascade-id
           :precedence (vec precedence)
           ;; a construction receipt per candidate (H7's runtime will emit the
           ;; real ones; the gate only requires presence)
           :construction-receipt {:cascade-id cascade-id
                                  :moves (count precedence)}
           ;; one interpretation receipt per pattern in the precedence
           :interpretation-receipts (mapv (fn [p] {:pattern p :admitted true})
                                          precedence)}
    (empty? precedence)
    (assoc :interpretation-receipts [])))

(defn- tick1-entries
  []
  [{:action (cascade-action :C0-empty []) :controller-score (:C0-empty tick1-G)}
   {:action (cascade-action :C1-test-first [:pattern-test :pattern-sov])
    :controller-score (:C1-test-first tick1-G)}
   {:action (cascade-action :C2-fix-first [:pattern-ph :pattern-test :pattern-sov])
    :controller-score (:C2-fix-first tick1-G)}
   {:action (cascade-action :C3-fix-only [:pattern-sov])
    :controller-score (:C3-fix-only tick1-G)}])

(def ^:private tick1-beta 0.25)

(defn- tick1-decision
  []
  (policy/select-action-cascades (tick1-entries) {:beta tick1-beta}))

(defn- refusal-of
  [thunk]
  (try
    (thunk)
    (catch clojure.lang.ExceptionInfo e
      (let [d (ex-data e)]
        (when (not= :inadmissible-decision (:error d))
          (throw (ex-info "wrong error type" {:got (:error d)})))
        (:reason d)))))

;; --- admissible ------------------------------------------------------------

(deftest admissible-cascade-decision-passes-the-gate
  (testing "tick 1's four candidates through the real select-action-cascades,
            receipts on every candidate: emit! returns the decision unchanged"
    (let [decision (tick1-decision)]
      (is (map? decision))
      (is (= :cascade-selection-posterior
             (get-in decision [:selection-law :applied])))
      (is (= decision (gate/emit! decision))))))

(deftest admissible-typed-abstention-passes-the-gate
  (testing "an abstention with a non-empty per-target refusal list at an
            allowed kind is returned unchanged"
    (let [abstention {:status :abstained
                      :refusals
                      [{:target "M-foo" :kind :want-not-declared
                        :clause "no want tokens declared"}
                       {:target "M-bar" :kind :beta-not-declared}]}]
      (is (= abstention (gate/emit! abstention))))))

;; --- rejections ------------------------------------------------------------

(deftest flat-action-decision-is-refused
  (testing "the removed flat grain: a controller-head style flat decision throws"
    (is (= :flat-action
           (refusal-of
            #(gate/emit! {:action {:type :advance-ticket :target "T-42"}
                          :selection-law {:applied :controller-head}}))))))

(deftest single-pattern-cascade-without-receipts-is-refused
  (testing "a cascade decision whose candidates carry no receipts throws"
    (let [bare {:action {:kind :cascade-candidate :cascade-id :C9
                         :precedence [:pattern-solo]}
                :controller-score 1.0}
          decision (-> (policy/select-action-cascades [bare] {:beta 1.0})
                       (assoc :beta {:value 1.0 :status :declared}))]
      (is (= :missing-construction-receipt
             (refusal-of #(gate/emit! decision)))))))

(deftest interpreted-guard-tokens-must-retain-observation-locators
  (let [target "M-observed"
        token [target :premise]
        pattern {:id :p/observed
                 :target target
                 :guard {:status :interpreted :operator :and
                         :clauses [{:status :interpreted
                                    :present #{token} :absent #{}}]}
                 :produces #{[target :result]}}
        candidate {:kind :cascade-candidate :id :C1 :target target
                   :precedence [pattern]
                   :construction-receipt {:moves 1}
                   :interpretation-receipts
                   {:p/observed {:reading "Observe the premise."}}}
        decision (policy/select-action-cascades
                  [{:action candidate :controller-score 1.0}]
                  {:beta 1.0})]
    (is (= :missing-observation-locators
           (refusal-of #(gate/emit! decision))))
    (let [located (assoc candidate :observation-locators
                         {token {:class :C6 :repo "futon2" :sha "HEAD"
                                 :path "evidence.edn"}})
          located-decision (policy/select-action-cascades
                            [{:action located :controller-score 1.0}]
                            {:beta 1.0})]
      (is (= located-decision (gate/emit! located-decision))))))

(deftest bare-abstention-with-empty-refusals-is-refused
  (testing "an abstention that lists no refusals throws"
    (is (= :empty-refusals
           (refusal-of #(gate/emit! {:status :abstained :refusals []}))))))

(deftest chosen-mass-not-the-marginal-is-refused
  (testing "the chosen action's recorded mass is the mass of ONE cascade while
            two share the action: not the marginal of the posterior"
    (let [shared-first {:kind :cascade-candidate :cascade-id :A
                        :precedence [:pattern-test :pattern-sov]
                        :construction-receipt {:moves 2}
                        :interpretation-receipts [{:pattern :pattern-test}
                                                  {:pattern :pattern-sov}]}
          also-shared {:kind :cascade-candidate :cascade-id :B
                       :precedence [:pattern-test]
                       :construction-receipt {:moves 1}
                       :interpretation-receipts [{:pattern :pattern-test}]}
          decision (policy/select-action-cascades
                    [{:action shared-first :controller-score 1.0}
                     {:action also-shared :controller-score 2.0}]
                    {:beta 1.0})
          single-mass (get (get-in decision [:selection-law :posterior])
                            (:action decision))
          forged (assoc decision :chosen-action-mass single-mass)]
      (is (number? single-mass))
      (is (= :chosen-mass-not-marginal
             (refusal-of #(gate/emit! forged)))))))

(deftest missing-beta-is-refused
  (testing "a cascade decision with no recorded β throws"
    (let [decision (dissoc (tick1-decision) :beta)]
      (is (= :beta-not-recorded
             (refusal-of #(gate/emit! decision)))))))

;; --- claude-4 review additions -------------------------------------------

(deftest flat-action-dressed-as-cascade-decision-is-refused
  (testing "a flat action carried under a cascade selection law is not one of the posterior's candidates"
    (let [decision (assoc (tick1-decision)
                          :action {:type :advance-ticket :target "T-42"})]
      (is (= :chosen-action-not-a-candidate
             (refusal-of #(gate/emit! decision)))))))

(deftest non-bayes-choice-is-refused
  (testing "choosing a first acting pattern with less marginal mass than another throws,
            even when its own marginal is recorded correctly"
    (let [decision (tick1-decision)
          posterior (get-in decision [:selection-law :posterior])
          c0 (some #(when (= :C0-empty (:cascade-id %)) %) (keys posterior))
          c3 (some #(when (= :C3-fix-only (:cascade-id %)) %) (keys posterior))
          low (if (seq (:precedence c3)) c3 c0)
          forged (assoc decision :action low :chosen-action-mass
                        (reduce + (for [[c p] posterior
                                        :when (= (first (:precedence c)) (first (:precedence low)))]
                                    p)))]
      (is (= :chosen-not-bayes-action
             (refusal-of #(gate/emit! forged)))))))

(deftest posterior-over-non-cascade-is-refused
  (testing "a recorded posterior whose support includes a flat action throws"
    (let [decision (tick1-decision)
          posterior (get-in decision [:selection-law :posterior])
          [c p] (first posterior)
          forged (assoc-in decision [:selection-law :posterior]
                           (-> posterior (dissoc c) (assoc {:type :advance-ticket :target "T-42"} p)))]
      (is (= :posterior-over-non-cascade
             (refusal-of #(gate/emit! forged)))))))

;; --- empty cascades take no action mass (2026-09-21) -----------------------

(deftest gate-and-selector-agree-on-what-counts-as-an-action
  ;; The gate recomputes the marginal INDEPENDENTLY and shares no code with
  ;; the selector -- deliberately, since not trusting the selector's outputs is
  ;; the whole point of the gate. That independence is also how the two drifted:
  ;; excluding empty cascades in policy.clj alone made the gate refuse a
  ;; correct decision with :chosen-not-bayes-action (chosen-marginal 0.17286
  ;; vs best 0.79267, caught pre-click on 2026-09-21). This pins that the two
  ;; private rules still classify the same things as actions.
  (let [selector-rule @#'policy/cascade-first-action
        gate-rule @#'gate/first-acting-pattern
        cases [(cascade-action :empty [])
               (cascade-action :one [:pattern-sov])
               (cascade-action :many [:pattern-ph :pattern-test])
               {:kind :no-op :type :abstain/stand-down}]]
    (doseq [a cases]
      (is (= (selector-rule a) (gate-rule a))
          (str "selector and gate must agree on the first acting action of " (pr-str a))))
    (is (nil? (selector-rule (cascade-action :empty [])))
        "an empty cascade has no acting action, under both rules")
    (is (some? (selector-rule {:kind :no-op :type :abstain/stand-down}))
        "a TYPED no-op does have one, so it keeps its own key")))

(deftest gate-refuses-a-chosen-empty-cascade
  ;; The exact bad case: a decision naming an empty cascade as its action.
  ;; Before this rule the gate admitted it, because nil was just another key.
  (let [decision (tick1-decision)
        empty-action (cascade-action :C0-empty [])
        posterior (get-in decision [:selection-law :posterior])
        forged (-> decision
                   (assoc :action empty-action)
                   (assoc :chosen-action nil)
                   (assoc :chosen-action-mass (get posterior empty-action)))]
    (is (= :chosen-action-is-not-an-action
           (refusal-of #(gate/emit! forged)))
        "an empty cascade cannot be the enacted step")))

(deftest gate-excludes-empty-cascades-from-the-marginal
  ;; The pooled-nil inversion, at the gate: with the empty candidate's mass
  ;; excluded, the acting candidate the selector chose must still be the Bayes
  ;; action rather than losing to a nil key.
  (let [decision (tick1-decision)
        posterior (get-in decision [:selection-law :posterior])
        empty-mass (->> posterior
                        (filter (fn [[a _]] (empty? (:precedence a))))
                        (map val)
                        (reduce + 0.0))]
    (is (pos? empty-mass) "the fixture really does contain a scoring empty cascade")
    (is (some? (:chosen-action decision)) "the selector chose an acting cascade")
    (is (= decision (gate/emit! decision))
        "and the gate admits it rather than preferring the excluded nil mass")))
