(ns futon2.aif.outer-score
  "Pure task-selection cascade application and expected-free-energy scoring.

  This namespace follows DarkTower.WarMachine.R20Step. It constructs no field
  and carries no state between clicks; the ruled stopping altitude is read from
  its pinned resource."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity]))

(def ^:dynamic *abstention-altitude-path*
  "Optional file override (tests). When nil the resource is resolved on the
  classpath as `wm/outer-abstention-altitude.edn` (futon2/resources), so the
  serving JVM finds it regardless of its working directory."
  nil)

(def abstention-altitude-resource "wm/outer-abstention-altitude.edn")

(defn- abstention-altitude-bytes
  "Returns [location bytes] or throws. Location is the canonical file path
  when overridden, else the classpath URL string."
  []
  (if *abstention-altitude-path*
    (let [file (io/file *abstention-altitude-path*)]
      [(.getCanonicalPath file) (java.nio.file.Files/readAllBytes (.toPath file))])
    (let [url (io/resource abstention-altitude-resource)]
      (when-not url
        (throw (ex-info "Outer abstention altitude resource is not on the classpath"
                        {:resource abstention-altitude-resource})))
      [(str url) (with-open [in (io/input-stream url)] (.readAllBytes in))])))

(defn abstention-altitude
  "Read and pin Joe's explicit stopping altitude. Absence and malformed data
  refuse rather than silently turning the altitude into a pause."
  []
  (try
    (let [[location bytes] (abstention-altitude-bytes)
          resource (edn/read-string (String. bytes "UTF-8"))]
      (when-not (and (= :wm/outer-abstention-altitude-v1 (:schema resource))
                     (number? (:nats resource))
                     (<= 0 (:nats resource))
                     (string? (:authority resource)) (seq (:authority resource))
                     (string? (:authorized-at resource)) (seq (:authorized-at resource)))
        (throw (ex-info "Outer abstention altitude is malformed" {:location location})))
      {:path location
       :sha256 (identity/sha256 bytes)
       :nats (:nats resource)})
    (catch Exception e
      (throw (ex-info "Outer abstention altitude is not configured"
                      {:kind :abstention-altitude-unconfigured
                       :path (or *abstention-altitude-path* abstention-altitude-resource)} e)))))

(def ^:private endings
  [:focused :related :unrelated :stop-the-line :not-yet-evaluated])
(def ^:private gains [:none :one :several])
(def ^:private states [:doable :blocked])
(def ^:private outcomes (vec (for [ending endings gain gains] [ending gain])))

(def kernel-definition
  "Versioned constants consumed by the scorer (and task reach producer).
  Functional forms are fixed by this version, not executable data."
  {:schema :wm/outer-kernel-parameters-v1 :version 1
   :endings endings :gains gains :states states
   :retraction {:focus :focused :associated :related :useful-elsewhere :unrelated}
   :unknown-relation
   {:form :prior-over-retraction-endings
    ;; Normalised restriction of Joe's c-kit (11/20, 7/20, 1/20) to the
    ;; three retraction endings. This is a pinned kernel parameter, not a
    ;; per-click posterior.
    :prior {:focused 11/19 :related 7/19 :unrelated 1/19}}
   :transition-kernel {:form :reach-on-landing-remainder-on-stopped
                       :stopped [:stop-the-line :none] :stopped-mass 1.0
                       :reach-bounds [0.0 1.0]}
   :discovery-gain {:status :undetermined :gain :none :source :discovery-yields-no-ready}
   :novelty-coefficient 0.5
   :readiness-reach-prior {:ready 3/4 :undetermined 1/2 :not-ready 1/4}})

(defn kernel-parameters
  "Bind the code-consumed constants and this makeup's initializer/rate kits.
  Learned counts remain separately retained state; these are not per-policy q."
  [makeup]
  (assoc kernel-definition :makeup-parameters
         (into (sorted-map) (select-keys makeup [:c-kit :kappa :state-kit :gain-kit
                                                :eta-c :omega-c :eta-state :omega-state
                                                :eta-gain :omega-gain :precision-iterations]))))

(defn- value-of [x k]
  (if (fn? x) (x k) (get x k)))

(defn- stable-sort-by [keyfn direction xs]
  (->> xs
       (map-indexed (fn [index x] [(direction (keyfn x)) index x]))
       (sort-by #(subvec % 0 2))
       (mapv #(nth % 2))))

(defn- graph-term [facts target term]
  (let [fact (value-of facts target) value (get fact term)]
    (when-not (nat-int? value)
      (throw (ex-info "Numeric ranking requires a known graph term"
                      {:kind :outer-score/graph-term-unavailable :target target :term term
                       :graph-terms (:graph-terms fact)
                       :producer (or (get-in fact [:graph-terms :producer]) :dependency-reader-reviewer)})))
    value))

(defn apply-selection-step
  "Lean `applySelectionStep`: apply one gate or stable ordering using Giraffe facts."
  [facts field step]
  (cond
    (and (map? step) (contains? step :gate))
    (filterv (:gate step) field)

    (and (map? step) (contains? step :order))
    (stable-sort-by (:order step) identity field)

    (= :ready-gate step)
    (filterv #(= :ready (:status (value-of facts (:target %)))) field)

    (and (map? step) (or (contains? step :gate-status) (contains? step :status-gate)))
    (filterv #(= (or (:status-gate step) (:gate-status step))
                 (:status (value-of facts (:target %)))) field)

    (= :by-unblocks step)
    (stable-sort-by #(graph-term facts (:target %) :unblocks) - field)

    (= :by-rank step)
    (stable-sort-by #(graph-term facts (:target %) :rank) identity field)

    (= :by-cone step)
    (stable-sort-by #(graph-term facts (:target %) :cone) - field)

    :else
    (throw (ex-info "Unknown outer selection step"
                    {:kind :outer-score/unknown-selection-step :step step}))))

(defn apply-selection-cascade
  "Lean `applySelectionCascade`: apply selection steps from left to right."
  [cascade facts field]
  (reduce (partial apply-selection-step facts) (vec field) cascade))

(defn cascade-pick
  "Lean `cascadePick`: return the first candidate left by the cascade, or nil."
  [cascade facts field]
  (first (apply-selection-cascade cascade facts field)))

(defn- normalize [counts support]
  (let [total (reduce + 0 (map #(double (or (value-of counts %) 0)) support))]
    (when-not (pos? total)
      (throw (ex-info "Outer count row has no positive mass"
                      {:kind :outer-score/nonpositive-count-total})))
    (into {} (map (fn [x] [x (/ (double (or (value-of counts x) 0)) total)]) support))))

(defn- bounded-reach [reach]
  (let [[lo hi] (get-in kernel-definition [:transition-kernel :reach-bounds])]
    (max lo (min hi (double reach)))))

(defn- ready-gain [unblocks]
  (cond (zero? unblocks) :none (= 1 unblocks) :one :else :several))

(defn- stopped-kernel []
  (into {} (for [state states]
             [state (assoc (zipmap outcomes (repeat 0.0))
                           (get-in kernel-definition [:transition-kernel :stopped])
                           (get-in kernel-definition [:transition-kernel :stopped-mass]))])))

(defn- picked-kernel [landing raw-reach]
  (let [reach (bounded-reach raw-reach)
        stopped (get-in kernel-definition [:transition-kernel :stopped])
        row0 (zipmap outcomes (repeat 0.0))
        blocked (assoc row0 stopped (get-in kernel-definition [:transition-kernel :stopped-mass]))
        doable (if (= landing stopped)
                 blocked
                 (assoc row0 landing reach stopped (- 1.0 reach)))]
    {:doable doable :blocked blocked}))

(defn- unknown-relation-kernel [gain raw-reach]
  (let [reach (bounded-reach raw-reach)
        stopped (get-in kernel-definition [:transition-kernel :stopped])
        prior (get-in kernel-definition [:unknown-relation :prior])
        row0 (zipmap outcomes (repeat 0.0))
        doable (reduce-kv (fn [row ending mass]
                            (assoc row [ending gain] (* reach (double mass))))
                          (assoc row0 stopped (- 1.0 reach)) prior)
        blocked (assoc row0 stopped 1.0)]
    {:doable doable :blocked blocked}))

(defn stopped-model
  "The explicit abstention model, independent of any menu cascade."
  []
  {:pick nil :status nil :counts {:doable 0 :blocked 1}
   :q {:doable 0.0 :blocked 1.0} :kernel (stopped-kernel)
   :landing (get-in kernel-definition [:transition-kernel :stopped])})

(defn typed-step
  "Serializable description of the actual interpreter step. Function steps
  remain named/unverifiable; their printed JVM object identity is not evidence.
  Unknown README prose is described, but apply-selection-step still refuses it."
  ([step] (typed-step step "anonymous-step"))
  ([step fallback-name]
  (cond
    (and (map? step) (contains? step :gate)) {:named (or (:name step) fallback-name)}
    (and (map? step) (contains? step :order)) {:named (or (:name step) fallback-name)}
    (= :ready-gate step) :ready-gate
    (and (map? step) (contains? step :gate-status)) {:status-gate (:gate-status step)}
    (and (map? step) (contains? step :status-gate)) step
    (contains? #{:by-cone :by-unblocks :by-rank} step) step
    :else {:named (if (string? step) step (pr-str step))})))

(defn outer-model
  "Lean `outerModel`: construct the doable/blocked prior and picked-task kernel.

  Input is `{:focus :relation :facts :field :state-counts :cascade}`."
  [{:keys [focus relation facts field state-counts cascade]}]
  (if-let [candidate (cascade-pick cascade facts field)]
    (let [fact (value-of facts (:target candidate))
          status (:status fact)
          relation-class (relation focus (:target candidate))
          ending (get (:retraction kernel-definition) relation-class)
          prior? (= :unknown relation-class)
          discovery? (= status (get-in kernel-definition [:discovery-gain :status]))
          gain (when (or ending prior?)
                 (if discovery?
                   (get-in kernel-definition [:discovery-gain :gain])
                   (ready-gain (graph-term facts (:target candidate) :unblocks))))
          landing (cond
                    ending [ending gain]
                    prior? (assoc (get kernel-definition :unknown-relation) :gain gain)
                    :else (get-in kernel-definition [:transition-kernel :stopped]))]
      (cond-> {:pick candidate
       :status status
       :gain-source (if discovery? (get-in kernel-definition [:discovery-gain :source])
                        :giraffe-unblocks)
       :counts (value-of state-counts status)
       :q (normalize (value-of state-counts status) states)
       :kernel (if prior?
                 (unknown-relation-kernel gain (:reach candidate))
                 (picked-kernel landing (:reach candidate)))
       :landing landing}
        prior? (assoc :relation-basis :prior)))
    (stopped-model)))

(defn outer-preference
  "Lean `outerPreferenceKernel`: product of normalized ending and gain counts."
  [ending-counts gain-counts]
  (let [ending (normalize ending-counts endings)
        gain (normalize gain-counts gains)]
    (into {} (for [[e g :as outcome] outcomes]
               [outcome (* (ending e) (gain g))]))))

(defn outer-novelty
  "Lean `outerKernelNovelty` / `outerNovelty`: Fisher-information novelty.

  A model with no pick has zero novelty."
  [{:keys [pick counts kernel]}]
  (if-not pick
    0.0
    (let [a (double (value-of counts :doable))
          b (double (value-of counts :blocked))
          total (+ a b)]
      (when-not (and (pos? a) (pos? b))
        (throw (ex-info "Outer novelty requires positive Beta counts"
                        {:kind :outer-score/nonpositive-state-counts :counts counts})))
      (let [variance (/ (* a b) (* total total (inc total)))
            fisher (reduce + 0.0
                           (for [outcome outcomes
                                 :let [ad (get-in kernel [:doable outcome] 0.0)
                                       ab (get-in kernel [:blocked outcome] 0.0)
                                       prediction (/ (+ (* a ad) (* b ab)) total)]
                                 :when (pos? prediction)]
                             (/ (* (- ad ab) (- ad ab)) prediction)))]
        (* (:novelty-coefficient kernel-definition) variance fisher)))))

(defn push
  "Lean `push q a`: marginalize an outer model's state prior through its kernel."
  [{:keys [q kernel]}]
  (into {} (for [outcome outcomes]
             [outcome (reduce + 0.0
                              (for [state states]
                                (* (q state) (get-in kernel [state outcome] 0.0))))])))

(defn- risk [prediction preference]
  (if (some (fn [[outcome mass]]
              (and (pos? mass) (not (pos? (get preference outcome 0.0)))))
            prediction)
    :inadmissible
    (reduce + 0.0
            (for [[outcome mass] prediction :when (pos? mass)]
              (* mass (Math/log (/ mass (preference outcome))))))))

(defn- entropy [row]
  (- (reduce + 0.0 (for [[_ mass] row :when (pos? mass)]
                         (* mass (Math/log mass))))))

(defn- ambiguity [{:keys [q kernel]}]
  (reduce + 0.0 (for [state states] (* (q state) (entropy (kernel state))))))

(defn score-model
  "Retain the finite-sum terms and prediction for one already-computed model.
  No rounding: G is the same risk + ambiguity - novelty used by outer-score."
  [{:keys [ending-counts gain-counts]} model]
  (let [prediction (push model)
        r (risk prediction (outer-preference ending-counts gain-counts))
        a (ambiguity model)
        n (outer-novelty model)
        stopping? (= prediction (push (stopped-model)))
        altitude-source (when stopping? (abstention-altitude))
        altitude (or (:nats altitude-source) 0)]
    (cond-> {:terms (cond-> {:risk r :ambiguity a :novelty n}
              stopping? (assoc :altitude altitude))
     :altitude-source altitude-source
     :G (if (= :inadmissible r) :inadmissible (+ (- (+ r a) n) altitude))
     :gain-source (:gain-source model)
     :prediction (assoc (select-keys model [:q :landing :status :gain-source])
                        :outcomes prediction)}
      (:relation-basis model)
      (assoc :relation-basis (:relation-basis model)
             :prediction (assoc (select-keys model [:q :landing :status :gain-source
                                                     :relation-basis])
                                :outcomes prediction)))))

(defn abstention-score
  "Score stopping separately over the same preference and ruled altitude;
  never a menu member."
  [input]
  (score-model input (stopped-model)))

(defn outer-score
  "Lean outerScore: risk + ambiguity - novelty, or :inadmissible for positive
  predicted mass at zero preference. Kept as the scalar compatibility API."
  [input]
  (:G (score-model input (outer-model input))))
