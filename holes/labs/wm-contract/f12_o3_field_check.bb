#!/usr/bin/env bb
;; f12_o3_field_check.bb -- `:F12` slice 11.  RECOMPUTES the one measurement the
;; O3 field question turns on, over the recorded cascades, from the records
;; themselves.
;;
;; THE QUESTION.  O3 says organise's edges are exactly the authored
;; fast-forwards.  WHICH SET the fast-forward quantifies over is undetermined
;; and the sources disagree: `Holes.lean:909` quantifies over `.selected`,
;; while `futon3:checks/find_organise.clj:529` and the constructor that builds
;; the edges (`futon3:checks/construct_cascade.clj:415`) quantify over `nodes`.
;; Slice 2 proved the two disagree on the recorded zaif run
;; (`Holes.lean:1034`).  Slice 11 adds a THIRD reading -- fast-forward over the
;; nodes organise did not itself add, `nodes \ addedByOrganise` at `Cascade` and
;; `selected u admittedBy` at `CascadeDiff` -- which refuses to let organise use
;; its own additions as fast-forward endpoints.
;;
;; WHAT SEPARATES THE THIRD READING FROM THE NODE-SET READING is a cascade in
;; which organise ADDED something that an edge then runs through: the two
;; readings are the same proposition exactly when `addedByOrganise n nodes` is
;; empty.  That is a fact about the records, not about Lean, so it is recomputed
;; here rather than asserted in the Lean file or in prose.
;;
;; WHY THE SECOND CHECK IS NOT REDUNDANT.  Finding the set empty on every
;; recorded run would be a fact about the runs that happened.  This script also
;; reads the sole producer and requires that its `:added-by-organise` be a
;; LITERAL empty set, which makes the emptiness a fact about the constructor:
;; no run of it can separate the readings, so the separating witness has to be
;; constructed rather than found.  If that literal is ever replaced by a
;; computed value this check FAILS, because the finding it supports would then
;; be stale.
;;
;; WHAT IT DOES NOT DO.  It takes no ruling on the O3 field, on D1, or on the
;; sorry at `Holes.lean:861`.  It records what the records contain.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F12_CENSUS, F12_FUTON3, F12_OUT.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def futon3 (or (System/getenv "F12_FUTON3") (str home "/code/futon3")))
(def census-path (or (System/getenv "F12_CENSUS")
                     (str lab "/runs/F12-organise/00-census.edn")))
(def out-path (or (System/getenv "F12_OUT") (str lab "/runs/F12-organise/07-o3-field.edn")))

(defn die [m data]
  (binding [*out* *err*] (println "f12-o3-field:" m (pr-str data)))
  (System/exit 1))

;; --------------------------------------------------------------------------
;; 1. the record set, taken from slice 1's census rather than re-invented here
;; --------------------------------------------------------------------------
(def census
  (if (.exists (io/file census-path))
    (edn/read-string (slurp census-path))
    (die "census not found" census-path)))

(def record-paths
  (vec (sort (map :record (:recorded-cascades census)))))

(when (empty? record-paths) (die "census names no records" census-path))

