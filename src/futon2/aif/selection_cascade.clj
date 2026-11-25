(ns futon2.aif.selection-cascade
  "Task-selection cascade application, moved unchanged from the deleted
  futon2.aif.outer-score (M-cascade-live-causal step 2b, Joe 2026-11-25
  19:34): the outer-score model/push/score-model track is retired; what
  remains live is the cascade machinery the click still uses -- the gates
  and orderings (apply-selection-step/-cascade/-traced, cascade-pick), the
  serializable step descriptions (typed-step), the placement gate
  (placed-workable?), the versioned kernel constants the reach producer and
  the menu's kernel-input read (kernel-definition, kernel-parameters), and
  the pinned stopping altitude (abstention-altitude) that the selection
  why-renderer pins.

  Behaviour is unchanged: every function body is byte-identical to the
  outer-score original. The refused-kind keywords keep their historical
  :outer-score/... names so existing refusal receipts and tests are
  untouched."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
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

(defn placed-workable?
  "Lean `SelectionStep.gate keep` with keep = this predicate. A fact with no
  :placement key refuses: the adapter was not wired, which is not a placement."
  [facts target]
  (let [fact (value-of facts target)]
    (when-not (contains? fact :placement)
      (throw (ex-info "Placement fact unavailable"
                      {:kind :outer-score/placement-unavailable :target target})))
    (let [p (:placement fact)]
      (boolean (and (map? p) (not= :absent (:status p))
                    (= "open" (:status p))
                    (seq (:ways p))
                    (or (= "work" (:action p))
                        (and (string? (:action p))
                             (str/starts-with? (:action p) "cluster:"))))))))

(defn apply-selection-step
  "Lean `applySelectionStep`: apply one gate or stable ordering using Giraffe facts."
  ([facts field step] (apply-selection-step facts field step nil))
  ([facts field step {:keys [relation-class]}]
  (cond
    (and (map? step) (contains? step :gate))
    (filterv (:gate step) field)

    (and (map? step) (contains? step :order))
    (stable-sort-by (:order step) identity field)

    (= :ready-gate step)
    (filterv #(= :ready (:status (value-of facts (:target %)))) field)

    (= :focus-gate step)
    (if-not (fn? relation-class)
      (throw (ex-info "Focus relation unavailable"
                      {:kind :outer-score/relation-producer-unavailable}))
      (filterv #(= :focus (relation-class (:target %))) field))

    (= :placed-gate step)
    (filterv #(placed-workable? facts (:target %)) field)

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
                    {:kind :outer-score/unknown-selection-step :step step})))))

(defn apply-selection-cascade
  "Lean `applySelectionCascade`: apply selection steps from left to right."
  ([cascade facts field] (apply-selection-cascade cascade facts field nil))
  ([cascade facts field selection-context]
   (reduce #(apply-selection-step facts %1 %2 selection-context) (vec field) cascade)))

(declare typed-step)

(defn- step-value
  [facts step candidate]
  (when candidate
    (case step
      :by-rank (graph-term facts (:target candidate) :rank)
      :by-cone (graph-term facts (:target candidate) :cone)
      :by-unblocks (graph-term facts (:target candidate) :unblocks)
      nil)))

(defn apply-selection-cascade-traced
  "Apply a cascade and retain the deterministic facts that explain each step.
  The returned field is exactly `apply-selection-cascade`'s field."
  ([cascade facts field] (apply-selection-cascade-traced cascade facts field nil))
  ([cascade facts field selection-context]
  (reduce
   (fn [{:keys [field trace]} step]
     (let [next-field (apply-selection-step facts field step selection-context)
           typed (typed-step step)
           ordered? (contains? #{:by-rank :by-cone :by-unblocks} step)
           pick (first next-field)
           runner (second next-field)
           pick-value (step-value facts step pick)
           runner-value (step-value facts step runner)]
       {:field next-field
        :trace (conj trace
                     (if ordered?
                       (cond-> {:step typed :pick-value pick-value}
                         runner (assoc :runner-up {:target (:target runner)
                                                   :value runner-value}
                                       :margin (Math/abs (double (- runner-value pick-value)))))
                       {:step typed :in (count field) :out (count next-field)}))}))
   {:field (vec field) :trace []}
   cascade)))

(defn cascade-pick
  "Lean `cascadePick`: return the first candidate left by the cascade, or nil."
  ([cascade facts field] (cascade-pick cascade facts field nil))
  ([cascade facts field selection-context]
   (first (apply-selection-cascade cascade facts field selection-context))))

(defn typed-step
  "Serializable description of the actual interpreter step. Function steps
  remain named/unverifiable; their printed JVM object identity is not evidence.
  Unknown README prose is described, but apply-selection-step still refuses it."
  ([step] (typed-step step "anonymous-step"))
  ([step fallback-name]
  (cond
    (and (map? step) (contains? step :gate)) {:named (or (:name step) fallback-name)}
    (and (map? step) (contains? step :order)) {:named (or (:name step) fallback-name)}
    (contains? #{:ready-gate :focus-gate} step) step
    (and (map? step) (contains? step :gate-status)) {:status-gate (:gate-status step)}
    (and (map? step) (contains? step :status-gate)) step
    (contains? #{:placed-gate :by-cone :by-unblocks :by-rank} step) step
    :else {:named (if (string? step) step (pr-str step))})))
