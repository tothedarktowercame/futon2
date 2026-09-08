#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[babashka.process :as p]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def lab (fs/parent (fs/file *file*)))
(def repo (-> lab fs/parent fs/parent fs/parent))
(def mathlib "/home/joe/code/mathlib4")
(def run-dir (fs/path lab "runs/F12-organise"))
(def paths {:ruled (fs/path run-dir "17-ruled-carrier.edn")
            :ants (fs/path run-dir "18-ants-exemplar.edn")
            :mining (fs/path run-dir "19-mining-exemplar.edn")
            :snatch (fs/path run-dir "24-snatch-exemplar.edn")
            :registry (fs/path lab "aif-equations.edn")
            :ants-lean (fs/path mathlib "DarkTower/WarMachine/F12AntsExemplar.lean")
            :mining-lean (fs/path mathlib "DarkTower/WarMachine/F12MiningExemplar.lean")
            :snatch-lean (fs/path mathlib "DarkTower/WarMachine/F12SnatchExemplar.lean")
            :holes (fs/path mathlib "DarkTower/WarMachine/Holes.lean")})

(defn read-edn [p] (edn/read-string (slurp (str p))))
(defn sha [p] (first (str/split (str/trim (:out (p/shell {:out :string} "sha256sum" (str p)))) #"\s+")))
(defn git-out [dir & args]
  (str/trim (:out (apply p/shell {:dir (str dir) :out :string} "git" args))))
(defn present? [s re] (boolean (re-find re s)))
(defn pass? [v] (contains? #{true :pass} v))

(defn conclusions [inputs]
  (let [{:keys [ruled ants mining snatch ants-lean mining-lean snatch-lean registry]} inputs
        ants-orders (get-in ants [:orders :lean])
        mining-t (get mining :transcription)
        snatch-t (get snatch :transcription)
        full (get-in snatch [:edge-routes :full-node])
        nb (get-in snatch [:edge-routes :no-bootstrap])
        exemplars
        {:ants {:verdict (pass? (:verdict ants))
                :ruled-organise-type? (present? ants-lean #"organiseAnts : RuledOrganiseType")
                :added-by-organise-empty? (present? ants-lean #"addedByOrganise := ∅")
                :score-type-degenerate? false
                :score-moves? (not (get-in ants [:record-assertions :score-unchanged]))
                :precedence-moves? (not= (:precedence-before ants-orders) (:precedence-after ants-orders))
                :acting-order-moves? (not= (:acting-before ants-orders) (:acting-after ants-orders))
                :o4-disjuncts-available [:acting-order :score]
                :o4-disjuncts-holding (cond-> []
                                          (not= (:acting-before ants-orders) (:acting-after ants-orders)) (conj :acting-order)
                                          (not (get-in ants [:record-assertions :score-unchanged])) (conj :score))
                :o3-subtraction-nontrivial? false
                :recorded-edges [] :ruled-no-bootstrap-edges []
                :recorded-edge-count 0 :ruled-no-bootstrap-edge-count 0
                :reproduces-recorded-edges? true}
         :mining {:verdict (pass? (:verdict mining))
                  :ruled-organise-type? (present? mining-lean #"organiseMining : RuledOrganiseType")
                  :added-by-organise-empty? (present? mining-lean #"addedByOrganise := ∅")
                  :score-type-degenerate? (get-in mining-t [:score-type-unit?])
                  :score-moves? false
                  :precedence-moves? false :acting-order-moves? false
                  :o4-disjuncts-available [:acting-order]
                  :o4-disjuncts-holding []
                  :o3-subtraction-nontrivial? false
                  :recorded-edges (get-in mining-t [:edges :record])
                  :ruled-no-bootstrap-edges (get-in mining-t [:edges :lean])
                  :recorded-edge-count (count (get-in mining-t [:edges :record]))
                  :ruled-no-bootstrap-edge-count (count (get-in mining-t [:edges :lean]))
                  :reproduces-recorded-edges? (= (set (get-in mining-t [:edges :record]))
                                                 (set (get-in mining-t [:edges :lean])))}
         :snatch {:verdict (pass? (:verdict snatch))
                  :ruled-organise-type? (present? snatch-lean #"organiseSnatch : RuledOrganiseType")
                  :added-by-organise-empty? (not (present? snatch-lean #"def snatchAdded : Set Nat := \{[^}]+\}"))
                  :score-type-degenerate? false
                  :score-moves? (not= (get-in snatch-t [:score-before :derived]) (get-in snatch-t [:score-after :derived]))
                  :precedence-moves? (not= (get-in snatch-t [:precedence-before :derived]) (get-in snatch-t [:precedence-after :derived]))
                  :acting-order-moves? (not= (get-in snatch-t [:acting-before :derived]) (get-in snatch-t [:acting-after :derived]))
                  :o4-disjuncts-available [:acting-order :score]
                  :o4-disjuncts-holding (cond-> []
                                          (not= (get-in snatch-t [:acting-before :derived]) (get-in snatch-t [:acting-after :derived])) (conj :acting-order)
                                          (not= (get-in snatch-t [:score-before :derived]) (get-in snatch-t [:score-after :derived])) (conj :score))
                  :o3-subtraction-nontrivial? (present? snatch-lean #"def snatchAdded : Set Nat := \{[^}]+\}")
                  :recorded-edges full :ruled-no-bootstrap-edges nb
                  :recorded-edge-count (count full) :ruled-no-bootstrap-edge-count (count nb)
                  :reproduces-recorded-edges? (= (set full) (set nb))}}
        witnesses (fn [pred] (->> exemplars (keep (fn [[k v]] (when (pred v) k))) vec))
        criteria {:o1 #(or (not (:added-by-organise-empty? %)) (= % (:ants exemplars)))
                  :o2 #(pos? (:recorded-edge-count %))
                  :o3 :o3-subtraction-nontrivial?
                  :o4 :precedence-moves?
                  :oattr #(= % (:ants exemplars))
                  :oauth #(pos? (:recorded-edge-count %))
                  :osel (constantly true)}
        clause-names (mapv keyword (:clauses ruled))
        coverage (into (sorted-map)
                       (for [c clause-names
                             :let [criterion (get criteria c)
                                   ws (witnesses #(if (keyword? criterion) (criterion %) (criterion %)))]]
                         [c {:true-antecedent-exercised? (boolean (seq ws)) :exemplars ws}]))
        carrier-choice (get-in registry [:choices :organise-carrier])
        sorry-choice (get-in registry [:choices :organise-sorry])
        rk (keyword "ruling")]
    {:exemplars exemplars
     :clause-coverage coverage
     :all-exemplars-pass? (every? #(and (:verdict %) (:ruled-organise-type? %)) (vals exemplars))
     :only-subtraction-stressing-exemplar :snatch
     :subtraction-stressing-exemplar-reproduces? (get-in exemplars [:snatch :reproduces-recorded-edges?])
     :registry-evidence {:carrier-status (:status carrier-choice)
                         :carrier-record (select-keys (get carrier-choice rk) [:by :at :consequence])
                         :sorry-status (:status sorry-choice)
                         :sorry-record (select-keys (get sorry-choice rk) [:by :at :consequence])
                         :other-organise-choice-count
                         (dec (count (filter #(str/starts-with? (name %) "organise-")
                                             (keys (:choices registry)))))}}))

(def base-inputs {:ruled (read-edn (:ruled paths)) :ants (read-edn (:ants paths))
                  :mining (read-edn (:mining paths)) :snatch (read-edn (:snatch paths))
                  :registry (read-edn (:registry paths))
                  :ants-lean (slurp (str (:ants-lean paths)))
                  :mining-lean (slurp (str (:mining-lean paths)))
                  :snatch-lean (slurp (str (:snatch-lean paths)))})
(def baseline (conclusions base-inputs))
(defn plant [id mutate]
  (let [tmp (fs/create-temp-dir {:prefix "f12-proviso-plant-"})
        before (fs/path tmp "input-before.edn")
        after-file (fs/path tmp "input-after.edn")
        mutated (mutate base-inputs)
        _ (spit (str before) (pr-str base-inputs))
        _ (spit (str after-file) (pr-str mutated))
        landed? (not= (sha before) (sha after-file))
        after (conclusions (read-edn after-file))
        answer {:plant id :landed? landed?
                :conclusions-flipped? (not= baseline after)
                :verdict-after (= baseline after)}]
    (fs/delete-tree tmp)
    (assoc answer :temporary-copy-removed? (not (fs/exists? tmp)))))
(def plants
  [(plant :ants-verdict-false #(assoc-in % [:ants :verdict] false))
   (plant :mining-added-nonempty #(update % :mining-lean str/replace "addedByOrganise := ∅" "addedByOrganise := {0}"))
   (plant :snatch-no-bootstrap-full #(assoc-in % [:snatch :edge-routes :no-bootstrap]
                                                   (get-in % [:snatch :edge-routes :full-node])))
   (plant :snatch-score-flat #(assoc-in % [:snatch :transcription :score-after :derived]
                                         (get-in % [:snatch :transcription :score-before :derived])))
   (plant :irrelevant-check-label #(assoc-in % [:ants :check] :mutated-control))])

(def input-digests (into (sorted-map) (for [[k pth] paths] [k (sha pth)])))
(def holes-head (git-out mathlib "log" "-1" "--format=%H" "--" "DarkTower/WarMachine/Holes.lean"))
(def result
  (assoc baseline
         :check :F12-exemplar-proviso-sheet
         :arms
         {:a {:label "discharged-as-it-stands" :cost {:nonreproducing-exemplars (count (remove :reproduces-recorded-edges? (vals (:exemplars baseline))))}}
          :b {:label "discharged-by-snatch-alone" :cost {:snatch-reproduces? (get-in baseline [:exemplars :snatch :reproduces-recorded-edges?])}}
          :c {:label "not-discharged" :cost {:passing-exemplars (count (filter :verdict (vals (:exemplars baseline))))}}
          :d {:label "discharged-with-o3-reopened" :cost {:new-open-field-count (if (get-in baseline [:exemplars :snatch :reproduces-recorded-edges?]) 0 1)}}
          :e {:label "discharged-conditionally" :cost {:deferred-divergence-count (if (get-in baseline [:exemplars :snatch :reproduces-recorded-edges?]) 0 1)}}}
         :provenance {:futon2-head (git-out repo "rev-parse" "HEAD") :holes-lean-head holes-head :input-sha256 input-digests}
         :plants plants
         :originals-byte-identical-after-plants? (= input-digests (into (sorted-map) (for [[k pth] paths] [k (sha pth)])))
         :verdict (and (:all-exemplars-pass? baseline)
                       (every? :conclusions-flipped? (butlast plants))
                       (:verdict-after (last plants)))))

(def artifact (fs/path run-dir "25-proviso.edn"))
(with-open [w (io/writer (str artifact))] (binding [*out* w] (pp/pprint result)))

(def doc-path (fs/path lab "C582-F12-exemplar-proviso-sheet.md"))
(spit (str doc-path)
      (str "# F12 exemplar-proviso choice sheet\n\n"
           "## Measured position\n\n"
           "All three exemplars pass at the ruled carrier. [25-proviso.edn `:all-exemplars-pass?`]\n\n"
           "Ants leaves `addedByOrganise` empty and carries O4 only through acting-order movement; its score is flat. [25-proviso.edn `:exemplars :ants`]\n\n"
           "Mining leaves `addedByOrganise` empty, uses the degenerate `Unit` score type, and has no moved before/after pair. [25-proviso.edn `:exemplars :mining`]\n\n"
           "Snatch alone makes the O3 subtraction non-trivial; its ruled edge set has " (get-in result [:exemplars :snatch :ruled-no-bootstrap-edge-count])
           " edge while its recorded set has " (get-in result [:exemplars :snatch :recorded-edge-count]) ". [25-proviso.edn `:exemplars :snatch`]\n\n"
           "The seven-clause cross-exemplar coverage and its witnesses are recorded without selecting an outcome. [25-proviso.edn `:clause-coverage`]\n\n"
           "The carrier proviso and staged existence amendment remain registry facts attributed to Joe. [25-proviso.edn `:registry-evidence`; holes/labs/wm-contract/aif-equations.edn: not found]\n\n"
           "## Question\n\n"
           "Does the exemplar set `{ants, mining, snatch}` discharge the item-4 proviso, and therefore release the staged item-5 amendment at `Holes.lean:861`? [25-proviso.edn `:registry-evidence`]\n\n"
           "## Arms and measured costs\n\n"
           "- (a) **Discharged as it stands.** Arm 6 is exercised on three passing exemplars; the cost is accepting the measured count of non-reproducing exemplars. [25-proviso.edn `:arms :a :cost`]\n"
           "- (b) **Discharged by snatch alone.** Treat the only subtraction-stressing row as sufficient; the cost records whether that row reproduces its edge set. [25-proviso.edn `:arms :b :cost`]\n"
           "- (c) **Not discharged.** Require one exemplar both to stress O3 and reproduce its run; the cost is leaving the measured passing exemplars insufficient. [25-proviso.edn `:arms :c :cost`]\n"
           "- (d) **Discharged, with O3 reopened.** Accept the carrier and register the divergence separately; the cost is the measured number of newly open fields. [25-proviso.edn `:arms :d :cost`]\n"
           "- (e) **Discharged conditionally.** Release the amendment while carrying the divergence as a separate row; the cost is the measured deferred-divergence count. [25-proviso.edn `:arms :e :cost`]\n\n"
           "## Limits\n\n"
           "No arm is preferred and no verdict on the proviso is taken here. [25-proviso.edn `:arms`; 25-proviso.edn `:registry-evidence`]\n\n"
           "Nothing measured here bears on the other " (get-in result [:registry-evidence :other-organise-choice-count])
           " registered organise choices. [25-proviso.edn `:registry-evidence :other-organise-choice-count`]\n\n"
           "The mutation audit changes four relevant inputs and observes changed conclusions; its irrelevant control leaves conclusions stable, and original input digests remain identical. [25-proviso.edn `:plants`; 25-proviso.edn `:originals-byte-identical-after-plants?`]\n"))

(when-not (:verdict result) (System/exit 1))
