(ns futon2.aif.meta-outer-policy-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-outer-policy :as meta]))

(def pin {:path "library/meta/meta-outer-policy-cascade.edn"
          :sha256 (apply str (repeat 64 "a"))})

(def contract
  {:schema :meta/outer-policy-cascade-v1
   :id :meta/test
   :patterns [:meta/observe :meta/fill :meta/select]
   :precedence [[:meta/observe :meta/fill] [:meta/fill :meta/select]]
   :slots
   {:task-kind {:required true :type :enum}
    :target {:required true :type :current-meta-item-id}
    :next-move {:required true :type :enum}
    :resource-envelope
    {:required true :type :resource-envelope
     :fields {:time-budget-ms :positive-integer :token-budget :positive-integer
              :author-seat :agent-id :reviewer-seat :agent-id}}
    :evidence-channel
    {:required true :type :closure-or-progress-observer
     :fields {:source :source-pin :locator :checkable-locator}}
    :stopping-rule {:required true :type :enum}}
   :generative-model {:outcomes [:closure]
                      :missing-term-policy :refuse-not-zero}
   :selection {:law :argmin-G}})

(def zero-information
  {:prior {:s 1.0} :predicted-observations {:o 1.0}
   :posteriors {:o {:s 1.0}}})

(defn candidate [id kind target mean]
  {:id id
   :slots {:task-kind kind :target target
           :next-move (if (= :algorithm kind) :run-algorithm :advance)
           :resource-envelope {:time-budget-ms 1000 :token-budget 100
                               :author-seat "author" :reviewer-seat "reviewer"}
           :evidence-channel
           {:source {:path (str "holes/" target ".md")
                     :sha256 (apply str (repeat 64 "b"))}
            :locator {:class :C4 :repo "repo" :path "task.md" :decl "done"}}
           :stopping-rule :grounded-progress}
   :g-input {:means [mean] :variances [1.0]
             :preference-means [0.0] :preference-variances [1.0]
             :outcome-vocabulary [:closure]
             :information-model zero-information
             :source-pin {:path (str "predictions/" (name id) ".edn")
                          :sha256 (apply str (repeat 64 "c"))}}})

(defn field [candidates]
  {:schema :wm/meta-field-observation-v1
   :source-pin {:path "observations/meta-field.edn"
                :sha256 (apply str (repeat 64 "d"))}
   :algorithm-catalog-source {:path "algorithms/approved.edn"
                              :sha256 (apply str (repeat 64 "e"))}
   :rows (mapv (fn [candidate]
                 {:id (get-in candidate [:slots :target])
                  :kind (get-in candidate [:slots :task-kind])
                  :source (get-in candidate [:slots :evidence-channel :source])
                  :approved (= :algorithm (get-in candidate [:slots :task-kind]))})
               candidates)})

(defn evaluate [input]
  (meta/evaluate (assoc input :field-observation (field (:candidates input)))))

(deftest healthy-field-admits-m-e-t-and-useful-algorithms
  (let [field [(candidate :m :mission "M-one" 0.1)
               (candidate :e :excursion "E-one" 0.2)
               (candidate :t :ticket "T-one" 0.3)
               (candidate :a :algorithm "A-approved" 0.0)]
        receipt (evaluate {:contract contract :contract-source pin
                           :observation {:injury-observation {:absent :none}}
                           :candidates field})]
    (is (= :selected (:status receipt)))
    (is (= {:mission 1 :excursion 1 :ticket 1 :algorithm 1}
           (:field-census receipt)))
    (is (= :a (:selected-policy receipt)))
    (is (= :minimum-canonical-G (:selection-reason receipt)))
    (is (every? number? (vals (:g receipt))))
    (is (= receipt
           (evaluate {:contract contract :contract-source pin
                      :observation {:injury-observation {:absent :none}}
                      :candidates field}))
        "explicit pinned inputs replay byte-for-value")))

(deftest injured-field-is-exact-match-only-and-no-match-abstains
  (let [source {:path "runs/injury.edn" :sha256 (apply str (repeat 64 "d"))}
        heal (assoc (candidate :heal :algorithm "A-self-heal" 0.2)
                    :repairs-capability :write
                    :rearm-observation {:kind :probe :requires [:terminal-record]})
        other (assoc (candidate :other :algorithm "A-network" 0.0)
                     :repairs-capability :network
                     :rearm-observation {:kind :probe :requires [:network-ok]})
        observation {:injury-observation :active :injured-capability :write
                     :source-pin source}
        selected (evaluate {:contract contract :contract-source pin
                            :observation observation :candidates [heal other]})
        no-match (evaluate {:contract contract :contract-source pin
                            :observation observation :candidates [other]})]
    (is (= :heal (:selected-policy selected)))
    (is (= :singleton-admitted-support (:selection-reason selected)))
    (is (= :no-matching-self-heal-algorithm (:reason no-match)))
    (is (= :refused (:status no-match)))))

