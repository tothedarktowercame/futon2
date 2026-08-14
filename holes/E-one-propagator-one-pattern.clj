;; ONE PROPAGATOR ACTING ON ONE DESIGN PATTERN — the smallest honest exhibit.
;;
;; *** REFUTED 2026-07-16, SAME DAY, BY oxf-claude-2. READ THIS FIRST. ***
;;
;; The exhibit below RUNS, and what it shows is a PUN ON THE NUMBER 8.
;;
;; I wrote "N = the 8 flexiarg slots ... every pattern IS an 8-bit byte — the same
;; shape as a MetaCA genotype, no encoding, no analogy". The last clause is false.
;;
;; MetaCA's 8 is not a count, it is an EXPONENTIAL: N = W_P(P) = Bool³, so index k
;; IS the neighbourhood truth-table-3[k]. That is *why* M-propagators can say "bit
;; positions are NOT interchangeable" and why FREE = {7} names something there.
;; oxf-claude-2 checked it in the Lean while answering: `Fintype.card (Context Pos
;; Bool) = 8`, `card (Nbhd → Bool) = 256` — MetaCA's 8 IS 2³.
;;
;; Flexiarg's 8 is `len(grammar fields)`. It has no such decomposition. The two
;; eights coincide and nothing follows from the coincidence.
;;
;; And the theory PROVES the index cannot supply the meaning: `precompEquiv` plus
;; the conjugacy theorem give a BIJECTION of fixed-point sets under relabelling the
;; index. So |FREE| is invariant while WHICH element is free is not — by
;; construction, not by omission.
;;
;; So caveat (a) below does not dissolve; it is WORSE than I wrote it. I said the
;; problem was my arbitrary slot ORDER. The real problem is that the index set has
;; no structure for an order to be OF. A pun is not a carrier.
;;
;; WHAT WOULD REVIVE IT (oxf-claude-2, and it is a real research step, not a patch):
;; exhibit the slots AS an exponential — find S, P with slots ≅ [S,P]: three binary
;; features generating the eight. If that decomposition exists, FREE is determined
;; and (a) becomes a result. Nobody knows whether it does. That is the open question,
;; and it is much better than the thing I thought I had.
;;
;; Kept, not deleted, because the run is real and the refutation is the finding.
;; Everything below this banner is v0 and should be read as refuted.
;;
;; Joe, 2026-07-16: "Can we exhibit even one propagator acting on a cascade?
;; Or even one propagator acting on one design pattern to keep it really simple?"
;;
;; A propagator needs THREE things. Asking which carrier supplies all three is
;; the whole question:
;;   N : a finite index set        g[s(k)] := nu(g[k])  needs k to range over something
;;   P : a value set               g : N -> P
;;   nu : P -> P                   the value map (MetaCA: NOT on {0,1})
;;   s : N -> N                    the shape map — a FUNCTION, one target per source
;;
;; VERDICT PER CARRIER (checked, 2026-07-16):
;;
;;   MetaCA rule byte   N = 8 neighbourhoods  P = {0,1}   nu = NOT     -> WORKS (the original)
;;   A design pattern   N = 8 SLOTS           P = ?       nu = ?       -> N is FREE, see below
;;   The ants cascade   N = 8 boxes           s = wires?               -> BLOCKED: the wires are
;;                        NOT a function. Measured on cascade-ants.edn: b0 has 4 outgoing wires,
;;                        hp 4, cr 3, pt 2. s : N -> N needs exactly one target per source. So
;;                        the cascade's wiring cannot BE the shape map. (Its FREE = [b0] is real
;;                        and matches its own :free-note — but FREE-by-no-incoming-wire is a
;;                        different construction from FREE = N \ image(s).)
;;
;; THE FIND: a design pattern is ALREADY a function [N -> P] and nobody had to build it.
;; N = the 8 flexiarg slots. Take P = {absent, present} and every pattern IS AN 8-BIT BYTE —
;; the same shape as a MetaCA genotype, no encoding, no analogy. Measured over the 1076-pattern
;; snapshot: only 9 of 256 possible bytes are realised; the modal one is 11111101 (728 of 1076
;; — everything but evidence).
;;
;; So the propagator runs. It is run below, and it behaves exactly as the theory says:
;;   - FREE = N \ image(s) = {7}: the propagator can never WRITE slot 7, only read it.
;;   - The modal byte converges in 5 steps to 01010101 — a FIXED POINT, period 1.
;;     That is M-propagators §1.3's known alternating absorbing state, reproduced here from
;;     the pattern corpus rather than from a CA. And per oxf-claude-2: a carrier WITH a fixed
;;     point SETTLES, which is death. This propagator kills this pattern in five steps.
;;
;; TWO CAVEATS, AND THEY ARE THE POINT — not decoration on a win:
;;
;; 1. "FREE = next-steps" IS AN ARTIFACT OF MY SLOT ORDERING, NOT A DISCOVERY.
;;    I put next-steps at index 7. emacsShift's FREE is {7} for any ordering; WHICH SLOT that
;;    names is my arbitrary choice. It is tempting to read "the propagator can never write
;;    NEXT-STEPS, and NEXT-STEPS is the sign channel of §3" as profound. It is not — not yet.
;;    It becomes a claim only if the slot order is SEMANTIC. In MetaCA it is: M-propagators
;;    §1.4 says "bit positions are NOT interchangeable — position k is the rule's response to
;;    a specific neighbourhood, truth-table-3[k]". The flexiarg grammar does order its slots,
;;    but whether that order is load-bearing or merely conventional is UNKNOWN, and it is the
;;    single question that decides whether any of this is real. See §Q1 below.
;;
;; 2. THE SEMANTICS ARE THIN. P = {absent, present} rewrites slot OCCUPANCY, not content.
;;    "Empty the evidence slot, fill the context slot" is not a design move anyone wants.
;;    This exhibits the MECHANISM honestly and buys no meaning. The meaningful version needs
;;    nu on slot CONTENT — and nu : P -> P is UNDEFINED on free English. Which is the finding
;;    that connects today's two threads: the controlled vocabulary is not adjacent to
;;    propagators-on-patterns, it is their PRECONDITION. No typed P, no nu, no propagator.
;;
;; Run: bb E-one-propagator-one-pattern.clj

