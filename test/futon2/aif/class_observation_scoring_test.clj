(ns futon2.aif.class-observation-scoring-test
  "PROOF-wm-works 1.3, handoff A (codex-20 corrections): the class
  observation model scores the reference input's real candidates through the
  real loaders, the real qualifier (replicated from war_machine.clj:6145)
  and the real bounded scorer rank-cascade-actions. No stubs."
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-observation-scoring :as cos]
            [futon2.aif.cascade-policy :as cpol]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.observation-model :as om]
            [futon2.aif.scoring-input-receipts :as ir]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as war-machine]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")
(def joe-c {:focused 55/100 :related 35/100 :unrelated 5/100 :stop-the-line 5/100})

(defn- reference-family []
  (let [sources (cs/with-context-fn (cs/load-declared))
        assembled (cp/assemble {:targets [t] :sources (assoc sources :horizon-steps 4)})
        problems (:problems assembled)
        qualification (fn [target token] [target token])
        joint-candidates
        (vec (mapcat (fn [problem]
                       (let [t2 (:target problem)
                             cp2 (:cascade-problem problem)
                             qual (partial qualification t2)
                             patterns (into {}
                                            (map (fn [[id {:keys [guard produces]}]]
                                                   [id (-> (cpol/token-interpretation
                                                            id {:guard {:needs (set (map qual (:needs guard)))
                                                                        :forbids (set (map qual (:forbids guard)))}
                                                                :produces (set (map qual produces))})
                                                           (assoc :target t2))]))
                                            (:interpretations cp2))]
                         (mapv (fn [{:keys [candidate-id precedence]}]
                                 {:kind :cascade-candidate :id candidate-id :target t2
                                  :precedence (mapv patterns precedence)})
                               (:constructed-candidates problem))))
                     problems))
        q0 (:value (ir/initial-belief problems))
        universe (reduce (fn [acc c]
                           (reduce (fn [a pattern]
                                     (reduce conj a (concat (:produces pattern)
                                                            (mapcat #(concat (:present %) (:absent %))
                                                                    (get-in pattern [:guard :clauses])))))
                                   acc (:precedence c)))
                         (reduce set/union
                                 (set (for [[f _v] (get-in (first problems) [:cascade-problem :facts])] [t f]))
                                 (keys q0))
                         joint-candidates)]
    {:problems problems :candidates joint-candidates :q0 q0 :universe universe}))

(defn- class-model [{:keys [universe acceptance horizon target-class]}]
  (let [not-yet :ending/not-yet-evaluated]
    {:schema :wm/observation-model-v1 :backend :exact-enumeration :kind :class-emission
     :universe universe :horizon horizon
     :class-universe [:focused :related :unrelated :stop-the-line not-yet]
     :acceptance acceptance
     :target-class (or target-class {t :focused})
     :class-preference (into {} (for [tau (range 1 (inc horizon))]
                                  [tau (if (= tau horizon) joe-c {not-yet 1})]))
     :provenance {:status :synthetic :calibrated false
                  :source "PROOF-wm-works 1.3; Joe 2026-09-22 ruling (55/35/5/5; unmeasured -> stop-the-line)"}}))

