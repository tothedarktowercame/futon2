#!/usr/bin/env bb
;; F2 -- TRANSCRIBE THE RUN4 PREREGISTRATION INTO LEAN.
;;
;;   bb holes/labs/wm-contract/run4_prereg_transcribe.bb [outdir]
;;
;; Run FROM THE LAB DIRECTORY: the default outdir and every read path below are
;; relative to the working directory.
;;
;; WHY THIS EXISTS. Joe, 2026-09-05: "i don't get a sense of how far off we are
;; from being able to do RUN4 without it being a waste of time. Maybe RUN4 needs
;; its own manifest/preregistration. That is unlikely to fit in Voxterm but it
;; could go well in Lean." This is the manifest half. It states, as data a
;; machine can check, exactly WHAT an acceptance would assert, WHICH authorities
;; the assertions are pinned to, and WHICH pending work would make an acceptance
;; stale on arrival. `run4_readiness.bb` is the other half: it compares those
;; pins to the live repositories and reports READY or BLOCKED-ON.
;;
;; WHAT IT IS NOT. It accepts nothing, closes no hole, writes no ruling and
;; edits no registry. `wmRunConformsToWiring` stays `mkHole` until Joe accepts a
;; certificate over a run he judges qualifying -- the RUN4 ruling reserves both
;; the acceptance and the judgement of which run qualifies (worklist.edn
;; :run4-lean-ruling, Joe 2026-09-03).
;;
;; THE PINS ARE REUSED, NOT RE-TAKEN. Every authority in the emitted manifest is
;; copied out of the committed certificates, which `u49_route_transcribe.bb`
;; generated from the two pinned sources. This script takes no fresh hash of the
;; control map or of a trace; re-pinning here would produce a manifest that
;; agreed with today's tree instead of with the certificates, which is the one
;; thing a staleness meter must not do.
;;
;; WHAT IT WRITES (outdir defaults to runs/F2-run4-preregistration):
;;   00-source.edn        the certificates read, and their identities.
;;   01-assertions.edn    the assertion set: theorem, run, status.
;;   02-authorities.edn   the pinned authorities, copied from the certificates.
;;   03-invalidators.edn  the declared invalidation set, each MEASURED against
;;                        the producer source and the emitted Lean blocks for
;;                        whether it touches the certificates' definitions.
;;   04-controls.edn      the controls (C1-C6 below).
;;   Run4Preregistration.lean  the generated Lean module, which mathlib4 holds
;;                        at DarkTower/WarMachine/Run4Preregistration.lean.
;;
;; WHY A SEPARATE MODULE AND NOT A BLOCK IN Holes.lean. The certificates are
;; pinned to Holes.lean by commit, and the emitted contract's authority is the
;; last commit that touched that file (C175). A preregistration written INTO
;; Holes.lean would move the authority the manifest is about, so writing the
;; manifest would invalidate its own pin. A sibling module -- the shape every
;; other DarkTower/WarMachine witness uses -- leaves the authority still.
;;
;; DETERMINISM. No wall-clock field is written; two runs over unchanged
;; certificates and unchanged sources produce byte-identical artifacts.
;;
;; CONTROLS.
;;   C1 every certificate transcribed carries :status :minted-awaiting-acceptance
;;      and schema :wm/run-conformance-certificate-v1, and names a run, a wiring
;;      pin and at least one theorem. A manifest over an accepted or a schemaless
;;      certificate would be a manifest about something else.
;;   C2 ROUND-TRIP: every theorem the manifest names occurs in the certificate it
;;      came from AND in the live Holes.lean, and every slug derived from a run
;;      name occurs in that certificate's own generated Lean block. This is what
;;      makes the emitted literal a transcription rather than a retyping.
;;   C3 a fabricated theorem name and a fabricated authority occur nowhere in the
;;      emitted Lean text.
;;   C4 DEFINITIONAL IDENTITY: each certificate's generated lean-block.lean
;;      occurs VERBATIM in the live Holes.lean. Without this the manifest would
;;      be pinned to theorems that had since been restated.
;;   C5 the invalidator measurements are executable and are re-run here, not
;;      quoted: each carries the counts it was decided from (see 03).
;;   C6 WHAT THE MANIFEST DOES NOT SETTLE: which run qualifies. Both censuses are
;;      identical hop for hop, and the two runs' :selection-discrimination
;;      verdicts differ (:defect for s5, :green for re5). That is recorded as a
;;      declared item and NOT resolved: the RUN4 ruling reserves it.

