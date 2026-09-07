#!/usr/bin/env bb
;; f11_non_self_certifying_check.bb -- `:F11` slice 7.  CHECKS the premises the
;; F3 measurement rests on, in the files that hold them, none of which the
;; measuring module writes.
;;
;; WHY A CHECK AND NOT A READ.  The slice measures what today's F3 CANNOT state.
;; Seven things could make that false while `lake build` stays green, because
;; each is a fact about a file the module never mentions:
;;
;;   1. `Receipt` (`Holes.lean`) could grow a citation field.  The whole finding
;;      is that F3's WHICH has nowhere to live; a `citedText` field on `Receipt`
;;      would make `findCErasuresAreEqual` describe a carrier that no longer
;;      exists.
;;   2. `Receipt.nonSelfCertifying` could stop being the conjunction of the two
;;      propositions.  M1's self-assertion result is exactly that both are the
;;      finder's to fill.
;;   3. `FindSnatchRowLit` could grow a warrant column.  The REVIEW FINDING is
;;      that the record's F3 data is not in Lean; a warrant column would make the
;;      34 rounds able to decide the two readings after all.
;;   4. The two Clojure F3 predicates could converge.  M3's non-equivalence is a
;;      fact about `find_snatch.clj` having TWO conjuncts where
;;      `find_organise.clj` has FOUR.
;;   5. The transcriber could switch which predicate it transcribes.  The Lean
;;      column carries the CHECK's reading, not `find_organise`'s.
;;   6. The fixture could change.  Every claim about the 96 receipts is a claim
;;      about one pinned file, recomputed here rather than quoted.
;;   7. `RelationalReceipt` could grow a citation field, which would make slice
;;      4's finders the right witnesses for F3 and this slice's carrier redundant.
;;
;; It takes NO ruling and registers no choice.  It asserts that `find` at
;; `Holes.lean:264` is still a sorry.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F11NSC_HOLES, F11NSC_MODULE,
;; F11NSC_RECEIPT, F11NSC_SNATCH, F11NSC_ORGANISE, F11NSC_TRANSCRIBE,
;; F11NSC_FIXTURE.
(require '[clojure.edn :as edn]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def dark (str home "/code/mathlib4/DarkTower"))
(def holes-path (or (System/getenv "F11NSC_HOLES") (str dark "/WarMachine/Holes.lean")))
(def module-path (or (System/getenv "F11NSC_MODULE")
                     (str dark "/WarMachine/F11NonSelfCertifying.lean")))
(def receipt-path (or (System/getenv "F11NSC_RECEIPT")
                      (str dark "/WarMachine/F11ReceiptCarrier.lean")))
(def snatch-path (or (System/getenv "F11NSC_SNATCH")
                     (str home "/code/futon3/checks/find_snatch.clj")))
(def organise-path (or (System/getenv "F11NSC_ORGANISE")
                       (str home "/code/futon3/checks/find_organise.clj")))
(def transcribe-path (or (System/getenv "F11NSC_TRANSCRIBE")
                         (str home "/code/futon2/holes/labs/wm-contract/u46_find_transcribe.bb")))
(def fixture-path (or (System/getenv "F11NSC_FIXTURE")
                      (str home "/code/futon3/checks/find-snatch.edn")))

(def pinned-fixture-sha256
  "839897ef8fe44952403700bd237389449ae4735d3da7df8239b1b94dc7ef4dfa")

(defn slurp-lines [path] (str/split-lines (slurp path)))

(defn sha256 [path]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (format "%064x" (java.math.BigInteger. 1 (.digest d (java.nio.file.Files/readAllBytes
                                                         (.toPath (java.io.File. path))))))))

(defn block
  "The lines of the declaration named, from its header to the first blank line."
  [lines decl]
  (let [start (first (keep-indexed
                      (fn [i l] (when (re-find (re-pattern (str "^(noncomputable )?(theorem|def|structure|abbrev) "
                                                               (java.util.regex.Pattern/quote decl) "\\b"))
                                               l)
                                  i))
                      lines))]
    (when start
      (->> (drop start lines) (take-while (complement str/blank?)) vec))))

