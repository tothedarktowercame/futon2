(ns futon2.aif.scoring-cache-prewarm
  "Own-JVM cache builder.  This deliberately calls the report's live judge:
   registry loading, mission-hole wants, library retrieval, construction and
   cascade scoring are not reimplemented here."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.report.war-machine :as wm]
            [futon2.data-paths :as data-paths])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(defn- sha256 [x]
  (format "%064x" (BigInteger. 1 (.digest
                                  (MessageDigest/getInstance "SHA-256")
                                  (.getBytes (pr-str x) "UTF-8")))))

(defn- parse-args [xs]
  (loop [m {} xs xs]
    (if (empty? xs) m
        (let [[flag value & more] xs
              k ({"--scan-data" :scan-data
                  "--pattern-library-root" :pattern-library-root
                  "--cache" :cache
                  "--workers" :workers} flag)]
          (when-not (and k value)
            (throw (ex-info "usage: --scan-data FILE [--cache FILE] [--pattern-library-root DIR] [--workers N]"
                            {:args xs})))
          (recur (assoc m k (if (= k :workers) (Long/parseLong value) value))
                 more)))))

(defn- click-running? []
  (.isFile (io/file (data-paths/path "wm-trace" ".run-lock"))))

(defn prewarm! [{:keys [scan-data pattern-library-root cache workers]}]
  (when (click-running?)
    (throw (ex-info "WM click is in flight"
                    {:status :refused :kind :scoring-cache-prewarm-busy})))
  (let [world (with-open [r (java.io.PushbackReader. (io/reader scan-data))]
                (edn/read r))
        started (System/nanoTime)
        input-digest (sha256 world)
        cache-path (or cache (data-paths/path "wm-scoring-cache" "global-rank.edn"))
        result (wm/judge world
                         (cond-> {:trace? false
                                  :scoring-parallelism (or workers 16)
                                  :scoring-cache? true
                                  :scoring-cache-prewarm? true
                                  :scoring-cache-path cache-path
                                  :scoring-cache-prewarm-start-ns started
                                  :scoring-cache-prewarm-metadata
                                  {:schema :wm/scoring-cache-prewarm-v1
                                   :inputs-digest input-digest
                                   :source :live-judge-input}
                                  :run-id (str "offline-prewarm-" input-digest)}
                           pattern-library-root
                           (assoc :pattern-library-root pattern-library-root)))
        duration-ms (/ (- (System/nanoTime) started) 1e6)
        cache-value (when (.isFile (io/file cache-path))
                      (edn/read-string (slurp cache-path)))]
    {:status :completed :cache cache-path
     :generation (:generation cache-value)
     :inputs-digest input-digest :duration-ms duration-ms
     :selection-status (get-in result [:decision :status])
     :cache-prewarm (:prewarm cache-value)}))

(defn -main [& xs]
  (prn (prewarm! (parse-args xs))))
