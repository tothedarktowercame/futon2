(ns futon2.aif.l5-9-selection-carriers-test
  "L5-9: the immutable outer decision carries the execution identity and the
  refusal history visible when the click began."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.outer-menu-fixture :as menu-fixture]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-select-test :as fixtures]
            [futon2.aif.outer-selection-wiring-test :as wiring]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.selection-record :as selection-record]
            [wm-run-facts :as facts]))

(use-fixtures :each menu-fixture/with-fixture-constructor)

(defn- phase-log [identity]
  (let [file (java.io.File/createTempFile "l5-9-phases" ".edn.log")]
    (spit file
          (str (pr-str {:phase :selection :transition :end
                        :execution-identity identity}) "\n"
               (pr-str {:phase :occurrence-square-authoring :transition :start
                        :execution-identity identity}) "\n"))
    (.getCanonicalPath file)))

(deftest run-opportunity-retains-execution-and-refusal-history
  (let [dir (wiring/temp-dir)
        identity {:kind :runner-execution :id "ea1-fixture--attempt-001"}
        cascade "refused-cascade"
        digest "same-input-digest"
        history [{:cascade cascade :input-digest digest
                  :run-id "refused-before" :target "M-ready"}]
        predecessor {:refusal-history {:entries history :unsealed 0
                                       :sealed-before-r18 0}}
        result
        (with-redefs [previous-run/predecessor (fn [& _] predecessor)]
          (wiring/call-ordinary
           {:run-id "l5-9" :startedAt "2026-11-01T12:00:00Z"
            :run-record-dir (.getPath dir)}
           {#'outer-state/load-makeup (constantly fixtures/base-makeup)
            #'outer-state/read-state (fn [_ _] fixtures/base-state)
            #'outer-observation/outer-observation (constantly fixtures/base-observation)
            #'runtime/run-ordinary-opportunity-with-task!
            (fn [opts]
              ((:selection-record/retain-fn opts) identity)
              {:status :fixture-complete})}))
        ref (:selection-record result)
        record (selection-record/read-reference ref)
        export (facts/selection-facts-for-record
                record (:path ref) {} {:phase-log-path (phase-log identity)})
        projected (:facts export)]
    (is (= :fixture-complete (:status result)) (pr-str result))
    (is (= identity (:runner-execution/identity record)))
    (is (= {:refusal-history history :refusal-history-unsealed 0
            :refusal-history-sealed-before-r18 0}
           (get-in record [:outer/selection :q6])))
    (is (= "selectionBeforeInterpretation" (projected "interpretationOrder")))
    (is (= [{"cascade" cascade "input-digest" digest}]
           (projected "refusalHistory")))
    (is (= "retained" (projected "refusalHistorySource")))
    (is (= 0 (projected "refusalHistoryUnsealed")
           (projected "refusalHistorySealedBeforeR18")))
    (is (= cascade (get-in (first (projected "refusalHistory")) ["cascade"])))
    (is (= digest (get-in (first (projected "refusalHistory")) ["input-digest"])))))

(deftest pre-l5-9-selection-keeps-its-old-absences
  (let [record {:schema selection-record/schema
                :run/id "old" :click/id "old"
                :startedAt "2026-11-01T11:00:00Z"
                :chain-index 1
                :outer/selection {:construction {:policy-count 0}
                                  :selection nil}
                :selection-outcome {:status :abstained :case :empty-field}}
        before (:facts (facts/selection-facts-for-record
                        record "pre-l5-9.edn" {} {:phase-log-path nil}))]
    (is (= {"not-recomputable" "no carrier on the selection path"}
           (before "refusalHistory")))
    (is (= {"not-recomputable" "no carrier on the selection path"}
           (before "refusalHistorySource")))))

(deftest refusal-before-scoring-still-carries-available-context
  (let [dir (wiring/temp-dir)
        result (with-redefs [runtime/find-giraffe-run (constantly nil)]
                 (#'runtime/run-ordinary-opportunity!
                  {:run-id "early-refusal" :click-id "click-early"
                   :startedAt "2026-11-01T13:00:00Z"
                   :run-record-dir (.getPath dir)}))
        record (selection-record/read-reference (:selection-record result))]
    (is (= {:kind :selection-execution :id "selection--click-early"}
           (:runner-execution/identity record)))
    (is (= {:refusal-history [] :refusal-history-unsealed 0
            :refusal-history-sealed-before-r18 0}
           (get-in record [:outer/selection :q6])))))

(deftest a-chosen-selection-whose-runner-fails-before-retaining-is-still-recorded-as-chosen
  ;; the bad case the retiming creates: the runner throws in agent-readiness or
  ;; code-state, before the cohort-allocation retain point
  (let [dir (wiring/temp-dir)
        predecessor {:refusal-history {:entries [] :unsealed 0 :sealed-before-r18 0}}
        result
        (with-redefs [previous-run/predecessor (fn [& _] predecessor)]
          (wiring/call-ordinary
           {:run-id "l5-9-early-throw" :click-id "click-early-throw"
            :startedAt "2026-11-01T12:30:00Z" :run-record-dir (.getPath dir)}
           {#'outer-state/load-makeup (constantly fixtures/base-makeup)
            #'outer-state/read-state (fn [_ _] fixtures/base-state)
            #'outer-observation/outer-observation (constantly fixtures/base-observation)
            #'runtime/run-ordinary-opportunity-with-task!
            (fn [_] (throw (ex-info "code-state failed" {:kind :code-state-unavailable})))}))
        record (selection-record/read-reference (:selection-record result))]
    (is (= :chosen (get-in record [:selection-outcome :status])) (pr-str (:selection-outcome record)))
    (is (string? (get-in record [:selection-outcome :pick])))
    (is (= {:kind :selection-execution :id "selection--click-early-throw"}
           (:runner-execution/identity record)))
    (is (= [] (get-in record [:outer/selection :q6 :refusal-history])))))
