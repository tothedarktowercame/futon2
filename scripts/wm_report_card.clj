(ns wm-report-card
  "Public, provenance-bearing report card for one persisted War Machine run."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [wm-run-facts :as run-facts]))

(defn absent [reason & [source-path]]
  (cond-> {:status :absent :reason reason}
    source-path (assoc :source-path source-path)))

(defn absent? [x]
  (and (map? x) (contains? #{:absent :missing :failed :refused
                             :typed-missing :not-recomputable}
                           (:status x))))

(defn- nr? [x]
  (and (map? x) (contains? x "not-recomputable")))

(defn- value-at [record path reason]
  (let [v (get-in record path ::missing)]
    (if (or (= ::missing v) (nil? v)) (absent reason path) v)))

(defn- public-string [s]
  (-> s
      (str/replace #"/home/joe/code/" "")
      (str/replace #"/home/joe(?:/[^\s\]\[(){}<>\"']*)?" "<host-path-redacted>")
      ;; Public cards never retain URL userinfo or query/fragment secrets.
      (str/replace #"https?://[^\s/@]+:[^\s/@]+@" "https://<credentials-redacted>@")
      (str/replace #"(https?://[^\s?#]+)[?#][^\s]*" "$1?<query-redacted>")))

(defn sanitize [x]
  (cond
    (string? x) (public-string x)
    (map? x) (into (empty x) (map (fn [[k v]] [k (sanitize v)]) x))
    (vector? x) (mapv sanitize x)
    (set? x) (set (map sanitize x))
    (sequential? x) (doall (map sanitize x))
    :else x))

(def q-deps
  ;; Exact fields read by Requirements.lean Q1..Q10, including fields newer
  ;; than the current exporter. A missing exporter carrier therefore yields
  ;; not-recomputable, never a guessed verdict.
  {:Q1 ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"]
   :Q2 ["openMissions" "openExcursions" "openTickets" "targetConstruction"
        "libraryPatternCount" "constructorPatternCount"]
   :Q3 ["cascadesWithoutG"]
   :Q4 ["gTerms" "horizonLength" "preferenceSteps" "gradedPreferenceSteps"
        "policiesWithRiskTerm" "policiesWithAmbiguityTerm"
        "policiesWithInformationTerm" "comparedPolicies"]
   :Q5 ["interpretationOrder"]
   :Q6 ["previousChoice" "previousOutcome" "previousInputDigest"
        "currentChoice" "currentInputDigest"]
   :Q7 ["pathAbsenceCount"]
   :Q8 ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"
        "targetsReachingScoring" "targetsWithG" "libraryPatternCount"
        "targetConstruction" "constructedCascades" "comparedPolicies"
        "seatsAvailable" "seatsUsed"]
   :Q9 ["completionPreferencePairs" "completionPairsStrictlyPreferred"
        "earlierProgressPairs" "earlierProgressNoGreaterRisk"]
   :Q10 ["differentArrangementPairs" "arrangementPairsDistinguishedByG"]})

(defn- open-tasks [f]
  (set/union (set (f "openMissions")) (set (f "openExcursions"))
             (set (f "openTickets"))))
(defn- tc-targets [tc] (set (tc "targets")))
(defn- tc-union [rows k] (reduce set/union #{} (map #(set (% k)) rows)))
(defn- total-policies [rows]
  (reduce + 0 (map #(* (count (tc-targets %)) (% "policyCount")) rows)))
(defn- max-slice [rows] (reduce max 0 (map #(count (% "slice")) rows)))

(defn- requirement-value [q f]
  (case q
    :Q1 (= (set (f "enumeratedTasks")) (open-tasks f))
    :Q2 (let [rows (f "targetConstruction") ot (open-tasks f)]
          (and (every? #(and (set/subset? (tc-targets %) ot)
                             (= (set (% "pool")) (set (% "slice")))
                             (true? (% "sliceFromWholeLibrary"))
                             (<= (count (% "slice")) (f "libraryPatternCount"))
                             (pos? (% "policyCount"))) rows)
               (= (tc-union rows "targets") ot)
               (= (f "constructorPatternCount")
                  (count (tc-union rows "pool")))))
    :Q3 (empty? (f "cascadesWithoutG"))
    :Q4 (let [g (f "gTerms") n (f "horizonLength") expected (set (range n))
              pc (count (f "comparedPolicies"))]
          (and (true? (g "risk")) (true? (g "ambiguity"))
               (true? (g "informationGain")) (pos? n)
               (= expected (set (f "preferenceSteps")))
               (= expected (set (f "gradedPreferenceSteps")))
               (= pc (f "policiesWithRiskTerm")
                     (f "policiesWithAmbiguityTerm")
                     (f "policiesWithInformationTerm"))))
    :Q5 (= "selectionBeforeInterpretation" (f "interpretationOrder"))
    :Q6 (not (and (= "refused" (f "previousOutcome"))
                  (= (f "previousInputDigest") (f "currentInputDigest"))
                  (= (f "previousChoice") (f "currentChoice"))))
    :Q7 (zero? (f "pathAbsenceCount"))
    :Q8 (let [ot (open-tasks f) e (set (f "enumeratedTasks"))
              scoring (set (f "targetsReachingScoring")) wg (set (f "targetsWithG"))
              rows (f "targetConstruction") avail (set (f "seatsAvailable"))
              used (set (f "seatsUsed"))]
          (and (>= (* 10 (count e)) (* 9 (count ot)))
               (>= (* 2 (count scoring)) (count e))
               (>= (* 2 (count wg)) (count e))
               (or (< (f "libraryPatternCount") 100) (<= 10 (max-slice rows)))
               (<= 2 (count (f "constructedCascades")))
               (<= 2 (count (f "comparedPolicies")))
               (<= (* 2 (count ot)) (total-policies rows))
               (or (< (count avail) 10) (<= (count avail) (* 10 (count used))))))
    :Q9 (and (pos? (f "completionPreferencePairs"))
             (= (f "completionPairsStrictlyPreferred") (f "completionPreferencePairs"))
             (pos? (f "earlierProgressPairs"))
             (= (f "earlierProgressNoGreaterRisk") (f "earlierProgressPairs")))
    :Q10 (and (pos? (f "differentArrangementPairs"))
              (= (f "arrangementPairsDistinguishedByG")
                 (f "differentArrangementPairs")))))

(defn verdict-rows [export]
  (let [facts (:facts export)]
    (mapv (fn [q]
            (let [deps (q-deps q)
                  unavailable (vec (filter #(or (not (contains? facts %))
                                                (nr? (facts %))) deps))
                  deciding (select-keys facts deps)]
              (if (seq unavailable)
                {:requirement q :status :not-recomputable
                 :deciding-facts deciding
                 :reason (str "Required fact unavailable: " (str/join ", " unavailable))}
                (let [pass? (requirement-value q facts)]
                  {:requirement q :status (if pass? :pass :fail)
                   :deciding-facts deciding
                   :reason (str "Requirements.lean " (name q) " predicate evaluated " pass?)}))))
          (map #(keyword (str "Q" %)) (range 1 11)))))

(defn- candidate-id [c] (or (get-in c [:id :id]) (:id c) (:candidate c)))
(defn- candidate-target [c] (or (get-in c [:id :target]) (:target c)))
(defn- candidate-g [c]
  (or (:G c) (:g c) (:expected-free-energy c) (:controller-score c)
      (get-in c [:score :G]) (get-in c [:score :g])))
(defn- candidate-patterns [c]
  (or (get-in c [:f-prefix :policy :precedence]) (get-in c [:id :precedence])
      (:precedence c) []))
(defn- pattern-id [p] (if (map? p) (:id p) p))

(defn- find-paths [x pred]
  (letfn [(walk [path v]
            (concat (when (pred path v) [[path v]])
                    (cond (map? v) (mapcat (fn [[k y]] (walk (conj path k) y)) v)
                          (sequential? v) (mapcat (fn [[i y]] (walk (conj path i) y))
                                                  (map-indexed vector v))
                          :else [])))]
    (walk [] x)))

(defn- first-key [record k reason]
  (if-let [[p v] (first (find-paths record (fn [path _] (= k (peek path)))))]
    {:value v :source-path p}
    (absent reason)))

(defn- document [record chosen-candidate target]
  (let [locs (or (get-in chosen-candidate [:f-prefix :policy :observation-locators])
                 (:observation-locators chosen-candidate))
        loc (some (fn [[[t _] v]] (when (= target t) v)) locs)]
    {:title (or (:title loc) (absent "document title not recorded"))
     :reference (if (and (:repo loc) (:path loc))
                  (str (:repo loc) "/" (:path loc))
                  (absent "document reference not recorded"))
     :source-path (if loc
                    [:decision :selection-certificate :candidates :chosen
                     :observation-locators]
                    (absent "chosen candidate locator absent"))}))

(defn- run-section [record]
  (let [timing (:registered-run/timing record)
        phases (or (:phase-timings-ms timing) (:phase-wall-timings-ms timing))
        jobs (get-in record [:registered-run/model-usage :jobs])]
    {:started-at (value-at record [:startedAt] "run start absent")
     :duration-ms (or (:wall-clock-ms timing) (absent "run duration absent"))
     :outcome (or (get-in record [:terminal-receipt :failure-kind])
                  (get-in record [:terminal-receipt :outcome])
                  (absent "terminal outcome absent"))
     :phase-timeline (if (map? phases)
                       (mapv (fn [[phase duration]] {:phase phase :duration-ms duration}) phases)
                       (absent "phase timings absent"))
     :debugger (if (contains? timing :debugger-dwell-receipts)
                 {:stopped-ms (:debugger-stopped-ms timing)
                  :stops (:debugger-dwell-receipts timing)}
                 (absent "debugger records absent"))
     :seats-and-roles (or (get-in record [:participants :roles])
                          (absent "participant roles absent"))
     :token-usage (if (seq jobs)
                    (mapv #(select-keys % [:role :job-id :phase :status :model
                                           :input-tokens :output-tokens :total-tokens]) jobs)
                    (absent "no per-seat token usage recorded"))
     :terminal-receipt (value-at record [:terminal-receipt] "terminal receipt absent")}))

(defn- cascade-section [record]
  (let [chosen (get-in record [:decision :chosen])
        cert (get-in record [:decision :selection-certificate])
        candidates (vec (:candidates cert))
        target (:target chosen)
        selected (or (first (filter #(and (= target (candidate-target %))
                                          (= (:id chosen) (candidate-id %))) candidates))
                     (first (filter #(= target (candidate-target %)) candidates)))
        scoring (:scoring cert)
        ranked (->> candidates
                    (map-indexed (fn [i c] {:index i :candidate c}))
                    (filter #(number? (candidate-g (:candidate %))))
                    (sort-by #(candidate-g (:candidate %))) (take 4))
        receipts (or (get-in selected [:f-prefix :policy :interpretation-receipts])
                     (:interpretation-receipts selected))
        patterns (vec (or (:precedence chosen) (map pattern-id (candidate-patterns selected))))]
    {:chosen-target (or target (absent "chosen target absent"))
     :document (if target (document record selected target)
                   (absent "chosen target absent"))
     :why-it-won
     (if (seq ranked)
       {:comparison (mapv (fn [rank]
                            (if-let [{:keys [index candidate]} (nth ranked rank nil)]
                              {:rank (inc rank)
                               :target (candidate-target candidate)
                               :policy (candidate-id candidate)
                               :G (candidate-g candidate)
                               :terms (or (:g-terms candidate)
                                          (get-in candidate [:certificate :g-terms])
                                          (get-in scoring [index :g-terms])
                                          (absent "risk/ambiguity/EIG/F terms absent"))}
                              (absent (str "rank " (inc rank)
                                           " policy absent; fewer than four numeric policies recorded"))))
                          (range 4))
        :ties-within-resolution (or (get-in cert [:selection-law :ties-within-resolution])
                                    (get-in cert [:law-applied :ties-within-resolution])
                                    (absent "tie-resolution carrier absent"))}
       (absent "numeric candidate comparison absent"))
     :arrangement (if (seq patterns)
                    (mapv (fn [p]
                            {:pattern p
                             :interpretation-receipt
                             (or (get receipts p)
                                 (absent "interpretation receipt absent"))}) patterns)
                    (absent "chosen pattern arrangement absent"))
     :selection-redecision (or (:selection-redecision record)
                               (get-in record [:failure :detail :selection-redecision])
                               (absent "selection redecision outcome absent"))
     :author-request (or (get-in record [:d-task-enactment :ask])
                         (get-in record [:d-task-enactment :request])
                         (get-in record [:interpretation-ask :reading])
                         (absent "author build request not retained"))}))

(defn- outcome-section [record]
  (let [falsifier (or (get-in record [:failure :detail :reviewer-falsifier])
                      (get-in record [:terminal :reviewer-falsifier]))]
    {:commits (let [xs (distinct (keep identity
                            [(:grounded-commit record)
                             (get-in record [:failure :detail :commit])
                             (get-in record [:failure :detail :artifact-binding :commit])]))]
                (if (seq xs) (vec xs) (absent "commit evidence absent")))
     :review-verdict (or (get-in record [:failure :detail :review-job :result-summary])
                         (get-in record [:terminal :review-verdict])
                         (absent "review verdict absent"))
     :falsifier-verdict (or falsifier (absent "falsifier verdict absent"))
     :grounding (or (:grounded-commit record)
                    (get-in record [:failure :detail :artifact-binding])
                    (absent "grounding absent"))
     :run-output (or (:run-output record) (absent "run-output feature card absent"))
     :delivery-qa (first-key record :delivery-qa "Field Desk delivery-QA note absent")
     :morning-brief-addendum-id
         (or (some-> (first (find-paths record
                                    (fn [path _]
                                      (contains? #{:morning-brief/addendum-id
                                                   :addendum-id} (peek path))))) second)
         (absent "morning-brief addendum id absent"))}))

(defn- example-text [record cascade outcome]
  (let [target (:chosen-target cascade)
        pats (when (vector? (:arrangement cascade)) (mapv :pattern (:arrangement cascade)))
        terminal (get-in record [:terminal-receipt :failure-kind])
        commit (when (vector? (:commits outcome)) (first (:commits outcome)))
        sentences (cond-> []
                    (not (absent? target))
                    (conj {:text (str "The run addressed " target ".")
                           :source-path [:decision :chosen :target]})
                    (seq pats)
                    (conj {:text (str "It selected the arranged cascade "
                                      (str/join " → " (map str pats)) ".")
                           :source-path [:decision :chosen :precedence]})
                    terminal
                    (conj {:text (str "The recorded terminal result was " terminal ".")
                           :source-path [:terminal-receipt :failure-kind]})
                    commit
                    (conj {:text (str "The recorded artifact commit was " commit ".")
                           :source-path [:failure :detail :commit]}))]
    (if (seq sentences) {:paragraph (str/join " " (map :text sentences))
                         :sentences sentences}
        (absent "record contains no fields from which to compose example text"))))

(defn build-card
  ([record record-path snap] (build-card record record-path snap nil nil))
  ([record record-path snap previous previous-path]
   (let [export (run-facts/facts-for-record record record-path snap previous previous-path)
         cascade (cascade-section record)
         outcome (outcome-section record)]
     (sanitize
      {:schema :wm/report-card-v1
       :run-id (or (:run/id record) (absent "run id absent"))
       :source-record record-path
       :sections
       [{:id :verdict :title "Verdict" :rows (verdict-rows export)
         :export-snapshot export}
        {:id :run :title "Run" :data (run-section record)}
        {:id :cascade :title "Cascade" :data cascade}
        {:id :outcome :title "Outcome" :data outcome}
        {:id :example-text :title "Example text"
         :data (example-text record cascade outcome)}]}))))

(defn- html-escape [x]
  (-> (str x) (str/replace "&" "&amp;") (str/replace "<" "&lt;")
      (str/replace ">" "&gt;") (str/replace "\"" "&quot;")
      (str/replace "'" "&#39;")))

(defn markdown [card]
  (let [[verdict run cascade outcome example] (:sections card)]
    (str "# War Machine report card: " (:run-id card) "\n\n"
         "Source: `" (:source-record card) "`\n\n"
         "## Verdict\n\n| Requirement | Status | Reason | Deciding facts |\n"
         "|---|---|---|---|\n"
         (apply str (for [r (:rows verdict)]
                      (format "| %s | **%s** | %s | `%s` |\n"
                              (name (:requirement r)) (name (:status r))
                              (:reason r) (pr-str (:deciding-facts r)))))
         "\n## Run\n\n```clojure\n" (pr-str (:data run)) "\n```\n"
         "\n## Cascade\n\n```clojure\n" (pr-str (:data cascade)) "\n```\n"
         "\n## Outcome\n\n```clojure\n" (pr-str (:data outcome)) "\n```\n"
         "\n## Example text\n\n"
         (if (absent? (:data example)) (str "`" (pr-str (:data example)) "`")
             (str (get-in example [:data :paragraph]) "\n\nSources: `"
                  (pr-str (mapv :source-path (get-in example [:data :sentences]))) "`"))
         "\n")))

(defn html [card]
  (let [[verdict run cascade outcome example] (:sections card)
        pre (fn [x] (str "<pre>" (html-escape (pr-str x)) "</pre>"))]
    (str "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
         "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
         "<title>WM report card " (html-escape (:run-id card)) "</title>"
         "<link rel=\"stylesheet\" href=\"../paper-site.css\">"
         "<style>body{max-width:1100px;margin:auto;padding:2rem}pre{white-space:pre-wrap}"
         "table{border-collapse:collapse;width:100%}td,th{border:1px solid #aaa;padding:.4rem;vertical-align:top}</style>"
         "</head><body><main><h1>War Machine report card: "
         (html-escape (:run-id card)) "</h1><p>Source: <code>"
         (html-escape (:source-record card)) "</code></p>"
         "<h2>Verdict</h2><table><thead><tr><th>Requirement</th><th>Status</th>"
         "<th>Reason</th><th>Deciding facts</th></tr></thead><tbody>"
         (apply str (for [r (:rows verdict)]
                      (str "<tr><td>" (html-escape (name (:requirement r))) "</td><td>"
                           (html-escape (name (:status r))) "</td><td>"
                           (html-escape (:reason r)) "</td><td>"
                           (pre (:deciding-facts r)) "</td></tr>")))
         "</tbody></table><h2>Run</h2>" (pre (:data run))
         "<h2>Cascade</h2>" (pre (:data cascade))
         "<h2>Outcome</h2>" (pre (:data outcome))
         "<h2>Example text</h2>"
         (if (absent? (:data example)) (pre (:data example))
             (str "<p>" (html-escape (get-in example [:data :paragraph])) "</p>"
                  (pre (mapv :source-path (get-in example [:data :sentences])))))
         "</main></body></html>")))

(defn generate!
  ([record-path] (generate! record-path {}))
  ([record-path {:keys [output-dir snap]}]
   (let [record (run-facts/read-edn record-path)
         [previous previous-path] (run-facts/lookup-previous record-path record)
         snap (or snap (run-facts/snapshot))
         run-id (:run/id record)
         out (io/file (or output-dir
                          (io/file (.getParentFile (io/file record-path)) run-id)))
         card (build-card record record-path snap previous previous-path)
         paths {:edn (io/file out "report-card.edn")
                :markdown (io/file out "report-card.md")
                :html (io/file out "report-card.html")}]
     (.mkdirs out)
     (spit (:edn paths) (pr-str card))
     (spit (:markdown paths) (markdown card))
     (spit (:html paths) (html card))
     (into {} (map (fn [[k f]] [k (.getPath ^java.io.File f)]) paths)))))

(defn -main [& args]
  (let [m (apply hash-map args)
        path (get m "--run")]
    (when-not path
      (throw (ex-info "usage: clojure -M -m wm-report-card --run RECORD [--output-dir DIR]" {})))
    (prn (generate! path {:output-dir (get m "--output-dir")}))))
