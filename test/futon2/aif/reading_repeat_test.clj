(ns futon2.aif.reading-repeat-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.reading-refusal-history :as history]
            [futon2.aif.outer-refusal-history :as outer]
            [futon2.aif.outer-refusal-index :as index]
            [futon2.aif.effective-run-history :as effective]
            [futon2.aif.selected-target-reading :as reading]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.reading-file-delivery-test :as file-test]
            [futon2.aif.selected-target-reading-test :as existing]
            [futon2.aif.target-reading-registry :as registry]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))
(use-fixtures :once fixture/with-hermetic-traces hermetic/with-hermetic-stores)
(def retained (edn/read-string (slurp (io/resource "fixtures/reading-repeat/retained.edn"))))
(defn setup []
  (let [root (#'existing/temp-dir "reading-repeat-") source (io/file root "M-typed-memories.md")]
    (io/copy (io/file (io/resource "fixtures/reading-file-delivery/M-typed-memories.md")) source)
    {:opts {:run-id "repeat" :author "wm-author" :request-root (str (io/file root "requests"))
            :run-record-dir (str root) :target-reading-root (str (io/file root "readings"))
            :reader-configuration {:seat "wm-author" :provider :zai-api-compatible
                                   :model "glm-5.3" :output-budget 8192}}
     :judgement (#'existing/judgement "M-typed-memories" (str source) (registry/excerpt-digest source))}))
(defn fail-reading [opts judgement]
  (reading/repair! (assoc opts :answer-fn (fn [_] {:state "done" :job-id "failed" :result "No JSON"})) judgement))
(defn publish-row [opts attempt]
  (let [r (assoc retained :selected-target-reading
                 (assoc (:selected-target-reading retained) :reading-attempts
                        [attempt (second (get-in retained [:selected-target-reading :reading-attempts]))]))
        path (io/file (:run-record-dir opts) "tick-run-record-fixture.edn")]
    (spit path (pr-str r))
    (index/append-run-record! (:run-record-dir opts) path r)))
(deftest intermediate-refusal-survives-success-and-prevents-contact
  (let [{:keys [opts judgement]} (setup)
        failed (fail-reading opts judgement)
        _ (publish-row opts failed)
        calls (atom 0)
        result (reading/repair! (assoc opts :run-id "next-click" :answer-fn
                                     (fn [_] (swap! calls inc) (throw (ex-info "contact forbidden" {})))) judgement)
        rows (outer/read-index (:run-record-dir opts))]
    (is (= :selected-target-reading-unavailable (:kind failed)))
    (is (= 2 (count rows)))
    (is (= "M-peradam-grounding" (:target (first rows))))
    (is (= {} (:failure (first rows))))
    (is (= "M-typed-memories" (:target (second rows))))
    (is (nil? (:failure (second rows))))
    (is (= 0 @calls))
    (is (= :selected-target-reading-unchanged-refusal (:kind result)))
    (is (= [] (get-in result [:reading-repeat-comparison :matched-refusal :differing-identities])))
    (is (empty? (outer/prior-refusals (:run-record-dir opts) "M-typed-memories")))
    (let [output (str (io/file (:run-record-dir opts) "backfill.ednl"))]
      (index/backfill! (:run-record-dir opts) output)
      (is (= 2 (count (str/split-lines (slurp output))))))))
(deftest changed-delivery-admits-real-file-and-retains-difference
  (let [{:keys [opts judgement]} (setup)
        measured (fail-reading opts judgement)
        ;; Explicit control arm, not fabricated historical evidence: only the
        ;; measured delivery identity differs; source and request stay fixed.
        prior (assoc-in measured [:reading-inputs :delivery :version] :inline-v1)
        _ (publish-row opts prior)
        calls (atom 0)
        result (reading/repair! (assoc opts :answer-fn (fn [r] (swap! calls inc) (file-test/delivered r))) judgement)]
    (is (= 1 @calls))
    (is (= :published (:status result)))
    (is (= :inputs-changed (get-in result [:reading-repeat-comparison :status])))
    (is (= [:delivery] (get-in result [:reading-repeat-comparison :comparisons 0 :differing-identities])))
    (is (= :valid (get-in result [:validation :status])))))
(deftest legacy-and-unresolved-reader-identity-remain-incomparable
  (let [{:keys [opts judgement]} (setup)
        measured (fail-reading opts judgement)
        legacy (first (get-in retained [:selected-target-reading :reading-attempts]))
        _ (publish-row opts legacy)
        result (reading/repair! (assoc opts :answer-fn file-test/delivered) judgement)]
    (is (= :published (:status result)))
    (is (= :incomparable (get-in result [:reading-repeat-comparison :status])))
    (is (false? (history/complete? (assoc-in (:reading-inputs measured) [:reader :model]
                                                    (history/absence :not-retained)))))))
(deftest record-drift-refuses-before-contact
  (let [{:keys [opts judgement]} (setup)
        _ (publish-row opts (fail-reading opts judgement))
        _ (spit (io/file (:run-record-dir opts) "tick-run-record-fixture.edn") "{}")
        calls (atom 0)
        result (reading/repair! (assoc opts :answer-fn (fn [_] (swap! calls inc))) judgement)]
    (is (= 0 @calls))
    (is (= :selected-target-reading-history-invalid (:kind result))))
  (is (nil? (:reading-inputs (first (outer/reading-index-entries retained "retained" "pin"))))))

(deftest unchanged-refusal-uses-the-existing-one-reselection-boundary
  (let [calls (atom []) selections (atom 0)
        result (runner/selected-target-reading-step
                {:selected-target-reading-reselect-fn
                 (fn [_ _] (swap! selections inc) (assoc (#'existing/judgement "M-second" "/unused" "pin") :next true))}
                (#'existing/judgement "M-typed-memories" "/unused" "pin")
                (fn [_ j]
                  (swap! calls conj j)
                  {:status :refused :kind :selected-target-reading-unchanged-refusal
                   :target (if (:next j) "second" "first")})
                (fn [_] (throw (ex-info "No published reading" {}))))]
    (is (= 1 @selections))
    ;; Both passes go through the same preparation/repair function.
    (is (= 2 (count @calls)))
    (is (= 2 (count (get-in result [:record :reading-attempts]))))))

(deftest each-target-refusal-survives-and-terminal-consumers-stay-separate
  (let [first-attempt (first (get-in retained [:selected-target-reading :reading-attempts]))
        second-attempt (assoc first-attempt :target "M-second"
                                           :kind :selected-target-reading-validation-refused)
        record (assoc-in retained [:selected-target-reading :reading-attempts]
                         [first-attempt second-attempt first-attempt])
        rows (outer/reading-index-entries record "record" "sha")]
    (is (= ["M-typed-memories" "M-second"] (mapv :target rows)))
    (is (not-any? effective/blocking-row? rows))
    (is (= (outer/index-entry retained "record" "sha")
           (outer/index-entry record "record" "sha")))))

(deftest changed-source-or-contract-is-explicit-and-missing-is-not-equality
  (let [{:keys [opts judgement]} (setup)
        current (:reading-inputs (fail-reading opts judgement))]
    (doseq [field [:source-sha256 :contract-sha256 :reader]]
      (let [prior (assoc current field (if (= field :reader)
                                        (assoc (:reader current) :output-budget 4096) "different"))
            comparison (history/compare-rows current [{:target "M-typed-memories" :reading-inputs prior}])]
        (is (= :inputs-changed (:status comparison)))
        (is (= [field] (get-in comparison [:comparisons 0 :differing-identities])))))
    (is (= :incomparable (:status (history/compare-rows current [{:reading-inputs (dissoc current :reader)}]))))))
