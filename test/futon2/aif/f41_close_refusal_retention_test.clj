(ns futon2.aif.f41-close-refusal-retention-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.wm.cascade-runner-seam-test :as seam]))

(defn- isolated [f]
  ;; The test alias can preload the canonical runner while discovering its
  ;; large fixture namespace. Reload the worktree source in this isolated JVM
  ;; before exercising the changed close boundary.
  (require 'futon2.aif.full-loop-runner :reload)
  (hermetic/with-hermetic-stores #(fixture/with-hermetic-traces f)))

(defn- retained-action-id
  "The action id persist-run-record! retains: the judgment's controller
  decision when the click selected and constructed, else the decision the
  :no-selection sorry carries (this fixture's shape: the click held at the
  open cascade after selection, as click 13 did at cascade-resume)."
  [result]
  (or (get-in result [:checkpoints :selection :judgment
                      :controller-decision :action :id])
      (get-in result [:checkpoints :selection :sorry :decision :action :id])))

(defn- run-record [result]
  (let [rr (:run-record result)]
    (cond (map? rr) rr
          (string? rr) (edn/read-string (slurp rr))
          :else (edn/read-string (slurp (io/file rr))))))

(defn- run-refused-selection [close-refuses?]
  (let [{:keys [root] :as c} (#'fixture/retention-cohort "f41-close-refusal-")
        findings (atom [])
        refused-selection? (atom false)
        append cohort/append-checkpoint!
        close cohort/close-attempt!
        record-finding (fn [finding]
                         (let [record (assoc finding :repair/id "f41-close-refusal")]
                           (swap! findings conj record)
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
                           cohort/close-attempt!
                           (if close-refuses?
                             (fn [& _]
                               (throw (ex-info "required checkpoints missing"
                                               {:failure-kind
                                                :required-checkpoints-missing})))
                             close)]
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
                    {:readings (fn [_] seam/square-readings)}})))
            attempt-dir (io/file root "test-cohort-exhaustion" "attempt-001")
            events (cohort/attempt-events attempt-dir)
            result (:result click)
            action-id (get-in click [:decision :action :id])]
        {:result result :events events :findings @findings :action-id action-id})
      (finally
        (doseq [file (reverse (file-seq (io/file root)))]
          (io/delete-file file true))))))

(deftest close-refusal-retains-selection-and-closes-or-returns
  (isolated
   (fn []
     (testing "missing required checkpoints are represented before close"
       (let [{:keys [result events findings action-id]}
             (run-refused-selection false)
             sorry-checkpoints
             (frequencies
              (for [event events :when (get-in event [:payload :sorry])]
                (:checkpoint/type event)))]
         (is (= action-id (retained-action-id result)))
         ;; click 13 lost exactly this: :selection-event :selection-not-reached
         (is (= {:event :cascade-selected :target "M-current"}
                (select-keys (:selection-event (run-record result))
                             [:event :target])))
         (is (= :build-failed (:outcome result)))
         (is (= :cohort-checkpoint-oversized
                (get-in result [:data :failure-kind])))
         (is (= 1 (count (filter #(= :closed (:checkpoint/type %)) events))))
         (is (= {:construction 1 :dispatch 1 :build 1 :adjudication 1}
                (select-keys sorry-checkpoints
                             [:construction :dispatch :build :adjudication])))
         (is (not-any? #(= :initialization (:failure-stage %)) findings))
         (is (not (.startsWith (str (:attempt-id result)) "initialization-")))))

     (testing "a secondary close refusal is retained rather than escaping"
       (let [{:keys [result action-id]} (run-refused-selection true)]
         (is (= action-id (retained-action-id result)))
         (is (= :cascade-selected
                (get-in (run-record result) [:selection-event :event])))
         (is (= :build-failed (:outcome result)))
         (is (= :cohort-checkpoint-oversized
                (get-in result [:data :failure-kind])))
         (is (nil? (:closed-event result)))
         (is (not (.startsWith (str (:attempt-id result)) "initialization-")))
         (is (= :required-checkpoints-missing
                (get-in result [:data :close-refusal :failure-kind]))))))))
