(ns futon2.aif.checked-problem-map
  "Pure, proof-erased operations for a supplied fixed-universe checked problem
  map.

  Structural closure is not proof authentication.  H-real--that every
  supplied artifact and reduction has a corresponding proof-bearing Lean input
  for one fixed interpretation--is a caller assumption and remains
  UNDISCHARGED here.  This namespace reads no status, label, claim, receipt or
  checker Boolean as evidence; it has no production caller, store, or I/O.

  The executable closure is intended to compute CheckedProblemMap.closedSet.
  Its equivalence to Lean Closed is pending the separate reviewed Lean gate."
  (:refer-clojure :exclude [update])
  (:require [clojure.set :as set]))

(defn- refuse [reason data]
  (throw (ex-info "Invalid checked problem-map skeleton"
                  (assoc data :refusal reason))))

(defn- required-key! [m key reason context]
  (when-not (contains? m key)
    (refuse reason (assoc context :missing key)))
  (get m key))

(defn- declared! [universe id context]
  ;; `contains?`, rather than truthiness, deliberately admits false and nil as
  ;; declared IDs when the fixed universe contains them.
  (when-not (contains? universe id)
    (refuse :undeclared-statement (assoc context :statement id))))

(defn- artifact! [universe artifact context]
  (when-not (map? artifact)
    (refuse :malformed-map (assoc context :value artifact)))
  (let [stmt (required-key! artifact :stmt :malformed-map context)]
    (declared! universe stmt context)))

(defn- reduction! [universe reduction context]
  (when-not (map? reduction)
    (refuse :malformed-map (assoc context :value reduction)))
  (let [head (required-key! reduction :head :malformed-map context)
        tails (required-key! reduction :tails :malformed-map context)]
    (when-not (set? tails)
      (refuse :malformed-map (assoc context :field :tails :value tails)))
    (declared! universe head context)
    (doseq [tail tails]
      (declared! universe tail context))))

(defn validate
  "Validate the supplied fixed-universe skeleton and return it unchanged.

  Required map keys are :universe, :artifacts, :reductions and :obligations.
  IDs may be nil or false when explicitly present in :universe.  Artifacts and
  reductions are vectors to preserve supplied ordering and metadata; closure
  itself is intentionally insensitive to their order and duplication."
  [problem-map]
  (when-not (map? problem-map)
    (refuse :malformed-map {:value problem-map}))
  (let [universe (required-key! problem-map :universe :malformed-map {})
        artifacts (required-key! problem-map :artifacts :malformed-map {})
        reductions (required-key! problem-map :reductions :malformed-map {})
        obligations (required-key! problem-map :obligations :malformed-map {})]
    (when-not (set? universe)
      (refuse :malformed-map {:field :universe :value universe}))
    (when-not (vector? artifacts)
      (refuse :malformed-map {:field :artifacts :value artifacts}))
    (when-not (vector? reductions)
      (refuse :malformed-map {:field :reductions :value reductions}))
    (when-not (set? obligations)
      (refuse :malformed-map {:field :obligations :value obligations}))
    (doseq [obligation obligations]
      (declared! universe obligation {:field :obligations}))
    (doseq [[index artifact] (map-indexed vector artifacts)]
      (artifact! universe artifact {:field :artifacts :index index}))
    (doseq [[index reduction] (map-indexed vector reductions)]
      (reduction! universe reduction {:field :reductions :index index}))
    problem-map))

(defn- checked! [universe checked]
  (when-not (map? checked)
    (refuse :malformed-report {:field :checked :value checked}))
  (let [artifacts (required-key! checked :artifacts :report-checked-absent {})
        reductions (required-key! checked :reductions :report-checked-absent {})]
    (when-not (vector? artifacts)
      (refuse :malformed-report {:field [:checked :artifacts] :value artifacts}))
    (when-not (vector? reductions)
      (refuse :malformed-report {:field [:checked :reductions] :value reductions}))
    (doseq [[index artifact] (map-indexed vector artifacts)]
      (artifact! universe artifact {:field [:checked :artifacts] :index index}))
    (doseq [[index reduction] (map-indexed vector reductions)]
      (reduction! universe reduction {:field [:checked :reductions] :index index}))
    checked))

(defn- closure-step [problem-map closed]
  (let [seeded (into closed (map :stmt (:artifacts problem-map)))]
    (reduce (fn [next-closed {:keys [head tails]}]
              (if (set/subset? tails closed)
                (conj next-closed head)
                next-closed))
            seeded
            (:reductions problem-map))))

(defn closed-set
  "Least structural closure over artifacts and AND-reductions.

  Starts empty, then repeatedly adds all artifact statements and every head
  whose entire tail set was already closed.  Empty tails therefore fire.  An
  unseeded cycle remains open; unrelated components still close."
  [problem-map]
  (let [problem-map (validate problem-map)]
    (loop [closed #{}]
      (let [next-closed (closure-step problem-map closed)]
        (if (= closed next-closed)
          closed
          (recur next-closed))))))

(defn closed?
  "Whether a declared statement is structurally closed; not proof-authenticated."
  [problem-map statement]
  (let [problem-map (validate problem-map)]
    (declared! (:universe problem-map) statement {:field :statement})
    (contains? (closed-set problem-map) statement)))

(defn has-reduction?
  "Whether any supplied reduction names the declared statement as its head."
  [problem-map statement]
  (let [problem-map (validate problem-map)]
    (declared! (:universe problem-map) statement {:field :statement})
    (boolean (some #(= statement (:head %)) (:reductions problem-map)))))

(defn open-obligations
  "Declared obligations not in the structural closure."
  [problem-map]
  (let [problem-map (validate problem-map)]
    (set/difference (:obligations problem-map) (closed-set problem-map))))

(defn admit
  "Append a syntactically valid checked skeleton without changing its fixed
  universe or obligations.  H-real for the appended material is not checked."
  [problem-map checked]
  (let [problem-map (validate problem-map)
        checked (checked! (:universe problem-map) checked)]
    (-> problem-map
        (clojure.core/update :artifacts into (:artifacts checked))
        (clojure.core/update :reductions into (:reductions checked)))))

(defn update
  "Apply a return report structurally.  nil means absent report and returns the
  identical valid map.  A present report must explicitly contain a complete
  :checked skeleton; missing is not an empty checked report.  :label and
  :claimed-closed are carried by callers but deliberately do not affect entry."
  [problem-map report]
  (let [problem-map (validate problem-map)]
    (cond
      (nil? report) problem-map
      (not (map? report)) (refuse :malformed-report {:value report})
      (not (contains? report :checked))
      (refuse :report-checked-absent {:report-keys (set (keys report))})
      :else (admit problem-map (checked! (:universe problem-map) (:checked report))))))

(defn return-obs
  "Structural observation of a supplied scored subset after one report update.
  The report's label and claimed closure have no effect."
  [problem-map scored report]
  (let [problem-map (validate problem-map)]
    (when-not (set? scored)
      (refuse :malformed-map {:field :scored :value scored}))
    (doseq [statement scored]
      (declared! (:universe problem-map) statement {:field :scored}))
    (set/intersection scored (closed-set (update problem-map report)))))
