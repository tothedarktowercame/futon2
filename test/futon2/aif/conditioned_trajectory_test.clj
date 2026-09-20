(ns futon2.aif.conditioned-trajectory-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.conditioned-trajectory :as trajectory]
            [futon2.aif.efe :as efe]
            [futon2.aif.exact-belief-adapter :as adapter]
            [futon2.aif.g-term-decomposition :as decomposition]))

(def states [#{} #{:judgement}])
(def symmetric {:judgement {:false-neg 1/4 :false-pos 1/4}})
(def asymmetric {:judgement {:false-neg 1/8 :false-pos 1/4}})
(def perfect {:judgement {:false-neg 0 :false-pos 0}})
(def uniform {#{} 1/2 #{:judgement} 1/2})
(def spec {:want #{:judgement} :lam 1 :mu 1 :evidence #{} :zeroed #{}})
(def mixing-pattern
  {:id :mix :theta 1/2 :produces #{:judgement}
   :guard {:status :interpreted
           :clauses [{:status :interpreted :present #{} :absent #{:judgement}}]}})

(def artifact-root "holes/labs/wm-contract/runs/d-token-carry-2b-2026-09-20/")
(def frozen-examples
  (:examples (edn/read-string (slurp (io/resource (str artifact-root "examples.edn"))))))
(defn sparse [q] (trajectory/nonzero-mass q))

(defn wrap-receipt [update universe observation tau]
  (let [refused? (= :refused (:status update))
        predicted (sparse (:predicted-state update))
        posterior (when-not refused? (sparse (:posterior update)))
        vacuous? (= predicted posterior)
        license (cond
                  refused? {:status :not-applicable :reason :impossible-observation}
                  vacuous? (dissoc (trajectory/vacuity-license predicted (:likelihoods update)) :likelihoods)
                  :else {:status :not-licensed :reason :likelihood-varies-on-support
                         :support (set (keys predicted))})
        obs {:status :observed :occurrence-id "Q-fixture" :tau tau
             :present observation :absent (set (remove observation universe))}]
    (cond-> {:schema :wm/token-belief-update-v1
             :occurrence-id "Q-fixture" :tau tau :observation obs
             :pre-belief (sparse (:prior update)) :predicted-belief predicted
             :continuation-belief (if refused? predicted posterior)
             :status (if refused? :refused :value) :consumed (not refused?)
             :observation-probability (:observation-probability update)
             :model (:model update)
             :carrier {:universe universe :states (vec (keys (:predicted-state update)))
                       :representation :support-plus-transition-closure}
             :predecessor {:scope :isolated-test :source :exact-adapter-fixture}
             :observation-authority {:channel :judgement :admission {:scope :isolated-test}}
             :vacuity-license license :z-semantics :per-step-redraw}
      refused? (assoc :policy :refused-observation-discarded :kind :belief-update-refused
                      :refused-trajectory update)
      (not refused?) (assoc :posterior posterior :calculation update :policy :conditioned-posterior))))

(defn receipt
  "Frozen D interface built from the real exact adapter, never a Bayes stub."
  [rates prior B observation tau]
  (wrap-receipt
   (adapter/exact-update states #(m/observation-distribution rates %) B observation prior
                         {:status :declared :parameter-basis :synthetic
                          :z-semantics :per-step-redraw})
   (set (keys rates)) observation tau))

(defn score [rates q receipt]
  (m/horizon-g-sparse-cert
   (cond-> {:rates rates :q0 q :precedence-fn (constantly [])
            :horizon 2 :spec spec :universe #{:judgement}}
     receipt (assoc :belief-update-receipt receipt))))

(defn q-value [result] (get-in result [:certificate :consumed-g :Q]))
(defn verdict [result] (decomposition/verdict :Q (q-value result)))
(defn close? [a b] (< (abs (- (double a) (double b))) 1e-12))

(defn comparison [rates]
  (let [components [{:weight 1/2 :rates perfect}
                    {:weight 1/2 :rates (update-vals rates #(update-vals % (partial * 2)))}]
        update (adapter/synthetic-mixture-update
                states components #(m/cascade-kernel [mixing-pattern] %) #{:judgement} {#{} 1})
        r (wrap-receipt update #{:judgement} #{:judgement} 1)]
    {:receipt r :conditioned (score rates (:continuation-belief r) r)
     :open-loop (score rates (:predicted-belief r) nil)}))

(defn demonstrations []
  (let [vacuous (receipt perfect {#{:judgement} 1} #(hash-map % 1) #{:judgement} 1)
        refused (receipt perfect {#{} 1} (constantly {#{:judgement} 1}) #{} 1)
        first-r (receipt symmetric uniform #(hash-map % 1) #{:judgement} 1)
        second-r (receipt symmetric (:posterior first-r) #(hash-map % 1) #{:judgement} 2)]
    {:identity (score perfect (:continuation-belief vacuous) vacuous)
     :symmetric (comparison symmetric) :asymmetric (comparison asymmetric)
     :refused (score perfect (:continuation-belief refused) refused)
     :threading {:first first-r :second second-r
                 :first-scored (score symmetric (:continuation-belief first-r) first-r)
                 :second-scored (score symmetric (:continuation-belief second-r) second-r)}}))

(deftest two-sided-q-and-same-source
  (let [{:keys [identity symmetric asymmetric refused threading]} (demonstrations)]
    (is (= :conditioning-vacuous (:reason (verdict identity))))
    (is (= :degenerate (:verdict (verdict identity))))
    (is (true? (get-in (q-value identity) [:observation-updates 0 :vacuous])))
    (doseq [{:keys [receipt conditioned open-loop]} [symmetric asymmetric]]
      (is (= :factorized-nonzero-rates (get-in conditioned [:certificate :evaluation])))
      (is (= :observation-conditioned (:reason (verdict conditioned))))
      (is (= :non-degenerate (:verdict (verdict conditioned))))
      (is (= :open-loop-no-conditioning (:reason (verdict open-loop))))
      (is (not (close? (:g conditioned) (:g open-loop))))
      (is (= receipt (get-in (q-value conditioned) [:observation-updates 0 :receipt])))
      ;; On a ONE-token universe, this declared two-component mixture IS the
      ;; per-token kernel consumed by sparse scoring; no coupled G is claimed.
      (doseq [s states]
        (is (= (get-in receipt [:calculation :observation-rows s])
               (m/observation-distribution (get-in conditioned [:certificate :rates]) s))))
      (doseq [[belief-step score-step] (map vector (:steps (q-value conditioned))
                                          (get-in conditioned [:certificate :steps]))]
        (let [q (:belief belief-step)
              rates (get-in conditioned [:certificate :rates])]
          (is (= (:posterior receipt) q))
          (is (close? (:risk score-step)
                      (m/outcome-risk (m/predict-observations rates q)
                                      (m/preference-distribution spec #{:judgement}))))
          (is (close? (:ambiguity score-step) (m/step-ambiguity rates q))))))
    (is (close? (get-in symmetric [:conditioned :certificate :steps 0 :ambiguity])
                (get-in symmetric [:open-loop :certificate :steps 0 :ambiguity])))
    (is (not (close? (get-in asymmetric [:conditioned :certificate :steps 0 :ambiguity])
                     (get-in asymmetric [:open-loop :certificate :steps 0 :ambiguity]))))
    (is (= :open-loop-no-conditioning (:reason (verdict refused))))
    (is (= :refused (get-in (q-value refused) [:observation-updates 0 :status])))
    (is (= :none (get-in (q-value refused) [:observation-updates 0 :receipt :refused-trajectory :option])))
    (is (number? (:g refused)))
    (is (= {#{:judgement} 1} (:initial-belief (q-value refused))))
    (is (= {#{} 1/4 #{:judgement} 3/4} (get-in threading [:first :posterior])))
    (is (= {#{} 1/10 #{:judgement} 9/10} (get-in threading [:second :posterior])))
    (is (= (get-in threading [:first :posterior]) (get-in threading [:second :pre-belief])))))

(deftest actual-bad-receipt-rejected
  (let [r (receipt symmetric uniform #(hash-map % 1) #{:judgement} 1)
        post (:posterior r)]
    (testing "real adapter posterior cannot be attached to open-loop input"
      (is (= :belief-update-consumption-mismatch
             (get-in (score symmetric uniform r) [:g :kind]))))
    (doseq [[changed kind]
            [[(assoc r :posterior uniform) :invalid-belief-update-continuation]
             [(assoc-in r [:observation :tau] 2) :invalid-belief-update-observation]
             [(assoc-in r [:observation :occurrence-id] "other") :invalid-belief-update-observation]
             [(assoc r :z-semantics :persistent) :invalid-belief-update-receipt]
             [(assoc r :status :invalid) :invalid-belief-update-receipt]]]
      (is (= kind (get-in (score symmetric post changed) [:g :kind]))))
    (is (= :missing (:status (decomposition/verdict :Q {:observation-updates [{}]}))))
    (is (= :missing (:status (decomposition/verdict :Q {}))))))

(deftest sequential-future-uses-varying-precedence-once
  (let [seen (atom [])
        precedence (fn [k] (swap! seen conj k) (if (zero? k) [mixing-pattern] []))
        args {:rates symmetric :q0 {#{} 1} :precedence-fn precedence
              :horizon 3 :spec spec :universe #{:judgement}}
        result (m/horizon-g-sparse-cert args)]
    (is (= [0 1 2] @seen))
    (is (= [uniform uniform uniform] (mapv :belief (:steps (q-value result)))))
    (reset! seen [])
    (is (close? (:g result) (m/horizon-g-sparse args)))
    (is (= [0 1 2] @seen))))

(deftest retains-correlated-belief-refusal
  (let [rates {:judgement {:false-neg 1/4 :false-pos 1/4}
               :other {:false-neg 1/4 :false-pos 1/4}}
        result (m/horizon-g-sparse-cert
                {:rates rates :q0 {#{} 1/2 #{:judgement :other} 1/2}
                 :precedence-fn (constantly []) :horizon 1 :spec spec
                 :universe #{:judgement :other}})]
    (is (= :non-factorizable-belief (get-in result [:g :kind])))
    (is (nil? (:certificate result)))))

(deftest recorded-tick-shape-identity-stays-degenerate
  ;; Six-token comparison domain and four established tokens from the recorded
  ;; tick-001 replay: vm-test/futon2/vm/tick_001_s06_r5_test.clj, T/q0/spec.
  (let [have #{:summary-without-total-repos-throws
               :active-repo-ratio-absent-default-is-0
               :coupling-density-reads-same-key-with-default
               :observe-empty-does-not-throw}
        want #{:summary-without-total-repos-observes-cleanly
               :active-repo-ratio-absent-default-is-0 :test-covers-missing-total-repos}
        universe (into have want)
        rates (zipmap universe (repeat {:false-neg 0 :false-pos 0}))
        q {have 1}
        update (adapter/exact-update [have] #(m/observation-distribution rates %)
                                     #(hash-map % 1) have q
                                     {:z-semantics :per-step-redraw})
        r (wrap-receipt update universe have 1)
        args {:rates rates :q0 q :horizon 3 :precedence-fn (constantly [])
              :spec (assoc spec :want want) :universe universe}
        baseline (m/horizon-g-sparse-cert args)
        conditioned (m/horizon-g-sparse-cert (assoc args :belief-update-receipt r))]
    (is (= 6 (count universe)))
    (is (= (:g baseline) (:g conditioned)))
    (is (= :conditioning-vacuous (:reason (verdict conditioned))))
    (is (= :degenerate (:verdict (verdict conditioned))))
    (is (every? #(= q (:belief %)) (:steps (q-value conditioned))))))

(deftest missing-zero-and-refusal-are-distinct
  (let [r (receipt perfect {#{:judgement} 1} #(hash-map % 1) #{:judgement} 1)
        missing (-> r (assoc :status :missing :consumed false :tau nil
                            :observation {:status :missing :reason :no-observation
                                          :occurrence-id "Q-fixture" :tau nil}
                            :policy :no-observation-prediction-only
                            :vacuity-license {:status :not-applicable :reason :no-observation})
                    (dissoc :posterior :calculation :observation-probability))
        result (score perfect (:continuation-belief missing) missing)]
    (is (= :missing (get-in (q-value result) [:conditioning-input :status])))
    (is (= :not-supplied
           (get-in (q-value (score perfect (:continuation-belief missing) nil))
                   [:conditioning-input :status])))
    (is (= [] (:observation-updates (q-value result))))
    (is (= :open-loop-no-conditioning (:reason (verdict result))))
    (is (= 0 (get-in result [:certificate :steps 0 :ambiguity])))
    (is (= :reduced-identically-zero (get-in result [:certificate :steps 0 :ambiguity-status])))))

(deftest reader-rejects-a-false-green-vacuity-claim
  (let [r (receipt perfect {#{:judgement} 1} #(hash-map % 1) #{:judgement} 1)
        value (q-value (score perfect (:continuation-belief r) r))]
    (is (= :conditioning-vacuous (:reason (decomposition/verdict :Q value))))
    (is (= :missing (:status (decomposition/verdict
                             :Q (assoc-in value [:observation-updates 0 :vacuous] false)))))
    (is (= :missing (:status (decomposition/verdict
                             :Q (assoc-in value [:observation-updates 0 :consumed] false)))))))

(deftest scoring-entry-passes-the-same-d-receipt
  (let [r (receipt symmetric uniform #(hash-map % 1) #{:judgement} 1)
        candidates [{:kind :cascade-candidate :id :noop :precedence []}]
        opts {:horizon-steps 2 :cascade-spec spec :adjudication-rates symmetric}
        state {:cascade-belief (:continuation-belief r)}
        base (first (efe/rank-cascade-actions state candidates opts))
        conditioned (first (efe/rank-cascade-actions
                            (assoc state :belief-update-receipt r) candidates opts))]
    (is (= r (get-in conditioned [:certificate :consumed-g :Q :observation-updates 0 :receipt])))
    (is (= (:G-efe base) (:G-efe conditioned)))
    (is (= (:f base) (:f conditioned)))
    (is (= (get-in base [:certificate :f]) (get-in conditioned [:certificate :f])))
    (is (= :observation-conditioned
           (:reason (decomposition/verdict :Q (get-in conditioned [:certificate :consumed-g :Q])))))))

(deftest vacuity-is-relative-to-prediction-not-incoming-prior
  (let [r (receipt perfect {#{} 1} (constantly {#{:judgement} 1}) #{:judgement} 1)
        ;; Sparse versus explicit zero entries denote the same distribution.
        value (q-value (score perfect {#{:judgement} 1} r))]
    (is (not= (:pre-belief r) (:predicted-belief r)))
    (is (= :conditioning-vacuous (:reason (decomposition/verdict :Q value))))
    (is (true? (get-in value [:observation-updates 0 :vacuous]))))
  (let [r (receipt perfect uniform #(hash-map % 1) #{:judgement} 1)]
    (is (= :observation-conditioned
           (:reason (verdict (score perfect (:posterior r) r)))))))

(deftest predictive-producer-preserves-early-stop
  (let [seen (atom [])
        result (m/horizon-g-sparse-cert
                {:rates perfect :q0 {#{} 1} :horizon 3
                 :precedence-fn (fn [k] (swap! seen conj k) [])
                 :c-fn-pointwise (fn [_] (constantly 0))})]
    (is (= :infinite (:g result)))
    (is (= [0] @seen))
    (is (= 1 (count (get-in result [:certificate :steps]))))))

(deftest vacuity-license-is-checkable-and-can-expire
  (let [r (receipt symmetric {#{:judgement} 1} #(hash-map % 1) #{:judgement} 1)
        result (score symmetric (:posterior r) r)
        q (q-value result)
        license (get-in q [:observation-updates 0 :vacuity-license])
        likelihoods (get-in r [:calculation :likelihoods])]
    (is (= :conditioning-vacuous (:reason (verdict result))))
    (is (= 'DarkTower.WarMachine.BeliefConditionedRollout.exactUpdate_pointMass_vacuous
           (:theorem license)))
    (is (= #{#{:judgement}} (:support license)))
    (is (= {#{:judgement} 3/4} (select-keys likelihoods (:support license))))
    (is (= 3/4 (:constant-likelihood license)))
    (is (= 1 (:predicted-total license)))
    ;; Keep this real A, but spread the predicted belief. The old license no
    ;; longer applies; the real adapter then produces a non-vacuous posterior.
    (is (= :nonconstant-vacuity-likelihood
           (:kind (trajectory/vacuity-license uniform likelihoods))))
    (let [spread (receipt symmetric uniform #(hash-map % 1) #{:judgement} 1)]
      (is (= :observation-conditioned
             (:reason (verdict (score symmetric (:posterior spread) spread))))))
    (doseq [bad-q [(assoc-in q [:observation-updates 0 :vacuity-license :constant-likelihood] 1)
                   (assoc-in q [:observation-updates 0 :vacuity-license :support]
                             (set states))
                   (update-in q [:observation-updates 0] dissoc :vacuity-license)]]
      (is (= :missing (:status (decomposition/verdict :Q bad-q)))))
    (is (= :missing-or-invalid-vacuity-likelihood
           (get-in (score symmetric (:posterior r) (update r :calculation dissoc :likelihoods))
                   [:g :kind]))))
  (let [rates {:judgement {:false-neg 1/2 :false-pos 1/2}}
        r (receipt rates uniform #(hash-map % 1) #{:judgement} 1)
        result (score rates (:posterior r) r)
        license (get-in (q-value result) [:observation-updates 0 :vacuity-license])]
    (is (= :conditioning-vacuous (:reason (verdict result))))
    (is (= 'DarkTower.WarMachine.BeliefConditionedRollout.exactUpdate_vacuous_of_const_likelihood
           (:theorem license)))
    (is (= (set states) (:support license)))
    (is (= {#{} 1/2 #{:judgement} 1/2} (get-in r [:calculation :likelihoods])))
    (is (= 1/2 (:constant-likelihood license)))
    (is (= :zero-vacuity-likelihood
           (:kind (trajectory/vacuity-license uniform (zipmap states (repeat 0))))))))

(defn score-frozen-example [{:keys [receipt q0]}]
  (let [universe (get-in receipt [:carrier :universe])
        rates (zipmap universe
                      (repeat (if (= :not-licensed (get-in receipt [:vacuity-license :status]))
                                {:false-neg 1/4 :false-pos 1/4}
                                {:false-neg 0 :false-pos 0})))]
    (m/horizon-g-sparse-cert
     {:rates rates :q0 q0 :belief-update-receipt receipt :precedence-fn (constantly [])
      :horizon 2 :universe universe :spec (assoc spec :want universe)})))

(deftest frozen-d-examples-are-consumed-without-reconditioning
  (is (= 6 (count frozen-examples)))
  (doseq [[id {:keys [receipt q0 expected-intake] :as fixture}] frozen-examples]
    (let [intake (trajectory/intake {:q0 q0 :belief-update-receipt receipt})
          result (score-frozen-example fixture)]
      (is (= expected-intake (:status intake)) (str id))
      (if (= :invalid expected-intake)
        (do (is (= :invalid (get-in result [:g :status])))
            (is (nil? (:certificate result))))
        (let [value (q-value result) events (:observation-updates value)
              q-verdict (verdict result)]
          (is (number? (:g result)))
          (is (= receipt (:conditioning-input value)))
          (is (= q0 (:initial-belief value)))
          (is (every? #(= q0 (:belief %)) (:steps value)))
          (if (= :missing (:status receipt))
            (is (= [] events))
            (do (is (= receipt (:receipt (first events))))
                (is (= 0 (:tau (first events))))
                (is (= (:tau receipt) (get-in events [0 :observation :tau])))))
          (is (= (case id
                   :value-point-mass :conditioning-vacuous
                   :value-distributed :observation-conditioned
                   :open-loop-no-conditioning)
                 (:reason q-verdict)))
          (when (= :refused (:status receipt))
            (is (= :refused (:status (first events))))
            (is (false? (:consumed (first events))))
            (is (= :none (get-in events [0 :receipt :refused-trajectory :option])))
            (is (= :degenerate (:verdict q-verdict)))))))))

(deftest frozen-refusal-authority-and-continuation-cannot-be-invented
  (let [{:keys [receipt q0]} (:refused-checkable frozen-examples)]
    (doseq [bad [(assoc-in receipt [:observation-authority :channel] :judgement)
                 (update receipt :observation-authority dissoc :contract)
                 (update receipt :reinitialization dissoc :authority)
                 (assoc-in receipt [:reinitialization :occurrence-id] "other")
                 (assoc-in receipt [:reinitialization :tau] 2)
                 (assoc-in receipt [:reinitialization :observation :present] #{})
                 (assoc-in receipt [:reinitialization :initialization-receipt :value]
                           (:predicted-belief receipt))
                 (assoc receipt :continuation-belief (:predicted-belief receipt))
                 (dissoc receipt :finding)
                 (assoc receipt :consumed true)
                 (assoc-in receipt [:refused-trajectory :option] :some)]]
      (is (= :invalid (:status (trajectory/intake {:q0 q0 :belief-update-receipt bad}))))))
  (let [{:keys [receipt q0]} (:refused-judgement frozen-examples)]
    (is (= :invalid
           (:status (trajectory/intake
                     {:q0 q0 :belief-update-receipt
                      (assoc-in receipt [:observation-authority :channel] :checkable)}))))))

(deftest frozen-context-domain-and-missing-evidence-remain-distinct
  (let [{:keys [receipt q0]} (:value-point-mass frozen-examples)]
    (doseq [bad [(assoc-in receipt [:carrier :universe] #{})
                 (assoc-in receipt [:observation :absent] (get-in receipt [:observation :present]))
                 (assoc receipt :tau 0)
                 (assoc receipt :schema :wm/token-belief-stage-v1)
                 (assoc receipt :observation-probability 0)
                 (assoc-in receipt [:calculation :predicted-state] {#{} 1})]]
      (is (= :invalid (:status (trajectory/intake {:q0 q0 :belief-update-receipt bad}))))))
  (let [{:keys [receipt q0]} (:missing frozen-examples)]
    (doseq [bad [(assoc receipt :observation-probability 0)
                 (assoc receipt :calculation {})
                 (assoc receipt :policy :conditioned-posterior)
                 (assoc-in receipt [:observation :reason] :carry-no-predecessor)
                 (assoc receipt :tau 1)]]
      (is (= :invalid (:status (trajectory/intake {:q0 q0 :belief-update-receipt bad})))))))
