#!/usr/bin/env bb
(require '[clojure.edn :as edn] '[clojure.string :as str]
         '[clojure.java.shell :as sh] '[clojure.pprint :as pp])

(def root "/home/joe/code")
(def prior-checker (str root "/futon2/holes/labs/wm-contract/f12_o4_edges_constructibility.bb"))
(def prior-artifact (str root "/futon2/holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn"))
(def gate-file (str root "/futon3c/scripts/zaif_cascade_gate.clj"))
(def out (str root "/futon2/holes/labs/wm-contract/runs/F12-organise/22-o4-selected-rule-carriers.edn"))

(defn ls [s] (str/split-lines s))
(def prior-source (slurp prior-checker))
(def pin (second (re-find #"\(def pin \"([0-9a-f]+)\"\)" prior-source)))
(def corpus
  (edn/read-string (second (re-find #"(?s)\(def corpus\s+(\[.*?\])\)\s+\(def gate-path" prior-source))))
(defn source-name [d] (str (name (:repo d)) ":" (:path d)))
(defn read-record [d]
  (if (= :futon3 (:repo d))
    (let [r (sh/sh "git" "-C" (str root "/futon3") "show" (str pin ":" (:path d)))]
      (when-not (zero? (:exit r)) (throw (ex-info "pinned record unavailable" d))) (:out r))
    (slurp (str root "/" (name (:repo d)) "/" (:path d)))))
(defn walk [x]
  (letfn [(go [v p] (lazy-seq (cons [p v]
    (cond (map? v) (mapcat (fn [[k z]] (go z (conj p k))) v)
          (vector? v) (mapcat (fn [[i z]] (go z (conj p i))) (map-indexed vector v))))))]
    (go x [])))
(defn line-of [text needle]
  (some (fn [[i s]] (when (str/includes? s needle) (inc i))) (map-indexed vector (ls text))))

(def gate-text (slurp gate-file))
(def gate-lines (ls gate-text))
(def rule-starts
  (vec (keep (fn [[i s]] (when-let [[_ id] (re-find #"\{:id\s+(:[^\s\]}]+)" s)] [i (keyword (subs id 1))]))
             (map-indexed vector gate-lines))))
(def rules
  (mapv (fn [[pos [i id]]]
          (let [next-i (or (first (nth rule-starts (inc pos) nil)) 260)
                block (subvec (vec gate-lines) i next-i)
                cf-offset (some (fn [[j s]] (when (re-find #":counterfactual\?\s+true" s) j))
                                (map-indexed vector block))]
            {:id id :id-line (inc i) :counterfactual? (boolean cf-offset)
             :counterfactual-line (if cf-offset (+ i cf-offset 1) "not found")
             :id-pointer (str "futon3c:scripts/zaif_cascade_gate.clj:" (inc i))
             :counterfactual-pointer (if cf-offset
                                       (str "futon3c:scripts/zaif_cascade_gate.clj:" (+ i cf-offset 1))
                                       "not found")}))
        (map-indexed vector rule-starts)))
(def selected-ids (set (map :id (remove :counterfactual? rules))))

(defn member-nodes [d]
  (let [text (read-record d) data (edn/read-string text)]
    (for [[p v] (walk data)
          :when (and (map? v)
                     (or (and (= :find (last p)) (sequential? (:selected v)))
                         (and (= :cascade (last p)) (sequential? (:members v)))
                         (and (= :mining-exemplar (:id d)) (empty? p))))]
      (let [members (vec (or (:members v) [])) selected (vec (or (:selected v) []))
            member-list (if (and (empty? members) (= :find (last p))) selected members)]
        {:record-id (:id d) :source (source-name d) :structural-path (pr-str p)
         :member-list-source (if (and (empty? members) (= :find (last p))) :selected :members)
         :members member-list :member-count (count member-list)
         :selected-rule-carriers (vec (filter selected-ids member-list))
         :selected-rule-carrier-count (count (filter selected-ids member-list))
         :meets-precondition-a-at-the-gate-grain? (>= (count (filter selected-ids member-list)) 2)}))))

(def prior (edn/read-string (slurp prior-artifact)))
(def prior-lines (ls (slurp prior-artifact)))
(def prior-flat (vec (for [r (:records prior) row (:member-sets r)]
                       (assoc row :record-id (:record-id r)))))
(def prior-row-lines
  (loop [xs prior-flat cursor (or (line-of (slurp prior-artifact) ":records") 1) out {}]
    (if-let [row (first xs)]
      (let [needle (pr-str (:structural-path row))
            hit (first (for [i (range cursor (count prior-lines))
                             :when (str/includes? (nth prior-lines i) needle)] i))]
        (when-not hit (throw (ex-info "prior row line not found" row)))
        (recur (next xs) (inc hit) (assoc out [(:record-id row) (:structural-path row)] (inc hit))))
      out)))
(defn prior-row [rid path]
  (first (for [r (:records prior) row (:member-sets r)
               :when (and (= rid (:record-id r)) (= path (:structural-path row)))] row)))
(defn prior-pointer [rid path]
  (if-let [n (get prior-row-lines [rid path])]
    (str "futon2:holes/labs/wm-contract/runs/F12-organise/21-o4-edges-constructibility.edn:" n)
    "not found"))
(defn enrich [row]
  (let [old (prior-row (:record-id row) (:structural-path row))
        old-list (vec (or (:rule-table-carriers old) [])) old-count (or (:rule-table-carrier-count old) 0)
        differs (or (not= old-list (:selected-rule-carriers row))
                    (not= old-count (:selected-rule-carrier-count row)))]
    (assoc row :slice-6-carriers old-list :slice-6-carrier-count old-count
           :slice-6-row-pointer (prior-pointer (:record-id row) (:structural-path row))
           :differs-from-slice-6? differs
           :reason-for-the-difference (if differs :slice-6-counted-counterfactual-rules-removed-by-gate
                                         :slice-6-carriers-already-selected-or-empty))))
(def rows (mapv enrich (mapcat member-nodes corpus)))

(def gate-path-reading
  {:selected-rules-pointer "futon3c:scripts/zaif_cascade_gate.clj:412"
   :cascade-rules-pointer "futon3c:scripts/zaif_cascade_gate.clj:413"
   :counterfactual-rules-pointer "futon3c:scripts/zaif_cascade_gate.clj:419"
   :o4-two-pointer "futon3c:scripts/zaif_cascade_gate.clj:444"
   :o4-exercisable-pointer "futon3c:scripts/zaif_cascade_gate.clj:445"
   :o4-emission-pointer "futon3c:scripts/zaif_cascade_gate.clj:549-558"
   :counterfactual-only-can-make-o4-exercisable? false
   :reason :o4-two-and-o4-plays-use-cascade-rules-not-counterfactual-rules
   :x-plays-uses-counterfactual-rules? true})

(defn recomputed []
  {:rules rules :rows (mapv enrich (mapcat member-nodes corpus))
   :headline (boolean (some :meets-precondition-a-at-the-gate-grain? (mapcat member-nodes corpus)))})
(defn pointer-line [p]
  (when-not (= p "not found")
    (let [[repo path span] (str/split p #":") n (parse-long (first (str/split span #"-")))]
      (case repo
        "futon3c" (nth (ls (slurp (str root "/futon3c/" path))) (dec n) nil)
        "futon2" (nth (ls (slurp (str root "/futon2/" path))) (dec n) nil)
        "futon3" (let [r (sh/sh "git" "-C" (str root "/futon3") "show" (str pin ":" path))]
                   (nth (ls (:out r)) (dec n) nil))))))
(defn refusals [report]
  (let [fresh (recomputed) rr (:rows report) fr (:rows fresh)
        reasons (transient [])]
    (doseq [[i row] (map-indexed vector rr)]
      (let [src (nth fr i nil)]
        (when-not src (conj! reasons [:row i :not-in-source]))
        (doseq [k [:members :member-count :selected-rule-carriers :selected-rule-carrier-count
                   :meets-precondition-a-at-the-gate-grain? :slice-6-carriers :slice-6-carrier-count
                   :differs-from-slice-6? :reason-for-the-difference]]
          (when (not= (get row k) (get src k)) (conj! reasons [:row i k :disagrees-with-source])))
        (when-not (every? (set (:members row)) (:selected-rule-carriers row))
          (conj! reasons [:row i :selected-carrier-not-a-member]))
        (when-not (= (:selected-rule-carrier-count row) (count (:selected-rule-carriers row)))
          (conj! reasons [:row i :selected-carrier-count-mismatch]))
        (when-not (and (string? (:slice-6-row-pointer row)) (pointer-line (:slice-6-row-pointer row)))
          (conj! reasons [:row i :slice-6-pointer-does-not-resolve]))))
    (when-not (= (count rr) (count fr)) (conj! reasons [:rows :coverage-mismatch]))
    (when-not (= (:rules report) (:rules fresh)) (conj! reasons [:rules :disagree-with-gate-source]))
    (doseq [r (:rules report)]
      (when-not (str/includes? (or (pointer-line (:id-pointer r)) "") (str (:id r)))
        (conj! reasons [:rule (:id r) :id-pointer-wrong]))
      (when (and (:counterfactual? r)
                 (not (str/includes? (or (pointer-line (:counterfactual-pointer r)) "") ":counterfactual? true")))
        (conj! reasons [:rule (:id r) :counterfactual-pointer-wrong])))
    (when-not (= (:headline report) (:headline fresh)) (conj! reasons [:headline :disagrees-with-source]))
    (when-not (= (:gate-path report) gate-path-reading) (conj! reasons [:gate-path :disagrees-with-source]))
    (persistent! reasons)))
(defn plant [base k]
  (let [kangaroo (first (keep-indexed #(when (and (= :retrodiction (:record-id %2))
                                                   (seq (:slice-6-carriers %2))) %1) (:rows base)))
        zaif (first (keep-indexed #(when (= :zaif (:record-id %2)) %1) (:rows base)))]
    (case k
      :flip-counterfactual (update-in base [:rules 2 :counterfactual?] not)
      :shift-rule-pointer (update-in base [:rules 2 :id-pointer] #(str/replace % #":234$" ":235"))
      :change-count (update-in base [:rows kangaroo :selected-rule-carrier-count] inc)
      :add-nonmember-carrier (update-in base [:rows zaif :selected-rule-carriers] conj :agent/budget-bounds-exploration)
      :drop-member-set (update base :rows pop)
      :erase-real-delta (assoc-in base [:rows kangaroo :differs-from-slice-6?] false))))

(let [base {:check :F12-o4-selected-rule-carriers
            :scope {:corpus-source-pointer "futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:8-19"
                    :records (mapv source-name corpus)}
            :futon3-pin {:value pin :source-pointer "futon2:holes/labs/wm-contract/f12_o4_edges_constructibility.bb:6"}
            :rules rules :rows rows
            :headline (boolean (some :meets-precondition-a-at-the-gate-grain? rows))
            :gate-path gate-path-reading}
      ks [:flip-counterfactual :shift-rule-pointer :change-count :add-nonmember-carrier
          :drop-member-set :erase-real-delta]
      plants (mapv (fn [k] (let [p (plant base k) why (refusals p)]
                              {:plant k :landed? (not= p base)
                               :caught-by-source-grounded-predicate? (boolean (seq why))
                               :refused-for why
                               :pointer "futon2:holes/labs/wm-contract/f12_o4_selected_rule_carriers.bb:147-172"})) ks)
      report (assoc base :plants plants
                    :validity {:source-grounded? true
                               :recomputes-counts-from-record-and-gate-bytes? true
                               :resolves-recorded-pointers? true
                               :pointer "futon2:holes/labs/wm-contract/f12_o4_selected_rule_carriers.bb:112-146"}
                    :verdict (and (empty? (refusals base))
                                  (every? #(and (:landed? %) (:caught-by-source-grounded-predicate? %)) plants)))]
  (when-not (:verdict report) (binding [*out* *err*] (pp/pprint report)) (System/exit 1))
  (.mkdirs (.getParentFile (java.io.File. out)))
  (spit out (with-out-str (pp/pprint (into (sorted-map) report)))))
