(ns futon2.aif.wm-run-facts-test
  (:require [clojure.test :refer [deftest is]]
            [wm-run-facts :as facts]))

(def snap {:open-missions #{"M-x"} :open-excursions #{} :open-tickets #{}
           :patterns #{"p1" "p2"} :seats #{"author"}
           :pins {:test true}})

(def record
  {:decision {:selection-certificate
              {:candidates [{:id :c1 :target "M-x" :precedence [:p1]
                             :g 1.0}]
               :policies [{:id :pi1}]}}
   :participants {:author "author"}
   :terminal-receipt {:outcome :changed}})

(deftest typed-not-supplied-increments-absence-count
  (let [base (facts/facts-for-record record "r" snap nil nil)
        planted (assoc-in record [:terminal-receipt :f]
                          {:status :not-supplied :reason :plant})
        changed (facts/facts-for-record planted "r" snap nil nil)]
    (is (= (inc (get-in base [:facts "pathAbsenceCount"]))
           (get-in changed [:facts "pathAbsenceCount"])))))

(deftest missing-slice-is-not-recomputable-not-zero-or-conforming
  (let [export (facts/facts-for-record record "r" snap nil nil)
        construction (get-in export [:facts "targetConstruction"])]
    (is (contains? construction "not-recomputable"))
    (is (not= 0 construction))
    (is (not= [] construction))))

(deftest recorded-construction-is-exported-with-the-actual-union-pool
  (let [carried (assoc-in record [:decision :selection-certificate :target-construction]
                          [{:target "M-x" :slice [:p1 :p2] :pool [:p1 :p2]
                            :slice-from-whole-library true :library-size 1436
                            :policy-count 2}])
        exported (:facts (facts/facts-for-record carried "r" snap nil nil))]
    (is (= [{"targets" ["M-x"] "slice" [":p1" ":p2"]
             "pool" [":p1" ":p2"] "sliceFromWholeLibrary" true
             "policyCount" 2}]
           (exported "targetConstruction")))
    (is (= 2 (exported "constructorPatternCount")))))

(deftest typed-absent-construction-carrier-is-never-read-as-an-empty-pool
  (let [carried (assoc-in record [:decision :selection-certificate :target-construction]
                          {:status :absent
                           :reason :query-time-construction-slice-not-recorded})
        exported (:facts (facts/facts-for-record carried "r" snap nil nil))]
    (is (contains? (exported "targetConstruction") "not-recomputable"))
    (is (contains? (exported "constructorPatternCount") "not-recomputable"))))

(deftest runfacts-field-set-is-exact
  (let [exported (:facts (facts/facts-for-record record "r" snap nil nil))]
    (is (= (set facts/run-fact-fields) (set (keys exported))))))

(deftest new-world-carrier-removes-record-nr-fields
  (let [world {:open-tasks {:missions {:ids ["M-x"]}
                            :excursions {:ids []} :tickets {:ids []}}
               :enumerated-tasks {:ids ["M-x"]}
               :seat-roster {:codex {:ids ["author"]}}
               :selection-input-digest "digest"
               :selection-ended-at "2026-09-30T00:00:00Z"
               :interpretation-issued-at nil
               :failures []}
        exported (:facts (facts/facts-for-record
                          (assoc record :world-at-selection world)
                          "r" snap nil nil))]
    (is (= ["M-x"] (exported "enumeratedTasks")))
    (is (= "digest" (exported "currentInputDigest")))
    (is (= ["author"] (exported "seatsAvailable")))
    (is (= "selectionBeforeInterpretation" (exported "interpretationOrder")))))

