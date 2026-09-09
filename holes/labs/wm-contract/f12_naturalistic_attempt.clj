;; Run from futon3: bb -cp checks ../futon2/holes/labs/wm-contract/f12_naturalistic_attempt.clj
;; Offline construction only; does not call the WM, play rules, or change the library.
(require '[construct-cascade :as c]
         '[find-organise :as fo]
         '[clojure.java.shell :as sh]
         '[clojure.string :as str]
         '[clojure.pprint :as pp])

(defn git-head []
  (let [{:keys [exit out err]} (sh/sh "git" "rev-parse" "HEAD")]
    (when-not (zero? exit) (throw (ex-info err {:exit exit})))
    (str/trim out)))

(def o4-fields [:precedence-before :precedence-after
                :acting-order-before :acting-order-after :score-before :score-after])

(defn o4-evidence [cascade]
  (let [missing (filterv #(not (contains? cascade %)) o4-fields)
        moved? (not= (:precedence-before cascade) (:precedence-after cascade))]
    (sorted-map
     :missing-fields missing
     :precedence-moved? moved?
     :law-predicate (boolean (fo/o4-precedence-governance cascade))
     :exercised? (and (empty? missing) moved?
                      (boolean (fo/o4-precedence-governance cascade))))))

(let [head (git-head)
      checked (c/require-pass! (c/report))
      repo (fo/read-repository c/library-root (c/library-sections c/library-root)
                              {:kinds #{:why}})
      related-repo (fo/read-repository c/library-root (c/library-sections c/library-root)
                                      {:kinds #{:why :how}})
      ctx (assoc (c/seed-and-candidates repo) :related (c/related-adjacency related-repo))
      rows (into (sorted-map)
                 (for [t c/temperaments
                       :let [final (c/run t ctx)
                             cascade (c/cascade-of final (:seed ctx) repo)]]
                   [(:id t)
                    (sorted-map
                     :selected (vec (sort (:selected cascade)))
                     :admitted (vec (sort (:admitted-by cascade)))
                     :added-by-organise (vec (sort (:added-by-organise cascade)))
                     :nodes (vec (sort (:nodes cascade)))
                     :edges (vec (sort (:edges cascade)))
                     :o1-o3 (into (sorted-map)
                                  (for [[k f] (dissoc fo/organise-laws :O4)]
                                    [k (boolean (f cascade))]))
                     :o4 (o4-evidence cascade)
                     :precedence-fields (select-keys cascade o4-fields)
                     :constructor-record (get-in checked [:runs (:id t) :record]))]))
      blocked (mapv key (filter #(not (get-in (val %) [:o4 :exercised?])) rows))]
  (assert (= head (git-head)) "Repository HEAD moved during measurement")
  (assert (= (c/read-digest repo) (get-in checked [:as-of :read-digest]))
          "Repository input changed between constructions")
  (doseq [[id row] rows]
    (assert (= (:o1-o3 row) (get-in checked [:runs id :laws])))
    (assert (= (count (:nodes row)) (get-in checked [:runs id :cascade-nodes])))
    (assert (= (count (:edges row)) (get-in checked [:runs id :cascade-edges]))))
  (pp/pprint
   (sorted-map
    :run-id "F12-32-naturalistic-construction"
    :futon3-head head
    :as-of (:as-of checked)
    :constructor-gate :pass
    :constructor-controls (:controls checked)
    :rows rows
    :o4-unexercised-rows blocked
    :scope :whole-library-constructor-only
    :exemplar-ready? (empty? blocked)))
  ;; A true implication with a false antecedent is not the commissioned witness.
  (when (seq blocked) (System/exit 2)))
