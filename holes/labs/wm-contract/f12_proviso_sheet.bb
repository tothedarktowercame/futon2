#!/usr/bin/env bb
;; F12 slice 10: the exemplar-proviso choice sheet.
;;
;; The sheet READS its values; it does not restate them. Every number and every
;; boolean that reaches C582 is present here under a key path and was derived
;; from an artifact, from aif-equations.edn, or from a Lean source file. Where a
;; value cannot be derived from those, this script emits a marker saying so
;; rather than a literal standing in for a measurement.
;;
;; NO RULING IS TAKEN. Nothing here writes :ruling, :decision, :chosen-arm or
;; :decisions, and neither aif-equations.edn nor control-map-edges.edn is
;; written. The choice is Joe's.

(require '[babashka.fs :as fs]
         '[babashka.process :as p]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def lab (fs/parent (fs/file *file*)))
(def repo (-> lab fs/parent fs/parent fs/parent))
(def mathlib "/home/joe/code/mathlib4")
(def run-dir (fs/path lab "runs/F12-organise"))
(def paths {:ruled (fs/path run-dir "17-ruled-carrier.edn")
            :ants (fs/path run-dir "18-ants-exemplar.edn")
            :mining (fs/path run-dir "19-mining-exemplar.edn")
            :snatch (fs/path run-dir "24-snatch-exemplar.edn")
            :registry (fs/path lab "aif-equations.edn")
            :ruled-lean (fs/path mathlib "DarkTower/WarMachine/F12RuledCarrier.lean")
            :ants-lean (fs/path mathlib "DarkTower/WarMachine/F12AntsExemplar.lean")
            :mining-lean (fs/path mathlib "DarkTower/WarMachine/F12MiningExemplar.lean")
            :snatch-lean (fs/path mathlib "DarkTower/WarMachine/F12SnatchExemplar.lean")
            :holes (fs/path mathlib "DarkTower/WarMachine/Holes.lean")})

(defn read-edn [pth] (edn/read-string (slurp (str pth))))
(defn sha [pth] (first (str/split (str/trim (:out (p/shell {:out :string} "sha256sum" (str pth)))) #"\s+")))
(defn git-out [dir & args] (str/trim (:out (apply p/shell {:dir (str dir) :out :string} "git" args))))
(defn present? [s re] (boolean (re-find re s)))
(defn pass? [v] (contains? #{true :pass} v))

;; ---------------------------------------------------------------------------
;; Clause shapes, read from the ruled predicate rather than enumerated by hand.
;;
;; This is the correction the review made to the dispatching packet's own
;; premise. The packet asked for a "vacuity table" over all seven ruled clauses.
;; Five of them are UNIVERSALLY QUANTIFIED EQUATIONS with no antecedent
;; (DarkTower/WarMachine/F12RuledCarrier.lean:23-33), so there is nothing about
;; them that can be vacuous, and any per-exemplar "witness" for such a clause is
;; an invention. Only `o2` and `o4` are implications. The classification is
;; therefore derived from the structure's own text, and the antecedent it found
;; is recorded beside it so the classification can be audited.
;; ---------------------------------------------------------------------------

(defn clause-blocks
  "Text of each field of `ConformantOrganiseRuled`, keyed by field name."
  [ruled-lean]
  (let [body (-> ruled-lean
                 (str/split #"structure ConformantOrganiseRuled") second
                 (str/split #"\n\n") first)
        idx (vec (re-seq #"(?m)^  (\w+) :" body))
        starts (loop [acc [] s body seen 0]
                 (if-let [m (re-find #"(?m)^  (\w+) :" s)]
                   (let [at (str/index-of s (first m))]
                     (recur (conj acc [(second m) (+ seen at)]) (subs s (+ at 1)) (+ seen at 1)))
                   acc))]
    (assert (= (count idx) (count starts)) "clause scan disagreed with itself")
    (into (sorted-map)
          (map-indexed (fn [i [nm at]]
                         [(keyword nm)
                          (str/trim (subs body at (if-let [[_ nxt] (get starts (inc i))] nxt (count body))))])
                       starts))))

(defn clause-shape [text]
  ;; An implication is exactly a clause whose statement carries `→` after the
  ;; binder. `o3`'s `↔` is an equivalence, not an antecedent to satisfy.
  (let [after (or (second (str/split text #"," 2)) text)]
    (if (str/includes? after "→")
      {:shape :implication
       :antecedent (str/trim (first (str/split (str/replace after #"\n\s+" " ") #"→")))}
      {:shape :unconditional-equation :antecedent :none})))

;; ---------------------------------------------------------------------------
;; Per-exemplar measurements.
;; ---------------------------------------------------------------------------

(defn ants-facts [ants ants-lean]
  (let [o (get-in ants [:orders :lean])
        ;; antsRepo's relation is the empty relation
        ;; (DarkTower/WarMachine/F12AntsExemplar.lean:22), so fastForward over it
        ;; is empty and the file proves exactly that at
        ;; DarkTower/WarMachine/F12AntsExemplar.lean:72-74. Derived from the
        ;; source, not asserted here.
        empty-rel? (present? ants-lean #"standsOn := fun _ _ => False")]
    {:verdict (pass? (:verdict ants))
     :ruled-organise-type? (present? ants-lean #"organiseAnts : RuledOrganiseType")
     :added-by-organise-empty? (present? ants-lean #"addedByOrganise := ∅")
     :score-type-degenerate? false
     :score-type-basis :rat-score-in-ruled-organise-type
     :score-moves? (not (get-in ants [:record-assertions :score-unchanged]))
     :precedence-moves? (not= (:precedence-before o) (:precedence-after o))
     :acting-order-moves? (not= (:acting-before o) (:acting-after o))
     :organised-edge-count (if empty-rel? 0 :not-derivable)
     :organised-edges-basis (if empty-rel? :repository-relation-is-empty :not-derivable)
     ;; The ants artifact records no edge set at all, so whether this exemplar
     ;; reproduces "its" edges is NOT MEASURABLE rather than trivially true.
     ;; Calling it true would count an absent record as agreement.
     :recorded-edges :not-recorded-in-artifact
     :reproduces-recorded-edges? :not-measurable}))

(defn field-pair
  "The two sides of a before/after pair as they are WRITTEN in the carrier body,
  e.g. `precedenceBefore := X, precedenceAfter := Y` -> [\"X\" \"Y\"]. Comparing the
  two written values derives movement in BOTH directions; matching a single
  expected line could only ever establish that the pair does not move, so a
  source that started moving would silently drop out of the table."
  [lean-src base]
  (when-let [m (re-find (re-pattern (str base "Before := ([^,\n]+), " base "After := ([^,\n}]+)"))
                        lean-src)]
    [(str/trim (nth m 1)) (str/trim (nth m 2))]))

(defn mining-facts [mining mining-lean]
  (let [t (:transcription mining)
        rec (get-in t [:edges :record])
        lean (get-in t [:edges :lean])
        ;; Both O4 sides are instantiated from the same recorded list
        ;; (DarkTower/WarMachine/F12MiningExemplar.lean:63), which is why the
        ;; antecedent is false. Read as a pair and compared, so a source in which
        ;; they diverge reports movement rather than non-derivability.
        prec (field-pair mining-lean "precedence")
        acting (field-pair mining-lean "actingOrder")
        unit? (boolean (:score-type-unit? t))]
    {:verdict (pass? (:verdict mining))
     :ruled-organise-type? (present? mining-lean #"organiseMining : RuledOrganiseType")
     :added-by-organise-empty? (present? mining-lean #"addedByOrganise := ∅")
     :score-type-degenerate? unit?
     :score-type-basis (if unit? :score-typed-unit :not-derivable)
     ;; With Score := Unit the two scores are equal for every function at the
     ;; type (proved at DarkTower/WarMachine/F12MiningExemplar.lean:72), so this
     ;; follows from the type rather than from a reading of the record.
     :score-moves? (if unit? false :not-derivable)
     :precedence-moves? (if prec (not= (first prec) (second prec)) :not-derivable)
     :precedence-basis (if prec {:written-before (first prec) :written-after (second prec)} :not-derivable)
     :acting-order-moves? (if acting (not= (first acting) (second acting)) :not-derivable)
     :organised-edge-count (count lean)
     :organised-edges-basis :artifact-transcription-edges-lean
     :recorded-edges rec
     :reproduces-recorded-edges? (= (set rec) (set lean))}))

(defn snatch-facts [snatch snatch-lean]
  (let [t (:transcription snatch)
        full (get-in snatch [:edge-routes :full-node])
        ;; organisedEdges AT THE RULED SIGNATURE is the no-bootstrap set
        ;; (DarkTower/WarMachine/F12SnatchExemplar.lean:108), not the row's
        ;; recorded ten.
        ruled (get-in snatch [:edge-routes :no-bootstrap])
        added (get-in snatch [:edge-routes :lean-in-added])]
    {:verdict (pass? (:verdict snatch))
     :ruled-organise-type? (present? snatch-lean #"organiseSnatch : RuledOrganiseType")
     :added-by-organise-empty? (empty? added)
     :score-type-degenerate? false
     :score-type-basis :nat-score-moves-on-the-record
     :score-moves? (not= (get-in t [:score-before :derived]) (get-in t [:score-after :derived]))
     :precedence-moves? (not= (get-in t [:precedence-before :derived]) (get-in t [:precedence-after :derived]))
     :acting-order-moves? (not= (get-in t [:acting-before :derived]) (get-in t [:acting-after :derived]))
     :organised-edge-count (count ruled)
     :organised-edges-basis :artifact-edge-routes-no-bootstrap
     :recorded-edges full
     :reproduces-recorded-edges? (= (set full) (set ruled))}))

(defn conclusions [inputs]
  (let [{:keys [ruled ants mining snatch ruled-lean ants-lean mining-lean snatch-lean registry]} inputs
        exemplars {:ants (ants-facts ants ants-lean)
                   :mining (mining-facts mining mining-lean)
                   :snatch (snatch-facts snatch snatch-lean)}
        blocks (clause-blocks ruled-lean)
        clause-names (mapv keyword (:clauses ruled))
        ;; Only the two implications have an antecedent that an exemplar can
        ;; satisfy or fail to satisfy; the antecedent each one tests is named
        ;; here so the row can be checked against the clause text.
        satisfies {:o2 (fn [e] (let [n (:organised-edge-count e)] (if (number? n) (pos? n) :not-derivable)))
                   :o4 (fn [e] (:precedence-moves? e))}
        coverage
        (into (sorted-map)
              (for [c clause-names
                    :let [shape (clause-shape (get blocks c ""))]]
                [c (if (= :unconditional-equation (:shape shape))
                     ;; No antecedent: vacuity does not apply and no exemplar is
                     ;; claimed as a witness for it.
                     (merge shape {:vacuity-applies? false})
                     (let [f (get satisfies c)
                           per (into (sorted-map) (for [[k e] exemplars] [k (f e)]))
                           ws (vec (sort (keep (fn [[k v]] (when (true? v) k)) per)))]
                       (merge shape {:vacuity-applies? true
                                     :antecedent-satisfied-by ws
                                     :per-exemplar per
                                     :vacuous-on (vec (sort (keep (fn [[k v]] (when (false? v) k)) per)))})))]))
        carrier (get-in registry [:choices :organise-carrier])
        sorry-c (get-in registry [:choices :organise-sorry])
        rk (keyword "ruling")
        subtraction-stressing (vec (sort (keep (fn [[k e]] (when-not (:added-by-organise-empty? e) k)) exemplars)))]
    {:exemplars exemplars
     :clause-shapes (into (sorted-map) (for [c clause-names] [c (:shape (clause-shape (get blocks c "")))]))
     :clause-coverage coverage
     :all-exemplars-pass? (every? #(and (:verdict %) (:ruled-organise-type? %)) (vals exemplars))
     :subtraction-stressing-exemplars subtraction-stressing
     :subtraction-stressing-exemplar-reproduces?
     (into (sorted-map) (for [k subtraction-stressing] [k (get-in exemplars [k :reproduces-recorded-edges?])]))
     :reproduction (into (sorted-map) (for [[k e] exemplars] [k (:reproduces-recorded-edges? e)]))
     :registry-evidence {:carrier-status (:status carrier)
                         :carrier-record (select-keys (get carrier rk) [:by :at :consequence])
                         :sorry-status (:status sorry-c)
                         :sorry-record (select-keys (get sorry-c rk) [:by :at :consequence])
                         :other-organise-choice-count
                         (dec (count (filter #(str/starts-with? (name %) "organise-") (keys (:choices registry)))))}}))

(def base-inputs {:ruled (read-edn (:ruled paths)) :ants (read-edn (:ants paths))
                  :mining (read-edn (:mining paths)) :snatch (read-edn (:snatch paths))
                  :registry (read-edn (:registry paths))
                  :ruled-lean (slurp (str (:ruled-lean paths)))
                  :ants-lean (slurp (str (:ants-lean paths)))
                  :mining-lean (slurp (str (:mining-lean paths)))
                  :snatch-lean (slurp (str (:snatch-lean paths)))})
(def baseline (conclusions base-inputs))

;; A plant NAMES the conclusion it must move. Comparing whole result maps would
;; let any plant that perturbs any field at all report success, which proves
;; nothing about which conclusion depends on which input.
(defn plant [id at mutate]
  (let [tmp (fs/create-temp-dir {:prefix "f12-proviso-plant-"})
        before (fs/path tmp "input-before.edn") after-f (fs/path tmp "input-after.edn")
        mutated (mutate base-inputs)]
    (spit (str before) (pr-str base-inputs))
    (spit (str after-f) (pr-str mutated))
    (let [landed? (not= (sha before) (sha after-f))
          after (conclusions (read-edn after-f))
          was (get-in baseline at) now (get-in after at)
          answer {:plant id :conclusion-under-test at :landed? landed?
                  :conclusion-before was :conclusion-after now
                  :flipped? (not= was now)}]
      (fs/delete-tree tmp)
      (assoc answer :temporary-copy-removed? (not (fs/exists? tmp))))))

(def plants
  [(plant :snatch-no-bootstrap-becomes-full [:reproduction :snatch]
          #(assoc-in % [:snatch :edge-routes :no-bootstrap] (get-in % [:snatch :edge-routes :full-node])))
   (plant :snatch-added-emptied [:subtraction-stressing-exemplars]
          #(assoc-in % [:snatch :edge-routes :lean-in-added] []))
   (plant :snatch-precedence-flattened [:clause-coverage :o4 :antecedent-satisfied-by]
          #(assoc-in % [:snatch :transcription :precedence-after :derived]
                     (get-in % [:snatch :transcription :precedence-before :derived])))
   (plant :mining-edges-emptied [:clause-coverage :o2 :antecedent-satisfied-by]
          #(assoc-in % [:mining :transcription :edges :lean] []))
   ;; Carried because a review plant caught the derivation being one-sided: an
   ;; earlier version matched a single expected line and so could only ever
   ;; report "does not move", leaving a diverged source as :not-derivable.
   (plant :mining-precedence-diverges [:clause-coverage :o4 :antecedent-satisfied-by]
          #(update % :mining-lean str/replace
                   "precedenceBefore := miningPrecedence, precedenceAfter := miningPrecedence"
                   "precedenceBefore := miningPrecedence, precedenceAfter := [0, 2, 1]"))
   (plant :ants-verdict-false [:all-exemplars-pass?]
          #(assoc-in % [:ants :verdict] false))
   (plant :o2-implication-arrow-removed [:clause-shapes :o2]
          #(update % :ruled-lean str/replace
                   "(f t sel repo adm).organisedEdges u v → Reach repo.standsOn u v"
                   "(f t sel repo adm).organisedEdges u v = Reach repo.standsOn u v"))
   ;; Control: perturbs something no conclusion reads. It must NOT move.
   (plant :control-irrelevant-check-label [:all-exemplars-pass?]
          #(assoc-in % [:ants :check] :mutated-control))])

(def input-digests (into (sorted-map) (for [[k pth] paths] [k (sha pth)])))
(def arms
  (let [ex (:exemplars baseline)
        reproduces (fn [v] (true? v))
        n-repro (count (filter reproduces (map :reproduces-recorded-edges? (vals ex))))
        n-not (count (filter false? (map :reproduces-recorded-edges? (vals ex))))
        n-unmeasurable (count (filter #(= :not-measurable %) (map :reproduces-recorded-edges? (vals ex))))
        stressing (:subtraction-stressing-exemplars baseline)]
    {:a {:label "discharged-as-it-stands"
         :cost {:exemplars-reproducing-their-recorded-edges n-repro
                :exemplars-not-reproducing n-not
                :exemplars-where-reproduction-is-not-measurable n-unmeasurable
                :what-is-paid :the-only-row-that-stresses-the-ruled-subtraction-is-the-one-that-does-not-reproduce}}
     :b {:label "discharged-by-snatch-alone"
         :cost {:snatch-reproduces? (get-in ex [:snatch :reproduces-recorded-edges?])
                :snatch-ruled-edge-count (get-in ex [:snatch :organised-edge-count])
                :snatch-recorded-edge-count (count (get-in ex [:snatch :recorded-edges]))
                :what-is-paid :the-designated-exemplar-keeps-one-of-its-ten-recorded-edges}}
     :c {:label "not-discharged"
         :cost {:exemplars-passing-at-the-ruled-signature
                (count (filter :verdict (vals ex)))
                :subtraction-stressing-exemplars stressing
                :what-is-paid :three-passing-exemplars-and-a-producer-that-does-not-exist-yet}}
     :d {:label "discharged-with-o3-reopened"
         :cost {:reopens :organise-o3-field
                :ruled-arm-that-would-reopen :over-the-nodes-organise-did-not-add
                :what-is-paid :a-ruling-of-2026-09-07-is-reopened-after-its-exemplar-was-built}}
     :e {:label "discharged-conditionally"
         :cost {:divergence-carried-open-on stressing
                :amendment-released-before-the-divergence-is-settled? true
                :what-is-paid :the-holes-lean-edit-lands-while-the-clause-it-encodes-is-unsettled}}}))

(def result
  (assoc baseline
         :check :F12-exemplar-proviso-sheet
         :arms arms
         :provenance {:futon2-head (git-out repo "rev-parse" "HEAD")
                      :holes-lean-head (git-out mathlib "log" "-1" "--format=%H" "--" "DarkTower/WarMachine/Holes.lean")
                      :input-sha256 input-digests}
         :plants plants
         :originals-byte-identical-after-plants?
         (= input-digests (into (sorted-map) (for [[k pth] paths] [k (sha pth)])))
         :verdict (and (:all-exemplars-pass? baseline)
                       (every? :flipped? (butlast plants))
                       (not (:flipped? (last plants))))))

(with-open [w (io/writer (str (fs/path run-dir "25-proviso.edn")))]
  (binding [*out* w] (pp/pprint result)))

(let [ex (:exemplars result)
      cov (:clause-coverage result)
      uncond (vec (sort (keep (fn [[c v]] (when-not (:vacuity-applies? v) c)) cov)))
      snatch (:snatch ex)]
  (spit (str (fs/path lab "C582-F12-exemplar-proviso-sheet.md"))
        (str
         "# F12 exemplar-proviso choice sheet\n\n"
         "Generated by `f12_proviso_sheet.bb` from `runs/F12-organise/25-proviso.edn`, which is\n"
         "itself derived from the three exemplar artifacts, `aif-equations.edn` and the Lean\n"
         "sources. Every number below is read from that artifact at the key path cited beside it.\n"
         "**No ruling is taken here and no arm is preferred.**\n\n"
         "## The question\n\n"
         "Does the exemplar set {ants, mining, snatch} discharge the item-4 exemplar proviso on\n"
         "the arm-6 carrier ruling, and therefore release the staged item-5 amend-to-existence\n"
         "edit at `mathlib4/DarkTower/WarMachine/Holes.lean:861`?\n"
         "[`:registry-evidence :carrier-record`, `:registry-evidence :sorry-record`]\n\n"
         "## Measured position\n\n"
         "All three exemplars pass at the ruled signature `RuledOrganiseType`\n"
         "(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:32-34`): "
         (:all-exemplars-pass? result) ". [`:all-exemplars-pass?`]\n\n"
         "**The exemplar set stresses the ruled o3 subtraction exactly once.** `addedByOrganise`\n"
         "is non-empty on " (pr-str (:subtraction-stressing-exemplars result))
         " and empty on the other two, so on ants and mining the ruled subtraction\n"
         "`nodes \\ addedByOrganise` removes nothing and the no-bootstrap reading is\n"
         "indistinguishable there from the node-set reading it was chosen over.\n"
         "[`:exemplars * :added-by-organise-empty?`, `:subtraction-stressing-exemplars`]\n\n"
         "**And that one row does not reproduce its own run.** At the ruled signature snatch's\n"
         "`organisedEdges` is `fastForward (nodes \\ addedByOrganise)`\n"
         "(`mathlib4/DarkTower/WarMachine/F12SnatchExemplar.lean:108`), which is "
         (:organised-edge-count snatch) " edge against the "
         (count (:recorded-edges snatch)) " the row records; `:reproduces-recorded-edges?` is "
         (pr-str (:reproduces-recorded-edges? snatch)) ".\n"
         "[`:exemplars :snatch :organised-edge-count`, `:exemplars :snatch :recorded-edges`,\n"
         "`:reproduction :snatch`]\n\n"
         "**Reproduction across the set** is " (pr-str (:reproduction result)) ". Ants is\n"
         "`:not-measurable` rather than true: its artifact records no edge set at all, and its\n"
         "repository relation is empty (`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:22`,\n"
         "with the vacuity proved at `mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:72-74`),\n"
         "so counting it as reproducing would count an absent record as agreement.\n"
         "[`:reproduction`, `:exemplars :ants :recorded-edges`]\n\n"
         "## What the clause table can and cannot say\n\n"
         "The dispatching packet asked for a vacuity table over all seven ruled clauses. **That\n"
         "framing does not survive the clause text.** " (pr-str uncond) " are universally\n"
         "quantified EQUATIONS with no antecedent\n"
         "(`mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean:23-33`), so nothing about them can\n"
         "be vacuous and no exemplar is a witness for them. Only `:o2` and `:o4` are implications,\n"
         "and only they carry a per-exemplar row. [`:clause-shapes`, `:clause-coverage`]\n\n"
         "- `:o2` antecedent `" (get-in cov [:o2 :antecedent]) "` — satisfied by "
         (pr-str (get-in cov [:o2 :antecedent-satisfied-by])) ", vacuous on "
         (pr-str (get-in cov [:o2 :vacuous-on])) ". [`:clause-coverage :o2`]\n"
         "- `:o4` antecedent `" (get-in cov [:o4 :antecedent]) "` — satisfied by "
         (pr-str (get-in cov [:o4 :antecedent-satisfied-by])) ", vacuous on "
         (pr-str (get-in cov [:o4 :vacuous-on])) ". [`:clause-coverage :o4`]\n\n"
         "## Arms and their measured costs\n\n"
         "- **(a) Discharged as it stands.** Three exemplars pass, so arm 6 has been exercised on\n"
         "  something real. Cost: of the three, " (get-in arms [:a :cost :exemplars-reproducing-their-recorded-edges])
         " reproduces its recorded edges, "
         (get-in arms [:a :cost :exemplars-not-reproducing]) " does not, and "
         (get-in arms [:a :cost :exemplars-where-reproduction-is-not-measurable])
         " is not measurable — and the one that\n  does not is the only one that stresses the clause the o3 ruling introduced.\n"
         "  [`:arms :a :cost`]\n"
         "- **(b) Discharged by snatch alone.** The only row where the subtraction bites is the one\n"
         "  that counts. Cost: that row keeps " (get-in arms [:b :cost :snatch-ruled-edge-count])
         " of its " (get-in arms [:b :cost :snatch-recorded-edge-count])
         " recorded edges at the ruled\n  signature. [`:arms :b :cost`]\n"
         "- **(c) Not discharged.** Require one exemplar both to stress the subtraction and to\n"
         "  reproduce its run. Cost: "
         (get-in arms [:c :cost :exemplars-passing-at-the-ruled-signature])
         " passing exemplars are set aside, and no exemplar IN THIS SET\n"
         "  does both — whether the wider corpus holds one is not measured here. Building one\n"
         "  needs a producer that populates `:added-by-organise` consistently with the\n"
         "  no-bootstrap reading. [`:arms :c :cost`]\n"
         "- **(d) Discharged, with o3 reopened.** Accept the carrier; reopen "
         (pr-str (get-in arms [:d :cost :reopens]))
         ".\n  Cost: a ruling of 2026-09-07 is reopened after its exemplar was built to it.\n"
         "  [`:arms :d :cost`]\n"
         "- **(e) Discharged conditionally.** Release the amendment and carry the divergence as a\n"
         "  separate open row. Cost: the `Holes.lean` edit lands while the clause it encodes is\n"
         "  still unsettled on " (pr-str (get-in arms [:e :cost :divergence-carried-open-on]))
         ". [`:arms :e :cost`]\n\n"
         "## What this sheet does not claim\n\n"
         "No arm is preferred and no verdict is taken on the proviso; the choice is Joe's.\n"
         "Nothing measured here bears on the other "
         (get-in result [:registry-evidence :other-organise-choice-count])
         " registered `organise-` choices, and\n"
         "`mathlib4/DarkTower/WarMachine/Holes.lean` is untouched at "
         (subs (get-in result [:provenance :holes-lean-head]) 0 10) " with the\n"
         "item-5 amendment still staged. [`:registry-evidence`, `:provenance`]\n\n"
         "## Plants\n\n"
         "Each plant names the conclusion it must move and is checked against THAT key path, not\n"
         "against whole-result inequality. " (count (butlast plants)) " plants each moved their\n"
         "named conclusion; the control moved nothing. Every input is byte-identical afterwards: "
         (:originals-byte-identical-after-plants? result) ". [`:plants`,\n"
         "`:originals-byte-identical-after-plants?`]\n")))

(when-not (:verdict result) (System/exit 1))
