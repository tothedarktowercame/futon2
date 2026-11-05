(ns futon2.aif.run-ending-failure-close-classification-test
  "OUTER-DISPATCH-CLASSIFICATION-ABSENT packet: a completed cohort close
  whose failure happened before construction has no action occurrence, yet
  its run-ending classification receipt must be retained through the normal
  failure-close path. Occurrence-specific retention (retention-inputs,
  evidence manifest) stays gated on the occurrence. The reader side
  (outer-consumption-test) already covers not-yet-evaluated consumption,
  exactly-once, in-flight/tamper deferral and terminal-class passthrough."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.outer-pending-observation :as pending]
            [futon2.aif.outer-refusal-index :as durable]
            [futon2.aif.wm.cascade-runner-seam-test :as seam]))

(use-fixtures :once (fn [f] (f)))

(defn- isolated [f]
  ;; The test alias can preload the canonical runner while discovering its
  ;; large fixture namespace. Reload the worktree source BEFORE entering the
  ;; hermetic fixtures: reloading inside with-hermetic-traces re-defs the
  ;; default roots and would point the run-record writer back at production
  ;; (an earlier revision of this test did exactly that).
  (require 'futon2.aif.full-loop-runner :reload)
  (hermetic/with-hermetic-stores #(fixture/with-hermetic-traces f)))

(defn- run-pre-construction-failure
  "A cohort click whose selection checkpoint append fails before any
  construction or dispatch, so the close handler runs with a nil action
  occurrence — the f74df9cc shape at the runner level."
  []
  (let [{:keys [root] :as c} (#'fixture/retention-cohort "classification-producer-")
        refused-selection? (atom false)
        append cohort/append-checkpoint!
        close cohort/close-attempt!
        record-finding (fn [finding]
                         (let [record (assoc finding :repair/id "classification-producer")]
                           record))]
    (try
      (let [click
            (with-redefs-fn
               {#'runner/refuse-on-runner-source-drift!
                (constantly {:runner/source-check :fixture-current})}
               #(with-redefs [cohort/append-checkpoint!
                           (fn [& args]
                             (let [checkpoint (nth args (- (count args) 2))
                                   cell (last args)]
                               (if (and (= :selection checkpoint)
                                        (get cell :judgment)
                                        (compare-and-set! refused-selection? false true))
                                 (throw
                                  (ex-info
                                   "cohort checkpoint exceeds the bounded event size"
                                   {:failure-kind :cohort-checkpoint-oversized
                                    :reason :checkpoint-size-limit-exceeded
                                    :size-bytes (inc cohort/max-checkpoint-bytes)
                                    :max-bytes cohort/max-checkpoint-bytes}))
                                 (apply append args))))
                           cohort/close-attempt! close]
                  (#'seam/click!
                   [seam/px seam/py]
                   {:cohort? true
                    :execution-cohort (:binding c)
                    :tripwire/cohort-history []
                    :tripwire/a-matrix-events []
                    :tripwire/grounding-witnesses []
                    :repair-system-record-fn record-finding
                    :cascade-reading-pins
                    {"M-current" {:excerpt-digest (#'seam/hex [:excerpt])
                                  :analysis-digest (#'seam/hex [:analysis])}}
                    :cascade-occurrence-readings (fn [_] seam/square-readings)
                    :open-cascade-resume-fn (fn [error & _] error)}
                   {:bind-cascade-identity
                    {:readings (fn [_] seam/square-readings)}})))]
        {:result (:result click)
         :events (cohort/attempt-events
                  (io/file root "test-cohort-exhaustion" "attempt-001"))
         ;; Read before the finally below deletes the temp cohort root.
         :retained-receipt
         (let [f (io/file root "test-cohort-exhaustion" "attempt-001"
                          "retained" "run-ending-classification.edn")]
           (when (.isFile f)
             {:path (.getPath f)
              :receipt (edn/read-string (slurp f))}))
         :attempt-dir (io/file root "test-cohort-exhaustion" "attempt-001")})
      (finally
        (doseq [file (reverse (file-seq (io/file root)))]
          (io/delete-file file true))))))

(deftest failure-close-retains-classification-without-action-occurrence
  (isolated
   (fn []
     (let [{:keys [result events retained-receipt]} (run-pre-construction-failure)
           closed (filter #(= :closed (:checkpoint/type %)) events)]
       (is (= :build-failed (:outcome result)))
       (is (= :cohort-checkpoint-oversized
              (get-in result [:data :failure-kind])))
       ;; The failure predates construction: no action transition occurrence.
       (is (nil? (get-in result [:data :occurrence])))
       ;; 1. The receipt was retained through the normal writer.
       (is (map? retained-receipt)
           "failure close retains run-ending-classification.edn without an occurrence")
       (let [receipt (:receipt retained-receipt)]
         (is (= :wm/run-ending-classification-receipt-v1 (:schema receipt)))
         (is (= :recorded (:status receipt)))
         ;; Typed failure details preserved; nothing turned into a success.
         (is (= :stop-the-line (:class receipt)))
         (is (= :cohort-checkpoint-oversized (:failure-kind receipt)))
         (is (nil? (:missing receipt)))
         (is (nil? (get-in receipt [:identity :occurrence])))
         ;; No action occurrence means no attested increment or facet.
         (is (nil? (:attestation receipt)))
         ;; 2. The sealed close event and the retained receipt agree.
         (is (= 1 (count closed)))
         (is (= receipt (get-in (first closed) [:payload :judgment
                                                :run-ending-classification])))
         ;; 3. Occurrence-specific retention stays gated: no retention-inputs
         ;;    or evidence manifest on an occurrence-less failure close.
         (is (nil? (get-in result [:data :retention-inputs])))
         (is (nil? (get-in result [:data :evidence-manifest]))))))))

(defn retained-ending
  "Read a content-verified completed fixture with RECEIPT republished."
  [receipt]
  ;; Reuses the outer-consumption fixture's real record/stream/index shape;
  ;; only the classification receipt is replaced and its pin re-published.
  (let [source (.getParentFile (io/file (io/resource "fixtures/outer-consumption/README.md")))
           ;; resolved from a FILE resource: a directory resource enters the
           ;; Test Registry closure as an entry it cannot hash and the warrant
           ;; dies on it (zai-16, 2026-09-17; cascade_sources/load-declared)
        root (.toFile (java.nio.file.Files/createTempDirectory
                       "classification-producer-reader-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))]
    (doseq [file (file-seq source) :when (.isFile file)
            :let [_ (io/resource (str "fixtures/outer-consumption/"
                                      (.relativize (.toPath source) (.toPath file))))]]
      (let [target (io/file root (str (.relativize (.toPath source)
                                                   (.toPath file))))]
        (io/make-parents target) (io/copy file target)))
    (let [record-file (io/file root (str "tick-run-record-"
                                         "2026-10-29-f74df9cc-c27b-420b-91ec-7a32db708c41"
                                         ".edn"))
          index (io/file root "reading-policy-class-history-index.ednl")
          receipt (merge {:schema :wm/run-ending-classification-receipt-v1
                          :kernel/id :wm/run-ending-classification-kernel-v1
                          :mode :record-only :status :recorded
                          :missing [] :identity {:occurrence nil}}
                         receipt)]
      (spit record-file
            (pr-str (assoc (edn/read-string (slurp record-file))
                           :run-ending-classification receipt)))
      (spit index
            (str (pr-str (assoc (edn/read-string (slurp index))
                                :record-sha256
                                (#'durable/sha256-file record-file)))
                 "\n"))
      (pending/observe
       root "2026-10-29-f74df9cc-c27b-420b-91ec-7a32db708c41"))))

(defn nonterminal-ending []
  (retained-ending {:class :unknown :failure-kind :dispatch-failed}))

(defn typed-failure-ending []
  (retained-ending {:class :stop-the-line :failure-kind :dispatch-failed
                    :facet {:target "M-selected" :class :focus
                            :ending-class :focused}}))

(deftest retained-unclassified-receipt-defers-learning
  (isolated
   (fn []
     ;; A genuinely non-terminal producer class remains deferred under ENDING-2.
     (let [ending (nonterminal-ending)]
       (is (= :deferred (:status ending)))
       (is (= :terminal-ending-class-absent (:reason ending)))
       (is (not (contains? ending :ending-class)))
       (is (= :terminal-ending-class-unavailable
              (get-in ending [:classification-absence :reason])))
       (is (= :unknown
              (get-in ending [:classification-absence :recorded-class])))
       (is (= true (get-in ending [:classification-absence :completed])))))))

(deftest retained-typed-failure-is-a-ready-stop-the-line-ending
  (isolated
   (fn []
     (let [ending (typed-failure-ending)]
       (is (= :ready (:status ending)))
       (is (= :stop-the-line (:ending-class ending)))
       (is (not (contains? ending :classification-absence)))))))
