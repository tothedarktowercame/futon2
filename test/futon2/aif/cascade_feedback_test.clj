(ns futon2.aif.cascade-feedback-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-feedback :as feedback]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.aif.policy :as policy]
            [futon2.aif.selected-want-outcome :as want-outcome]
            [futon2.aif.token-outcome :as token-outcome]))

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

(def want-outcomes
  {:schema :wm/selected-want-outcome-v1 :status :verified
   :target "M-current" :selected-wants [["M-current" :done]]
   :by-class {:reached [["M-current" :done]]
              :progressed [] :blocked [] :untouched []}})

(defn- input [& {:as overrides}]
  (merge {:run-id "run-1"
          :selected-action action
          :outcome :grounded-change
          :accepted-increment accepted
          :want-outcome-accounting want-outcomes
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

(deftest selected-wants-require-verified-terminal-outcome-accounting
  (let [receipt (feedback/receipt
                 (input :want-outcome-accounting
                        {:schema :wm/selected-want-outcome-v1
                         :status :refused
                         :reason :selected-want-after-observation-missing}))]
    (is (empty? (get-in receipt [:patterns :applications])))
    (is (= :refused (:status receipt)))
    (is (empty? (get-in receipt [:patterns :positive-reinforcement])))
    (is (= :selected-want-after-observation-missing
           (get-in receipt [:blocker :kind])))
    (is (= :refused
           (get-in receipt [:mission-state :execution-outcomes :status])))
    (is (empty? (get-in receipt [:mission-state :reached-wants])))
    (is (map? (get-in receipt [:mission-state :construction-reachability])))))

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

;; The selected action of run 2026-10-05-c9d25d6a (four wants, four patterns),
;; copied from the run record into a tracked fixture. The want accounting is
;; produced by the real prediction, comparison and accounting functions from
;; measurements in which the wants named in REACHED were observed true and the
;; rest false. With REACHED empty this is what the run's own measurement gave:
;; all four mission lines were still unticked after the work.
(defn- recorded-run-input [reached]
  (let [{:keys [action selection-certificate]}
        (edn/read-string
         (slurp "test/fixtures/selected-want-outcome/2026-10-05-c9d25d6a-action.edn"))
        prediction (token-outcome/freeze-prediction
                    {:action action :selection-certificate selection-certificate})
        artifact-sha "recorded-action-test-artifact"
        measurements
        (mapv (fn [{:keys [token]}]
                (let [locator (get (:observation-locators action) token)]
                  {:token token :declared-locator locator
                   :after-locator (assoc locator :sha artifact-sha)
                   :result {:observed (contains? reached token)
                            :evidence {:resolved-sha artifact-sha}}}))
              (:wanted prediction))
        comparison (token-outcome/compare-outcomes prediction measurements artifact-sha)
        step (:id (first (:precedence action)))]
    {:run-id "2026-10-05-c9d25d6a"
     :selected-action action
     :outcome :grounded-change
     :accepted-increment {:accepted? :no-acceptance-declared
                          :criterion-step {:id step :source :recorded-decision}}
     :want-outcome-accounting (want-outcome/receipt {:selected-action action
                                                     :token-comparison comparison})
     :d-task-enactment admitted-enactment
     :artifact {:repo "/repo" :commit "abc1234"}}))

(deftest grounded-close-with-no-want-reached-does-not-reinforce
  (let [in (recorded-run-input #{})
        action (:selected-action in)
        patterns (mapv :id (:precedence action))
        receipt (feedback/receipt in)
        metadata (feedback/construction-metadata {:events [receipt]})
        target-metadata (get metadata (:target action))]
    (is (= :verified (get-in in [:want-outcome-accounting :status])))
    (is (= 4 (count (get-in in [:want-outcome-accounting :by-class :untouched]))))
    (is (= :verified (:status receipt)))
    (is (= patterns (mapv :pattern (get-in receipt [:patterns :applications]))))
    (is (= #{:no-want-effect}
           (set (map :status (get-in receipt [:patterns :applications])))))
    (is (= #{:none}
           (set (map :reinforcement (get-in receipt [:patterns :applications])))))
    (is (empty? (get-in receipt [:patterns :positive-reinforcement])))
    (is (empty? (get-in receipt [:patterns :selected-only])))
    (is (= {:status :absent :reason :verified-grounded-work} (:blocker receipt)))
    (doseq [pattern patterns]
      (is (= {:successful-applications 0 :supporting-attestations 0
              :incomplete-applications 0 :no-want-effect-applications 1
              :selected-only 0}
             (get-in target-metadata [:patterns pattern]))))
    (is (= 1.0 (:factor (feedback/pattern-evidence-prior target-metadata action)))
        "the habit factor of the same cascade is unchanged by this close")))

(deftest grounded-close-with-one-want-reached-reinforces
  (let [none (recorded-run-input #{})
        action (:selected-action none)
        reached-want [(:target action) (first (:want action))]
        in (recorded-run-input #{reached-want})
        receipt (feedback/receipt in)
        metadata (feedback/construction-metadata {:events [receipt]})]
    (is (= [reached-want] (get-in in [:want-outcome-accounting :by-class :reached])))
    (is (= 3 (count (get-in in [:want-outcome-accounting :by-class :untouched]))))
    (is (= [:successful :supporting-attestation :supporting-attestation
            :supporting-attestation]
           (mapv :status (get-in receipt [:patterns :applications]))))
    (is (= (mapv :id (:precedence action))
           (get-in receipt [:patterns :positive-reinforcement])))
    (is (< 1.0 (:factor (feedback/pattern-evidence-prior
                         (get metadata (:target action)) action))))))

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
