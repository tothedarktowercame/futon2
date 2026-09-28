(ns futon2.test-support.runner-fixture
  "Report-free fixtures shared by full-loop runner tests."
  (:require [clojure.java.io :as io]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as learning-ledger]
            [futon2.aif.policy :as policy]
            [futon2.aif.trace :as trace])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn declared-effective-configuration [opts]
  {:schema :wm/effective-run-configuration-v1
   :run/id (:run-id opts)
   :loaded-code-identity (or (:loaded-code-identity opts) {:status :unavailable})
   :evaluation :not-reached
   :policy-details? false
   :fpi-dark? false
   :fpi-posterior? false})

(defn with-hermetic-traces [f]
  (let [root (.toFile (Files/createTempDirectory "wm-runner-trace-suite-"
                                                (make-array FileAttribute 0)))
        run-record-root (.toFile (Files/createTempDirectory
                                  "wm-runner-record-suite-"
                                  (make-array FileAttribute 0)))]
    (try
      (with-redefs-fn {#'trace/default-trace-dir (.getPath root)
                       #'runner/default-run-record-dir (.getPath run-record-root)
                       #'learning-ledger/default-root
                       (str (io/file root "learning-ledger"))}
        f)
      (finally
        (doseq [file (reverse (file-seq root))] (io/delete-file file true))
        (doseq [file (reverse (file-seq run-record-root))]
          (io/delete-file file true))))))

(def ^:private selected-action
  {:kind :cascade-candidate :cascade-id :test/selected :id :test/selected
   :target "M-selected" :precedence [:test/selected-pattern]
   :construction-receipt {:cascade/id :test/selected :moves 1
                          :family-searched :unit :coverage 1}
   :interpretation-receipts [{:pattern :test/selected-pattern
                              :admitted-by :test-suite}]})

(def ^:private judgement
  {:decision (policy/select-action-cascades
              [{:action selected-action :controller-score -2.0 :rank 1}
               {:action (assoc selected-action
                               :cascade-id "M-rank-head" :id "M-rank-head"
                               :precedence [:test/other-pattern])
                :controller-score -1.0 :rank 2}]
              {:beta 2.0})
   :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace []
   :mode :maintain})

(defn- synthetic-artifact-binding [_repo before author-job]
  {:fresh-author? true
   :repo "/repo"
   :pre-dispatch-head (:head before)
   :observed-head (:artifact-ref author-job)
   :author-window-start-ms 1000
   :author-window-end-ms 2000
   :corroborates? true
   :disagreement? false
   :commit (:artifact-ref author-job)})

(defn- enriched-test-fold [{:keys [shown]}]
  {:wiring {:boxes [] :wires [] :terminals []}
   :coverage-score-delta nil
   :policy-holes
   (mapv (fn [pattern-id]
           {:free (str "test construction for " pattern-id)
            :why "Production fold is isolated by this runner test"
            :unfolded-pattern pattern-id
            :obligation/id (str "test/" pattern-id)})
         shown)})

(defn isolated-runner-opts []
  (merge
   (hermetic/runner-repair-options)
   {:cohort? false
    :author "zai-5" :reviewer "codex-7" :repair-reviewer "codex-1"
    :phase-log-fn (fn [_])
    :roster-fn (fn [_] {:zai-5 {:status "idle" :invoke-ready? true}
                        :codex-7 {:status "idle" :invoke-ready? true}
                        :codex-1 {:status "idle" :invoke-ready? true}})
    :judge-fn (fn [_] {:judgement judgement})
    :refresh-fn (fn [])
    :substrate-preflight-fn (fn [_] {:route :test})
    :code-state-fn (fn [] {:repo "/futon2" :git-sha "head"
                           :git-dirty? false :repo-heads {}})
    :mode-flags-fn (fn [] {})
    :scan-render-fn (fn [& _] nil)
    :effective-run-configuration-fn declared-effective-configuration
    :version-stamp-fn identity
    :mission-fn (fn [target] {:id target})
    :construct-fn runner/construct-for-decision
    :construction-wiring-fn enriched-test-fold
    :author-artifact-observer-fn synthetic-artifact-binding
    :r16-park-fn (fn [_ finding]
                   {:ok true :id (str "test-park/" (:repair/id finding))
                    :status :parked})
    :delivery-qa-fn
    (fn [_ item]
      {:morning-brief/addendum-id (str "qa-" (:attempt-id item))})
    :queue-fn identity}))
