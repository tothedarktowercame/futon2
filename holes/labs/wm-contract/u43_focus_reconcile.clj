#!/usr/bin/env clojure
;; U43 -- reconciling the focus projection with the witnessed clock edge.
;;
;;   clojure -M holes/labs/wm-contract/u43_focus_reconcile.clj [outdir]
;;
;; WHAT THIS ROW OWES: (a) the divergence cases enumerated, one option chosen
;; with grounds and implemented behind a declared input, default off; (b) the
;; durable clock and the tick's own focus rendered as two distinguishable
;; lines; and a replay showing WHAT EACH DIVERGENCE CASE RECORDS.
;;
;; THE SHIPPED PATH IS WHAT IS MEASURED. Every case below is produced by
;; `war-machine/tick-mission-focus` and rendered by
;; `war-machine/render-mission-focus-line` -- the functions the tick calls --
;; run on fields of the persisted 2026-09-02 trace records. Section 3 is the
;; only place that constructs input, and it says so per row: the corpus records
;; predate the flag and carry no clock edge with a witness source, so the
;; landed/missing discriminator has no live instance to read. Those rows are
;; marked :constructed and their construction is one field, the witness source.
;;
;; NOTHING IS WRITTEN UNDER data/. Replay only, no live tick, no run lock.

(require '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         'futon2.report.war-machine)

