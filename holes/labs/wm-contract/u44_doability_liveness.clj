#!/usr/bin/env clojure
;; U44 -- doability liveness: the phase the judge already fetched, fed to the
;; doability factor behind a declared input.
;;
;;   clojure -M holes/labs/wm-contract/u44_doability_liveness.clj [outdir] [mode]
;;
;;   mode = "baseline"  write 00-preedit-baseline.edn ONLY, using nothing but
;;                      the pre-U44 API (no :live-doability? opt is mentioned),
;;                      so it runs against the code as it stood before the
;;                      repair and pins what "unchanged" means.
;;   mode = "full"      (default) every arm, every control, the whole run.
;;
;; WHAT THIS ROW OWES (worklist :U44): a live phase read behind a declared
;; input with the default byte-identical and PINNED; an S2-style step-through
;; on the current field showing before/after rankings under the live doable
;; factor; the cross-carrier phase-agreement check re-run through the new path;
;; and the J7 revisit packet's numbers.
;;
;; THE SHIPPED PATH IS WHAT IS MEASURED. Every ranking below comes from
;; `war-machine/enrich-candidates-with-mission-value` -- the function the live
;; judge calls -- on the same pinned candidate set. The arms differ only in the
;; declared `:live-doability?` input and the declared weights map. Nothing is
;; recomputed by hand except the controls, which recompute deliberately.
;;
;; THE CANDIDATE SET IS PINNED, NOT RE-ENUMERATED: the same 133 ids U22 used,
;; from the committed S2 baseline, so a reviewer replays against bytes.
;;
;; NOTHING IS WRITTEN UNDER data/. Replay only: no tick, no trace append, no
;; run lock, no substrate write, no weight or table changed. The only substrate
;; traffic is the read of the `code/v05/mission-doc` family the judge already
;; performs.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         'futon2.aif.mission-epistemic-value
         'futon2.report.war-machine)

(import '[java.security MessageDigest]
        '[java.time LocalDate])

(def enrich futon2.report.war-machine/enrich-candidates-with-mission-value)
(def phase-doability @#'futon2.report.war-machine/phase-doability)

(def as-of (LocalDate/parse "2026-09-03"))

(def args *command-line-args*)
(def outdir (io/file (or (first args)
                         "holes/labs/wm-contract/runs/U44-doability-liveness")))
(def mode (or (second args) "full"))

(def s2-baseline-path
  "holes/labs/zaif-harness/runs/S2-step-through-1/02-weights.edn")

(def wm-source-path "scripts/futon2/report/war_machine.clj")

(def declared-weights
  "Joe's option-B declaration, 2026-09-02 -- the weights the 09-02 live records
   were produced under (zaif-harness runs/S3-declared-weights.edn), and the
   weights U22's before-arm used. Held FIXED across the two primary arms so the
   only thing that changes is where the doability factor gets its phase."
  {:central 0.20 :strategic 0.50 :doable 0.30})

(def shipped-default-weights
  "war_machine/default-mission-value-weights, for the control arm that shows
   the rank moves are not an artefact of the declared weights."
  {:central 0.25 :strategic 0.45 :doable 0.30 :epistemic 0.0})

(def epistemic-weight
  "U22's step-through weight, reused UNCHANGED for the J7 arms. Declared by a
   step-through, not by Joe; the default is 0.0 and flipping it is J7."
  0.15)

(def epistemic-arm-weights
  (-> (update-vals declared-weights #(* (- 1.0 epistemic-weight) %))
      (assoc :epistemic epistemic-weight)))

(defn fmt [f & args] (apply format f args))

(defn sha256 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(fmt "%02x" %) d))))

(.mkdirs outdir)

(defn spit-edn! [name value]
  (let [f (io/file outdir name)]
    (spit f (with-out-str (pp/pprint value)))
    (println (fmt "wrote %s (%d bytes)" (.getPath f) (.length f)))))

;; ---------------------------------------------------------------------------
;; The pinned candidate set.

