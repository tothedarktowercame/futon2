(ns futon2.aif.trace-test
  "Tests for R8 per-call trace persistence."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
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

(def ^:private pre-beta-dark-trace-sha
  "The last commit that touched src/futon2/aif/trace.clj before the RUN7 dark
   beta field was added.

   PINNED, NOT HEAD~ (the RUN5 review finding, 2026-09-01): a moving anchor
   rots. After the next edit to this file HEAD~ already CONTAINS the field, so
   the control compares the flag-off path with itself and the claim it backs
   turns quietly from `the default record is unchanged by dark beta` into `the
   last commit changed nothing`. Both are checks; only the first is the claim.
   A control for a fixed claim gets a fixed anchor."
  "183749a")

(defn- record-at
  "Load a past revision's trace implementation under a parallel namespace and
   build a record with it. This is the cross-version control: it does not
   restate the current strip logic."
  [sha ns-suffix judge-output]
  (let [{:keys [exit out err]}
        (shell/sh "git" "show" (str sha ":src/futon2/aif/trace.clj"))]
    (when-not (zero? exit)
      (throw (ex-info "could not load previous trace implementation"
                      {:sha sha :err err})))
    (load-string
     (str/replace-first out
                        "(ns futon2.aif.trace"
                        (str "(ns futon2.aif.trace-" ns-suffix)))
    ;; The parallel namespace has its OWN copy of the policy-detail var, which
    ;; reads the env at load. Without this the "flag on" comparison would put a
    ;; details-on record beside a details-off one and fail for a reason that has
    ;; nothing to do with the change under test.
    (let [old-ns (symbol (str "futon2.aif.trace-" ns-suffix))]
      (with-bindings {(ns-resolve old-ns '*persist-policy-trace-details?*)
                      trace/*persist-policy-trace-details?*}
        ((ns-resolve old-ns 'trace-record) judge-output)))))

(defn- previous-trace-record [judge-output]
  (record-at pre-beta-dark-trace-sha "previous" judge-output))

(def ^:private i5-retired-keys
  "The two keys whose difference from the pinned historical anchors below is
   DELIBERATE and belongs to I5 slice (c) rather than to any flag: the retired
   scalar F, and the producer contract that declares its retirement.

   The anchors are NOT re-pinned to HEAD. Re-pinning would turn each control's
   claim from `this flag adds nothing to the record` into `the last commit
   changed nothing` -- exactly the rot `pre-beta-dark-trace-sha` was pinned to
   avoid. So the anchors stay and the comparison drops these two keys from both
   sides, which keeps every remaining byte under the control."
  [:variational-free-energy :producer-contract])

(defn- modulo-i5
  "A record with the I5-retired keys removed, for comparison against an anchor
   written before the retirement."
  [record]
  (apply dissoc record i5-retired-keys))

(def ^:private sample-judge-output
  "Minimal judge-style output covering the trace-record fields."
  {:belief (belief/initial-belief-state [:m1])
   :observation {:loop-health 0.7 :stack-pct 0.2}
   :free-energy {:preference-gap-score 0.05 :coverage-uncertainty-pressure 0.10 :controller-score 0.075
                 :per-channel {:loop-health {:value 0.7 :gap 0.0 :in-range? false}}
                 :avoided-active []}
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
      (is (= trace/r8-producer-contract (:producer-contract r)))
      ;; I5 slice (c): the scalar F is retired, and a record that carries the
      ;; retired-F contract must not carry the key -- absence is the contract,
      ;; not an omission the reader should tolerate.
      (is (not (contains? r :variational-free-energy)))
      (is (= :r8/retired-f-controller-v1 (:producer-contract r)))
      (is (contains? r :ranked-actions))
      (is (contains? r :decision))
      (is (contains? r :mode)))))

(deftest route-roundtrips-in-hop-order-test
  (let [route [{:node :R20 :via "scan" :at "2026-09-01T00:00:01Z"}
               {:node :R12 :via "inventory" :at "2026-09-01T00:00:02Z"}
               {:node :R2 :via "observe" :at "2026-09-01T00:00:03Z"}]
        record (trace/trace-record (assoc sample-judge-output :wm/route route))
        roundtrip (edn/read-string (pr-str record))]
    (is (= route (:wm/route roundtrip))
        "EDN round-trip preserves the traversal sequence")
    (is (= [:R20 :R12 :R2] (mapv :node (:wm/route roundtrip))))
    (is (not (contains? (trace/trace-record sample-judge-output) :wm/route))
        "a producer with no route makes no traversal claim")
    (is (not (contains? (trace/trace-record
                         (assoc sample-judge-output :wm/route []))
                        :wm/route))
        "an empty route is also absent rather than persisted as evidence")))

(deftest run-id-roundtrips-through-the-shared-trace-file-test
  (testing "RUN11: records of two runs in one per-date file are separable by id"
    (let [run-id "0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9"
          other-id "ffffffff-0000-1111-2222-333333333333"
          date "2026-09-01"]
      (trace/write-trace! (assoc sample-judge-output :run/id run-id)
                          :dir *tmpdir* :date-str date)
      (trace/write-trace! (assoc sample-judge-output :run/id other-id)
                          :dir *tmpdir* :date-str date)
      (trace/write-trace! sample-judge-output :dir *tmpdir* :date-str date)
      (let [records (trace/read-trace :dir *tmpdir* :date-str date)]
        (is (= 3 (count records)))
        (is (= [run-id other-id] (keep :run/id records))
            "the id survives the EDN write/read round trip, in write order")
        (is (= 1 (count (filter #(= run-id (:run/id %)) records)))
            "selection is by identity, with no timestamp arithmetic")
        (is (not (contains? (nth records 2) :run/id))
            "a producer with no run id makes no claim about which run wrote it")))))

(deftest run-id-is-absent-not-nil-when-the-producer-has-none-test
  (is (not (contains? (trace/trace-record sample-judge-output) :run/id))
      "absence is the explicit signal that the producer minted no run id")
  (is (= "run-7" (:run/id (trace/trace-record
                           (assoc sample-judge-output :run/id "run-7"))))
      "and a producer that has one persists it verbatim"))

(deftest f-pi-dark-off-is-byte-identical-to-previous-implementation-test
  (testing "the default-off record matches HEAD~ byte-for-byte apart from its clock"
    (binding [trace/*persist-policy-trace-details?* false]
      (let [now (trace/trace-record sample-judge-output)
            before (previous-trace-record sample-judge-output)
            fix-clock #(assoc (modulo-i5 %) :timestamp "<same-instant>")]
        (is (= (pr-str (fix-clock before))
               (pr-str (fix-clock now))))))))

(deftest f-pi-dark-fields-roundtrip-when-supplied-test
  (let [details {:f-pi-by-candidate-id
                 {"rank/1" {:status :present :value 1.25}
                  "rank/2" {:status :absent :reason :channel-mismatch}}
                 :f-pi-provenance
                 {:previous-trace-timestamp "2026-07-04T00:00:00Z"
                  :matched-count 1 :unmatched-count 1}}
        record (trace/trace-record (merge sample-judge-output details))
        roundtrip (edn/read-string (pr-str record))]
    (is (= details (select-keys roundtrip (keys details))))))

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
    ;; The golden FILE is left exactly as captured; only the comparison changes,
    ;; dropping the two keys I5 slice (c) retired from both sides (see
    ;; `i5-retired-keys`). Re-capturing the file would delete the pre-I3 claim
    ;; this test exists to make.
    (let [golden (str/trim-newline
                  (slurp (io/resource "futon2/aif/trace-flag-off-golden.txt")))
          strip #(pr-str (modulo-i5 (dissoc % :timestamp)))
          golden' (strip (edn/read-string golden))
          actual' (strip (trace/trace-record sample-judge-output))]
      (is (= (count golden') (count actual'))
          "flag-off record changed size against the pre-I3 bytes")
      (is (= golden' actual')
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

(deftest beta-dark-off-is-byte-identical-to-the-pinned-pre-beta-record-test
  (testing "RUN7: with the dark beta flag off the record matches 183749a's"
    (binding [trace/*persist-policy-trace-details?* false]
      (let [fix-clock #(assoc (modulo-i5 %) :timestamp "<same-instant>")]
        (is (= (pr-str (fix-clock (record-at pre-beta-dark-trace-sha
                                             "pre-beta" sample-judge-output)))
               (pr-str (fix-clock (trace/trace-record sample-judge-output))))
            "no key appears, and no key moves, when the flag is off")))
    (testing "and with the policy-detail flag on, which is the shape S2 runs in"
      (binding [trace/*persist-policy-trace-details?* true]
        (let [fix-clock #(assoc (modulo-i5 %) :timestamp "<same-instant>")]
          (is (= (pr-str (fix-clock (record-at pre-beta-dark-trace-sha
                                               "pre-beta-details" sample-judge-output)))
                 (pr-str (fix-clock (trace/trace-record sample-judge-output))))))))))

(deftest beta-dark-state-roundtrips-when-supplied-test
  (let [state {:status :present
               :beta 0.9877
               :beta-source :converged-posterior
               :solved-tick-count 3
               :f-pi-present-count 108
               :f-pi-absent-count 2
               :solve {:solver :bisect
                       :beta-prior 1.0
                       :beta-posterior 0.9877
                       :gamma 1.012452
                       :iterations 70
                       :converged? true
                       :bracketed? true
                       :fixed-point-residual 1.1e-13
                       :candidate-count 108}}
        record (trace/trace-record
                (assoc sample-judge-output :policy-precision-state state))
        roundtrip (edn/read-string (pr-str record))]
    (is (= state (:policy-precision-state roundtrip))
        "the carried beta survives the EDN write/read the next tick reads it through")
    (is (not (contains? (trace/trace-record sample-judge-output)
                        :policy-precision-state))
        "a producer with the flag off makes no claim about policy precision")))
