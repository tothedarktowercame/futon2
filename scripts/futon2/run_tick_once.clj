(ns futon2.run-tick-once
  "Run one on-demand WM tick and leave a Lean-shaped receipt."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.efe :as efe]
            [futon2.report.war-machine :as wm])
  (:import (java.time Instant LocalDate ZoneId)))

(def ^:private utc-zone
  (ZoneId/of "UTC"))

(def ^:private live-selector-var
  'futon3c.peripheral.live-wm-selection/validated-selection)

(def ^:private stub-selector-name
  "first-ranked-authorized-mission")

(defn- today-date-string []
  (str (LocalDate/now utc-zone)))

(defn- trace-path-for-date [date-str]
  (str (System/getProperty "user.home")
       "/code/futon2/data/wm-trace/wm-trace-" date-str ".edn"))

(defn- receipt-path-for-date [date-str]
  (str (System/getProperty "user.home")
       "/code/futon2/holes/labs/wm-contract/tick-run-record-" date-str ".edn"))

(defn- trace-stat [path]
  (let [f (io/file path)]
    (when (.exists f)
      {:size (.length f)
       :mtime (.lastModified f)})))

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

(defn- evidence-basis [days]
  (let [limit (#'wm/session-evidence-limit)
        since (#'wm/since-str days)
        result (#'wm/fetch-evidence-result :limit limit :since since)
        entries (or (:entries result) [])]
    {:count (count entries)
     :max-at (latest-at entries)
     :diagnostic (dissoc result :entries)}))

(defn- tick-run-record [started-at basis result selector-seam trace-written?]
  (let [input-status (:input-status result)
        preference-stack (:preference-stack result)]
    {:startedAt started-at
     :basisCount (:count basis)
     :basisMaxAt (:max-at basis)
     :inputsRead (long (or (:inputs-read input-status) 0))
     :inputIssues (long (count (:issues input-status)))
     :preferenceLayers (long (count preference-stack))
     :traceWritten (boolean trace-written?)
     :selectorSeam selector-seam}))

(defn- write-receipt! [path record]
  (io/make-parents path)
  (spit path (str (pr-str record) "\n"))
  path)

(defn- parse-days [args]
  (if-let [s (first args)]
    (Long/parseLong s)
    14))

(defn- run-tick-once* [days]
  (let [started-at (str (Instant/now))
        date-str (today-date-string)
        trace-path (trace-path-for-date date-str)
        before-trace (trace-stat trace-path)
        basis (evidence-basis days)
        {:keys [selector selector-seam selector-resolution-error]} (selector-seam)
        generated (wm/generate-war-machine
                   days
                   {:trace? true
                    :include-advisory-lanes? false
                    :strategic-selection-fn selector})
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
        record (tick-run-record started-at basis result selector-seam trace-written?)
        receipt-path (receipt-path-for-date date-str)]
    (write-receipt! receipt-path record)
    {:days days
     :selector-seam selector-seam
     :trace-path trace-path
     :trace-before before-trace
     :trace-after after-trace
     :receipt-path receipt-path
     :tick-run-record record
     :result result
     :basis basis}))

(defn run-tick-once
  "Callable one-shot entrypoint for WM-RUN1."
  ([] (run-tick-once 14))
  ([days]
   (run-tick-once* days)))

(defn -main [& args]
  (let [days (parse-days args)
        run (run-tick-once days)]
    (pp/pprint
     (select-keys run
                  [:selector-seam :trace-path :trace-before :trace-after
                   :receipt-path :tick-run-record]))
    (flush)))
