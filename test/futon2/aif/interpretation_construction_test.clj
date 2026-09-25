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

;; ---------------------------------------------------------------- clause 0
;; H-order: each candidate's :construction-receipt carries :order, the
;; containment order over units derived from the interpretations'
;; produces/consumes. Fixture is the recorded hand cascade itself
;; (futon3c holes/labs/M-futon-seams/proto/instance-6.edn @ 6149272b),
;; bad case (1) of the packet: instance 6's one overlapping pair without a
;; meet must come out with mode-gate and wr-8 as the maximal common units.
(def instance-6
  (edn/read-string
   (slurp "test/fixtures/want-interp-library/M-futon-seams-instance-6@futon3c-6149272b.edn")))

(deftest instance-6-order-has-one-missing-meet-with-the-conflicting-pair-maximal
  (let [interpretations (into {}
                              (map (fn [[id p]] [id (select-keys p [:guard :produces])]))
                              (:patterns instance-6))
        tokens (reduce into
                       (set (:want instance-6))
                       (map (fn [p]
                              (into (into (:produces p) (-> p :guard :needs))
                                    (-> p :guard :forbids)))
                            (vals interpretations)))
        receipts (zipmap (keys interpretations)
                         (repeat {:kind :fixture-interpretation :by "test"}))
        r (sut/construct {:target "M-futon-seams-instance-6"
                          :want (vec (:want instance-6))
                          :observation (zipmap tokens (repeat false))
                          :interpretations interpretations
                          :interpretation-receipts receipts
                          :budget {:max-moves 1 :max-expansions 500}
                          :horizon 6
                          :move-cost 0
                          :evaluate-g (fn [c] (if (empty? (:precedence c)) 100.0 1.0))})
        c (first (:candidates r))
        order (get-in c [:construction-receipt :order])]
    (is (= :constructed (:status r)))
    (is (= 8 (count (:units order)))
        "one unit per pattern application; pattern id as an attribute")
    (is (every? #(= (:unit %) (:pattern %)) (:units order)))
    (testing "the derived descent is exactly the instance's recorded :above"
      (is (= (set (map (juxt :context :pattern) (:above instance-6)))
             (set (:descent order)))))
    (testing "the one overlapping pair without a meet names the pair and the
             maximal units of their common part — the two incomparable
             conflicting patterns (clause 0: the missing meet and the
             frontier conflict are the same fact)"
      (is (= [{:pair [:cascade-construction/choose-the-grain-where-state-lives
                      :or3/count-every-card-back]
               :common-maximal [:realtime/mode-gate
                                :war-room/wr-8-typed-files-are-sources-of-truth]}]
             (:missing-meets order))))
    (is (= 27 (count (:meets order))))
    (testing "a finding, not a failure: construction still hands the
             candidate over with its order attached"
      (is (= :machine-constructed (get-in c [:construction-receipt :kind]))))))

;; ------------------------------------------------------ NONFINITE-G-I
;; A candidate whose G is nonfinite is left out of the comparison and kept on
;; the record under :left-out; the rest are compared as before (owner's
;; decision, claude-10, 2026-09-25, REFUSAL-REGISTER-D row 12). Three
;; alternative producers of :q give three candidates [:Pn :Q]; G is set per
;; candidate by its first pattern, and the empty baseline scores 5.0.
(def three-p
  (let [p {:guard {:needs #{} :forbids #{}} :produces #{:q}}]
    {:P p :P2 p :P3 p :Q {:guard {:needs #{:q} :forbids #{}} :produces #{:w}}}))

(defn- three-input [g-by]
  {:target "M-construction" :want [:w] :observation {:q false :w false}
   :interpretations three-p
   :interpretation-receipts (zipmap (keys three-p) (repeat {:kind :fixture-interpretation :by "test"}))
   :budget {:max-moves 1 :max-expansions 50} :horizon 2 :move-cost 0
   :evaluate-g (fn [c] (get (merge {nil 5.0} g-by) (first (:precedence c))))})

(defn- g-of-best [r] (get-in r [:candidates 0 :construction-receipt :g-of-best :value]))

(deftest nf-1-one-nan-is-left-out-the-finite-two-are-compared
  (let [r (sut/construct (three-input {:P ##NaN :P2 1.5 :P3 2}))
        [lo] (:left-out r)]
    (is (= :constructed (:status r)))
    (is (= #{[:P2 :Q] [:P3 :Q]} (set (map :precedence (:candidates r)))))
    (is (= 1.5 (g-of-best r)) "the winner is the finite min")
    (is (= 1 (count (:left-out r))))
    (is (= [:P :Q] (:precedence lo)))
    (is (= :nonfinite-g (get-in lo [:g :absent])))
    (is (Double/isNaN (get-in lo [:g :value])))
    (is (= #{:P :Q} (set (keys (:interpretation-receipts lo)))))))

(deftest nf-2-positive-infinity-is-left-out-the-same-way
  (let [r (sut/construct (three-input {:P 3.0 :P2 ##Inf :P3 2}))]
    (is (= :constructed (:status r)))
    (is (= #{[:P :Q] [:P3 :Q]} (set (map :precedence (:candidates r)))))
    (is (= 2.0 (double (g-of-best r))))
    (is (= [{:precedence [:P2 :Q] :g {:absent :nonfinite-g :value ##Inf}}]
           (mapv #(select-keys % [:precedence :g]) (:left-out r))))))

(deftest nf-3-every-candidate-nonfinite-is-a-typed-result-not-a-throw
  (let [r (sut/construct (three-input {:P ##NaN :P2 ##Inf :P3 :infinite}))]
    (is (= :refused (:status r)))
    (is (= :nonfinite-g (:kind r)) "the refusal kind the constructor already used")
    (is (= :no-finite-g (:absent r)))
    (is (= [] (:candidates r)) "no constructed candidate")
    (is (= #{[:P :Q] [:P2 :Q] [:P3 :Q]} (set (map :precedence (:left-out r)))))
    (is (every? #(= :nonfinite-g (get-in % [:g :absent])) (:left-out r)))))

(deftest nf-4-all-finite-is-byte-identical-to-before
  ;; fixture: pr-str of this call on the unchanged source (futon2 3bbf5059,
  ;; interpretation_construction.clj sha256 ef18699e...), captured before the change
  (let [r (sut/construct (three-input {:P 3.0 :P2 1.5 :P3 2}))]
    (is (= (slurp "test/fixtures/interpretation-construction/nf4-all-finite@futon2-3bbf5059.edn")
           (pr-str r)))
    (is (not (contains? r :left-out)))))

(deftest nf-bad-case-a-zero-g-boxed-double-or-long-is-finite
  (doseq [z [0 0.0 (Double/valueOf 0.0) (Long/valueOf 0) 0N 0M]]
    (let [r (sut/construct (three-input {:P z :P2 1.5 :P3 2}))]
      (is (= :constructed (:status r)) (pr-str (type z)))
      (is (not (contains? r :left-out)) (pr-str (type z)))
      (is (zero? (g-of-best r)) (pr-str (type z))))))

(deftest nf-5-left-out-survives-when-no-move-is-taken
  ;; claude-10 review of f4c1fb09: the baseline (5.0) beats both finite
  ;; candidates, so the constructor takes no move and the result is
  ;; :construction-not-taken; the NaN candidate must still be on the record
  (let [r (sut/construct (three-input {:P ##NaN :P2 9.0 :P3 8.0}))]
    (is (= :refused (:status r)))
    (is (= :construction-not-taken (:kind r)) (pr-str (dissoc r :findings)))
    (is (= [[:P :Q]] (mapv :precedence (:left-out r))))
    (is (= :nonfinite-g (get-in r [:left-out 0 :g :absent])))))
