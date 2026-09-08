#!/usr/bin/env bb
(require '[clojure.edn :as edn] '[clojure.string :as str]
         '[clojure.java.shell :as sh] '[clojure.pprint :as pp])

(def root "/home/joe/code")
(def futon3 (str root "/futon3"))
(def mathlib (str root "/mathlib4"))
(def lean-path (str mathlib "/DarkTower/WarMachine/F12SnatchExemplar.lean"))
(def ruled-path (str mathlib "/DarkTower/WarMachine/F12RuledCarrier.lean"))
(def fixture-path (str futon3 "/checks/snatch-cascade.edn"))
(def out-path (str root "/futon2/holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn"))
(def futon3-pin "cdb5e8a56fd907beb6a99f8b88af9de50ff93126")
(def holes-pin "61c4825dc3e373fd1b761b800814bf85f5770b88")
(def clauses #{"osel" "oauth" "oattr" "o1" "o2" "o3" "o4"})
(def pointer-re #"futon3:checks/snatch-cascade\.edn:(\d+(?:-\d+)?)")

(defn stable [x]
  (cond (set? x) (vec (sort-by pr-str (map stable x)))
        (map? x) (into (sorted-map) (map (fn [[k v]] [k (stable v)]) x))
        (sequential? x) (mapv stable x) :else x))
(defn write-temp [fx]
  (let [f (java.io.File/createTempFile "f12-snatch-exemplar" ".edn")]
    (.deleteOnExit f) (spit f (pr-str fx)) (.getAbsolutePath f)))
(defn expression [fixture drop-edge?]
  (str "(load-file \"checks/find_organise.clj\") "
       "(require '[clojure.edn :as edn] '[clojure.set :as set]) "
       "(let [n (find-ns 'find-organise) r #(deref (ns-resolve n %)) "
       "fixture (edn/read-string (slurp \"" fixture "\")) "
       "repo ((r 'read-repository) \"library\" [:snatch]) "
       "row (first (filter #(= [:g4 :snatcher] (:scenario %)) ((r 'cascade-diff-table) fixture repo))) "
       "stands (:stands-on row) stands (if " drop-edge?
       " (update stands :snatch/protect-the-unprotected-move disj :snatch/institutions-vary-by-position-and-force) stands) "
       "ff (r 'fast-forward) laws (r 'organise-laws) repro (r 'organise-reproduces-record?)] "
       "(prn {:row (select-keys row [:selected :added-by-organise :admitted-by :nodes :edges :stands-on :precedence-before :precedence-after :acting-order-before :acting-order-after :score-before :score-after]) "
       ":patterns (:patterns repo) :laws (into {} (map (fn [[k f]] [k (boolean (f row))]) laws)) "
       ":reproduces-record? (boolean (repro row)) :full (ff (:nodes row) stands) "
       ":no-bootstrap (ff (set/difference (:nodes row) (:added-by-organise row)) stands)}))"))
(defn derive-data [fixture & [drop-edge?]]
  (let [p (sh/sh "bb" "-cp" "checks" "-e" (expression fixture (boolean drop-edge?)) :dir futon3)]
    (if (zero? (:exit p)) (edn/read-string (:out p))
        {:subprocess-error {:exit (:exit p) :err (:err p)}})))

(defn spans-in [s] (set (map second (re-seq pointer-re s))))
(defn pointer-map [s]
  (into (sorted-map "module-header" (spans-in (or (second (re-find #"(?s)/-!(.*?)-/" s)) "")))
        (keep (fn [[_ doc nm]] (let [sp (spans-in doc)] (when (seq sp) [nm sp]))))
        (re-seq #"(?s)/--(.*?)-/\s*(?:noncomputable\s+)?(?:def|theorem)\s+([A-Za-z0-9'À-￿]+)" s)))
(defn line-containing [ls needle from]
  (some (fn [i] (when (str/includes? (nth ls i) needle) (inc i))) (range (or from 0) (count ls))))
(defn block [ls treatment disposition policy]
  (let [anchors (for [i (range (count ls))
                      :when (str/includes? (nth ls i) (str ":treatment " treatment))
                      :let [lo (last (filter #(str/starts-with? (nth ls %) "  {") (range (inc i))))
                            hi (inc (first (filter #(str/ends-with? (nth ls %) "}") (range i (count ls)))))
                            txt (str/join "\n" (subvec ls lo hi))]]
                  [lo hi txt])]
    (first (filter (fn [[_ _ txt]] (and (str/includes? txt (str ":disposition " disposition))
                                        (str/includes? txt (str ":policy " policy)))) anchors))))
(defn field-span [ls [lo hi _] field]
  (let [a (line-containing ls (str ":" field) lo)
        b (or (some (fn [i] (when (and (> (inc i) a) (re-find #"^   :[a-z]" (nth ls i))) (inc i)))
                    (range lo hi)) (inc hi))]
    (str a (when (< a (dec b)) (str "-" (dec b))))))
(defn expected-pointers [fixture-text]
  (let [ls (vec (str/split-lines fixture-text)) p (block ls ":g4" ":snatcher" ":patterns")
        e (block ls ":g4" ":snatcher" ":exchange-first")]
    (sorted-map
     "module-header" #{}
     "snatchAdded" #{(field-span ls p "added-by-organise")}
     "snatchSelected" (set [(field-span ls p "acting") (field-span ls p "nodes")])
     "snatchActingOrderBefore" #{(field-span ls p "acting")}
     "snatchScoreBefore" #{(field-span ls p "score")}
     "snatchPrecedenceBefore" #{(field-span ls p "precedence")}
     "snatchActingOrderAfter" #{(field-span ls e "acting")}
     "snatchScoreAfter" #{(field-span ls e "score")}
     "snatchPrecedenceAfter" #{(field-span ls e "precedence")})))
(defn index-map [lean]
  (let [h (second (re-find #"(?s)/-!(.*?)-/" lean))]
    (into {} (map (fn [[_ i nm]] [(keyword "snatch" nm) (parse-long i)]))
          (re-seq #"(?m)(?:^|;\s)(\d+)\s+([a-z][a-z-]+)(?=;|\.)" h))))
(defn lean-body [lean nm]
  (second (re-find (re-pattern (str "(?s)def\\s+" nm ".*?:=\\s*(.*?)(?=\\n\\n/--|\\n\\ntheorem|\\n#print)")) lean)))
(defn nums [s] (mapv parse-long (re-seq #"\d+" (or s ""))))
(defn lean-set [lean nm] (set (nums (second (re-find #"\{([^}]*)\}" (lean-body lean nm))))))
(defn lean-list [lean nm] (nums (second (re-find #"\[([^]]*)\]" (lean-body lean nm)))))
(defn lean-in-added
  "`snatchInAdded`'s own literal, read from the Lean.  Re-spelling its four
   indices here would leave the declaration the filter route decides through
   unguarded: dropping `22` from it passed an earlier build of this check."
  [lean] (set (map parse-long (map second (re-seq #"n == (\d+)" (or (lean-body lean "snatchInAdded") ""))))))
(defn lean-pairs [s] (set (map (fn [[_ a b]] [(parse-long a) (parse-long b)])
                               (re-seq #"\((\d+),\s*(\d+)\)" (or s "")))))
(defn idx-get [idx k] (or (idx k) (idx (keyword "snatch" (name k)))))
(defn mapped-set [idx xs] (set (map #(idx-get idx %) xs)))
(defn mapped-edges [idx xs] (set (map (fn [[a b]] [(idx-get idx a) (idx-get idx b)]) xs)))
(defn conformance [lean ruled]
  (let [b (second (re-find #"(?s)theorem organiseSnatchConformant.*?where(.*?)(?=\n/--)" lean))
        used (set (map second (re-seq #"(?m)^\s{2}(o(?:sel|auth|attr|[1-4]))\s*:=" b)))]
    {:ruled-clause-names (vec (sort used)) :all-seven-used? (= clauses used)
     :new-conformant-structure-count (count (re-seq #"(?m)^structure Conformant" lean))
     :reuses-sans-o3? (str/includes? lean "ConformantOrganiseRuledSansO3")
     :reuses-sans-o4? (str/includes? lean "ConformantOrganiseRuledSansO4")
     :sans-o3-defined-in-ruled? (str/includes? ruled "structure ConformantOrganiseRuledSansO3")
     :sans-o4-defined-in-ruled? (str/includes? ruled "structure ConformantOrganiseRuledSansO4")}))

(defn facts [lean fixture-text derived]
  (let [idx (index-map lean) row (:row derived) repo-names (set (:patterns derived))
        full (mapped-edges idx (:full derived)) no-boot (mapped-edges idx (:no-bootstrap derived))
        lean-edges (lean-pairs (lean-body lean "snatchOrganisedEdgeList"))
        in-added (lean-in-added lean)
        filtered (set (filter (fn [[a b]] (and (not (in-added a)) (not (in-added b)))) lean-edges))
        actual-p (pointer-map lean) expected-p (expected-pointers fixture-text)
        stands (set (mapcat (fn [[a bs]] (map #(vector (idx-get idx a) (idx-get idx %)) bs)) (:stands-on row)))
        trans {:selected {:derived (mapped-set idx (:selected row)) :lean (lean-set lean "snatchSelected")}
               :added {:derived (mapped-set idx (:added-by-organise row)) :lean (lean-set lean "snatchAdded")}
               :admitted {:derived (mapped-set idx (:admitted-by row)) :lean-empty? (boolean (re-find #"def\s+snatchAdmitted.*?:=\s*∅" lean))}
               :patterns {:derived (mapped-set idx repo-names) :lean-iCC-0-23? (str/includes? lean "patterns := Icc 0 23")}
               :stands-on {:derived stands :lean (lean-pairs (lean-body lean "snatchRepo"))}
               :precedence-before {:derived (mapv #(idx-get idx %) (:precedence-before row)) :lean (lean-list lean "snatchPrecedenceBefore")}
               :precedence-after {:derived (mapv #(idx-get idx %) (:precedence-after row)) :lean (lean-list lean "snatchPrecedenceAfter")}
               :acting-before {:derived (mapv #(idx-get idx %) (:acting-order-before row)) :lean (lean-list lean "snatchActingOrderBefore")}
               :acting-after {:derived (mapv #(idx-get idx %) (:acting-order-after row)) :lean (lean-list lean "snatchActingOrderAfter")}
               :score-before {:derived (:score-before row) :lean (first (map parse-long (re-seq #"-?\d+" (lean-body lean "snatchScoreBefore"))))}
               :score-after {:derived (:score-after row) :lean (first (map parse-long (re-seq #"-?\d+" (lean-body lean "snatchScoreAfter"))))}}
        edges {:row-equals-fast-forward? (= (:edges row) (:full derived))
               :full-node full :lean-list lean-edges :full-equals-lean? (= full lean-edges)
               :lean-list-count (count (re-seq #"\(\d+,\s*\d+\)" (lean-body lean "snatchOrganisedEdgeList")))
               :lean-list-no-duplicates? (= 10 (count lean-edges))
               :no-bootstrap no-boot :lean-filter filtered
               :lean-in-added in-added
               :lean-in-added-matches-added? (= in-added (lean-set lean "snatchAdded"))
               :three-routes-agree? (= no-boot filtered #{[0 2]})}
        ix {:count (count idx) :values (vec (sort (vals idx))) :bijection-0-23? (= (set (range 24)) (set (vals idx)))
            :names-match-repository? (= (set (keys idx)) repo-names)}]
    {:index-map ix :transcription trans :edge-routes edges
     :pointer-bindings {:actual actual-p :expected expected-p :match? (= actual-p expected-p)}
     :laws (stable (:laws derived)) :organise-reproduces-record? (:reproduces-record? derived)
     :conformance (conformance lean (slurp ruled-path))
     :forbidden {:sorry (count (re-seq #"\bsorry\b" lean)) :axiom (count (re-seq #"(?m)^[ \t]*axiom\b" lean))
                 :native-decide (count (re-seq #"\bnative_decide\b" lean))}
     :all-transcriptions-match? (and (every? (fn [[_ x]] (if (contains? x :lean) (= (:derived x) (:lean x)) true)) trans)
                                     (empty? (get-in trans [:admitted :derived]))
                                     (get-in trans [:admitted :lean-empty?]) (get-in trans [:patterns :lean-iCC-0-23?]))}))
(defn verdict [f]
  (let [c (:conformance f)]
    (and (get-in f [:index-map :bijection-0-23?]) (get-in f [:index-map :names-match-repository?])
         (:all-transcriptions-match? f) (get-in f [:edge-routes :row-equals-fast-forward?])
         (get-in f [:edge-routes :full-equals-lean?]) (= 10 (get-in f [:edge-routes :lean-list-count]))
         (get-in f [:edge-routes :lean-list-no-duplicates?]) (get-in f [:edge-routes :three-routes-agree?])
         (get-in f [:edge-routes :lean-in-added-matches-added?])
         (get-in f [:pointer-bindings :match?]) (every? true? (vals (:laws f))) (:organise-reproduces-record? f)
         (:all-seven-used? c) (zero? (:new-conformant-structure-count c))
         (:reuses-sans-o3? c) (:reuses-sans-o4? c) (:sans-o3-defined-in-ruled? c) (:sans-o4-defined-in-ruled? c)
         (every? zero? (vals (:forbidden f))))))

(defn scenario-rows [fx] (filter #(and (= :g4 (:treatment %)) (= :snatcher (:disposition %))) (:scenarios fx)))
(defn mutate-row [fx policy fun]
  (update fx :scenarios (fn [ss] (mapv #(if (and (= :g4 (:treatment %)) (= :snatcher (:disposition %)) (= policy (:policy %))) (fun %) %) ss))))
(defn mutate [s old new] (let [r (str/replace-first s old new)] {:source r :landed? (not= s r)}))

(let [lean (slurp lean-path) fixture-text (slurp fixture-path) fixture (edn/read-string fixture-text)
      base-d (derive-data fixture-path) base-f (facts lean fixture-text base-d)
      patterns (first (filter #(= :patterns (:policy %)) (scenario-rows fixture)))
      fx1 (mutate-row fixture :patterns #(update % :acting (fn [v] (vec (remove #{:re-enter-after-observed-repair} v)))))
      fx2 (mutate-row fixture :exchange-first #(assoc % :score 3))
      fx3 (mutate-row fixture :exchange-first #(assoc % :precedence (:precedence patterns)))
      fx4 (mutate-row fixture :patterns #(update % :added-by-organise (fn [v] (vec (remove #{:institutions-vary-by-position-and-force} v)))))
      swap (-> lean (str/replace "snatch-cascade.edn:84" "snatch-cascade.edn:SWAP")
               (str/replace "snatch-cascade.edn:211" "snatch-cascade.edn:84")
               (str/replace "snatch-cascade.edn:SWAP" "snatch-cascade.edn:211"))
      shift (mutate lean "snatch-cascade.edn:212-219" "snatch-cascade.edn:213-219")
      sorry {:source (str lean "\ntheorem plant : True := by sorry\n") :landed? true}
      lean-multi (frequencies (re-seq #"futon3:checks/snatch-cascade\.edn:\d+(?:-\d+)?" lean))
      plant-data [[:fixture-drop-selected :fixture fx1]
                  [:fixture-flatten-score :fixture fx2]
                  [:fixture-flatten-precedence :fixture fx3]
                  [:fixture-drop-recorded-added :fixture fx4]
                  [:derived-drop-stands-edge :derived true]
                  [:exchange-declaration-citations :lean {:source swap :landed? (and (not= swap lean) (= lean-multi (frequencies (re-seq #"futon3:checks/snatch-cascade\.edn:\d+(?:-\d+)?" swap))))}]
                  [:shift-declaration-citation :lean shift]
                  [:append-sorry :lean sorry]]
      plants (mapv (fn [[nm kind data]]
                     (let [d (case kind :fixture (derive-data (write-temp data)) :derived (derive-data fixture-path true) base-d)
                           l (if (= kind :lean) (:source data) lean)
                           landed (case kind :fixture (not= fixture data) :derived (not= (:full d) (:full base-d)) (:landed? data))
                           f (facts l fixture-text d)]
                       (sorted-map :plant nm :landed? landed :verdict-after (verdict f)
                                   :citation-multiset-unchanged? (when (= nm :exchange-declaration-citations) landed))))
                   plant-data)
      head (str/trim (:out (sh/sh "git" "-C" futon3 "rev-parse" "HEAD")))
      holes (str/trim (:out (sh/sh "git" "-C" mathlib "log" "-1" "--format=%H" "--" "DarkTower/WarMachine/Holes.lean")))
      report (into (sorted-map) (merge base-f {:check :F12-snatch-exemplar :futon3-head head :futon3-pin futon3-pin
              :holes-lean-head holes :holes-lean-pin holes-pin :plants plants
              :source-pointers ["mathlib4:DarkTower/WarMachine/F12SnatchExemplar.lean:1-337"
                                "futon3:checks/find_organise.clj:284-293" "futon3:checks/find_organise.clj:603-640"]}))
      ok (and (= head futon3-pin) (= holes holes-pin) (verdict base-f)
              (every? #(and (:landed? %) (false? (:verdict-after %))) plants))
      report (stable (assoc report :verdict (if ok :pass :fail)))]
  (when-not ok (binding [*out* *err*] (pp/pprint report)) (System/exit 1))
  (.mkdirs (.getParentFile (java.io.File. out-path)))
  (spit out-path (with-out-str (pp/pprint report)))
  (println (str "f12_snatch_exemplar_check: pass full=" (pr-str (get-in report [:edge-routes :full-node]))
                " no-bootstrap=" (pr-str (get-in report [:edge-routes :no-bootstrap]))
                " artifact=" out-path)))
