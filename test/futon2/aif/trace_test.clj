(ns futon2.aif.trace-test
  "Tests for R8 per-call trace persistence."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.belief :as belief]
            [futon2.aif.observation :as observation]
            [futon2.aif.trace :as trace])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)
           (java.time LocalDate)))

(def ^:dynamic *tmpdir* nil)

(defn- with-tmpdir [f]
  (let [dir (Files/createTempDirectory "wm-trace-test" (into-array FileAttribute []))]
    (binding [*tmpdir* (str dir)]
      (try (f)
           (finally
             (doseq [^File child (reverse (file-seq (io/file (str dir))))]
               (.delete child)))))))

(use-fixtures :each with-tmpdir)

(def ^:private sample-judge-output
  "Minimal judge-style output covering the trace-record fields."
  {:belief (belief/initial-belief-state [:m1])
   :observation {:loop-health 0.7 :stack-pct 0.2}
   :free-energy {:preference-gap-score 0.05 :coverage-uncertainty-pressure 0.10 :controller-score 0.075
                 :per-channel {:loop-health {:value 0.7 :gap 0.0 :in-range? false}}
                 :avoided-active []}
   :variational-free-energy 0.0125
   :ranked-actions [{:action {:type :no-op}
                     :G-risk 0.05 :G-ambiguity 0.0 :structural-pressure 0.0
                     :goal-outcome-replay-inputs
                     {:version 1
                      :c-entries [{:outcome-ref {:id :goal/x}
                                   :status :open :weight {:value 0.4}
                                   :preferred {:op :becomes :value :closed}}]
                      :entry-evaluations
                      [{:outcome-ref {:id :goal/x} :advanced? false
                        :q-sat {:status :present :value 0.0}}]}
                     :controller-score 0.05 :rank 1
                     :preference-stack [{:layer/id :floor :folded? true}]
                     :prediction {:next-observation
                                  {:mean {:loop-health 0.8 :stack-pct 0.3}
                                   :variance {:loop-health 0.02 :stack-pct 0.01}}
                                  :next-belief {:huge :nested}}}
                    {:action {:type :address-sorry :target :sorry/x}
                     :G-risk 0.03 :G-ambiguity 0.015 :structural-pressure 0.7
                     :controller-score 0.045 :rank 2
                     :preference-stack [{:layer/id :floor :folded? true}]
                     :prediction {:next-observation
                                  {:mean {:loop-health 0.6 :stack-pct 0.4}}
                                  :next-belief {:also :stripped}}}]
   :decision {:action {:type :no-op}
              :rank 1 :controller-score 0.05 :tau 0.2
              :softmax-weights {:will-be-stripped :for-trace}}
   :mode :multiplied})

(deftest trace-record-shape-test
  (testing "trace-record extracts all documented fields"
    (let [r (trace/trace-record sample-judge-output)]
      (is (string? (:timestamp r)) "ISO-8601 timestamp")
      (is (contains? r :mu-pre))
      (is (contains? r :mu-post))
      (is (contains? r :observation))
      (is (contains? r :free-energy))
      (is (= 0.0125 (:variational-free-energy r)))
      (is (= trace/r8-producer-contract (:producer-contract r)))
      (is (contains? r :ranked-actions))
      (is (contains? r :decision))
      (is (contains? r :mode)))))

(deftest observation-envelope-distinguishes-absence-from-zero-test
  (testing "the trace derives a lossless envelope from the exact scored observation"
    (let [absent (observation/observe {})
          measured-zero (observation/observe {:loop-health {:overall 0.0}})
          absent-record (trace/trace-record
                         (assoc sample-judge-output :observation absent))
          zero-record (trace/trace-record
                       (assoc sample-judge-output :observation measured-zero))]
      (is (= absent measured-zero)
          "the compatible numeric projections are deliberately equal")
      (is (= :absent
             (get-in absent-record
                     [:observation-envelope :channels :loop-health :variant])))
      (is (= :observed
             (get-in zero-record
                     [:observation-envelope :channels :loop-health :variant])))
      (is (= measured-zero (:observation zero-record))
          "persistence does not alter the object used by scoring"))))

