(ns futon2.materials-catalogue
  "Constructor M0 fixed-byte and quiescent-subshift catalogue
  (T-ladder-04-materials-catalogue).

  A rule byte b names the Wolfram-order elementary map f_b(x) = bit x of b,
  x = 4l+2c+r over the eight 3-bit neighbourhoods.  A bi-infinite binary
  word w is quiescent under b when f_b(neighbourhood) = centre bit at every
  position, i.e. w is a fixed point of the cellular automaton b; the
  quiescent words form a subshift of finite type whose states are the four
  (left,centre) pairs and whose edges are the neighbourhoods with
  f_b(x) = centre(x).

  A writing sigma (a permutation of the eight truth-table positions)
  performs, at source position s, the invert write
  bit[sigma(s)] := not bit[s] (mmca-clj propagate-at with invert? true,
  the genotype-step default).  The fixed set of a writing is the set of
  rule bytes unchanged by that write at every source:

    Fix(sigma) = {b : bit_b(sigma(s)) = 1 - bit_b(s) for all s}

  which is nonempty only when every orbit of sigma is even, and then has
  2^(orbits) members.  The catalogue writings all decompose into the same
  four transpositions (0 1)(2 4)(3 5)(6 7) chained into larger even
  cycles, so their fixed sets nest: the union of all five is the
  16-material set fixed by the transposition skeleton itself, and the
  catalogue stratifies those materials by how strongly the writes pin
  them.  All acceptance numbers of the ticket are properties of these
  sixteen bytes: even8 = Fix((0 1 2 4)(3 5)(6 7)) = the eight named
  materials; the quiescent-subshift entropy of 77 is log2 of the golden
  ratio, of 78 and 141 log2 of the plastic number, of 90 and 178 zero;
  and 113, a member of the 16-material set, has an empty quiescent
  edge set, hence no bi-infinite quiescent word at all."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def bit-count- 8)

(defn rule-bit ^long [^long rule ^long x] (if (pos? (bit-and (bit-shift-right rule x) 1)) 1 0))

(defn fixed-set
  "Rule bytes unchanged by the invert write under `writing` at every
  source position: bit[writing[s]] = not bit[s] for all s."
  [writing]
  {:pre [(= bit-count- (count writing))]}
  (set (for [b (range 256)
             :when (every? (fn [s] (= (rule-bit b (nth writing s))
                                      (- 1 (rule-bit b s))))
                           (range bit-count-))]
         b)))

;; Catalogue writings, all built by chaining the transposition skeleton
;; (0 1)(2 4)(3 5)(6 7).  Order matches the ticket's size list 4/16/8/2/4.
(def catalogue-writings
  {:w4a   [1 2 4 5 0 7 3 6]      ; (0 1 2 4)(3 5 7 6)
   :w16   [1 0 4 5 2 3 7 6]      ; (0 1)(2 4)(3 5)(6 7) the skeleton
   :even8 [1 2 4 5 0 3 7 6]      ; (0 1 2 4)(3 5)(6 7)
   :w2    [1 2 4 5 3 6 7 0]      ; (0 1 2 4 3 5 6 7)
   :w4b   [1 2 4 5 0 6 7 3]})    ; (0 1 2 4)(3 5 6 7)

(def catalogue
  "The fixed-byte catalogue: writing name -> its fixed set of rule bytes."
  (into {} (map (fn [[k w]] [k (fixed-set w)])) catalogue-writings))

(defn materials
  "The union of the catalogue's fixed sets: the sixteen materials."
  []
  (reduce into #{} (vals catalogue)))

(defn quiescent-edges
  "Edges of the quiescent (fixed-point) subshift of rule `b`: the
  (left,centre) -> (centre,right) transitions whose neighbourhood output
  equals the centre bit."
  [b]
  (set (for [l (range 2) c (range 2) r (range 2)
             :when (= (rule-bit b (+ (* 4 l) (* 2 c) r)) c)]
         [[l c] [c r]])))

(defn- adjacency ^doubles [edges]
  (let [idx {[0 0] 0, [0 1] 1, [1 0] 2, [1 1] 3}
        m (double-array 16)]
    (doseq [[from to] edges
            :let [i (get idx from) j (get idx to)]]
      (when-not (and i j) (throw (ex-info "bad edge" {:from from :to to})))
      (aset-double m (+ (* 4 i) j) (inc (aget m (+ (* 4 i) j)))))
    m))

(defn quiescent-entropy
  "Base-2 entropy of the quiescent subshift of rule `b`: log2 of the
  spectral radius of its 4x4 de Bruijn adjacency matrix (power iteration
  on the nonnegative matrix; zero when the subshift is finite, which has
  zero topological entropy)."
  [b]
  (let [m (adjacency (quiescent-edges b))
        rho (loop [v (double-array [1.0 1.0 1.0 1.0]) iter 0]
              (let [w (double-array 4)
                    _ (dotimes [i 4]
                        (dotimes [j 4]
                          (aset-double w i (+ (aget w i)
                                              (* (aget v j)
                                                 (aget m (+ (* 4 i) j)))))))
                    norm (Math/sqrt (reduce + (map #(let [d (aget w %)] (* d d)) (range 4))))
                nv (double-array (map #(/ (aget w %) norm) (range 4)))]
                (if (or (>= iter 200) (zero? norm))
                  norm
                  (recur nv (inc iter)))))]
    (if (> rho 1.0)
      (/ (Math/log rho) (Math/log 2))
      0.0)))

(defn has-bi-infinite-quiescent-word?
  "A bi-infinite path in a finite directed graph exists iff the graph has
  a directed cycle.  True when the quiescent edge set of `b` contains one.
  Iterative DFS with an explicit ancestor list for back-edge detection."
  [b]
  (let [edges (vec (quiescent-edges b))
        succ (reduce (fn [acc [from to]] (update acc from (fnil conj []) to))
                     {} edges)
        starts (distinct (mapcat identity edges))]
    (loop [stack (mapv (fn [n] [n []]) starts)
           done #{}]
      (if (zero? (count stack))
        false
        (let [[node ancestors] (peek stack)]
          (cond
            (some #{node} ancestors) true
            (done node) (recur (pop stack) done)
            :else (let [children (remove (fn [c] (or (done c) (some #{c} ancestors)))
                                         (succ node))]
                    (if (empty? children)
                      (recur (pop stack) (conj done node))
                      (recur (into (pop stack)
                                   (mapv (fn [c] [c (conj ancestors node)]) children))
                             done)))))))))
