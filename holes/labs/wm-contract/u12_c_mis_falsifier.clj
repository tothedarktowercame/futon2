#!/usr/bin/env clojure
;; U12 -- the C_mis falsifier run (DESIGN-c-vector.md §7) over the live corpus.
;;
;;   clojure -M holes/labs/wm-contract/u12_c_mis_falsifier.clj [outdir]
;;
;; WHAT THIS ROW OWES, and the order it is owed in. U12's :statement names the
;; per-node extractor as the FIRST step, then three measurements from
;; DESIGN-c-vector.md §7: (a) determinism -- risk_mis reproduces from record
;; fields alone; (b) discrimination -- the clocked mission's advance actions
;; carry LOWER risk_mis than non-mission actions on at least one recorded tick,
;; with named numbers; (c) the satisfied-criteria zero on a planted field.
;; §7's own revert condition: "Failure of (b) on all fields = the status-quo
;; forward model is too weak, and the row that flips anything on reverts."
;;
;; THE SHIPPED PATH IS WHAT IS MEASURED, not a re-implementation. Every number
;; below comes from `war-machine/mission-c-readback` (war_machine.clj:1557-1613)
;; or from `futon2.aif.mission-c`'s public fns, called on record fields. The one
;; thing this script constructs is the PLANT: U11 recorded 0 of 9 criteria
;; measurable on the two live fixtures, so clause (b) has no number to compare
;; unless a criterion is given the declared `:observable` U11 named as the
;; repair. The plant is written to disk as an artifact so the reviewer reads
;; the same bytes the measurement did.
;;
;; NOTHING IS WRITTEN UNDER data/. Replay only, no live tick, no run lock.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[futon2.aif.mission-c :as mc]
         '[futon2.aif.preferences :as pref]
         'futon2.report.war-machine)

