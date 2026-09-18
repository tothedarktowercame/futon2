(ns futon2.aif.find-expectations-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.find-expectations :as fx]
            [futon2.aif.find-receipt :as find]
            [futon2.aif.find-receipt-test :as base])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn external-fixture
  "Build the real occurrence binding, a correct external artifact, and the
  emitted result from the same retained corpus the find tests use. The
  artifact is compiled from the RESULT here only because the test needs a
  known-correct expectation; validate-external! never sees how it was made."
  []
  (let [{:keys [record captured id]} (base/sample)
        result (find/find record captured base/root nil)
        sources (into {} (map (juxt :id identity)) (:sources record))
        target-source (get sources (get-in record [:target :source]))
        occurrence {:target (get-in record [:target :id])
                    :target-source {:path (:path target-source)
                                    :sha256 (:sha256 target-source)}
                    :repository-sha256 (:repository-sha256 result)
                    :pinned-at (get-in record [:target :pinned-at])
                    :source-digests (into (sorted-map)
                                          (map (fn [s] [(:id s) (:sha256 s)])
                                               (:sources record)))}
        artifact {:schema :wm/find-expectations-v1
                  :author {:id "codex-9" :role :external-expectation-producer}
                  :occurrence occurrence
                  :expected {id (select-keys (get-in result [:receipts id])
                                             fx/comparison-fields)}}]
    {:record record :captured captured :id id :result result
     :occurrence occurrence :artifact artifact}))

