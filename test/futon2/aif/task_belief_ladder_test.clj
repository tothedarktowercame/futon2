(ns futon2.aif.task-belief-ladder-test
  "U52 -- the three-rung ladder at the selection scoring seam.

   The replay over the recorded fields lives in
   `holes/labs/wm-contract/u52_ladder.clj`; these are the per-node tests that
   run in their own JVM without a corpus, a trace or a tick."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.task-belief-ladder :as ladder]))

(def ^:private reader
  "A pinned document reader, so the kin relation is exercised without touching
   the filesystem."
  {"/repo-a/M-alpha.md" "M-alpha works with M-beta."
   "/repo-a/M-beta.md" "M-beta, see M-alpha."
   "/repo-b/M-gamma.md" "M-gamma stands alone."})

(defn- cand [target repo & [extra]]
  (merge {:type :advance-mission
          :target target
          :mission-path (str "/home/joe/code/" repo "/holes/missions/" target ".md")
          :mission-value-factor 0.09}
         extra))

(def ^:private field
  [(cand "M-alpha" "futon2" {:mission-path "/repo-a/M-alpha.md"})
   (cand "M-beta" "futon2" {:mission-path "/repo-a/M-beta.md"})
   (cand "M-gamma" "futon3" {:mission-path "/repo-b/M-gamma.md"})
   {:type :no-op :target nil}])

(def ^:private history {[:advance-mission "M-alpha"] 4})

(defn- ctx-for [relation]
  (ladder/field-context field history {:relation relation :doc-reader reader}))

(deftest rung-1-is-direct-case-history
  (let [c (ladder/classify (ctx-for :k-doc-xref) (first field))]
    (is (= 1 (:task-belief/rung c)))
    (is (= 4.0 (:task-belief/support c)))
    (is (= 0.8 (:task-belief/factor c)) "n/(n+1) at n = 4")
    (is (= :direct-case-history (get-in c [:task-belief/derivation :rule])))
    (is (nil? (:task-belief/constructed c))
        "a value read off the record is not constructed")))

(deftest rung-2-is-constructed-marked-and-provenanced
  (let [c (ladder/classify (ctx-for :k-doc-xref) (second field))]
    (is (= 2 (:task-belief/rung c)))
    (is (true? (:task-belief/constructed c)))
    (is (= 4.0 (:task-belief/support c)) "generalized from M-alpha's 4 decisions")
    (is (= (* 0.5 0.8) (:task-belief/factor c)) "discount x n/(n+1)")
    (is (= [{:kin-key [:advance-mission "M-alpha"] :decisions 4}]
           (get-in c [:task-belief/derivation :provenance]))
        "the provenance points at the kin record it generalized from")))

(deftest zero-open-holes-refuses-even-recorded-case-history
  (let [action (assoc (first field) :open-hole-count 0)
        c (ladder/classify (ctx-for :k-doc-xref) action)
        refusal (ladder/refusal-record (ctx-for :k-doc-xref) action c)]
    (is (= 3 (:task-belief/rung c)))
    (is (= :no-open-holes (:refusal/reason refusal)))
    (is (= :no-open-holes (:refusal/basis refusal)))
    (is (= 1 (get-in refusal [:refusal/overridden-task-belief
                              :task-belief/rung])))
    (is (= 4.0 (get-in refusal [:refusal/overridden-task-belief
                                :task-belief/support]))
        "the refusal preserves the case history it overrode")))

(deftest planted-nonzero-hole-candidate-escapes-the-availability-refusal
  (let [action (assoc (first field) :open-hole-count 1)
        c (ladder/classify (ctx-for :k-doc-xref) action)]
    (is (= 1 (:task-belief/rung c)))
    (is (= (* 0.8 0.5) (:task-belief/factor c)))
    (is (not= :no-open-holes
              (get-in c [:task-belief/derivation :rule])))))

(deftest recorded-mission-band-tie-is-drained-by-hole-availability
  (let [base (second field)
        c2 (ladder/classify (ctx-for :k-doc-xref)
                            (assoc base :open-hole-count 2))
        c24 (ladder/classify (ctx-for :k-doc-xref)
                             (assoc base :open-hole-count 24))]
    (is (= 2 (:task-belief/rung c2) (:task-belief/rung c24)))
    (is (not= (:task-belief/factor c2) (:task-belief/factor c24)))
    (is (= (/ 2.0 3.0)
           (get-in c2 [:task-belief/derivation :hole-availability :factor])))
    (is (= (/ 24.0 25.0)
           (get-in c24 [:task-belief/derivation :hole-availability :factor])))))

(deftest rung-3-is-a-typed-refusal-with-a-not-found-basis
  (let [c (ladder/classify (ctx-for :k-doc-xref) (nth field 2))]
    (is (= 3 (:task-belief/rung c)))
    (is (zero? (:task-belief/factor c)))
    (is (= :construction-exhausted (get-in c [:task-belief/derivation :rule])))
    (is (re-find #"^not found" (get-in c [:task-belief/derivation :basis]))
        "an absence is typed as an absence, not left silent")))

(deftest case-history-wins-is-an-ordering-property-of-the-arithmetic
  (testing "no constructed value can reach the weakest observed one"
    (let [c1 (ladder/classify (ctx-for :k-doc-xref) (first field))
          c2 (ladder/classify (ctx-for :k-doc-xref) (second field))]
      (is (> (:task-belief/factor c1) (:task-belief/factor c2))))
    (testing "and that holds at the extremes: one decision against unbounded kin mass"
      (let [weakest (ladder/support->factor 1)
            strongest-constructed (* ladder/default-generalization-discount
                                     (ladder/support->factor 1e9))]
        (is (> weakest strongest-constructed))))))

(deftest the-relation-changes-who-can-be-constructed-for
  (testing ":k-type reaches every mission candidate and separates none of them"
    (let [{:keys [census]} (ladder/apply-ladder field (ctx-for :k-type))]
      (is (= {1 1, 2 2, :out-of-scope 1} census))))
  (testing ":k-doc-xref reaches only the candidate whose document names a kin"
    (let [{:keys [census]} (ladder/apply-ladder field (ctx-for :k-doc-xref))]
      (is (= {1 1, 2 1, 3 1, :out-of-scope 1} census))))
  (testing "an unknown relation is refused loudly rather than defaulted"
    (is (thrown? clojure.lang.ExceptionInfo
                 (ladder/field-context field history {:relation :k-invented})))))

(deftest apply-ladder-removes-rung-3-and-scales-the-rest
  (let [{:keys [candidates refusals census]} (ladder/apply-ladder field (ctx-for :k-doc-xref))
        by-target (into {} (map (juxt :target identity)) candidates)]
    (is (= 3 (count candidates)) "one refused, one out of scope, two kept")
    (is (= 1 (count refusals)))
    (is (= 1 (get census 3)))
    (testing "refusing means leaving the field, which is what refusing is at a scoring seam"
      (is (nil? (by-target "M-gamma"))))
    (testing "the pre-ladder number is kept beside the new one"
      (is (= 0.09 (:task-belief/pre-ladder-mission-value-factor (by-target "M-alpha"))))
      (is (= (* 0.09 0.8) (:mission-value-factor (by-target "M-alpha")))))
    (testing "out-of-scope candidates carry a scope marker and nothing else changes"
      (let [n (first (filter #(= :no-op (:type %)) candidates))]
        (is (= :out-of-scope (:task-belief/rung n)))
        (is (= {:type :no-op :target nil} (dissoc n :task-belief/rung)))))))

(deftest an-empty-history-refuses-every-in-scope-candidate
  (let [ctx (ladder/field-context field {} {:relation :k-doc-xref :doc-reader reader})
        {:keys [census refusals]} (ladder/apply-ladder field ctx)]
    (is (= 3 (get census 3)))
    (is (= 3 (count refusals)))
    (is (every? #(= ladder/refusal-reason (:refusal/reason %)) refusals))))

(deftest a-planted-unknown-candidate-is-classified-not-looked-up
  (let [plant {:type :learn-action-class
               :target "M-not-a-real-mission"
               :mission-path "/nowhere/M-not-a-real-mission.md"}
        ctx (ctx-for :k-type)]
    (is (= 3 (:task-belief/rung (ladder/classify ctx plant)))
        "a type no chosen key carries reaches rung 3")
    (is (= 2 (:task-belief/rung (ladder/classify ctx (assoc plant :type :advance-mission))))
        "the SAME plant retyped reaches rung 2 under :k-type, so rung 3 above is a classification and not a lookup miss")))

(deftest the-mint-payload-counts-the-partition-by-reason
  ;; :U63. The defect this pins: `refusal-payload` labelled the whole rung-3
  ;; partition `refusal-reason` and counted all of it, so on the 2026-09-05 u60
  ;; field the payload asserted 98 zero-support refusals over a partition of 45
  ;; zero-support and 53 :no-open-holes. A fixture with BOTH reasons is what
  ;; makes the test able to fail: over the plain field every refusal is
  ;; zero-support and a single-reason payload reads correct.
  (let [mixed (assoc-in field [0 :open-hole-count] 0)   ; M-alpha: rung 1, refused by the hole rule
        ctx (ladder/field-context mixed history {:relation :k-doc-xref :doc-reader reader})
        {:keys [refusals]} (ladder/apply-ladder mixed ctx)
        {:keys [tension]} (ladder/refusal-tension
                           {:subject-id :test-mixed :refusals refusals :relation :k-doc-xref
                            :artifact "runs/U52-ladder/04-refusals.edn" :at "2026-09-05"
                            :pointers ["runs/U52-ladder/04-refusals.edn"]})
        payload (:tension/refusal tension)]
    (is (= 2 (count refusals)) "M-alpha by the hole rule, M-gamma by construction-exhausted")
    (is (= {:no-open-holes 1
            :task-belief/zero-support-construction-exhausted 1}
           (:refused-by-reason payload))
        "one count per reason, not one count under one reason")
    (is (= [:no-open-holes :task-belief/zero-support-construction-exhausted]
           (:task-belief/refusal payload))
        "the reasons the records actually carry, in sorted order")
    (is (= 2 (:refused-count payload)))
    (is (= (:refused-count payload) (reduce + 0 (vals (:refused-by-reason payload))))
        "the per-reason counts sum to the total, which is what makes the total checkable")
    (is (= {:no-open-holes ["M-alpha"]
            :task-belief/zero-support-construction-exhausted ["M-gamma"]}
           (:refused-sample payload))
        "the sample is per reason: one drawn from the union cannot be read against either count")
    (testing "the mint event names the same partition rather than one reason"
      (let [{:keys [event]} (ladder/refusal-tension
                             {:subject-id :test-mixed :refusals refusals :relation :k-doc-xref
                              :artifact "a" :at "2026-09-05" :pointers ["p:1"]})]
        (is (re-find #"1 :no-open-holes" (:event/note event)))
        (is (re-find #"1 :task-belief/zero-support-construction-exhausted"
                     (:event/note event)))))
    (testing "and a single-reason partition still reads as one reason"
      (let [only (:tension/refusal
                  (:tension (ladder/refusal-tension
                             {:subject-id :test-one
                              :refusals (filterv #(= ladder/refusal-reason (:refusal/reason %))
                                                 refusals)
                              :relation :k-doc-xref :artifact "a" :at "2026-09-05"
                              :pointers ["p:1"]})))]
        (is (= [ladder/refusal-reason] (:task-belief/refusal only)))
        (is (= {ladder/refusal-reason 1} (:refused-by-reason only)))))
    (testing "every reason a record carries is one this namespace declares"
      (is (every? (set ladder/refusal-reasons) (map :refusal/reason refusals))))))

(deftest the-mint-payload-has-the-shape-the-ledger-declares
  (let [{:keys [refusals]} (ladder/apply-ladder field (ctx-for :k-doc-xref))
        {:keys [tension event]}
        (ladder/refusal-tension {:subject-id :test-field
                                 :refusals refusals
                                 :relation :k-doc-xref
                                 :artifact "runs/U52-ladder/04-refusals.edn"
                                 :at "2026-09-04"
                                 :pointers ["runs/U52-ladder/04-refusals.edn"]})]
    (is (= :refused-prediction (:tension/born-of tension)))
    (is (= :carried (:tension/status tension)))
    (is (= (:tension/status tension) (:event/type event))
        "the ledger's rule: status at mint equals the mint event's type")
    (is (= 2 (count (:tension/poles tension))) "a tension with one pole is a plan")
    (is (= (:tension/refusal tension)
           (edn/read-string (:tension/statement tension)))
        "u41 control 12: the statement is the in-record refusal read back verbatim")
    (is (= (:tension/id tension) (:event/tension event)))
    (is (seq (get-in tension [:tension/provenance :pointers])))))

(deftest the-structured-run-key-is-carried-exactly-when-it-is-passed
  ;; :U60. u41's deposit scan prefers :tension/provenance :records over a
  ;; substring scan of the record's prose, so the producer has to write it. It
  ;; is OMITTED rather than written as [] when the caller passes none, and that
  ;; is the half worth a test: `append-tension!` is :already-present only for a
  ;; payload matching the committed one exactly, so an unconditional key would
  ;; turn the two U52 tensions committed on 2026-09-04 -- minted before this key
  ;; existed -- from a documented replay into an identity-conflict refusal.
  (let [{:keys [refusals]} (ladder/apply-ladder field (ctx-for :k-doc-xref))
        args {:subject-id :test-field :refusals refusals :relation :k-doc-xref
              :artifact "runs/U52-ladder/04-refusals.edn" :at "2026-09-05"
              :pointers ["runs/U52-ladder/04-refusals.edn"]}
        without (get-in (ladder/refusal-tension args) [:tension :tension/provenance])
        with (get-in (ladder/refusal-tension (assoc args :records ["run-a" "run-b"]))
                     [:tension :tension/provenance])]
    (is (= #{:who :when :pointers} (set (keys without)))
        "no :records key at all, not :records []")
    (is (= ["run-a" "run-b"] (:records with)))
    (is (= (dissoc with :records) without)
        "passing the run key changes nothing else about the provenance")
    (is (= without (get-in (ladder/refusal-tension (assoc args :records []))
                           [:tension :tension/provenance]))
        "an empty :records is the same as none: a caller with no run to name must not diverge the payload")))
