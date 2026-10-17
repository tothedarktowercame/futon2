(ns futon2.aif.selected-target-reading
  "Same-click repair for an outer-selected target whose current HEAD has no
  validated reading.  The outer receipt is input, never recomputed here."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.analysis-cascade :as analysis-cascade]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.target-reading-registry :as registry]))

(def default-request-root
  "/home/joe/code/storage/wm-target-reading-requests")
(def default-request-builder
  "/home/joe/code/futon2/scripts/wm_task_reading.py")
(def default-analysis-validator
  "/home/joe/code/futon3c/scripts/session_turn_analysis.py")

(defn- digest [bytes]
  (identity/sha256 bytes))

(defn- file-digest [path]
  (digest (java.nio.file.Files/readAllBytes (.toPath (io/file path)))))

(defn analysis-string
  "Decode the author's terminal reply string. The requisition asks for
   exactly one JSON object; authors regularly return that object wrapped in
   a single Markdown code fence (occ-dc843e15, 2026-10-17: a valid fenced
   analysis was refused :selected-target-reading-answer-invalid and the
   whole click abstained). One fence wrapping exactly one JSON object is
   accepted; prose around an unfenced object, multiple fences, or anything
   that still does not parse to a map is still refused."
  [raw]
  (handoff/response-json-object raw))

(defn- codepoint-slice
  "Python's validator interprets offsets as Unicode codepoints, while Java
  substring indexes UTF-16 code units. Translate explicitly so correction
  diagnostics quote the same immutable source slice as the validator."
  [^String source start end]
  (when (and (integer? start) (integer? end) (<= 0 start) (< start end)
             (<= end (.codePointCount source 0 (.length source))))
    (let [a (.offsetByCodePoints source 0 start)
          b (.offsetByCodePoints source 0 end)]
      (.substring source a b))))

(defn- occurrences
  "Return all UTF-16 start indexes of needle in haystack."
  [^String haystack ^String needle]
  (when (and (seq needle) (string? haystack))
    (loop [from 0 found []]
      (let [at (.indexOf haystack needle from)]
        (if (neg? at)
          found
          (recur (inc at) (conj found at)))))))

(defn- resolve-quote
  [^String source scope-start scope-end text]
  (let [scope (codepoint-slice source scope-start scope-end)
        matches (occurrences scope text)]
    (case (count matches)
      1 (let [char-at (first matches)
              relative-start (.codePointCount ^String scope 0 char-at)
              length (.codePointCount ^String text 0 (.length ^String text))]
          {:status :resolved
           :start (+ scope-start relative-start)
           :end (+ scope-start relative-start length)})
      0 {:status :refused :reason :quoted-text-absent :text text
         :scope [scope-start scope-end]}
      ;; A repeated cue at the beginning of its enclosing fragment is still
      ;; structurally unambiguous: "the leading X", without trusting numbers.
      (if (zero? (first matches))
        (let [length (.codePointCount ^String text 0 (.length ^String text))]
          {:status :resolved :start scope-start :end (+ scope-start length)})
        {:status :refused :reason :quoted-text-ambiguous :text text
         :scope [scope-start scope-end] :matches (count matches)}))))

(def ^:private relation-aliases
  {"evidence" "rationale"
   "sequence" "dependency"
   "elaboration" "context"
   "authority" "context"})

(def ^:private relation-vocabulary
  #{"action" "condition" "context" "contrast" "dependency" "goal" "rationale"})

(defn- normalize-relations [relations]
  (->> relations
       (map #(get relation-aliases % %))
       (filter relation-vocabulary)
       distinct
       vec))

(defn- words [s]
  (if (str/blank? s) 0 (count (str/split (str/trim s) #"\s+"))))

(defn- apply-cue-budget [sentence source fragments]
  (let [total (words (codepoint-slice source (:start sentence) (:end sentence)))
        budget (if (> total 8) (max (quot total 2) 2) Long/MAX_VALUE)
        remaining (atom budget)]
    (mapv (fn [fragment]
            (let [kept (reduce (fn [out cue]
                                 (let [n (words (:text cue))]
                                   (if (<= n @remaining)
                                     (do (swap! remaining - n) (conj out cue))
                                     out)))
                               [] (:display_cues fragment))]
              (cond-> (assoc fragment :display_cues kept)
                (empty? kept)
                (assoc :no_surface_cue
                       "Display cues omitted by deterministic sentence density budget."))))
          fragments)))

(defn resolve-analysis-offsets
  "Replace author-supplied fragment and display-cue offsets with offsets found
  deterministically from their quoted text. Quotes must occur exactly once in
  their enclosing sentence/fragment. The Python validator remains authoritative
  for every semantic and structural rule after this projection."
  [request analysis]
  (let [source (:source_text request)
        sentences (into {} (map (juxt :id identity) (:sentences request)))]
    (if-not (and (string? source) (map? analysis))
      {:status :refused :reason :malformed-offset-resolution-input}
      (loop [remaining (:sentences analysis) resolved []]
        (if-let [{:keys [id fragments] :as analysis-sentence} (first remaining)]
          (if-let [sentence (get sentences id)]
            (let [fragment-results
                  (mapv
                   (fn [fragment]
                     (let [located (resolve-quote source (:start sentence)
                                                  (:end sentence) (:text fragment))]
                       (if-not (= :resolved (:status located))
                         (assoc located :sentence id :field :fragment)
                         (let [cue-results
                               (mapv (fn [cue]
                                       (let [cue-location
                                             (resolve-quote source (:start located)
                                                            (:end located) (:text cue))]
                                         (if (= :resolved (:status cue-location))
                                           (merge cue (select-keys cue-location [:start :end]))
                                           (assoc cue-location :sentence id
                                                  :field :display-cue))))
                                     (:display_cues fragment))]
                           (if-let [problem (first (filter :reason cue-results))]
                             problem
                             (-> fragment
                                 (merge (select-keys located [:start :end]))
                                 (assoc :relations (normalize-relations (:relations fragment))
                                        :display_cues cue-results)))))))
                   fragments)]
              (if-let [problem (first (filter :reason fragment-results))]
                problem
                (recur (next remaining)
                       (conj resolved
                             (assoc analysis-sentence :fragments
                                    (apply-cue-budget sentence source fragment-results))))))
            {:status :refused :reason :unknown-analysis-sentence :sentence id})
          {:status :resolved :analysis (assoc analysis :sentences resolved)})))))

(defn offset-diagnostics
  "Name fragment/cue offset mismatches in ANALYSIS against REQUEST. These are
  feedback for the author; the Python validator remains publication authority."
  [request analysis]
  (let [source (:source_text request)
        sentences (into {} (map (juxt :id identity) (:sentences request)))]
    (vec
     (mapcat
      (fn [{:keys [id fragments]}]
        (let [sentence (get sentences id)]
          (mapcat
           (fn [{:keys [start end text display_cues]}]
             (let [expected (when (string? source) (codepoint-slice source start end))
                   fragment-error
                   (when (or (nil? sentence)
                             (not (and (integer? start) (integer? end)
                                       (<= (:start sentence) start)
                                       (< start end)
                                       (<= end (:end sentence))))
                             (not= expected text))
                     [{:sentence id :field :fragment :start start :end end
                       :submitted text :expected expected}])]
               (into (or fragment-error [])
                     (keep (fn [{a :start b :end cue-text :text}]
                             (let [cue-expected (when (string? source)
                                                  (codepoint-slice source a b))]
                               (when (or (not (and (integer? a) (integer? b)
                                                   (integer? start) (integer? end)
                                                   (<= start a) (< a b) (<= b end)))
                                         (not= cue-expected cue-text))
                                 {:sentence id :field :display-cue
                                  :fragment-start start :fragment-end end
                                  :start a :end b :submitted cue-text
                                  :expected cue-expected})))
                           display_cues))))
           fragments)))
      (:sentences analysis)))))

(defn selected-need
  "Return the exact outer-selected target/source/current-digest need, or nil.
  A failure belonging to another target is never eligible."
  [judgement]
  (let [outer-target (get-in judgement [:outer-task-selection :chosen :id])
        families (get-in judgement [:decision :selection-certificate
                                    :target-policy-field :families])
        ;; The live judge can expose the target-local family before its outer
        ;; receipt is retained on the runner checkpoint.  In that interval a
        ;; single family is still an immutable target identity: production
        ;; constructs the inner field for exactly one outer-selected target.
        ;; Never infer from the first of several families.
        sole-family-target (when (= 1 (count families))
                             (:target-id (first families)))
        target (or outer-target sole-family-target)
        family (first (filter #(= target (:target-id %)) families))
        failure (first (filter #(contains? #{:no-current-target-reading
                                             :stale-target-reading}
                                           (:kind %))
                               (:failures family)))
        row (first (filter #(= target (:target-id %))
                           (get-in judgement [:cascade-problems :target-sources])))
        outer-source (get-in judgement [:outer-task-selection :chosen :source])
        outer-path (or (:path outer-source) (:source-path outer-source))
        row-path (:source-path row)]
    (when failure
      {:target target
       ;; The retained outer receipt is authoritative.  The cascade source row
       ;; is a compatibility fallback for older judge fixtures, never a reason
       ;; to select a different target.
       :source-path (or outer-path row-path)
       :source-pin-conflict? (boolean (and outer-path row-path
                                           (not= outer-path row-path)))
       :excerpt-digest (:expected-digest failure)
       :failure (:kind failure)})))

(defn prompt
  "Prompt for the designated author.  The reply is analysis JSON only; the
  machine, not the seat, validates and publishes it."
  [{:keys [request acceptance-contract validation-feedback prior-analysis]}]
  (str "Produce one session_turn_analysis analysis for this immutable request. "
       "Quote every fragment and display cue exactly, including punctuation and "
       "whitespace. The machine derives Unicode-codepoint offsets from each quote "
       "inside its enclosing sentence or fragment; your numeric start/end values "
       "are not trusted. A quote that is absent or occurs more than once in its "
       "scope is refused, so choose enough exact context to identify one occurrence. "
       "Display cues must also obey the acceptance contract's density limit, and "
       "relations must come from its documented vocabulary. Use only actual patterns under "
       "/home/joe/code/futon3/library. Do not publish it and do not change repositories. "
       "Reply with exactly one JSON object in the analysis-input shape accepted by "
       "/home/joe/code/futon3c/scripts/session_turn_analysis.py complete.\n\n"
       (when validation-feedback
         (str "Your preceding answer was rejected by that validator: "
              validation-feedback
              " Correct the analysis against the unchanged request. The rejected "
              "answer is included only so you can repair it:\n"
              (json/generate-string prior-analysis {:pretty true}) "\n\n"))
       (handoff/prompt-block acceptance-contract)
       "\n"
       (json/generate-string request {:pretty true})))

(defn- request-file [root run-id target excerpt]
  (io/file root (digest (.getBytes (str run-id "\u0000" target "\u0000" excerpt)
                                    "UTF-8"))
           "request.json"))

(defn- refused [need kind & [detail]]
  (cond-> {:schema :wm/selected-target-reading-request-v1
           :status :refused :kind kind :published false
           :target (:target need) :source-path (:source-path need)
           :excerpt-digest (:excerpt-digest need)}
    detail (assoc :detail detail)))

(defn repair!
  "Issue, validate, and publish one reading for JUDGEMENT's exact retained
  outer choice. ANSWER-FN is the sole contact port and must address the
  designated author supplied in OPTS. Returns a recorded request outcome."
  [{:keys [run-id author answer-fn request-root request-builder analysis-validator
           target-reading-root]
    :or {request-root default-request-root
         request-builder default-request-builder
         analysis-validator default-analysis-validator}}
   judgement]
  (when-let [need (selected-need judgement)]
    (cond
      (or (:source-pin-conflict? need)
          (not (string? (:source-path need)))
          (not (.isFile (io/file (:source-path need))))
          (not (string? (:excerpt-digest need))))
      (refused need :selected-target-reading-unpinned)

      (not (fn? answer-fn))
      (refused need :designated-author-unavailable {:author author :contacted? false})

      :else
      (let [request-file (request-file request-root run-id (:target need)
                                       (:excerpt-digest need))
            _ (io/make-parents request-file)
            built (shell/sh "python3" request-builder (:source-path need)
                            "--target" (str (:target need)) "--mission-head"
                            "--out" (.getPath request-file))]
        (if-not (zero? (:exit built))
          (refused need :selected-target-reading-request-refused
                   {:exit (:exit built) :stderr (:err built)})
          (let [request (json/parse-string (slurp request-file) true)
                template-result (shell/sh "python3" analysis-validator "template"
                                          (.getPath request-file))
                acceptance-contract
                (when (zero? (:exit template-result))
                  (some-> (:out template-result)
                          (json/parse-string true)
                          :acceptance_contract
                          (update :schema keyword)
                          (update :id keyword)
                          (update-in [:validator :id] keyword)
                          (update-in [:response :required] #(mapv keyword %))
                          handoff/contract))
                pinned? (and (= (:target need) (get-in request [:task :target_id]))
                             (= (.getCanonicalPath (io/file (:source-path need)))
                                (get-in request [:task :file_path]))
                             (= (:excerpt-digest need)
                                (get-in request [:task :excerpt_sha256]))
                             (= (file-digest (:source-path need))
                                (get-in request [:task :content_sha256])))
                request-sha (file-digest request-file)]
            (cond
              (nil? acceptance-contract)
              (refused need :selected-target-reading-contract-unavailable
                       {:exit (:exit template-result) :stderr (:err template-result)})

              (not pinned?)
              (refused need :selected-target-reading-pin-mismatch
                       {:request-file (.getPath request-file)})

              :else
              (let [issued {:target (:target need) :kind :selected-target-reading
                            :request-id request-sha :request request :author author
                            :acceptance-contract acceptance-contract}
                    decode (fn [raw]
                             (cond
                               (map? (:analysis raw)) (:analysis raw)
                               (map? raw) raw
                               :else (analysis-string raw)))
                    contact (fn [issued]
                              (handoff/exchange {:contact answer-fn :request issued
                                                 :decode decode}))
                    exchange (contact issued)
                    analysis (:value exchange)]
                (if-not (= :validated (:status exchange))
                  (refused need
                           (if (= :designated-author-unavailable (:reason exchange))
                             :designated-author-unavailable
                             :selected-target-reading-answer-invalid)
                           {:author author :contacted? (:contacted? exchange)
                            :answer (select-keys exchange
                                                 [:status :reason :job-id :message])})
                  (let [analysis-input (io/file (.getParentFile request-file)
                                                "analysis-input.json")
                        validation-request (io/file (.getParentFile request-file)
                                                    "validation-request.json")
                        attempt (atom 0)
                        ;; The validator's `complete` publishes its output by
                        ;; EXCLUSIVE creation (os.link): it refuses to replace
                        ;; an earlier interpretation. A correction round re-runs
                        ;; `complete` on the same request, so each attempt gets
                        ;; its own request copy and its own output; the legacy
                        ;; single-name output is refreshed after the fact.
                        last-output (atom nil)
                        validate (fn [candidate]
                                   (let [n (swap! attempt inc)
                                         resolution (resolve-analysis-offsets request candidate)
                                         attempt-file (io/file (.getParentFile request-file)
                                                               (str "analysis-input-attempt-" n ".json"))
                                         resolved-file (io/file (.getParentFile request-file)
                                                                (str "analysis-resolved-attempt-" n ".json"))
                                         attempt-request (io/file (.getParentFile request-file)
                                                                  (str "validation-request-attempt-"
                                                                       n ".json"))]
                                     ;; Keep the answer exactly as received; later attempts never
                                     ;; overwrite the evidence needed to assess correction quality.
                                     (spit attempt-file
                                           (json/generate-string candidate {:pretty true}))
                                     (if-not (= :resolved (:status resolution))
                                       {:exit 1 :err (str "Offset resolution refused: "
                                                         (pr-str (dissoc resolution :analysis)))}
                                       (do
                                         (spit resolved-file
                                               (json/generate-string (:analysis resolution)
                                                                     {:pretty true}))
                                         ;; Compatibility copy for existing tooling; it is always
                                         ;; the deterministic projection, never the raw answer.
                                         (spit analysis-input
                                               (json/generate-string (:analysis resolution)
                                                                     {:pretty true}))
                                         (io/copy request-file attempt-request)
                                         (let [result (shell/sh "python3" analysis-validator
                                                                "complete"
                                                                (.getPath attempt-request)
                                                                (.getPath resolved-file))]
                                           (reset! last-output
                                                   (io/file (str (.getPath attempt-request)
                                                                 ".analysis.json")))
                                           result)))))
                        ;; The python validator stamps library-verified
                        ;; pattern_refs (author sends id+rationale; it adds
                        ;; status/source_sha256) but does not require one per
                        ;; fragment; the registry's cascade construction does.
                        ;; Check that second layer BEFORE deciding on a
                        ;; correction round (occ-a9c0c166, 2026-10-17: a
                        ;; shape-valid answer with zero pattern_refs passed
                        ;; the validator, was refused only at publication, and
                        ;; the author never learned what was missing).
                        cascade-failures
                        (fn []
                          (let [output @last-output]
                            (when (.isFile output)
                              (let [result (analysis-cascade/analysis->cascades
                                            (json/parse-string (slurp output) true)
                                            {:mode :overlap})]
                                (when (= :failed (:status result))
                                  (:failures result))))))
                        validated0 (validate analysis)
                        cascade0 (cascade-failures)
                        correction (when-not (and (zero? (:exit validated0))
                                                  (nil? cascade0))
                                     (contact (assoc issued
                                                     :kind :selected-target-reading-correction
                                                     :validation-feedback
                                                     (str (when-not (zero? (:exit validated0))
                                                            (str (:err validated0) "\n"))
                                                          "Pattern citation diagnostics: "
                                                          (json/generate-string
                                                           (or cascade0 []))
                                                          "\nEvery fragment must cite at least one pattern in pattern_refs as {\"id\": \"<pattern id under /home/joe/code/futon3/library>\", \"rationale\": \"why it fits\"}; a fragment with no citation cannot form a cascade."
                                                          (when (zero? (:exit validated0))
                                                            (str "\nExact offset diagnostics: "
                                                                 (json/generate-string
                                                                  (offset-diagnostics request analysis)))))
                                                     :prior-analysis analysis)))
                        corrected-analysis (when (= :validated (:status correction))
                                             (:value correction))
                        validated (if corrected-analysis
                                    (validate corrected-analysis)
                                    validated0)
                        exchange (if corrected-analysis correction exchange)
                        output @last-output
                        legacy-output (io/file (str (.getPath validation-request)
                                                    ".analysis.json"))]
                    (if-not (and (zero? (:exit validated)) (.isFile output))
                      (refused need :selected-target-reading-validation-refused
                               {:exit (:exit validated) :stderr (:err validated)})
                      (let [canonical (json/parse-string (slurp output) true)
                            ;; Refresh the legacy single-name output for
                            ;; existing tooling; the attempt-scoped sibling
                            ;; above is the authoritative record.
                            _ (spit legacy-output (slurp output))
                            publication
                            (registry/publish!
                             (or target-reading-root registry/default-root)
                             {:target-id (:target need)
                              :source-path (:source-path need)
                              :excerpt-digest (:excerpt-digest need)
                              :source-file-digest (get-in request [:task :content_sha256])
                              :request request :analysis canonical
                              :validator-version "session-turn-analysis-v2"})]
                        (if (= :refused (:status publication))
                          (refused need :selected-target-reading-publication-refused
                                   publication)
                          {:schema :wm/selected-target-reading-request-v1
                           :status :published :published true
                           :target (:target need) :author author
                           :source-path (:source-path need)
                           :source-file-digest (:source-file-digest publication)
                           :excerpt-digest (:excerpt-digest need)
                           :request-file (.getPath request-file)
                           :request-sha256 request-sha
                           :analysis-digest (:analysis-digest publication)
                           :job-id (:job-id exchange)
                           :validation (:validation publication)})))))))))))))