(defn refusal [reason f]
  (let [d (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (map? d) (str "expected refusal " reason))
    (is (= reason (:reason d)) (str "expected " reason ", got " (:reason d)))
    (is (= :F2 (:law d)))
    d))

(deftest control-1-correct-independent-input-is-accepted
  (let [{:keys [occurrence artifact result]} (external-fixture)]
    (is (= result (fx/validate-external! occurrence artifact result)))
    ;; Round-trip through the EDN entry point: the artifact is a file, not a
    ;; hand-passed map, in real use.
    (let [temp (.toFile (Files/createTempDirectory "find-expectations" (make-array FileAttribute 0)))
          path (.getAbsolutePath (io/file temp "expectations.edn"))]
      (try
        (spit path (pr-str artifact))
        (is (= result (fx/validate-external! occurrence (fx/read-artifact path) result)))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))

(deftest control-2-self-supplied-author-is-rejected
  (let [{:keys [occurrence artifact result]} (external-fixture)]
    (doseq [role [:finder :interpreter :pattern-author]]
      (refusal :self-supplied-expectations
               #(fx/validate-external! occurrence
                                       (assoc-in artifact [:author :role] role)
                                       result)))
    ;; The declared-role control is not authentication: any other id with the
    ;; external role still passes, which is exactly what the TASK permits.
    (is (= result (fx/validate-external! occurrence
                                         (assoc-in artifact [:author :id] "someone-else")
                                         result)))))

(deftest control-3-mismatched-occurrence-is-rejected
  (let [{:keys [occurrence artifact result]} (external-fixture)]
    (doseq [mutate [#(assoc-in % [:occurrence :target] "M-someone-elses-mission")
                    #(assoc-in % [:occurrence :target-source :sha256] (apply str (repeat 64 "0")))
                    #(assoc-in % [:occurrence :repository-sha256] "not-this-repository")
                    #(assoc-in % [:occurrence :pinned-at] "1999-01-01T00:00:00Z")
                    #(assoc-in % [:occurrence :source-digests "s"] "changed")]]
      (refusal :occurrence-binding-mismatch
               #(fx/validate-external! occurrence (mutate artifact) result)))
    (refusal :occurrence-required #(fx/validate-external! nil artifact result))
    (refusal :occurrence-required #(fx/validate-external! {} artifact result))))

(deftest control-4-unexpected-selected-pattern-is-rejected
  (let [{:keys [occurrence artifact result id]} (external-fixture)]
    (refusal :unexpected-selected-pattern
             #(fx/validate-external! occurrence (update artifact :expected dissoc id) result))))

(deftest control-5-deliberately-wrong-expected-clause-is-rejected
  (let [{:keys [occurrence artifact result id]} (external-fixture)]
    (doseq [mutate [#(assoc-in % [:expected id :acknowledged-clause :text] "plausible but wrong clause text")
                    #(assoc-in % [:expected id :acknowledged-clause :lines] [1 1])
                    #(assoc-in % [:expected id :clause-kind] :however-clause)
                    #(assoc-in % [:expected id :route] :agent-asserted)]]
      (refusal :expectation-mismatch
               #(fx/validate-external! occurrence (mutate artifact) result)))
    ;; And the mirror direction: a genuine receipt tampered after the fact.
    (refusal :expectation-mismatch
             #(fx/validate-external! occurrence artifact
                                     (assoc-in result [:receipts id :acknowledged-clause :text]
                                               "finder made this up")))))

(deftest there-is-no-optional-artifact-path
  (let [{:keys [occurrence result]} (external-fixture)]
    (refusal :external-expectations-required #(fx/validate-external! occurrence nil result)))
  (let [{:keys [occurrence result artifact]} (external-fixture)]
    (refusal :invalid-expectation-artifact
             #(fx/validate-external! occurrence (dissoc artifact :schema) result))
    ;; An artifact with no expected rows reaches the comparison and refuses
    ;; on the selected receipt's missing row -- not silently skipped.
    (refusal :unexpected-selected-pattern
             #(fx/validate-external! occurrence (assoc artifact :expected {}) result))
    ;; Structural emptiness is refused at the EDN entry point.
    (let [temp (.toFile (Files/createTempDirectory "find-expectations-bad" (make-array FileAttribute 0)))
          path (.getAbsolutePath (io/file temp "bad.edn"))]
      (try
        (spit path (pr-str {:schema :wm/find-expectations-v1
                            :author {:id "x" :role :external-expectation-producer}
                            :occurrence (:occurrence artifact) :expected {}}))
        (refusal :invalid-expectation-artifact #(fx/read-artifact path))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))
    (refusal :invalid-result
             #(fx/validate-external! occurrence artifact {:selected []}))))

;; claude-4's review, 2026-09-17. Two holes in the accept direction:
;;
;; (a) an artifact expecting a pattern the finder never selected passed. A
;;     finder that retrieves a SUBSET of what was independently expected is
;;     exactly what these expectations exist to catch — retrieving too little
;;     is the quiet failure, and it validated clean.
;; (b) a row carrying its own provenance (which frozen context it came from,
;;     who wrote it) refused as :expectation-mismatch, because the whole row
;;     was compared against four selected keys.
(deftest control-6-an-expectation-that-did-not-fire-is-rejected
  (let [{:keys [occurrence artifact result id]} (external-fixture)
        as-of (get-in artifact [:expected id :as-of])
        never {:clause-kind :if-clause
               :acknowledged-clause {:text "never emitted" :lines [1 2]}
               :route :structured-antecedent
               :as-of as-of}
        with-missing (assoc-in artifact [:expected :made-up/pattern] never)]
    (let [d (refusal :expected-pattern-not-selected
                     #(fx/validate-external! occurrence with-missing result))]
      (is (= [:made-up/pattern] (:patterns d))))
    ;; an explicit opt-out is honoured: a row may constrain content only IF
    ;; the pattern fires, but it must say so
    (is (= result (fx/validate-external!
                   occurrence
                   (assoc-in with-missing [:expected :made-up/pattern :must-fire?] false)
                   result)))))

(deftest a-row-may-carry-its-own-provenance
  (let [{:keys [occurrence artifact result id]} (external-fixture)
        tagged (-> artifact
                   (assoc-in [:expected id :produced-from] "FROZEN-CONTEXT.edn")
                   (assoc-in [:expected id :produced-by] "an independent producer"))]
    (is (= result (fx/validate-external! occurrence tagged result)))))

(deftest build-artifact-pulls-text-from-captured-bytes
  ;; The build-time strengthening ported from the reverted singular
  ;; duplicate (6eabefe6, claude-4 duplicate finding 2026-09-18): the
  ;; author declares LINES, never TEXT — text comes from the pinned bytes,
  ;; and a span outside the authored IF block is refused at BUILD.
  (let [{:keys [record captured id result occurrence]} (external-fixture)
        entry-lines (get-in result [:receipts id :acknowledged-clause :lines])
        built (fx/build-artifact
               {:library-root base/root
                :sources (:sources record)
                :read-bytes captured
                :author {:id "an-external-producer"}
                :occurrence occurrence
                :expected {id {:lines entry-lines}}})]
    (is (= (get-in result [:receipts id :acknowledged-clause :text])
           (get-in built [:expected id :acknowledged-clause :text]))
        "text read from the captured bytes at the declared span")
    (is (= result (fx/validate-external! occurrence built result))
        "a built artifact validates the real occurrence's receipts")
    (is (= :not-authored-clause
           (:reason (try (fx/build-artifact
                          {:library-root base/root
                           :sources (:sources record)
                           :read-bytes captured
                           :author {:id "an-external-producer"}
                           :occurrence occurrence
                           :expected {id {:lines [(inc (second entry-lines))
                                                  (inc (second entry-lines))]}}})
                         (catch clojure.lang.ExceptionInfo e (ex-data e)))))
        "a span outside the authored IF block is refused at BUILD")))
