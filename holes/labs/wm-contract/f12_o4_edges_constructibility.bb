#!/usr/bin/env bb
(require '[clojure.edn :as edn] '[clojure.string :as str]
         '[clojure.java.shell :as sh] '[clojure.pprint :as pp])

(def root "/home/joe/code")
(def pin "cdb5e8a56fd907beb6a99f8b88af9de50ff93126")
(def out (str root "/futon2/holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn"))
(def corpus
  [{:id :construct :repo :futon3 :path "checks/construct-cascade.edn"}
   {:id :zaif :repo :futon3 :path "checks/zaif-cascade.edn"}
   {:id :ants :repo :futon3 :path "checks/ants-cascade.edn"}
   {:id :alfworld :repo :futon3 :path "checks/alfworld-cascade.edn"}
   {:id :snatch :repo :futon3 :path "checks/snatch-cascade.edn"}
   {:id :open :repo :futon3 :path "checks/open-cascade.edn"}
   {:id :open-short-cue :repo :futon3 :path "checks/open-cascade-short-cue.edn"}
   {:id :retrodiction :repo :futon3 :path "checks/retrodiction-cascade.edn"}
   {:id :retrodiction-per-clause :repo :futon3 :path "checks/retrodiction-cascade-per-clause.edn"}
   {:id :mining-exemplar :repo :futon2 :path "holes/labs/library-loop/runs/mining-exemplar/cascade.edn"}])
(def gate-path (str root "/futon3c/scripts/zaif_cascade_gate.clj"))

(defn read-source [{:keys [repo path]}]
  (if (= repo :futon3)
    (:out (sh/sh "git" "-C" (str root "/futon3") "show" (str pin ":" path)))
    (slurp (str root "/" (name repo) "/" path))))
(defn source-name [d] (str (name (:repo d)) ":" (:path d)))
(defn lines [s] (str/split-lines s))
(defn key-lines [s k]
  (mapv (comp inc first) (filter #(re-find (re-pattern (str "(^|\\s)" (java.util.regex.Pattern/quote (str k)) "(\\s|$)")) (second %))
                                  (map-indexed vector (lines s)))))
(defn ptr [d n] (str (source-name d) ":" n))
(defn walk [x]
  (letfn [(go [v p] (lazy-seq (cons [p v]
    (cond (map? v) (mapcat (fn [[k z]] (go z (conj p k))) v)
          (vector? v) (mapcat (fn [[i z]] (go z (conj p i))) (map-indexed vector v))
          :else nil))))]
    (go x [])))