(deftest observation-envelope-write-read-preservation-test
  (let [observed (observation/observe {:loop-health {:overall 0.0}})
        output (assoc sample-judge-output :observation observed)
        expected (:observation-envelope (trace/trace-record output))]
    (trace/write-trace! output :dir *tmpdir* :date-str "2026-08-31")
    (let [[record] (trace/read-trace :dir *tmpdir* :date-str "2026-08-31")]
      (is (= expected (:observation-envelope record)))
      (is (= :observed
             (get-in record
                     [:observation-envelope :channels :loop-health :variant]))))))

(deftest r8-producer-contract-write-read-preservation-test
  (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str "2026-08-31")
  (let [[record] (trace/read-trace :dir *tmpdir* :date-str "2026-08-31")]
    (is (= trace/r8-producer-contract (:producer-contract record)))))

(deftest support-typed-scoring-shadow-is-non-authoritative-test
  (let [ranked [{:action {:type :a} :rank 1 :controller-score 0.0
                 :support-shadow-terms
                 {:by-channel {:loop-health -2.0}
                  :non-channel-contribution 2.0}}
                {:action {:type :b} :rank 2 :controller-score 1.0
                 :support-shadow-terms
                 {:by-channel {:loop-health 2.0}
                  :non-channel-contribution -1.0}}]
        output (assoc sample-judge-output
                      :observation (observation/observe {})
                      :ranked-actions ranked)
        record (trace/trace-record output)
        shadow (:support-typed-scoring-shadow record)]
    (is (= :shadow-only (:authority shadow)))
    (is (= [2.0 -1.0]
           (mapv :support-typed-score (:candidates shadow))))
    (is (true? (get-in shadow [:comparison :winner-changed?])))
    (is (= (dissoc (:decision sample-judge-output) :softmax-weights)
           (:decision record))
        "the counterfactual cannot feed back into the live decision")))

(deftest measured-zero-remains-in-shadow-support-test
  (let [ranked [{:action {:type :a} :rank 1 :controller-score 0.0
                 :support-shadow-terms
                 {:by-channel {:loop-health -2.0}
                  :non-channel-contribution 2.0}}]
        output (assoc sample-judge-output
                      :observation
                      (observation/observe {:loop-health {:overall 0.0}})
                      :ranked-actions ranked)
        candidate (get-in (trace/trace-record output)
                          [:support-typed-scoring-shadow :candidates 0])]
    (is (= [:loop-health] (:support candidate)))
    (is (= 0.0 (:support-typed-score candidate)))
    (is (false? (:would-rank-differently candidate)))))

