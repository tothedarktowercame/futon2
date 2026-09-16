(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str] '[clojure.walk :as walk])
(defn read-all [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [xs []] (let [x (edn/read {:eof ::eof :default tagged-literal} r)] (if (= ::eof x) xs (recur (conj xs x)))))))
(def recs (read-all "data/wm-trace/wm-trace-2026-09-12.edn"))
(def sel (nth recs 3))
(println "timestamps" (mapv :timestamp recs))
(doseq [k [:mu-pre :mu-post]]
  (let [m (get sel k)]
    (println k "type" (type m) "count" (when (coll? m) (count m)))
    (when (map? m) (println "  sample keys" (take 8 (keys m)))
      (println "  keys mentioning shared-memory" (filter #(str/includes? (str %) "shared-memory") (keys m))))))
;; every path whose key or string value mentions the target
(def hits (atom []))
(defn walkp [x path]
  (cond (map? x) (doseq [[k v] x] (when (str/includes? (str k) "M-shared-memory-control-build-test") (swap! hits conj [:key (conj path k)])) (walkp v (conj path k)))
        (sequential? x) (doseq [[i v] (map-indexed vector x)] (walkp v (conj path i)))
        (string? x) (when (str/includes? x "M-shared-memory-control-build-test") (swap! hits conj [:val path]))))
(walkp sel [])
(println "target mentions" (count @hits))
(doseq [h (take 40 @hits)] (prn h))
;; any nested key naming run/cohort/attempt/checkpoint/started/completed/cutoff/disposition
(def okeys (atom #{}))
(walk/postwalk (fn [x] (when (map? x) (doseq [k (keys x)] (when (re-find #"(?i)run|cohort|attempt|checkpoint|started|completed|cutoff|disposition" (str k)) (swap! okeys conj k)))) x) sel)
(println "nested occurrence-like keys" (sort-by str @okeys))
(println "--- part 2")
(println "mu-pre key types" (frequencies (map type (keys (:mu-pre sel)))))
(println "mu-pre keys with mission/M-" (filter #(re-find #"(?i)mission|/M-|shared" (str %)) (keys (:mu-pre sel))))
(def paths (atom []))
(defn walkk [x path]
  (cond (map? x) (doseq [[k v] x] (when (#{:attempt-count :disposition} k) (swap! paths conj [(conj path k) v])) (walkk v (conj path k)))
        (sequential? x) (doseq [[i v] (map-indexed vector x)] (walkk v (conj path i)))))
(walkk sel [])
(println "nested :attempt-count/:disposition paths" (count @paths))
(doseq [p (take 6 @paths)] (prn (update p 1 #(let [s (pr-str %)] (subs s 0 (min 160 (count s)))))))
(println "observation" (pr-str (:observation-envelope sel)))
