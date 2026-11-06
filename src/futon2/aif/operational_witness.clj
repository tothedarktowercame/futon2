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

;; --- experiment artifact reproduction -------------------------------------

(defn experiment-run-key
  "The non-collapsing identity of one experiment run. `seed` is the complete
   seed triple, not the historically ambiguous food seed alone. Factorial
   experiments also include the grid cell so paired seeds may be reused across
   registered environmental levels without collapsing distinct runs."
  ([scenario arm run]
   (experiment-run-key nil scenario arm run))
  ([grid scenario arm run]
   (let [seed {:food-seed (:food-seed run)
               :move-seed (:move-seed run)
               :choice-seed (:choice-seed run)}]
     (if grid
       [grid scenario arm seed]
       [scenario arm seed]))))

(defn artifact-rows
  "Flatten an experiment artifact into `[full-key run-record]` rows."
  [artifact]
  (mapv (fn [[cell run]]
          [(experiment-run-key (:grid cell) (:scenario cell) (:arm cell) run) run])
        (mapcat (fn [cell]
                  (map (fn [run] [cell run]) (:runs cell)))
                (:cells artifact))))

(defn reproduced-runo
  "Relation: two indexed rows have the same full key and run record. Intended
   to run FORWARD; `key` is the witnessing tuple."
  [committed-row rerun-row key]
  (l/fresh [row]
    (l/== [key row] committed-row)
    (l/== [key row] rerun-row)))

(defn- duplicate-keys
  [rows]
  (->> rows
       (map first)
       frequencies
       (keep (fn [[key n]] (when (> n 1) key)))
       vec))

(defn verify-artifact-reproduction
  "Run the artifact relation FORWARD and report missing, changed, or duplicate
   `(scenario, arm, seed-triple)` cells. Exact run records must reproduce."
  [committed-artifact rerun-artifact]
  (let [committed-rows (artifact-rows committed-artifact)
        rerun-rows (artifact-rows rerun-artifact)
        committed-duplicates (duplicate-keys committed-rows)
        rerun-duplicates (duplicate-keys rerun-rows)
        committed-index (into {} committed-rows)
        rerun-index (into {} rerun-rows)
        all-keys (into (set (keys committed-index)) (keys rerun-index))
        mismatches
        (->> all-keys
             (keep (fn [key]
                     (let [committed? (contains? committed-index key)
                           rerun? (contains? rerun-index key)
                           committed (get committed-index key)
                           rerun (get rerun-index key)]
                       (cond
                         (not committed?) {:key key :kind :unexpected-rerun-row
                                           :rerun rerun}
                         (not rerun?) {:key key :kind :missing-rerun-row
                                       :committed committed}
                         (not= committed rerun) {:key key :kind :different-run-record
                                                 :committed committed :rerun rerun}))))
             (sort-by (comp pr-str :key))
             vec)
        matched-keys
        (->> (keys committed-index)
             (keep (fn [key]
                     (first
                      (l/run* [witness-key]
                        (reproduced-runo [key (get committed-index key)]
                                         [key (get rerun-index key)]
                                         witness-key)))))
             vec)
        verified? (and (empty? committed-duplicates)
                       (empty? rerun-duplicates)
                       (empty? mismatches)
                       (= (count committed-rows) (count matched-keys))
                       (= (count rerun-rows) (count matched-keys)))]
    {:verified? verified?
     :committed-row-count (count committed-rows)
     :rerun-row-count (count rerun-rows)
     :matched-row-count (count matched-keys)
     :committed-duplicate-keys committed-duplicates
     :rerun-duplicate-keys rerun-duplicates
     :mismatches mismatches}))

(defn witness-artifact-reproduction
  "Registered witness adapter. A verified reproduction yields its count report;
   a mismatch yields no witness."
  [{:keys [committed-artifact rerun-artifact]}]
  (if (and committed-artifact rerun-artifact)
    (let [report (verify-artifact-reproduction committed-artifact rerun-artifact)]
      (if (:verified? report) [(dissoc report :mismatches)] []))
    []))