(def slots ["if" "however" "then" "because" "conclusion" "context" "evidence" "next-steps"])

;; The 2014 Emacs bug as a shape map (M-propagators §1.1): (goto-char 0) clamps to point-min,
;; so pos 0 and pos 1 both write bit 0 and bit 7 is never written. That IS k |-> max(k-1,0):
;; non-injective at {0,1}, non-surjective missing 7. Measured a priori to within 0.13% over
;; 87,632 instrumented writes.
(defn emacs-shift [k] (max (dec k) 0))
(defn nu [b] (if (= b 1) 0 1))
(defn step [g k] (assoc g (emacs-shift k) (nu (g k))))

(def FREE (vec (remove (set (map emacs-shift (range 8))) (range 8))))

(println "=== the propagator: g[s(k)] := NOT(g[k]),  s(k) = max(k-1,0)")
(println "    image(s) =" (vec (sort (distinct (map emacs-shift (range 8))))))
(println "    FREE = N \\ image(s) =" FREE "->" (mapv slots FREE))
(println "    non-injective: s(0) = s(1) = 0. Two sources write one target BY CONSTRUCTION —")
(println "    which is oxf-claude-2's point: propagators MANUFACTURE contention, not resolve it.")

(def modal [1 1 1 1 1 1 0 1])
(println "\n=== orbit of the corpus's modal pattern byte" (apply str modal) "(728 of 1076 patterns)")
(loop [g modal, seen {}, t 0]
  (let [s (apply str g)]
    (if-let [prev (seen s)]
      (println (format "    t=%-3d %s  <- CYCLE, first seen t=%d, PERIOD %d" t s prev (- t prev)))
      (do (println (format "    t=%-3d %s" t s))
          (when (< t 50) (recur (reduce step g (range 8)) (assoc seen s t) (inc t)))))))

(println "\n=== FREE never moves (that is what FREE means)")
(let [orbit (take 12 (iterate (fn [g] (reduce step g (range 8))) modal))]
  (println "    slot 7 across the orbit:" (vec (map #(nth % 7) orbit)))
  (println "    s never targets 7, so 7 can only be READ. It is the scaffold: the given."))

(println "\n=== WHAT THIS DOES AND DOES NOT SHOW")
(println "    DOES : a design pattern is already [N -> P] with N = 8 slots. A propagator acts")
(println "           on it, has a FREE set, and reaches a fixed point — i.e. it SETTLES, which")
(println "           on oxf-claude-2's reading is death, not success.")
(println "    DOES NOT : mean anything yet. P = occupancy, so this rewrites which slots are")
(println "           filled, not what they say. And FREE naming 'next-steps' is my ordering,")
(println "           not a fact about patterns.")
