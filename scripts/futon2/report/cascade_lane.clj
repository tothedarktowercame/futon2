(ns futon2.report.cascade-lane
  "v1 cascade-policy lane for the live WM (M-wm-policies go-live, 2026-06-09).

   ADDITIVE + self-contained — does NOT touch the judge's mission ranking. Given the WM's
   ranked missions, construct the coverage-saturated coherence-greedy *cascade-policy* (a
   pattern-semilattice = a scored ARGUE, Alexander) for each top mission's circumstance |psi>,
   budget-truncated (the parsimony ceiling, set from data). Shells out to the Python
   constructor (cascade_serve.py / minilm) — NOT in-JVM, like the notions path.

   v1-thin: this is THE visible non-degenerate-policy lane. The rollout-over-the-gradient-prior
   deepens at scope-grain v2 (in v1 its reachable set is 3 summits). Sim-only, read-only:
   constructs cascades over a state copy, never promotes/writes."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.util.concurrent TimeUnit]))

(def ^:private py "/home/joe/code/futon3a/.venv/bin/python")
(def ^:private script "/home/joe/code/futon3a/holes/labs/M-memes-arrows/cascade_serve.py")
(def ^:private script-dir "/home/joe/code/futon3a/holes/labs/M-memes-arrows")
(def default-budget 6)

(def ^:private cascade-timeout-ms
  "Hard ceiling on the Python cascade constructor (minilm cold-load + build). A hung child must NEVER
   wedge the calling scheduler thread permanently (fable-1, WM cycle-#2 hardening). Env-overridable."
  (long (or (some-> (System/getenv "FUTON2_CASCADE_TIMEOUT_MS") parse-long) 30000)))

(defn- read-stream [stream]
  ;; drain in a future so a chatty child can't deadlock on a full pipe buffer
  (future (with-open [rdr (io/reader stream)] (slurp rdr))))

(defn- sh-timed
  "Run cmd (a seq of strings) in dir with a HARD timeout. Returns {:exit :out} or nil on
   timeout/failure. Mirrors futon3c watcher/projections/python.clj: ProcessBuilder + waitFor(timeout)
   + destroy/destroyForcibly, so a hung cascade_serve.py can never wedge the calling (scheduler)
   thread — unlike clojure.java.shell/sh, whose waitFor is unbounded."
  [cmd dir timeout-ms]
  (let [proc (-> (ProcessBuilder. ^java.util.List (vec cmd))
                 (.directory (io/file dir))
                 (.start))
        out* (read-stream (.getInputStream proc))
        _err* (read-stream (.getErrorStream proc))]
    (if (.waitFor proc timeout-ms TimeUnit/MILLISECONDS)
      {:exit (.exitValue proc) :out @out*}
      (do (.destroy proc)
          (when-not (.waitFor proc 1000 TimeUnit/MILLISECONDS)
            (.destroyForcibly proc)
            (.waitFor proc 1000 TimeUnit/MILLISECONDS))
          nil))))

;; A cascade is deterministic in (|psi>, budget) — the pattern library + minilm are stable —
;; so memoize across windows/ticks. Only successes are cached (failures retry).
(defonce ^:private !cache (atom {}))
(defn clear-cache! "Drop the cascade cache (e.g. after the pattern library changes)." [] (reset! !cache {}))

(defn cascade-policy-for
  "Construct the budget-truncated cascade-policy for a circumstance |psi> (text).
   Read-only, sim-only, memoized. Returns the parsed map or nil on failure."
  ([psi-text] (cascade-policy-for psi-text default-budget))
  ([psi-text budget]
   (or (get @!cache [psi-text budget])
       (let [v (try
                 (let [{:keys [exit out]} (sh-timed [py script psi-text (str budget)]
                                                    script-dir cascade-timeout-ms)]
                   (when (and exit (zero? exit))
                     (json/parse-string out true)))
                 (catch Exception _ nil))]
         (when v (swap! !cache assoc [psi-text budget] v))
         v))))

(defn mission->psi
  "v1 circumstance text from a mission target id (strip M-, hyphens->spaces).
   Enrichable with full mission text later; the id-stem is a workable v1 query."
  [target]
  (-> (str target)
      (str/replace #"^M-" "")
      (str/replace #"-" " ")))

(defn cascade-lane
  "The v1 cascade lane: for the top-n :open-mission targets in ranked-actions, build the
   cascade-policy for each circumstance. Returns
   [{:mission :psi :size :wholeness :budget :truncated :shown [pattern-ids...]} ...]."
  ([ranked-actions] (cascade-lane ranked-actions {}))
  ([ranked-actions {:keys [n budget] :or {n 3 budget default-budget}}]
   (->> ranked-actions
        ;; live-judgement comes through the JSON API -> :type is the STRING "open-mission";
        ;; the in-process path uses the keyword. Accept both.
        (filter #(#{:open-mission "open-mission"} (get-in % [:action :type])))
        (take n)
        (keep (fn [e]
                (let [m (get-in e [:action :target])
                      psi (str/trim (str (mission->psi m) " "
                                         (or (get-in e [:action :rationale]) "")))
                      c (cascade-policy-for psi budget)]
                  (when c
                    {:mission m :psi psi
                     :size (:size c) :wholeness (:wholeness c) :budget (:budget c)
                     :truncated (:truncated c)
                     :shown (mapv :pattern (:shown c))}))))
        vec)))

(def ^:private gap-free-energy-threshold
  "A cascade whose marginal-likelihood F = accuracy − λ·complexity falls at/below this is a
   defensive-driving GAP. F is the AIF-grounded grain-2 act-gate leg (M-wm-policies omission
   2): F > 0 = the cascade's ψ-coverage outweighs the complexity of justifying its patterns
   against the base-rate prior = a net-positive model expansion (Bayesian Occam accept). This
   REPLACES the Salingaros-C / coverage-size heuristics — C was an analogy (L=T·H is not
   formally a free-energy functional), size was scale-but-not-prior-aware. λ is set from data
   in cascade_construct.py (the rich/thin F=0 knee, λ=0.25 on the 2026-06-24 spectrum)."
  0.0)

(defn gap-lane
  "Defensive-driving HORIZON SCAN (M-wm-policies Track 3, proactive). For the top-n
   open-mission targets, construct each circumstance's cascade and flag those whose
   marginal-likelihood F ≤ gap-F-threshold (or no cascade) as PATTERN-GAPS — classes whose
   coverage does not pay for its pattern-complexity = the WM has no good-enough patterns for
   them yet, the places to seed cascades *before* it gets stuck. Reuses cascade-policy-for
   (read-only / sim-only / never promotes — WM-I4 intact) and its memo cache. F is the
   AIF act-gate leg; wholeness/size reported as context. Returns
   [{:mission :psi :F-free-energy :accuracy :complexity :wholeness :size :gap? :note} ...], gaps first (lowest F)."
  ([ranked-actions] (gap-lane ranked-actions {}))
  ([ranked-actions {:keys [n budget] :or {n 10 budget default-budget}}]
   (->> ranked-actions
        (filter #(#{:open-mission "open-mission"} (get-in % [:action :type])))
        (take n)
        (map (fn [e]
               (let [m   (get-in e [:action :target])
                     psi (str/trim (str (mission->psi m) " "
                                        (or (get-in e [:action :rationale]) "")))
                     c   (cascade-policy-for psi budget)
                     free-energy (:F-free-energy c)
                     gap? (or (nil? c) (nil? free-energy) (<= free-energy gap-free-energy-threshold))]
                 {:mission m :psi psi :F-free-energy free-energy
                  :accuracy (:accuracy c) :complexity (:complexity c)
                  :wholeness (:wholeness c) :size (:size c) :gap? gap?
                  :note (cond
                          (nil? c)
                          "no cascade constructed (constructor unavailable or no patterns — foothold needed)"
                          gap?
                          (format "F=%.2f ≤ 0 (acc %.2f < λ·complexity) — coverage doesn't pay for its patterns; seed here"
                                  (double (or free-energy 0.0)) (double (or (:accuracy c) 0.0)))
                          :else
                          (format "F=%.2f > 0 (acc %.2f vs λ·complexity) — net-positive cascade"
                                  (double free-energy) (double (or (:accuracy c) 0.0))))})))
        (sort-by (fn [r] [(if (:gap? r) 0 1) (or (:F-free-energy r) 0.0)]))
        vec)))
