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

(defn- action-target
  [action]
  (or (:target action)
      (:target-class action)
      (:policy action)
      (:mission action)))

;; --- policy/cascade grain (claude-16's emitter-upgrade, 2026-07-04) -------------
;; The ranking machinery rates cascades under :risk-mode (M-evaluate-policies /
;; M-G-over-cascades are live in the rank lanes; cd0d25d flipped :risk-mode :kl into
;; production). But the emitter only ever wrote action-grain fields (:G/:candidates),
;; so a reader of the Evidence Landscape sees "top actions by G" — the cascade grain
;; vanished before evidence. These helpers carry it through. Additive; the live-efe-map
;; WM-attention layer (C5) inherits cascade-level pulses at the same time.

(defn- winner
  "Rank-1 ranked action. It carries the policy-grain EFE decomposition and the
   :risk-mode that actually scored the tick — the decision's :action holds only
   type/target/rationale, so the scoring provenance has to come from here."
  [tick]
  (first (:ranked-actions tick)))

(defn- g-breakdown
  "The winning action's EFE decomposition — the *why* behind :G, not just the total.
   Short keys; nil/non-numeric legs dropped."
  [w]
  (not-empty
   (into {} (keep (fn [[in out]] (when (number? (get w in)) [out (get w in)]))
                  {:G-core :core :G-risk :risk :G-ambiguity :ambiguity :predictability-bonus :info
                   :G-goal-outcome :goal-outcome :homeostatic-pressure :survival
                   :controller-augmentation :augmentation :gap-exploration-bonus :gap}))))

(defn- cascade-lane
  "The cascade-lane verdicts (record's :act-gate-verdicts): per candidate mission, the
   cascade's pass/fail, the ΔG its cascade rollout achieves, and the ΔG source. This is
   the cascade grain M-G-over-cascades put under the ranking — computed upstream in the
   live loop (close_loop/enact cascade-lane) and previously dropped before evidence."
  [tick]
  (mapv (fn [e] {:mission (:mission e)
                 :verdict (:verdict e)
                 :coverage-score-delta (:coverage-score-delta e)
                 :source (:coverage-score-source e)})
        (:act-gate-verdicts tick)))

(defn- nm [x] (cond (keyword? x) (name x) (nil? x) "?" :else (str x)))
(defn- r2 [x] (when (number? x) (/ (Math/round (* 100.0 (double x))) 100.0)))

(defn- tick-text
  "Human-readable account of the tick: mode, the chosen policy + its rationale, the G
   with its risk-mode regime, the cascade-lane verdicts, and realized-vs-expected. This
   is what a reader opens to understand what the WM did and why — not a metrics row."
  [tick]
  (let [decision (:decision tick)
        action (:action decision)
        w (winner tick)
        outcome (:realized-outcome tick)
        target (when (map? action) (action-target action))
        dtype (if (map? action) (:type action) action)
        rationale (when (map? action) (:rationale action))
        lane (cascade-lane tick)
        lane-str (when (seq lane)
                   (str/join "; "
                             (map (fn [e] (str (:mission e) " "
                                               (case (:verdict e) :pass "✓" :fail "✗" "·")
                                               (when (number? (:coverage-score-delta e)) (str " (ΔG " (r2 (:coverage-score-delta e)) ")"))))
                                  lane)))]
    (str "War Machine · " (nm (:mode tick)) " · " (nm dtype)
         (when target (str " → " target))
         " (G " (r2 (or (:controller-score decision) (:controller-score w))) ", risk-mode " (nm (:risk-mode w)) ")."
         (when rationale (str "\nWhy: " rationale))
         (when lane-str (str "\nCascade lane: " lane-str))
         (when-let [en (or (get-in tick [:enactment :mission]) (get-in tick [:enactment :policy]))]
           (str "\nEnacted: " en))
         (when (number? (:realized-score outcome))
           (str "\nRealized G " (r2 (:realized-score outcome)) " vs expected " (r2 (:expected-score outcome)))))))

(defn compact-body
  "Return the compact WM tick summary sent to the shared evidence bus.
   Carries both action-grain fields (back-compat) AND the policy/cascade grain the
   ranking actually used: :risk-mode regime, the :G-breakdown decomposition, and the
   :cascade-lane verdicts — plus a readable :text. (claude-16 emitter-upgrade.)"
  [tick]
  (let [decision (:decision tick)
        action (:action decision)
        outcome (:realized-outcome tick)
        w (winner tick)]
    {:mode (:mode tick)
     :decision (if (map? action) (:type action) action)
     :target (when (map? action) (action-target action))
     :G (or (:controller-score decision) (:controller-score action) (:controller-score w))
     :risk-mode (:risk-mode w)
     :G-breakdown (g-breakdown w)
     :cascade-lane (cascade-lane tick)
     :belly (:belly tick)
     :gates (gate-summary tick)
     :enacted (or (get-in tick [:enactment :mission])
                  (get-in tick [:enactment :policy])
                  (:enacted tick))
     :realized-score (:realized-score outcome)
     :expected-score (:expected-score outcome)
     :trigger (get-in tick [:wm-version :trigger])
     :candidates (or (:candidates tick) (count (:ranked-actions tick)))
     :text (tick-text tick)
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
