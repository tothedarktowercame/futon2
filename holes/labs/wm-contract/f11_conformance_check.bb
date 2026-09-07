#!/usr/bin/env bb
;; f11_conformance_check.bb -- `:F11` slice 2.  CHECKS the find-conformance
;; file's premises against sources it does not itself write.
;;
;; WHY A CHECK AND NOT A READ.  Slice 2 states F1-F4 of the FUNCTION `find`
;; rather than of a recorded row, so every result in it is a result ABOUT THE
;; s3e SIGNATURE and about the recorded snatch find.  Five things can go
;; silently wrong in a way `lake build` cannot see, because each leaves the
;; file elaborating perfectly while being about something else:
;;
;;   1. `FindType` could drift from the type `find` actually has at
;;      `Holes.lean:264`.  Then every theorem is a theorem about a different
;;      interface.  Checked by reading BOTH types out of their own files.
;;   2. "F4 is not stateable of one FindResult" is a claim about which FIELDS
;;      `FindResult` has.  A docstring cannot be wrong about that in a way the
;;      build notices.  Checked by reading the field lists of `FindResult` and
;;      `FindReceiptRow` and requiring `zeroMass` to be absent from the first
;;      and present in the second.
;;   3. The same for the reported `Receipt` limitation: F2's prose asks for the
;;      acknowledged clause, the retrieval route and an as-of, and the carrier
;;      has two Props.  Checked by reading `Receipt`'s fields.
;;   4. The Lean literals could have been retyped by hand instead of read from
;;      the transcribed fixture.  Checked by requiring every `SnatchPattern`
;;      constructor named in the conformance file's CODE to be a member of
;;      `snatchRepository` as `Holes.lean` states it.
;;   5. The scenario facts the theorems name -- the declared zero-mass member
;;      excluded, `askForSurplusNotSurrender` selected at `g1Snatcher`, the
;;      receipted column -- are claims about the RECORD.  They are recomputed
;;      here from `futon3:checks/find-snatch.edn`, the pinned fixture the four
;;      `findF*` docstrings name and the one `u46_find_transcribe.bb`
;;      transcribed, so a theorem cannot be true of Lean literals that no
;;      longer match the record.
;;
;; The live re-run (`runs/F11-find/01-find-snatch-live.edn`, C559) is read too,
;; but only to REPORT the pin-vs-live divergence, never to gate: the Lean
;; transcription is at the pin's 18-pattern era and the live library carries 24
;; (C559 s3).  Gating on the live file would fail this check for a reason that
;; belongs to C500 s3's fixture-pin defect, not to this slice.
;;
;; It takes NO ruling -- on which reading of F4 is the right one, or on whether
;; the sorry at `Holes.lean:264` should be discharged.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F11_CONF, F11_HOLES, F11_PIN,
;; F11_LIVE, F11_OUT.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def conf-path (or (System/getenv "F11_CONF")
                   (str home "/code/mathlib4/DarkTower/WarMachine/F11Conformance.lean")))
(def holes-path (or (System/getenv "F11_HOLES")
                    (str home "/code/mathlib4/DarkTower/WarMachine/Holes.lean")))
(def pin-path (or (System/getenv "F11_PIN")
                  (str home "/code/futon3/checks/find-snatch.edn")))
(def live-path (or (System/getenv "F11_LIVE")
                   (str lab "/runs/F11-find/01-find-snatch-live.edn")))
(def out-path (or (System/getenv "F11_OUT") (str lab "/runs/F11-find/04-conformance.edn")))

(defn die [m data]
  (binding [*out* *err*] (println "f11-conformance:" m (pr-str data)))
  (System/exit 1))

(defn- read-source [path what]
  (if (.exists (io/file path)) (slurp path) (die "file not found" {:what what :path path})))

(def src (read-source conf-path :conformance-file))
(def holes (read-source holes-path :holes))
(def pin (edn/read-string (read-source pin-path :pinned-fixture)))
(def live (edn/read-string (read-source live-path :live-rerun)))

