#!/usr/bin/env bb
;; U49 -- TRANSCRIBE THE 2026-09-01-s5 RUN'S ROUTE AND THE DRAWN WIRING INTO LEAN.
;;
;;   bb holes/labs/wm-contract/u49_route_transcribe.bb [outdir]
;;   bb holes/labs/wm-contract/u49_route_transcribe.bb --deposit <run-id> [outdir]
;;       ; also deposits one run-era ledger row (RE3)
;;
;; Both forms are run FROM THE LAB DIRECTORY: run-dir and the default outdir are
;; relative to the working directory.
;;
;; Joe's RUN4 ruling (2026-09-03, worklist.edn :run4-lean-ruling) refuses the
;; "permanent external attestation" reading of `wmRunConformsToWiring`: "all
;; that's really needed here is to run the machine and see if it conforms to
;; the wiring that we drew. And that should be something we can validate in
;; Lean." This script is the transcription half of that validation. It reads
;;
;;   - the drawn control map, p4ng empirics-futon/control-map-edges.edn, and
;;   - the pinned run runs/2026-09-01-s5 (futon2 sha 5a66411, RUN3
;;     :verdict :conformant, 36 hops),
;;
;; and emits the Lean literal block that mathlib4
;; DarkTower/WarMachine/Holes.lean holds, plus the artifacts a reviewer needs
;; to check that the literal says what the record and the map say.
;;
;; THE COMPARISON TRANSCRIBED IS RUN3's, NOT A NEW ONE. run3_conformance.bb is
;; what produced the pinned :verdict :conformant, so its decision rule is the
;; one the certificate has to reproduce: a hop is classified by the SET of
;; grounds on which its pair was retired (:code at route grain -> refutation;
;; :code at dependency grain, i.e. the pair is also in :route-measured-drawn ->
;; excluded; :ruling -> the ruling is not realised in code), and only then by
;; the drawn and measured layers. A flat "no retired edge traversed" would
;; report this run NOT conformant, because s5 traverses R2->R7 (code-retired,
;; dependency grain) and R5->R6 (ruling-retired) -- see control C5.
;;
;; READ-ONLY. No tick, no run lock, no substrate call, no network, and nothing
;; is written outside outdir. In particular it does NOT re-run
;; run3_conformance.bb, which would rewrite the run's conformance.edn and
;; runs/latest-conformance.edn with a fresh :checked-at.
;;
;; DETERMINISM. No wall-clock field is written; two runs over an unchanged map
;; and an unchanged run directory produce byte-identical artifacts.
;;
;; WHAT IT WRITES (outdir defaults to runs/U49-run-conformance):
;;   00-source.edn        the identities: control map path/sha256/commit, the
;;                        run directory, its README sha, its extracted trace's
;;                        sha256, and the four :run/id values.
;;   01-topology.edn      the drawn, measured and retired tables as transcribed.
;;   02-routes.edn        the four recorded routes and their reassembled hops.
;;   03-classification.edn this script's own classification of every distinct
;;                        hop, the class census, and the unfired drawn edges.
;;   04-controls.edn      the controls (C1-C6 below).
;;   lean-block.lean      the generated Lean literal block.
;;   README.md            what the run found, with pointers.
;;
;; CONTROLS.
;;   C1 this script's classifier reproduces the run's PINNED conformance.edn
;;      -- hops, distinct, drawn, unfired, excluded, ruling-unrealised,
;;      refutations, unmapped, verdict -- at the CURRENT control map. A
;;      transcription that did not reproduce the pinned verdict would be
;;      transcribing a different comparison.
;;   C2 round-trip: every emitted Lean constructor name decodes back to the
;;      node it came from, the names are distinct, and the decoded routes equal
;;      the recorded routes. This is what makes the literal a transcription
;;      rather than a retyping.
;;   C3 a fabricated node occurs in no route, no edge table and nowhere in the
;;      emitted Lean text.
;;   C4 NEGATIVE, four mutations that must each break the conformance
;;      predicate: (a) the R99->R100 hop wm_route_conformance.clj --negative
;;      plants, appended to a route -> unmapped; (b) a route emptied ->
;;      the non-empty conjunct fails; (c) R2->R3 traversed -- code-retired and
;;      NOT in :route-measured-drawn, so route grain -> refutation; (d) every
;;      route emptied out of the table -> the non-empty table conjunct fails.
;;      Without these the certificate would be a check that cannot fail.
;;   C5 THE TWO CHECKERS DISAGREE ON THE DRAWN SET, and the transcription
;;      follows run3 because run3 produced the pinned verdict: run3 takes ALL
;;      of :edges regardless of :status (22, including the one :unresolved
;;      self-loop R5->R5), while checks/wm_route_conformance.clj filters to
;;      :status :drawn (21). Inert on this run -- R5->R5 is not traversed --
;;      and reported rather than silently resolved.
;;   C6 WHAT THE CERTIFICATE DOES NOT SHOW: 5 of the 9 distinct hops are on the
;;      :route-measured-drawn layer, which is the layer a previous route
;;      MEASUREMENT put there; and 19 of the 22 drawn edges never fired. So the
;;      certificate says this run stayed inside the union of the drawn and
;;      measured layers, not that the drawn figure predicted the run.

