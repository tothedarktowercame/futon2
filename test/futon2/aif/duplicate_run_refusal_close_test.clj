(ns futon2.aif.duplicate-run-refusal-close-test
  "DUPLICATE-RUN-REFUSAL-INVALID-CLOSE: run 0ba3370c's duplicate-run guard
  refusal closed as :build-failed and opened injury a67d3e49. These tests
  drive the real guard exception (real compare-index over a real index file,
  real require-novel!) through the runner's real outcome mapping, the real
  cohort close, and the real meta-terminal classification."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.effective-run-history :as effective-run-history]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-terminal-injury :as terminal])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def prereg-path
  (let [f (io/file "holes/labs/M-aif-full-loop-40/cohort.edn")]
    (when-not (.isFile f)
      (throw (IllegalStateException. "cohort prereg not found; run from worktree root")))
    (.getPath f)))

(defn- tmp-dir [prefix]
  (.getPath (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0)))))

(defn- term [judgment]
  {:judgment judgment :ground {:kind :test-witness}})

(def effective-run-identity
  "Comparable identity: every required key present, plus base-head."
  {:schema :wm/effective-run-identity-v1
   :target "M-typed-memories"
   :giraffe-manifest-sha256 (apply str (repeat 64 "a"))
   :policy-id "M-typed-memories/policy-ed6ff8783a9a9b045f59"
   :compiled-check-set-sha256 (apply str (repeat 64 "b"))
   :mission-source-sha256 (apply str (repeat 64 "c"))
   :base-head "b7dcf5ff678d56906b31c356b2839956d496019c"})

(defn- indexed-prior-run!
  "Write a real blocking row the way append-run-record! publishes one: an
  :indexed terminal row for a run whose effective-run-identity has already
  finished work."
  [dir]
  (let [index (io/file dir "outer-refusal-index.ednl")]
    (spit index
          (str (pr-str {:schema :wm/outer-refusal-index-entry-v1
                        :status :indexed
                        :run-id "2026-10-31-prior-run"
                        :finished-work? true
                        :target "M-typed-memories"
                        :effective-run-identity effective-run-identity})
               "\n"))
    (.getPath index)))

