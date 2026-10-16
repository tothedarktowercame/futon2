(ns futon2.aif.selected-target-reading
  "Same-click repair for an outer-selected target whose current HEAD has no
  validated reading.  The outer receipt is input, never recomputed here."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
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

(defn selected-need
  "Return the exact outer-selected target/source/current-digest need, or nil.
  A failure belonging to another target is never eligible."
  [judgement]
  (let [target (get-in judgement [:outer-task-selection :chosen :id])
        families (get-in judgement [:decision :selection-certificate
                                    :target-policy-field :families])
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
  [{:keys [request]}]
  (str "Produce one session_turn_analysis analysis for this immutable request. "
       "Use exact Unicode-codepoint offsets and only actual patterns under "
       "/home/joe/code/futon3/library. Do not publish it and do not change repositories. "
       "Reply with exactly one JSON object in the analysis-input shape accepted by "
       "/home/joe/code/futon3c/scripts/session_turn_analysis.py complete.\n\n"
       (json/generate-string request {:pretty true})))

(defn- answer-analysis [answer]
  (cond
    (map? (:analysis answer)) (:analysis answer)
    (map? answer) (when (and (contains? answer :sentences)
                             (contains? answer :labeller)) answer)
    :else nil))

(defn- answer-text [answer]
  (when (and (= "done" (:state answer)) (string? (:text answer)))
    (try (json/parse-string (:text answer) true)
         (catch Exception _ nil))))

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
                pinned? (and (= (:target need) (get-in request [:task :target_id]))
                             (= (.getCanonicalPath (io/file (:source-path need)))
                                (get-in request [:task :file_path]))
                             (= (:excerpt-digest need)
                                (get-in request [:task :excerpt_sha256]))
                             (= (file-digest (:source-path need))
                                (get-in request [:task :content_sha256])))
                request-sha (file-digest request-file)]
            (if-not pinned?
              (refused need :selected-target-reading-pin-mismatch
                       {:request-file (.getPath request-file)})
              (let [issued {:target (:target need) :kind :selected-target-reading
                            :request-id request-sha :request request :author author}
                    answer (try (answer-fn issued)
                                (catch Throwable e
                                  {:state :failed :reason :answer-threw
                                   :message (ex-message e)}))
                    analysis (or (answer-analysis answer) (answer-text answer))]
                (if-not analysis
                  (refused need
                           (if (= :designated-author-unavailable (:reason answer))
                             :designated-author-unavailable
                             :selected-target-reading-answer-invalid)
                           {:author author :contacted? (not= :not-contacted (:state answer))
                            :answer (select-keys answer [:state :reason :job-id :message])})
                  (let [analysis-input (io/file (.getParentFile request-file)
                                                "analysis-input.json")
                        validation-request (io/file (.getParentFile request-file)
                                                    "validation-request.json")
                        validated (do
                                    (spit analysis-input
                                          (json/generate-string analysis {:pretty true}))
                                    (io/copy request-file validation-request)
                                    (shell/sh "python3" analysis-validator "complete"
                                              (.getPath validation-request)
                                              (.getPath analysis-input)))
                        output (io/file (str (.getPath validation-request)
                                             ".analysis.json"))]
                    (if-not (and (zero? (:exit validated)) (.isFile output))
                      (refused need :selected-target-reading-validation-refused
                               {:exit (:exit validated) :stderr (:err validated)})
                      (let [canonical (json/parse-string (slurp output) true)
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
                           :job-id (:job-id answer)
                           :validation (:validation publication)})))))))))))))
