(ns futon2.aif.wm.injury-94091-acceptance-test
  "Acceptance pin for injury/94091b05988598abb17ce238088862c5d7f47201.

  A selected reading-family cascade with incomplete authoring holds the
  SAME phase as a typed, repairable open-cascade condition.  When the
  production resume handler at :cascade-resume returns a typed :error
  (here :answer-holes-unresolved, exactly the injury's kind, with no :phase,
  mirroring open-cascade-resume/validate-answer output), the runner must
  NOT throw the abstention message: that relabels a machine error as
  abstention, records a spurious repair obligation, and drops the selection
  checkpoint.  open_cascade_resume.clj's contract forbids exactly this.

  Uses the real production functions from the diagnosis:
  futon2.aif.full-loop-runner/run-opportunity!,
  futon2.aif.wm.loop-node-exercise/require-complete!, and
  futon2.aif.terminal-publication-receipt/publish!, plus the shared isolated
  test fixtures.  No production seam is stubbed: the injected
  :open-cascade-resume-fn is a typed failure carrier, the same injection
  pattern as cascade_runner_seam_test.clj."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.policy :as policy]
            [futon2.aif.terminal-publication-receipt :as terminal-publication]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]
            [futon2.aif.wm.reading-family-decision-test :as rfd]
            [futon2.test-support.runner-fixture :as fixture])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)

(use-fixtures :each
  (fn [run-tests]
    (binding [runner/*wm-status-reporting?* false]
      (with-redefs [ledger/default-root
                    (.getPath (.toFile (Files/createTempDirectory
                                        "injury-94091-ledger-"
                                        (make-array FileAttribute 0))))]
        (run-tests)))))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(def square-fixture
  (edn/read-string (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(def px "p/x")

(def py "p/y")

(defn- hex [x] (evidence/value-digest x))

(def occ-x0 [px 0])

(def occ-y1 [py 1])

(defn- reading-with-square [oid]
  {:reading-record-id (str "M-current:" (pr-str oid))
   :span {:start 0 :end 2 :text "fixture excerpt"}
   :source-text-digest (hex [:excerpt])
   :analysis-digest (hex [:analysis])
   :source-sha256 (apply str (repeat 64 "a"))
   :square square-fixture})

(def square-readings
  {occ-x0 (reading-with-square occ-x0)
   occ-y1 (reading-with-square occ-y1)})

(defn- judgement-for [decision]
  {:decision decision :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace [] :mode :maintain})

(def resume-error
  "The typed production failure the injury pinned: validate-answer's
  :answer-holes-unresolved rejection, carried with no :phase (so a naive
  classifier reads :held-phase nil) and :repair detail preserved whole."
  {:status :error
   :kind :answer-holes-unresolved
   :repair {:requisitioned [(pr-str occ-x0) (pr-str occ-y1)]
            :answered []}})

(deftest cascade-resume-typed-error-is-not-an-abstention
  (let [selector-calls (atom 0)
        repairs (atom [])
        fx (@#'rfd/fixture [px py])
        select policy/select-action-cascades
        decision (:decision (with-redefs
                              [policy/select-action-cascades
                               (fn [& args]
                                 (swap! selector-calls inc)
                                 (apply select args))]
                              (#'rfd/decide fx [(get-in fx [:rows :current])]
                                           {"M-current" 1}
                                           {:bind-cascade-identity
                                            {:readings (fn [_] square-readings)}})))
        cascades (temp-dir "injury-94091-cascades-")
        journal (temp-dir "injury-94091-journal-")
        run-record-dir (temp-dir "injury-94091-records-")
        injury-root (temp-dir "injury-94091-injuries-")
        injury-ledger (io/file injury-root "ledger.edn")
        result (with-redefs [runner/refuse-on-runner-source-drift! (fn [] nil)]
                 (runner/run-opportunity!
                  (merge (fixture/isolated-runner-opts)
                         {:run-id "injury-94091-acceptance"
                          :run-record-dir (.getPath run-record-dir)
                          :meta-injury-root (.getPath injury-root)
                          :meta-injury-ledger-path (.getPath injury-ledger)
                          :judge-fn (fn [_] {:judgement (judgement-for decision)})
                          :selected-reading-ask-fn
                          (fn [request]
                            {:request-id (:request-id request)
                             :by "stub-reader"
                             :verdicts (mapv (fn [o]
                                               {:occurrence-id (:occurrence-id o)
                                                :pattern (:pattern o)
                                                :verdict "accepted"
                                                :reason "fits"})
                                             (:owed request))})
                          :interpretation-ask-fn (fn [& _] (throw (ex-info "old ask reached" {})))
                          :repair-system-record-fn (fn [m]
                                                     (swap! repairs conj m)
                                                     {:repair/id "repair-injury-94091"
                                                      :repair/class (:repair-class m)})
                          :dispatch-fn (fn [& _] (throw (ex-info "no enactment" {})))
                          :pattern-library-root (.getPath (temp-dir "injury-94091-library-"))
                          :executable-cascade-root (.getPath cascades)
                          :execution-journal-root (.getPath journal)
                          :cascade-reading-pins
                          {"M-current" {:excerpt-digest (hex [:excerpt])
                                        :analysis-digest (hex [:analysis])}}
                          :cascade-occurrence-readings (fn [_] square-readings)
                          :open-cascade-resume-fn (constantly resume-error)})))
        cell (get-in result [:checkpoints :selection])
        sorry (:sorry cell)
        retained-decision (get-in cell [:judgment :decision])]
    (testing "the resume handler's typed error is never relabeled abstention"
      (is (nil? (get-in result [:data :error]))
          "no abstention error text on the result")
      (is (empty? @repairs)
          "a typed cascade-resume handler error is not a system failure; no repair obligation is recorded"))
    (testing "the selection checkpoint survives with the typed condition retained"
      (is (map? cell) "the selection checkpoint is retained on the result")
      (is (= :answer-holes-unresolved (get-in sorry [:downstream-refusal :kind])))
      (is (get-in sorry [:held-phase :open-cascade])
          "the open-cascade held condition stays held, not converted to no-selection")
      (is (true? (get-in sorry [:held-phase :repairable]))))
    (testing "one G selection; the resume did not reselect"
      (is (= 1 @selector-calls)))
    (testing "the required loop-node-evaluator receipt is retained on the held decision"
      (is (map? retained-decision))
      (let [annotated (loop-node-exercise/require-complete! retained-decision)]
        (is (= (get-in annotated [:selection-certificate :loop-node-exercise])
               (get-in retained-decision [:selection-certificate :loop-node-exercise]))
            "the checkpoint decision already carries the measured node exercise")))
    (testing "terminal routing still publishes a receipt for the persisted run record"
      (let [record-path (get-in result [:run-record])]
        (is (and (string? record-path) (.isFile (io/file record-path)))
            "the run record was persisted for terminal routing")
        (let [sink-hit (atom [])
              receipt (terminal-publication/publish!
                       record-path
                       (:run/id result)
                       [{:sink :meta-injury-deposit
                         :thunk (fn []
                                  (swap! sink-hit conj :meta-injury-deposit)
                                  {:schema :wm/meta-injury-deposit-v1
                                   :status :deposited
                                   :run-id (:run/id result)})}])]
          (is (= :present (:status receipt))
              "publish! reports the receipt as present")
          (is (= [:meta-injury-deposit] @sink-hit)
              "each sink ran exactly once")
          (is (= :deposited
                 (get-in (terminal-publication/sink-result receipt :meta-injury-deposit)
                         [:status]))))))))