(defn clj-block
  "The lines of a Clojure defn, from its header to the first blank line."
  [lines name]
  (let [start (first (keep-indexed
                      (fn [i l] (when (re-find (re-pattern (str "^\\(defn-? "
                                                               (java.util.regex.Pattern/quote name) "(?=[ \\[]|$)"))
                                               l)
                                  i))
                      lines))]
    (when start
      (->> (drop start lines) (take-while (complement str/blank?)) vec))))

(defn finding [k v] [k v])

;; ---------------------------------------------------------------- checks ----

(defn check-receipt-carrier []
  (let [lines (slurp-lines holes-path)
        b (block lines "Receipt")
        fields (->> b rest (map str/trim) (filter #(re-find #"^\w+ :" %)) sort vec)
        nsc (block lines "Receipt.nonSelfCertifying")]
    (cond-> []
      (not= fields ["citesTextOrEdges : Prop" "scoreAlone : Prop"])
      (conj (finding :receipt-fields-moved fields))

      (not (some #(str/includes? % "receipt.citesTextOrEdges ∧ ¬ receipt.scoreAlone") nsc))
      (conj (finding :non-self-certifying-body-moved (vec nsc))))))

(defn check-row-literal-carries-no-warrant []
  (let [b (block (slurp-lines holes-path) "FindSnatchRowLit")
        fields (->> b rest (map str/trim) (filter #(re-find #"^\w+ :" %))
                    (map #(first (str/split % #" "))) sort vec)]
    (cond-> []
      (not= fields ["absence" "nonSelfCertifying" "receipted" "round" "scenario" "selected"])
      (conj (finding :row-literal-fields-moved fields))

      (some #{"warrant" "route" "asOf"} fields)
      (conj (finding :row-literal-gained-a-warrant fields)))))

(defn check-relational-receipt-has-no-citation []
  (let [b (block (slurp-lines receipt-path) "RelationalReceipt")
        fields (->> b rest (map str/trim) (filter #(re-find #"^\w+ :" %))
                    (map #(first (str/split % #" "))) sort vec)]
    (cond-> []
      (not= fields ["acknowledgedClause" "asOf" "retrievalRoute"])
      (conj (finding :relational-receipt-fields-moved fields))

      (some #{"citedText" "warrant" "citation"} fields)
      (conj (finding :relational-receipt-gained-a-citation fields)))))

(defn conjuncts
  "The conjuncts of a Clojure `and` form, given the line its head is on."
  [lines head-re]
  (let [start (first (keep-indexed (fn [i l] (when (re-find head-re l) i)) lines))]
    (when start
      (->> (drop start lines)
           (take-while #(not (re-find #"^\s*$" %)))
           (mapcat #(re-seq #"\(?(?:not=|string\?|vector\?)[^)]*\)" %))
           (map str/trim) vec))))

(defn check-two-versus-four []
  (let [snatch (conjuncts (slurp-lines snatch-path) #"F3 receipt is self-certifying")
        snatch-block (let [ls (slurp-lines snatch-path)
                           i (first (keep-indexed (fn [i l] (when (str/includes? l "(not= :score-alone (:route receipt))") i)) ls))]
                       (when i (vec (take 4 (drop i ls)))))
        organise (clj-block (slurp-lines organise-path) "receipt-cites-text?")
        n-snatch (count (filter #(re-find #"not=|string\?|vector\?" %) (or snatch-block [])))
        n-organise (count (filter #(re-find #"not=|string\?|vector\?" %) (or organise [])))]
    (cond-> []
      (nil? snatch-block) (conj (finding :snatch-f3-leg-not-found snatch))
      (nil? organise) (conj (finding :organise-predicate-not-found nil))
      (and snatch-block (not= 2 n-snatch))
      (conj (finding :snatch-f3-conjunct-count {:expected 2 :got n-snatch :lines snatch-block}))
      (and organise (not= 4 n-organise))
      (conj (finding :organise-f3-conjunct-count {:expected 4 :got n-organise :lines (vec organise)})))))

(defn check-transcriber-reads-the-check []
  (let [ls (slurp-lines transcribe-path)
        b (clj-block ls "non-self-certifying")
        txt (str/join "\n" (or b []))]
    (cond-> []
      (nil? b) (conj (finding :transcriber-predicate-not-found nil))
      (and b (not (str/includes? txt "(not= :score-alone (:route receipt))")))
      (conj (finding :transcriber-dropped-the-route-conjunct txt))
      (and b (not (str/includes? txt "(string? (get-in receipt [:warrant :file]))")))
      (conj (finding :transcriber-dropped-the-file-conjunct txt))
      (and b (str/includes? txt ":if-lines"))
      (conj (finding :transcriber-switched-to-the-organise-reading txt)))))

(defn check-fixture []
  (let [sha (sha256 fixture-path)
        fx (edn/read-string (slurp fixture-path))
        receipts (for [s (:scenarios fx)
                       r (:round-results s)
                       [id receipt] (get-in r [:find :receipts])]
                   [id receipt])
        n (count receipts)
        routes (frequencies (map (comp :route second) receipts))
        has (fn [k] (count (filter #(contains? (:warrant (second %)) k) receipts)))
        own-file (count (filter (fn [[id receipt]]
                                  (= (get-in receipt [:warrant :file])
                                     (str "library/snatch/" (name id) ".flexiarg")))
                                receipts))]
    (cond-> []
      (not= sha pinned-fixture-sha256) (conj (finding :fixture-sha-moved {:got sha}))
      (not= n 96) (conj (finding :receipt-count-moved n))
      (not= routes {:structured-antecedent 96}) (conj (finding :routes-moved routes))
      (not= [96 96 96 96 96] [(has :file) (has :if-lines) (has :if-text)
                              (has :however-lines) (has :however-text)])
      (conj (finding :warrant-fields-moved
                     {:file (has :file) :if-lines (has :if-lines) :if-text (has :if-text)
                      :however-lines (has :however-lines) :however-text (has :however-text)}))
      (not= own-file n)
      (conj (finding :warrant-not-the-patterns-own-file {:own own-file :of n})))))

(defn check-sorry-still-there []
  ;; Located by DECLARATION, not by line 264: an inserted line above it would
  ;; otherwise read as a discharged sorry (the shift C559 recorded of the
  ;; fixture receipts).
  (let [ls (slurp-lines holes-path)
        hit (first (filter #(re-find #"^def find \{State P : Type\*\}" %) ls))]
    (cond-> []
      (nil? hit) (conj (finding :find-declaration-not-found nil))
      (and hit (not (str/includes? hit ":= sorry")))
      (conj (finding :find-no-longer-a-sorry hit)))))

(defn check-module-states-what-it-claims []
  (let [ls (slurp-lines module-path)
        need ["findCErasuresAreEqual" "CitesTheSelectedPattern" "findCMiscitingFailsCitation"
              "findAssertingF3_conformant" "f3Two_not_equivalent_f3Four"
              "recordedRounds_all_three_columns_equal"]
        missing (vec (sort (remove (fn [d] (some #(re-find (re-pattern (str "^(theorem|def) "
                                                                           (java.util.regex.Pattern/quote d) "\\b")) %) ls))
                                   need)))]
    (cond-> []
      (seq missing) (conj (finding :module-declaration-missing missing)))))

;; ----------------------------------------------------------------- main -----

(def checks
  [[:receipt-carrier-is-two-propositions check-receipt-carrier]
   [:row-literal-carries-no-warrant check-row-literal-carries-no-warrant]
   [:relational-receipt-has-no-citation check-relational-receipt-has-no-citation]
   [:two-conjuncts-versus-four check-two-versus-four]
   [:transcriber-reads-the-checks-predicate check-transcriber-reads-the-check]
   [:fixture-facts check-fixture]
   [:find-is-still-a-sorry check-sorry-still-there]
   [:module-states-what-it-claims check-module-states-what-it-claims]])

(let [results (into (sorted-map)
                    (for [[k f] checks]
                      [k (vec (sort-by str (f)))]))
      findings (vec (sort-by str (mapcat val results)))]
  (pprint/pprint
   (sorted-map
    :check :f11-non-self-certifying
    :slice 7
    :findings findings
    :finding-count (count findings)
    :per-check (into (sorted-map) (for [[k v] results] [k (count v)]))
    :verdict (if (empty? findings) :pass :fail)))
  (System/exit (if (empty? findings) 0 1)))
