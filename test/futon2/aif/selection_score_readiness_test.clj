(ns futon2.aif.selection-score-readiness-test
  "U1 (worklist.edn :U1; SPEC-dormant-wiring.md:26-52) — the readiness suite for
   the theory-aligned selection score `ln E − G/τ_eff − F_π`.

   The score already exists at the one seam I2(b2) built, `policy/softmax-weights`
   (src/futon2/aif/policy.clj:148-207), behind the four coupled flags
   `war_machine.clj:102-121` names. This namespace does NOT flip the default —
   that is a separate J-gated act. It makes the flip VERIFIABLE, by pinning the
   four properties the U1 falsifier reads:

     (a) replay determinism  — every recorded posterior is reproduced at delta 0
                               from that record's own components;
     (b) per-term attribution — every argmax change against the default law
                               carries, computable from the record alone, the
                               term (E, τ or F_π) that moved it;
     (c) precondition refusal — an incoherent flag set refuses loudly (existing
                               behaviour PINNED here, not assumed);
     (d) coverage refusal    — S4's declining tick is a declared case, not a
                               flake.

   THE JOIN IS BY CANDIDATE IDENTITY, NOT BY THE ENVELOPE'S KEY. The `rank/N`
   keys of `:f-pi-by-candidate-id` are the ranks of the tick that PRODUCED the
   predictions — the previous tick (`:f-pi-provenance :previous-trace-timestamp`,
   join key `:action-type-and-target`). Reading them as this tick's ranks
   silently transposes F_π between candidates whose ordering moved. That is not
   a hypothetical: `rank-key-join-breaks-the-replay-test` below measures it on
   S4 tick 0, where 12 of 145 entries sit under a key whose current action has a
   different identity and the replay leaves delta 0.

   RECORDED FIELDS. S2 (20 ticks, dark carry: F_π computed, not applied) and S4
   (4 ticks, live posterior: F_π applied on 3, declined on 1). S3 is NOT FOUND —
   runs/2026-09-01-s3 holds ARMS.txt and README.md only, and README.md:3 says
   the directory is a replay and not a 20-tick stage run — so it contributes 0
   ticks and is declared rather than omitted. Replay only: no live run, no run
   lock, nothing written under data/."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.habit-prior :as habit-prior]
            [futon2.aif.policy :as policy]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as wm]))

;; ---------------------------------------------------------------------------
;; The declared fields. Absence is declared, not omitted (TN §1).

(def ^:private declared-fields
  [{:label "S2"
    :path "holes/labs/wm-contract/runs/2026-09-01-s2/wm-trace-s2.edn"
    :ticks 20}
   {:label "S3"
    :path "holes/labs/wm-contract/runs/2026-09-01-s3/wm-trace-s3.edn"
    :ticks :not-found}
   {:label "S4"
    :path "holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn"
    :ticks 4}])

(defn- read-records
  "The concatenated EDN records of one wm-trace file, in write order."
  [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [out []]
      (let [form (edn/read {:eof ::eof
                            :default (fn [t v] {:trace/edn-tag t :trace/value v})}
                           r)]
        (if (= ::eof form) out (recur (conj out form)))))))

(def ^:private records
  "S2 is 20 MB; every deftest here reads the same two files, so the parse is
   done once per JVM."
  (memoize read-records))

(defn- present-fields
  "The declared fields whose trace file is on disk, each with its records."
  []
  (for [f declared-fields
        :when (.exists (io/file (:path f)))]
    (assoc f :records (records (:path f)))))

;; ---------------------------------------------------------------------------
;; One tick, reduced to the components the score is a function of.

(def ^:private uncovered ::uncovered)

(defn- f-pi-by-identity
  "candidate-identity → F_π, from the tick's readback envelope. The envelope's
   own keys are the PRODUCING tick's ranks and are deliberately dropped here."
  [record]
  (let [envelope (:f-pi-by-candidate-id record)]
    (when (= :present (:status envelope))
      (into {} (keep (fn [[_ entry]]
                       (when (and (= :present (:status entry))
                                  (number? (:value entry)))
                         [(:candidate-identity entry) (double (:value entry))])))
            (:by-candidate-id envelope)))))

