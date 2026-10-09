(ns futon2.test-support.runner-fixture
  "Report-free fixtures shared by full-loop runner tests."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.policy :as policy]
            [futon2.data-paths :as data-paths])
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

(defn- production-file-set [dir]
  (let [d (io/file dir)]
    (if (.isDirectory d)
      ;; Relative path -> [length mtime]. This detects creation, deletion,
      ;; append, replacement, and same-size modification anywhere in data/.
      (into (sorted-map) (map (fn [^java.io.File x]
                                [(.toString (.relativize (.toPath d) (.toPath x)))
                                 [(.length x) (.lastModified x)]]))
            (filter #(.isFile ^java.io.File %) (file-seq d)))
      (sorted-map))))

(defn- changed-production-paths [before after]
  (->> (into #{} (concat (keys before) (keys after)))
       (filter #(not= (get before %) (get after %)))
       sort vec))

(defn with-hermetic-traces
  "Bind every futon2 mutable-data default to one fresh suite root, then prove
  the entire production data tree is byte-size/mtime identical."
  [f]
  (let [root (.toFile (Files/createTempDirectory "wm-data-suite-"
                                                 (make-array FileAttribute 0)))
        production data-paths/production-data-root
        before (production-file-set production)]
    (try
      ;; with-redefs is intentional: runner work can cross raw executor/thread
      ;; boundaries that do not convey Clojure dynamic bindings. The one root
      ;; remains the sole override, but it must be process-visible for :once.
      (with-redefs [data-paths/*data-root* (.getPath root)] (f))
      (let [changed (changed-production-paths
                     before (production-file-set production))]
        (is (empty? changed)
            (str "the suite must not mutate ANY production data file; changed: "
                 (pr-str changed))))
      (finally
        (doseq [file (reverse (file-seq root))] (io/delete-file file true))))))

(def ^:private selected-action
  {:kind :cascade-candidate :cascade-id :test/selected :id :test/selected
   :target "M-selected" :precedence [:test/selected-pattern]
   :construction-receipt {:cascade/id :test/selected :moves 1
                          :family-searched :unit :coverage 1}
   :interpretation-receipts [{:pattern :test/selected-pattern
                              :admitted-by :test-suite}]})

(def ^:private judgement
  {:decision (assoc-in
              (policy/select-action-cascades
               [{:action selected-action :controller-score -2.0 :rank 1}
                {:action (assoc selected-action
                                :cascade-id "M-rank-head" :id "M-rank-head"
                                :precedence [:test/other-pattern])
                 :controller-score -1.0 :rank 2}]
               {:beta 2.0})
              [:selection-certificate :token-belief-stage :prospective-carry]
              {:schema :wm/prospective-token-carry-v1
               :conditioning-status :not-wired
               :occurrence-id "test-selection-carry"
               :universe #{}})
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