(defn- guard-exception
  "The REAL guard exception: compare-index reads the real index file and
  require-novel! throws exactly what the runner's :effective-run-comparison
  phase saw on run 0ba3370c."
  []
  (let [dir (tmp-dir "duplicate-run-index-")]
    (indexed-prior-run! dir)
    (try
      ((requiring-resolve 'futon2.aif.effective-run-history/require-novel!)
       (effective-run-history/compare-index dir effective-run-identity))
      (throw (IllegalStateException. "guard did not refuse"))
      (catch clojure.lang.ExceptionInfo e
        e))))

(defn- open-attempt! [root opportunity-id]
  (cohort/start-attempt!
   prereg-path root
   (term {:opportunity-id opportunity-id
          :trigger :wallclock-cron
          :machine-state {:tick 1}
          :agent-roster []
          :code-state {:git-sha "07a6b7eceb57a912698e7c670426f0c0f1663956"
                       :git-dirty? false
                       :resolved-mode-flags {}
                       :configuration-digest "test"}
          :semantic-epoch :epoch-1})))

(defn- append-required! [root attempt]
  (doseq [checkpoint [:selection :construction :dispatch :build :adjudication]]
    (cohort/append-checkpoint! prereg-path root attempt checkpoint
                               {:sorry {:kind (keyword (str "test-" (name checkpoint)))}})))

(def pin
  {:path "fixtures/duplicate-run-close.edn"
   :sha256 (apply str (repeat 64 "d"))})

;; The run record 0ba3370c would have persisted: guard failure retained
;; through run-record-failure, close outcome on the terminal receipt.
(defn- refusal-run-record [outcome]
  {:run/id "2026-10-31-0ba3370c-1cc7-4f7d-8537-7edbf6a9ba9c"
   :click/id "wm-click-duplicate-run"
   :failure {:kind :outer/duplicate-effective-run
             :stage :effective-run-comparison
             :error "Effective run identity already has a terminal close"}
   :terminal-receipt {:outcome outcome}})

(deftest guard-refusal-maps-to-typed-refusal-outcome
  (let [e (guard-exception)
        data (ex-data e)]
    (is (= :refused (:outcome data)))
    (is (= :outer/duplicate-effective-run (:failure-kind data)))
    ;; the runner's own mapping: :refused must become a close outcome the
    ;; cohort schema knows, not pass through raw.
    (let [outcome (@#'runner/outcome-from e)]
      (is (= :guardrail-refusal outcome))
      (is (contains? cohort/outcome-kinds outcome)
          "mapped outcome must be a valid close outcome kind"))))

(deftest raw-refused-outcome-is-refused-by-the-real-close
  ;; The exact 0ba3370c failure, retained as the bad case: passing the
  ;; guard's raw :refused outcome to the real close throws
  ;; :invalid-close-outcome / [:unknown-outcome].
  (let [root (tmp-dir "duplicate-run-bad-close-")
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open-attempt! root "duplicate-run/refused-raw"))
        _ (append-required! root attempt)
        cell (term {:outcome :refused
                    :grounded? false
                    :artifact-only? false
                    :duration-ms 1
                    :resource-use {:agent-turns 0}
                    :failure-kind :outer/duplicate-effective-run
                    :failure-stage :effective-run-comparison})]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"invalid close outcome"
         (cohort/close-attempt! prereg-path root attempt cell)))
    (let [data (try
                 (cohort/close-attempt! prereg-path root attempt cell)
                 nil
                 (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :invalid-close-outcome (:failure-kind data)))
      (is (= :build-failed (:outcome data)))
      (is (= [:unknown-outcome] (:errors data))))))

(deftest guard-refusal-closes-typed-through-the-real-close
  (let [root (tmp-dir "duplicate-run-typed-close-")
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open-attempt! root "duplicate-run/typed-refusal"))
        _ (append-required! root attempt)
        e (guard-exception)
        outcome (@#'runner/outcome-from e)
        closed (cohort/close-attempt!
                prereg-path root attempt
                (term {:outcome outcome
                       :grounded? false
                       :artifact-only? false
                       :duration-ms 1
                       :resource-use {:agent-turns 0}
                       :failure-kind (:failure-kind (ex-data e))
                       :failure-stage (:failure-stage (ex-data e))}))]
    (is (= :guardrail-refusal
           (get-in closed [:payload :judgment :outcome])))
    (is (= :outer/duplicate-effective-run
           (get-in closed [:payload :judgment :failure-kind])))))

(deftest guard-refusal-terminal-classifies-non-injury
  ;; The good case: the typed guard refusal close classifies non-injury with
  ;; its own retained reason. The close outcome does not rescue an
  ;; unenumerated failure kind, and the classification stays fail-closed for
  ;; kinds outside the explicit enumeration.
  (let [classified (terminal/classify (refusal-run-record :guardrail-refusal) pin)]
    (is (= :non-injury (:classification classified)))
    (is (= :duplicate-effective-run (:non-injury-reason classified))))
  ;; Even the 0ba3370c shape as actually persisted (:build-failed fallback)
  ;; classifies non-injury now: the guard refusal is not a repairable
  ;; machine failure however the close was forced.
  (let [fallback (terminal/classify (refusal-run-record :build-failed) pin)]
    (is (= :non-injury (:classification fallback))))
  ;; Fail-closed: an unenumerated kind still opens the default injury.
  (let [unknown (terminal/classify {:run/id "unknown-kind-run"
                                    :click/id "click-x"
                                    :failure {:kind :fold-output-invalid
                                              :stage :close}}
                                   pin)]
    (is (= :injury (:classification unknown)))))
