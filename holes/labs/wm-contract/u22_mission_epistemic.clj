#!/usr/bin/env clojure
;; U22 -- the epistemic term of mission value: before/after through the
;; shipped selector (ported from zaif-harness :S5).
;;
;;   clojure -M holes/labs/wm-contract/u22_mission_epistemic.clj [outdir]
;;
;; WHAT THIS ROW OWES: a derived design plus the registry :choices entry
;; (both text, elsewhere), an implementation behind a DECLARED input, and a
;; before/after S2-style step-through in which a MAP-phase mission's rank
;; moves for a STATED epistemic reason.
;;
;; THE SHIPPED PATH IS WHAT IS MEASURED. Both rankings come from
;; `war-machine/enrich-candidates-with-mission-value` -- the same function the
;; live judge calls -- on the same candidate set, differing only in the
;; declared weights map handed to it. Nothing is recomputed by hand: the
;; script sorts what the selector returned.
;;
;; THE CANDIDATE SET IS PINNED, NOT RE-ENUMERATED. The 133 ids come from the
;; committed S2 baseline (zaif-harness runs/S2-step-through-1/02-weights.edn),
;; so a reviewer replays against bytes rather than against whatever the mission
;; scan returns today. S2's own central/strategic numbers are carried along and
;; compared against what the selector computes now; drift is reported, not
;; hidden.
;;
;; ONE DECLARED SUBSTITUTION, and it is the freshness clock: `:epistemic-as-of`
;; is passed explicitly so the staleness questions are reproducible. Every
;; other input is read.
;;
;; NOTHING IS WRITTEN UNDER data/. Replay only: no tick, no trace append, no
;; run lock, no substrate write. The only substrate traffic is the read of the
;; `code/v05/mission-doc` family the judge already performs.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         'futon2.aif.mission-epistemic-value
         'futon2.report.war-machine)

