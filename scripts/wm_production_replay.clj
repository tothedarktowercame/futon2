(ns wm-production-replay
  "Offline full-loop replay using production inputs at production scale.

  Mutable Futon data is rooted in a fresh directory.  The only live-system
  boundary replaced is Agency: the synthetic author returns a typed refusal,
  which deliberately drives the real runner through selection, construction,
  dispatch, and close without changing a source repository."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.stacktrace :as stacktrace]
            [futon2.aif.durable-hydrate :as durable-hydrate]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.data-paths :as data-paths]
            [wm-report-card :as report-card])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.time Instant]
           [java.util UUID]))

(def phases [:selection :selection-persist :construction :d-task-capture
             :author-dispatch :close])

(defn read-edn! [path]
  ;; Checkpoints may be interned (durable-intern).
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (durable-hydrate/hydrate (edn/read r))))

(defn checkpoint->judgement [cell]
  (let [cell (or (:payload cell) cell)
        j (:judgment cell)
        decision (:controller-decision j)]
    (when-not (and (map? decision) (map? (:action decision)))
      (throw (ex-info "Selection checkpoint has no enacted controller decision"
                      {:failure-kind :replay-selection-invalid})))
    {:decision decision
     :outer-task-selection (:outer-task-selection j)
     :effective-run-configuration (:effective-run-configuration j)
     :mode :production-checkpoint-replay}))

