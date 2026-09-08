#!/usr/bin/env bb
;; U46 -- TRANSCRIBE THE PINNED find-snatch RECORD INTO LEAN.
;;
;;   bb holes/labs/wm-contract/u46_find_transcribe.bb [outdir]
;;
;; The four `find` declarations findF1Containment / findF2Receipted /
;; findF3NonSelfCertifying / findF4Falsifiable were narrowed on 2026-08-31 to
;; "exactly the recorded-row invariant" over one pinned FindReceiptRow, and the
;; J9 criterion (RUNBOOK.md) asks leg (3) of that criterion for a Lean
;; transcription proved by `decide`. This script IS the transcription step: it
;; reads the pinned fixture, converts it to the Lean literal that
;; mathlib4 DarkTower/WarMachine/Holes.lean holds, and writes the artifacts a
;; reviewer needs to check that the literal says what the record says.
;;
;; READ-ONLY over futon3. No tick, no run lock, no substrate call, no network.
;; The one thing it will not do is run futon3's `find_snatch.clj`: that check
;; REWRITES the pinned fixture from the current library (see control C6 and
;; C500), so a producer that invoked it would move the thing it transcribes.
;;
;; DETERMINISM. No wall-clock field is written; two runs over an unchanged
;; fixture produce byte-identical artifacts.
;;
;; WHAT IT WRITES (outdir defaults to runs/U46-find-rows):
;;   00-source.edn      the pinned fixture's identity: path, sha256, :as-of,
;;                      repository size, scenario/round counts.
;;   01-rows.edn        the 34 round rows and 6 scenario rows as transcribed,
;;                      in fixture order, still in EDN keywords.
;;   02-predicates.edn  F1-F4 evaluated over those rows by THIS script, at the
;;                      same grain find_snatch.clj:157-177 uses.
;;   03-controls.edn    the controls (C1-C6 below).
;;   lean-block.lean    the generated Lean literal block.
;;   README.md          what the run found, with pointers.
;;
;; CONTROLS.
;;   C1 the fixture's sha256 equals the value pinned in all four Holes.lean
;;      docstrings. A transcription of an unpinned file proves
;;      nothing about the declaration.
;;   C2 round-trip: every Lean constructor name emitted decodes back to the
;;      fixture keyword it came from, and the decoded rows equal the fixture
;;      rows as sets. This is what makes the literal a transcription rather
;;      than a retyping.
;;   C3 a fabricated pattern name occurs in no row, no repository, no emitted
;;      Lean text.
;;   C4 negative: a mutated row (a selected member outside the repository, a
;;      selected member with its receipt dropped, a score-alone receipt, the
;;      zero-mass member selected) is REJECTED by this script's own F1/F2/F3/F4
;;      evaluators -- the four mutations futon3's --negative-f1..f4 make.
;;   C5 independence: on this record `receipted` and `nonSelfCertifying` are
;;      the same set in all 34 rows, so F3 discriminates nothing that F2 does
;;      not. Reported, not excused: what separates them is the --negative-f3
;;      control, not the record.
;;   C6 the former snapshot divergence is closed: the pinned fixture was
;;      regenerated from the committed 24-pattern library and its :as-of is
;;      the commit that last touched library/snatch. The producer remains
;;      read-only; the regeneration is a separate, explicit step.

