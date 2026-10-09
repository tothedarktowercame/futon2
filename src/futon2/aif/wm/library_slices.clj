(ns futon2.aif.wm.library-slices
  "Bounded whole-library retrieval for the all-target cascade field."
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.selection-world :as world])
  (:import [java.time Instant]))

(defn tokens [x]
  (set (re-seq #"[a-z][a-z0-9-]+" (str/lower-case (str x)))))

(defn library-manifest
  "Read LIBRARY-ROOT once into a content-pinned retrieval index."
  [library-root]
  (let [root (.getCanonicalFile (io/file library-root))
        rows (->> (file-seq root)
                  (filter #(.isFile ^java.io.File %))
                  (filter #(str/ends-with? (.getName ^java.io.File %) ".flexiarg"))
                  (map (fn [f]
                         (let [text (slurp f)
                               rel (.toString (.relativize (.toPath root) (.toPath f)))]
                           {:id (str/replace rel #"\\" "/")
                            :sha256 (world/sha256 text)
                            :tokens (tokens text)})))
                  (sort-by :id)
                  vec)
        pin (mapv #(select-keys % [:id :sha256]) rows)]
    {:schema :wm/pinned-pattern-library-v1
     :digest (world/sha256 (pr-str pin))
     :size (count rows)
     :patterns rows}))

(defn retrieve
  "Retrieve K pattern ids per query from the complete MANIFEST.  Ties are
  stable by id, so zero-overlap queries still have a reproducible slice."
  [manifest query k]
  (let [q (tokens query)]
    (->> (:patterns manifest)
         (map (fn [{:keys [id tokens]}]
                {:pattern (str/replace id #"\.flexiarg$" "")
                 :score (count (set/intersection q tokens))}))
         (sort-by (juxt (comp - :score) :pattern))
         (take (min k (:size manifest)))
         (map-indexed #(assoc %2 :slice-rank (inc %1) :retriever :whole-library-lexical))
         vec)))

(defn batch
  "Return shared pin plus per-target slices/refusals. OPTIONS supplies an
  optional monotonic NANO-TIME-FN, MAX-MILLIS and K."
  [manifest target-queries {:keys [k max-millis nano-time-fn]
                            :or {k 40 max-millis 30000 nano-time-fn #(System/nanoTime)}}]
  (let [started (nano-time-fn)
        over? #(> (/ (- (nano-time-fn) started) 1e6) max-millis)
        step (fn [{:keys [slices refusals exhausted?] :as acc} [target query]]
               (cond
                 exhausted? (update acc :refusals assoc target
                                    {:status :refused :kind :budget-exhausted
                                     :stage :whole-library-retrieval})
                 (over?) (-> acc
                             (assoc :exhausted? true)
                             (update :refusals assoc target
                                     {:status :refused :kind :budget-exhausted
                                      :stage :whole-library-retrieval}))
                 (str/blank? (str query))
                 (update acc :refusals assoc target
                         {:status :refused :kind :target-query-absent})
                 :else
                 (assoc-in acc [:slices target]
                           {:schema :wm/query-time-library-slice-v1
                            :target target :query-digest (world/sha256 query)
                            :candidates (retrieve manifest query k)
                            :slice-size (min k (:size manifest))
                            :library-size (:size manifest)
                            :library-manifest-digest (:digest manifest)
                            :slice-from-whole-library true})))]
    (assoc (reduce step {:slices {} :refusals {} :exhausted? false}
                   target-queries)
           :library-pin (dissoc manifest :patterns)
           :elapsed-ms (/ (- (nano-time-fn) started) 1e6)
           :generated-at (str (Instant/now)))))
