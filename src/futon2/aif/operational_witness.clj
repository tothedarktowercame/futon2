(ns futon2.aif.operational-witness
  "Operational (behavioural) witnesses as core.logic relations — the THIRD
   verification layer.

     interface ✓  clean_argcheck   (the spec is a well-formed typed composition)
     structure ✓  build-match      (the build inhabits the composition)
     BEHAVIOUR    a logic relation that holds against the live TRANSITION

   A witness is a relation over (before, event, after). Run FORWARD it verifies the
   live transition and is ungameable in A3's sense: a goal either succeeds against
   reality or it does not. It can also run BACKWARD (generate) or with a hole
   (explain) where the relation stays simple — but FORWARD/verify is the workhorse.
   This namespace is the register OF RELATIONS."
  (:require [clojure.core.logic :as l]))

(defn ranked-aboveo
  "Relation: mission `m` is ranked strictly above `n` in `order`."
  [order m n]
  (l/fresh [pre post]
    (l/appendo pre (l/lcons m post) order)
    (l/membero n post)))

(defn closure-witnesso
  "The A6/A7 closure behaviour AS A RELATION: a discharge of capability `c`
   (produced by mission `m`) inverts `m` below some `n` in the ranking. Holds iff
   the discharge moved the recommendation — E-pur-si-muove, witnessed, with the
   explaining inversion (d c m n) bound."
  [discharges produces before-order after-order d c m n]
  (l/all
   (l/membero [d c] discharges)        ; d discharges capability c
   (l/membero [m c] produces)          ; mission m produces c
   (ranked-aboveo before-order m n)    ; m above n BEFORE the discharge
   (ranked-aboveo after-order n m)))   ; n above m AFTER (m dropped) — the move

(defn witness-closure
  "Run the closure witness FORWARD against observed facts. Returns the witnessing
   bindings `[[d c m n] …]` — non-empty ⇒ the discharge BEHAVED (moved the queue),
   with the explaining inversion; empty ⇒ NO discharge-driven move (the mirror)."
  [{:keys [discharges produces before-order after-order]}]
  (l/run* [d c m n]
    (closure-witnesso discharges produces before-order after-order d c m n)))

;; --- the standing register (a register OF RELATIONS) ---

(def register
  "Named operational witnesses. Each `:run` takes an observation map and returns
   witnessing bindings (`[]` ⇒ behaviour absent). Re-run continuously to detect
   drift — a behaviour that used to hold and now does not is a recorded event."
  {:closure {:desc "a discharge inverts the ranking (A6/A7 E-pur-si-muove)"
             :run  witness-closure}})

(defn run-register
  "Re-run every registered witness against the current observation. Returns a
   per-witness `{:witnessed? :bindings :desc}` — the standing operational check."
  [observation]
  (into {}
        (for [[nm {:keys [run desc]}] register]
          [nm (let [bs (run observation)]
                {:witnessed? (boolean (seq bs))
                 :bindings   (vec bs)
                 :desc       desc})])))