(defn- field
  "The tick as the score's arguments: G, ln E and F_π aligned with
   `:ranked-actions`, the τ that was used, and the posterior that was recorded.
   `:f-pi` carries `::uncovered` for a candidate the readback did not reach, so
   a coverage gap is a value rather than a silent zero."
  [record]
  (let [decision (:decision record)
        ranked (:ranked-actions record)
        envelope (:f-pi-posterior decision)
        by-identity (f-pi-by-identity record)
        recorded (:softmax-weights-by-candidate-id decision)]
    {:ids (mapv #(str "rank/" (:rank %)) ranked)
     :g (mapv #(double (:controller-score %)) ranked)
     :ln-e (mapv #(double (or (:habit-prior-bias %) 0.0)) ranked)
     :f-pi (mapv (fn [entry]
                   (get by-identity (habit-prior/policy-key (:action entry))
                        uncovered))
                 ranked)
     :tau (double (:tau decision))
     :tau-source (:tau-source decision)
     :applied? (boolean (:applied? envelope))
     :scaling (:scaling envelope)
     :envelope envelope
     :recorded (mapv #(some-> (get recorded %) double) (mapv #(str "rank/" (:rank %)) ranked))}))

(defn- replay-weights
  "The posterior `policy/softmax-weights` produces from this field's own
   components, through the live function and not a second implementation."
  [{:keys [g ln-e f-pi tau applied? scaling]}]
  (policy/softmax-weights
   g tau ln-e
   (when applied?
     {:f-pi-policy-posterior? true
      :f-pi-values f-pi
      :f-pi-scaling scaling})))

(defn- max-abs-delta [xs ys]
  (reduce max 0.0 (map (fn [x y] (Math/abs (- (double x) (double y)))) xs ys)))

;; ---------------------------------------------------------------------------
;; (a) replay determinism

(deftest recorded-posteriors-replay-at-delta-zero-test
  (testing "every recorded posterior is reproduced from its own record at delta 0"
    (let [fields (present-fields)]
      (is (= #{"S2" "S4"} (set (map :label fields)))
          "S2 and S4 are the fields U1 pins; S3 is declared not-found")
      (doseq [{:keys [label path records ticks]} fields]
        (is (= ticks (count records))
            (str label " tick count is pinned at " path))
        (doseq [[idx record] (map-indexed vector records)]
          (let [f (field record)
                replayed (replay-weights f)]
            (is (every? number? (:recorded f))
                (str label " tick " idx ": every candidate has a recorded weight"))
            (when (:applied? f)
              (is (not-any? #(= uncovered %) (:f-pi f))
                  (str label " tick " idx ": an applied posterior has no uncovered candidate")))
            (is (= 0.0 (max-abs-delta replayed (:recorded f)))
                (str label " tick " idx ": replay delta must be exactly 0"))))))))

(deftest rank-key-join-breaks-the-replay-test
  (testing "the delta-0 check has power: joining F_π by the envelope's rank key
            instead of by candidate identity mis-assigns it and the replay fails"
    (let [record (first (records "holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn"))
          ranked (:ranked-actions record)
          entries (:by-candidate-id (:f-pi-by-candidate-id record))
          by-rank (into {} (map (fn [e] [(str "rank/" (:rank e)) e])) ranked)
          mismatched (count (for [[id entry] entries
                                  :when (not= (:candidate-identity entry)
                                              (habit-prior/policy-key
                                               (:action (get by-rank id))))]
                              id))
          f (field record)
          wrong (assoc f :f-pi (mapv (fn [id] (double (:value (get entries id))))
                                     (:ids f)))]
      (is (= 12 mismatched)
          "S4 tick 0: 12 of 145 envelope keys carry another candidate's identity")
      (is (= 0.0 (max-abs-delta (replay-weights f) (:recorded f)))
          "identity join: exact")
      (is (< 1.0e-6 (max-abs-delta (replay-weights wrong) (:recorded f)))
          "rank-key join: the transposition shows up as a weight error"))))

(deftest synthetic-planted-field-replays-and-each-term-enters-once-test
  (testing "a hand-computed posterior, so (a) is checked against arithmetic and
            not only against the recorder"
    (let [g [1.0 2.0 3.0]
          ln-e [0.0 -1.0 -2.0]
          f-pi [-1.0 -0.5 0.0]
          tau 2.0
          scores (mapv (fn [gi li fi] (+ li (/ (- gi) tau) (- fi))) g ln-e f-pi)
          expected (let [m (apply max scores)
                         es (mapv #(Math/exp (- % m)) scores)
                         z (reduce + es)]
                     (mapv #(/ % z) es))
          replayed (replay-weights {:g g :ln-e ln-e :f-pi f-pi :tau tau
                                    :applied? true :scaling :unscaled})]
      (is (> 1.0e-12 (max-abs-delta replayed expected)))
      (is (> 1.0e-12 (Math/abs (- 1.0 (reduce + replayed)))) "normalised")
      (testing "each term is present exactly once: dropping it moves the posterior"
        (is (< 1.0e-6 (max-abs-delta
                       replayed
                       (replay-weights {:g g :ln-e [0.0 0.0 0.0] :f-pi f-pi :tau tau
                                        :applied? true :scaling :unscaled}))))
        (is (< 1.0e-6 (max-abs-delta
                       replayed
                       (replay-weights {:g g :ln-e ln-e :f-pi f-pi :tau 1.0
                                        :applied? true :scaling :unscaled}))))
        (is (< 1.0e-6 (max-abs-delta
                       replayed
                       (replay-weights {:g g :ln-e ln-e :f-pi f-pi :tau tau
                                        :applied? false})))))
      (testing ":by-tau is a different law and is not offered to the live path"
        (is (< 1.0e-6 (max-abs-delta
                       replayed
                       (replay-weights {:g g :ln-e ln-e :f-pi f-pi :tau tau
                                        :applied? true :scaling :by-tau}))))))))

;; ---------------------------------------------------------------------------
;; (b) per-term attribution
;;
;; The default law is the one the live selector runs today: τ inert at 1.0, no
;; ln E, no F_π. `ablate` turns terms off one at a time; the FULL score is the
;; empty ablation and the DEFAULT score is the total one, so the two laws are
;; the two ends of one expression rather than two implementations.

(def ^:private default-tau 1.0)

(def ^:private terms [:ln-e :tau :f-pi])

(defn- ablate
  "The score vector with the named terms removed."
  [{:keys [g ln-e f-pi tau applied?]} off]
  (mapv (fn [gi li fi]
          (+ (if (contains? off :ln-e) 0.0 li)
             (/ (- gi) (if (contains? off :tau) default-tau tau))
             (if (or (contains? off :f-pi) (not applied?) (= uncovered fi))
               0.0
               (- (double fi)))))
        g ln-e f-pi))

(defn- argmax [xs]
  (first (apply max-key second (map-indexed vector xs))))

(defn- subsets-by-size [xs]
  (sort-by count
           (rest (reduce (fn [acc x] (into acc (map #(conj % x)) acc))
                         [#{}] xs))))

(defn- minimal-mover-sets
  "ALL smallest sets of terms whose removal restores the default law's argmax,
   in a deterministic order. Total by construction — removing all three IS the
   default law — so an argmax change always carries an explanation, which is
   what the U1 falsifier asks for. `nil` when the argmax did not change.

   Plural because sufficiency is not always unique: with F_π off, dropping ln E
   restores the default argmax on ANY field, since −G/τ is monotone in G for
   every τ > 0. A tick with more than one minimal set is attributed to a
   disjunction, and saying so is more honest than picking whichever the search
   happened to reach first."
  [f]
  (let [default (argmax (ablate f (set terms)))]
    (when (not= default (argmax (ablate f #{})))
      (let [sufficient (filter #(= default (argmax (ablate f %)))
                               (subsets-by-size terms))
            smallest (count (first sufficient))]
        (vec (sort-by (fn [s] (mapv name (sort s)))
                      (filter #(= smallest (count %)) sufficient)))))))

(defn- term-contributions
  "Per candidate, the exact decomposition of (full score − default score) into
   the three terms. The identity is checked, not asserted, in the test below."
  [{:keys [g ln-e f-pi tau applied?]}]
  (mapv (fn [gi li fi]
          {:ln-e li
           :tau (* gi (- (/ 1.0 default-tau) (/ 1.0 tau)))
           :f-pi (if (or (not applied?) (= uncovered fi)) 0.0 (- (double fi)))})
        g ln-e f-pi))

(deftest argmax-attribution-is-total-on-every-recorded-tick-test
  (testing "every argmax change against the default law names the term that
            moved it, from the record alone"
    (let [seen (atom [])]
      (doseq [{:keys [label records]} (present-fields)
              [idx record] (map-indexed vector records)]
        (let [f (field record)
              full (ablate f #{})
              default (ablate f (set terms))
              movers (minimal-mover-sets f)
              contributions (term-contributions f)]
          (testing "the decomposition is exact"
            (is (> 1.0e-9
                   (reduce max 0.0
                           (map (fn [s d c]
                                  (Math/abs (- (- s d)
                                               (+ (:ln-e c) (:tau c) (:f-pi c)))))
                                full default contributions)))
                (str label " tick " idx ": full − default = ln E + τ + F_π per candidate")))
          (when movers
            (swap! seen conj [label idx movers])
            (is (seq movers)
                (str label " tick " idx ": a change with no mover is unexplained"))
            (doseq [set-of-movers movers]
              (is (= (argmax default) (argmax (ablate f set-of-movers)))
                  (str label " tick " idx ": removing " set-of-movers
                       " restores the default argmax")))
            (let [size (count (first movers))]
              (doseq [smaller (filter #(and (seq %) (< (count %) size))
                                      (subsets-by-size terms))]
                (is (not= (argmax default) (argmax (ablate f smaller)))
                    (str label " tick " idx ": " smaller " is not sufficient, so the "
                         "reported sets are minimal")))))))
      (testing "what the recorded fields actually attribute"
        (let [by-movers (frequencies (map (fn [[label _ movers]] [label movers]) @seen))]
          (is (= {["S2" [#{:ln-e}]] 20
                  ["S4" [#{:ln-e}]] 1
                  ["S4" [#{:ln-e :f-pi}]] 3}
                 by-movers)
              "S2's 20 ticks move on ln E alone; S4's three applied ticks need
               ln E and F_π together and its declining tick moves on ln E alone.
               Every tick's attribution is UNIQUE (one minimal set), so no
               recorded argmax change rests on a disjunction.")))
      (testing "τ moves nothing on these fields, and the reason is in the record"
        (is (every? (fn [{:keys [records]}]
                      (every? #(= 1.0 (:tau (field %))) records))
                    (present-fields))
            "τ = 1.0 on every recorded tick (:selection-gain-only, g = 1.0), so
             the recorded fields cannot exercise the τ term — the planted field
             below is what does")))))

(deftest planted-fields-attribute-the-planted-term-test
  (testing "one synthetic field per term, each planted so that exactly that term
            moves the argmax — the attribution is checked where the answer is
            known in advance"
    (let [base {:g [1.0 1.2] :ln-e [0.0 0.0] :f-pi [0.0 0.0]
                :tau default-tau :applied? true :scaling :unscaled}]
      (is (= 0 (argmax (ablate base #{}))) "unplanted: the lower G wins")
      (is (nil? (minimal-mover-sets base)) "unplanted: nothing to attribute")
      (testing "ln E planted"
        (let [f (assoc base :ln-e [0.0 1.0])]
          (is (= 1 (argmax (ablate f #{}))))
          (is (= [#{:ln-e}] (minimal-mover-sets f)))))
      (testing "τ planted. τ can only be the UNIQUE mover when F_π is competing
                too: with F_π off, −G/τ is monotone in G at every τ > 0, so
                dropping ln E restores the G-ordered argmax whatever τ is, and
                {ln E} is sufficient by arithmetic rather than by evidence."
        (let [f (assoc base :g [1.0 2.0] :ln-e [0.0 0.3] :f-pi [0.0 -0.2]
                       :tau 10.0)]
          (is (= 1 (argmax (ablate f #{}))))
          (is (= 0 (argmax (ablate f #{:tau}))) "at τ = 1 the G gap wins again")
          (is (= 1 (argmax (ablate f #{:ln-e}))) "ln E alone does not restore it")
          (is (= 1 (argmax (ablate f #{:f-pi}))) "F_π alone does not restore it")
          (is (= [#{:tau}] (minimal-mover-sets f)))))
      (testing "F_π planted"
        (let [f (assoc base :f-pi [0.0 -1.0])]
          (is (= 1 (argmax (ablate f #{}))))
          (is (= [#{:f-pi}] (minimal-mover-sets f)))))
      (testing "a disjunction is reported as one, not resolved by search order"
        (let [f (assoc base :g [1.0 2.0] :ln-e [0.0 0.15] :tau 10.0)]
          (is (= 1 (argmax (ablate f #{}))))
          (is (= [#{:ln-e} #{:tau}] (minimal-mover-sets f)))))
      (testing "an uncovered candidate contributes no F_π rather than a zero
                that pretends to be a reading"
        (let [f (assoc base :f-pi [0.0 uncovered])]
          (is (= 0.0 (:f-pi (nth (term-contributions f) 1))))
          (is (nil? (minimal-mover-sets f))))))))

;; ---------------------------------------------------------------------------
;; (c) precondition refusal — existing behaviour PINNED, not assumed.

(deftest incoherent-flag-sets-refuse-loudly-test
  (testing "FUTON_WM_FPI_POSTERIOR=1 with the chain off makes every tick record
            :no-f-pi-readback and leave the posterior unchanged — a run nobody
            can read, so it throws at the top of the tick"
    (doseq [[dark? details?] [[false true] [true false] [false false]]]
      (with-redefs-fn {#'wm/*f-pi-posterior?* true
                       #'wm/*f-pi-dark?* dark?
                       #'trace/*persist-policy-trace-details?* details?}
        (fn []
          (let [thrown (try (wm/f-pi-posterior-preconditions! :selection-gain-only)
                            nil
                            (catch clojure.lang.ExceptionInfo e e))]
            (is (some? thrown)
                (str "FPI_DARK=" dark? " TRACE_POLICY_DETAILS=" details? " must refuse"))
            (is (seq (:missing (ex-data thrown)))
                "the refusal names the flags that are missing, not just that it failed"))))))
  (testing "the coherent sets do not complain"
    (with-redefs-fn {#'wm/*f-pi-posterior?* true
                     #'wm/*f-pi-dark?* true
                     #'trace/*persist-policy-trace-details?* true}
      (fn [] (is (nil? (wm/f-pi-posterior-preconditions! :selection-gain-only)))))
    (with-redefs-fn {#'wm/*f-pi-posterior?* true
                     #'wm/*f-pi-dark?* false
                     #'trace/*persist-policy-trace-details?* true}
      (fn [] (is (nil? (wm/f-pi-posterior-preconditions! :variational-beta-gamma))
                 "the variational τ mode runs the readback itself")))
    (with-redefs-fn {#'wm/*f-pi-posterior?* false
                     #'wm/*f-pi-dark?* false
                     #'trace/*persist-policy-trace-details?* false}
      (fn [] (is (nil? (wm/f-pi-posterior-preconditions! :selection-gain-only))
                 "flag off: nothing to refuse"))))
  (testing "the same discipline on the τ side, since U1 flips both defaults"
    (with-redefs-fn {#'wm/*beta-dark?* false
                     #'wm/*f-pi-dark?* true
                     #'trace/*persist-policy-trace-details?* true}
      (fn [] (is (thrown? clojure.lang.ExceptionInfo
                          (wm/variational-tau-preconditions! :variational-beta-gamma)))))))

(deftest an-unaligned-or-unknown-f-pi-input-refuses-at-the-seam-test
  (testing "the seam itself refuses a misaligned or non-numeric F_π rather than
            scoring a candidate against another candidate's fit"
    (is (thrown? clojure.lang.ExceptionInfo
                 (policy/softmax-weights [1.0 2.0] 1.0 [0.0 0.0]
                                         {:f-pi-policy-posterior? true
                                          :f-pi-values [0.0]})))
    (is (thrown? clojure.lang.ExceptionInfo
                 (policy/softmax-weights [1.0 2.0] 1.0 [0.0 0.0]
                                         {:f-pi-policy-posterior? true
                                          :f-pi-values [0.0 nil]})))
    (is (thrown? clojure.lang.ExceptionInfo
                 (policy/softmax-weights [1.0 2.0] 1.0 [0.0 0.0]
                                         {:f-pi-policy-posterior? true
                                          :f-pi-values [0.0 0.0]
                                          :f-pi-scaling :by-g})))))

;; ---------------------------------------------------------------------------
;; (d) the coverage refusal is a declared case, not a flake.

(deftest s4-coverage-decline-is-declared-and-falls-back-to-the-old-law-test
  (let [fields (map field (records "holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn"))
        declined (filter #(not (:applied? %)) fields)
        applied (filter :applied? fields)]
    (testing "exactly one of S4's four ticks declines, and it says why"
      (is (= 1 (count declined)))
      (is (= 3 (count applied)))
      (let [envelope (:envelope (first declined))]
        (is (= :absent (:status envelope)))
        (is (= :incomplete-coverage (:reason envelope)))
        (is (= 1 (:uncovered-count envelope)))
        (is (= 145 (:candidate-count envelope)))
        (is (false? (:applied? envelope)))))
    (testing "the applied ticks declare complete coverage and the settled scaling"
      (doseq [f applied]
        (is (= :complete (:coverage (:envelope f))))
        (is (= 0 (:uncovered-count (:envelope f))))
        (is (= :unscaled (:scaling (:envelope f)))
            "scaling is settled by source (friston2017.txt:660-666), not chosen per tick")))
    (testing "declining means running the OLD law, not a degraded new one"
      (let [f (first declined)]
        (is (= 1 (count (filter #(= uncovered %) (:f-pi f))))
            "exactly the one uncovered candidate the envelope counted")
        (is (= 0.0 (max-abs-delta (replay-weights f) (:recorded f))))
        (is (< 1.0e-9 (max-abs-delta
                       (:recorded f)
                       (replay-weights (assoc f :applied? true :scaling :unscaled
                                              :f-pi (mapv #(if (= uncovered %) 0.0 %)
                                                          (:f-pi f))))))
            "and the posterior it recorded is NOT the one a zero-filled F_π
             would have produced — the decline is visible in the numbers")))))
