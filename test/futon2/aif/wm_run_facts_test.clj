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
               :class-preference {1 {:ending/not-yet-evaluated 1}
                                  2 {:progress/zero 0.7 :progress/one 0.3}
                                  3 {:progress/zero 0.2 :progress/one 0.8}
                                  4 {:focused 0.55 :related 0.35}}}
        scored {:g 1.5
                :g-terms {:risk 1.0 :ambiguity 0.75
                          :expected-information-gain 0.25}
                :observation-model model}
        r (assoc-in record [:decision :selection-certificate]
                    {:candidates [{:id :c1 :target "M-x" :g 1.5}]
                     :policies [{:id :pi1}]
                     :g-term-decomposition {:policies
                                            [{:terms {:A {:value model}}}]}
                     :scoring {0 scored}})
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (is (= 4 (f "horizonLength")))
    (is (= [1 2 3] (f "preferenceSteps")))
    (is (= [1 2] (f "gradedPreferenceSteps")))
    (is (= {"risk" true "ambiguity" true "informationGain" true}
           (f "gTerms")))
    (is (= [1 1 1]
           (mapv f ["policiesWithRiskTerm" "policiesWithAmbiguityTerm"
                    "policiesWithInformationTerm"])))))

(deftest q4-refuses-to-mix-scoring-occurrences-with-reused-policy-labels
  (let [scored {:g 1.0
                :g-terms {:risk 1.0 :ambiguity 1.0
                          :expected-information-gain 1.0}
                :observation-model {:horizon 1
                                    :class-preference {1 {:focused 1.0}}}}
        r (assoc-in record [:decision :selection-certificate]
                    {:candidates [{:id :c1 :target "M-a" :g 1.0}
                                  {:id :c1 :target "M-b" :g 1.0}]
                     :policies [{:id :pi1}]
                     :scoring {0 scored 1 scored}})
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
