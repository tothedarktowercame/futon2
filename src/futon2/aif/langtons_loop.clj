(ns futon2.aif.langtons-loop
  "T-ladder-02-langton-loop: the eight-state synchronous von Neumann CA of
  Langton's 1984 loop, read only from the ticket's pinned inputs
  (holes/tickets/step0/Langtons-Loops.table and .rle, verified against
  PROVENANCE.md before use).

  Every acceptance claim of the ticket is a measurement this namespace can
  take: the table's active row count, the seed's non-quiescent cell count
  (under the declared quiescence threshold, and separately under the wider
  {0 2} stable-state reading), and the generation-151 replication verdict
  judged on canonical (translation/rotation-normalised) representatives
  with digests recorded through a registered, deterministic run record.

  The simulation is a fixed point: no clocks, no randomness — the same
  inputs always yield the same record bytes."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as ai]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

;; ---------------------------------------------------------------- inputs

(def table-path "holes/tickets/step0/Langtons-Loops.table")
(def rle-path "holes/tickets/step0/Langtons-Loops.rle")

(def provenance-sha256
  "The SHA-256 values PROVENANCE.md pins for the two ticket inputs."
  {table-path "83dc6c0d10aecc9717c003e670a67aa81d099e9491da749f90e48484df7c2605"
   rle-path "333134abf0901f026ad9eac3030a799a7e548c83b2301d3cce9989877ee4de41"})

(defn file-sha256
  "SHA-256 of FILE's bytes, as lowercase hex."
  [file]
  (ai/sha256 (slurp (io/file file))))

(defn verify-inputs!
  "Refuse (by thrown ex-info) unless both ticket inputs match the digests
  PROVENANCE.md pins.  The simulation never runs on unpinned bytes."
  []
  (doseq [[path expected] provenance-sha256]
    (let [actual (file-sha256 path)]
      (when-not (= expected actual)
        (throw (ex-info "ticket input does not match PROVENANCE.md"
                        {:kind :input-provenance-mismatch
                         :path path :expected expected :actual actual})))))
  provenance-sha256)

;; ---------------------------------------------------------------- table

(defn- table-rule-lines
  "The active rule rows of the table: non-comment lines that carry a
  six-symbol CNESWC' rule rather than a header directive."
  [text]
  (->> (str/split-lines text)
       (remove str/blank?)
       (remove #(str/starts-with? % "#"))
       (remove #(re-matches #"[a-z_]+:.*" %))
       vec))

(defn expand-rule
  "One base rule row to its four rotate4 images.  CNESWC' orders the
  neighbourhood North, East, South, West; rotation acts on that ring."
  [row]
  (let [[c n e s w c'] (mapv #(Character/digit ^char % 10) row)]
    (loop [ring [n e s w] images [] i 0]
      (if (= i 4)
        images
        (recur [(ring 1) (ring 2) (ring 3) (ring 0)]
               (conj images [c (ring 0) (ring 1) (ring 2) (ring 3) c'])
               (inc i))))))

(defn parse-table
  "Parse the table text into {:active-rows n :base-rows rows :rules m}.
  Active rows are counted from the file itself, never taken from the
  '# rules:' comment."
  [text]
  (let [rows (table-rule-lines text)]
    {:active-rows (count rows)
     :base-rows rows
     :rules (into {}
                  (mapcat (fn [row]
                            (map (fn [[c n e s w c']]
                                   [[c n e s w] c'])
                                 (expand-rule row))))
                  rows)}))

;; ---------------------------------------------------------------- seed

(def ^:private rle-state
  "RLE character -> state: '.' is 0; letters index states from A=1 (B=2,
  D=4, G=7 in this pattern)."
  (into {\. 0 "." 0}
        (map-indexed (fn [i ch] [(str ch) (inc i)]))
        "ABCDEFGHIJKLMNOPQRSTUVWX"))

(defn parse-rle
  "Parse the RLE text into a rectangular vector of vectors of states,
  right-padding ragged rows with quiescent 0."
  [text]
  (let [body (->> (str/split-lines text)
                  (remove #(str/starts-with? % "#"))
                  (remove #(str/starts-with? (str/trim %) "x"))
                  (str/join))
        rows (map (fn [row-text]
                    (mapcat (fn [[_ n ch]]
                              (repeat (Long/parseLong (if (str/blank? n) "1" n))
                                      (get rle-state ch 0)))
                            (re-seq #"(\d*)([A-Za-z.])" row-text)))
                  (str/split body #"\$"))
        width (transduce (map count) max 0 rows)]
    (vec (map #(vec (take width (concat % (repeat 0)))) rows))))

(defn non-quiescent-cells
  "Cells whose state is outside QUIESCENT-STATES.  The ticket's '86
  non-quiescent cells' uses the threshold #{0} (state 0 is the empty
  state); under the wider #{0 2} threshold — state 2 is also stable in
  isolation — the count is reported separately, so the figure is never
  mistaken for a threshold-free property of the seed."
  [grid quiescent-states]
  (count (remove quiescent-states (flatten grid))))

;; ---------------------------------------------------------------- CA

(defn step-grid
  "One synchronous step of the CA on GRID (vector of row vectors) under the
  expanded RULES.  Outside the grid is state 0; a neighbourhood with no
  matching rule yields 0 (Golly rule-table semantics)."
  [rules grid]
  (let [h (count grid) w (count (grid 0))]
    (loop [y 0 out (transient [])]
      (if (= y h)
        (persistent! out)
        (recur (inc y)
               (conj! out
                      (loop [x 0 row (transient [])]
                        (if (= x w)
                          (persistent! row)
                          (recur (inc x)
                                 (conj! row
                                        (get rules
                                             [(get-in grid [y x] 0)
                                              (get-in grid [(dec y) x] 0)
                                              (get-in grid [y (inc x)] 0)
                                              (get-in grid [(inc y) x] 0)
                                              (get-in grid [y (dec x)] 0)]
                                             0)))))))))))

(defn evolve
  "GRID advanced N synchronous steps under RULES."
  [rules grid n]
  (nth (iterate #(step-grid rules %) grid) n))

(defn place-seed
  "SEED embedded in a SIZE x SIZE quiescent grid with its top-left at
  (ORIGIN ORIGIN)."
  [seed size origin]
  (let [blank (vec (repeat size (vec (repeat size 0))))]
    (reduce (fn [g [y x]]
              (assoc-in g [(+ origin y) (+ origin x)] (get-in seed [y x])))
            blank
            (for [y (range (count seed)) x (range (count (seed y)))] [y x]))))

;; ------------------------------------------------------- canonical forms

(defn crop
  "GRID reduced to the bounding box of its non-zero cells, as a vector of
  strings over state digits."
  [grid]
  (let [rendered (mapv #(apply str %) grid)
        live (keep (fn [[i s]] (when (re-find #"[1-7]" s) [i s]))
                   (map-indexed vector rendered))]
    (when (seq live)
      (let [lo (transduce (map #(str/index-of % (first (re-find #"[1-7]" %))))
                          min Long/MAX_VALUE (map second live))
            hi (transduce (map #(str/last-index-of % (last (re-find #"[1-7]*[1-7]" %))))
                          max -1 (map second live))]
        (mapv #(subs % lo (inc hi)) (map second live))))))

(defn rotate-pattern
  "Rotate PATTERN (vector of strings) 90 degrees clockwise."
  [pattern]
  (vec (for [x (range (count (first pattern)))]
         (apply str (for [y (range (dec (count pattern)) -1 -1)]
                      (get-in pattern [y x]))))))

(defn canonical-text
  "The lexicographically least 90-degree rotation of the cropped pattern:
  the canonical representative under the fourfold rotation group (cropping
  already removes translation)."
  [cropped]
  (when (seq cropped)
    (loop [best cropped i 0]
      (if (= i 4)
        best
        (let [r (rotate-pattern best)]
          (recur (if (neg? (compare r best)) r best) (inc i)))))))

(defn canonical-digest
  "SHA-256 of the canonical (translation- and rotation-normalised) form of
  GRID's live cells."
  [grid]
  (ai/sha256 (str/join "\n" (canonical-text (crop grid)))))

(defn find-canonical-copies
  "Every window of GRID whose content equals PATTERN (vector of strings)
  under any of its four rotations, as {:y :x :rotation}.  A hit is a
  recorded equality against the normalised representative, not an
  eyeballed resemblance."
  [grid pattern]
  (when (and (seq pattern) (seq (first pattern)))
    (let [th (count pattern) tw (count (first pattern))
        rendered (mapv #(apply str %) grid)
        variants (loop [vs [pattern] p pattern i 0]
                   (if (= i 3)
                     vs
                     (let [r (rotate-pattern p)]
                       (recur (conj vs r) r (inc i)))))
        vmap (into {} (map-indexed (fn [i v] [(str/join "\n" v) i]) variants))]
    (into []
          (mapcat (fn [y]
                    (keep (fn [x]
                            (when-let [rot (get vmap
                                                (str/join
                                                 "\n"
                                                 (mapv #(subs % x (+ x tw))
                                                       (subvec rendered y (+ y th)))))]
                              {:y y :x x :rotation rot}))
                          (range (- (count rendered) (dec tw))))))
          (range (- (count rendered) (dec th)))))))

(defn window-grid
  "The sub-grid of GRID covering the THxTW window at (Y X), as a grid of
  states (inverse of the string rendering used by the scan)."
  [grid y x th tw]
  (mapv (fn [dy] (subvec (grid (+ y dy)) x (+ x tw))) (range th)))

(defn replication-digests
  "The canonical digest of each daughter window found by
  FIND-CANONICAL-COPIES, recorded so the equality claim is bound to what
  actually ran rather than to prose."
  [grid copies pattern]
  (let [th (count pattern) tw (count (first pattern))]
    (mapv (fn [{:keys [y x]}] (canonical-digest (window-grid grid y x th tw)))
          copies)))

;; ---------------------------------------------------------------- run

(def grid-size 240)
(def origin 60)

(defn seed-facts
  "The measured seed facts: cell counts under both quiescence thresholds."
  [seed]
  {:grid-dims [(count seed) (count (seed 0))]
   :non-quiescent-cells-threshold-nonzero (non-quiescent-cells seed #{0})
   :non-quiescent-cells-threshold-nonzero-and-two (non-quiescent-cells seed #{0 2})
   :quiescence-threshold-statement
   "a cell counts as active when its state differs from 0 (the empty state);
   under the wider {0 2} threshold (state 2 is also stable in isolation)
   the count is reported separately"})

(def altered-rule-output 7)

(defn altered-table
  "The table with ONE deliberately altered transition for the ticket's
  falsifier: base rule 000012 (centre 0, W-neighbour 1 -> 2) has its
  output changed to 7 across all four of its rotate4 images.  This rule
  fires on the seed's sheath boundary from step 1, so the simulation must
  visibly reject the alteration."
  [table]
  (assoc table
         :altered-rule "000012"
         :rules (reduce (fn [rules [c n e s w _c']]
                          (assoc rules [c n e s w] altered-rule-output))
                        (:rules table)
                        (expand-rule "000012"))))

(defn first-divergence
  "The first generation at which the two rule sets evolve GRID differently,
  or nil when they never diverge over N generations."
  [rules-a rules-b grid n]
  (loop [g grid i 0]
    (cond (= i n) nil
          (= (step-grid rules-a g) (step-grid rules-b g))
          (recur (step-grid rules-a g) (inc i))
          :else (inc i))))

(defn run-record
  "The deterministic registered run record for the whole acceptance check.
  No clocks, no randomness: identical inputs yield identical bytes."
  []
  (verify-inputs!)
  (let [table (parse-table (slurp table-path))
        seed (parse-rle (slurp rle-path))
        placed (place-seed seed grid-size origin)
        gen151 (evolve (:rules table) placed 151)
        pattern (crop seed)
        copies (find-canonical-copies gen151 pattern)
        altered (altered-table table)]
    {:schema :wm/langtons-loop-run-record-v1
     :inputs (into {} (for [p [table-path rle-path]]
                        [p {:sha256 (file-sha256 p)}]))
     :table-facts {:active-rows (:active-rows table)
                   :expanded-rules (count (:rules table))}
     :seed-facts (seed-facts seed)
     :replication {:generation 151
                   :canonical-seed-digest (canonical-digest placed)
                   :daughter-copies copies
                   :daughter-canonical-digests
                   (replication-digests gen151 copies pattern)
                   :total-non-quiescent-cells-at-151 (non-quiescent-cells gen151 #{0})}
     :falsifier {:altered-rule (:altered-rule altered)
                 :altered-output altered-rule-output
                 :first-divergence-generation
                 (first-divergence (:rules altered) (:rules table) placed 151)}}))

(defn -main
  "Print the registered run record as EDN (deterministic; safe to retain)."
  []
  (prn (run-record)))
