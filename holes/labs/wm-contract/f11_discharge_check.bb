#!/usr/bin/env bb
;; f11_discharge_check.bb -- `:F11` slice 3.  CHECKS the premises of the find
;; discharge arm against sources the arm module does not itself write.
;;
;; WHY A CHECK AND NOT A READ.  Every arm of `:find-sorry` is a claim about what
;; the sorry at `Holes.lean:264` COSTS, and five of the things that could make
;; those claims false leave `lake build` perfectly green, because each is a fact
;; about a file the arm module never mentions:
;;
;;   1. The sorry could be gone.  Every arm is priced against a declaration that
;;      is still a refusal; if someone discharges or amends it, the entry is
;;      stale and the checker must say so, not the reader.  Checked by reading
;;      the `def find ... := sorry` line out of `Holes.lean`.
;;   2. The INERTNESS claim -- that the `:leave-it-refused` arm is cheap because
;;      no proof obligation depends on `find` -- is a census over all of
;;      `DarkTower`, recomputed here rather than copied from the note.  A single
;;      new term reference anywhere would make that arm's cost wrong.
;;   3. The hole registry still has to carry `find` as a refusal.  The
;;      `:amend-to-an-existence-statement` arm's cost is partly that the registry
;;      literal would have to be restated; if the literal moved or changed, that
;;      cost is different.  Checked by reading `mkRefused "find"` out of the file.
;;   4. The OPACITY measurement is the difference between two `#print axioms`
;;      footprints, and it only means anything while the file holds exactly two
;;      `opaque` declarations, exactly one of them bodiless.  A build is happy
;;      with one, or three.  Checked by parsing the arm module's `opaque` lines.
;;   5. The arm module could lose a declaration and still elaborate.  Every
;;      declaration the registry entry cites by name is required to be present,
;;      by EXACT name rather than by prefix -- the sibling row found that a
;;      prefix test passes when `organiseEmpty` is renamed but
;;      `organiseEmptyNotConformantSelected` remains.
;;
;; It takes NO ruling -- not on which reading of F4 is the right one, not on
;; F2's receipt carrier, and not on what should be done with the sorry.  It
;; reports what each arm is priced against and fails when that changes.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F11D_ARM, F11D_HOLES,
;; F11D_DARK (the inertness-census root), F11D_OUT.
(require '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def dark (or (System/getenv "F11D_DARK") (str home "/code/mathlib4/DarkTower")))
(def arm-path (or (System/getenv "F11D_ARM")
                  (str dark "/WarMachine/F11DischargeArm.lean")))
(def holes-path (or (System/getenv "F11D_HOLES")
                    (str dark "/WarMachine/Holes.lean")))
(def out-path (or (System/getenv "F11D_OUT")
                  (str lab "/runs/F11-find/05-discharge-arm.edn")))

(def failures (atom []))
(defn fail! [k detail] (swap! failures conj {:check k :detail detail}))

(defn lines [p] (str/split-lines (slurp p)))

;; ---------------------------------------------------------------- 1. the sorry
(def holes-lines (lines holes-path))

(defn line-of [ls pred]
  (first (keep-indexed (fn [i l] (when (pred l) (inc i))) ls)))

(def find-sorry-line
  (line-of holes-lines #(re-find #"^def find \{State P : Type\*\}.*:= sorry$" %)))

(when-not find-sorry-line
  (fail! :find-not-declared-as-a-sorry
         "no `def find {State P : Type*} ... := sorry` line in Holes.lean"))

;; ------------------------------------------------------- 2. the inertness census
;; Every `find` token in DarkTower that is not part of a longer identifier and
;; not qualified by a dot.  Each occurrence is classified as CODE or PROSE: a
;; line inside a `/-- ... -/` or `/-! ... -/` block, or a `--` comment, is prose.
(defn lean-files [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".lean"))
       (map #(.getPath ^java.io.File %))
       sort))

(def find-token #"(?<![A-Za-z0-9_.])find(?![A-Za-z0-9_])")

(defn classify-file [path]
  (loop [ls (lines path) n 1 in-doc? false acc []]
    (if-not (seq ls)
      acc
      (let [l (first ls)
            opens? (or (str/includes? l "/--") (str/includes? l "/-!") (str/includes? l "/-"))
            closes? (str/includes? l "-/")
            doc-line? (or in-doc? opens?)
            comment? (str/starts-with? (str/triml l) "--")
            hits (count (re-seq find-token l))
            acc' (if (pos? hits)
                   (conj acc {:file (str/replace path (str home "/code/") "")
                              :line n
                              :kind (cond (or doc-line? comment?) :prose
                                          (str/includes? l "mkRefused \"find\"") :registry-literal
                                          :else :code)
                              :text (str/trim (subs l 0 (min 96 (count l))))})
                   acc)
            in-doc'' (cond (and doc-line? (not closes?)) true
                           closes? false
                           :else in-doc?)]
        (recur (rest ls) (inc n) in-doc'' acc')))))

(def occurrences (vec (sort-by (juxt :file :line) (mapcat classify-file (lean-files dark)))))
(def code-refs (vec (filter #(= :code (:kind %)) occurrences)))
;; The declaration itself is the one code occurrence that is allowed.
;; The declaration's own file is whatever `holes-path` is, relabelled the same
;; way the census relabels every path.  Hard-coding the committed path here made
;; the census count the declaration itself as a term reference whenever the
;; checker was pointed at a planted tree -- which is every negative control, so
;; three of the five plants reported :find-is-no-longer-inert as well as the
;; thing they actually planted (repaired in review, 2026-09-07).
(def holes-rel (str/replace holes-path (str home "/code/") ""))
(def declaration-ref
  (filter #(and (= holes-rel (:file %))
                (= find-sorry-line (:line %))) code-refs))
(def term-refs (vec (remove (set declaration-ref) code-refs)))

(when (seq term-refs)
  (fail! :find-is-no-longer-inert
         {:term-references term-refs
          :why "the :leave-it-refused arm is priced on there being none"}))

;; ------------------------------------------------------ 3. the registry literal
(def registry-line (line-of holes-lines #(str/includes? % "mkRefused \"find\"")))
(when-not registry-line
  (fail! :hole-registry-no-longer-carries-find
         "no `mkRefused \"find\"` literal in Holes.lean"))

;; ------------------------------------------------------------- 4. the two opaques
(def arm-lines (lines arm-path))
(def opaque-lines
  (vec (sort-by :line
        (keep-indexed
         (fn [i l]
           (when (str/starts-with? l "opaque ")
             {:line (inc i)
              :declaration (second (str/split l #"\s+"))
              :bodiless? (not (str/includes? l ":="))}))
         arm-lines))))

(when-not (= 2 (count opaque-lines))
  (fail! :expected-two-opaque-declarations {:found opaque-lines}))
(when-not (= 1 (count (filter :bodiless? opaque-lines)))
  (fail! :expected-exactly-one-bodiless-opaque {:found opaque-lines}))

;; --------------------------------------------------- 5. the cited declarations
(def arm-declarations
  (into (sorted-map)
        (keep-indexed
         (fn [i l]
           (when-let [m (re-find #"^(?:theorem|def|opaque|local instance) ([A-Za-z][A-Za-z0-9']*)" l)]
             [(second m) (inc i)]))
         arm-lines)))

;; EXACT names, not prefixes.
(def required
  ["FindExcludesRecordedZeroMass"
   "findAllBut" "findAllButConformant" "findAllButFailsReadingB"
   "findAllButFalsifiable" "findAllButPairDisagreesExactlyOnTheTwoZeroMassMembers"
   "findDischargeExists" "findDischargeExistsReadingA" "findDischargeExistsReadingB"
   "findDischargeNotUniqueReadingA" "findDischargeNotUniqueReadingB"
   "findOpaqueNoBody" "findOpaqueRefusing"
   "findReadingAWitnessesAgreeOnFloor" "findReadingBWitnessesAgreeOnFloor"
   "findRefusingExcludesRecordedZeroMass" "findRefusingMeetsConformanceAndReadingA"
   "findSilent" "findSilentNotConformant" "findSnatchRepositoryNonempty"
   "findTypeNonempty" "findTypeNonemptyInstance"])

(def missing (vec (sort (remove (set (keys arm-declarations)) required))))
(when (seq missing) (fail! :required-declarations-missing missing))

;; -------------------------------------------------------- 6. hygiene in the arm
(defn code-token-count [ls re]
  (loop [ls ls in-doc? false n 0]
    (if-not (seq ls)
      n
      (let [l (first ls)
            opens? (or (str/includes? l "/--") (str/includes? l "/-!") (str/includes? l "/-"))
            closes? (str/includes? l "-/")
            doc-line? (or in-doc? opens?)
            comment? (str/starts-with? (str/triml l) "--")
            n' (if (or doc-line? comment?) n (+ n (count (re-seq re l))))]
        (recur (rest ls) (cond (and doc-line? (not closes?)) true closes? false :else in-doc?) n')))))

(def hygiene
  (into (sorted-map)
        (for [[k re] {:sorry #"(?<![A-Za-z0-9_])sorry(?![A-Za-z0-9_])"
                      :axiom #"(?<![A-Za-z0-9_])axiom(?![A-Za-z0-9_])"
                      :native-decide #"native_decide"}]
          [k (code-token-count arm-lines re)])))
(doseq [[k n] hygiene]
  (when (pos? n) (fail! :arm-module-uses-forbidden-token {:token k :code-occurrences n})))

;; ------------------------------------------------------------------ the record
(def record
  (sorted-map
   :record :F11-slice-3-discharge-arm
   :row :F11
   :basis (sorted-map
           :arm-module (str/replace arm-path (str home "/code/") "")
           :holes (str/replace holes-path (str home "/code/") "")
           :dark-tower-root (str/replace dark (str home "/code/") ""))
   :the-sorry (sorted-map
               :still-a-sorry? (some? find-sorry-line)
               :line find-sorry-line)
   :inertness (sorted-map
               :term-references-to-find (count term-refs)
               :lean-files-scanned (count (lean-files dark))
               :occurrences-by-kind (into (sorted-map)
                                          (map (fn [[k v]] [k (count v)]))
                                          (group-by :kind occurrences))
               :registry-literal-line registry-line
               :prose-occurrences (vec (map #(select-keys % [:file :line])
                                            (filter #(= :prose (:kind %)) occurrences))))
   :opacity (sorted-map :declarations opaque-lines)
   :arm-declarations arm-declarations
   :required-declarations-present (empty? missing)
   :hygiene hygiene
   :takes-no-ruling
   "This check prices arms; it does not choose one. It says nothing about which reading of F4 is correct, about F2's receipt carrier, or about what should be done with the sorry at Holes.lean:264."
   :failures (vec (sort-by :check @failures))))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint record)))

(if (seq @failures)
  (do (println "FAIL" (pr-str (mapv :check @failures))) (System/exit 1))
  (do (println "PASS f11_discharge_check:"
               "sorry at Holes.lean:" find-sorry-line
               "| term refs to find:" (count term-refs)
               "| opaque:" (count opaque-lines) "of which bodiless:"
               (count (filter :bodiless? opaque-lines))
               "| declarations:" (count arm-declarations))
      (System/exit 0)))
