#!/usr/bin/env bb
(require '[clojure.edn :as edn] '[clojure.string :as str]
         '[clojure.java.shell :as sh] '[clojure.pprint :as pp])

(def root "/home/joe/code")
(def lean-path (str root "/mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean"))
(def ruled-path (str root "/mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean"))
(def record-path (str root "/futon3/checks/ants-cascade.edn"))
(def out-path (str root "/futon2/holes/labs/wm-contract/runs/F12-organise/18-ants-exemplar.edn"))
(def holes-pin "61c4825dc3e373fd1b761b800814bf85f5770b88")
(def clauses #{"osel" "oauth" "oattr" "o1" "o2" "o3" "o4"})

(defn lean-list [s n]
  (->> (second (re-find (re-pattern (str "(?s)def\\s+" n ".*?:=\\s*\\[([^]]*)\\]")) s))
       (re-seq #"\d+") (mapv parse-long)))
(defn spans-in [s]
  (set (map second (re-seq #"futon3:checks/ants-cascade\.edn:(\d+(?:-\d+)?)" s))))
;; Per-DECLARATION, not per-file: a set of the file's spans is permutation-blind,
;; so exchanging the `:precedence-before` and `:precedence-after` citations
;; between their two docstrings left the set equal and the verdict true.  That is
;; the very defect this check exists to catch (the owning-lane review of part 1
;; found all seven record citations attached to the wrong values), so each span
;; is now bound to the declaration whose docstring carries it.
(defn pointer-map [s]
  (into (let [h (second (re-find #"(?s)/-!(.*?)-/" s))]
          (if h {"module-header" (spans-in h)} {}))
        (keep (fn [[_ doc nm]]
                (let [sp (spans-in doc)] (when (seq sp) [nm sp]))))
        (re-seq #"(?s)/--(.*?)-/\s*(?:noncomputable\s+)?(?:def|theorem)\s+([A-Za-z0-9'\u00c0-\uffff]+)" s)))
(defn index-map [s]
  (into {} (map (fn [[_ i n]] [(keyword "ants" n) (parse-long i)]))
        (re-seq #"`([0-4])`\s+([a-z-]+)" s)))
(defn ranked-order [m] (->> m (sort-by val) (mapv key)))
(defn line-containing [lines needle]
  (some (fn [[i line]] (when (str/includes? line needle) (inc i)))
        (map-indexed vector lines)))
(defn field-span [lines needle entry-count]
  (let [start (line-containing lines needle)]
    (str start "-" (+ start entry-count))))
(defn record-pointer-spans [record-text record]
  (let [lines (str/split-lines record-text)
        row (get-in record [:o4 :row])
        score-lines [(line-containing lines ":score-after")
                     (line-containing lines ":score-before")]
        selected (field-span lines ":selected" (count (get-in record [:find :selected])))
        admitted (str (line-containing lines ":admitted"))]
    {"module-header" #{selected admitted}
     "antsSelected" #{selected}
     "antsAdmitted" #{admitted}
     "antsRepo" #{(str (line-containing lines ":authored-why-edges"))}
     "antsPrecedenceBefore" #{(field-span lines ":precedence-before" (count (:precedence-before row)))}
     "antsPrecedenceAfter" #{(field-span lines ":precedence-after" (count (:precedence-after row)))}
     "antsActingOrderBefore" #{(field-span lines ":acting-order-before" (count (:acting-order-before row)))}
     "antsActingOrderAfter" #{(field-span lines ":acting-order-after" (count (:acting-order-after row)))}
     "antsScore" #{(str (apply min score-lines) "-" (apply max score-lines))}}))
(defn mutate [s old new]
  (let [r (str/replace-first s old new)]
    {:source r :landed? (and (not (str/includes? r old)) (str/includes? r new))}))

(defn facts [lean]
  (let [record-text (slurp record-path) record (edn/read-string record-text)
        idx (index-map lean) row (get-in record [:o4 :row])
        mapped {:precedence-before (mapv idx (ranked-order (:precedence-before row)))
                :precedence-after (mapv idx (ranked-order (:precedence-after row)))
                :acting-before (mapv idx (:acting-order-before row))
                :acting-after (mapv idx (:acting-order-after row))}
        parsed {:precedence-before (lean-list lean "antsPrecedenceBefore")
                :precedence-after (lean-list lean "antsPrecedenceAfter")
                :acting-before (lean-list lean "antsActingOrderBefore")
                :acting-after (lean-list lean "antsActingOrderAfter")}
        score-digits (map second (re-seq #":score-(?:before|after)\s+([0-9.]+)" record-text))
        rat (re-find #"def antsScore : Rat := (\d+) / (\d+)" lean)
        expected-pointers (record-pointer-spans record-text record)
        forbidden {:sorry (count (re-seq #"\bsorry\b" lean))
                   :axiom (count (re-seq #"(?m)^[ \t]*axiom\b" lean))
                   :native-decide (count (re-seq #"\bnative_decide\b" lean))}
        clause-use (set (map second (re-seq #"(?m)^\s{2}(o(?:sel|auth|attr|[1-4]))\s*:=" lean)))]
    {:record-assertions
     {:o4-holds (= true (get-in record [:o4 :holds?]))
      :precedence-changed (= true (get-in record [:o4 :precedence-changed?]))
      :score-unchanged (= false (get-in record [:o4 :score-changed?]))
      :authored-why-edges-zero (= 0 (get-in record [:as-of :authored-why-edges]))}
     :pointer-spans {:actual (into (sorted-map) (pointer-map lean))
                     :expected (into (sorted-map) expected-pointers)
                     :match? (= expected-pointers (pointer-map lean))}
     :index-map idx :orders {:record-through-map mapped :lean parsed :match? (= mapped parsed)}
     :scores {:record score-digits :rat-numerator (nth rat 1 nil)
              :rat-denominator (nth rat 2 nil)
              :match? (and (= 2 (count score-digits)) (apply = score-digits)
                           (= (str/replace (first score-digits) "." "") (nth rat 1 nil))
                           (= "1000000000000000" (nth rat 2 nil)))}
     :conformance {:clause-use clause-use :unchanged? (= clauses clause-use)
                   :new-structure-count (count (re-seq #"(?m)^structure Conformant" lean))
                   :reuses-sans-o4? (str/includes? lean "ConformantOrganiseRuledSansO4")}
     :forbidden forbidden}))

(defn verdict [lean]
  (let [f (facts lean)]
    (and (every? true? (vals (:record-assertions f))) (get-in f [:pointer-spans :match?])
         (get-in f [:orders :match?]) (get-in f [:scores :match?])
         (get-in f [:conformance :unchanged?])
         (zero? (get-in f [:conformance :new-structure-count]))
         (get-in f [:conformance :reuses-sans-o4?])
         (every? zero? (vals (:forbidden f))))))

(let [lean (slurp lean-path) ruled (slurp ruled-path)
      p1 (mutate lean "def antsPrecedenceAfter : List Nat := [0, 1, 3, 2]"
                 "def antsPrecedenceAfter : List Nat := [0, 1, 2, 3]")
      p2 (mutate lean "def antsActingOrderAfter : List Nat := [0, 1, 3, 2]"
                 "def antsActingOrderAfter : List Nat := [0, 1, 2, 3]")
      p3 {:source (str lean "\ntheorem plant : True := by sorry\n") :landed? true}
      p4 (mutate lean "futon3:checks/ants-cascade.edn:98-102"
                 "futon3:checks/ants-cascade.edn:95-99")
      ;; Exchanges the two precedence citations between their docstrings: every
      ;; span still occurs in the file, so a set-of-spans check cannot see it.
      swapped (-> lean
                  (str/replace "Recorded precedence before, `futon3:checks/ants-cascade.edn:98-102`"
                               "Recorded precedence before, `futon3:checks/ants-cascade.edn:<<SWAP>>`")
                  (str/replace "Recorded precedence after, `futon3:checks/ants-cascade.edn:93-97`"
                               "Recorded precedence after, `futon3:checks/ants-cascade.edn:98-102`")
                  (str/replace "<<SWAP>>" "93-97"))
      p5 {:source swapped
          :landed? (and (not= swapped lean)
                        (= (frequencies (re-seq #"futon3:checks/ants-cascade\.edn:\d+(?:-\d+)?" swapped))
                           (frequencies (re-seq #"futon3:checks/ants-cascade\.edn:\d+(?:-\d+)?" lean))))}
      plants (mapv (fn [[n p]] {:plant n :landed? (:landed? p)
                                :verdict (verdict (:source p))})
                   [[:flat-precedence p1] [:flat-acting p2] [:sorry p3] [:shift-pointer p4]
                    [:swap-pointer p5]])
      holes (sh/sh "git" "-C" (str root "/mathlib4") "log" "-1" "--format=%H" "--"
                   "DarkTower/WarMachine/Holes.lean")
      f (facts lean)
      report (sorted-map :check :F12-ants-exemplar :verdict (verdict lean)
               :record-assertions (:record-assertions f) :pointer-spans (:pointer-spans f)
               :index-map (:index-map f) :orders (:orders f) :scores (:scores f)
               :conformance (:conformance f) :forbidden (:forbidden f)
               :ruled-clause-source-present? (str/includes? ruled "structure ConformantOrganiseRuled")
               :holes-lean-head (str/trim (:out holes)) :plants plants)]
  (when-not (and (:verdict report) (= holes-pin (:holes-lean-head report))
                 (every? #(and (:landed? %) (false? (:verdict %))) plants))
    (binding [*out* *err*] (pp/pprint report)) (System/exit 1))
  (.mkdirs (.getParentFile (java.io.File. out-path)))
  (spit out-path (with-out-str (pp/pprint report))))
