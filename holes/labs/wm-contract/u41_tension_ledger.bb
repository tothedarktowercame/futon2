#!/usr/bin/env bb
;; U41 -- THE TENSION LEDGER: validator, status fold, and the birth-rule query.
;;
;;   bb holes/labs/wm-contract/u41_tension_ledger.bb [outdir]
;;
;; READ-ONLY over everything except its own artifact directory (default
;; runs/U41-tension-ledger/). No tick, no run lock, no substrate call, no
;; network. The ledger itself (tension-ledger.edn) is READ, never written: this
;; script cannot append an event, because appending is an authoring act and the
;; append-only discipline is worth more than the convenience.
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

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
(def lab (io/file repo-root "holes/labs/wm-contract"))
(def ledger-path (io/file lab "tension-ledger.edn"))

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

(defn controls [ledger defects birth]
  (let [fake :fabricated/not-a-tension
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
    (emit "U41 — TENSION LEDGER: the three carried tensions, typed, folded, and queried")
    (emit "read-only; the ledger is never written by this script; nothing written outside "
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

(apply -main *command-line-args*)
