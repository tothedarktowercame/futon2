(ns futon2.layout-premise
  "Ladder 06 layout premise check (T-ladder-06-layout-premise).

  The premise under test: a rule field laid out from the even8 materials
  77|90|178 (all members of Fix(even8), futon2.materials-catalogue) is a
  fixed configuration for the propagator write, so propagator-only steps
  preserve the field exactly; but mmca-clj `genotype-step` blends
  neighbouring truth tables before the write, so the interface cells
  (77 next to 90, 90 next to 178) leave the fixed set and the field is
  NOT invariant.

  The step mechanics are the mmca-clj Wolfram-order semantics
  (mmca-clj/src/mmca/core.clj: `blend-rule`, `propagate-at`,
  `genotype-step`, `genotype-step-alone`, invert? true), ported here
  because the mmca-clj checkout is outside this delivery's repository
  plan (same precedent as futon2.materials-catalogue, T-ladder-04).
  One deliberate difference: instead of the RNG source tape, every
  function takes the propagator source position per cell as data, so
  the evidence is exhaustive over source positions rather than
  seed-dependent. For a byte in Fix(even8) every source leaves the byte
  unchanged, which is exactly why propagator-only preservation holds for
  any RNG; and the interface blend bytes 95 and 250 are produced before
  any write, so they are source-independent facts of the layout."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.materials-catalogue :as materials]))

(load-identity/register! *ns* *file*)

(def bit-count 8)
(def default-rule 0)
(def wolfram-neighbourhoods
  ["111" "110" "101" "100" "011" "010" "001" "000"])

(defn rule-bits "Rule byte as an MSB-first bit vector (mmca.core/rule-bits)." [rule]
  (mapv #(if (pos? (bit-and (bit-shift-right rule %) 1)) 1 0) (range 7 -1 -1)))

(defn bits-rule "Inverse of `rule-bits` (mmca.core/bits-rule)." [bits]
  (reduce (fn [n bit] (+ (bit-shift-left n 1) bit)) 0 bits))

(defn rule-output "Wolfram ECA lookup for [left centre right] (mmca.core/rule-output)."
  [rule left centre right]
  (if (pos? (bit-and (bit-shift-right rule (+ (* 4 left) (* 2 centre) right)) 1)) 1 0))

(defn blend-rule
  "Neighbour-agreement blend from the 2014 engine, before propagation
  (mmca.core/blend-rule)."
  [predecessor centre successor]
  (let [pb (rule-bits predecessor) cb (rule-bits centre) sb (rule-bits successor)]
    (bits-rule
     (mapv (fn [left middle right]
             (if (= left right) left (rule-output centre left middle right)))
           pb cb sb))))

(defn propagate-at
  "bit[writing[source]] := not bit[source] (mmca.core/propagate-at, invert?)."
  [rule writing source]
  {:pre [(= bit-count (count writing)) (<= 0 source 7)]}
  (let [bits (rule-bits rule)
        value (bit-xor (nth bits source) 1)]
    (bits-rule (assoc bits (nth writing source) value))))

(def layout-materials
  "The 77|90|178 layout materials, all asserted members of Fix(even8)."
  [77 90 178])

(def even8-writing (:even8 materials/catalogue-writings))

(defn interface-blends
  "The blend bytes produced at each interface of the layout. For a
  77|90|178 block layout the first 90 after the 77 block (predecessor 77,
  successor 90) blends to 95 and the last 90 before the 178 block
  (predecessor 90, successor 178) blends to 250. Both are outside
  Fix(even8), so the layout premise fails for the blending dynamics."
  []
  {:left-interface-77-90 (blend-rule 77 90 90)
   :right-interface-90-178 (blend-rule 90 90 178)})

(defn genotype-step
  "One mmca-clj genotype step on `field` under `writing`: blend each cell
  against its original neighbours with zero-rule boundaries, then apply
  the invert propagator write at `source` (per-cell data in place of the
  RNG tape; the blend stage is the interface-evidence stage)."
  ([field writing] (genotype-step field writing (fn [_] 0)))
  ([field writing source]
   (let [width (count field)]
     (mapv (fn [i]
             (let [predecessor (if (zero? i) default-rule (nth field (dec i)))
                   centre (nth field i)
                   successor (if (= i (dec width)) default-rule
                                 (nth field (inc i)))]
               (propagate-at (blend-rule predecessor centre successor)
                             writing (source i))))
           (range width)))))

(defn blended-field
  "The blend stage alone (pre-propagation) of `genotype-step` on `field`:
  exactly the bytes mmca-clj blends before writing, which is where the
  interface blends 95 and 250 live."
  [field]
  (let [width (count field)]
    (mapv (fn [i]
            (blend-rule (if (zero? i) default-rule (nth field (dec i)))
                        (nth field i)
                        (if (= i (dec width)) default-rule (nth field (inc i)))))
          (range width))))

(defn propagator-only-step
  "One mmca-clj `genotype-step-alone` step: apply the invert propagator
  write at `source` to each rule independently, with no blending."
  ([field writing] (propagator-only-step field writing (fn [_] 0)))
  ([field writing source]
   (mapv (fn [i] (propagate-at (nth field i) writing (source i)))
         (range (count field)))))

(defn layout-field
  "A 77|90|178 layout: contiguous blocks of each material (interior
  cells are fixed by construction; the block interfaces are where the
  blending dynamics bites)."
  ([] (layout-field 5))
  ([block-width]
   (vec (concat (repeat block-width 77)
                (repeat block-width 90)
                (repeat block-width 178)))))

(defn probe-report
  "The ladder-06 evidence record: the layout field, the interface blend
  bytes, whether those blends sit outside Fix(even8), whether
  propagator-only steps preserve the field under EVERY source position
  (hence under any RNG tape), and one explicit genotype step showing the
  field is not invariant."
  ([] (probe-report 5))
  ([repeats]
   (let [field (layout-field repeats)
         fixed (materials/fixed-set even8-writing)
         every-source (fn [rule] (set (map #(propagate-at rule even8-writing %)
                                           (range bit-count))))
         blends (interface-blends)]
     {:field field
      :fixed-set-has-layout-materials (boolean (every? fixed layout-materials))
      :interface-blends blends
      :interface-blends-outside-fix (not-any? fixed (vals blends))
      :propagator-only-preserves-every-source
      (and (every? (fn [rule] (= #{rule} (every-source rule))) layout-materials)
           (= field (propagator-only-step field even8-writing (fn [i] (mod i 8)))))
      :genotype-step-field (genotype-step field even8-writing (fn [i] (mod i 8)))
      :genotype-step-invariant (= field (genotype-step field even8-writing (fn [i] (mod i 8))))})))