(deftest trace-record-strips-prediction-field-test
  (testing "ranked-actions in trace drop the heavy :prediction field"
    (let [r (trace/trace-record sample-judge-output)
          rs (:ranked-actions r)]
      (is (every? #(not (contains? % :prediction)) rs)
          "trace ranked-actions don't carry :prediction"))
    (let [r (trace/trace-record sample-judge-output)
          rs (:ranked-actions r)]
      (is (= [0.0 0.7] (mapv :structural-pressure rs))
          "trace preserves the structural-pressure term in ranked-actions"))))

(deftest goal-outcome-replay-inputs-survive-trace-test
  (let [expected (get-in sample-judge-output
                         [:ranked-actions 0 :goal-outcome-replay-inputs])
        actual (get-in (trace/trace-record sample-judge-output)
                       [:ranked-actions 0 :goal-outcome-replay-inputs])]
    (is (= expected actual))
    (is (= {:status :present :value 0.0}
           (get-in actual [:entry-evaluations 0 :q-sat]))
        "zero probability survives as a present value")))

(deftest preference-stack-survives-trace-with-typed-presence-test
  (let [rec (trace/trace-record sample-judge-output)
        evidence (:preference-stack rec)]
    (is (= :present (:status evidence)))
    (is (= :all-ranked-actions (:scope evidence)))
    (is (= 2 (:candidate-count evidence)))
    (is (= [{:layer/id :floor :folded? true}] (:value evidence)))
    (is (every? #(not (contains? % :preference-stack)) (:ranked-actions rec))
        "the identical 2.6KB stack is stored once, not repeated per candidate")))

(deftest preference-stack-empty-is-not-absence-test
  (let [output (assoc sample-judge-output :ranked-actions
                      [{:rank 1 :action {:type :no-op} :preference-stack []}])]
    (is (= {:status :present :scope :all-ranked-actions
            :candidate-count 1 :value []}
           (:preference-stack (trace/trace-record output))))))

(deftest preference-stack-absence-and-conflict-are-loud-test
  (let [missing (assoc sample-judge-output :ranked-actions
                       [{:rank 1 :action {:type :no-op}}])
        conflict (assoc sample-judge-output :ranked-actions
                        [{:rank 1 :preference-stack [{:layer/id :floor}]}
                         {:rank 2 :preference-stack [{:layer/id :habit-prior}]}])]
    (is (= {:status :absent :reason :not-recorded-by-evaluator}
           (:preference-stack (trace/trace-record missing))))
    (is (= :conflict
           (get-in (trace/trace-record conflict) [:preference-stack :status])))))

(deftest preference-stack-write-read-preservation-test
  (let [_ (trace/write-trace! sample-judge-output
                              :dir *tmpdir* :date-str "2026-08-31")
        [record] (trace/read-trace :dir *tmpdir* :date-str "2026-08-31")]
    (is (= (:preference-stack (trace/trace-record sample-judge-output))
           (:preference-stack record)))))

(deftest trace-record-strips-softmax-weights-test
  (testing "decision in trace drops :softmax-weights (non-stringable keys)"
    (let [r (trace/trace-record sample-judge-output)]
      (is (not (contains? (:decision r) :softmax-weights))
          "decision in trace doesn't carry :softmax-weights"))))

(deftest policy-trace-details-flag-off-is-byte-identical-test
  ;; The point of this test is that the DEFAULT record did not change when I3
  ;; landed. An assertion that re-lists the whitelist cannot show that: it
  ;; restates the current implementation, so it passes for any edit made in
  ;; both places. The golden below was captured by running the PRE-I3
  ;; `trace-record` (git 5febaee^) and the current one over the same input in
  ;; one process and confirming the serialized bytes were equal. What is pinned
  ;; here is therefore the pre-I3 output, not a description of today's code.
  (binding [trace/*persist-policy-trace-details?* false]
    (let [golden (str/trim-newline
                  (slurp (io/resource "futon2/aif/trace-flag-off-golden.txt")))
          actual (pr-str (dissoc (trace/trace-record sample-judge-output)
                                 :timestamp))]
      (is (= (count golden) (count actual))
          "flag-off record changed size against the pre-I3 bytes")
      (is (= golden actual)
          "flag-off record is no longer byte-identical to the pre-I3 record"))))

(deftest policy-trace-details-flag-on-roundtrip-test
  (binding [trace/*persist-policy-trace-details?* true]
    (let [weights {{:type :no-op} 0.75
                   {:type :address-sorry :target :sorry/x} 0.25}
          output (assoc-in sample-judge-output [:decision :softmax-weights] weights)
          record (trace/trace-record output)
          encoded (pr-str record)
          decoded (edn/read-string encoded)]
      (is (= {:loop-health 0.8 :stack-pct 0.3}
             (get-in decoded [:ranked-actions 0 :prediction-mean])))
      (is (not (contains? (first (:ranked-actions decoded)) :prediction))
          "the repeated next-belief is not persisted")
      ;; F_pi scores an observation under a DISTRIBUTION; a mean without a
      ;; variance leaves the consumer inventing the precision.
      (is (= {:loop-health 0.02 :stack-pct 0.01}
             (get-in decoded [:ranked-actions 0 :prediction-variance])))
      ;; without the mode on the record, a flat per-candidate field cannot be
      ;; told from a machine that had no discrimination
      (is (contains? decoded :effects-mode))
      (is (= {"rank/1" 0.75 "rank/2" 0.25}
             (get-in decoded [:decision :softmax-weights-by-candidate-id])))
      (is (every? string?
                  (keys (get-in decoded
                                [:decision :softmax-weights-by-candidate-id])))))))

(deftest trace-record-pure-test
  (testing "trace-record is pure (modulo timestamp): same input → same shape"
    (let [r1 (trace/trace-record sample-judge-output)
          r2 (trace/trace-record sample-judge-output)]
      (is (= (dissoc r1 :timestamp) (dissoc r2 :timestamp))))))

(deftest write-trace-creates-file-test
  (testing "write-trace! creates the daily file under the given dir"
    (let [path (trace/write-trace! sample-judge-output
                                   :dir *tmpdir*
                                   :date-str "2026-05-17")]
      (is (str/ends-with? path "wm-trace-2026-05-17.edn"))
      (is (.exists (io/file path))))))

(deftest write-trace-appends-test
  (testing "two writes produce two records in the file"
    (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str "2026-05-17")
    (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str "2026-05-17")
    (let [records (trace/read-trace :dir *tmpdir* :date-str "2026-05-17")]
      (is (= 2 (count records))))))

(deftest read-trace-roundtrip-test
  (testing "write then read returns the same records (modulo timestamp)"
    (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str "2026-05-17")
    (let [[r] (trace/read-trace :dir *tmpdir* :date-str "2026-05-17")]
      (is (= (:observation sample-judge-output) (:observation r)))
      (is (= (:mode sample-judge-output) (:mode r)))
      (is (= 2 (count (:ranked-actions r)))
          "both ranked actions preserved"))))

(deftest read-trace-missing-file-returns-empty-test
  (testing "read-trace on a non-existent file returns empty vec"
    (is (= [] (trace/read-trace :dir *tmpdir* :date-str "1999-01-01")))))

(deftest read-trace-records-are-clojure-types-test
  (testing "edn-lines preserve keyword keys and clojure-native types on read"
    (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str "2026-05-17")
    (let [[r] (trace/read-trace :dir *tmpdir* :date-str "2026-05-17")]
      (is (keyword? (:mode r)))
      (is (= :multiplied (:mode r)))
      (is (map? (:observation r))))))

(deftest trace-record-propagates-selection-gain-test
  (testing "R14 γ-state propagates through trace-record from judge output"
    (let [gain-state {:selection-gain 1.6 :error-history [0.2 0.1]
                       :mean-error 0.15 :samples 7}
          r (trace/trace-record (assoc sample-judge-output
                                       :selection-gain gain-state))]
      (is (= gain-state (:selection-gain r)))))
  (testing "absent γ-state reconstructs the prior (γ=1.0), never nil"
    (let [r (trace/trace-record sample-judge-output)]
      (is (= 1.0 (get-in r [:selection-gain :selection-gain]))
          "trace always carries a usable γ-state for the next tick's read-back"))))

(deftest selection-gain-roundtrips-through-trace-test
  (testing "γ-state survives write → read so the next tick continues the window"
    (let [gain-state {:selection-gain 0.75 :error-history [0.6 0.7 0.65]
                       :mean-error 0.65 :samples 12}
          out (assoc sample-judge-output :selection-gain gain-state)]
      (trace/write-trace! out :dir *tmpdir* :date-str "2026-06-26")
      (let [record (trace/latest-trace-record :dir *tmpdir*
                                              :end-date (LocalDate/parse "2026-06-26")
                                              :lookback-days 1)]
        (is (= gain-state (:selection-gain record)))))))

(deftest realized-outcome-present-only-passthrough-test
  (testing "R16 :realized-outcome is propagated when the enactor supplies it"
    (let [outcome {:policy :p/x :expected-score 0.2 :realized-score 0.05 :tick 41}
          r (trace/trace-record (assoc sample-judge-output :realized-outcome outcome))]
      (is (= outcome (:realized-outcome r)))))
  (testing "absent today (enactment not live-wired) ⇒ key not present (not nil)"
    (let [r (trace/trace-record sample-judge-output)]
      (is (not (contains? r :realized-outcome))
          "present-only: no noisy nil seam in ordinary records"))))

(deftest latest-trace-record-spans-midnight-utc-test
  (testing "latest-trace-record falls back to yesterday when today's bucket is empty"
    (let [yesterday-output (assoc sample-judge-output
                                  :precision-state {:annotation-health
                                                    {:precision 42.0
                                                     :error-history [0.1 0.2]}})]
      (trace/write-trace! yesterday-output :dir *tmpdir* :date-str "2026-05-17")
      (let [record (trace/latest-trace-record :dir *tmpdir*
                                              :end-date (LocalDate/parse "2026-05-18")
                                              :lookback-days 2)]
        (is (= 42.0 (get-in record [:precision-state :annotation-health :precision])))
        (is (= [0.1 0.2]
               (get-in record [:precision-state :annotation-health :error-history])))))))

(deftest latest-trace-record-survives-a-long-pause-test
  (let [old-output (assoc sample-judge-output :mode :oldest)
        newest-output (assoc sample-judge-output :mode :newest)]
    (trace/write-trace! old-output :dir *tmpdir* :date-str "2026-05-01")
    (trace/write-trace! newest-output :dir *tmpdir* :date-str "2026-05-03")
    (is (= :newest
           (:mode (trace/latest-trace-record
                   :dir *tmpdir* :end-date (LocalDate/parse "2026-06-01")
                   :lookback-days 2))))
    (is (= 2 (trace/reduce-traces (fn [n _] (inc n)) 0 :dir *tmpdir*)))))

(deftest recent-trace-records-reads-newest-files-to-a-record-bound
  (spit (io/file *tmpdir* "wm-trace-2026-07-20.edn")
        (str (pr-str {:id 1}) "\n" (pr-str {:id 2}) "\n"))
  (spit (io/file *tmpdir* "wm-trace-2026-07-21.edn")
        (str (pr-str {:id 3}) "\n" (pr-str {:id 4}) "\n"))
  (is (= [2 3 4]
         (mapv :id (trace/recent-trace-records 3 :dir *tmpdir*))))
  (is (= [] (trace/recent-trace-records 0 :dir *tmpdir*))))

;; ---------------------------------------------------------------------------
;; M-evaluate-policies D1a (2026-07-03) — whitelist covers the blend's terms
;; ---------------------------------------------------------------------------

(deftest strip-ranked-action-whitelist-test
  (testing "I4: every term entering :controller-score survives the trace strip"
    (let [entry {:action {:type :no-op}
                 :G-risk 1.0 :G-ambiguity 2.0 :predictability-bonus 0.1 :homeostatic-pressure 0.2
                 :structural-pressure 0.3 :G-goal-outcome 0.4
                 :gap-exploration-bonus 0.5 :graph-control-score 0.6 :G-core 3.0
                 :g-ambiguity-source :beta-predictive
                 :c-zone-load {:class :survey :mass 4.0 :load-weight 0.75}
                 :risk-mode :kl :ambiguity-mode :gaussian-entropy
                 :predictability-control-mode :telemetry-only
                 :homeostatic-control-mode :telemetry-only
                 :graph-feasibility-mode :policy-support
                 :controller-score 7.1 :rank 1
                 :prediction {:dropme true}}
          rec (trace/trace-record {:belief {} :observation {} :free-energy {}
                                   :ranked-actions [entry]
                                   :decision {:action :abstain} :mode :test})
          kept (first (:ranked-actions rec))]
      (doseq [k [:gap-exploration-bonus :graph-control-score :G-core :G-goal-outcome :controller-score]]
        (is (contains? kept k) (str k " must survive the strip")))
      (is (= :gaussian-entropy (:ambiguity-mode kept))
          "ambiguity-mode provenance survives the strip")
      (is (= :beta-predictive (:g-ambiguity-source kept))
          "learn-action ambiguity provenance survives the strip")
      (is (= {:class :survey :mass 4.0 :load-weight 0.75}
             (:c-zone-load kept))
          "the named empirical C channel survives the strip")
      (is (= :telemetry-only (:predictability-control-mode kept)))
      (is (= :telemetry-only (:homeostatic-control-mode kept)))
      (is (= :policy-support (:graph-feasibility-mode kept)))
      (is (not (contains? kept :prediction)) "the deep :prediction still drops"))))

(deftest policy-support-exclusions-survive-trace-test
  (let [exclusions [{:action {:type :open-mission :target "M-off-map"}
                     :reason :mission-absent-from-capability-graph}]
        rec (trace/trace-record
             (assoc sample-judge-output :policy-support-exclusions exclusions))]
    (is (= exclusions (:policy-support-exclusions rec))
        "the domain restriction is inspectable without replaying the scorer")))

;; ---------------------------------------------------------------------------
;; B-0a (M-aif-faithfulness §2.0) — tick provenance stamp
;; ---------------------------------------------------------------------------

(def ^:private sample-resolved-flags
  "A resolved mode/flag set as the scheduled runner assembles it (the arena
   fns + the live-wire switch); values here are fixtures, not env reads."
  {:risk-mode :kl
   :ambiguity-mode :gaussian-entropy
   :goal-outcome-mode :kl
   :kl-channel-weights {}
   :c-temperature 0.1
   :live-wire? true})

(deftest wm-version-stamp-shape-test
  (testing "stamp = git identity + resolved flags + schema version"
    (let [stamp (trace/wm-version-stamp sample-resolved-flags)]
      (is (or (= :unknown (:git-sha stamp))
              (and (string? (:git-sha stamp))
                   (re-matches #"[0-9a-f]{40}" (:git-sha stamp))))
          "full 40-char sha (or :unknown when git is unavailable)")
      (is (contains? stamp :git-dirty?))
      (is (= trace/trace-schema-version (:trace-schema-version stamp))
          "the record-shape version rides inside the stamp")
      (is (= :kl (:risk-mode stamp)))
      (is (true? (:live-wire? stamp))
          "caller-resolved flags pass through unmodified"))))

(deftest wm-version-roundtrips-through-trace-test
  (testing "acceptance: (wm-version-of tick) recovers sha+flags from a record"
    (let [stamp (trace/wm-version-stamp sample-resolved-flags)
          out (assoc sample-judge-output :wm-version stamp)]
      (trace/write-trace! out :dir *tmpdir* :date-str "2026-07-04")
      (let [[r] (trace/read-trace :dir *tmpdir* :date-str "2026-07-04")
            v (trace/wm-version-of r)]
        (is (= stamp v) "the stamp survives write → read intact")
        (is (some? (:git-sha v)) "which code — answerable from the record")
        (is (= :kl (:risk-mode v)) "which config — answerable from the record")))))

(deftest wm-version-absent-when-not-stamped-test
  (testing "purely additive: un-stamped records don't grow a nil :wm-version"
    (let [r (trace/trace-record sample-judge-output)]
      (is (not (contains? r :wm-version))
          "present-only, so bare judge calls and old records are unchanged")
      (is (nil? (trace/wm-version-of r))
          "the accessor answers nil, not a throw, for pre-B-0a records"))))

(deftest older-trace-fields-have-typed-version-skew-absence-test
  (let [v14 {:wm-version {:trace-schema-version 14}
             :ranked-actions [{}]}
        unversioned {:ranked-actions [{}]}]
    (doseq [record [v14 unversioned]
            field (keys trace/trace-evidence-fields)]
      (is (= :predates-field
             (:reason (trace/trace-field-evidence record field)))
          (str field " must be legacy absence, never a default")))
    (is (= {:status :present :value 0.0 :record-schema-version 17}
           (trace/trace-field-evidence
            {:wm-version {:trace-schema-version 17}
             :observation-envelope 0.0}
            :observation-envelope))
        "an explicit zero remains present")
    (is (= :malformed
           (:reason (trace/trace-field-evidence
                     {:wm-version {:trace-schema-version 20}}
                     :observation-envelope)))
        "a current contract cannot enter the permissive legacy arm")))
