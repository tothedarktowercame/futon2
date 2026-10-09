(ns futon2.aif.scoring-cache-prewarm
  "Offline, own-JVM builder for the incremental global scoring cache.
   It never invokes the HTTP runner or the Agency JVM."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [futon2.aif.focus-receipt :as focus]
            [futon2.aif.parameter-novelty :as novelty]
            [futon2.aif.ticket-queue :as ticket-queue]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.aif.wm.construction-inputs :as inputs]
            [futon2.aif.wm.library-slices :as slices]
            [futon2.data-paths :as data-paths])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(defn- sha256 [x]
  (format "%064x" (BigInteger. 1 (.digest
                                  (MessageDigest/getInstance "SHA-256")
                                  (.getBytes (pr-str x) "UTF-8")))))

(defn- args [xs]
  (loop [m {:facts "/tmp/c10/facts48.json"
            :library "/home/joe/code/futon3/library"
            :workers 16}
         xs xs]
    (if (empty? xs) m
        (let [[flag value & more] xs
              k ({"--facts" :facts "--library" :library
                  "--workers" :workers "--cache" :cache} flag)]
          (when-not (and k value)
            (throw (ex-info "usage: --facts FILE --library DIR --workers N [--cache FILE]"
                            {:args xs})))
          (recur (assoc m k (if (= k :workers) (Long/parseLong value) value)) more)))))

(defn- load-facts [path]
  (:facts (json/parse-string (slurp path) true)))

(defn- assemble [facts manifest]
  (let [targets (vec (concat (:openMissions facts) (:openTickets facts)
                             (:openExcursions facts)))
        slices-by-target
        (into {} (map (fn [target]
                        [target {:schema :wm/query-time-library-slice-v1
                                 :target target :query target
                                 :candidates (slices/retrieve manifest target 40)
                                 :slice-size (min 40 (:size manifest))
                                 :library-size (:size manifest)
                                 :library-manifest-digest (:digest manifest)
                                 :slice-from-whole-library true}])
                      targets))]
    {:targets targets
     :library-pin (dissoc manifest :patterns)
     :scoring-target-budget {:schema :wm/scoring-target-budget-v1
                             :target-limit (count targets)
                             :basis :offline-prewarm
                             :enumerated-target-count (count targets)}
     :sources {:universes (into {} (map (fn [t] [t {:work/open true :work/closed false}])) targets)
               :wants (into {} (map (fn [t] [t [:work/closed]])) targets)
               :locators (into {} (map (fn [t] [t {:work/open {:class :C4 :fixture true}
                                                   :work/closed {:class :C4 :fixture true}}])) targets)
               :interpretations (into {} (map (fn [t] [t {:patterns {} :receipts {}}])) targets)
               :query-time-slices slices-by-target
               :horizon-steps 2 :beta-by-context {:WM {:beta 1}}
               :context-of (constantly :WM)}}))

(defn- click-running? []
  ;; The runner's durable lock is the authoritative local guard.  A stale
  ;; lock is not reclaimed here; the normal WM lock owner must release it.
  (.isFile (io/file (data-paths/path "wm-trace" ".run-lock"))))

(defn prewarm! [{:keys [facts library workers cache]}]
  (when (click-running?)
    (throw (ex-info "WM click is in flight" {:status :refused
                                               :kind :scoring-cache-prewarm-busy})))
  (let [facts-data (load-facts facts)
        manifest (slices/library-manifest library)
        assembled (assemble facts-data manifest)
        inputs-digest (sha256 {:facts facts-data :library-pin (dissoc manifest :patterns)
                               :targets (:targets assembled)})
        path (or cache (data-paths/path "wm-scoring-cache" "global-rank.edn"))
        started (System/nanoTime)
        result (decision/cascade-decision
                assembled
                {:decision-as-of "offline-prewarm"
                 :scoring-parallelism workers
                 :scoring-cache? true
                 :scoring-cache-prewarm? true
                 :scoring-cache-path path
                 :scoring-cache-prewarm-metadata
                 {:schema :wm/scoring-cache-prewarm-v1
                  :inputs-digest inputs-digest
                  :started-at (str (java.time.Instant/now))}
                 :scoring-cache-prewarm-start-ns started
                 :learning-trial-ledger-root (data-paths/path "wm-learning-trials")
                 :cascade-habit-path (data-paths/path "cascade-prior.edn")
                 :novelty-inputs (novelty/read-inputs)
                 :ticket-queue ticket-queue/empty-declaration
                 :focus-inputs (assoc (focus/read-inputs) :relations
                                      (mapv (fn [target]
                                              {:target target :facet "WM" :relation "focus"
                                               :source {:repo "futon2" :path facts}})
                                            (:targets assembled)))})
        duration-ms (/ (- (System/nanoTime) started) 1e6)
        cache-value (edn/read-string (slurp path))]
    {:status :completed :schema :wm/scoring-cache-prewarm-v1
     :cache path :generation (:generation cache-value)
     :inputs-digest inputs-digest :duration-ms duration-ms
     :targets (count (:targets assembled))}))

(defn -main [& xs]
  (prn (prewarm! (args xs))))