(def s2-bytes (slurp s2-baseline-path))
(def candidate-ids (mapv :id (:rows (edn/read-string s2-bytes))))
(def candidates (mapv (fn [id] {:type :advance-mission :target id}) candidate-ids))

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
      (some? (:phase-source entry)) (assoc :phase-source (:phase-source entry))
      (some? (:epistemic entry))
      (assoc :epistemic (:epistemic entry)
             :epistemic-status (:status basis)
             :epistemic-phase (:phase basis)
             :phase-agreement (:phase-agreement basis)
             :doability-phase (:doability-phase basis)
             :nats (:nats basis)
             :open-questions (:open-question-count basis)))))

(defn ranked [entries]
  (->> entries
       (map row-of)
       (sort-by (juxt (comp - :value) :id))
       (map-indexed (fn [i r] (assoc r :rank (inc i))))
       vec))

;; ---------------------------------------------------------------------------
;; baseline mode: the pre-U44 API and nothing else.

(when (= mode "baseline")
  (let [rows (ranked (enrich candidates nil
                             {:mission-value-weights declared-weights
                              :epistemic-as-of as-of}))]
    (spit-edn! "00-preedit-baseline.edn"
               {:what "the shipped ranking BEFORE the U44 repair, on the pinned candidates at the declared weights"
                :produced-by "u44_doability_liveness.clj baseline (pre-U44 API only)"
                :war-machine-sha256 (sha256 (slurp wm-source-path))
                :weights declared-weights
                :candidates (count candidates)
                :doable-histogram (frequencies (map :doable rows))
                :phase-histogram (frequencies (map :phase rows))
                :rows rows}))
  (System/exit 0))

;; ---------------------------------------------------------------------------
;; full mode.

(defn arm
  "One ranking from the shipped selector. `live?` is the DECLARED input this
   row adds; nil means 'do not pass the key at all', which is the default path."
  [weights live?]
  (ranked (enrich candidates nil
                  (cond-> {:mission-value-weights weights
                           :epistemic-as-of as-of}
                    (some? live?) (assoc :live-doability? live?)))))

(def default-arm (arm declared-weights nil))
(def live-arm (arm declared-weights true))
(def explicit-off-arm (arm declared-weights false))

(defn rank-map [rows] (into {} (map (juxt :id :rank)) rows))
(defn by-id [rows] (into {} (map (juxt :id identity)) rows))

(defn moves-between [rows-before rows-after]
  (let [was (rank-map rows-before)
        before* (by-id rows-before)]
    (->> rows-after
         (keep (fn [{:keys [id rank] :as r}]
                 (when (not= (get was id) rank)
                   {:id id :rank-before (get was id) :rank-after rank
                    :delta (- (get was id) rank)
                    :phase (:phase r)
                    :doable-before (get-in before* [id :doable])
                    :doable-after (:doable r)
                    :value-before (get-in before* [id :value])
                    :value-after (:value r)})))
         (sort-by (juxt (comp - :delta) :id))
         vec)))

(def moves (moves-between default-arm live-arm))

;; ---------------------------------------------------------------------------
;; 01 -- inputs and pins.

(def wm-bytes (slurp wm-source-path))

(spit-edn! "01-inputs.edn"
           {:candidates {:source s2-baseline-path
                         :source-sha256 (sha256 s2-bytes)
                         :count (count candidates)
                         :provenance "S2 step-through 1, claude-1 2026-09-02; ids only, the same pin U22 used"}
            :weights {:primary declared-weights
                      :authority "Joe, 2026-09-02 (zaif-harness runs/S3-declared-weights.edn); held fixed across both primary arms"
                      :epistemic "absent; mission-value-weights defaults it to 0.0"
                      :control shipped-default-weights}
            :declared-input {:key :live-doability?
                             :env "FUTON_WM_LIVE_DOABILITY=1"
                             :var "futon2.report.war-machine/*live-doability?*"
                             :default false
                             :what "the doability factor reads :mission/phase off the code/v05/mission-doc hyperedge mission-doc-index already fetched, instead of the futon3c.aif.mission-delta-t resolution that returns nil on futon2's classpath"}
            :epistemic-as-of (str as-of)
            :war-machine-sha256 (sha256 wm-bytes)
            :fiat-table {:what "phase-doability, UNCHANGED by this row"
                         :value phase-doability}})

