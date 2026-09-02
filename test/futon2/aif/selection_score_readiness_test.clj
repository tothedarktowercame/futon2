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

;; ---------------------------------------------------------------------------
;; (e) U10 — THE MISSING HALF: attribution over the CHOSEN ACTION.
;;
;; (a)–(d) above are statements about the RECORDED POSTERIOR. U1 readiness
;; point 4 (SPEC-dormant-wiring.md:87-96) found that the posterior is not what
;; chooses: on `:selection-boundary :strategic-recommendation` the chosen action
;; is the head of the G-ordered list and the F_π-bearing posterior is recorded
;; and never read, while `:actuation` selects by ln E − G/τ but F_π cannot
;; reach it. So "the argmax changed" was, on every field above, a claim about a
;; number nothing consulted.
;;
;; U10 adds the law that consults it: `:selection-law :full-score-posterior`,
;; under which the chosen action IS the argmax of `policy/selection-scores` —
;; the same vector `:softmax-weights` normalises. These tests are the
;; attribution over the CHOICE: which action each law picks, whether they
;; differ, and which term moved it when they do.
;;
;; The law is OFF BY DEFAULT and these tests do not flip it. What they do is
;; measure what the flip would do on the recorded fields, so the prediction in
;; SPEC-dormant-wiring.md U10 is stated before the flip rather than read after.

(def ^:private strategic-base
  {:selection-boundary :strategic-recommendation
   :selection-gain 1.0
   :temperature-opts {:tau-mode :selection-gain-only}})

(defn- decide
  "One `select-action` call at the strategic boundary. `law` nil means the
   caller passes no `:selection-law` at all — the production default path."
  [ranked law f-pi-opts]
  (policy/select-action
   ranked
   (cond-> strategic-base
     law (assoc :selection-law law)
     f-pi-opts (assoc :f-pi-opts f-pi-opts))))

(defn- ranked-of
  "A ranked-action field from parallel G / ln E vectors, with distinguishable
   actions and no :no-op (both laws range over the same candidates)."
  [g ln-e]
  (mapv (fn [idx gi li]
          {:action {:type :advance-mission :target (str "M-" idx)}
           :rank (inc idx)
           :controller-score gi
           :habit-prior-bias li})
        (range) g ln-e))

(defn- applied-f-pi [values]
  {:f-pi-policy-posterior? true
   :f-pi-values values
   :f-pi-scaling :unscaled
   :f-pi-posterior {:status :present :coverage :complete :scaling :unscaled
                    :candidate-count (count values) :uncovered-count 0}})

;; --- the default is off, and off means structurally off ---------------------