(load-file "../../../src/futon2/aif/run4_route_conformance.clj")

(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[babashka.fs :as fs]
         '[babashka.process :as process]
         '[futon2.aif.run4-route-conformance :as route-core])

(def control-map-path
  (or (System/getenv "U49_CONTROL_MAP")
      "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn"))

(def run-dir
  (or (System/getenv "U49_RUN_DIR")
      "runs/2026-09-01-s5"))

(def trace-dir
  (or (System/getenv "FUTON_WM_TRACE_DIR")
      "/home/joe/code/futon2/data/wm-trace"))

(def cli
  "Positional arguments and flag pairs, split. An invocation with no flags parses
   exactly as it did before: the first positional is still the outdir."
  (loop [[a & more] *command-line-args*, pos [], flags {}]
    (cond
      (nil? a) {:positional pos :flags flags}
      (str/starts-with? a "--") (recur (rest more) pos (assoc flags a (first more)))
      :else (recur more (conj pos a) flags))))

(def outdir (or (first (:positional cli)) "runs/U49-run-conformance"))

(def repo-root ;; from the script's own location, so a worktree run targets its own checkout
  (-> (java.io.File. *file*) .getAbsoluteFile
      .getParentFile .getParentFile .getParentFile .getParentFile .getPath))

(def deposit-run-id (get-in cli [:flags "--deposit"]))

;; ------------------------------------------------------- run identity ------
;;
;; RE5: the run is a parameter, not a literal. `U49_SLUG` names the run inside
;; Lean -- `<slug>Routes`, `wm<Slug>RunConformsToDrawnWiring` -- and defaults to
;; `s5`, so an invocation with no environment set emits U49's block character
;; for character. `U49_EMIT_TABLES=0` suppresses the SHARED definitions
;; (`RouteNode` .. `runConformsToDrawnWiring`), which a second run's block must
;; not redefine; the tables are a function of the control map, and a run whose
;; block omits them is asserting that the map has not moved -- which control C7
;; below checks rather than assumes.

(def run-name (last (str/split run-dir #"/")))

(def slug (or (System/getenv "U49_SLUG") "s5"))

(def Slug (str (str/upper-case (subs slug 0 1)) (subs slug 1)))

(def trace-file
  (or (System/getenv "U49_TRACE_FILE") (str run-dir "/wm-trace-" slug ".edn")))

(def emit-tables? (not= "0" (System/getenv "U49_EMIT_TABLES")))

(def controls-ref
  "Where the emitted docstring tells a reader to find the exercised mutations.
   A LITERAL, never the outdir: the outdir is an invocation argument, and
   putting it in the generated text made the block differ between two runs that
   generate the same certificate (the defect RE3's review caught in the RE2
   report and self-test)."
  (or (System/getenv "U49_CONTROLS_REF") "runs/U49-run-conformance"))

(def row-ref
  "The worklist row this certificate is produced under. U49 built the machinery;
   a later row that mints a certificate for its own run says so."
  (or (System/getenv "U49_ROW") ":U49"))

;; --------------------------------------------------------------- reading ---

(defn sha256 [path]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (str/join (map #(format "%02x" %) (.digest d (fs/read-all-bytes path))))))

(defn read-forms [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [acc []]
      (let [f (edn/read {:eof ::eof} r)]
        (if (= ::eof f) acc (recur (conj acc f)))))))

(defn git-sha [repo relpath]
  (let [{:keys [exit out]} (process/shell {:dir repo :out :string :continue true}
                                          "git" "log" "-1" "--format=%h" "--" relpath)]
    (when (zero? exit) (str/trim out))))

;; ------------------------------------------------------------- topology ----
;; run3_conformance.bb:56-68, transcribed field for field.

(def control-map (edn/read-string (slurp control-map-path)))

(defn edge-pair [e] [(name (:from e)) (name (:to e))])

(def drawn-edges (mapv edge-pair (:edges control-map)))
(def measured-edges (mapv edge-pair (:route-measured-drawn control-map)))

(def retired
  (reduce (fn [acc [_ d]]
            (reduce (fn [a p] (update a (mapv name p) (fnil conj #{}) (:grounds d)))
                    acc (:retires d)))
          {} (:decisions control-map)))

(def drawn-set (set drawn-edges))
(def measured-set (set measured-edges))
(def route-index (route-core/index control-map))

(defn classify [hop]
  (route-core/classify route-index hop))

;; ------------------------------------------------------------- the run -----

(def receipt-re #"^tick-run-record-(\d{4}-\d{2}-\d{2})-(.+)\.edn$")

(def receipts
  (->> (.listFiles (io/file run-dir))
       (keep (fn [f]
               (when-let [[_ date _] (re-matches receipt-re (.getName f))]
                 (when-let [id (:run/id (edn/read-string (slurp f)))]
                   {:file (.getName f) :date date :run-id id}))))
       (sort-by :file)
       vec))

(def run-ids (set (map :run-id receipts)))
(def run-dates (sort (distinct (map :date receipts))))

(def records
  ;; RUN11 selection by :run/id out of the shared per-date trace file, exactly
  ;; as run3_conformance.bb:88-100 selects it.
  (let [shared (mapcat #(let [f (io/file trace-dir (str "wm-trace-" % ".edn"))]
                          (when (.exists f) (read-forms (str f))))
                       run-dates)]
    (filterv #(contains? run-ids (:run/id %)) shared)))

(defn route-nodes [record] (mapv name (map :node (:wm/route record))))

(defn hops [nodes] (mapv vec (partition 2 1 nodes)))

(def routes (mapv route-nodes records))
(def all-hops (vec (mapcat hops routes)))
(def distinct-hops (vec (distinct all-hops)))
(def unfired (vec (sort (remove (set all-hops) drawn-edges))))

;; ----------------------------------------------------------- predicates ----
;; The Lean side's `runConformsToWiring`, evaluated here so the controls can
;; mutate the tables and watch it fail.

(defn conforms? [rts]
  (route-core/conforms-routes? control-map rts))

;; ----------------------------------------------------------------- Lean ----

(def all-nodes
  ;; The map's own declared node list, plus any node a route visits that the
  ;; list omits. TRACE and R3a are in the declared list; the fallback is here
  ;; so a new node in a route cannot be silently dropped from the datatype.
  (let [declared (mapv name (:nodes control-map))
        seen (distinct (concat (mapcat identity routes)
                               (mapcat identity drawn-edges)
                               (mapcat identity measured-edges)))]
    (vec (concat declared (remove (set declared) seen)))))

(defn lean-node [n] n)

(defn decode-node [n] (first (filter #(= n (lean-node %)) all-nodes)))

(defn lean-edge [[a b]] (str "(." (lean-node a) ", ." (lean-node b) ")"))

(defn lean-edge-list [es indent]
  (let [pad (apply str (repeat indent \space))]
    (str "[" (str/join (str ",\n" pad) (map lean-edge es)) "]")))

(defn lean-grounds [gs]
  (str "[" (str/join ", " (map #(str "." (name %)) (sort gs))) "]"))

(defn lean-retired-list [pairs indent]
  (let [pad (apply str (repeat indent \space))]
    (str "[" (str/join (str ",\n" pad)
                       (map (fn [[p gs]]
                              (str "{ edge := " (lean-edge p)
                                   ", grounds := " (lean-grounds gs) " }"))
                            pairs))
         "]")))

(defn lean-route [nodes _indent]
  (str "[" (str/join ", " (map #(str "." (lean-node %)) nodes)) "]"))

(def source-facts
  {:control-map {:path control-map-path
                 :sha256 (sha256 control-map-path)
                 :p4ng-commit (git-sha "/home/joe/code/p4ng" "empirics-futon/control-map-edges.edn")
                 :as-of (:as-of control-map)}
   :run {:dir run-dir
         :run-sha (second (re-find #"sha `([0-9a-f]{7,40})`"
                                   (slurp (str run-dir "/README.md"))))
         :trace-sha256 (sha256 trace-file)
         :receipts receipts
         :records (count records)}})

(defn lean-block []
  (let [retired-sorted (sort-by key retired)
        cm (:control-map source-facts)
        rn (:run source-facts)]
    (str
     "/-! ### The " run-name " run's route against the drawn wiring (worklist `:U49`)\n\n"
     "Joe's RUN4 ruling (2026-09-03) refuses the permanent-attestation reading of\n"
     "`wmRunConformsToWiring`: what is wanted is to run the machine and validate in\n"
     "Lean that the run conforms to the wiring that was drawn. This block is the\n"
     "transcription that makes the validation decidable -- the drawn map as data and\n"
     "one pinned run's reassembled route -- and\n"
     "`wm" Slug "RunConformsToDrawnWiring` below is the certificate over it.\n\n"
     "SOURCES, both pinned:\n"
     "* `p4ng:empirics-futon/control-map-edges.edn`, `:as-of` " (:as-of cm)
     ", commit `" (:p4ng-commit cm) "`,\n"
     "  sha256 `" (:sha256 cm) "`\n"
     "  -- " (count drawn-edges) " `:edges`, " (count measured-edges)
     " `:route-measured-drawn`, " (count retired-sorted) " retired pairs.\n"
     "* `futon2:holes/labs/wm-contract/" run-dir "`, the run at futon2 sha `"
     (:run-sha rn) "` --\n"
     "  " (count records) " records selected by `:run/id` (RUN11), extracted trace sha256\n"
     "  `" (:trace-sha256 rn) "`.\n\n"
     "GENERATED from those two files by\n"
     "`futon2:holes/labs/wm-contract/u49_route_transcribe.bb`; edit the sources and\n"
     "regenerate rather than editing the literals.\n-/\n\n"

     "/-- The nodes of the drawn control map (`control-map-edges.edn :nodes`), plus\n"
     "`TRACE`, the sink the route's last hop reaches. Constructor names are the\n"
     "recorded ids verbatim. -/\n"
     "inductive RouteNode where\n"
     (str/join "\n" (map #(str "  | " (lean-node %)) all-nodes))
     "\n  deriving DecidableEq, Repr\n\n"

     "/-- A hop, and equally an edge of the figure: an ordered pair of nodes. The\n"
     "route a tick records is a SEQUENCE of node tags, so a hop is a consecutive\n"
     "pair (`run3_conformance.bb:113-114`). -/\n"
     "abbrev WiringEdge := RouteNode × RouteNode\n\n"

     "/-- The grounds on which a drawn edge was retired by a `:decisions` entry of\n"
     "the control map. `code` retirements are claims about the code; `ruling`\n"
     "retirements are Joe's. A pair may carry both, from different decisions. -/\n"
     "inductive RetirementGrounds where\n  | code\n  | ruling\n"
     "  deriving DecidableEq, Repr\n\n"

     "/-- A retired pair with the SET of grounds it was retired on -- the shape\n"
     "`run3_conformance.bb:57-61` reduces `:decisions` to. -/\n"
     "structure RetiredWiringEdge where\n"
     "  edge : WiringEdge\n"
     "  grounds : List RetirementGrounds\n"
     "  deriving DecidableEq, Repr\n\n"

     "/-- Every pair in the map's `:edges`, in file order. NOTE, and it is a real\n"
     "difference between the two checkers rather than an oversight: `:status` is NOT\n"
     "filtered here, because `run3_conformance.bb:55` does not filter it and run3 is\n"
     "what produced this run's pinned verdict. So the one `:unresolved` self-loop\n"
     "`R5 -> R5` is in this list, where `checks/wm_route_conformance.clj:28` would\n"
     "drop it. It is not traversed by this run, so nothing here turns on it. -/\n"
     "def figureDrawnEdges : List WiringEdge :=\n  "
     (lean-edge-list drawn-edges 3) "\n\n"

     "/-- The `:route-measured-drawn` layer: edges added to Figure 4 because a route\n"
     "measurement found them. Conformance against this layer is weaker than\n"
     "conformance against `figureDrawnEdges` and the certificate's docstring says so. -/\n"
     "def figureMeasuredEdges : List WiringEdge :=\n  "
     (lean-edge-list measured-edges 3) "\n\n"

     "/-- The retired pairs and their grounds. -/\n"
     "def figureRetiredEdges : List RetiredWiringEdge :=\n  "
     (lean-retired-list retired-sorted 3) "\n\n"

     "/-- Decidable membership without a `BEq` detour. -/\n"
     "def edgeMem (e : WiringEdge) (es : List WiringEdge) : Bool :=\n"
     "  es.any (fun x => decide (x = e))\n\n"

     "/-- The grounds recorded against a pair; `[]` when it was never retired. -/\n"
     "def retirementGroundsOf (e : WiringEdge) : List RetirementGrounds :=\n"
     "  match figureRetiredEdges.find? (fun r => decide (r.edge = e)) with\n"
     "  | some r => r.grounds\n"
     "  | none => []\n\n"

     "/-- How run3 dispositions one hop. -/\n"
     "inductive HopClass where\n"
     "  | refutation\n  | rulingUnrealised\n  | excludedDependencyGrain\n"
     "  | drawn\n  | routeMeasured\n  | unmapped\n"
     "  deriving DecidableEq, Repr\n\n"

     "/-- `run3_conformance.bb:116-124`, transcribed clause for clause. The order\n"
     "matters and is the script's: a `code` retirement whose pair is ALSO on the\n"
     "measured layer retired a dependency claim while the route stayed drawn as\n"
     "measured, so it is excluded rather than a refutation. -/\n"
     "def classifyHop (e : WiringEdge) : HopClass :=\n"
     "  let g := retirementGroundsOf e\n"
     "  let hasCode := g.any (fun x => decide (x = RetirementGrounds.code))\n"
     "  let hasRuling := g.any (fun x => decide (x = RetirementGrounds.ruling))\n"
     "  if hasCode && edgeMem e figureMeasuredEdges then .excludedDependencyGrain\n"
     "  else if hasCode then .refutation\n"
     "  else if hasRuling then .rulingUnrealised\n"
     "  else if edgeMem e figureDrawnEdges then .drawn\n"
     "  else if edgeMem e figureMeasuredEdges then .routeMeasured\n"
     "  else .unmapped\n\n"

     "/-- A recorded route reassembled into hops: the consecutive pairs. Written\n"
     "with `zip` rather than by recursion so that `decide` reduces it in the kernel\n"
     "without going through the equation compiler's `brecOn`. -/\n"
     "def routeHops (r : List RouteNode) : List WiringEdge := r.zip r.tail\n\n"

     "/-- THE CONFORMANCE VERDICT, as run3 states it and stripped of nothing:\n"
     "the run recorded at least one route, no route is empty, no hop is unmapped,\n"
     "and no hop is a refutation (a code-retired pair traversed at route grain).\n"
     "The two retired classes run3 does NOT count against a run --\n"
     "`excludedDependencyGrain` and `rulingUnrealised` -- are absent here for the\n"
     "same reason they are absent there, and this run hits both, so a flat \"no\n"
     "retired edge traversed\" would report it not conformant.\n\n"
     "`reducible` because `decide` needs the `Decidable` instance for THIS\n"
     "conjunction, and instance synthesis does not unfold an irreducible `def`. -/\n"
     "@[reducible] def runConformsToDrawnWiring (routes : List (List RouteNode)) : Prop :=\n"
     "  routes ≠ [] ∧\n"
     "    (∀ r ∈ routes, r ≠ []) ∧\n"
     "    (∀ h ∈ routes.flatMap routeHops, classifyHop h ≠ HopClass.unmapped) ∧\n"
     "    (∀ h ∈ routes.flatMap routeHops, classifyHop h ≠ HopClass.refutation)\n\n"

     )))

(defn lean-run-header
  "The header a run's block carries when the shared tables are NOT re-emitted:
   it names the two pinned sources exactly as the full header does, and says
   which block the definitions it uses come from."
  []
  (let [cm (:control-map source-facts)
        rn (:run source-facts)]
    (str
     "/-! ### The " run-name " run's route against the drawn wiring (worklist `:RE5`)\n\n"
     "The SECOND run certified against the drawn wiring, under Joe's RUN4 ruling.\n"
     "The transcription's shared definitions -- `RouteNode`, `WiringEdge`,\n"
     "`figureDrawnEdges`, `figureRouteMeasured`, `figureRetired`, `classifyHop`,\n"
     "`routeHops`, `runConformsToDrawnWiring` -- are the ones the `:U49` block\n"
     "above defines, and are NOT redefined here. They are a function of the drawn\n"
     "map alone, so reusing them is a claim that the map has not moved since that\n"
     "block was generated; the producer checks it (control C7) rather than\n"
     "assuming it.\n\n"
     "SOURCES, both pinned:\n"
     "* `p4ng:empirics-futon/control-map-edges.edn`, `:as-of` " (:as-of cm)
     ", commit `" (:p4ng-commit cm) "`,\n"
     "  sha256 `" (:sha256 cm) "`\n"
     "  -- " (count drawn-edges) " `:edges`, " (count measured-edges)
     " `:route-measured-drawn`, " (count (sort-by key retired)) " retired pairs.\n"
     "* `futon2:holes/labs/wm-contract/" run-dir "`, the run at futon2 sha `"
     (:run-sha rn) "` --\n"
     "  " (count records) " records selected by `:run/id` (RUN11), extracted trace sha256\n"
     "  `" (:trace-sha256 rn) "`.\n\n"
     "GENERATED from those two files by\n"
     "`futon2:holes/labs/wm-contract/u49_route_transcribe.bb`; edit the sources and\n"
     "regenerate rather than editing the literals.\n-/\n\n")))

(defn lean-run-defs []
  (str
     "/-- The " (count routes) " routes the run recorded, in the order `:run/id` selection\n"
     "returns them out of the shared trace:\n"
     (str/join ",\n" (map (fn [r] (str "`" (:run/id r) "`")) records)) ". -/\n"
     "def " slug "Routes : List (List RouteNode) :=\n  ["
     (str/join ",\n   " (map #(lean-route % 4) routes)) "]\n\n"

     "/-- The run's " (count all-hops) " hops, " (count distinct-hops) " of them distinct. -/\n"
     "def " slug "Hops : List WiringEdge := " slug "Routes.flatMap routeHops\n\n"

     "/-- The drawn edges this run never traversed. -/\n"
     "def " slug "UnfiredDrawnEdges : List WiringEdge :=\n"
     "  figureDrawnEdges.filter (fun e => !edgeMem e " slug "Hops)\n"))

(defn lean-theorems []
  (let [census (frequencies (map classify distinct-hops))]
    (str
     "\n/-- CLOSED UNDER THE J9 CRITERION · leg (3) · THE RUN-CONFORMANCE CERTIFICATE\n"
     "for the run `" run-dir "` (futon2 sha `" (get-in source-facts [:run :run-sha]) "`),\n"
     "worklist `" row-ref "` under Joe's RUN4 ruling of 2026-09-03. Every one of the "
     (count all-hops) "\n"
     "hops the run recorded is an edge of the drawn wiring on run3's own\n"
     "classification, no route is empty, and no code-retired pair was traversed at\n"
     "route grain. Proved by `decide` over the transcribed tables, no `sorry` and no\n"
     "`native_decide` -- the `wmTraceR2`/`wmTraceR8` precedent. The Clojure side of\n"
     "the same comparison is `futon2:holes/labs/wm-contract/run3_conformance.bb`,\n"
     "whose pinned verdict for this run is\n"
     "`runs/" run-name "/conformance.edn` `:verdict :conformant`; the mutations that\n"
     "break this proposition are listed at `" controls-ref "/04-controls.edn`\n"
     "control C4.\n\n"
     "WHAT IT DOES NOT SHOW, because a reader will otherwise take it for more: "
     (:route-measured census) " of\n"
     "the " (count distinct-hops) " distinct hops are on the `:route-measured-drawn` layer, which is the\n"
     "layer a previous route MEASUREMENT put on the figure, so for those the run is\n"
     "being compared against a record of a run; and " (count unfired) " of the "
     (count drawn-edges) " drawn edges never\n"
     "fired at all. The certificate says this run stayed inside the union of the two\n"
     "layers. It does not say the drawn figure predicted the run. -/\n"
     "theorem wm" Slug "RunConformsToDrawnWiring : runConformsToDrawnWiring " slug "Routes := by\n"
     "  decide\n\n"

     "/-- The census the certificate is stated over, so the numbers a reader checks\n"
     "against `runs/" run-name "/conformance.edn` are themselves decided rather than\n"
     "asserted in prose: " (count routes) " routes, " (count all-hops) " hops, "
     (count distinct-hops) " distinct, and the class split\n"
     "-- " (:drawn census 0) " drawn, " (:route-measured census 0) " route-measured, "
     (:excluded-dependency-grain census 0) " excluded at dependency grain, "
     (:ruling-unrealised census 0) " ruling-unrealised,\n"
     "0 refutations, 0 unmapped -- with " (count unfired) " of " (count drawn-edges)
     " drawn edges unfired. -/\n"
     "theorem wm" Slug "RouteCensus :\n"
     "    " slug "Routes.length = " (count routes) " ∧\n"
     "      " slug "Hops.length = " (count all-hops) " ∧\n"
     "      " slug "Hops.dedup.length = " (count distinct-hops) " ∧\n"
     "      (" slug "Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.drawn))).length = "
     (:drawn census 0) " ∧\n"
     "      (" slug "Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.routeMeasured))).length = "
     (:route-measured census 0) " ∧\n"
     "      (" slug "Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.excludedDependencyGrain))).length = "
     (:excluded-dependency-grain census 0) " ∧\n"
     "      (" slug "Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.rulingUnrealised))).length = "
     (:ruling-unrealised census 0) " ∧\n"
     "      (" slug "Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.refutation))).length = 0 ∧\n"
     "      (" slug "Hops.dedup.filter (fun h => decide (classifyHop h = HopClass.unmapped))).length = 0 ∧\n"
     "      figureDrawnEdges.length = " (count drawn-edges) " ∧\n"
     "      " slug "UnfiredDrawnEdges.length = " (count unfired) " := by\n"
     "  decide\n")))

;; ------------------------------------------------------------- controls ----

(def pinned-conformance
  (edn/read-string (slurp (str run-dir "/conformance.edn"))))

(def tables-source-path
  "The producer artifact recording what the SHARED Lean tables were generated
   from. Only consulted when this invocation does not re-emit them."
  (or (System/getenv "U49_TABLES_SOURCE") "runs/U49-run-conformance/00-source.edn"))

(defn controls [lean-text]
  (let [census (frequencies (map classify distinct-hops))
        reproduced {:hops (count all-hops)
                    :distinct (count distinct-hops)
                    :drawn (count drawn-edges)
                    :unfired (count unfired)
                    :records (count records)
                    :routes (count routes)
                    :selection-run-ids (count run-ids)
                    :refutations (seq (filterv #(= :refutation (classify %)) distinct-hops))
                    :unmapped (seq (filterv #(= :unmapped (classify %)) distinct-hops))
                    :excluded (vec (filter #(= :excluded-dependency-grain (classify %)) distinct-hops))
                    :ruling-unrealised (vec (filter #(= :ruling-unrealised (classify %)) distinct-hops))
                    :verdict (if (conforms? routes) :conformant :not-conformant)}
        pinned (select-keys pinned-conformance (keys reproduced))
        fabricated "R404"
        neg-a (update routes 0 conj "R99" "R100")
        neg-b (assoc routes 0 [])
        neg-c (update routes 0 #(vec (concat % ["R2" "R3"])))
        neg-d []]
    (merge
     {:C1-reproduces-the-pinned-verdict
     {:pinned pinned :reproduced reproduced
      :note (str "The pinned record was written by run3_conformance.bb at "
                 (:checked-at pinned-conformance)
                 "; this comparison is against the CURRENT control map, "
                 (get-in source-facts [:control-map :p4ng-commit]) ".")
      :pass (= pinned reproduced)}

     :C2-node-names-round-trip
     {:nodes-round-trip (= all-nodes (mapv #(decode-node (lean-node %)) all-nodes))
      :distinct-lean-names (= (count all-nodes) (count (distinct (map lean-node all-nodes))))
      :routes-round-trip (= routes (mapv (fn [r] (mapv #(decode-node (lean-node %)) r)) routes))
      :every-route-node-declared (every? (set all-nodes) (mapcat identity routes))
      :pass (and (= all-nodes (mapv #(decode-node (lean-node %)) all-nodes))
                 (= (count all-nodes) (count (distinct (map lean-node all-nodes))))
                 (= routes (mapv (fn [r] (mapv #(decode-node (lean-node %)) r)) routes))
                 (every? (set all-nodes) (mapcat identity routes)))}

     :C3-fabricated-node-absent
     {:name fabricated
      :in-nodes (boolean ((set all-nodes) fabricated))
      :in-any-route (boolean (some #(some #{fabricated} %) routes))
      :in-any-edge (boolean (some #(some #{fabricated} %) (concat drawn-edges measured-edges)))
      :in-lean-text (str/includes? lean-text (str "| " fabricated "\n"))
      :pass (not (or ((set all-nodes) fabricated)
                     (some #(some #{fabricated} %) routes)
                     (some #(some #{fabricated} %) (concat drawn-edges measured-edges))
                     (str/includes? lean-text (str "| " fabricated "\n"))))}

     :C4-mutations-rejected
     {:baseline-conformant (conforms? routes)
      :a-planted-unmapped-hop {:mutation "R99 R100 appended to route 0"
                               :hop-class (classify ["R99" "R100"])
                               :conforms (conforms? neg-a)}
      :b-route-emptied {:mutation "route 0 emptied" :conforms (conforms? neg-b)}
      :c-code-retired-route-grain {:mutation "R2 R3 appended to route 0"
                                   :hop-class (classify ["R2" "R3"])
                                   :grounds (retired ["R2" "R3"])
                                   :conforms (conforms? neg-c)}
      :d-no-routes {:mutation "route table emptied" :conforms (boolean (conforms? neg-d))}
      :pass (and (conforms? routes)
                 (not (conforms? neg-a)) (not (conforms? neg-b))
                 (not (conforms? neg-c)) (not (conforms? neg-d)))}

     :C5-drawn-set-differs-between-the-two-checkers
     {:run3-takes-all-edges (count drawn-edges)
      :wm-route-conformance-filters-status-drawn
      (count (filter #(= :drawn (:status %)) (:edges control-map)))
      :the-difference (mapv edge-pair (remove #(= :drawn (:status %)) (:edges control-map)))
      :traversed-by-this-run
      (boolean (some (set all-hops)
                     (map edge-pair (remove #(= :drawn (:status %)) (:edges control-map)))))
      :finding (str "run3_conformance.bb:55 does not filter :status and "
                    "checks/wm_route_conformance.clj:28 does. The transcription "
                    "follows run3, which produced the pinned verdict. Inert on "
                    "this run: the difference is not traversed.")}

     :C6-what-the-certificate-does-not-show
     {:distinct-hops (count distinct-hops)
      :on-the-measured-layer (:route-measured census 0)
      :on-the-drawn-layer (:drawn census 0)
      :unfired-drawn-edges (count unfired)
      :drawn-edges (count drawn-edges)
      :finding (str (:route-measured census 0) " of " (count distinct-hops)
                    " distinct hops are drawn only because a previous route "
                    "measurement put them on the :route-measured-drawn layer, and "
                    (count unfired) " of " (count drawn-edges)
                    " drawn edges never fired. The certificate says the run stayed "
                    "inside the union of the two layers; it does not say the drawn "
                    "figure predicted the run.")}}

     (when-not emit-tables?
       (let [prior (edn/read-string (slurp tables-source-path))
             prior-sha (get-in prior [:control-map :sha256])
             now-sha (get-in source-facts [:control-map :sha256])]
         {:C7-reused-tables-are-still-the-current-map
          {:tables-from tables-source-path
           :tables-control-map-sha256 prior-sha
           :this-run-control-map-sha256 now-sha
           :finding (str "This block omits the shared definitions and uses the ones the "
                         "earlier block defines. Those were generated from control map "
                         prior-sha ". If that is not the map this run was classified "
                         "against, the theorem below decides the wrong tables and says "
                         "nothing about the drawn wiring as it now stands.")
           :pass (= prior-sha now-sha)}})))))

;; ---------------------------------------------------------------- write ----

(defn pp-str [x] (with-out-str (pprint/pprint x)))

(fs/create-dirs outdir)

(def lean-text
  (str (if emit-tables? (lean-block) (lean-run-header))
       (lean-run-defs)
       (lean-theorems)))
(def control-results (controls lean-text))

(spit (str outdir "/00-source.edn") (pp-str source-facts))
(spit (str outdir "/01-topology.edn")
      (pp-str {:drawn-edges drawn-edges
               :measured-edges measured-edges
               :retired (into (sorted-map) retired)
               :nodes all-nodes}))
(spit (str outdir "/02-routes.edn")
      (pp-str {:routes routes :hops all-hops :distinct-hops distinct-hops}))
(spit (str outdir "/03-classification.edn")
      (pp-str {:by-hop (into (sorted-map) (map (juxt identity classify) distinct-hops))
               :census (frequencies (map classify distinct-hops))
               :unfired unfired
               :verdict (if (conforms? routes) :conformant :not-conformant)}))
(spit (str outdir "/04-controls.edn") (pp-str control-results))
(spit (str outdir "/lean-block.lean") lean-text)

;; -------------------------------------------------------------- deposit ----
;; --deposit <run-id> -- one run-era ledger row (RE3).
;;
;; This is the one check of the three RE3 wires whose evidence IS in the run
;; store: `conformance.edn` was written there by run3_conformance.bb when the
;; run was taken, and C1 above is the test that this transcription reproduces
;; that pinned verdict. So the row is a verdict about this run rather than a
;; property of the tree, and it deposits :green or :red, never a typed absence.
;;
;; :row/at is the run's own :checked-at, not a deposit-time stamp: the
;; transcription writes no wall-clock field by design (it re-decides the same
;; comparison rather than making a new observation), and the time a reader
;; wants on the series is when the run's conformance was determined.

(defn deposit! [run-id]
  (let [expected (last (str/split run-dir #"/"))]
    (when-not (= run-id expected)
      (println (format "u49 --deposit: run-id %s does not name the transcribed run (%s); refusing"
                       run-id run-dir))
      (System/exit 1))
    (let [failed (vec (sort (keep (fn [[k v]] (when (false? (:pass v)) (name k))) control-results)))
          conformant (= :conformant (:verdict pinned-conformance))
          verdict (if (and (empty? failed) conformant (conforms? routes)) :green :red)
          out-rel (str (fs/relativize repo-root (fs/absolutize outdir)))
          artifact (str out-rel "/03-classification.edn")
          notes (if (= :green verdict)
                  (str "the transcription reproduces the run's own pinned verdict :conformant "
                       "(runs/" run-id "/conformance.edn, :checked-at " (:checked-at pinned-conformance)
                       ", written by run3_conformance.bb when the run was taken) over "
                       (count routes) " routes, " (count all-hops) " hops, "
                       (count distinct-hops) " distinct, against control map "
                       (get-in source-facts [:control-map :p4ng-commit])
                       "; control C1 is that reproduction and every falsifier in C4 breaks it. "
                       "LIMITS, from C6 beside this artifact: "
                       (:route-measured (frequencies (map classify distinct-hops)) 0)
                       " of " (count distinct-hops) " distinct hops are on the :route-measured-drawn "
                       "layer, which a previous route measurement put there, and " (count unfired)
                       " of " (count drawn-edges) " drawn edges never fired -- the run stayed inside "
                       "the union of the drawn and measured layers, which is not the drawn figure "
                       "predicting the run. Controls: " out-rel "/04-controls.edn.")
                  (str "the run-conformance transcription FAILS at deposit time: controls "
                       (pr-str failed) " failed"
                       (when-not conformant
                         (str "; the run's pinned verdict is " (pr-str (:verdict pinned-conformance))))
                       ". Controls: " out-rel "/04-controls.edn."))
          {:keys [exit out err]}
          (process/shell {:dir repo-root :out :string :err :string :continue true}
                         "bb" "holes/labs/wm-contract/run_era_ledger.bb" "--deposit"
                         "--run-id" run-id
                         "--check-id" ":run-conformance"
                         "--verdict" (str verdict)
                         "--artifact" artifact
                         "--at" (:checked-at pinned-conformance)
                         "--author" "u49_route_transcribe.bb --deposit"
                         "--deposited-by" "RE3 -- wire the existing checks to deposit ledger rows"
                         "--notes" notes)]
      (print out) (print err) (flush)
      (when-not (zero? exit)
        (println (format "u49 --deposit: the ledger refused the row (exit %d)" exit))
        (println "  if the refusal is artifact-dirty, commit" artifact "and re-run"))
      (System/exit exit))))

(let [failures (keep (fn [[k v]] (when (false? (:pass v)) k)) control-results)]
  (println (format "u49: %d records, %d hops (%d distinct), verdict %s"
                   (count records) (count all-hops) (count distinct-hops)
                   (name (if (conforms? routes) :conformant :not-conformant))))
  (doseq [[k v] (sort-by key control-results)]
    (println (format "u49: %-46s %s" (name k)
                     (case (:pass v) true "PASS" false "FAIL" "REPORTED"))))
  (println (format "u49: wrote %s" outdir))
  (when (seq failures)
    (println "u49: FAIL" (pr-str failures))
    (System/exit 1))
  (when deposit-run-id (deposit! deposit-run-id)))
