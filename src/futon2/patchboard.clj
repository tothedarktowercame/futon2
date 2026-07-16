(ns futon2.patchboard
  "Pure constraint-shape model for the patch-board visual instrument.

  A wiring is a total vector `f`: source jack k writes the inverse of its
  value to target jack `(f k)`.  Targets may repeat: non-injective maps are
  the load-bearing case because they expose unwritten FREE scaffold jacks.

  `analyse` intentionally agrees with
  futon5/scripts/propagator_constraint_model.py.  In particular, fixed-point
  writers are the displayed UNSAT/motion set and are allowed to range when
  deriving the operational attractor readout."
  (:require [clojure.set :as set]))

(def metaca-terminals
  [{:jack 0 :condition "000"}
   {:jack 1 :condition "001"}
   {:jack 2 :condition "010"}
   {:jack 3 :condition "100"}
   {:jack 4 :condition "011"}
   {:jack 5 :condition "101"}
   {:jack 6 :condition "110"}
   {:jack 7 :condition "111"}])

(defn valid-wiring?
  "True when wiring is a total map [0,n) -> [0,n). Repeated targets are valid."
  [wiring]
  (let [n (count wiring)]
    (and (vector? wiring)
         (pos? n)
         (every? #(and (integer? %) (<= 0 % (dec n))) wiring))))

(defn assert-wiring
  [wiring]
  (when-not (valid-wiring? wiring)
    (throw (ex-info "Wiring must be a non-empty total map into its own jack set"
                    {:wiring wiring})))
  wiring)

(defn identity-wiring [n]
  (vec (range n)))

(defn clamp-shift-wiring [n]
  (mapv #(max (dec %) 0) (range n)))

(defn- propagate-from-free
  [wiring pin]
  (let [n (count wiring)
        image (set wiring)
        free (remove image (range n))
        seeded (reduce #(assoc %1 %2 pin) (vec (repeat n nil)) free)]
    (nth
     (iterate
      (fn [values]
        (reduce
         (fn [next-values source]
           (let [target (wiring source)]
             (if (and (some? (values source))
                      (not= source target)
                      (nil? (next-values target)))
               (assoc next-values target (- 1 (values source)))
               next-values)))
         values
         (range n)))
      seeded)
     n)))

(defn- bit-choices
  [n]
  (if (zero? n)
    [[]]
    (for [prefix (bit-choices (dec n))
          bit [0 1]]
      (conj (vec prefix) bit))))

(defn- bits->integer
  [bits]
  (reduce (fn [value bit] (+ (* 2 value) bit)) 0 bits))

(defn analyse
  "Return the visible FREE/CHAIN/UNSAT shape and derived binary attractors.

  `:pin` fixes every FREE jack to 0 or 1 (default 0).

  The returned sets are sorted vectors for stable UI and test rendering."
  ([wiring] (analyse wiring {}))
  ([wiring {:keys [pin derive-attractors?]
            :or {pin 0 derive-attractors? true}}]
   (assert-wiring wiring)
   (when-not (contains? #{0 1} pin)
     (throw (ex-info "FREE pin must be binary" {:pin pin})))
   (let [n (count wiring)
         image (set wiring)
         free (vec (remove image (range n)))
         unsat (vec (filter #(= % (wiring %)) (range n)))
         propagated (propagate-from-free wiring pin)
         chain (-> (set (keep-indexed #(when (some? %2) %1) propagated))
                   (set/difference (set free) (set unsat))
                   sort
                   vec)
         attractors
         (if derive-attractors?
           (->> (bit-choices (count unsat))
                (keep (fn [bits]
                        (let [candidate (reduce (fn [values [jack bit]]
                                                  (assoc values jack bit))
                                                propagated
                                                (map vector unsat bits))]
                          (when (every? some? candidate)
                            {:bits (apply str candidate)
                             :value (bits->integer candidate)}))))
                distinct
                (sort-by :value)
                vec)
           [])]
     {:size n
      :wiring wiring
      :free free
      :chain chain
      :unsat unsat
      :pin pin
      :attractors attractors})))
