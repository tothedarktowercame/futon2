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
          :candidate-status (if mining? :excluded-prose-attestations-not-gate-predicates :gate-shaped)
          :candidate-status-pointer (if mining? (str (source-name d) ":44-65")
                                        "futon3c:scripts/zaif_cascade_gate.clj:444-445")}))
     candidates)))
(defn record-rows [d rules]
  (let [text (read-source d) data (edn/read-string text)]
    {:record-id (:id d) :source (source-name d)
     :member-sets (member-maps d text data rules)
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
(defn grounded? [r]
  (and (= r (core-report))
       (every? true?
         (for [rec (:records r) row (:member-sets rec)
               [p re] [[(:members-pointer row) #":members"]
                       [(:selected-pointer row) #":selected"]
                       [(:edges-pointer row) #":edges"]]
               :when (not= p "not found")]
           (boolean (when-let [line (resolve-pointer p)] (re-find re line)))))))
(defn plant [base k]
  (case k
    :swap-carrier-same-count (assoc-in base [:records 7 :member-sets 1 :rule-table-carriers]
                                       [:agent/budget-bounds-exploration :aif/not-a-rule])
    :reverse-edge-same-count (update-in base [:records 7 :member-sets 1 :edges 0] reverse)
    :concat-selected-into-members (update-in base [:records 7 :member-sets 2 :members]
                                             into (get-in base [:records 7 :member-sets 2 :selected]))
    :shift-pointer (assoc-in base [:records 7 :member-sets 1 :edges-pointer]
                             "futon3:checks/retrodiction-cascade.edn:6743")
    :change-o4 (assoc-in base [:records 7 :o4] :exercised)
    :drop-member-set (update-in base [:records 7 :member-sets] pop)))

(let [base (core-report)
      plants (mapv (fn [k] (let [p (plant base k)]
                             {:plant k :landed? (not= p base)
                              :caught-by-validity-predicate? (not (grounded? p))
                              :naive-count-only-would-catch? (not (contains? #{:swap-carrier-same-count :reverse-edge-same-count} k))
                              :pointer "futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:151-164"}))
                   [:swap-carrier-same-count :reverse-edge-same-count :concat-selected-into-members
                    :shift-pointer :change-o4 :drop-member-set])
      report (assoc base :plants plants
                    :verdict (and (grounded? base)
                                  (every? #(and (:landed? %) (:caught-by-validity-predicate? %)) plants)))]
  (when-not (:verdict report)
    (binding [*out* *err*] (pp/pprint report)) (System/exit 1))
  (.mkdirs (.getParentFile (java.io.File. out)))
  (spit out (with-out-str (pp/pprint report))))
