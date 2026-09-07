#!/usr/bin/env bb
;; f12_discharge_check.bb -- `:F12` slice 14.  CHECKS the premises of the
;; discharge measurement against sources it does not itself write.
;;
;; WHY A CHECK AND NOT A READ.  Slice 14 measures what DISCHARGING the sorry at
;; `Holes.lean:861` costs.  Every load-bearing premise of that measurement can go
;; false while `lake build` stays green:
;;
;;   1. "The sorry is still there, at still that type."  The whole slice is about
;;      one declaration in a file it deliberately does not touch.  If another
;;      lane discharges it, retypes it, or moves it, every result in
;;      `F12DischargeArm.lean` is about a declaration that no longer exists.
;;      Read out of `Holes.lean` and compared with the `OrganiseType` abbrev the
;;      module's results are stated at.
;;   2. "The refusal says what the theorem denies."  R2 turns one sentence of the
;;      `organise` docstring -- `does not select one canonical implementation` --
;;      into a proved non-uniqueness.  R4 tests another -- the 2026-09-02 TYPE
;;      AMENDMENT's claim that the temperament is where the closure-policy datum
;;      lives.  Both sentences are read out of the file; if either is rewritten,
;;      the mapping from theorem to claim is stale.
;;   3. "The results are stated at organise's own type."  A module that declared
;;      its own signature synonym would prove the same theorems about a different
;;      interface.  The `OrganiseType` use count is recomputed and the module is
;;      scanned for a competing abbrev -- the trap slice 13 checked for at
;;      `armTwoOrganiseType`.
;;   4. "Both non-uniqueness witnesses are functions the laws ACCEPT."  A
;;      non-uniqueness between a conformant function and an arbitrary one is not
;;      the swappability clause.  The witness terms are read out of the
;;      existential proofs and each is required to appear in a conformance
;;      theorem.
;;   5. "The difference is invisible in the corpus."  That is this slice's cost
;;      statement and it is a claim about nine recorded files, not about Lean.
;;      Recomputed here: the two discharges differ only in `addedByOrganise`, and
;;      no recorded cascade carries a node organise added INTO it.  The sole
;;      producer's empty literal is read too, so the emptiness is a fact about
;;      the constructor rather than about which runs happened to be recorded --
;;      and the check FAILS if that literal ever becomes computed, because the
;;      finding would then be stale.
;;
;; WHAT IT DOES NOT DO.  It takes no ruling.  It does not say whether the sorry
;; SHOULD be discharged, amended, or left; it measures what each costs so the
;; registry entry's arms carry something a reader can check.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F12_DISCHARGE, F12_CONF,
;; F12_HOLES, F12_CHECKS, F12_CONSTRUCT, F12_OUT.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def wm (str home "/code/mathlib4/DarkTower/WarMachine"))
(def futon3 (str home "/code/futon3"))
(def arm-path (or (System/getenv "F12_DISCHARGE") (str wm "/F12DischargeArm.lean")))
(def conf-path (or (System/getenv "F12_CONF") (str wm "/F12Conformance.lean")))
(def holes-path (or (System/getenv "F12_HOLES") (str wm "/Holes.lean")))
(def checks-dir (or (System/getenv "F12_CHECKS") (str futon3 "/checks")))
(def construct-path (or (System/getenv "F12_CONSTRUCT") (str checks-dir "/construct_cascade.clj")))
(def out-path (or (System/getenv "F12_OUT")
                  (str lab "/runs/F12-organise/09-discharge-arm.edn")))

(def failures (atom []))
(defn fail! [k data] (swap! failures conj (sorted-map :check k :detail data)))

(defn die [m data]
  (binding [*out* *err*] (println "f12-discharge:" m (pr-str data)))
  (System/exit 1))

(defn- read-source [path what]
  (if (.exists (io/file path))
    (slurp path)
    (die "file not found" {:what what :path path})))

(def arm (read-source arm-path :discharge-arm))
(def conf (read-source conf-path :conformance))
(def holes (read-source holes-path :holes))
(def construct-src (read-source construct-path :construct-cascade))

