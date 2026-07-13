(ns futon2.aif.fold-llm
  "Impl #2 of the fold interface (`futon2.aif.fold`) — the LLM-TURN fold
   (E-close-the-loop §2, the build axis E-llm-fold names).

       fold : (cascade, circumstance) → {:wiring :coverage-score-delta :policy-holes}

   BUILD axis (what differs from impl #1): an inhabiting agent reads the cascade
   patterns' NL prose (IF / HOWEVER / THEN / BECAUSE / NEXT-STEPS) and the
   circumstance, and CONSTRUCTS a wiring *fitted to this mission* — resolving each
   pattern's HOWEVER, surfacing as policy-holes whatever it can't ground. This is
   the reach the classical rule-table lacks: it folds the contentful library
   patterns (e.g. `f6/self-play-loop`) the rule-table abstains on.

   EVALUATION axis (ΔG): the SHARED `futon2.aif.fold-eval/coverage-score-delta` —
   identical to impl #1 — so the comparison isolates the build.

   DATA is solution-side (the interface names none): impl #2's data is the NL
   pattern library (the `.flexiarg` prose) + the agent's own weights. Both are
   INJECTED (`:prose-fn`, `:turn-fn`) so the core is pure/testable AND so the
   serving JVM never makes an LLM call on a request path.

   INCIDENT-SAFE (2026-06-26): NO LLM call here by default. The agent turn is the
   injected `turn-fn` — produced out-of-process (a bell to an agent, or a recorded
   fold-turn read from escrow). `turn-fn` nil ⇒ no construction ⇒ ΔG nil ⇒ the
   gate abstains; the fold never blocks and spawns nothing."
  (:require [futon2.aif.fold-eval :as fe]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defn fold-prompt
  "PURE: the NL fold instruction an inhabiting agent answers. `proses` is a map
   pattern-id → prose (the NL halo; solution-side, injected). Asks for a
   construction fitted to `circumstance`, resolving each pattern's HOWEVER, with
   explicit policy-holes — never fabricated coverage."
  [cascade circumstance proses]
  (str "FOLD TASK — fold this cascade into a construction (wiring) fitted to the circumstance.\n\n"
       "CIRCUMSTANCE (the mission/sorry the construction must fit):\n  " (pr-str circumstance) "\n\n"
       "CASCADE — the pattern halo. Fold each pattern's THEN, fitted to THIS circumstance,\n"
       "resolving its HOWEVER. A box is a concrete construction step for this mission, NOT the\n"
       "pattern's generic NEXT-STEPS verbatim.\n\n"
       (str/join "\n\n" (for [p cascade]
                          (str "### " p "\n" (str/trim (str (get proses p "(prose unavailable)"))))))
       "\n\nRETURN ONLY EDN of this shape:\n"
       "  {:boxes [{:id <kw> :role <str> :fits-pattern <pattern-id> :addresses-however <str>} ...]\n"
       "   :wires [[<from-id> <to-id>] ...]\n"
       "   :terminals [<kw> ...]\n"
       "   :policy-holes [{:unfolded-pattern <pattern-id-or-nil> :free <str> :why <str>} ...]}\n\n"
       "Surface as a policy-hole EVERYTHING you cannot ground in the prose + circumstance.\n"
       "Do not fabricate coverage — an honest hole is worth more than a hollow box."))

(defn parse-construction
  "PURE: parse an agent turn (EDN string) into a construction map. Returns nil on
   unreadable output (the fold then yields no construction ⇒ the gate abstains)."
  [s]
  (cond
    (map? s) s
    (string? s) (try (edn/read-string s) (catch Exception _ nil))
    :else nil))

(defn construction->wiring
  "PURE: normalize a parsed construction into the `:wiring` shape the interface +
   `fold-eval/coverage` expect (`:boxes`/`:wires`/`:terminals`/`:policy-holes`)."
  [constr]
  {:boxes        (vec (:boxes constr))
   :wires        (vec (:wires constr))
   :terminals    (vec (:terminals constr))
   :policy-holes (vec (:policy-holes constr))})

(defn llm-fold
  "Impl #2 satisfying `futon2.aif.fold`. Opts:
     :turn-fn  (fn [prompt] -> edn-string|map)  — the agent turn (INJECTED;
               out-of-process / recorded). nil ⇒ no construction ⇒ ΔG nil.
     :prose-fn (fn [pattern-id] -> prose-string) — the NL halo source (INJECTED;
               solution-side). nil ⇒ patterns fold blind (prose unavailable).
   Returns {:wiring :coverage-score-delta :policy-holes}. ΔG via the SHARED coverage→rollout
   evaluation (`fold-eval`)."
  ([cascade circumstance] (llm-fold cascade circumstance {}))
  ([cascade circumstance {:keys [turn-fn prose-fn]}]
   (let [proses (into {} (for [p cascade] [p (when prose-fn (prose-fn p))]))
         prompt (fold-prompt cascade circumstance proses)
         constr (when turn-fn (parse-construction (turn-fn prompt)))
         wiring (construction->wiring constr)]
     {:wiring       wiring
      :coverage-score-delta      (fe/coverage-score-delta wiring)
      :policy-holes (:policy-holes wiring)})))
