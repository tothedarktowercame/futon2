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
   :generative-model {:missing-term-policy :refuse-not-zero}
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
             :information-model zero-information
             :source-pin {:path (str "predictions/" (name id) ".edn")
                          :sha256 (apply str (repeat 64 "c"))}}})

(deftest healthy-field-admits-m-e-t-and-useful-algorithms
  (let [field [(candidate :m :mission "M-one" 0.1)
               (candidate :e :excursion "E-one" 0.2)
               (candidate :t :ticket "T-one" 0.3)
               (candidate :a :algorithm "A-approved" 0.0)]
        receipt (meta/evaluate {:contract contract :contract-source pin
                                :observation {:injury-observation {:absent :none}}
                                :candidates field})]
    (is (= :selected (:status receipt)))
    (is (= {:mission 1 :excursion 1 :ticket 1 :algorithm 1}
           (:field-census receipt)))
    (is (= :a (:selected-policy receipt)))
    (is (= :minimum-canonical-G (:selection-reason receipt)))
    (is (every? number? (vals (:g receipt))))
    (is (= receipt
           (meta/evaluate {:contract contract :contract-source pin
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
        selected (meta/evaluate {:contract contract :contract-source pin
                                 :observation observation :candidates [heal other]})
        no-match (meta/evaluate {:contract contract :contract-source pin
                                 :observation observation :candidates [other]})]
    (is (= :heal (:selected-policy selected)))
    (is (= :singleton-admitted-support (:selection-reason selected)))
    (is (= :no-matching-self-heal-algorithm (:reason no-match)))
    (is (= :refused (:status no-match)))))

(deftest malformed-contract-candidates-and-g-inputs-refuse-with-reasons
  (let [good (candidate :m :mission "M-one" 0.1)]
    (testing "slot declaration mutation"
      (is (= :contract-invalid
             (:reason (meta/evaluate {:contract (assoc-in contract [:slots :target :type] :string)
                                      :contract-source pin :observation {}
                                      :candidates [good]})))))
    (testing "missing and invalid candidate slot data"
      (let [bad (-> good
                    (update :slots dissoc :target)
                    (assoc-in [:slots :resource-envelope :token-budget] 0))
            receipt (meta/evaluate {:contract contract :contract-source pin
                                    :observation {} :candidates [bad]})]
        (is (= :candidate-invalid (:reason receipt)))
        (is (= #{:required-slot-missing :target-invalid :resource-envelope-invalid}
               (set (get-in receipt [:details :candidate-errors :m]))))))
    (testing "complete G inputs are mandatory"
      (is (= :g-inputs-incomplete
             (:reason (meta/evaluate {:contract contract :contract-source pin
                                      :observation {}
                                      :candidates [(update good :g-input dissoc :information-model)]})))))
    (testing "tactical material cannot cross the outer boundary"
      (is (= :outer-boundary-violated
             (:reason (meta/evaluate {:contract contract :contract-source pin
                                      :observation {} :candidates [good]
                                      :tactical-precedence [:p]})))))))
