(ns futon2.aif.wm-run-facts-test
  (:require [clojure.test :refer [deftest is]]
            [wm-run-facts :as facts]))

(deftest run-record-reader-consumes-a-stream-without-slurp
  (let [file (java.io.File/createTempFile "wm-run-facts-stream-" ".edn")
        value {:run/id "streamed" :decision {:candidates (vec (range 1000))}}]
    (spit file (str (pr-str value) "\n"))
    (with-redefs [clojure.core/slurp
                  (fn [& _] (throw (AssertionError. "whole-file slurp used")))]
      (is (= value (facts/read-edn file))))))

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
        planted (assoc-in record [:decision :selection-certificate :f]
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

(deftest recorded-library-pin-is-the-construction-denominator
  (let [carried (assoc-in record [:decision :selection-certificate :library-pin]
                          {:schema :wm/pinned-pattern-library-v1
                           :digest "pin" :size 1436})
        exported (:facts (facts/facts-for-record carried "r" snap nil nil))]
    (is (= 1436 (exported "libraryPatternCount")))))

(deftest typed-absent-construction-carrier-is-never-read-as-an-empty-pool
  (let [carried (assoc-in record [:decision :selection-certificate :target-construction]
                          {:status :absent :reason :query-time-construction-slice-not-recorded})
        exported (:facts (facts/facts-for-record carried "r" snap nil nil))]
    (is (contains? (exported "targetConstruction") "not-recomputable"))
    (is (contains? (exported "constructorPatternCount") "not-recomputable"))))

(deftest runfacts-field-set-is-exact
  (let [exported (:facts (facts/facts-for-record record "r" snap nil nil))]
    (is (= (set facts/run-fact-fields) (set (keys exported))))))

(deftest computed-falsifier-receipt-is-present-not-an-absence
  (let [receipt {:schema :wm/reviewer-falsifier-v1 :target "M-x"
                 :commit "c0" :status :refused
                 :receipt/id "abc" :failed-checks [:mission-standing]
                 :checks {:mission-standing {:status :fail}}}
        planted (assoc-in record [:failure :detail]
                          {:reviewer-falsifier receipt})
        export (facts/facts-for-record planted "r" snap nil nil)]
    (is (= 0 (get-in export [:facts "pathAbsenceCount"])))
    (is (= [] (get-in export [:diagnostics :absencePaths])))))

(deftest schema-tagged-absence-record-is-still-counted
  ;; Adversarial: a :schema tag names the field type, not a computed record.
  ;; Genuine typed absences wearing schemas (g-term-decomposition :missing,
  ;; accumulation-bmr :absent) must remain counted.
  (let [planted (assoc-in record [:decision :g-term-decomposition]
                          {:schema :wm/g-term-decomposition-v1
                           :status :missing :policies []})
        export (facts/facts-for-record planted "r" snap nil nil)]
    (is (= 1 (get-in export [:facts "pathAbsenceCount"])))
    (is (= ["[:decision :g-term-decomposition]"]
           (get-in export [:diagnostics :absencePaths])))))

(deftest bare-typed-refusal-in-failure-detail-is-still-counted
  ;; Adversarial: the exclusion is for computed machine records, not for
  ;; every :refused map. A bare refusal with no record identity remains a
  ;; counted typed absence.
  (let [planted (assoc-in record [:failure :detail]
                          {:reviewer-falsifier-verification
                           {:status :refused
                            :reason :reviewer-falsifier-applicable-check-failed}})
        export (facts/facts-for-record planted "r" snap nil nil)]
    (is (= 1 (get-in export [:facts "pathAbsenceCount"])))
    (is (= ["[:failure :detail :reviewer-falsifier-verification]"]
           (get-in export [:diagnostics :absencePaths])))))

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

(deftest q4-reads-certificate-scoring-and-real-preference-steps
  (let [model {:horizon 4
               :class-preference {1 {:ending/not-yet-evaluated 1}
                                   2 {:ending/not-yet-evaluated 1}
                                   3 {:ending/not-yet-evaluated 1}
                                   4 {:focused 0.55 :related 0.35
                                      :unrelated 0.05 :stop-the-line 0.05}}}
        scored {:g 1.5 :g-terms {:risk 1.0 :ambiguity 0.75
                                  :expected-information-gain 0.25}
                :observation-model model}
        r (assoc-in record [:decision :selection-certificate]
                    {:candidates [{:id :c1 :target "M-x" :g 1.5}]
                     :g-term-decomposition {:policies
                                            [{:terms {:A {:value model}}}]}
                     :scoring {0 scored}})
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (is (= 4 (f "horizonLength")))
    (is (= [3] (f "preferenceSteps")))
    (is (= {"risk" true "ambiguity" true "informationGain" true}
           (f "gTerms")))
    (let [adversarial (assoc-in r [:decision :selection-certificate :scoring 0 :g-terms]
                                {:risk 1.0 :ambiguity 0.0})
          af (:facts (facts/facts-for-record adversarial "r" snap nil nil))]
      (is (= {"risk" true "ambiguity" false "informationGain" false}
          (af "gTerms"))
          "a numeric zero is still a recorded term, but absent information gain is not"))))

(deftest current-q4-carriers-export-graded-steps-and-per-policy-term-counts
  (let [model {:horizon 2
               :class-preference {1 {:progress-0 1/3 :progress-1 2/3}
                                  2 {:focused 1}}}
        r (assoc-in record [:decision :selection-certificate]
                    {:candidates [{:id :c1 :target "M-x" :g 1.5}]
                     :policies [{:id :c1}]
                     :g-term-decomposition {:policies [{:terms {:A {:value model}}}]}
                     :scoring {0 {:g-terms {:risk 1.0 :ambiguity 0.0
                                            :expected-information-gain 0.0}}}})
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (is (= [0] (f "gradedPreferenceSteps")))
    (is (= 1 (f "policiesWithRiskTerm")))
    (is (= 1 (f "policiesWithAmbiguityTerm")))
    (is (= 1 (f "policiesWithInformationTerm")))))

(deftest compared-policy-id-does-not-expand-the-whole-candidate-map
  (let [policy {:id {:kind :cascade-candidate :id :C1 :target "M-x"
                     :precedence [{:id :p1 :reading (apply str (repeat 500 "x"))}]}}
        r (assoc-in record [:decision :selection-certificate :policies] [policy])
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (is (= [":C1"] (f "comparedPolicies")))))

(deftest current-q9-q10-census-is-exported-verbatim
  (let [census {:completion-preference-pairs 3
                :completion-pairs-strictly-preferred 2
                :earlier-progress-pairs 4
                :earlier-progress-no-greater-risk 4
                :different-arrangement-pairs 5
                :arrangement-pairs-distinguished-by-g 5}
        r (assoc-in record [:decision :selection-certificate :q9-q10-census] census)
        f (:facts (facts/facts-for-record r "r" snap nil nil))]
    (doseq [[k v] {"completionPreferencePairs" 3
                   "completionPairsStrictlyPreferred" 2
                   "earlierProgressPairs" 4
                   "earlierProgressNoGreaterRisk" 4
                   "differentArrangementPairs" 5
                   "arrangementPairsDistinguishedByG" 5}]
      (is (= v (f k))))))

(deftest seats-used-counts-only-dispatched-seats
  ;; Lean: "Stable ids of seats included in dispatch or behavior counts for
  ;; the run." A role's identity counts only when the record shows a
  ;; dispatched job for it; configured-but-idle roles (click 48's
  ;; repair-reviewer zai-3) are not used seats.
  (let [participant-record
        (assoc record
               :participants
               {:schema :wm/run-participants-v1
                :roles {:author {:status :present :identity "zai-1"}
                        :reviewer-of-record {:status :present :identity "zai-2"}
                        :repair-reviewer {:status :present :identity "zai-3"}
                        :issuing-caller {:status :present :identity "claude-12"
                                         :source :wm-click-http-boundary}}}
               :registered-run/model-usage
               {:unit :tokens
                :jobs [{:role {:agent "zai-1" :caller "wm-full-loop"}
                        :job-id "invoke-1" :phase :author-dispatch
                        :status :complete}
                       {:role {:agent "zai-2" :caller "wm-full-loop"}
                        :job-id "invoke-2" :phase :review
                        :status :complete}
                       ;; a ledger row with no job id is not a dispatch
                       {:role {:agent "zai-4"} :status :planned}]})
        exported (:facts (facts/facts-for-record participant-record "r" snap nil nil))]
    (is (= ["zai-1" "zai-2"] (exported "seatsUsed")))
    (is (not (some #{"zai-3" "claude-12" "zai-4"} (exported "seatsUsed"))))))

(deftest seats-used-falls-back-to-present-roles-without-a-job-ledger
  ;; legacy records with no model-usage ledger keep the typed-participants
  ;; reading; the HTTP issuing caller is never a used seat.
  (let [participant-record
        (assoc record :participants
               {:schema :wm/run-participants-v1
                :roles {:author {:status :present :identity "zai-1"}
                        :reviewer-of-record {:status :present :identity "zai-2"}}})
        exported (:facts (facts/facts-for-record participant-record "r" snap nil nil))]
    (is (= ["zai-1" "zai-2"] (exported "seatsUsed")))))

(deftest previous-run-carrier-supplies-q6-facts
  (let [carrier {:schema :wm/previous-run-v1 :status :present
                 :run/id "prev-run"
                 :choice {:status :present :target "M-prev"
                          :precedence [:p1 :p2]}
                 :outcome {:status :present :outcome :refused}
                 :input-digest {:status :present :digest "abc"}}
        with-carrier (assoc record :previous-run carrier)
        export-with (:facts (facts/facts-for-record with-carrier "r" snap nil nil))
        partial-carrier (assoc record :previous-run
                               {:schema :wm/previous-run-v1 :status :present
                                :run/id "prev-run"
                                :choice {:status :absent
                                         :reason :previous-run-chose-nothing}
                                :outcome {:status :absent :reason :x}
                                :input-digest {:status :absent :reason :x}})
        export-partial (:facts (facts/facts-for-record partial-carrier "r" snap nil nil))]
    (is (= {"target" "M-prev" "cascade" "[:p1 :p2]"}
           (export-with "previousChoice")))
    (is (= "refused" (export-with "previousOutcome")))
    (is (= "abc" (export-with "previousInputDigest")))
    ;; the carrier is the source, not the threaded previous record
    (is (= "r" (get-in (facts/facts-for-record with-carrier "r" snap
                                               {:decision {:chosen {:target "M-other"}}}
                                               "prev.edn")
                       [:sources "previousChoice"])))
    ;; a previous run that genuinely chose nothing stays typed-absent
    (is (contains? (export-partial "previousChoice") "not-recomputable"))
    (is (contains? (export-partial "previousOutcome") "not-recomputable"))
    (is (contains? (export-partial "previousInputDigest") "not-recomputable"))))

(deftest cached-g-counts-only-with-a-valid-cache-digest
  (let [cached (assoc-in record [:decision :selection-certificate :candidates 0]
                         {:id :c1 :target "M-x" :controller-score 1.25
                          :cache {:status :cached :digest "input-digest"}})
        fresh (:facts (facts/facts-for-record cached "r" snap nil nil))
        stale (assoc-in cached [:decision :selection-certificate :candidates 0
                                :cache :status] :corrupt)
        stale-facts (:facts (facts/facts-for-record stale "r" snap nil nil))
        mismatched (assoc-in cached [:decision :selection-certificate :candidates 0
                                     :cache :inputs-digest] "other-input")
        mismatched-facts (:facts (facts/facts-for-record mismatched "r" snap nil nil))]
    (is (= ["M-x"] (fresh "targetsWithG")))
    (is (= [] (stale-facts "targetsWithG")))
     (is (= [] (mismatched-facts "targetsWithG")))))

(deftest cold-scored-g-counts-as-valid-cache-output
  (let [cold (assoc-in record [:decision :selection-certificate :candidates 0]
                       {:id :c1 :target "M-cold" :controller-score 1.25
                        :cache {:status :cold-scored :digest "input-digest"}})
        exported (:facts (facts/facts-for-record cold "r" snap nil nil))]
    (is (= ["M-cold"] (exported "targetsWithG")))))