(deftest full-score-law-is-off-by-default-test
  (testing "a caller that says nothing gets the controller head, even on a
            field where the full score would choose something else — the
            default is not a value that happens to agree"
    (let [ranked (ranked-of [1.0 2.0] [0.0 0.0])
          f-pi (applied-f-pi [0.0 -5.0])
          silent (decide ranked nil f-pi)
          explicit (decide ranked :controller-head f-pi)
          full (decide ranked :full-score-posterior f-pi)]
      (is (= "M-0" (get-in silent [:action :target])) "the G-ordered head")
      (is (= :controller-head (get-in silent [:selection-law :requested]))
          "the default is DECLARED on the record, not left to be inferred")
      (is (= :controller-head (get-in silent [:selection-law :applied])))
      (is (nil? (get-in silent [:selection-law :refusal])))
      (is (false? (get-in silent [:selection-law :moved-from-controller-head?])))
      (is (= (dissoc silent :selection-law) (dissoc explicit :selection-law))
          "naming the default explicitly changes nothing else about the decision")
      (is (= "M-1" (get-in full [:action :target]))
          "and the law, when asked for, really does choose differently here")
      (is (= policy/default-selection-law :controller-head)
          "the default is the historical law, stated as a var so the J-gated
           flip is one edit and not a search"))))

(deftest default-law-leaves-the-habit-keys-counterfactual-test
  (testing "under the default law ln E is not in the expression that chose, and
            the record says so; under the full-score law it is, and the record
            moves with it rather than contradicting itself"
    (let [ranked (ranked-of [1.0 2.0] [0.0 3.0])
          f-pi (applied-f-pi [0.0 0.0])
          head (decide ranked :controller-head f-pi)
          full (decide ranked :full-score-posterior f-pi)]
      (is (false? (:habit-prior-applied? head)))
      (is (= :counterfactual-only (:habit-authority head)))
      (is (= :G (get-in head [:decision-explanation :governed-by])))
      (is (true? (:habit-prior-applied? full)))
      (is (= :live-in-selection-score (:habit-authority full)))
      (is (= :full-score-posterior
             (get-in full [:decision-explanation :governed-by]))))))

;; --- the law consults the posterior it records ------------------------------

(deftest full-score-law-chooses-the-argmax-of-its-own-recorded-posterior-test
  (testing "U10's whole claim: the chosen action is the argmax of the posterior
            the same decision records. Checked against the decision's OWN
            :softmax-weights map, not against a recomputation"
    (doseq [[g ln-e f-pi] [[[1.0 2.0 3.0] [0.0 0.0 0.0] [0.0 0.0 0.0]]
                           [[1.0 2.0 3.0] [0.0 4.0 0.0] [0.0 0.0 0.0]]
                           [[1.0 2.0 3.0] [0.0 0.0 0.0] [0.0 0.0 -9.0]]
                           [[3.0 2.0 1.0] [1.0 0.5 0.0] [-2.0 -1.0 0.0]]]]
      (let [out (decide (ranked-of g ln-e) :full-score-posterior (applied-f-pi f-pi))
            weights (:softmax-weights out)
            top (key (apply max-key val weights))]
        (is (= top (:action out))
            (str "chosen action is the posterior's argmax on " (pr-str [g ln-e f-pi])))
        (is (= :full-score-posterior (get-in out [:selection-law :applied])))))))

(deftest full-score-law-selects-on-terms-not-on-candidate-internals-test
  (testing "U5's invariant: the candidate is OPAQUE at this seam. The law reads
            (G, ln E, F_π) and nothing inside the action, so the same test
            survives the action → cascade candidate change"
    (let [g [1.0 2.0 3.0] ln-e [0.0 0.0 0.0] f-pi [0.0 0.0 -9.0]
          plain (decide (ranked-of g ln-e) :full-score-posterior (applied-f-pi f-pi))
          opaque (decide (mapv (fn [idx gi li fi]
                                 {:action {:type :apply-cascade
                                           :target (str "cascade-" idx)
                                           :cascade {:shown [fi] :wholeness 0.5}}
                                  :rank (inc idx)
                                  :controller-score gi
                                  :habit-prior-bias li})
                               (range) g ln-e f-pi)
                        :full-score-posterior (applied-f-pi f-pi))]
      (is (= 3 (get-in plain [:selection-law :chosen-rank])))
      (is (= (get-in plain [:selection-law :chosen-rank])
             (get-in opaque [:selection-law :chosen-rank]))
          "same scores, same rank chosen, entirely different action payloads")
      (is (= :apply-cascade (get-in opaque [:action :type]))))))

(deftest ties-break-to-the-controller-head-test
  (testing "a tie must not read as a law change: the first maximum wins, so the
            better G-rank keeps the choice"
    (let [out (decide (ranked-of [1.0 1.0] [0.0 0.0]) :full-score-posterior
                      (applied-f-pi [0.0 0.0]))]
      (is (= "M-0" (get-in out [:action :target])))
      (is (false? (get-in out [:selection-law :moved-from-controller-head?]))))))

;; --- the planted field the acceptance asks for ------------------------------

(deftest planted-field-where-the-two-laws-choose-differently-test
  (testing "ONE PLANTED FIELD, per U10's acceptance: the old law and the full
            score choose DIFFERENT actions, and the moving term is named by the
            same `minimal-mover-sets` attribution the recorded fields use.

            The field is planted so F_π alone is the mover: three candidates at
            equal ln E, G ordered 1.0 < 2.0 < 3.0 so the old law takes M-0, and
            an F_π of −9 on the worst-G candidate — well inside the −19.7..−19.0
            band RUN9 measured on the live 145-candidate field — which carries
            it past a G gap of 2.0 at τ = 1."
    (let [g [1.0 2.0 3.0]
          ln-e [0.0 0.0 0.0]
          f-pi [0.0 0.0 -9.0]
          ranked (ranked-of g ln-e)
          opts (applied-f-pi f-pi)
          head (decide ranked :controller-head opts)
          full (decide ranked :full-score-posterior opts)
          planted {:g g :ln-e ln-e :f-pi f-pi :tau 1.0
                   :applied? true :scaling :unscaled}]
      (testing "the two laws choose differently"
        (is (= "M-0" (get-in head [:action :target])) "old law: the G-ordered head")
        (is (= "M-2" (get-in full [:action :target])) "full score: the posterior's argmax")
        (is (not= (:action head) (:action full)))
        (is (true? (get-in full [:selection-law :moved-from-controller-head?])))
        (is (= 1 (get-in full [:selection-law :controller-head-rank])))
        (is (= 3 (get-in full [:selection-law :chosen-rank]))))
      (testing "the moving term is NAMED, by the attribution (b) already uses"
        (is (= [#{:f-pi}] (minimal-mover-sets planted))
            "F_π alone moves it: dropping ln E or τ does not restore the head")
        (is (= 2 (argmax (ablate planted #{}))))
        (is (= 0 (argmax (ablate planted #{:f-pi}))))
        (is (= 2 (argmax (ablate planted #{:ln-e}))))
        (is (= 2 (argmax (ablate planted #{:tau})))))
      (testing "and the choice is the attribution's argmax, not a parallel rule"
        (is (= (argmax (ablate planted #{}))
               (dec (get-in full [:selection-law :chosen-rank])))))
      (testing "a second planted field, moving on ln E instead"
        (let [f {:g [1.0 2.0] :ln-e [0.0 3.0] :f-pi [0.0 0.0] :tau 1.0
                 :applied? true :scaling :unscaled}
              out (decide (ranked-of (:g f) (:ln-e f)) :full-score-posterior
                          (applied-f-pi (:f-pi f)))]
          (is (= "M-1" (get-in out [:action :target])))
          (is (= [#{:ln-e}] (minimal-mover-sets f))))))))

;; --- fail-closed ------------------------------------------------------------

(deftest full-score-law-falls-back-when-f-pi-did-not-enter-test
  (testing "coverage is complete-or-off per tick, so on a declining tick there
            is no F_π to select by. Selecting on ln E − G/τ and calling it the
            full score would be the substitution `f-pi-posterior-opts` refuses
            one seam earlier — so the law takes the head it would have taken
            and the record names the reason"
    (let [ranked (ranked-of [1.0 2.0 3.0] [0.0 0.0 0.0])]
      (doseq [[label f-pi-opts expected-reason]
              [["incomplete coverage"
                {:f-pi-policy-posterior? false
                 :f-pi-posterior {:status :absent :reason :incomplete-coverage
                                  :uncovered-count 1 :candidate-count 3}}
                :incomplete-coverage]
               ["flag off"
                {:f-pi-policy-posterior? false
                 :f-pi-posterior {:status :absent :reason :flag-off
                                  :candidate-count 3}}
                :flag-off]
               ["no opts at all" nil :no-f-pi-opts]]]
        (let [out (decide ranked :full-score-posterior f-pi-opts)]
          (is (= "M-0" (get-in out [:action :target]))
              (str label ": the controller head, not a degraded new law"))
          (is (= :full-score-posterior (get-in out [:selection-law :requested])))
          (is (= :controller-head (get-in out [:selection-law :applied]))
              (str label ": the record says which law RAN"))
          (is (= {:reason expected-reason :effect :fell-back-to-controller-head}
                 (get-in out [:selection-law :refusal]))
              (str label ": with the envelope's own reason"))
          (is (false? (:habit-prior-applied? out))
              (str label ": and the habit keys fall back with it")))))))

(deftest selection-law-preconditions-refuse-a-law-that-can-never-apply-test
  (testing "U10, and the reason it is a top-of-tick throw rather than a per-tick
            one: with FUTON_WM_FPI_POSTERIOR unset, `f-pi-posterior-opts`
            answers :flag-off on EVERY tick, so the run would record a law it
            never once ran"
    (with-redefs-fn {#'wm/*f-pi-posterior?* false}
      (fn []
        (let [thrown (try (wm/selection-law-preconditions! :full-score-posterior)
                          nil
                          (catch clojure.lang.ExceptionInfo e e))]
          (is (some? thrown))
          (is (= ["FUTON_WM_FPI_POSTERIOR=1"] (:missing (ex-data thrown)))
              "the refusal names the flag that is missing, not just that it failed")
          (is (nil? (wm/selection-law-preconditions! :controller-head))
              "the default law needs nothing and refuses nothing"))))
    (with-redefs-fn {#'wm/*f-pi-posterior?* true}
      (fn []
        (is (nil? (wm/selection-law-preconditions! :full-score-posterior))
            "the coherent set does not complain")))
    (testing "a per-tick coverage decline is NOT refused here — it is a declared
              case (1 of S4's 4 ticks) and the selector records the fallback"
      (with-redefs-fn {#'wm/*f-pi-posterior?* true}
        (fn [] (is (nil? (wm/selection-law-preconditions! :full-score-posterior))))))))

(deftest selection-law-parser-admits-exactly-what-select-action-accepts-test
  (testing "the env parser and the closed set change together, as
            `tau-mode-of` and `effective-temperature`'s dispatch do"
    (is (= :full-score-posterior (wm/selection-law-of "full-score-posterior")))
    (is (= :controller-head (wm/selection-law-of "controller-head")))
    (doseq [junk [nil "" "FULL-SCORE-POSTERIOR" "full_score_posterior" "typo"]]
      (is (= :controller-head (wm/selection-law-of junk))
          (str (pr-str junk) " falls to the default law")))
    (doseq [law [:full-score-posterior :controller-head]]
      (is (contains? policy/selection-laws law)
          "every value the parser returns is one select-action accepts"))
    (is (= 2 (count policy/selection-laws))
        "and the set is closed: a third law needs a parser arm and this count")))

(deftest an-unknown-law-or-the-wrong-boundary-refuses-at-the-seam-test
  (testing "a typo must not run the old law under the new name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"unknown :selection-law"
         (decide (ranked-of [1.0 2.0] [0.0 0.0]) :full-score-postrior nil))))
  (testing "and the law is a strategic-boundary law, because F_π reaches no
            other boundary — on :actuation the argmax would be of ln E − G/τ,
            the old habit law wearing the new law's name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"strategic-recommendation"
         (policy/select-action (ranked-of [1.0 2.0] [0.0 0.0])
                               {:selection-boundary :actuation
                                :selection-law :full-score-posterior})))
    (is (map? (policy/select-action (ranked-of [1.0 2.0] [0.0 0.0])
                                    {:selection-boundary :actuation
                                     :selection-law :controller-head}))
        "the default law on the actuation boundary is not an error")))

;; --- what the flip would do on the recorded fields --------------------------

(deftest chosen-action-attribution-on-the-recorded-fields-test
  (testing "the measurement SPEC-dormant-wiring.md U10 states as its prediction
            before the flip: replay each recorded tick's own components through
            `select-action` under BOTH laws and count where the CHOSEN action
            differs, naming the term that moved it.

            This is the half (b) could not supply. (b) counts argmax changes in
            the RECORDED POSTERIOR, which on every one of these ticks nothing
            consulted; this counts changes in what the tick would have CHOSEN,
            which until U10 was a quantity the code could not produce."
    (let [moved (atom [])
          held (atom [])
          fallbacks (atom [])
          overwritten (atom [])]
      (doseq [{:keys [label records]} (present-fields)
              [idx record] (map-indexed vector records)]
        (let [f (field record)
              ranked (mapv (fn [entry]
                             (select-keys entry [:action :rank :controller-score
                                                 :habit-prior-bias]))
                           (:ranked-actions record))
              f-pi-opts (if (:applied? f)
                          {:f-pi-policy-posterior? true
                           :f-pi-values (:f-pi f)
                           :f-pi-scaling (:scaling f)
                           :f-pi-posterior (:envelope f)}
                          {:f-pi-policy-posterior? false
                           :f-pi-posterior (:envelope f)})
              opts (assoc strategic-base :f-pi-opts f-pi-opts)
              head (policy/select-action ranked opts)
              full (policy/select-action
                    ranked (assoc opts :selection-law :full-score-posterior))]
          (is (= (:tau f) (:tau head))
              (str label " tick " idx ": the replay reproduces the recorded τ, so
                   the two laws are compared at the tick's own temperature"))
          (when (not= (:action head) (:action (:decision record)))
            (swap! overwritten conj [label (:selection-boundary (:decision record))]))
          (if (:applied? f)
            (do (is (= :full-score-posterior
                       (get-in full [:selection-law :applied]))
                    (str label " tick " idx ": an applied tick runs the law"))
                (when (not= (:action head) (:action full))
                  (swap! moved conj
                         {:field label :tick idx
                          :movers (minimal-mover-sets f)
                          :head-rank (get-in full [:selection-law
                                                   :controller-head-rank])
                          :chosen-rank (get-in full [:selection-law
                                                     :chosen-rank])})))
            (do (is (= :controller-head (get-in full [:selection-law :applied]))
                    (str label " tick " idx ": a tick with no F_π falls back"))
                (is (= (:action head) (:action full))
                    (str label " tick " idx ": and chooses what the old law chose"))
                (swap! fallbacks conj
                       [label (get-in full [:selection-law :refusal :reason])])
                (swap! held conj [label idx])))))
      (testing "the fallbacks, bucketed by the reason the record itself carries —
                a dark field and a declining tick are NOT the same absence"
        (is (= {["S2" :no-f-pi-opts] 20
                ["S4" :incomplete-coverage] 1}
               (frequencies @fallbacks))
            "S2's 20 ticks ran RUN7's DARK carry: F_π was computed and never put
             in the posterior, so those records carry no :f-pi-posterior envelope
             at all and the law has nothing to select by. S4's single declining
             tick is the different case — the envelope is present and says
             :incomplete-coverage, 1 uncovered candidate of 145. Both fall back
             to the controller head; only the second is the per-tick refusal U10
             built, and reporting them as one number would hide that.")
        (is (= 21 (count @held))))
      (testing "AND ON THE THREE TICKS WHERE THE LAW CAN RUN, IT MOVES THE
                CHOICE — all three of them. This is the number the flip is to
                be judged against, stated before the flip."
        (is (= [{:field "S4" :tick 0 :movers [#{:ln-e :f-pi}]
                 :head-rank 1 :chosen-rank 130}
                {:field "S4" :tick 2 :movers [#{:ln-e :f-pi}]
                 :head-rank 1 :chosen-rank 130}
                {:field "S4" :tick 3 :movers [#{:ln-e :f-pi}]
                 :head-rank 1 :chosen-rank 130}]
               @moved)
            "3 of 3 applied ticks: the full score chooses the rank-6 candidate
             where the old law took the G-ordered head at rank 1, and the
             minimal mover set is {ln E, F_π} TOGETHER on each — the same set
             (b) attributes the recorded posterior's argmax change to. So the
             posterior and the choice now agree about what moved, which is
             exactly what U1 readiness point 4 said they did not.

             THIS LITERAL IS THE FALSIFIER'S TRIPWIRE. A field that moves a
             different rank, or moves on a different term, or stops moving,
             fails here rather than being read off a run afterwards."))
      (testing "AND WHAT THE MODE STILL DOES NOT REACH, measured rather than
                asserted: the action the RECORD carries is not this selector's
                output on any tick"
        (is (= {["S2" :reason-bearing-strategic-policy] 20
                ["S4" :reason-bearing-strategic-policy] 4}
               (frequencies @overwritten))
            "24 of 24. war_machine.clj:5241-5244 replaces the controller
             decision's :action with the mission the R14 strategic selector
             returned, and that selector is handed three mission-id strings and
             a trace id (war_machine.clj:5211-5214) — no τ, no posterior, no
             score crosses that call. U3 found this and recorded it at
             aif-equations.edn :choices :temperature-update
             :sharper-than-the-headroom. U10 wires the R6 seam so that the
             posterior it records is the score it chooses by; it does NOT touch
             the overwrite, which is out of this row's acceptance. Anyone
             reading 'the mode exists' as 'the machine now acts on the full
             score' should read this count first.")))))
