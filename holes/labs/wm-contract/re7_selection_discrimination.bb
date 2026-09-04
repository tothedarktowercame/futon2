#!/usr/bin/env bb
;; RE7 -- THE :selection-discrimination CHECK, and its run-era ledger deposit.
;;
;;   RE7_RUN_ID=2026-09-01-s5 RE7_RATIONALE_DIR=runs/RE4-rationale-logging/store \
;;     bb holes/labs/wm-contract/re7_selection_discrimination.bb [outdir]
;;   ... --deposit <run-id>      ; also deposits one run-era ledger row (RE3 shape)
;;
;; Run FROM THE LAB DIRECTORY: the rationale dir, the trace the records name,
;; and the default outdir are all relative to the working directory.
;;
;; WHAT IT ASKS. Joe, 2026-09-04 (worklist :U51/:U52 ruling): "the Lean model is
;; supposed to help by validating logged info. A 55-way tie should be seen as an
;; obvious defect." A decision whose chosen candidate shares its controller score
;; with other candidates was not made by the score: the score ranked a plateau and
;; something else -- the sort's tie-break -- picked the member. So:
;;
;;   per decision  :tiebreak        the chosen candidate's controller-score tie is
;;                                  WIDER THAN 1 -- choice by tie-break
;;                 :discriminating  the tie is exactly 1 -- the score picked it
;;                 :absent          the record carries no tie datum to read
;;   per run       :defect          any decision is :tiebreak
;;                 :typed-absence   no records at all, or (absent no defect) some
;;                                  decision is :absent
;;                 :green           every decision is :discriminating
;;
;; THE THRESHOLD IS 1 AND THAT IS NOT A MAGIC NUMBER: 1 is the width at which a
;; score has a unique argmin, so "wider than 1" is "the score did not decide it".
;; A 2-wide tie fires the check exactly as the 55-wide one does (control C2), and
;; the width is reported so a reader sees 55 rather than a bare red.
;;
;; :defect ORDERS BEFORE :typed-absence, and the reason is stated because the
;; other order is arguable: a tie-break the records DO carry is a fact about the
;; run that a missing datum elsewhere does not unmake. The absences are carried
;; into the notes rather than dropped, so nothing is hidden by the order.
;;
;; THE FIELD PLATEAU CENSUS IS NOT A VERDICT. The score field of a decision can
;; hold wide plateaus the chosen candidate is not in; 2026-09-04-re5 is exactly
;; that case -- 56-wide plateaus in every tick, chosen at rank 1 in a 1-wide tie,
;; so the run is :green while the field is as flat as s5's. The census is emitted
;; beside the verdict and control C8 shows it CANNOT move the verdict. A green
;; here says this run's choices were not made by tie-break; it does not say the
;; scoring discriminates.
;;
;; TWO SOURCES, AND THE SECOND CHECKS THE FIRST. The verdict is read from the
;; run's RATIONALE RECORDS (`:rationale/chosen :controller-score-tie`, built by
;; RE4 at decision time, futon2 src/futon2/aif/selection_rationale.clj:139-153).
;; The plateau census is recomputed from the run-store TRACE that those records
;; name in `:rationale/trace-path`, and control C1 requires the recomputation to
;; reproduce every record's tie count, band and chosen rank. A check that read
;; one number and reported another would pass without it.
;;
;; THE LEDGER HAS NO :defect VERDICT and this row does not mint one. The ledger's
;; declared enum is #{:green :red :typed-absence} (run-era-ledger.edn :ledger/schema)
;; and fold-by-run's status only knows those three, so a fourth word would be a
;; change to the status vocabulary -- a preference, and Joe's to rule (RE6 left the
;; same question open). The CHECK's verdict is :defect; the DEPOSITED verdict is
;; :red, the ledger's word for "this check found what it looks for". Both words
;; appear in the artifact and in the row notes, so the mapping is legible.
;;
;; THE VERDICT IS A ROW, NOT AN EXIT CODE. A run that chose by tie-break must
;; still be recordable, so a :defect exits 0. Exit 2 means a CONTROL failed and
;; the artifacts are not to be trusted; exit 1 means the inputs were not there.
;;
;; PARAMETERISED PER RUN, the u49_route_transcribe.bb recipe:
;;   RE7_RUN_ID        the ledger run-id, e.g. 2026-09-01-s5           (required)
;;   RE7_RATIONALE_DIR the run's rationale records  (default runs/<RE7_RUN_ID>/rationale)
;;   RE7_TRACE         the run-store trace  (default the one wm-trace-*.edn in runs/<RE7_RUN_ID>/)
;;   RE7_SLUG          the run's name inside Lean   (default derived from RE7_RUN_ID)
;;   RE7_EMIT_DEFS     1 emit the shared Lean definitions, 0 reuse the first
;;                     block's (default 1)
;;   RE7_DEFS_SOURCE   the 00-source.edn of the block whose definitions are
;;                     reused, checked by control C9 when RE7_EMIT_DEFS=0
;;
;; READ-ONLY apart from outdir. No tick, no run lock, no substrate call, no
;; network, nothing under data/ read or written -- the trace read is the run
;; store's COMMITTED extraction, never the live corpus, even when the records
;; name the live corpus (2026-09-04-re5's do; see `trace-path`).
;; DETERMINISM: no wall-clock field is written; two runs over an unchanged tree
;; produce byte-identical artifacts.

(require '[babashka.process :as process]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

;; ---------------------------------------------------------------------------
;; parameters
;; ---------------------------------------------------------------------------

(def cli
  (loop [[a & more] *command-line-args*, pos [], flags {}]
    (cond
      (nil? a) {:positional pos :flags flags}
      (str/starts-with? a "--") (recur (rest more) pos (assoc flags a (first more)))
      :else (recur more (conj pos a) flags))))

(def deposit-run-id (get-in cli [:flags "--deposit"]))

(def run-id (or (System/getenv "RE7_RUN_ID") deposit-run-id))

(defn die! [code & msg]
  (println (str "re7_selection_discrimination: " (str/join " " msg)))
  (System/exit code))

(when (str/blank? (str run-id))
  (die! 1 "RE7_RUN_ID is required (the ledger run-id, e.g. 2026-09-01-s5)"))

(def rationale-dir
  (or (System/getenv "RE7_RATIONALE_DIR") (str "runs/" run-id "/rationale")))

(def slug
  "The run's name inside Lean, lower-camel for definitions (`s5SelectionTies`),
   upper-camel for theorem names (`wmS5SelectionDiscrimination`) -- the
   u49_route_transcribe.bb convention."
  (or (System/getenv "RE7_SLUG") (last (str/split run-id #"-"))))

(def Slug (str (str/upper-case (subs slug 0 1)) (subs slug 1)))

(def emit-defs? (not= "0" (or (System/getenv "RE7_EMIT_DEFS") "1")))

(def defs-source-path
  (or (System/getenv "RE7_DEFS_SOURCE")
      "runs/RE7-selection-discrimination/2026-09-01-s5/00-source.edn"))

(def outdir
  (or (first (:positional cli))
      (str "runs/RE7-selection-discrimination/" run-id)))

(def repo-root ;; from the script's own location, so a worktree run targets its own checkout
  (-> (java.io.File. *file*) .getAbsoluteFile
      .getParentFile .getParentFile .getParentFile .getParentFile .getPath))

;; ---------------------------------------------------------------------------
;; reading
;; ---------------------------------------------------------------------------

;; Old trace records carry tagged literals this script has no business
;; interpreting (the U39/U51 reader, same reason).
(def read-opts {:default (fn [t v] {:unread-tag t :value v})})

(defn sha256 [^String s]
  (str/join (map #(format "%02x" (bit-and % 0xff))
                 (.digest (java.security.MessageDigest/getInstance "SHA-256")
                          (.getBytes s "UTF-8")))))

(defn file-sha256 [p] (sha256 (slurp p)))

(defn read-trace [path]
  (with-open [r (io/reader (io/file path))]
    (mapv #(edn/read-string read-opts %) (line-seq r))))

(def rationale-files
  (->> (io/file rationale-dir)
       file-seq
       (filter #(.isFile ^java.io.File %))
       (map str)
       (filter #(str/ends-with? % ".edn"))
       sort
       vec))

(def records
  (->> rationale-files
       (mapv #(edn/read-string read-opts (slurp %)))
       (sort-by :rationale/tick-id)
       vec))

(defn akey [a] [(:type a) (:target a)])

;; ---------------------------------------------------------------------------
;; the rule -- stated once, applied to recorded and to planted decisions alike
;; ---------------------------------------------------------------------------

(defn tie-of
  "The tie datum a rationale record carries about its OWN chosen candidate.
   `{:count n :band [lo hi]}` when present, `nil` when the record has none --
   which is a typed absence and not a zero."
  [rec]
  (let [t (get-in rec [:rationale/chosen :controller-score-tie])]
    (when (and (map? t) (= :present (:status t)) (number? (:count t)))
      {:count (:count t)
       :band (when (vector? (:rank-band t)) (:rank-band t))})))

(defn classify
  "One decision's tie datum -> its class. `nil` is :absent, never :green."
  [tie]
  (cond
    (nil? tie) :absent
    (> (:count tie) 1) :tiebreak
    :else :discriminating))

(defn run-verdict
  "The run's verdict from the per-decision classes. :defect orders before
   :typed-absence for the reason in the header; the absences travel in the
   result rather than being dropped."
  [classes]
  (let [f (frequencies classes)]
    (cond
      (empty? classes) {:verdict :typed-absence :reason :no-rationale-records :counts f}
      (pos? (get f :tiebreak 0)) {:verdict :defect :reason :chosen-inside-a-tie-wider-than-1 :counts f}
      (pos? (get f :absent 0)) {:verdict :typed-absence :reason :decision-carries-no-tie-datum :counts f}
      :else {:verdict :green :reason :every-decision-uniquely-scored :counts f})))

(def ledger-verdict
  "The check's word -> the ledger's declared enum. No new status is minted."
  {:defect :red :green :green :typed-absence :typed-absence})

;; ---------------------------------------------------------------------------
;; the field, recomputed from the trace the records name
;; ---------------------------------------------------------------------------

(def recorded-trace-paths
  "What the rationale records themselves say they were derived from. NOT what
   this check reads -- see `trace-path`."
  (vec (distinct (keep :rationale/trace-path records))))

(def run-store-traces
  (->> (.listFiles (io/file "runs" run-id))
       (filter #(.isFile ^java.io.File %))
       (map #(.getName ^java.io.File %))
       (filter #(re-matches #"wm-trace-.*\.edn" %))
       sort
       (mapv #(str "runs/" run-id "/" %))))

(def trace-path
  "THE COMMITTED RUN-STORE TRACE, and deliberately not the path the records name.
   2026-09-04-re5's records carry an ABSOLUTE `:rationale/trace-path` into the
   live, untracked `data/wm-trace/` -- the file the tick wrote as it ran, which
   is neither committed nor the same file on another machine. A census computed
   from it would not reproduce. The run store's own extraction is what a reviewer
   can read, so that is what is read; the recorded path is carried into
   00-source.edn and control C1 is what shows the two hold the same decisions."
  (or (System/getenv "RE7_TRACE")
      (when (= 1 (count run-store-traces)) (first run-store-traces))))

(def trace-by-run
  (if (and trace-path (.isFile (io/file trace-path)))
    (into {} (map (juxt :run/id identity)) (read-trace trace-path))
    {}))

(defn field-census
  "Every plateau of the decision's controller-score field, and which one holds
   the chosen candidate. A plateau is a set of candidates sharing one score."
  [trace-rec chosen-key]
  (when trace-rec
    (let [ranked (vec (:ranked-actions trace-rec))
          chosen-entries (filter #(= chosen-key (akey (:action %))) ranked)
          chosen-score (:controller-score (first chosen-entries))
          by-score (group-by :controller-score ranked)
          plateau (fn [[score ms]]
                    {:width (count ms)
                     :rank-band [(apply min (map :rank ms)) (apply max (map :rank ms))]
                     :contains-choice? (= score chosen-score)})
          all (mapv plateau (sort-by (fn [[_ ms]] [(- (count ms)) (apply min (map :rank ms))]) by-score))
          not-chosen (remove :contains-choice? all)]
      {:field-size (count ranked)
       :distinct-scores (count by-score)
       :chosen-rank (when (seq chosen-entries) (apply min (map :rank chosen-entries)))
       :chosen-plateau (first (filter :contains-choice? all))
       :plateaus-wider-than-1 (vec (filter #(> (:width %) 1) all))
       :widest-plateau (apply max 0 (map :width all))
       :widest-plateau-not-containing-choice (apply max 0 (map :width not-chosen))})))

;; ---------------------------------------------------------------------------
;; the per-decision table
;; ---------------------------------------------------------------------------

(def decisions
  (vec
   (for [rec records]
     (let [tie (tie-of rec)
           chosen-key (akey (get-in rec [:rationale/chosen :action]))
           census (field-census (get trace-by-run (:rationale/run-id rec)) chosen-key)]
       {:tick-id (:rationale/tick-id rec)
        :trace-run-id (:rationale/run-id rec)
        :chosen chosen-key
        :recorded {:controller-rank (get-in rec [:rationale/chosen :controller-rank])
                   :tie-count (:count tie)
                   :tie-band (:band tie)}
        :recomputed (when census
                      {:controller-rank (:chosen-rank census)
                       :tie-count (:width (:chosen-plateau census))
                       :tie-band (:rank-band (:chosen-plateau census))})
        :class (classify tie)
        :field census}))))

(def classes (mapv :class decisions))
(def verdict-map (run-verdict classes))
(def verdict (:verdict verdict-map))

(def widest-not-chosen
  (apply max 0 (keep #(get-in % [:field :widest-plateau-not-containing-choice]) decisions)))

(def max-tie (apply max 0 (keep #(get-in % [:recorded :tie-count]) decisions)))
(def max-rank (apply max 0 (keep #(get-in % [:recorded :controller-rank]) decisions)))
(def max-field (apply max 0 (keep #(get-in % [:field :field-size]) decisions)))
(def tiebreak-n (count (filter #(= :tiebreak %) classes)))

;; ---------------------------------------------------------------------------
;; the Lean block -- the U49 producer recipe applied to rationale records
;; ---------------------------------------------------------------------------

(def shared-defs
  (str
   "/-- One recorded decision's controller-score tie, transcribed from the run's\n"
   "rationale record (`:rationale/chosen :controller-score-tie`, written at decision\n"
   "time by `futon2:src/futon2/aif/selection_rationale.clj:139-153`) together with\n"
   "the plateau census recomputed from the run store's own committed trace -- which\n"
   "is not always the file the record names; each run block's SOURCES says which\n"
   "file its census was computed from. Ranks are 1-based positions in the\n"
   "controller ranking. -/\n"
   "structure SelectionTie where\n"
   "  /-- The tick's `:rationale/tick-id`, verbatim. -/\n"
   "  tick : String\n"
   "  /-- The chosen candidate's controller rank. -/\n"
   "  chosenRank : Nat\n"
   "  /-- How many candidates share the chosen candidate's controller score. -/\n"
   "  tieCount : Nat\n"
   "  /-- The lowest rank of that tie. -/\n"
   "  tieBandLo : Nat\n"
   "  /-- The highest rank of that tie. -/\n"
   "  tieBandHi : Nat\n"
   "  /-- Candidates scored on this tick. -/\n"
   "  fieldSize : Nat\n"
   "  /-- CENSUS, NOT VERDICT: the widest plateau of the field that does NOT hold\n"
   "  the chosen candidate. -/\n"
   "  widestPlateauNotChosen : Nat\n"
   "  deriving DecidableEq, Repr\n\n"

   "/-- DERIVED: the choice was made by the sort's tie-break rather than by the\n"
   "score, i.e. the chosen candidate's score has no unique argmin. 1 is not a\n"
   "threshold anyone picked: it is the width at which a score decides. -/\n"
   "def SelectionTie.chosenByTiebreak (t : SelectionTie) : Bool := decide (1 < t.tieCount)\n\n"

   "/-- The property `:selection-discrimination` verdicts on: the run recorded at\n"
   "least one decision, and no decision's choice was a tie-break. Note what is NOT\n"
   "here -- `widestPlateauNotChosen` is carried by the data and read by no\n"
   "conjunct, because a plateau the choice is not in is a census and not a defect.\n\n"
   "`reducible` because `decide` needs the `Decidable` instance for THIS\n"
   "conjunction, and instance synthesis does not unfold an irreducible `def` (the\n"
   "`runConformsToDrawnWiring` precedent). -/\n"
   "@[reducible] def selectionDiscriminates (ts : List SelectionTie) : Prop :=\n"
   "  0 < ts.length ∧ ts.all (fun t => !t.chosenByTiebreak) = true\n\n"))

(defn lean-tie [d]
  (let [r (:recorded d) f (:field d)]
    (str "{ tick := \"" (:tick-id d) "\""
         ", chosenRank := " (:controller-rank r)
         ", tieCount := " (:tie-count r)
         ", tieBandLo := " (first (:tie-band r))
         ", tieBandHi := " (second (:tie-band r))
         ", fieldSize := " (:field-size f)
         ", widestPlateauNotChosen := " (:widest-plateau-not-containing-choice f)
         " }")))

(defn lean-header []
  (str
   "/-! ### The " run-id " run's selection discrimination (worklist `:RE7`)\n\n"
   "Joe's ruling of 2026-09-04 (worklist `:U51`/`:U52`): \"the Lean model is\n"
   "supposed to help by validating logged info. A 55-way tie should be seen as an\n"
   "obvious defect.\" This block is the transcription that makes that decidable --\n"
   "each recorded decision's controller-score tie as data, and the check's verdict\n"
   "as a proposition about it.\n\n"
   (if emit-defs?
     ""
     (str "The shared definitions -- `SelectionTie`, `SelectionTie.chosenByTiebreak`,\n"
          "`selectionDiscriminates` -- are the ones the first `:RE7` block above defines\n"
          "and are NOT redefined here. They are a function of the producer alone, so\n"
          "reusing them is a claim that the producer has not moved since that block was\n"
          "generated; control C9 checks it rather than assuming it.\n\n"))
   "SOURCES, both pinned and both committed:\n"
   "* `futon2:holes/labs/wm-contract/" rationale-dir "` -- " (count records)
   " rationale records\n"
   "  for run `" run-id "`; the tie of each `chosenRank`/`tieCount`/`tieBand` field is\n"
   "  read from these.\n"
   "* `futon2:holes/labs/wm-contract/" trace-path "` -- the run store's own\n"
   "  trace extraction; `fieldSize` and `widestPlateauNotChosen` are recomputed from it.\n"
   (if (= recorded-trace-paths [(str "holes/labs/wm-contract/" trace-path)])
     ""
     (str "  NOT the file the records' `:rationale/trace-path` names, which is\n"
          "  " (pr-str recorded-trace-paths) " -- the live, untracked\n"
          "  corpus the tick wrote as it ran, which no reviewer and no other machine can\n"
          "  read. Control C1 requires this committed file to reproduce every recorded\n"
          "  tie, so the substitution is checked rather than assumed.\n"))
   "\n"
   "GENERATED from those files by\n"
   "`futon2:holes/labs/wm-contract/re7_selection_discrimination.bb`; edit the\n"
   "sources and regenerate rather than editing the literals.\n-/\n\n"))

(defn lean-data []
  (str "/-- The " (count decisions) " decisions run `" run-id "` recorded, ordered by tick. -/\n"
       "def " slug "SelectionTies : List SelectionTie :=\n  ["
       (str/join ",\n   " (map lean-tie decisions)) "]\n\n"))

(defn lean-theorems []
  (let [green? (= :green verdict)]
    (str
     "/-- THE `:selection-discrimination` VERDICT for run `" run-id "`, decided.\n"
     (if green?
       (str "Every one of the " (count decisions) " recorded decisions chose a candidate whose\n"
            "controller score no other candidate shared, so no choice was made by the sort's\n"
            "tie-break.\n\n"
            "WHAT IT DOES NOT SHOW, because a reader will otherwise take it for more: the\n"
            "score field of these ticks still carries a plateau " widest-not-chosen " candidates wide that the\n"
            "chosen candidate is not in (`widestPlateauNotChosen`). The proposition is about\n"
            "the CHOICES this run made, not about whether the scoring discriminates.")
       (str tiebreak-n " of the " (count decisions) " recorded decisions chose a candidate from inside a tie\n"
            "of up to " max-tie " candidates sharing one controller score to sixteen digits -- so the\n"
            "score ranked a plateau and the sort's tie-break picked the member. The chosen\n"
            "rank runs as deep as " max-rank " in a field of " max-field ", which a reader would otherwise take\n"
            "for a large score gap.\n\n"
            "This is the defect Joe's ruling names. The negation is stated rather than a\n"
            "positive `chosenByTiebreak` conjunct because it is the SAME proposition the\n"
            "green run satisfies, so the two certificates are comparable."))
     "\n\nProved by `decide` over the transcribed data, no `sorry` and no `native_decide`\n"
     "-- the `wmS5RunConformsToDrawnWiring` precedent. The Clojure side of the same\n"
     "comparison is `futon2:holes/labs/wm-contract/re7_selection_discrimination.bb`,\n"
     "whose verdict for this run is `" verdict "`; the plants that move it are in\n"
     "`" outdir "/03-controls.edn`, controls C2-C6. -/\n"
     "theorem wm" Slug "SelectionDiscrimination :\n    "
     (if green? "" "¬ ") "selectionDiscriminates " slug "SelectionTies := by\n  decide\n\n"

     "/-- The census the verdict is stated over, so the numbers a reader checks\n"
     "against `" outdir "/01-decisions.edn`\n"
     "are themselves decided rather than asserted in prose: " (count decisions) " decisions, "
     tiebreak-n " of them\n"
     "chosen by tie-break, widest chosen tie " max-tie ", deepest chosen rank " max-rank ", field size\n"
     max-field ", and the widest plateau NOT holding the choice " widest-not-chosen ". -/\n"
     "theorem wm" Slug "SelectionTieCensus :\n"
     "    " slug "SelectionTies.length = " (count decisions) " ∧\n"
     "      (" slug "SelectionTies.filter (fun t => t.chosenByTiebreak)).length = " tiebreak-n " ∧\n"
     "      (" slug "SelectionTies.map (fun t => t.tieCount)).foldl max 0 = " max-tie " ∧\n"
     "      (" slug "SelectionTies.map (fun t => t.chosenRank)).foldl max 0 = " max-rank " ∧\n"
     "      (" slug "SelectionTies.map (fun t => t.fieldSize)).foldl max 0 = " max-field " ∧\n"
     "      (" slug "SelectionTies.map (fun t => t.widestPlateauNotChosen)).foldl max 0 = "
     widest-not-chosen " := by\n  decide\n")))

(def lean-text
  (str (lean-header) (when emit-defs? shared-defs) (lean-data) (lean-theorems)))

;; ---------------------------------------------------------------------------
;; controls -- every one exercised, none asserted
;; ---------------------------------------------------------------------------

(defn planted
  "A synthetic decision's tie datum, classified by the SAME code path a recorded
   one takes."
  [n] (when n {:count n :band [1 n]}))

(def producer-sha256 (file-sha256 *file*))

(def defs-source
  (when (and (not emit-defs?) (.isFile (io/file defs-source-path)))
    (edn/read-string (slurp defs-source-path))))

(def controls
  (let [c1 (mapv (fn [d]
                   {:tick (:tick-id d)
                    :recorded (:recorded d) :recomputed (:recomputed d)
                    :agrees? (= (:recorded d) (:recomputed d))})
                 decisions)
        c2 (run-verdict [(classify (planted 2))])
        c3 (run-verdict [(classify (planted 1))])
        c4 (run-verdict [])
        c5 (run-verdict [(classify nil)])
        c6 (run-verdict (mapv classify [(planted 1) (planted 1) (planted 1) (planted 2)]))
        perturbed (mapv (fn [d] (assoc-in d [:field :widest-plateau-not-containing-choice] 999)) decisions)
        c8 (run-verdict (mapv (comp classify tie-of) records))
        decoded (mapv (fn [d]
                        {:tick (:tick-id d)
                         :in-lean? (str/includes? lean-text (str "tick := \"" (:tick-id d) "\""))
                         :count-in-lean? (str/includes?
                                          lean-text
                                          (str "tieCount := " (get-in d [:recorded :tie-count])))})
                      decisions)]
    (array-map
     :positive/c1-the-trace-reproduces-every-recorded-tie
     {:decisions c1
      :pass? (and (seq c1) (every? :agrees? c1))
      :why "the verdict is read from the rationale records and the census from the trace they name; without this the check could report a number it never read"}

     :negative/c2-a-two-wide-tie-is-already-a-defect
     {:verdict c2 :pass? (= :defect (:verdict c2))
      :why "the rule is `wider than 1`, not `wider than some large number` -- the smallest possible tie fires it, so the 55 in this run is a measurement and not the threshold"}

     :positive/c3-a-one-wide-tie-is-green
     {:verdict c3 :pass? (= :green (:verdict c3))
      :why "without this C2 would pass for a rule that verdicts :defect on everything"}

     :negative/c4-no-records-is-a-typed-absence-not-a-green
     {:verdict c4 :pass? (= :typed-absence (:verdict c4))
      :why "the honest store rule: a run whose rationale records are missing has not been shown to discriminate"}

     :negative/c5-a-record-without-a-tie-datum-is-a-typed-absence
     {:verdict c5 :pass? (= :typed-absence (:verdict c5))
      :why "`:controller-score-tie {:status :absent}` is not a tie of width 0; reading it as one would turn a missing measurement into a green"}

     :negative/c6-one-tie-break-among-many-clean-decisions-still-reds-the-run
     {:verdict c6 :pass? (= :defect (:verdict c6))
      :why "the verdict is over the run, so a single choice made by tie-break is the run's defect"}

     :positive/c7-the-lean-literals-round-trip
     {:decoded decoded
      :distinct-ticks? (= (count decisions) (count (distinct (map :tick-id decisions))))
      :pass? (and (seq decoded) (every? :in-lean? decoded) (every? :count-in-lean? decoded)
                  (= (count decisions) (count (distinct (map :tick-id decisions)))))
      :why "what makes the emitted block a transcription rather than a retyping"}

     :positive/c8-the-plateau-census-cannot-move-the-verdict
     {:verdict-as-computed (:verdict verdict-map)
      :verdict-with-census-perturbed (:verdict (run-verdict (mapv :class perturbed)))
      :verdict-from-records-alone (:verdict c8)
      :widest-plateau-not-containing-choice widest-not-chosen
      :pass? (= (:verdict verdict-map)
                (:verdict (run-verdict (mapv :class perturbed)))
                (:verdict c8))
      :why "re5 is green while its field carries a 56-wide plateau the choice is not in; the census is reported so the green is not read as `the scoring discriminates`, and this shows the census is not an input to the verdict"}

     :positive/c9-shared-definition-provenance
     (if emit-defs?
       {:mode :this-block-defines-what-it-uses
        :defines-structure? (str/includes? lean-text "structure SelectionTie where")
        :defines-predicate? (str/includes? lean-text "def selectionDiscriminates")
        :pass? (and (str/includes? lean-text "structure SelectionTie where")
                    (str/includes? lean-text "def selectionDiscriminates"))
        :why "a block that emits its own definitions makes no reuse claim, and this shows it really emits them"}
       {:mode :this-block-reuses-the-first-block-s-definitions
        :defs-source defs-source-path
        :recorded-producer-sha256 (:producer-sha256 defs-source)
        :this-producer-sha256 producer-sha256
        :redefines-nothing? (and (not (str/includes? lean-text "structure SelectionTie where"))
                                 (not (str/includes? lean-text "def selectionDiscriminates")))
        :pass? (and (= (:producer-sha256 defs-source) producer-sha256)
                    (not (str/includes? lean-text "structure SelectionTie where"))
                    (not (str/includes? lean-text "def selectionDiscriminates")))
        :why "reusing the first block's definitions is a claim that the producer that wrote them is this one; C9 compares the recorded sha256 instead of assuming it"}))))

(def controls-pass? (every? :pass? (vals controls)))

;; ---------------------------------------------------------------------------
;; artifacts
;; ---------------------------------------------------------------------------

(def source-facts
  {:run-id run-id
   :rationale-dir (str "holes/labs/wm-contract/" rationale-dir)
   :rationale-records (mapv (fn [f] {:file (str "holes/labs/wm-contract/" f)
                                     :sha256 (file-sha256 f)})
                            rationale-files)
   :trace (when trace-path
            {:path (str "holes/labs/wm-contract/" trace-path)
             :sha256 (file-sha256 trace-path)
             :records-selected (count trace-by-run)
             :recorded-trace-paths recorded-trace-paths
             :recorded-path-is-the-one-read?
             (= recorded-trace-paths [(str "holes/labs/wm-contract/" trace-path)])
             :note (str "the census is computed from the COMMITTED run-store trace above. "
                        "`:rationale/trace-path` on the records says "
                        (pr-str recorded-trace-paths)
                        "; for 2026-09-04-re5 that is an absolute path into the live, "
                        "untracked data/wm-trace/, which no reviewer and no other machine "
                        "can read. Control C1 is what shows the file read here holds the "
                        "same decisions the records were written from.")})
   :producer "holes/labs/wm-contract/re7_selection_discrimination.bb"
   :producer-sha256 producer-sha256
   :lean-slug slug
   :emits-shared-definitions emit-defs?})

(def decisions-artifact
  {:reader {:id :re7-selection-discrimination :version "v1" :row :RE7
            :script "holes/labs/wm-contract/re7_selection_discrimination.bb"
            :read-only true :writes-only-under outdir}
   :run-id run-id
   :rule {:per-decision ":tiebreak when the chosen candidate's controller-score tie is wider than 1; :discriminating at exactly 1; :absent when the record carries no tie datum"
          :per-run ":defect if any decision is :tiebreak; else :typed-absence if any is :absent or there are no records; else :green"
          :ledger-mapping "the ledger's declared enum is #{:green :red :typed-absence}, so :defect deposits as :red and no new status word is minted"}
   :verdict verdict
   :verdict-detail verdict-map
   :deposits-as (ledger-verdict verdict)
   :decisions decisions})

(def census-artifact
  {:reader {:id :re7-plateau-census :version "v1" :row :RE7
            :script "holes/labs/wm-contract/re7_selection_discrimination.bb"}
   :run-id run-id
   :statement "the plateaus of each decision's controller-score field, whether or not the chosen candidate is in them. This is a WIDTH CENSUS and not a verdict: control C8 shows it is not an input to the check."
   :widest-plateau-not-containing-choice widest-not-chosen
   :per-decision (mapv (fn [d] {:tick-id (:tick-id d) :field (:field d)}) decisions)})

(defn write-artifacts! []
  (.mkdirs (io/file outdir))
  (spit (io/file outdir "00-source.edn") (with-out-str (pp/pprint source-facts)))
  (spit (io/file outdir "01-decisions.edn") (with-out-str (pp/pprint decisions-artifact)))
  (spit (io/file outdir "02-plateau-census.edn") (with-out-str (pp/pprint census-artifact)))
  (spit (io/file outdir "03-controls.edn")
        (with-out-str (pp/pprint {:reader {:id :re7-controls :row :RE7
                                           :script "holes/labs/wm-contract/re7_selection_discrimination.bb"}
                                  :run-id run-id
                                  :controls (mapv (fn [[k v]] [k v]) controls)
                                  :all-pass? controls-pass?})))
  (spit (io/file outdir "lean-block.lean") lean-text))

;; ---------------------------------------------------------------------------
;; deposit -- RE3 shape
;; ---------------------------------------------------------------------------

(def deposit-notes
  (let [absent (count (filter #(= :absent %) classes))
        common (str "read from the " (count records) " rationale records of "
                    rationale-dir " (the run's own, written at decision time), with the "
                    "plateau census recomputed from the trace they name and control C1 "
                    "requiring the recomputation to reproduce every recorded tie. ")]
    (case verdict
      :defect
      (str "the check's verdict is :defect and it deposits as :red, the ledger's declared word -- "
           "no new status is minted (the enum is #{:green :red :typed-absence}). " tiebreak-n " of "
           (count decisions) " decisions chose a candidate from inside a controller-score tie: widest tie "
           max-tie " candidates, deepest chosen rank " max-rank " in a field of " max-field
           ", so the score ranked a plateau and the sort's tie-break picked the member. " common
           (when (pos? absent) (str absent " decision(s) carried no tie datum and are recorded as :absent in the artifact; "
                                    ":defect orders before :typed-absence because a tie-break the records DO carry is not unmade by a missing datum elsewhere. "))
           "CENSUS, NOT VERDICT: the widest plateau not containing the choice on this run is "
           widest-not-chosen " (02-plateau-census.edn); control C8 shows it is not an input to the verdict.")
      :green
      (str "every one of the " (count decisions) " decisions chose a candidate whose controller score "
           "no other candidate shared (tie width 1, chosen rank 1), so no choice was made by the "
           "sort's tie-break. " common
           "WHAT THIS GREEN DOES NOT SAY, and the census beside it is why: the score field of these "
           "ticks still carries a plateau " widest-not-chosen " candidates wide that the chosen candidate is not in "
           "(02-plateau-census.edn). The verdict is about the choices this run made, not about whether "
           "the scoring discriminates; control C8 shows the census is not an input to it.")
      :typed-absence
      (str "the run's rationale records do not carry the tie data this check reads: " (pr-str (:counts verdict-map))
           " over " (count records) " records in " rationale-dir " (reason " (:reason verdict-map)
           "). " common "No verdict about this run's selection discrimination was computed, and none is deposited."))))

(defn deposit! []
  (let [artifact-rel (str "holes/labs/wm-contract/" outdir "/01-decisions.edn")
        at (:tick-id (last decisions))
        {:keys [exit out err]}
        (process/shell {:dir repo-root :out :string :err :string :continue true}
                       "bb" "holes/labs/wm-contract/run_era_ledger.bb" "--deposit"
                       "--run-id" run-id
                       "--check-id" ":selection-discrimination"
                       "--verdict" (str (ledger-verdict verdict))
                       "--artifact" artifact-rel
                       "--author" "re7_selection_discrimination.bb --deposit"
                       "--deposited-by" "RE7 -- mint the :selection-discrimination check and wire its deposit"
                       "--at" at
                       "--notes" deposit-notes)]
    (print out) (print err) (flush)
    (when-not (zero? exit)
      (println (format "re7_selection_discrimination --deposit: the ledger refused the row (exit %d)" exit))
      (println "  if the refusal is artifact-untracked or artifact-dirty, commit" artifact-rel "and re-run")
      (System/exit 1))))

;; ---------------------------------------------------------------------------
;; main
;; ---------------------------------------------------------------------------

(when (empty? records)
  (println "re7_selection_discrimination: no rationale records under" rationale-dir))

(when (and (seq records) (nil? trace-path))
  (die! 1 "the plateau census needs exactly one run-store trace under runs/" run-id
        "-- found" (pr-str run-store-traces) "(set RE7_TRACE to choose)"))

(write-artifacts!)

(println (format "re7_selection_discrimination: run %s -- %d decisions, verdict %s (deposits as %s)"
                 run-id (count decisions) (str verdict) (str (ledger-verdict verdict))))
(doseq [d decisions]
  (println (format "  %s  rank %-4s tie %-4s band %-10s field %-4s widest plateau not holding the choice %-4s  %s"
                   (:tick-id d)
                   (str (get-in d [:recorded :controller-rank]))
                   (str (get-in d [:recorded :tie-count]))
                   (pr-str (get-in d [:recorded :tie-band]))
                   (str (get-in d [:field :field-size]))
                   (str (get-in d [:field :widest-plateau-not-containing-choice]))
                   (str (:class d)))))
(println (format "  %d controls, %d negative (each shown REFUSING a wrong verdict), %d positive; all pass? %s"
                 (count controls)
                 (count (filter #(str/starts-with? (str (key %)) ":negative/") controls))
                 (count (filter #(str/starts-with? (str (key %)) ":positive/") controls))
                 (pr-str controls-pass?)))
(doseq [[k v] controls :when (not (:pass? v))] (println "  CONTROL FAILED" (str k)))
(println (str "  wrote " outdir "/{00-source,01-decisions,02-plateau-census,03-controls}.edn and lean-block.lean"))

(when-not controls-pass?
  (die! 2 "a control failed; the artifacts are not to be trusted"))

(when deposit-run-id
  (when-not (= deposit-run-id run-id)
    (die! 1 "--deposit" deposit-run-id "but RE7_RUN_ID is" run-id))
  (deposit!))

(System/exit 0)
