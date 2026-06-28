(ns futon2.aif.fold-clean
  "Adapter: a fold's `:wiring` → a CLean (`.clean.edn`) — the L2 STANDARD-VERIFY
   bridge for C-cascade-real (E-darktower-wiring × E-close-the-loop).

   It renders a generated cascade→wiring into the exact EDN shape
   `futon6/scripts/clean_to_lean.py` types against the DarkTower Lean theory, so
   the **standalone 0-sorry render is a STRUCTURAL soundness check** on the fold's
   output: the boxes compose (every `:consumes` wires to an upstream `:produces`
   or a declared hole), the spine is a valid `BV.seq`, the `copar` dual-reading
   coheres (one method-tag ↔ one box, in order), and the discharge polarities are
   valid.

   IT CERTIFIES STRUCTURE, NOT SEMANTIC CORRECTNESS. A 0-sorry render means the
   wiring is a well-typed typed-hole comb — NOT that the cascade is 'right', the
   construction adequate, or ΔG accurate. Its value is a soundness FLOOR that
   rules out malformed / hallucinated wirings (the LLM-fold's failure mode).

   The fold's boxes must be DarkTower-shaped — carry `:id`, `:method`, and the
   typed-hole directions `:consumes`/`:produces` (+ `:hole` or `:discharges`).
   `:wires` may be CLean maps `{:from :to :carries}` or bare `[from to]` pairs
   (then `:carries` is derived from the source box's `:produces`).")

(defn- box->produces [boxes]
  (into {} (for [b boxes :when (:produces b)] [(:id b) (:produces b)])))

(defn- normalize-wire
  "CLean wire {:from :to :carries}. Accepts a map (passthrough) or a [from to]
   pair (derive :carries from the source box's :produces)."
  [produces-by-id w]
  (cond
    (map? w) w
    (sequential? w) (let [[from to] w]
                      {:from from :to to :carries (get produces-by-id from)})
    :else (throw (ex-info "unrenderable wire" {:wire w}))))

(defn fold->clean
  "Pure: a fold `:wiring` + metadata → a CLean map (EDN-ready for clean_to_lean.py).
   `:clean/seq` = one method per box, in order (the spine clean_to_lean checks
   against the boxes — the copar coherence). `meta` carries `:proof` (id),
   optional `:title` / `:source` / `:macro`."
  [wiring {:keys [proof title source macro]}]
  (let [boxes      (vec (:boxes wiring))
        pbid       (box->produces boxes)
        wires      (mapv (partial normalize-wire pbid) (:wires wiring))
        holes-at   (->> boxes (filter :hole) (mapv :id))
        disch-at   (->> boxes (filter :discharges) (mapv :id))]
    {:clean/proof  (str proof)
     :clean/title  (or title (str proof))
     :clean/source (or source {:fold (str proof)})
     :clean/seq    (mapv :method boxes)
     :clean/boxes  boxes
     :clean/wires  wires
     :clean/copar  [{:reading :informal :is :clean/seq}
                    {:reading :formal   :is [:clean/boxes :clean/wires]}]
     :clean/shape  {:macro       (or macro :construct-exploit-discharge)
                    :holes-at    holes-at
                    :discharges-at disch-at
                    :note        "fold-generated wiring (C-cascade-real L2 structural check)"}}))

(defn ->edn
  "Render a CLean map to a `.clean.edn` string (a banner + the map, pprint-ish).
   Keeps keyword namespaces (`:clean/…`) which clean_to_lean.py's `kw` strips."
  [clean]
  (str ";; CLean — fold-generated wiring for " (:clean/proof clean) "\n"
       ";; C-cascade-real L2: STRUCTURAL soundness check vs the DarkTower Lean theory.\n"
       ";; 0-sorry render = well-typed typed-hole comb (NOT semantic correctness).\n\n"
       (binding [*print-namespace-maps* false] (pr-str clean)) "\n"))

(defn carries-resolvable?
  "Quick pre-flight (not the gate — the gate is the Lean render): every wire's
   `:carries` is produced by its `:from` box, and every non-root `:consumes` is
   produced upstream. Returns {:ok? :dangling-carries :dangling-consumes}."
  [wiring]
  (let [boxes    (vec (:boxes wiring))
        pbid     (box->produces boxes)
        produced (set (vals pbid))
        wires    (mapv (partial normalize-wire pbid) (:wires wiring))
        dangling-carries  (remove (fn [w] (= (:carries w) (get pbid (:from w)))) wires)
        dangling-consumes (for [b boxes c (:consumes b)
                                :when (not (contains? produced c))] {:box (:id b) :consumes c})]
    {:ok? (and (empty? dangling-carries) (empty? dangling-consumes))
     :dangling-carries (vec dangling-carries)
     :dangling-consumes (vec dangling-consumes)}))