(defn rule-ids []
  (let [s (slurp gate-path)
        body (second (re-find #"(?s)\(def rule-table\s+\[(.*?)\]\)\s+\(defn" s))]
    (mapv (comp keyword second) (re-seq #"\{:id\s+:([^\s\n]+)" body))))
(defn library-fact [id]
  (let [path (str "library/" (subs (str id) 1) ".flexiarg")
        r (sh/sh "git" "-C" (str root "/futon3") "show" (str pin ":" path))]
    {:id id :path (str "futon3:" path) :exists? (zero? (:exit r))
     :line-count (if (zero? (:exit r)) (count (lines (:out r))) 0)
     :basis (str "futon3@" pin ":" path)}))
(defn o4-reading [data]
  (or (:o4 data) (first (keep (fn [[p v]] (when (= :o4 (last p)) v)) (walk data))) "not found"))
(defn o4-line [text] (or (last (key-lines text :o4)) "not found"))
(defn member-maps [d text data rules]
  (let [member-line (first (key-lines text :members)) selected-line (first (key-lines text :selected))
        candidates
        (for [[p v] (walk data)
              :when (and (map? v)
                         (or (and (= :find (last p)) (sequential? (:selected v)))
                             (and (= :cascade (last p)) (sequential? (:members v)))
                             (and (= (:id d) :mining-exemplar) (empty? p))))]
          [p v])]
    (mapv
     (fn [[p v]]
       (let [members (vec (or (:members v) [])) selected (vec (or (:selected v) []))
             use-selected? (and (empty? members) (= :find (last p)))
             member-list (if use-selected? selected members)
             carriers (vec (filter (set rules) member-list))
             edges (vec (or (:edges v) []))
             mining? (= (:id d) :mining-exemplar)
             a? (and (not mining?) (>= (count carriers) 2)) b? (pos? (count edges))
             mp (if (seq members) (ptr d member-line) "not found")
             sp (if (seq selected) (ptr d selected-line) "not found")
             ep (if (seq edges) (ptr d (or (first (key-lines text :edges)) "not found")) "not found")]
         {:record-id (:id d) :source (source-name d) :structural-path (pr-str p)
          :members members :member-count (count members) :members-pointer mp
          :selected selected :selected-count (count selected) :selected-pointer sp
          :member-list-source (if use-selected? :selected :members)
          :rule-table-carriers carriers :rule-table-carrier-count (count carriers)
          :edges edges :edge-count (count edges) :edges-pointer ep
          :meets-precondition-a? (boolean a?) :meets-precondition-b? (boolean b?)
          :meets-both? (boolean (and a? b?))
          ;; The three pointers above locate the FIRST line of the record that carries the
          ;; key; they do not locate this row's own collection. Two rows of one record share
          ;; an :edges-pointer. Said here so a reader does not take them for row-grain.
          :pointer-grain :record-level-first-occurrence-of-the-key
          :pointer-grain-pointer "futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:68-70"
          :precondition-b-reads-only :edges-vector-under-this-node
          :precondition-b-scope-pointer "futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:65"
          :candidate-status (if mining? :excluded-prose-attestations-not-gate-predicates :gate-shaped)
          :candidate-status-pointer (if mining? (str (source-name d) ":44-65")
                                        "futon3c:scripts/zaif_cascade_gate.clj:444-445")}))
     candidates)))
(defn record-rows [d rules]
  (let [text (read-source d) data (edn/read-string text)]
    {:record-id (:id d) :source (source-name d)
     :member-sets (member-maps d text data rules)
     ;; Some records state their authored-edge count under :cascade-edges instead of
     ;; carrying an :edges vector, so precondition B above reads them as edge-free.
     ;; Lines are listed, not zipped to nodes: a positional zip is the drop slice 5 warned of.
     :cascade-edges-key-lines (mapv #(ptr d %) (key-lines text :cascade-edges))
     :o4 (o4-reading data) :o4-pointer (if (= "not found" (o4-reading data)) "not found" (ptr d (o4-line text)))}))
(defn candidate-detail [records]
  (for [r records row (:member-sets r) :when (:meets-both? row)]
    (assoc (select-keys row [:record-id :structural-path :member-count :rule-table-carriers :edges])
           :what-the-record-says-about-o4 (:o4 r)
           :o4-pointer (:o4-pointer r)
           :what-is-missing :an-o4-play-measurement-for-this-member-set
           :emitter-class :unconditional-constructor-literal
           :emitter-pointer "futon3:checks/construct_retrodiction_cascade.clj:335"
           :gate-pair-shape :vectors
           :gate-pair-shape-pointer "futon3c:scripts/zaif_cascade_gate.clj:555-558"
           :slice-5-map-pair-census-would-see-it? false
           :slice-5-shape-pointer "futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:53-57")))
(defn core-report []
  (let [rules (rule-ids) records (mapv #(record-rows % rules) corpus)
        candidates (vec (candidate-detail records))]
    (sorted-map
     :check :F12-o4-edges-constructibility
     :scope {:source :slice-5-corpus-restated :records (mapv source-name corpus)
             :pointer "futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:8-19"}
     :futon3-pin {:value pin :pointer "futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:6"}
     :rule-table {:ids rules :count (count rules)
                  :library-patterns (mapv library-fact rules)
                  :definition-pointers ["futon3c:scripts/zaif_cascade_gate.clj:206"
                                        "futon3c:scripts/zaif_cascade_gate.clj:218"
                                        "futon3c:scripts/zaif_cascade_gate.clj:234"
                                        "futon3c:scripts/zaif_cascade_gate.clj:249"]
                  :precondition-pointer "futon3c:scripts/zaif_cascade_gate.clj:444-445"}
     :records records
     :any-member-set-meets-both-preconditions? (boolean (seq candidates))
     :member-sets-meeting-both candidates
     :blocking-reason-per-candidate
     (mapv #(select-keys % [:record-id :structural-path :what-the-record-says-about-o4
                            :o4-pointer :what-is-missing :emitter-class :emitter-pointer]) candidates)
     :premises
     [{:id 1 :holds? (and (= 4 (count rules))
                          (= [22 95 32 30] (mapv :line-count (map library-fact rules)))
                          (every? :exists? (map library-fact rules)))
       :pointer "futon3c:scripts/zaif_cascade_gate.clj:206-249"}
      {:id 2 :holds? (= [[11 1] [20 1] [17 1]]
                        (mapv (juxt #(if (= :selected (:member-list-source %)) (:selected-count %) (:member-count %))
                                    :rule-table-carrier-count)
                              (get-in records [1 :member-sets])))
       :pointer "futon3:checks/zaif-cascade.edn:167"}
      {:id 3 :holds? (= 4 (count candidates))
       :pointer "futon3:checks/retrodiction-cascade.edn:34090"}
      {:id 4 :holds? (= :excluded-prose-attestations-not-gate-predicates
                        (get-in records [9 :member-sets 0 :candidate-status]))
       :pointer "futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:44-65"}])))

(defn resolve-pointer [p]
  (when-not (= p "not found")
    (let [[repo path span] (str/split p #":") n (parse-long (first (str/split span #"-")))
          d {:repo (keyword repo) :path path}]
      (nth (lines (read-source d)) (dec n) nil))))
(defn pointer-carries? [p re]
  (or (= p "not found") (boolean (when-let [line (resolve-pointer p)] (re-find re line)))))
(defn bytes-at
  "The node a row's structural path names, read out of the record's own bytes."
  [d path]
  (let [data (edn/read-string (read-source d)) sp (edn/read-string path)]
    (if (seq sp) (get-in data sp) data)))
(defn source-node-count
  "How many member-set nodes the record's bytes carry. A coverage count, so a row deleted
   from the report is refused rather than passing on the rows that remain."
  [d]
  (let [data (edn/read-string (read-source d))]
    (count (for [[p v] (walk data)
                 :when (and (map? v)
                            (or (and (= :find (last p)) (sequential? (:selected v)))
                                (and (= :cascade (last p)) (sequential? (:members v)))
                                (and (= (:id d) :mining-exemplar) (empty? p))))]
             p))))
(defn row-refusals
  "The reasons the record's bytes refuse this row as written."
  [d rules row]
  (let [node (bytes-at d (:structural-path row))
        members (vec (or (:members node) [])) selected (vec (or (:selected node) []))
        member-list (if (= :selected (:member-list-source row)) selected members)
        edges (vec (or (:edges node) []))
        carriers (vec (filter (set rules) member-list))]
    (cond-> []
      (nil? node) (conj :structural-path-does-not-resolve-in-the-source)
      (not= members (:members row)) (conj :members-disagree-with-the-source)
      (not= selected (:selected row)) (conj :selected-disagree-with-the-source)
      (not= edges (:edges row)) (conj :edges-disagree-with-the-source)
      (not= carriers (:rule-table-carriers row)) (conj :carriers-disagree-with-the-source)
      (not (every? (set rules) (:rule-table-carriers row))) (conj :carrier-is-not-in-the-gate-rule-table)
      (not (every? (set member-list) (:rule-table-carriers row))) (conj :carrier-is-not-a-member)
      (not (pointer-carries? (:members-pointer row) #":members")) (conj :members-pointer-does-not-carry-the-key)
      (not (pointer-carries? (:selected-pointer row) #":selected")) (conj :selected-pointer-does-not-carry-the-key)
      (not (pointer-carries? (:edges-pointer row) #":edges")) (conj :edges-pointer-does-not-carry-the-key))))
(defn record-refusals [d rules rec]
  (let [data (edn/read-string (read-source d))]
    (cond-> (vec (mapcat #(row-refusals d rules %) (:member-sets rec)))
      (not= (o4-reading data) (:o4 rec)) (conj :o4-disagrees-with-the-source)
      (not (pointer-carries? (:o4-pointer rec) #":o4")) (conj :o4-pointer-does-not-carry-the-key)
      (not= (source-node-count d) (count (:member-sets rec)))
      (conj :member-set-rows-do-not-cover-the-source))))
(defn refusals
  "Every reason the record files refuse the report AS GIVEN. This does not recompute the
   report and compare it with itself: each claim is contradicted by the bytes it cites, or
   it stands. A predicate that opened with (= r (core-report)) would refuse every mutation
   of a deterministic report by construction and so would measure nothing -- the defect
   found in slice 5 and repaired at futon2 b9e85cf3."
  [r rules]
  (vec (for [rec (:records r)
             :let [d (first (filter #(= (:id %) (:record-id rec)) corpus))]
             why (record-refusals d rules rec)]
         [(:record-id rec) why])))
(defn valid? [r rules] (empty? (refusals r rules)))
(defn count-only-valid?
  "The naive alternative: every count the report states equals the length of the collection
   it states it for. Reads no source. Computed here rather than asserted, so the column
   saying which plants a count-only check would miss is a measurement."
  [r]
  (every? (fn [row] (and (= (:member-count row) (count (:members row)))
                         (= (:selected-count row) (count (:selected row)))
                         (= (:edge-count row) (count (:edges row)))
                         (= (:rule-table-carrier-count row) (count (:rule-table-carriers row)))))
          (mapcat :member-sets (:records r))))
;; Rows 4 and 5 of the retrodiction record are two of the four member sets the verdict
;; turns on; every plant that can be aimed is aimed there. Aiming them at rows the answer
;; does not rest on was the second slice-5-class defect found in review of this slice.
(def candidate-row 4)
(def candidate-row-2-edges 5)
(defn plant [base k]
  (case k
    :swap-carrier-same-count (assoc-in base [:records 7 :member-sets candidate-row :rule-table-carriers]
                                       [:agent/budget-bounds-exploration :aif/not-a-rule])
    :carrier-not-a-member (assoc-in base [:records 7 :member-sets candidate-row :rule-table-carriers]
                                    [:agent/budget-bounds-exploration :war-machine/ambient-pattern-retrieval])
    :reverse-edge-same-count (update-in base [:records 7 :member-sets candidate-row-2-edges :edges 0]
                                        (comp vec reverse))
    :drop-one-edge-of-two (update-in base [:records 7 :member-sets candidate-row-2-edges :edges] pop)
    :concat-selected-into-members (update-in base [:records 7 :member-sets candidate-row :members]
                                             into (get-in base [:records 7 :member-sets candidate-row :selected]))
    :shift-pointer (assoc-in base [:records 7 :member-sets candidate-row :edges-pointer]
                             "futon3:checks/retrodiction-cascade.edn:6743")
    :change-o4 (assoc-in base [:records 7 :o4] :exercised)
    :drop-member-set (update-in base [:records 7 :member-sets] pop)))

(let [rules (rule-ids)
      base (core-report)
      plants (mapv (fn [k] (let [p (plant base k) why (vec (distinct (map second (refusals p rules))))]
                             {:plant k :landed? (not= p base)
                              :caught-by-validity-predicate? (boolean (seq why))
                              :refused-for why
                              :naive-count-only-would-catch? (not (count-only-valid? p))
                              :pointer "futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:224-238"}))
                   [:swap-carrier-same-count :carrier-not-a-member :reverse-edge-same-count
                    :drop-one-edge-of-two :concat-selected-into-members :shift-pointer
                    :change-o4 :drop-member-set])
      report (assoc base
                    :plants plants
                    :validity-predicate
                    {:kind :source-grounded
                     :reads :the-record-files-plus-the-gate-rule-table
                     :recomputes-the-report-and-compares? false
                     :pointer "futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:153-208"}
                    :verdict (and (valid? base rules)
                                  (every? :holds? (:premises base))
                                  (every? #(and (:landed? %) (:caught-by-validity-predicate? %)) plants)))]
  (when-not (:verdict report)
    (binding [*out* *err*] (pp/pprint report)) (System/exit 1))
  (.mkdirs (.getParentFile (java.io.File. out)))
  (spit out (with-out-str (pp/pprint report))))
