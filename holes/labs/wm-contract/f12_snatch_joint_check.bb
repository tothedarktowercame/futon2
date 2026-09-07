#!/usr/bin/env bb
;; F12 slice 8 -- do the snatch CascadeDiff rows carry organised edges and a
;; moved precedence TOGETHER?
;;
;; The rows are COMPUTED, not recorded: futon3:checks/find_organise.clj:603
;; `cascade-diff-table` pairs the `:patterns` policy row of each
;; treatment/disposition with the `:exchange-first` row and organises over the
;; snatch repository, so neither the edges nor the O4 before/after pair appears
;; in any record file.  That is why the record census
;; (runs/F12-organise/20-o4-edges-census.edn:238-249) reads snatch as carrying
;; neither.
;;
;; Nothing here re-spells `organise`, `fast-forward` or the O-laws: every value
;; is derived by loading `find-organise` itself in a subprocess and calling its
;; own functions.  The plants perturb the FIXTURE or the derived rows and
;; re-run the pipeline; none of them writes the verdict's own field.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as sh]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def root "/home/joe/code")
(def futon3 (str root "/futon3"))
(def pin "cdb5e8a56fd907beb6a99f8b88af9de50ff93126")
(def lab (str root "/futon2/holes/labs/wm-contract"))
(def census-path (str lab "/runs/F12-organise/20-o4-edges-census.edn"))
(def fixture-path (str futon3 "/checks/snatch-cascade.edn"))
(def out-path (str lab "/runs/F12-organise/23-snatch-joint.edn"))
(def findings (atom []))
(defn fail! [finding detail] (swap! findings conj (sorted-map :detail detail :finding finding)))

(def head (str/trim (:out (sh/sh "git" "-C" futon3 "rev-parse" "HEAD"))))
(when-not (= pin head) (fail! :futon3-pin-moved {:expected pin :actual head}))

;; --------------------------------------------------------------------------
;; the derivation -- find-organise's own functions, over a fixture we name
;; --------------------------------------------------------------------------

(defn expression [fixture]
  (str "(load-file \"checks/find_organise.clj\") "
       "(require '[clojure.edn :as edn]) "
       "(let [n (find-ns 'find-organise) r #(deref (ns-resolve n %)) "
       "fixture (edn/read-string (slurp \"" fixture "\")) "
       "repo ((r 'read-repository) \"library\" [:snatch]) "
       "rows ((r 'cascade-diff-table) fixture repo) laws (r 'organise-laws) "
       "repro (r 'organise-reproduces-record?)] "
       "(prn (mapv (fn [row] {:row row "
       ":laws (into (sorted-map) (map (fn [[k f]] [k (boolean (f row))]) laws)) "
       ":reproduces-record? (boolean (repro row))}) rows)))"))

(defn derive-rows
  "The six CascadeDiff rows as `find-organise` computes them, over `fixture`."
  [fixture]
  (let [p (sh/sh "bb" "-cp" "checks" "-e" (expression fixture) :dir futon3)]
    (if (zero? (:exit p))
      (edn/read-string (:out p))
      (do (fail! :find-organise-subprocess-failed {:exit (:exit p) :err (:err p)}) []))))

(defn stable-value [x]
  (cond
    (set? x) (vec (sort-by pr-str (map stable-value x)))
    (map? x) (into (sorted-map) (map (fn [[k v]] [k (stable-value v)]) x))
    (sequential? x) (mapv stable-value x)
    :else x))

(defn build-rows
  "One artifact row per derived row.  Every boolean is COMPUTED from the derived
   values; nothing is asserted."
  [derived]
  (vec
   (sort-by :scenario
            (for [{:keys [row laws reproduces-record?]} derived]
              (let [edges (vec (sort-by pr-str (map vec (:edges row))))
                    precedence-moved? (not= (:precedence-before row) (:precedence-after row))]
                (sorted-map
                 :acting-order-moved? (not= (:acting-order-before row) (:acting-order-after row))
                 :carries-edges-and-a-moved-precedence? (boolean (and (seq edges) precedence-moved?))
                 :edge-count (count edges)
                 :edges (stable-value edges)
                 :laws (into (sorted-map) laws)
                 :o4-holds? (boolean (:O4 laws))
                 :precedence-moved? precedence-moved?
                 :reproduces-record? reproduces-record?
                 :scenario (:scenario row)
                 :score-after (:score-after row)
                 :score-before (:score-before row)
                 :score-moved? (not= (:score-before row) (:score-after row))))))))

(def fixture (edn/read-string (slurp fixture-path)))
(def rows (build-rows (derive-rows fixture-path)))

;; --------------------------------------------------------------------------
;; what the record census says about snatch, read from its artifact
;; --------------------------------------------------------------------------

