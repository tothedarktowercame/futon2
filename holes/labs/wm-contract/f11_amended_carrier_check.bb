#!/usr/bin/env bb
;; f11_amended_carrier_check.bb -- `:F11` slice 6.  CHECKS the premises the
;; JOINT `:amend-the-carrier` arm is priced against, in the files that hold
;; them, none of which the arm module writes.
;;
;; WHY A CHECK AND NOT A READ.  The arm prices an amendment that is NOT TAKEN:
;; what it would buy and what it would cost if `Receipt` and `FindResult` at
;; `Holes.lean` grew fields.  Seven things could make that pricing false while
;; `lake build` stays green, because each is a fact about a file the arm module
;; never mentions:
;;
;;   1. The amendment could have been TAKEN.  If `Receipt` grows the three data
;;      fields or `FindResult` grows a `zeroMass` field, the arm is no longer a
;;      priced option, it is the state of the world, and every :buys below is
;;      describing something already paid for.
;;   2. `FindReceiptRow` could stop carrying `zeroMass`.  The whole reason F4 is
;;      not a conjunct of `ConformantFind` today is that the designation lives on
;;      the ROW carrier and not on the result.  Move it and the F4 half of the
;;      arm evaporates.
;;   3. `ConformantFind.f2Receipted` could stop stating PRESENCE.  The F2 half's
;;      buy is measured as the difference between `.isSome` and a content
;;      predicate; a content-stating `f2Receipted` would already be the amendment.
;;   4. Slice 4's erasure-equality benchmark could go.  M2's claim is that the
;;      joint carrier separates a pair today's carrier cannot, and "cannot" is
;;      `findRErasuresAreEqual`.  Without it the separation is a difference and
;;      not a buy.
;;   5. A reading of F4 could move.  The arm reports which readings the returned
;;      designation implies and which it refutes, all three by name in three
;;      different files.
;;   6. The mechanical cost could move.  The joint census is the set of record
;;      literals that would have to gain a field, RECOMPUTED here over every Lean
;;      file under DarkTower and COMPARED site by site against the pinned list --
;;      not merely counted, because a site that moved file is a different price
;;      from a site that moved line.
;;   7. The recorded designation could change shape.  Every nonempty-selection
;;      floor in the module is witnessed at `g1Snatcher` against a designation of
;;      one pattern per scenario over six scenarios.
;;
;; It takes NO ruling.  It does not say whether to amend the carrier; it says
;; what amending it would buy and cost.  It asserts that `find` at
;; `Holes.lean:264` is still a sorry, because an entry that priced a discharged
;; declaration would be pricing nothing.
;;
;; CLASSIFICATION RULE for the census (stated because it is textual, not
;; type-directed): a record literal that assigns `citesTextOrEdges` is a plain
;; `Receipt` construction unless the same literal also assigns
;; `acknowledgedClause`, `acknowledgesClause`, `retrievalRoute`, `hasRoute` or
;; `hasAsOf`, in which case it builds one of the amended receipt carriers
;; (`RelationalReceipt`, `ReceiptWithAssertions`, `AmendedReceipt`).  A record
;; literal that assigns `receipts` is a plain `FindResult` construction unless it
;; also assigns `zeroMass` (`AmendedFindResult`) or its receipt payload assigns
;; `acknowledgedClause` (`FindResultR`).  The rule is applied to the literal's
;; own text, delimited by the closing brace or the `where` block's blank line.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F11AC_ARM, F11AC_HOLES,
;; F11AC_CONF, F11AC_DISCHARGE, F11AC_RECEIPT, F11AC_F4, F11AC_DARK, F11AC_OUT.
(require '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def dark (or (System/getenv "F11AC_DARK") (str home "/code/mathlib4/DarkTower")))
(def holes-path (or (System/getenv "F11AC_HOLES") (str dark "/WarMachine/Holes.lean")))
(def conf-path (or (System/getenv "F11AC_CONF") (str dark "/WarMachine/F11Conformance.lean")))
(def discharge-path (or (System/getenv "F11AC_DISCHARGE")
                        (str dark "/WarMachine/F11DischargeArm.lean")))
(def receipt-path (or (System/getenv "F11AC_RECEIPT")
                      (str dark "/WarMachine/F11ReceiptCarrier.lean")))
(def f4-path (or (System/getenv "F11AC_F4") (str dark "/WarMachine/F11F4Reading.lean")))
(def arm-path (or (System/getenv "F11AC_ARM") (str dark "/WarMachine/F11AmendedCarrier.lean")))
(def out-path (or (System/getenv "F11AC_OUT") (str lab "/runs/F11-find/11-amended-carrier.edn")))

(defn lines [p] (if (.exists (io/file p)) (str/split-lines (slurp p)) []))
(def findings (atom []))
(defn fail! [k m] (swap! findings conj {:finding k :detail m}))

(def decl-re "^(noncomputable )?(theorem|def|abbrev|structure) ")

(defn decl-body
  "Lines of the declaration named `nm` in `ls`, from its signature to the blank
   line that ends it.  Recognises the declaration by NAME rather than by line
   number, so a planted tree is read the same way as the committed one."
  [ls nm]
  (when-let [start (first (keep-indexed
                           (fn [i l] (when (re-find (re-pattern (str decl-re nm "\\b")) l) i))
                           ls))]
    (let [body (take-while #(not (str/blank? %)) (drop (inc start) ls))]
      (vec (cons (nth ls start) body)))))

(defn decl-line [ls nm]
  (first (keep-indexed
          (fn [i l] (when (re-find (re-pattern (str decl-re nm "\\b")) l) (inc i)))
          ls)))

;; ------------------------------------------- 1 the amendment is NOT yet taken
(def holes-lines (lines holes-path))
(def receipt-struct (decl-body holes-lines "Receipt"))
(def receipt-fields
  (vec (sort (keep #(second (re-find #"^\s{2,}(\w+)\s*:" %)) (rest (or receipt-struct []))))))
(def find-result-struct (decl-body holes-lines "FindResult"))
(def find-result-fields
  (vec (sort (keep #(second (re-find #"^\s{2,}(\w+)\s*:" %)) (rest (or find-result-struct []))))))
(when-not (= ["citesTextOrEdges" "scoreAlone"] receipt-fields)
  (fail! :receipt-carrier-is-no-longer-the-two-proposition-carrier
         {:file holes-path :line (decl-line holes-lines "Receipt") :fields receipt-fields}))
(when-not (= ["absence" "receipts" "selected"] find-result-fields)
  (fail! :find-result-carrier-changed
         {:file holes-path :line (decl-line holes-lines "FindResult") :fields find-result-fields}))

;; -------------------------------- 2 zeroMass still lives on the ROW carrier
(def row-struct (decl-body holes-lines "FindReceiptRow"))
(def row-fields
  (vec (sort (keep #(second (re-find #"^\s{2,}(\w+)\s*:" %)) (rest (or row-struct []))))))
(when-not (some #{"zeroMass"} row-fields)
  (fail! :zero-mass-no-longer-on-the-row-carrier
         {:file holes-path :line (decl-line holes-lines "FindReceiptRow") :fields row-fields}))

;; ------------------------------------------- 3 today's F2 states PRESENCE only
(def conf-lines (lines conf-path))
(def conformant (decl-body conf-lines "ConformantFind"))
(def conformant-text (str/join " " (or conformant [])))
(def f2-is-presence-only?
  (boolean (and conformant
                (re-find #"f2Receipted[^\n]*isSome" conformant-text)
                (not (str/includes? conformant-text "acknowledgedClause")))))
(when-not f2-is-presence-only?
  (fail! :todays-f2-no-longer-states-presence-only
         {:file conf-path :line (decl-line conf-lines "ConformantFind") :text conformant-text}))

;; ------------------------------------ 4 slice 4's erasure-equality benchmark
(def receipt-lines (lines receipt-path))
(def benchmark (decl-body receipt-lines "findRErasuresAreEqual"))
(def benchmark-text (str/join " " (or benchmark [])))
(def benchmark-is-an-equality?
  (boolean (and benchmark
                (re-find #"eraseFinder findRFaithful = eraseFinder findRMisattributing"
                         benchmark-text))))
(when-not benchmark-is-an-equality?
  (fail! :slice-4-erasure-equality-benchmark-missing-or-changed
         {:file receipt-path :line (decl-line receipt-lines "findRErasuresAreEqual")}))

;; ------------------------------------------------------- 5 the three readings
(def discharge-lines (lines discharge-path))
(def f4-lines (lines f4-path))
(def readings
  {:a {:declaration "FindFalsifiable" :file conf-path :ls conf-lines
       :must #"∃ p ∈ repo\.patterns, p ∉ \(f t repo\)\.selected"}
   :b {:declaration "FindExcludesRecordedZeroMass" :file discharge-path :ls discharge-lines
       :must #"findSnatchZeroMass t\.context"}
   :c {:declaration "FindRespectsZeroMass" :file f4-path :ls f4-lines
       :must #"\(zm : State → Set P\)"}})
(def reading-report
  (into (sorted-map)
        (for [[k {:keys [declaration file ls must]}] readings]
          (let [body (decl-body ls declaration)
                text (str/join " " (or body []))
                ok (boolean (and body (re-find must text)))]
            (when-not ok
              (fail! (keyword (str "reading-" (name k) "-missing-or-changed"))
                     {:file file :declaration declaration :text text}))
            [k {:declaration declaration
                :file (str/replace file (str home "/code/") "")
                :line (decl-line ls declaration)
                :shape-holds ok}]))))

;; ------------------------------------------------------ 6 the joint census
(def lean-files
  (vec (sort (map #(.getPath %)
                  (filter #(str/ends-with? (.getName %) ".lean")
                          (file-seq (io/file dark)))))))

(defn literal-window
  "The text of the record literal that begins at line index `i` of `ls`: the
   line itself plus following lines up to and including the first line carrying
   a closing brace, or up to the first blank line for a `where` block.  Bounded
   at 8 lines so a runaway file cannot make the window swallow the next
   declaration."
  [ls i]
  (let [tail (take 8 (drop i ls))
        [head & more] tail
        upto (loop [acc [head] more' more]
               (cond (empty? more') acc
                     (str/blank? (first more')) acc
                     (str/includes? (first (take-last 1 acc)) "}") acc
                     :else (recur (conj acc (first more')) (next more'))))]
    (str/join " " upto)))

(def amended-receipt-markers
  ["acknowledgedClause" "acknowledgesClause" "retrievalRoute" "hasRoute" "hasAsOf"])

(defn sites
  "Every record literal in `dark` assigning `field`, as {:file :line :window}."
  [field]
  (vec (sort-by (juxt :file :line)
                (mapcat (fn [f]
                          (let [ls (lines f)]
                            (keep-indexed
                             (fn [i l]
                               (when (re-find (re-pattern (str "(^|[{ ])" field "\\s*:=")) l)
                                 {:file (str/replace f (str home "/code/") "")
                                  :line (inc i)
                                  :window (literal-window ls i)}))
                             ls)))
                        lean-files))))

(defn classify-receipt [w]
  (if (some #(str/includes? w %) amended-receipt-markers) :excluded-by-receipt-data-marker :receipt))

(defn classify-result [w]
  (cond (str/includes? w "zeroMass :=") :excluded-by-zero-mass-marker
        (some #(str/includes? w %) amended-receipt-markers) :excluded-by-receipt-data-marker
        :else :find-result))

(def receipt-sites
  (vec (for [s (sites "citesTextOrEdges")]
         (assoc (dissoc s :window) :carrier (classify-receipt (:window s))))))
(def result-sites
  (vec (for [s (sites "receipts")
             ;; a `receipts` MENTION that is not an assignment inside a literal
             ;; (an equality goal, say) carries no `:=` and is already excluded
             ;; by the regex; a projection `.receipts` is excluded by the
             ;; leading-boundary group.
             :when (not (str/includes? (:window s) ".receipts ="))]
         (assoc (dissoc s :window) :carrier (classify-result (:window s)))))) 

(defn plain [ss k] (vec (filter #(= k (:carrier %)) ss)))
(def joint-census
  {:receipt (plain receipt-sites :receipt)
   :find-result (plain result-sites :find-result)})

;; The sites the entry quotes.  A change here changes the arm's price.  The
;; comparison is SITE BY SITE, not by count: a site that moved file is a
;; different price from one that moved line.
(def quoted-census
  {:receipt [{:file "mathlib4/DarkTower/WarMachine/F11Conformance.lean" :line 67 :carrier :receipt}]
   :find-result
   [{:file "mathlib4/DarkTower/WarMachine/F11AmendedCarrier.lean" :line 51 :carrier :find-result}
    {:file "mathlib4/DarkTower/WarMachine/F11Conformance.lean" :line 76 :carrier :find-result}
    {:file "mathlib4/DarkTower/WarMachine/F11Conformance.lean" :line 121 :carrier :find-result}
    {:file "mathlib4/DarkTower/WarMachine/F11Conformance.lean" :line 143 :carrier :find-result}
    {:file "mathlib4/DarkTower/WarMachine/F11DischargeArm.lean" :line 27 :carrier :find-result}
    {:file "mathlib4/DarkTower/WarMachine/F11DischargeArm.lean" :line 76 :carrier :find-result}
    {:file "mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean" :line 22 :carrier :find-result}
    {:file "mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean" :line 144 :carrier :find-result}]})
(doseq [k [:receipt :find-result]]
  (when-not (= (get joint-census k) (get quoted-census k))
    (fail! :joint-census-changed
           {:carrier k :quoted (get quoted-census k) :recomputed (get joint-census k)})))

;; ---------------------------------------------------- 7 the recorded designation
(def zm-decl (decl-body holes-lines "findSnatchZeroMass"))
(def zm-arms
  (vec (sort-by :scenario
                (keep (fn [l]
                        (when-let [[_ sc pats] (re-find #"^\s*\|\s*\.(\w+)\s*=>\s*\[([^\]]*)\]" l)]
                          {:scenario sc
                           :patterns (vec (sort (map #(str/replace (str/trim %) #"^\." "")
                                                     (str/split pats #","))))}))
                      (or zm-decl [])))))
(def zm-distinct (vec (sort (distinct (mapcat :patterns zm-arms)))))
(when-not (and (= 6 (count zm-arms))
               (every? #(= 1 (count (:patterns %))) zm-arms)
               (= 3 (count zm-distinct)))
  (fail! :recorded-designation-changed-shape
         {:file holes-path :arms zm-arms :distinct zm-distinct}))

;; --------------------------------------------- the sorry is still a sorry
(def find-line (decl-line holes-lines "find"))
(def find-text (when find-line (nth holes-lines (dec find-line))))
(when-not (and find-text (str/includes? find-text "sorry"))
  (fail! :find-not-declared-as-a-sorry {:file holes-path :line find-line :text find-text}))

;; ------------------------------------------------------------------- artifact
(def arm-lines (lines arm-path))
(def verdict (if (empty? @findings) :pass :fail))
(def artifact
  (sorted-map
   :record :F11-slice-6-amended-carrier
   :row :F11
   :slice 6
   :verdict verdict
   :findings (vec (sort-by :finding @findings))
   :amendment-not-taken {:file "mathlib4/DarkTower/WarMachine/Holes.lean"
                         :receipt-line (decl-line holes-lines "Receipt")
                         :receipt-fields receipt-fields
                         :find-result-line (decl-line holes-lines "FindResult")
                         :find-result-fields find-result-fields
                         :zero-mass-still-on-the-row-carrier
                         {:line (decl-line holes-lines "FindReceiptRow") :fields row-fields}}
   :todays-f2-states-presence-only {:file "mathlib4/DarkTower/WarMachine/F11Conformance.lean"
                                    :line (decl-line conf-lines "ConformantFind")
                                    :holds f2-is-presence-only?}
   :slice-4-benchmark {:declaration "findRErasuresAreEqual"
                       :file "mathlib4/DarkTower/WarMachine/F11ReceiptCarrier.lean"
                       :line (decl-line receipt-lines "findRErasuresAreEqual")
                       :is-an-erasure-equality benchmark-is-an-equality?}
   :readings reading-report
   :joint-census {:receipt (:receipt joint-census)
                  :find-result (:find-result joint-census)
                  :total (+ (count (:receipt joint-census)) (count (:find-result joint-census)))
                  :total-excluded-by-the-classification-rule
                  {:receipt-field-sites-excluded-by-a-receipt-data-marker
                   (count (plain receipt-sites :excluded-by-receipt-data-marker))
                   :receipts-field-sites-excluded-by-a-receipt-data-marker
                   (count (plain result-sites :excluded-by-receipt-data-marker))
                   :receipts-field-sites-excluded-by-a-zero-mass-marker
                   (count (plain result-sites :excluded-by-zero-mass-marker))}}
   :recorded-designation {:file "mathlib4/DarkTower/WarMachine/Holes.lean"
                          :line (decl-line holes-lines "findSnatchZeroMass")
                          :scenarios (count zm-arms)
                          :distinct-values zm-distinct
                          :arms zm-arms}
   :arm-module {:file "mathlib4/DarkTower/WarMachine/F11AmendedCarrier.lean"
                :declarations (count (filter #(re-find (re-pattern decl-re) %) arm-lines))}
   :find-still-a-sorry {:file "mathlib4/DarkTower/WarMachine/Holes.lean" :line find-line}))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint artifact)))
(println (format "f11_amended_carrier_check: %s -- %d findings; artifact %s"
                 (name verdict) (count @findings) out-path))
(doseq [f (:findings artifact)] (println "  " (name (:finding f)) (pr-str (:detail f))))
(when (= :fail verdict) (System/exit 1))
