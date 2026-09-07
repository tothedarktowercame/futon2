#!/usr/bin/env bb
(require '[clojure.edn :as edn] '[clojure.string :as str]
         '[clojure.java.shell :as sh] '[clojure.pprint :as pp])

(def root "/home/joe/code")
(def lean-path (str root "/mathlib4/DarkTower/WarMachine/F12MiningExemplar.lean"))
(def ruled-path (str root "/mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean"))
(def record-path (str root "/futon2/holes/labs/library-loop/runs/mining-exemplar/cascade.edn"))
(def out-path (str root "/futon2/holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn"))
(def futon3-pin "cdb5e8a56fd907beb6a99f8b88af9de50ff93126")
(def holes-pin "61c4825dc3e373fd1b761b800814bf85f5770b88")
(def clauses #{"osel" "oauth" "oattr" "o1" "o2" "o3" "o4"})
(def record-pointer-re #"futon2:holes/labs/library-loop/runs/mining-exemplar/cascade\.edn:(\d+(?:-\d+)?)")

(defn spans-in [s] (set (map second (re-seq record-pointer-re s))))
(defn pointer-map [s]
  (into (let [h (second (re-find #"(?s)/-!(.*?)-/" s))]
          (if h {"module-header" (spans-in h)} {}))
        (keep (fn [[_ doc nm]]
                (let [sp (spans-in doc)] (when (seq sp) [nm sp]))))
        (re-seq #"(?s)/--(.*?)-/\s*(?:noncomputable\s+)?(?:def|theorem)\s+([A-Za-z0-9'\u00c0-\uffff]+)" s)))
(defn line-containing [lines needle]
  (some (fn [[i line]] (when (str/includes? line needle) (inc i)))
        (map-indexed vector lines)))
(defn top-field-line [lines field]
  (some (fn [[i line]]
          (when (re-find (re-pattern (str "^ :" field "(?:\\s|$)")) line) (inc i)))
        (map-indexed vector lines)))
(defn span [a b] (if (= a b) (str a) (str a "-" b)))
(defn expected-pointers [text]
  (let [ls (vec (str/split-lines text))]
    {"module-header" #{(span (top-field-line ls "nodes")
                              (dec (top-field-line ls "selected")))}
     "miningSelected" #{(span (top-field-line ls "selected")
                              (dec (top-field-line ls "admitted")))}
     "miningAdmitted" #{(span (top-field-line ls "admitted")
                              (dec (top-field-line ls "edges")))}
     "miningRepo" #{(span (inc (top-field-line ls "edges"))
                          (dec (top-field-line ls "rules")))}
     "miningPrecedence" #{(span (top-field-line ls "precedence")
                                (dec (top-field-line ls "o4")))}
     "miningActingOrder" #{(span (line-containing ls ":acting-order")
                                 (dec (line-containing ls ":basis")))
                            (span (top-field-line ls "o4") (count ls))}}))

