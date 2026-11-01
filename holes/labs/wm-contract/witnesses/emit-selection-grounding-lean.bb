#!/usr/bin/env bb
;; Usage: bb emit-selection-grounding-lean.bb WITNESS_DIR PASS_EDN OUTPUT_LEAN
;; Open-set pin hashes pr-str of the sorted vector of ID strings, not file bytes.
(require '[clojure.edn :as edn] '[clojure.string :as str] '[clojure.set :as set])
(import '[java.security MessageDigest] '[java.math BigDecimal BigInteger RoundingMode])
(defn sha [s] (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8")))))
(defn ensure! [ok msg data] (when-not ok (throw (ex-info msg data))))
(defn pin-id [s] (ensure! (boolean (re-matches #"[0-9a-f]{64}" s)) "invalid digest" {:sha s})
  (let [n (BigInteger. (subs s 0 15) 16)] (ensure! (pos? n) "zero ID" {:sha s}) (str n)))
(defn text-id [s] (pin-id (sha s)))
(defn rat [x] (if (ratio? x) (str "(" (numerator x) " / " (denominator x) " : ℚ)") (str "(" x " : ℚ)")))
(defn rounded [x] (/ (.longValueExact (.movePointRight (.setScale (BigDecimal/valueOf (double x)) 6 RoundingMode/HALF_EVEN) 6)) 1000000))
(defn rounded-score [s]
  (let [ts (mapv #(rounded (get s %)) [:risk :ambiguity :novelty]) g (- (+ (ts 0) (ts 1)) (ts 2))
        diff (- (double g) (double (:G s)))]
    (ensure! (< (Math/abs diff) 0.000002) "rounded score differs" {:difference diff})
    {:terms ts :g g :difference diff}))
(defn terms [ts] (str "⟨" (str/join ", " (map rat ts)) "⟩"))
(defn list-lean [xs]
  ;; Chunk long literals to bound Lean elaborator recursion without shrinking the field.
  (if (> (count xs) 32)
    (str "(List.flatten " (list-lean (map list-lean (partition-all 32 xs))) ")")
    (str "[" (str/join ",\n    " xs) "]")))
(def status {:ready ".ready" :not-ready ".notReady" :undetermined ".undetermined"})
(let [[dir pass-path out] *command-line-args*]
  (ensure! (and dir pass-path out) "expected WITNESS_DIR PASS_EDN OUTPUT_LEAN" {})
  (let [wtext (slurp (str dir "/outer-selection-conformant-2026-11-01.edn")) w (edn/read-string wtext)
        ptext (slurp pass-path) psha (sha ptext)
        _ (ensure! (= psha "a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91"
                      (get-in w [:field :giraffe :pass-sha256])) "pass pin mismatch" {:actual psha})
        p (edn/read-string ptext)
        otext (slurp (str dir "/" (get-in w [:field :population :ids-file])))
        osha (sha otext)
        opens (:ids (edn/read-string otext))
        canonical-open-sha (sha (pr-str (vec (sort (map str opens)))))
        _ (ensure! (= canonical-open-sha "e82f6381fa3e056fff332cdb510662469e353fb120cbb7120d1eb2b46f9333d9" (get-in w [:field :population :ids-sha256])) "open-set canonical pin mismatch" {:actual canonical-open-sha})
        rows (:order p) row-names (mapv #(name (:id %)) rows)
        missing (sort (set/difference (set opens) (set row-names)))
        drift (set (map name (get-in p [:graph-coverage :drift :unread-new])))
        _ (ensure! (and (= 626 (count opens) (count (set opens))) (= 609 (count rows) (count (set row-names)))
                        (= 17 (count missing)) (set/subset? (set row-names) (set opens))
                        (= (set missing) (set/intersection (set opens) drift))) "population partition mismatch" {})
        _ (ensure! (= (set missing) (set (mapcat :ids (get-in w [:field :excluded])))) "witness exclusions differ" {})
        _ (ensure! (= {:ready 500 :not-ready 57 :undetermined 52} (frequencies (map :status rows))) "status counts differ" {})
        _ (ensure! (= 626 (count (set (map text-id opens)))) "task ID collision" {})
        cs (:cascades w)
        ;; Canonical cascade carrier: ordered vectors; never map iteration order.
        carriers (mapv (fn [c] [(name (:id c)) (:readme-cascade c)
                               (mapv (fn [u] [(:pattern u) (:sha256 u) (:index u)]) (:units c))
                               (:edges c)]) cs)
        cids (mapv #(text-id (pr-str %)) carriers)
        _ (ensure! (= 3 (count (set cids))) "cascade ID collision" {})
        _ (ensure! (= 629 (count (set (concat cids (map text-id opens))))) "task/cascade ID collision" {})
        scores (mapv #(rounded-score (:score %)) cs)
        abst (rounded-score (get-in w [:abstention :score]))
        picks (mapv #(or (get-in % [:applied :pick-target]) (get-in % [:applied :pick])) cs)
        _ (ensure! (every? #(contains? (set opens) %) picks) "pick not enumerated" {})
        c3head (name (:id (first (sort-by :rank (filter #(= :undetermined (:status %)) rows)))))
        c3known (= c3head (picks 2))
        ;; Preserve repair-click's named obligations: target equality alone is not a proof of these steps.
        step-str (fn [s] (case s :ready-gate ".readyGate" :by-cone ".byCone" :by-unblocks ".byUnblocks"
                          (str "(.named " (text-id (pr-str s)) ")")))
        steps [(mapv step-str (get-in cs [0 :applied :steps]))
               (mapv step-str (get-in cs [1 :applied :steps]))
               (if c3known ["(.statusGate .undetermined)" ".byRank"]
                 (mapv #(str "(.named " (text-id (pr-str %)) ")") (get-in cs [2 :applied :steps]))) ]
        reaches [1/2 1/2 609/615]
        _ (doseq [i (range 3)] (ensure! (< (Math/abs (- (double (reaches i)) (double (get-in cs [i :model :reach])))) 1.0e-12)
                                                  "reach mismatch" {:cascade i}))
        policy-lines (mapv (fn [i]
                            (ensure! (= :associated (get-in cs [i :relation :class])) "unsupported relation" {:cascade i})
                            (str "def c" (inc i) " : GroundedPolicy :=\n  ⟨" (cids i) ", " (list-lean (steps i))
                                 ", some " (text-id (picks i)) ", " (rat (reaches i)) ", .associated, "
                                 (terms (:terms (scores i))) ", " (rat (:g (scores i))) "⟩\n")) (range 3))
        chosen (first (sort-by #(get-in scores [% :g]) (range 3)))
        _ (ensure! (= (get-in w [:selection :minimum]) (:id (cs chosen))) "minimum differs" {})
        facts (mapv (fn [r]
                      (ensure! (and (status (:status r)) (pos-int? (:rank r))) "invalid status/rank" {:id (:id r)})
                      (doseq [k [:cone :unblocks]]
                        (ensure! (or (nat-int? (k r)) (and (= :undetermined (:status r)) (nil? (k r)))) "invalid metric" {:id (:id r) :key k}))
                      (str ".fact " (text-id (name (:id r))) " ⟨" (status (:status r)) ", " (or (:cone r) 0)
                           ", " (or (:unblocks r) 0) ", " (:rank r) ", " (pin-id (get-in r [:dependency-reading :record-pin :sha256])) "⟩")) rows)
        excluded (map #(str ".excluded " (text-id %) " 1 " (pin-id psha)) missing)
        result (str "import DarkTower.WarMachine.SelectionGrounding\n\n"
                    "/-! Generated by emit-selection-grounding-lean.bb; do not hand-edit.\n"
                    "Witness raw SHA256: " (sha wtext) "\nPass raw SHA256: " psha "\nOpen-set raw SHA256: " osha "\n"
                    "IDs: first 15 SHA256 hex digits interpreted as Nat; task digest = UTF-8 task name.\n"
                    "Open-set canonical SHA256: " canonical-open-sha " (sorted ID vector pr-str).\n"
                    "Drift outside enumerated set: " (pr-str (sort (set/difference drift (set opens)))) "\n"
                    "Cascade digest = pr-str of [name readme-row [[pattern sha index] ...] edges].\n"
                    "Fact pins use dependency-reading.record-pin.sha256; exclusion reason 1 = giraffe/unread-new.\n"
                    "Exclusion pin = first 15 digits of pass digest (not a second hash).\n"
                    "Undetermined nil cone/unblocks encoded as 0; status preserved. Descending metric orders follow readyGate here. Ordering undetermined rows by those unavailable metrics would be an invalid use of this carrier; status/rank projection does not use them.\n"
                    "Terms rounded HALF_EVEN at 6 decimals; G recomputed from rounded terms.\n"
                    "Repair-click retains named README steps, so overall G2 is unverifiable.\n"
                    "Cascade 3 status/rank projection matches retained pick: " c3known ".\n"
                    "This is a hand-built witness, not evidence that the runtime produced it. -/\n"
                    "namespace DarkTower.WarMachine.SelectionGroundingWitness\nopen SelectionGrounding\nset_option maxRecDepth 4096\n\n"
                    (str/join "\n" policy-lines)
                    "\ndef witness : Grounding where\n  enumerated := " (list-lean (map text-id opens)) ".toFinset\n"
                    "  field := " (list-lean (concat facts excluded)) "\n  policies := [c1, c2, c3]\n"
                    "  abstention := (" (terms (:terms abst)) ", " (rat (:g abst)) ")\n  chosen := " (cids chosen) "\n\n"
                    "def witnessRun : Requirements.RunFacts :=\n  { Requirements.selectionGood with\n"
                    "    enumeratedTasks := witness.enumerated\n    currentDecision := .pick " (text-id (picks chosen)) " " (cids chosen) "\n"
                    "    policyG := [(c1.cascade, c1.g), (c2.cascade, c2.g), (c3.cascade, c3.g)]\n    abstentionG := witness.abstention.2 }\n\n"
                    "theorem witness_field_considered : fieldConsidered witness = true := by native_decide\n"
                    "theorem witness_scored_from_terms : scoredFromTerms witness = true := by native_decide\n"
                    "theorem witness_agrees : agreesWithRunFacts witness witnessRun = true := by native_decide\n"
                    "theorem witness_prioritised_unverifiable : prioritised witness = none := by native_decide\n"
                    "theorem witness_c1_pick : recomputedPick witness c1 = some c1.pick := by native_decide\n"
                    (when c3known "theorem witness_c3_pick : recomputedPick witness c3 = some c3.pick := by native_decide\n")
                    "\n#print axioms witness_field_considered\nend DarkTower.WarMachine.SelectionGroundingWitness\n")]
    (spit out result)
    (doseq [i (range 3)] (println (:id (cs i)) :term-difference (:difference (scores i))))
    (println :abstention-difference (:difference abst) :cascade3-recomputed c3known :repair-click :unverifiable)
    (println :file-sha256 (sha result) :bytes (count (.getBytes result "UTF-8")))))