(require '[clojure.edn :as edn]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str]
         '[babashka.fs :as fs])

(def fixture-path
  (or (System/getenv "U46_FIXTURE")
      "../futon3/checks/find-snatch.edn"))

(def pinned-sha256
  "c11673ea7164e90b10cc378ab6b2dfe14e545449d85e0dde70d5c2282e2430ce")

(def reconciled-library-as-of
  "e58576cec0f14c3da4667ed452d522c561487ee8")

(defn sha256 [path]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")]
    (->> (.digest md (java.nio.file.Files/readAllBytes (fs/path path)))
         (map #(format "%02x" %))
         (apply str))))

;; ---------------------------------------------------------------- names ----

(defn lean-name
  "A fixture keyword as a Lean constructor name: kebab -> lowerCamel."
  [kw]
  (let [parts (str/split (name kw) #"-")]
    (apply str (first parts) (map str/capitalize (rest parts)))))

(defn decode-name
  "The inverse of `lean-name` against a known vocabulary (control C2)."
  [vocab s]
  (first (filter #(= s (lean-name %)) vocab)))

(defn scenario-key [s] [(:treatment s) (:disposition s)])

(defn scenario-lean-name [[treatment disposition]]
  (str (name treatment) (str/capitalize (name disposition))))

;; ---------------------------------------------------------------- rows -----

(defn non-self-certifying
  "The receipted patterns whose receipt is not score-alone AND cites a warrant
   file -- find_snatch.clj:166-171, transcribed as a set."
  [receipts]
  (into (sorted-set)
        (keep (fn [[id receipt]]
                (when (and (not= :score-alone (:route receipt))
                           (string? (get-in receipt [:warrant :file])))
                  id)))
        receipts))

(defn round-rows
  "One row per recorded round -- the grain find_snatch.clj:157-171 iterates."
  [fixture]
  (vec (for [s (:scenarios fixture)
             r (:round-results s)
             :let [f (:find r)
                   receipts (:receipts f)]]
         {:scenario (scenario-key s)
          :round (:round r)
          :selected (vec (:selected f))
          :receipted (vec (sort (keys receipts)))
          :non-self-certifying (vec (non-self-certifying receipts))
          :zero-mass [(get-in s [:f4 :zero-mass-pattern])]
          :absence (:absence f)})))

(defn scenario-rows
  "One row per recorded scenario, its selection being the recorded
   :selected-union -- the grain find_snatch.clj:172-177 iterates for F4. The
   receipt sets are the union of the scenario's rounds, which is a recorded
   quantity too; nothing here is invented to fill a field."
  [fixture]
  (vec (for [s (:scenarios fixture)
             :let [rounds (:round-results s)
                   receipts (mapcat #(keys (get-in % [:find :receipts])) rounds)
                   nsc (mapcat #(non-self-certifying (get-in % [:find :receipts])) rounds)]]
         {:scenario (scenario-key s)
          :round nil
          :selected (vec (:selected-union s))
          :receipted (vec (sort (distinct receipts)))
          :non-self-certifying (vec (sort (distinct nsc)))
          :zero-mass [(get-in s [:f4 :zero-mass-pattern])]
          :absence nil})))

;; ---------------------------------------------------------- predicates -----

(defn f1 [repository row]
  (and (every? (set repository) (:selected row))
       (or (seq (:selected row))
           (= :no-pattern-addresses-this-tension (:absence row)))))

(defn f2 [_repository row]
  (every? (set (:receipted row)) (:selected row)))

(defn f3 [_repository row]
  (every? (set (:non-self-certifying row)) (:selected row)))

(defn f4 [repository row]
  (and (seq repository)
       (boolean (some (fn [p] (and ((set repository) p)
                                   (not ((set (:selected row)) p))))
                      (:zero-mass row)))))

;; ---------------------------------------------------------------- lean -----

(defn lean-list
  "The ids as a Lean list literal, wrapped at 92 columns with `indent` spaces of
   continuation so the transcribed rows stay readable beside their docstrings."
  ([xs] (lean-list xs 0 0))
  ([xs indent used]
   (let [items (map #(str "." (lean-name %)) xs)
         pad (apply str (repeat indent \space))]
     (loop [[item & more] items, line (str "[" ) out [] col (+ used 1)]
       (cond
         (nil? item) (str/join "\n" (conj out (str line "]")))
         (and (> col indent) (> (+ col (count item) 2) 92))
         (recur (cons item more) pad (conj out (str/trimr line)) indent)
         :else (recur more (str line item (when more ", "))
                      out (+ col (count item) 2)))))))

(defn lean-row [row]
  (str "  { scenario := ." (scenario-lean-name (:scenario row))
       ", round := " (if (:round row) (str "some " (:round row)) "none")
       "\n    selected := " (lean-list (:selected row) 6 (count "    selected := "))
       "\n    receipted := " (lean-list (:receipted row) 6 (count "    receipted := "))
       "\n    nonSelfCertifying := " (lean-list (:non-self-certifying row) 6
                                                (count "    nonSelfCertifying := "))
       "\n    absence := " (if (:absence row)
                             (str "some ." (lean-name (:absence row)))
                             "none")
       " }"))

(defn lean-block [fixture rounds scenarios]
  (let [repository (:repository fixture)
        scen-keys (mapv scenario-key (:scenarios fixture))]
    (str
     "/-! ### The pinned `find-snatch` record, transcribed (worklist `:U46`)\n\n"
     "`futon3:checks/find-snatch.edn`, sha256 `" pinned-sha256 "`, whose\n"
     "`:as-of` is the futon3 commit `" (:as-of fixture) "` that last touched\n"
     "`library/snatch` -- " (count repository) " authored patterns, "
     (count (:scenarios fixture)) " scenarios, " (count rounds) " recorded rounds.\n"
     "This block is GENERATED from that file by\n"
     "`futon2:holes/labs/wm-contract/u46_find_transcribe.bb`; edit the fixture and\n"
     "regenerate rather than editing the literals.\n-/\n\n"
     "/-- The " (count repository) " authored Snatch patterns of the pinned record, in its\n"
     "sorted order. Constructor names are the recorded ids in lowerCamel. -/\n"
     "inductive SnatchPattern where\n"
     (str/join "\n" (map #(str "  | " (lean-name %)) repository))
     "\n  deriving DecidableEq, Repr\n\n"
     "/-- The recorded repository as a list -- what `findF1Containment` contains\n"
     "selection within, and where `findF4Falsifiable` finds its zero-mass member. -/\n"
     "def snatchRepository : List SnatchPattern :=\n  "
     (lean-list repository 2 2) "\n\n"
     "/-- The six recorded scenarios, `treatment`/`disposition`\n"
     "(`find_snatch.clj:22-24`, declaration order). -/\n"
     "inductive FindSnatchScenario where\n"
     (str/join "\n" (map #(str "  | " (scenario-lean-name %)) scen-keys))
     "\n  deriving DecidableEq, Repr\n\n"
     "/-- The declared zero-mass pattern per scenario (`find_snatch.clj:25-31`),\n"
     "as recorded in each scenario's `:f4` map. -/\n"
     "def findSnatchZeroMass : FindSnatchScenario → List SnatchPattern\n"
     (str/join "\n"
               (map (fn [s]
                      (str "  | ." (scenario-lean-name (scenario-key s)) " => "
                           (lean-list [(get-in s [:f4 :zero-mass-pattern])])))
                    (:scenarios fixture)))
     "\n\n"
     "/-- A recorded find row as a Lean literal: finite lists, so every predicate\n"
     "over it is decidable. `round` is `some n` for a recorded round and `none`\n"
     "for the scenario-grain row, whose `selected` is the recorded\n"
     "`:selected-union`. -/\n"
     "structure FindSnatchRowLit where\n"
     "  scenario : FindSnatchScenario\n"
     "  round : Option Nat\n"
     "  selected : List SnatchPattern\n"
     "  receipted : List SnatchPattern\n"
     "  nonSelfCertifying : List SnatchPattern\n"
     "  absence : Option TypedAbsence\n"
     "  deriving DecidableEq, Repr\n\n"
     "/-- The literal read as the `FindReceiptRow` the four declarations speak\n"
     "about: each list becomes the set of its members, the repository and the\n"
     "zero-mass set come from the record's own two constants. -/\n"
     "def FindSnatchRowLit.toRow (r : FindSnatchRowLit) :\n"
     "    FindReceiptRow FindSnatchScenario SnatchPattern where\n"
     "  scenario := r.scenario\n"
     "  repository := {p | p ∈ snatchRepository}\n"
     "  selected := {p | p ∈ r.selected}\n"
     "  receipted := {p | p ∈ r.receipted}\n"
     "  nonSelfCertifying := {p | p ∈ r.nonSelfCertifying}\n"
     "  zeroMass := {p | p ∈ findSnatchZeroMass r.scenario}\n"
     "  absence := r.absence\n\n"
     "/-- The " (count rounds) " recorded rounds, in fixture order. The grain\n"
     "`find_snatch.clj:157-171` iterates for F1, F2 and F3. -/\n"
     "def findSnatchRounds : List FindSnatchRowLit :=\n[\n"
     (str/join ",\n" (map lean-row rounds))
     "\n]\n\n"
     "/-- The " (count scenarios) " recorded scenarios, each with its `:selected-union`.\n"
     "The grain `find_snatch.clj:172-177` iterates for F4. -/\n"
     "def findSnatchScenarios : List FindSnatchRowLit :=\n[\n"
     (str/join ",\n" (map lean-row scenarios))
     "\n]\n")))

;; ------------------------------------------------------------- controls ----

(defn mutate [row kind repository]
  (case kind
    :f1 (update row :selected conj (first (remove (set repository) [:outside-repository])))
    :f2 (update row :receipted #(vec (remove #{(first (:selected row))} %)))
    :f3 (update row :non-self-certifying #(vec (remove #{(first (:selected row))} %)))
    :f4 (update row :selected conj (first (:zero-mass row)))))

(defn controls [fixture rounds scenarios lean-text]
  (let [repository (:repository fixture)
        fabricated :not-a-pattern-in-this-repository
        nonempty-round (first (filter #(seq (:selected %)) rounds))
        decoded (fn [row]
                  (update-vals (select-keys row [:selected :receipted :non-self-certifying])
                               (fn [xs] (mapv #(decode-name repository (lean-name %)) xs))))]
    {:C1-fixture-sha-is-the-pinned-one
     {:expected pinned-sha256 :actual (sha256 fixture-path)
      :pass (= pinned-sha256 (sha256 fixture-path))}
     :C2-constructor-names-round-trip
     {:patterns-round-trip (= (vec repository)
                              (mapv #(decode-name repository (lean-name %)) repository))
      :rows-round-trip (every? (fn [row] (= (decoded row)
                                            (select-keys row [:selected :receipted
                                                              :non-self-certifying])))
                               (concat rounds scenarios))
      :distinct-lean-names (= (count repository)
                              (count (distinct (map lean-name repository))))
      :pass (and (= (vec repository)
                    (mapv #(decode-name repository (lean-name %)) repository))
                 (every? (fn [row] (= (decoded row)
                                      (select-keys row [:selected :receipted
                                                        :non-self-certifying])))
                         (concat rounds scenarios))
                 (= (count repository) (count (distinct (map lean-name repository)))))}
     :C3-fabricated-pattern-absent
     {:name fabricated
      :in-repository (boolean ((set repository) fabricated))
      :in-any-row (boolean (some #(some #{fabricated}
                                        (concat (:selected %) (:receipted %)
                                                (:non-self-certifying %) (:zero-mass %)))
                                 (concat rounds scenarios)))
      :in-lean-text (str/includes? lean-text (lean-name fabricated))
      :pass (not (or ((set repository) fabricated)
                     (some #(some #{fabricated}
                                  (concat (:selected %) (:receipted %)
                                          (:non-self-certifying %) (:zero-mass %)))
                           (concat rounds scenarios))
                     (str/includes? lean-text (lean-name fabricated))))}
     :C4-mutations-rejected
     (let [row nonempty-round
           scen (first scenarios)
           results {:f1 (f1 repository (mutate row :f1 repository))
                    :f2 (f2 repository (mutate row :f2 repository))
                    :f3 (f3 repository (mutate row :f3 repository))
                    :f4 (f4 repository (mutate scen :f4 repository))}]
       {:mutated-round [(:scenario row) (:round row)]
        :mutated-scenario (:scenario scen)
        :verdicts results
        :pass (every? false? (vals results))})
     :C5-f3-not-independent-of-f2-on-this-record
     {:rows-where-receipted-equals-non-self-certifying
      (count (filter #(= (set (:receipted %)) (set (:non-self-certifying %))) rounds))
      :rows (count rounds)
      :receipt-routes (frequencies (for [s (:scenarios fixture)
                                         r (:round-results s)
                                         [_ receipt] (get-in r [:find :receipts])]
                                     (:route receipt)))
      :note (str "F3 adds no discrimination over this record: every recorded "
                 "receipt is :structured-antecedent with a warrant file, so the "
                 "non-self-certifying set IS the receipted set. What separates "
                 "F3 from F2 here is futon3's --negative-f3 control, not the record.")}
     :C6-pin-reconciled-with-committed-library
     {:fixture-as-of (:as-of fixture)
      :expected-library-as-of reconciled-library-as-of
      :repository-count (count repository)
      :pass (and (= reconciled-library-as-of (:as-of fixture))
                 (= 24 (count repository)))
      :note (str "The fixture was explicitly regenerated by futon3 "
                 "checks/find_snatch.clj from the committed 24-pattern library. "
                 "Its :as-of is the commit that last touched library/snatch; this "
                 "read-only producer transcribes that reconciled fixture.")} }))

;; ----------------------------------------------------------------- main ----

(defn -main [& args]
  (let [outdir (or (first args) "holes/labs/wm-contract/runs/U46-find-rows")
        fixture (edn/read-string (slurp fixture-path))
        repository (:repository fixture)
        rounds (round-rows fixture)
        scenarios (scenario-rows fixture)
        lean-text (lean-block fixture rounds scenarios)
        preds (fn [rows]
                (mapv (fn [row]
                        {:scenario (:scenario row) :round (:round row)
                         :selected-count (count (:selected row))
                         :f1 (f1 repository row) :f2 (f2 repository row)
                         :f3 (f3 repository row) :f4 (f4 repository row)})
                      rows))
        round-preds (preds rounds)
        scenario-preds (preds scenarios)
        ctrls (controls fixture rounds scenarios lean-text)]
    (fs/create-dirs outdir)
    (spit (fs/file outdir "00-source.edn")
          (with-out-str
            (pprint/pprint
             {:fixture {:repo "futon3" :path "checks/find-snatch.edn"
                        :sha256 (sha256 fixture-path)
                        :as-of (:as-of fixture)}
              :repository-count (count repository)
              :scenario-count (count (:scenarios fixture))
              :round-count (count rounds)
              :laws (:laws fixture)
              :drift-mismatch-count (get-in fixture [:drift :mismatch-count])
              :grains {:rounds "find_snatch.clj:157-171 (F1, F2, F3)"
                       :scenarios "find_snatch.clj:172-177 (F4, over :selected-union)"}})))
    (spit (fs/file outdir "01-rows.edn")
          (with-out-str (pprint/pprint {:rounds rounds :scenarios scenarios})))
    (spit (fs/file outdir "02-predicates.edn")
          (with-out-str
            (pprint/pprint
             {:rounds round-preds
              :scenarios scenario-preds
              :totals {:rounds {:f1 (count (filter :f1 round-preds))
                                :f2 (count (filter :f2 round-preds))
                                :f3 (count (filter :f3 round-preds))
                                :f4 (count (filter :f4 round-preds))
                                :of (count round-preds)}
                       :scenarios {:f1 (count (filter :f1 scenario-preds))
                                   :f2 (count (filter :f2 scenario-preds))
                                   :f3 (count (filter :f3 scenario-preds))
                                   :f4 (count (filter :f4 scenario-preds))
                                   :of (count scenario-preds)}}})))
    (spit (fs/file outdir "03-controls.edn")
          (with-out-str (pprint/pprint ctrls)))
    (spit (fs/file outdir "lean-block.lean") lean-text)
    (println (format "u46: %d rounds, %d scenarios; F1 %d/%d F2 %d/%d F3 %d/%d over rounds; F4 %d/%d over scenarios"
                     (count rounds) (count scenarios)
                     (count (filter :f1 round-preds)) (count round-preds)
                     (count (filter :f2 round-preds)) (count round-preds)
                     (count (filter :f3 round-preds)) (count round-preds)
                     (count (filter :f4 scenario-preds)) (count scenario-preds)))
    (doseq [[k v] (sort-by key ctrls)]
      (when (contains? v :pass)
        (println (format "  control %s %s" (name k) (if (:pass v) "PASS" "FAIL")))))
    (println (format "  wrote %s" outdir))
    (when-not (every? true? (keep :pass (vals ctrls)))
      (System/exit 1))))

(apply -main *command-line-args*)
