#!/usr/bin/env bb
;; U41 -- THE TENSION LEDGER: validator, status fold, and the birth-rule query.
;;
;;   bb holes/labs/wm-contract/u41_tension_ledger.bb [outdir]
;;   bb holes/labs/wm-contract/u41_tension_ledger.bb --deposit <run-id>   (RE6)
;;   bb holes/labs/wm-contract/u41_tension_ledger.bb --deposit <run-id> --dry-run
;;
;; FUTON_TENSION_LEDGER redirects every read and the append to another file, so
;; a control can show what a mint does without writing the curated artifact.
;; --deposit refuses while it is set.
;;
;; The report path is read-only over the ledger. `append-tension!` is the sole
;; write API: it validates the existing and proposed ledgers, adds exactly one
;; tension and mint event, and atomically replaces the file. No tick, run lock,
;; substrate call, or network is involved.
;;
;; WHAT IT DOES, in the order the row asks for it:
;;   (a) VALIDATES tension-ledger.edn against the schema DECLARED IN THAT FILE
;;       (:ledger/schema) -- there is no second copy of the shape here, so the
;;       two cannot drift.
;;   (b) FOLDS the events into a current status per tension. The record's
;;       :tension/status is the status at mint; the fold is the current one.
;;   (c) RUNS THE BIRTH-RULE QUERY (DESIGN-tensions-as-patterns.md:79-84): the
;;       same tension resolved the SAME WAY in >= 2 distinct contexts, each with
;;       a typed receipt, is a pattern CANDIDATE. It proposes; it never writes a
;;       pattern. Expected today: zero candidates, and the report says why.
;;   (d) RUNS THE CONTROLS, including the two that make (a) and (c) mean
;;       something: a fabricated tension reads zero everywhere, and a SYNTHETIC
;;       corpus that does satisfy the birth rule yields exactly one candidate --
;;       so a zero here is an absence, not a dead query. (U23's lesson: the
;;       positive control is what caught a live carrier being reported empty.)
;;   (e) VALIDATES U39's worked MINT PAYLOAD against this schema, which is what
;;       "cross-cited, schema not forked" means when it is runnable rather than
;;       asserted.
;;
;; DETERMINISM. The artifact carries no wall-clock field, so two runs over an
;; unchanged ledger are byte-identical.

(require '[babashka.process :as process]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def repo-root ;; derived from the script location (holes/labs/wm-contract under the root) so a worktree run targets its own checkout -- U28z reviewer finding, 2026-09-03
  (-> (java.io.File. *file*) .getAbsoluteFile .getParentFile .getParentFile .getParentFile .getParentFile .getPath))
(def lab (io/file repo-root "holes/labs/wm-contract"))
(def curated-ledger-path (io/file lab "tension-ledger.edn"))

(def ledger-override
  "FUTON_TENSION_LEDGER redirects every read AND the append to another file.
   It exists because `append-tension!` writes, so a control that wants to show
   what a mint does has no way to show it without either mutating the curated
   artifact or reimplementing the writer. `--deposit` REFUSES while it is set
   (see `deposit!`): a run-era row is a claim about the curated ledger, and a
   deposit read off a planted copy would be a false one."
  (System/getenv "FUTON_TENSION_LEDGER"))

(def ledger-path (if ledger-override (io/file ledger-override) curated-ledger-path))

