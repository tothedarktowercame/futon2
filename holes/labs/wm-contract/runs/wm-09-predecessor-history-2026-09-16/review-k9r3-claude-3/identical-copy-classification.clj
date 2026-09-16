;; claude-3 supplement to REVIEW-K9R3: classifier coverage for the latest identical-copy refusal at 276a8dc1.
;; The refusal is produced by the real previous! on a hermetic identical-copy fixture, then thrown from the
;; construct! port through the real receipt-mode run-case inside both hermetic namespace fixtures.
(require '[clojure.java.io :as io]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.receipt-construction-test :as history]
         '[futon2.aif.interpretation-job :as job]
         '[futon2.aif.interpretation-job-test :as caller]
         '[futon2.aif.full-loop-runner :as runner]
         '[futon2.aif.hermetic-repair-fixture :as hermetic]
         '[futon2.aif.full-loop-runner-test :as runner-fixture])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])

(def temp (.toFile (Files/createTempDirectory "c3-k9r3-cls" (make-array FileAttribute 0))))
(def refusal
  (try
    (let [files (history/discovery-history! (io/file temp "live") "M-history" false)
          dest (io/file temp "live" "archives" "snapshot" "fixture" "attempt-002")]
      (.mkdirs dest)
      (doseq [f files] (spit (io/file dest (.getName f)) (slurp f)))
      (try (construction/previous! {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}}
                                   [(io/file temp "live")])
           nil
           (catch clojure.lang.ExceptionInfo e e)))
    (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))

(let [d (ex-data refusal)]
  (prn {:refusal-ex-data-keys (sort (keys d))
        :interpretation/refusal (:interpretation/refusal d)
        :construction/refusal (:construction/refusal d)
        :failure-kind-present? (contains? d :failure-kind) :outcome-present? (contains? d :outcome)
        :identical-checkpoint-sets? (:identical-checkpoint-sets? d)
        :paths (count (:files d)) :cause (some-> (.getCause ^Throwable refusal) class .getName)}))

(let [{:keys [kind failure-kind]} (@#'job/failure-classification refusal @#'runner/transport-failure-kind)]
  (prn {:direct :identical-copy-refusal :job-kind kind :failure-kind failure-kind
        :repair-class (#'runner/repair-class-for failure-kind)}))

(hermetic/with-hermetic-stores
 (fn []
   (runner-fixture/with-hermetic-traces
    (fn []
      (doseq [[label thrower] [[:identical-copy-refusal-through-run-case #(throw refusal)]
                               [:injected-code-fault #(throw (NullPointerException. "controlled code fault"))]]]
        (let [{:keys [result calls constructors failures]}
              (with-redefs [construction/construct! (fn [& _] (thrower))]
                (caller/run-case :receipt :valid))]
          (prn {:case label
                :runner-failure-kind (get-in result [:data :failure-kind])
                :repair-class (get-in result [:data :repair-obligation :repair/class])
                :recorded-interpretation-kind (get-in failures [0 :failure :kind])
                :calls calls :legacy-constructor-calls constructors})))))))
(shutdown-agents)