(import '[java.security MessageDigest]
        '[java.time LocalDate])

(def enrich futon2.report.war-machine/enrich-candidates-with-mission-value)
(def mev (requiring-resolve 'futon2.aif.mission-epistemic-value/field-readings))

(def as-of (LocalDate/parse "2026-09-03"))

(def s2-baseline-path
  "holes/labs/zaif-harness/runs/S2-step-through-1/02-weights.edn")

(def before-weights
  "Joe's option-B declaration, 2026-09-02, the weights the 09-02 live records
   were produced under (zaif-harness runs/S3-declared-weights.edn)."
  {:central 0.20 :strategic 0.50 :doable 0.30})

(def epistemic-weight
  "DECLARED HERE, for this step-through only. The default is 0.0 and flipping
   it is the J row."
  0.15)

(def after-weights
  "The NEUTRAL rebalance: the three exploit weights keep Joe's option-B ratio
   exactly and are shrunk by (1 - epistemic-weight), which is why this arm is
   a clean before/after. value_after = 0.85 * value_before + 0.15 * epistemic,
   and a common positive factor cannot reorder the exploit part, so EVERY rank
   move below is the epistemic term and not a re-weighting of my choosing."
  (-> (update-vals before-weights #(* (- 1.0 epistemic-weight) %))
      (assoc :epistemic epistemic-weight)))

(defn fmt [f & args] (apply format f args))

(defn sha256 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(fmt "%02x" %) d))))

(def outdir
  (io/file (or (first *command-line-args*)
               "holes/labs/wm-contract/runs/U22-mission-epistemic")))

(.mkdirs outdir)

(defn spit-edn! [name value]
  (let [f (io/file outdir name)]
    (spit f (with-out-str (pp/pprint value)))
    (println (fmt "wrote %s (%d bytes)" (.getPath f) (.length f)))))

;; ---------------------------------------------------------------------------
;; 01 -- the pinned candidate set.

(def s2-bytes (slurp s2-baseline-path))
(def s2 (edn/read-string s2-bytes))
(def s2-rows (:rows s2))
(def candidate-ids (mapv :id s2-rows))
(def s2-by-id (into {} (map (juxt :id identity)) s2-rows))

(def candidates
  (mapv (fn [id] {:type :advance-mission :target id}) candidate-ids))

(spit-edn! "01-candidates.edn"
           {:source s2-baseline-path
            :source-sha256 (sha256 s2-bytes)
            :count (count candidate-ids)
            :provenance "S2 step-through 1, claude-1 2026-09-02; ids only"
            :ids candidate-ids})

;; ---------------------------------------------------------------------------
;; 02 -- the two weight declarations.

(spit-edn! "02-weights.edn"
           {:before {:value before-weights
                     :authority "Joe, 2026-09-02 (zaif-harness runs/S3-declared-weights.edn); the 09-02 live records carry it"
                     :epistemic "absent; mission-value-weights defaults it to 0.0"}
            :after {:value after-weights
                    :authority "DECLARED BY THIS STEP-THROUGH, not by Joe; the default flip is the J row"
                    :construction "the three exploit weights * (1 - 0.15), epistemic 0.15"
                    :identity "value_after = 0.85 * value_before + 0.15 * epistemic * (gates * decay); checked per row in 05-rank-moves.edn"}
            :as-of (str as-of)
            :carrier "the :mission-value-weights opt (equivalently FUTON_WM_VALUE_WEIGHTS)"})

;; ---------------------------------------------------------------------------
;; 03/04 -- the two rankings, both from the shipped selector.
;;
;; ARM A/B run the selector as THIS PROCESS finds it. That matters and is not
;; hidden: `compute-delta-t-mission` resolves phase through
;; `futon3c.aif.mission-delta-t`, which is not on futon2's classpath, so the
;; doability factor gets :phase nil -> 0.3 for every candidate -- exactly what
;; the 09-01 and 09-02 live trace records show (`:doable 0.3` on 783 of 807
;; ranked mission rows). Arm C repairs that with a declared substitution so the
;; trade against a LIVE doability factor can be measured too.

(defn run-enrich [weights]
  (enrich candidates nil
          {:mission-value-weights weights
           :epistemic-as-of as-of}))

(defn row-of [entry]
  (let [basis (:epistemic-basis entry)]
    (cond-> {:id (:target entry)
             :value (:mission-value-factor entry)
             :central (:central entry)
             :strategic (:strategic entry)
             :doable (:doable entry)
             :phase (:phase entry)
             :completion-gate-factor (:completion-gate-factor entry)
             :operator-gate-factor (:operator-gate-factor entry)
             :non-progress-decay (:non-progress-decay entry)}
      (some? (:epistemic entry))
      (assoc :epistemic (:epistemic entry)
             :epistemic-status (:status basis)
             :epistemic-phase (:phase basis)
             :phase-agreement (:phase-agreement basis)
             :availability (:availability basis)
             :nats (:nats basis)
             :clamped? (:clamped? basis)
             :open-questions (:open-question-count basis)
             :questions (:question-count basis)
             :unresolvable-refs (:unresolvable-cross-reference-count basis)))))

(defn ranked [entries]
  (->> entries
       (map row-of)
       (sort-by (juxt (comp - :value) :id))
       (map-indexed (fn [i r] (assoc r :rank (inc i))))
       vec))

(def before (ranked (run-enrich before-weights)))
(def after (ranked (run-enrich after-weights)))

(spit-edn! "03-before.edn" {:weights before-weights :rows before})
(spit-edn! "04-after.edn" {:weights after-weights :rows after})

;; ---------------------------------------------------------------------------
;; 05 -- what moved, and the epistemic reason for each mover.

(defn rank-map [rows] (into {} (map (juxt :id :rank)) rows))
(defn by-id [rows] (into {} (map (juxt :id identity)) rows))

(def before-rank (rank-map before))
(def before-by-id (by-id before))

(defn moves-between [rows-before rows-after]
  (let [was (rank-map rows-before)]
    (->> rows-after
         (keep (fn [{:keys [id rank] :as r}]
                 (when (not= (get was id) rank)
                   {:id id :rank-before (get was id) :rank-after rank
                    :delta (- (get was id) rank)
                    :phase (:epistemic-phase r)
                    :epistemic (:epistemic r)
                    :nats (:nats r)
                    :open-questions (:open-questions r)
                    :questions (:questions r)})))
         (sort-by :delta >)
         vec)))

(def moves (moves-between before after))

(def value-identity-check
  "The whole claim of the before/after, checked per row:

     value_after = 0.85 * value_before + 0.15 * epistemic * gates * decay

   The gate product and the decay multiply BOTH arms, so they appear once on
   the new term. A common positive factor cannot reorder the exploit part, so
   if this identity holds on every row then every rank move below is the
   epistemic term. Rows the gates zero (value 0.0 in both arms) satisfy it
   trivially and are counted separately, not excused."
  (let [rows (for [{:keys [id value epistemic completion-gate-factor
                           operator-gate-factor non-progress-decay]} after
                   :let [b (get-in before-by-id [id :value])
                         gates (* (double (or completion-gate-factor 1.0))
                                  (double (or operator-gate-factor 1.0))
                                  (double (or non-progress-decay 1.0)))
                         predicted (+ (* (- 1.0 epistemic-weight) (double b))
                                      (* epistemic-weight (double epistemic)
                                         gates))]]
               {:id id :value value :predicted predicted :gates gates
                :gap (Math/abs (- (double value) predicted))})
        bad (filterv #(> (:gap %) 1.0e-9) rows)]
    {:formula "value_after = (1 - w) * value_before + w * epistemic * completion-gate * operator-gate * non-progress-decay"
     :rows (count rows)
     :agreeing (- (count rows) (count bad))
     :disagreeing (count bad)
     :gate-zeroed (count (filter #(zero? (:gates %)) rows))
     :detail (mapv #(select-keys % [:id :value :predicted :gates :gap]) bad)}))

(def map-phase-rows
  (->> after (filter #(= "map" (:epistemic-phase %))) vec))

(spit-edn! "05-rank-moves.edn"
           {:moved (count moves)
            :unmoved (- (count after) (count moves))
            :value-identity value-identity-check
            :top-before (mapv (juxt :rank :id :value) (take 5 before))
            :top-after (mapv (juxt :rank :id :value) (take 5 after))
            :map-phase-candidates
            (mapv #(select-keys % [:id :rank :value :epistemic :nats
                                   :open-questions :questions])
                  map-phase-rows)
            :moves moves})

;; ---------------------------------------------------------------------------
;; 06 -- the field census the score does NOT use, recorded as the denominator.

(def field (mev after-weights {:epistemic-as-of as-of}))

(spit-edn! "06-field-census.edn"
           {:as-of (:as-of field)
            :census (:census field)
            :not-in-the-score
            "the same for every candidate on a tick, so it cannot move a rank"
            :epistemic-status-counts
            (frequencies (map :epistemic-status after))
            :clamp-counts (frequencies (map :clamped? after))
            :unresolvable-cross-references
            {:total (reduce + 0 (keep :unresolvable-refs after))
             :missions-affected (count (filter #(pos? (long (or (:unresolvable-refs %) 0))) after))
             :worst (->> after
                         (filter #(pos? (long (or (:unresolvable-refs %) 0))))
                         (sort-by :unresolvable-refs >)
                         (mapv #(select-keys % [:id :unresolvable-refs
                                                :questions :open-questions]))
                         (take 10)
                         vec)}
            :max-open-questions (apply max 0 (keep :open-questions after))
            :max-nats (apply max 0.0 (keep :nats after))
            :phase-agreement-counts
            (frequencies (map :phase-agreement after))
            :s2-drift
            (let [drift (keep (fn [{:keys [id central strategic]}]
                                (let [s (get s2-by-id id)]
                                  (when (or (> (Math/abs (- (double central)
                                                            (double (:central s))))
                                               5.0e-4)
                                            (> (Math/abs (- (double strategic)
                                                            (double (:strategic s))))
                                               5.0e-4))
                                    {:id id
                                     :central-now central :central-s2 (:central s)
                                     :strategic-now strategic
                                     :strategic-s2 (:strategic s)})))
                              before)]
              {:rows-compared (count before)
               :drifted (count drift)
               :detail (vec (take 20 drift))})})

;; ---------------------------------------------------------------------------
;; 07 -- ARM C: the same two arms with a LIVE doability factor.
;;
;; DECLARED SUBSTITUTION, and it is the only one: `compute-delta-t-mission` is
;; redefined to return the `:mission/phase` already on the mission-doc
;; hyperedge, which is the same carrier the epistemic term reads and the same
;; string futon3c's mission_delta_t reads off the endpoint vertex
;; (mission_delta_t.clj:237-240). Nothing else is redefined. This is what makes
;; the trade against doability measurable at all: with doability inert, "the
;; epistemic term beats doability" would be untested.

(def wm-mission-index (#'futon2.report.war-machine/mission-doc-index))

(def endpoint->phase
  (into {}
        (keep (fn [[id entry]]
                (when-some [phase (get-in field [:missions id :phase])]
                  [(if (map? entry) (:endpoint entry) entry) phase])))
        wm-mission-index))

(defn run-enrich-live-doability [weights]
  (with-redefs-fn
    {#'futon2.report.war-machine/compute-delta-t-mission
     (fn [endpoint] {:mission-phase (get endpoint->phase endpoint)})}
    (fn [] (ranked (run-enrich weights)))))

(def live-before (run-enrich-live-doability before-weights))
(def live-after (run-enrich-live-doability after-weights))
(def live-moves (moves-between live-before live-after))
(def live-rank (rank-map live-before))

(def live-map-rows
  (->> live-after (filter #(= "map" (:epistemic-phase %))) vec))

(spit-edn! "07-doability-live.edn"
           {:substitution
            {:what "compute-delta-t-mission -> {:mission-phase <mission/phase on the code/v05/mission-doc hyperedge>}"
             :why "futon3c.aif.mission-delta-t is not on futon2's classpath in this process; arms A/B therefore run with a doable factor pinned at the 'unknown' 0.3"
             :endpoints-with-a-phase (count endpoint->phase)
             :index-size (count wm-mission-index)}
            :doable-histogram-before (frequencies (map :doable live-before))
            :top-before (mapv (juxt :rank :id :value :doable) (take 5 live-before))
            :top-after (mapv (juxt :rank :id :value :doable) (take 5 live-after))
            :map-phase-candidates
            (mapv (fn [r] (assoc (select-keys r [:id :rank :value :doable
                                                 :epistemic :nats
                                                 :open-questions :questions])
                                 :rank-before (get live-rank (:id r))))
                  live-map-rows)
            :moved (count live-moves)
            :moves live-moves
            :phase-agreement (frequencies (map :phase-agreement live-after))})

;; ---------------------------------------------------------------------------
;; Console summary.

(defn report-arm [label rows-before rows-after map-rows]
  (let [was (rank-map rows-before)]
    (println)
    (println label)
    (println (fmt "  rank 1 before: %-34s %.4f" (:id (first rows-before))
                  (double (:value (first rows-before)))))
    (println (fmt "  rank 1 after:  %-34s %.4f" (:id (first rows-after))
                  (double (:value (first rows-after)))))
    (doseq [r map-rows]
      (println (fmt "  MAP  rank %3d (was %3s)  %-34s value %.4f  doable %.2f  epistemic %.4f  %.4f nats  %d/%d open"
                    (:rank r) (str (get was (:id r))) (:id r)
                    (double (:value r)) (double (:doable r))
                    (double (:epistemic r)) (double (:nats r))
                    (:open-questions r) (:questions r))))))

(println)
(println (fmt "candidates: %d (pinned from %s)" (count candidates) s2-baseline-path))
(println (fmt "weights before: %s" (pr-str before-weights)))
(println (fmt "weights after:  %s" (pr-str after-weights)))
(println (fmt "value identity: %d/%d rows agree with 0.85*before + 0.15*epistemic"
              (:agreeing value-identity-check) (:rows value-identity-check)))
(report-arm "ARM A/B -- doability as this process reads it (phase nil -> 0.3)"
            before after map-phase-rows)
(report-arm "ARM C -- doability fed the mission-doc phase (declared substitution)"
            live-before live-after live-map-rows)
(println)
(println (fmt "rank moves: %d of %d (A/B), %d of %d (C)"
              (count moves) (count after) (count live-moves) (count live-after)))
(println "field census (not in the score):" (:census field))
(println "epistemic statuses:" (frequencies (map :epistemic-status after)))
(println "phase agreement A/B:" (frequencies (map :phase-agreement after)))
(println "phase agreement C:" (frequencies (map :phase-agreement live-after)))
