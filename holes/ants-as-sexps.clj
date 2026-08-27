;; THE ANT PATTERNS AS S-EXPS — a few examples, serialized to look at.
;;
;; Joe, 2026-07-16. Writing them down found a bug in the vocabulary I proposed
;; two hours ago, so the file leads with that rather than with the pretty ones.
;;
;; ===========================================================================
;; THE BUG: "raise :forage" IS AMBIGUOUS, AND THE AMBIGUITY FLIPS THE MEANING.
;;
;; policy.clj:703 — "sample via softmax over -G/tau". G is a COST. Lower G means
;; MORE likely. So "raise :forage" has two readings that are exact opposites:
;;     raise the ACTION's likelihood  -> forage MORE
;;     raise the action's COST (G)    -> forage LESS
;; Both are ordinary English. The corpus uses both.
;;
;; white-space-scout settles its own case, three ways, all agreeing:
;;   conclusion  : "penalise foraging, and gently bias toward pheromone laying"
;;   THEN        : "raise G-adjustments for :forage, lower them for :return and :pheromone"
;;   pattern_efe.clj:51 : ";; In white space: penalize pointless foraging" -> +0.3 risk
;; The pattern DISCOURAGES foraging on barren cells. "raise G" = raise the cost.
;;
;; But cascade-ants.edn's gist for it reads:
;;   "White space implies scout stance: RAISE :FORAGE, tolerate uncertainty."
;; Under the likelihood reading that inverts the pattern. Under the cost reading
;; it is right. The sentence cannot say which, and a reader cannot tell.
;;
;; MY v0 OVERLAY INHERITED THIS AND DID NOT NOTICE. ants-controlled-vocabulary-v0
;; maps "raise" -> :up with no referent, so its reading
;;   {:forage :up, :return :down, :pheromone :down}
;; is *correct as G-directions* and reads to a human as "forage more, return
;; less" — the precise opposite of the pattern. A controlled vocabulary whose
;; terms are ambiguous is not a controlled vocabulary; it is the same prose with
;; fewer words. THE DIRECTION IS NOT THE TERM. The term is (REFERENT, DIRECTION).
;;
;; This is the strongest argument for the exercise I have found today, and it is
;; the exercise embarrassing itself: the vocabulary's first job was to say what
;; "raise" raises, and v0 skipped it.
;; ===========================================================================

(def ambiguous-v0
  ;; what ants-controlled-vocabulary-v0 says. DO NOT USE.
  '(pattern white-space-scout
     (push (:forage :up) (:return :down) (:pheromone :down))))

(def unambiguous
  ;; the same pattern with the referent named. `cost` is the only referent the
  ;; ants actually have (G), so :up means LESS LIKELY. Reading it aloud now
  ;; matches the conclusion instead of contradicting it.
  '(pattern white-space-scout
     (push (:forage    (cost :up))     ; penalise foraging      — barren cell
           (:return    (cost :down))   ; bias toward returning
           (:pheromone (cost :down))   ; bias toward laying trail
           (:tau       (:up)))))       ; "anneal tau upward" — NOT an action; see below

;; ===========================================================================
;; THE THREE EXAMPLES, IN THE HONEST FORM.
;;
;; Every prescription in the corpus is CONDITIONAL, so the shape is
;;   (condition -> (action -> (cost direction)))
;; which is rho : Sigma -> (Pat -> Pat), not a propagator. The propagator is
;; what rho STAGES. Writing them out is what makes that unarguable — the `when`
;; is right there in the authors' text and there is nowhere in (action -> dir)
;; to put it.
;; ===========================================================================

