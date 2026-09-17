(ns futon2.aif.evidence-emit
  "Best-effort Evidence Landscape emitter for War Machine ticks.

   Disabled by default. Set FUTON2_WM_EMIT_EVIDENCE to 1/true/yes/on to POST
   compact tick summaries to {FUTON3C_EVIDENCE_BASE:-http://127.0.0.1:7070}."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [futon2.aif.pattern-registry :as pattern-registry])
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
  ([] (evidence-base (System/getenv)))
  ([env]
   (or (not-empty (get env "FUTON2_WM_EMIT_BASE"))
       (pattern-registry/configured-evidence-base env))))

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

;; --- cascade-decision grain (SPEC flat-removal H4, 2026-09-17) -------------
;; The tick's decision is a cascade decision or a typed abstention; the flat
;; ranked-action field no longer exists. The emitted entry carries the chosen
;; cascade's target, its enacted first step, the posterior mass and beta, and —
;; for an abstention — the refusals grouped by kind (a readiness state, not
;; an error).

(defn- cascade-decision?
  [decision]
  (= :cascade-selection-posterior (get-in decision [:selection-law :applied])))

(defn- abstention?
  [decision]
  (and (map? decision) (= :abstained (:status decision))))

(defn- refusals-by-kind
  [refusals]
  (into (sorted-map)
        (map (fn [[kind rs]] [kind (count rs)]))
        (group-by :kind (vec refusals))))

(defn- cascade-lane
  "The cascade-lane verdicts (record's :act-gate-verdicts): per candidate mission, the
   cascade's pass/fail, the ΔG its cascade rollout achieves, and the ΔG source."
  [tick]
  (mapv (fn [e] {:mission (:mission e)
                 :verdict (:verdict e)
                 :coverage-score-delta (:coverage-score-delta e)
                 :source (:coverage-score-source e)})
        (:act-gate-verdicts tick)))

(defn- nm [x] (cond (keyword? x) (name x) (nil? x) "?" :else (str x)))
(defn- r2 [x] (when (number? x) (/ (Math/round (* 100.0 (double x))) 100.0)))

(defn- pattern-id
  [p]
  (if (map? p) (str (or (:id p) (:cascade-id p))) (str p)))

(defn- abstention-text
  [decision problems]
  (str "War Machine · abstained (readiness, not an error)."
       (when-let [by-kind (not-empty (refusals-by-kind (:refusals decision)))]
         (str "\nRefusals by kind: "
              (str/join ", " (map (fn [[k n]] (str (nm k) " ×" n)) by-kind))))
       (when-let [cp (not-empty (refusals-by-kind (:refusals problems)))]
         (str "\nUnassembled cascade problems by kind: "
              (str/join ", " (map (fn [[k n]] (str (nm k) " ×" n)) cp))))))

(defn- cascade-decision-text
  [tick decision]
  (let [action (:action decision)
        target (:cascade-id action)
        enacted-step (some-> (:precedence action) first pattern-id)
        lane (cascade-lane tick)
        outcome (:realized-outcome tick)
        lane-str (when (seq lane)
                   (str/join "; "
                             (map (fn [e] (str (:mission e) " "
                                               (case (:verdict e) :pass "✓" :fail "✗" "·")
                                               (when (number? (:coverage-score-delta e)) (str " (ΔG " (r2 (:coverage-score-delta e)) ")"))))
                                  lane)))]
    (str "War Machine · " (nm (:mode tick))
         " · cascade " (nm target)
         (when enacted-step (str " → enacts " enacted-step))
         " (posterior mass " (r2 (:chosen-action-mass decision))
         ", β " (r2 (get-in decision [:beta :value]))
         " " (nm (get-in decision [:beta :status])) ")."
         (when lane-str (str "\nCascade lane: " lane-str))
         (when-let [en (or (get-in tick [:enactment :mission]) (get-in tick [:enactment :policy]))]
           (str "\nEnacted: " en))
         (when (number? (:realized-score outcome))
           (str "\nRealized G " (r2 (:realized-score outcome)) " vs expected " (r2 (:expected-score outcome)))))))

(defn- tick-text
  "Human-readable account of the tick: the chosen cascade and its enacted
   first step, the cascade-lane verdicts, and realized-vs-expected — or, for
   an abstention, the refusals that made the tick unready."
  [tick]
  (let [decision (:decision tick)
        problems (:cascade-problems tick)]
    (if (abstention? decision)
      (abstention-text decision problems)
      (when (cascade-decision? decision)
        (cascade-decision-text tick decision)))))

(defn compact-body
  "Return the compact WM tick summary sent to the shared evidence bus.
   Carries the cascade-decision grain: the chosen cascade's target, its
   enacted first step, posterior mass, beta with status, the cascade-lane
   verdicts — and, for an abstention, refusals grouped by kind."
  [tick]
  (let [decision (:decision tick)
        outcome (:realized-outcome tick)
        action (:action decision)]
    (if (abstention? decision)
      {:mode (:mode tick)
       :decision :abstained
       :refusals-by-kind (refusals-by-kind (:refusals decision))
       :cascade-problems-refusals-by-kind
       (refusals-by-kind (get-in tick [:cascade-problems :refusals]))
       :cascade-lane (cascade-lane tick)
       :belly (:belly tick)
       :gates (gate-summary tick)
       :trigger (get-in tick [:wm-version :trigger])
       :text (tick-text tick)
       :at (or (:timestamp tick) (str (Instant/now)))}
      {:mode (:mode tick)
       :decision (:cascade-id action)
       :kind (:kind action)
       :enacted-step (some-> (:precedence action) first pattern-id)
       :posterior-mass (:chosen-action-mass decision)
       :beta (get-in decision [:beta :value])
       :beta-status (get-in decision [:beta :status])
       :G (:controller-score decision)
       :cascade-lane (cascade-lane tick)
       :belly (:belly tick)
       :gates (gate-summary tick)
       :enacted (or (get-in tick [:enactment :mission])
                    (get-in tick [:enactment :policy])
                    (:enacted tick))
       :realized-score (:realized-score outcome)
       :expected-score (:expected-score outcome)
       :trigger (get-in tick [:wm-version :trigger])
       :candidates (count (get-in decision [:selection-law :posterior]))
       :text (tick-text tick)
       :at (or (:timestamp tick) (str (Instant/now)))})))

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
