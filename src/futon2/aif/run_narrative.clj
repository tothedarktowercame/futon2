(ns futon2.aif.run-narrative
  "Read retained run evidence and write only the requested Markdown output.
  CLI: clojure -M -m futon2.aif.run-narrative <run-id> [out.md].
  FUTON2_NARRATIVE_ROOT selects the evidence checkout (default current directory)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.cascade-plan :as plan]))

(def checkpoint-order [:time-step :selection :construction :dispatch :build :adjudication :closed])

(defn- read-one [file]
  (when (.isFile (io/file file))
    (with-open [r (java.io.PushbackReader. (io/reader file))]
      (let [x (edn/read {:eof ::eof} r)]
        (when (or (= ::eof x) (not= ::eof (edn/read {:eof ::eof} r)))
          (throw (ex-info "Expected one retained EDN record" {:path (str file)})))
        x))))

(defn- matching-forms [file pred]
  (if-not (.isFile (io/file file)) []
    (with-open [r (java.io.PushbackReader. (io/reader file))]
      (loop [i 1 matches []]
        (let [x (edn/read {:eof ::eof} r)]
          (if (= ::eof x) matches
              (recur (inc i) (cond-> matches (pred x) (conj {:form i :value x})))))))))

(defn- unique-record [records kind]
  (when (> (count records) 1)
    (throw (ex-info "Ambiguous retained run evidence" {:kind kind :matches (count records)})))
  (first records))