;; ---------------------------------------------------------------------------
;; 02/03 -- the two arms.

(defn arm-summary [rows]
  {:doable-histogram (frequencies (map :doable rows))
   :phase-histogram (frequencies (map :phase rows))
   :phase-source-histogram (frequencies (map :phase-source rows))
   :top-10 (mapv (juxt :rank :id :value :phase :doable) (take 10 rows))})

(spit-edn! "02-default-arm.edn"
           (assoc (arm-summary default-arm)
                  :arm "DEFAULT: no :live-doability? key passed at all"
                  :weights declared-weights
                  :rows default-arm))

(spit-edn! "03-live-arm.edn"
           (assoc (arm-summary live-arm)
                  :arm "LIVE: :live-doability? true, the same weights"
                  :weights declared-weights
                  :rows live-arm))

;; ---------------------------------------------------------------------------
;; 04 -- what moved.

(def default-doable-values (set (map :doable default-arm)))
(def live-doable-values (set (map :doable live-arm)))

(defn gate-census [rows]
  {:completion-gate (frequencies (map :completion-gate-factor rows))
   :operator-gate (frequencies (map :operator-gate-factor rows))
   :zero-value (count (filter #(zero? (double (:value %))) rows))})

(spit-edn! "04-rank-moves.edn"
           {:moved (count moves)
            :unmoved (- (count live-arm) (count moves))
            ;; THE SECOND CONSEQUENCE, and it is not the doability factor: the
            ;; completion gate is `(if (= "complete" phase) 0.0 1.0)`, so a nil
            ;; phase disabled it on the same date and for the same reason.
            ;; Under the live read it fires, and the missions the field marks
            ;; complete leave the ranking at value 0.0.
            :gates {:default (gate-census default-arm)
                    :live (gate-census live-arm)
                    :complete-phase-candidates
                    (mapv :id (filter #(= "complete" (:phase %)) live-arm))
                    :operator-gated-candidates
                    (mapv :id (filter #(zero? (double (:operator-gate-factor %)))
                                      live-arm))}
            :rank-1-before (select-keys (first default-arm) [:id :value :phase :doable])
            :rank-1-after (select-keys (first live-arm) [:id :value :phase :doable])
            :distinct-doable-values {:default (vec (sort default-doable-values))
                                     :live (vec (sort live-doable-values))}
            :biggest-gains (vec (take 10 moves))
            :biggest-losses (vec (take 10 (reverse moves)))
            :moves moves})

;; ---------------------------------------------------------------------------
;; 05 -- the default pin: the flag-off arm against the PRE-EDIT ranking.

(def baseline-file (io/file outdir "00-preedit-baseline.edn"))

(def default-pin
  (if (.exists baseline-file)
    (let [baseline (edn/read-string (slurp baseline-file))
          base-rows (:rows baseline)
          base-by-id (by-id base-rows)
          compared (for [r default-arm
                         :let [b (get base-by-id (:id r))]]
                     {:id (:id r) :now r :was b})
          differing (->> compared
                         (filter (fn [{:keys [now was]}] (not= now was)))
                         (mapv (fn [{:keys [id now was]}]
                                 {:id id
                                  :keys-differing (vec (sort (keep (fn [k]
                                                                     (when (not= (get now k) (get was k)) k))
                                                                   (distinct (concat (keys now) (keys was))))))
                                  :now now :was was})))]
      {:baseline-file (.getName baseline-file)
       :baseline-war-machine-sha256 (:war-machine-sha256 baseline)
       :war-machine-sha256-now (sha256 wm-bytes)
       :rows-compared (count compared)
       :identical (- (count compared) (count differing))
       :differing (count differing)
       :detail (vec (take 20 differing))})
    {:baseline-file :absent
     :note "run `... u44_doability_liveness.clj <outdir> baseline` at the pre-U44 commit first"}))

(spit-edn! "05-default-pin.edn"
           (assoc default-pin
                  :what "the DEFAULT arm after the repair, row for row against the ranking the code produced before it"
                  :explicit-off-equals-default
                  (= (mapv #(dissoc % :phase-source) explicit-off-arm)
                     (mapv #(dissoc % :phase-source) default-arm))))

;; ---------------------------------------------------------------------------
;; 06 -- the cross-carrier phase check, re-run through the new path.
;;
;; :phase-agreement is computed by mission-epistemic-value/record-for and needs
;; a positive :epistemic weight to exist at all, so this arm declares U22's
;; 0.15 FOR THE DIAGNOSTIC ONLY. The two carriers are two independent substrate
;; reads of the same prop: mission-epistemic-value's own hyperedges-by-type
;; read on one side, war-machine's mission-doc-index on the other.

(def epistemic-live-arm (arm epistemic-arm-weights true))
(def epistemic-default-arm (arm epistemic-arm-weights nil))

(defn agreement-counts [rows] (frequencies (map :phase-agreement rows)))

(spit-edn! "06-phase-agreement.edn"
           {:what "the U22 cross-carrier check re-run through the shipped :live-doability? path instead of a with-redefs substitution"
            :carriers {:doability "mission/phase on the code/v05/mission-doc hyperedge, via war-machine/mission-doc-index (this row's repair)"
                       :epistemic "mission/phase on the same family, via mission-epistemic-value/field-readings' own substrate read"}
            :declared "epistemic weight 0.15, for this diagnostic only; the default is 0.0"
            :through-the-new-path (agreement-counts epistemic-live-arm)
            :through-the-default-path (agreement-counts epistemic-default-arm)
            :disagreements (->> epistemic-live-arm
                                (filter #(= :disagree (:phase-agreement %)))
                                (mapv #(select-keys % [:id :phase :epistemic-phase :doability-phase])))
            :absent-detail (->> epistemic-live-arm
                                (filter #(not= :agree (:phase-agreement %)))
                                (mapv #(select-keys % [:id :phase-agreement :phase :epistemic-phase]))
                                (sort-by :id)
                                vec)})

;; ---------------------------------------------------------------------------
;; 07 -- the J7 arms: what the epistemic weight buys against a LIVE doability.
;;
;; Four arms, two inputs. J7 was ruled "stay off, come back to this later" on
;; the ground that a live epistemic default would trade against a CONSTANT.
;; These are the same four numbers with that ground removed. NO RULING HERE.

(def j7-arms
  {:epistemic-0.0-doability-inert  default-arm
   :epistemic-0.0-doability-live   live-arm
   :epistemic-0.15-doability-inert epistemic-default-arm
   :epistemic-0.15-doability-live  epistemic-live-arm})

(defn map-phase-rows [rows]
  (->> rows (filter #(= "map" (:phase %))) vec))

(spit-edn! "07-j7-arms.edn"
           {:what "the epistemic-weight question re-posed against a live doability factor; J7's named prerequisite"
            :not-ruled "no default is changed by this file. :epistemic stays 0.0 and :live-doability? stays false in the shipped code."
            :weights {:exploit-only declared-weights :with-epistemic epistemic-arm-weights}
            :rank-1 (update-vals j7-arms #(select-keys (first %) [:id :value :phase :doable]))
            :moves {:epistemic-at-inert-doability
                    (count (moves-between default-arm epistemic-default-arm))
                    :epistemic-at-live-doability
                    (count (moves-between live-arm epistemic-live-arm))
                    :doability-at-epistemic-0.0 (count moves)
                    :doability-at-epistemic-0.15
                    (count (moves-between epistemic-default-arm epistemic-live-arm))}
            :map-phase-candidates
            {:at-inert-doability
             (mapv #(select-keys % [:id :rank :value :doable :epistemic :nats :open-questions])
                   (->> epistemic-default-arm (filter #(= "map" (:epistemic-phase %))) vec))
             :at-live-doability
             (mapv #(select-keys % [:id :rank :value :doable :epistemic :nats :open-questions])
                   (map-phase-rows epistemic-live-arm))}
            :headline
            (let [id "M-web-arxana-missions"
                  pick (fn [rows] (select-keys (first (filter #(= id (:id %)) rows))
                                               [:rank :value :doable :epistemic]))]
              {:id id :arms (update-vals j7-arms pick)})})

;; ---------------------------------------------------------------------------
;; 08 -- controls.

(def blank-phase-arm
  "NEGATIVE CONTROL: the flag on, but every phase stripped off the index the
   repair reads. If the live arm is reading THAT prop and not something else,
   this must reproduce the default arm exactly."
  (let [idx (#'futon2.report.war-machine/mission-doc-index)
        stripped (update-vals idx #(dissoc % :phase))]
    (with-redefs-fn {#'futon2.report.war-machine/mission-doc-index (fn [] stripped)}
      (fn [] (arm declared-weights true)))))

(def env-var-arm
  "The env-var switch and the opt are ONE input: binding the var with no opt
   passed must equal passing the opt."
  (with-redefs-fn {#'futon2.report.war-machine/*live-doability?* true}
    (fn [] (arm declared-weights nil))))

(def default-arm-index (by-id default-arm))

(def controls
  {:c1-default-arm-is-the-inert-state
   {:claim "with no declared input the doable factor is the single 'unknown' 0.3, or 0.0 where the operator gate fires, and :phase is nil on every row"
    :doable-histogram (frequencies (map :doable default-arm))
    :non-nil-phases (count (remove #(nil? (:phase %)) default-arm))
    :pass (and (nil? (some :phase default-arm))
               (every? #(contains? #{0.0 0.3} (double (:doable %))) default-arm))}

   :c2-live-doable-is-the-fiat-table-applied-to-the-read-phase
   {:claim "every live-arm :doable is phase-doability[:phase], except where the operator gate zeroes it -- recomputed here from the table, not read off the row"
    :checked (count live-arm)
    :violations (->> live-arm
                     (remove (fn [{:keys [phase doable operator-gate-factor]}]
                               (== (double doable)
                                   (if (zero? (double (or operator-gate-factor 1.0)))
                                     0.0
                                     (double (get phase-doability (or phase "unknown") 0.3))))))
                     (mapv #(select-keys % [:id :phase :doable :operator-gate-factor])))}

   :c3-value-identity
   {:claim "value = (0.20*central + 0.50*strategic + 0.30*doable-ungated) * completion-gate * operator-gate * decay, recomputed per row in BOTH arms"
    :formula "blend * completion-gate-factor * operator-gate-factor * non-progress-decay"
    :violations
    (vec (for [[label rows] [[:default default-arm] [:live live-arm]]
               {:keys [id value central strategic phase completion-gate-factor
                       operator-gate-factor non-progress-decay]} rows
               :let [ungated (double (get phase-doability (or phase "unknown") 0.3))
                     blend (+ (* 0.20 (double central))
                              (* 0.50 (double strategic))
                              (* 0.30 ungated))
                     predicted (* blend
                                  (double (or completion-gate-factor 1.0))
                                  (double (or operator-gate-factor 1.0))
                                  (double (or non-progress-decay 1.0)))
                     gap (Math/abs (- (double value) predicted))]
               :when (> gap 1.0e-9)]
           {:arm label :id id :value value :predicted predicted :gap gap}))}

   :c4-fabricated-mission-appears-nowhere
   {:claim "a mission id that does not exist appears in no arm and in no move"
    :id "M-u44-fabricated-control"
    :hits (count (filter #(= "M-u44-fabricated-control" (:id %))
                         (concat default-arm live-arm moves)))}

   :c5-blank-phase-reproduces-the-default
   {:claim "flag ON with :phase stripped from the index == the default arm; so the live arm reads mission/phase and not some other carrier"
    :rows (count blank-phase-arm)
    :equal? (= (mapv #(dissoc % :phase-source) blank-phase-arm) default-arm)
    :differing (count (remove (fn [[a b]] (= (dissoc a :phase-source) b))
                              (map vector blank-phase-arm default-arm)))}

   :c6-env-var-equals-the-opt
   {:claim "*live-doability?* bound true with NO opt passed == :live-doability? true"
    :equal? (= env-var-arm live-arm)}

   :c7-not-an-artefact-of-the-declared-weights
   {:claim "the same repair at the SHIPPED default weights moves ranks too"
    :weights shipped-default-weights
    :moved (count (moves-between (arm shipped-default-weights nil)
                                 (arm shipped-default-weights true)))
    :of (count candidates)}

   :c9-the-completion-gate-was-inert-on-the-same-date
   {:claim "the nil phase disabled the completion gate as well as the doability factor: no default row is completion-gated, every live completion-gated row carries phase \"complete\", and no other live row does"
    :default-completion-gated (count (filter #(zero? (double (:completion-gate-factor %))) default-arm))
    :live-completion-gated (count (filter #(zero? (double (:completion-gate-factor %))) live-arm))
    :live-complete-phase (count (filter #(= "complete" (:phase %)) live-arm))
    :pass (and (zero? (count (filter #(zero? (double (:completion-gate-factor %))) default-arm)))
               (= (set (map :id (filter #(zero? (double (:completion-gate-factor %))) live-arm)))
                  (set (map :id (filter #(= "complete" (:phase %)) live-arm)))))}

   :c8-every-mover-has-a-phase-that-explains-it
   {:claim "no row moves rank without its own doable changing, or another row's doable changing above it"
    :movers-with-unchanged-doable
    (->> moves
         (filter #(== (double (:doable-before %)) (double (:doable-after %))))
         count)
    :movers-total (count moves)
    :note "movers whose own doable is unchanged move because rows above them changed; both are reported"}})

(spit-edn! "08-controls.edn"
           {:controls controls
            :all-pass (and (:pass (:c1-default-arm-is-the-inert-state controls))
                           (empty? (:violations (:c2-live-doable-is-the-fiat-table-applied-to-the-read-phase controls)))
                           (empty? (:violations (:c3-value-identity controls)))
                           (zero? (:hits (:c4-fabricated-mission-appears-nowhere controls)))
                           (true? (:equal? (:c5-blank-phase-reproduces-the-default controls)))
                           (true? (:equal? (:c6-env-var-equals-the-opt controls)))
                           (pos? (:moved (:c7-not-an-artefact-of-the-declared-weights controls)))
                           (true? (:pass (:c9-the-completion-gate-was-inert-on-the-same-date controls)))
                           (zero? (:differing default-pin)))})

;; ---------------------------------------------------------------------------
;; Console summary.

(println)
(println (fmt "candidates: %d (pinned from %s)" (count candidates) s2-baseline-path))
(println (fmt "weights: %s" (pr-str declared-weights)))
(println (fmt "default arm doable: %s" (pr-str (frequencies (map :doable default-arm)))))
(println (fmt "live arm    doable: %s" (pr-str (frequencies (map :doable live-arm)))))
(println (fmt "rank moves: %d of %d" (count moves) (count live-arm)))
(println (fmt "rank 1: %s -> %s"
              (:id (first default-arm)) (:id (first live-arm))))
(println (fmt "default pin: %s of %s rows identical to the pre-edit ranking"
              (:identical default-pin) (:rows-compared default-pin)))
(println (fmt "phase agreement through the new path: %s"
              (pr-str (agreement-counts epistemic-live-arm))))
(println (fmt "controls all pass: %s"
              (str (and (:pass (:c1-default-arm-is-the-inert-state controls))
                        (empty? (:violations (:c2-live-doable-is-the-fiat-table-applied-to-the-read-phase controls)))
                        (empty? (:violations (:c3-value-identity controls)))
                        (zero? (:hits (:c4-fabricated-mission-appears-nowhere controls)))
                        (true? (:equal? (:c5-blank-phase-reproduces-the-default controls)))
                        (true? (:equal? (:c6-env-var-equals-the-opt controls)))))))
(println (fmt "top 3 gains: %s"
              (pr-str (mapv (juxt :id :rank-before :rank-after) (take 3 moves)))))