(def census (edn/read-string (slurp census-path)))
(def snatch-census (first (filter #(= :snatch (:record-id %)) (:records census))))
(defn gap-of [snatch]
  (sorted-map
   :before-after-key-path (:before-after-key-path snatch)
   :cascade-edge-key-paths (mapv :key-path (get-in snatch [:cascade-edge-counts :per-run]))
   :carries-both? (:carries-both? snatch)
   :has-before-after-pair? (:has-before-after-pair? snatch)
   :o4-key-paths (get-in snatch [:o4-shape :key-paths])
   :o4-shape (get-in snatch [:o4-shape :shape])
   :pointers ["futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:238-249"
              "futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:8-22"]))
(def census-gap (gap-of snatch-census))

(defn verdict
  "The headline: six rows, each carrying organised edges AND a moved precedence,
   each satisfying all four O-laws through find-organise's own predicates and
   reproducing its record -- beside a record census that saw none of it."
  [rs gap]
  (and (= 6 (count rs))
       (every? :carries-edges-and-a-moved-precedence? rs)
       (every? #(every? true? (vals (:laws %))) rs)
       (every? :reproduces-record? rs)
       (= :absent (:o4-shape gap))
       (false? (:has-before-after-pair? gap))
       (false? (:carries-both? gap))
       (= "not found" (:before-after-key-path gap))
       (empty? (:cascade-edge-key-paths gap))
       (= ["not found"] (:o4-key-paths gap))
       (= "futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:238-249"
          (first (:pointers gap)))))

;; --------------------------------------------------------------------------
;; the plants -- each perturbs an INPUT and re-runs the pipeline
;; --------------------------------------------------------------------------

(defn scenario-rows [fx treatment disposition]
  (filter #(and (= treatment (:treatment %)) (= disposition (:disposition %))) (:scenarios fx)))

(defn write-temp [fx]
  (let [f (java.io.File/createTempFile "f12-slice8-fixture" ".edn")]
    (.deleteOnExit f)
    (spit f (pr-str fx))
    (.getAbsolutePath f)))

(defn flatten-precedence
  "Give the `:exchange-first` row of [:g4 :snatcher] the `:patterns` row's
   precedence, so that row's precedence no longer moves."
  [fx]
  (let [before (first (filter #(= :patterns (:policy %)) (scenario-rows fx :g4 :snatcher)))]
    (update fx :scenarios
            (fn [ss] (mapv #(if (and (= :g4 (:treatment %)) (= :snatcher (:disposition %))
                                     (= :exchange-first (:policy %)))
                              (assoc % :precedence (:precedence before))
                              %)
                           ss)))))

(defn drop-scenario [fx]
  (update fx :scenarios
          (fn [ss] (vec (remove #(and (= :g1 (:treatment %)) (= :cautious (:disposition %))) ss)))))

(def plant-flattened (build-rows (derive-rows (write-temp (flatten-precedence fixture)))))
(def plant-dropped (build-rows (derive-rows (write-temp (drop-scenario fixture)))))
(def plant-edgeless
  (build-rows (assoc-in (derive-rows fixture-path) [0 :row :edges] #{})))

(def g4 (comp first (partial filter #(= [:g4 :snatcher] (:scenario %)))))

(def mutations
  [(sorted-map
    :plant :fixture-precedence-flattened
    :perturbs "the fixture: [:g4 :snatcher] :exchange-first :precedence set to the :patterns row's"
    :landed? (and (:precedence-moved? (g4 rows))
                  (false? (:precedence-moved? (g4 plant-flattened))))
    :verdict-after (verdict plant-flattened census-gap))
   (sorted-map
    :plant :fixture-scenario-dropped
    :perturbs "the fixture: both policy rows of [:g1 :cautious] removed"
    :landed? (and (= 6 (count rows)) (= 5 (count plant-dropped)))
    :verdict-after (verdict plant-dropped census-gap))
   (sorted-map
    :plant :derived-edges-emptied
    :perturbs "the derived rows: the first row's organised edge set emptied before the booleans are computed"
    :landed? (and (pos? (:edge-count (first rows))) (zero? (:edge-count (first plant-edgeless))))
    :verdict-after (verdict plant-edgeless census-gap))
   (sorted-map
    :plant :census-carries-both-flipped
    :perturbs "the census artifact's snatch row: :carries-both? set to true"
    :landed? (false? (:carries-both? census-gap))
    :verdict-after (verdict rows (gap-of (assoc snatch-census :carries-both? true))))
   (sorted-map
    :plant :census-pointer-repointed
    :perturbs "the census pointer: :238-249 repointed to :1"
    :landed? (str/ends-with? (first (:pointers census-gap)) ":238-249")
    :verdict-after (verdict rows (assoc census-gap :pointers
                                        ["futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:1"
                                         (second (:pointers census-gap))])))])

(doseq [m mutations]
  (when-not (and (:landed? m) (false? (:verdict-after m)))
    (fail! :mutation-did-not-fail m)))

(when-not (verdict rows census-gap)
  (fail! :joint-census-verdict-failed {:rows rows :census-gap census-gap}))

;; --------------------------------------------------------------------------

(def rows-both (mapv :scenario (filter :carries-edges-and-a-moved-precedence? rows)))
(def score-moved (mapv :scenario (filter :score-moved? rows)))
(def artifact
  (sorted-map
   :any-row-carries-edges-and-a-moved-precedence? (boolean (seq rows-both))
   :census-gap census-gap
   :findings (vec (sort-by :finding @findings))
   :futon3-pin pin
   :mutation-plants (vec mutations)
   :record :F12-slice-8-snatch-joint-exemplar
   :rows rows
   :rows-carrying-both rows-both
   :rows-total (count rows)
   :rows-where-the-score-moved score-moved
   :source-pointers ["futon3:checks/find_organise.clj:603-633"
                     "futon3:checks/snatch-cascade.edn:84"
                     "futon3:checks/snatch-cascade.edn:210-211"
                     "futon3:checks/find-organise.edn:62-64"]
   :verdict (if (empty? @findings) :pass :fail)))

(io/make-parents out-path)
(spit out-path (with-out-str (pp/pprint artifact)))
(println (format "f12_snatch_joint_check: %s rows=%d both=%d artifact=%s"
                 (name (:verdict artifact)) (count rows) (count rows-both) out-path))
(when (= :fail (:verdict artifact)) (System/exit 1))
