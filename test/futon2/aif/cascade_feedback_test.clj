(ns futon2.aif.cascade-feedback-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-feedback :as feedback]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.aif.policy :as policy]))

(def action
  {:kind :cascade-candidate
   :id :C1
   :target "M-current"
   :want [["M-current" :done]]
   :reached-wants [["M-current" :done]]
   :unreached-wants []
   :precedence [{:id :patterns/applied :produces #{["M-current" :done]}}
                {:id :patterns/selected-only :produces #{}}]})

(def admitted-enactment
  {:verification
   {:status :admitted
    :candidate-to-minted-join
    {:status :verified
     :selected-action-sha256 "same-action"
     :enacted-action-sha256 "same-action"}}})

(def accepted
  {:accepted? true
   :criterion-step {:id :patterns/applied :source :recorded-decision}})

(defn- input [& {:as overrides}]
  (merge {:run-id "run-1"
          :selected-action action
          :outcome :grounded-change
          :accepted-increment accepted
          :d-task-enactment admitted-enactment
          :artifact {:repo "/repo" :commit "abc1234"}}
         overrides))

(deftest feedback-separates-selection-application-and-success
  (let [receipt (feedback/receipt (input))]
    (is (= :provisional-per-run (:cascade-status receipt)))
    (is (= [:patterns/applied :patterns/selected-only]
           (get-in receipt [:patterns :selected])))
    (is (= [{:pattern :patterns/applied
             :status :successful
             :evidence {:selected-enacted-action :verified
                        :grounded-work :attested
                        :application-role :enacted-step
                        :terminal-outcome :grounded-change
                        :accepted-increment true
                        :accepted-reason nil}
             :reinforcement :positive}
            {:pattern :patterns/selected-only
             :status :supporting-attestation
             :evidence {:selected-enacted-action :verified
                        :grounded-work :attested
                        :application-role :cascade-support
                        :terminal-outcome :grounded-change}
             :reinforcement :positive}]
           (get-in receipt [:patterns :applications])))
    (is (empty? (get-in receipt [:patterns :selected-only])))
    (is (= [:patterns/applied :patterns/selected-only]
           (get-in receipt [:patterns :positive-reinforcement])))
    (is (= {:repo "/repo" :commit "abc1234" :grounded? true}
           (:artifact receipt)))))

(deftest selected-without-exact-application-is-not-use
  (let [receipt (feedback/receipt
                 (input :d-task-enactment {:verification {:status :refused}}))]
    (is (empty? (get-in receipt [:patterns :applications])))
    (is (= [:patterns/applied :patterns/selected-only]
           (get-in receipt [:patterns :selected-only])))
    (is (empty? (get-in receipt [:patterns :positive-reinforcement])))))

(deftest grounded-work-attests-cascade-without-operator-acceptance
  (let [revision {:schema :wm/provisional-cascade-revision-v1
                  :status :refused
                  :kind :no-distinct-whole-mission-proposal
                  :repair-evidence {:status :present}}
        receipt (feedback/receipt
                 (input :outcome :grounded-progress
                        :cascade-revision revision
                        :accepted-increment
                        {:accepted? false
                         :reason :declared-product-not-observed-true
                         :criterion-step {:id :patterns/applied
                                          :source :recorded-decision}}))]
    (is (= :successful
           (get-in receipt [:patterns :applications 0 :status])))
    (is (= :supporting-attestation
           (get-in receipt [:patterns :applications 1 :status])))
    (is (= :positive
           (get-in receipt [:patterns :applications 0 :reinforcement])))
    (is (= [:patterns/applied :patterns/selected-only]
           (get-in receipt [:patterns :positive-reinforcement])))
    (is (= {:status :absent :reason :verified-grounded-work}
           (:blocker receipt)))
    (is (= revision (:cascade-revision receipt)))))

(deftest partial-grounded-run-gives-enacted-step-more-credit-than-support
  (let [receipt (feedback/receipt
                 (input :outcome :grounded-progress
                        :accepted-increment
                        {:accepted? :no-acceptance-declared
                         :reason :target-has-no-mechanical-acceptance-declaration
                         :criterion-step {:id :patterns/applied
                                          :source :recorded-decision}}))
        metadata (feedback/construction-metadata {:events [receipt]})
        applied (feedback/pattern-evidence-prior
                 (get metadata "M-current")
                 {:precedence [:patterns/applied]})
        supporting (feedback/pattern-evidence-prior
                    (get metadata "M-current")
                    {:precedence [:patterns/selected-only]})]
    (is (= 1 (get-in metadata ["M-current" :patterns :patterns/applied
                               :successful-applications])))
    (is (= 1 (get-in metadata ["M-current" :patterns :patterns/selected-only
                               :supporting-attestations])))
    (is (> (:factor applied) (:factor supporting))
        "the next construction can distinguish enacted work from support")))

(deftest next-construction-receives-retained-pattern-feedback
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "cascade-feedback-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        path (str (io/file dir "events.edn"))]
    (try
      (let [positive (feedback/receipt (input))
            blocked (feedback/receipt
                     (input :run-id "run-2"
                            :outcome :grounded-progress
                            :accepted-increment
                            {:accepted? false :reason :blocked-on-new-fact
                             :criterion-step {:id :patterns/applied
                                              :source :recorded-decision}}))]
        (is (= :recorded (:status (feedback/record! path positive))))
        (is (= :already-recorded (:status (feedback/record! path positive))))
        (is (= :recorded (:status (feedback/record! path blocked))))
        (let [metadata (feedback/load-construction-metadata path)
              target "M-current"
              assembled
              (problems/assemble
               {:targets [target]
                :sources
                (locfix/locate-all
                 {:universes {target {:seed true :done false}}
                  :interpretations
                  {target {:patterns
                           {:patterns/applied
                            {:guard {:needs #{:seed} :forbids #{:done}}
                             :produces #{:done}}}
                           :receipts {:patterns/applied {:kind :test-reading}}}}
                  :wants {target [:done]}
                  :pattern-feedback metadata
                  :horizon-steps 1
                  :beta-by-context {:test {:beta 1}}
                  :context-of (constantly :test)
                  :construction
                  {:construct construction/construct
                   :budget {:max-moves 2 :max-expansions 20}
                   :move-cost 0
                   :evaluate-g (fn [_ candidate]
                                 (if (seq (:precedence candidate)) 0.0 1.0))}})})
              problem (first (:problems assembled))
              carried (get-in problem [:constructed-candidates 0
                                       :construction-receipt :pattern-feedback])]
          (is (empty? (:refusals assembled)))
          (is (= feedback/metadata-schema (:schema carried)))
          (is (= 2 (get-in carried [:patterns :patterns/applied
                                    :successful-applications])))
          (is (= 0 (get-in carried [:patterns :patterns/applied
                                    :incomplete-applications])))
          (is (= 0 (get-in carried [:patterns :patterns/selected-only
                                    :successful-applications])))
          (is (= 2 (get-in carried [:patterns :patterns/selected-only
                                    :supporting-attestations])))
          (is (= 0 (get-in carried [:patterns :patterns/selected-only
                                    :selected-only])))))
      (finally
        (doseq [file (reverse (file-seq dir))]
          (io/delete-file file true))))))

(deftest verified-feedback-changes-the-unchanged-policy-choice-through-e
  (let [entry (fn [id]
                {:action {:kind :cascade-candidate :id id :target "M-current"
                          :precedence [{:id id :target "M-current"}]}
                 :controller-score 1.0 :certificate {:f nil}})
        entries [(entry :patterns/a) (entry :patterns/b)]
        metadata {:wm/global
                  {:schema feedback/metadata-schema
                   :scope :global
                   :global-patterns
                   {:patterns/a {:successful-applications 0
                                 :incomplete-applications 3
                                 :selected-only 9}
                    :patterns/b {:successful-applications 2
                                 :incomplete-applications 0
                                 :selected-only 0}}}}
        before (policy/select-action-cascades entries {:beta 1})
        after (policy/select-action-cascades entries
                                             {:beta 1 :pattern-feedback metadata})
        habits (get-in after [:selection-certificate :policies])]
    (is (= :patterns/a (get-in before [:action :id]))
        "the unchanged tied menu uses the declared action-name tie-break")
    (is (= :patterns/b (get-in after [:action :id]))
        "verified global feedback changes the posterior, not mission state")
    (is (= 1.0 (reduce + (map :habit habits))))
    (is (= :verified-application-beta11-likelihood-ratio
           (get-in after [:selection-certificate :candidates 0
                          :habit-provenance :pattern-feedback :basis])))
    (is (= :none
           (get-in after [:selection-certificate :candidates 0
                          :habit-provenance :pattern-feedback
                          :selected-only-effect])))))

(deftest construction-ranks-proposals-with-the-same-receipted-evidence
  (let [metadata {:schema feedback/metadata-schema :target "M-current"
                  :global-patterns
                  {:patterns/a {:successful-applications 0
                                :incomplete-applications 2}
                   :patterns/b {:successful-applications 2
                                :incomplete-applications 0}}}
        result (construction/construct
                {:target "M-current" :want [:done]
                 :observation {:done false}
                 :interpretations
                 {:patterns/a {:guard {:needs #{} :forbids #{}} :produces #{:done}}
                  :patterns/b {:guard {:needs #{} :forbids #{}} :produces #{:done}}}
                 :interpretation-receipts
                 {:patterns/a {:source :test} :patterns/b {:source :test}}
                 :horizon 1 :move-cost 0
                 :budget {:max-moves 2 :max-expansions 20}
                 :pattern-feedback metadata
                 :evaluate-g (fn [candidate]
                               {:value (if (seq (:precedence candidate)) 1.0 2.0)
                                :universe [:done]})})]
    (is (= :constructed (:status result)))
    (is (= [[:patterns/b] [:patterns/a]]
           (mapv :precedence (:candidates result))))
    (is (> (get-in result [:candidates 0 :construction-receipt
                           :pattern-feedback-prior :factor])
           (get-in result [:candidates 1 :construction-receipt
                           :pattern-feedback-prior :factor])))))