(defn load-run
  "Resolve by exact run/trace/click identity; never select the latest attempt."
  [root run-id]
  (when-not (re-matches #"[A-Za-z0-9_-]+" run-id)
    (throw (ex-info "Invalid run id" {:run-id run-id})))
  (let [record-path (str (io/file root "data/wm-runs" (str "tick-run-record-" run-id ".edn")))
        record (read-one record-path)
        _ (when-not (= run-id (:run/id record))
            (throw (ex-info "Run record absent or identity mismatch" {:path record-path})))
        date (re-find #"^\d{4}-\d{2}-\d{2}" run-id)
        trace-path (str (io/file root "data/wm-trace" (str "wm-trace-" date ".edn")))
        trace-row (unique-record (matching-forms trace-path #(= run-id (:run/id %))) :trace)
        trace (:value trace-row)
        binding-path (str (io/file root "../futon3c/data/wm-click-run-bindings"
                                  (str "click-run-binding-" (:click/id record) ".edn")))
        binding (read-one binding-path)
        _ (when (and binding (not= run-id (get-in binding [:run-id-observation :value])))
            (throw (ex-info "Click binding identifies a different run" {:path binding-path})))
        cohort-id (or (get-in record [:cohort-attempt :cohort/id])
                      (get-in trace [:cohort-attempt :cohort/id])
                      (get-in record [:execution-cohort :cohort-id]))
        attempt (or (get-in record [:cohort-attempt :attempt/id])
                    (get-in trace [:cohort-attempt :attempt/id])
                    (:runner-attempt/id record) (:attempt/id binding)
                    (some->> (get-in record [:runner-execution/identity :id])
                             (re-find #"--(attempt-[0-9]+)$") second))
        starts (when (and cohort-id attempt)
                 (for [f (file-seq (io/file root "data"))
                       :when (and (= "001-time-step.edn" (.getName f))
                                  (= attempt (.getName (.getParentFile f)))
                                  (= (name cohort-id) (.getName (.getParentFile (.getParentFile f)))))
                       :let [x (read-one f)] :when (= cohort-id (:cohort/id x))]
                   {:file f :record x}))
        authority (or (get-in record [:runner-execution/provenance :authority-id])
                      (some->> (get-in record [:runner-execution/identity :id])
                               (re-find #"^ea1-([0-9a-f]{64})--") second))
        starts (if authority (filter #(= authority (get-in % [:record :payload :judgment :execution-authority :authority-id])) starts) starts)
        start (unique-record starts :cohort-attempt)
        dir (some-> start :file .getParentFile)
        checkpoints (into {} (map-indexed
                               (fn [i stage]
                                 (let [path (when dir (str (io/file dir (format "%03d-%s.edn" (inc i) (name stage)))))
                                       event (when path (read-one path))]
                                   (when (and event (not (and (= stage (:checkpoint/type event))
                                                              (= cohort-id (:cohort/id event))
                                                              (= attempt (:attempt/id event)))))
                                     (throw (ex-info "Checkpoint type mismatch" {:path path})))
                                   [stage {:path path :event event :judgment (get-in event [:payload :judgment])}]))
                               checkpoint-order))
        opportunity (get-in checkpoints [:time-step :judgment :opportunity-id])
        phase-path (str (io/file root "data/wm-full-loop-phases.edn.log"))
        phases (matching-forms phase-path #(or (= run-id (:run/id %))
                                               (and opportunity (= opportunity (:opportunity-id %)))))]
    {:root root :run-id run-id :record record :record-path record-path
     :trace trace :trace-path trace-path :trace-form (:form trace-row)
     :binding-path binding-path :binding binding :checkpoints checkpoints
     :phase-path phase-path :phases phases}))

(defn- shown [x] (if (nil? x) "not recorded" (if (string? x) x (pr-str x))))
(defn- cite [path key-path]
  (str "- Source: `" path "` — `" (pr-str key-path) "`.\n"))
(defn- judgment [bundle stage] (get-in bundle [:checkpoints stage :judgment]))
(defn- decision-source [b]
  (cond
    (:controller-decision (judgment b :selection))
    [(get-in b [:checkpoints :selection :path]) [:payload :judgment :controller-decision]]
    (get-in b [:trace :decision]) [(:trace-path b) [:form (:trace-form b) :decision]]
    :else [(:record-path b) [:decision]]))

(defn- decision [b] (or (:controller-decision (judgment b :selection))
                        (get-in b [:trace :decision]) (get-in b [:record :decision])))

(defn candidate-rows [b]
  (let [d (decision b) certificate (get-in d [:selection-certificate :candidates])
        posterior (get-in d [:selection-law :posterior])
        rows (if (seq certificate)
               (map (fn [c] {:target (get-in c [:id :target]) :cascade-id (or (get-in c [:id :cascade-id]) (get-in c [:id :id]))
                             :G (:g c) :posterior (get posterior (:id c))
                             :habit (:habit c) :F (:f c)}) certificate)
               ;; Fix-4 rows declare :G explicitly. Never reinterpret legacy :G-efe.
               (filter #(contains? % :G) (:ranked-candidates (judgment b :selection))))]
    (sort-by (juxt #(if (number? (:posterior %)) (- (:posterior %)) Double/POSITIVE_INFINITY)
                   #(str (:target %)) #(str (:cascade-id %))) rows)))

(defn- construction [b]
  (let [j (judgment b :construction) a (or (get-in j [:cascade :selected-action])
                                           (:selected-action (judgment b :selection)))]
    (merge (:cascade j) {:precedence (:precedence a) :selected-action a
                         :interpretation-receipts (:interpretation-receipts a)
                         :wiring (:wiring j) :fold-output (:fold-output j)
                         :shown (:patterns j)})))

(defn- jobs [b]
  (->> checkpoint-order (mapcat #(get-in b [:checkpoints % :judgment :job-texts]))
       (reduce (fn [m row] (assoc m (:job-id row) row)) (sorted-map)) vals))

(defn- retained-text [b ref]
  (when (= :present (:status ref))
    (let [f (io/file (:path ref)) f (if (.isAbsolute f) f (io/file (:root b) (:path ref)))]
      (when (.isFile f)
        (let [bytes (java.nio.file.Files/readAllBytes (.toPath f))
              sha (apply str (map #(format "%02x" (bit-and 255 %))
                                  (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)))]
          (when-not (= sha (:sha256 ref))
            (throw (ex-info "Retained text digest mismatch" {:path (str f)})))
          (String. bytes java.nio.charset.StandardCharsets/UTF_8))))))

(defn- quote-text [s] (str/join "\n" (map #(str "> " %) (str/split-lines s))))

(defn- plan-quote [b]
  (let [author-id (:job-id (judgment b :dispatch))
        job (some #(when (= author-id (:job-id %)) %) (jobs b))
        prompt (retained-text b (:prompt job))
        section (when prompt
                  (second (re-find #"(?s)PATTERN CASCADE:\s*(.*?)(?:\n(?:CONSTRUCTION CONTRACT:|Name in your reply|STOP-THE-LINE|Requirements:)|\z)" prompt)))]
    (if section
      (str "The following build plan is quoted from the retained author prompt.\n\n"
           (quote-text section) "\n\n" (cite (get-in job [:prompt :path]) [:text "PATTERN CASCADE"]))
      (str "Not recorded in this run: " (if prompt "PATTERN CASCADE section in retained prompt"
                                                     "prompt text (retention began e9ee4a73)")
           ". The following plan is a reconstruction from the saved construction, not a transcript of what the author saw.\n\n"
           (quote-text (plan/cascade-plan-text (construction b))) "\n"))))

(defn- timing [b stage]
  (let [names (case stage :time-step #{:agent-readiness :code-state :substrate-preflight}
                    :selection #{:selection} :construction #{:construction}
                    :dispatch #{:author-dispatch} :build #{:author-wait :build-resolution}
                    :adjudication #{:reviewer-wait :grounding} :closed #{:opportunity})
        rows (filter #(and (names (get-in % [:value :phase]))
                           (number? (get-in % [:value :duration-ms]))) (:phases b))]
    (if (seq rows)
      (str "\nRecorded phase durations: "
           (str/join "; " (map #(str (name (get-in % [:value :phase])) " "
                                     (get-in % [:value :duration-ms]) " ms") rows)) ".\n"
           (apply str (map #(cite (:phase-path b) [:form (:form %) :duration-ms]) rows)))
      "\nNot recorded in this run: matching phase duration.\n")))

(defn- selection-text [b]
  (let [d (decision b) rows (candidate-rows b)
        winner (or (:selected-action (judgment b :selection)) (:action d)
                   (get-in d [:selection-law :per-policy-argmax :action]))
        cp (get-in b [:trace :cascade-problems])
        refusals (:refusals cp) policy (get-in d [:selection-law :policy-comparison])
        action (get-in d [:selection-law :action-comparison])
        supply (or (:proposal-supply d) (get-in d [:selection-certificate :proposal-supply])
                   (:proposal-supply cp))
        gs (filter number? (map :G rows))]
    (str "The machine scored " (if (seq rows) (count rows) "an unrecorded number of") " admitted cascades; enumeration completeness is " (shown (get-in d [:enumeration-completeness :verdict])) ". "
         (if (and (coll? (:problems cp)) (coll? refusals))
           (str (+ (count (:problems cp)) (count refusals)) " targets were considered; "
                     (count refusals) " were refused ("
                     (str/join ", " (map (fn [[k n]] (str n " " (shown (:kind k))
                                                                     (when (:missing k) (str " missing " (shown (:missing k))))))
                                          (sort-by (comp pr-str key) (frequencies (map #(select-keys % [:kind :missing]) refusals))))) ")"
                     (if supply (str "; proposal supply retained " (if (contains? supply :proposals) (count (:proposals supply)) "not recorded") " proposals and "
                                     (if (contains? supply :admissions) (count (:admissions supply)) "not recorded") " admissions. ") "; proposal supply: not recorded. "))
             "Not recorded in this run: target admission counts. ")
         "It chose " (shown (:target winner)) " (cascade " (shown (:id winner)) "); the checkpoint's selected-mission field is "
         (shown (:selected-mission (judgment b :selection))) ". "
         (if (and (seq gs) (= (count rows) (count gs))
                  (every? #(Double/isFinite (double %)) gs))
           (str "The computed full G spread is " (- (apply max gs) (apply min gs)) " nats. ")
             "Not recorded in this run: comparable G values. ")
         (if policy (str "The policy comparison records decided-by " (shown (:decided-by policy))
                         ", near-tie " (shown (:near-tie? policy)) ", threshold " (shown (:near-tie-threshold policy)) ". ")
             "Not recorded in this run: policy comparison and near-tie threshold; no near-tie verdict is inferred. ")
         (if action (str "The action comparison records decided-by " (shown (:decided-by action)) ".\n")
             "Not recorded in this run: action comparison.\n")
         "\n| Target | Cascade | G (nats) | Posterior | Habit | F consumed |\n|---|---|---:|---:|---:|---:|\n"
         (apply str (for [r rows] (str "| " (str/join " | " (map #(shown (get r %)) [:target :cascade-id :G :posterior :habit :F])) " |\n")))
         "\n[Selection plot placeholder — slice 14b.]\n"
         (if (seq (get-in d [:selection-certificate :candidates]))
           "\nG and posterior are read separately from the certificate and selection law; legacy checkpoint G-efe is not treated as G.\n"
           "\nG and posterior use explicit ranked-candidates fields; legacy G-efe is not treated as G.\n"))))

(defn- body [b stage]
  (let [j (judgment b stage)]
    (case stage
      :time-step (str "The run began at " (shown (get-in b [:record :startedAt]))
                      " with trigger " (shown (:trigger j)) ". Its opportunity is " (shown (:opportunity-id j))
                      ", and the recorded semantic epoch is " (shown (:semantic-epoch j)) ".\n")
      :selection (selection-text b)
      :construction
      (let [c (construction b) patterns (:precedence c) wires (get-in c [:wiring :wires])
            receipt (get-in c [:selected-action :construction-receipt])]
        (str "The cascade contains " (if (some? patterns) (count patterns) "an unrecorded number of") " pattern(s), in precedence order: "
             (str/join ", " (map #(shown (if (map? %) (:id %) %)) patterns)) ". "
             "Its recorded construction method is " (shown (:kind receipt))
             "; retrieval method: "
             (shown (or (:retrieval-method c) (:retrieval receipt)
                        (get-in c [:selected-action :retrieval]))) " for this selected cascade. "
             (if (and (= 1 (count patterns)) (= [] wires)) "Its observed structure is a singleton, with no wires. "
                 (str "There are " (if (some? wires) (count wires) "an unrecorded number of") " recorded wires; no stronger structure is inferred. "))
             (if (= [] (:semilattice c)) "The semilattice field is not computed (literal []).\n\n"
                 (str "The semilattice field records " (shown (:semilattice c)) "; this is not a proof of structure.\n\n"))
             (plan-quote b)))
      :dispatch (str "The runner dispatched agent " (shown (:agent j)) " as job " (shown (:job-id j)) ". "
                     "Its prompt reference is " (shown (:prompt-ref j)) ", with availability recorded as " (shown (:availability j)) ".\n")
      :build (str "The author produced commit(s) " (shown (:commits j)) ". "
                  "The recorded changed artifacts are " (shown (:artifacts j)) ". "
                  "Author execution evidence records " (shown (get-in j [:validation :author :command-events]))
                  " command events; reviewer execution records " (shown (get-in j [:validation :reviewer :command-events])) ".\n"
                  (if-let [review (get-in j [:validation :review-text])]
                    (str "\nRetained review statement:\n\n" (quote-text review) "\n")
                    "\nNot recorded in this run: review text in build checkpoint.\n")
                  (apply str (for [job (jobs b) :let [reply (retained-text b (:reply job))]]
                               (if reply
                                 (str "\nRetained reply for " (shown (:role job)) " job " (:job-id job) ":\n\n"
                                      (quote-text reply) "\n" (cite (get-in job [:reply :path]) [:text]))
                                 (str "\nNot recorded in this run: retained reply for job " (:job-id job) ".\n"))))
                  (when-not (seq (jobs b)) "\nNot recorded in this run: job-texts transcript references.\n"))
      :adjudication (str "The recorded build match says review-approved " (shown (get-in j [:build-match :review-approved?]))
                         " for commit " (shown (get-in j [:build-match :commit])) ". "
                         "Grounding returned implementation " (shown (get-in j [:witness :implementation-id]))
                         " and discharge " (shown (get-in j [:witness :discharge-id]))
                         "; its dial-moved claim is " (shown (get-in j [:dial :moved?])) ".\n")
      :closed (str "The attempt closed with outcome " (shown (:outcome j)) " after " (shown (:duration-ms j)) " ms. "
                   "The recorded entity state at close has status " (shown (get-in j [:entity-state-at-close :status]))
                   " and reason " (shown (get-in j [:entity-state-at-close :reason])) ". "
                   "Its Morning Brief reference is " (shown (:morning-brief-ref j)) ".\n"))))

(defn narrative-text [b]
  (str "# Run " (:run-id b) "\n\nA narrative of retained evidence; no selection, observation, or actuation was rerun.\n\n"
       (apply str
              (for [stage checkpoint-order
                    :let [{:keys [path judgment]} (get-in b [:checkpoints stage])]]
                (str "## " (name stage) "\n\n"
                     (if judgment (body b stage)
                         "Not recorded in this run: checkpoint. The remaining evidence does not establish this stage's outcome.\n")
                     "\nCited facts:\n"
                     (cite (or path (:record-path b)) (if path [:payload :judgment] [:cohort-attempt]))
                     (when (= stage :time-step) (cite (:record-path b) [:startedAt]))
                     (when (= stage :selection)
                       (let [[p k] (decision-source b)]
                         (str (cite p (conj k :selection-certificate :candidates))
                            (cite p (conj k :selection-law))
                            (cite (:trace-path b) [:form (:trace-form b) :cascade-problems])
                            "\nDocstring correspondence, not runtime Lean execution: `src/futon2/aif/cascade_selection.clj` namespace docstring cites `PolicySelection.lean` at `a434947c63`, `selectionPosterior`.\n")))
                     (timing b stage) "\n")))))

(defn render-run! [root run-id output]
  (when-not (str/ends-with? (str output) ".md")
    (throw (ex-info "Narrative output must be a Markdown path" {:output output})))
  (let [bundle (load-run root run-id)
        paths (concat [(:record-path bundle) (:trace-path bundle) (:phase-path bundle) (:binding-path bundle)]
                      (keep :path (vals (:checkpoints bundle)))
                      (for [job (jobs bundle) kind [:prompt :reply]
                            :let [p (get-in job [kind :path])] :when p]
                        (if (.isAbsolute (io/file p)) p (str (io/file root p)))))
        _ (when (some #(= (.getCanonicalPath (io/file output))
                          (.getCanonicalPath (io/file %))) paths)
            (throw (ex-info "Output would overwrite retained evidence" {:output output})))
        content (narrative-text bundle)]
    ;; Parent must already exist. No caches, directories, stores, or sidecars.
    (spit output content)
    (str output)))

(defn -main [& [run-id output & extra]]
  (when (or (nil? run-id) (seq extra))
    (throw (ex-info "Usage: run-narrative <run-id> [out.md]" {})))
  (println (render-run! (or (System/getenv "FUTON2_NARRATIVE_ROOT") ".")
                        run-id (or output (str run-id "-narrative.md")))))