(defn- squeeze [s] (str/trim (str/replace s #"\s+" " ")))
(defn- lines-of [s] (vec (str/split-lines s)))
(def holes-lines (lines-of holes))
(def arm-lines (lines-of arm))

(defn- line-of
  "1-indexed line number of the first line matching re, or nil."
  [ls re]
  (first (for [[i l] (map-indexed vector ls) :when (re-find re l)] (inc i))))

;; --------------------------------------------------------------------------
;; 1. the sorry, its line, and its type
;; --------------------------------------------------------------------------
(def organise-decl-line (line-of holes-lines #"^def organise \{Policy P : Type\*\}"))
(def organise-type
  (if-let [m (re-find #"def organise \{Policy P : Type\*\} :\s*([^\n]*?):= sorry" holes)]
    (squeeze (second m))
    (do (fail! :organise-not-declared-as-a-sorry {:path holes-path}) nil)))

(def organise-type-abbrev
  (if-let [m (re-find #"abbrev OrganiseType \(Policy P : Type\*\) :=\s*\n?\s*([^\n]+)" conf)]
    (squeeze (second m))
    (do (fail! :no-organise-type-abbrev {:path conf-path}) nil)))

(def type-matches-abbrev? (= organise-type organise-type-abbrev))
(when-not type-matches-abbrev?
  (fail! :module-signature-is-not-organises
         {:organise organise-type :abbrev organise-type-abbrev}))

;; --------------------------------------------------------------------------
;; 2. the two docstring sentences the results are stated against
;; --------------------------------------------------------------------------
(def docstring-line (line-of holes-lines #"owner: P-validated-R5 §3e organise"))

(def docstring
  (if docstring-line (nth holes-lines (dec docstring-line)) ""))

(def refusal-sentences
  (sorted-map
   :refusal-marker (str/includes? docstring "DELIBERATE IMPLEMENTATION REFUSAL")
   :hole-intentionally (str/includes? docstring "contract kind HOLE intentionally")
   :no-canonical-implementation
   (str/includes? docstring "does not select one canonical implementation")
   :temperament-is-where-the-datum-lives
   (str/includes? docstring "the temperament is where that datum lives")
   :two-policies-named
   (and (str/includes? docstring "playout_snatch.clj")
        (str/includes? docstring "wmCascadeDiffFixture"))))

(doseq [[k v] refusal-sentences]
  (when-not v (fail! :docstring-claim-missing {:claim k :line docstring-line})))

;; --------------------------------------------------------------------------
;; 3. the module's declarations, matched at a word boundary
;; --------------------------------------------------------------------------
(def decl-re
  #"(?m)^(?:local )?(?:noncomputable )?(?:private )?(?:theorem|def|abbrev|opaque|instance|lemma) (\w+)")

(def declared
  (vec (sort (map second (re-seq decl-re arm)))))

(def declaration-lines
  (into (sorted-map)
        (for [[i l] (map-indexed vector arm-lines)
              :let [m (re-find #"^(?:local )?(?:noncomputable )?(?:private )?(?:theorem|def|abbrev|opaque|instance|lemma) (\w+)" l)]
              :when m]
          [(second m) (inc i)])))

;; The prefix-match trap slice 7 found: `organiseEmpty` is a prefix of
;; `organiseEmptyNotConformantSelected`, so a substring test would report the
;; longer name as evidence for the shorter one.  Membership in the parsed set is
;; exact.
(def required
  ["organiseEmptyCascade" "organiseEmpty" "organiseTypeNonempty"
   "organiseEmptyNotConformantSelected" "organiseEmptyNotConformantNodes"
   "organiseDischargeExistsSelected" "organiseDischargeExistsNodes"
   "upClosureNodesSet" "organiseNodesUpClosure" "organiseNodesUpClosureConformantNodes"
   "organiseDischargeNotUniqueSelected" "organiseDischargeNotUniqueNodes"
   "organiseNodesUpClosureAddsZaifVertex" "organiseNodesUpClosureEdgeExists"
   "organiseSelectedOnlyNoSuchEdge" "organiseNodesUpClosureNoUnauthoredEdge"
   "nonTrivialPolicyCascade" "organiseByTemperamentSelected"
   "organiseByTemperamentSelectedConformant"
   "organiseByTemperamentSelectedReadsTemperament"
   "organiseByTemperamentNodes" "organiseByTemperamentNodesConformant"
   "organiseByTemperamentNodesReadsTemperament"
   "organiseSelectedOnlyIgnoresTemperament"
   ;; review additions, slice 14: the R2 floor, and the pair that measures the
   ;; opacity route.  The packet claimed `opaque` requires a body; it does not,
   ;; it requires inhabitance, and the two routes are separated by their axiom
   ;; footprint -- so BOTH declarations have to be here or the correction is
   ;; unrecorded.
   "organiseDischargeWitnessesAgreeOnSelected"
   "organiseTypeNonemptyInstance" "organiseOpaqueZaif" "organiseOpaqueNoBody"])

(def declared-set (set declared))
(def missing (vec (sort (remove declared-set required))))
(when (seq missing) (fail! :required-declarations-missing missing))

;; --------------------------------------------------------------------------
;; 4. the results are stated at organise's own type, not a synonym
;; --------------------------------------------------------------------------
(def organise-type-uses (count (re-seq #"OrganiseType(?![A-Za-z0-9_'])" arm)))
(def module-declares-its-own-signature?
  (boolean (re-find #"(?m)^abbrev \w+ \(Policy P : Type\*\) :=" arm)))
(when module-declares-its-own-signature?
  (fail! :module-declares-a-competing-signature-abbrev {:path arm-path}))
(when (zero? organise-type-uses)
  (fail! :module-never-uses-organises-own-type {:path arm-path}))

;; --------------------------------------------------------------------------
;; 5. the token gates, read here as well as at build time
;; --------------------------------------------------------------------------
;; Counted over CODE, not over prose.  The R5 docstring has to be able to say
;; the word `sorry` -- it is a correction about how the sorry can be removed --
;; and a raw grep would then report a module the build reports 0 sorry warnings
;; for.  Both numbers are recorded; only the code one is a failure.
(def arm-code
  (-> arm
      (str/replace #"(?s)/-[-!].*?-/" " ")
      (str/replace #"(?m)--.*$" " ")))

(defn- token-count [src t]
  (count (re-seq (re-pattern (str "(?<![A-Za-z0-9_'])" t "(?![A-Za-z0-9_'])")) src)))

(def forbidden-tokens
  (into (sorted-map)
        (for [t ["sorry" "axiom" "native_decide"]]
          [(keyword t) (sorted-map :in-code (token-count arm-code t)
                                   :anywhere (token-count arm t))])))
(doseq [[k n] forbidden-tokens]
  (when (pos? (:in-code n)) (fail! :forbidden-token-in-module {:token k :count n})))

;; The opaque arm's whole content is that `opaque` REQUIRES a body.  Read the
;; declaration back out: an `opaque` with no `:=` would refute the claim the
;; docstring makes about it.
(def opaque-decls
  (vec (for [[i l] (map-indexed vector arm-lines)
             :when (re-find #"^opaque \w+" l)]
         (sorted-map :line (inc i) :text (str/trim l)
                     :carries-a-body? (str/includes? l ":=")))))
;; The packet claimed `opaque` REQUIRES a body.  Review refuted that: a bodiless
;; `opaque` elaborates from a `Nonempty` instance, and the two routes are told
;; apart by `#print axioms` (none against `Classical.choice`).  So the module has
;; to carry BOTH, and this check fails if either disappears -- with only the
;; body-carrying one left, the refuted claim would read as true again.
(when-not (= 2 (count opaque-decls))
  (fail! :expected-two-opaque-declarations {:found (count opaque-decls)}))
(when-not (= 1 (count (filter :carries-a-body? opaque-decls)))
  (fail! :expected-exactly-one-opaque-with-a-body opaque-decls))
(when-not (= 1 (count (remove :carries-a-body? opaque-decls)))
  (fail! :expected-exactly-one-bodiless-opaque opaque-decls))
;; and the bodiless one only elaborates because an inhabitance instance is in
;; scope, so the instance is part of the measurement, not decoration
(when-not (contains? declared-set "organiseTypeNonemptyInstance")
  (fail! :bodiless-opaque-has-no-inhabitance-instance {:path arm-path}))

;; --------------------------------------------------------------------------
;; 6. both non-uniqueness witnesses are functions the laws ACCEPT
;; --------------------------------------------------------------------------
(defn- proof-body
  "Source text of one declaration, from its header line to the next top-level
   declaration or docstring."
  [name]
  (when-let [start (get declaration-lines name)]
    (let [after (drop start arm-lines)
          body (take-while #(not (re-find #"^(?:/--|noncomputable |private |theorem |def |abbrev |opaque |instance |lemma |end )" %)) after)]
      (str/join "\n" (cons (nth arm-lines (dec start)) body)))))

(def conformance-theorem-names
  ;; every declaration in either file whose statement mentions a conformance
  ;; structure, with the function names it mentions
  (let [both (str conf "\n" arm)]
    (into (sorted-map)
          (for [f ["organiseSelectedOnly" "organiseUpClosure" "organiseNodesUpClosure"
                   "organiseEmpty" "organiseByTemperamentSelected" "organiseByTemperamentNodes"]]
            [f (vec (sort (distinct
                           (for [[_ thm] (re-seq (re-pattern (str "(?m)^theorem (\\w+)[^\\n]*\\n?[^\\n]*ConformantOrganise\\w*\\s*\\n?\\s*\\(?" f "(?![A-Za-z0-9_'])")) both)]
                             thm))))]))))

(defn- witnesses-in [name]
  (let [body (or (proof-body name) "")]
    (vec (sort (distinct
                (for [f ["organiseSelectedOnly" "organiseUpClosure" "organiseNodesUpClosure"
                         "organiseEmpty"]
                      :when (re-find (re-pattern (str "(?<![A-Za-z0-9_'])" f "(?![A-Za-z0-9_'])")) body)]
                  f))))))

(def non-uniqueness
  (into (sorted-map)
        (for [t ["organiseDischargeNotUniqueSelected" "organiseDischargeNotUniqueNodes"]]
          [t (sorted-map :line (get declaration-lines t)
                         :witnesses (witnesses-in t))])))

(doseq [[t {:keys [witnesses]}] non-uniqueness]
  (when (< (count witnesses) 2)
    (fail! :non-uniqueness-names-fewer-than-two-witnesses {:theorem t :witnesses witnesses}))
  (doseq [w witnesses]
    (when (empty? (get conformance-theorem-names w))
      (fail! :non-uniqueness-witness-is-not-proved-conformant {:theorem t :witness w}))))

;; --------------------------------------------------------------------------
;; 7. the corpus: where the two discharges could be told apart, and where they
;;    could not
;; --------------------------------------------------------------------------
(defn o1-shaped? [m]
  (and (map? m) (contains? m :nodes) (contains? m :added-by-organise)))

(defn as-set [x] (cond (nil? x) #{} (set? x) x (coll? x) (set x) :else #{x}))

(defn walk-cascades [x path]
  (cond
    (map? x)
    (into (if (o1-shaped? x) [[path x]] [])
          (mapcat (fn [[k v]] (walk-cascades v (conj path k)))) (sort-by (comp str key) x))
    (sequential? x)
    (into [] (mapcat (fn [[i v]] (walk-cascades v (conj path i)))) (map-indexed vector x))
    :else []))

(def record-files
  (vec (sort (for [f (.listFiles (io/file checks-dir))
                   :let [n (.getName f)]
                   :when (re-matches #".*cascade.*\.edn" n)]
               n))))

(defn- cascade-facts [record path c]
  (let [nodes (as-set (:nodes c))
        added (as-set (:added-by-organise c))
        inside (set/intersection nodes added)]
    (sorted-map
     :record record
     :at (vec path)
     :node-count (count nodes)
     :added-count (count added)
     ;; the discriminator for THIS slice: the two conformant discharges differ
     ;; only in what organise puts INTO the cascade, so a recorded run can tell
     ;; them apart only if some node organise added is a node of the cascade
     :added-inside-nodes (count inside)
     :tells-the-two-discharges-apart? (boolean (seq inside)))))

(def corpus
  (vec (for [n record-files
             :let [doc (edn/read-string (slurp (str checks-dir "/" n)))
                   found (walk-cascades doc [])]]
         (sorted-map
          :record n
          :o1-shaped-cascades (count found)
          :cascades (vec (sort-by :at (map (fn [[path c]] (cascade-facts n path c)) found)))))))
(def all-cascades (vec (mapcat :cascades corpus)))
(def separating (vec (filter :tells-the-two-discharges-apart? all-cascades)))
(def non-empty-added (vec (filter #(pos? (:added-count %)) all-cascades)))

(def construct-lines (lines-of construct-src))
(def added-field-sites
  (vec (for [[i l] (map-indexed vector construct-lines)
             :when (str/includes? l ":added-by-organise")]
         (sorted-map :line (inc i) :text (str/trim l)))))
(def added-field-literal?
  (boolean (some #(re-find #":added-by-organise\s+#\{\}\s*$" (:text %)) added-field-sites)))
(when-not added-field-literal?
  (fail! :producer-no-longer-writes-an-empty-literal
         {:path construct-path :sites added-field-sites
          :why "the finding that no recorded run can tell the two discharges apart rests on the constructor never populating the field; if it is computed now, re-measure"}))

;; --------------------------------------------------------------------------
;; 8. verdict
;; --------------------------------------------------------------------------
(def artifact
  (sorted-map
   :check "f12_discharge_check.bb"
   :slice ":F12 slice 14"
   :what "what discharging the sorry at Holes.lean:861 costs"
   :sorry
   (sorted-map
    :declared-at (str "mathlib4/DarkTower/WarMachine/Holes.lean:" organise-decl-line)
    :docstring-at (str "mathlib4/DarkTower/WarMachine/Holes.lean:" docstring-line)
    :type organise-type
    :module-signature organise-type-abbrev
    :module-signature-is-organises? type-matches-abbrev?
    :docstring-claims refusal-sentences)
   :module
   (sorted-map
    :path "mathlib4/DarkTower/WarMachine/F12DischargeArm.lean"
    :declaration-count (count declared)
    :declarations declared
    :declaration-lines declaration-lines
    :required-present (vec (sort (filter declared-set required)))
    :required-missing missing
    :organise-type-uses organise-type-uses
    :declares-its-own-signature? module-declares-its-own-signature?
    :forbidden-tokens forbidden-tokens
    :opaque-declarations opaque-decls)
   :non-uniqueness non-uniqueness
   :conformance-theorems conformance-theorem-names
   :corpus
   (sorted-map
    :records record-files
    :o1-shaped-cascades (count all-cascades)
    :cascades-with-a-non-empty-added-set (count non-empty-added)
    :cascades-that-tell-the-two-discharges-apart (count separating)
    :where-the-added-sets-live
    (vec (sort (distinct (map :record (for [r corpus, c (:cascades r)
                                            :when (pos? (:added-count c))]
                                        (assoc c :record (:record r)))))))
    :per-record corpus
    :producer
    (sorted-map :path "futon3:checks/construct_cascade.clj"
                :added-by-organise-sites added-field-sites
                :writes-an-empty-literal? added-field-literal?))
   :failures (vec (sort-by (comp str :check) @failures))
   :verdict (if (seq @failures) :FAIL :PASS)))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint artifact)))

(println "f12-discharge:" (name (:verdict artifact))
         "-- declarations" (count declared)
         "required-missing" (count missing)
         "organise-type-uses" organise-type-uses
         "corpus o1-shaped" (count all-cascades)
         "separating" (count separating)
         "->" out-path)
(when (seq @failures)
  (doseq [f @failures] (binding [*out* *err*] (println "  FAIL" (pr-str f))))
  (System/exit 1))
