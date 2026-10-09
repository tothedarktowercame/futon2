(ns futon2.aif.previous-run-test
  "Q6 carry and enforcement. Unit tests over the typed lookup/carrier
  (ordered by recorded start time, not filename), the exclusion decision,
  and the ranked-pair filter, plus end-to-end runner tests: a tick whose
  previous run refused with an unchanged selection-input digest re-decides
  with that (target, cascade) pair excluded; only a click with no
  admissible alternative refuses."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.policy :as policy]
            [futon2.aif.previous-run :as previous-run]
            [futon2.test-support.runner-fixture :as trace-fixture]))

(use-fixtures :once hermetic/with-hermetic-stores trace-fixture/with-hermetic-traces)
(use-fixtures :each
  (fn [f]
    ;; The drift check runs on the close path too (off the test thread, where
    ;; a with-redefs-fn binding does not reach), and this worktree's runner
    ;; legitimately differs from the canonical checkout it will be merged
    ;; into. Stub the check for the whole var, restoring afterwards.
    (let [original #'runner/refuse-on-runner-source-drift!]
      (alter-var-root original (constantly (constantly {:test-only true})))
      (try
        (binding [runner/*wm-status-reporting?* false] (f))
        (finally
          (alter-var-root original (constantly @original)))))))

(defn- write-record! [dir name record]
  (let [f (io/file dir name)]
    (spit f (str (pr-str record) "\n"))
    f))

(defn- refusal-carrier
  ([digest choice]
   {:schema :wm/previous-run-v1 :status :present :run/id "prev"
    :choice (merge {:status :present} choice)
    :outcome {:status :present :outcome :refused}
    :input-digest {:status :present :digest digest}}))

(defn- tmp-dir []
  (let [d (io/file (System/getProperty "java.io.tmpdir")
                   (str "wm-prev-run-test-" (random-uuid)))]
    (.mkdirs d)
    d))

(deftest lookup-is-typed-at-every-absence
  (let [tmp (tmp-dir)
        absent (previous-run/lookup tmp "2026-10-09-bbbb")]
    (is (= :absent (:status absent)))
    (is (= :previous-run-record-absent (:reason absent)))
    (let [file (write-record! tmp "tick-run-record-2026-10-08-aaaa.edn"
                              {:run/id "2026-10-08-aaaa"
                               :startedAt "2026-10-08T10:00:00Z"
                               :decision {:chosen {:target "M-t"
                                                   :precedence [:p1]}}
                               :terminal-receipt {:outcome :guardrail-refusal}
                               :world-at-selection
                               {:selection-input-digest "d0"}})
          carrier (previous-run/lookup tmp "2026-10-09-bbbb")]
      (is (= :present (:status carrier)))
      (is (= "2026-10-08-aaaa" (:run/id carrier)))
      (is (= {:status :present :target "M-t" :precedence [:p1]} (:choice carrier)))
      (is (= {:status :present :outcome :guardrail-refusal} (:outcome carrier)))
      (is (= {:status :present :digest "d0"} (:input-digest carrier)))
      ;; the current run's own record is never its previous run
      (write-record! tmp "tick-run-record-2026-10-09-bbbb.edn"
                     {:run/id "2026-10-09-bbbb"})
      (is (= "2026-10-08-aaaa" (:run/id (previous-run/lookup tmp "2026-10-09-bbbb"))))
      ;; a previous run that chose nothing carries typed absences
      (write-record! tmp "tick-run-record-2026-10-09-cccc.edn"
                     {:run/id "2026-10-09-cccc" :startedAt "2026-10-09T08:00:00Z"
                      :decision {}})
      (let [empty (previous-run/lookup tmp "2026-10-09-dddd")]
        (is (= :present (:status empty)))
        (is (= :absent (get-in empty [:choice :status])))
        (is (= :absent (get-in empty [:outcome :status])))
        (is (= :absent (get-in empty [:input-digest :status])))))))

(deftest lookup-orders-by-recorded-start-time-not-filename
  ;; Same-day run ids are random uuids: bbbb sorts after aaaa but STARTED
  ;; earlier, so the previous run of dddd is aaaa.
  (let [tmp (tmp-dir)]
    (write-record! tmp "tick-run-record-2026-10-09-bbbb.edn"
                   {:run/id "2026-10-09-bbbb"
                    :startedAt "2026-10-09T09:00:00Z"
                    :decision {:chosen {:target "M-early" :precedence [:p]}}
                    :terminal-receipt {:outcome :changed}
                    :world-at-selection {:selection-input-digest "d-early"}})
    (write-record! tmp "tick-run-record-2026-10-09-aaaa.edn"
                   {:run/id "2026-10-09-aaaa"
                    :startedAt "2026-10-09T15:00:00Z"
                    :decision {:chosen {:target "M-late" :precedence [:p]}}
                    :terminal-receipt {:outcome :changed}
                    :world-at-selection {:selection-input-digest "d-late"}})
    (let [carrier (previous-run/lookup tmp "2026-10-09-dddd")]
      (is (= "2026-10-09-aaaa" (:run/id carrier)))
      (is (= "M-late" (get-in carrier [:choice :target])))
      (is (= "d-late" (get-in carrier [:input-digest :digest]))))
    ;; a record with no :startedAt never started and is skipped
    (write-record! tmp "tick-run-record-2026-10-09-cXXX.edn" {:run/id "x"})
    (is (= "2026-10-09-aaaa"
           (:run/id (previous-run/lookup tmp "2026-10-09-dddd"))))))

(deftest exclusion-decision-fires-only-on-the-q6-conjunction
  (let [choice {:target "M-t" :precedence [:p1]}
        digest "d1"]
    (is (map? (previous-run/exclusion-decision
               (refusal-carrier digest choice)
               {:target "M-t" :precedence [:p1]} digest)))
    (is (= :q6-repeat-after-refusal
           (:excluded-by (previous-run/exclusion-decision
                          (refusal-carrier digest choice)
                          {:target "M-t" :precedence [:p1]} digest))))
    ;; a different cascade is allowed
    (is (nil? (previous-run/exclusion-decision
               (refusal-carrier digest choice)
               {:target "M-t" :precedence [:p2]} digest)))
    ;; a different target is allowed
    (is (nil? (previous-run/exclusion-decision
               (refusal-carrier digest choice)
               {:target "M-u" :precedence [:p1]} digest)))
    ;; a changed selection-input digest is allowed
    (is (nil? (previous-run/exclusion-decision
               (refusal-carrier digest choice)
               {:target "M-t" :precedence [:p1]} "d2")))
    ;; a previous run that did not refuse is allowed
    (is (nil? (previous-run/exclusion-decision
               (assoc-in (refusal-carrier digest choice)
                         [:outcome :outcome] :changed)
               {:target "M-t" :precedence [:p1]} digest)))
    ;; a previous run whose digest is absent is allowed
    (is (nil? (previous-run/exclusion-decision
               (assoc (refusal-carrier digest choice)
                      :input-digest {:status :absent :reason :x})
               {:target "M-t" :precedence [:p1]} digest)))
    ;; no previous run at all is allowed
    (is (nil? (previous-run/exclusion-decision
               {:schema :wm/previous-run-v1 :status :absent
                :reason :previous-run-record-absent}
               {:target "M-t" :precedence [:p1]} digest)))))

(deftest excluded-ranked-splits-around-the-bound-pair
  (let [ranked [{:action {:target "M-t" :precedence [:p1]} :rank 1}
                {:action {:target "M-t" :precedence [{:id :p1}]} :rank 2}
                {:action {:target "M-u" :precedence [:p2]} :rank 3}]]
    ;; unbound: everything is admissible
    (is (= 3 (count (:kept (previous-run/excluded-ranked ranked)))))
    (binding [previous-run/*excluded-pair*
              {:target "M-t" :precedence [:p1]}]
      (let [q6 (previous-run/excluded-ranked ranked)]
        (is (= 1 (count (:kept q6))))
        (is (= "M-u" (get-in q6 [:kept 0 :action :target])))
        ;; pattern maps and ids name the same cascade
        (is (= 2 (count (:excluded q6))))
        (is (= :q6-repeat-after-refusal
               (:excluded-by {:excluded-by :q6-repeat-after-refusal})))))))

(def ^:private selected-action
  {:kind :cascade-candidate :cascade-id :test/selected :id :test/selected
   :target "M-selected" :precedence [:test/selected-pattern]
   :construction-receipt {:cascade/id :test/selected :moves 1
                          :family-searched :unit :coverage 1}
   :interpretation-receipts [{:pattern :test/selected-pattern
                              :admitted-by :test-suite}]})

(def ^:private alternative-action
  (assoc selected-action :cascade-id "M-rank-head" :id "M-rank-head"
         :precedence [:test/other-pattern]))

(defn- q6-aware-judge
  "A judge like the live one: ranked candidates, filtered around a bound
  previous-run/*excluded-pair* (exactly what cascade-decision R14 and
  family-selection do), typed refusal when nothing admissible remains."
  [ranked]
  (fn [_]
    (let [q6 (previous-run/excluded-ranked ranked)]
      (when (empty? (:kept q6))
        (throw (ex-info "no admissible cascade candidate"
                        {:kind :no-acting-cascade-candidate
                         :excluded (count (:excluded q6))})))
      {:judgement
       {:decision (cond-> (policy/select-action-cascades
                           (:kept q6) {:beta 2.0})
                    (seq (:excluded q6))
                    (assoc :q6-exclusion
                           {:excluded-by :q6-repeat-after-refusal
                            :pair (:pair q6)
                            :excluded-count (count (:excluded q6))}))
        :belief {} :belief-pre {} :observation {} :free-energy {}
        :prediction-errors {} :precision-state {} :micro-step-trace []
        :mode :maintain}})))

(def ^:private two-candidate-ranked
  [{:action selected-action :controller-score -2.0 :rank 1}
   {:action alternative-action :controller-score -1.0 :rank 2}])

(defn- selection-run
  "One hermetic tick. PREVIOUS-RUN-FN overrides the typed lookup; JUDGE
  overrides the judge (defaults to the two-candidate Q6-aware judge)."
  ([] (selection-run nil nil))
  ([previous-run-fn] (selection-run previous-run-fn nil))
  ([previous-run-fn judge]
   (let [findings (atom [])
         result (runner/run-opportunity!
                 (merge (fixture/isolated-runner-opts)
                        {:judge-fn (or judge (q6-aware-judge two-candidate-ranked))
                         :repair-open-fn (constantly [])
                         :repair-system-record-fn
                         (fn [m] (swap! findings conj m)
                           {:repair/id (str "repair-" (count @findings))})
                         :dispatch-fn (fn [& _]
                                        (throw (ex-info "stop after selection"
                                                        {:kind :test/stop})))}
                        (when previous-run-fn
                          {:previous-run-fn previous-run-fn})))]
     {:result result
      :record (edn/read-string (slurp (:run-record result)))})))

(deftest run-record-carries-previous-run
  (let [carrier (refusal-carrier "d-old" {:target "M-old" :precedence [:old]})
        {:keys [record]} (selection-run (constantly carrier))]
    (is (= carrier (:previous-run record)))
    (is (nil? (:q6-exclusion record)) "no exclusion without a Q6 trigger")))

(deftest q6-repeat-excludes-the-pair-and-chooses-the-alternative
  (let [{:keys [record]} (selection-run)
        digest (get-in record [:world-at-selection :selection-input-digest])
        chosen (get-in record [:decision :chosen])]
    (is (string? digest) "fixture run must carry a selection-input digest")
    (is (= "M-selected" (:target chosen)) "the natural choice is the rank-1 pair")
    ;; previous refusal, unchanged digest, same (target, cascade): the pair
    ;; is excluded and selection re-decides — the alternative is chosen and
    ;; the exclusion is recorded as typed evidence, not a wasted refusal.
    (let [{:keys [record result]}
          (selection-run (constantly (refusal-carrier
                                      digest
                                      {:target "M-selected"
                                       :precedence [:test/selected-pattern]})))]
      (is (not= :repeat-choice-after-refusal
                (get-in record [:failure :kind])))
      ;; the alternative cascade on the same target is chosen
      (is (= "M-rank-head" (get-in record [:decision :chosen :id])))
      (is (= [:test/other-pattern]
             (get-in record [:decision :chosen :precedence])))
      (is (= :q6-repeat-after-refusal
             (get-in record [:q6-exclusion :excluded-by])))
      (is (= "M-selected" (get-in record [:q6-exclusion :pair :target])))
    ;; changed digest: the same choice stands
    (let [{:keys [record]}
          (selection-run (constantly (refusal-carrier
                                      (str digest "-changed")
                                      {:target "M-selected"
                                       :precedence [:test/selected-pattern]})))]
      (is (= [:test/selected-pattern]
             (get-in record [:decision :chosen :precedence])))
      (is (nil? (:q6-exclusion record)))))))

(deftest q6-repeat-with-no-alternative-refuses-typed
  (let [single (q6-aware-judge [{:action selected-action
                                 :controller-score -2.0 :rank 1}])
        {:keys [record]} (selection-run nil single)
        digest (get-in record [:world-at-selection :selection-input-digest])]
    (is (string? digest))
    (let [{:keys [record]}
          (selection-run (constantly (refusal-carrier
                                      digest
                                      {:target "M-selected"
                                       :precedence [:test/selected-pattern]}))
                         single)]
      (is (= :repeat-choice-after-refusal
             (get-in record [:failure :kind])))
      (is (= :refused (get-in record [:failure :detail :outcome]))))))
