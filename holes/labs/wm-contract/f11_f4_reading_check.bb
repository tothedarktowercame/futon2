#!/usr/bin/env bb
;; f11_f4_reading_check.bb -- `:F11` slice 5.  CHECKS the premises the
;; `:find-f4-reading` arms are priced against, in the three files that hold
;; them, none of which the arm module writes.
;;
;; WHY A CHECK AND NOT A READ.  The entry prices WHICH READING F4 IS.  Six
;; things could make that pricing false while `lake build` stays green, because
;; each is a fact about a file the arm module never mentions:
;;
;;   1. Reading A could stop being the forall-over-inputs existential.  Every
;;      arm is stated against `FindFalsifiable` letting the FINDER nominate the
;;      excluded pattern, per input, after the fact.  If that definition moves,
;;      the whole ordering of the arms moves with it.
;;   2. Reading B could stop being record-grain.  `FindExcludesRecordedZeroMass`
;;      quantifies over `findSnatchRepository` ALONE, and that is exactly why
;;      C implies B and B does not imply C.  A B that quantified over
;;      repositories would be C, and one arm would vanish.
;;   3. Reading C could stop taking its designation as a PARAMETER.  What makes
;;      C the deontic reading is that `zm` is an argument, supplied from outside
;;      the finder.  Inline the recorded designation and C collapses to B.
;;   4. The recorded designation could change shape.  The entry prices B and C
;;      on a designation of exactly ONE pattern per scenario taking THREE
;;      distinct values over SIX scenarios: a scenario with two zero-mass
;;      patterns, or with none, changes what a finder must exclude and therefore
;;      what the conjunction arm costs.
;;   5. The RECORD could grow a second exclusion reason.  M3's finding is that
;;      the executable F4 leg tests precisely repository membership and omission
;;      of the single declared member -- so the record discriminates the readings
;;      only through that member.  If the check grew another reason, the record
;;      would start to speak about the readings and the entry's M3 would be
;;      stale.
;;   6. The mechanical cost could move.  The entry quotes occurrence counts for
;;      each reading, SPLIT by whether the occurrence is inside the module built
;;      to price the question -- a distinction that matters because a reading
;;      used only by its own pricing module costs nothing to change.  The counts
;;      are recomputed here and COMPARED, not merely reported.
;;
;; It takes NO ruling.  It does not say which reading F4 is; it says what each
;; costs.  It asserts that `find` at `Holes.lean:264` is still a sorry, because
;; an entry that priced a discharged declaration would be pricing nothing.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F11F4_ARM, F11F4_HOLES,
;; F11F4_CONF, F11F4_DISCHARGE, F11F4_CHECK, F11F4_DARK, F11F4_OUT.
(require '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def dark (or (System/getenv "F11F4_DARK") (str home "/code/mathlib4/DarkTower")))
(def holes-path (or (System/getenv "F11F4_HOLES") (str dark "/WarMachine/Holes.lean")))
(def conf-path (or (System/getenv "F11F4_CONF") (str dark "/WarMachine/F11Conformance.lean")))
(def discharge-path (or (System/getenv "F11F4_DISCHARGE")
                        (str dark "/WarMachine/F11DischargeArm.lean")))
(def arm-path (or (System/getenv "F11F4_ARM") (str dark "/WarMachine/F11F4Reading.lean")))
(def check-path (or (System/getenv "F11F4_CHECK")
                    (str home "/code/futon3/checks/find_snatch.clj")))
(def out-path (or (System/getenv "F11F4_OUT") (str lab "/runs/F11-find/09-f4-reading.edn")))

(defn lines [p] (if (.exists (io/file p)) (str/split-lines (slurp p)) []))
(def findings (atom []))
(defn fail! [k m] (swap! findings conj {:finding k :detail m}))

;; ---------------------------------------------------------------- declaration
(defn decl-body
  "Lines of the declaration named `nm` in `ls`, from its signature to the blank
   line that ends it.  Recognises the declaration by NAME rather than by line
   number, so a planted tree is read the same way as the committed one."
  [ls nm]
  (let [start (first (keep-indexed
                      (fn [i l] (when (re-find (re-pattern (str "^(noncomputable )?(theorem|def|abbrev|structure) "
                                                                nm "\\b")) l) i))
                      ls))]
    (when start
      (let [tail (drop (inc start) ls)
            body (take-while #(not (str/blank? %)) tail)]
        (vec (cons (nth ls start) body))))))

(defn decl-line [ls nm]
  (first (keep-indexed
          (fn [i l] (when (re-find (re-pattern (str "^(noncomputable )?(theorem|def|abbrev|structure) "
                                                    nm "\\b")) l) (inc i)))
          ls)))

;; ------------------------------------------------------------------ 1 A shape
(def conf-lines (lines conf-path))
(def reading-a (decl-body conf-lines "FindFalsifiable"))
(when-not reading-a (fail! :reading-a-declaration-missing {:file conf-path}))
(def reading-a-text (str/join " " reading-a))
(def a-quantifies-over-inputs?
  (boolean (and reading-a (re-find #"∀ t repo" reading-a-text)
                (re-find #"∃ p ∈ repo\.patterns" reading-a-text)
                (re-find #"∉ \(f t repo\)\.selected" reading-a-text))))
(when-not a-quantifies-over-inputs?
  (fail! :reading-a-is-no-longer-the-forall-over-inputs-existential
         {:file conf-path :line (decl-line conf-lines "FindFalsifiable") :text reading-a-text}))

;; ------------------------------------------------------------------ 2 B shape
(def discharge-lines (lines discharge-path))
(def reading-b (decl-body discharge-lines "FindExcludesRecordedZeroMass"))
(when-not reading-b (fail! :reading-b-declaration-missing {:file discharge-path}))
(def reading-b-text (str/join " " reading-b))
(def b-is-record-grain?
  (boolean (and reading-b
                (re-find #"findSnatchZeroMass t\.context" reading-b-text)
                (re-find #"findSnatchRepository" reading-b-text)
                (not (re-find #"∀ t repo" reading-b-text)))))
(when-not b-is-record-grain?
  (fail! :reading-b-is-no-longer-record-grain
         {:file discharge-path :line (decl-line discharge-lines "FindExcludesRecordedZeroMass")
          :text reading-b-text}))

;; ------------------------------------------------------------------ 3 C shape
(def arm-lines (lines arm-path))
(def reading-c (decl-body arm-lines "FindRespectsZeroMass"))
(when-not reading-c (fail! :reading-c-declaration-missing {:file arm-path}))
(def reading-c-text (str/join " " reading-c))
(def c-takes-its-designation-as-a-parameter?
  (boolean (and reading-c
                (re-find #"\(zm : State → Set P\)" reading-c-text)
                (re-find #"∀ t repo" reading-c-text)
                (re-find #"zm t\.context" reading-c-text))))
(when-not c-takes-its-designation-as-a-parameter?
  (fail! :reading-c-no-longer-takes-an-external-designation
         {:file arm-path :line (decl-line arm-lines "FindRespectsZeroMass") :text reading-c-text}))

;; ---------------------------------------------------------- 4 the designation
(def holes-lines (lines holes-path))
(def zm-decl (decl-body holes-lines "findSnatchZeroMass"))
(when-not zm-decl (fail! :zero-mass-designation-missing {:file holes-path}))
(def zm-arms
  (vec (sort-by :scenario (keep (fn [l]
                     (when-let [[_ sc pats] (re-find #"^\s*\|\s*\.(\w+)\s*=>\s*\[([^\]]*)\]" l)]
                       {:scenario sc
                        :patterns (vec (sort (map #(str/replace (str/trim %) #"^\." "")
                                                  (str/split pats #","))))}))
                                (or zm-decl [])))))
(def zm-distinct-values (vec (sort (distinct (mapcat :patterns zm-arms)))))
(def zm-one-per-scenario? (every? #(= 1 (count (:patterns %))) zm-arms))
(when-not (= 6 (count zm-arms))
  (fail! :zero-mass-designation-scenario-count-changed
         {:expected 6 :got (count zm-arms) :file holes-path}))
(when-not zm-one-per-scenario?
  (fail! :zero-mass-designation-no-longer-one-pattern-per-scenario
         {:arms zm-arms :file holes-path}))
(when-not (= 3 (count zm-distinct-values))
  (fail! :zero-mass-designation-distinct-value-count-changed
         {:expected 3 :got (count zm-distinct-values) :values zm-distinct-values}))

;; ------------------------------------------------- 5 the record's F4 leg only
(def check-lines (lines check-path))
(defn line-hits [ls re] (vec (sort (keep-indexed (fn [i l] (when (re-find re l) (inc i))) ls))))
(def f4-throws (line-hits check-lines #"F4 violated|F4 recorded omitted member failed"))
(def f4-membership (line-hits check-lines #"contains\? \(set \(:repository result\)\) zero-mass"))
(def f4-omission (line-hits check-lines #"not \(contains\? \(set selected-union\) zero-mass\)"))
(def zero-mass-table (line-hits check-lines #"^\(def zero-mass-patterns"))
(when (empty? zero-mass-table)
  (fail! :record-zero-mass-table-missing {:file check-path}))
(when-not (= 1 (count f4-membership))
  (fail! :record-f4-membership-test-changed {:hits f4-membership :file check-path}))
(when-not (= 1 (count f4-omission))
  (fail! :record-f4-omission-test-changed {:hits f4-omission :file check-path}))
;; A SECOND exclusion reason would show up as an F4 throw that mentions neither
;; the declared member nor the two tests above.  M3 reports "not found"; this
;; asserts the two known throws are still the only two.
(when-not (= 2 (count f4-throws))
  (fail! :record-f4-leg-grew-a-second-exclusion-reason
         {:expected 2 :got (count f4-throws) :hits f4-throws :file check-path}))

;; ---------------------------------------------------------- 6 mechanical cost
(def lean-files
  (vec (sort (map #(.getPath %)
                  (filter #(str/ends-with? (.getName %) ".lean")
                          (file-seq (io/file dark)))))))
(defn occurrences [token]
  (vec (sort-by (juxt :file :line)
                (mapcat (fn [f]
                          (keep-indexed (fn [i l] (when (str/includes? l token)
                                                    {:file (str/replace f (str home "/code/") "")
                                                     :line (inc i)}))
                                        (lines f)))
                        lean-files))))
(defn split-count [token]
  (let [occ (occurrences token)
        pricing? #(str/includes? (:file %) "F11F4Reading.lean")]
    {:total (count occ)
     :in-the-pricing-module (count (filter pricing? occ))
     :outside-the-pricing-module (count (remove pricing? occ))}))
(def cost {:reading-a (split-count "FindFalsifiable")
           :reading-b (split-count "FindExcludesRecordedZeroMass")
           :reading-c (split-count "FindRespectsZeroMass")})
;; The numbers the entry quotes.  A change here changes an arm's price.
(def quoted {:reading-a {:total 21 :in-the-pricing-module 11 :outside-the-pricing-module 10}
             :reading-b {:total 18 :in-the-pricing-module 10 :outside-the-pricing-module 8}
             :reading-c {:total 10 :in-the-pricing-module 10 :outside-the-pricing-module 0}})
(doseq [k [:reading-a :reading-b :reading-c]]
  (when-not (= (get cost k) (get quoted k))
    (fail! :mechanical-cost-changed {:reading k :quoted (get quoted k) :recomputed (get cost k)})))

;; ------------------------------------- neither reading is a conjunct of F1-F3
(def conformant (decl-body conf-lines "ConformantFind"))
(def conformant-text (str/join " " (or conformant [])))
(when (or (str/includes? conformant-text "FindFalsifiable")
          (str/includes? conformant-text "FindExcludesRecordedZeroMass")
          (str/includes? conformant-text "FindRespectsZeroMass"))
  (fail! :a-reading-of-f4-became-a-conjunct-of-conformant-find
         {:file conf-path :line (decl-line conf-lines "ConformantFind")}))

;; ------------------------------------------------------- the sorry is still a sorry
(def find-line (decl-line holes-lines "find"))
(def find-text (when find-line (nth holes-lines (dec find-line))))
(when-not (and find-text (str/includes? find-text "sorry"))
  (fail! :find-not-declared-as-a-sorry {:file holes-path :line find-line :text find-text}))

;; ------------------------------------------------------------------- artifact
(def verdict (if (empty? @findings) :pass :fail))
(def artifact
  (sorted-map
   :record :F11-slice-5-f4-reading
   :row :F11
   :slice 5
   :verdict verdict
   :findings (vec (sort-by :finding @findings))
   :reading-a {:declaration "FindFalsifiable"
               :file "mathlib4/DarkTower/WarMachine/F11Conformance.lean"
               :line (decl-line conf-lines "FindFalsifiable")
               :forall-over-inputs-existential a-quantifies-over-inputs?}
   :reading-b {:declaration "FindExcludesRecordedZeroMass"
               :file "mathlib4/DarkTower/WarMachine/F11DischargeArm.lean"
               :line (decl-line discharge-lines "FindExcludesRecordedZeroMass")
               :record-grain b-is-record-grain?}
   :reading-c {:declaration "FindRespectsZeroMass"
               :file "mathlib4/DarkTower/WarMachine/F11F4Reading.lean"
               :line (decl-line arm-lines "FindRespectsZeroMass")
               :designation-is-a-parameter c-takes-its-designation-as-a-parameter?}
   :recorded-designation {:file "mathlib4/DarkTower/WarMachine/Holes.lean"
                          :line (decl-line holes-lines "findSnatchZeroMass")
                          :scenarios (count zm-arms)
                          :one-pattern-per-scenario zm-one-per-scenario?
                          :distinct-values zm-distinct-values
                          :arms zm-arms}
   :record-f4-leg {:file "futon3/checks/find_snatch.clj"
                   :zero-mass-table-line (first zero-mass-table)
                   :membership-test-line (first f4-membership)
                   :omission-test-line (first f4-omission)
                   :throw-lines f4-throws
                   :second-exclusion-reason :not-found}
   :mechanical-cost cost
   :neither-reading-is-a-conjunct-of-conformant-find true
   :find-still-a-sorry {:file "mathlib4/DarkTower/WarMachine/Holes.lean" :line find-line}))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint artifact)))
(println (format "f11_f4_reading_check: %s -- %d findings; artifact %s"
                 (name verdict) (count @findings) out-path))
(doseq [f (:findings artifact)] (println "  " (name (:finding f)) (pr-str (:detail f))))
(when (= :fail verdict) (System/exit 1))
