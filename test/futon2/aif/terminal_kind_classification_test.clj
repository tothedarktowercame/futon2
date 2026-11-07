(ns futon2.aif.terminal-kind-classification-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.debugger :as debugger]))

(use-fixtures
  :each
  (fn [test-fn]
    (debugger/detach!)
    (try (test-fn)
         (finally (debugger/detach!)))))

(defn- classification-table []
  (some-> "wm/terminal-kind-classification.edn" io/resource slurp edn/read-string))

(def valid-classes
  #{:aif-valid-stop :recorded-defect :repairable-error :operator-termination})

;; These enter the runner through typed seam values rather than source literals
;; at their terminal assembly sites.  Keeping this list independent of the EDN
;; table makes removal from both the producer and its carrier visible as stale.
(def documented-dynamic-kinds
  #{:abstained :all-refused-targets-unknown-class :answer-reviewer-mismatch
    :artifact-ref-unresolved :author-delivery-wrong-repository
    :carry-identity-unavailable :cascade-enactment-open :close-exception
    :co-apply-pattern-malformed :co-apply-units-missing-or-empty
    :criterion-rendering-failed :designated-author-unavailable :dispatch-failed
    :failed-before-selection :fold-correspondence-invalid :fold-output-invalid
    :full-loop-close-failure :giraffe-run-unavailable
    :in-force-belief-row-unavailable :independent-observation-unavailable
    :job-read-transport-failure :no-addressable-action :no-selection
    :no-selection-made :occurrence-square-answer-invalid
    :open-cascade-author-answer-invalid
    :open-cascade-author-contact-failed
    :open-cascade-review-contact-failed
    :ordinary-click-budget-observation-malformed
    :ordinary-click-budget-observation-time-malformed
    :ordinary-click-budget-reader-failed :ordinary-click-budget-reader-unavailable
    :patch-source-digest-mismatch :patch-var-not-a-function
    :patch-var-unavailable :placement-source-unavailable
    :priority-projection-sha-mismatch :provisional-choice-without-interpretation
    :repair-lifecycle-census-unavailable :repair-machine-failure
    :repeat-choice-after-refusal :resume-error-unknown-class
    :reviewed-claim-refused :revision-unchanged :source-unavailable
    :unparseable-second-review :wm/open-cascade-execution-error})

(defn- source-files []
  (map io/file
       ["src/futon2/aif/full_loop_runner.clj"
        "src/futon2/aif/full_loop_runtime.clj"
        "src/futon2/aif/full_loop_cohort.clj"
        "src/futon2/aif/wm/open_cascade_resume.clj"
        "src/futon2/aif/wm/cascade_decision.clj"
        "src/futon2/aif/task_execution_evidence.clj"
        "src/futon2/aif/interpretation_evidence.clj"
        "src/futon2/aif/tripwire.clj"
        "src/futon2/aif/effective_run_history.clj"
        "src/futon2/aif/meta_self_heal_harness.clj"]))

(defn- literal-terminal-kinds-in-text [text]
  (let [failure-kinds (map second (re-seq #":failure-kind\s+(:[A-Za-z0-9_./?!+*<>-]+)" text))
        terminal-maps (re-seq #"(?s):status\s+:(?:error|refused|abstained)(.{0,500})" text)
        seam-kinds (mapcat (fn [[_ body]]
                             (map second
                                  (re-seq #":(?:kind|reason)\s+(:[A-Za-z0-9_./?!+*<>-]+)"
                                          body)))
                           terminal-maps)
        ;; R20 empty-field abstention: {:status :abstained :case k}
        abstention-cases (map second
                              (re-seq #":status\s+:abstained\s+:case\s+(:[A-Za-z0-9_./?!+*<>-]+)"
                                      text))]
    (set (map #(keyword (subs % 1))
              (concat failure-kinds seam-kinds abstention-cases)))))

(defn- literal-terminal-kinds [files]
  (reduce set/union #{} (map #(literal-terminal-kinds-in-text (slurp %)) files)))

(defn- wait-for-stop [run-id running]
  (loop [remaining 1000]
    (if-let [stop (first (filter #(= run-id (:run-id %)) (debugger/stopped)))]
      stop
      (if (pos? remaining)
        (do (Thread/sleep 5) (recur (dec remaining)))
        (throw (ex-info "Debugger stop did not appear"
                        {:run-id run-id :realized? (realized? running)}))))))

(deftest terminal-kind-table-is-present
  (let [table (classification-table)]
    (is (map? table))
    (is (every? valid-classes (map :class (vals table))))
    (is (every? #(and (contains? % :lean-basis)
                      (vector? (:raise-sites %))
                      (seq (:raise-sites %)))
                (vals table)))
    (let [produced (set/union (literal-terminal-kinds (source-files))
                              documented-dynamic-kinds)
          classified (set (keys table))]
      (is (empty? (set/difference produced classified)) "unclassified production kind")
      (is (empty? (set/difference classified produced)) "stale classification row"))))

(deftest scanner-catches-a-new-literal-terminal-kind
  (let [fixture (slurp "test/fixtures/terminal_kind_unclassified.clj")
        produced (literal-terminal-kinds-in-text fixture)]
    (is (= #{:test/new-stop} produced))
    (is (= #{:test/new-stop}
           (set/difference produced (set (keys (classification-table))))))))

(deftest unclassified-agency-kind-stops-as-unclassified-when-attached
  (debugger/attach!)
  (let [run-id "terminal-kind-unclassified"
        running (future
                  (runner/run-phase!
                   {:run-id run-id :phase-log-fn (fn [_])}
                   {:opportunity-id "stop-table-1"}
                   :reviewer-wait
                   (constantly {:job-id "job-1" :state "done"
                                :failure-kind :test/new-stop})
                   nil
                   runner/reviewer-wait-terminal-condition))
        stop (wait-for-stop run-id running)]
    (is (= :unclassified-terminal-kind (get-in stop [:condition :kind])))
    (is (= :test/new-stop (get-in stop [:ex-data :original-kind])))
    (debugger/continue! run-id :abort)
    (try (deref running 5000 ::timeout)
         (catch java.util.concurrent.ExecutionException _))))

(deftest unclassified-agency-kind-is-unchanged-when-detached
  (let [value {:job-id "job-1" :state "done" :failure-kind :test/new-stop}]
    (is (= value
           (runner/run-phase!
            {:run-id "terminal-kind-detached" :phase-log-fn (fn [_])}
            {:opportunity-id "stop-table-1"}
            :reviewer-wait (constantly value) nil
            runner/reviewer-wait-terminal-condition)))))

(deftest empty-field-abstention-is-the-aif-valid-stop
  (is (= #{:empty-field}
         (set (keep (fn [[k v]] (when (= :aif-valid-stop (:class v)) k))
                    (classification-table)))))
  (debugger/attach!)
  (let [value {:status :abstained :case :empty-field}]
    (is (= value
           (runner/run-phase!
            {:run-id "terminal-kind-empty-field" :phase-log-fn (fn [_])}
            {:opportunity-id "stop-table-1"}
            :selection (constantly value) nil nil)))))