(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[babashka.fs :as fs])

(def outdir (or (first *command-line-args*) "runs/F2-run4-preregistration"))

(def code-root (str (System/getProperty "user.home") "/code/"))
(def holes-lean (or (System/getenv "F2_HOLES_LEAN")
                    (str code-root "mathlib4/DarkTower/WarMachine/Holes.lean")))
(def u49-producer (or (System/getenv "F2_U49_PRODUCER") "u49_route_transcribe.bb"))
(def runs-dir (or (System/getenv "F2_RUNS_DIR") "runs"))
(def darktower-dir (or (System/getenv "F2_DARKTOWER")
                       (str code-root "mathlib4/DarkTower/")))

(def problems (atom []))
(defn fail! [& parts] (swap! problems conj (str/join " " (map str parts))))

(defn sha256 [path]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (str/join (map #(format "%02x" %) (.digest d (fs/read-all-bytes path))))))

;; ---------------------------------------------------------- certificates ---
;;
;; Discovered, not listed: a third certificate minted tomorrow joins the
;; manifest by being written, and a certificate Joe ACCEPTS leaves it by having
;; its :status changed. A hard-coded list would have to be edited in both cases,
;; and the edit that was forgotten would be invisible.

(def certificate-files
  (->> (fs/list-dir runs-dir)
       (map #(fs/file % "certificate.edn"))
       (filter fs/regular-file?)
       (map str)
       sort
       vec))

(defn read-cert [path]
  (let [c (edn/read-string {:default (fn [_ v] v)} (slurp path))]
    (assoc c ::path path ::dir (str (fs/parent path)))))

(def certificates
  (->> certificate-files
       (map read-cert)
       (filter #(= :minted-awaiting-acceptance (:status %)))
       (sort-by #(get-in % [:run :name]))
       vec))

(when (empty? certificates)
  (fail! "no certificate under" runs-dir
         "carries :status :minted-awaiting-acceptance -- a preregistration over"
         "an empty assertion set asserts nothing"))

;; The slug is the Lean name stem, and it is DERIVED from the run name rather
;; than read from a field, because U49's certificate predates the field
;; (:produced-with arrived with RE5). C2 checks the derivation against the
;; certificate's own generated Lean block, so a wrong stem is caught here and
;; not at `lake build`.
(defn slug-of [cert] (last (str/split (get-in cert [:run :name]) #"-")))
(defn slug-cap [cert]
  (let [s (slug-of cert)] (str (str/upper-case (subs s 0 1)) (subs s 1))))

(def holes-text (slurp holes-lean))

;; ---------------------------------------------------------- the assertions -

(def assertions
  (vec (for [c certificates
             t (:theorems c)]
         {:theorem (:name t)
          :short-name (last (str/split (:name t) #"\."))
          :run (get-in c [:run :name])
          :row (:worklist-item c)
          :status (:status c)
          :tactic (:tactic t)
          :asserts (:statement t)})))

(def acceptance-closes
  "What an acceptance DOES, stated once. Read off the ruling, not decided here."
  {:declaration "wmRunConformsToWiring"
   :from "mkHole"
   :to "mkClosed"
   :whose-act "Joe"
   :ruling "worklist.edn :run4-lean-ruling, Joe 2026-09-03"
   :which-run-qualifies "Joe's call at certificate time -- reserved by the same ruling"})

;; --------------------------------------------------------- the authorities -
;;
;; Copied out of the certificates field for field. The :from field names the
;; certificate each identity was copied from, so a reader can check the copy
;; without trusting it.

(def authorities
  (vec (concat
        ;; The wiring is one authority even when several certificates pin it:
        ;; they pin the same bytes, which control C7 of the U49 producer checks
        ;; on the RE5 side. Emitting it twice would report one measurement as
        ;; two.
        (let [ws (distinct (map (fn [c] (select-keys (:wiring c) [:path :sha256 :p4ng-commit :as-of])) certificates))]
          (when (< 1 (count ws))
            (fail! "the certificates pin DIFFERENT wiring:" (pr-str ws)))
          (for [w ws]
            {:key "control-map"
             :kind :sha256
             :identity (:sha256 w)
             :path (:path w)
             :git-commit (:p4ng-commit w)
             :as-of (:as-of w)
             :from (mapv :worklist-item certificates)}))
        (for [c certificates]
          {:key (str "trace:" (get-in c [:run :name]))
           :kind :sha256
           :identity (get-in c [:run :extracted-trace-sha256])
           :path (str "futon2:holes/labs/wm-contract/" (get-in c [:run :extracted-trace]))
           :git-commit (get-in c [:run :run-sha])
           :as-of (get-in c [:run :pinned-checked-at])
           :from [(:worklist-item c)]})
        (for [c certificates]
          {:key (str "contract:" (name (:worklist-item c)))
           :kind :git-commit
           :identity (get-in c [:minted-at :contract-git-sha])
           :path "mathlib4:DarkTower/WarMachine/Holes.lean"
           :git-commit (get-in c [:minted-at :contract-git-sha])
           :as-of nil
           :from [(:worklist-item c)]}))))

;; -------------------------------------------------------- the invalidators -
;;
;; THE QUESTION THIS MANIFEST CAN SETTLE, and the one it cannot. It can settle
;; whether a piece of pending work TOUCHES THE CERTIFICATES' DEFINITIONS -- that
;; is a property of what the transcription reads and of what the emitted Lean
;; block contains, and both are measurable here. It cannot settle whether the
;; work has LANDED: that is a property of the live repositories and is
;; `run4_readiness.bb`'s line. The two are kept apart on purpose, because an
;; invalidator that would touch the definitions and has not landed is READY
;; while an invalidator that would not touch them is READY however often it
;; lands.

(def cert-definition-names
  ["RouteNode" "WiringEdge" "RetirementGrounds" "HopClass" "figureDrawnEdges"
   "figureRouteMeasured" "figureRetired" "edgeMem" "classifyHop" "routeHops"
   "runConformsToDrawnWiring"])

(def f1-surface-names
  ["PredictiveOutcomeKernel" "GenerativeModel" "BeliefState" "Policy"
   "eigCounterPredictive"])

(def selection-names ["SelectionTie" "selectionDiscriminates" "chosenByTiebreak"])

(def lean-blocks
  (into {} (for [c certificates]
             [(:worklist-item c) (slurp (str (::dir c) "/lean-block.lean"))])))

(defn count-in [hay needles]
  (into (sorted-map) (for [n needles] [n (count (re-seq (re-pattern (java.util.regex.Pattern/quote n)) hay))])))

(def producer-text (slurp u49-producer))

(def blocks-joined (str/join "\n" (vals lean-blocks)))

(def label-terms ["control-stages" "aif-control-map" "control-stage-amendments"
                  "Grounded actuation" "observe construction" ":label"])

(def invalidators
  [{:id :r16-base-drawing-stroke
    :what (str "Joe's hand correcting the R16 box label on the companion paper's base drawing "
               "(p4ng/aif-control-map-paper.svg), from which aif-control-map-futon.svg and "
               "control-stages.edn are derived. WR-8: never edited from this side. "
               "worklist :U31/:U48 carry it as the one artifact, one hand remainder.")
    :touches-certificate-definitions? false
    :measured-by (str "the certificates' definitions are generated by " u49-producer
                      " from control-map-edges.edn :edges, :route-measured-drawn and :decisions"
                      " -- node IDS, never node LABELS. Counted below: the producer names no"
                      " label source at all, and no label string occurs in either emitted Lean block.")
    :measurements {:label-terms-in-producer (count-in producer-text label-terms)
                   :label-terms-in-lean-blocks (count-in blocks-joined label-terms)
                   :r16-node-in-routes
                   (into (sorted-map)
                         (for [c certificates]
                           [(get-in c [:run :name])
                            (count (re-seq #"R16" (slurp (str (::dir c) "/02-routes.edn"))))]))}
    :grounds (str "A label is not an edge. The transcription reads :from and :to as node ids"
                  " (edge-pair, " u49-producer "), and RouteNode's constructors are those ids"
                  " verbatim, so a relabelled box changes no literal in the block. R16 is also"
                  " traversed by neither candidate run, so even a stroke that moved an R16 EDGE"
                  " would not move a recorded hop -- but the manifest does not rest on that,"
                  " because a moved edge would move figureDrawnEdges whether it fired or not.")}

   {:id :post-pin-control-map-decisions
    :what (str "any :decisions entry appended to p4ng/empirics-futon/control-map-edges.edn after"
               " the pinned commit. A decision retires edges, and a retirement is what"
               " classifyHop reads to tell a refutation from an excluded hop.")
    :touches-certificate-definitions? true
    :measured-by (str "the producer folds (:decisions control-map) into the `retired` map and"
                      " classifies every hop by the SET of grounds it finds there; figureRetired"
                      " in the emitted block is that fold. Counted below.")
    :measurements {:decisions-read-by-producer
                   (count (re-seq #"\(:decisions control-map\)" producer-text))
                   :figure-retired-in-lean-blocks
                   (count (re-seq #"figureRetired" blocks-joined))}
    :grounds (str "This is the one declared item that WOULD invalidate. It is not a hazard the"
                  " manifest can retire by argument; it is a live comparison, and"
                  " run4_readiness.bb :wiring-pin is where it is checked -- the control map's"
                  " live sha256 against the pinned one, and its last commit against the pinned"
                  " commit, so a decisions entry cannot land unseen.")}

   {:id :f1-model-extensions
    :what (str "worklist :F1 -- a non-private, non-toy PredictiveOutcomeKernel constructed from"
               " GenerativeModel + BeliefState + Policy, plus the same composition at the R4"
               " runtime seam behind a default-off flag.")
    :touches-certificate-definitions? false
    :measured-by (str "name disjointness in both directions, counted below: no F1 surface name"
                      " occurs in either emitted Lean block, and no certificate definition name"
                      " occurs in any DarkTower module other than Holes.lean and this manifest's"
                      " own generated module -- both excluded, and the exclusion named"
                      " beside the counts.")
    :measurements {:f1-names-in-lean-blocks (count-in blocks-joined f1-surface-names)
                   ;; Two files are excluded, and naming the exclusion is the honest half
                   ;; of the measurement. Holes.lean is where the definitions live. The
                   ;; preregistration module is this script's own output and cites them BY
                   ;; DESIGN, so counting it would let the manifest report itself as the
                   ;; leak it exists to rule out. Every other DarkTower module is counted,
                   ;; which is where an F1 construction would land.
                   :cert-definitions-outside-holes-excluding
                   ["Holes.lean" "Run4Preregistration.lean"]
                   :cert-definitions-outside-holes
                   (into (sorted-map)
                         (for [n cert-definition-names]
                           [n (count (for [f (fs/glob darktower-dir "**.lean")
                                           :when (not (#{"Holes.lean" "Run4Preregistration.lean"}
                                                       (fs/file-name f)))
                                           :when (str/includes? (slurp (str f)) n)]
                                       f))]))}
    :grounds (str "Conformance is wiring topology -- ordered pairs of node ids and a class per"
                  " hop; Q(o|pi) is scores. They share no name and no type. What F1 CAN change is"
                  " what a FUTURE run does, since the R4 seam is on the tick path; it cannot"
                  " change what a pinned run's recorded trace says, and the assertion set is"
                  " over pinned traces. Stated rather than left implied, because likely-disjoint"
                  " was the reason this item was declared.")}

   {:id :selection-discrimination-verdicts
    :what (str "worklist :RE7 -- both candidate runs carry a Lean-decided"
               " :selection-discrimination verdict, and they differ: wmS5SelectionDiscrimination"
               " proves NOT selectionDiscriminates for 2026-09-01-s5 (4 of 4 decisions chosen"
               " from a 55-wide tie), wmRe5SelectionDiscrimination proves selectionDiscriminates"
               " for 2026-09-04-re5.")
    :touches-certificate-definitions? false
    :measured-by (str "name disjointness, counted below: the selection structures occur in"
                      " neither emitted conformance block.")
    :measurements {:selection-names-in-lean-blocks (count-in blocks-joined selection-names)}
    :grounds (str "RECORDED, NOT RESOLVED. It does not touch the definitions and cannot make an"
                  " acceptance stale. It is declared because it bears on WHICH RUN QUALIFIES,"
                  " and the RUN4 ruling reserves that judgement to Joe at certificate time."
                  " Naming it here is what stops an acceptance being made without it; deciding"
                  " it here would be writing his ruling for him.")}])

(doseq [i invalidators]
  (when (str/blank? (:measured-by i))
    (fail! "invalidator" (:id i) "carries no measurement -- an unmeasured invalidator blocks")))

;; ------------------------------------------------------------- the controls -

(def touching (filterv :touches-certificate-definitions? invalidators))

(def controls
  {:C1
   (let [bad (remove #(and (= :wm/run-conformance-certificate-v1 (:schema %))
                           (= :minted-awaiting-acceptance (:status %))
                           (get-in % [:run :name])
                           (get-in % [:wiring :sha256])
                           (seq (:theorems %)))
                     certificates)]
     {:claim "every transcribed certificate is a v1 certificate awaiting acceptance, naming a run, a wiring pin and at least one theorem"
      :certificates (mapv #(get-in % [:run :name]) certificates)
      :rejected (mapv ::path bad)
      :pass? (empty? bad)})

   :C2
   (let [rows (vec (for [c certificates
                         :let [blk (get lean-blocks (:worklist-item c))
                               s (slug-of c) S (slug-cap c)]
                         t (:theorems c)
                         :let [short (last (str/split (:name t) #"\."))]]
                     {:run (get-in c [:run :name])
                      :theorem short
                      :in-certificate-block? (str/includes? blk short)
                      :in-live-holes? (str/includes? holes-text short)
                      :slug-routes-in-block? (str/includes? blk (str s "Routes"))
                      :slug-theorem-matches? (str/starts-with? short (str "wm" S))}))]
     {:claim "every named theorem occurs in its own certificate's generated block and in the live Holes.lean, and every derived slug matches the names that block uses"
      :rows rows
      :pass? (every? #(and (:in-certificate-block? %) (:in-live-holes? %)
                           (:slug-routes-in-block? %) (:slug-theorem-matches? %))
                     rows)})

   :C3 {:claim "a fabricated theorem and a fabricated authority occur nowhere in the emitted Lean"
        :fabricated ["wmZ99RunConformsToDrawnWiring"
                     "0000000000000000000000000000000000000000000000000000000000000000"]
        :pass? :deferred-to-emit}

   :C4
   (let [rows (vec (for [c certificates]
                     {:run (get-in c [:run :name])
                      :block (str (::dir c) "/lean-block.lean")
                      :block-sha256 (sha256 (str (::dir c) "/lean-block.lean"))
                      :verbatim-in-live-holes?
                      (str/includes? holes-text (str/trim (get lean-blocks (:worklist-item c))))}))]
     {:claim "each certificate's generated Lean block occurs verbatim in the live Holes.lean, so the manifest is pinned to theorems that have not been restated"
      :holes-lean holes-lean
      :holes-lean-sha256 (sha256 holes-lean)
      :rows rows
      :pass? (every? :verbatim-in-live-holes? rows)})

   :C5 {:claim "every declared invalidator carries an executable measurement, re-run on this invocation"
        :rows (mapv #(select-keys % [:id :touches-certificate-definitions? :measurements]) invalidators)
        :touching (mapv :id touching)
        :pass? (every? #(and (seq (:measurements %)) (not (str/blank? (:measured-by %)))) invalidators)}

   :C6 {:claim "WHAT IS NOT SETTLED HERE: which run qualifies"
        :censuses-identical?
        (= 1 (count (distinct (map :census certificates))))
        :selection-verdicts
        (into (sorted-map)
              (for [c certificates]
                [(get-in c [:run :name])
                 (let [d (str runs-dir "/RE7-selection-discrimination/" (get-in c [:run :name]) "/01-decisions.edn")]
                   (if (fs/regular-file? d)
                     (:verdict (edn/read-string {:default (fn [_ v] v)} (slurp d)))
                     :not-found))]))
        :reserved-to "Joe, at certificate time (worklist.edn :run4-lean-ruling)"
        :pass? :not-a-gate}})

;; ------------------------------------------------------------------ Lean ---

(defn lean-str [s] (str "\"" (-> s (str/replace "\\" "\\\\") (str/replace "\"" "\\\"")) "\""))

(defn lean-ctor
  "kebab-or-dotted id -> lowerCamel Lean constructor."
  [id]
  (let [parts (str/split (name id) #"[-.]")]
    (str (first parts) (str/join (map str/capitalize (rest parts))))))

(def census-lines
  "Census key -> the Lean expression it is stated as, in the order
   wm<Slug>RouteCensus states them. Restated here rather than cited by name so
   that a census number moving BREAKS this module -- which is the whole point of
   a preregistration: the acceptance would then be over different numbers."
  [[:routes "%sRoutes.length"]
   [:hops "%sHops.length"]
   [:distinct-hops "%sHops.dedup.length"]
   [:drawn "(%sHops.dedup.filter (fun h => decide (classifyHop h = HopClass.drawn))).length"]
   [:route-measured "(%sHops.dedup.filter (fun h => decide (classifyHop h = HopClass.routeMeasured))).length"]
   [:excluded-dependency-grain "(%sHops.dedup.filter (fun h => decide (classifyHop h = HopClass.excludedDependencyGrain))).length"]
   [:ruling-unrealised "(%sHops.dedup.filter (fun h => decide (classifyHop h = HopClass.rulingUnrealised))).length"]
   [:refutations "(%sHops.dedup.filter (fun h => decide (classifyHop h = HopClass.refutation))).length"]
   [:unmapped "(%sHops.dedup.filter (fun h => decide (classifyHop h = HopClass.unmapped))).length"]
   [:drawn-edges "figureDrawnEdges.length"]
   [:unfired "%sUnfiredDrawnEdges.length"]])

(defn census-block [cert]
  (let [s (slug-of cert) c (:census cert)]
    (str/join " ∧\n      "
              (for [[k tmpl] census-lines]
                (str (if (str/includes? tmpl "%s") (format tmpl s) tmpl)
                     " = " (get c k))))))

(def lean-module
  (str
"import DarkTower.WarMachine.Holes

/-!
# RUN4 preregistration: what accepting a run-conformance certificate asserts

GENERATED by `futon2:holes/labs/wm-contract/run4_prereg_transcribe.bb` from the
committed certificates under `futon2:holes/labs/wm-contract/runs/`. Edit a
certificate and regenerate; do not edit the literals below.

Joe, 2026-09-05: \"i don't get a sense of how far off we are from being able to
do RUN4 without it being a waste of time. Maybe RUN4 needs its own
manifest/preregistration. That is unlikely to fit in Voxterm but it could go
well in Lean.\" This module is that manifest. It is a PREREGISTRATION: it fixes,
before the acceptance, what the acceptance would be an acceptance OF, so that a
later reader can tell an assertion that was declared in advance from one that
was assembled to fit the outcome.

WHAT IT DOES NOT DO. It accepts nothing and closes nothing.
`Holes.wmRunConformsToWiring` stays `mkHole` until Joe accepts a certificate
over a run he judges qualifying; the RUN4 ruling (futon2 `worklist.edn`
`:run4-lean-ruling`, 2026-09-03) reserves both the acceptance and the judgement
of which run qualifies, and nothing here anticipates either.

THE SPLIT BETWEEN THIS MODULE AND THE METER. Lean decides the in-repo part: the
named theorems ELABORATE at the types stated below, the manifest's own census is
decided, and the invalidator partition is decided. What Lean cannot see is
whether a pinned file still hashes to its pinned value, or whether a declared
piece of pending work has landed since the pin. That is
`futon2:holes/labs/wm-contract/run4_readiness.bb`, which compares these pins to
the live repositories and reports READY or BLOCKED-ON.

WHY THE ASSERTION SET IS CITED BY REFERENCE. `run4Assertions` carries theorem
NAMES as strings, for the reader. What binds them is the block of definitions
below it: each is the named theorem itself, at a restated type. A rename, a
removal, or a census number moving breaks this module at `lake build` -- which
is the detection a string list cannot give.
-/

namespace DarkTower.WarMachine.Run4Preregistration

open Holes

/-- A run for which a run-conformance certificate has been minted and not yet
accepted. -/
inductive CandidateRun where\n"
   (str/join "\n" (for [c certificates] (str "  | " (lean-ctor (slug-of c)))))
"\n  deriving DecidableEq, Repr

/-- Where a certificate stands. `accepted` exists so that the manifest can say
which value it is NOT in, rather than leaving the reader to infer it from an
absence. -/
inductive AcceptanceStatus where
  | mintedAwaitingAcceptance
  | accepted
  deriving DecidableEq, Repr

/-- One theorem that an acceptance would be an acceptance OF. -/
structure PreregAssertion where
  /-- The fully qualified name, carried for the reader; the citation that binds
  it is the definition block below. -/
  theoremName : String
  /-- The run the theorem is about. -/
  run : CandidateRun
  /-- Minted and awaiting acceptance, for every row of this manifest. -/
  status : AcceptanceStatus
  deriving DecidableEq, Repr

/-- How a pinned authority is named. -/
inductive PinKind where
  | sha256
  | gitCommit
  deriving DecidableEq, Repr

/-- One authority the assertion set is pinned to. COPIED from the certificates,
which the U49 producer generated from the two pinned sources; not re-taken here,
because a manifest that re-pinned would agree with today's tree instead of with
the certificates. -/
structure PinnedAuthority where
  /-- `control-map`, `trace:<run>`, `contract:<row>`. -/
  key : String
  /-- Whether `identity` is a content hash or a commit. -/
  kind : PinKind
  /-- The pinned value, verbatim. -/
  identity : String
  deriving DecidableEq, Repr

/-- The declared candidate invalidators: pending work that could make an
acceptance stale on arrival. Declared in advance and closed: an invalidator
discovered later is a new row and a new commit, not a quiet addition. -/
inductive InvalidatorId where\n"
   (str/join "\n" (for [i invalidators] (str "  | " (lean-ctor (:id i)))))
"\n  deriving DecidableEq, Repr

/-- One declared invalidator with the one question this manifest can settle
answered. `touchesCertificateDefinitions` is a property of what the
transcription READS and of what the emitted block CONTAINS, so it is measurable
here; whether the work has LANDED is a property of the live repositories and is
the meter's line. The grounds for each answer are in
`futon2:holes/labs/wm-contract/runs/F2-run4-preregistration/03-invalidators.edn`,
with the counts each was decided from. -/
structure Invalidator where
  /-- Which declared item. -/
  id : InvalidatorId
  /-- Would it change a definition the certificates are stated over? -/
  touchesCertificateDefinitions : Bool
  /-- Does it carry an executable measurement? An unmeasured item blocks. -/
  measured : Bool
  deriving DecidableEq, Repr

/-- THE ASSERTION SET. -/
def run4Assertions : List PreregAssertion :=
  ["
   (str/join ",\n   " (for [a assertions]
                        (str "{ theoremName := " (lean-str (:theorem a))
                             ", run := ." (lean-ctor (last (str/split (:run a) #"-")))
                             ", status := .mintedAwaitingAcceptance }")))
"]

/-- THE PINNED AUTHORITIES. -/
def run4Authorities : List PinnedAuthority :=
  ["
   (str/join ",\n   " (for [p authorities]
                        (str "{ key := " (lean-str (:key p))
                             ", kind := ." (if (= :sha256 (:kind p)) "sha256" "gitCommit")
                             ", identity := " (lean-str (:identity p)) " }")))
"]

/-- THE DECLARED INVALIDATION SET. -/
def run4Invalidators : List Invalidator :=
  ["
   (str/join ",\n   " (for [i invalidators]
                        (str "{ id := ." (lean-ctor (:id i))
                             ", touchesCertificateDefinitions := " (:touches-certificate-definitions? i)
                             ", measured := true }")))
"]

"
   (str/join "\n\n"
             (for [c certificates
                   :let [s (slug-of c) S (slug-cap c) rn (get-in c [:run :name])]]
               (str
                "/-- CITED BY REFERENCE: the conformance theorem for `" rn "`, at its\n"
                "stated type. If `wm" S "RunConformsToDrawnWiring` were renamed, removed or\n"
                "restated, this definition would not elaborate. -/\n"
                "theorem run4Certified" S "Conformance : runConformsToDrawnWiring " s "Routes :=\n"
                "  wm" S "RunConformsToDrawnWiring\n\n"
                "/-- CITED BY REFERENCE WITH ITS NUMBERS RESTATED: the census for `" rn "`.\n"
                "The eleven numbers are written out rather than cited by name, so that a census\n"
                "number moving breaks this module -- an acceptance made after such a move would\n"
                "be an acceptance of different numbers than the ones preregistered here. -/\n"
                "theorem run4Certified" S "Census :\n"
                "    " (census-block c) " :=\n"
                "  wm" S "RouteCensus")))
"

/-- THE MANIFEST'S OWN CENSUS, decided: how many assertions, all of them awaiting
acceptance; how many pinned authorities; how many declared invalidators, how many
of them touch the certificates' definitions, and how many are unmeasured (an
unmeasured invalidator is a declared hazard nobody checked, so the count that
matters is that it is zero). -/
theorem wmRun4PreregCensus :
    run4Assertions.length = " (count assertions) " ∧
      (run4Assertions.filter
        (fun a => decide (a.status = AcceptanceStatus.mintedAwaitingAcceptance))).length = " (count assertions) " ∧
      run4Authorities.length = " (count authorities) " ∧
      run4Invalidators.length = " (count invalidators) " ∧
      (run4Invalidators.filter (fun i => i.touchesCertificateDefinitions)).length = " (count touching) " ∧
      (run4Invalidators.filter (fun i => !i.measured)).length = 0 := by
  decide

/-- WHICH DECLARED INVALIDATORS TOUCH THE CERTIFICATES' DEFINITIONS, decided and
named rather than counted. This is the manifest entry the ruling asked for:
`likely disjoint` is not a manifest entry, so each declared item carries a
measured answer and the answers are stated as a list, which is false if an item
is added, removed or reclassified without regenerating. -/
theorem wmRun4InvalidatorsTouchingDefinitions :
    (run4Invalidators.filter (fun i => i.touchesCertificateDefinitions)).map (fun i => i.id)
      = [" (str/join ", " (for [i touching] (str "InvalidatorId." (lean-ctor (:id i))))) "] := by
  decide

end DarkTower.WarMachine.Run4Preregistration
"))

;; ------------------------------------------------------------------ emit ---

(defn pp-spit [path v]
  (spit path (with-out-str (pprint/pprint v))))

(io/make-parents (io/file outdir "x"))
(.mkdirs (io/file outdir))

(def fabricated (get-in controls [:C3 :fabricated]))
(def c3-pass? (not-any? #(str/includes? lean-module %) fabricated))
(def controls* (assoc-in controls [:C3 :pass?] c3-pass?))

(when-not c3-pass?
  (fail! "control C3: a fabricated name occurs in the emitted Lean module"))
(doseq [[k v] controls*]
  (when (false? (:pass? v)) (fail! "control" k "failed:" (:claim v))))

(pp-spit (str outdir "/00-source.edn")
         {:row :F2
          :epic "holes/labs/wm-contract/EPIC-run-era.md"
          :ruling "Joe, 2026-09-05 (RUN4 preregistration ruling, EPIC-run-era.md)"
          :produced-by "holes/labs/wm-contract/run4_prereg_transcribe.bb"
          :certificates (vec (for [c certificates]
                               {:path (str/replace (::path c) #"^" "holes/labs/wm-contract/")
                                :row (:worklist-item c)
                                :run (get-in c [:run :name])
                                :status (:status c)
                                :sha256 (sha256 (::path c))
                                :lean-block-sha256 (sha256 (str (::dir c) "/lean-block.lean"))}))
          :holes-lean {:path "mathlib4/DarkTower/WarMachine/Holes.lean"
                       :sha256 (sha256 holes-lean)}
          :emitted-lean "mathlib4/DarkTower/WarMachine/Run4Preregistration.lean"})

(pp-spit (str outdir "/01-assertions.edn")
         {:assertions assertions
          :acceptance-closes acceptance-closes
          :count (count assertions)})

(pp-spit (str outdir "/02-authorities.edn")
         {:authorities authorities
          :count (count authorities)
          :note (str "COPIED from the certificates, not re-taken. run4_readiness.bb compares"
                     " each identity to the live repositories; this file is the comparand.")})

(pp-spit (str outdir "/03-invalidators.edn")
         {:invalidators invalidators
          :count (count invalidators)
          :touching (mapv :id touching)
          :note (str "TOUCHES-CERTIFICATE-DEFINITIONS is settled here and is static."
                     " WHETHER IT HAS LANDED is run4_readiness.bb's :invalidators line.")})

(pp-spit (str outdir "/04-controls.edn") controls*)

(spit (str outdir "/Run4Preregistration.lean") lean-module)

(doseq [p @problems] (println "  PROBLEM" p))
(println (format "run4_prereg_transcribe: %d certificates | %d assertions | %d authorities | %d invalidators (%d touching) -> %s"
                 (count certificates) (count assertions) (count authorities)
                 (count invalidators) (count touching) outdir))
(if (seq @problems)
  (do (println (format "run4_prereg_transcribe: FAIL (%d problems) exit-convention=0-pass/1-fail" (count @problems)))
      (System/exit 1))
  (println "run4_prereg_transcribe: PASS exit-convention=0-pass/1-fail"))
