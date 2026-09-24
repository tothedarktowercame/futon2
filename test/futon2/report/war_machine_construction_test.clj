(ns futon2.report.war-machine-construction-test
  "The tick constructs candidates from admitted interpretations
  (E-cascade-real D4) with the lane's own G (D12). Fixture: the
  M-aif-policy-conditioned-eig declared source as loaded at futon2 b32ac3be,
  with the universe its locators read that day (three wants, two already
  checked, h42fceb4ad48b open). The declared candidates produce only the two
  checked wants, so they cannot advance the target."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.interpretation-construction :as ic]
            [futon2.report.war-machine :as wm]))

(def target "M-aif-policy-conditioned-eig")

(def universe
  {:admission/task-stated true
   :hole/h6378c65a4012 true
   :hole/h0e270aa090bc true
   :hole/h42fceb4ad48b false})

(defn- sources []
  (-> (cs/with-context-fn (cs/load-declared cs/default-dir))
      (assoc-in [:universes target] universe)
      (assoc :horizon-steps 2
             :construction {:construct ic/construct
                            :budget {:max-moves 4 :max-expansions 20000}
                            :move-cost 0
                            :evaluate-g wm/constructed-candidate-g})))

(defn- only-target [srcs] (cp/assemble {:targets [target] :sources srcs}))

(deftest m-aif-eig-constructs-the-open-want
  (let [{:keys [problems refusals]} (only-target (sources))
        c (first (mapcat :constructed-candidates problems))]
    (is (empty? refusals) (pr-str refusals))
    (is (= [:aif/two-layer-calibration] (:precedence c)))
    (is (= :machine-constructed (get-in c [:construction-receipt :kind])))
    (is (= [] (get-in c [:construction-receipt :unreached-wants])))
    (testing "the problem's executable family is the constructed order only"
      (is (= [[:aif/two-layer-calibration]]
             (get-in (first problems) [:cascade-problem :precedences]))))))

(deftest constructed-g-beats-the-empty-cascade
  ;; the constructor takes a plan only if the lane's G prefers it
  (let [problem (-> (only-target (sources)) :problems first :cascade-problem (dissoc :precedences))
        g-plan (wm/constructed-candidate-g problem {:precedence [:aif/two-layer-calibration]})
        g-empty (wm/constructed-candidate-g problem {:precedence []})]
    (is (< g-plan g-empty) [g-plan g-empty])))

(deftest positive-move-cost-declines-the-plan
  ;; the bad case D12 names: a move cost in G's units larger than the
  ;; improvement (about 0.33 here) makes the constructor decline
  (let [{:keys [problems refusals]}
        (only-target (assoc-in (sources) [:construction :move-cost] 1))]
    (is (empty? problems))
    (is (= :construction-not-taken
           (get-in (first refusals) [:constructor-refusal :kind])))))

(deftest refusal-is-not-masked-by-idle-declared-candidates
  ;; bad case: construction refuses (no pattern produces the open want), and
  ;; the declared candidates, which advance nothing, must not be selected
  ;; in its place
  (let [srcs (update-in (sources) [:interpretations target :patterns]
                        dissoc :aif/two-layer-calibration)
        {:keys [problems refusals]} (only-target srcs)]
    (is (empty? problems) (pr-str (map :constructed-candidates problems)))
    (is (= :no-constructed-candidate (:kind (first refusals))))))
