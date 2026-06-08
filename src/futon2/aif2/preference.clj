(ns futon2.aif2.preference
  "M-aif2 slice-2 (β): the **playful-precision-prior** — make the EFE
   exploration/info-weight an *inferred* preference rather than a static literal.

   IDENTIFY sibling #3 (`playful-precision-prior`, I3/R7-tier) + MAP Surprise 2
   ('preferences are static data — a lighter (B) locus'). Today the EFE
   exploration weight is a static `def` (v0.13 `info-weight` etc., sourced from
   `war-machine-terminal-vocabulary.edn`); R12 does not infer it. β makes it
   **hidden state inferred from evidence**, where the evidence is the
   **false-floor lull**: a zero-anamnesis / queue-dry state (ΔT≈0) is read not as
   'hand off' but as 'forage at the competence edge' — so the exploration weight
   *rises* when concrete work is scarce and *falls* when it is plentiful.

   This is the SAME aif2 primitive as slice-1, one stratum over: a credited +
   admissibility-gated registry entry whose `:producer` is a value-inference
   (not a candidate-emission). It is:
   - **(B)-not-(A):** it *learns what to value* (a new priority: explore when
     idle), it does not merely enumerate more work.
   - **additive / reducible:** with inference frozen (consent denied) it returns
     the static prior unchanged — `aif` preserved (INV-reduction).
   - **gated:** mutating a preference is a niche-construction write (T3); the
     consent gate keeps it (B), not an always-on heuristic.

   Complements slice-1: the tension-proposer fires at structural bridges; β
   re-weights toward exploration when even the bridges are quiet — covering the
   flat-but-unresolved residue slice-1's coverage bound (M-aif2) leaves open."
  (:require [futon2.aif.intrinsic-values :as iv]))

;; ---------------------------------------------------------------------------
;; Bounds + rate (v0 constants; would themselves be hyperpriors in a later rung)
;; ---------------------------------------------------------------------------

(def ^:const w-min 0.10)   ; busy/exploit floor
(def ^:const w0    0.30)   ; static baseline (the reduced-model value)
(def ^:const w-max 0.90)   ; dry/forage ceiling
(def ^:const eta   0.30)   ; EWMA learning rate

(defn- clamp [x lo hi] (max lo (min hi (double x))))

(defn lull-target
  "The exploration weight the lull *argues for*: a queue-dry lull (lull→1) targets
   forage (w-max); a busy state (lull→0) targets exploit (w-min)."
  [lull]
  (let [l (clamp lull 0.0 1.0)]
    (+ w-min (* (- w-max w-min) l))))

(defn infer-step
  "One online inference step: EWMA of the prior toward the lull-target, clamped
   to [w-min, w-max]. Contraction toward the target (stable)."
  ([prior lull] (infer-step prior lull eta))
  ([prior lull rate]
   (let [p (clamp prior w-min w-max)
         tgt (lull-target lull)
         r (clamp rate 0.0 1.0)]
     (clamp (+ (* (- 1.0 r) p) (* r tgt)) w-min w-max))))

;; ---------------------------------------------------------------------------
;; The S-value registry entry + its admissibility (consent) gate
;; ---------------------------------------------------------------------------

(def preference-entry
  {:id         :s-value/playful-precision
   :stratum    :s-value
   :status     :active
   :provenance {:source "M-aif2 slice-2 (β)" :version 1
                :infers "EFE exploration/info-weight"
                :evidence "false-floor lull (ΔT≈0 / queue-dry)"}})

(def default-consent
  "v0 consent context for mutating a preference (a niche-construction write, T3)."
  {:operator-ratified? true})

(defn admissible?
  "May this entry mutate the preference? Gated: not pruned AND consent grants it.
   Deny ⇒ the preference is frozen at its prior (reduction to static `aif`)."
  [entry consent]
  (boolean
   (and (not= :pruned (:status entry))
        (:operator-ratified? consent))))

;; ---------------------------------------------------------------------------
;; Producer: gated inference of the exploration weight
;; ---------------------------------------------------------------------------

(defn inferred-info-weight
  "The (B)-mechanism: given the entry, consent, the current/prior weight, and the
   false-floor lull signal, return the inferred exploration weight. If the entry
   is inadmissible the prior is returned UNCHANGED (frozen = the static value),
   which is the INV-reduction guarantee. The entry's Beta `:credit` (keyed by
   `:id`) records how much the inference is trusted."
  [entry consent prior lull]
  (if (admissible? entry consent)
    (infer-step prior lull)
    (clamp prior w-min w-max)))

(defn credit
  "Live Beta-credit for this preference-inference entry (per-entry keying, as in
   slice-1). New entry ⇒ Beta(1,1) mode 0.5."
  []
  (iv/credit-for (:id preference-entry)))

(defn reduces-to-static?
  "INV-reduction: with inference frozen (denied), the entry returns the static
   baseline w0 unchanged — `aif` (static preference) is preserved exactly."
  []
  (== w0 (inferred-info-weight preference-entry {:operator-ratified? false} w0 1.0)))