(defn phase-summary [events]
  (let [grouped (group-by :phase events)]
    (into (sorted-map)
          (for [[phase xs] grouped
                :let [start (first (filter #(= :start (:transition %)) xs))
                      end (last (filter #(= :end (:transition %)) xs))]
                :when (and start end)]
            [phase {:elapsed-ms (or (:elapsed-ms end)
                                    (:duration-ms end)
                                    (when (and (:at-ms start) (:at-ms end))
                                      (- (:at-ms end) (:at-ms start))))
                    :transition (:transition end)}]))))

(defn file-size [path]
  (when (and path (.isFile (io/file path))) (.length (io/file path))))

(defn tree-snapshot [root]
  (let [root (io/file root)]
    (into (sorted-map)
          (comp (filter #(.isFile ^java.io.File %))
                (map (fn [^java.io.File f]
                       [(.toString (.relativize (.toPath root) (.toPath f)))
                        [(.length f) (.lastModified f)]])))
          (if (.isDirectory root) (file-seq root) []))))

(defn git-snapshot [repo]
  (let [run (fn [& args]
              (let [r (apply shell/sh "git" "-C" repo args)]
               (when-not (zero? (:exit r))
                 (throw (ex-info "git snapshot failed" {:repo repo :args args :result r})))
               (.trim ^String (:out r))))]
    {:head (run "rev-parse" "HEAD")
     :status (run "status" "--porcelain=v1" "--untracked-files=all")}))

(defn assert-isolation! [before after]
  (when-not (= before after)
    (throw (ex-info "Offline replay changed canonical production state"
                    {:failure-kind :replay-isolation-violation
                     :before before :after after})))
  true)

(defn find-attempt-dir [data-root attempt-id]
  (when attempt-id
    (first (filter #(and (.isDirectory ^java.io.File %)
                         (= attempt-id (.getName ^java.io.File %)))
                   (file-seq (io/file data-root))))))

(defn replay!
  [{:keys [selection output-root run-id live-selection?]
    :or {run-id (str "offline-replay-" (UUID/randomUUID))}}]
  (when-not selection
    (throw (ex-info "--selection is required" {})))
  (when-not data-paths/test-mode?
    (throw (ex-info "Offline production replay requires -Dfuton2.data-root.test=true"
                    {:failure-kind :replay-isolation-not-process-wide})))
  (let [root (io/file (or output-root
                          (.getPath (.toFile (Files/createTempDirectory
                                             "wm-production-replay-"
                                             (make-array FileAttribute 0))))))
        _ (.mkdirs root)
        events (atom [])
        exception (atom nil)
        started-ns (System/nanoTime)
        started-at (str (Instant/now))
        judgement (when-not live-selection?
                    (checkpoint->judgement (read-edn! selection)))
        ;; This must be selected before namespaces load. A dynamic binding is
        ;; insufficient because several data-owning namespaces retain derived
        ;; paths in top-level vars (D12).
        data-root data-paths/test-data-root
        record-dir (.getPath (io/file data-root "wm-runs"))
        trace-dir (.getPath (io/file data-root "wm-trace"))
        repair-root (.getPath (io/file data-root "wm-repair-obligations"))
        _ (.mkdirs (io/file repair-root))
        prereg-path (.getPath (io/file data-root "offline-cohort.edn"))
        prereg-raw (pr-str (-> (read-edn! cohort/default-preregistration)
                               (assoc :cohort/id :wm-offline-production-replay-v1)
                               (assoc-in [:stopping-rule :target] 1)))
        prereg-sha (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                         (.getBytes prereg-raw "UTF-8"))]
                     (apply str (map #(format "%02x" (bit-and 255 %)) digest)))
        _ (spit prereg-path prereg-raw)
        _ (cohort/activate! prereg-path data-root)
        execution-cohort {:preregistration prereg-path
                          :data-root data-root
                          :cohort-id :wm-offline-production-replay-v1
                          :sha256 prereg-sha}
        dispatches (atom [])
        canonical-before {:git (git-snapshot data-paths/production-repo-root)
                          :data (tree-snapshot data-paths/production-data-root)}
        base-opts {:run-id run-id
               :cohort? true
               :execution-cohort execution-cohort
               :author "offline-author"
               :reviewer "offline-reviewer"
               :repair-reviewer "offline-repair-reviewer"
               :run-record-dir record-dir
               :repair-root repair-root
               :surprise-root data-root
               :trace-dir trace-dir
               :phase-log-fn #(swap! events conj %)
               :repair-open-fn (constantly [])
               :previous-run-fn (fn [& _] nil)
               :refresh-fn (fn [] {:outcome :ok :source :offline-replay})
               :substrate-preflight-fn (fn [_] {:route :offline-replay})
               :roster-fn (fn [_] {:offline-author {:status "idle" :invoke-ready? true}
                                    :offline-reviewer {:status "idle" :invoke-ready? true}
                                    :offline-repair-reviewer {:status "idle" :invoke-ready? true}})
               :interpretation-ask-fn nil
               :trace-fn (fn [_] (.getPath (io/file trace-dir (str run-id ".edn"))))
               ;; This is the sole simulated external boundary.  Refusal is
               ;; intentional: no repository or substrate actuator follows it.
               :dispatch-fn (fn [_ actor _ target _]
                              (let [id (str "offline-agency/" actor)]
                                (swap! dispatches conj {:actor actor :target target :job-id id})
                                {:job-id id}))
               :poll-fn (fn [_ job-id]
                          {:job-id job-id :state "done"
                           :result-summary "FULL_LOOP_AUTHOR: REFUSE offline production replay"
                           :execution {:executed false :reason :offline-production-replay}})
               :read-job-fn (fn [_ job-id]
                              {:job-id job-id :state "done"
                               :result-summary "FULL_LOOP_AUTHOR: REFUSE offline production replay"
                               :execution {:executed false :reason :offline-production-replay}})}
        base-opts (cond-> base-opts
                    (not live-selection?)
                    (assoc :judge-fn (fn [_] {:judgement judgement})))
        defaults (runtime/production-defaults base-opts)
        opts (merge defaults base-opts)]
    (binding [runner/*runtime-defaults* defaults]
      (try
        (let [result (runner/run-opportunity! opts)
              record-path (.getPath (io/file record-dir (str "tick-run-record-" run-id ".edn")))
              attempt-dir (find-attempt-dir data-root (:attempt-id result))
              checkpoints (when attempt-dir
                            (into (sorted-map)
                                  (for [f (file-seq attempt-dir)
                                        :when (and (.isFile ^java.io.File f)
                                                   (.endsWith (.getName ^java.io.File f) ".edn"))]
                                    [(.getName ^java.io.File f) (.length ^java.io.File f)])))
              cards (when (.isFile (io/file record-path))
                      (let [registry (.getPath (io/file root "empty-preregistrations"))]
                        (.mkdirs (io/file registry))
                        (report-card/generate!
                         record-path {:output-dir (.getPath (io/file root "report-card"))
                                      :preregistration-root registry
                                      :cards-by-run {}})))
              canonical-after {:git (git-snapshot data-paths/production-repo-root)
                               :data (tree-snapshot data-paths/production-data-root)}
              _ (assert-isolation! canonical-before canonical-after)
              report {:schema :wm/offline-production-replay-v1
                      :started-at started-at
                      :live-selection? (boolean live-selection?)
                      :source-selection (.getCanonicalPath (io/file selection))
                      :source-selection-bytes (file-size selection)
                      :data-root (.getCanonicalPath (io/file data-root))
                      :run-id run-id
                      :result (select-keys result [:attempt-id :outcome :data])
                      :dispatches @dispatches
                      :phase-timings (phase-summary @events)
                      :total-elapsed-ms (long (/ (- (System/nanoTime) started-ns) 1000000))
                      :checkpoint-bytes checkpoints
                      :run-record {:path record-path :bytes (file-size record-path)}
                      :report-card cards
                      :caught-exception @exception}
              report-path (io/file root "replay-report.edn")]
          (spit report-path (with-out-str (pp/pprint report)))
          (assoc report :report-path (.getPath report-path)))
        (catch Throwable t
          (reset! exception {:class (.getName (class t))
                             :message (.getMessage t)
                             :stack (with-out-str (stacktrace/print-stack-trace t))})
          (let [report {:schema :wm/offline-production-replay-v1
                        :source-selection selection
                        :data-root data-root
                        :phase-timings (phase-summary @events)
                        :total-elapsed-ms (long (/ (- (System/nanoTime) started-ns) 1000000))
                        :caught-exception @exception}
                report-path (io/file root "replay-report.edn")]
            (spit report-path (with-out-str (pp/pprint report)))
            (throw (ex-info "Offline production replay failed"
                            {:report-path (.getPath report-path)} t))))))))

(defn -main [& args]
  (let [m (apply hash-map args)
        result (replay! {:selection (get m "--selection")
                         :output-root (get m "--output-root")
                         :run-id (get m "--run-id")
                         :live-selection? (= "true" (get m "--live-selection"))})]
    (pp/pprint result)))