(def terminal-statuses #{:cashed :refuted :dissolved})
(def status-moving #{:carried :cashed :refuted :dissolved})

;; ---------------------------------------------------------------------------
;; (a) Validation, against the schema the ledger declares about itself.
;; ---------------------------------------------------------------------------

(defn- blank-string? [x] (and (string? x) (str/blank? x)))

(defn- check-map
  "Required keys present and non-blank; no key outside required+optional; every
   enum-constrained value inside its declared set."
  [kind spec m]
  (let [{:keys [required optional enums]} spec
        allowed (set (concat required optional))
        id (or (:tension/id m) (:event/id m) m)]
    (concat
     (for [k required :when (or (not (contains? m k)) (nil? (get m k)) (blank-string? (get m k)))]
       {:kind kind :subject id :defect :missing-required-key :key k})
     (for [k (keys m) :when (not (contains? allowed k))]
       {:kind kind :subject id :defect :undeclared-key :key k})
     (for [[k allowed-vals] enums :when (contains? m k)
           :when (not (contains? allowed-vals (get m k)))]
       {:kind kind :subject id :defect :value-outside-declared-set
        :key k :value (get m k) :declared allowed-vals}))))

(defn validate
  "Every rule the ledger's :rules strings state, mechanised. Returns a vector of
   defect maps; empty means the ledger conforms."
  [ledger]
  (let [schema (:ledger/schema ledger)
        tensions (:tensions ledger)
        events (:events ledger)
        t-ids (set (map :tension/id tensions))
        shape (concat (mapcat #(check-map :tension (:tension schema) %) tensions)
                      (mapcat #(check-map :event (:event schema) %) events))
        by-tension (group-by :event/tension events)]
    (vec
     (concat
      shape
      ;; poles are exactly two, both non-blank
      (for [t tensions
            :let [p (:tension/poles t)]
            :when (not (and (vector? p) (= 2 (count p)) (every? #(and (string? %) (not (str/blank? %))) p)))]
        {:kind :tension :subject (:tension/id t) :defect :poles-are-not-two-named-poles :value p})
      ;; ids unique
      (for [[id n] (frequencies (map :tension/id tensions)) :when (> n 1)]
        {:kind :tension :subject id :defect :duplicate-tension-id :n n})
      (for [[id n] (frequencies (map :event/id events)) :when (> n 1)]
        {:kind :event :subject id :defect :duplicate-event-id :n n})
      ;; seq unique and ascending across the whole ledger
      (for [[s n] (frequencies (map :event/seq events)) :when (> n 1)]
        {:kind :event :subject s :defect :duplicate-event-seq :n n})
      (when (not= (map :event/seq events) (sort (map :event/seq events)))
        [{:kind :ledger :subject :events :defect :event-seq-not-ascending
          :value (mapv :event/seq events)}])
      ;; every event names an existing tension
      (for [e events :when (not (contains? t-ids (:event/tension e)))]
        {:kind :event :subject (:event/id e) :defect :event-names-no-such-tension
         :value (:event/tension e)})
      ;; exactly one mint per tension, and it is that tension's lowest seq
      (mapcat
       (fn [t]
         (let [es (sort-by :event/seq (get by-tension (:tension/id t)))
               mints (filter #(= :carried (:event/type %)) es)]
           (concat
            (when (not= 1 (count mints))
              [{:kind :tension :subject (:tension/id t) :defect :not-exactly-one-mint-event
                :n (count mints)}])
            (when (and (seq es) (seq mints) (not= (:event/id (first es)) (:event/id (first mints))))
              [{:kind :tension :subject (:tension/id t) :defect :mint-is-not-the-first-event
                :first (:event/id (first es))}])
            ;; :tension/status is the status AT MINT
            (when (and (seq mints) (not= (:tension/status t) (:event/type (first mints))))
              [{:kind :tension :subject (:tension/id t) :defect :status-field-is-not-the-mint-status
                :field (:tension/status t) :mint (:event/type (first mints))}]))))
       tensions)
      ;; a status move needs evidence AND the row that moved it
      (for [e events
            :when (and (contains? status-moving (:event/type e))
                       (not= :carried (:event/type e)))
            :when (or (empty? (:event/evidence e)) (nil? (:event/row e)))]
        {:kind :event :subject (:event/id e) :defect :status-move-without-row-or-evidence
         :type (:event/type e) :row (:event/row e) :evidence-count (count (:event/evidence e))})
      ;; :evidence-added never moves status
      (for [e events
            :when (and (= :evidence-added (:event/type e)) (contains? (set (keys e)) :event/to-status))]
        {:kind :event :subject (:event/id e) :defect :evidence-added-carries-a-status})
      ;; nothing follows a terminal event for the same tension
      (mapcat
       (fn [[tid es]]
         (let [es (sort-by :event/seq es)
               terminal-at (first (keep-indexed
                                   (fn [i e] (when (contains? terminal-statuses (:event/type e)) i))
                                   es))]
           (when (and terminal-at (some #(contains? status-moving (:event/type %))
                                        (drop (inc terminal-at) es)))
             [{:kind :tension :subject tid :defect :status-move-after-a-terminal-event}])))
       by-tension)))))

(defn append-tension!
  "Validated append-only API. Replaying the identical pair is a no-op; any
  partial/conflicting identity or invalid ledger is refused before writing.

  `:event/seq` IS ASSIGNED BY THIS FUNCTION, so the replay comparison ignores
  it. Until 2026-09-04 it did not, and the documented no-op was unreachable for
  any caller that did not already know the sequence number the ledger would hand
  its event -- which is every caller, since the number is chosen here. U52's
  rung-3 mint is the second caller of this API and hit it on its first replay;
  the first caller (U28z) appended once and never replayed."
  [tension event]
  (let [ledger (edn/read-string (slurp ledger-path))
        existing-defects (validate ledger)
        old-t (some #(when (= (:tension/id tension) (:tension/id %)) %) (:tensions ledger))
        old-e (some #(when (= (:event/id event) (:event/id %)) %) (:events ledger))]
    (when (seq existing-defects)
      (throw (ex-info "existing tension ledger is invalid" {:defects existing-defects})))
    (cond
      (and (= tension old-t)
           (= (dissoc event :event/seq) (dissoc old-e :event/seq)))
      {:status :already-present :tension/id (:tension/id tension)}

      (or old-t old-e)
      (throw (ex-info "refusal identity conflicts with an existing append"
                      {:tension/id (:tension/id tension) :event/id (:event/id event)}))

      :else
      (let [next-seq (inc (reduce max 0 (map :event/seq (:events ledger))))
            candidate (-> ledger
                          (update :tensions conj tension)
                          (update :events conj (assoc event :event/seq next-seq)))
            defects (validate candidate)]
        (when (seq defects)
          (throw (ex-info "proposed tension append is invalid" {:defects defects})))
        (let [tmp (io/file (.getParentFile (.getAbsoluteFile ledger-path))
                           (str (.getName ledger-path) ".u41-append"))]
          (spit tmp (str (with-out-str (pp/pprint candidate))))
          (java.nio.file.Files/move
           (.toPath tmp) (.toPath ledger-path)
           (into-array java.nio.file.CopyOption
                       [java.nio.file.StandardCopyOption/ATOMIC_MOVE
                        java.nio.file.StandardCopyOption/REPLACE_EXISTING])))
        {:status :appended :tension/id (:tension/id tension)}))))

;; ---------------------------------------------------------------------------
;; (b) The fold: current status is the last status-moving event.
;; ---------------------------------------------------------------------------

(defn current-status [ledger tension-id]
  (let [es (->> (:events ledger)
                (filter #(= tension-id (:event/tension %)))
                (sort-by :event/seq))
        moves (filter #(contains? status-moving (:event/type %)) es)]
    {:tension tension-id
     :status (:event/type (last moves))
     :status-since (:event/at (last moves))
     :moved-by-row (:event/row (last moves))
     :events (count es)
     :evidence-events (count (filter #(= :evidence-added (:event/type %)) es))
     :evidence-pointers (vec (mapcat :event/evidence es))}))

;; ---------------------------------------------------------------------------
;; (c) The birth rule, as a QUERY. Proposes only.
;; ---------------------------------------------------------------------------
;;
;; DESIGN-tensions-as-patterns.md:79-84: "when the SAME tension is resolved the
;; SAME way in enough distinct contexts (>=2 with typed receipts), that
;; recurrence is a pattern candidate". Mechanised, the three conjuncts are:
;;   1. the tension REACHED a terminal status (a carried tension has resolved
;;      nothing, so it cannot recur);
;;   2. it shares a :tension/resolution-key with another terminal tension;
;;   3. the contexts (:tension/carried-by) are DISTINCT, and each side carries a
;;      typed receipt -- the status-moving event's own :event/evidence.
;; A candidate is a REPORT LINE. Nothing is written to the pattern library.

(defn birth-rule [ledger]
  (let [statuses (into {} (map (juxt :tension (fn [s] s)))
                       (map #(current-status ledger (:tension/id %)) (:tensions ledger)))
        resolved (->> (:tensions ledger)
                      (filter #(contains? terminal-statuses (:status (get statuses (:tension/id %)))))
                      (filter #(seq (:evidence-pointers (get statuses (:tension/id %))))))
        by-key (group-by :tension/resolution-key resolved)]
    {:tensions-considered (count (:tensions ledger))
     :terminal (count resolved)
     :resolution-keys-seen (into (sorted-map) (map (fn [[k v]] [k (count v)])) by-key)
     :candidates
     (vec (for [[k ts] by-key
                :let [contexts (distinct (map :tension/carried-by ts))]
                :when (and (some? k) (>= (count contexts) 2))]
            {:pattern-candidate/resolution-key k
             :pattern-candidate/contexts (vec contexts)
             :pattern-candidate/tensions (mapv :tension/id ts)
             :pattern-candidate/receipts (vec (mapcat #(:evidence-pointers (get statuses (:tension/id %))) ts))
             :pattern-candidate/status :proposed-only
             :pattern-candidate/note "a person or a reviewed row writes the pattern; this query never does (DESIGN-tensions-as-patterns.md:121-125)"}))}))

;; ---------------------------------------------------------------------------
;; (d)+(e) Controls.
;; ---------------------------------------------------------------------------

(def synthetic-corpus
  "A ledger that DOES satisfy the birth rule, so the query's zero on the real
   ledger is an absence and not a dead code path. Two tensions in two different
   missions, both cashed the same way, each with a receipt."
  {:ledger/schema nil
   :tensions [{:tension/id :synthetic/A :tension/carried-by "M-synthetic-one"
               :tension/resolution-key :measure-before-tuning :tension/status :carried}
              {:tension/id :synthetic/B :tension/carried-by "M-synthetic-two"
               :tension/resolution-key :measure-before-tuning :tension/status :carried}]
   :events [{:event/id :synthetic/A#mint :event/seq 1 :event/tension :synthetic/A
             :event/type :carried :event/at "2026-01-01" :event/by "control" :event/evidence ["control:1"]}
            {:event/id :synthetic/A#cash :event/seq 2 :event/tension :synthetic/A
             :event/type :cashed :event/at "2026-01-02" :event/by "control" :event/row :CTRL
             :event/evidence ["control-receipt-A:1"]}
            {:event/id :synthetic/B#mint :event/seq 3 :event/tension :synthetic/B
             :event/type :carried :event/at "2026-01-01" :event/by "control" :event/evidence ["control:2"]}
            {:event/id :synthetic/B#cash :event/seq 4 :event/tension :synthetic/B
             :event/type :cashed :event/at "2026-01-03" :event/by "control" :event/row :CTRL
             :event/evidence ["control-receipt-B:1"]}]})

(def u39-mint-payload
  "U39's worked mint edge, transcribed from
   runs/U39-selection-retrospective/README.md:265-288 -- the :mint/* keys are
   U39's envelope and the :tension/* keys are the record this row implements.
   Only the :tension/* half is validated here; the envelope is U39's."
  {:tension/id :u39/worked-example
   :tension/poles ["select by the <lost-on> margin as scored this tick"
                   "select by a preference that survives to the next tick"]
   :tension/statement "M-zaif-harness-v1 was preferred over M-expressions-of-interest at 2026-09-02T14:00:29.357627605Z on a G-risk margin of 0.004028678 nats; one tick later M-expressions-of-interest out-ranked it, and its own G-core had fallen by 0.005237161."
   :tension/statement-source "runs/U39-selection-retrospective/README.md:296-300"
   :tension/carried-by "M-wm-aif-policy-grain-compliance"
   :tension/resolution-path "none named at mint"
   :tension/status :carried
   :tension/born-of :refuted-rationale
   :tension/provenance {:who "U39's retrospective evaluator" :when "2026-09-03"
                        :how "minted by a :rationale-refuted verdict"
                        :pointers ["runs/U39-selection-retrospective/README.md:265-288"]}})

;; FORWARD DECLARATION, and it is load order rather than taste: `run-attribution`
;; is defined with the deposit path far below, because that is where it is read,
;; while the two :AD1 controls that pin what its `:cashed?` field now means
;; belong with the other controls. Declaring it here keeps both in their place.
(declare run-attribution)

(defn controls [ledger defects birth]
  (let [ad1-subject
        ;; THE SUBJECT OF THE TWO :AD1 CONTROLS: the LAST tension that has not
        ;; already reached a terminal status. Not simply the last tension --
        ;; `validate` refuses a status move after a terminal event, so planting a
        ;; cashing on an already-cashed tension would make both controls fail for
        ;; a reason that has nothing to do with the reading. `controls` runs over
        ;; whatever ledger it is handed, including the planted ones the (A)-strict
        ;; controls build, so this has to hold for a ledger that already carries a
        ;; cashing and not only for the curated one, which carries none.
        (last (remove #(contains? terminal-statuses
                                  (:status (current-status ledger (:tension/id %))))
                      (:tensions ledger)))
        fake :fabricated/not-a-tension
        fake-status (current-status ledger fake)
        fake-events (filter #(= fake (:event/tension %)) (:events ledger))
        synth (birth-rule synthetic-corpus)
        u39-defects (check-map :tension (get-in ledger [:ledger/schema :tension]) u39-mint-payload)
        broken (dissoc u39-mint-payload :tension/statement)
        broken-defects (check-map :tension (get-in ledger [:ledger/schema :tension]) broken)]
    {:negative/fabricated-tension
     {:subject fake :status (:status fake-status) :events (count fake-events)
      :pass? (and (nil? (:status fake-status)) (zero? (count fake-events)))
      :why "a tension id that exists nowhere must fold to no status and no events; if it did not, the statuses above would be a broken query rather than a fact"}
     :positive/birth-rule-fires-on-a-corpus-that-satisfies-it
     {:candidates (count (:candidates synth))
      :resolution-key (first (keys (:resolution-keys-seen synth)))
      :pass? (= 1 (count (:candidates synth)))
      :why "the real ledger reports zero candidates; this synthetic corpus -- two cashed tensions in two missions sharing a resolution key, each with a receipt -- must report exactly one, so the zero is an absence and not a dead query"}
     :negative/birth-rule-needs-two-distinct-contexts
     {:candidates (count (:candidates (birth-rule (update synthetic-corpus :tensions
                                                          (fn [ts] (mapv #(assoc % :tension/carried-by "M-synthetic-one") ts))))))
      :pass? (zero? (count (:candidates (birth-rule (update synthetic-corpus :tensions
                                                            (fn [ts] (mapv #(assoc % :tension/carried-by "M-synthetic-one") ts)))))))
      :why "the same synthetic corpus with both tensions in ONE mission must report zero: recurrence across contexts is the rule, not recurrence"}
     :positive/u39-mint-payload-validates
     {:defects (vec u39-defects)
      :pass? (empty? u39-defects)
      :why "U39's worked mint payload (README.md:265-288) validates against THIS schema unchanged -- the cross-cite is runnable rather than asserted, and neither row forked the shape"}
     :negative/a-mint-payload-missing-its-statement-is-refused
     {:defects (mapv :defect broken-defects)
      :pass? (some #(= :missing-required-key (:defect %)) broken-defects)
      :why "the validator must refuse a record the schema does not admit; otherwise the control above passes vacuously"}
     :negative/mutated-ledger-is-refused-status-field
     (let [mutated (update ledger :tensions (fn [ts] (assoc-in (vec ts) [0 :tension/status] :cashed)))
           ds (validate mutated)]
       {:defects (mapv :defect ds)
        :pass? (some #(= :status-field-is-not-the-mint-status (:defect %)) ds)
        :why "the real ledger with T1's status field flipped to :cashed and no cashing event must be REFUSED -- otherwise 'the real ledger conforms' is a statement about a validator that accepts anything"})
     :negative/mutated-ledger-is-refused-unevidenced-status-move
     (let [mutated (update ledger :events conj
                           {:event/id :mutant#cash :event/seq 99 :event/tension :zaif-v1/T3
                            :event/type :cashed :event/at "2026-09-03" :event/by "control"
                            :event/evidence []})
           ds (validate mutated)]
       {:defects (mapv :defect ds)
        :pass? (some #(= :status-move-without-row-or-evidence (:defect %)) ds)
        :why "a cashing event with no receipt and no row must be refused -- section 3's 'status moves only with a pointer to the cashing/refuting row', mechanised"})
     :positive/machine-born-statement-is-the-refusal-verbatim
     (let [mb (filter #(= :refused-prediction (:tension/born-of %)) (:tensions ledger))]
       {:count (count mb)
        :pass? (and (seq mb)
                    (every? #(= (clojure.edn/read-string (:tension/statement %))
                                (:tension/refusal %))
                            mb))
        :why "rule clarified 2026-09-03 (U28z review finding): a machine-born tension's statement IS the in-record refusal read back verbatim as data; drift fails here mechanically instead of surviving as prose"})
     :negative/planted-double-mint-is-refused
     (let [t (last (:tensions ledger))
           e (last (:events ledger))
           mutated (-> ledger (update :tensions conj t) (update :events conj e))
           ds (validate mutated)]
       {:defects (mapv :defect ds)
        :pass? (and (some #(= :duplicate-tension-id (:defect %)) ds)
                    (some #(= :duplicate-event-id (:defect %)) ds))
        :why "a planted second mint of the same refusal identity must be refused; append-tension! instead returns :already-present for an exact replay"})
     :negative/the-real-ledger-conforms
     {:defects (count defects)
      :pass? (zero? (count defects))
      :why "the seed corpus is held to the schema it declares"}
     :positive/every-seed-carries-a-pointer
     {:without-pointers (vec (keep (fn [t] (when (empty? (get-in t [:tension/provenance :pointers]))
                                             (:tension/id t)))
                                   (:tensions ledger)))
      :pass? (every? #(seq (get-in % [:tension/provenance :pointers])) (:tensions ledger))
      :why "the row's standard: every claim carries a file:line pointer, a run-record id, or 'not found'"}
     :positive/a-planted-cashing-makes-the-attributed-set-all-cashed
     ;; :AD1. THE READING HAS TO BE ABLE TO SAY GREEN, and on the curated ledger
     ;; it never does: zero :cashed events have ever been written, so every
     ;; attributed tension is :carried and every in-scope run reads :red. A
     ;; reading that can only ever say one thing is not a reading. This plants
     ;; the cashing -- a :cashed event with a row and a receipt, which
     ;; `validate` demands -- on the ledger's LAST tension, points a control run
     ;; at it structurally, and requires the green.
     (let [ctrl-run "ctrl-all-cashed-run"
           t ad1-subject
           tid (:tension/id t)
           declared (assoc-in t [:tension/provenance :records] [ctrl-run])
           cashed-event {:event/id :ctrl/all-cashed#cash
                         :event/seq (inc (reduce max 0 (map :event/seq (:events ledger))))
                         :event/tension tid :event/type :cashed
                         :event/at "2026-09-05" :event/by "control" :event/row :AD1
                         :event/evidence ["control-receipt:1"]}
           planted (-> ledger
                       (update :tensions (fn [ts] (mapv #(if (= tid (:tension/id %)) declared %) ts)))
                       (update :events conj cashed-event))
           att (run-attribution planted ctrl-run [])
           naming (:naming-this-run att)]
       {:subject tid
        :attributed (count naming)
        :statuses (mapv :status naming)
        :defects (count (validate planted))
        :pass? (and (some? t)
                    (= 1 (count naming))
                    (every? :cashed? naming)
                    (empty? (:uncashed att))
                    (zero? (count (validate planted))))
        :why "the input (A)-strict reads -- `:cashed?` on every tension attributed to the run -- must be able to come back ALL-TRUE. The curated ledger has no :cashed event, so without this plant the field would be pinned only on the side it always takes. The plant is a schema-conforming cashing event and the status is the FOLD, so :cashed? is reached through validate and current-status rather than asserted. THE VERDICT ITSELF IS NOT CHECKED HERE and that is deliberate: `deposit-receipt` reds on any failing control, so a verdict computed inside `controls` would either be circular or a second copy of the rule it is meant to guard (holes/TN-edge-review-aif-wiring.md:627-633). The end-to-end green and red are pinned by negative_controls.sh control 8z, which calls the shipped `deposit-receipt`"})
     :negative/without-the-cashing-the-same-attribution-stays-uncashed
     ;; :AD1, the other half. THE SAME PLANT WITH THE CASHING EVENT REMOVED must
     ;; go red, and it must be red for the RUN and not for the tree: defects 0
     ;; and every other control passing, so the only thing separating this from
     ;; the green above is the cashing.
     (let [ctrl-run "ctrl-uncashed-run"
           t ad1-subject
           tid (:tension/id t)
           declared (assoc-in t [:tension/provenance :records] [ctrl-run])
           planted (update ledger :tensions
                           (fn [ts] (mapv #(if (= tid (:tension/id %)) declared %) ts)))
           att (run-attribution planted ctrl-run [])
           naming (:naming-this-run att)]
       {:subject tid
        :attributed (count naming)
        :uncashed (:uncashed att)
        :statuses (mapv :status naming)
        :defects (count (validate planted))
        :pass? (and (some? t)
                    (= 1 (count naming))
                    (not-any? :cashed? naming)
                    (= 1 (count (:uncashed att)))
                    (zero? (count (validate planted))))
        :why "the same planted attribution WITHOUT the cashing event must leave that tension uncashed and in the `:uncashed` set, which is what (A)-strict deposits :red for. Paired with the control above this is the discrimination -- one cashing event is the whole difference -- and `defects 0` on both plants is what says neither side is reached by a broken ledger"})
     :report/birth-rule-is-zero-today
     {:candidates (count (:candidates birth))
      :terminal (:terminal birth)
      :pass? true
      :why "stated, not asserted as a pass: zero candidates BECAUSE zero of the three seed tensions has reached a terminal status. The rule cannot fire on carried tensions, by construction."}}))

;; ---------------------------------------------------------------------------
;; Main
;; ---------------------------------------------------------------------------

(defn -main [& args]
  (let [outdir (io/file (or (first args) (str (io/file repo-root "holes/labs/wm-contract/runs/U41-tension-ledger"))))
        _ (.mkdirs outdir)
        ledger (edn/read-string (slurp ledger-path))
        defects (validate ledger)
        statuses (mapv #(current-status ledger (:tension/id %)) (:tensions ledger))
        birth (birth-rule ledger)
        ;; sorted so the artifact's control order is stable across runs
        ctrls (into (sorted-map) (controls ledger defects birth))
        lines (atom [])
        emit (fn [& xs] (swap! lines conj (apply str xs)))
        report {:reader {:id :u41-tension-ledger :version "v1" :row :U41
                         :script "holes/labs/wm-contract/u41_tension_ledger.bb"
                         :ledger "holes/labs/wm-contract/tension-ledger.edn"
                         :implements "DESIGN-tensions-as-patterns.md:86-101"
                         :read-only true
                         :writes-only-under (.getPath outdir)}
                :schema (:ledger/schema ledger)
                :validation {:defects defects :conforms? (empty? defects)}
                :current-statuses statuses
                :birth-rule birth
                :library-links (:ledger/library-links ledger)
                :cross-cites (:ledger/cross-cites ledger)
                :controls ctrls}]
    (emit "U41 — TENSION LEDGER: carried tensions, typed, folded, and queried")
    (emit "report mode is read-only; the validated append API was not invoked; report writes only under "
          (.getPath outdir))
    (emit "")
    (emit "VALIDATION")
    (emit (format "  %d tensions, %d events, %d defects against the schema the ledger declares"
                  (count (:tensions ledger)) (count (:events ledger)) (count defects)))
    (doseq [d defects] (emit "    " (pr-str d)))
    (emit "")
    (emit "CURRENT STATUS — the fold of the events, not the record's mint-time field")
    (doseq [s statuses]
      (emit (format "  %-16s %-10s since %-12s %d events (%d evidence-added), %d pointers"
                    (str (:tension s)) (str (:status s)) (str (:status-since s))
                    (:events s) (:evidence-events s) (count (:evidence-pointers s)))))
    (emit "")
    (emit "BIRTH RULE — recurrence across contexts proposes a pattern candidate")
    (emit (format "  %d tensions considered, %d terminal, %d candidates"
                  (:tensions-considered birth) (:terminal birth) (count (:candidates birth))))
    (emit "  Zero candidates today, and the reason is structural rather than incidental:")
    (emit "  the rule reads RESOLVED tensions, and all three seeds are carried. The first")
    (emit "  candidate is possible only after two tensions are cashed the same way in two")
    (emit "  missions. The positive control below shows the query firing on a corpus that")
    (emit "  does satisfy it, so this zero is an absence, not a dead query.")
    (emit "")
    (emit "LIBRARY LINKS — linked, not duplicated (DESIGN-tensions-as-patterns.md:66-72)")
    (doseq [l (:ledger/library-links ledger)]
      (emit (format "  %-42s resolved=%-5s %s" (:pattern-id l) (str (:resolved l))
                    (if (:resolved l) (:path l) (str "→ " (:resolved-instead l))))))
    (emit "")
    (emit "CONTROLS")
    (doseq [[k v] ctrls]
      (emit (format "  %-58s pass?=%s" (str k) (pr-str (:pass? v)))))
    (emit "")
    (emit "WHAT IS NOT DONE HERE: no ruling, no status moved by this script, no write to the")
    (emit "pattern library, no registry entry. The ledger proposes; a person or a reviewed")
    (emit "row disposes.")
    (spit (io/file outdir "U41-TENSION-LEDGER.txt") (str (str/join "\n" @lines) "\n"))
    (spit (io/file outdir "tension-report.edn") (with-out-str (pp/pprint report)))
    (println (str/join "\n" @lines))
    (println)
    (println "wrote" (str (.getPath outdir) "/U41-TENSION-LEDGER.txt")
             "and" (str (.getPath outdir) "/tension-report.edn"))
    (when (seq defects) (System/exit 1))
    (when-not (every? :pass? (vals ctrls)) (System/exit 2))))

;; ---------------------------------------------------------------------------
;; --deposit <run-id> -- one run-era ledger row (RE6)
;; ---------------------------------------------------------------------------
;;
;; WHAT VERDICT THIS DEPOSITS, and the finding that decides it: THE TENSION
;; LEDGER CARRIES NO RUN PROVENANCE. Its :event schema
;; (tension-ledger.edn:528-537) has :event/at, :event/by and :event/row and no
;; run field; a tension records :tension/provenance {:who :when :how :pointers}
;; and :tension/carried-by, a MISSION. So no event in the committed ledger can
;; be attributed to a named WM run, and a per-run verdict cannot be read out of
;; it. The deposit says that in a typed absence rather than depositing the
;; deposit-time fold under a run-id it has no claim to.
;;
;; The shape that WOULD carry it exists: U39's tension mint payload writes
;; :tension/provenance {:records [run-id run-id]} (u39_selection_retrospective.bb,
;; section 6c), and since :U60 the ladder's producer mints it too
;; (futon2.aif.task-belief-ladder/refusal-tension). :U60 wires the READ side to
;; match: `run-attribution` below prefers that field and falls back to the
;; substring scan for a tension that carries none, marking which is which.
;;
;; :U60 BUILT THE BASIS AND LEFT THE VERDICT ALONE; :AD1 MOVED THE VERDICT ONTO
;; IT. Until 2026-09-05 `:verdict-deposited` read two substring conditions -- the
;; run id or one of its tick ids appearing anywhere in the ledger TEXT -- which
;; is reading (B) of C511-repair-or-elaborate.md:437-439, "the ledger can
;; attribute a tension to this run", and greens a run for being MENTIONED. It now
;; reads (A)-strict: green iff every tension the ledger attributes to the run has
;; been cashed, :red if any has not, and the typed absence only where nothing is
;; attributed. Adopted by the machine, recorded at aif-equations.edn :choices
;; :tensions-cashed-reading, reversible by re-recording the choice.
;;
;; THE THREE DEPOSITED ROWS ARE NOT REPAIRED. The ledger is append-only
;; (run_era_ledger.bb:241-243) and :AD1's second adoption is that a repair of a
;; deposited run is a RECEIPT and never a row mutation, a new run-id or a
;; supersedes field. So 2026-09-01-s5 and 2026-09-04-re5, deposited
;; :typed-absence, now REPLAY :red, and that divergence is reported beside the
;; ledger rather than written into it.
;;
;; A ledger defect or a failing control deposits :red whatever the run store
;; holds: that failure is about the tree the deposit is made from.

(defn run-tick-ids
  "The tick run/ids of a run, read off its own store's receipt filenames.
   These are what an event would have to name for the ledger to be run-scoped."
  [run-id]
  (let [d (io/file lab "runs" run-id)]
    (when (.isDirectory d)
      (->> (.listFiles d)
           (map #(.getName ^java.io.File %))
           (keep #(second (re-matches #"tick-run-record-\d{4}-\d{2}-\d{2}-(.+)\.edn" %)))
           sort vec))))

(defn structured-run-keys
  "The run identities a tension names STRUCTURALLY: `:tension/provenance
   :records`. That is the field U39's mint payload has always written
   (u39_selection_retrospective.bb section 6c) and the field the ladder's
   producer now mints (`futon2.aif.task-belief-ladder/refusal-tension`).

   STRINGS ONLY. A non-string entry is not a run id and is not coerced into one:
   the point of a structured key is that a reader can tell what it holds, and a
   keyword or a map in there is a defect to be seen rather than stringified into
   a match."
  [t]
  (vec (filter string? (get-in t [:tension/provenance :records]))))

(defn attribute-tension
  "How this tension can be tied to a run, and whether it ties to THIS one.

   STRUCTURED IS PREFERRED AND IS NOT BACKED UP BY PROSE. A tension carrying
   `:records` has said which runs it is about; its pointers and its statement
   are then context, and a run id that happens to appear in one of them is not a
   second, weaker vote. A tension carrying none falls back to the substring scan
   over its printed form -- which is what this check has always done, and which
   is marked `:prose-scan` in the basis rather than presented as the same
   evidence: a substring match cannot tell a run the tension is ABOUT from a run
   it merely mentions.

   STATUS IS THE FOLD, NOT THE RECORD'S FIELD (:AD1). `:cashed?` is read from
   `status`, which the caller computes with `current-status` -- the last
   status-moving EVENT -- and not from `:tension/status`, which is the status
   the tension was minted at and which `validate` refuses to let move. A reading
   of `:tensions-cashed` off the mint-time field could never see a cashing."
  [t sought status]
  (let [structured (structured-run-keys t)
        cashed? (= :cashed (:status status))]
    (if (seq structured)
      (let [hits (vec (filter (set structured) sought))]
        (array-map :tension (:tension/id t) :attribution :structural
                   :declares structured :matched hits
                   :names-this-run? (boolean (seq hits))
                   :status (:status status) :cashed? cashed?))
      (let [text (pr-str t)
            hits (vec (filter #(str/includes? text %) sought))]
        (array-map :tension (:tension/id t) :attribution :prose-scan
                   :matched hits
                   :names-this-run? (boolean (seq hits))
                   :status (:status status) :cashed? cashed?)))))

(defn run-attribution
  "The per-tension basis: which tensions name this run, by which method, and --
   since :AD1 -- whether each has been cashed, which is what the verdict reads.
   Ledger order, not sorted -- tension ids are keywords AND vectors here (the
   zaif rung-3 mint's id is the refusal itself), so there is no total order to
   sort by, and the ledger's own order is already deterministic."
  [ledger run-id tick-ids]
  (let [sought (into [run-id] tick-ids)
        per (mapv #(attribute-tension % sought (current-status ledger (:tension/id %)))
                  (:tensions ledger))
        naming (filterv :names-this-run? per)]
    (array-map
     :method
     (str "prefer the structured field :tension/provenance :records; fall back to a substring "
          "scan of the tension's printed form for a tension that carries none. The fallback is "
          "marked :prose-scan and is what attributes every tension minted before the key existed.")
     :sought sought
     :fold (into (sorted-map) (frequencies (map :attribution per)))
     :naming-this-run naming
     :reading
     (str "(A)-strict, adopted by the machine 2026-09-05 under :AD1 and recorded at "
          "aif-equations.edn :choices :tensions-cashed-reading. A green asserts that every "
          "tension this ledger attributes to the run has been CASHED; an attributed tension that "
          "has not is a :red, because the ledger has FOUND the tension debt rather than failed to "
          "look. A run with no attributed tension is outside the reading's scope and deposits the "
          "typed absence it always did. Reversal: re-record the choice.")
     :uncashed (mapv :tension (remove :cashed? naming))
     :status-fold (into (sorted-map) (frequencies (map :status naming)))
     :per-tension per)))

(defn run-provenance-scan
  "Everything in the ledger that could tie it to this run: the run-id itself,
   any of the run's tick ids, and the run-carrying fields the schema declares.
   A miss on all three is the typed absence's basis, stated as a measurement
   rather than as an assertion about the schema.

   `:run-attribution` (:U60) is carried ONLY when some tension names this run,
   and that placement is the point rather than a detail: this receipt is
   required to be committed and unmodified, and a deposit that rewrites the
   receipt of an already-deposited run turns its replay from :already-present
   into the append-only ledger's divergence refusal. A run no tension names
   therefore gets the same eight fields it got before this row, byte for byte.
   The map is built with `apply array-map` rather than as a literal because a
   literal of nine pairs is a hash-map, which would print in hash order and move
   every existing receipt."
  [ledger-text ledger run-id tick-ids]
  (let [event-keys (vec (sort (distinct (mapcat keys (:events ledger)))))
        tension-keys (vec (sort (distinct (mapcat keys (:tensions ledger)))))
        run-ish (fn [ks] (vec (filter #(re-find #"(?i)run" (str %)) ks)))
        att (run-attribution ledger run-id tick-ids)]
    (apply array-map
           (concat
            [:run-id-appears-in-ledger? (str/includes? ledger-text run-id)
             :tick-ids-sought tick-ids
             :tick-ids-appearing (vec (filter #(str/includes? ledger-text %) tick-ids))
             :event-keys-in-use event-keys
             :tension-keys-in-use tension-keys
             :run-carrying-keys (vec (concat (run-ish event-keys) (run-ish tension-keys)))
             :events-by-date (into (sorted-map) (frequencies (map :event/at (:events ledger))))
             :tension-provenance-shapes
             (vec (sort (distinct (map #(vec (sort (keys (:tension/provenance %)))) (:tensions ledger)))))]
            (when (seq (:naming-this-run att))
              [:run-attribution att])))))

(defn deposit-receipt [run-id ledger defects statuses birth ctrls scan]
  ;; array-map, not a literal: a map literal of this size is a hash-map and
  ;; would print in hash order, so the receipt would not be stable to read.
  (array-map
   :schema :wm/run-era-deposit-receipt-v1
   :row :RE6
   :check :tensions-cashed
   :run-id run-id
   :produced-by "holes/labs/wm-contract/u41_tension_ledger.bb --deposit"
   :deterministic
   (str "No wall-clock field. This receipt is rewritten byte-identically on every deposit, "
        "which is what lets the deposit require it to be committed and unmodified, and lets "
        "the same deposit repeat as :already-present.")
   :run-provenance scan
   ;; (A)-STRICT, adopted by the machine under :AD1 (2026-09-05); the basis is
   ;; `:run-provenance :run-attribution`, which the scan carries exactly when
   ;; some tension names the run. Before :AD1 this read the (B) condition -- the
   ;; run id or one of its tick ids appearing anywhere in the ledger TEXT -- so
   ;; a run was green for being MENTIONED. Under (A)-strict green asserts what
   ;; the check-id says: every attributed tension is cashed.
   :verdict-deposited (let [naming (get-in scan [:run-attribution :naming-this-run])]
                        (cond (seq defects) :red
                              (not (every? :pass? (vals ctrls))) :red
                              (empty? naming) :typed-absence
                              (every? :cashed? naming) :green
                              :else :red))
   :live-derivation
   {:read-from "the tension ledger at deposit time, which names no run"
    :artifact "holes/labs/wm-contract/tension-ledger.edn"
    :tensions (count (:tensions ledger))
    :events (count (:events ledger))
    :defects defects
    :statuses (mapv #(select-keys % [:tension :status :status-since :moved-by-row]) statuses)
    :status-fold (into (sorted-map) (frequencies (map :status statuses)))
    :cashed (mapv :tension (filter #(= :cashed (:status %)) statuses))
    :birth-rule-candidates (count (:candidates birth))
    :controls (into (sorted-map) (map (fn [[k v]] [k (:pass? v)])) ctrls)}
   :why-the-ledger-cannot-be-run-scoped
   (let [carriers (count (filter #(seq (structured-run-keys %)) (:tensions ledger)))]
     (if (zero? carriers)
       ;; VERBATIM the sentence this check has emitted since RE6, because it is
       ;; still the true one while no tension carries the key, and every
       ;; deposited receipt has to keep replaying byte-identically.
       (str "no field of a tension or an event names a run: the keys in use are "
            (pr-str (:event-keys-in-use scan)) " on events and "
            (pr-str (:tension-keys-in-use scan)) " on tensions, of which "
            (pr-str (:run-carrying-keys scan)) " carry a run. The nearest carrier is :event/at, "
            "a DATE, and a date is not provenance: two lanes writing on the same day would both "
            "match. U39's mint payload does carry :tension/provenance {:records [run-id ...]}, and "
            "no committed tension was minted that way.")
       (str carriers " of " (count (:tensions ledger)) " tension(s) name their runs structurally "
            "at :tension/provenance :records, so the sentence this field carried before :U60 -- "
            "that no field of a tension names a run -- is no longer true of the whole ledger. It "
            "remains true of the other " (- (count (:tensions ledger)) carriers)
            ": they are attributed, if at all, by the substring scan marked :prose-scan in "
            ":run-attribution. The event schema still carries no run field; the keys in use are "
            (pr-str (:event-keys-in-use scan)) " on events and "
            (pr-str (:tension-keys-in-use scan)) " on tensions, of which "
            (pr-str (:run-carrying-keys scan)) " carry a run in their own name.")))
   :not-what-this-says
   (str "The live derivation above is the fold of the ledger at deposit time, over tensions "
        "minted by operator dictation on 2026-09-02. It is recorded so the absence is legible, "
        "and it is NOT the deposited verdict.")))

(defn attribution-note
  "The one sentence :U60 adds to a row's notes, and only when there is an
   attribution to name. Appended rather than woven in, so a row deposited
   before this key existed keeps the notes it was deposited with -- the
   run-era ledger compares an existing row's notes field by field."
  [scan]
  (when-let [att (:run-attribution scan)]
    (let [naming (:naming-this-run att)
          by-method (group-by :attribution naming)]
      (str " ATTRIBUTION (:U60): " (count naming) " tension(s) name this run -- "
           (str/join "; "
                     (for [m [:structural :prose-scan]
                           :when (seq (get by-method m))]
                       (str (count (get by-method m)) " " (name m) " ("
                            (str/join ", " (map #(pr-str (:tension %)) (get by-method m))) ")")))
           ". :structural means the tension declares the run at :tension/provenance :records; "
           ":prose-scan means the run id was found as a substring of the record and the tension "
           "declares no runs. The ledger-wide fold is " (pr-str (:fold att))
           ". This names WHICH tensions and by WHAT evidence. Since :AD1 the verdict READS it: "
           "under (A)-strict a green asserts every one of them is cashed, and "
           (pr-str (:uncashed att)) " is the uncashed set that makes it a :red."))))

(defn deposit-notes [run-id receipt]
  (let [d (:live-derivation receipt)
        scan (:run-provenance receipt)]
    (str
     (case (:verdict-deposited receipt)
      :typed-absence
      ;; NO TENSION IS ATTRIBUTED TO THIS RUN, which is a weaker and truer claim
      ;; than the one this branch made before :AD1. It used to say the run-id
      ;; "appears nowhere in tension-ledger.edn"; since :U60 a tension that
      ;; DECLARES its runs at :records is not attributed by its prose, so a run
      ;; can be mentioned in the file and still have nothing attributed to it.
      ;; Both measurements are printed rather than one being inferred from the
      ;; other.
      (str "no tension in the tension ledger is attributed to run " run-id ", so under (A)-strict "
           "there is nothing for a green to be about and nothing for a red to have found. "
           "Measured, not assumed -- the run-id string appears in tension-ledger.edn? "
           (:run-id-appears-in-ledger? scan) "; of the run's " (count (:tick-ids-sought scan))
           " tick ids (" (str/join ", " (:tick-ids-sought scan)) ") the ones appearing are "
           (pr-str (:tick-ids-appearing scan)) "; and of the keys actually in use ("
           (pr-str (:event-keys-in-use scan)) " on events, "
           (pr-str (:tension-keys-in-use scan)) " on tensions) the run-carrying ones are "
           (pr-str (:run-carrying-keys scan)) ". The events carry no run field at all; their "
           "nearest carrier is :event/at, a date, and the ledger's events fall on "
           (str/join ", " (map key (:events-by-date scan))) ". So no tension can be said to "
           "have been cashed BY this run. The fold at deposit time is "
           (pr-str (:status-fold d)) " over " (:tensions d) " tensions and " (:events d)
           " events, with " (:birth-rule-candidates d) " birth-rule candidates; that is a "
           "property of the ledger at deposit time, is recorded in the artifact this row points "
           "at, and is not deposited as this run's verdict.")
      :red
      ;; TWO WAYS TO BE RED AND THEY ARE NOT THE SAME FINDING, so the note says
      ;; which. A defect or a failing control is about the TREE the deposit is
      ;; made from; an uncashed attributed tension is about the RUN, and is the
      ;; (A)-strict reading doing its job.
      (if (or (seq (:defects d)) (not (every? true? (vals (:controls d)))))
        (str "the tension ledger check FAILS at deposit time: " (pr-str (:defects d))
             " defects, controls " (pr-str (:controls d)))
        (let [att (:run-attribution scan)]
          (str "the ledger attributes " (count (:naming-this-run att)) " tension(s) to this run "
               "and " (count (:uncashed att)) " of them are NOT cashed: "
               (pr-str (:uncashed att)) ", status fold " (pr-str (:status-fold att))
               ". Under (A)-strict -- adopted 2026-09-05, aif-equations.edn :choices "
               ":tensions-cashed-reading -- that is a :red and not an absence: the ledger has "
               "FOUND the tension debt. Whole-ledger fold at deposit time "
               (pr-str (:status-fold d)) " over " (:tensions d) " tensions, cashed "
               (pr-str (:cashed d)) ".")))
      :green
      (let [att (:run-attribution scan)]
        (str "every tension the ledger attributes to this run is cashed: "
             (pr-str (mapv :tension (:naming-this-run att)))
             ", status fold " (pr-str (:status-fold att))
             ". Measured too, and kept because it is what the pre-:AD1 (B) reading tested: "
             "run-id in ledger? " (:run-id-appears-in-ledger? scan)
             ", tick ids appearing " (pr-str (:tick-ids-appearing scan))
             "; whole-ledger status fold " (pr-str (:status-fold d))
             ", cashed " (pr-str (:cashed d)))))
     (attribution-note scan))))

(defn deposit! [run-id dry-run?]
  (when (and ledger-override (not dry-run?))
    ;; FAIL CLOSED ON THE WRITE, NOT ON THE REPORT. A row is a claim about the
    ;; curated ledger; deposited off a planted copy it would be a false one, and
    ;; the receipt file would carry the copy's fold under the real run's id.
    ;; --dry-run writes nothing anywhere, so it is allowed and BANNERED instead
    ;; -- refusing it too would leave a control with no way to show what a mint
    ;; does to the basis.
    (println "u41_tension_ledger --deposit: REFUSED -- FUTON_TENSION_LEDGER is set to"
             ledger-override)
    (println "  a deposit is a claim about the curated ledger; unset the override and re-run")
    (System/exit 3))
  (when ledger-override
    (println "LEDGER OVERRIDE:" ledger-override)
    (println "  this is NOT a receipt for the curated tension ledger and may not be deposited"))
  (let [ledger-text (slurp ledger-path)
        ledger (edn/read-string ledger-text)
        defects (validate ledger)
        statuses (mapv #(current-status ledger (:tension/id %)) (:tensions ledger))
        birth (birth-rule ledger)
        ctrls (into (sorted-map) (controls ledger defects birth))
        scan (run-provenance-scan ledger-text ledger run-id (or (run-tick-ids run-id) []))
        r (deposit-receipt run-id ledger defects statuses birth ctrls scan)
        rel (str "holes/labs/wm-contract/runs/RE6-check-deposits/tensions-cashed-" run-id ".edn")
        path (io/file repo-root rel)]
    (when dry-run?
      ;; The receipt and the notes, exhibited and NOT written. A run-era row is
      ;; a claim about an ACCEPTED run, so this is how the citation for a run
      ;; that has not been accepted -- or for one already deposited, whose
      ;; receipt may not be rewritten -- is shown to a reviewer.
      (pp/pprint r)
      (println)
      (println "NOTES:" (deposit-notes run-id r))
      (when ledger-override
        (println "LEDGER OVERRIDE:" ledger-override "-- see the banner above"))
      (println "u41_tension_ledger --deposit --dry-run: verdict" (:verdict-deposited r)
               "-- nothing written, ledger not touched")
      (System/exit 0))
    (io/make-parents path)
    (spit path (with-out-str (pp/pprint r)))
    (println "u41_tension_ledger --deposit: receipt" rel)
    (let [{:keys [exit out err]}
          (process/shell {:dir repo-root :out :string :err :string :continue true}
                         "bb" "holes/labs/wm-contract/run_era_ledger.bb" "--deposit"
                         "--run-id" run-id
                         "--check-id" ":tensions-cashed"
                         "--verdict" (str (:verdict-deposited r))
                         "--artifact" rel
                         "--author" "u41_tension_ledger.bb --deposit"
                         "--deposited-by" "RE6 -- wire the four remaining catalogued checks"
                         "--notes" (deposit-notes run-id r))]
      (print out) (print err) (flush)
      (when-not (zero? exit)
        (println (format "u41_tension_ledger --deposit: the ledger refused the row (exit %d)" exit))
        (println "  if the refusal is artifact-untracked or artifact-dirty, commit" rel "and re-run")
        (System/exit 1))
      (System/exit 0))))

;; RUN AS A SCRIPT, LOADABLE AS A LIBRARY. `append-tension!` is declared above
;; as the ledger's SOLE write API; a second caller that wants it (U52's rung-3
;; refusal mint) has to be able to `load-file` this file without also running
;; the report and its side effects. The guard is babashka's own answer to
;; "am I the file that was invoked".
(when (= *file* (System/getProperty "babashka.file"))
  (if-let [run-id (first (remove #(str/starts-with? % "--")
                                 (rest (drop-while #(not= "--deposit" %) *command-line-args*))))]
    (deposit! run-id (contains? (set *command-line-args*) "--dry-run"))
    (if (contains? (set *command-line-args*) "--deposit")
      (do (println "u41_tension_ledger --deposit needs a run-id") (System/exit 1))
      (apply -main *command-line-args*))))
