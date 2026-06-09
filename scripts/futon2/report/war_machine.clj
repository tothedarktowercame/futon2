(ns futon2.report.war-machine
  "War Machine — strategic synthesis scans.

   Harmonizes terminal vocabularies across the futon stack into a
   unified observation of strategic state:

   | Source vocabulary          | What it contributes               |
   |----------------------------|-----------------------------------|
   | joe-terminal (futon5a)     | Personal life signals             |
   | jsdq-terminal (futon5a)    | Market interface signals          |
   | peripheral-aif (futon3c)   | Stack observation channels        |
   | logic model (futon5a)      | Dimensions, workstreams, ticks    |
   | sorry topology (futon5a)   | Constraint domain, typed holes    |
   | holistic argument (futon3) | Structural claims, loop arrows    |

   Each scan function follows the g-observe pattern from cyberants
   (futon2/src/ants/aif/observe.clj): read external state, normalize,
   return a data map. The scan functions ARE the war machine's
   observation layer — the AIF 'o' for the strategic domain.

   Scan → Observation → Tick pattern (cf. joe_hud.clj):
     scan-X [days] → {data}       (raw observation)
     generate-war-machine [days]   (compose all scans)
     render-war-machine [data]     (produce markdown)

   Invariant: WM-I1 (read-only observer — no writes to stack).
   Pattern:   war-machine/operational-not-decorative"
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn]
            [clojure.java.shell :as shell]
            [clojure.set]
            [clojure.string :as str]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.anticipation :as anticipation]
            [futon2.aif.adapters.interest-network :as interest-net]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.free-energy :as fe]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.observation :as obs]
            [futon2.aif.pattern-registry :as pattern-registry]
            [futon2.aif.policy :as policy]
            [futon2.aif.precision :as precision]
            [futon2.aif.preferences :as pref]
            [futon2.aif.sorry-registry :as sorry-registry]
            [futon2.aif.trace :as trace]
            [futon2.aif2.tension :as tension])
  (:import (java.time LocalDate ZoneId ZonedDateTime)
           (java.time.format DateTimeFormatter)))

;; ---------------------------------------------------------------------------
;; Config
;; ---------------------------------------------------------------------------

(def ^:private home (System/getProperty "user.home"))
(def ^:private futon3c-url
  (or (System/getenv "FUTON3C_EVIDENCE_BASE")
      (System/getenv "FUTON3C_SERVER")
      (str "http://localhost:" (or (System/getenv "FUTON3C_PORT") "7070"))))
(def ^:private tz (ZoneId/of "Europe/London"))
(def ^:private futon5a-root (str home "/code/futon5a"))
(def ^:private strategic-vocabulary-path
  (str futon5a-root "/data/war-machine-strategic-vocabulary.edn"))
(def ^:private vsatarcs-status-script
  (str home "/code/futon4/scripts/build-invariant-state-projection.bb"))
(def ^:private futon1a-url
  (or (System/getenv "FUTON1A_URL") "http://localhost:7071"))
(def ^:private futon1a-penholder
  (or (System/getenv "FUTON1A_PENHOLDER") "api"))
(def ^:private g-total-tie-epsilon 1.0e-6)
(def ^:private capability-star-map-path
  (str home "/code/futon0/holes/missions/M-capability-star-map.graph.edn"))
(def ^:private live-star-map-goal :wm-overnight-unsupervised)
(def ^:private live-star-map-efe-weights
  {:graph-applicability-penalty 5.0
   :graph-ascent-weight 6.0
   :graph-body-weight 3.0
   ;; M-wm-policies Track-1 (regulator-swept P=4, operator-consented 2026-06-09):
   ;; off-map work no longer scores a free 0; body is next-step (leaf-aware), not
   ;; whole-mission hole-count; ascent ignores already-:satisfied caps. Flips the
   ;; live top from M-emacs-cursor-peripheral to on-ascent work (star-map / leaf).
   :graph-off-map-penalty 4.0
   :graph-body-mode :leaf
   :graph-ascent-status-aware? true})
(defonce ^:private capability-star-map-cache (atom nil))
(def ^:private mission-fold-view-path
  (str home "/code/futon6/data/mission-fold-view.edn"))
(def ^:private mission-domain-ratified-path
  (str home "/code/futon6/data/mission-domain-ratified.edn"))
(def ^:private forward-model-centrality-path
  (str home "/code/futon7/holes/M-futon-forward-model.centrality.json"))
(def ^:private forward-model-roi-results-path
  (str home "/code/futon7/holes/M-futon-forward-model.roi-results.edn"))
(def ^:private live-gap-view-efe-weights
  {:gap-weight 6.0})
(defonce ^:private mission-fold-view-cache (atom nil))
(defonce ^:private mission-domain-ratified-cache (atom nil))
(defonce ^:private centrality-cache (atom nil))
(defonce ^:private roi-results-cache (atom nil))
(defonce !last-wm-inputs (atom nil))

(def ^:private all-repos
  "All repos in the stack, classified by workstream.

   Workstream rules:
     :stack         - infrastructure repos (futon0..futon5a)
     :mathematics   - futon6 (math dictionary, proofs)
     :portfolio     - landscape/intelligence work (futon7*)
     :consulting    - paid or paid-track work delivered for clients
                      (vsat = billable; vsat.wiki = billable wiki sidecar;
                      npt = pro-bono, expected to convert)

   Note: ~/npt is currently a plain directory, not a git repo, so it
   contributes zero commit-evidence here.  When npt is git-init'ed (or
   when invoice-evidence is wired), it will start contributing."
  [{:label "futon0"  :path (str home "/code/futon0")  :workstream :stack}
   {:label "futon1"  :path (str home "/code/futon1")  :workstream :stack}
   {:label "futon1a" :path (str home "/code/futon1a") :workstream :stack}
   {:label "futon2"  :path (str home "/code/futon2")  :workstream :stack}
   {:label "futon3"  :path (str home "/code/futon3")  :workstream :stack}
   {:label "futon3a" :path (str home "/code/futon3a") :workstream :stack}
   {:label "futon3b" :path (str home "/code/futon3b") :workstream :stack}
   {:label "futon3c" :path (str home "/code/futon3c") :workstream :stack}
   {:label "futon4"  :path (str home "/code/futon4")  :workstream :stack}
   {:label "futon5"  :path (str home "/code/futon5")  :workstream :stack}
   {:label "futon5a" :path (str home "/code/futon5a") :workstream :stack}
   {:label "futon6"  :path (str home "/code/futon6")  :workstream :mathematics}
   {:label "futon7"  :path (str home "/code/futon7")  :workstream :portfolio}
   {:label "futon7a" :path (str home "/code/futon7a") :workstream :portfolio}
   ;; Consulting: vsat + vsat.wiki are billable client work; npt is pro-bono.
   ;; Reclassified vsat.wiki from :portfolio (was a misfit) on 2026-04-24.
   {:label "vsat"      :path (str home "/vsat")      :workstream :consulting}
   {:label "vsat.wiki" :path (str home "/vsat.wiki") :workstream :consulting}
   {:label "npt"       :path (str home "/npt")       :workstream :consulting}
   ;; UKRN-S services simulation — active consulting-track work (added 2026-05-30;
   ;; was producing real commits but invisible to the WM, a cause of "0% consulting").
   {:label "ukrn-services-simulation" :path (str home "/code/ukrn-services-simulation") :workstream :consulting}])

;; NOTE (2026-05-30): commit-count is a WEAK proxy for billable consulting —
;; vsat has 219 commits but its value is billable HOURS, and recent consulting
;; (the Eric/Brookes work) leaves little commit trace in these repos. The real
;; consulting signal is the billable-hours ledger (~/code/ledger/, to be wired)
;; + the paid-invoice log (~/code/invoices/log.edn). Indexing ukrn-services-sim
;; fixes the immediate false-zero; wiring ledger/invoice evidence is the real fix.

;; ---------------------------------------------------------------------------
;; Helpers (same patterns as joe_hud.clj)
;; ---------------------------------------------------------------------------

(defn- http-get-json [url]
  (try
    (let [resp (http/get url {:headers {"Accept" "application/json"}
                              :timeout 5000
                              :throw false})]
      (when (= 200 (:status resp))
        (json/parse-string (:body resp) true)))
    (catch Exception _ nil)))

(defn- git [repo-path & args]
  (let [{:keys [exit out]} (apply shell/sh "git" "-C" repo-path args)]
    (when (zero? exit) (str/trim out))))

(defn- read-edn-file [path]
  (try
    (when (.exists (java.io.File. path))
      (read-string (slurp path)))
    (catch Exception _ nil)))

(defn- read-json-file [path]
  (try
    (when (.exists (java.io.File. path))
      (json/parse-string (slurp path) false))
    (catch Exception _ nil)))

(defn- capability-star-map []
  (let [{:keys [path graph]} @capability-star-map-cache]
    (if (= path capability-star-map-path)
      graph
      (let [graph (read-edn-file capability-star-map-path)]
        (reset! capability-star-map-cache {:path capability-star-map-path
                                           :graph graph})
        graph))))

(defn- live-star-map-efe-opts
  [base-opts]
  (if-let [graph (capability-star-map)]
    (merge base-opts
           live-star-map-efe-weights
           {:capability-graph graph
            :pre-registered-goal live-star-map-goal})
    base-opts))

(defn- normalize-mission-gap-view
  [fold-view]
  (when (map? fold-view)
    (->> (:missions fold-view)
         (keep (fn [{:keys [mission gap-score]}]
                 (when (and mission (number? gap-score))
                   [(str mission) (double gap-score)])))
         (into {}))))

(defn- normalize-mission-domain-view
  [domain-view]
  (when (map? domain-view)
    (->> (:missions domain-view)
         (keep (fn [{:keys [mission domain]}]
                 (when (and mission domain)
                   [(str mission) domain])))
         (into {}))))

(defn- mission-domain-ratified
  []
  (let [{:keys [path domain-view]} @mission-domain-ratified-cache]
    (if (= path mission-domain-ratified-path)
      domain-view
      (let [domain-view (normalize-mission-domain-view
                         (read-edn-file mission-domain-ratified-path))]
        (reset! mission-domain-ratified-cache {:path mission-domain-ratified-path
                                               :domain-view domain-view})
        domain-view))))

(defn- mission-gap-view
  []
  (let [{:keys [path gap-view]} @mission-fold-view-cache]
    (if (= path mission-fold-view-path)
      gap-view
      (let [raw-gap-view (normalize-mission-gap-view
                          (read-edn-file mission-fold-view-path))
            domain-view (mission-domain-ratified)
            gap-view (if (seq domain-view)
                       (into {}
                             (filter (fn [[mission _gap-score]]
                                       (= :local-capability
                                          (get domain-view mission))))
                             raw-gap-view)
                       {})]
        (reset! mission-fold-view-cache {:path mission-fold-view-path
                                         :gap-view gap-view})
        gap-view))))

(defn- live-gap-view-efe-opts
  [base-opts]
  (if-let [gap-view (mission-gap-view)]
    (merge base-opts
           live-gap-view-efe-weights
           {:mission-gap-view gap-view})
    base-opts))

(defn- centrality-joint-map []
  (let [{:keys [path centrality]} @centrality-cache]
    (if (= path forward-model-centrality-path)
      centrality
      (let [centrality (->> (or (read-json-file forward-model-centrality-path) {})
                            (keep (fn [[mission row]]
                                    (when-let [c (get row "c_joint")]
                                      [(str mission) (double c)])))
                            (into {}))]
        (reset! centrality-cache {:path forward-model-centrality-path
                                  :centrality centrality})
        centrality))))

(defn- valuable-path-set [centrality]
  (->> centrality
       (sort-by (comp - val))
       (take 25)
       (map key)
       set))

(defn- roi-results []
  (let [{:keys [path results]} @roi-results-cache]
    (if (= path forward-model-roi-results-path)
      results
      (let [results (read-edn-file forward-model-roi-results-path)]
        (reset! roi-results-cache {:path forward-model-roi-results-path
                                   :results results})
        results))))