;; --- registration conformance (M-experiment-build-match, layer 3) ---------
;;
;; The reviewer's re-run comparison keyed on :food-seed alone and so could
;; compare arbitrary arms. This relation names its tuple — (scenario, arm,
;; seed-triple) — and additionally checks the run artifact's DESIGN against
;; the registration, so a run that drifted from what was registered cannot
;; silently pass.

(defn registered-cello
  "Relation: `key` is a registered `[scenario arm seed]` cell — its (scenario,
  arm) prefix is a declared cell of the registration. Intended to run
  FORWARD; `key` is the witnessing tuple."
  [cells key]
  (l/fresh [scenario arm seed]
    (l/== key [scenario arm seed])
    (l/membero [scenario arm] cells)))

(defn- registration-cells
  [registration]
  (let [design (:clean/experiment-design registration)]
    (set (for [scenario (:scenarios design)
               arm (map :id (:arms design))]
           [scenario arm]))))

(defn verify-registration-conformance
  "Compare a run artifact against the registration it claims to run under.
  Rejects any row whose (scenario, arm) tuple is not registered, any cell
  whose run count differs from the registration's :runs-per-cell, and any
  row carrying a :design that differs from the registered arm's :ablates or
  the registered :environment."
  [{:keys [registration run-artifact]}]
  (let [design (:clean/experiment-design registration)
        cells (registration-cells registration)
        runs-per-cell (get-in design [:seeds :runs-per-cell])
        ablates (into {} (map (juxt :id :ablates)) (:arms design))
        rows (artifact-rows run-artifact)
        row-counts (frequencies (map (fn [[key _]] (vec (butlast key))) rows))
        rejections
        (vec
         (concat
          (keep (fn [[[scenario arm _seed] run]]
                  (when-not (contains? cells [scenario arm])
                    {:key [scenario arm] :kind :unregistered-tuple :run run}))
                rows)
          (keep (fn [[cell n]]
                  (when (and runs-per-cell (not= n runs-per-cell))
                    {:key cell :kind :cell-run-count
                     :registered runs-per-cell :found n}))
                row-counts)
          (keep (fn [[[scenario arm _seed] run]]
                  (let [row-design (:design run)]
                    (when (and (:design run)
                               (or (not= (:ablates row-design) (get ablates arm))
                                   (and (:environment row-design)
                                        (not= (:environment row-design)
                                              (:environment design)))))
                      {:key [scenario arm] :kind :design-differs
                       :registered (get ablates arm) :found (:ablates row-design)})))
                rows)))
        witnessed
        (vec (for [[key _] rows
                   :when (registered-cello (mapv vec cells) key)]
               key))]
    {:verified? (empty? rejections)
     :registered-cell-count (count cells)
     :row-count (count rows)
     :witnessed-tuples witnessed
     :rejections rejections}))

(defn witness-registration-conformance
  "Registered witness adapter. A conforming artifact yields its witness
  report (the verified tuple count); any rejection yields no witness."
  [{:keys [registration run-artifact] :as observation}]
  (if (and registration run-artifact)
    (let [report (verify-registration-conformance observation)]
      (if (:verified? report) [(dissoc report :rejections)] []))
    []))

;; --- the standing register (a register OF RELATIONS) ---

(def register
  "Named operational witnesses. Each `:run` takes an observation map and returns
   witnessing bindings (`[]` ⇒ behaviour absent). Re-run continuously to detect
   drift — a behaviour that used to hold and now does not is a recorded event."
  {:closure {:desc "a discharge inverts the ranking (A6/A7 E-pur-si-muove)"
             :run  witness-closure}
   :artifact-reproduction
   {:desc "a re-run exactly reproduces every full (scenario, arm, seed) cell"
    :run witness-artifact-reproduction}
   :registration-conformance
   {:desc "a run artifact conforms to its registration: every (scenario, arm, seed) cell registered, run counts and arm design unchanged"
    :run witness-registration-conformance}})

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