(import '[java.security MessageDigest])

(def readback
  "The shipped readback, var-quoted because it is private to war_machine.
   war_machine.clj:1557-1613."
  #'futon2.report.war-machine/mission-c-readback)

(def ^:dynamic *criteria-sources*
  "war_machine.clj:1532-1541 -- rebound by the plant so the plant travels the
   shipped path rather than a copy of it."
  futon2.report.war-machine/mission-c-criteria-sources)

(defn fmt [f & args] (apply format f args))

;; ---------------------------------------------------------------------------
;; Corpus

(def trace-dir "data/wm-trace")
(def corpus-from
  "U12 names the three 2026-09-02 records 'plus any newer'. Selecting by
   filename date rather than by a hardcoded id list means a later tick enters
   the corpus without an edit here, and the run says how many it read."
  "wm-trace-2026-09-02.edn")

(defn corpus-files []
  (->> (.listFiles (io/file trace-dir))
       (map #(.getName %))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" %))
       (filter #(>= (compare % corpus-from) 0))
       sort
       (mapv #(str trace-dir "/" %))))

(defn read-records [path]
  (with-open [r (io/reader path)]
    (mapv edn/read-string (line-seq r))))

(defn corpus []
  (->> (corpus-files) (mapcat read-records) (sort-by :timestamp) vec))

(defn short-id [rec] (subs (str (:run/id rec)) 0 8))

(defn sha256 [path]
  (let [md (MessageDigest/getInstance "SHA-256")
        b (with-open [in (io/input-stream path)]
            (let [buf (byte-array 8192)]
              (loop []
                (let [n (.read in buf)]
                  (when (pos? n) (.update md buf 0 n) (recur))))
              (.digest md)))]
    (apply str (map #(fmt "%02x" %) b))))

;; ---------------------------------------------------------------------------
;; 1. The per-node extractor
;;
;; THE MAP IS NOT INVENTED: every node below is a node the record's OWN
;; `:route` names, and the `:via` column is the producer that route entry
;; records. A node the route names but the record carries no field for is a
;; TYPED ABSENCE with the reason, never an omitted row -- an extractor that
;; silently skipped R16 would read as "R16 has no data" when what is true is
;; "no flight has flown".

(def node-map
  [{:node :R2  :field [:observation]
    :via "futon2.aif.observation/observe"
    :basis "record :route R12->R2"}
   {:node :R7  :field [:precision-state]
    :via "futon2.aif.precision/update-precision-state"
    :basis "record :route R2->R7"}
   {:node :R3  :field [:mu-post]
    :via "futon2.report.war-machine/apply-arena-belief-events"
    :basis "record :route R7->R3"}
   {:node :R1  :field [:mu-pre]
    :via "belief prior carried into the tick"
    :basis "record field; not a route hop"}
   {:node :R8  :field [:prediction-errors]
    :via "futon2.aif.free-energy/compute-prediction-error"
    :basis "record :route R3->R8"}
   {:node :R5  :field [:ranked-actions]
    :via "futon2.aif.efe/rank-actions"
    :basis "record :route R8->R5"}
   {:node :R6  :field [:decision]
    :via "futon2.aif.policy/select-action"
    :basis "record :route R5->R6"}
   {:node :R14 :field [:decision :tau]
    :via "futon2.report.war-machine/invoke-strategic-selection"
    :basis "record :route R6->R14"}
   {:node :R9  :field [:preference-stack]
    :via "futon2.aif.preferences/preferences"
    :basis "record field; C_int, the half this row does NOT measure"}
   {:node :S4  :field [:active-mission]
    :via "futon2.report.war-machine/carry-active-mission"
    :basis "war_machine.clj:1524-1526"}
   {:node :R12 :field nil
    :via "futon2.report.war-machine/scan-r12-apparatus"
    :basis "record :route R20->R12"
    :absent :no-record-field}
   {:node :R20 :field nil
    :via "the apparatus scan's input"
    :basis "record :route R20->R12"
    :absent :no-record-field}
   {:node :R16 :field nil
    :via "enactment"
    :basis "S7: R16 enactment data does not exist until a flight flies"
    :absent :no-enactment-yet}])

(defn extract-node
  "One fixture. `:value` present only when the record answers the node."
  [rec {:keys [node field via basis absent]}]
  (let [base {:node node :run/id (:run/id rec) :via via :basis basis}]
    (if (nil? field)
      (assoc base :status :absent :reason absent)
      (let [v (get-in rec field)]
        (if (nil? v)
          (assoc base :status :absent :reason :field-nil-on-record :field field)
          (assoc base :status :present :field field :value v))))))

(defn node-summary
  "A one-line shape for the README table -- a count where the value is a
   collection, the value itself where it is a scalar, so the table is readable
   without opening 39 fixtures."
  [{:keys [status reason value]}]
  (cond
    (= :absent status) (str "absent " reason)
    (map? value) (fmt "map, %d keys" (count value))
    (sequential? value) (fmt "seq, %d entries" (count value))
    :else (pr-str value)))

(defn fixtures-readme
  "S7's shape: fixture -> R node -> record field -> the pointer that says why
   that field is that node. The `:via` column is copied off the record's own
   `:route`, so the mapping is the tick's claim about itself and not this
   script's."
  [rows]
  (str/join
   "\n"
   (concat
    ["# U12 node fixtures"
     ""
     "One fixture per (run-id, node), harvested from `data/wm-trace/` tick records by"
     "`holes/labs/wm-contract/u12_c_mis_falsifier.clj`. Never planted: every value is a"
     "record field, and a node the record cannot answer is a typed absence with a reason"
     "rather than a missing file."
     ""
     "| fixture | node | record field | producer (`:via`) | basis | value |"
     "|---|---|---|---|---|---|"]
    (map (fn [r]
           (fmt "| `%s` | %s | %s | `%s` | %s | %s |"
                (:file r) (name (:node r))
                (if (:field r) (str "`" (pr-str (:field r)) "`") "--")
                (:via r) (:basis r) (:summary r)))
         rows)
    [""])))

(defn write-fixtures! [outdir records]
  (let [dir (io/file outdir "node-fixtures")]
    (.mkdirs dir)
    (let [rows (for [rec records nm node-map]
                 (let [fx (extract-node rec nm)
                       f (io/file dir (fmt "%s-%s.edn" (short-id rec) (name (:node nm))))]
                   (spit f (with-out-str (pp/pprint fx)))
                   (assoc fx :file (str "node-fixtures/" (.getName f))
                          :summary (node-summary fx))))
          rows (vec rows)]
      (spit (io/file dir "README.md") (fixtures-readme rows))
      rows)))

(defn r7-answer
  "U12's :statement asks for R7's location or a typed absence. zaif S7 recorded
   it as ':precision has no top-level key on the record'. Answered here against
   the records themselves rather than restated."
  [records]
  (mapv (fn [rec]
          (let [ps (:precision-state rec)
                obs (:observation rec)]
            {:run/id (:run/id rec)
             :located (if ps :precision-state :not-found)
             :precision-channels (when ps (count ps))
             :observation-channels (count obs)
             :observed-without-precision
             (when ps (vec (sort (remove (set (keys ps)) (keys obs)))))}))
        records))

;; ---------------------------------------------------------------------------
;; 2. The readback on the UNMODIFIED corpus

(defn replay [rec]
  (readback (:active-mission rec) (:ranked-actions rec) (:observation rec)))

;; ---------------------------------------------------------------------------
;; 3. The plant
;;
;; WHAT IT IS AND WHAT IT IS NOT. The plant adds a declared `:observable` to
;; each of the S4 ingest's three criteria -- exactly the repair U11 named for
;; `:unresolved-observable` -- and changes nothing else: the same criterion
;; ids, the same statements, the same absent `:spec` (so each takes
;; `mission-c/default-spec`, `{:becomes 1}`). The observable each criterion is
;; bound to is ARBITRARY and is not a claim about what these criteria mean;
;; three real channels are picked so a number exists where the live corpus has
;; a typed absence. Whether clause (b) passes does not depend on the choice,
;; and the channel sweep below measures that rather than asserting it.

(def plant-bindings
  [[:u-rows-green :support-coverage]
   [:reporting-gate-holds :mission-health]
   [:honest-gap-list-published :annotation-health]])

(defn plant-ingest [src-path bindings]
  (let [ingest (edn/read-string (slurp src-path))
        by-id (into {} bindings)]
    (-> ingest
        (assoc :ingest/note
               (str "U12 PLANT -- not an authority. Copy of " src-path
                    " with one declared :observable added per criterion (the repair"
                    " U11 named for :unresolved-observable). Channel choice is arbitrary;"
                    " see u12_c_mis_falsifier.clj plant-bindings."))
        (update :preferences/c
                (fn [cs] (mapv #(assoc % :observable (get by-id (:criterion %))) cs))))))

(defn write-plant! [outdir filename ingest]
  (let [f (io/file outdir filename)]
    (spit f (with-out-str (pp/pprint ingest)))
    (str f)))

(defn planted-replay
  "The shipped readback, with the criteria source redirected at the plant. Same
   fn, same composition, same absence typing -- only the file it reads changes."
  [rec mission-id plant-path]
  (with-redefs [futon2.report.war-machine/mission-c-criteria-sources
                {mission-id plant-path}]
    (replay rec)))

;; ---------------------------------------------------------------------------
;; 4. The channel sweep -- does risk_mis vary with the WORLD while being
;; constant across CANDIDATES? Public API, same composition the readback uses
;; (mission_c.clj:270-336).

(defn sweep-one-channel [read-result channel observation]
  (let [criteria [{:criterion :swept :statement "swept" :observable channel}]
        c (mc/c-mis (assoc read-result
                           :criteria (mapv #(mc/criterion-row % observation "u12-sweep")
                                           criteria)
                           :status :present))]
    (assoc (select-keys (mc/risk-mis c observation) [:status :risk :reason])
           :channel channel :value (get observation channel))))

;; ---------------------------------------------------------------------------
;; 5. Clause (c) -- the satisfied field

(defn satisfied-observation
  "Every planted observable at its `{:becomes 1}` target. Built from the
   bindings, not hand-listed, so it cannot drift from the plant."
  [bindings]
  (into {} (map (fn [[_ ch]] [ch 1]) bindings)))

(defn unsatisfied-observation
  "`zero` is passed in rather than literal because the long 0 and the double 0.0
   are not the same outcome to `log-preference` -- see §4's type finding."
  [bindings zero]
  (into {} (map (fn [[_ ch]] [ch zero]) bindings)))

;; ---------------------------------------------------------------------------
;; Report

(def lines (atom []))
(defn emit [& ss] (swap! lines conj (str/join "" ss)) nil)

(defn -main [& args]
  (let [outdir (or (first args) "holes/labs/wm-contract/runs/U12-c-mis-falsifier")
        _ (.mkdirs (io/file outdir))
        records (corpus)
        files (corpus-files)]

    (emit "U12 -- the C_mis falsifier (DESIGN-c-vector.md §7), replay only.")
    (emit "")
    (emit "Corpus: " (str/join ", " files))
    (emit (fmt "        %d records: %s" (count records)
               (str/join " " (map short-id records))))
    (emit "Shipped path under test: war-machine/mission-c-readback (war_machine.clj:1557-1613),")
    (emit "composing futon2.aif.mission-c (mission_c.clj:270-376). No live tick, no run lock,")
    (emit "nothing written under data/.")
    (emit "")

    ;; -- 1. extractor ------------------------------------------------------
    (emit "=== 1. PER-NODE EXTRACTOR (U12 :statement, zaif S7's shape) ===")
    (emit "")
    (emit "One fixture per node per tick, written to node-fixtures/. Every node below is a")
    (emit "node the record's OWN :route names (or a record field), and a node the record")
    (emit "cannot answer is a typed absence with the reason, never an omitted row.")
    (emit "")
    (let [rows (write-fixtures! outdir records)]
      (emit (fmt "  %-8s %-5s %-22s %s" "run" "node" "record field" "value"))
      (doseq [r rows]
        (emit (fmt "  %-8s %-5s %-22s %s"
                   (subs (str (:run/id r)) 0 8)
                   (name (:node r))
                   (if (:field r) (pr-str (:field r)) "--")
                   (:summary r))))
      (emit "")
      (emit (fmt "  %d fixtures (%d present, %d typed absence)."
                 (count rows)
                 (count (filter #(= :present (:status %)) rows))
                 (count (filter #(= :absent (:status %)) rows)))))
    (emit "")
    (emit "R7's location, which U12's :statement asks for by name. zaif S7 carried it as a")
    (emit "named gap -- \":precision has no top-level key on the record\". FOUND, not absent:")
    (doseq [a (r7-answer records)]
      (emit (fmt "  %s  %s  %s precision channels of %s observed; observed-without-precision: %s"
                 (subs (str (:run/id a)) 0 8) (name (:located a))
                 (:precision-channels a) (:observation-channels a)
                 (pr-str (:observed-without-precision a)))))
    (emit "")
    (emit "So the gap S7 records is not that R7 is unlocated but that R7 covers 8 of the 14")
    (emit "channels R2 observes -- update-precision-state carries an error history only for")
    (emit "channels with one, and the six without are absent from R7 rather than at precision 0.")
    (emit "")

    ;; -- 2. the unmodified corpus -----------------------------------------
    (emit "=== 2. THE UNMODIFIED CORPUS: WHAT risk_mis IS ON IT ===")
    (emit "")
    (let [rbs (mapv (fn [rec] [rec (replay rec)]) records)]
      (doseq [[rec rb] rbs]
        (emit (fmt "  %s  %-38s %s"
                   (short-id rec)
                   (or (:mission rb) "--")
                   (fmt "%s %s" (:status rb) (or (:reason rb) ""))))
        (when (:criteria-source rb)
          (emit (fmt "            source %s" (:criteria-source rb)))
          (emit (fmt "            shape %s; %d criteria, %d measurable%s"
                     (:criteria-shape rb) (:criterion-count rb) (:measurable-count rb)
                     (if (:criteria-reason rb)
                       (str "; :criteria-reason " (:criteria-reason rb)) "")))
          (emit (fmt "            %d mission actions, %d non-mission"
                     (:mission-action-count rb) (:non-mission-action-count rb))))
        (doseq [u (:unmeasurable rb)]
          (emit (fmt "            %-26s %-24s %s"
                     (:criterion u) (:reason u) (:source u)))))
      (emit "")
      (emit (fmt "  %d of %d records carry a risk_mis NUMBER: %d."
                 (count (filter #(= :measured (:status (second %))) rbs))
                 (count rbs)
                 (count (filter #(= :measured (:status (second %))) rbs))))
      (emit "  0 of 9 criteria measurable is U11's finding, reproduced here from the same")
      (emit "  files; this row's job is what follows from it.")

      ;; -- 3. clause (a) ---------------------------------------------------
      (emit "")
      (emit "=== 3. CLAUSE (a): DETERMINISM -- reproduces from record fields alone ===")
      (emit "")
      (let [second-pass (mapv (fn [[rec _]] (replay rec)) rbs)
            same? (= (mapv second rbs) second-pass)]
        (emit (fmt "  Two passes of mission-c-readback over the same %d records, called with"
                   (count rbs)))
        (emit "  (:active-mission, :ranked-actions, :observation) and nothing else:")
        (emit (fmt "    identical: %s" (if same? "YES (all fields, =)" "NO")))
        (emit "")
        (emit "  BUT THE READBACK IS NOT A FUNCTION OF RECORD FIELDS ALONE, and that is the")
        (emit "  finding of this clause. The criteria arrive from a FILE on disk, named by")
        (emit "  war_machine.clj:1532-1541 or by the candidate's :action :mission-path. The")
        (emit "  record carries the PATH (:criteria-source) and no content hash, so a replay")
        (emit "  after that file is edited returns different numbers with no way to tell.")
        (emit "  The sources this replay actually read, hashed here so the run is pinnable:")
        (doseq [[_ rb] rbs]
          (when-let [p (:criteria-source rb)]
            (emit (fmt "    %s  %s"
                       (if (.exists (io/file p)) (subs (sha256 p) 0 16) "MISSING        ")
                       p))))
        (emit "")
        (emit "  VERDICT (a): PASSES as stated for the shipped code -- the readback is a pure")
        (emit "  function of its three arguments and reproduces exactly. It does NOT pass the")
        (emit "  stronger reading a replay needs: 'from record fields alone' is false while the")
        (emit "  criteria source is a mutable path with no recorded digest. The repair is one")
        (emit "  field, :criteria-source-sha256, on the readback map."))

      ;; -- 4. clause (b) ---------------------------------------------------
      (emit "")
      (emit "=== 4. CLAUSE (b): DISCRIMINATION ===")
      (emit "")
      (emit "  §7 asks: do the clocked mission's advance actions carry LOWER risk_mis than")
      (emit "  non-mission actions on at least one recorded tick, with named numbers?")
      (emit "")
      (emit "  ON THE CORPUS AS IT STANDS: unanswerable, and typed as such. Every record's")
      (emit "  risk_mis is :absent :no-measurable-criteria (§2 above), so there is no number")
      (emit "  on either side of the comparison. That is a typed absence, not a low score.")
      (emit "")
      (emit "  SO THE ROW PLANTS the repair U11 named -- a declared :observable per criterion")
      (emit "  -- and asks the question again where a number exists.")
      (emit "")
      (let [src (get futon2.report.war-machine/mission-c-criteria-sources "M-zaif-harness-v1")
            plant (plant-ingest src plant-bindings)
            plant-path (write-plant! outdir "planted-criteria-declared-observable.edn" plant)
            clocked (filterv (fn [rec] (:mission-id (:active-mission rec))) records)]
        (emit (fmt "  Plant: %s" plant-path))
        (emit (fmt "         %s + one :observable per criterion: %s"
                   src (pr-str (into {} plant-bindings))))
        (emit "         BOTH clocked records are pointed at this ONE file, including the one")
        (emit "         whose own mission has no criteria section. The mission's identity is")
        (emit "         therefore held fixed on purpose: what varies between the two rows")
        (emit "         below is the tick, not the criteria, so a difference in risk_mis")
        (emit "         could only come from the tick's observation or its candidates.")
        (emit "")
        (doseq [rec clocked]
          (let [mission (:mission-id (:active-mission rec))
                rb (planted-replay rec mission plant-path)
                per (:per-mission-action rb)
                by-scope (group-by :scope per)]
            (emit (fmt "  %s  clocked %s" (short-id rec) mission))
            (emit (fmt "        status %s; %d criteria, %d measurable; weights %s"
                       (:status rb) (:criterion-count rb) (:measurable-count rb)
                       (:weight-basis rb)))
            (when (:risk-mis-per-criterion rb)
              (doseq [pc (:risk-mis-per-criterion rb)]
                (emit (fmt "          %-26s %-20s value %-22s surprisal %-22s w %.6f  contrib %.10f"
                           (:criterion pc) (:observable pc) (:value pc)
                           (:surprisal pc) (:weight pc) (:contribution pc)))))
            (when per
              (emit (fmt "        risk_mis over %d mission actions: %d distinct value(s) %s"
                         (count per)
                         (:distinct-risk-values (:action-sensitivity rb))
                         (pr-str (vec (sort (into #{} (map :risk-mis) per))))))
              (emit (fmt "          clocked-mission actions %d, other-mission actions %d,"
                         (count (:clocked-mission by-scope)) (count (:other-mission by-scope))))
              (emit (fmt "          non-mission actions %d -- and non-mission actions carry NO"
                         (:non-mission-action-count rb)))
              (emit "          risk_mis field at all (war_machine.clj:1584-1592 maps only")
              (emit "          mission-action? entries).")
              (let [cl (into #{} (map :risk-mis) (:clocked-mission by-scope))
                    ot (into #{} (map :risk-mis) (:other-mission by-scope))]
                (emit (fmt "          clocked set %s vs other-mission set %s -- %s"
                           (pr-str (vec (sort cl))) (pr-str (vec (sort ot)))
                           (if (= cl ot) "IDENTICAL" "DIFFERENT")))))
            (emit "")))
        (emit "  VERDICT (b): FAILS, and fails STRUCTURALLY rather than numerically.")
        (emit "  Three separate reasons, each measurable above; the sweep below adds a fourth.")
        (emit "   1. Non-mission actions have no risk_mis to compare against. The comparison")
        (emit "      §7 names is between a number and an absent field.")
        (emit "   2. Among mission actions the number is CONSTANT -- 1 distinct value over all")
        (emit "      of them -- because v0's forward model is Q(o|π) = status quo for every π")
        (emit "      (mission_c.clj:337-342), so no candidate moves any observable.")
        (emit "   3. It does not even separate the CLOCKED mission's actions from other")
        (emit "      missions' actions: both scopes carry the same single value. :scope")
        (emit "      records the difference; the number does not.")
        (emit "  §7's revert condition is met: 'Failure of (b) on all fields = the status-quo")
        (emit "  forward model is too weak, and the row that flips anything on reverts.'")
        (emit "  NOTHING IS FLIPPED ON TODAY -- FUTON_WM_MISSION_C defaults off")
        (emit "  (war_machine.clj:1615-1621), so the revert is a no-op and the finding is a")
        (emit "  gate on U13 rather than a rollback.")

        ;; -- the sweep -----------------------------------------------------
        (emit "")
        (emit "  IS ONLY THE FORWARD MODEL DEAD, OR THE MEASUREMENT TOO? The plant's channel")
        (emit "  choice is arbitrary, so the constancy above could be an artefact of it. The")
        (emit "  sweep below asks the weaker question -- does risk_mis vary with the WORLD at")
        (emit "  all? -- one criterion at a time over every channel R2 observes, same")
        (emit "  composition the readback uses (mission_c.clj:317-376):")
        (emit "")
        (let [rec (last clocked)
              obs (:observation rec)
              rr {:version mc/version :mission (:mission-id (:active-mission rec))
                  :source "u12-sweep" :criteria [] :status :present}
              sw (mapv #(sweep-one-channel rr % obs) (sort (keys obs)))
              distinct-risks (into (sorted-set) (map :risk) sw)]
          (emit (fmt "    record %s" (short-id rec)))
          (doseq [s sw]
            (emit (fmt "      %-22s value %-24s risk_mis %s"
                       (:channel s) (:value s)
                       (if (= :measured (:status s)) (:risk s)
                           (str (:status s) " " (:reason s))))))
          (emit "")
          (emit (fmt "    %d distinct risk_mis value(s) over %d channels: %s"
                     (count distinct-risks) (count sw) (pr-str (vec distinct-risks))))
          (emit "")
          (emit "    THIS IS THE SECOND FINDING OF THE ROW AND IT IS INDEPENDENT OF THE")
          (emit "    FORWARD MODEL. risk_mis does not vary with the world either, and the")
          (emit "    cause is a type comparison rather than a modelling choice. Every")
          (emit "    undeclared criterion takes `{:becomes 1}` (mission_c.clj:57-65, and no")
          (emit "    criterion in the corpus declares a :spec), so the term goes through")
          (emit "    log-preference's Bernoulli branch (preferences.clj:242-244):")
          (emit "")
          (emit "        (if (or (= 0 x) (false? x)) (- 1.0 p1) p1)")
          (emit "")
          (emit "    `(= 0 x)` is Clojure value equality, which is FALSE across number classes:")
          (emit "    it holds for the long 0 and not for the double 0.0. R2's observation")
          (emit "    vector is doubles throughout")
          (emit "    (:consulting-pct 0.0, :sorry-count-norm 0.0, :ticks-firing-ratio 0.0 on")
          (emit "    this record), so no observed value can select the non-target branch.")
          (emit "    Measured on the shipped fn:")
          (let [d (pref/c-distribution {:becomes 1})]
            (doseq [x [(long 0) 0.0 1.0 0.5 -3.0 false nil]]
              (emit (fmt "      log-preference {:becomes 1} %-8s (%-17s) = %s"
                         (pr-str x)
                         (if (nil? x) "nil" (.getName (class x)))
                         (pref/log-preference d x)))))
          (emit "")
          (emit "    So under the default spec a criterion bound to any R2 channel reads")
          (emit "    SATISFIED at every value the machine can observe, 0.0 and a negative one")
          (emit "    included. `nil` -- an absent channel -- also reads satisfied, which is")
          (emit "    the C130 discipline inverted: an unread observable scores as a met")
          (emit "    criterion rather than as a typed absence.")
          (emit "")
          (emit "    SCOPE, so this is not read wider than it is: grepped across every .clj")
          (emit "    under ~/code, log-preference has exactly two production callers,")
          (emit "    mission_c.clj:333 and mission_c.clj:370, plus this script and tests.")
          (emit "    C_int and the c-vector lane score Bernoulli outcomes through `pref/kl`")
          (emit "    with an explicit `{:kind :bernoulli :p q}` (c_vector.clj:681-684,")
          (emit "    preferences.clj:297-299) and")
          (emit "    never take this path. The exposure is U11's new code and nothing older.")
          (emit "    mission_c_test.clj:197-205 pins the branch with the LONG 0, so it passes")
          (emit "    while the live vocabulary cannot reach the value it pins -- the test and")
          (emit "    the corpus disagree about what an outcome is.")
          (emit "")
          (emit "    A range :spec goes through the other branch, gives a real gradient (§5")
          (emit "    has the numbers) and is not affected. None of the corpus's criteria")
          (emit "    declare one."))

        ;; -- 5. clause (c) -------------------------------------------------
        (emit "")
        (emit "=== 5. CLAUSE (c): THE SATISFIED-CRITERIA ZERO ===")
        (emit "")
        (emit "  §7 asks that risk_mis 'go to zero as criteria are met on a planted field where")
        (emit "  all criteria read satisfied'. Planted field: every planted observable at its")
        (emit "  {:becomes 1} target, built from the bindings so it cannot drift from them.")
        (emit "  The unsatisfied field is shown twice, once at the long 0 and once at the")
        (emit "  double 0.0, because §4's type finding says those are not the same outcome.")
        (emit "")
        (let [rec (last clocked)
              mission (:mission-id (:active-mission rec))
              read-result (mc/read-criteria plant-path :observables (:observation rec)
                                            :mission mission)
              c (mc/c-mis read-result)
              sat (satisfied-observation plant-bindings)
              unsat (unsatisfied-observation plant-bindings 0)
              unsat-d (unsatisfied-observation plant-bindings 0.0)
              r-sat (mc/risk-mis c sat)
              r-unsat (mc/risk-mis c unsat)
              r-unsat-d (mc/risk-mis c unsat-d)
              r-live (mc/risk-mis c (:observation rec))
              ratio (/ (double (:risk r-unsat)) (double (:risk r-sat)))]
          (emit (fmt "    all criteria SATISFIED     (%s)  risk_mis = %s"
                     (pr-str sat) (:risk r-sat)))
          (emit (fmt "    UNSATISFIED, long 0        (%s)  risk_mis = %s"
                     (pr-str unsat) (:risk r-unsat)))
          (emit (fmt "    UNSATISFIED, double 0.0    (%s)  risk_mis = %s"
                     (pr-str unsat-d) (:risk r-unsat-d)))
          (emit (fmt "    the live observation of %s                                   risk_mis = %s"
                     (short-id rec) (:risk r-live)))
          (emit "")
          (emit (fmt "    zero? %s. The satisfied value is a FLOOR set by temperature alone,"
                     (if (zero? (double (:risk r-sat))) "YES" "NO")))
          (emit (fmt "    −ln c* where c* = 1/(1+e^(−1/T)) = %s at T = %s"
                     (:p1 (pref/c-distribution {:becomes 1})) pref/default-c-temperature))
          (emit "    (preferences.clj:216-221). The literal zero needs T → 0.")
          (emit "")
          (emit (fmt "    AND THE UNSATISFIED POLE IS NOT REACHABLE FROM THE MACHINE'S OWN"))
          (emit (fmt "    OBSERVATION VOCABULARY: at the double 0.0 the same field scores %s,"
                     (:risk r-unsat-d)))
          (emit "    identical to the satisfied field, for §4's reason. The 10.0 above needs a")
          (emit "    long or a boolean, and no R2 channel produces one. So the ratio below is")
          (emit "    what the two POLES are worth in principle, not a spread the live path can")
          (emit "    produce.")
          (emit "")
          (emit "    A RANGE SPEC IS A DIFFERENT PROBLEM AND IT IS REACHABLE. For :spec [lo hi]")
          (emit "    the term is −ln C(x) = gap/T + ln Z, and inside the range that is ln Z,")
          (emit "    which is NEGATIVE for any range narrower than [0,1]:")
          (doseq [spec [[0.0 1.0] [0.5 1.0] [0.4 0.6] [0.8 1.0]]]
            (let [d (pref/c-distribution spec)
                  x (/ (+ (first spec) (second spec)) 2.0)]
              (emit (fmt "      spec %-12s at x = %-6s  risk_mis = %s"
                         (pr-str spec) x (- (pref/log-preference d x))))))
          (emit "")
          (emit "    No criterion in the corpus declares a range :spec, so this is reachable")
          (emit "    and unexercised rather than a live wrong number. It matters for U13")
          (emit "    because C_int's risk is a KL (≥ 0, preferences.clj:277-299) while")
          (emit "    risk_mis is a surprisal: summing them in one G would let a satisfied")
          (emit "    range criterion pay a BONUS, which is not what a cost term may do.")
          (emit "")
          (emit "    VERDICT (c): FAILS AS STATED, AND THE FAILURE IS BIGGER THAN THE FLOOR.")
          (emit (fmt "    Satisfied is %s, not 0 -- that part is a temperature floor and would"
                     (:risk r-sat)))
          (emit (fmt "    be a nit on its own, %.0f× below the long-0 unsatisfied value %s."
                     ratio (:risk r-unsat)))
          (emit "    What defeats the clause is that on the machine's own doubles the")
          (emit "    satisfied and unsatisfied fields score the SAME number, so 'goes to zero")
          (emit "    as criteria are met' is not measurable on the live path at all: nothing")
          (emit "    there is ever unmet. The completion gate §3 argues for does not exist yet.")

          ;; machine-readable
          (spit (io/file outdir "measurements.edn")
                (with-out-str
                  (pp/pprint
                   {:row :U12
                    :design "DESIGN-c-vector.md §7"
                    :corpus {:files files :record-count (count records)
                             :run-ids (mapv :run/id records)}
                    :criteria-source-sha256
                    (into {} (for [[_ rb] rbs :when (:criteria-source rb)]
                               [(:criteria-source rb)
                                (when (.exists (io/file (:criteria-source rb)))
                                  (sha256 (:criteria-source rb)))]))
                    :unmodified-corpus
                    (mapv (fn [[rec rb]]
                            {:run/id (:run/id rec)
                             :mission (:mission rb)
                             :status (:status rb) :reason (:reason rb)
                             :criterion-count (:criterion-count rb)
                             :measurable-count (:measurable-count rb)})
                          rbs)
                    :clause-a {:verdict :pass-as-stated
                               :qualification :criteria-source-is-an-unhashed-path}
                    :clause-b {:verdict :fail
                               :reasons [:non-mission-actions-carry-no-risk-mis
                                         :constant-across-mission-actions
                                         :clocked-and-other-mission-scopes-identical
                                         :constant-across-the-world-too-see-defect]}
                    :clause-c {:verdict :fail
                               :satisfied (:risk r-sat)
                               :unsatisfied-long-0 (:risk r-unsat)
                               :unsatisfied-double-0.0 (:risk r-unsat-d)
                               :pole-ratio ratio
                               :reasons [:satisfied-is-a-temperature-floor-not-zero
                                         :unsatisfied-pole-unreachable-from-double-observables]}
                    :defect
                    {:where "src/futon2/aif/preferences.clj:242-244"
                     :what "log-preference's Bernoulli branch selects the non-target outcome with (= 0 x), true for the long 0 and false for the double 0.0"
                     :effect "every R2 observation value, 0.0 and negatives included, and nil, read as the target outcome"
                     :production-callers ["src/futon2/aif/mission_c.clj:333"
                                          "src/futon2/aif/mission_c.clj:370"]
                     :not-exposed "C_int and the c-vector lane use pref/kl with an explicit {:kind :bernoulli :p q} (src/futon2/aif/c_vector.clj:682-684)"
                     :test-gap "test/futon2/aif/mission_c_test.clj:197-205 pins the branch with the long 0"
                     :owner :not-fixed-by-U12}
                    :plant {:path plant-path :bindings (into {} plant-bindings)}})))

          ;; -- verdict block -------------------------------------------------
          (emit "")
          (emit "=== 6. VERDICT, one line per §7 clause ===")
          (emit "")
          (emit "  (a) determinism    PASS with a named qualification -- mission-c-readback is a")
          (emit "                     pure function of the three record fields and reproduces")
          (emit "                     exactly, but the criteria arrive from an unhashed path, so")
          (emit "                     'from record fields alone' is not yet true of a replay.")
          (emit "  (b) discrimination FAIL on every field, for four independent reasons (§4):")
          (emit "                     non-mission actions carry no risk_mis at all; among")
          (emit "                     mission actions it is one constant value; clocked and")
          (emit "                     other-mission scopes carry the SAME value; and the")
          (emit "                     Bernoulli type defect makes it constant in the world too.")
          (emit (fmt "  (c) satisfied zero FAIL. Satisfied is %s, not 0; and on the"
                     (:risk r-sat)))
          (emit "                     machine's double-valued observables the unsatisfied field")
          (emit "                     scores the same number, so the gate is not measurable.")
          (emit "")
          (emit "  §7's revert condition is MET by (b). Nothing is flipped on -- FUTON_WM_MISSION_C")
          (emit "  defaults off (war_machine.clj:1615-1621) -- so the revert is a no-op today,")
          (emit "  and what it binds is U13: the :choices :c-grain entry must record the v0")
          (emit "  binding as MEASURED NON-DISCRIMINATING rather than as an untested limitation.")
          (emit "")
          (emit "  ONE DEFECT FOUND AND NOT FIXED HERE (U12 measures; it does not repair src):")
          (emit "  preferences.clj:242-244's `(= 0 x)`. Two production callers, both in U11's")
          (emit "  new code (mission_c.clj:333, :370). The repair is a decision about what an")
          (emit "  outcome IS for a Bernoulli criterion -- `==`, a threshold, or a refusal to")
          (emit "  score a continuous observable against a Bernoulli C at all -- and that is a")
          (emit "  design choice, not a typo to patch inside a measurement row.")))

    (let [text (str (str/join "\n" @lines) "\n")
          out (io/file outdir "U12-FALSIFIER.txt")]
      (spit out text)
      (print text)
      (flush)))))

(apply -main *command-line-args*)
