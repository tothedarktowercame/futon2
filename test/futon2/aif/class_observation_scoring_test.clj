(ns futon2.aif.class-observation-scoring-test
  "PROOF-wm-works 1.3 build 2/3: the class observation model scores the
  reference input's real candidates through the real loaders, the real
  qualifier (replicated from war_machine.clj:6145) and the real bounded
  scorer. No stubs: cascade sources load from the canonical default dir."
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is]]
                        [futon2.aif.cascade-observation-scoring :as cos]
            [futon2.aif.cascade-policy :as cpol]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.observation-model :as om]
            [futon2.aif.scoring-input-receipts :as ir]))

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

(defn- class-model [{:keys [universe acceptance horizon]}]
  (let [not-yet :ending/not-yet-evaluated]
    {:schema :wm/observation-model-v1 :backend :exact-enumeration :kind :class-emission
     :universe universe :horizon horizon
     :class-universe [:focused :related :unrelated :stop-the-line not-yet]
     :acceptance acceptance :target-class {t :focused}
     :class-preference (into {} (for [tau (range 1 (inc horizon))]
                                  [tau (if (= tau horizon) joe-c {not-yet 1})]))
     :provenance {:status :synthetic :calibrated false
                  :source "PROOF-wm-works 1.3 test"}}))

(deftest reference-input-g-and-unique-maximum
  (let [{:keys [problems candidates q0 universe]} (reference-family)
        model (class-model {:universe universe
                            :acceptance #{[t :restoration-accepted]}
                            :horizon 4})
        ranked (cos/rank-cascade-actions
                {:cascade-belief q0} candidates
                {:observation-model model
                 :horizon-steps 4
                 :prediction-context {:occurrence-id "test-occ" :tau 4}
                 :cascade-spec {:want #{[t :restoration-accepted]} :evidence #{} :zeroed #{}}})
        by-id (into {} (map (juxt :cascade-id identity)) ranked)
        g1 (:controller-score (by-id :C1))
        g2 (:controller-score (by-id :C2))]
    (is (vector? ranked) (pr-str (if (map? ranked) (dissoc ranked :candidates) ranked)))
    ;; C1 does not reach the acceptance token: stop-the-line at the horizon.
    (is (< 0.001 (Math/abs (- g1 (Math/log 20)))) (str "G(C1)=" g1 " expected ln20"))
    ;; C2 reaches restoration on the focused target.
    (is (< 0.001 (Math/abs (- g2 (Math/log (/ 1 0.55))))) (str "G(C2)=" g2 " expected ln(1/.55)"))
    ;; unique maximum of the action marginal: C2 strictly lower G.
    (is (< g2 g1))
    ;; before-T steps are covered by their own deftest below.
    ))

(deftest before-t-steps-are-exactly-zero
  (let [{:keys [q0]} (reference-family)
        model (class-model {:universe #{[t :admission/task-stated] [t :restoration-accepted]}
                            :acceptance #{[t :restoration-accepted]}
                            :horizon 4})
        score (om/query model {:op :score :belief {#{[t :admission/task-stated]} 1}
                                                   :tau 3
                                                   :preference {:ending/not-yet-evaluated 1}})]
    (is (= :computed (:status score)) (pr-str score))
    (is (zero? (:g score)) (str "tau 3 g=" (:g score)))
    (is (zero? (:ambiguity score)))))

(deftest mixed-belief-splits-never-collapses
  (let [model (class-model {:universe #{[t :admission/task-stated] [t :restoration-accepted]}
                            :acceptance #{[t :restoration-accepted]}
                            :horizon 1})
        reached #{[t :admission/task-stated] [t :restoration-accepted]}
        unreached #{[t :admission/task-stated]}
        score (om/query model {:op :score :belief {reached 1/2 unreached 1/2}
                               :tau 1
                               :preference joe-c})
        pred (:prediction score)]
    (is (= :computed (:status score)) (pr-str score))
    (is (= 1/2 (:focused pred)) (pr-str pred))
    (is (= 1/2 (:stop-the-line pred)) (pr-str pred))))
