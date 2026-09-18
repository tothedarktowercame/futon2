(ns futon2.aif.capability-zones
  "Capability-zone membership in the shared BGE embedding space.

  Raw-space distance is plain cosine and is explicitly an :interim-metric.
  The operative pca3-v1 partition uses Euclidean distance tagged
  :interim-metric-3d. The ground metric remains :held by M-substrate-metric;
  this namespace consumes that hold and does not introduce a competing one."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [futon2.aif.forward-model :as fm]))

(def ^:dynamic *seeds-path*
  "/home/joe/code/data/notions/bge_action_class_seeds.json")

(def ^:dynamic *seed-generation*
  "Seed vector generation used for membership: :centroid by default, with
  :text available for reproducible first-generation harvest classification."
  :centroid)

(def ^:dynamic *seed-records*
  "Optional in-memory seeds for batch callers and deterministic fixtures."
  nil)

(def declared-unseeded
  "Action types deliberately OUTSIDE the frozen partition's coverage.

  Declared 2026-09-18 (E09, decision claude-4): `:advance-ticket` entered
  `fm/action-types` after the 2026-07-19 harvest, so the frozen partition
  covers 14 of the 15 action types. The partition is NOT re-harvested to
  close the gap: a re-harvest adds a seed, moves the PCA-3 partition and
  silently re-assigns actions zoned under it — the automatic rebuild E09's
  clause forbids. An unseeded action type zones to nothing; callers asking
  to zone one get a typed refusal (see `zone-of-action`), never a
  nearest-seed guess."
  #{:advance-ticket})

(defn cosine
  "Plain cosine similarity between equal-length numeric vectors."
  [a b]
  (when-not (= (count a) (count b))
    (throw (ex-info "Embedding dimensions differ"
                    {:left (count a) :right (count b)})))
  (let [[dot aa bb]
        (reduce (fn [[dot aa bb] [x y]]
                  (let [x (double x) y (double y)]
                    [(+ dot (* x y)) (+ aa (* x x)) (+ bb (* y y))]))
                [0.0 0.0 0.0]
                (map vector a b))]
    (if (or (zero? aa) (zero? bb))
      0.0
      (/ dot (Math/sqrt (* aa bb))))))

(defn- read-seeds []
  (let [file (io/file *seeds-path*)]
    (when-not (.isFile file)
      (throw (ex-info "Capability-zone seed file is unavailable"
                      {:path *seeds-path*})))
    (json/parse-string (slurp file) true)))

(defn seeded-classes
  "Classes actually present in the current seed records."
  []
  (set (map (comp keyword :class) (or *seed-records* (read-seeds)))))

(defn declared-coverage
  "Declared coverage of the frozen partition against `fm/action-types`.

  `:covered` is the seed classes; `:unseeded` is the declared remainder.
  The vocabulary test asserts `:unseeded` equals `declared-unseeded`, so
  further vocabulary drift fails loudly instead of fabricating zones."
  []
  (let [seeded (seeded-classes)]
    {:covered seeded
     :unseeded (set/difference fm/action-types seeded)
     :declared-unseeded declared-unseeded
     :declared-at "2026-09-18"}))

(defn- seed-vector [seed]
  (or (when (= :centroid *seed-generation*) (:centroid_seed seed))
      (:text_seed seed)
      (:vector seed)
      (throw (ex-info "Seed has no usable embedding"
                      {:class (:class seed)
                       :generation *seed-generation*}))))

(defn zone-of
  "Return nearest action-class seed and the top1-top2 cosine margin.

  No confidence threshold is applied here.  Callers own any resisted-
  description policy.  The result carries the metric both as an explicit key
  and Clojure metadata so downstream consumers cannot mistake it for the held
  ground metric."
  [embedding-vector]
  (let [ranked (->> (or *seed-records* (read-seeds))
                    (map (fn [seed]
                           {:class (keyword (:class seed))
                            :cosine (cosine embedding-vector
                                            (seed-vector seed))}))
                    (sort-by (juxt (comp - :cosine) (comp name :class)))
                    vec)]
    (when (< (count ranked) 2)
      (throw (ex-info "At least two action-class seeds are required"
                      {:seed-count (count ranked)})))
    (let [top (first ranked)
          runner (second ranked)]
      (with-meta
        {:class (:class top)
         :cosine (:cosine top)
         :margin (- (:cosine top) (:cosine runner))
         :runner-up (:class runner)
         :metric :interim-metric}
        {:metric :interim-metric}))))

(defn transform-3d
  "Apply an inspectable PCA-3 reduction artifact to EMBEDDING.
  Pure matrix math: each coordinate is one component dot (v - mean)."
  [reduction embedding]
  (let [mean (or (:mean reduction) (get reduction "mean"))
        components (or (:components reduction) (get reduction "components"))]
    (when-not (= (count mean) (count embedding))
      (throw (ex-info "Reduction and embedding dimensions differ"
                      {:mean (count mean) :embedding (count embedding)})))
    (when-not (= 3 (count components))
      (throw (ex-info "Reduction must have exactly three components"
                      {:components (count components)})))
    (let [centered (mapv - embedding mean)]
      (mapv (fn [component]
              (when-not (= (count component) (count centered))
                (throw (ex-info "PCA component dimension differs"
                                {:component (count component)
                                 :embedding (count centered)})))
              (reduce + (map * component centered)))
            components))))

(defn operative-seed
  "Select the S1.5 operative seed generation for one seed record."
  [seed]
  (let [centroid? (pos? (long (or (:centroid_evidence_count seed) 0)))]
    {:class (keyword (:class seed))
     :generation (if centroid? :centroid-seed :text-seed)
     :vector (if centroid? (:centroid_seed seed) (:text_seed seed))}))

(defn seeds-3d
  "Transform operative action-class seeds into the versioned 3-D space."
  [reduction seeds]
  (mapv (fn [seed]
          (let [{:keys [class generation vector]} (operative-seed seed)]
            {:class class :generation generation
             :point (transform-3d reduction vector)}))
        seeds))

(defn- euclidean [a b]
  (Math/sqrt (double (reduce + (map (fn [x y]
                                      (let [d (- (double x) (double y))]
                                        (* d d)))
                                    a b)))))

(defn zone-of-3d
  "Assign V to the nearest operative seed in PCA-3 Euclidean space.

  SEEDS-3D are produced by `seeds-3d`. Margin is runner-up distance minus
  nearest distance and is always reported; callers own resistance thresholds."
  [reduction seeds-3d-records v]
  (let [point (transform-3d reduction v)
        version (or (:version reduction) (get reduction "version"))
        ranked (->> seeds-3d-records
                    (map (fn [{seed-point :point :as seed}]
                           (assoc seed :distance (euclidean seed-point point))))
                    (sort-by (juxt :distance (comp name :class)))
                    vec)]
    (when (< (count ranked) 2)
      (throw (ex-info "At least two 3-D seeds are required"
                      {:seed-count (count ranked)})))
    (let [top (first ranked) runner (second ranked)]
      (with-meta
        {:class (:class top)
         :distance (:distance top)
         :margin (- (:distance runner) (:distance top))
         :runner-up (:class runner)
         :point point
         :reduction-version version
         :metric :interim-metric-3d}
        {:metric :interim-metric-3d
         :reduction-version version}))))

(defn- assert-seeded-class!
  [type partition]
  (when (contains? (set/difference fm/action-types (seeded-classes)) type)
    (throw (ex-info "Action class is outside the frozen partition's declared coverage"
                    {:type :unseeded-action-class
                     :class type
                     :partition partition
                     :declared-unseeded declared-unseeded}))))

(defn zone-of-action
  "Zone an ACTION MAP ({:type action-type, :embedding vector}) under the
  raw-space seeds. Typed refusal when the action's class is unseeded: the
  machine must not fabricate a zone for it by nearest-seed. Callers holding
  a bare embedding with no declared class keep using `zone-of` unchanged."
  [{:keys [type embedding] :as _action}]
  (assert-seeded-class! type :raw-space)
  (zone-of embedding))

(defn zone-of-action-3d
  "Zone an ACTION MAP ({:type action-type, :embedding vector}) under the
  operative frozen PCA-3 partition. Same typed refusal on an unseeded
  class as `zone-of-action`; bare-embedding callers keep `zone-of-3d`."
  [reduction seeds-3d-records {:keys [type embedding] :as _action}]
  (assert-seeded-class! type (or (:version reduction) (get reduction "version")))
  (zone-of-3d reduction seeds-3d-records embedding))
