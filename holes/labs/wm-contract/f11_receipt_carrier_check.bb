#!/usr/bin/env bb
;; f11_receipt_carrier_check.bb -- `:F11` slice 4.  CHECKS the premises the
;; `:find-f2-receipt-carrier` arms are priced against, in the three files that
;; hold them, none of which the arm module writes.
;;
;; WHY A CHECK AND NOT A READ.  The entry prices what it would cost to make
;; `Receipt` carry what F2 asks for.  Six things could make that pricing false
;; while `lake build` stays green, because each is a fact about a file the arm
;; module never mentions:
;;
;;   1. `Receipt` could gain or lose a field.  Every arm is stated against a
;;      carrier with exactly `citesTextOrEdges` and `scoreAlone`, neither of
;;      which is the acknowledged clause, the route or the as-of that
;;      `P-validated-R5.md:486` asks for.  If someone adds one, the do-nothing
;;      arm's cost is no longer what the entry says.
;;   2. The MECHANICAL cost of the data arm is the number of sites that build a
;;      `Receipt`, because a field without a default breaks each one.  It is
;;      recomputed here, and the count is deliberately compared against the
;;      number the entry quotes rather than merely reported: a second
;;      construction site changes the arm's price.
;;      The census must NOT count `Holes.lean:1209`, where `Receipt` is a bound
;;      TYPE PARAMETER of `Handoff` and has nothing to do with this structure --
;;      that line is the reason this check classifies every occurrence instead
;;      of grepping for the word.
;;   3-5. The RECORD could stop carrying what the data arm would transcribe.
;;      The entry prices that arm on: a warrant per receipt with a file and two
;;      clause texts; a route that is present but takes ONE value across all 96
;;      receipts, so a route field would not discriminate on this record; and no
;;      per-receipt as-of at all, only one for the fixture.  Each is read back
;;      from `futon3:checks/find-snatch.edn`.
;;   6. The Lean transcription could start carrying warrants.  The data arm
;;      costs a NEW transcription only while `FindSnatchRowLit` carries none;
;;      its field list is read back here.
;;
;; It takes NO ruling.  It does not say `Receipt` should be amended, and it says
;; nothing about F4's reading, which is a separate unregistered question.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F11R_ARM, F11R_HOLES,
;; F11R_FIXTURE, F11R_DARK, F11R_OUT.
(require '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def dark (or (System/getenv "F11R_DARK") (str home "/code/mathlib4/DarkTower")))
(def holes-path (or (System/getenv "F11R_HOLES") (str dark "/WarMachine/Holes.lean")))
(def arm-path (or (System/getenv "F11R_ARM")
                  (str dark "/WarMachine/F11ReceiptCarrier.lean")))
(def fixture-path (or (System/getenv "F11R_FIXTURE")
                      (str home "/code/futon3/checks/find-snatch.edn")))
(def out-path (or (System/getenv "F11R_OUT")
                  (str lab "/runs/F11-find/07-receipt-carrier.edn")))

(def failures (atom []))
(defn fail! [k detail] (swap! failures conj {:check k :detail detail}))
(defn lines [p] (str/split-lines (slurp p)))
(defn rel [p] (str/replace p (str home "/code/") ""))

;; ------------------------------------------------- 1. the carrier's field list
(def holes-lines (lines holes-path))

(defn block-after
  "Field lines of the `structure NAME where` block starting at the header, taken
   while lines are indented and non-blank."
  [ls header]
  (when-let [i (first (keep-indexed (fn [i l] (when (= header (str/trim l)) i)) ls))]
    {:line (inc i)
     :fields (vec (for [l (take-while #(re-find #"^\s+\S" %) (drop (inc i) ls))
                        :let [t (str/trim l)]
                        :when (str/includes? t " : ")]
                    (let [[f ty] (str/split t #" : " 2)]
                      {:field (str/trim f) :type (str/trim ty)})))}))

(def receipt-block (block-after holes-lines "structure Receipt where"))
(when-not receipt-block (fail! :receipt-structure-not-found {:file (rel holes-path)}))

(def receipt-fields (mapv :field (:fields receipt-block)))
(def expected-receipt-fields ["citesTextOrEdges" "scoreAlone"])
(when (not= receipt-fields expected-receipt-fields)
  (fail! :receipt-field-list-changed
         {:expected expected-receipt-fields :found receipt-fields
          :why "every arm of :find-f2-receipt-carrier is priced against this field list"}))

(def f2-asks ["the acknowledged tension clause" "the retrieval route" "an as-of"])
(def carrier-carries-none-of-what-f2-asks
  (every? (fn [f] (not (re-find #"(?i)clause|route|asOf|as-of" f))) receipt-fields))
(when-not carrier-carries-none-of-what-f2-asks
  (fail! :receipt-may-now-carry-what-f2-asks
         {:fields receipt-fields
          :why "the do-nothing arm is priced on the carrier holding none of P-validated-R5.md:486's three"}))

;; ------------------------------- 2. who builds a Receipt (the mechanical cost)
;; THREE WAYS TO GET THIS WRONG, each of which is in the tree right now.
;;
;;  (a) `DarkTower/APMCycleMachine.lean:87` declares a `structure Receipt` of its
;;      own, with nothing to do with find.  A census that greps the word counts
;;      its uses as costs of amending this one.  Files that declare their own
;;      are excluded by name and the exclusion is recorded.
;;  (b) `Holes.lean:1201` binds `Receipt` as a TYPE PARAMETER of `Handoff`, so
;;      `Holes.lean:1209`'s `receipt : Receipt` is a bound variable.  Occurrences
;;      inside such a block are marked :shadowed-type-parameter and cost nothing.
;;  (c) A field added without a default breaks CONSTRUCTION sites, and a
;;      structure that `extends Receipt` inherits the obligation, so its own
;;      construction sites break too.  The closure over extending structures is
;;      taken and recorded rather than assumed to be empty.
(defn lean-files [root]
  (sort (map str (filter #(and (.isFile %) (str/ends-with? (.getName %) ".lean"))
                         (file-seq (io/file root))))))

(def re-receipt #"(?<![A-Za-z])Receipt(?![A-Za-z])")

(defn own-receipt-declaring? [ls]
  (boolean (some #(re-find #"^structure Receipt where" (str/triml %)) ls)))

(def file-lines (into (sorted-map) (for [f (lean-files dark)] [f (lines f)])))

(def holes-rel (rel holes-path))
(def foreign-receipt-files
  (vec (sort (for [[f ls] file-lines
                   :when (and (own-receipt-declaring? ls) (not= (rel f) holes-rel))]
               (rel f)))))

(def in-scope-files
  (vec (sort (for [[f ls] file-lines
                   :when (and (some #(re-find re-receipt %) ls)
                              (not (and (own-receipt-declaring? ls) (not= (rel f) holes-rel))))]
               f))))

(defn shadowed-block-lines
  "Line numbers (1-based) inside a `structure X (... Receipt ... : Type*)` block,
   where `Receipt` names a bound parameter and not this structure."
  [ls]
  (loop [i 0 acc #{}]
    (if (>= i (count ls))
      acc
      (let [l (nth ls i)]
        (if (and (re-find #"^structure " (str/triml l))
                 (re-find #"\(([^)]*\bReceipt\b[^)]*): Type\*\)" l))
          (let [block (count (take-while #(re-find #"^\s+\S" %) (drop (inc i) ls)))]
            (recur (+ i 1 block) (into acc (range (+ i 2) (+ i 2 block)))))
          (recur (inc i) acc))))))

(defn classify-line [t]
  (cond
    (re-find #"^structure Receipt where" t) :declaration
    (re-find #"^(?:noncomputable )?(?:private )?structure \S+ extends Receipt\b" t) :extension
    (re-find #"^(?:noncomputable )?(?:private )?structure \S+ \([^)]*\) extends Receipt\b" t) :extension
    (re-find #":\s*Receipt\s+where\s*$" t) :construction
    (re-find #"^def Receipt\.|^theorem Receipt\.|^lemma Receipt\." t) :derived-definition
    (re-find #"Receipt\.[a-zA-Z]" t) :field-or-lemma-reference
    (re-find #"Option Receipt|:\s*Receipt\b|→ Receipt|Receipt →" t) :field-or-signature
    :else :other))

(defn doc-lines
  "1-based line numbers inside a doc comment or block comment, so a `Receipt`
   that occurs in PROSE is not reported as an occurrence in code.  Tracked
   across lines: `MemoryArmPreregistration.lean:101` and this module's own
   header both mention `Receipt` in the middle of a block a single-line test
   would miss."
  [ls]
  (loop [i 0 in-doc? false acc #{}]
    (if (>= i (count ls))
      acc
      (let [l (nth ls i)
            opens? (or (str/includes? l "/--") (str/includes? l "/-!") (str/includes? l "/-"))
            closes? (str/includes? l "-/")
            doc? (or in-doc? opens?)]
        (recur (inc i)
               (cond (and doc? (not closes?)) true closes? false :else in-doc?)
               (if doc? (conj acc (inc i)) acc))))))

(def occurrences
  (vec (sort-by (juxt :file :line)
                (for [f in-scope-files
                      :let [ls (file-lines f)
                            shadowed (shadowed-block-lines ls)
                            doc (doc-lines ls)]
                      [i l] (map-indexed vector ls)
                      :let [t (str/triml l)]
                      :when (and (re-find re-receipt l)
                                 (not (str/starts-with? t "--"))
                                 (not (doc (inc i))))]
                  {:file (rel f) :line (inc i)
                   :kind (cond (shadowed (inc i)) :shadowed-type-parameter
                               (re-find #"\bReceipt\b[^)]*: Type\*\)" l) :shadowing-header
                               :else (classify-line t))}))))

;; The closure: structures that extend `Receipt` inherit a new field's
;; construction obligation, so their construction sites are edit sites too.
(def extending-structures
  (vec (sort (for [{:keys [file line kind]} occurrences
                   :when (= :extension kind)
                   :let [l (nth (file-lines (str home "/code/" file)) (dec line))
                         m (re-find #"structure (\S+)" (str/triml l))]
                   :when m]
               (nth m 1)))))

;; A site that must be edited is one that BUILDS a receipt, and the reliable
;; marker is the field rather than the type name: `: Receipt where` finds only
;; the named-type form and misses `some { citesTextOrEdges := True, ... }`, which
;; is how the finders in the arm module build theirs.  Every construction of
;; `Receipt` or of anything extending it has to give `citesTextOrEdges` exactly
;; once, so this counts each construction once and no signature or projection.
(def edit-sites
  (vec (sort-by (juxt :file :line)
                (for [f in-scope-files
                      :let [ls (file-lines f) doc (doc-lines ls)]
                      [i l] (map-indexed vector ls)
                      :when (and (re-find #"citesTextOrEdges\s*:=" l)
                                 (not (doc (inc i)))
                                 (not (str/starts-with? (str/triml l) "--")))]
                  {:file (rel f) :line (inc i)
                   :in (if (re-find #"\bwhere\s*$" (nth ls (dec i) ""))
                         :named-type-form
                         :anonymous-constructor-form)}))))

;; The arm module is scaffolding built to PRICE this question; the cost of an
;; amendment to code that would exist either way is the count outside it.
(def arm-module-files
  #{"mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean"})
(def edit-sites-outside-the-arm-module
  (vec (remove #(arm-module-files (:file %)) edit-sites)))

;; The number the registry entry quotes as the data arm's mechanical cost.
(def expected-edit-sites 7)
(when (not= expected-edit-sites (count edit-sites))
  (fail! :edit-site-census-changed
         {:expected expected-edit-sites
          :found (count edit-sites) :sites edit-sites
          :why "the data arm's mechanical cost is one edit per site that builds a Receipt or a structure extending one"}))

;; ------------------------------------------ 3-5. what the record can supply
(def fixture (read-string (slurp fixture-path)))
(def receipts (vec (mapcat (comp vals :receipts :find)
                           (mapcat :round-results (:scenarios fixture)))))

(def receipt-key-set (vec (sort (map name (distinct (mapcat keys receipts))))))
(def warrant-key-set (vec (sort (map name (distinct (mapcat (comp keys :warrant) receipts))))))
(def route-values (into (sorted-map) (frequencies (map :route receipts))))
(def state-field-values (into (sorted-map) (frequencies (map :state-fields receipts))))
(def per-receipt-as-of (count (filter :as-of receipts)))

(when-not (= warrant-key-set ["file" "however-lines" "however-text" "if-lines" "if-text"])
  (fail! :warrant-shape-changed
         {:found warrant-key-set
          :why "the data arm is priced on transcribing exactly these"}))
(when (not= 1 (count route-values))
  (fail! :route-is-no-longer-single-valued
         {:found route-values
          :why "the entry's measurement is that a route FIELD would not discriminate on this record"}))
(when (pos? per-receipt-as-of)
  (fail! :record-now-carries-a-per-receipt-as-of
         {:count per-receipt-as-of
          :why "the entry prices the as-of as available once per fixture only"}))

;; ------------------------------- 6. the Lean transcription carries no warrant
(def row-lit (block-after holes-lines "structure FindSnatchRowLit where"))
(def row-lit-fields (mapv :field (:fields row-lit)))
(when (some #(re-find #"(?i)warrant|route|text|asOf" %) row-lit-fields)
  (fail! :transcription-now-carries-warrants
         {:fields row-lit-fields
          :why "the data arm's transcription cost is priced on this carrier holding none"}))

;; --------------------------------------------- 7. the arm module, if present
(def arm-present? (.exists (io/file arm-path)))
(def arm-lines (if arm-present? (lines arm-path) []))

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

(def arm-declarations
  (vec (sort-by :name
                (for [[i l] (map-indexed vector arm-lines)
                      :let [m (re-find #"^(?:noncomputable )?(?:private )?(theorem|lemma|def|abbrev|structure|instance|example|opaque) ([A-Za-z_][A-Za-z0-9_'.]*)" l)]
                      :when m]
                  {:kind (keyword (nth m 1)) :name (nth m 2) :line (inc i)}))))

;; Declarations the registry entry will cite by name.  EXACT names, not
;; prefixes: the sibling row found a prefix test passing on a renamed
;; declaration whose longer neighbour survived.  Filled at review, from the
;; entry's own pointers.
(def required-declarations
  (if-let [s (System/getenv "F11R_REQUIRED")]
    (vec (sort (remove str/blank? (str/split s #","))))
    ["ContentF2"
     "FindResultR"
     "ReceiptWithAssertions"
     "RelationalReceipt"
     "assertionFieldsDoNotFixAttribution"
     "findRErasuresAreEqual"
     "findRFaithfulContentF2"
     "findRMisattributingErasureConformant"
     "findRMisattributingFailsContentF2"
     "findSnatchMisattributingConformant"
     "findSnatchMisattributingWitness"
     "handCarriedReceiptHasRightClause"
     "receiptContentAttributedHoldsOfEveryFinder"]))
(def declared-names (set (map :name arm-declarations)))
(def missing (vec (sort (remove declared-names required-declarations))))
(when (seq missing)
  (fail! :required-declaration-missing {:missing missing}))

;; ------------------------------------------------------------------ the record
(def record
  (sorted-map
   :record :F11-slice-4-receipt-carrier
   :row :F11
   :basis (sorted-map :holes (rel holes-path)
                      :arm-module (rel arm-path)
                      :arm-module-present? arm-present?
                      :fixture (rel fixture-path)
                      :dark-tower-root (rel dark))
   :carrier (sorted-map
             :structure-line (:line receipt-block)
             :fields (:fields receipt-block)
             :carries-none-of-what-f2-asks? carrier-carries-none-of-what-f2-asks
             :what-f2-asks-for f2-asks)
   :mechanical-cost (sorted-map
                     :edit-sites edit-sites
                     :edit-sites-outside-the-arm-module edit-sites-outside-the-arm-module
                     :named-type-construction-sites
                     (vec (filter #(= :construction (:kind %)) occurrences))
                     :extending-structures extending-structures
                     :occurrences-by-kind (into (sorted-map)
                                                (map (fn [[k v]] [k (count v)]))
                                                (group-by :kind occurrences))
                     :shadowed-type-parameter-occurrences
                     (vec (filter #(= :shadowed-type-parameter (:kind %)) occurrences))
                     :excluded-files-declaring-their-own-receipt foreign-receipt-files
                     :files-in-scope (mapv rel in-scope-files)
                     :lean-files-scanned (count (lean-files dark)))
   :what-the-record-supplies
   (sorted-map :receipts (count receipts)
               :receipt-keys receipt-key-set
               :warrant-keys warrant-key-set
               :routes route-values
               :state-fields state-field-values
               :per-receipt-as-of per-receipt-as-of
               :fixture-as-of (:as-of fixture)
               :repository-size (count (:repository fixture)))
   :transcription (sorted-map
                   :row-literal-line (:line row-lit)
                   :fields row-lit-fields
                   :carries-a-warrant? false)
   :arm-declarations arm-declarations
   :required-declarations required-declarations
   :hygiene hygiene
   :takes-no-ruling
   "This check prices a carrier amendment; it does not choose one. It says nothing about whether Receipt should be amended, nothing about F4's reading, and nothing about what should be done with the sorry at Holes.lean:264."
   :failures (vec (sort-by :check @failures))))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint record)))

(if (seq @failures)
  (do (println "FAIL" (pr-str (mapv :check @failures))) (System/exit 1))
  (do (println "PASS f11_receipt_carrier_check:"
               "Receipt fields" (pr-str receipt-fields)
               "| edit sites" (count edit-sites)
               "of which outside the arm module" (count edit-sites-outside-the-arm-module)
               "| recorded receipts" (count receipts)
               "| routes" (pr-str (keys route-values))
               "| per-receipt as-of" per-receipt-as-of
               "| arm module" (if arm-present? "present" "ABSENT"))
      (System/exit 0)))
