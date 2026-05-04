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
            [clojure.java.shell :as shell]
            [clojure.set]
            [clojure.string :as str])
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
   {:label "npt"       :path (str home "/npt")       :workstream :consulting}])

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

(defn- arrow-health
  "Compute health [0,1] from evidence count and recency.
   Combines frequency (count in window) and freshness (days since last)."
  [evidence-count days-since-last window-days]
  (let [;; Frequency component: >10 entries/window = healthy
        freq (min 1.0 (/ (double evidence-count) 10.0))
        ;; Freshness component: seen today = 1.0, unseen in window = 0.0
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
        today (str (LocalDate/now tz))
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
                               (let [n (count daily-frames)]
                                 (reduce (fn [acc frame]
                                           (let [cd (or (:frame/cardinal-direction frame) {})]
                                             (-> acc
                                                 (update :hermit + (get cd :hermit 0.0))
                                                 (update :foraging + (get cd :foraging 0.0))
                                                 (update :cargo + (get cd :cargo 0.0))
                                                 (update :depositing + (get cd :depositing 0.0)))))
                                         {:hermit 0.0 :foraging 0.0 :cargo 0.0 :depositing 0.0}
                                         daily-frames)))
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
  "Map a pressure value to a tier keyword. Matches the working-tree
   thresholds used by futon3c.logic.metabolic-balance so the harmonic
   structure (per drain-channel-shape) is preserved across channels."
  [p]
  (cond
    (>= p 10.0) :stop-the-line
    (>= p 3.0)  :high
    (>= p 1.0)  :advisory
    :else       :silent))

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
;; Scan 12: Blocks in Flight
;;
;; A 'Block' (futon-Block, Cook-Ting sense — see
;; futon3/library/structure/block-as-futonic-revolution.flexiarg) is one
;; revolution of the futonic loop. Per M-bounded-in-flight-state, each
;; block is committed with a footer of the form:
;;   Block: <kind>-<YYYY-MM-DD>-<slug>
;; where <slug> typically references a mission doc-id (e.g. mbi05031b8c
;; for M-bounded-in-flight-state). This scan walks the 14-repo manifest
;; for recent commits carrying that footer, so the operator can read
;; recently-completed Block revolutions in one place — distinct from the
;; mission-edge sense of 'block' (which is what Strategic Judgement's
;; critical-path / mission-bottleneck priorities surface).
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
  [{:keys [loop-health support-attack mission-triage graph portfolio
           metabolic-balance blocks now days] :as data}]
  (let [sb (StringBuilder.)]
    (.append sb "# War Machine — Strategic Synthesis\n\n")
    (.append sb (str "**" now "** | " days "-day window\n\n"))

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
                                          last-evidence status invariant-evidence]}]
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
                                          repo-count max-age-days]
                                   :as ch}]
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

    ;; --- Blocks in Flight (futonic-Block, Cook-Ting sense) ---
    (when (and blocks (pos? (:total blocks 0)))
      (.append sb "## Blocks in Flight\n\n")
      (.append sb (str "_One Block = one revolution of the futonic loop "
                       "(see structure/block-as-futonic-revolution). "
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
                         "Block revolutions see the Blocks in Flight section "
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
;; Normalized observation vector (AIF terminal vocabulary)
;;
;; Converts the raw scan data into a normalized [0,1] observation vector
;; following the core-terminal-vocabulary.md schema.
;;
;; cf. cyberants observe.clj/sense->vector — same pattern, strategic domain
;; ---------------------------------------------------------------------------

(def ^:private observation-channels
  "The war machine's observation channels, harmonized from all vocabularies.
   Each channel is a named terminal with a source vocabulary and normalization."
  [:loop-health           ;; overall loop health [0,1] — from holistic argument
   :support-coverage      ;; S1-S5 evidence coverage [0,1] — from holistic argument
   :attack-coverage       ;; A1-A4 evidence coverage [0,1] — from holistic argument
   :mission-health        ;; mission triage health [0,1] — from peripheral-aif
   :stack-pct             ;; stack commit % [0,1] — from logic model / joe-hud
   :consulting-pct        ;; consulting commit % [0,1] — from JSDQ
   :portfolio-pct         ;; portfolio commit % [0,1] — from JSDQ
   :mathematics-pct       ;; mathematics commit % [0,1] — from JSDQ
   :active-repo-ratio     ;; active repos / total repos [0,1] — from logic model
   :sorry-count-norm      ;; open sorrys / 10 (capped at 1) — from sorry topology
   :coupling-density      ;; coupling edges / max edges [0,1] — from temporal analysis
   :ticks-firing-ratio    ;; firing ticks / total ticks [0,1] — from logic model
   :depositing-signal     ;; depositing cardinal direction [0,1] — from daily scan frames
   ])

(defn observe
  "Produce normalized observation vector from raw scan data.
   Returns a map of channel-id → [0,1] value.

   This is the war machine's g-observe: the bridge between
   raw scan data and the AIF loop."
  [data]
  (let [{:keys [loop-health support-attack mission-triage graph frames]} data
        {:keys [commit-percentages ticks]} (:dynamics graph {})
        {:keys [summary]} graph
        depositing-signal (or (:depositing-signal frames) 0.0)]
    {:loop-health (:overall loop-health 0.0)
     :support-coverage (:support-coverage support-attack 0.0)
     :attack-coverage (:attack-coverage support-attack 0.0)
     :mission-health (:health mission-triage 0.0)
     :stack-pct (:stack commit-percentages 0.0)
     :consulting-pct (:consulting commit-percentages 0.0)
     :portfolio-pct (:portfolio commit-percentages 0.0)
     :mathematics-pct (:mathematics commit-percentages 0.0)
     :active-repo-ratio (if (and summary (pos? (:total-repos summary)))
                          (/ (double (:active-repos summary 0))
                             (:total-repos summary))
                          0.0)
     :sorry-count-norm (min 1.0 (/ (double (:total-sorrys summary 0)) 10.0))
     :coupling-density (let [n (:total-repos summary 0)
                              max-edges (/ (* n (dec n)) 2)]
                          (if (pos? max-edges)
                            (min 1.0 (/ (double (:coupling-edges summary 0)) max-edges))
                            0.0))
     :ticks-firing-ratio (let [total (count (or ticks []))
                                firing (:ticks-firing summary 0)]
                            (if (pos? total)
                              (/ (double firing) total)
                              0.0))
     :depositing-signal depositing-signal}))

(defn sense->vector
  "Convert observation map to ordered vector (for ML/AIF consumption).
   cf. cyberants observe.clj/sense->vector."
  [obs]
  (mapv #(get obs % 0.0) observation-channels))

;; ---------------------------------------------------------------------------
;; Judgement Layer: Preferences, Free Energy, AIF Heads, Invariants
;;
;; The war machine's inference step. Sits between observe (scan data →
;; 12-channel vector) and render (display).
;;
;; Reads:
;;   1. Observation vector (from observe)
;;   2. Preferences (from war-machine-terminal-vocabulary.edn)
;;   3. AIF head states (from live endpoints)
;;   4. Invariant inventory (from futon-stack-invariant-model.edn)
;;
;; Produces: ranked priority list, free energy decomposition, losses.
;;
;; cf. cyberants policy.clj — EFE computation
;; cf. portfolio/policy.clj — action ranking
;; cf. M-aif-head: the war machine integrates all heads, not replaces them
;;
;; Invariant: WM-I1 (read-only — judge produces data, never writes)
;; Invariant: WM-I4 (sovereignty — priorities are informational, not commands)
;; ---------------------------------------------------------------------------

;; --- Preferences (C) from terminal vocabulary ---

(def ^:private preferences
  "Expected observation ranges from war-machine-terminal-vocabulary.edn :C/preferred.
   Each channel maps to [lo hi] — the range where things are healthy."
  {:loop-health        [0.8 1.0]
   :support-coverage   [0.8 1.0]
   :attack-coverage    [0.8 1.0]
   :mission-health     [0.5 1.0]
   :stack-pct          [0.15 0.25]
   :consulting-pct     [0.20 0.35]
   :portfolio-pct      [0.20 0.35]
   :mathematics-pct    [0.15 0.25]
   :active-repo-ratio  [0.5 1.0]
   :sorry-count-norm   [0.0 0.3]
   :coupling-density   [0.1 0.3]
   :ticks-firing-ratio [0.0 0.0]})

(def ^:private avoided-states
  "States the system should not be in. From :C/avoided."
  {:strategic-mode     :hermit
   :stack-pct          [0.7 1.0]
   :consulting-pct     [0.0 0.0]
   :ticks-firing-ratio [0.5 1.0]
   :sorry-count-norm   [0.8 1.0]
   :active-repo-ratio  [0.0 0.2]})

(def ^:private mode-prior
  "Prior probability over strategic modes. From :C/mode-prior."
  {:multiplied       0.35
   :depositing       0.25
   :foraging-trapped 0.15
   :hermit           0.10
   :stagnant         0.10
   :dark             0.05})

;; --- Free energy computation ---

(defn- channel-gap
  "Distance of observation from preferred range.
   Returns 0.0 if within [lo, hi], positive distance otherwise."
  [obs-val [lo hi]]
  (let [v (double (or obs-val 0.0))]
    (cond (< v lo) (- lo v)
          (> v hi) (- v hi)
          :else 0.0)))

(defn- in-avoided?
  "True if observation value falls within an avoided range."
  [obs-val [lo hi]]
  (let [v (double (or obs-val 0.0))]
    (and (>= v lo) (<= v hi))))

(def ^:private pragmatic-weights
  "Per-channel weights for pragmatic free energy.
   From :G/pragmatic-fn in terminal vocabulary."
  {:stack-pct          0.25
   :consulting-pct     0.25
   :portfolio-pct      0.15
   :mission-health     0.15
   :ticks-firing-ratio 0.10
   :sorry-count-norm   0.10})

(defn compute-free-energy
  "Compute strategic free energy from observation vector.

   Returns {:G-total :G-pragmatic :G-epistemic :per-channel-gaps :avoided-active}.

   G-pragmatic: weighted distance from preferences (dominated by workstream balance).
   G-epistemic: uncertainty from dark arrows and unaddressed claims.
   G-total: 0.65 * pragmatic + 0.35 * epistemic.

   cf. war-machine-terminal-vocabulary.edn :G/pragmatic-fn, :G/epistemic-fn"
  [obs]
  (let [;; Pragmatic: gap between observations and preferences
        per-channel (into {}
                          (for [[ch pref] preferences
                                :let [v (get obs ch 0.0)
                                      gap (channel-gap v pref)]]
                            [ch {:value v
                                 :preferred pref
                                 :gap gap
                                 :in-range? (zero? gap)}]))
        g-pragmatic (reduce-kv (fn [acc ch weight]
                                 (+ acc (* weight (get-in per-channel [ch :gap] 0.0))))
                               0.0
                               pragmatic-weights)
        ;; Epistemic: uncertainty from dark areas
        g-epistemic (+ (* 0.4 (- 1.0 (:loop-health obs 0.0)))
                       (* 0.3 (- 1.0 (:attack-coverage obs 0.0)))
                       (* 0.3 (- 1.0 (:support-coverage obs 0.0))))
        ;; Total
        g-total (+ (* 0.65 g-pragmatic) (* 0.35 g-epistemic))
        ;; Avoided states currently active
        avoided (vec (for [[k v] avoided-states
                           :when (not= k :strategic-mode)
                           :when (vector? v)
                           :when (in-avoided? (get obs k 0.0) v)]
                       k))]
    {:G-total g-total
     :G-pragmatic g-pragmatic
     :G-epistemic g-epistemic
     :per-channel per-channel
     :avoided-active avoided}))

(defn infer-mode
  "Infer strategic mode from observation vector.
   Returns keyword: :multiplied, :depositing, :foraging-trapped, :hermit, :stagnant, :dark."
  [obs]
  (let [stack (get obs :stack-pct 0.0)
        consulting (get obs :consulting-pct 0.0)
        portfolio (get obs :portfolio-pct 0.0)
        loop-h (get obs :loop-health 0.0)
        active (get obs :active-repo-ratio 0.0)
        ticks (get obs :ticks-firing-ratio 0.0)
        depositing (get obs :depositing-signal 0.0)]
    (cond
      ;; Dark: nothing happening
      (and (< active 0.2) (< loop-h 0.3))
      :dark

      ;; Depositing: consulting active (commit-based or frame-based)
      (or (> consulting 0.2) (> depositing 0.15))
      :depositing

      ;; Hermit: stack-dominated, no consulting AND no depositing signal
      (and (> stack 0.7) (< consulting 0.05) (< depositing 0.05))
      :hermit

      ;; Scanning: stack-dominated but daily scans active (transitional)
      (and (> stack 0.7) (> depositing 0.0))
      :scanning

      ;; Foraging-trapped: stuck on stack under math/portfolio pressure
      (and (> stack 0.5) (> ticks 0.5))
      :foraging-trapped

      ;; Stagnant: surfaces used but not improving
      (and (> active 0.3) (< loop-h 0.5))
      :stagnant

      ;; Multiplied: healthy balance
      :else
      :multiplied)))

;; --- AIF Head integration ---

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
        ;; Mission AIF Head — not yet exposed via HTTP
        ;; When M-aif-head wires an endpoint, add it here.
        mission-head {:head-id :mission-aif-head
                      :source "futon3c aif/mission_head.clj"
                      :available? false
                      :note "Exists in code but no HTTP endpoint yet. Needs wiring."}
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
                         (:domains live-data))]
      ;; Extract individual candidate invariants for the visualiser
      (let [individual-candidates
            (vec (mapcat (fn [fam]
                           (map (fn [inv-kw]
                                  {:id inv-kw
                                   :name (name inv-kw)
                                   :family-id (:id fam)
                                   :layer (:layer fam)})
                                (or (:candidate-invariants fam) [])))
                         candidate))]
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
       :live-domains live-domains}))))

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
   into a ranked priority list with free energy decomposition.

   This is the function the previous session identified as missing:
   the bridge between 'BUILD 0.74' and 'build THIS because THAT'.

   Returns {:mode :free-energy :priorities :losses :heads :invariants}."
  [scan-data]
  (let [obs (observe scan-data)
        free-energy (compute-free-energy obs)
        mode (infer-mode obs)
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
                     (when (= mode (:strategic-mode avoided-states))
                       [{:type :avoided-mode
                         :mode mode
                         :summary (str "System in avoided mode: " (name mode))}])
                     (map (fn [ch]
                            {:type :avoided-channel
                             :channel ch
                             :value (get obs ch 0.0)
                             :range (get avoided-states ch)
                             :summary (str (name ch) " at "
                                           (format "%.2f" (double (get obs ch 0.0)))
                                           " — in avoided range "
                                           (pr-str (get avoided-states ch)))})
                          (:avoided-active free-energy))))]
    {:mode mode
     :mode-prior (get mode-prior mode 0.0)
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
     :observation obs}))

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

(defn generate-war-machine
  "Collect all strategic scans, run judgement layer, and render.
   Returns {:data ... :judgement ... :markdown ...}."
  [days]
  (let [now-zdt (ZonedDateTime/now tz)
        now (.toString (.toLocalDate now-zdt))
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
        blocks (scan-blocks days)
        window (scan-window days now-zdt)
        scan-data {:loop-health loop-health
                   :support-attack support-attack
                   :mission-triage mission-triage
                   :graph graph
                   :sessions sessions
                   :portfolio portfolio
                   :mission-detail mission-detail
                   :patterns patterns
                   :frames frames
                   :metabolic-balance metabolic-balance
                   :blocks blocks
                   :window window}
        ;; Run the judgement layer
        judgement (judge scan-data)]
    {:data scan-data
     :judgement judgement
     :markdown (render-war-machine {:loop-health loop-health
                                    :support-attack support-attack
                                    :mission-triage mission-triage
                                    :graph graph
                                    :portfolio portfolio
                                    :metabolic-balance metabolic-balance
                                    :blocks blocks
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
