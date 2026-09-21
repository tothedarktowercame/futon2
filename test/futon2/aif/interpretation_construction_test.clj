(ns futon2.aif.interpretation-construction-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.active-horizon-g :as ah]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.interpretation-construction :as sut]
            [futon2.report.war-machine :as wm]))

(def interpretations
  {:P {:guard {:needs #{} :forbids #{}} :produces #{:q}}
   :Q {:guard {:needs #{:q} :forbids #{}} :produces #{:w}}
   :R {:guard {:needs #{} :forbids #{}} :produces #{:r}}})
(def receipts (zipmap (keys interpretations) (repeat {:kind :fixture-interpretation :by "test"})))
(def observation {:q false :w false :r false})
(defn model-order [patterns order]
  (mapv #(policy/token-interpretation % (get patterns %)) order))
(defn reaches? [patterns order horizon]
  (let [row (model/rollout (constantly (model-order patterns order)) {#{} 1} horizon)]
    (some #(contains? % :w) (keys row))))
(defn score [{:keys [patterns precedence]}]
  (let [by-id (into {} (map (juxt :id identity)) patterns)]
    (ah/active-horizon-g
     {:q0 {#{} 1} :precedence-fn (constantly (model-order by-id precedence))
      :horizon 2 :universe #{:q :w :r} :masked #{} :cap 0
      :rates (zipmap [:q :w :r] (repeat {:false-pos 0 :false-neg 0}))
      :c-fn-pointwise (fn [_ o] (/ (if (contains? o :w) 3 1) 16))})))
(def input
  {:target "M-construction" :want [:w] :observation observation
   :interpretations interpretations :interpretation-receipts receipts
   :budget {:max-moves 1 :max-expansions 20} :horizon 2
   :move-cost 0 :evaluate-g score})

(deftest composes-a-real-dependency-and-records-the-taken-move
  (let [r (sut/construct input) c (first (:candidates r)) receipt (:construction-receipt c)]
    (is (= :constructed (:status r)))
    (is (= [[:P :Q]] (mapv :precedence (:candidates r))))
    (is (= #{[:P :Q]} (:need-edges c)))
    (is (= :machine-constructed (:kind receipt)))
    (is (= [:compose-by-need] (mapv :move-id (:moves receipt))))
    (is (= [:P :Q] (get-in receipt [:ordering 0 :after])))
    (is (= [:Q :P] (get-in receipt [:ordering 0 :before])))
    (is (= :order-by-need (get-in receipt [:ordering 0 :move-id])))
    (is (= #{:P :Q} (set (keys (:interpretation-receipts c)))))
    (is (= 1 (:budget-used receipt)))
    (is (true? (:stopped-is-not-success receipt)))
    (is (reaches? interpretations (:precedence c) 2))
    (is (not (reaches? interpretations [:Q] 2)))
    (is (not (reaches? interpretations (:precedence c) 1)))))

(deftest missing-producer-is-not-a-padded-singleton
  (let [r (sut/construct (assoc-in input [:interpretations :P :produces] #{}))]
    (is (= :no-supported-order (:kind r)))
    (is (= [] (:candidates r)))
    (is (some #(= {:kind :unproduced-need :token :q} (select-keys % [:kind :token]))
              (:findings r)))
    (is (not (reaches? (assoc-in interpretations [:P :produces] #{}) [:P :Q] 2)))))

(deftest existing-assembly-and-admission-accept-machine-receipts
  (let [candidates (:candidates (sut/construct input))
        assembled (problems/assemble
                   {:targets [(:target input)]
                    :sources {:universes {(:target input) observation}
                              :wants {(:target input) [:w]}
                              :interpretations {(:target input) {:patterns interpretations :receipts receipts}}
                              :locators {(:target input) (zipmap (keys observation) (repeat {:class :C4}))}
                              :candidates {(:target input) candidates}
                              :horizon-steps 2 :beta-by-context {:WM 1} :context-of (constantly :WM)}})
        admit #'wm/admit-cascade-problem
        result (admit (first (:problems assembled)))]
    (is (empty? (:refusals assembled)))
    (is (seq (:constructed-candidates (:problem result))))
    (is (empty? (:declines result)))
    (testing "the real admission check catches the named bad case"
      (let [bad (admit (update (first (:problems assembled)) :interpretation-receipts dissoc :P))]
        (is (nil? (:problem bad)))
        (is (= :interpretation-receipts-missing (get-in bad [:declines 0 :reason])))))))

(deftest limits-and-economics-are-not-overridden
  (doseq [[edit kind] [[#(dissoc % :budget) :budget-required]
                       [#(assoc-in % [:budget :max-expansions] 1) :search-budget-exhausted]
                       [#(assoc-in % [:budget :max-moves] 0) :construction-not-taken]
                       [#(assoc % :evaluate-g (constantly 1)) :construction-not-taken]
                       [#(assoc % :move-cost 100) :construction-not-taken]
                       [#(assoc % :horizon 1) :no-supported-order]
                       [#(assoc % :horizon nil) :horizon-required]
                       [#(update % :interpretation-receipts dissoc :P) :interpretation-receipt-missing]]]
    (let [r (sut/construct (edit input))]
      (is (= kind (:kind r)))
      (is (empty? (:candidates r))))))

(deftest cycles-forbids-and-unknowns-do-not-establish-facts
  (doseq [patterns [(assoc-in interpretations [:P :guard :needs] #{:w})
                    (assoc-in interpretations [:Q :guard :forbids] #{:q})]]
    (is (= :no-supported-order (:kind (sut/construct (assoc input :interpretations patterns))))))
  (let [r (sut/construct (assoc-in input [:observation :q] :unknown))]
    (is (= :observation-required (:kind r)))
    (is (= [:q] (:tokens r)))))

(deftest recorded-declaration-shape-does-not-invent-missing-producers
  ;; Actual three wants / one interpretation from the reference run declaration.
  (let [d (edn/read-string (slurp "resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn"))
        r (sut/construct (merge input {:target (:target d) :want (:want d)
                                      :observation (zipmap (:facts d) (repeat false))
                                      :interpretations (:patterns d)
                                      :interpretation-receipts (:interpretation-receipts d)}))]
    (is (= :no-supported-order (:kind r)))
    (is (empty? (:candidates r)))
    (is (some #(= :unproduced-need (:kind %)) (:findings r)))))

(deftest alternative-producers-remain-separate-and-observed-needs-need-no-producer
  (let [with-alternative (-> input
                             (assoc-in [:interpretations :P2] (:P interpretations))
                             (assoc-in [:interpretation-receipts :P2] (:P receipts)))
        candidates (:candidates (sut/construct with-alternative))]
    (is (= #{[:P :Q] [:P2 :Q]} (set (map :precedence candidates))))
    (is (every? #(= 2 (count (:interpretation-receipts %))) candidates)))
  (let [observed (assoc-in input [:observation :q] true)
        ;; The evaluator must consume the same initial observation as the caller.
        observed (assoc observed :evaluate-g #(if (seq (:precedence %)) 0 1))]
    (is (= [[:Q]] (mapv :precedence (:candidates (sut/construct observed)))))))

(deftest unsupported-semantics-and-invalid-g-stay-explicit
  (doseq [edit [#(assoc-in % [:interpretations :P :theta] 1/2)
                #(assoc-in % [:interpretations :Q :guard :or] [:q :r])]]
    (is (= :invalid-input (:kind (sut/construct (edit input))))))
  (doseq [g [##Inf ##NaN :infinite {:status :missing :kind :missing-preference-spec}]]
    (is (= :nonfinite-g (:kind (sut/construct (assoc input :evaluate-g (constantly g))))))))