(defn- squeeze [s] (str/trim (str/replace s #"\s+" " ")))

(defn- camel
  "`:ask-for-surplus-not-surrender` as the Lean constructor
  `askForSurplusNotSurrender`, so record keywords and Lean names compare."
  [kw]
  (let [[h & t] (str/split (name kw) #"-")]
    (apply str h (map str/capitalize t))))

;; --- 1. the signature -------------------------------------------------------

(def find-type-in-holes
  (if-let [m (re-find #"def find \{State P : Type\*\} :\s*([^:=]+):= sorry" holes)]
    (squeeze (second m))
    (die "Holes.lean does not declare find in the expected shape" {:path holes-path})))

(def find-type-in-slice
  (if-let [m (re-find #"abbrev FindType \(State P : Type\*\) :=\s*\n?\s*([^\n]+)" src)]
    (squeeze (second m))
    (die "the conformance file states no FindType abbrev" {:path conf-path})))

;; --- 2/3. the carriers the non-statements are about -------------------------

(defn- fields-of [source structure-name]
  (if-let [m (re-find (re-pattern (str "(?s)structure " structure-name
                                       "[^\n]*where\n(.*?)\n\n"))
                      source)]
    (vec (sort (map second (re-seq #"(?m)^  (\w+)\s*:" (second m)))))
    (die "no fields found for" {:structure structure-name})))

(def find-result-fields (fields-of holes "FindResult"))
(def find-receipt-row-fields (fields-of holes "FindReceiptRow"))
(def receipt-fields (fields-of holes "Receipt"))
(def conformant-find-fields (fields-of src "ConformantFind"))

;; --- 4. declarations the slice and its review require -----------------------

(def required
  [[:signature "abbrev FindType"]
   [:predicate "structure ConformantFind"]
   [:f4-separate "def FindFalsifiable"]
   [:repository-carrier "def findSnatchRepository"]
   [:selected-column "def findSnatchSelected"]
   [:receipted-column "def findSnatchReceipted"]
   [:receipted-bridge "findSnatchSelectedSubsetReceipted"]
   [:structured-receipt "def findStructuredReceipt"]
   [:witness-replay "def findSnatchReplay"]
   [:witness-replay-conformant "findSnatchReplayConformant"]
   [:witness-replay-receipts-partial "findSnatchReplayReceiptsArePartial"]
   [:witness-refusing "def findRefusing"]
   [:witness-refusing-conformant "findRefusingConformant"]
   [:witness-refusing-falsifiable "findRefusingFalsifiable"]
   [:witness-identity "def findIdentity"]
   [:witness-identity-conformant "findIdentityConformant"]
   [:measurement-identity-unfalsifiable "findIdentityNotFalsifiable"]
   [:measurement-declared-zero-mass "findSnatchReplayExcludesDeclaredZeroMass"]
   [:zero-mass-nonempty "findSnatchZeroMassNonempty"]
   [:f4-on-recorded-repository "findSnatchReplayFalsifiableOnRecordedRepository"]
   [:measurement-forall-reading-refuted "findSnatchReplayNotFalsifiable"]
   [:non-vacuity "findSnatchReplaySelectsNamedPattern"]
   [:swappability "findConformantImplementationsDifferOnSnatch"]])

(def declarations
  ;; `(some? false)` is TRUE, so the obvious spelling would record every
  ;; declaration as present whatever the source says (the F12 slice-5 plant).
  (into (sorted-map) (for [[k needle] required] [k (str/includes? src needle)])))

;; --- code, with the prose removed -------------------------------------------
;; Read over CODE, not docstrings: an accurate sentence about a `sorry` or about
;; a pattern the file does not use must not fail the check, or the next slice
;; learns to soften its prose.
(def code-only
  (-> src
      (str/replace #"(?s)/--.*?-/" " ")
      (str/replace #"(?s)/-!.*?-/" " ")))

(def sorry-free?
  (not (re-find #"(?m)^\s*sorry\b|[^A-Za-z]sorry\b|\badmit\b|\bsorryAx\b|\bnative_decide\b|(?m)^axiom\b"
                code-only)))

(def reads-the-fixture-defs
  (into (sorted-map)
        (for [d ["findSnatchScenarios" "snatchRepository" "findSnatchZeroMass"]]
          [(keyword d) (str/includes? code-only d)])))

;; --- 5. the record, recomputed ----------------------------------------------

(def repository (vec (sort (map camel (:repository pin)))))

(def scenario-facts
  (vec (for [s (sort-by (juxt :treatment :disposition) (:scenarios pin))
             :let [sel (set (map camel (:selected-union s)))
                   receipted (set (map camel (mapcat #(keys (get-in % [:find :receipts]))
                                                     (:round-results s))))
                   zm (camel (get-in s [:f4 :zero-mass-pattern]))]]
         (sorted-map
          :scenario (str (name (:treatment s)) "/" (name (:disposition s)))
          :selected (vec (sort sel))
          :receipted (vec (sort receipted))
          :zero-mass zm
          ;; what findSnatchReplayExcludesDeclaredZeroMass claims, per scenario
          :zero-mass-in-repository? (contains? (set repository) zm)
          :zero-mass-selected? (contains? sel zm)
          ;; what findSnatchSelectedSubsetReceipted claims, per scenario
          :selected-minus-receipted (vec (sort (remove receipted sel)))
          ;; what findSnatchReplayReceiptsArePartial needs: some repository
          ;; member with no receipt
          :repository-without-receipt (vec (sort (remove receipted repository)))))))

(def g1-snatcher (first (filter #(= "g1/snatcher" (:scenario %)) scenario-facts)))

;; The pattern constructors the CODE names must all be recorded patterns; a
;; hand-typed or invented name is exactly what this catches.
(def named-snatch-constructors
  (vec (sort (distinct (map second (re-seq #"SnatchPattern\.(\w+)" code-only))))))
(def unrecorded-patterns (vec (remove (set repository) named-snatch-constructors)))

;; --- pin vs live, reported and never gated ----------------------------------

(def pin-vs-live
  (sorted-map
   :pin-as-of (:as-of pin)
   :live-as-of (:as-of live)
   :pin-repository-count (count (:repository pin))
   :live-repository-count (count (:repository live))
   :live-only-patterns (vec (sort (map camel (remove (set (:repository pin))
                                                     (:repository live)))))
   :selected-unions-agree?
   (= (mapv #(vec (sort (map camel (:selected-union %))))
            (sort-by (juxt :treatment :disposition) (:scenarios pin)))
      (mapv #(vec (sort (map camel (:selected-union %))))
            (sort-by (juxt :treatment :disposition) (:scenarios live))))
   :zero-mass-agree?
   (= (mapv #(camel (get-in % [:f4 :zero-mass-pattern]))
            (sort-by (juxt :treatment :disposition) (:scenarios pin)))
      (mapv #(camel (get-in % [:f4 :zero-mass-pattern]))
            (sort-by (juxt :treatment :disposition) (:scenarios live))))
   :note "C559 s3: the pin and the live re-run differ in receipt line coordinates only; the repository grew 18 -> 24.  The Lean transcription is at the pin, so the pin is what gates here."))

;; --- verdict ----------------------------------------------------------------

(def problems
  (cond-> []
    (not= find-type-in-slice find-type-in-holes)
    (conj [:signature-drift find-type-in-slice find-type-in-holes])

    (some #{"zeroMass"} find-result-fields)
    (conj [:findresult-has-a-zeromass-field find-result-fields])

    (not (some #{"zeroMass"} find-receipt-row-fields))
    (conj [:findreceiptrow-lost-its-zeromass-field find-receipt-row-fields])

    (not= ["citesTextOrEdges" "scoreAlone"] receipt-fields)
    (conj [:receipt-fields-moved receipt-fields])

    (not= ["f1Containment" "f1TypedAbsence" "f2Receipted" "f3NonSelfCertifying"]
          conformant-find-fields)
    (conj [:conformant-find-fields-moved conformant-find-fields])

    (re-find #"zeroMass" (or (second (re-find #"(?s)structure ConformantFind[^\n]*where\n(.*?)\n\n" src)) ""))
    (conj [:conformant-find-states-f4-after-all])

    (seq unrecorded-patterns)
    (conj (into [:pattern-name-not-in-the-record] unrecorded-patterns))

    (seq (for [[k present?] reads-the-fixture-defs :when (not present?)] k))
    (conj (into [:does-not-read-the-fixture]
                (for [[k present?] reads-the-fixture-defs :when (not present?)] k)))

    (seq (remove :zero-mass-in-repository? scenario-facts))
    (conj (into [:zero-mass-outside-repository]
                (map :scenario (remove :zero-mass-in-repository? scenario-facts))))

    (seq (filter :zero-mass-selected? scenario-facts))
    (conj (into [:zero-mass-was-selected]
                (map :scenario (filter :zero-mass-selected? scenario-facts))))

    (seq (remove #(empty? (:selected-minus-receipted %)) scenario-facts))
    (conj (into [:selected-not-receipted]
                (map :scenario (remove #(empty? (:selected-minus-receipted %)) scenario-facts))))

    (seq (remove #(seq (:repository-without-receipt %)) scenario-facts))
    (conj (into [:every-repository-member-receipted]
                (map :scenario (remove #(seq (:repository-without-receipt %)) scenario-facts))))

    (not (contains? (set (:selected g1-snatcher)) "askForSurplusNotSurrender"))
    (conj [:non-vacuity-witness-not-selected (:selected g1-snatcher)])

    (not (contains? (set (:repository-without-receipt g1-snatcher))
                    "consultTheRemedyBeforeExiting"))
    (conj [:partiality-witness-has-a-receipt (:repository-without-receipt g1-snatcher)])

    (not sorry-free?) (conj [:sorry-or-axiom-in-conformance-file])

    (seq (for [[k present?] declarations :when (not present?)] k))
    (conj (into [:missing-declarations]
                (for [[k present?] declarations :when (not present?)] k)))))

(def result
  (sorted-map
   :basis (sorted-map :conformance-file conf-path
                      :holes holes-path
                      :pinned-fixture pin-path
                      :live-rerun live-path
                      :pin-as-of (:as-of pin))
   :conformant-find-fields conformant-find-fields
   :declarations declarations
   :find-result-fields find-result-fields
   :find-receipt-row-fields find-receipt-row-fields
   :find-type (sorted-map :in-holes find-type-in-holes :in-slice find-type-in-slice)
   :named-snatch-constructors named-snatch-constructors
   :pin-vs-live pin-vs-live
   :problems problems
   :reads-the-fixture-defs reads-the-fixture-defs
   :receipt-fields receipt-fields
   :repository repository
   :scenarios scenario-facts))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint result)))
(println "f11-conformance: wrote" out-path)
(if (seq problems)
  (do (binding [*out* *err*] (println "f11-conformance: FAIL" (pr-str problems))) (System/exit 1))
  (println "f11-conformance: PASS -- signature, the zeroMass and Receipt field claims, the declaration inventory, and every scenario fact the theorems name agree with their sources"))
