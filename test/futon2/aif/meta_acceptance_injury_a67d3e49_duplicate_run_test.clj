(ns futon2.aif.meta-acceptance-injury-a67d3e49-duplicate-run-test
  "Out-of-band acceptance for injury a67d3e49; fixed by 79755af7c."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.effective-run-history :as effective-run-history]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-terminal-injury :as terminal])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

;; Injury a67d3e495480792260838e8e3f5098dfdb8d9ccc (ledger ordinal 133,
;; :outer/duplicate-effective-run at :effective-run-comparison) was opened
;; because the duplicate-run guard's typed refusal ({:outcome :refused,
;; :failure-kind :outer/duplicate-effective-run}) passed through the
;; runner's outcome mapping untouched, the real cohort close refused the raw
;; :refused outcome (:invalid-close-outcome / [:unknown-outcome]), and the
;; last-resort close fell back to :build-failed, which the terminal
;; classifier treats as a default injury. Fix 79755af7c6fa9c10768add5adf4
;; 9fb793341a8b0 maps :refused to :guardrail-refusal and enumerates
;; :outer/duplicate-effective-run (and :outer/unchanged-refused-repeat) as
;; explicit non-injury kinds.
;;
;; On the fixed tree the real guard exception (real compare-index over a
;; real index row, real require-novel!) maps to a closeable outcome kind
;; and classifies non-injury. On the counterfactual (fix reverted) the
;; mapping still returns the raw :refused and classification still returns
;; :injury, so the is-assertions FAIL; nothing in the test body throws on
;; either tree.

(def effective-run-identity
  {:schema :wm/effective-run-identity-v1
   :target "M-typed-memories"
   :giraffe-manifest-sha256 (apply str (repeat 64 "a"))
   :policy-id "M-typed-memories/policy-ed6ff8783a9a9b045f59"
   :compiled-check-set-sha256 (apply str (repeat 64 "b"))
   :mission-source-sha256 (apply str (repeat 64 "c"))
   :base-head "b7dcf5ff678d56906b31c356b2839956d496019c"})

(defn- guard-exception
  []
  (let [dir (.toFile (Files/createTempDirectory "a67d3e49-index-"
                                                (make-array FileAttribute 0)))]
    (spit (io/file dir "outer-refusal-index.ednl")
          (str (pr-str {:schema :wm/outer-refusal-index-entry-v1
                        :status :indexed
                        :run-id "2026-10-31-prior-run"
                        :finished-work? true
                        :target "M-typed-memories"
                        :effective-run-identity effective-run-identity})
               "\n"))
    (try
      (effective-run-history/require-novel!
       (effective-run-history/compare-index dir effective-run-identity))
      (throw (IllegalStateException. "guard did not refuse"))
      (catch clojure.lang.ExceptionInfo e e))))

(def pin
  {:path "fixtures/a67d3e49-acceptance.edn"
   :sha256 (apply str (repeat 64 "d"))})

(deftest duplicate-run-guard-refusal-closes-typed-and-non-injury
  (let [e (guard-exception)
        data (ex-data e)
        outcome (@#'runner/outcome-from e)]
    ;; The real guard refusal, exactly as run 0ba3370c saw it.
    (is (= :refused (:outcome data)))
    (is (= :outer/duplicate-effective-run (:failure-kind data)))
    ;; The runner maps it to a close outcome the cohort schema accepts.
    (is (= :guardrail-refusal outcome))
    (is (contains? cohort/outcome-kinds outcome))
    ;; The terminal classifier treats the guard refusal as a typed
    ;; non-injury, not a repairable machine failure.
    (let [classified
          (terminal/classify
           {:run/id "2026-10-31-0ba3370c-1cc7-4f7d-8537-7edbf6a9ba9c"
            :click/id "wm-click-502a5b4d-2c7c-457e-8673-542d97ddc006"
            :failure {:kind :outer/duplicate-effective-run
                      :stage :effective-run-comparison
                      :error "Effective run identity already has a terminal close"}
            :terminal-receipt {:outcome outcome}}
           pin)]
      (is (= :non-injury (:classification classified)))
      (is (= :duplicate-effective-run (:non-injury-reason classified))))))