(def focus-of
  "The shipped focus resolver, var-quoted because it is private to war_machine.
   scripts/futon2/report/war_machine.clj:1740."
  #'futon2.report.war-machine/tick-mission-focus)

(def render-focus
  "The shipped focus line (U43 half b).
   scripts/futon2/report/war_machine.clj:4068."
  #'futon2.report.war-machine/render-mission-focus-line)

(def render-active
  "The shipped durable-clock line, unchanged by this row.
   scripts/futon2/report/war_machine.clj:4058."
  #'futon2.report.war-machine/render-active-mission-line)

(def selection-focus-flag #'futon2.report.war-machine/*selection-focus?*)
(def reconcile-flag #'futon2.report.war-machine/*focus-reconcile?*)
(def cases futon2.report.war-machine/focus-reconciliation-cases)

(defn fmt [f & args] (apply format f args))

;; ---------------------------------------------------------------------------
;; Corpus -- the same three records U21 read, selected by filename date so a
;; later tick enters without an edit here.

(def trace-dir "data/wm-trace")
(def corpus-from "wm-trace-2026-09-02.edn")

(defn corpus-files []
  (->> (.listFiles (io/file trace-dir))
       (map #(.getName %))
       (filter #(and (str/starts-with? % "wm-trace-")
                     (str/ends-with? % ".edn")
                     (>= (compare % corpus-from) 0)))
       sort
       vec))

(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader (io/file trace-dir f)))]
    (loop [acc []]
      (let [x (read {:eof ::eof :default (fn [_ v] v)} r)]
        (if (= x ::eof) acc (recur (conj acc x)))))))

(def records (vec (mapcat read-records (corpus-files))))

(defn focus
  "The focus map the tick would attach, both flags on."
  [active decision prev]
  (with-bindings {selection-focus-flag true reconcile-flag true}
    (focus-of active decision prev)))

(defn u21-focus
  "The focus map U21 shipped -- reconcile flag OFF -- for the same inputs."
  [active decision]
  (with-bindings {selection-focus-flag true reconcile-flag false}
    (focus-of active decision)))

;; ---------------------------------------------------------------------------
;; 1. The corpus, as it stands. Each record's own `:active-mission` and
;; `:decision`, with the record before it as `prev`.

(defn corpus-rows []
  (vec
   (for [[prev rec] (map vector (cons nil records) records)]
     (let [f (focus (:active-mission rec) (:decision rec) prev)
           r (:reconciliation f)]
       {:run-id (:run/id rec)
        :selected-mission (get-in rec [:decision :action :target])
        :durable-mission (get-in rec [:active-mission :mission-id])
        :durable-reason (get-in rec [:active-mission :reason])
        :u21-agrees-with-durable? (:agrees-with-durable? (u21-focus
                                                          (:active-mission rec)
                                                          (:decision rec)))
        :case (:case r)
        :comparable? (:comparable? r)
        :previous-write-landed? (:previous-write-landed? r)
        :unknown-reason (:unknown-reason r)
        :durable-witness-source (:durable-witness-source r)
        :previous-trace-id (:previous-trace-id r)
        :previous-selected-mission-id (:previous-selected-mission-id r)
        :rendered (str/trim (render-focus f))}))))

;; ---------------------------------------------------------------------------
;; 2/3. Every case in the declared vocabulary, with what produces it. The
;; :source field is the whole honesty of this section: :corpus means the inputs
;; are a persisted record's own fields; :constructed names the ONE field added
;; and why the corpus cannot supply it.

(def diverging-index
  "The index of the first corpus record whose durable clock names a mission AND
   whose decision selects a DIFFERENT one -- the lag U21 measured on 3 of 3
   records. Both sides of every case below come from this ONE record, so the
   pair is a real tick's pair and not two fields taken from two ticks."
  (first (keep-indexed
          (fn [i r]
            (let [d (get-in r [:active-mission :mission-id])
                  s (get-in r [:decision :action :target])]
              (when (and d s (not= d (str s))) i)))
          records)))

(def diverging-record (nth records diverging-index))

(def stale-clock
  "That record's own `:active-mission` -- the PREVIOUS tick's selection."
  (:active-mission diverging-record))

(def mission-decision
  "That record's own `:decision` -- a mission action naming a different mission
   from the one the durable clock holds."
  (:decision diverging-record))

(def prev-record
  "The record immediately before it in the corpus. Its identity is what the
   durable edge's witness source would name had the previous tick's write
   landed."
  (when (pos? diverging-index) (nth records (dec diverging-index))))

(defn case-rows []
  (let [prev-id (or (:run/id prev-record) (:timestamp prev-record))]
    [{:case-key :clock-not-read :source :corpus
      :what "FUTON_WM_CLOCK_FOCUS off: `active-mission` is nil, so the durable
             side was never consulted. U21's boolean reads FALSE here, which an
             operator reads as `the clock names another mission`."
      :active nil :decision mission-decision :prev prev-record}
     {:case-key :durable-unreadable :source :constructed
      :what "The S4 read's own typed failure. Constructed because no record in
             the corpus was taken while the store was unreachable; the value is
             exactly what `read-active-mission-clock` returns
             (war_machine.clj:1549)."
      :active {:ok false :reason :clock-unreadable}
      :decision mission-decision :prev prev-record}
     {:case-key :no-durable-edge :source :corpus
      :what "The successful empty read. Record 0a18c4f7 carries exactly this."
      :active (:active-mission (first records))
      :decision mission-decision :prev prev-record}
     {:case-key :no-selection-this-tick :source :constructed
      :what "A non-mission decision. Constructed because all three corpus
             records select a mission; the decision shape is the one
             `selected-mission-focus` rejects."
      :active stale-clock :decision {:action {:type :address-sorry}}
      :prev prev-record}
     {:case-key :agrees :source :constructed
      :what "Both sides naming the same mission. Constructed by setting the
             durable edge's mission-id to the selected one; no corpus record
             agrees, which is U21's 3-of-3 finding."
      :active (assoc stale-clock :mission-id
                     (get-in mission-decision [:action :target]))
      :decision mission-decision :prev prev-record}
     {:case-key :lagged-write-landed :source :constructed
      :what "The designed one-tick lag: the edge's witness source names the
             PREVIOUS trace record. Constructed by ONE field -- the witness
             source, set to the previous record's own id. The corpus cannot
             supply it: `active-clock-edge->mission` dropped `witness.source`
             until this row, so no persisted `:active-mission` carries it."
      :active (assoc stale-clock :witness-source prev-id)
      :decision mission-decision :prev prev-record}
     {:case-key :lagged-write-missing :source :constructed
      :what "THE CASE THIS ROW EXISTS FOR: the edge's witness source names
             something OTHER than the previous trace record, so the previous
             tick's fire-and-forget write never landed. Constructed by the same
             ONE field, set to an older id."
      :active (assoc stale-clock :witness-source "run-before-the-previous-tick")
      :decision mission-decision :prev prev-record}
     {:case-key :lag-unattributable :source :corpus
      :what "The discriminator cannot run: the edge carries no witness source.
             This is what EVERY corpus record produces today, and it is the
             honest reading of the live corpus -- the divergence is real and its
             cause is not yet measurable."
      :active stale-clock :decision mission-decision :prev prev-record}]))

(defn run-case [{:keys [case-key source what active decision prev]}]
  (let [f (focus active decision prev)
        r (:reconciliation f)]
    {:case-key case-key
     :source source
     :what (str/replace what #"\s+" " ")
     :produced (:case r)
     :matches-declared? (= case-key (:case r))
     :in-vocabulary? (contains? cases (:case r))
     :u21-agrees-with-durable? (:agrees-with-durable? (u21-focus active decision))
     :reconciliation r
     :rendered-focus-line (str/trim (render-focus f))
     ;; `render-active-mission-line` is only ever called by the report when the
     ;; judgement CONTAINS :active-mission, which it does not when the S4 flag
     ;; is off. The :clock-not-read case is exactly that state, so there is no
     ;; durable line to render and the artifact says so rather than inventing
     ;; one.
     :rendered-active-line (if (nil? active)
                             "(no **Active mission:** line -- FUTON_WM_CLOCK_FOCUS off)"
                             (str/trim (render-active active)))}))

(defn -main [& args]
  (let [outdir (io/file (or (first args)
                            "holes/labs/wm-contract/runs/U43-focus-reconcile"))
        sb (StringBuilder.)
        emit (fn [& xs] (.append sb (str (str/join " " xs) "\n")) nil)
        rows (corpus-rows)
        cs (mapv run-case (case-rows))
        unmatched (filterv #(not (:matches-declared? %)) cs)
        out-of-vocab (filterv #(not (:in-vocabulary? %)) cs)
        boolean-collapse (->> cs
                              (filter #(false? (:u21-agrees-with-durable? %)))
                              (map :produced)
                              distinct sort vec)
        artifact
        {:row :U43
         :generated-by "holes/labs/wm-contract/u43_focus_reconcile.clj"
         :corpus {:dir trace-dir :files (corpus-files)
                  :record-count (count records)}
         :seam
         {:flag "scripts/futon2/report/war_machine.clj:125 *focus-reconcile?* / FUTON_WM_FOCUS_RECONCILE"
          :clock-read "scripts/futon2/report/war_machine.clj:1549 read-active-mission-clock"
          :witness-source "scripts/futon2/report/war_machine.clj:1525 active-clock-edge->mission, witness source at :1547"
          :identity "scripts/futon2/report/war_machine.clj:1610 trace-record-identity, stamped by clock-source at :1977"
          :clock-write "scripts/futon2/report/war_machine.clj:1986 record-selection-clock!"
          :vocabulary "scripts/futon2/report/war_machine.clj:1631 focus-reconciliation-cases"
          :reconciler "scripts/futon2/report/war_machine.clj:1669 focus-reconciliation"
          :focus-resolver "scripts/futon2/report/war_machine.clj:1740 tick-mission-focus, called at :6195"
          :render-durable "scripts/futon2/report/war_machine.clj:4058 render-active-mission-line"
          :render-focus "scripts/futon2/report/war_machine.clj:4068 render-mission-focus-line, appended at :4126"}
         :declared-vocabulary (vec (sort cases))
         :corpus-rows rows
         :cases cs
         :controls
         {:every-case-produced-what-it-declared (empty? unmatched)
          :every-case-in-declared-vocabulary (empty? out-of-vocab)
          :u21-boolean-false-covers-these-cases boolean-collapse
          :u21-boolean-false-case-count (count boolean-collapse)}}]
    (.mkdirs outdir)
    (emit "U43 -- focus projection vs witnessed clock edge, reconciled")
    (emit "============================================================")
    (emit "")
    (emit "Corpus:" (str/join ", " (corpus-files)) "--" (count records) "records.")
    (emit "Replay only: no live tick, no run lock, nothing written under data/.")
    (emit "")
    (emit "1. THE SEAM, WITH POINTERS")
    (emit "")
    (doseq [[k v] (sort-by key (:seam artifact))]
      (emit (fmt "   %-16s %s" (name k) v)))
    (emit "")
    (emit "2. THE CORPUS AS IT STANDS")
    (emit "")
    (emit (fmt "   %-9s %-38s %-22s %s" "run" "selected this tick"
               "case" "U21 :agrees?"))
    (doseq [r rows]
      (emit (fmt "   %-9s %-38s %-22s %s"
                 (subs (str (:run-id r)) 0 8)
                 (str (:selected-mission r))
                 (name (or (:case r) :nil))
                 (str (:u21-agrees-with-durable? r)))))
    (emit "")
    (emit "   No corpus record reaches :lagged-write-landed or")
    (emit "   :lagged-write-missing, and that is a finding, not a gap in the")
    (emit "   replay: the S4 read dropped the edge's witness source until this")
    (emit "   row, so every persisted :active-mission is missing the one field")
    (emit "   that discriminates them. On the live corpus the honest answer is")
    (emit "   :lag-unattributable -- the divergence is real and its cause is")
    (emit "   not yet measurable. It becomes measurable on the next tick run")
    (emit "   with FUTON_WM_FOCUS_RECONCILE=1.")
    (emit "")
    (emit "3. EVERY CASE IN THE VOCABULARY, AND WHAT IT RECORDS")
    (emit "")
    (doseq [c cs]
      (emit (fmt "   %s  [%s]" (name (:case-key c)) (name (:source c))))
      (emit (fmt "     %s" (:what c)))
      (emit (fmt "     produced          %s   (declared match: %s; in vocabulary: %s)"
                 (name (or (:produced c) :nil))
                 (:matches-declared? c) (:in-vocabulary? c)))
      (emit (fmt "     comparable?       %s" (:comparable? (:reconciliation c))))
      (emit (fmt "     previous write    %s%s"
                 (str (:previous-write-landed? (:reconciliation c)))
                 (if-let [u (:unknown-reason (:reconciliation c))]
                   (str "  (" (name u) ")") "")))
      (emit (fmt "     U21 :agrees?      %s" (str (:u21-agrees-with-durable? c))))
      (emit (fmt "     renders as        %s" (:rendered-focus-line c)))
      (emit ""))
    (emit "4. THE TWO LINES, NEVER COLLAPSED (half b)")
    (emit "")
    (let [c (first (filter #(= :lagged-write-missing (:case-key %)) cs))]
      (emit (fmt "   %s" (:rendered-active-line c)))
      (emit (fmt "   %s" (:rendered-focus-line c))))
    (emit "")
    (emit "   The first line is `render-active-mission-line`, unchanged by this")
    (emit "   row and byte-identical to what it rendered before. The second is")
    (emit "   `render-mission-focus-line`, appended after it and never folded")
    (emit "   into it. It is present only when the judgement carries")
    (emit "   :mission-focus (U21's flag), and it reads no environment")
    (emit "   variable -- the case clause appears only when the record itself")
    (emit "   carries a :reconciliation.")
    (emit "")
    (emit "5. CONTROLS")
    (emit "")
    (emit (fmt "   every case produced what it declared      %s"
               (:every-case-produced-what-it-declared (:controls artifact))))
    (emit (fmt "   every case in the declared vocabulary     %s"
               (:every-case-in-declared-vocabulary (:controls artifact))))
    (emit (fmt "   U21's `false` covers %d distinct cases:    %s"
               (count boolean-collapse)
               (str/join ", " (map name boolean-collapse))))
    (emit "")
    (emit "   The last line is the row's finding in one number: on these inputs")
    (emit "   `:agrees-with-durable? false` is returned for several different")
    (emit "   states, including one where the durable side was never read. That")
    (emit "   is why the case vocabulary is added BESIDE the boolean rather")
    (emit "   than replacing it -- U21's field is pinned by tests and by a")
    (emit "   committed replay, and a reader that needs the distinction now has")
    (emit "   :case.")
    (emit "")
    (emit "6. WHAT THIS DOES NOT DO")
    (emit "")
    (emit "   It mints NO clock edge, retries NO write, and changes NO focus.")
    (emit "   The chosen option is `record the divergence and let a later tick")
    (emit "   clear it` (:choices :focus-clock-reconciliation): it makes the")
    (emit "   dropped write VISIBLE, it does not repair it.")
    (emit "   record-selection-clock! is still a fire-and-forget future whose")
    (emit "   failure is printed to stderr and dropped; a tick that records")
    (emit "   :lagged-write-missing does not re-attempt the write. Repairing")
    (emit "   the write -- a retry, or a same-tick witnessed edge -- would")
    (emit "   change what a clock edge MEANS and needs its own witness rule")
    (emit "   named, which the row's statement puts out of scope. It remains")
    (emit "   open, and is NOT minted as a row here.")
    (emit "")
    (emit "   The reconciliation is a terminal projection: computed from")
    (emit "   wm-decision and the previous trace record, both already final,")
    (emit "   and attached after ranking and selection. Nothing it records can")
    (emit "   move a rank, a weight, a temperature, an admissibility verdict or")
    (emit "   a selection. Default OFF; flipping it is J-gated.")
    (spit (io/file outdir "U43-FOCUS-RECONCILE.txt") (str sb))
    (spit (io/file outdir "cases.edn")
          (with-out-str (pp/pprint artifact)))
    (print (str sb))
    (flush)
    (when (or (seq unmatched) (seq out-of-vocab))
      (binding [*out* *err*]
        (println "U43: a case did not produce what it declared")
        (pp/pprint {:unmatched (mapv :case-key unmatched)
                    :out-of-vocabulary (mapv :case-key out-of-vocab)}))
      (System/exit 1))))

(apply -main *command-line-args*)
