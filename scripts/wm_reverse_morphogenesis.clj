#!/usr/bin/env clojure
;;; wm_reverse_morphogenesis.clj -- read-only reconstruction of the pattern
;;; cascade a COMPLETED unit of work actually followed, from 象 analyses
;;; that already exist (RM2, PROOF-2b reverse morphogenesis).
;;;
;;;   clojure -M scripts/wm_reverse_morphogenesis.clj -- \
;;;     --mission M-futon-seams \
;;;     --ops-cache /tmp/wm-ops.json \
;;;     --commits-cache /tmp/wm-commits.json \
;;;     --out /tmp/wm-rm-seams.json
;;;
;;; --ops-cache is the bounded harvest of the evidence store's operator
;;; turns (one author=joe walk, page limit 1000; entries [at mission-id
;;; turn-id session-id text-prefix] as JSON). --commits-cache is the same
;;; walk's turn-commits for the mission ([committed-at repo sha subject]).
;;; Both are caches: this script performs NO network access, writes only
;;; --out, and never mutates the analysis files or the store.
;;;
;;; The strict join is (session-id, turn-id) between the store's operator
;;; turns and the 象 analysis records; turn-id alone collides across
;;; sessions. Per analysed turn, futon2.aif.analysis-cascade/
;;; analysis->cascades runs in :overlap mode; turns citing no pattern are
;;; counted as failures, never dropped silently. The FOLLOWED cascade
;;; aggregates: nodes = cited patterns (with the turns that cite them),
;;; within-turn edges from the cascade edges, across-turn edges
;;; :next-in-session between consecutive analysed turns of one session.

(require '[futon2.aif.analysis-cascade :as ac]
         '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(def live-dir "/home/joe/.emacs-graph/session-turn-analysis")
(def batch-dirs "/home/joe/code/storage/operator-turns/batches")

(defn- analysis-index
  "Map [session-id turn-id] -> analysis map, live and batch. Only records
  whose analysis file exists are indexed."
  []
  (let [turn-file? (fn [f]
                     (and (.isFile f)
                          (str/ends-with? (.getName f) ".json")
                          (not (str/ends-with? (.getName f)
                                               ".analysis.json"))
                          (not (str/ends-with? (.getName f)
                                               ".candidates.json"))))
        live (filter turn-file? (file-seq (io/file live-dir)))
        batch (for [d (.listFiles (io/file batch-dirs))
                    :when (and (.isDirectory d)
                               (not (str/includes? (.getName d) "old-copy")))
                    f (.listFiles d)
                    :when (turn-file? f)]
                f)]
    (into {}
          (comp (remove nil?)
                (keep (fn [f]
                        (let [af (io/file (str (.getCanonicalPath f)
                                               ".analysis.json"))]
                          (when (.isFile af)
                            (try (let [t (json/parse-string (slurp f) true)]
                                   (when (and (:session_id t) (:turn_id t))
                                     [[(:session_id t) (:turn_id t)]
                                      (json/parse-string (slurp af) true)]))
                                 (catch Exception _ nil)))))))
          (concat live batch))))

(defn- analysed-turns
  "The mission's operator turns joined strictly to analyses, in time order."
  [mission ops analysis-idx]
  (->> ops
       (filter #(= mission (get % 1)))
       (keep (fn [[at _m turn-id session-id _text]]
               (when-let [analysis (get analysis-idx [session-id turn-id])]
                 {:at at :turn-id turn-id :session-id session-id
                  :analysis analysis})))
       (sort-by :at)
       vec))

(defn- turn-cascades
  "analysis->cascades in :overlap mode for one turn, with the cited
  patterns and the count kept on the record."
  [turn]
  (let [r (ac/analysis->cascades (:analysis turn) {:mode :overlap})
        cascades (:cascades r)
        patterns (distinct (mapcat (fn [c] (map :pattern (:nodes c)))
                                   cascades))]
    (assoc turn
           :cascade-status (:status r)
           :cascade-count (count cascades)
           :failure-count (:failure-count r)
           :patterns patterns
           :edges (vec (mapcat :edges cascades)))))

(defn- followed-cascade
  "The mission's followed cascade: nodes with their citing turns, edges
  within turns (from the cascades) and :next-in-session between
  consecutive analysed turns of one session."
  [turns]
  (let [nodes (reduce (fn [acc t]
                        (reduce (fn [a p]
                                  (update a p (fnil conj []) (:at t)))
                                acc (:patterns t)))
                      (sorted-map) turns)
        within (vec (mapcat :edges turns))
        by-session (group-by :session-id turns)
        next-in-session
        (vec (mapcat (fn [[_session ts]]
                       (map (fn [[a b]]
                              {:from (first (:patterns b)) :to (first (:patterns a))
                               :kind :next-in-session
                               :from-turn (:turn-id a) :to-turn (:turn-id b)
                               :from-at (:at a) :to-at (:at b)})
                            (partition 2 1 (sort-by :at ts))))
                     by-session))]
    {:nodes (into {} (map (fn [[p ats]] [p {:cited-by (count ats)
                                             :first (first (sort ats))
                                             :last (last (sort ats))}])
                          nodes))
     :within-turn-edges within
     :next-in-session-edges next-in-session}))

(defn- phase-markers
  "Where the mission's phase-exit commits fall in the turn sequence: the
  index of the last analysed turn at or before each commit date."
  [turns commits phase-regex]
  (vec
   (for [[at sha subject] commits
         :when (re-find phase-regex subject)]
     (let [idx (count (take-while #(< (compare (:at %) at) 0) turns))]
       {:committed-at at :sha sha :subject subject
        :after-turn-index (dec idx)
        :after-turn (get-in (vec turns) [(dec idx) :turn-id])}))))

(defn -main [& raw-args]
  (let [arg-pairs (apply hash-map (remove #{"--"} raw-args))
        mission (get arg-pairs "--mission")
        ops-cache (get arg-pairs "--ops-cache")
        commits-cache (get arg-pairs "--commits-cache")
        out (get arg-pairs "--out")
        ops (json/parse-string (slurp ops-cache))
        commits (json/parse-string (slurp commits-cache))
        analysis-idx (analysis-index)
        base (analysed-turns mission ops analysis-idx)
        turns (mapv turn-cascades base)
        with-patterns (filter #(seq (:patterns %)) turns)
        followed (followed-cascade turns)
        result {:mission mission
                :operator-turns (count (filter #(= mission (get % 1)) ops))
                :analysed-turns (count turns)
                :turns-without-patterns (- (count turns)
                                           (count with-patterns))
                :turns-with-cascades (count (filter #(pos? (:cascade-count %))
                                                    turns))
                :cascade-failure-counts (reduce + 0 (map :failure-count
                                                         turns))
                :followed followed
                :recurring-patterns
                (into (sorted-map)
                      (filter (fn [[_k v]] (> (:cited-by v) 1))
                              (:nodes followed)))
                :phase-markers
                (phase-markers turns commits
                               #"(?i)(head|identify|map|derive|argue|verify|instantiate|document).{0,60}(met|complete|passed|eight of eight)|exit is met")
                :turn-sequence
                (vec (for [t turns]
                       (select-keys t [:at :turn-id :session-id
                                       :cascade-status :cascade-count
                                       :failure-count :patterns])))}]
    (json/generate-stream result (io/writer out) {:pretty true})
    (println {:mission mission
              :analysed (count turns)
              :with-patterns (count with-patterns)
              :nodes (count (:nodes followed))
              :within-edges (count (:within-turn-edges followed))
              :next-edges (count (:next-in-session-edges followed))
              :out out})))

(when *command-line-args* (apply -main *command-line-args*))
