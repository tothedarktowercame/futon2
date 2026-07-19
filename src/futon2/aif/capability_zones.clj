(ns futon2.aif.capability-zones
  "Capability-zone membership in the shared BGE embedding space.

  The distance is plain cosine and is explicitly an :interim-metric while the
  ground metric remains :held by M-substrate-metric.  This namespace consumes
  that hold; it does not introduce a competing ground metric."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def ^:dynamic *seeds-path*
  "/home/joe/code/data/notions/bge_action_class_seeds.json")

(def ^:dynamic *seed-generation*
  "Seed vector generation used for membership: :centroid by default, with
  :text available for reproducible first-generation harvest classification."
  :centroid)

(def ^:dynamic *seed-records*
  "Optional in-memory seeds for batch callers and deterministic fixtures."
  nil)

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
