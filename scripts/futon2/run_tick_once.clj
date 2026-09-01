(ns futon2.run-tick-once
  "Run one on-demand WM tick and leave a Lean-shaped receipt."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.efe :as efe]
            [futon2.aif.pattern-registry :as pattern-registry]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as wm]
            [futon2.wm-run-lock :as run-lock])
  (:import (java.time Instant LocalDate ZoneId)))

(def ^:private utc-zone
  (ZoneId/of "UTC"))

(def ^:private live-selector-var
  'futon3c.peripheral.live-wm-selection/validated-selection)

(def ^:private stub-selector-name
  "first-ranked-authorized-mission")

(defn- today-date-string []
  (str (LocalDate/now utc-zone)))

(defn- lock-path
  "Beside the per-date trace file this tick appends to."
  []
  (run-lock/default-lock-path))

(defn- trace-path-for-date [date-str]
  (str (System/getProperty "user.home")
       "/code/futon2/data/wm-trace/wm-trace-" date-str ".edn"))

(defn- receipt-path-for-run [date-str run-id]
  (str (System/getProperty "user.home")
       "/code/futon2/holes/labs/wm-contract/tick-run-record-"
       date-str "-" run-id ".edn"))

(defn- trace-stat [path]
  (let [f (io/file path)]
    (when (.exists f)
      {:size (.length f)
       :mtime (.lastModified f)})))

(defn- node->str [node]
  (cond
    (keyword? node) (subs (str node) 1)
    (symbol? node) (str node)
    (string? node) node
    :else (str node)))

(defn- latest-at [entries]
  (or (->> entries
           (keep :evidence/at)
           (map str)
           seq
           sort
           last)
      ""))

(defn- resolve-live-selector []
  (try
    (when-let [selector (requiring-resolve live-selector-var)]
      {:selector selector
       :seam "live"})
    (catch Throwable e
      {:selector nil
       :resolution-error (str (.getClass e) ": " (.getMessage e))})))

(defn- stub-selection [{:keys [scheduler-habit-ranking]}]
  (let [mission-id (first scheduler-habit-ranking)]
    {:status :verified-live-selection
     :selected-policy-id (str "stub:" stub-selector-name)
     :selected-policy
     {:policy-id (str "stub:" stub-selector-name)
      :mission-ids [mission-id]
      :memory-ids []
      :e-s nil
      :predicted-g-s nil
      :hard-support []
      :proposal-reasons ["stubbed on-demand WM tick"]
      :provenance {:selector-seam :stub}
      :explanation-complete? true}
     :selected-mission-ids [mission-id]
     :selected-memory-ids []
     :relation-contributions []
     :path-diversity nil
     :budget nil
     :blockers []
     :calibration {:status :stub}
     :serving-cache-gate {:status :stub}
     :counterfactuals {:scheduler-habit scheduler-habit-ranking}
     :actuation
     {:status :machine-authorized-bounded-autonomy
      :authorized? true
      :executed? false}}))

(defn- selector-seam []
  (let [{:keys [selector seam resolution-error]} (resolve-live-selector)]
    (if selector
      {:selector selector
       :selector-seam seam}
      {:selector stub-selection
       :selector-seam (str "stub:" stub-selector-name)
       :selector-resolution-error resolution-error})))

(defn- get-json [url]
  (let [resp (http/get url {:headers {"Accept" "application/json"}
                            :timeout 30000
                            :throw false})]
    (when (= 200 (:status resp))
      (json/parse-string (:body resp) true))))

(defn- evidence-base []
  (pattern-registry/configured-evidence-base))

(defn- store-basis []
  (let [base (evidence-base)
        count-body (get-json (str base "/api/alpha/evidence/count"))
        latest-body (get-json (str base "/api/alpha/evidence?limit=1"))
        latest-entry (first (:entries latest-body))]
    {:count (long (or (:count count-body) 0))
     :max-at (or (some-> latest-entry :evidence/at str) "")
     :count-source (str base "/api/alpha/evidence/count")
     :max-at-source (str base "/api/alpha/evidence?limit=1")}))

(defn- evidence-sample [days]
  (let [limit (#'wm/session-evidence-limit)
        since (#'wm/since-str days)
        result (#'wm/fetch-evidence-result :limit limit :since since)
        entries (or (:entries result) [])]
    {:entries-read (count entries)
     :entries-limit limit
     :max-at (latest-at entries)
     :diagnostic (dissoc result :entries)}))

(def ^:private control-map-path
  "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn")

(defn- drawn-edge-pairs []
  (->> (:edges (edn/read-string (slurp control-map-path)))
       (filter #(= :drawn (:status %)))
       (map (fn [{:keys [from to]}]
              [(node->str from) (node->str to)]))
       set))

(defn assemble-route [tags]
  (->> tags
       (partition 2 1)
       (mapv (fn [[from to]]
               {:fromNode (node->str (:node from))
                :toNode (node->str (:node to))
                :via (:via to)
                :at_ (:at to)}))))

(defn route-verdict [hops]
  (let [drawn (drawn-edge-pairs)
        classified (mapv (fn [hop]
                           (assoc hop :drawn?
                                  (contains? drawn
                                             [(:fromNode hop) (:toNode hop)])))
                         hops)
        conformant (filterv :drawn? classified)
        unmapped (mapv #(dissoc % :drawn?) (remove :drawn? classified))]
    {:hops (count hops)
     :conformant (count conformant)
     :unmapped (count unmapped)
     :drawn-edges-fired (count (set (map (juxt :fromNode :toNode) conformant)))
     :drawn-edges-total (count drawn)
     :unmapped-hops unmapped}))

(defn- tick-run-record [run-id started-at store-basis sample result selector-seam trace-written?]
  (let [input-status (:input-status result)
        preference-stack (:preference-stack result)
        route (assemble-route (:wm/route result))
        verdict (route-verdict route)]
    {:run/id run-id
     :startedAt started-at
     :storeBasisCount (:count store-basis)
     :storeBasisMaxAt (:max-at store-basis)
     :entriesRead (long (:entries-read sample))
     :entriesLimit (long (:entries-limit sample))
     :inputsRead (long (or (:inputs-read input-status) 0))
     :inputIssues (long (count (:issues input-status)))
     :preferenceLayers (long (count preference-stack))
     :traceWritten (boolean trace-written?)
     :selectorSeam selector-seam
     :route route
     :route-verdict verdict}))

(defn- write-receipt! [path record]
  (io/make-parents path)
  (spit path (str (pr-str record) "\n"))
  path)

(defn- parse-days [args]
  (if-let [s (first args)]
    (Long/parseLong s)
    14))

(defn- diagnostic-judge-opts
  "RUN11: `run-id` is threaded into the judge opts so the trace record carries
  the same `:run/id` this tick's receipt carries. Both come from the one id
  minted in `run-tick-once*`, so the join between a receipt file and its trace
  record is an equality, not a timestamp comparison."
  ([selector version-stamp] (diagnostic-judge-opts selector version-stamp nil))
  ([selector version-stamp run-id]
   (cond-> {:trace? true
            :include-advisory-lanes? false
            :step-portfolio? false
            :step-mission-detail-portfolio? false
            :eval-invariant-fallback? false
            :strategic-selection-fn selector
            :wm-version version-stamp}
     run-id (assoc :run-id run-id))))

(defn- tick* [days started-at run-id]
  (let [date-str (today-date-string)
        trace-path (trace-path-for-date date-str)
        before-trace (trace-stat trace-path)
        store-basis (store-basis)
        sample (evidence-sample days)
        {:keys [selector selector-seam selector-resolution-error]} (selector-seam)
        version-stamp (trace/wm-version-stamp
                       (assoc (wm/arena-mode-flags)
                              :trigger :diagnostic-run-tick-once
                              :live-wire? false))
        generated (wm/generate-war-machine
                   days (diagnostic-judge-opts selector version-stamp run-id))
        result (-> (:judgement generated)
                   (assoc :preference-stack efe/preference-stack-record
                          :selector-seam selector-seam)
                   (cond-> selector-resolution-error
                     (assoc :selector-resolution-error
                            selector-resolution-error)))
        after-trace (trace-stat trace-path)
        trace-written? (and (nil? (:trace-write-failed result))
                            after-trace
                            (not= before-trace after-trace))
        record (tick-run-record run-id started-at store-basis sample result selector-seam trace-written?)
        receipt-path (receipt-path-for-run date-str run-id)]
    (write-receipt! receipt-path record)
    {:days days
     :selector-seam selector-seam
     :trace-path trace-path
     :trace-before before-trace
     :trace-after after-trace
     :receipt-path receipt-path
     :tick-run-record record
     :result result
     :store-basis store-basis
     :sample sample}))

(defn- run-tick-once*
  "One machine, one runner (RUN12): the lock is taken before any of the tick's
  reads and released after the trace write, so a second starter refuses instead
  of appending into the same per-date trace file. Under a run script that has
  taken the outer lock and exported FUTON_WM_RUN_LOCK_TOKEN this nests and the
  script holds the lock across every tick."
  [days]
  (let [started-at (str (Instant/now))
        run-id (str (java.util.UUID/randomUUID))]
    (run-lock/call-with-run-lock
     {:path (lock-path) :run-id run-id}
     (fn [_handle] (tick* days started-at run-id)))))

(defn run-tick-once
  "Callable one-shot entrypoint for WM-RUN1."
  ([] (run-tick-once 14))
  ([days]
   (run-tick-once* days)))

(defn -main [& args]
  (let [days (parse-days args)
        run (try (run-tick-once days)
                 (catch clojure.lang.ExceptionInfo e
                   (if (run-lock/refused? e)
                     (do (binding [*out* *err*] (println (ex-message e)) (flush))
                         (System/exit 3))
                     (throw e))))]
    (pp/pprint
     (select-keys run
                  [:selector-seam :trace-path :trace-before :trace-after
                   :receipt-path :tick-run-record]))
    (flush)))
