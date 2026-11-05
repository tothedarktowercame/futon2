(ns futon2.aif.wm-run-facts-test
  (:require [clojure.edn :as edn]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [wm-run-facts :as facts]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.outer-menu :as outer-menu]
            [futon2.aif.outer-score :as outer-score]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.preference-registry :as preference-registry]
            [futon2.aif.policy :as policy]
            [futon2.aif.full-loop-runner]
            [futon2.aif.selection-record :as selection-record]))

(def click-12-selection-record
  "/home/joe/code/futon2/data/wm-runs/selection-record-2026-11-01-wm-click-878186ce-4e68-411b-8d2e-581c559b72cb.edn")

(def click-1-selection-record
  "/home/joe/code/futon2/data/wm-runs/selection-record-2026-11-01-wm-click-0068e006-6fc1-4f29-8a72-5d307748ce12.edn")

(def click-12-reselection-export (atom nil))

(def snap {:open-missions #{"M-x"} :open-excursions #{} :open-tickets #{}
           :patterns #{"p1" "p2"} :seats #{"author"}
           :pins {:test true}})

(deftest fallback-snapshot-omits-requisitions-but-keeps-parent-tickets
  (let [requisition {:id "E-requisition" :path "/fixture/E-requisition.md"
                     :status-class :open :requisition-record? true
                     :source {:path "/fixture/E-requisition.md" :sha256 "fixture"}}
        parent-ticket {:id "T-parent-only" :path "/fixture/T-parent-only.md"
                       :status-class :live :parent "E-parent"
                       :requisition-record? false}
        snapshot (with-redefs [mission-registry/load-missions (constantly {:missions []})
                               mission-registry/load-excursions (constantly {:excursions [requisition]})
                               mission-registry/load-tickets (constantly {:tickets [parent-ticket]})]
                   (facts/snapshot {:code-root "/fixture"
                                    :agency-json "/fixture/missing-agency.json"}))]
    (is (= #{} (:open-excursions snapshot)))
    (is (= #{"T-parent-only"} (:open-tickets snapshot)))))

(def record
  {:decision {:selection-certificate
              {:candidates [{:id :c1 :target "M-x" :precedence [:p1]
                             :g 1.0}]
               :policies [{:id :pi1}]}}
   :participants {:author "author"}
   :terminal-receipt {:outcome :changed}})

(deftest typed-not-supplied-increments-absence-count
  (let [base (facts/facts-for-record record "r" snap)
        planted (assoc-in record [:terminal-receipt :f]
                          {:status :not-supplied :reason :plant})
        changed (facts/facts-for-record planted "r" snap)]
    (is (= (inc (get-in base [:facts "pathAbsenceCount"]))
           (get-in changed [:facts "pathAbsenceCount"])))))

(deftest noncanonical-class-history-is-a-q7-absence-but-impossibility-is-not
  (let [chosen {:target "M-x" :candidate :c1 :precedence [:p1]}
        with-status (fn [status]
                      {:decision {:chosen chosen
                                  :selection-certificate
                                  {:candidates [{:id {:target "M-x" :candidate :c1
                                                      :precedence [:p1]}
                                                 :class-f {:status status}}]}}})]
    (is (= [[:decision :selected-candidate :class-f]]
           (facts/absence-paths
            (with-status :history-unavailable-noncanonical-policy-key))))
    (is (empty? (facts/absence-paths
                 (with-status :observation-impossible-under-policy))))
    (is (= [[:decision :selected-candidate :class-f]]
           (facts/absence-paths (with-status :history-index-absent))))
    (is (= [[:decision :selected-candidate :class-f]]
           (facts/absence-paths (with-status :history-index-incomplete))))))

(deftest missing-slice-is-not-recomputable-not-zero-or-conforming
  (let [export (facts/facts-for-record record "r" snap)
        construction (get-in export [:facts "targetConstruction"])]
    (is (contains? construction "not-recomputable"))
    (is (not= 0 construction))
    (is (not= [] construction))))

(deftest recorded-construction-is-exported-with-the-actual-union-pool
  (let [carried (assoc-in record [:decision :selection-certificate :target-construction]
                          [{:target "M-x" :slice [:p1 :p2] :pool [:p1 :p2]
                            :slice-from-whole-library true :library-size 1436
                            :policy-count 2}])
        exported (:facts (facts/facts-for-record carried "r" snap))]
    (is (= [{"targets" ["M-x"] "slice" [":p1" ":p2"]
             "pool" [":p1" ":p2"] "sliceFromWholeLibrary" true
             "policyCount" 2}]
           (exported "targetConstruction")))
    (is (= 2 (exported "constructorPatternCount")))))

(deftest typed-absent-construction-carrier-is-never-read-as-an-empty-pool
  (let [carried (assoc-in record [:decision :selection-certificate :target-construction]
                          {:status :absent
                           :reason :query-time-construction-slice-not-recorded})
        exported (:facts (facts/facts-for-record carried "r" snap))]
    (is (contains? (exported "targetConstruction") "not-recomputable"))
    (is (contains? (exported "constructorPatternCount") "not-recomputable"))))


(deftest the-projector-labels-a-tick-record-as-work
  (let [planted (assoc record :kind "selection" :run-kind :selection)
        exported (:facts (facts/facts-for-record planted "r" snap))]
    (is (= "work" (exported "kind")))
    (is (not (contains? exported "selectionPath")))))

;; The construction, G and selection paths of the 2026-11-01 record ac5bc477,
;; verbatim, cut down by keeping only those paths: the record itself is 5.9 MB.
(def live-record (facts/read-edn "test/fixtures/wm-run-facts/ac5bc477-carriers.edn"))
(def live-facts (:facts (facts/facts-for-record live-record "r" snap)))
;; `scripts/wm_run_facts.clj --run` on the whole record, for the fields that need
;; the Q4 and Q6 carriers intact.  Regenerate it rather than editing it.
(def live-export
  (get (json/parse-string (slurp "test/fixtures/wm-run-facts/ac5bc477-export.json")) "facts"))

(defn- live-certificate [f]
  (update-in live-record [:decision :selection-certificate] f))

(defn- reason [x] (get x "not-recomputable"))

(deftest production-preference-registry-shape-reaches-the-projector
  (let [carrier (preference-registry/load!)
        planted (assoc-in record
                          [:decision :selection-certificate
                           :factorized-preference]
                          carrier)
        exported (:facts (facts/facts-for-record planted "r" snap))]
    (is (= 4001 (get-in exported ["factorizedPreference"
                                  "calibration-authorization"])))
    (is (= [{"receipt" 4001
             "calibration" {"identity" 2001 "version" 1
                            "authority" 3001 "registration-token" 3101
                            "focused-cost" 0 "related-cost" 1
                            "unrelated-cost" 2}
             "authorized-by" 3001}]
           (exported "calibrationAuthorizations")))
    (is (= [{"receipt" 4002 "identity" 2002 "version" 1
             "authority" 3001}]
           (exported "factorizationAuthorizations")))))

(deftest construction-comes-from-the-recorded-census
  (let [[tc :as all] (live-facts "targetConstruction")]
    (is (= 1 (count all)))
    (is (= ["E-comprehension-foundation"] (tc "targets")))
    (is (= 0 (tc "action")))
    (is (true? (tc "sliceFromWholeLibrary")))
    (is (= (set (tc "slice")) (set (tc "pool"))))
    (is (= 7 (count (tc "pool")) (live-facts "constructorPatternCount")))
    (is (= 3 (tc "policyCount") (count (tc "policies"))))
    (is (= "9992e2c62e41453d9ae33ba6c660b20195c91006759c84a3154af858436612e5" (tc "graph")))
    (is (= "5f41e1a3896fa08f6d79105551c66b8910e5363cdfaeb4af93b76679624488c0" (tc "library")))
    (is (= 1527 (live-facts "libraryPatternCount")))
    (is (= (set (tc "policies")) (set (live-facts "constructedCascades"))))))

(deftest every-side-names-a-policy-the-same-way
  (let [[tc] (live-export "targetConstruction")
        compared (set (live-export "comparedPolicies"))]
    (is (= (set (tc "policies")) (set (live-export "constructedCascades")) compared))
    (is (contains? compared (get-in live-export ["currentChoice" "cascade"])))
    (is (= compared (set (map #(% "policy") (live-export "policyG")))))
    (is (= compared (set (map #(% "policy") (live-export "policyPrediction")))))
    (is (= compared (set (map #(% "policy") (live-export "policyPreferences")))))))

(deftest a-slice-pattern-at-other-bytes-is-not-from-the-whole-library
  (let [pattern (-> (live-certificate identity)
                    (get-in [:decision :selection-certificate :q2-construction :entries 0 :slice 0 :pattern]))
        changed (assoc-in live-record
                          [:decision :selection-certificate :q2-construction :library :patterns pattern]
                          "other-bytes")
        exported (:facts (facts/facts-for-record changed "r" snap))]
    (is (false? (get-in exported ["targetConstruction" 0 "sliceFromWholeLibrary"])))))

(deftest policy-g-is-exact-with-the-source-double-beside-it
  (let [rows (live-facts "policyG")
        chosen (first (filter #(= 1635919 (get-in % ["g" "numerator"])) rows))]
    (is (= 3 (count rows)))
    (is (= [1635919 1796620 1796620] (sort (map #(get-in % ["g" "numerator"]) rows))))
    (is (= 1.6359186228431266 (get-in chosen ["g" "double"])))
    (is (every? #(= 1000000 (get-in % ["g" "denominator"])) rows))
    (is (= (get-in live-export ["currentChoice" "cascade"]) (chosen "policy")))
    (is (= 1000000 (live-facts "policyGScale")))))

(deftest policy-prediction-digests-what-the-law-scored
  (let [rows (live-facts "policyPrediction")]
    (is (= 3 (count rows)))
    (is (= 2 (count (set (map #(% "digest") rows)))))))

(deftest stopping-has-no-g-on-the-inner-path
  (doseq [field ["abstentionG" "abstentionPrediction"]]
    (is (= "no stopped-kernel G is computed on the inner path (L4b row 4; L5)"
           (reason (live-facts field))))))

(deftest an-empty-census-names-the-path
  (let [emptied (live-certificate #(assoc-in % [:q2-construction :entries] []))
        exported (:facts (facts/facts-for-record emptied "r" snap))]
    (is (= "[:decision :selection-certificate :q2-construction :entries] empty"
           (reason (exported "targetConstruction"))
           (reason (exported "constructorPatternCount"))))))

(deftest an-absent-census-names-the-path
  (let [exported (:facts (facts/facts-for-record record "r" snap))]
    (is (= "[:decision :selection-certificate :q2-construction] absent"
           (reason (exported "targetConstruction"))))
    (is (str/includes? (reason (exported "libraryPatternCount")) ":library :count"))))

(deftest one-unjoined-candidate-makes-the-carrier-not-recomputable
  (let [orphaned (live-certificate #(assoc-in % [:candidates 0 :id :id] "policy-with-no-row"))
        exported (:facts (facts/facts-for-record orphaned "r" snap))]
    (is (str/includes? (reason (exported "policyG")) "policy-with-no-row"))
    (is (str/includes? (reason (exported "policyPrediction")) "policy-with-no-row"))))

(deftest a-candidate-without-predictions-or-g-is-named
  (let [no-steps (live-certificate #(assoc-in % [:candidates 1 :class-emission :certificate :steps] []))
        no-g (live-certificate #(assoc-in % [:candidates 1 :g] nil))]
    (is (str/includes? (reason (get (:facts (facts/facts-for-record no-steps "r" snap)) "policyPrediction"))
                       "no recorded progress or class predictions"))
    (is (str/includes? (reason (get (:facts (facts/facts-for-record no-g "r" snap)) "policyG"))
                       "no finite :g"))))

(deftest doubles-within-a-scale-unit-become-one-rational
  (is (= (get (facts/exact-g 1.23456701) "numerator")
         (get (facts/exact-g 1.23456708) "numerator")
         1234567))
  (is (= -1500000 (get (facts/exact-g -1.5) "numerator")))
  (is (nil? (facts/exact-g Double/NaN)))
  (is (nil? (facts/exact-g nil))))

(deftest runfacts-field-set-is-exact
  (let [exported (:facts (facts/facts-for-record record "r" snap))]
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
                          "r" snap))]
    (is (= ["M-x"] (exported "enumeratedTasks")))
    ;; Q6 reads its digest from the run's :q6 carrier, computed before the
    ;; one selector; the census digest is taken after selection and is not
    ;; a substitute for it
    (is (contains? (exported "currentInputDigest") "not-recomputable"))
    (is (= ["author"] (exported "seatsAvailable")))
    (is (= "selectionBeforeInterpretation" (exported "interpretationOrder")))))

(deftest q4-is-exported-at-one-coherent-policy-grain
  (let [action {:id :c1 :target "M-x"}
        model {:horizon 4
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
                        {:candidates [{:id action :target "M-x" :g 1.5}]
                         :policies [{:id :pi1}]
                         :g-term-decomposition {:policies
                                                [{:terms {:A {:value model}}}]}
                         :scoring {0 scored}})
              (assoc-in [:decision :selection-law :posterior]
                        {action 1.0}))
        f (:facts (facts/facts-for-record r "r" snap))]
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
  (let [action {:id :c1 :target "M-x"}
        model {:horizon 2 :preference-semantics :terminal-only
               :class-preference {1 {:ending/not-yet-evaluated 1.0}
                                  2 {:ending/changed 1.0}}}
        scored {:g 0.0 :g-terms {:risk 0.0 :ambiguity 0.0
                                 :expected-information-gain 0.0}
                :observation-model model}
        r (-> record
              (assoc-in [:decision :selection-certificate]
                        {:candidates [{:id action :target "M-x" :g 0.0}]
                         :policies [{:id :pi1}] :scoring {0 scored}})
              (assoc-in [:decision :selection-law :posterior]
                        {action 1.0}))
        f (:facts (facts/facts-for-record r "r" snap))]
    (is (= [0 1] (f "preferenceSteps")))
    (is (= "terminal-only" (f "preferenceSemantics")))
    (is (= {"risk" true "ambiguity" true "informationGain" true} (f "gTerms")))))

(deftest q4-retains-declared-progressive-obligation
  (let [action {:id :c1 :target "M-x"}
        model {:horizon 2 :preference-semantics :progressive
               :class-preference {1 {:ending/not-yet-evaluated 1.0}
                                  2 {:ending/changed 1.0}}}
        scored {:g 0.0 :g-terms {:risk 0.0 :ambiguity 0.0
                                 :expected-information-gain 0.0}
                :observation-model model}
        r (-> record
              (assoc-in [:decision :selection-certificate]
                        {:candidates [{:id action :target "M-x" :g 0.0}]
                         :policies [{:id :pi1}] :scoring {0 scored}})
              (assoc-in [:decision :selection-law :posterior]
                        {action 1.0}))
        f (:facts (facts/facts-for-record r "r" snap))]
    (is (= "progressive" (f "preferenceSemantics")))
    (is (= [] (f "gradedPreferenceSteps")))))

(defn q4-two-policy-facts [model-a model-b]
  (let [action-a {:id :c1 :target "M-x"}
        action-b {:id :c2 :target "M-x"}
        scored (fn [model] {:g 0.0 :g-terms {:risk 0.0 :ambiguity 0.0
                                              :expected-information-gain 0.0}
                            :observation-model model})
        r (-> record
              (assoc-in [:decision :selection-certificate]
                        {:candidates [{:id action-a :target "M-x" :g 0.0}
                                      {:id action-b :target "M-x" :g 0.0}]
                         :policies [{:id :pi1} {:id :pi2}]
                         :scoring {0 (scored model-a) 1 (scored model-b)}})
              (assoc-in [:decision :selection-law :posterior]
                        {action-a 0.5 action-b 0.5}))]
    (:facts (facts/facts-for-record r "r" snap))))

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

(deftest q4-requires-exact-scoring-to-posterior-identity-bijection
  (let [model {:horizon 1 :preference-semantics :terminal-only
               :class-preference {1 {:ending/changed 1.0}}}
        score {:g 0.0 :g-terms {:risk 0.0 :ambiguity 0.0
                                :expected-information-gain 0.0}
               :observation-model model}
        a {:id :a :target "M-x"} b {:id :b :target "M-x"}
        c {:id :c :target "M-x"} d {:id :d :target "M-x"}
        facts-for (fn [candidates scoring posterior]
                    (:facts (facts/facts-for-record
                             (-> record
                                 (assoc-in [:decision :selection-certificate]
                                           {:candidates candidates
                                            :policies (mapv (fn [x] {:id x}) (keys posterior))
                                            :scoring scoring})
                                 (assoc-in [:decision :selection-law :posterior] posterior))
                             "r" snap)))
        disjoint (facts-for [{:id a} {:id b}] {0 score 1 score} {c 0.5 d 0.5})
        duplicate (facts-for [{:id a} {:id a}] {0 score 1 score} {a 0.5 b 0.5})
        outside (facts-for [{:id a} {:id b}] {0 score 2 score} {a 0.5 b 0.5})
        exact (facts-for [{:id a} {:id b}] {0 score 1 score} {a 0.5 b 0.5})]
    (doseq [export [disjoint duplicate outside]]
      (is (contains? (export "gTerms") "not-recomputable")))
    (is (= {"risk" true "ambiguity" true "informationGain" true}
           (exact "gTerms")))
    (is (= "terminal-only" (exact "preferenceSemantics")))))

(deftest q4-gpolicy-model-fallback-requires-the-same-bound-identity
  (let [a {:id :a :target "M-x"} b {:id :b :target "M-x"}
        model {:horizon 1 :preference-semantics :terminal-only
               :class-preference {1 {:ending/changed 1.0}}}
        score {:g 0.0 :g-terms {:risk 0.0 :ambiguity 0.0
                                :expected-information-gain 0.0}}
        r (-> record
              (assoc-in [:decision :selection-certificate]
                        {:candidates [{:id a}] :policies [{:id a}]
                         :scoring {0 score}
                         :g-term-decomposition {:policies [{:id b
                                                            :terms {:A {:value model}}}]}})
              (assoc-in [:decision :selection-law :posterior] {a 1.0}))
        export (:facts (facts/facts-for-record r "r" snap))]
    (is (contains? (export "preferenceSemantics") "not-recomputable"))))

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
        f (:facts (facts/facts-for-record r "r" snap))]
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

(defn prefix-selected-candidate [prefix-status]
  (let [action {:target "M-selected" :candidate :C1 :precedence [:pattern/selected]}
        prefix {:schema :wm/policy-prefix-evidence-v1 :policy action
                :status prefix-status :f 1.25 :route :bootstrap}]
    {:id action :f 1.25 :f-status :computed
     :inputs {:f {:status :computed :value 1.25
                  :presence :prefix-receipt :reason nil}}
     :f-prefix prefix
     :computed-f {:f {:value 0.0 :status :not-supplied
                      :reason :class-model-unconditioned-at-selection}
                  :value {:value 0.0 :status :not-supplied
                          :reason :class-model-unconditioned-at-selection}}}))

(defn q7-prefix-record [candidate]
  {:decision {:chosen (:id candidate)
              :selection-certificate {:candidates [candidate]}}})

(deftest q7-excludes-only-superseded-computed-f-after-exact-prefix-consumption
  (let [candidate (prefix-selected-candidate :computed)]
    (is (empty? (facts/absence-paths (q7-prefix-record candidate))))
    (is (= [[:decision :selected-candidate :other]]
           (facts/absence-paths
            (q7-prefix-record (assoc candidate :other {:status :absent})))))))

(deftest q7-prefix-exclusion-requires-an-exact-consumed-receipt
  (let [candidate (prefix-selected-candidate :computed)]
    (doseq [changed [(dissoc candidate :f-prefix)
                     (assoc-in candidate [:f-prefix :status] :refused)
                     (assoc-in candidate [:f-prefix :policy :candidate] :other)
                     (assoc candidate :f 9.0)
                     (assoc-in candidate [:inputs :f :value] 9.0)]]
      (is (some #{[:decision :selected-candidate :computed-f :f]}
                (facts/absence-paths (q7-prefix-record changed)))))))

(deftest q7-positive-real-selector-and-export-carry-canonical-prefix-f
  (let [action {:kind :cascade-candidate :id :C1 :target "M-selected"
                :precedence [:pattern/selected]}
        decision (policy/select-action-cascades
                  [{:action action :controller-score 0
                    :f-prefix {:schema :wm/policy-prefix-evidence-v1
                               :policy action :status :computed :f 1.25}}]
                  {:beta 1})
        candidate (first (get-in decision [:selection-certificate :candidates]))
        r (assoc record :decision (assoc decision :chosen (:action decision)))
        export (facts/facts-for-record r "r" snap)
        entry ((ns-resolve 'futon2.aif.full-loop-runner 'selected-entry)
               {:decision decision})]
    (is (= 1.25 (:f-consumed candidate)))
    (is (= (:action decision) (:action entry)))
    (is (= 0 (get-in export [:facts "pathAbsenceCount"])))
    (is (empty? (get-in export [:diagnostics :absenceGroups])))))

(deftest q7-adversarial-no-flight-carry-refusal-is-not-enacted
  (let [action {:kind :cascade-candidate :id :C1 :target "M-selected"
                :precedence [:pattern/selected]}
        refusal (try
                  (policy/select-action-cascades
                   [{:action action :controller-score 0
                     :f-prefix {:schema :wm/policy-prefix-evidence-v1
                                :policy action :status :not-supplied
                                :reason :no-flight-records
                                :conditioning {:conditioning-status :not-run
                                               :reason :carry-admission-refused}}}]
                   {:beta 1})
                  nil
                  (catch clojure.lang.ExceptionInfo e (ex-data e)))
        candidate (assoc (prefix-selected-candidate :not-supplied)
                         :f nil :f-status :not-supplied
                         :f-consumed {:status :absent :reason :not-supplied}
                         :f-prefix {:schema :wm/policy-prefix-evidence-v1
                                    :policy (:id (prefix-selected-candidate :not-supplied))
                                    :status :not-supplied :reason :no-flight-records
                                    :conditioning {:conditioning-status :not-run
                                                   :reason :carry-admission-refused}}
                         :inputs {:f {:status :not-supplied :value nil
                                      :presence :prefix-receipt
                                      :reason :no-flight-records}})
        r (q7-prefix-record candidate)
        export (facts/facts-for-record r "r" snap)
        groups (get-in export [:diagnostics :absenceGroups])
        selected-entry (ns-resolve 'futon2.aif.full-loop-runner 'selected-entry)
        legacy-decision {:action (:id candidate)
                         :selection-law {:applied :cascade-selection-posterior}
                         :selection-certificate {:candidates [candidate]}}]
    (is (= :free-energy-not-supplied (get-in refusal [:refusal :kind])))
    (is (nil? (selected-entry {:decision legacy-decision})))
    (is (= 5 (get-in export [:facts "pathAbsenceCount"])))
    (is (= 1 (count groups)))
    (is (= :selected-policy-prefix-f/no-flight-records (:root-cause (first groups))))
    (is (= :carry-admission-refused (:conditioning-reason (first groups))))
    (is (= (set (facts/absence-paths r))
           (set (mapcat :paths groups))))))

(deftest q7-cascade-decision-without-candidate-certificate-is-not-enacted
  (let [action {:kind :cascade-candidate :id :C1 :target "M-selected"
                :precedence [:pattern/selected]}
        selected-entry (ns-resolve 'futon2.aif.full-loop-runner 'selected-entry)]
    (is (nil? (selected-entry
               {:decision {:action action
                           :selection-law {:applied :cascade-selection-posterior}}})))
    (is (nil? (selected-entry
               {:decision {:action action
                           :selection-law {:applied :cascade-selection-posterior}
                           :selection-certificate {:candidates []}}})))))

(deftest q7-counts-the-abstention-path-when-nothing-was-chosen
  (let [r {:decision {:chosen nil
                      :abstention {:kind :typed-refusal
                                   :reason {:status :missing}}
                      :selection-certificate
                      {:candidates [{:status :absent}]}}}]
    (is (= [[:decision :abstention]
            [:decision :abstention :reason]]
           (facts/absence-paths r)))))

(deftest q7-retains-reviewer-falsifier-refusals-on-the-enacted-path
  (let [r {:decision {:chosen {:target "M-selected" :candidate :C1}
                      :selection-certificate
                      {:candidates [{:id {:target "M-selected" :candidate :C1}}]}}
           :failure {:kind :reviewer-falsifier-failed
                     :detail {:reviewer-falsifier {:status :refused}
                              :reviewer-falsifier-verification
                              {:kind :typed-refusal}}}}]
    (is (= [[:failure :detail :reviewer-falsifier]
            [:failure :detail :reviewer-falsifier-verification]]
           (facts/absence-paths r)))))

(deftest q7-does-not-count-population-diagnostics-without-selection
  (let [r {:decision {:chosen nil
                      :abstention {:status :not-abstained}
                      :selection-certificate
                      {:candidates (repeat 100 {:status :absent})
                       :scoring {0 {:diagnostic {:kind :typed-refusal}}}}}}]
    (is (empty? (facts/absence-paths r)))))

(deftest cascades-without-g-share-the-constructed-id-space
  (let [no-g (live-certificate #(assoc-in % [:candidates 1 :g] nil))
        exported (:facts (facts/facts-for-record no-g "r" snap))
        without (exported "cascadesWithoutG")]
    (is (= 1 (count without)))
    (is (contains? (set (exported "constructedCascades")) (first without))
        "a cascade named as lacking G is one of the constructed cascades, by the same id")
    (is (= [] (live-facts "cascadesWithoutG")))))

;; -- the selection run -----------------------------------------------------------

;; `data/wm-learning/outer-makeup.edn` at the sha the record pins.
(def makeup-path "test/fixtures/wm-run-facts/outer-makeup-f3125835.edn")

(defn- selection-of
  ([r] (selection-of r makeup-path))
  ([r path] (:facts (facts/selection-facts-for-record r "r" snap {:makeup-path path}))))

(def selection-live (selection-of live-record))

(def live-execution-id
  "ea1-6c24e2ab05edb2e75c0bbb01000beebce6a5486e90b91622d9528cb14759164d--attempt-002")
(def live-phase-log "test/fixtures/wm-run-facts/ac5bc477-phases.edn.log")

(defn- temp-phase-log [events]
  (let [file (java.io.File/createTempFile "wm-phases" ".edn.log")]
    (.deleteOnExit file)
    (spit file (str (str/join "\n" (map pr-str events)) "\n"))
    (.getPath file)))

(defn- with-selection [& kvs]
  (reduce (fn [r [path value]] (assoc-in r (into [:outer/selection] path) value))
          live-record (partition 2 kvs)))

(defn- temp-makeup [text]
  (let [file (java.io.File/createTempFile "outer-makeup" ".edn")]
    (.deleteOnExit file)
    (spit file text)
    (.getPath file)))

(defn- sha-of [path] (load-identity/sha256 (.getBytes ^String (slurp path) "UTF-8")))

(def menu-ids
  (set (map action-identity/digest
            [:what-others-wait-on-first :only-what-can-start-now :find-out-before-building])))

(deftest the-projector-labels-a-selection-run
  (is (= "selection" (selection-live "kind")))
  (is (= (set facts/run-fact-fields)
         (disj (set (keys selection-live)) "selectionPath"))))

(deftest the-field-is-admitted-and-excluded
  (is (= 626 (count (selection-live "enumeratedTasks")))))

(deftest the-menu-comes-from-the-pinned-makeup
  (is (= menu-ids (set (selection-live "comparedPolicies"))
         (set (selection-live "constructedCascades"))))
  (is (= [] (selection-live "cascadesWithoutG"))))

(deftest selection-g-is-exact-by-menu-position
  (let [rows (selection-live "policyG")]
    (is (= [3212121 3212121 3212121] (map #(get-in % ["g" "numerator"]) rows)))
    (is (every? #(= 1000000 (get-in % ["g" "denominator"])) rows))
    (is (= menu-ids (set (map #(% "policy") rows))))))

(deftest the-chosen-cascade-is-on-the-menu
  (is (= "E-comprehension-foundation" (get-in selection-live ["currentChoice" "target"])))
  (is (contains? (set (selection-live "comparedPolicies"))
                 (get-in selection-live ["currentChoice" "cascade"]))))

(deftest a-makeup-at-another-sha-says-nothing-about-this-run
  (let [path (temp-makeup (str (slurp makeup-path) "\n"))
        changed (selection-of live-record path)
        pin (get-in live-record [:outer/selection :makeup/sha256])]
    (doseq [field ["comparedPolicies" "constructedCascades" "policyG" "cascadesWithoutG"]]
      (is (= (str "outer-makeup.edn at pinned sha " pin " not available")
             (reason (changed field)))))
    (is (str/includes? (reason ((selection-of live-record "/no/such/file") "policyG")) pin))))

(deftest a-score-count-off-the-menu-names-both-counts
  (let [changed (selection-of (with-selection [:cascade-scores] [1.0 2.0]))]
    (is (= "2 scores for 3 menu cascades" (reason (changed "policyG"))))
    (is (= 1 (count (changed "cascadesWithoutG"))))))

(deftest a-chosen-cascade-that-picked-nothing-is-the-empty-field-decision
  (is (= "abstain-empty-field"
         ((selection-of (with-selection [:cascade-picks] [:no-pick :no-pick :no-pick]))
          "currentChoice")))
  (is (= "abstain-empty-field"
         ((selection-of (with-selection [:selection] {})) "currentChoice"))))

(deftest the-pick-relation-is-observed-only-when-its-class-is-known
  (let [path (fn [relation]
               (get-in (selection-of (with-selection [:selected-relation] relation))
                       ["selectionPath" "pickRelation"]))]
    (is (= "absent" (get-in selection-live ["selectionPath" "pickRelation"])))
    (is (= "absent" (path :something-else)))
    (is (contains? (path :associated) "observed"))))

(deftest pick-reach-compares-the-record-with-the-pinned-makeup
  (is (= "defaulted" (get-in selection-live ["selectionPath" "pickReach"])))
  (let [path (temp-makeup (str/replace (slurp makeup-path) ":default-reach 1/2" ":default-reach 1/3"))
        other (selection-of (with-selection [:makeup/sha256] (sha-of path)) path)]
    ;; the rule: a reach that is neither measured nor the default is absent
    (is (= "absent" (get-in other ["selectionPath" "pickReach"]))))
  (is (= "absent" (get-in (selection-of (with-selection [:selection :selected :reach] nil))
                          ["selectionPath" "pickReach"])))
  (is (contains? (get-in (selection-of (with-selection [:reach-receipt] {:measured 2/4}))
                         ["selectionPath" "pickReach"])
                 "observed")))

(deftest learning-is-applied-from-its-receipt
  (let [path (selection-live "selectionPath")
        run "2026-11-01-c5590f95-a22e-4f21-a5e2-2cf47f393803"]
    (is (true? (path "learningApplied")))
    (is (= [[run "absent"]] (path "consumedRuns")))
    (is (= [run "absent"] (path "pendingRun")))))

(deftest a-classified-ending-is-observed
  (let [ending [:outer/learning :receipt :ending]
        classified (update-in live-record (into [:outer/selection] ending)
                              #(-> % (dissoc :classification-absence) (assoc :ending-class :focused)))
        [[_ provenance]] (get-in (selection-of classified) ["selectionPath" "consumedRuns"])]
    (is (contains? provenance "observed"))))

(deftest a-deferral-has-no-run-id-and-nothing-consumed
  (let [path ((selection-of (with-selection [:outer/learning]
                              {:status :deferred :reason :terminal-ending-class-absent}))
              "selectionPath")]
    (is (= [0 "absent"] (path "pendingRun")))
    (is (= [] (path "consumedRuns")))
    (is (false? (path "learningApplied")))))

(deftest nothing-pending-is-a-typed-path-with-nothing-consumed
  (let [path ((selection-of (with-selection [:outer/learning] {:status :none})) "selectionPath")]
    (is (not (contains? path "pendingRun")))
    (is (= [] (path "consumedRuns")))
    (is (false? (path "learningApplied")))
    (is (contains? path "receipt"))))

(deftest learning-absent-leaves-the-path-not-recomputable
  (is (contains? ((selection-of (with-selection [:outer/learning] nil)) "selectionPath")
                 "not-recomputable")))

(deftest selection-absences-count-the-receipt-and-the-stub
  (is (= 2 (selection-live "pathAbsenceCount")))
  (is (= ["[:outer/selection :outer/learning :receipt :ending :classification-absence]"
          "[:outer-task-selection]"]
         (get-in (facts/selection-facts-for-record live-record "r" snap {:makeup-path makeup-path})
                 [:diagnostics :absencePaths]))))

(deftest population-wide-exclusions-are-not-path-absences
  (let [crowded (with-selection [:outer-field/excluded] [{:target "t" :status :absent}])]
    (is (= 2 ((selection-of crowded) "pathAbsenceCount")))))

(deftest unavailable-selection-carriers-say-why
  (doseq [[field text] {"targetConstruction" "L4b row 11"
                        "policyPrediction" "L4b row 9"
                        "abstentionG" "L4b row 10"
                        "abstentionPrediction" "L4b row 10"
                        "previousChoice" "L4b row 13"
                        "currentInputDigest" "L4b row 13"
                        "gTerms" "L4b row 9"
                        ;; L5-4h review: no --phase-log means the canonical log; the
                        ;; absence reason names that path whatever is missing in it
                        ;; whichever carrier is missing (the log, or the execution identity
                        ;; that selects this run in it), the reason names what it needs
                        "interpretationOrder" "selection interpretation order needs"}]
    (is (str/includes? (reason (selection-live field)) text))))

(deftest live-11-01-phase-log-puts-selection-before-interpretation
  (let [record (assoc live-record :runner-execution/identity
                      {:kind :runner-execution :id live-execution-id})
        projected (facts/selection-facts-for-record
                   record "ac5bc477-carriers.edn" snap
                   {:makeup-path makeup-path :phase-log-path live-phase-log})]
    (is (= "selectionBeforeInterpretation"
           (get-in projected [:facts "interpretationOrder"])))
    (is (= live-phase-log (get-in projected [:sources "interpretationOrder"])))))

(deftest authoring-before-selection-projects-the-lean-counter-constructor
  (let [identity {:kind :runner-execution :id "planted-execution"}
        path (temp-phase-log
              [{:phase :occurrence-square-authoring :transition :start
                :execution-identity identity}
               {:phase :selection :transition :end :execution-identity identity}])]
    (is (= "interpretationBeforeSelection"
           (facts/selection-interpretation-order
            {:runner-execution/identity identity} path)))))

(deftest missing-phase-log-is-typed-absent-and-names-the-carrier
  (clojure.test/testing "a log that does not exist at the given path is a typed absence naming that path"
    (let [projected (facts/selection-facts-for-record
                     live-record "ac5bc477-carriers.edn" snap
                     {:makeup-path makeup-path :phase-log-path "/nonexistent/claude2/phases.edn.log"})]
      (is (= "selection interpretation order needs retained phase log at /nonexistent/claude2/phases.edn.log"
             (reason (get-in projected [:facts "interpretationOrder"]))))
      (is (= "/nonexistent/claude2/phases.edn.log"
             (get-in projected [:sources "interpretationOrder"])))))
  (clojure.test/testing "an explicitly nil path (a caller refusing to supply one) is the older absence"
    (let [projected (facts/selection-facts-for-record
                     live-record "ac5bc477-carriers.edn" snap
                     {:makeup-path makeup-path :phase-log-path nil})]
      (is (= "selection interpretation order needs :phase-log path"
             (reason (get-in projected [:facts "interpretationOrder"]))))
      (is (= ":phase-log path absent"
             (get-in projected [:sources "interpretationOrder"]))))))

(deftest a-record-with-no-section-has-no-selection-carriers
  (let [bare (selection-of (dissoc live-record :outer/selection))]
    (is (= "selection" (bare "kind")))
    (doseq [field ["enumeratedTasks" "comparedPolicies" "currentChoice" "selectionPath"
                   "policyPrediction" "abstentionG"]]
      (is (= "no :outer/selection section" (reason (bare field)))))))

(deftest the-default-export-is-the-work-export
  (let [text #(json/generate-string (facts/export-for-kind live-record "r" snap %))
        direct (json/generate-string (facts/facts-for-record live-record "r" snap))]
    (is (= direct (text {}) (text {:kind nil}) (text {:kind "work"})))))

(deftest task-field-reach-is-observed-and-exclusions-project
  (let [target (get-in live-record [:outer/selection :selection :selected :target])
        value (get-in live-record [:outer/selection :selection :selected :reach])
        reach {:value value :kind :prior :source :giraffe-status-readiness-prior-v1
               :inputs {:status :ready :pin "row-sha"} :kit-version 1}
        field {:enumerated [target "M-unread"]
               :admitted {target {:status :ready :pin "row-sha" :reach reach}}
               :excluded [{:target "M-unread" :reason :unread-new :pin "pass-sha"}]}
        r (assoc-in live-record [:outer/selection :field-facts] field)
        projected (facts/selection-facts-for-record r "r" snap {:makeup-path makeup-path})]
    (is (= {"observed" (action-identity/digest reach)}
           (get-in projected [:facts "selectionPath" "pickReach"])))
    (is (= (:excluded field) (get-in projected [:diagnostics :taskExclusions])))
    (is (= (set [target "M-unread"]) (set (get-in projected [:facts "enumeratedTasks"])) ))
    (is (= "absent" (get-in (selection-of (assoc-in r [:outer/selection :field-facts :admitted target :reach]
                                                  {:status :absent :reason :status-unavailable}))
                            ["selectionPath" "pickReach"])))
    (is (= "defaulted" (get-in selection-live ["selectionPath" "pickReach"])))))

(deftest removed-stub-drops-only-the-new-record-absence
  ;; The retained 11-01 fixture remains unchanged and still counts its stub.
  (let [old (selection-of live-record)
        modern (selection-of (dissoc live-record :outer-task-selection))]
    (is (= 2 (old "pathAbsenceCount")))
    (is (= 1 (modern "pathAbsenceCount")))
    (is (= (dissoc old "pathAbsenceCount") (dissoc modern "pathAbsenceCount")))))

(deftest constructed-selection-projects-pinned-library-and-scoring
  (let [construct (requiring-resolve 'futon2.aif.outer-menu/construct)
        load-makeup (requiring-resolve 'futon2.aif.outer-state/load-makeup)
        init-state (requiring-resolve 'futon2.aif.outer-state/init-state)
        select (requiring-resolve 'futon2.aif.outer-select/select)
        root "/home/joe/code/futon3/library/task-selection"
        m (assoc (load-makeup "data/wm-learning/outer-makeup.edn") :task-selection-library-root root)
        obs {:focus "M-large" :relation (fn [_ target] (if (= target "M-large") :focus :associated))
             :giraffe/pass-sha256 "a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91"
             :giraffe/input-sha256 "730407c514e2fd21905da2b533d92e046a454372c919fbd89089fc953ebf1667"
             :field [{:target "M-first" :reach 3/4} {:target "M-large" :reach 3/4}]
             :facts {"M-first" {:status :ready :cone 0 :unblocks 0 :rank 2
                                :placement {:status "open" :ways ["fixture"] :action "work"}}
                     "M-large" {:status :ready :cone 3 :unblocks 3 :rank 1
                                :placement {:status "open" :ways ["fixture"] :action "work"}}}}
        s (select m (init-state m) obs)
        r {:outer/selection s}
        export #(:facts (facts/selection-facts-for-record % "tooling" snap {}))
        f (export r) c (first (f "targetConstruction"))
        old-before (selection-of live-record)]
    (is (= (assoc (construct root m obs) :policy-count 2) (update (:construction s) :cascades #(mapv (fn [c] (dissoc c :class)) %))))
    (is (true? (c "sliceFromWholeLibrary")))
    (is (= 2 (c "policyCount") (count (f "policyPrediction"))))
    (is (str/starts-with? ((requiring-resolve 'wm-check-run-facts/evidence) f :q2) ".known"))
    (is (= ".nr \"not recomputable: previousOutcome,previousInputDigest,refusalHistory\""
           ((requiring-resolve 'wm-check-run-facts/evidence) f :q11)))
    (is (= (sort (map :id (:classes s))) (f "comparedPolicies") (f "constructedCascades")))
    (is (= (set (f "comparedPolicies")) (set (map #(get % "policy") (f "policyG")))))
    (is (= (:chosen-class s) (get-in f ["currentChoice" "cascade"])))
    (is (= (facts/exact-g (get-in s [:abstention :G])) (f "abstentionG")))
    (is (= (action-identity/digest (get-in s [:abstention :prediction :outcomes])) (f "abstentionPrediction")))
    (is (= (:giraffe/input-sha256 obs) (c "graph")))
    (is (false? (get-in (export (assoc-in r [:outer/selection :construction :cascades 0 :units 0 :sha256] "bad"))
                       ["targetConstruction" 0 "sliceFromWholeLibrary"])))
    (is (false? (get-in (export (assoc-in r [:outer/selection :construction :slice] ["invented"]))
                       ["targetConstruction" 0 "sliceFromWholeLibrary"])))
    (is (= "" (get-in (export (assoc-in r [:outer/selection :construction :graph]
                                        {:status :absent :reason :missing})) ["targetConstruction" 0 "graph"])))
    (is (false? (get-in (export (assoc-in r [:outer/selection :construction :library :manifest-sha] "bad"))
                       ["targetConstruction" 0 "sliceFromWholeLibrary"])))
    (let [collapsed (select m (init-state m) (update obs :field #(vec (reverse %))))
          f2 (export {:outer/selection collapsed})]
      (is (= 1 (count (:classes collapsed)) (count (f2 "policyG"))))
      (is (= (f2 "comparedPolicies") (f2 "constructedCascades")
             (get-in f2 ["targetConstruction" 0 "policies"])))
      ;; Three members since 90c7273eb: serves-a-way shares the single class
      ;; once the fixture facts carry a uniform placement.
      (is (= 3 (reduce + (map #(count (get % "members")) (f2 "classMembers"))))))
    (is (= old-before (selection-of live-record)) "Historical projection is unchanged")))

(deftest retained-refusal-history-replaces-the-one-step
  (let [retained [{:cascade "c1" :input-digest "d1" :run-id "r1" :target "M"}
                  {:cascade "c2" :input-digest "d2" :run-id "r2" :target "M"}]
        carried (assoc-in live-record [:decision :selection-certificate :q6 :refusal-history] retained)
        exported (:facts (facts/facts-for-record carried "r" snap))]
    (is (= [{"cascade" "c1" "input-digest" "d1"} {"cascade" "c2" "input-digest" "d2"}]
           (exported "refusalHistory")))
    (is (= "retained" (exported "refusalHistorySource"))))
  (is (= "predecessor-only" ((:facts (facts/facts-for-record live-record "r" snap))
                             "refusalHistorySource"))
      "an old record falls back to the one step, and says so"))

(deftest refusal-history-is-the-one-step-the-record-carries
  (let [exported (:facts (facts/facts-for-record record "r" snap))
        live (:facts (facts/facts-for-record live-record "r" snap))]
    (clojure.test/testing "a predecessor with absent carriers gives a typed absence, never an empty history"
      (when (map? (get exported "previousOutcome"))
        (is (contains? (get exported "refusalHistory") "not-recomputable"))))
    (clojure.test/testing "a predecessor that was not refused contributes no entry"
      (when (= "changed" (get live "previousOutcome"))
        (is (= [] (get live "refusalHistory")))))
    (clojure.test/testing "the history is exported for every record (L5-2L-2 deps)"
      (is (contains? exported "refusalHistory"))
      (is (contains? live "refusalHistory")))))

(deftest selection-q5-reads-the-canonical-phase-log-by-default
  (clojure.test/testing "the alert passes no --phase-log; the projector reads the runtime's canonical log (L5-4h review)"
    (let [exported (facts/selection-facts-for-record live-record "r" snap {:kind "selection"})
          order (get-in exported [:facts "interpretationOrder"])
          source (get-in exported [:sources "interpretationOrder"])]
      (is (not= ":phase-log path absent" source))
      (is (= "data/wm-full-loop-phases.edn.log" source))
      (is (or (string? order) (contains? order "not-recomputable"))
          "a string constructor when the log holds this run, else a typed absence naming the log"))))

(deftest selection-path-absences-are-the-chosen-decisions-not-the-fields
  (clojure.test/testing "L5-4f review: a member's absent relation receipt is population evidence; the chosen pick's is on the path"
    (let [sel (:outer/selection live-record)
          pick (get-in sel [:selection :selected :target])
          other (first (remove #{pick} (keys (get-in sel [:field-facts :admitted]))))
          plant (fn [t] (assoc-in live-record [:outer/selection :field-facts :admitted t :relation :receipt]
                                  {:status :absent :reason :claude2-review-probe}))
          count-of (fn [r] (count (facts/selection-absence-paths r)))
          base (count-of live-record)]
      (when (and pick other (get-in sel [:field-facts :admitted]))
        (is (= base (count-of (plant other))) "an absence on another member does not count")
        (is (= (inc base) (count-of (plant pick))) "an absence on the chosen pick's fact counts"))
      (is (< base 50) "the live 11-01 section's path count is small, not the whole field"))))

(deftest selection-path-narratives-count-only-the-chosen-class
  (let [rejected-placement {:status :absent :reason :no-placement}
        chosen-placement {:status :open :ways ["backlog-understood"]}
        selection {:cascade-id :serves-a-way
                   :chosen-class "chosen-class-hash"
                   :why {:header "all classes were compared"
                         :classes [{:class "rejected-class-hash"
                                    :pick "M-peradam-grounding"
                                    :placement rejected-placement}
                                   {:class "chosen-class-hash"
                                    :pick "M-typed-holes-mathlib-handoff"
                                    :placement chosen-placement}]}
                   :argument {:header "retained argument"
                              :arguments [{:id :giraffe-order
                                           :facts {:placement rejected-placement}}
                                          {:id :serves-a-way
                                           :facts {:placement chosen-placement}}
                                          {:id :stopping :facts {}}]}}
        record {:outer/selection selection}
        paths (facts/selection-absence-paths record)]
    (is (= [] paths)
        "the 25c81574 rejected class's missing placement is population evidence, not Q7's path")))

(deftest selection-path-narratives-still-count-the-chosen-class
  (let [absent {:status :absent :reason :no-placement}
        record {:outer/selection
                {:cascade-id :serves-a-way
                 :chosen-class "chosen-class-hash"
                 :why {:classes [{:class "chosen-class-hash"
                                  :steps [{:step :placed-gate}]
                                  :placement absent}]}
                 :argument {:arguments [{:id :serves-a-way
                                         :facts {:placement absent}}
                                        {:id :stopping :facts {}}]}}}
        paths (set (facts/selection-absence-paths record))]
    (is (contains? paths [:outer/selection :why :classes 0 :placement]))
    (is (contains? paths [:outer/selection :argument :arguments 0 :facts :placement]))
    (is (= 2 (count paths)) "both retained narratives still expose the chosen placement absence")))

(deftest selection-path-drops-placement-the-chosen-class-did-not-read
  (let [absent {:status :absent :reason :no-placement}
        record {:outer/selection
                {:cascade-id :giraffe-order
                 :chosen-class "chosen-class-hash"
                 :why {:classes [{:class "chosen-class-hash"
                                  :steps [{:step :ready-gate}
                                          {:step :by-cone}
                                          {:step :by-unblocks}]
                                  :placement absent}]}
                 :argument {:arguments [{:id :giraffe-order
                                         :facts {:placement absent}}
                                        {:id :stopping :facts {}}]}}}]
    (is (= [] (facts/selection-absence-paths record))
        "placement is off-path when the chosen cascade has no placed-gate")))

(deftest selection-path-real-199fe7fd-does-not-count-unread-placement
  (let [record (edn/read-string
                (slurp "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-05-199fe7fd-0a51-47aa-83a8-f95b1f92a668.edn"))]
    (is (= [] (facts/selection-absence-paths record)))))

(deftest predecessor-refusal-detail-is-not-on-the-current-selection-path
  (let [record (edn/read-string (slurp click-1-selection-record))
        export (facts/selection-facts-for-record record click-1-selection-record snap {})
        projected (:facts export)
        paths (get-in export [:diagnostics :absencePaths])]
    (is (<= (get-in export [:facts "pathAbsenceCount"]) 3))
    (is (= {"refused" true} (projected "previousChoice")))
    (is (str/starts-with? ((requiring-resolve 'wm-check-run-facts/evidence) projected :q6)
                          ".known"))
    (is (not (str/includes? ((requiring-resolve 'wm-check-run-facts/evidence) projected :q11)
                            "previousChoice")))
    (is (not-any? #(and (str/includes? % ":previous")
                        (str/includes? % ":detail"))
                  paths))
    (is (= ["[:outer/selection :outer/learning :receipt :ending :classification-absence]"
            "[:outer/selection :previous :choice]"]
           paths))))

(deftest current-selection-outcome-detail-remains-on-its-own-path
  (let [planted (assoc-in live-record [:outer/selection :selection-outcome]
                          {:status :changed
                           :detail {:probe {:status :absent :reason :current-detail-probe}}})]
    (is (some #{[:outer/selection :selection-outcome :detail :probe]}
              (facts/selection-absence-paths planted)))))

(deftest selection-previous-narrowing-does-not-change-the-work-projector
  (let [work-record (edn/read-string
                     (slurp "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-01-c5590f95-a22e-4f21-a5e2-2cf47f393803.edn"))
        before (get-in (facts/facts-for-record work-record "click-11" snap)
                       [:facts "pathAbsenceCount"])
        with-large-previous (assoc-in work-record [:outer/selection :previous
                                                   :selection-outcome :detail]
                                      {:probe {:status :absent :reason :off-work-path}})
        after (get-in (facts/facts-for-record with-large-previous "click-11" snap)
                      [:facts "pathAbsenceCount"])]
    (is (= before after))))

(deftest unsealed-count-is-typed-absent-on-records-before-the-carrier
  (clojure.test/testing "L5-2L-3: an old record has no count; it must not read as 0"
    (let [exported (:facts (facts/facts-for-record live-record "r" snap))]
      (is (contains? exported "refusalHistoryUnsealed"))
      (is (contains? (get exported "refusalHistorySealedBeforeR18") "not-recomputable"))
      (is (contains? (get exported "refusalHistoryUnsealed") "not-recomputable")))))


(deftest work-roster-follows-verified-selection-reference
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "work-roster" (make-array java.nio.file.attribute.FileAttribute 0)))
        opts {:run-id "roster-test" :startedAt "2026-11-01T00:00:00Z"}
        root (selection-record/write-decision!
              dir opts {:world-census {:open-tasks {:missions ["M-stale"]}}} {:status :chosen})
        ref (selection-record/write-decision!
             dir (assoc opts :selection-predecessor root)
             {:world-census {:open-tasks {:missions ["M-retained"] :excursions [] :tickets ["T-retained"]}}}
             {:status :chosen})
        tick {:selection-record ref
              :world-at-selection {:enumerated-tasks {:ids ["M-different"]}}}
        project #(facts/facts-for-record % "tick.edn" snap)
        actual (project tick)
        override (assoc-in tick [:world-at-selection :open-tasks]
                           {:missions {:ids []} :excursions {:ids ["E-world"]}
                            :tickets {:ids ["T-world"]}})
        legacy (project (dissoc tick :selection-record))]
    (is (= ["M-retained"] (get-in actual [:facts "openMissions"])))
    (is (= [] (get-in actual [:facts "openExcursions"])))
    (is (= ["T-retained"] (get-in actual [:facts "openTickets"])))
    (is (= ["M-different"] (get-in actual [:facts "enumeratedTasks"]))
        "Do not repair disagreement with the enumeration")
    (is (= (str (:path ref) " [:outer/selection :world-census :open-tasks :missions]")
           (get-in actual [:sources "openMissions"])))
    (is (= [] (get-in (project override) [:facts "openMissions"]))
        "An explicitly empty world roster has precedence")
    (is (= ["E-world"] (get-in (project override) [:facts "openExcursions"])))
    (is (= ["T-world"] (get-in (project override) [:facts "openTickets"])))
    (is (= "tick.edn [:world-at-selection :open-tasks :missions :ids]"
           (get-in (project override) [:sources "openMissions"])))
    (is (= ["M-x"] (get-in legacy [:facts "openMissions"])))
    (is (= (:pins snap) (get-in legacy [:sources "openMissions"])))
    (let [old-ref (selection-record/write-decision!
                   dir (assoc opts :run-id "old-roster") {} {:status :chosen})]
      (is (= legacy (project (assoc tick :selection-record old-ref)))
          "An old selection record without a roster preserves the full export"))
    (spit (:path ref) "{}")
    (is (= :selection-record/reference-invalid
           (try (project tick) nil
                (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))

(deftest independent-census-does-not-certify-the-selectors-own-roster
  (let [section {:field-facts {:open-tasks {:missions ["M-selected"] :excursions [] :tickets []}
                              :admitted {"M-selected" {}} :excluded []}
                 :world-census {:open-tasks {:missions ["M-selected" "M-gated"]
                                             :excursions [] :tickets []}}}
        project #(facts/selection-facts-for-record {:outer/selection %} "selection.edn" snap {})
        out (project section)
        old (project (dissoc section :world-census))]
    (is (= ["M-gated" "M-selected"] (get-in out [:facts "openMissions"])))
    (is (= ["M-selected"] (get-in out [:facts "enumeratedTasks"])))
    (is (not= (set (get-in out [:facts "openMissions"]))
              (set (get-in out [:facts "enumeratedTasks"]))))
    (is (= ["M-gated"] (get-in out [:diagnostics :fieldFacts :censusDifference :missions :census-only])))
    (is (= ["M-x"] (get-in old [:facts "openMissions"])))
    (is (= (:pins snap) (get-in old [:sources "openMissions"])))
    (is (= "selection.edn [:outer/selection :world-census :open-tasks :missions]"
           (get-in out [:sources "openMissions"])))))

(deftest runtime-retains-independent-census-on-decision
  (require 'futon2.aif.full-loop-runtime)
  (let [context (ns-resolve 'futon2.aif.full-loop-runtime 'selection-context)
        retain (ns-resolve 'futon2.aif.full-loop-runtime 'retain-selection!)
        dir (.toFile (java.nio.file.Files/createTempDirectory
                      "census-decision" (make-array java.nio.file.attribute.FileAttribute 0)))
        calls (atom [])
        opts (context {:run-id "census-test" :startedAt "2026-11-01T00:00:00Z"
                       :run-record-dir (str dir) :code-root "/fixture"
                       :world-task-sets-fn (fn [root] (swap! calls conj root)
                                            {:missions ["M-gated" "M-selected"]
                                             :excursions [] :tickets []})})
        section {:field-facts {:open-tasks {:missions ["M-selected"]}}}
        ref (retain opts section {:status :refused :kind :test})
        retained (selection-record/read-reference ref)
        census (get-in retained [:outer/selection :world-census])
        out (facts/selection-facts-for-record
             {:selection-record ref
              :world-at-selection {:open-tasks {:missions {:ids ["M-world"]}}}}
             "tick.edn" snap {})]
    (is (= ["/fixture"] @calls))
    (is (= :files (:source census)))
    (is (= "/fixture" (:code-root census)))
    (is (string? (:captured-at census)))
    (is (= ["M-gated" "M-selected"] (get-in census [:open-tasks :missions])))
    (is (= ["M-world"] (get-in out [:facts "openMissions"])))
    (is (= "tick.edn [:world-at-selection :open-tasks :missions :ids]"
           (get-in out [:sources "openMissions"])))
    (is (= ref (retain opts section {:status :refused :kind :test})))
    (is (= ["/fixture"] @calls))))

(deftest retained-post-score-refusals-have-the-same-decision-signature
  (let [pins [["878186ce-4e68-411b-8d2e-581c559b72cb" "c92ed1991a46c00af10d4279135888af0703fa542c6570a3614b3db9799535c4"]
              ["28d27460-bc9d-44c1-9555-74b8b6931300" "0cca2d2a5a1d32fdd3e6db829586691622c98833721bfb9861cb7754e6cd9d02"]]
        records (mapv (fn [[id sha]]
                        (let [text (slurp (str "/home/joe/code/futon2/data/wm-runs/selection-record-2026-11-01-wm-click-" id ".edn"))]
                          (is (= sha (action-identity/sha256 text)))
                          (edn/read-string text))) pins)
        project #(facts/selection-facts-for-record % "read-only-fixture" snap {})
        exports (mapv project records)
        keys ["currentChoice" "pathAbsenceCount" "interpretationOrder"]
        signature #(select-keys (:facts %) keys)]
    (is (= (signature (first exports)) (signature (second exports))))
    (doseq [e exports]
      (is (= ".nr \"selection refused: :pick-not-below-abstention\""
             ((requiring-resolve 'wm-check-run-facts/evidence) (:facts e) :q11)))
      (is (= ".known { Requirements.click20 with interpretationOrder := .noInterpretation }"
             ((requiring-resolve 'wm-check-run-facts/evidence) (:facts e) :q5)))
      (is (= {"not-recomputable" "selection refused: :pick-not-below-abstention"}
             (get-in e [:facts "currentChoice"])))
      (is (= 0 (get-in e [:facts "pathAbsenceCount"])))
      (is (= "noInterpretation" (get-in e [:facts "interpretationOrder"]))))
    (doseq [r records]
      (let [e (project (assoc-in r [:outer/selection :outer/learning] {:status :none}))]
        (is (= false (get-in e [:facts "selectionPath" "learningApplied"])))
        (is (= [] (get-in e [:facts "selectionPath" "consumedRuns"])))))))

(deftest click-12-reselects-below-stopping-at-ruled-altitude
  (let [record (edn/read-string (slurp click-12-selection-record))
        retained (:outer/selection record)
        params (get-in retained [:construction :kernel-input :makeup-parameters])
        admitted (get-in retained [:field-facts :admitted])
        targets (:outer-score/admitted retained)
        relation-by-target (into {} (map (juxt :target :relation)
                                         (:cascade-pick-relations retained)))
        state {:c (update-vals (:c-kit params) #(* 100 %))
               :state-counts (:state-kit params) :gain-counts (:gain-kit params)}
        observation {:focus (:focus retained)
                     :relation (fn [_ target] (get relation-by-target target :unknown))
                     :field (mapv (fn [target]
                                    {:target target
                                     :reach (get-in admitted [target :reach :value])
                                     :reach-source (get-in admitted [target :reach :source])}) targets)
                     :facts (update-vals admitted
                                         #(assoc % :placement
                                                 {:status "open" :ways ["fixture"] :action "work"}))
                     :field-facts (:field-facts retained)
                     :previous-selection (:previous retained)
                     :giraffe/pass-sha256 (:giraffe/pass-sha256 retained)
                     :giraffe/input-sha256 (get-in retained [:construction :graph])}
        altitude-source (outer-score/abstention-altitude)
        stopping-outcomes (get-in retained [:abstention :prediction :outcomes])
        with-altitude (fn [score]
                        (if (= stopping-outcomes (get-in score [:prediction :outcomes]))
                          (-> score
                              (update :G + (:nats altitude-source))
                              (assoc-in [:terms :altitude] (:nats altitude-source))
                              (assoc :altitude-source altitude-source))
                          score))
        retained-scores (atom (:cascades retained))
        reselection (with-redefs [outer-menu/construct
                                  (fn [_ _ _] (dissoc (:construction retained) :policy-count))
                                  outer-score/score-model
                                  (fn [_ _]
                                    (let [score (first @retained-scores)]
                                      (swap! retained-scores subvec 1)
                                      (with-altitude
                                        (select-keys score [:G :terms :prediction :gain-source]))))
                                  outer-score/abstention-score
                                  (fn [_] (with-altitude (:abstention retained)))]
                      (outer-select/select params state observation))
        reselection (assoc reselection :q6 (:q6 retained))
        export (facts/selection-facts-for-record
                (assoc record :selection-outcome {:status :chosen}
                       :outer/selection reselection)
                "click-12-read-only-reselection" snap {})
        projected (:facts export)]
    (reset! click-12-reselection-export export)
    (is (= "M-interim-director-proxy-metric-inventory"
           (get-in reselection [:selection :selected :target])))
    (is (= 3.3896147707408804 (get-in reselection [:classes 0 :G])))
    (is (= 13.278213771580794 (get-in reselection [:abstention :G])))
    (is (= 10 (get-in export [:diagnostics :abstentionTerms :terms :altitude])))
    (is (.startsWith ((requiring-resolve 'wm-check-run-facts/evidence) projected :q11)
                     ".known"))))
