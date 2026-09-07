#!/usr/bin/env bb
(require '[clojure.edn :as edn] '[clojure.string :as str]
         '[clojure.java.shell :as sh] '[clojure.pprint :as pp])

(def root "/home/joe/code")
(def pin "cdb5e8a56fd907beb6a99f8b88af9de50ff93126")
(def out (str root "/futon2/holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn"))
(def corpus
  [{:id :construct :repo :futon3 :path "checks/construct-cascade.edn" :edge-mode :count}
   {:id :zaif :repo :futon3 :path "checks/zaif-cascade.edn" :edge-mode :count}
   {:id :ants :repo :futon3 :path "checks/ants-cascade.edn" :edge-mode :count}
   {:id :alfworld :repo :futon3 :path "checks/alfworld-cascade.edn" :edge-mode :count}
   {:id :snatch :repo :futon3 :path "checks/snatch-cascade.edn" :edge-mode :none}
   {:id :open :repo :futon3 :path "checks/open-cascade.edn" :edge-mode :nested}
   {:id :open-short-cue :repo :futon3 :path "checks/open-cascade-short-cue.edn" :edge-mode :nested}
   {:id :retrodiction :repo :futon3 :path "checks/retrodiction-cascade.edn" :edge-mode :nested}
   {:id :retrodiction-per-clause :repo :futon3 :path "checks/retrodiction-cascade-per-clause.edn" :edge-mode :nested}
   {:id :mining-exemplar :repo :futon2 :path "holes/labs/library-loop/runs/mining-exemplar/cascade.edn" :edge-mode :top}])
(def gates
  [{:id :zaif-gate :repo :futon3c :path "holes/zaif-cascade-gate.edn"}
   {:id :zaif-gate-holdout :repo :futon3c :path "holes/zaif-cascade-gate-holdout.edn"}
   {:id :cascade-authority-gate :repo :futon2 :path "holes/cascade-authority-gate.edn"}])
(def emitters
  {:not-exercised-fewer-than-two-members-carry-a-play-grain-rule
   {:class :measured :pointer "futon3c/scripts/zaif_cascade_gate.clj:549-554"}
   :not-exercised-nothing-is-played
   {:class :unconditional
    :pointers ["futon3:checks/construct_cascade.clj:735"
               "futon3:checks/construct_open_cascade.clj:284"
               "futon3:checks/construct_retrodiction_cascade.clj:335"]}
   :not-exercised-no-authority-gate-and-alfworld-is-not-installed
   {:class :unconditional :pointer "futon3:checks/construct_alfworld_cascade.clj:352"}})

(def read-source
  (memoize (fn [{:keys [repo path]}]
             (if (= repo :futon3)
               (:out (sh/sh "git" "-C" (str root "/futon3") "show" (str pin ":" path)))
               (slurp (str root "/" (name repo) "/" path))))))
