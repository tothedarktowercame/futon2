(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.data :as data])
(defn read-all [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [xs []] (let [x (edn/read {:eof ::eof :default tagged-literal} r)] (if (= ::eof x) xs (recur (conj xs x)))))))
(def trace (nth (read-all "data/wm-trace/wm-trace-2026-09-12.edn") 3))
(def item-path "data/wm-morning-brief/items/ea1-418bb56e1f0d7ad8986839e45f5268a98e62dcbf9c8ad3634bd998f421a293ae--attempt-001.edn")
(def item (first (read-all item-path)))
(def found (atom []))
(defn walkp [x path]
  (cond (map? x) (do (when (= "pi-s-9dbc2ceb3317bc38050c41ce" (:selected-policy-id x)) (swap! found conj [path x]))
                     (doseq [[k v] x] (walkp v (conj path k))))
        (sequential? x) (doseq [[i v] (map-indexed vector x)] (walkp v (conj path i)))))
(walkp item [])
(println "item: selected-target" (:selected-target item) "opportunity" (:opportunity-id item) "trigger" (:trigger item) "queued-at" (:queued-at item))
(println "maps in item with selected-policy-id = pi-s-9dbc:" (mapv first @found))
(doseq [[p m] @found]
  (let [td (:decision trace)
        [only-item only-trace both] (data/diff m td)]
    (println "path" p)
    (println "  item decision keys" (count (keys m)) "trace decision keys" (count (keys td)))
    (println "  (= item-decision trace-decision)" (= m td))
    (println "  keys differing:" (sort (distinct (concat (keys only-item) (keys only-trace)))))
    (println "  action equal" (= (:action m) (:action td)))))
(println "trace keys :moved-from-controller-head?" (get-in trace [:decision :live-selector]) )
(println "trace decision keys" (sort (keys (:decision trace))))
(println "--- softmax comparison")
(let [m (second (first @found)) td (:decision trace)]
  (doseq [k [:softmax-weights :softmax-weights-by-candidate-id]]
    (let [a (get m k) b (get td k)]
      (println k "item type" (type a) "count" (when (coll? a) (count a)) "| trace type" (type b) "count" (when (coll? b) (count b)))
      (println "  item sample" (pr-str (take 2 (if (map? a) (seq a) a))))
      (println "  trace sample" (pr-str (take 2 (if (map? b) (seq b) b))))))
  (let [a (:softmax-weights-by-candidate-id m) b (:softmax-weights-by-candidate-id td)]
    (when (and (map? a) (map? b))
      (println "  same candidate ids" (= (set (keys a)) (set (keys b))))
      (println "  max abs diff" (reduce max 0 (for [k (keys a) :let [x (get a k) y (get b k)] :when (and (number? x) (number? y))] (Math/abs (double (- x y)))))))))
(def backup (nth (read-all "data/wm-trace/wm-trace-2026-09-12.edn.pre-migration-backup") 3))
(println "backup timestamp" (:timestamp backup) "backup decision = item decision" (= (:decision backup) (second (first @found))))
(println "backup keys only / trace keys only" (clojure.set/difference (set (keys backup)) (set (keys trace))) (clojure.set/difference (set (keys trace)) (set (keys backup))))
(println "backup decision keys differing from trace" (let [[a b] (data/diff (:decision backup) (:decision trace))] (sort (distinct (concat (keys a) (keys b))))))
(println "--- weight multiset")
(let [m (second (first @found)) td (:decision trace)]
  (println "sorted weight vectors equal" (= (sort (vals (:softmax-weights m))) (sort (vals (:softmax-weights-by-candidate-id td))))))
(println "--- selection reasons")
(let [sr (get-in item [:selection-review :selection-reasons])]
  (println "selection-reasons keys" (sort (keys sr)))
  (doseq [[k v] sr :when (not= k :ordinary-selector-decision)]
    (println " " k (let [s (pr-str v)] (subs s 0 (min 400 (count s)))))))
(println "enacted target" (get-in item [:selection-review :enacted-candidate-action :target]) "operator" (get-in item [:selection-review :operator]))