(def examples
  '[;; ---------------------------------------------------------------- 1
    ;; The clean one. One condition, three pushes, all agreeing with the gist.
    (pattern white-space-scout
      (when (white? obs)                       ; Sigma = the sign, from `white?` detector
        (push (:forage    (cost :up))
              (:return    (cost :down))
              (:pheromone (cost :down))
              (:tau       (:up))))
      (silent :hold))                          ; the prose says nothing about :hold

    ;; ---------------------------------------------------------------- 2
    ;; TWO conditions, and one of them is a STOPPING condition, not a state.
    ;; "until a deposit event fires" is temporal — it is not a predicate on the
    ;; current observation at all. Sigma would have to carry history.
    (pattern cargo-return-discipline
      (when (and (>= cargo 0.05) (cargo-stall-streak? obs))
        (push (:return (cost :up)))            ; "penalise :return" — the gist's word
        (until (deposit-fires?)))              ; <- NOT expressible as (action -> dir)
      (when (resting-on-nest? obs)
        (push (:hold (cost :down))))           ; "slightly boost :hold"
      (bound (:tau (cap 0.65)))                ; <- a BOUND, not a push. See below.
      (silent :forage :pheromone))

    ;; ---------------------------------------------------------------- 3
    ;; The one that breaks (action -> dir) outright: ONE action, BOTH directions,
    ;; under different conditions. No function from actions to directions exists.
    (pattern pheromone-trail-tuner
      (when (and (high? novelty) (low? cargo))
        (push (:pheromone (cost :down))))      ; "bias downward" = lay MORE
      (when (or (near? nest) (reserve-crisis? obs))
        (push (:pheromone (cost :up))))        ; "but penalise it near the nest"
      (push (:tau (:up)))                      ; "anneal tau upward to encourage exploration"
      (silent :hold :forage :return))

    ;; ---------------------------------------------------------------- 4
    ;; Touches NO action. Under N = actions this pattern is EMPTY.
    (pattern hunger-precision-coupling
      (when (hungry? obs)        (push (:tau (:down))))   ; "hungrier ants lower tau"
      (when (home-safe? obs)     (push (:tau (:up))))     ; "home safety lifts tau"
      (when (deficit? reserves)  (push (:tau (:down))))   ; "deficit reserves subtract from tau"
      (silent :hold :forage :return :pheromone))

    ;; ---------------------------------------------------------------- 5
    ;; The identity. @aif-delta is literally {}. cascade-ants calls b0 FREE.
    (pattern baseline-cyber-ant
      (describe the-loop)                      ; its THEN describes, it does not prescribe
      (push))])

;; ===========================================================================
;; WHAT THE SERIALIZATION EXPOSES — four things, none of them cosmetic.
;;
;; 1. THE REFERENT IS PART OF THE TERM. (:forage :up) is not a term; it is a
;;    coin flip. (:forage (cost :up)) is a term. v0's vocabulary was ambiguous
;;    exactly where it mattered and I did not see it until I wrote a `push` and
;;    had to read it aloud.
;;
;; 2. tau IS NOT AN ACTION, and every pattern pushes it. Four of five touch tau;
;;    hunger-coupling touches ONLY tau. So N = [:hold :forage :return :pheromone]
;;    does not cover the corpus. Either N is actions + tau (5 — not a power of 2,
;;    so the exponential question dies there) or tau lives at another level: a
;;    precision ON the choice rather than a choice. The second is almost certainly
;;    right — tau is the softmax temperature, it parameterises the distribution
;;    rather than being an option in it. Which means THE ANT PATTERNS SPLIT INTO
;;    TWO CARRIERS, and pretending otherwise is what made pattern 4 look empty.
;;
;; 3. NOT EVERY PRESCRIPTION IS A PUSH. `cap tau around 0.65` is a BOUND: it does
;;    nothing until tau exceeds it. `until a deposit fires` is TEMPORAL: it is not
;;    a predicate on the current observation. Neither fits (action -> direction),
;;    and v0's overlay mapped "cap" -> :down, which is simply false — I flagged it
;;    as "strained" at the time, which was too kind.
;;
;; 4. ONE ACTION, BOTH DIRECTIONS (pattern 3) is the clean refutation. There is
;;    NO function from actions to directions for pheromone-trail-tuner, so the
;;    carrier cannot be (action -> dir) — not for want of effort, by construction.
;;    The conditions are not decoration on the pushes; they are what individuates
;;    them.
;;
;; So: rho : Sigma -> (Pat -> Pat), and Sigma here is
;;   #{:white :cargo-stalled :resting-on-nest :high-novelty-low-cargo
;;     :near-nest :reserve-crisis :hungry :home-safe :deficit}
;; — nine signs, and NOT a monoid (what is :white * :hungry?). Which is fine, and
;; is the point: N-patterns-as-arrows §1 says Sigma must be a monoid ONLY if it
;; rides the writer, and egoLambda FORBIDS it riding the writer. It belongs in
;; e : W(A) -> Sigma. The refuted lambda and this serialization agree, from
;; opposite ends, about where the sign channel lives.
;; ===========================================================================

(println "=== ants as s-exps — the ambiguity that made this worth doing")
(println "\nv0 (AMBIGUOUS — 'raise :forage' could mean either of two opposite things):")
(clojure.pprint/pprint ambiguous-v0)
(println "\ncorrected (referent named; now agrees with the pattern's own conclusion):")
(clojure.pprint/pprint unambiguous)
(println "\n=== the five, in the honest conditional form:")
(doseq [e examples] (println) (clojure.pprint/pprint e))
(println "\n=== Sigma (the signs the conditions range over):")
(clojure.pprint/pprint '#{:white :cargo-stalled :resting-on-nest :high-novelty-low-cargo
                          :near-nest :reserve-crisis :hungry :home-safe :deficit})
(println "\nnine signs, and no multiplication — so Sigma is NOT a monoid.")
(println "That is only a problem if Sigma rides the writer, and egoLambda forbids it.")
(println "It belongs in e : W(A) -> Sigma. Two independent arguments, same answer.")
