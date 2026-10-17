(ns futon2.aif.wm.cascade-runner-seam-test
  "Packet D3 focused tests: the production runner seam for a selected
  reading-family cascade, driven through runner/run-opportunity!  One G
  selection, then the mandatory canonical identity verification, append-only
  persistence, attributed compilation, validation and journal open.  With
  incomplete authoring the SAME phase is held as a typed, repairable
  open-cascade condition — never abstention, refusal or no-selection — and
  selection is never rerun."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.policy :as policy]
            
            [futon2.aif.wm.execution-journal :as execution-journal]
            [futon2.aif.wm.executable-cascade :as xc]
            [futon2.aif.wm.open-cascade-resume :as resume]
            [futon2.aif.wm.reading-family-decision-test :as rfd]
            [futon2.test-support.runner-fixture :as fixture])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each
  (fn [run]
    (binding [runner/*wm-status-reporting?* false]
      (with-redefs [ledger/default-root
                    (.getPath (.toFile (Files/createTempDirectory
                                        "seam-ledger-"
                                        (make-array FileAttribute 0))))]
        (run)))))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(def square-fixture
  (edn/read-string (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(defn- sha [c] (apply str (repeat 64 c)))

(def px "p/x")
(def py "p/y")

(defn- hex [x] (evidence/value-digest x))

;; occurrence ids the reading route can produce for a reading of [p/x p/y]:
;; alternatives mode forks per fragment, overlap joins both on fragment 0/1
(def occ-x0 [px 0])
(def occ-y1 [py 1])

(defn- reading-with-square [oid]
  {:reading-record-id (str "M-current:" (pr-str oid))
   :span {:start 0 :end 2 :text "fixture excerpt"}
   :source-text-digest (hex [:excerpt])
   :analysis-digest (hex [:analysis])
   :source-sha256 (sha "a")
   :square square-fixture})

(def square-readings
  {occ-x0 (reading-with-square occ-x0)
   occ-y1 (reading-with-square occ-y1)})

(defn- judgement-for [decision]
  {:decision decision :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace [] :mode :maintain})

(defn- click!
  "One production click whose decision is the REAL cascade-decision over the
  reading family, with the selector counted and the old routes made to
  throw.  SEAM-OPTS reach the runner (cascade roots, readings, authoring);
  DECIDE-OPTS reach cascade-decision (the Packet B opt-in)."
  [read seam-opts decide-opts]
  (let [selector-calls (atom 0)
        fx (@#'rfd/fixture read)
        select policy/select-action-cascades
        decision (:decision (with-redefs
                              [policy/select-action-cascades
                               (fn [& args] (swap! selector-calls inc)
                                 (apply select args))]
                              (#'rfd/decide fx [(get-in fx [:rows :current])]
                                {"M-current" 1} decide-opts)))
        cascades (temp-dir "seam-cascades-")
        journal (temp-dir "seam-journal-")
        ;; this suite runs from an isolated Packet worktree, not the
        ;; canonical checkout the runner's source-drift guard pins to; the
        ;; guard is redef'd away HERE ONLY, with the runner otherwise real
        result (with-redefs [runner/refuse-on-runner-source-drift! (fn [] nil)]
                 (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:judge-fn (fn [_] {:judgement (judgement-for decision)})
                        :selected-reading-ask-fn (fn [request]
                                                  {:request-id (:request-id request)
                                                   :by "stub-reader"
                                                   :verdicts (mapv (fn [o]
                                                                     {:occurrence-id (:occurrence-id o)
                                                                      :pattern (:pattern o)
                                                                      :verdict "accepted"
                                                                      :reason "fits"})
                                                                   (:owed request))})
                        :pattern-library-root (.getPath (temp-dir "seam-library-"))
                        :interpretation-ask-fn (fn [& _] (throw (ex-info "old ask reached" {})))
                        :repair-system-record-fn (fn [m] {:repair/id "repair-seam"
                                                          :repair/class (:repair-class m)})
                        :dispatch-fn (fn [& _] (throw (ex-info "no enactment" {})))
                        :executable-cascade-root (.getPath cascades)
                        :execution-journal-root (.getPath journal)}
                       seam-opts)))]
    {:result result :decision decision ::selector-calls @selector-calls
     :cascades cascades :journal journal :click true}))

(deftest selected-cascade-is-bound-saved-validated-and-held-open
  (let [runtime-error (atom nil)
        {:keys [result decision cascades journal] :as click}
        (click! [px py]
                {:cascade-reading-pins
                 {"M-current" {:excerpt-digest (hex [:excerpt])
                               :analysis-digest (hex [:analysis])}}
                 :cascade-occurrence-readings (fn [_] square-readings)
                 :open-cascade-resume-fn
                 (fn [error]
                   (reset! runtime-error error)
                   (resume/resume! (resume/new-ledger "seam-click")
                                   (resume/recognize error)))}
                {:bind-cascade-identity
                 {:readings (fn [_] square-readings)}})
        cell (get-in result [:checkpoints :selection :sorry])
        held (get-in cell [:held-phase])
        d (:selected-cascade-digest held)
        cd (:cascade-digest held)
        saved-file (io/file cascades (str d ".edn"))
        compiled-file (io/file cascades (str cd ".edn"))]
    (testing "one G selection; the runner never reselects"
      (is (= 1 (::selector-calls click)) "the selector ran exactly once"))
    (testing "the seam held the SAME phase as a typed open-cascade condition"
      (is (map? held))
      (is (= :wm/open-cascade-execution-error (:kind held)))
      (is (= :cascade-enactment-open (:reason held)))
      (is (= :compile (:phase held)))
      (is (true? (:open-cascade held)))
      (is (true? (:repairable held)))
      (is (not= :reading-policy-execution-semantics-owed (:kind held))
          "the legacy execution-semantics refusal is gone")
      (is (not (contains? #{:refused :abstained :no-selection}
                          (get-in result [:run-output :outcome])))))
    (testing "the live carrier conforms to D4 but executable code is not persisted"
      (is (= :held (:status (resume/recognize @runtime-error))))
      (is (ifn? (get-in @runtime-error [:resume-port :retry])))
      (is (not (contains? held :resume-port))))
    (testing "the selected identity and its executable refinement are both bound"
      (is (string? d))
      (is (string? cd))
      (is (= d (get-in decision [:action :cascade-digest]))
          "the pre-selector bind's digest is the seam's digest")
      (is (not= d cd)
          "an executable refinement has its own identity")
      (is (.isFile saved-file) "the canonical cascade was persisted")
      (is (.isFile compiled-file) "the executable refinement was persisted")
      (let [reloaded (xc/load-record saved-file)
            compiled (xc/load-record compiled-file)
            certificate (xc/validate-cascade reloaded)
            compiled-certificate (xc/validate-cascade compiled)]
        (is (= d (:digest reloaded)))
        (is (= d (xc/digest reloaded)) "reload re-digests to the same identity")
        (is (= cd (:digest compiled)))
        (is (= (:structural-digest reloaded) (:structural-digest compiled))
            "refinement preserves the certified structural projection")
        (is (xc/certified? certificate))
        (is (xc/certified? compiled-certificate))))
    (testing "open holes retained with re-entry inputs"
      (is (seq (:holes held)))
      (is (every? :owner (:holes held)))
      (is (= :operational-authoring-supplied
             (get-in held [:re-entry :condition])))
      (let [reloaded (xc/load-record saved-file)]
        (is (every? #(= :open-hole (:enactment-status %))
                    (:executable-nodes reloaded)) "no hole was silently closed")))
    (testing "the journal is opened bound to the cascade identity"
      (let [entries (execution-journal/load-journal journal)]
        (is (seq entries))
        (is (every? #(= cd (:journal-id %)) entries)
            "the journal binds the compiled refinement's identity")
        (is (every? #(= :open-hole (:phase %)) entries)
            "no enactment without a verified effect")))
    (testing "no enactment fired"
      (is (not-any? #(= :enacted (:phase %))
                    (execution-journal/load-journal journal))))
    (testing "the selected cascade is retained on the run record"
      (is (= (get-in decision [:action :id]) (:cascade-id held))))))

(deftest production-default-without-squares-holds-open-honestly
  (let [resume-called? (atom false)
        {:keys [result journal]}
        (click! [px py]
                {:open-cascade-resume-fn
                 (fn [_]
                   (reset! resume-called? true)
                   (throw (ex-info "D4 called for an unrecognized carrier" {})))}
                {})
        cell (get-in result [:checkpoints :selection :sorry])
        held (get-in cell [:held-phase])]
    (testing "no 象 squares on this route yet: citations are typed unread"
      (is (= :cascade-enactment-open (:kind held)))
      (is (seq (:unread-citations held))
          "the unread citations are the exposed re-entry inputs")
      (is (empty? (:holes held)) "no occurrence exists to carry a hole"))
    (testing "no canonical record can certify over zero occurrences yet"
      ;; the honest open condition carries no digest and no journal: the
      ;; re-entry input is the 象 square authoring itself
      (is (nil? (:cascade-digest held)))
      (is (empty? (:journal-append held)))
      (is (empty? (execution-journal/load-journal journal))))
    (testing "the pre-materialisation carrier does not enter D4 resume"
      (is (false? @resume-called?)))
    (testing "and it is still not the legacy refusal"
      (is (not= :reading-policy-execution-semantics-owed (:kind held))))))

(deftest failed-open-cascade-resume-surfaces-the-handler-error
  (let [resume-error {:status :error
                      :kind :open-cascade-review-answer-invalid
                      :phase :cascade-execution
                      :repairable true
                      :review-answer {:status :invalid}}
        {:keys [result]}
        (click! [px py]
                {:cascade-reading-pins
                 {"M-current" {:excerpt-digest (hex [:excerpt])
                               :analysis-digest (hex [:analysis])}}
                 :cascade-occurrence-readings (fn [_] square-readings)
                 :open-cascade-resume-fn (constantly resume-error)}
                {:bind-cascade-identity
                 {:readings (fn [_] square-readings)}})
        cell (get-in result [:checkpoints :selection :sorry])]
    (testing "the resume handler's typed failure replaces the stale open carrier"
      (is (= resume-error (get-in cell [:downstream-refusal])))
      (is (nil? (get-in cell [:held-phase]))))))

(deftest identity-mismatch-is-a-typed-machine-error
  ;; a pre-bound action whose digest does not match the rebuilt chain is a
  ;; :cascade-materialisation-error, never a policy refusal
  (let [{:keys [result]}
        (click! [px py]
                {:cascade-reading-pins
                 {"M-current" {:excerpt-digest (hex [:excerpt])
                               :analysis-digest (hex [:analysis])}}
                 ;; a DIFFERENT span: the seam's rebuild cannot re-derive the
                 ;; digest the decision pre-bound, and must refuse as forged
                 :cascade-occurrence-readings
                 (fn [_] (update-in square-readings [occ-x0 :span :end]
                                    (constantly 9)))}
                {:bind-cascade-identity
                 {:readings (fn [_] square-readings)}})
        cell (get-in result [:checkpoints :selection :sorry])
        refusal (get-in cell [:downstream-refusal])]
    (is (map? refusal))
    (is (= :cascade-materialisation-error (:status refusal)))
    (is (= :identity-forged (:kind refusal)))
    (is (true? (:repairable refusal)))
    (is (not= :reading-policy-execution-semantics-owed (:kind refusal)))))
