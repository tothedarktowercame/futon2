#!/usr/bin/env bb
;; RUN3 -- conformance of a recorded run against the drawn topology.
;;
;;   bb run3_conformance.bb runs/2026-09-01-s1b/wm-trace-s1b.edn
;;
;; A route is a SEQUENCE of {:node :via :at} tags (war_machine.clj:4340-4345),
;; so a hop is a CONSECUTIVE PAIR of nodes. The receipt's :fromNode/:toNode is a
;; derived rendering; looking for those keys here would find nothing and report
;; a clean run.
;;
;; The :retires set is read from control-map-edges.edn AT RUN TIME and no list
;; is carried: the list in the campaign brief was wrong in three places, naming
;; a pair that is not retired and omitting two that are.
;;
;; THE DECISION RULE IS BY GROUNDS, not pass/fail (claude-1):
;;   (1) :code-retired, ROUTE-GRAIN, traversed  -> REFUTATION. The retirement was
;;       wrong and the figure changes, not the run.
;;   (2) :code-retired, DEPENDENCY-GRAIN, traversed -> EXCLUDED, and reported as
;;       excluded rather than passed over. A route is a sequence of tag stamps,
;;       not the dependency DAG (D6), so it cannot refute a dependency claim.
;;   (3) :ruling-retired, traversed -> THE RULING IS NOT REALISED IN CODE. A
;;       build row, not a conformance failure.
;; A pair may be retired by more than one decision on different grounds, so the
;; classification is by the SET of grounds.
;;
;; OPERATIONALISING "dependency-grain": a :code retirement whose pair ALSO
;; appears in :route-measured-drawn retired a dependency claim while the route
;; itself stayed drawn as measured. That is a checkable proxy for a distinction
;; the registry states in prose; it is stated here so a reader can disagree with
;; it rather than have to infer it.
(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str])

(def edges-path "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn")

(defn read-forms [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [acc []] (let [f (edn/read {:eof ::eof} r)]
                     (if (= ::eof f) acc (recur (conj acc f)))))))

(defn topology []
  (let [m (edn/read-string (slurp edges-path))
        pair (fn [e] [(name (:from e)) (name (:to e))])
        drawn (set (map pair (:edges m)))
        measured (set (map pair (:route-measured-drawn m)))
        retired (reduce (fn [acc [_ d]]
                          (reduce (fn [a p] (update a (mapv name p)
                                                    (fnil conj #{}) (:grounds d)))
                                  acc (:retires d)))
                        {} (:decisions m))]
    {:drawn drawn :measured measured :retired retired}))

(defn hops [record]
  (->> (:wm/route record) (map :node) (map name) (partition 2 1) (mapv vec)))

(defn classify [{:keys [drawn measured retired]} hop]
  (let [g (retired hop)]
    (cond
      (and g (:code g) (measured hop)) :excluded-dependency-grain
      (and g (:code g))                :refutation
      (and g (:ruling g))              :ruling-unrealised
      (drawn hop)                      :drawn
      (measured hop)                   :route-measured
      :else                            :unmapped)))

(defn report [trace-path]
  (let [topo (topology)
        records (read-forms trace-path)
        all (mapcat hops records)
        by-class (group-by #(classify topo %) (distinct all))
        f-pi (count (filter #(contains? % :f-pi-by-candidate-id) records))
        f-pi-present (count (filter #(= :present (get-in % [:f-pi-by-candidate-id :status])) records))
        cs (count (filter #(some :controller-score (:ranked-actions %)) records))]
    (println (format "run3: %d records, %d routes, %d hops (%d distinct)"
                     (count records) (count (filter :wm/route records))
                     (count all) (count (distinct all))))
    (println (format "run3: :controller-score on %d/%d, :f-pi-by-candidate-id on %d/%d (:present %d)"
                     cs (count records) f-pi (count records) f-pi-present))
    (doseq [k [:refutation :ruling-unrealised :excluded-dependency-grain :drawn :route-measured :unmapped]]
      (when-let [hs (seq (sort (by-class k)))]
        (println (format "run3: %-26s %d" (name k) (count hs)))
        (doseq [h hs] (println "run3:    " (str/join "->" h)))))
    (let [never (sort (remove (set all) (:drawn topo)))]
      (println (format "run3: drawn edges never traversed: %d of %d" (count never) (count (:drawn topo))))
      (doseq [h never] (println "run3:     unfired" (str/join "->" h)))
      {:refutations (by-class :refutation)
       :unmapped (by-class :unmapped)
       :ruling-unrealised (by-class :ruling-unrealised)
       :excluded (by-class :excluded-dependency-grain)
       :records (count records) :routes (count (filter :wm/route records))
       :hops (count all) :distinct (count (distinct all))
       :unfired (count never) :drawn (count (:drawn topo))})))

(let [args *command-line-args*
      path (or (first args) "runs/2026-09-01-s1b/wm-trace-s1b.edn")
      summary (report path)
      {:keys [refutations unmapped]} summary
      ;; Machine-readable summary beside the trace, and a stable copy the figure
      ;; generator reads (p4ng gen_live_topology.bb: legend line "route
      ;; conformance"), so the drawing names the run it was last checked
      ;; against instead of two August records. Only for a file under runs/.
      run-dir (let [f (io/file path)] (when (re-find #"runs/" (str f)) (.getParentFile f)))
      readme (when run-dir (io/file run-dir "README.md"))
      sha (when (and readme (.exists readme))
            (second (re-find #"sha `([0-9a-f]{7,40})`" (slurp readme))))
      edn-summary (assoc summary
                         :verdict (if (or (seq refutations) (seq unmapped)) :not-conformant :conformant)
                         :run (when run-dir (.getName run-dir))
                         :sha (when sha (subs sha 0 7))
                         :trace (str path)
                         :checked-at (str (java.time.Instant/now)))]
  (when run-dir
    (spit (io/file run-dir "conformance.edn") (pr-str edn-summary))
    (spit (io/file (.getParentFile run-dir) "latest-conformance.edn") (pr-str edn-summary)))
  (println)
  (if (or (seq refutations) (seq unmapped))
    (do (println "run3: NOT CONFORMANT --"
                 (count refutations) "refutation(s),"
                 (count unmapped) "unmapped hop(s)")
        (System/exit 1))
    (println "run3: CONFORMANT -- every hop is an edge of the checked topology, and no code-retired route-grain edge was traversed")))
