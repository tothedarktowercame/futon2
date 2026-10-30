(ns futon2.aif.outer-consumption-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.edn :as edn] [clojure.string :as str] [clojure.java.io :as io]
            [futon2.aif.outer-state :as state]
            [futon2.aif.outer-pending-observation :as pending]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.outer-observation :as observation]
            [futon2.aif.outer-refusal-index :as durable]
            [futon2.aif.focus-receipt :as focus]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))
(use-fixtures :once fixture/with-hermetic-traces hermetic/with-hermetic-stores)
(def run-id "2026-10-29-f74df9cc-c27b-420b-91ec-7a32db708c41")
(defn read-edn [p] (edn/read-string (slurp p)))
(defn copy-fixture []
  (let [source (io/file (io/resource "fixtures/outer-consumption"))
        root (.toFile (java.nio.file.Files/createTempDirectory "outer-consumption-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (doseq [file (file-seq source) :when (.isFile file)]
      (let [target (io/file root (str (.relativize (.toPath source) (.toPath file))))]
        (io/make-parents target) (io/copy file target)))
    root))
(defn makeup [root]
  ;; Migration evidence retains the canonical declaration identity, not the
  ;; test copy's physical location. No canonical state/record is read.
  (assoc (state/load-makeup (io/file root "outer-makeup.edn"))
         :makeup/path state/default-makeup-path))
(defn current-observation []
  (let [{:keys [observation relation-classes]}
        (read-edn (io/resource "fixtures/q6-reselection/preview.edn"))]
    (assoc observation :relation (fn [_ target] (get relation-classes target)))))
(defn approx [a b] (< (Math/abs (- (double a) (double b))) 1e-12))

(deftest retained-run-consumed-and-replaced-atomically-on-real-outer-path
  (let [root (copy-fixture) m (makeup root) state-path (io/file root "outer-state.edn")
        before (state/read-state m state-path) old-pending (:pending before)
        current (current-observation) current-dir (io/file root "current")
        opts {:outer/state-path (str state-path) :run-record-dir (str root) :run-id "click51-test"}
        ending (pending/observe root run-id)
        prev (#'runtime/previous-giraffe-observation current-dir m old-pending)
        obs (#'runtime/learning-observation old-pending prev)
        gain (#'runtime/ready-gain prev current (get-in old-pending [:selection :selection :selected :target]))
        captured (atom nil)
        result (with-redefs-fn
                 {#'state/load-makeup (constantly m)
                  #'runtime/find-giraffe-run (constantly current-dir)
                  #'observation/outer-observation (constantly current)
                  #'focus/read-inputs (constantly {}) #'focus/relation-context (constantly {})
                  #'runtime/run-ordinary-opportunity-with-task!
                  (fn [o] (reset! captured o) {:status :before-author})}
                 #(#'runtime/run-ordinary-opportunity! opts))
        after (state/read-state m state-path)
        repeated (state/consume-observation m after old-pending obs (assoc ending :observed-gain gain))]
    (is (= :before-author (:status result)) (pr-str result))
    (is (= :not-yet-evaluated (:ending-class ending)))
    (is (= :classification-receipt-absent (get-in ending [:classification-absence :reason])))
    (is (= {:kind :dispatch-failed :stage :ticket-readiness-refresh :error "Agency dispatch failed"}
           (get-in ending [:classification-absence :failure])))
    (is (= 0 (:click-count before)))
    (is (= 1 (:click-count after)))
    ;; R20Step.endingDelta / advanceC: notYetEvaluated contributes zero C mass.
    (doseq [[k v] (:c before)] (is (approx (* (:omega-c m) v) (get-in after [:c k]))))
    ;; R20Step.advanceStateCounts: zero likelihood contributes no posterior.
    (doseq [[status row] (:state-counts before) [k v] row]
      (is (approx (* (:omega-state m) v) (get-in after [:state-counts status k]))))
    ;; R20Step.advanceGainCounts still learns the observed gain on selection.
    (doseq [[k v] (:gain-counts before)]
      (is (approx (+ (* (:omega-gain m) v) (if (= k gain) (:eta-gain m) 0))
                  (get-in after [:gain-counts k]))))
    (is (= "click51-test" (get-in after [:pending :run-id])))
    (is (= (get-in @captured [:outer/selection :outer/learning :receipt])
           (get-in after [:outer/consumptions run-id])))
    (is (= :already-consumed (:status repeated)))
    (is (= after (:state repeated)))
    ;; The existing Q6/reading reselection replaces this same click's pending
    ;; target without learning the predecessor a second time.
    ((:selected-target-reading-reselect-fn @captured)
     {:target (get-in @captured [:task :id]) :kind :repeat-choice-after-refusal})
    (let [reselected (state/read-state m state-path)]
      (is (= 1 (:click-count reselected)))
      (is (= "M-peradam-grounding"
             (get-in reselected [:pending :selection :selection :selected :target])))
      (is (= (:outer/consumptions after) (:outer/consumptions reselected))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Outer state refused"
                         (state/replace-pending before {:run-id "overwrite"})))
    (println :consumption {:click-count (:click-count after) :gain gain
                          :target (get-in after [:pending :selection :selection :selected :target])})))

(deftest in-flight-and-tampered-records-defer
  (let [root (copy-fixture) stream (io/file root "run-stream.edn")
        saved (slurp stream) file (io/file root (str "tick-run-record-" run-id ".edn"))]
    (spit stream (str (first (str/split-lines saved)) "\n"))
    (is (= :run-in-flight (:reason (pending/observe root run-id))))
    (spit stream saved)
    (spit file (pr-str (assoc (read-edn file) :failure {:kind :fabricated})))
    (is (= :run-record-tampered (:reason (pending/observe root run-id))))
    (spit file "{")
    (is (= :run-record-unreadable (:reason (pending/observe root run-id))))))

(deftest verified-terminal-class-is-used
  (let [root (copy-fixture) file (io/file root (str "tick-run-record-" run-id ".edn"))
        index (io/file root "reading-policy-class-history-index.ednl")]
    ;; Control for the already-classified arm. It uses the real record shape
    ;; and re-publishes its pin, rather than treating a changed record as trusted.
    (spit file (pr-str (assoc (read-edn file) :run-ending-classification
                             {:schema :wm/run-ending-classification-receipt-v1
                              :status :recorded :class :related})))
    (spit index (str (pr-str (assoc (read-edn index) :record-sha256 (durable/sha256-file file))) "\n"))
    (is (= :related (:ending-class (pending/observe root run-id))))))