(defn- rank [q0 candidates model horizon acceptance]
  (cos/rank-cascade-actions
   {:cascade-belief q0} candidates
   {:observation-model model
    :horizon-steps horizon
    :prediction-context {:occurrence-id "test-occ" :tau horizon}
    :cascade-spec {:want acceptance :evidence #{} :zeroed #{}}}))

(defn- steps-of [entry]
  (get-in entry [:certificate :steps]))

(deftest reference-input-g-and-unique-maximum
  (let [{:keys [candidates q0 universe]} (reference-family)
        acceptance #{[t :restoration-accepted]}
        model (class-model {:universe universe :acceptance acceptance :horizon 4})
        ranked (rank q0 candidates model 4 acceptance)
        by-id (into {} (map (juxt :cascade-id identity)) ranked)
        g1 (:controller-score (by-id :C1))
        g2 (:controller-score (by-id :C2))]
    (is (vector? ranked) (pr-str (if (map? ranked) (dissoc ranked :candidates) ranked)))
    ;; C1 never reaches acceptance: exactly one terminal ln 20, not H x ln 20.
    (is (< (Math/abs (- g1 (Math/log 20))) 0.001) (str "G(C1)=" g1 " expected ln20"))
    ;; C2 reaches restoration on the focused target.
    (is (< (Math/abs (- g2 (Math/log (/ 1 0.55)))) 0.001) (str "G(C2)=" g2 " expected ln(1/.55)"))
    ;; unique maximum: strictly lower G, every intermediate step exactly 0.
    (is (< g2 g1))
    (doseq [entry ranked
            step (steps-of entry)
            :when (< (:tau step) 4)]
      (is (zero? (:g step)) (str "intermediate tau " (:tau step) " g=" (:g step))))
    (is (every? #(pos? (:g %)) (filter #(= 4 (:tau %)) (steps-of (by-id :C1)))))))

(deftest two-step-never-accepted-is-one-ln20-not-two
  ;; A two-step rollout that never accepts: only the terminal step carries
  ;; risk (codex-20's probe showed 2*ln20 before the tau fix).
  (let [ta "A" tb "B"
        step (cpol/token-interpretation :p {:guard {:needs #{[ta :s]} :forbids #{[ta :x]}}
                                          :produces #{[ta :x]}})
        q0 {#{[ta :s]} 1}
        candidates [{:kind :cascade-candidate :id :C1 :target ta
                     :precedence [(assoc step :id :p :target ta)]}]
        universe #{[ta :s] [ta :x] [ta :done]}
        model (class-model {:universe universe :acceptance #{[ta :done]} :horizon 2
                            :target-class {ta :focused}})
        ranked (rank q0 candidates model 2 #{[ta :done]})]
    (is (vector? ranked) (pr-str (if (map? ranked) (dissoc ranked :candidates) ranked)))
    (is (< (Math/abs (- (:controller-score (first ranked)) (Math/log 20))) 0.001)
        (str "G=" (:controller-score (first ranked)) " expected ln20 exactly once"))
    (is (every? zero? (map :g (butlast (steps-of (first ranked)))))
        "tau 1 is before T=2: its g must be exactly 0")))

(deftest accepted-before-t-scores-zero-before-t
  ;; A state that already holds the acceptance token before the horizon
  ;; still emits :ending/not-yet-evaluated at tau < T (codex-20: acceptance
  ;; checked first made G infinite before the fix).
  (let [ta "A"
        q0 {#{[ta :s] [ta :done]} 1}
        universe #{[ta :s] [ta :done]}
        model (class-model {:universe universe :acceptance #{[ta :done]} :horizon 3
                            :target-class {ta :focused}})
        score (om/query model {:op :score :belief q0 :tau 2 :target ta
                               :preference {:ending/not-yet-evaluated 1}})]
    (is (= :computed (:status score)) (pr-str (dissoc score :model)))
    (is (zero? (:g score)) (str "g=" (:g score)))
    (is (= {:ending/not-yet-evaluated 1} (:prediction score)))))

(deftest attributable-acceptance-is-the-candidates-own-target
  ;; A state holding ANOTHER target's accepted token is still stop-the-line
  ;; for THIS candidate; no averaging, deterministic emission.
  (let [ta "A" tb "B"
        state #{[ta :s] [tb :done]}
        universe #{[ta :s] [ta :done] [tb :done]}
        model (class-model {:universe universe :acceptance #{[ta :done] [tb :done]}
                            :horizon 1 :target-class {ta :focused tb :related}})
        score (om/query model {:op :score :belief {state 1} :tau 1 :target ta
                               :preference joe-c})]
    (is (= :computed (:status score)) (pr-str (dissoc score :model)))
    (is (= {:stop-the-line 1} (:prediction score))
        "other targets' acceptance never counts as this candidate's ending")))

(deftest two-targets-different-classes-and-the-swap
  ;; Two candidates on two targets whose accepted outcomes class differently;
  ;; E and F equal, so the maximum must follow the class preference, and
  ;; swapping the class assignments must swap the maximum.
  (let [ta "A" tb "B"
        mk (fn [target]
             (let [pat (cpol/token-interpretation :p {:guard {:needs #{[target :s]}
                                                              :forbids #{[target :done]}}
                                                    :produces #{[target :done]}})]
               {:kind :cascade-candidate :id :C1 :target target
                :precedence [(assoc pat :id :p :target target)]}))
        q0 {#{[ta :s] [tb :s]} 1}
        universe #{[ta :s] [ta :done] [tb :s] [tb :done]}
        acceptance #{[ta :done] [tb :done]}
        run (fn [target-class]
              (let [model (class-model {:universe universe :acceptance acceptance :horizon 1
                                        :target-class target-class})
                    ranked (rank q0 [(mk ta) (mk tb)] model 1 acceptance)]
                (is (vector? ranked) (pr-str (if (map? ranked) (dissoc ranked :candidates) ranked)))
                {:winner (get-in (first ranked) [:action :target])
                 :scores (mapv (juxt (fn [e] (get-in e [:action :target])) :controller-score) ranked)}))
        r1 (run {ta :focused tb :unrelated})]
    ;; focused beats unrelated by Joe's own numbers
    (is (= ta (:winner r1)))
    (let [[_a ga] (first (:scores r1)) [_b gb] (second (:scores r1))]
      (is (< (Math/abs (- ga (Math/log (/ 1 0.55)))) 0.001) (str "focused G=" ga))
      (is (< (Math/abs (- gb (Math/log 20))) 0.001) (str "unrelated G=" gb)))
    ;; ...and the swap flips it
    (let [r2 (run {ta :unrelated tb :focused})]
      (is (= tb (:winner r2))))))

(deftest unnormalised-class-preference-refuses
  ;; The acceptance token is in the universe, so the model itself validates
  ;; and the only defect is the preference total (codex-20 review of
  ;; 62fcfa1e: the earlier version failed on :invalid-class-acceptance and
  ;; one "bad" case summed to 1).
  (let [ta "A"
        model (class-model {:universe #{[ta :s] [ta :done]} :acceptance #{[ta :done]}
                            :horizon 1 :target-class {ta :focused}})
        joe {:focused 55/100 :related 35/100 :unrelated 5/100 :stop-the-line 5/100}
        score (fn [pref]
                (om/query (assoc-in model [:class-preference 1] pref)
                          {:op :score :belief {#{[ta :s]} 1} :tau 1 :target ta
                           :preference pref}))]
    (is (= :computed (:status (score joe))) "normalised control passes")
    (doseq [bad [{:focused 0 :related 0 :unrelated 0 :stop-the-line 0}
                 {:focused 1/4 :related 1/4 :unrelated 0 :stop-the-line 0}]]
      (let [r (try (score bad) (catch clojure.lang.ExceptionInfo e (ex-data e)))]
        (is (= :invalid-class-preference (:kind r))
            (pr-str (dissoc r :model)))))))

(deftest posterior-serialisation-keeps-both-targets-c1
  ;; handoff A(b): two targets both naming :C1 must serialise as two distinct
  ;; posterior entries -- neither overwrites the other.
  (let [a {:kind :cascade-candidate :id :C1 :target "A" :precedence []}
        b {:kind :cascade-candidate :id :C1 :target "B" :precedence []}
        serialise (fn [posterior]
                        (let [strip @#'trace/strip-decision]
                          (get-in (strip {:status :cascade-selection-posterior-implied
                                          :selection-law {:applied :cascade-selection-posterior
                                                          :posterior posterior}})
                                  [:selection-law :posterior])))
        out (serialise {a 0.6 b 0.4})]
    (is (= 2 (count out)) (pr-str out))
    (is (= 0.6 (get out "A/:C1")))
    (is (= 0.4 (get out "B/:C1")))
    ;; targetless candidates keep the bare id
    (is (= {":C1" 1.0} (serialise {{:kind :cascade-candidate :id :C1 :precedence []} 1.0})))))

;; PROOF-wm-works 1.3 handoff B: target class via the target relation; an
;; unknown focus is recorded, never silently :unrelated; class provenance.
(deftest target-class-unknown-focus-is-never-unrelated
  (let [ta "A" tb "B"
        ;; two facets for tb -> ambiguous -> :unknown; unknown focus -> both :unknown
        classes @#'war-machine/facet-class-of-target]
    (is (= :unknown (classes {:status :unknown :focus nil :facet-graph {:background []}}
                             ["resources/wm/eig/x.edn"]))
        "unknown focus: the target class is :unknown, never :unrelated")
    (is (= :focused (classes {:status :retained :focus "WM" :facet-graph {:background ["APM"]}}
                             ["resources/wm/rechecks/x.edn"])))
    (is (= :related (classes {:status :retained :focus "WM" :facet-graph {:background ["APM"]}}
                             ["src/apm/thing.clj"])))
    (is (= :unknown (classes {:status :retained :focus "WM" :facet-graph {:background ["APM"]}}
                             ["resources/wm/eig/x.edn" "src/apm/thing.clj"]))
        "locators spanning two classes are ambiguous exactly as close refuses ambiguity")))

(deftest unknown-class-target-scores-stop-line-but-records-unknown
  (let [ta "A"
        model (class-model {:universe #{[ta :s] [ta :done]} :acceptance #{[ta :done]}
                            :horizon 1 :target-class {ta :unknown}})
        score (om/query model {:op :score :belief {#{[ta :s] [ta :done]} 1} :tau 1 :target ta
                               :preference joe-c})]
    (is (= :computed (:status score)) (pr-str (dissoc score :model)))
    (is (= {:stop-the-line 1} (:prediction score))
        "unknown class scores in the unmeasured bucket")
    (is (= :unknown (get-in score [:model :target-class ta]))
        "the record says :unknown, never :unrelated")))

(deftest swap-test-carries-class-provenance
  (let [ta "A" tb "B"
        mk (fn [target]
             (let [pat (cpol/token-interpretation :p {:guard {:needs #{[target :s]}
                                                              :forbids #{[target :done]}}
                                                    :produces #{[target :done]}})]
               {:kind :cascade-candidate :id :C1 :target target
                :precedence [(assoc pat :id :p :target target)]}))
        q0 {#{[ta :s] [tb :s]} 1}
        universe #{[ta :s] [ta :done] [tb :s] [tb :done]}
        acceptance #{[ta :done] [tb :done]}
        run (fn [target-class]
              (let [model (class-model {:universe universe :acceptance acceptance :horizon 1
                                        :target-class target-class})]
                [model (rank q0 [(mk ta) (mk tb)] model 1 acceptance)]))
        [m1 r1] (run {ta :focused tb :unrelated})
        [_m2 r2] (run {ta :unrelated tb :focused})]
    (is (= ta (get-in (first r1) [:action :target])))
    (is (= tb (get-in (first r2) [:action :target])) "the swap flips the maximum")
    ;; provenance: the class C and its ruling ride on the model and the spec
    (is (= :class-emission (:kind m1)))
    (is (= joe-c (get-in m1 [:class-preference 1])))
    (is (re-find #"Joe 2026-09-22" (get-in m1 [:provenance :source])))))
