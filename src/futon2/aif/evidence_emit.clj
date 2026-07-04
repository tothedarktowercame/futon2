(ns futon2.aif.evidence-emit
  "Best-effort Evidence Landscape emitter for War Machine ticks.

   Disabled by default. Set FUTON2_WM_EMIT_EVIDENCE to 1/true/yes/on to POST
   compact tick summaries to {FUTON3C_EVIDENCE_BASE:-http://localhost:7070}."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.string :as str])
  (:import (java.time Instant)))

(def ^:private evidence-path "/api/alpha/evidence")
(def ^:private post-timeout-ms 2000)

(defn enabled?
  "True when WM evidence emission is explicitly enabled."
  []
  (contains? #{"1" "true" "yes" "on"}
             (some-> (System/getenv "FUTON2_WM_EMIT_EVIDENCE")
                     str/trim
                     str/lower-case)))

(defn evidence-base
  "Where WM tick evidence is POSTed. FUTON2_WM_EMIT_BASE takes precedence so the emit
   target can differ from the WM's READ base (FUTON3C_EVIDENCE_BASE) — e.g. emit to the
   server the web viewer reads, while the tick still reads its own local store."
  []
  (or (not-empty (System/getenv "FUTON2_WM_EMIT_BASE"))
      (not-empty (System/getenv "FUTON3C_EVIDENCE_BASE"))
      "http://localhost:7070"))

(defn- evidence-url
  [base]
  (str (str/replace base #"/+$" "") evidence-path))

(defn- basis-tag
  [tick]
  (case (get-in tick [:wm-version :trigger])
    :duree-click-regulated "wm-click"
    :wallclock-cron "wm-cron"
    nil))

(defn- gate-summary
  [tick]
  (if-let [gates (:gates tick)]
    gates
    (let [counts (frequencies (map :verdict (:act-gate-verdicts tick)))]
      {:pass (long (get counts :pass 0))
       :fail (long (get counts :fail 0))})))

(defn- action-target
  [action]
  (or (:target action)
      (:target-class action)
      (:policy action)
      (:mission action)))

(defn compact-body
  "Return the compact WM tick summary sent to the shared evidence bus."
  [tick]
  (let [decision (:decision tick)
        action (:action decision)
        outcome (:realized-outcome tick)]
    {:mode (:mode tick)
     :decision (if (map? action) (:type action) action)
     :target (when (map? action) (action-target action))
     :G (or (:G-total decision) (:G-total action))
     :belly (:belly tick)
     :gates (gate-summary tick)
     :enacted (or (get-in tick [:enactment :mission])
                  (get-in tick [:enactment :policy])
                  (:enacted tick))
     :realized-G (:realized-G outcome)
     :expected-G (:expected-G outcome)
     :trigger (get-in tick [:wm-version :trigger])
     :candidates (or (:candidates tick) (count (:ranked-actions tick)))
     :at (or (:timestamp tick) (str (Instant/now)))}))

(defn evidence-entry
  "Build the Evidence Landscape entry for one WM tick."
  [tick]
  {:type "coordination"
   :claim-type "step"
   :author "war-machine"
   ;; ref/type must be a value the EvidenceEntry shape enum accepts ("war-machine"
   ;; is rejected with invalid-entry). The WM is semantically an agent; ref/id +
   ;; author + the wm-tick tag keep it cleanly filterable. (Review fix, claude-10.)
   :subject {:ref/type "agent"
             :ref/id "war-machine"}
   :tags (cond-> ["wm-tick"]
           (basis-tag tick) (conj (basis-tag tick)))
   :body (compact-body tick)})

(defn post-evidence!
  [entry]
  (http/post (evidence-url (evidence-base))
             {:headers {"Content-Type" "application/json"
                        "Accept" "application/json"}
              :body (json/generate-string entry)
              :timeout post-timeout-ms
              :throw false}))

(defn- warn!
  [message & more]
  (binding [*out* *err*]
    (apply println (str (Instant/now) " WARN wm evidence emit:" message) more)))

(defn emit!
  "POST one compact WM tick summary when FUTON2_WM_EMIT_EVIDENCE is on.

   Best-effort by contract: never throws into the WM tick."
  [tick]
  (when (enabled?)
    (try
      (let [resp (post-evidence! (evidence-entry tick))
            status (:status resp)]
        (when-not (and (integer? status) (<= 200 status 299))
          (warn! "POST failed" {:status status :body (:body resp)}))
        resp)
      (catch Throwable t
        (warn! (.getMessage t))
        nil))))