(defn index-map [s]
  (let [header (second (re-find #"(?s)/-!(.*?)-/" s))]
    (into {} (map (fn [[_ i n]] [(keyword "aif" n) (parse-long i)]))
          (re-seq #"`([0-2])`\s+([a-z-]+)" header))))
(defn lean-list [s n]
  (->> (second (re-find (re-pattern (str "(?s)def\\s+" n ".*?:=\\s*\\[([^]]*)\\]")) s))
       (re-seq #"\d+") (mapv parse-long)))
(defn lean-set [s n]
  (->> (second (re-find (re-pattern (str "(?s)def\\s+" n ".*?:=\\s*\\{([^}]*)\\}")) s))
       (re-seq #"\d+") (mapv parse-long) set))
(defn ranked [m] (->> m (sort-by val) (mapv key)))
(defn record-edges [record idx]
  (mapv (fn [{:keys [from to]}] [(idx from) (idx to)]) (:edges record)))
(defn lean-edges [s]
  (mapv (fn [[_ u v]] [(parse-long u) (parse-long v)])
        (re-seq #"\(u = (\d+) ∧ v = (\d+)\)" (second (re-find #"(?s)def miningRepo.*?standsOn := fun u v => (.*?)\n  acyclic" s)))))
(defn score-keys [x]
  (cond (map? x) (concat (filter #(and (nil? (namespace %))
                                      (str/starts-with? (name %) "score")) (keys x))
                         (mapcat score-keys (vals x)))
        (coll? x) (mapcat score-keys x)
        :else []))

(defn git-blob [path]
  (let [r (sh/sh "git" "-C" (str root "/futon3") "show" (str futon3-pin ":" path))]
    {:exit (:exit r) :text (:out r)}))
(defn block-data [lines]
  (let [then-line (first (keep-indexed #(when (re-find #"^\s*\+ THEN:" %2) (inc %1)) lines))
        next-block (or (first (keep-indexed #(when (and then-line (> (inc %1) then-line)
                                                        (re-find #"^\s*\+ [A-Z-]+:" %2))
                                               (inc %1)) lines))
                       (inc (count lines)))
        last-content (last (filter #(not (str/blank? (nth lines (dec %))))
                                   (range then-line next-block)))]
    {:then-line then-line :next-block next-block :last-content last-content}))
(defn then-correspondence [record]
  (mapv (fn [{:keys [id member then-source]}]
          (let [[path range] (str/split then-source #":")
                [from to] (mapv parse-long (str/split range #"-"))
                {:keys [exit text]} (git-blob path)
                lines (vec (str/split-lines text))
                {:keys [then-line next-block last-content]} (block-data lines)
                expected (keyword (str/replace path #"^library/|\.flexiarg$" ""))]
            (sorted-map
             :rule id :member member :then-source then-source
             :file-exists? (zero? exit)
             :pattern-id-matches-path? (= member expected)
             :then-block [then-line last-content]
             :span-inside-zaif-strict?
             (boolean (and then-line next-block (> from then-line) (< to next-block)))
             :span-inside-marker-inclusive?
             (boolean (and then-line next-block (>= from then-line) (< to next-block)))
             :span-is-exactly-the-then-block? (= [from to] [then-line last-content])
             :span-is-non-empty?
             (boolean (and (zero? exit) (<= 1 from to (count lines))
                           (seq (str/trim (str/join " " (subvec lines (dec from) to)))))))))
        (:rules record)))

(defn directive-data [record]
  (let [members (set (:selected record))
        rows (mapv (fn [{:keys [pattern library-path]}]
                     (let [{:keys [exit text]} (git-blob library-path)
                           directives (mapv (fn [[_ rel rest]]
                                              (let [tokens (str/split (str/trim rest) #"\s+")]
                                                {:relation (keyword rel)
                                                 :targets (if (= rel "see-also") tokens
                                                              [(first tokens)])}))
                                            (re-seq #"(?m)^@(why|how|see-also)\s+([^\n]+)" text))]
                       {:member pattern :file-exists? (zero? exit)
                        :why-how-targets (vec (mapcat :targets (filter #(#{:why :how} (:relation %)) directives)))
                        :see-also-targets (vec (mapcat :targets (filter #(= :see-also (:relation %)) directives)))}))
                   (:nodes record))
        internal (set (map #(subs (str %) 1) members))]
    {:patterns rows
     :why-how-internal-targets (vec (sort (filter internal (mapcat :why-how-targets rows))))
     :no-library-edge-among-members? (empty? (filter internal (mapcat :why-how-targets rows)))}))

(defn conformance [lean ruled]
  (let [block (second (re-find #"(?s)theorem organiseMiningConformant.*?where(.*?)(?=\n/--)" lean))
        used (set (map second (re-seq #"(?m)^\s{2}(o(?:sel|auth|attr|[1-4]))\s*:=" block)))]
    {:ruled-clause-set-used used :all-seven-used? (= clauses used)
     :new-conformant-structure-count (count (re-seq #"(?m)^structure Conformant" lean))
     :uses-sans-o3? (str/includes? lean "ConformantOrganiseRuledSansO3")
     :uses-sans-o4? (str/includes? lean "ConformantOrganiseRuledSansO4")
     :sans-o3-defined-in-ruled? (str/includes? ruled "structure ConformantOrganiseRuledSansO3")
     :sans-o4-defined-in-ruled? (str/includes? ruled "structure ConformantOrganiseRuledSansO4")}))

(defn facts [lean record-text]
  (let [record (edn/read-string record-text) idx (index-map lean)
        actual-pointers (pointer-map lean) expected (expected-pointers record-text)
        tc (then-correspondence record) lib (directive-data record)
        ruled (slurp ruled-path)]
    {:transcription
     {:index-map idx
      :selected {:record (set (map idx (:selected record))) :lean (lean-set lean "miningSelected")}
      :admitted {:record (set (map idx (:admitted record)))
                 :lean-empty? (boolean (re-find #"def\s+miningAdmitted.*?:=\s*∅" lean))}
      :edges {:record (record-edges record idx) :lean (lean-edges lean)}
      :precedence {:record (mapv idx (ranked (:precedence record)))
                   :lean (lean-list lean "miningPrecedence")}
      :acting-order {:record (mapv idx (get-in record [:o4 :acting-order]))
                     :lean (lean-list lean "miningActingOrder")}
      :score-keys-found (vec (score-keys record))
      :score-type-unit? (str/includes? lean "organiseMining : RuledOrganiseType Unit Nat Unit")}
     :pointer-bindings {:actual (into (sorted-map) actual-pointers)
                        :expected (into (sorted-map) expected)
                        :match? (= actual-pointers expected)}
     :then-correspondence tc
     :library-edge-absence lib
     :conformance (conformance lean ruled)
     :forbidden {:sorry (count (re-seq #"\bsorry\b" lean))
                 :axiom (count (re-seq #"(?m)^[ \t]*axiom\b" lean))
                 :native-decide (count (re-seq #"\bnative_decide\b" lean))}}))

(defn verdict [lean record-text]
  (let [f (facts lean record-text) t (:transcription f) c (:conformance f)]
    (and (= (get-in t [:selected :record]) (get-in t [:selected :lean]))
         (empty? (get-in t [:admitted :record])) (get-in t [:admitted :lean-empty?])
         (= (get-in t [:edges :record]) (get-in t [:edges :lean]))
         (= (get-in t [:precedence :record]) (get-in t [:precedence :lean]))
         (= (get-in t [:acting-order :record]) (get-in t [:acting-order :lean]))
         (empty? (:score-keys-found t)) (:score-type-unit? t)
         (get-in f [:pointer-bindings :match?])
         (every? #(and (:file-exists? %) (:pattern-id-matches-path? %)
                       (:span-inside-marker-inclusive? %) (:span-is-exactly-the-then-block? %)
                       (:span-is-non-empty? %)) (:then-correspondence f))
         (get-in f [:library-edge-absence :no-library-edge-among-members?])
         (:all-seven-used? c) (zero? (:new-conformant-structure-count c))
         (:uses-sans-o3? c) (:uses-sans-o4? c)
         (:sans-o3-defined-in-ruled? c) (:sans-o4-defined-in-ruled? c)
         (every? zero? (vals (:forbidden f))))))

(defn mutate [s old new]
  (let [r (str/replace-first s old new)]
    {:source r :landed? (and (not= r s) (str/includes? r new))}))

(let [lean (slurp lean-path) record-text (slurp record-path)
      p1 (mutate lean "(u = 0 ∧ v = 1)" "(u = 1 ∧ v = 0)")
      p2 (mutate lean "def miningPrecedence : List Nat := [0, 1, 2]"
                 "def miningPrecedence : List Nat := [0, 2, 1]")
      swapped (-> lean
                  (str/replace "cascade.edn:26-29" "cascade.edn:<<SWAP>>")
                  (str/replace "cascade.edn:30-32" "cascade.edn:26-29")
                  (str/replace "<<SWAP>>" "30-32"))
      p3 {:source swapped
          :landed? (and (not= swapped lean)
                        (= (frequencies (re-seq record-pointer-re lean))
                           (frequencies (re-seq record-pointer-re swapped))))}
      p4 (mutate lean "cascade.edn:34-43" "cascade.edn:35-43")
      p5 {:source (str lean "\ntheorem plant : True := by sorry\n") :landed? true}
      p6 (mutate record-text
                 "library/aif/candidate-pattern-action-space.flexiarg:26-27"
                 "library/aif/candidate-pattern-action-space.flexiarg:25-27")
      plants (mapv (fn [[n kind p]]
                     {:plant n :mutation-target kind :landed? (:landed? p)
                      :citation-multiset-unchanged?
                      (when (= n :exchange-declaration-citations) (:landed? p))
                      :verdict (if (= kind :record)
                                 (verdict lean (:source p))
                                 (verdict (:source p) record-text))})
                   [[:reverse-recorded-edge :lean p1]
                    [:permute-transcribed-precedence :lean p2]
                    [:exchange-declaration-citations :lean p3]
                    [:shift-declaration-citation :lean p4]
                    [:append-sorry :lean p5]
                    [:then-span-outside-block :record p6]])
      holes (sh/sh "git" "-C" (str root "/mathlib4") "log" "-1" "--format=%H" "--"
                   "DarkTower/WarMachine/Holes.lean")
      f (facts lean record-text)
      report (into (sorted-map)
                   {:check :F12-mining-exemplar :verdict (verdict lean record-text)
                    :source-pins (get (edn/read-string record-text) :source-pins)
                    :transcription (:transcription f)
                    :pointer-bindings (:pointer-bindings f)
                    :then-correspondence (:then-correspondence f)
                    :library-edge-absence (:library-edge-absence f)
                    :conformance (:conformance f) :forbidden (:forbidden f)
                    :holes-lean-head (str/trim (:out holes)) :plants plants})]
  (when-not (and (:verdict report) (= holes-pin (:holes-lean-head report))
                 (every? #(and (:landed? %) (false? (:verdict %))) plants))
    (binding [*out* *err*] (pp/pprint report)) (System/exit 1))
  (.mkdirs (.getParentFile (java.io.File. out-path)))
  (spit out-path (with-out-str (pp/pprint report))))