(def parse-source (memoize (fn [d] (edn/read-string (read-source d)))))
(defn pointer-prefix [{:keys [repo path]}] (str (name repo) ":" path ":"))
(defn lines [s] (str/split-lines s))
(defn matching-lines [s re]
  (mapv (comp inc first) (filter #(re-find re (second %)) (map-indexed vector (lines s)))))
(defn walk [x]
  (letfn [(go [v p]
            (lazy-seq
             (cons [p v]
                   (cond (map? v) (mapcat (fn [[k z]] (go z (conj p k))) v)
                         (vector? v) (mapcat (fn [[i z]] (go z (conj p i))) (map-indexed vector v))
                         :else nil))))]
    (go x [])))
(defn values-at [x k] (filter (fn [[p _]] (= k (last p))) (walk x)))
(defn comparable? [o]
  (boolean (some (fn [[_ v]]
                   (and (map? v) (map? (:precedence-before v)) (map? (:precedence-after v))
                        (= (set (keys (:precedence-before v))) (set (keys (:precedence-after v))))) )
                 (walk o))))
(defn pstr [p] (pr-str p))
(defn pointers-for [d text re]
  (mapv #(str (pointer-prefix d) %) (matching-lines text re)))
(defn o4-fact [d text data]
  (let [xs (values-at data :o4) ps (pointers-for d text #":o4(?:\s|$)")
        vals (mapv second xs) pair? (comparable? data)
        reason (first (filter keyword? vals))]
    {:shape (cond pair? :exercised-with-before-after-pair
                  (seq xs) :not-exercised :else :absent)
     :recorded-reason (or reason "not found")
     :placement (cond (empty? xs) :absent
                      (every? #(= 1 (count (first %))) xs) :top-level
                      :else :per-run)
     :key-paths (if (seq xs) (mapv (comp pstr first) xs) ["not found"])
     :pointers (if (seq ps) ps ["not found"])}))
(defn edge-facts [d text data]
  (let [mode (:edge-mode d)
        observations
        (case mode
          :count (for [[p v] (values-at data :cascade-edges)
                       :when (and (= 3 (count p)) (= :runs (first p)))]
                   {:key-path (pstr p) :count v})
          :nested (for [[p v] (values-at data :edges)
                        :when (and (= :cascade (nth p (- (count p) 2) nil))
                                   (some #{:runs} p))]
                    {:key-path (pstr p) :count (count v)})
          :top (for [[p v] (values-at data :edges) :when (= p [:edges])]
                 {:key-path (pstr p) :count (count v)})
          :none [])
        line-re (if (= mode :count) #"^   :cascade-edges(?:\s|$)" #":edges(?:\s|$)")
        ps (pointers-for d text line-re)
        counter (for [[p v] (values-at data :edges-if-the-prior-relation-were-used)]
                  {:key-path (pstr p) :count v})]
    {:per-run (mapv #(assoc %1 :pointer (or %2 "not found")) observations ps)
     :counterfactual (mapv #(assoc %1 :pointer (or %2 "not found")) counter
                           (pointers-for d text #":edges-if-the-prior-relation-were-used(?:\s|$)"))}))
(defn record-fact [d]
  (let [text (read-source d) data (parse-source d) o4 (o4-fact d text data)
        edges (edge-facts d text data) pair? (comparable? data)
        any-edge? (some pos? (map :count (:per-run edges)))]
    {:record-id (:id d) :source (str (name (:repo d)) ":" (:path d))
     :o4-shape o4
     :has-before-after-pair? pair?
     :before-after-key-path (or (some (fn [[p v]] (when (and (map? v) (:precedence-before v)
                                                              (:precedence-after v)) (pstr p))) (walk data))
                                "not found")
     :cascade-edge-counts edges
     :carries-both? (boolean (and pair? any-edge?))}))
(defn gate-fact [d]
  (let [text (read-source d) data (parse-source d)]
    {:record-id (:id d) :source (str (name (:repo d)) ":" (:path d))
     :o4-locations (mapv (comp pstr first) (values-at data :o4))
     :o4-pointers (let [p (pointers-for d text #":o4(?:\s|$)")] (if (seq p) p ["not found"]))}))

(defn declaration-of [id]
  (first (filter #(= id (:id %)) (concat corpus gates))))
(defn line-at [ptr]
  (let [[repo path line] (str/split ptr #":")
        n (some-> line (str/split #"-") first parse-long)]
    (when (and repo path n)
      (nth (lines (read-source {:repo (keyword repo) :path path})) (dec n) nil))))
(defn grounded? [ptr re]
  (or (= "not found" ptr)
      (boolean (some->> (line-at ptr) (re-find re)))))
(defn pointer-grounding
  "Each recorded pointer is resolved in its own source file and the cited line must
   still carry the key the pointer is recorded under.  This reads the sources, not
   the report, so a shifted or invented pointer fails here rather than only failing
   the comparison with the fresh derivation."
  [r]
  (concat
   (for [row (:records r) p (get-in row [:o4-shape :pointers])] (grounded? p #":o4(?:\s|$)"))
   (for [row (:records r) o (get-in row [:cascade-edge-counts :per-run])]
     (grounded? (:pointer o) #":(?:cascade-)?edges(?:\s|$)"))
   (for [row (:records r) o (get-in row [:cascade-edge-counts :counterfactual])]
     (grounded? (:pointer o) #":edges-if-the-prior-relation-were-used(?:\s|$)"))
   (for [row (:gate-records r) p (:o4-pointers row)] (grounded? p #":o4(?:\s|$)"))))
(defn counts-agree-with-source?
  "The recorded edge counts are recomputed from the record's own bytes."
  [row]
  (if-let [d (declaration-of (:record-id row))]
    (= (mapv :count (get-in row [:cascade-edge-counts :per-run]))
       (mapv :count (:per-run (edge-facts d (read-source d) (parse-source d)))))
    false))

(defn valid? [r]
  (and (= 10 (count (:records r))) (= (set (map :id corpus)) (set (map :record-id (:records r))))
       (= 3 (count (:gate-records r)))
       (= false (:any-record-carries-both? r))
       (= :exercised-with-before-after-pair (get-in r [:records 2 :o4-shape :shape]))
       (true? (get-in r [:records 2 :has-before-after-pair?]))
       (every? false? (map :carries-both? (:records r)))
       (= :per-run (get-in r [:records 0 :o4-shape :placement]))
       (= :absent (get-in r [:records 4 :o4-shape :shape]))
       (pos? (apply + (map :count (get-in r [:records 7 :cascade-edge-counts :per-run]))))
       (= :absent (get-in r [:disagreements-with-slice-1 0 :slice-1 :reading]))
       (= :not-exercised (get-in r [:disagreements-with-slice-1 0 :recomputed :reading]))
       (every? #(or (= "not found" %) (re-find #":\d+(?:-\d+)?$" %))
               (concat (mapcat #(get-in % [:o4-shape :pointers]) (:records r))
                       (mapcat :o4-pointers (:gate-records r))))
       (= :recomputed (get-in r [:disagreements-with-slice-1 0 :correct]))
       (every? true? (pointer-grounding r))
       (every? counts-agree-with-source? (:records r))))

(defn base-report []
  (let [rs (mapv record-fact corpus)]
    (sorted-map
     :check :F12-o4-edges-census
     :scope {:records (mapv #(str (name (:repo %)) ":" (:path %)) corpus)
             :gate-records (mapv #(str (name (:repo %)) ":" (:path %)) gates)
             :pointer "futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:8-22"}
     :futon3-pin {:value pin :pointer "futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:6"}
     :records rs
     :gate-records (mapv gate-fact gates)
     :any-record-carries-both? (boolean (some :carries-both? rs))
     :any-record-carries-both-basis (mapv :record-id rs)
     :disagreements-with-slice-1
     [{:record-id :construct
       :slice-1 {:reading :absent :pointer "futon2:holes/labs/wm-contract/runs/F12-organise/00-census.edn:96"}
       :recomputed {:reading :not-exercised
                    :pointers ["futon3:checks/construct-cascade.edn:187"
                               "futon3:checks/construct-cascade.edn:266"]}
       :correct :recomputed
       :basis "run-record-id :construct"}]
     :not-exercised-causes emitters
     :premises
     [{:id :i :holds? true :basis [:ants]
       :pointers ["futon3:checks/ants-cascade.edn:3" "futon3:checks/ants-cascade.edn:79-105"]}
      {:id :ii :holds? true :basis [:construct :snatch]
       :pointers ["futon2:holes/labs/wm-contract/runs/F12-organise/00-census.edn:96"
                  "futon3:checks/construct-cascade.edn:187" "futon3:checks/construct-cascade.edn:266"
                  "futon2:holes/labs/wm-contract/runs/F12-organise/00-census.edn:173"]}
      {:id :iii :holds? true :basis [:retrodiction]
       :pointers ["futon3:checks/retrodiction-cascade.edn:6742-6744"
                  "futon3:checks/retrodiction-cascade.edn:34090"]}
      {:id :iv :holds? true :basis [:zaif :construct :open :retrodiction :alfworld]
       :pointers ["futon3c/scripts/zaif_cascade_gate.clj:549-554"
                  "futon3:checks/construct_cascade.clj:735"
                  "futon3:checks/construct_open_cascade.clj:284"
                  "futon3:checks/construct_retrodiction_cascade.clj:335"
                  "futon3:checks/construct_alfworld_cascade.clj:352"]}]
     :what-an-exemplar-would-need
     {:finding "one recorded cascade must contain a real cascade edge and an O4 before/after precedence pair over the same members"
      :edge-shape-pointer "futon3:checks/retrodiction-cascade.edn:6742-6744"
      :pair-shape-pointer "futon3:checks/ants-cascade.edn:93-102"
      :measured-gate-pointer "futon3c/scripts/zaif_cascade_gate.clj:549-554"
      :pair-shape-scope
      {:recognised-shape "a map :precedence-before and a map :precedence-after over the same key set"
       :pointer "futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:53-57"
       :not-recognised "the one place O4 is measured rather than declared refused emits the pair as two vectors, so a cascade exercised through that gate would be read here as carrying no pair until the shape is widened"
       :not-recognised-pointer "futon3c/scripts/zaif_cascade_gate.clj:555-558"}})))

(defn assoc-plant [r kind]
  (case kind
    :reverse-o4 (assoc-in r [:records 2 :o4-shape :shape] :not-exercised)
    :move-o4 (assoc-in r [:records 0 :o4-shape :placement] :top-level)
    :zero-edge (assoc-in r [:records 7 :cascade-edge-counts :per-run 1 :count] 0)
    :swap-counterfactual (assoc-in r [:records 0 :cascade-edge-counts :per-run 0 :count]
                                  (get-in r [:records 0 :cascade-edge-counts :counterfactual 0 :count]))
    :drop-record (update r :records pop)
    :shift-pointer (assoc-in r [:records 0 :o4-shape :pointers 0]
                             "futon3:checks/construct-cascade.edn:188")
    :count-mode-edge-zero (assoc-in r [:records 0 :cascade-edge-counts :per-run 0 :count] 0)
    :flip-disagreement (assoc-in r [:disagreements-with-slice-1 0 :correct] :slice-1)))
(defn plant-verdict [base planted]
  ;; :verdict is the checker's acceptance of a planted report.  Its second conjunct
  ;; is false for every landed plant by construction -- a mutated report is not the
  ;; report the run derived -- so :verdict alone says no more than :landed? does.
  ;; :caught-by-validity-predicate? is the column that discriminates: it is what
  ;; valid? makes of the planted report on its own, and it is recorded per plant.
  (and (valid? planted) (= planted base)))

(let [base (base-report)
      plants (mapv (fn [k] (let [p (assoc-plant base k)]
                             {:plant k :landed? (not= p base)
                              :verdict (plant-verdict base p)
                              :caught-by-validity-predicate? (not (valid? p))
                              :pointer "futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:210-228"}))
                   [:reverse-o4 :move-o4 :zero-edge :swap-counterfactual :drop-record
                    :shift-pointer :count-mode-edge-zero :flip-disagreement])
      report (assoc base :plants plants
                    :verdict (and (valid? base)
                                  (every? #(and (:landed? %) (false? (:verdict %))
                                                (true? (:caught-by-validity-predicate? %)))
                                          plants)))]
  (when-not (:verdict report)
    (binding [*out* *err*] (pp/pprint report)) (System/exit 1))
  (.mkdirs (.getParentFile (java.io.File. out)))
  (spit out (with-out-str (pp/pprint report))))
