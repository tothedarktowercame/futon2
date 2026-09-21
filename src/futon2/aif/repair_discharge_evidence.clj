(ns futon2.aif.repair-discharge-evidence
  "Discharge evidence is an observation by an authored, reviewed evaluator,
   never a limb label or a substrate insertion. Evaluators are registered code;
   admissions pin their source closure and review. This namespace does not load
   evaluator code, invoke a shell command from a finding, or write the store."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as digest]
            [futon2.aif.task-execution-evidence :as execution])
  (:import [java.io PushbackReader StringReader]
           [java.nio.file Files]))

(defn refuse! [reason data]
  (throw (ex-info "Repair discharge evidence refused"
                  (assoc data :repair-discharge/refusal reason))))

(defn require! [predicate reason data]
  (when-not predicate (refuse! reason data)))

(defn bytes-at [path] (Files/readAllBytes (.toPath (io/file path))))
(defn sha256 [bytes] (digest/sha256 bytes))
(defn text-bytes [s] (.getBytes ^String s "UTF-8"))

(defn read-one [text]
  (with-open [reader (PushbackReader. (StringReader. text))]
    (let [eof (Object.) value (edn/read {:eof eof} reader)]
      (require! (and (not (identical? eof value))
                     (identical? eof (edn/read {:eof eof} reader)))
                :not-one-edn-form {})
      value)))

(defn canonical-text
  "Stable EDN bytes, including maps nested in sets. Reject unreadable values."
  [value]
  (letfn [(stable [x]
            (cond
              (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                             (map (fn [[k v]] [(stable k) (stable v)])) x)
              (set? x) (into (sorted-set-by #(compare (pr-str %1) (pr-str %2)))
                             (map stable) x)
              (vector? x) (mapv stable x)
              (seq? x) (apply list (map stable x))
              :else x))]
    (let [text (str (pr-str (stable value)) "\n")]
      (require! (= value (read-one text)) :not-round-trippable {})
      text)))

(defn safe-id! [id]
  (require! (and (string? id) (re-matches #"[A-Za-z0-9][A-Za-z0-9._-]*" id)
                 (not (#{"." ".."} id))) :unsafe-repair-id {:repair/id id})
  id)

(defn git! [repo & args]
  (let [result (apply shell/sh "git" "-C" repo args)]
    (require! (zero? (:exit result)) :git-evidence-unavailable
              {:repo repo :arguments (vec args) :error (:err result)})
    (:out result)))

(defn snapshot [path]
  (let [bytes (bytes-at path) text (String. bytes "UTF-8")]
    ;; Do not silently replace malformed UTF-8 while embedding original bytes.
    (require! (java.util.Arrays/equals bytes (text-bytes text)) :invalid-utf8 {:path (str path)})
    {:path (.getCanonicalPath (io/file path)) :sha256 (sha256 bytes)
     :edn-text text :value (read-one text)}))

(defn review! [files author reviewer job]
  (let [gate (assoc (execution/independent-review-evidence files job) :reviewer reviewer)]
    (require! (and (string? author) (not (str/blank? author))
                   (string? reviewer) (not (str/blank? reviewer)) (not= author reviewer)
                   (= reviewer (:agent-id job)) (:valid? gate))
              :review-not-approved {:reviewer reviewer :gate gate})
    gate))

(defn admitted-evaluator!
  "Resolve a configured admission at its immutable git revision. REGISTERED
   contains compiled implementations with captured loaded-source digests;
   callers cannot use a finding to name executable code. READ-JOB fetches the
   actual review job. Its prompt must bind this exact admission digest."
  [{:keys [repo sha path]} registered finding read-job]
  (require! (and repo sha path) :no-repair-evaluator {:failure-kind (:failure-kind finding)})
  (let [commit (str/trim (git! repo "rev-parse" "--verify" (str sha "^{commit}")))
        text (git! repo "show" (str commit ":" path))
        admission (read-one text)
        {:keys [evaluator failure-kind author reviewer review-job source-sha source-paths]} admission
        implementation (get registered evaluator)
        job (when (string? review-job) (read-job review-job))
        ;; The approval binds the admission body, excluding the review id to
        ;; avoid a job/admission self-reference. No prose verdict substitutes
        ;; for the executed independent review gate.
        binding (digest/value-digest (dissoc admission :review-job))
        prompts (str/join "\n" (keep :text (filter #(= "prompt" (:type %)) (:events job))))]
    (require! (= :wm/repair-evaluator-admission-v1 (:schema admission)) :evaluator-admission-invalid {})
    (require! (= failure-kind (:failure-kind finding)) :evaluator-kind-mismatch {})
    (require! (and (string? source-sha) (re-matches #"[0-9a-f]{40}" source-sha)
                   (= source-sha (str/trim (git! repo "rev-parse" "--verify" "--end-of-options"
                                               (str source-sha "^{commit}")))))
              :evaluator-source-not-pinned {})
    (require! (and implementation (fn? (:evaluate implementation))) :no-repair-evaluator {:evaluator evaluator})
    (require! (and (seq source-paths) (= (set source-paths) (set (keys (:loaded-source implementation)))))
              :evaluator-source-closure-mismatch {})
    (doseq [source-path source-paths]
      (let [expected (sha256 (text-bytes (git! repo "show" (str source-sha ":" source-path))))]
        (require! (= expected (get-in implementation [:loaded-source source-path]))
                  :evaluator-source-mismatch {:path source-path})))
    (require! (= review-job (:job-id job)) :evaluator-review-job-mismatch {})
    (require! (str/includes? prompts (str "REPAIR_EVALUATOR_ADMISSION_SHA256: " binding))
              :evaluator-review-binding-missing {})
    {:admission admission :admission-text text :admission-sha256 (sha256 (text-bytes text))
     :locator {:repo repo :sha commit :path path}
     :review-job job :review-evidence (review! source-paths author reviewer job)
     :implementation implementation}))

(defn observe!
  "Run the admitted evaluator on the exact finding and actual closed attempt.
   The evaluator owns per-kind semantic judgment; common checks bind its two
   observations to this invocation. Unknown/missing evidence never passes."
  [evaluator finding finding-pin close artifact]
  (let [request {:admission (:admission evaluator) :finding finding :finding-pin finding-pin :close close :artifact artifact}
        result ((get-in evaluator [:implementation :evaluate]) request)
        identity {:repair/id (:repair/id finding) :finding-sha256 (:sha256 finding-pin)
                  :attempt/id (:attempt/id close) :run/id (:run/id close)
                  :artifact-commit (:commit artifact)}]
    (require! (= identity (:identity result)) :repair-observation-binding-mismatch {})
    (require! (and (= :wm/repair-observation-v1 (:schema result))
                   (true? (get-in result [:recorded-failure :reproduced-before?]))
                   (false? (get-in result [:recorded-failure :reproduced-after?]))
                   (seq (get-in result [:recorded-failure :evidence]))
                   (true? (get-in result [:successor :production-shaped?]))
                   (true? (get-in result [:successor :passed?]))
                   (seq (get-in result [:successor :evidence])))
              :repair-observation-not-established {:observation result})
    ;; Serialize now, before a store writer can receive the witness.
    (canonical-text result)
    {:resolved? true :dial-moved? true :repair/id (:repair/id finding)
     :observation result
     :evaluator (dissoc evaluator :implementation)}))
