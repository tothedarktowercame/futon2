#!/usr/bin/env bb
;; f12_d1_arms_check.bb -- `:F12` slice 5.  CHECKS the D1 arm file against the
;; slice-2 derivation, and RECORDS which of O1-O4 each arm states and proves.
;;
;; WHY A CHECK AND NOT A READ.  The three arms in
;; `mathlib4:DarkTower/WarMachine/F12D1Arms.lean` are elaborated against the
;; zaif cascade, whose numbers were derived in slice 2 into
;; `runs/F12-organise/01-zaif-transcription.edn`.  The arm file re-declares
;; those numbers, because the slice-2 declarations in `Holes.lean` are
;; `private`.  A re-declaration that drifts from the derivation would make
;; every arm verdict a verdict about different data, and nothing in `lake
;; build` can see that.  So this file re-reads the arm source and fails on any
;; disagreement with the artifact.
;;
;; WHAT IT DOES NOT DO.  It does not decide D1, and it does not rank the arms.
;; The per-arm law table it writes is a record of what elaborated.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two
;; runs over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F12_ARMS, F12_RECORD, F12_OUT.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def arms-path (or (System/getenv "F12_ARMS")
                   (str home "/code/mathlib4/DarkTower/WarMachine/F12D1Arms.lean")))
(def record-path (or (System/getenv "F12_RECORD")
                     (str lab "/runs/F12-organise/01-zaif-transcription.edn")))
(def out-path (or (System/getenv "F12_OUT") (str lab "/runs/F12-organise/03-d1-arms.edn")))

(defn die [m data]
  (binding [*out* *err*] (println "f12-d1-arms:" m (pr-str data)))
  (System/exit 1))

(def src
  (if (.exists (io/file arms-path))
    (slurp arms-path)
    (die "arm file not found" {:path arms-path})))

(def record (edn/read-string (slurp record-path)))

(defn- number-after [re what]
  (if-let [m (re-find re src)]
    (parse-long (second m))
    (die "the arm file does not state" {:what what})))

(defn- edges-of
  "The `(u,v)` arms of a Lean `Nat -> Nat -> Prop` match, read from the source."
  [decl]
  (let [body (second (re-find (re-pattern (str "(?s)" decl " : Nat → Nat → Prop\n(.*?)=> True")) src))]
    (when-not body (die "the arm file states no arms for" {:decl decl}))
    (vec (sort (map (fn [[_ a b]] [(parse-long a) (parse-long b)])
                    (re-seq #"\|\s*(\d+),\s*(\d+)" body))))))

(def facts
  {:selected-below (number-after #"d1Selected : Set Nat := \{n \| n < (\d+)\}" :d1Selected)
   :admitted-from  (number-after #"d1Admitted : Set Nat := \{n \| (\d+) ≤ n" :d1Admitted-lower)
   :admitted-below (number-after #"d1Admitted : Set Nat := \{n \| \d+ ≤ n ∧ n < (\d+)\}" :d1Admitted-upper)
   :nodes-below    (number-after #"d1Nodes : Set Nat := \{n \| n < (\d+)\}" :d1Nodes)
   :closure-below  (number-after #"d1Closure : Set Nat := \{n \| n < (\d+)\}" :d1Closure)
   :authored-edges (edges-of "d1Authored")})

;; The declarations this slice's packet required, per arm.  `:stated` means the
;; declaration is present in the source; whether it PROVES anything is `lake
;; build`'s verdict, recorded beside this one and not inferred here.
(def required
  [[:arm-1-widen-cascade
    [[:carrier "structure ArmOneCascade"]
     [:organise-type "abbrev armOneOrganiseType"]
     [:fixture "def armOneZaif"]
     [:o1 "armOneO1"]
     [:o2 "armOneO2"]
     [:o3 "armOneO3"]]]
   [:arm-2-cascadediff-codomain
    [[:organise-type "abbrev armTwoOrganiseType"]
     [:o4-antecedent "armTwoO4AntecedentUnsatisfiable"]
     [:o4-acting-order-flat "armTwoZaifActingOrderFlat"]
     [:o4-score-flat "armTwoZaifScoreFlat"]]]
   [:arm-3-cascade-repository-pair
    [[:repository "def d1Repo"]
     [:cascade "def d1CascadeZaif"]
     [:o2 "armThreeO2"]
     [:o3 "armThreeO3"]
     [:o1-substitution-refuted "armThreeO1PatternsSubstitutionFails"]
     [:non-vacuity-edge-exists "armThreeOrganisedEdgeExists"]
     [:non-vacuity-no-unauthored-edge "armThreeNoUnauthoredEdge"]]]])

(def declarations
  (into (sorted-map)
        (for [[arm decls] required]
          [arm (into (sorted-map)
                     ;; `(some? false)` is TRUE, so the obvious spelling of this
                     ;; would record every declaration as present whatever the
                     ;; source says; the third plant below caught it.
                     (for [[k needle] decls]
                       [k (str/includes? src needle)]))])))

(def sorry-free?
  (not (re-find #"(?m)^\s*sorry\b|[^A-Za-z]sorry\b" src)))

(def touches-holes?
  (some? (re-find #"(?m)^import DarkTower\.WarMachine\.Holes\b" src)))

(def problems
  (let [enc (:encoding record)
        counts (:counts record)]
    (cond-> []
      (not= (:selected-below facts) (:selected-below enc))
      (conj [:selected-bound (:selected-below facts) (:selected-below enc)])

      (not= (:admitted-from facts) (:admitted-from enc))
      (conj [:admitted-lower-bound (:admitted-from facts) (:admitted-from enc)])

      (not= (:admitted-below facts) (:nodes-below enc))
      (conj [:admitted-upper-bound (:admitted-below facts) (:nodes-below enc)])

      (not= (:nodes-below facts) (:nodes-below enc))
      (conj [:nodes-bound (:nodes-below facts) (:nodes-below enc)])

      (not= (:closure-below facts) (:closure counts))
      (conj [:closure-bound (:closure-below facts) (:closure counts)])

      (not= (:authored-edges facts) (vec (sort (map vec (:authored-edges record)))))
      (conj [:authored-edges (:authored-edges facts) (:authored-edges record)])

      (not= (count (:authored-edges facts)) (:authored-edges counts))
      (conj [:authored-edge-count (count (:authored-edges facts)) (:authored-edges counts)])

      (not sorry-free?) (conj [:sorry-in-arm-file])
      (not touches-holes?) (conj [:arm-file-does-not-import-holes])

      (seq (for [[arm ds] declarations [k present?] ds :when (not present?)] [arm k]))
      (conj (into [:missing-declarations]
                  (for [[arm ds] declarations [k present?] ds :when (not present?)] [arm k]))))))

(def result
  (sorted-map
   :arms declarations
   :basis (sorted-map :record record-path
                      :arm-file arms-path
                      :zaif-basis (get-in record [:basis :sha])
                      :run (get-in record [:basis :run]))
   :facts (into (sorted-map) facts)
   :problems problems))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint result)))
(println "f12-d1-arms: wrote" out-path)
(if (seq problems)
  (do (binding [*out* *err*] (println "f12-d1-arms: FAIL" (pr-str problems))) (System/exit 1))
  (println "f12-d1-arms: PASS -- arm file agrees with the slice-2 derivation"))