(defn futon3-relative
  "The census writes `futon3/checks/x.edn`; resolve that against the repo root
   under test so a negative control can point the whole scan at a planted tree."
  [p]
  (str futon3 "/" (str/replace-first p #"^futon3/" "")))

;; --------------------------------------------------------------------------
;; 2. every O1-shaped cascade in every record, found by walking the record
;;    rather than by knowing where each producer puts it
;; --------------------------------------------------------------------------
(defn o1-shaped?
  "A map is an O1-shaped cascade when it carries a node set AND an
   organise-added set.  That pair is what the two O3 readings differ over; a
   map carrying only one of them cannot separate them and is not counted."
  [m]
  (and (map? m) (contains? m :nodes) (contains? m :added-by-organise)))

(defn as-set [x] (cond (nil? x) #{} (set? x) x (coll? x) (set x) :else #{x}))

(defn walk-cascades
  "Depth-first, recording the key path so each finding carries where it came
   from.  Returns a vector of [path cascade-map]."
  [x path]
  (cond
    (map? x)
    (into (if (o1-shaped? x) [[path x]] [])
          (mapcat (fn [[k v]] (walk-cascades v (conj path k)))) (sort-by (comp str key) x))

    (sequential? x)
    (into [] (mapcat (fn [[i v]] (walk-cascades v (conj path i))))
          (map-indexed vector x))

    :else []))

(defn cascade-facts [record-path path c]
  (let [nodes (as-set (:nodes c))
        added (as-set (:added-by-organise c))
        overlap (set/intersection nodes added)
        edges (:edges c)
        edge-pairs (when edges (into #{} (map vec) edges))
        touching (when edge-pairs
                   (into (sorted-set)
                         (for [e edge-pairs, v e :when (contains? overlap v)] v)))]
    (sorted-map
     :record record-path
     :at (vec path)
     :node-count (count nodes)
     :added-count (count added)
     ;; the discriminator: the two readings are one proposition exactly when
     ;; no node organise added is in the set the node-set reading quantifies over
     :added-inside-nodes (count overlap)
     :added-disjoint-from-nodes? (empty? overlap)
     :carries-edges? (boolean edges)
     :edge-count (if edge-pairs (count edge-pairs) 0)
     :edges-touching-an-added-node (if touching (count touching) 0)
     ;; a record separates the node-set reading from the no-bootstrap reading
     ;; only if an edge actually runs through a node organise added
     :separates-the-two-readings?
     (boolean (and edge-pairs (seq touching))))))

(def per-record
  (vec (for [p record-paths
             :let [f (futon3-relative p)]]
         (if-not (.exists (io/file f))
           (sorted-map :record p :status :not-found :path f)
           (let [doc (edn/read-string (slurp f))
                 found (walk-cascades doc [])]
             (sorted-map
              :record p
              :status :read
              :o1-shaped-cascades (count found)
              :cascades (vec (sort-by (juxt :at) (map (fn [[path c]] (cascade-facts p path c)) found)))))))))

(def all-cascades (vec (mapcat :cascades per-record)))

;; --------------------------------------------------------------------------
;; 3. the producer, so the emptiness is a fact about the constructor and not
;;    about which runs happen to have been recorded
;; --------------------------------------------------------------------------
(def constructor-path (str futon3 "/checks/construct_cascade.clj"))

(def constructor-lines
  (if (.exists (io/file constructor-path))
    (vec (str/split-lines (slurp constructor-path)))
    (die "constructor not found" constructor-path)))

(def added-field-sites
  (vec (for [[i line] (map-indexed vector constructor-lines)
             :when (str/includes? line ":added-by-organise")]
         (sorted-map :line (inc i) :text (str/trim line)))))

(def added-field-literal?
  ;; the exact shape the finding rests on: the field is written as an empty set
  ;; literal, so no construction this file performs can populate it
  (boolean (some #(re-find #":added-by-organise\s+#\{\}\s*$" (:text %)) added-field-sites)))

;; --------------------------------------------------------------------------
;; 4. verdict
;; --------------------------------------------------------------------------
(def separating (vec (filter :separates-the-two-readings? all-cascades)))
(def non-empty-added (vec (filter #(pos? (:added-count %)) all-cascades)))
(def added-not-in-nodes
  (vec (filter #(and (pos? (:added-count %)) (:added-disjoint-from-nodes? %)) all-cascades)))

(def problems
  (cond-> []
    (some #(= :not-found (:status %)) per-record)
    (conj (into [:records-not-found] (sort (map :record (filter #(= :not-found (:status %)) per-record)))))

    (empty? all-cascades)
    (conj [:no-o1-shaped-cascade-found-in-any-record])

    (empty? added-field-sites)
    (conj [:constructor-does-not-mention-the-field constructor-path])

    (not added-field-literal?)
    (conj (into [:constructor-no-longer-fixes-the-field-to-a-literal-empty-set]
                (map :text added-field-sites)))))

(def result
  (sorted-map
   :slice :F12-slice-11
   :what "which set O3's fast-forward quantifies over: whether any recorded cascade separates the node-set reading from the no-bootstrap reading"
   :basis (sorted-map :census census-path
                      :futon3 futon3
                      :records record-paths)
   :producer (sorted-map
              :file "futon3/checks/construct_cascade.clj"
              :added-by-organise-sites added-field-sites
              :fixed-to-a-literal-empty-set? added-field-literal?)
   :per-record per-record
   :rollup (sorted-map
            :records (count per-record)
            :o1-shaped-cascades (count all-cascades)
            :with-a-non-empty-added-set (count non-empty-added)
            :whose-added-set-is-disjoint-from-its-own-nodes (count added-not-in-nodes)
            :separating-the-two-readings (count separating))
   :separating-cascades separating
   :non-empty-added-sets non-empty-added
   :problems problems))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint result)))
(println "f12-o3-field: wrote" out-path)
(if (seq problems)
  (do (binding [*out* *err*] (println "f12-o3-field: FAIL" (pr-str problems))) (System/exit 1))
  (println "f12-o3-field: PASS --" (count all-cascades) "O1-shaped cascades over"
           (count per-record) "records,"
           (count separating) "separating the node-set reading from the no-bootstrap reading;"
           "producer fixes :added-by-organise to a literal empty set"))