(defn- normalize-feature-key [v]
  (-> (if (keyword? v) (name v) (str v))
      (str/lower-case)
      (str/replace #"^[a-z]\\+" "")
      (str/replace #"[^a-z0-9]+" "")))

(defn- roi-feature-map []
  (->> (get-in (roi-results) [:default-effort :features])
       (keep (fn [{:keys [id] :as feature}]
               (when id
                 [(normalize-feature-key id)
                  (select-keys feature [:id :expected-roi-gbp])])))
       (into {})))

(defn- roi-map-for-missions [missions]
  (let [features (roi-feature-map)]
    (->> missions
         (keep (fn [{:keys [id title]}]
                 (let [mission-key (normalize-feature-key (or title id))
                       match (some (fn [[feature-key feature]]
                                     (when (or (str/includes? mission-key feature-key)
                                               (str/includes? feature-key mission-key))
                                       feature))
                                   features)]
                   (when (and id match)
                     [id {:expected-roi-gbp
                          (double (or (:expected-roi-gbp match) 0.0))
                          :feature-id (:id match)}]))))
         (into {}))))

(defn pin-wm-snapshot
  "Return the last live WM ranking input bundle with structure/grounding data.
   Returns nil until `judge` has run once in this JVM."
  []
  (when-let [snapshot @!last-wm-inputs]
    (let [centrality (centrality-joint-map)]
      (merge snapshot
             {:structure {:capability-graph (capability-star-map)
                          :pre-registered-goal live-star-map-goal
                          :mission-gap-view (mission-gap-view)
                          :mission-domain-view (mission-domain-ratified)}
              :grounding {:centrality centrality
                          :valuable-path (valuable-path-set centrality)
                          :roi-map (roi-map-for-missions (:wm-missions snapshot))}
              :live-weights (merge live-star-map-efe-weights
                                   live-gap-view-efe-weights)}))))

(declare apply-anamnesis-tiebreak filter-live-open-mission-ranked-actions)

(defn rollout-snapshot-under-weights
  "Replay a pinned WM snapshot under injected EFE weights. Pure for the same
   snapshot and weight-overrides; no scanning or live state mutation."
  ([snapshot weight-overrides]
   (rollout-snapshot-under-weights snapshot weight-overrides {}))
  ([snapshot weight-overrides {:keys [k] :or {k 5}}]
   (let [structure (:structure snapshot)
         opts (merge (live-star-map-efe-opts
                      (live-gap-view-efe-opts {:time-pressure 0}))
                     (select-keys structure
                                  [:capability-graph :pre-registered-goal
                                   :mission-gap-view :mission-domain-view])
                     weight-overrides)
         ranked (->> (efe/rank-actions (:wm-state snapshot)
                                        (:candidates snapshot)
                                        opts)
                     apply-anamnesis-tiebreak
                     (filter-live-open-mission-ranked-actions
                      (:wm-missions snapshot)))
         admissible (filterv #(fm/can-execute? (:wm-state snapshot)
                                               (:action %))
                             ranked)
         bundle (vec (take k admissible))]
     {:ranked ranked
      :admissible admissible
      :bundle bundle
      :opts opts})))

(defn- substrate-get-json [url]
  (try
    (let [resp (http/get url {:headers {"Accept" "application/json"
                                        "X-Penholder" futon1a-penholder}
                              :timeout 5000
                              :throw false})]
      (when (= 200 (:status resp))
        (json/parse-string (:body resp) true)))
    (catch Exception _ nil)))

(defn- url-encode [s]
  (java.net.URLEncoder/encode (str s) "UTF-8"))

(defn- fetch-hyperedges-by-type
  [hx-type]
  (vec
   (or (:hyperedges
        (substrate-get-json
         (str futon1a-url "/api/alpha/hyperedges?type="
              (url-encode hx-type)
              "&limit=500")))
       [])))

(defn- real-endpoints
  [hx]
  (vec (remove #(and (string? %) (str/starts-with? % "dir:"))
               (:hx/endpoints hx))))

(defn- hx-prop
  [hx k]
  (or (get-in hx [:hx/props k])
      (get-in hx [:hx/props (keyword k)])
      (get-in hx [:hx/props (str k)])))

(defn- normalize-mission-id
  [mission-name]
  (str/replace-first (str mission-name) #"^M-" ""))

(defn- action-target-key
  [target]
  (cond
    (keyword? target) (if-let [ns-part (namespace target)]
                        (str ns-part "/" (name target))
                        (name target))
    (string? target) target
    :else (some-> target str)))

(defn- rerank
  [ranked-actions]
  (->> ranked-actions
       (map-indexed (fn [i entry] (assoc entry :rank (inc i))))
       vec))

(defn- live-open-mission-ranked-entry?
  [missions entry]
  (let [action (:action entry)]
    (or (not= :open-mission (:type action))
        (mission-registry/live-mission-target? missions (:target action)))))

(defn- canonicalize-open-mission-ranked-entry
  [entry]
  (if (= :open-mission (get-in entry [:action :type]))
    (let [registry-id (mission-registry/mission-target-id
                       (get-in entry [:action :target]))]
      (cond-> entry
        registry-id (assoc-in [:action :target] registry-id)))
    entry))

(defn- filter-live-open-mission-ranked-actions
  [missions ranked-actions]
  (->> ranked-actions
       (filter #(live-open-mission-ranked-entry? missions %))
       (map canonicalize-open-mission-ranked-entry)
       rerank))

(defn- sorry-doc-index
  []
  (reduce (fn [idx hx]
            (let [endpoint (first (real-endpoints hx))
                  local-id (when endpoint
                             (second (re-find #"/sorry/(.+)$" endpoint)))
                  action-target (when local-id
                                  (str "sorry/" local-id))]
              (cond-> idx
                endpoint (assoc endpoint hx)
                action-target (assoc action-target hx))))
          {}
          (fetch-hyperedges-by-type "code/v05/sorry")))

(defn- mission-doc-index
  []
  (reduce (fn [idx hx]
            (let [endpoint (first (real-endpoints hx))
                  mission-id (some-> (hx-prop hx :mission/id)
                                     normalize-mission-id)]
              (if (and endpoint mission-id (not (str/blank? mission-id)))
                (assoc idx mission-id endpoint)
                idx)))
          {}
          (fetch-hyperedges-by-type "code/v05/mission-doc")))

(defn- compute-delta-t-mission
  [mission-endpoint]
  (if-let [f (requiring-resolve 'futon3c.aif.mission-delta-t/delta-t-mission)]
    (f mission-endpoint)
    {:delta-T 0.0}))

(defn- related-mission-endpoints
  [sorry-doc mission-idx]
  (->> (or (hx-prop sorry-doc :sorry/related-missions) [])
       (keep (fn [mission-name]
               (get mission-idx (normalize-mission-id mission-name))))
       distinct
       vec))

(defn- address-sorry-entry?
  [entry]
  (= :address-sorry (get-in entry [:action :type])))

(defn- tied-g-total?
  [left right]
  (<= (Math/abs (- (double (or (:G-total left) 0.0))
                   (double (or (:G-total right) 0.0))))
      g-total-tie-epsilon))

(defn- partition-tied-groups
  [ranked-actions]
  (reduce (fn [groups entry]
            (if-let [current (peek groups)]
              (if (tied-g-total? (peek current) entry)
                (conj (pop groups) (conj current entry))
                (conj groups [entry]))
              [[entry]]))
          []
          ranked-actions))

(defn- structural-pressure-for-action
  [action {:keys [sorry-idx mission-idx delta-cache]}]
  (if (= :address-sorry (:type action))
    (let [target (action-target-key (:target action))
          sorry-doc (get sorry-idx target)
          mission-endpoints (when sorry-doc
                              (related-mission-endpoints sorry-doc mission-idx))]
      (double
       (reduce (fn [acc mission-endpoint]
                 (let [delta-result (or (get @delta-cache mission-endpoint)
                                        (let [result (compute-delta-t-mission
                                                      mission-endpoint)]
                                          (swap! delta-cache assoc mission-endpoint result)
                                          result))]
                   (+ acc (- 1.0 (double (:mission-T delta-result 0.5))))))
               0.0
               mission-endpoints)))
    0.0))

(defn- anamnesis-concentration-for-entry
  [entry ctx]
  (structural-pressure-for-action (:action entry) ctx))

(defn- enrich-candidates-with-structural-pressure
  [candidates]
  (if-not (some #(= :address-sorry (:type %)) candidates)
    (vec candidates)
    (let [ctx {:sorry-idx (sorry-doc-index)
               :mission-idx (mission-doc-index)
               :delta-cache (atom {})}]
      (mapv (fn [action]
              (assoc action
                     :structural-pressure-per-action
                     (structural-pressure-for-action action ctx)))
            candidates))))

(defn- apply-anamnesis-tiebreak
  [ranked-actions]
  (if (< (count ranked-actions) 2)
    ranked-actions
    (let [groups (partition-tied-groups ranked-actions)
          needs-tiebreak? (some #(and (> (count %) 1)
                                      (every? address-sorry-entry? %))
                                groups)]
      (if-not needs-tiebreak?
        ranked-actions
        (let [ctx {:sorry-idx (sorry-doc-index)
                   :mission-idx (mission-doc-index)
                   :delta-cache (atom {})}]
          (->> groups
               (mapcat (fn [group]
                         (if (and (> (count group) 1)
                                  (every? address-sorry-entry? group))
                           (->> group
                                (map-indexed
                                 (fn [i entry]
                                   {:entry entry
                                    :original-index i
                                    :anamnesis-concentration
                                    (anamnesis-concentration-for-entry entry ctx)}))
                                (sort-by (juxt (comp - :anamnesis-concentration)
                                               :original-index))
                                (mapv (fn [m]
                                        (assoc (:entry m)
                                               :anamnesis-concentration
                                               (:anamnesis-concentration m)))))
                           group)))
               (map-indexed (fn [i entry] (assoc entry :rank (inc i))))
               vec))))))

(defn- since-str
  "Date string for N days ago."
  [days]
  (.toString (.minusDays (LocalDate/now tz) days)))

(defn- parse-iso-date
  "Extract YYYY-MM-DD from an ISO timestamp string."
  [ts-str]
  (try
    (when (and ts-str (>= (count ts-str) 10))
      (subs ts-str 0 10))
    (catch Exception _ nil)))

(defn- count-commits-since
  "Count commits in a repo since a date string."
  [repo-path since]
  (try
    (when (.isDirectory (java.io.File. repo-path))
      (when-let [out (git repo-path "log" "--oneline" "--since" since)]
        (if (str/blank? out) 0 (count (str/split-lines out)))))
    (catch Exception _ 0)))

;; ---------------------------------------------------------------------------
;; Evidence API helpers
;; ---------------------------------------------------------------------------

(defn- fetch-evidence
  "Fetch evidence entries from the API. Returns vec or nil.

   Options:
   - :limit  maximum number of entries to request
   - :since  inclusive ISO date string (YYYY-MM-DD) lower bound"
  [& {:keys [limit since]}]
  (let [params (cond-> []
                 (and (int? limit) (pos? limit))
                 (conj (str "limit=" limit))
                 (string? since)
                 (conj (str "since=" since)))
        qs (when (seq params) (str "?" (str/join "&" params)))]
    (when-let [data (http-get-json (str futon3c-url "/api/alpha/evidence" qs))]
      (when (:ok data)
        (or (:entries data) [])))))

(defn- fetch-missions
  "Fetch mission inventory from the API. Returns vec or nil."
  []
  (when-let [data (http-get-json (str futon3c-url "/api/alpha/missions"))]
    (when (:ok data)
      (or (:missions data) []))))

(defn- normalize-keyword [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else nil))

(defn- event-name [entry]
  (some-> (or (get-in entry [:evidence/body :event])
              (get-in entry [:evidence/body "event"]))
          name))

(defn- evidence-at [entry]
  (some-> (:evidence/at entry) str))

(defn- newer-entry? [candidate prior]
  (let [candidate-at (evidence-at candidate)
        prior-at (evidence-at prior)]
    (cond
      (nil? prior) true
      (and candidate-at prior-at) (pos? (compare candidate-at prior-at))
      candidate-at true
      :else false)))

(defn- latest-by-key
  "Keep the newest entry per derived key."
  [entries key-fn]
  (->> entries
       (reduce (fn [acc entry]
                 (if-let [k (key-fn entry)]
                   (let [prior (get acc k)]
                     (if (newer-entry? entry prior)
                       (assoc acc k entry)
                       acc))
                   acc))
               {})
       vals
       vec))

(defn- severity-rank [severity]
  (case severity
    :critical 2
    :warning 1
    :info 0
    -1))

(defn- family-fired->issue
  [entry]
  (let [body (:evidence/body entry)
        family-id (normalize-keyword (or (:family-id body) (get body "family-id")))
        outcome (normalize-keyword (or (:outcome body) (get body "outcome")))
        detail (or (:detail body) {})
        at (evidence-at entry)]
    (when (= :violation outcome)
      (case family-id
        :obsolescence-recognition/pipeline-tracer
        (let [tracks (->> (:obsolete-artifacts detail)
                          (keep #(normalize-keyword (:track-id %)))
                          (map name)
                          sort
                          vec)
              n (or (:obsolete-count detail) (count tracks))]
          {:severity :warning
           :surface "archaeology"
           :summary (str n " overdue pipeline tracer"
                         (when (not= 1 n) "s")
                         " need close-or-extend decisions")
           :action (if (seq tracks)
                     (str "Review tracks: " (str/join ", " tracks))
                     "Close or extend overdue pipeline tracers")
           :at at
           :source-id (some-> family-id name)})

        :bounded-disposition/mission-doc
        (let [violations (vec (:violations detail))
              repos (->> violations
                         (map (fn [{:keys [repo undecided-count total]}]
                                (str (last (str/split repo #"/"))
                                     " " undecided-count "/" total " unresolved")))
                         (str/join ", "))]
          {:severity :warning
           :surface "mission-doc"
           :summary (str "Mission-doc disposition pressure in "
                         (count violations) " repo"
                         (when (not= 1 (count violations)) "s"))
           :action (if (seq repos)
                     (str "Normalize mission statuses or adjust parser semantics in: " repos)
                     "Normalize mission statuses or adjust parser semantics")
           :at at
           :source-id (some-> family-id name)})

        :metabolic-balance/working-tree
        (let [violators (->> (:per-repo detail)
                             (filter #(contains? #{:high :stop-the-line
                                                   "high" "stop-the-line"}
                                                 (:tier %)))
                             (sort-by :P >)
                             vec)
              top-repo (some-> violators first :repo (str/split #"/") last)
              max-p (or (:max-pressure detail)
                        (some-> violators first :P)
                        0.0)]
          {:severity (if (= :stop-the-line (normalize-keyword (:max-tier detail)))
                       :critical
                       :warning)
           :surface "metabolic"
           :summary (str "Working-tree pressure is "
                         (or (some-> (:max-tier detail) name)
                             (some-> (:max-tier detail) str/lower-case)
                             "elevated"))
           :action (str "Triage dirty trees"
                        (when top-repo
                          (format "; %s dominates at P=%.2f" top-repo (double max-p)))
                        ". Use .futon-disposition.edn for generated/volatile tracked surfaces.")
           :at at
           :source-id (some-> family-id name)})

        {:severity :warning
         :surface "invariant"
         :summary (str "Invariant warning: " (some-> family-id name))
         :action "Inspect the latest :family-fired detail and decide whether to close, classify, or tune the check"
         :at at
         :source-id (some-> family-id name)}))))

(defn- process-event->issue
  [entry]
  (let [body (:evidence/body entry)
        event (event-name entry)
        process-id (or (:process-id body) (get body "process-id"))
        kind (or (:kind body) (get body "kind"))
        severity (normalize-keyword (or (:severity body) (get body "severity")))
        message (or (:message body) (get body "message"))
        at (evidence-at entry)]
    (case event
      "process-alert"
      {:severity (or severity :warning)
       :surface "watchdog"
       :summary (str process-id " " kind)
       :action (str "Inspect " process-id ": " message)
       :at at
       :source-id process-id}

      "process-recovery"
      {:severity :info
       :surface "watchdog"
       :summary (str process-id " recovered")
       :action "No action unless the alert recurs"
       :at at
       :source-id process-id}

      nil)))

(defn- summarize-self-watch
  [entries]
  (let [family-entries (filter #(= "family-fired" (event-name %)) entries)
        latest-family (latest-by-key
                       family-entries
                       #(normalize-keyword (or (get-in % [:evidence/body :family-id])
                                               (get-in % [:evidence/body "family-id"]))))
        family-issues (->> latest-family
                           (keep family-fired->issue))
        process-entries (filter #(contains? #{"process-alert" "process-recovery"}
                                             (event-name %))
                                entries)
        latest-process (latest-by-key
                        process-entries
                        #(or (get-in % [:evidence/body :process-id])
                             (get-in % [:evidence/body "process-id"])))
        process-issues (->> latest-process
                            (keep process-event->issue))
        issues (->> (concat family-issues
                            (remove #(= :info (:severity %)) process-issues))
                    (sort-by (juxt (comp - severity-rank :severity) :at))
                    vec)
        recoveries (->> process-issues
                        (filter #(= :info (:severity %)))
                        (sort-by :at #(compare %2 %1))
                        vec)]
    {:issues issues
     :recoveries recoveries
     :issue-count (count issues)
     :critical-count (count (filter #(= :critical (:severity %)) issues))
     :warning-count (count (filter #(= :warning (:severity %)) issues))}))

(defn scan-self-watch
  "Project self-observation evidence into operator-facing maintenance items.

   Reads two already-emitted surfaces:
   - `:family-fired` boot/probe warnings from archaeology/metabolic checks
   - `process-alert` / `process-recovery` from the futon3c watchdog

   The result is intentionally action-shaped rather than raw-evidence-shaped,
   so War Machine can say what needs fixing instead of only replaying boot
   banners."
  [days]
  (let [entries (fetch-evidence :limit 1000 :since (since-str days))]
    (if (nil? entries)
      {:available? false
       :issues []
       :recoveries []
       :issue-count 0
       :critical-count 0
       :warning-count 0}
      (assoc (summarize-self-watch entries)
             :available? true))))

;; ---------------------------------------------------------------------------
;; Scan 1: Loop Health
;;
;; Maps the 6 arrows in the holistic argument self-improvement loop
;; to evidence freshness. Each arrow is a step in:
;;
;;   work → proof-paths → pattern-discovery → coordination
;;        → self-representation → portfolio-inference → better-work
;;
;; cf. holistic-argument-sketch.md lines 215-226
;; cf. cyberants AIF: g-observe normalizes sensory channels to [0,1]
;; ---------------------------------------------------------------------------

(def ^:private loop-arrows
  "The six arrows of the self-improvement loop.
   Each arrow has evidence-type filters for detecting when it fires."
  [{:id :work→proof
    :label "Work → Proof paths"
    :description "Work produces auditable evidence via gate pipeline"
    :detect-fn
    (fn [entries _] ;; Any chat-turn = work is happening
      (filter #(= "chat-turn" (get-in % [:evidence/body :event])) entries))}

   {:id :proof→patterns
    :label "Proof paths → Pattern discovery"
    :description "Evidence feeds pattern discovery (Baldwin cycle)"
    :detect-fn
    (fn [entries _] ;; PSR/PUR evidence OR goal-typed claims OR pattern mentions
      (filter #(or (#{"pattern-selection" "pattern-outcome"} (:evidence/type %))
                   (= "goal" (:evidence/claim-type %))
                   (when-let [text (get-in % [:evidence/body :text])]
                     (re-find #"(?i)pattern.*select|PSR|PUR|pattern.*record" text)))
              entries))}

   {:id :patterns→coordination
    :label "Patterns → Coordination"
    :description "Patterns inform coordination via ambient retrieval"
    :detect-fn
    (fn [entries _] ;; Context retrieval certificates or retrieval mentions
      (filter #(or (= "context-retrieval" (:evidence/type %))
                   (when-let [text (get-in % [:evidence/body :text])]
                     (re-find #"(?i)context.?retriev|pattern.?retriev|notions.*search" text)))
              entries))}

   {:id :coordination→self-rep
    :label "Coordination → Self-representation"
    :description "Coordination improvements update devmaps/missions"
    :detect-fn
    (fn [_ since] ;; Commits to mission docs, devmaps, or data files
      (let [mission-repos [(str home "/code/futon3c")
                           (str home "/code/futon3b")
                           (str home "/code/futon3")
                           (str home "/code/futon5a")]]
        (->> mission-repos
             (mapcat (fn [rp]
                       (when-let [out (git rp "log" "--format=%aI %s" "--since" since
                                          "--" "holes/missions/" "devmaps/" "data/")]
                         (when-not (str/blank? out)
                           (map (fn [line]
                                  (let [space-idx (.indexOf line " ")
                                        ts (when (pos? space-idx) (subs line 0 space-idx))]
                                    {:evidence/type "git-commit"
                                     :evidence/at ts
                                     :evidence/body {:text line}}))
                                (str/split-lines out))))))
             vec)))}

   {:id :self-rep→inference
    :label "Self-representation → Portfolio inference"
    :description "Self-images feed portfolio inference engine"
    :detect-fn
    (fn [entries _] ;; Portfolio evidence or review/inference mentions
      (filter #(or (str/starts-with? (or (:evidence/type %) "") "portfolio")
                   (when-let [text (get-in % [:evidence/body :text])]
                     (re-find #"(?i)portfolio|inference|review|triage" text)))
              entries))}

   {:id :inference→work
    :label "Portfolio inference → Better work"
    :description "Inference improves work selection"
    :detect-fn
    (fn [entries _] ;; Forum posts = collective coordination outputs
      (filter #(= "forum-post" (:evidence/type %)) entries))}])

(def ^:private arrow-health-freq-k
  "Half-rate parameter for the asymptotic frequency component.
   At count=k freq=0.5; at count=10k freq≈0.91; at count=100k freq≈0.99.
   k=10 places the inflection at the same spot the prior min(1, count/10)
   shape used as its saturation threshold, but the new shape never locks
   at 1.0 — it just approaches asymptotically.  See E-wm-metric-redesign
   (futon3c/holes/missions/E-wm-metric-redesign.md) for the saturation
   diagnosis that motivated this change."
  10.0)

(defn- arrow-health
  "Compute health [0,1) from evidence count and recency.

   Frequency component: freq = count / (count + k) — asymptotic shape;
   approaches 1.0 but never locks (avoids 100% saturation per F1 in
   E-wm-metric-redesign).  At count=k=10 the freq is 0.50.

   Freshness component: 1.0 at seen-today, 0.0 at unseen-in-window."
  [evidence-count days-since-last window-days]
  (let [c (double evidence-count)
        freq (/ c (+ c arrow-health-freq-k))
        fresh (if days-since-last
                (max 0.0 (- 1.0 (/ (double days-since-last) (double window-days))))
                0.0)]
    ;; Geometric mean — both must be nonzero for health
    (Math/sqrt (* freq fresh))))

(defn scan-loop-health
  "Scan the 6 arrows of the holistic argument self-improvement loop.

   For each arrow, detects evidence of it firing in the window.
   Returns a map with :arrows (per-arrow health) and :overall.

   cf. cyberants observe.clj — each arrow is a sensory channel,
   health is the normalized [0,1] observation."
  [days]
  (let [since (since-str days)
        entries (or (fetch-evidence :limit 2000) [])
        ;; Filter entries to window
        window-entries (filter (fn [e]
                                 (when-let [d (parse-iso-date (:evidence/at e))]
                                   (>= (compare d since) 0)))
                               entries)
        arrows (mapv (fn [{:keys [id label description detect-fn]}]
                       (let [matches (detect-fn window-entries since)
                             cnt (count matches)
                             last-date (when (seq matches)
                                         (->> matches
                                              (keep #(parse-iso-date (:evidence/at %)))
                                              sort
                                              last))
                             days-since (when last-date
                                          (let [last-ld (LocalDate/parse last-date)
                                                now-ld (LocalDate/now tz)]
                                            (.getDays (java.time.Period/between last-ld now-ld))))
                             health (arrow-health cnt days-since days)]
                         {:arrow-id id
                          :label label
                          :description description
                          :count cnt
                          :last-seen last-date
                          :days-since days-since
                          :health health}))
                     loop-arrows)
        healthy (filter #(> (:health %) 0.5) arrows)
        overall (if (seq arrows)
                  (/ (reduce + 0.0 (map :health arrows)) (count arrows))
                  0.0)]
    {:arrows arrows
     :overall overall
     :healthy-count (count healthy)
     :total-count (count arrows)
     :loop-complete? (= (count healthy) (count arrows))}))

;; ---------------------------------------------------------------------------
;; Scan 2: Support / Attack
;;
;; Maps the structural claims (S1-S5 support, A1-A4 attack) from the
;; holistic argument to evidence freshness.
;;
;; cf. holistic-argument-sketch.md lines 127-195
;; cf. JSDQ terminal vocabulary — same normalized observation pattern
;; ---------------------------------------------------------------------------

(def ^:private claim-patterns
  "Regex patterns for matching evidence text to structural claims.
   Follows the turn-topic-patterns approach from joe_hud.clj."
  {:S1 {:type :support :label "Evidence discipline works"
        :pattern #"(?i)pattern|evidence|traceab|PSR|PUR|gate|discipline|replicat"}
   :S2 {:type :support :label "AIF framing is generative"
        :pattern #"(?i)active.?inference|AIF|observe.*infer|free.?energy|generative.?model|predictive"}
   :S3 {:type :support :label "Pattern transfer is real"
        :pattern #"(?i)pattern.*transfer|compose.*domain|metaca.*ant|cross.?domain|flexiarg"}
   :S4 {:type :support :label "Commercial demand exists"
        :pattern #"(?i)consult|revenue|commercial|UKRN|Bristol|invoice|paid|prospect"}
   :S5 {:type :support :label "Reflexive architecture is rare"
        :pattern #"(?i)reflexiv|self.?improv|meta.?model|observe.?own|stack.*observ"}
   :A1 {:type :attack :label "Complexity cost"
        :pattern #"(?i)complex|too.?many|cognitive.?load|overwhelm|layers.*process|sprawl"}
   :A2 {:type :attack :label "Solo-developer bottleneck"
        :pattern #"(?i)bottleneck|solo|bus.?factor|single.?point|one.?person|joe.*only"}
   :A3 {:type :attack :label "Commercialisation gap"
        :pattern #"(?i)no.?revenue|unfunded|precarious|gap.*commercial|runway|burn.?rate"}
   :A4 {:type :attack :label "Explanation problem"
        :pattern #"(?i)hard.?to.?explain|jargon|opaque|communicate|legib|outsider"}})

(defn scan-support-attack
  "Scan evidence for structural claim coverage.

   For each of the 9 claims (S1-S5 support, A1-A4 attack), counts
   evidence entries whose text matches the claim's keywords.

   Returns {:claims [...] :support-coverage :attack-coverage}.
   Coverage = fraction of claims with at least one recent evidence entry."
  [days]
  (let [since (since-str days)
        entries (or (fetch-evidence :limit 2000) [])
        window-entries (filter (fn [e]
                                 (when-let [d (parse-iso-date (:evidence/at e))]
                                   (>= (compare d since) 0)))
                               entries)
        claims (mapv
                (fn [[claim-id {:keys [type label pattern]}]]
                  (let [matches (filter
                                 (fn [e]
                                   (let [body (:evidence/body e {})
                                         text (str (or (:text body) "")
                                                   " " (or (:evidence/type e) ""))]
                                     (re-find pattern text)))
                                 window-entries)
                        cnt (count matches)
                        last-date (when (seq matches)
                                    (->> matches
                                         (keep #(parse-iso-date (:evidence/at %)))
                                         sort last))]
                    {:claim-id claim-id
                     :type type
                     :label label
                     :evidence-count cnt
                     :last-evidence last-date
                     :status (cond
                               (>= cnt 5) :strong
                               (>= cnt 1) :present
                               :else :absent)}))
                (sort-by key claim-patterns))
        support-claims (filter #(= :support (:type %)) claims)
        attack-claims (filter #(= :attack (:type %)) claims)
        covered? #(pos? (:evidence-count %))]
    {:claims claims
     :support-coverage (if (seq support-claims)
                         (/ (double (count (filter covered? support-claims)))
                            (count support-claims))
                         0.0)
     :attack-coverage (if (seq attack-claims)
                        (/ (double (count (filter covered? attack-claims)))
                           (count attack-claims))
                        0.0)}))

;; ---------------------------------------------------------------------------
;; Scan 3: Mission Triage
;;
;; Queries the missions API and classifies missions by health.
;; Detects abandoned-in-progress missions (the strategic debt).
;;
;; cf. peripheral-aif-vocabulary.sexp — mission-complete-ratio,
;; blocked-ratio, stall-count channels
;; ---------------------------------------------------------------------------

(defn scan-mission-triage
  "Scan mission inventory for strategic health.

   Queries GET /api/alpha/missions, cross-references with git activity
   to detect abandoned-in-progress missions.

   Returns {:total :by-status :by-repo :abandoned-missions :health}."
  [days]
  (let [since (since-str days)
        missions (or (fetch-missions) [])
        ;; Classify by status
        by-status (frequencies (map #(or (:mission/status %) "unknown") missions))
        ;; Classify by repo
        by-repo (frequencies (map #(or (:mission/repo %) "unknown") missions))
        ;; Detect abandoned: in-progress but no commits in window
        in-progress (filter #(#{"active" "in-progress" "in_progress"}
                               (:mission/status %))
                            missions)
        abandoned (when (seq in-progress)
                    (let [;; Build repo→commit-count cache
                          repo-commits
                          (into {}
                                (for [{:keys [label path]} all-repos]
                                  [label (or (count-commits-since path since) 0)]))]
                      (->> in-progress
                           (filter (fn [m]
                                     (let [repo (:mission/repo m)
                                           commits (get repo-commits repo 0)]
                                       (zero? commits))))
                           vec)))
        total (count missions)
        completed (get by-status "complete" 0)
        blocked (get by-status "blocked" 0)
        active (+ (get by-status "active" 0) (get by-status "in-progress" 0)
                  (get by-status "in_progress" 0))]
    {:total total
     :by-status by-status
     :by-repo by-repo
     :active active
     :completed completed
     :blocked blocked
     :abandoned-count (count (or abandoned []))
     :abandoned-missions (or abandoned [])
     ;; Health: high completion, low abandoned, low blocked
     :health (if (pos? total)
               (let [completion-ratio (/ (double completed) total)
                     abandon-penalty (/ (double (count (or abandoned []))) (max 1 active))
                     block-penalty (/ (double blocked) total)]
                 (max 0.0 (- completion-ratio (* 0.5 abandon-penalty) (* 0.3 block-penalty))))
               0.0)}))

;; ---------------------------------------------------------------------------
;; Scan 5: Sessions
;;
;; Builds a session index from the evidence store.
;; Each session becomes an "ant" that can be replayed across the hex grid.
;;
;; cf. cyberants: ant has position, mode, cargo, trail
;; cf. FuLab terminal vocabulary: session/turn terminals
;; ---------------------------------------------------------------------------

(def ^:private repo-detect-patterns
  "Patterns for detecting which repo an evidence entry relates to."
  {"futon0"  #"(?i)\bfuton0\b"
   "futon1"  #"(?i)\bfuton1\b(?!a)"
   "futon1a" #"(?i)\bfuton1a\b"
   "futon2"  #"(?i)\bfuton2\b"
   "futon3"  #"(?i)\bfuton3\b(?![abc])"
   "futon3a" #"(?i)\bfuton3a\b"
   "futon3b" #"(?i)\bfuton3b\b"
   "futon3c" #"(?i)\bfuton3c\b"
   "futon4"  #"(?i)\bfuton4\b"
   "futon5"  #"(?i)\bfuton5\b(?!a)"
   "futon5a" #"(?i)\bfuton5a\b"
   "futon6"  #"(?i)\bfuton6\b|prelim|proof|theorem|nlab"
   "futon7"  #"(?i)\bfuton7\b"
   "futon7a" #"(?i)\bfuton7a\b"
   "vsat.wiki" #"(?i)\bvsat\b|prospectus|working.paper"
   "npt"     #"(?i)\bnpt\b|consult|ukrn|bristol"})

(def ^:private known-repos
  (set (keys repo-detect-patterns)))

(defn- detect-repos
  "Detect which repos an evidence entry relates to from its tags and text."
  [entry]
  (let [text (str (get-in entry [:evidence/body :text] "")
                  " " (or (:evidence/type entry) ""))
        tag-repos (->> (or (:evidence/tags entry) [])
                       (keep (fn [tag]
                               (let [t (cond
                                         (keyword? tag) (name tag)
                                         (string? tag) tag
                                         :else nil)]
                                 (when (contains? known-repos t) t)))))
        text-repos (when-not (str/blank? text)
                     (for [[repo pat] repo-detect-patterns
                           :when (re-find pat text)]
                       repo))
        repos (vec (distinct (concat tag-repos text-repos)))]
    (when (seq repos)
      repos)))

(defn- detect-missions
  "Detect which specific missions an evidence entry relates to.
   Looks for mission IDs (e.g. 'war-machine', 'portfolio-inference')
   and M- prefixed references (e.g. 'M-war-machine')."
  [entry mission-ids]
  (let [text (str (get-in entry [:evidence/body :text] ""))]
    (when-not (str/blank? text)
      (let [text-lower (str/lower-case text)]
        (vec (for [mid mission-ids
                   :when (or (str/includes? text-lower (str/lower-case mid))
                             (str/includes? text-lower (str "m-" (str/lower-case mid))))]
               mid))))))

(defn scan-sessions
  "Build session index from the evidence store.

   Each session has:
   - :session-id, :entry-count, :start, :end
   - :steps — chronological entries with detected repo AND mission positions
   - :repos-touched, :missions-touched — distinct repos/missions mentioned

   Returns {:sessions [...] :total-sessions N}."
  [days]
  (let [since (since-str days)
        entries (or (fetch-evidence :since since) [])
        ;; Get mission IDs for mission detection
        mission-ids (when-let [missions (fetch-missions)]
                      (vec (keep :mission/id missions)))
        ;; Group by session-id
        by-session (group-by :evidence/session-id entries)
        sessions (->> by-session
                      (remove (fn [[sid _]] (nil? sid)))
                      (map (fn [[sid ses-entries]]
                             (let [sorted (sort-by :evidence/at ses-entries)
                                   start (:evidence/at (first sorted))
                                   end (:evidence/at (last sorted))
                                   steps (mapv (fn [e]
                                                 {:at (:evidence/at e)
                                                  :type (:evidence/type e)
                                                  :event (get-in e [:evidence/body :event])
                                                  :repos (detect-repos e)
                                                  :missions (if mission-ids
                                                              (detect-missions e mission-ids)
                                                              [])
                                                  :text (subs (str (get-in e [:evidence/body :text] ""))
                                                              0 (min 80 (count (str (get-in e [:evidence/body :text] "")))))})
                                               sorted)
                                   repos-touched (->> steps (mapcat :repos) distinct vec)
                                   missions-touched (->> steps (mapcat :missions) distinct vec)]
                               {:session-id sid
                                :entry-count (count ses-entries)
                                :start start
                                :end end
                                :steps steps
                                :repos-touched repos-touched
                                :missions-touched missions-touched})))
                      (sort-by :start #(compare %2 %1))
                      vec)]
    {:sessions sessions
     :total-sessions (count sessions)}))

;; ---------------------------------------------------------------------------
;; Scan 6b: Mission Detail (for mission hex view)
;;
;; Richer mission data for the zoomed-in mission view.
;; Groups missions by repo, includes dependency edges.
;; ---------------------------------------------------------------------------

(defn scan-mission-detail
  "Fetch detailed mission data for the mission hex view.
   Returns missions grouped by repo with dependency info."
  []
  (when-let [missions (fetch-missions)]
    (let [by-repo (group-by :mission/repo missions)
          ;; Extract blocked-by pairs from the portfolio structure
          step-data (try
                      (let [resp (http/post (str futon3c-url "/api/alpha/portfolio/step")
                                           {:headers {"Content-Type" "application/json"
                                                      "Accept" "application/json"}
                                            :body "{\"emit-evidence\":false}"
                                            :timeout 10000
                                            :throw false})]
                        (when (= 200 (:status resp))
                          (json/parse-string (:body resp) true)))
                      (catch Exception _ nil))
          blocked-pairs (get-in step-data [:structure :blocked-pairs] [])
          dep-edges (mapv (fn [[a b]] {:from a :to b :type :blocked-by}) blocked-pairs)]
      {:missions missions
       :by-repo by-repo
       :dependency-edges dep-edges
       :total (count missions)
       :repos (keys by-repo)})))

;; ---------------------------------------------------------------------------
;; Scan 7: Portfolio Inference
;;
;; Queries the live Portfolio Inference AIF loop.
;; Returns current belief state (μ), precision (τ), mode, urgency,
;; and the policy recommendation (ranked actions with EFE scores).
;;
;; This is the war machine reading the output of an existing AIF loop —
;; the portfolio inference engine is the "brain" and we're reading its
;; state, like reading an ant's mu/mode/hunger.
;; ---------------------------------------------------------------------------

(defn scan-portfolio
  "Query the Portfolio Inference engine for current state and recommendation.

   Calls two endpoints:
   - GET /api/alpha/portfolio/state — current beliefs without stepping
   - POST /api/alpha/portfolio/step — run one AIF step, get recommendation

   Returns {:state {...} :recommendation {...} :available? true/false}."
  ([] (scan-portfolio false))
  ([step?]
   (let [state-data (http-get-json (str futon3c-url "/api/alpha/portfolio/state"))
         step-data (when step?
                     (try
                       (let [resp (http/post (str futon3c-url "/api/alpha/portfolio/step")
                                            {:headers {"Content-Type" "application/json"
                                                       "Accept" "application/json"}
                                             :body "{\"emit-evidence\":false}"
                                             :timeout 10000
                                             :throw false})]
                         (when (= 200 (:status resp))
                           (json/parse-string (:body resp) true)))
                       (catch Exception _ nil)))]
     (if (or state-data step-data)
       {:available? true
        :state (when state-data
                 (let [s (:state state-data)]
                   {:mode (get-in s [:mu :mode])
                    :urgency (get-in s [:mu :urgency])
                    :tau (:tau s)
                    :free-energy nil ;; only available after step
                    :step-count (:step-count s)
                    :focus (get-in s [:mu :focus])
                    :channels (get-in s [:mu :sens])}))
        :recommendation (when step-data
                          {:action (:action step-data)
                           :abstain? (:abstain step-data)
                           :mode (get-in step-data [:diagnostics :mode])
                           :urgency (get-in step-data [:diagnostics :urgency])
                           :tau (get-in step-data [:diagnostics :tau])
                           :free-energy (get-in step-data [:diagnostics :free-energy])
                           :recommendation-text (:recommendation step-data)
                           :structure (:structure step-data)})}
       {:available? false}))))

;; ---------------------------------------------------------------------------
;; Scan 8: Graph (strategic state graph)
;;
;; Builds the unified graph from all sources. This is the core data
;; structure that the visualiser renders.
;;
;; Node types: repo, sorry, mission, workstream
;; Edge types: temporal-coupling, evidence-flow, sorry-affinity,
;;             workstream-dependency, logic-model-edge
;;
;; cf. war-machine-exotype.edn C-scan-graph
;; cf. cyberants: world grid with cells → here: stack with nodes/edges
;; ---------------------------------------------------------------------------

(defn- repo-nodes
  "Build repo nodes with commit activity."
  [days]
  (let [since (since-str days)]
    (vec
     (for [{:keys [label path workstream]} all-repos
           :let [commits (or (count-commits-since path since) 0)]
           :when (.isDirectory (java.io.File. path))]
       {:type :repo
        :id label
        :label label
        :workstream workstream
        :commits commits
        :active? (pos? commits)}))))

(defn- sorry-nodes
  "Build sorry nodes from the topology."
  []
  (when-let [alignment (read-edn-file (str futon5a-root "/data/alignment.edn"))]
    (vec
     (for [s (:sorry-topology alignment)]
       {:type :sorry
        :id (:id s)
        :label (name (:id s))
        :severity (:severity s)
        :status (:status s)
        :layer (:layer s)
        :closes-by (:closes-by s)}))))

(defn- workstream-nodes
  "Build workstream nodes from the logic model."
  []
  (when-let [model (read-edn-file (str futon5a-root "/data/stack-logic-model.edn"))]
    (vec
     (for [ws (:workstreams model)]
       {:type :workstream
        :id (:id ws)
        :label (:label ws)
        :jsdq-mode (:jsdq-mode ws)
        :target-hours (get-in ws [:pocketwatch-hours :target])
        :constraint (:constraint ws)}))))

(defn- mission-nodes
  "Build mission nodes from the API. Limits to active/blocked/ready
   missions to keep the graph legible."
  []
  (when-let [missions (fetch-missions)]
    (vec
     (for [m missions
           :let [status (or (:mission/status m) "unknown")]
           :when (#{"active" "in-progress" "in_progress" "blocked" "ready" "testing"} status)]
       {:type :mission
        :id (or (:mission/id m) (:id m))
        :label (or (:mission/id m) (:id m) "?")
        :status status
        :repo (or (:mission/repo m) "unknown")}))))

(defn- temporal-coupling-edges
  "Detect co-change patterns: repos changed on the same days.
   Returns edges with coupling strength [0,1]."
  [days]
  (let [since (since-str days)
        ;; Collect per-repo per-day commit dates
        repo-days (into {}
                        (for [{:keys [label path]} all-repos
                              :when (.isDirectory (java.io.File. path))
                              :let [out (git path "log" "--since" since "--format=%aI")
                                    dates (when (and out (not (str/blank? out)))
                                            (->> (str/split-lines out)
                                                 (keep parse-iso-date)
                                                 distinct
                                                 set))]]
                          [label (or dates #{})]))
        ;; Compute Jaccard similarity between repo day-sets
        repos (keys repo-days)]
    (->> (for [a repos, b repos
               :when (pos? (compare (str b) (str a)))
               :let [da (get repo-days a)
                     db (get repo-days b)
                     inter (count (clojure.set/intersection da db))
                     union (count (clojure.set/union da db))
                     jaccard (if (pos? union) (/ (double inter) union) 0.0)]
               :when (> jaccard 0.1)]  ;; threshold: at least 10% co-change
           {:type :temporal-coupling
            :from a :to b
            :strength jaccard
            :co-change-days inter})
         (sort-by :strength >)
         vec)))

(defn- workstream-dependency-edges
  "Logic model edges between workstreams."
  []
  (when-let [model (read-edn-file (str futon5a-root "/data/stack-logic-model.edn"))]
    (vec
     (for [edge (:edges model)
           :when (:from edge)]
       {:type :logic-model-edge
        :id (:id edge)
        :from (:from edge)
        :to (:to edge)
        :edge-type (:type edge)
        :status (:status edge)
        :reinforcing? (:reinforcing edge)
        :constraint (:constraint edge)}))))

(defn- evidence-flow-edges
  "Count evidence entries per workstream (approximated by topic)."
  [entries]
  (let [ws-patterns {:mathematics #"(?i)prelim|proof|theorem|nlab|math"
                     :portfolio   #"(?i)portfolio|prospectus|working.paper|placemat|vsat"
                     :consulting  #"(?i)consult|ukrn|bristol|invoice|paid|eric"
                     :stack       #"(?i)repl|evidence|mission|futon|agency|pattern"}]
    (->> (for [[ws pat] ws-patterns]
           (let [cnt (count (filter (fn [e]
                                      (let [text (str (get-in e [:evidence/body :text] ""))]
                                        (and (not (str/blank? text))
                                             (re-find pat text))))
                                    entries))]
             {:type :evidence-flow
              :from ws
              :to :evidence-store
              :count cnt}))
         (filter #(pos? (:count %)))
         vec)))

(defn scan-graph
  "Build the strategic state graph.

   Nodes: repos (with commit activity), sorrys (with severity/status),
          workstreams (with JSDQ mode and pocketwatch targets).
   Edges: temporal coupling (Jaccard co-change), workstream dependencies
          (logic model), evidence flow (topic counts).
   Dynamics: pocketwatch ticks, sorry signals.

   This is the core data structure consumed by the visualiser.
   cf. cyberants world grid — here the 'world' is the stack itself."
  [days]
  (let [since (since-str days)
        entries (or (fetch-evidence :limit 2000) [])
        window-entries (filter (fn [e]
                                 (when-let [d (parse-iso-date (:evidence/at e))]
                                   (>= (compare d since) 0)))
                               entries)
        ;; Nodes
        repos (repo-nodes days)
        sorrys (or (sorry-nodes) [])
        workstreams (or (workstream-nodes) [])
        missions (or (mission-nodes) [])
        ;; Edges
        coupling (temporal-coupling-edges days)
        dependencies (or (workstream-dependency-edges) [])
        evidence-flows (evidence-flow-edges window-entries)
        ;; Dynamics: pocketwatch ticks
        model (read-edn-file (str futon5a-root "/data/stack-logic-model.edn"))
        ticks (when model (get-in model [:pocketwatch :ticks]))
        ;; Compute actual workstream commit ratios for tick evaluation
        ws-commits (reduce (fn [acc {:keys [workstream commits]}]
                             (update acc workstream (fnil + 0) commits))
                           {}
                           repos)
        total-commits (max 1 (reduce + (vals ws-commits)))
        stack-pct (/ (double (:stack ws-commits 0)) total-commits)
        consulting-pct (/ (double (:consulting ws-commits 0)) total-commits)
        portfolio-pct (/ (double (:portfolio ws-commits 0)) total-commits)
        math-pct (/ (double (:mathematics ws-commits 0)) total-commits)
        pct-str (fn [x] (format "%.0f%%" (* 100.0 (double (or x 0.0)))))
        ;; Evaluate ticks
        tick-results (vec
                      (for [t (or ticks [])]
                        (let [{:keys [fired? observed proxy-note computed?]}
                              (case (:id t)
                                :hermit-warning
                                {:fired? (and (> stack-pct 0.7) (< consulting-pct 0.05))
                                 :observed (str "stack=" (pct-str stack-pct)
                                                ", consulting=" (pct-str consulting-pct)
                                                " of commits in the last " days "d")
                                 :proxy-note "Directly computed from commit shares in the current 14d/90d window."
                                 :computed? true}

                                :hobby-warning
                                {:fired? (and (pos? (:portfolio ws-commits 0))
                                              (< portfolio-pct 0.05))
                                 :observed (str "portfolio commits=" (:portfolio ws-commits 0)
                                                ", share=" (pct-str portfolio-pct))
                                 :proxy-note "Approximation: the model wants 'portfolio work but no JSDQ maturity advance in 14 days'; current code proxies that as 'portfolio commits exist but still occupy <5% of recent commits'."
                                 :computed? false}

                                :foraging-warning
                                {:fired? (> stack-pct 0.7)
                                 :observed (str "stack=" (pct-str stack-pct)
                                                ", mathematics=" (pct-str math-pct)
                                                ", portfolio+consulting="
                                                (pct-str (+ portfolio-pct consulting-pct)))
                                 :proxy-note "Approximation: the model wants 'math evidence turns > portfolio+consulting turns AND cargo > 0.5'; current code proxies that as 'stack share >70%' because cargo/evidence-turn accounting is not wired here yet."
                                 :computed? false}

                                :cargo-warning
                                (let [wp? (.exists (java.io.File.
                                                    (str home "/vsat.wiki/ukrn-demo/UKRN_WP_draft_v2.md")))
                                      prospectus? (.exists (java.io.File.
                                                            (str home "/vsat.wiki/prospectus.md")))]
                                  {:fired? (and wp? (not prospectus?))
                                   :observed (str "working-paper=" (if wp? "present" "absent")
                                                  ", prospectus=" (if prospectus? "present" "absent"))
                                   :proxy-note "Direct file-existence check. The warning clears once the prospectus exists."
                                   :computed? true})

                                {:fired? false
                                 :observed "no evaluator"
                                 :proxy-note "No runtime evaluator implemented."
                                 :computed? false})]
                          (assoc t
                                 :fired? fired?
                                 :observed observed
                                 :proxy-note proxy-note
                                 :computed? computed?))))]
    {:nodes {:repos repos
             :sorrys sorrys
             :workstreams workstreams
             :missions missions}
     :edges {:temporal-coupling coupling
             :dependencies dependencies
             :evidence-flows evidence-flows}
     :dynamics {:ticks tick-results
                :commit-ratios ws-commits
                :commit-percentages {:stack stack-pct
                                     :consulting consulting-pct
                                     :portfolio portfolio-pct
                                     :mathematics math-pct}}
     :summary {:total-repos (count repos)
               :active-repos (count (filter :active? repos))
               :total-sorrys (count sorrys)
               :total-workstreams (count workstreams)
               :coupling-edges (count coupling)
               :ticks-firing (count (filter :fired? tick-results))}}))

;; ---------------------------------------------------------------------------
;; Scan 9: Pattern Library
;;
;; Loads the pattern library index (TSV) and groups by collection.
;; This gives the war machine visibility into the pattern landscape:
;; which collections exist, how many patterns each has, and which
;; domains they cover.
;;
;; cf. futon3/resources/sigils/patterns-index.tsv
;; cf. futon3a/notions — pattern search (MiniLM embeddings)
;; ---------------------------------------------------------------------------

(def ^:private patterns-index-path
  (str home "/code/futon3/resources/sigils/patterns-index.tsv"))

(def ^:private embeddings-path
  (str home "/code/futon3a/resources/notions/minilm_pattern_embeddings.json"))

(defn- load-embeddings
  "Load MiniLM pattern embeddings and compute 2D collection centroids.
   Uses the two dimensions with highest variance across collection centroids
   (cheap PCA approximation). Returns a map of collection-id → [x y]."
  []
  (try
    (when (.exists (java.io.File. embeddings-path))
      (let [raw (json/parse-string (slurp embeddings-path) true)
            ;; Group embeddings by collection
            by-coll (group-by (fn [e]
                                (let [id (:id e "")]
                                  (if (str/includes? id "/")
                                    (subs id 0 (.indexOf id "/"))
                                    "_uncategorized")))
                              raw)
            ;; Compute centroid per collection (mean of 384-dim vectors)
            centroids (into {}
                            (for [[coll entries] by-coll
                                  :let [vecs (mapv :vector entries)
                                        n (count vecs)
                                        dim (count (first vecs))]
                                  :when (and (pos? n) (pos? dim))]
                              [coll (mapv (fn [d]
                                            (/ (reduce + (map #(nth % d 0.0) vecs))
                                               (double n)))
                                          (range dim))]))
            all-vecs (vec (vals centroids))
            n-colls (count all-vecs)]
        (when (>= n-colls 2)
          (let [dim (count (first all-vecs))
                ;; Find 2 dimensions with highest variance
                dim-vars (mapv (fn [d]
                                 (let [vals (mapv #(nth % d 0.0) all-vecs)
                                       mean (/ (reduce + vals) (double n-colls))
                                       var (/ (reduce + (map #(let [v (- % mean)] (* v v)) vals))
                                              (double n-colls))]
                                   {:dim d :var var}))
                               (range dim))
                top-2 (->> dim-vars (sort-by :var >) (take 2) (mapv :dim))
                d1 (first top-2)
                d2 (second top-2)
                ;; Project centroids onto these 2 dimensions
                coords (into {}
                             (for [[coll vec] centroids]
                               [coll [(nth vec d1 0.0) (nth vec d2 0.0)]]))]
            coords))))
    (catch Exception _ nil)))

(defn scan-patterns
  "Load the pattern library index and group by collection.
   Optionally loads MiniLM embeddings for 2D spatial coordinates.

   Returns {:collections [{:id :name :count :patterns [...] :x :y}]
            :total-patterns N
            :total-collections N}."
  []
  (try
    (when (.exists (java.io.File. patterns-index-path))
      (let [lines (str/split-lines (slurp patterns-index-path))
            entries (->> lines
                         (remove #(str/starts-with? % "#"))
                         (remove str/blank?)
                         (mapv (fn [line]
                                 (let [fields (str/split line #"\t" -1)
                                       pattern-id (first fields)
                                       slash-idx (.indexOf (str pattern-id) "/")
                                       collection (if (pos? slash-idx)
                                                    (subs pattern-id 0 slash-idx)
                                                    "uncategorized")
                                       pattern-name (if (pos? slash-idx)
                                                      (subs pattern-id (inc slash-idx))
                                                      pattern-id)]
                                   {:pattern-id pattern-id
                                    :collection collection
                                    :name pattern-name
                                    :tokipona (get fields 1 "")
                                    :sigil (get fields 2 "")
                                    :rationale (get fields 3 "")
                                    :hotwords (get fields 4 "")}))))
            by-collection (group-by :collection entries)
            ;; Load embedding coordinates
            embed-coords (or (load-embeddings) {})
            collections (->> by-collection
                             (mapv (fn [[coll-id patterns]]
                                     (let [[x y] (get embed-coords coll-id [0.0 0.0])]
                                       {:id coll-id
                                        :name coll-id
                                        :count (count patterns)
                                        :x x :y y
                                        :has-embedding? (contains? embed-coords coll-id)
                                        :patterns (mapv #(select-keys % [:pattern-id :name :sigil :rationale])
                                                        patterns)})))
                             (sort-by :count >)
                             vec)]
        {:collections collections
         :total-patterns (count entries)
         :total-collections (count collections)
         :embedding-coverage (count (filter :has-embedding? collections))}))
    (catch Exception _ nil)))

;; ---------------------------------------------------------------------------
;; Scan 10: Daily Scan Frames
;;
;; Reads structured frames from futon5a/data/frames/.
;; These are the outputs of the daily scan pipeline (M-daily-scan).
;; The war machine consumes frames to assess depositing activity,
;; pipeline status, and cardinal direction trends.
;;
;; cf. futon5a/data/frame-schema.edn
;; cf. futon7 bb daily-scan
;; ---------------------------------------------------------------------------

(def ^:private frames-dir
  (str futon5a-root "/data/frames"))

(defn scan-frames
  "Load all frames from futon5a/data/frames/ and extract strategic signals.

   Returns {:frames [...] :daily-scan-count N :latest-frame {...}
            :depositing-signal float :cardinal-trend {...}}."
  []
  (try
    (let [dir (java.io.File. frames-dir)]
      (when (.exists dir)
        (let [frame-files (->> (.listFiles dir)
                               (filter #(str/ends-with? (.getName %) ".edn"))
                               (sort-by #(.getName %))
                               vec)
              frames (mapv #(try (read-string (slurp %))
                                (catch Exception _ nil))
                           frame-files)
              frames (filterv some? frames)
              ;; Find daily scan frames specifically
              daily-frames (filterv #(= :daily-scan (:frame/type %)) frames)
              latest (last frames)
              ;; Extract depositing signal from cardinal directions
              depositing-signal (if-let [cd (:frame/cardinal-direction latest)]
                                  (get cd :depositing 0.0)
                                  0.0)
              ;; Compute trend: average cardinal direction across daily frames
              cardinal-trend (when (seq daily-frames)
                               (reduce (fn [acc frame]
                                         (let [cd (or (:frame/cardinal-direction frame) {})]
                                           (-> acc
                                               (update :hermit + (get cd :hermit 0.0))
                                               (update :foraging + (get cd :foraging 0.0))
                                               (update :cargo + (get cd :cargo 0.0))
                                               (update :depositing + (get cd :depositing 0.0)))))
                                       {:hermit 0.0 :foraging 0.0 :cargo 0.0 :depositing 0.0}
                                       daily-frames))
              cardinal-avg (when cardinal-trend
                             (let [n (max 1 (count daily-frames))]
                               {:hermit (/ (:hermit cardinal-trend) n)
                                :foraging (/ (:foraging cardinal-trend) n)
                                :cargo (/ (:cargo cardinal-trend) n)
                                :depositing (/ (:depositing cardinal-trend) n)}))]
          {:frames-count (count frames)
           :daily-scan-count (count daily-frames)
           :latest-frame (select-keys latest [:frame/id :frame/timestamp
                                              :frame/mode :frame/cardinal-direction
                                              :frame/constraints])
           :depositing-signal depositing-signal
           :cardinal-trend cardinal-avg
           ;; Pipeline status from latest frame
           :pipeline-status (get-in latest [:frame/constraints :income-deadline :status])
           ;; Daily scan streak
           :scan-streak (get-in latest [:frame/constraints :daily-scan-streak :completed] 0)})))
    (catch Exception _ nil)))

;; ---------------------------------------------------------------------------
;; Scan 11: Metabolic Balance
;;
;; Reads the mana snapshot for two M-bounded-in-flight-state signals:
;; per-repo working-tree drain (the metabolic-balance/working-tree
;; channel as a continuous reading) and per-session AIF-head data
;; (sessions ARE peripherals per the M-aif-head reread).
;;
;; Source: futon0/scripts/mana-snapshot.bb output at
;; ~/code/storage/futon0/mana-snapshot.json.
;; ---------------------------------------------------------------------------

(def ^:private mana-snapshot-path
  (str home "/code/storage/futon0/mana-snapshot.json"))

(defn- repo-label-from-path [abs-path]
  (when (string? abs-path)
    (last (str/split abs-path #"/"))))

(defn- pressure->tier
  "Map a pressure value to a tier keyword. RECONCILED 2026-05-27 per
   E-wm-staleness-meta-stop §3 to match the producer
   (futon0/scripts/mana-snapshot.bb lines 81-86) and the JVM canonical
   (futon3c.logic.metabolic-balance/pressure->tier line 70). Before this
   fix the consumer's thresholds (silent<1, adv≥1, high≥3, stop≥10) silently
   diverged from those two (silent<1, adv<2, high<4, stop≥4) — the
   docstring claimed they matched but lied. Behavior change is nominal at
   the call site because the consumer inherits the producer's
   pre-computed :tier label (see scan-metabolic-balance ~line 1545); this
   function is the fallback when :max-tier is absent from the snapshot
   AND it determines the tier any time the consumer computes tier from
   pressure directly. Canonical rationale: producer wins because it's
   the de-facto rendered behavior and the JVM check-fn already agrees."
  [p]
  (cond
    (>= p 4.0) :stop-the-line
    (>= p 2.0) :high
    (>= p 1.0) :advisory
    :else      :silent))

(defn- tier-rank [tier]
  (case (some-> tier name)
    "stop-the-line" 3
    "high"          2
    "advisory"      1
    "silent"        0
    -1))

(defn- tier-max
  "Return the more severe of two tiers."
  [a b]
  (if (>= (tier-rank a) (tier-rank b)) a b))

(defn- iso->ms [s]
  (try
    (when (string? s)
      (.toEpochMilli (java.time.Instant/parse s)))
    (catch Exception _ nil)))

(def ^:private active-sessions-N 3)
(def ^:private active-sessions-D-days 7.0)

(defn- compute-active-sessions-channel
  "Compute the active-sessions drain channel from session data.

   Per drain-channel-shape, every drain channel shares the harmonic
   structure: P = max(count/N, age/D, ...). For active-sessions:
     - count = total registered sessions
     - N = 3 (more than 3 sessions = mental-effort drain begins)
     - age = days since the oldest session was last active
     - D = 7 days
   Bytes channel does not apply here.

   Joe's intuition (2026-05-04): mental effort to remember which session
   is which scales with count above ~3. Once tickle-1 is removed from
   automatic startup, the floor drops by 1."
  [sessions]
  (let [count* (count sessions)
        now-ms (System/currentTimeMillis)
        ages-days (->> sessions
                       (keep (fn [{:keys [last-active]}]
                               (when-let [ms (iso->ms last-active)]
                                 (/ (- now-ms ms) 86400000.0)))))
        max-age (if (seq ages-days) (apply max ages-days) 0.0)
        p-count (/ (double count*) active-sessions-N)
        p-age   (/ (double max-age) active-sessions-D-days)
        p (max p-count p-age)]
    {:channel :active-sessions
     :pressure p
     :count count*
     :max-age-days max-age
     :p-count p-count
     :p-age p-age
     :tier (pressure->tier p)
     :nominal {:N active-sessions-N :D-days active-sessions-D-days}}))

(defn scan-metabolic-balance
  "Read the mana snapshot for drain-channel state and per-session AIF-head data.

   Returns nil if the snapshot is missing.
   Otherwise returns a map with:
     :available?
     :channels — drain channels (working-tree, active-sessions),
                  each with :pressure, :tier, contributing detail
     :max-tier / :max-pressure — across channels (replaces the snapshot's
       working-tree-only values)
     :per-repo — working-tree per-repo detail (sorted by pressure desc)
     :sessions — per-session AIF-head detail
     :pool, :generated-at, :snapshot-age-minutes, :stale?"
  []
  (let [f (java.io.File. mana-snapshot-path)]
    (when (.exists f)
      (try
        (let [snap (json/parse-string (slurp f) true)
              age-min (/ (- (System/currentTimeMillis) (.lastModified f))
                         60000.0)
              per-repo (->> (or (:per-repo snap) [])
                            (map (fn [r]
                                   {:repo (or (:repo r)
                                              (repo-label-from-path (:abs-path r)))
                                    :pressure (or (:P r) 0.0)
                                    :count (or (:count r) 0)
                                    :max-age-days (or (:max-age-days r) 0.0)
                                    :bytes (or (:total-bytes r) 0)
                                    :tier (:tier r)}))
                            (sort-by :pressure #(compare %2 %1))
                            vec)
              sessions (->> (or (:sessions snap) [])
                            (mapv (fn [s]
                                    {:session-id (:session-id s)
                                     :agent-id (:agent-id s)
                                     :phase (get s (keyword "aif-head/phase"))
                                     :status (:status s)
                                     :balance (or (:balance s) 0)
                                     :earned (or (:earned s) 0)
                                     :spent (or (:spent s) 0.0)
                                     :last-active (:last-active s)})))
              ;; Working-tree channel summary (the snapshot's existing data)
              working-tree-channel
              {:channel :working-tree
               :pressure (or (:max-pressure snap) 0.0)
               :tier (or (some-> (:max-tier snap) keyword)
                         (pressure->tier (or (:max-pressure snap) 0.0)))
               :repo-count (count (filter #(pos? (or (:pressure %) 0)) per-repo))
               :nominal (:nominals snap)}
              ;; New channel: active-sessions
              active-sessions-channel (compute-active-sessions-channel sessions)
              channels [working-tree-channel active-sessions-channel]
              max-tier (reduce tier-max :silent (map :tier channels))
              max-pressure (apply max 0.0 (map :pressure channels))]
          {:available? true
           :channels channels
           :max-tier max-tier
           :max-pressure max-pressure
           :per-repo per-repo
           :sessions sessions
           :pool (:pool snap)
           :generated-at (:generated-at snap)
           :snapshot-age-minutes age-min
           :stale? (> age-min 60.0)})
        (catch Exception _ {:available? false :error :parse-failed})))))

(defn- summarize-working-tree-hygiene
  "Project the metabolic-balance snapshot into an operator-facing hygiene
   surface for the War Machine dashboard.

   This is intentionally honest about what exists today: per-repo pressure
   is available, but coherent file-cluster synthesis is not yet landed.
   The `:commit-hygiene` payload therefore exposes repo-level queues and a
   `:clustering-status` marker instead of pretending we already have safe
   auto-commit bundles."
  [metabolic-balance]
  (if-not (:available? metabolic-balance)
    {:available? false
     :queues []
     :active-count 0
     :high-count 0
     :stop-count 0
     :clustering-status :unavailable}
    (let [repos (or (:per-repo metabolic-balance) [])
          active (->> repos
                      (filter #(pos? (double (or (:pressure %) 0.0))))
                      (mapv (fn [{:keys [repo pressure tier count max-age-days bytes]}]
                              {:repo repo
                               :pressure pressure
                               :tier tier
                               :count count
                               :max-age-days max-age-days
                               :bytes bytes
                               :needs-fixing
                               (format "%s has %d dirty paths, age %.1fd, %.2f pressure"
                                       repo (or count 0) (double (or max-age-days 0.0))
                                       (double (or pressure 0.0)))
                               :action
                               (format "Review %s for commit/disposition clustering"
                                       repo)}))
                      (sort-by (juxt (comp - tier-rank :tier) (comp - :pressure)))
                      vec)
          channels (or (:channels metabolic-balance) [])
          active-sessions (some #(when (= :active-sessions (:channel %)) %) channels)
          working-tree (some #(when (= :working-tree (:channel %)) %) channels)]
      {:available? true
       :clustering-status :not-yet-grouped
       :queues (take 8 active)
       :active-count (count active)
       :high-count (count (filter #(= :high (:tier %)) active))
       :stop-count (count (filter #(= :stop-the-line (:tier %)) active))
       :max-tier (:max-tier metabolic-balance)
       :max-pressure (:max-pressure metabolic-balance)
       :snapshot-age-minutes (:snapshot-age-minutes metabolic-balance)
       :stale? (:stale? metabolic-balance)
       :working-tree-channel working-tree
       :active-sessions-channel active-sessions})))

(defn- tier-glyph [tier]
  (case (some-> tier name)
    "stop-the-line" "●"
    "high"          "◑"
    "advisory"      "◔"
    "silent"        "○"
    "?"))

(defn- bytes-str [n]
  (let [n (long (or n 0))]
    (cond
      (>= n 1048576) (format "%.1fM" (/ n 1048576.0))
      (>= n 1024)    (format "%dK" (long (/ n 1024)))
      :else          (str n))))

;; ---------------------------------------------------------------------------
;; Scan 12: Block Closures
;;
;; A 'Block' (futon-Block, Cook-Ting sense — see
;; futon3/library/structure/block-as-futonic-revolution.flexiarg) is one
;; revolution of the futonic loop. Per M-bounded-in-flight-state, each
;; closed block is committed with a footer of the form:
;;   Block: <kind>-<YYYY-MM-DD>-<slug>
;; where <slug> typically references a mission doc-id (e.g. mbi05031b8c
;; for M-bounded-in-flight-state). This scan walks the 14-repo manifest
;; for recent commits carrying that footer — i.e. *closed* Blocks. In-
;; flight Blocks (identified but not yet committed) require a separate
;; identification mechanism (see M-bounded-in-flight-state Q-21
;; guidance-note follow-up). Distinct from the mission-edge sense of
;; 'block' (which is what Strategic Judgement's critical-path /
;; mission-bottleneck priorities surface).
;; ---------------------------------------------------------------------------

(def ^:private block-footer-pattern
  "Matches: Block: <kind>-<YYYY-MM-DD>-<slug>"
  #"(?m)^\s*Block:\s+([a-z][a-z0-9-]*?)-(\d{4}-\d{2}-\d{2})-([A-Za-z0-9._-]+)\s*$")

(defn- parse-block-footer [body]
  (when (string? body)
    (when-let [m (re-find block-footer-pattern body)]
      {:kind (nth m 1)
       :ymd  (nth m 2)
       :slug (nth m 3)
       :tag  (str (nth m 1) "-" (nth m 2) "-" (nth m 3))})))

(defn- repo-blocks [repo-path label workstream since]
  (let [;; Use ASCII-control delimiters so commit bodies don't collide
        ;; (\x1e = record sep between commits, \x1f = field sep within commit)
        log (git repo-path "log"
                 (str "--since=" since)
                 "--all"
                 "--format=%H%x1f%aI%x1f%B%x1e")]
    (when (string? log)
      (->> (str/split log #"\x1e")
           (keep (fn [chunk]
                   (let [chunk (str/trim chunk)]
                     (when (seq chunk)
                       (let [[hash date & body-parts] (str/split chunk #"\x1f")
                             body (str/join "" body-parts)]
                         (when-let [b (parse-block-footer body)]
                           (let [first-line (or (some-> body
                                                        (str/split #"\n")
                                                        first
                                                        str/trim)
                                                "")]
                             (assoc b
                                    :hash hash
                                    :date (parse-iso-date date)
                                    :first-line first-line
                                    :repo label
                                    :workstream workstream))))))))
           vec))))

(defn scan-blocks
  "Walk the 14-repo manifest's recent commits for Block: footers.

   A Block is one revolution of the futonic loop (see
   block-as-futonic-revolution.flexiarg). The footer ties commits across
   repos that participate in the same revolution.

   Returns {:total N :blocks [...] :by-kind {kind→count}
            :by-slug {slug→count} :by-date {ymd→count}}."
  [days]
  (let [since (since-str days)
        ;; Filter to repos that exist on disk
        repos (filter #(.exists (java.io.File. ^String (:path %))) all-repos)
        all-blocks (->> repos
                        (mapcat (fn [{:keys [label path workstream]}]
                                  (repo-blocks path label workstream since)))
                        (sort-by :date #(compare %2 %1))
                        vec)]
    {:total (count all-blocks)
     :blocks all-blocks
     :by-kind (frequencies (map :kind all-blocks))
     :by-slug (frequencies (map :slug all-blocks))
     :by-date (frequencies (map :date all-blocks))
     :by-repo (frequencies (map :repo all-blocks))}))

;; ---------------------------------------------------------------------------
;; Renderer: markdown fallback
;;
;; The primary renderer is the Swing visualiser (war_machine_visual.clj).
;; This markdown renderer is the fallback for M-x war-machine in Emacs.
;; ---------------------------------------------------------------------------

(defn- pct-str [n] (format "%.0f%%" (* 100.0 (double n))))

(defn- render-table
  "Render a justified markdown table."
  [headers aligns rows]
  (let [ncols (count headers)
        widths (mapv (fn [i]
                       (apply max (count (nth headers i))
                              (map #(count (nth % i "")) rows)))
                     (range ncols))
        pad (fn [s w align]
              (let [s (or s "")
                    gap (- w (count s))]
                (case align
                  :right (str (apply str (repeat gap \space)) s)
                  (str s (apply str (repeat gap \space))))))
        fmt-row (fn [cells]
                  (str "| "
                       (str/join " | "
                                 (map-indexed (fn [i cell]
                                                (pad cell (nth widths i) (nth aligns i)))
                                              cells))
                       " |\n"))
        sep (str "|"
                 (str/join "|"
                           (map-indexed (fn [i a]
                                          (let [w (nth widths i)]
                                            (case a
                                              :right (str (apply str (repeat (inc w) \-)) ":")
                                              (str (apply str (repeat (+ w 2) \-))))))
                                        aligns))
                 "|\n")]
    (str (fmt-row headers) sep (apply str (map fmt-row rows)))))

(defn- health-indicator [h]
  (cond (>= h 0.7) "●"  ;; healthy
        (>= h 0.3) "◐"  ;; partial
        :else       "○")) ;; absent

(defn render-war-machine
  "Render the War Machine strategic synthesis as markdown."
  [{:keys [self-watch loop-health support-attack mission-triage graph
           metabolic-balance commit-hygiene blocks now days] :as data}]
  (let [sb (StringBuilder.)]
    (.append sb "# War Machine — Strategic Synthesis\n\n")
    (.append sb (str "**" now "** | " days "-day window\n\n"))

    ;; --- Self Watch ---
    (when self-watch
      (.append sb "## Self-Watch\n\n")
      (cond
        (not (:available? self-watch))
        (.append sb "*Self-watch evidence unavailable — futon3c evidence API did not answer.*\n\n")

        (seq (:issues self-watch))
        (do
          (.append sb (str "Active warnings: " (:warning-count self-watch)
                           " | critical: " (:critical-count self-watch) "\n\n"))
          (.append sb (render-table
                       ["Severity" "Surface" "Needs Fixing" "Action" "Last seen"]
                       [:left :left :left :left :left]
                       (mapv (fn [{:keys [severity surface summary action at]}]
                               [(name severity)
                                surface
                                summary
                                action
                                (or (parse-iso-date at) at "-")])
                             (:issues self-watch))))
          (.append sb "\n"))

        :else
        (.append sb "*No active self-watch warnings in window.*\n\n"))

      (when (seq (:recoveries self-watch))
        (.append sb "**Recent recoveries:**\n\n")
        (doseq [{:keys [summary at]} (take 5 (:recoveries self-watch))]
          (.append sb (str "- " summary
                           " (" (or (parse-iso-date at) at "-") ")\n")))
        (.append sb "\n")))

    ;; --- Commit Hygiene ---
    (when commit-hygiene
      (.append sb "## Commit Hygiene\n\n")
      (cond
        (not (:available? commit-hygiene))
        (.append sb "*Commit-hygiene snapshot unavailable.*\n\n")

        (seq (:queues commit-hygiene))
        (do
          (.append sb (str "Active repos: " (:active-count commit-hygiene)
                           " | high: " (:high-count commit-hygiene)
                           " | stop-the-line: " (:stop-count commit-hygiene)
                           " | grouping: " (name (:clustering-status commit-hygiene)) "\n\n"))
          (.append sb (render-table
                       ["Repo" "Tier" "Pressure" "Dirty" "Max age" "Action"]
                       [:left :left :right :right :right :left]
                       (mapv (fn [{:keys [repo tier pressure count max-age-days action]}]
                               [repo
                                (name tier)
                                (format "%.2f" (double pressure))
                                (str count)
                                (format "%.1fd" (double (or max-age-days 0.0)))
                                action])
                             (:queues commit-hygiene))))
          (.append sb "\n"))

        :else
        (.append sb "*No working-tree queues above the reporting floor.*\n\n")))

    ;; --- Loop Health ---
    (.append sb "## Loop Health\n\n")
    (when loop-health
      (let [{:keys [arrows overall loop-complete?]} loop-health]
        (.append sb (str "Overall: " (pct-str overall)
                         (if loop-complete? " — **loop complete**" " — gaps detected")
                         "\n\n"))
        (.append sb (render-table
                     ["Arrow" "Health" "Count" "Last seen" ""]
                     [:left :right :right :left :left]
                     (mapv (fn [{:keys [label health count last-seen]}]
                             [label
                              (pct-str health)
                              (str count)
                              (or last-seen "never")
                              (health-indicator health)])
                           arrows)))
        (.append sb "\n")))

    ;; --- Support / Attack (enriched with invariant evidence) ---
    (.append sb "## Holistic Argument\n\n")
    (let [sa (or (get-in data [:judgement :support-attack-enriched]) support-attack)]
      (when sa
        (let [{:keys [claims support-coverage attack-coverage
                       support-coverage-enriched attack-coverage-enriched]} sa]
          ;; Show both raw and enriched coverage
          (.append sb (str "Support coverage: " (pct-str (or support-coverage 0))
                           (when support-coverage-enriched
                             (str " → " (pct-str support-coverage-enriched) " (with invariants)"))
                           " | Attack coverage: " (pct-str (or attack-coverage 0))
                           (when attack-coverage-enriched
                             (str " → " (pct-str attack-coverage-enriched) " (with invariants)"))
                           "\n\n"))
          (.append sb (render-table
                       ["Claim" "Type" "Evidence" "Invariant signal" "Status"]
                       [:left :left :right :left :left]
                       (mapv (fn [{:keys [claim-id type label evidence-count
                                          status invariant-evidence]}]
                               [(str (name claim-id) ": " label)
                                (name type)
                                (str evidence-count)
                                (if invariant-evidence
                                  (:detail invariant-evidence)
                                  "-")
                                (name status)])
                             claims)))
          (.append sb "\n"))))

    ;; --- Mission Triage ---
    (.append sb "## Mission Triage\n\n")
    (when mission-triage
      (let [{:keys [total active completed blocked abandoned-count
                    abandoned-missions by-repo health]} mission-triage]
        (.append sb (render-table
                     ["Metric" "Value"]
                     [:left :right]
                     [["Total missions" (str total)]
                      ["Active" (str active)]
                      ["Completed" (str completed)]
                      ["Blocked" (str blocked)]
                      ["Abandoned (no commits in window)" (str abandoned-count)]
                      ["Health" (pct-str health)]]))
        (.append sb "\n")
        ;; By repo
        (when (seq by-repo)
          (.append sb "**By repo:**\n\n")
          (doseq [[repo cnt] (sort-by val > by-repo)]
            (.append sb (str "- " repo ": " cnt "\n")))
          (.append sb "\n"))
        ;; Abandoned missions
        (when (seq abandoned-missions)
          (.append sb "**Abandoned missions (in-progress, no recent commits):**\n\n")
          (doseq [m (take 10 abandoned-missions)]
            (.append sb (str "- " (:mission/id m) " (" (:mission/repo m) ")\n")))
          (.append sb "\n"))))

    ;; --- Metabolic Balance — Drain Channels + Session AIF Heads ---
    (when (and metabolic-balance (:available? metabolic-balance))
      (.append sb "## Metabolic Balance\n\n")
      (let [{:keys [channels per-repo max-tier max-pressure sessions pool
                    snapshot-age-minutes stale?]} metabolic-balance]
        (.append sb (str "Snapshot: "
                         (if (and snapshot-age-minutes (number? snapshot-age-minutes))
                           (format "%.1f min ago" (double snapshot-age-minutes))
                           "?")
                         (when stale? " ⚠ stale")
                         " | max-tier: " (or (some-> max-tier name) "-")
                         " | P=" (if (and max-pressure (number? max-pressure))
                                   (format "%.2f" (double max-pressure))
                                   "?")
                         "\n\n"))
        ;; Drain channels overview (per drain-channel-shape: every channel
        ;; has the same harmonic structure)
        (when (seq channels)
          (.append sb "### Drain Channels\n\n")
          (.append sb (render-table
                       ["Channel" "P" "Tier" "" "Detail"]
                       [:left :right :left :left :left]
                       (mapv (fn [{:keys [channel pressure tier count
                                          repo-count max-age-days]}]
                               [(name channel)
                                (format "%.2f" (double (or pressure 0)))
                                (or (some-> tier name) "-")
                                (tier-glyph tier)
                                (case channel
                                  :working-tree
                                  (str (or repo-count 0) " repo(s) with drain")
                                  :active-sessions
                                  (str (or count 0) " sessions"
                                       ", oldest "
                                       (format "%.1fd" (double (or max-age-days 0))))
                                  "-")])
                             channels)))
          (.append sb "\n"))
        ;; Per-repo working-tree drain (omit silent rows below the fold)
        (let [active (filterv #(or (pos? (or (:pressure %) 0))
                                   (pos? (or (:count %) 0))) per-repo)]
          (when (seq active)
            (.append sb "### Working-Tree Drain (per repo)\n\n")
            (.append sb (render-table
                         ["Repo" "P" "Files" "Max age (d)" "Bytes" "Tier" ""]
                         [:left :right :right :right :right :left :left]
                         (mapv (fn [{:keys [repo pressure count max-age-days bytes tier]}]
                                 [(or repo "?")
                                  (format "%.2f" (double (or pressure 0)))
                                  (str count)
                                  (format "%.1f" (double (or max-age-days 0)))
                                  (bytes-str bytes)
                                  (or (some-> tier name) "-")
                                  (tier-glyph tier)])
                               active)))
            (.append sb "\n")))
        ;; Sessions as AIF heads
        (when (seq sessions)
          (.append sb "### Session AIF Heads\n\n")
          (.append sb (render-table
                       ["Agent" "Phase" "Status" "Balance" "Earned" "Spent" "Last active"]
                       [:left :left :left :right :right :right :left]
                       (mapv (fn [{:keys [agent-id phase status balance earned spent last-active]}]
                               [(or agent-id "?")
                                (or (some-> phase name) "?")
                                (or (some-> status name) "?")
                                (str balance)
                                (str earned)
                                (format "%.2f" (double (or spent 0)))
                                (or (parse-iso-date last-active) "-")])
                             sessions)))
          (.append sb "\n"))
        ;; Pool
        (when pool
          (.append sb (str "Pool: balance=" (or (:balance pool) 0)
                           " | donated=" (or (:total-donated pool) 0)
                           " | funded=" (or (:total-funded pool) 0)
                           " | proposals=" (or (:active-proposals pool) 0)
                           "\n\n")))))

    ;; --- Block Closures (futonic-Block, Cook-Ting sense) ---
    ;; Every row here is a *closed* Block — one whose work was committed
    ;; with a `Block:` footer. In-flight Blocks (identified but not yet
    ;; committed) aren't visible at this surface; they'd require a
    ;; separate identification step (see follow-up guidance note —
    ;; M-bounded-in-flight-state QA item Q-21).
    (when (and blocks (pos? (:total blocks 0)))
      (.append sb "## Block Closures\n\n")
      (.append sb (str "_One Block = one revolution of the futonic loop "
                       "(see structure/block-as-futonic-revolution). "
                       "Each row is a *closed* Block — one that's been "
                       "committed; in-flight Blocks (identified but not yet "
                       "committed) are not visible at this surface. "
                       "Distinct from the mission-edge sense of \"block\" "
                       "in Strategic Judgement below._\n\n"))
      (let [{:keys [total blocks by-kind by-slug by-repo]} blocks]
        (.append sb (str "Total Blocks (last " days "d): **" total "**\n\n"))
        ;; Summary tables
        (when (seq by-slug)
          (.append sb "**By mission slug:**\n\n")
          (doseq [[slug n] (sort-by val > by-slug)]
            (.append sb (str "- `" slug "`: " n "\n")))
          (.append sb "\n"))
        (when (seq by-kind)
          (.append sb "**By kind:**\n\n")
          (doseq [[kind n] (sort-by val > by-kind)]
            (.append sb (str "- " kind ": " n "\n")))
          (.append sb "\n"))
        (when (seq by-repo)
          (.append sb "**By repo:**\n\n")
          (doseq [[repo n] (sort-by val > by-repo)]
            (.append sb (str "- " repo ": " n "\n")))
          (.append sb "\n"))
        ;; Recent blocks (most recent first, capped). Each block IS a
        ;; commit — we show the short hash so the operator can pull the
        ;; full diff with `git show <hash>`.
        (.append sb "**Recent revolutions** (each row = one commit):\n\n")
        (.append sb (render-table
                     ["Date" "Repo" "Hash" "Kind" "Slug" "First line"]
                     [:left :left :left :left :left :left]
                     (mapv (fn [{:keys [date repo hash kind slug first-line]}]
                             (let [trimmed (str (or first-line ""))
                                   short-line (if (> (count trimmed) 80)
                                                (str (subs trimmed 0 77) "...")
                                                trimmed)
                                   short-hash (if (and (string? hash)
                                                       (>= (count hash) 7))
                                                (subs hash 0 7)
                                                (or hash "-"))]
                               [(or date "?")
                                (or repo "?")
                                short-hash
                                (or kind "?")
                                (or slug "?")
                                short-line]))
                           (take 20 blocks))))
        (.append sb "\n")))

    ;; --- Judgement (priorities, losses, free energy) ---
    (when-let [j (:judgement data)]
      (.append sb "## Strategic Judgement\n\n")
      (.append sb (str "**Mode:** " (name (:mode j))
                       " (prior: " (format "%.0f%%" (* 100.0 (double (:mode-prior j 0))))
                       ")\n\n"))

      ;; Free energy
      (let [{:keys [G-total G-pragmatic G-epistemic]} (:free-energy j)]
        (.append sb (render-table
                     ["Free Energy" "Value"]
                     [:left :right]
                     [["G-total" (format "%.4f" (double G-total))]
                      ["G-pragmatic (0.65)" (format "%.4f" (double G-pragmatic))]
                      ["G-epistemic (0.35)" (format "%.4f" (double G-epistemic))]]))
        (.append sb "\n"))

      ;; Losses (avoided states currently active)
      (when (seq (:losses j))
        (.append sb "### Losses (avoided states active)\n\n")
        (doseq [loss (:losses j)]
          (.append sb (str "- **" (:summary loss) "**\n")))
        (.append sb "\n"))

      ;; Top priorities
      (when (seq (:priorities j))
        (.append sb "### Priorities\n\n")
        (.append sb (str "_Note: `critical-path` and `blocked-pair` here "
                         "refer to mission-edge dependencies. For futonic-"
                         "Block revolutions see the Block Closures section "
                         "above._\n\n"))
        (.append sb (render-table
                     ["#" "Type" "Summary"]
                     [:right :left :left]
                     (mapv (fn [{:keys [rank type summary]}]
                             [(str rank)
                              (name type)
                              (or summary "?")])
                           (take 15 (:priorities j)))))
        (.append sb "\n"))

      ;; AIF Heads
      (.append sb "### AIF Heads\n\n")
      (let [{:keys [heads missing]} (:heads j)
            ;; Group session-aif-head/* into one summary line
            session-heads (filterv #(= "session-aif-head"
                                       (namespace (:head-id %))) heads)
            other-heads (filterv #(not= "session-aif-head"
                                        (namespace (:head-id %))) heads)]
        (doseq [h other-heads]
          (.append sb (str "- **" (name (:head-id h)) "** — "
                           (get-in h [:judgement :summary] "available") "\n")))
        (when (seq session-heads)
          (let [phases (frequencies (keep #(get-in % [:state :phase])
                                          session-heads))
                balance-sum (reduce + 0 (keep #(get-in % [:state :balance])
                                              session-heads))]
            (.append sb (str "- **session-aif-head** — "
                             (count session-heads) " sessions"
                             ", phases " (pr-str phases)
                             ", balance Σ=" balance-sum
                             " (per-session detail in Metabolic Balance)\n"))))
        (when (seq missing)
          (.append sb "\n**Missing heads:**\n\n")
          (doseq [h missing]
            (.append sb (str "- " (name (:head-id h)) " — " (:note h) "\n"))))
        (.append sb "\n"))

      ;; Invariant inventory summary
      (when-let [inv (:invariants j)]
        (.append sb "### Invariant Inventory\n\n")
        (.append sb (str "Operational families: " (:operational-count inv)
                         " | Candidate families: " (:candidate-count inv)
                         " (" (:total-candidate-invariants inv) " candidate invariants)\n\n"))
        ;; Live runner results
        (if (:live-available? inv)
          (let [{:keys [active dormant violating clean-active
                        obligations-total auto-fixable needs-review]} (:live-summary inv)]
            (.append sb "**Live invariant runner** (from `/api/alpha/invariants`):\n\n")
            (.append sb (render-table
                         ["Metric" "Value"]
                         [:left :right]
                         [["Active domains" (str active)]
                          ["Clean" (str clean-active)]
                          ["Violating" (str violating)]
                          ["Dormant" (str dormant)]
                          ["Obligations" (str obligations-total)]
                          ["Auto-fixable" (str auto-fixable)]
                          ["Needs review" (str needs-review)]]))
            (.append sb "\n")
            (when-let [domains (:live-domains inv)]
              (let [violating-domains (filter :has-violations domains)]
                (when (seq violating-domains)
                  (.append sb "**Domains with violations:**\n\n")
                  (doseq [d violating-domains]
                    (.append sb (str "- **" (name (:domain d)) "**: "
                                     (str/join ", " (map (fn [[k v]] (str (name k) "=" v))
                                                         (:violation-categories d)))
                                     "\n")))
                  (.append sb "\n")))))
          (.append sb "*Live invariant runner not available (endpoint `/api/alpha/invariants` not yet loaded)*\n\n"))
        (when (seq (:candidate-families inv))
          (.append sb "**Candidate families (unwired structural laws):**\n\n")
          (doseq [fam (:candidate-families inv)]
            (.append sb (str "- " (name (:id fam)) " (" (name (:layer fam)) "): "
                             (:question fam) "\n")))
          (.append sb "\n"))))

    ;; --- R-Criterion Status (M-war-machine-frontend-upgrade1 §6.20 — surfaces
    ;;     the contract's R1-R12 ✓/✗/N-A from futon-aif-completeness.md
    ;;     Summary table; parser in `scan-r-criteria`).
    (when-let [rc (:r-criteria data)]
      (.append sb "## R-Criterion Status\n\n")
      (cond
        (not (:available? rc))
        (.append sb (str "*R-criteria contract unreadable: "
                         (or (:error rc) "(unspecified)") "*\n\n"))

        (zero? (:total rc))
        (.append sb (str "*R-criteria contract found but Summary table empty; "
                         "parser regex may need updating.* `" (:path rc) "`\n\n"))

        :else
        (do
          (.append sb (str "Source: `" (:path rc) "` § Summary. "
                           "Total criteria: " (:total rc) ".\n\n"))
          (.append sb (render-table
                       ["R-criterion" "Status" "Gap-closing checkpoint / blocker"]
                       [:left :left :left]
                       (mapv (fn [{:keys [id name status blocker]}]
                               [(str id " — " name)
                                status
                                (if (> (count blocker) 90)
                                  (str (subs blocker 0 87) "...")
                                  blocker)])
                             (:rows rc))))
          (.append sb "\n"))))

    ;; --- R12 Apparatus (M-war-machine-frontend-upgrade1 §6.20 — surfaces the
    ;;     intrinsic-values atom state via wm-hyperparameter-update hyperedges
    ;;     in futon1a XTDB; parser in `scan-r12-apparatus`).
    (when-let [r12 (:r12-apparatus data)]
      (.append sb "## R12 Apparatus (intrinsic-values atom)\n\n")
      (cond
        (not (:available? r12))
        (.append sb (str "*R12 apparatus state unavailable: "
                         (or (:error r12) "(unspecified)")
                         ". Source-of-truth: `code/v05/wm-hyperparameter-update` "
                         "hyperedges in futon1a XTDB.*\n\n"))

        (zero? (:class-count r12))
        (.append sb (str "*R12 apparatus initialized but no class records yet "
                         "— wm-outer-loop hasn't fired. Cron entry: "
                         "`30 4 * * *`; see "
                         "`~/code/futon0/data/cron-jobs.edn`.*\n\n"))

        :else
        (do
          (.append sb (str "Per-class Beta(α,β) posteriors from "
                           (:total-records r12) " update record(s) "
                           "(latest per class). Posterior mode = intrinsic-value "
                           "the WM inner loop consults at `:learn-action-class` "
                           "selection time.\n\n"))
          (.append sb (render-table
                       ["Class" "Beta(α, β)" "Intrinsic-value" "Emissions"
                        "Followthrough" "Substrate" "As-of"]
                       [:left :right :right :right :right :left :left]
                       (mapv (fn [[class
                                   {:keys [alpha beta intrinsic-value
                                           n-emissions n-followthrough
                                           n-followthrough-observed
                                           substrate-status as-of]}]]
                               [(str class)
                                (format "(%.1f, %.1f)" (double (or alpha 1.0))
                                                       (double (or beta 1.0)))
                                (format "%.3f" (double (or intrinsic-value 0.5)))
                                (str (or n-emissions 0))
                                (cond
                                  (and n-followthrough-observed
                                       (not= n-followthrough-observed n-followthrough))
                                  (str n-followthrough " (capped from "
                                       n-followthrough-observed ")")
                                  :else
                                  (str (or n-followthrough 0)))
                                (str (or substrate-status "unknown"))
                                (or as-of "-")])
                             (sort-by first (:per-class r12)))))
          (.append sb "\n"))))

    ;; --- Portfolio Recommendation (adjacent-possible + EFE-ranked actions) ---
    (when-let [pr (get-in data [:judgement :portfolio-recommendation])]
      (.append sb "## Portfolio Recommendation\n\n")
      (.append sb (str "**Action:** " (or (:action pr) "none") "\n\n"))
      (when-let [rec (:recommendation pr)]
        (.append sb (str "```\n" rec "```\n\n")))
      (when (seq (:adjacent pr))
        (.append sb "**Adjacent-possible missions** (structurally enabled):\n\n")
        (doseq [m (:adjacent pr)]
          (.append sb (str "- " (if (map? m) (:mission m) m) "\n")))
        (.append sb "\n"))
      (when (seq (:critical-path pr))
        (.append sb "**Critical path** (blocking other missions):\n\n")
        (.append sb (render-table
                     ["Mission" "Blocks"]
                     [:left :right]
                     (mapv (fn [{:keys [mission depth]}]
                             [mission (str depth " mission(s)")])
                           (:critical-path pr))))
        (.append sb "\n")))

    ;; --- Portfolio Inference (raw state for reference) ---
    (.append sb "## Portfolio Inference\n\n")
    (when-let [pf (:portfolio data)]
      (if (:available? pf)
        (do
          (when-let [s (:state pf)]
            (.append sb (render-table
                         ["Metric" "Value"]
                         [:left :left]
                         [["Mode" (str (:mode s))]
                          ["Urgency" (format "%.2f" (double (or (:urgency s) 0)))]
                          ["Temperature (τ)" (format "%.2f" (double (or (:tau s) 0)))]
                          ["Step count" (str (:step-count s))]
                          ["Focus" (str (or (:focus s) "(none)"))]]))
            (.append sb "\n"))
          (when-let [r (:recommendation pf)]
            (.append sb (str "**Recommendation:** " (:action r) "\n"))
            (.append sb (str "Free energy: " (when (:free-energy r)
                                               (format "%.4f" (double (:free-energy r))))
                             " | τ: " (when (:tau r) (format "%.2f" (double (:tau r))))
                             "\n\n"))))
        (.append sb "Portfolio inference not available.\n\n")))

    ;; --- Graph Summary ---
    (.append sb "## Strategic Graph\n\n")
    (when graph
      (let [{:keys [summary dynamics]} graph
            {:keys [total-repos active-repos total-sorrys coupling-edges ticks-firing]} summary
            {:keys [commit-percentages ticks]} dynamics]
        (.append sb (render-table
                     ["Metric" "Value"]
                     [:left :right]
                     [["Active repos" (str active-repos "/" total-repos)]
                      ["Open sorrys" (str total-sorrys)]
                      ["Temporal coupling edges" (str coupling-edges)]
                      ["Ticks firing" (str ticks-firing)]]))
        (.append sb "\n")
        ;; Workstream balance
        (.append sb "**Workstream balance (commit %):**\n\n")
        (doseq [[ws pct] (sort-by val > commit-percentages)]
          (.append sb (str "- " (name ws) ": " (pct-str pct) "\n")))
        (.append sb "\n")
        ;; Firing ticks
        (when (seq (filter :fired? ticks))
          (.append sb "**Ticks firing:**\n\n")
          (doseq [t (filter :fired? ticks)]
            (.append sb (str "- **" (name (:id t)) ":** " (:fires t) "\n")))
          (.append sb "\n"))
        ;; Temporal coupling
        (when-let [coupling (seq (get-in graph [:edges :temporal-coupling]))]
          (.append sb "**Strongest temporal coupling:**\n\n")
          (.append sb (render-table
                       ["Repo A" "Repo B" "Jaccard" "Co-change days"]
                       [:left :left :right :right]
                       (mapv (fn [{:keys [from to strength co-change-days]}]
                               [from to (format "%.2f" strength) (str co-change-days)])
                             (take 10 coupling))))
          (.append sb "\n"))))

    (str sb)))

;; ---------------------------------------------------------------------------
;; Scan: mark2 arxiv pipeline (Arm A of the proxy-metric inventory)
;;
;; Reads two local caches populated by `scripts/prefetch-mark2.bb`:
;;
;;   ~/code/storage/mark2/state.json        — Rob-side authoritative
;;                                              batch lifecycle (rsynced
;;                                              from linode-chicago).
;;   ~/code/storage/mark2/manifests/*.json  — per-batch manifest.json
;;                                              extracted from each
;;                                              results-NNN.tar.gz.
;;
;; The discipline (per Arm A.4 finding 2026-05-17): expose TWO
;; sub-coordinates separately, never conflated:
;;
;;   :input-throughput — what fraction of attempted papers made it
;;                        through preflight into the pipeline
;;                        (state.json ok/papers).
;;   :output-quality   — what fraction of papers that entered the
;;                        pipeline produced clean output (manifest's
;;                        Stage 6 parse rate, Stage 9 production rates).
;;
;; A more-selective-but-equally-capable pipeline drops the first
;; coordinate without touching the second; conflating them would
;; misread that case as a regression.
;;
;; cf. ~/code/futon7/holes/M-interim-director-proxy-metric-inventory.md
;;     §2 Arm A and §2.A bundle-shape findings.
;; ---------------------------------------------------------------------------

(def ^:private mark2-state-path
  (str home "/code/storage/mark2/state.json"))

(def ^:private mark2-manifests-dir
  (str home "/code/storage/mark2/manifests"))

(defn- safe-slurp-json [path]
  (when (.exists (java.io.File. ^String path))
    (try (json/parse-string (slurp path) true)
         (catch Exception _ nil))))

(defn- batch-input-throughput [state-batch]
  (let [papers (or (:papers state-batch) 0)
        ok     (or (:ok state-batch) 0)
        failed (or (:failed state-batch) 0)]
    {:papers papers
     :ok ok
     :failed failed
     :ok-rate (if (pos? papers) (/ (double ok) papers) 0.0)}))

(defn- batch-output-quality [manifest]
  ;; Pull the structured rates straight off the manifest. Each is a
  ;; clean per-stage parse-rate or production-rate (0..1).
  (let [s6   (:stage6_stats manifest)
        s9a  (:stage9a_stats manifest)
        s9b  (:stage9b_stats manifest)
        gate (:health_gate_thresholds manifest)
        s9a-papers (or (:papers_processed s9a) 0)
        s9a-hg     (or (:hypergraphs_produced s9a) 0)
        s9b-thr    (or (:n_threads s9b) 0)
        s9b-emb    (or (:n_embedded s9b) 0)]
    {:stage6-parse-rate         (or (:parse_rate s6) nil)
     :stage6-parse-threshold    (or (:stage6_parse_rate_min gate) nil)
     :stage9a-production-rate   (if (pos? s9a-papers)
                                  (/ (double s9a-hg) s9a-papers) nil)
     :stage9b-embed-rate        (if (pos? s9b-thr)
                                  (/ (double s9b-emb) s9b-thr) nil)
     :ner-coverage              (get-in manifest [:stage5_stats :ner_coverage])
     :readiness-status          (get-in manifest [:readiness :status])
     :health-issues-count       (count (or (:health_issues manifest) []))
     :elapsed-seconds           (:elapsed_seconds manifest)
     :unique-tags               (get-in manifest [:stats :unique_tags])
     :entity-count              (:entity_count manifest)
     :relation-count            (:relation_count manifest)
     :pattern-schema            (:pattern_schema manifest)
     :embed-model               (:embed_model manifest)
     :llm-model                 (:llm_model manifest)}))

(defn- collection-lag-days [returned-at-iso]
  (when returned-at-iso
    (try (let [returned (java.time.LocalDate/parse
                          (subs returned-at-iso 0 10))
               today    (LocalDate/now tz)]
           (.getDays (java.time.Period/between returned today)))
         (catch Exception _ nil))))

(defn scan-mark2
  "Scan the mark2 arxiv pipeline's state for Arm A proxy-metric signals.

   Reads two local caches (refresh with scripts/prefetch-mark2.bb):
     state.json     — Rob-side lifecycle authority
     manifests/*    — per-batch processing receipts

   Returns a map separating input-throughput (preflight gating) from
   output-quality (Stage 6 + Stage 9 production rates). Conflating
   them misreads a more-selective pipeline as a regression.

   Returns {:batches [...] :overall {...} :cache-status ...}.
   If the cache is missing or stale, returns
   {:cache-status :missing :hint \"run scripts/prefetch-mark2.bb\"}."
  []
  (let [state    (safe-slurp-json mark2-state-path)
        manifest-files (when (.exists (java.io.File. ^String mark2-manifests-dir))
                        (->> (.listFiles (java.io.File. ^String mark2-manifests-dir))
                             (filter #(str/ends-with? (.getName ^java.io.File %) ".json"))
                             (map #(.getAbsolutePath ^java.io.File %))))
        manifests-by-batch (into {}
                                 (for [f (or manifest-files [])
                                       :let [raw-id (-> ^String f
                                                        (java.io.File.)
                                                        .getName
                                                        (str/replace #"\.json$" ""))
                                             ;; state.json uses "1","2",...; cache uses "001","002",...
                                             stripped-id (str/replace raw-id #"^0+" "")
                                             stripped-id (if (empty? stripped-id) raw-id stripped-id)
                                             m (safe-slurp-json f)]
                                       :when m
                                       k [raw-id stripped-id]]
                                   [k m]))]
    (cond
      (nil? state)
      {:cache-status :missing
       :hint "Local mark2 state.json cache missing. Run scripts/prefetch-mark2.bb to pull from linode-chicago."
       :state-path mark2-state-path}

      (empty? manifests-by-batch)
      {:cache-status :missing
       :hint "No batch manifests cached. Run scripts/prefetch-mark2.bb to extract from local tarballs."
       :manifests-dir mark2-manifests-dir}

      :else
      (let [batches (->> (:batches state)
                         (map (fn [[k v]] [(name k) v]))
                         (sort-by first)
                         (mapv (fn [[batch-id batch]]
                                 (let [m (get manifests-by-batch batch-id)]
                                   {:batch-id        batch-id
                                    :status          (:status batch)
                                    :created-at      (:created_at batch)
                                    :returned-at     (:returned_at batch)
                                    :collected-at    (:collected_at batch)
                                    :collection-lag-days
                                    (when (and (#{"results-ready"} (:status batch))
                                               (:returned_at batch))
                                      (collection-lag-days (:returned_at batch)))
                                    :input-throughput (batch-input-throughput batch)
                                    :output-quality   (when m (batch-output-quality m))
                                    :has-manifest     (boolean m)}))))
            done    (filter #(= "done" (:status %)) batches)
            ready   (filter #(= "results-ready" (:status %)) batches)
            total-papers (reduce + 0 (map (comp :papers :input-throughput) batches))
            total-ok     (reduce + 0 (map (comp :ok :input-throughput) batches))
            total-failed (reduce + 0 (map (comp :failed :input-throughput) batches))
            ;; Trend: input-throughput rate over the last 4 batches vs first 4
            last-4-itr (->> batches reverse (take 4) (map (comp :ok-rate :input-throughput)))
            first-4-itr (->> batches (take 4) (map (comp :ok-rate :input-throughput)))
            mean (fn [xs] (when (seq xs) (/ (reduce + 0.0 xs) (count xs))))
            ;; Output quality trend: Stage 6 parse-rate across batches with manifests
            s6-rates (keep #(get-in % [:output-quality :stage6-parse-rate]) batches)]
        {:cache-status     :ok
         :as-of            (str (LocalDate/now tz))
         :state-mtime      (-> (java.io.File. ^String mark2-state-path)
                               .lastModified
                               java.time.Instant/ofEpochMilli str)
         :batches          batches
         :overall
         {:batches-total     (count batches)
          :batches-done      (count done)
          :batches-uncollected (count ready)
          :uncollected-batch-ids (mapv :batch-id ready)
          :max-collection-lag-days
          (reduce max 0 (keep :collection-lag-days ready))
          :papers-attempted-total total-papers
          :papers-ok-total        total-ok
          :papers-failed-total    total-failed
          :input-throughput-rate-overall
          (if (pos? total-papers) (/ (double total-ok) total-papers) 0.0)
          :input-throughput-trend
          {:first-4-mean (mean first-4-itr)
           :last-4-mean  (mean last-4-itr)
           :delta        (when (and (mean first-4-itr) (mean last-4-itr))
                           (- (mean last-4-itr) (mean first-4-itr)))}
          :output-quality-stage6-stats
          {:n           (count s6-rates)
           :min         (when (seq s6-rates) (apply min s6-rates))
           :max         (when (seq s6-rates) (apply max s6-rates))
           :mean        (mean s6-rates)
           :threshold   (some #(get-in % [:output-quality :stage6-parse-threshold]) batches)}}}))))

;; ---------------------------------------------------------------------------
;; Evaluate: per-batch verdicts for the mark2 pipeline (Arm A rubric / A.5)
;;
;; Builds on scan-mark2's structured observations. For each batch, emits a
;; verdict — :adds-signal / :saturates / :regresses / :inconclusive — based
;; on three axes computed against the running corpus:
;;
;;   :novelty   — fraction of this batch's headliner patterns NOT seen in
;;                prior batches' union; high novelty = new ground.
;;   :density   — avg_nodes/paper relative to the running mean; high = more
;;                structure extracted per paper.
;;   :quality   — Stage 6 parse-rate + Stage 9 production rate; a drop means
;;                output regressed regardless of throughput.
;;
;; Discipline: this is ground-truth assessment of pipeline behaviour, NOT an
;; AIF model output. The verdicts come from observation, not inference.
;; ---------------------------------------------------------------------------

(defn- batch-patterns [manifest]
  (set (or (:patterns manifest) [])))

(defn- evaluate-novelty [this-patterns prior-union]
  (let [novel (clojure.set/difference this-patterns prior-union)
        denom (max 1 (count this-patterns))]
    {:novel-count (count novel)
     :total-count (count this-patterns)
     :rate        (/ (double (count novel)) denom)
     :novel       (sort novel)}))

(defn- evaluate-density [manifest running-mean]
  (let [avg-nodes (get-in manifest [:stage9a_stats :avg_nodes])]
    (when (and avg-nodes running-mean (pos? running-mean))
      {:avg-nodes      avg-nodes
       :running-mean   running-mean
       :ratio          (/ (double avg-nodes) running-mean)})))

(defn- verdict-from-axes [novelty density quality-drop?]
  ;; Verdict combination — keep simple and inspectable:
  ;;   :regresses     — quality drop, regardless of other axes
  ;;   :adds-signal   — novelty > 20% OR density > 1.10×, no regression
  ;;   :saturates     — novelty ≤ 5% AND density ≈ 1.0, no regression
  ;;   :inconclusive  — everything in between
  (cond
    quality-drop?                              :regresses
    (or (>= (:rate novelty 0.0) 0.20)
        (when-let [r (:ratio density)]
          (>= r 1.10)))                        :adds-signal
    (and (<= (:rate novelty 0.0) 0.05)
         (when-let [r (:ratio density)]
           (and (>= r 0.95) (<= r 1.05))))     :saturates
    :else                                      :inconclusive))

(defn evaluate-mark2-batches
  "Per-batch evaluation of the mark2 arxiv pipeline (Arm A.5 rubric).

   Consumes the same manifest cache as `scan-mark2` and produces verdicts:
     :adds-signal | :saturates | :regresses | :inconclusive

   Returns a map {:batches [{...per-batch-with-verdict...}]
                  :overall {:corpus-pattern-union [...]
                            :n-batches :verdict-counts}}.

   Returns {:cache-status :missing :hint \"...\"} if the manifest cache
   hasn't been prefetched."
  []
  (let [manifest-files (when (.exists (java.io.File. ^String mark2-manifests-dir))
                        (->> (.listFiles (java.io.File. ^String mark2-manifests-dir))
                             (filter #(str/ends-with? (.getName ^java.io.File %) ".json"))
                             (sort-by #(.getName ^java.io.File %))
                             (map #(.getAbsolutePath ^java.io.File %))))]
    (if (empty? manifest-files)
      {:cache-status :missing
       :hint "No batch manifests cached. Run scripts/prefetch-mark2.bb."}
      (let [manifests (for [f manifest-files
                            :let [batch-id (-> ^String f
                                               (java.io.File.)
                                               .getName
                                               (str/replace #"\.json$" ""))
                                  m (safe-slurp-json f)]
                            :when m]
                        {:batch-id batch-id :manifest m})
            ;; Walk batches in cache-order, accumulating prior pattern union
            ;; and running density mean as we go.
            stage6-threshold 0.95  ;; below this = quality regression
            [batches _]
            (reduce (fn [[acc prior-state] {:keys [batch-id manifest]}]
                      (let [patterns         (batch-patterns manifest)
                            prior-union      (:pattern-union prior-state)
                            novelty          (evaluate-novelty patterns prior-union)
                            avg-nodes        (get-in manifest [:stage9a_stats :avg_nodes])
                            running-mean     (:density-mean prior-state)
                            density          (when running-mean
                                               (evaluate-density manifest running-mean))
                            s6-rate          (get-in manifest [:stage6_stats :parse_rate])
                            quality-drop?    (and s6-rate (< s6-rate stage6-threshold))
                            ;; verdict is meaningful only when we have priors
                            verdict          (if (nil? prior-union)
                                               :inconclusive  ; first batch, no comparison
                                               (verdict-from-axes novelty density quality-drop?))
                            n-prior          (or (:n prior-state) 0)
                            new-density-mean (if (and avg-nodes running-mean)
                                               (/ (+ (* n-prior running-mean) avg-nodes)
                                                  (inc n-prior))
                                               (or avg-nodes running-mean))]
                        [(conj acc {:batch-id     batch-id
                                    :patterns     (sort patterns)
                                    :novelty      novelty
                                    :density      density
                                    :stage6-rate  s6-rate
                                    :quality-drop? quality-drop?
                                    :verdict      verdict})
                         {:pattern-union (clojure.set/union (or prior-union #{}) patterns)
                          :density-mean  new-density-mean
                          :n             (inc n-prior)}]))
                    [[] {}]
                    manifests)
            verdicts (frequencies (map :verdict batches))]
        {:cache-status :ok
         :as-of (str (LocalDate/now tz))
         :batches batches
         :overall
         {:n-batches (count batches)
          :verdict-counts verdicts
          :corpus-pattern-union (sort (reduce clojure.set/union #{}
                                              (map #(set (:patterns %)) batches)))
          :corpus-pattern-count (count (reduce clojure.set/union #{}
                                               (map #(set (:patterns %)) batches)))}}))))

;; ---------------------------------------------------------------------------
;; Scan: AIF Head integration
;; ---------------------------------------------------------------------------

(defn scan-aif-heads
  "Query all available AIF heads for their current state.

   Each head is an independent AIF loop running somewhere in the stack.
   The war machine reads their states — it doesn't run them.

   Currently available:
   - Portfolio Inference (futon3c): mode, urgency, tau, 16 channels, recommendation
   - Session AIF Heads (per-session, sourced from the mana snapshot):
     each chat session is an AIF head with phase + balance + status,
     per the M-aif-head reread carried by M-bounded-in-flight-state
   - Mission AIF Head (futon3c): per-mission phase, obligation, law compliance
     (not yet exposed via HTTP — future endpoint)

   Returns {:heads [...] :head-count N :missing [...]}."
  []
  (let [;; Portfolio Inference head
        portfolio-state (http-get-json (str futon3c-url "/api/alpha/portfolio/state"))
        portfolio-head (when (and portfolio-state (:ok portfolio-state))
                         (let [s (:state portfolio-state)]
                           {:head-id :portfolio-inference
                            :source "futon3c /api/alpha/portfolio/state"
                            :available? true
                            :state {:mode (get-in s [:mu :mode])
                                    :urgency (get-in s [:mu :urgency])
                                    :tau (:tau s)
                                    :step-count (:step-count s)
                                    :focus (get-in s [:mu :focus])
                                    :channels (get-in s [:mu :sens])}
                            :judgement (let [mode (get-in s [:mu :mode])
                                            urgency (get-in s [:mu :urgency] 0)]
                                         {:mode mode
                                          :urgency urgency
                                          :summary (str mode
                                                        ", urgency " (format "%.2f" (double urgency))
                                                        ", tau " (format "%.2f" (double (:tau s 1.0))))})}))
        ;; Session AIF Heads — sourced from the mana snapshot.
        ;; Per M-bounded-in-flight-state's M-aif-head reread, every session
        ;; IS a peripheral and each carries an AIF head with phase + balance.
        mb (scan-metabolic-balance)
        session-heads (when (and mb (:available? mb) (seq (:sessions mb)))
                        (mapv (fn [{:keys [session-id agent-id phase status balance
                                           earned spent last-active]}]
                                {:head-id (keyword "session-aif-head" (str session-id))
                                 :source "mana-snapshot.json :sessions"
                                 :available? true
                                 :state {:session-id session-id
                                         :agent-id agent-id
                                         :phase phase
                                         :status status
                                         :balance balance
                                         :earned earned
                                         :spent spent
                                         :last-active last-active}
                                 :judgement {:phase phase
                                             :status status
                                             :balance balance
                                             :summary (str (or agent-id session-id)
                                                           " phase=" (or (some-> phase name) "?")
                                                           " status=" (or (some-> status name) "?")
                                                           " balance=" balance)}})
                              (:sessions mb)))
        ;; Mission AIF Head — bridge candidate (c) from
        ;; sorry/mission-aif-head-not-served: the WM head reads the mission-AIF
        ;; head's LOCAL computation in-process (a typed reading), making it
        ;; "readable by the WM head" — the literal gap named by the sorry.
        ;; (Unblocked by the futon3c.aif.invariant defonce fix, 2026-05-29;
        ;; M-pilot-appearance REPL substantive cycle; cg in sorrys.edn :resolution.)
        mission-head (try
                       (let [sma (requiring-resolve 'futon3c.aif.mission-head/select-mission-action)
                             arena (some-> (requiring-resolve 'futon3c.aif.mission-head/mission-arena) deref)
                             reading (sma {} 1.0)]
                         {:head-id :mission-aif-head
                          :source "futon3c aif/mission_head.clj (in-process, bridge-c)"
                          :available? true
                          :state {:arena-id (:arena/id arena)
                                  :selected-action (:action reading)
                                  :abstain? (:abstain? reading)
                                  :tau (:tau reading)}
                          :judgement {:action (:action reading)
                                      :summary (str "mission-aif-head selected "
                                                    (some-> (:action reading) name)
                                                    " (tau " (:tau reading) ")")}})
                       (catch Throwable t
                         {:head-id :mission-aif-head
                          :source "futon3c aif/mission_head.clj"
                          :available? false
                          :note (str "in-process read failed: " (.getMessage t))}))
        all (vec (concat [portfolio-head] session-heads [mission-head]))
        heads (filterv :available? all)
        missing (filterv (complement :available?) all)]
    {:heads heads
     :head-count (count heads)
     :missing missing
     :missing-count (count missing)}))

;; --- Invariant inventory ---

(def ^:private invariant-model-path
  (str home "/code/futon4/futon-stack-invariant-model.edn"))

(defn load-invariant-inventory
  "Load the structural law inventory from futon-stack-invariant-model.edn.

   Returns the invariant layers and families with their operational/candidate status.
   The inventory is the war machine's structural judgement input — it tells us
   what properties the system claims to maintain and whether they're enforced.

   Also queries GET /api/alpha/invariants (when available) for live violation
   data from the invariant runner.

   cf. structural-law-inventory.sexp (source of truth)
   cf. futon4/futon-stack-invariant-model.edn (machine-readable hypergraph)"
  []
  (when-let [model (read-edn-file invariant-model-path)]
    (let [families (:families model [])
          invariants (:invariants model [])
          operational (filterv #(= :operational (:status %)) families)
          candidate (filterv #(= :candidate (:status %)) families)
          ;; Try to get live invariant runner results.
          ;; First try the dedicated endpoint, then fall back to /eval (Drawbridge).
          live-data (or (http-get-json (str futon3c-url "/api/alpha/invariants"))
                        (try
                          (let [token (try (str/trim (slurp (str home "/code/futon3c/.admintoken")))
                                          (catch Exception _ nil))
                                drawbridge-port (or (System/getenv "FUTON3C_DRAWBRIDGE_PORT") "6768")
                                eval-url (str "http://localhost:" drawbridge-port "/eval")]
                            (when token
                              (let [clj-code (str "(let [domains (vec (keep (fn [[did ns-sym]]"
                                                  " (try (require ns-sym)"
                                                  " (let [bd (resolve (symbol (str ns-sym) \"build-db\"))"
                                                  " qv (resolve (symbol (str ns-sym) \"query-violations\"))]"
                                                  " (when (and bd qv) {:domain-id did :build-db @bd :query-violations @qv}))"
                                                  " (catch Exception _ nil)))"
                                                  " [[:agency 'futon3c.agency.logic]"
                                                  " [:tickle 'futon3c.agents.tickle-logic]"
                                                  " [:proof 'futon3c.peripheral.proof-logic]"
                                                  " [:mission 'futon3c.peripheral.mission-logic]"
                                                  " [:codex 'futon3c.agents.codex-code-logic]]))"
                                                  " agg ((resolve 'futon3c.logic.invariant-runner/run-aggregate) {} domains)]"
                                                  " {:ok true :summary (:summary agg)"
                                                  " :domains (mapv (fn [r] {:domain (:domain-id r) :state (:state r)"
                                                  " :has-violations (:has-violations? r)"
                                                  " :violation-categories (when (:has-violations? r)"
                                                  " (into {} (for [[k v] (:violations r) :when (seq v)] [k (count v)])))})"
                                                  " (:reports agg))})")
                                    resp (http/post eval-url
                                                    {:headers {"Content-Type" "text/plain"
                                                               "x-admin-token" token}
                                                     :body clj-code
                                                     :timeout 10000
                                                     :throw false})]
                                (when (= 200 (:status resp))
                                  ;; eval returns application/edn, not JSON
                                  (let [result (read-string (:body resp))]
                                    (when (:ok result)
                                      (:value result)))))))
                          (catch Exception _ nil)))
          live-summary (when (and live-data (:ok live-data))
                         (:summary live-data))
          live-domains (when (and live-data (:ok live-data))
                         (:domains live-data))
          individual-candidates (vec (mapcat (fn [fam]
                                                (map (fn [inv-kw]
                                                       {:id inv-kw
                                                        :name (name inv-kw)
                                                        :family-id (:id fam)
                                                        :layer (:layer fam)})
                                                     (or (:candidate-invariants fam) [])))
                                              candidate))]
      ;; Extract individual candidate invariants for the visualiser
      {:layers invariants
       :families families
       :operational-families operational
       :candidate-families candidate
       :individual-candidates individual-candidates
       :operational-count (count operational)
       :candidate-count (count candidate)
       :total-candidate-invariants (count individual-candidates)
       ;; Live runner data (nil if endpoint not available)
       :live-available? (boolean live-data)
       :live-summary live-summary
       :live-domains live-domains})))

;; --- Invariant → Support/Attack enrichment ---

(defn enrich-support-attack
  "Overlay invariant and AIF head health onto the support/attack claims.

   The structural claims (S1-S5, A1-A4) from the holistic argument are
   normally matched by keyword in evidence text. But invariant health and
   AIF head coverage are *direct* evidence for several claims:

   S1 (Evidence discipline works):
     - 9 operational invariant families = the system checks itself
     - Evidence for S1 = operational-count / total-families

   S5 (Reflexive architecture is rare):
     - The system can describe itself via the invariant model
     - Evidence for S5 = (operational + candidate with model) / total

   A1 (Complexity cost):
     - Candidate families in active domains = unwired structural debt
     - Evidence for A1 = candidate-count in active repos

   A2 (Solo-developer bottleneck):
     - Missing AIF heads = coordination gaps requiring manual bridging
     - Evidence for A2 = missing-head-count

   Returns enriched support-attack data with :invariant-evidence per claim."
  [support-attack inventory aif-heads]
  (let [op-count (:operational-count inventory 0)
        cand-count (:candidate-count inventory 0)
        total (+ op-count cand-count)
        missing-heads (:missing-count aif-heads 0)
        head-count (:head-count aif-heads 0)
        ;; Compute invariant-derived evidence per claim
        inv-evidence
        {:S1 {:source :invariant-model
              :signal (if (pos? total) (/ (double op-count) total) 0.0)
              :detail (str op-count "/" total " families operational")}
         :S5 {:source :invariant-model
              :signal (if (pos? total)
                        (/ (double total) 18.0) ;; 18 = approximate max families
                        0.0)
              :detail (str total " families modelled (op:" op-count " cand:" cand-count ")")}
         :A1 {:source :invariant-model
              :signal (if (pos? total) (/ (double cand-count) total) 0.0)
              :detail (str cand-count " candidate families unwired")}
         :A2 {:source :aif-heads
              :signal (if (pos? (+ head-count missing-heads))
                        (/ (double missing-heads) (+ head-count missing-heads))
                        0.0)
              :detail (str missing-heads " AIF heads missing, " head-count " available")}}
        ;; Enrich each claim
        enriched-claims (mapv (fn [claim]
                                (let [cid (:claim-id claim)
                                      inv (get inv-evidence cid)]
                                  (if inv
                                    (assoc claim :invariant-evidence inv)
                                    claim)))
                              (:claims support-attack))
        ;; Recompute coverage including invariant evidence
        support-claims (filter #(= :support (:type %)) enriched-claims)
        attack-claims (filter #(= :attack (:type %)) enriched-claims)
        has-evidence? (fn [c]
                        (or (pos? (:evidence-count c 0))
                            (when-let [inv (:invariant-evidence c)]
                              (> (:signal inv) 0.3))))]
    (assoc support-attack
           :claims enriched-claims
           :support-coverage-enriched
           (if (seq support-claims)
             (/ (double (count (filter has-evidence? support-claims)))
                (count support-claims))
             0.0)
           :attack-coverage-enriched
           (if (seq attack-claims)
             (/ (double (count (filter has-evidence? attack-claims)))
                (count attack-claims))
             0.0))))

;; --- Priority computation (the judge) ---

(defn- channel-priorities
  "Rank observation channels by gap size. Largest gaps = highest priority."
  [free-energy]
  (->> (:per-channel free-energy)
       (sort-by (comp :gap val) >)
       (filterv (fn [[_ v]] (pos? (:gap v))))
       (map-indexed (fn [i [ch v]]
                      {:rank (inc i)
                       :type :channel-gap
                       :id ch
                       :gap (:gap v)
                       :value (:value v)
                       :preferred (:preferred v)
                       :summary (str (name ch) " at " (format "%.2f" (double (:value v)))
                                     ", preferred " (pr-str (:preferred v)))}))))

(defn- invariant-priorities
  "Identify candidate invariant families relevant to current activity.
   Candidate families in active domains = structural debt."
  [inventory mission-triage]
  (let [active-repos (set (for [[repo cnt] (or (:by-repo mission-triage) {})
                                :when (pos? cnt)]
                            (str "R-" repo)))
        candidate-fams (:candidate-families inventory [])]
    (->> candidate-fams
         (filter (fn [fam]
                   ;; Family is relevant if any of its repos are active
                   (seq (clojure.set/intersection
                         (set (map str (:repos fam [])))
                         active-repos))))
         (mapv (fn [fam]
                 {:type :unwired-invariant-family
                  :id (:id fam)
                  :layer (:layer fam)
                  :question (:question fam)
                  :candidate-count (count (or (:candidate-invariants fam) []))
                  :active-repos (vec (clojure.set/intersection
                                      (set (map str (:repos fam [])))
                                      active-repos))
                  :summary (str (name (:id fam)) " (" (name (:layer fam))
                                "): " (:question fam))})))))

(defn- head-priorities
  "Identify missing AIF heads and head-level signals."
  [aif-heads]
  (let [missing (:missing aif-heads [])
        head-signals (for [h (:heads aif-heads [])
                           :let [urgency (get-in h [:state :urgency] 0)]
                           :when (> urgency 0.6)]
                       {:type :head-signal
                        :id (:head-id h)
                        :urgency urgency
                        :summary (get-in h [:judgement :summary] (str (:head-id h)))})]
    (concat
     ;; Missing heads are structural gaps
     (map (fn [h]
            {:type :missing-head
             :id (:head-id h)
             :note (:note h)
             :summary (str "No AIF head: " (name (:head-id h))
                           " — " (:note h))})
          missing)
     head-signals)))

(defn- mission-priorities
  "Identify mission-level concerns: stalls, abandoned work, blocked chains."
  [mission-triage portfolio-step]
  (let [abandoned (:abandoned-missions mission-triage [])
        ;; Critical path from portfolio step
        critical-path (get-in portfolio-step [:structure :critical-path] [])
        blocked-pairs (get-in portfolio-step [:structure :blocked-pairs] [])]
    (concat
     ;; Abandoned missions
     (when (> (count abandoned) 5)
       [{:type :mission-debt
         :id :abandoned-missions
         :count (count abandoned)
         :summary (str (count abandoned) " abandoned missions (in-progress, no recent commits)")}])
     ;; Critical path missions (mission-bottleneck — distinct from
     ;; futonic-Block, which is one revolution of the futonic loop)
     (map (fn [{:keys [mission depth]}]
            {:type :critical-path
             :id (keyword mission)
             :depth depth
             :summary (str mission " is a critical predecessor of "
                           depth " other mission(s)")})
          critical-path)
     ;; Blocked pairs — phrased as 'waits on' so the word 'block' stays
     ;; reserved for the futonic-Block sense in this report.
     (map (fn [[a b]]
            {:type :blocked-pair
             :id (keyword (str a "→" b))
             :blocker a
             :blocked b
             :summary (str b " waits on " a)})
          blocked-pairs))))

(defn judge
  "The war machine's inference step.

   Composes observations, AIF head states, and invariant inventory
   into a ranked priority list with free energy decomposition. As of
   v0.5 of `futon2/docs/futon-aif-completeness.md`, also produces an
   R5/R6 action recommendation: belief state, EFE-ranked candidate
   actions, and a softmax-with-abstain decision. As of v0.7, optionally
   persists a per-call trace record (R8) when an opts map carrying
   `:trace-path` is passed — the production WM entrypoint passes the
   default trace path; tests / one-off invocations omit it.

   Arity-1 ([scan-data]): no trace persistence.
   Arity-2 ([scan-data opts]): writes trace if `:trace?` is truthy in
     opts; uses `:trace-dir` if provided or default."
  ([scan-data] (judge scan-data {}))
  ([scan-data {:keys [trace? trace-dir scan-id] :or {trace? false}}]
  (let [observation (obs/observe scan-data)
        free-energy (fe/compute-free-energy observation)
        ;; Base mode from equilibrium-classification of observations.
        base-mode (fe/infer-mode observation)
        ;; OVERRIDE-MODE check: when any metabolic-balance channel hits
        ;; tier :stop-the-line, set mode to :stop-the-line regardless
        ;; of base-mode.  See :μ/override-modes in
        ;; war-machine-strategic-vocabulary.edn.  Andon-cord semantics.
        ;;
        ;; INV: staleness gate (added 2026-05-27 per E-wm-staleness-meta-stop §1).
        ;; When the metabolic snapshot is stale (file mtime > 60min per
        ;; futon2.report.war-machine/scan-metabolic-balance's :stale? flag),
        ;; do NOT honor :max-tier as :stop-the-line — fall through to
        ;; base-mode and record the suppression in :override-suppressed-reason
        ;; so the UI can render a "stale" indicator instead of red banner.
        ;; The :stale? flag is computed (age-min > 60.0) but was previously
        ;; logged-but-not-propagated (CLAUDE.md §9 silent-swallow anti-pattern).
        metabolic-max-tier (get-in scan-data [:metabolic-balance :max-tier])
        metabolic-stale?   (get-in scan-data [:metabolic-balance :stale?])
        mode (cond
               (and metabolic-stale?
                    (= :stop-the-line metabolic-max-tier))
               base-mode
               (= :stop-the-line metabolic-max-tier)
               :stop-the-line
               :else
               base-mode)
        override-suppressed-reason (when (and metabolic-stale?
                                              (= :stop-the-line metabolic-max-tier))
                                     :stale-data)
        ;; AIF action selection (v0.5+):
        ;; uniform-prior belief over empty entity set today; R8 trace
        ;; persistence will carry belief across calls.
        wm-sorrys (try (sorry-registry/open-sorrys) (catch Exception _ []))
        ;; v0.9 symmetric bootstrap: belief domain = stack-annotations.edn
        ;; :sections[] :id ∪ sorry-registry ids. Mirrors VSATARCS-side
        ;; bootstrap so per-entity comparison reduces to alist-lookup
        ;; equality on shared string entity-ids.
        wm-belief-pre (belief/bootstrap-from-stack-annotations (map :id wm-sorrys))
        ;; E-support-coverage Cycle 4 (cg-a5d2e756, 2026-05-26): static
        ;; entity-tags from stack-annotations.edn :ref classification.
        ;; WM pilot cycle 2 (cg-7fa6aec3, 2026-05-30): static entity-repos
        ;; from stack-annotations.edn provenance/source path, bridging the
        ;; entity-level belief domain to repo-level temporal-coupling edges.
        ;; WM pilot cycle 4 (cg-77efb863, 2026-05-30): first-class tick
        ;; entities from stack-annotations.edn bridge logic-model tick
        ;; results to entity-level belief.
        wm-entity-tags (belief/classify-entity-tags-from-stack-annotations)
        wm-entity-repos (belief/classify-entity-repos-from-stack-annotations)
        wm-entity-ticks (belief/classify-entity-ticks-from-stack-annotations)
        wm-missions (try (mission-registry/open-missions) (catch Exception _ []))
        ;; v0.10/v0.11/v0.13/R3a/R3b/R3d wiring: compute prediction-errors
        ;; for every channel with a likelihood model. All errors record into
        ;; the trace's :prediction-errors map.
        ;;
        ;; v0.13 R3 multi-step inner iteration: run up to `r3-max-steps`
        ;; micro-steps per call. Each step: compute prediction-errors on
        ;; current belief → update precision-state → synthesize event from
        ;; :annotation-health (weight-annealed per step) → apply belief
        ;; update. Terminates early if all errors drop below
        ;; `r3-error-eps`. Ports the inner-loop pattern from
        ;; ants/aif/perceive.clj.
        r3-max-steps 3
        r3-error-eps 1.0e-3
        prev-precision-state
        (or (some-> (try (trace/latest-trace-record
                          :dir (or trace-dir
                                   (str (System/getProperty "user.home")
                                        "/code/futon2/data/wm-trace")))
                         (catch Exception _ nil))
                    :precision-state)
            (precision/initial-precision-state))
        ;; Inner loop result
        {:keys [belief precision-state prediction-errors micro-step-trace]}
        (loop [step 0
               belief wm-belief-pre
               prec-state prev-precision-state
               micro-trace []]
          (let [predictions (belief/predict-observation
                             belief
                             wm-entity-tags
                             {:entity-repos wm-entity-repos
                              :coupling-edges (get-in scan-data [:graph :edges :temporal-coupling])
                              :entity-ticks wm-entity-ticks
                              :tick-results (get-in scan-data [:graph :dynamics :ticks])})
                raw-errors (into {}
                                 (for [ch belief/channels-with-likelihood]
                                   [ch (fe/compute-prediction-error
                                        (get observation ch 0.0)
                                        (get predictions ch))]))
                prec-state' (precision/update-precision-state prec-state raw-errors)
                weighted-errors (into {}
                                      (for [[ch err-map] raw-errors]
                                        [ch (precision/weighted-error
                                             prec-state' ch err-map)]))
                ;; v0.16 multi-channel R3d sign-aggregation: aggregate
                ;; signed weighted-errors across all R3a-covered channels
                ;; using per-channel :health-sign so sign-direction is
                ;; coherent (high :sorry-count-norm = unhealthier vs high
                ;; :annotation-health = healthier).
                aggregated-signed-error
                (reduce + (for [[ch err-map] weighted-errors
                                :let [sign (double (get pref/channel-health-signs ch 0))
                                      we (double (:weighted-error err-map 0.0))]]
                            (* sign we)))
                aggregated-magnitude (Math/abs aggregated-signed-error)
                ann-error (get weighted-errors :annotation-health)
                error-mag (Math/abs (double (:error ann-error 0.0)))
                ;; Anneal event weight by step: step 0 = full; step K-1 = small
                anneal-factor (max 0.0 (- 1.0 (/ (double step) r3-max-steps)))
                base-weight (min 1.0 aggregated-magnitude)
                event-weight (* base-weight anneal-factor 0.1)
                ;; R3d v0.17 (sorry/r3d-per-entity-attribution): per-entity
                ;; attribution by CONTRIBUTION, not uniform. The aggregate signal
                ;; is distributed unequally — each entity's update is weighted by
                ;; how INCONSISTENT its current belief is with the error direction
                ;; (the entities the signal is actually "about" move most;
                ;; entities already consistent with the observation barely move).
                ;; Weights are normalised so the MEAN equals event-weight, so the
                ;; aggregate magnitude is preserved while attribution is honest.
                ;; This fixes the stated dishonesty ("the aggregate signal can't
                ;; legitimately be attributed equally to every entity") using the
                ;; per-entity expected-health that predict-annotation-health
                ;; already computes — no new event streams required.
                events (when (pos? event-weight)
                         (let [event-type (if (pos? aggregated-signed-error)
                                            :strengthened :foreclosed)
                               incons (into {}
                                            (for [[eid p] belief]
                                              (let [h (belief/entity-expected-health p)]
                                                [eid (if (pos? aggregated-signed-error)
                                                       (- 1.0 h)   ; healthier-than-predicted: surprise lives in low-health entities
                                                       h)])))       ; unhealthier: surprise lives in high-health entities
                               total (reduce + 0.0 (vals incons))
                               n (count belief)
                               norm (if (pos? total) (/ (* event-weight n) total) 0.0)]
                           (->> (keys belief)
                                (mapv (fn [eid]
                                        {:entity-id eid :type event-type
                                         :weight (* (double (get incons eid 0.0)) norm)})))))
                belief' (if (seq events)
                          (belief/update-belief-batch belief events)
                          belief)
                step-entry {:step step
                            :error-magnitude error-mag
                            :aggregated-signed-error aggregated-signed-error
                            :anneal-factor anneal-factor
                            :events-applied (count events)
                            :event-weight event-weight}
                micro-trace' (conj micro-trace step-entry)]
            (if (or (>= (inc step) r3-max-steps)
                    (< error-mag r3-error-eps))
              {:belief belief'
               :precision-state prec-state'
               :prediction-errors weighted-errors
               :micro-step-trace micro-trace'}
              (recur (inc step) belief' prec-state' micro-trace'))))
        wm-belief belief
        ;; v0.13 anticipation v0.13 (read-only): expose upcoming typed
        ;; events to the trace. R5 time-conditioning and R4 multi-horizon
        ;; composition are deferred (v0.14 / v0.15 candidates).
        anticipation-snapshot (anticipation/anticipation-snapshot)
        wm-patterns (try (pattern-registry/open-patterns) (catch Exception _ []))
        wm-state {:observation observation :belief wm-belief :sorrys wm-sorrys
                  :missions wm-missions
                  :patterns wm-patterns
                  :anticipation anticipation-snapshot
                  ;; M-aif2 slice-1 live install (consent-gated, Joe 2026-06-01):
                  ;; inject the delivered E1 curvature signal. Fail-safe —
                  ;; absent/malformed ⇒ [] ⇒ tension-proposer silent ⇒ WM unchanged.
                  :curvature-signal (tension/read-curvature-signal)}
        wm-candidates (ap/compose-proposers
                       [ap/bootstrap-proposer
                        pattern-registry/pattern-enumerator-proposer
                        mission-registry/mission-enumerator-proposer
                        sorry-registry/sorry-enumerator-proposer
                        ;; M-aif2 slice-1: credited + admissibility-gated
                        ;; tension-proposer — emits existing S2 classes via κ at
                        ;; high-curvature actionable substrate-2 nodes (E1 consume).
                        (tension/tension-proposer)]
                       wm-state)
        ;; v0.14 anticipation-driven time-pressure: scale G-risk + G-survival
        ;; by proximity to closest anticipated event in horizon. When no
        ;; events are within horizon, time-pressure = 0 (no scaling).
        wm-time-pressure (anticipation/time-pressure anticipation-snapshot
                                                     (java.time.Instant/now))
        ;; v0.15: multi-horizon scoring activates when anticipation
        ;; loaded events in horizon. Falls back to single-step if no
        ;; anticipation data.
        wm-horizon-steps (when (and (:events-loaded? anticipation-snapshot)
                                    (seq (:events anticipation-snapshot)))
                           3)
        wm-enriched-candidates (->> wm-candidates
                                    enrich-candidates-with-structural-pressure
                                    ;; M-interest-network-coupling capstone:
                                    ;; bias candidates by the lived interest posterior
                                    interest-net/enrich-candidates)
        wm-as-of (str (java.time.Instant/now))
        _wm-snapshot-stash (reset! !last-wm-inputs
                                   {:wm-state wm-state
                                    :candidates wm-enriched-candidates
                                    :wm-missions wm-missions
                                    :as-of wm-as-of
                                    :scan-id (or (:scan-id scan-data)
                                                 scan-id
                                                 "war-machine/judge")})
        wm-ranked (->> (efe/rank-actions wm-state
                                          wm-enriched-candidates
                                          (live-star-map-efe-opts
                                           (live-gap-view-efe-opts
                                            {:time-pressure wm-time-pressure
                                             :horizon-steps wm-horizon-steps})))
                       apply-anamnesis-tiebreak
                       (filter-live-open-mission-ranked-actions wm-missions))
        ;; v0.13 R6 enhancement: pre-filter by can-execute? admissibility
        ;; (composes with can-propose? at proposer-side); then run
        ;; deliberative select-action with default-mode-select as a
        ;; try/catch fallback for I6 compositional closure.
        wm-admissible (filterv #(fm/can-execute? wm-state (:action %)) wm-ranked)
        wm-decision (try (policy/select-action wm-admissible)
                         (catch Exception _
                           (policy/default-mode-select wm-state wm-admissible)))
        aif-heads (scan-aif-heads)
        inventory (load-invariant-inventory)
        ;; Get portfolio step data for structural info
        portfolio-step (try
                         (let [resp (http/post (str futon3c-url "/api/alpha/portfolio/step")
                                              {:headers {"Content-Type" "application/json"
                                                         "Accept" "application/json"}
                                                :body "{\"emit-evidence\":false}"
                                                :timeout 10000
                                                :throw false})]
                           (when (= 200 (:status resp))
                             (json/parse-string (:body resp) true)))
                         (catch Exception _ nil))
        ;; Enrich support/attack with invariant + head evidence
        enriched-sa (enrich-support-attack
                     (:support-attack scan-data) inventory aif-heads)
        ;; Compute priorities from all sources
        ch-pris (channel-priorities free-energy)
        inv-pris (invariant-priorities inventory (:mission-triage scan-data))
        hd-pris (head-priorities aif-heads)
        ms-pris (mission-priorities (:mission-triage scan-data) portfolio-step)
        ;; Merge and rank all priorities
        all-priorities (->> (concat ch-pris inv-pris hd-pris ms-pris)
                            (sort-by (fn [p]
                                       ;; Sort: missing heads first (structural),
                                       ;; then channel gaps (by gap size),
                                       ;; then invariant families, then missions
                                       (case (:type p)
                                         :missing-head        0
                                         :channel-gap         (- 1.0 (min 1.0 (:gap p 0)))
                                         :unwired-invariant-family 2
                                         :head-signal         3
                                         :critical-path       4
                                         :blocked-pair        5
                                         :mission-debt        6
                                         10)))
                            (map-indexed (fn [i p] (assoc p :rank (inc i))))
                            vec)
        ;; Losses: avoided states that are currently active
        losses (vec (concat
                     (when (= mode (:strategic-mode pref/avoided-states))
                       [{:type :avoided-mode
                         :mode mode
                         :summary (str "System in avoided mode: " (name mode))}])
                     (map (fn [ch]
                            {:type :avoided-channel
                             :channel ch
                             :value (get observation ch 0.0)
                             :range (get pref/avoided-states ch)
                             :summary (str (name ch) " at "
                                           (format "%.2f" (double (get observation ch 0.0)))
                                           " — in avoided range "
                                           (pr-str (get pref/avoided-states ch)))})
                          (:avoided-active free-energy))))
        result {:mode mode
                :mode-prior (get pref/mode-prior mode 0.0)
                ;; INV (2026-05-27 staleness-gate): when the metabolic
                ;; snapshot was stale, the :stop-the-line override is
                ;; suppressed and :mode falls to base-mode. Record the
                ;; reason so downstream UI can render "stale" indicator
                ;; instead of red banner. Per CLAUDE.md §9 (no silent
                ;; swallow): if we ignore the stale snapshot's override,
                ;; we MUST surface why.
                :override-suppressed-reason override-suppressed-reason
                :metabolic-stale? (boolean metabolic-stale?)
                :free-energy free-energy
                :priorities all-priorities
                :priority-count (count all-priorities)
                :losses losses
                :loss-count (count losses)
                :heads aif-heads
                :invariants inventory
                :support-attack-enriched enriched-sa
                :portfolio-recommendation
                (when portfolio-step
                  {:action (:action portfolio-step)
                   :recommendation (:recommendation portfolio-step)
                   :adjacent (get-in portfolio-step [:structure :adjacent] [])
                   :critical-path (get-in portfolio-step [:structure :critical-path] [])})
                :observation observation
                :belief wm-belief
                :belief-pre wm-belief-pre
                :prediction-errors prediction-errors
                :precision-state precision-state
                :micro-step-trace micro-step-trace
                :anticipation anticipation-snapshot
                :ranked-actions wm-ranked
                :decision wm-decision
                ;; M-wm-policies v1: the visible cascade-policy lane (additive, defensive —
                ;; a cascade failure can never break the scan; memoized; shell-out to minilm).
                :cascade-policies (try
                                    ((requiring-resolve 'futon2.report.cascade-lane/cascade-lane)
                                     wm-ranked {:n 3 :budget 6})
                                    (catch Throwable _ []))}]
    (when trace?
      (try
        (if trace-dir
          (trace/write-trace! result :dir trace-dir)
          (trace/write-trace! result))
        (catch Exception _ nil)))
    result)))

;; ---------------------------------------------------------------------------
;; Orchestrator
;; ---------------------------------------------------------------------------

(defn- scan-window
  "Return explicit wall-clock bounds for a DAYS-wide scan window.

   The start matches the date boundary used by `since-str`, so the viewer can
   render the same window the server actually queried."
  [days now-zdt]
  {:days days
   :start (.format (.atStartOfDay (.minusDays (.toLocalDate now-zdt) days) tz)
                   DateTimeFormatter/ISO_OFFSET_DATE_TIME)
   :end (.format now-zdt DateTimeFormatter/ISO_OFFSET_DATE_TIME)})

(defn scan-annotation-graph
  "Read `stack-annotations.edn` and summarise its health for the WM's
   `:annotation-health` observation channel (v0.10).

   Returns `{:health <[0,1]> :anomaly-count <int> :section-count <int>}`.
   Health is `1 − min(1, anomalies/sections)` — fewer anomalies relative to
   sections = healthier. Resilient: returns `{:health 0.0 ...}` if the
   canonical source is unreadable."
  []
  (try
    (let [path (str home "/code/futon5a/holes/stack-annotations.edn")
          doc (clojure.edn/read-string (slurp path))
          sections (count (:sections doc))
          anomalies (count (:lift-anomalies doc))
          health (if (pos? sections)
                   (max 0.0 (- 1.0 (min 1.0 (/ (double anomalies) sections))))
                   0.0)]
      {:health health :anomaly-count anomalies :section-count sections})
    (catch Exception _
      {:health 0.0 :anomaly-count 0 :section-count 0})))

(defn- mode-vocabulary-record
  [mode rationales]
  (cond-> {:id mode
           :name (name mode)}
    (contains? rationales mode)
    (assoc :rationale (get rationales mode))))

(defn- vocab-block-between
  [text start-marker end-marker]
  (when-let [start (str/index-of text start-marker)]
    (let [after-start (+ start (count start-marker))
          end (or (str/index-of text end-marker after-start)
                  (count text))]
      (subs text after-start end))))

(defn- strip-edn-comments
  [text]
  (->> (str/split-lines text)
       (map #(first (str/split % #";;" 2)))
       (str/join "\n")))

(defn- read-vocab-form-between
  [text start-marker end-marker]
  (some-> (vocab-block-between text start-marker end-marker)
          strip-edn-comments
          str/trim
          clojure.edn/read-string))

(defn scan-strategic-vocabulary
  "Project the canonical WM strategic vocabulary into the JSON payload.

   The HUD consumes `:mu :modes` so its Mode tooltip rationale follows the
   vocabulary file instead of a duplicated CLJS-side map. The file is not
   valid strict EDN as a whole, so read only the named forms needed here."
  []
  (try
    (let [text (slurp strategic-vocabulary-path)
          modes (vec (read-vocab-form-between text
                                               ":μ/modes"
                                               ":μ/mode-rationales"))
          rationales (or (read-vocab-form-between text
                                                   ":μ/mode-rationales"
                                                   ":μ/override-modes")
                         {})]
      {:source-file strategic-vocabulary-path
       :mu {:mode-ids modes
            :modes (mapv #(mode-vocabulary-record % rationales)
                         modes)}})
    (catch Exception e
      {:source-file strategic-vocabulary-path
       :error (.getMessage e)
       :mu {:mode-ids []
            :modes []}})))

;; ---------------------------------------------------------------------------
;; Scan 11: R-criterion contract status
;;
;; Parses the Summary table of `~/code/futon2/docs/futon-aif-completeness.md`
;; so the markdown rendering surfaces the contract's current R1-R12 ✓/✗/N-A
;; state. Mirrors claude-4's VSATARCS-side Q1 reader parser
;; (`arxana-vsatarcs-r-criteria-wm.el`) — same source-of-truth, different
;; consumer surface (this one writes into the markdown render; the elisp one
;; renders into the VSATARCS browser chrome).
;;
;; Operator-surfaced gap 2026-05-24: the WM markdown rendering didn't carry
;; R-criterion status at all, despite the apparatus tracking R1-R12 via the
;; contract file. M-war-machine-frontend-upgrade1 §6.20 captures the fix.
;; ---------------------------------------------------------------------------

(def ^:private contract-path
  (str home "/code/futon2/docs/futon-aif-completeness.md"))

(def ^:private r-row-re
  ;; Matches lines like `| R1 — Explicit belief state | **✓ as of v0.2** | — ... |`
  #"^\|\s*(R\d+)\s+—\s+([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|")

(defn scan-r-criteria
  "Parse the contract's Summary table; return per-R status rows. v1."
  []
  (try
    (when (.exists (java.io.File. contract-path))
      (let [lines (str/split-lines (slurp contract-path))
            rows (->> lines
                      (keep (fn [line]
                              (when-let [m (re-find r-row-re line)]
                                {:id (nth m 1)
                                 :name (str/trim (nth m 2))
                                 :status (str/trim (nth m 3))
                                 :blocker (str/trim (nth m 4))})))
                      vec)]
        {:available? (boolean (seq rows))
         :rows rows
         :total (count rows)
         :path contract-path}))
    (catch Exception e
      {:available? false :error (.getMessage e)})))

;; ---------------------------------------------------------------------------
;; Scan 12: R12 apparatus state (intrinsic-values atom rehydrated from XTDB)
;;
;; Reads `code/v05/wm-hyperparameter-update` hyperedges from futon1a; groups
;; by class; surfaces the latest Beta(α,β) posterior + intrinsic-value per
;; WM action-class. This is the live state of the R12 narrow take-up
;; apparatus shipped 2026-05-21 (M-war-machine-frontend-upgrade1 §6.20).
;; Resilient: returns {:available? false ...} when XTDB is unreachable.
;; ---------------------------------------------------------------------------

(def ^:private wm-hp-update-type "code/v05/wm-hyperparameter-update")
(def ^:private futon1a-base
  (or (System/getenv "FUTON1A_BASE_URL") "http://localhost:7071/api/alpha"))

(defn scan-r12-apparatus
  "Query XTDB for wm-hyperparameter-update hyperedges; return per-class
   latest Beta posterior + intrinsic-value + emission/follow-through counts.
   v1."
  []
  (try
    (let [url (str futon1a-base "/hyperedges?type=" wm-hp-update-type "&limit=200")
          resp (http/get url {:headers {"Accept" "application/json"}
                              :timeout 5000 :throw false})]
      (if (= 200 (:status resp))
        (let [body (json/parse-string (:body resp) true)
              hxs (or (:hyperedges body) [])
              ;; Group by class; pick latest by :as-of per class
              by-class (group-by #(get-in % [:hx/props :class]) hxs)
              per-class (into {}
                              (for [[c class-hxs] by-class
                                    :when c
                                    :let [latest (last (sort-by #(get-in % [:hx/props :as-of])
                                                                class-hxs))
                                          p (:hx/props latest)]]
                                [c {:alpha (:alpha-post p)
                                    :beta (:beta-post p)
                                    :intrinsic-value (:intrinsic-value-post p)
                                    :n-emissions (:n-emissions-in-window p)
                                    :n-followthrough (:n-followthrough-in-window p)
                                    :n-followthrough-observed (:n-followthrough-observed p)
                                    :substrate-status (:substrate-status p)
                                    :window-days (:window-days p)
                                    :as-of (:as-of p)
                                    :outer-loop-run-id (:outer-loop-run-id p)}]))]
          {:available? true
           :per-class per-class
           :total-records (count hxs)
           :class-count (count per-class)})
        {:available? false :error (str "HTTP " (:status resp))}))
    (catch Exception e
      {:available? false :error (.getMessage e)})))

;; ---------------------------------------------------------------------------
;; Scan 13: VSATARCS projection status
;;
;; Reads the futon4 invariant-state projection in compact WM-status mode. This
;; scan is read-only and failure-is-data: the WM UI should show VSATARCS as
;; unavailable rather than block the rest of the strategic snapshot.
;; ---------------------------------------------------------------------------

(defn scan-vsatarcs-status
  []
  (try
    (if-not (.exists (java.io.File. vsatarcs-status-script))
      {:available? false
       :build {:status :error}
       :error (str "Missing projection script: " vsatarcs-status-script)}
      (let [{:keys [exit out err]} (shell/sh "bb" vsatarcs-status-script "--wm-status")]
        (if (zero? exit)
          (assoc (clojure.edn/read-string out) :available? true)
          {:available? false
           :build {:status :error}
           :error (or (not-empty err)
                      (str "VSATARCS status command exited " exit))})))
    (catch Exception e
      {:available? false
       :build {:status :error}
       :error (.getMessage e)})))

(defn generate-war-machine
  "Collect all strategic scans, run judgement layer, and render.
   Returns {:data ... :judgement ... :markdown ...}."
  [days]
  (let [now-zdt (ZonedDateTime/now tz)
        now (.toString (.toLocalDate now-zdt))
        self-watch (scan-self-watch days)
        loop-health (scan-loop-health days)
        support-attack (scan-support-attack days)
        mission-triage (scan-mission-triage days)
        graph (scan-graph days)
        sessions (scan-sessions days)
        portfolio (scan-portfolio)
        mission-detail (scan-mission-detail)
        patterns (scan-patterns)
        frames (scan-frames)
        metabolic-balance (scan-metabolic-balance)
        commit-hygiene (summarize-working-tree-hygiene metabolic-balance)
        blocks (scan-blocks days)
        window (scan-window days now-zdt)
        annotation-graph (scan-annotation-graph)
        strategic-vocabulary (scan-strategic-vocabulary)
        r-criteria (scan-r-criteria)
        r12-apparatus (scan-r12-apparatus)
        vsatarcs-status (scan-vsatarcs-status)
        capability-star-map (capability-star-map)
        scan-data {:self-watch self-watch
                   :loop-health loop-health
                   :support-attack support-attack
                   :mission-triage mission-triage
                   :graph graph
                   :sessions sessions
                   :portfolio portfolio
                   :mission-detail mission-detail
                   :patterns patterns
                   :frames frames
                   :metabolic-balance metabolic-balance
                   :commit-hygiene commit-hygiene
                   :blocks blocks
                   :window window
                   :annotation-graph annotation-graph
                   :strategic-vocabulary strategic-vocabulary
                   :r-criteria r-criteria
                   :r12-apparatus r12-apparatus
                   :capability-star-map capability-star-map
                   :vsatarcs-status vsatarcs-status}
        ;; Run the judgement layer
        judgement (judge scan-data)]
    {:data scan-data
     :judgement judgement
     :markdown (render-war-machine {:self-watch self-watch
                                    :loop-health loop-health
                                    :support-attack support-attack
                                    :mission-triage mission-triage
                                    :graph graph
                                    :portfolio portfolio
                                    :metabolic-balance metabolic-balance
                                    :commit-hygiene commit-hygiene
                                    :blocks blocks
                                    :r-criteria r-criteria
                                    :r12-apparatus r12-apparatus
                                    :vsatarcs-status vsatarcs-status
                                    :judgement judgement
                                    :now now :days days})}))

;; ---------------------------------------------------------------------------
;; Main
;; ---------------------------------------------------------------------------

(defn -main [& args]
  (let [days (if (seq args) (Integer/parseInt (first args)) 14)
        {:keys [markdown judgement]} (generate-war-machine days)]
    (println markdown)
    ;; Print observation vector summary
    (println "\n## Observation Vector\n")
    (let [obs (:observation judgement)]
      (doseq [[k v] (sort-by key obs)]
        (println (str "  " (name k) ": " (format "%.2f" (double v))))))))