(deftest malformed-contract-candidates-and-g-inputs-refuse-with-reasons
  (let [good (candidate :m :mission "M-one" 0.1)]
    (testing "slot declaration mutation"
      (is (= :contract-invalid
             (:reason (evaluate {:contract (assoc-in contract [:slots :target :type] :string)
                                 :contract-source pin :observation {}
                                 :candidates [good]})))))
    (testing "missing and invalid candidate slot data"
      (let [bad (-> good
                    (update :slots dissoc :target)
                    (assoc-in [:slots :resource-envelope :token-budget] 0))
            receipt (evaluate {:contract contract :contract-source pin
                               :observation {} :candidates [bad]})]
        (is (= :candidate-invalid (:reason receipt)))
        (is (= #{:required-slot-missing :target-invalid :resource-envelope-invalid}
               (set (get-in receipt [:details :candidate-errors :m]))))))
    (testing "complete G inputs are mandatory"
      (is (= :g-inputs-incomplete
             (:reason (evaluate {:contract contract :contract-source pin
                                 :observation {}
                                 :candidates [(update good :g-input dissoc :information-model)]})))))
    (testing "non-finite model values and negative weights are not G inputs"
      (doseq [bad [(assoc-in good [:g-input :means 0] Double/NaN)
                   (assoc-in good [:g-input :preference-means 0]
                             Double/POSITIVE_INFINITY)
                   (assoc-in good [:g-input :weights] [-1.0])]]
        (is (= :g-inputs-incomplete
               (:reason (evaluate {:contract contract :contract-source pin
                                   :observation {} :candidates [bad]}))))))
    (testing "tactical material cannot cross the outer boundary"
      (is (= :outer-boundary-violated
             (:reason (evaluate {:contract contract :contract-source pin
                                 :observation {} :candidates [good]
                                 :tactical-precedence [:p]})))))))

(deftest adversarial-vocabulary-graph-and-field-mutations-fail-closed
  (let [a (candidate :a :mission "M-a" 0.1)
        b (candidate :b :excursion "E-b" 0.2)
        input {:contract contract :contract-source pin :observation {}
               :candidates [a b]}]
    (testing "different Gaussian dimensions cannot be compared"
      (let [two-channel (-> b
                            (assoc-in [:g-input :outcome-vocabulary] [:closure :progress])
                            (assoc-in [:g-input :means] [0.2 0.3])
                            (assoc-in [:g-input :variances] [1.0 1.0])
                            (assoc-in [:g-input :preference-means] [0.0 0.0])
                            (assoc-in [:g-input :preference-variances] [1.0 1.0]))
            receipt (evaluate (assoc input :candidates [a two-channel]))]
        (is (= :outcome-vocabulary-invalid (:reason receipt)))
        (is (= #{:candidate-outcome-vocabulary-mismatch :outcome-dimension-mismatch}
               (set (get-in receipt [:details :errors]))))))
    (testing "a cycle cannot masquerade as an outer cascade"
      (let [cyclic (assoc contract :precedence [[:meta/observe :meta/fill]
                                                [:meta/fill :meta/observe]
                                                [:meta/fill :meta/select]])
            receipt (evaluate (assoc input :contract cyclic))]
        (is (= :contract-invalid (:reason receipt)))
        (is (some #{:precedence-cyclic} (get-in receipt [:details :errors])))))
    (testing "candidate subset cannot call itself the field"
      (let [manifest (field [a b])
            receipt (meta/evaluate (assoc input :candidates [a]
                                          :field-observation manifest))]
        (is (= :field-observation-invalid (:reason receipt)))
        (is (= [:field-coverage-incomplete]
               (get-in receipt [:details :errors])))))
    (testing "algorithm rows require a pinned approval catalog and approval"
      (let [algorithm (candidate :alg :algorithm "A-one" 0.1)
            manifest (-> (field [algorithm])
                         (dissoc :algorithm-catalog-source)
                         (assoc-in [:rows 0 :approved] false))
            receipt (meta/evaluate {:contract contract :contract-source pin
                                    :field-observation manifest :observation {}
                                    :candidates [algorithm]})]
        (is (= #{:algorithm-catalog-unpinned :algorithm-not-approved}
               (set (get-in receipt [:details :errors]))))))))