(deftest q4-is-exported-at-one-coherent-policy-grain
  (let [model {:horizon 4
               :preference-semantics :progressive
               :class-preference {1 {:ending/not-yet-evaluated 1}
                                  2 {:progress/zero 0.7 :progress/one 0.3}
                                  3 {:progress/zero 0.2 :progress/one 0.8}
                                  4 {:focused 0.55 :related 0.35}}}
        scored {:g 1.5
                :g-terms {:risk 1.0 :ambiguity 0.75
                          :expected-information-gain 0.25}
                :observation-model model}
        r (-> record
              (assoc-in [:decision :selection-certificate]
                        {:candidates [{:id :c1 :target "M-x" :g 1.5}]
                         :policies [{:id :pi1}]
                         :g-term-decomposition {:policies
                                                [{:terms {:A {:value model}}}]}
                         :scoring {0 scored}})
              (assoc-in [:decision :selection-law :posterior]
                        {{:id :c1 :target "M-x"} 1.0}))
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (is (= 4 (f "horizonLength")))
    (is (= [0 1 2 3] (f "preferenceSteps")))
    (is (= [1 2] (f "gradedPreferenceSteps")))
    (is (= "progressive" (f "preferenceSemantics")))
    (is (= {"risk" true "ambiguity" true "informationGain" true}
           (f "gTerms")))
    (is (= [1 1 1]
           (mapv f ["policiesWithRiskTerm" "policiesWithAmbiguityTerm"
                    "policiesWithInformationTerm"])))))

(deftest q4-distinguishes-zero-terms-from-missing-carriers
  (let [model {:horizon 2 :preference-semantics :terminal-only
               :class-preference {1 {:ending/not-yet-evaluated 1.0}
                                  2 {:ending/changed 1.0}}}
        scored {:g 0.0 :g-terms {:risk 0.0 :ambiguity 0.0
                                 :expected-information-gain 0.0}
                :observation-model model}
        r (-> record
              (assoc-in [:decision :selection-certificate]
                        {:candidates [{:id :c1 :target "M-x" :g 0.0}]
                         :policies [{:id :pi1}] :scoring {0 scored}})
              (assoc-in [:decision :selection-law :posterior]
                        {{:id :c1 :target "M-x"} 1.0}))
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (is (= [0 1] (f "preferenceSteps")))
    (is (= "terminal-only" (f "preferenceSemantics")))
    (is (= {"risk" true "ambiguity" true "informationGain" true} (f "gTerms")))))

(deftest q4-retains-declared-progressive-obligation
  (let [model {:horizon 2 :preference-semantics :progressive
               :class-preference {1 {:ending/not-yet-evaluated 1.0}
                                  2 {:ending/changed 1.0}}}
        scored {:g 0.0 :g-terms {:risk 0.0 :ambiguity 0.0
                                 :expected-information-gain 0.0}
                :observation-model model}
        r (-> record
              (assoc-in [:decision :selection-certificate]
                        {:candidates [{:id :c1 :target "M-x" :g 0.0}]
                         :policies [{:id :pi1}] :scoring {0 scored}})
              (assoc-in [:decision :selection-law :posterior]
                        {{:id :c1 :target "M-x"} 1.0}))
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (is (= "progressive" (f "preferenceSemantics")))
    (is (= [] (f "gradedPreferenceSteps")))))

(defn q4-two-policy-facts [model-a model-b]
  (let [scored (fn [model] {:g 0.0 :g-terms {:risk 0.0 :ambiguity 0.0
                                              :expected-information-gain 0.0}
                            :observation-model model})
        r (-> record
              (assoc-in [:decision :selection-certificate]
                        {:candidates [{:id :c1 :target "M-x" :g 0.0}
                                      {:id :c2 :target "M-x" :g 0.0}]
                         :policies [{:id :pi1} {:id :pi2}]
                         :scoring {0 (scored model-a) 1 (scored model-b)}})
              (assoc-in [:decision :selection-law :posterior]
                        {{:id :c1 :target "M-x"} 0.5
                         {:id :c2 :target "M-x"} 0.5}))]
    (:facts (facts/facts-for-record r "r" snap nil nil))))

(deftest q4-requires-explicit-authorized-preference-semantics
  (let [schedule {1 {:ending/not-yet-evaluated 1.0}}
        missing (q4-two-policy-facts {:horizon 1 :class-preference schedule}
                                     {:horizon 1 :class-preference schedule})
        unknown (q4-two-policy-facts {:horizon 1 :preference-semantics :mystery
                                      :class-preference schedule}
                                     {:horizon 1 :preference-semantics :mystery
                                      :class-preference schedule})]
    (is (contains? (missing "preferenceSemantics") "not-recomputable"))
    (is (contains? (unknown "preferenceSemantics") "not-recomputable"))))

(deftest q4-requires-one-common-model-at-policy-grain
  (let [terminal {:horizon 1 :preference-semantics :terminal-only
                  :class-preference {1 {:ending/changed 1.0}}}
        progressive {:horizon 1 :preference-semantics :progressive
                     :class-preference {1 {:progress/zero 1.0}}}
        horizon-two (assoc terminal :horizon 2
                          :class-preference {1 {:ending/not-yet-evaluated 1.0}
                                             2 {:ending/changed 1.0}})
        schedule-two (assoc terminal :class-preference {1 {:ending/refused 1.0}})]
    (doseq [export [(q4-two-policy-facts terminal progressive)
                    (q4-two-policy-facts terminal horizon-two)
                    (q4-two-policy-facts terminal schedule-two)]]
      (is (contains? (export "preferenceSemantics") "not-recomputable"))
      (is (contains? (export "horizonLength") "not-recomputable")))))

(deftest q4-refuses-to-mix-scoring-occurrences-with-reused-policy-labels
  (let [scored {:g 1.0
                :g-terms {:risk 1.0 :ambiguity 1.0
                          :expected-information-gain 1.0}
                :observation-model {:horizon 1
                                    :class-preference {1 {:focused 1.0}}}}
        r (-> record
              (assoc-in [:decision :selection-certificate]
                        {:candidates [{:id :c1 :target "M-a" :g 1.0}
                                      {:id :c1 :target "M-b" :g 1.0}]
                         :policies [{:id :pi1}]
                         :scoring {0 scored 1 scored}})
              (assoc-in [:decision :selection-law :posterior]
                        {{:id :c1 :target "M-a"} 1.0}))
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (doseq [field ["gTerms" "policiesWithRiskTerm"
                   "policiesWithAmbiguityTerm" "policiesWithInformationTerm"]]
      (is (contains? (f field) "not-recomputable") field))))

(deftest q7-counts-only-the-enacted-candidate-and-receipt-path
  (let [chosen {:target "M-selected" :candidate :C1
                :precedence [:pattern/selected]}
        selected {:id {:target "M-selected" :candidate :C1
                       :precedence [:pattern/selected]}
                  :selected-input {:status :absent}}
        rejected {:id {:target "M-rejected" :candidate :C2
                       :precedence [:pattern/rejected]}
                  :large-alternative-tree
                  [{:status :absent} {:outcome :failed} {:kind :typed-refusal}]}
        r {:decision {:chosen chosen
                      :selection-certificate {:candidates [rejected selected]
                                              :parameter-novelty
                                              (repeat 100 {:status :absent})}}
           :selection-event {:status :ok}
           :terminal-receipt {:review {:outcome :refused}}}]
    (is (= [[:terminal-receipt :review]
            [:decision :selected-candidate :selected-input]]
           (facts/absence-paths r)))))

(deftest q7-counts-the-abstention-path-when-nothing-was-chosen
  (let [r {:decision {:chosen nil
                      :abstention {:kind :typed-refusal
                                   :reason {:status :missing}}
                      :selection-certificate
                      {:candidates [{:status :absent}]}}}]
    (is (= [[:decision :abstention]
            [:decision :abstention :reason]]
           (facts/absence-paths r)))))

(deftest q8-uses-global-action-identities-not-reused-local-labels
  (let [a {:id :C1 :target "M-a" :precedence [:p]}
        b {:id :C1 :target "M-b" :precedence [:p]}
        r (-> record
              (assoc-in [:decision :selection-certificate :candidates]
                        [{:id a :g 1.0} {:id b :g 2.0}])
              (assoc-in [:decision :selection-law :posterior]
                        {a 0.25 b 0.75}))
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (is (= 2 (count (f "constructedCascades"))))
    (is (= 2 (count (f "comparedPolicies"))))
    (is (= #{"M-a" "M-b"} (set (f "targetsWithG"))))))
