(ns futon2.aif.run-narrative
  "Read retained run evidence and write Markdown with two adjacent SVG figures.
  CLI: clojure -M -m futon2.aif.run-narrative <run-id> [out.md].
  FUTON2_NARRATIVE_ROOT selects the evidence checkout (default current directory)."
  (:require [futon2.aif.load-identity :as load-identity]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.cascade-plan :as plan]
            [futon2.aif.cascade-structure :as structure]
            [futon2.aif.route-attestation :as route-attestation]
            [futon2.aif.narrative-figures :as figures]))

(load-identity/register! *ns* *file*)

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
       (reduce (fn [rows row]
                 (conj (filterv #(not= (:job-id row) (:job-id %)) rows) row)) [])))

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

(defn- perceive-text [b]
  (let [t (:trace b) observation (:observation t) pre (:mu-pre t) post (:mu-post t)
        row-ids (into (set (keys pre)) (keys post))
        gaps (get-in t [:free-energy :per-channel])
        sampled (take 4 (sort-by (fn [[k _]] [(- (double (or (get-in gaps [k :gap]) 0))) (str k)]) observation))]
    (str "\n"
         (if (contains? t :mode)
           (str "The machine perceived in mode " (shown (:mode t)) ", with stop-the-line "
                (if (= :stop-the-line (:mode t)) "active" "inactive according to the mode") ". ")
           "Not recorded in this run: mode and stop-the-line status. ")
         (if-let [energy (:free-energy t)]
           (str "Its recorded free-energy account is "
                (if (map? energy)
                  (str/join ", " (for [k [:controller-score :preference-gap-score :coverage-uncertainty-pressure]]
                                    (str (name k) " " (shown (get energy k)))))
                  (shown energy))
                (when (seq (:avoided-active energy))
                  (str "; active avoided channels are " (str/join ", " (map shown (:avoided-active energy))))) ". ")
           "Not recorded in this run: free energy. ")
         (if (map? observation)
           (str "It received " (count observation) " observation channels; "
                (if (seq gaps) "the largest recorded preference gaps" "a sample ordered by channel name")
                " give " (str/join ", " (for [[k v] sampled]
                                           (str (shown k) " = " (shown v)
                                                (when-let [gap (get-in gaps [k :gap])] (str " (gap " gap ")"))))) ". ")
           "Not recorded in this run: observation channels. ")
         (if (and (map? pre) (map? post))
           (str (count (filter #(not= (find pre %) (find post %)) row-ids)) " of " (count row-ids)
                " belief rows changed from mu-pre to mu-post (including added or removed rows). ")
           "Not recorded in this run: paired mu-pre and mu-post belief rows. ")
         (if (contains? t :wm/route)
           (str "The recorded route is " (str/join " → " (map #(shown (:node %)) (:wm/route t))) ".\n")
           "Not recorded in this run: route.\n")
         "\n"
         (apply str (for [k [:mode :free-energy :observation :mu-pre :mu-post :wm/route]]
                      (cite (:trace-path b) [:form (:trace-form b) k]))))))

(defn- final-review [text]
  (when text
    (->> (str/split (str/replace text #"\r\n?" "\n") #"(?m)(?=^FULL_LOOP_REVIEW:)")
         (filter #(str/starts-with? % "FULL_LOOP_REVIEW:")) last
         (#(when % (str/trim %))))))

(defn- review-text [b]
  (let [checkpoint {:text (get-in (judgment b :build) [:validation :review-text])
                    :path (get-in b [:checkpoints :build :path])
                    :keys [:payload :judgment :validation :review-text]}
        replies (for [job (jobs b)]
                  {:text (retained-text b (:reply job)) :path (get-in job [:reply :path])
                   :keys [:text] :role (:role job) :job-id (:job-id job)})
        reviews (filter :final (map #(assoc % :final (final-review (:text %))) (cons checkpoint replies)))
        chosen (or (final-review (:text checkpoint)) (:final (last reviews)))
        ;; Deduplicate the final block itself, regardless of transport preamble or job id.
        sources (distinct (map #(select-keys % [:path :keys]) (filter #(= chosen (:final %)) reviews)))]
    (str (if chosen
           (str "\nFinal review statement:\n\n" (quote-text chosen) "\n\nFull review text (including any earlier discussion) lives in:\n\n"
                (apply str (for [{:keys [path keys]} sources] (cite path keys))))
           "\nNot recorded in this run: final FULL_LOOP_REVIEW statement.\n")
         (apply str (for [{:keys [text path keys role job-id]} replies]
                      (cond
                        (and text (or (= :reviewer role) (final-review text)))
                        (str "\nFull review text for job " job-id " is retained at the following source.\n\n" (cite path keys))
                        text (str "\nRetained reply for " (shown role) " job " job-id ":\n\n"
                                  (quote-text text) "\n" (cite path keys))
                        :else (str "\nNot recorded in this run: retained reply for job " job-id ".\n"))))
         (when-not (seq (jobs b)) "\nNot recorded in this run: job-texts transcript references.\n"))))

(defn- selected-action [b]
  (or (:selected-action (judgment b :selection)) (:action (decision b))))

(defn- outcome-receipt [b]
  (if (contains? (judgment b :closed) :token-outcome-comparison)
    [(:token-outcome-comparison (judgment b :closed))
     (get-in b [:checkpoints :closed :path]) [:payload :judgment :token-outcome-comparison]]
    [(get-in b [:record :token-outcome-comparison]) (:record-path b) [:token-outcome-comparison]]))

(defn- d-task-source [b]
  (or (get-in b [:record :d-task-enactment :source])
      (get-in (judgment b :closed) [:d-task-enactment :source])))

(defn- historical-outcomes [b]
  (let [ref (d-task-source b)
        text (when (:path ref) (retained-text b (assoc ref :status :present)))
        d (when text
            (with-open [r (java.io.PushbackReader. (java.io.StringReader. text))]
              (let [value (edn/read {:eof ::eof} r)]
                (when (or (= ::eof value) (not= ::eof (edn/read {:eof ::eof} r)))
                  (throw (ex-info "Expected one retained D-task record" {:path (:path ref)})))
                value)))
        action (selected-action b) target (:target action)
        domains (get-in (decision b) [:selection-certificate :token-belief-stage :domain-inputs])
        wanted (some #(when (= target (:target %)) (get-in % [:declaration :want])) domains)
        produces (into #{} (mapcat :produces (:precedence action)))
        artifact (get-in d [:revision-pair :after])
        rows (:after-token-evidence d)]
    (cond
      (nil? d) {:missing "D-task record" :source ref}
      (not= (:run-id b) (get-in d [:dispatch :occurrence :run/id]))
      (throw (ex-info "D-task record identifies a different run" {:path (:path ref)}))
      (not= action (get-in d [:dispatch :occurrence :action/value]))
      (throw (ex-info "D-task record identifies a different selected action" {:path (:path ref)}))
      (nil? wanted) {:missing "selected target's wanted tokens" :source ref}
      :else
      {:source ref :artifact artifact :measurements rows
       :tokens (for [want (sort-by pr-str wanted)
                     :let [token [target want] measurements (filter #(= token (:token %)) rows)
                           measurement (first measurements)
                           result (:result measurement)
                           valid? (and (= 1 (count measurements)) (boolean? (:observed result))
                                       artifact (= artifact (get-in result [:evidence :resolved-sha])))]]
                 {:token token :predicted (contains? produces token)
                  :observed (if valid? (:observed result)
                                {:status :missing :kind :missing-ambiguous-or-unpinned-measurement})
                  :measurement measurement})})))

(defn- outcome-text [b]
  (let [[receipt path keys] (outcome-receipt b)
        old (when-not receipt (historical-outcomes b))
        target (:target (selected-action b))
        _ (when (and receipt (get-in receipt [:prediction :target])
                     (not= target (get-in receipt [:prediction :target])))
            (throw (ex-info "Token comparison identifies a different selected target" {:path path})))
        rows (if receipt (:tokens receipt) (:tokens old))
        rows (filter #(= target (first (:token %))) rows)
        misses (filter #(and (false? (:observed %))
                             (if receipt (= :predicted-not-observed (:verdict %))
                                 (true? (:predicted %)))) rows)]
    (str "\nHere, grounded change means a reviewed commit recorded in futon1b, not wanted-token completion.\n"
         (cite "src/futon2/aif/full_loop_runner.clj" ['ground-commit!])
         (if receipt
           (str "The retained token-outcome comparison receipt is preferred; its status is " (shown (:status receipt))
                ", with model prediction rule " (shown (get-in receipt [:prediction :prediction-rule])) ".\n"
                (cite path keys)
                (when-let [sha (:artifact-sha receipt)] (str "Observed at artifact commit `" sha "`.\n"))
                (when (and (= :compared (:status receipt)) (empty? rows))
                  "Not recorded in this run: selected target wanted-token comparisons.\n")
                (when-not (= :compared (:status receipt))
                  (str "Not recorded in this run: completed token comparison (" (shown (:reason receipt)) ").\n")))
           (str "Not recorded in this run: token-outcome comparison receipt. "
                (if (:missing old)
                  (str "Not recorded in this run: " (:missing old) ".\n")
                  (str "The table reconstructs model prediction from the selected cascade's declared :produces: true means produced; false means not declared as an output, not a full rollout prediction. "
                       "The D-task record retains " (count (:measurements old)) " after-build measurements ("
                       (count (filter #(true? (get-in % [:result :observed])) (:measurements old))) " true, "
                       (count (filter #(false? (get-in % [:result :observed])) (:measurements old))) " false), at artifact commit `"
                       (:artifact old) "`; these measurements alone do not establish causation.\n"))))
         (when (seq rows)
           (str "\nSelected target: " target ".\n\n"
                "| Wanted token | model prediction | Observed after build | Establishing check |\n|---|---|---|---|\n"
                (apply str (for [row rows]
                             (str "| " (shown (second (:token row))) " | " (shown (:predicted row)) " | "
                                  (shown (:observed row)) " | " (shown (get-in row [:measurement :result :check])) " |\n")))
                (apply str (for [row rows :when (not (boolean? (:observed row)))]
                             (str "\nNot recorded in this run: after-build measurement for " (shown (second (:token row))) ".\n")))
                (apply str (for [row misses]
                             (str "\nToken " (shown (second (:token row))) " was predicted true but observed false"
                                  (when (number? (:predicted row)) (str " (positive marginal support " (:predicted row) ")")) ".\n")))))
         (when-not receipt
           (let [[dp dk] (decision-source b)]
             (str (cite dp (conj dk :selection-certificate :token-belief-stage :domain-inputs))
                  (cite (get-in b [:checkpoints :selection :path]) [:payload :judgment :selected-action :precedence])
                  (cite (:record-path b) [:d-task-enactment :source])
                  (when-let [p (get-in old [:source :path])]
                    (str "D-task SHA-256: `" (get-in old [:source :sha256]) "`.\n"
                         (cite p [:dispatch :occurrence]) (cite p [:revision-pair :after])
                         (cite p [:after-token-evidence])))))))))

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

(defn- coverage-source [b k]
  (if (contains? (:record b) k)
    [(get-in b [:record k]) (:record-path b) [k]]
    [(get-in b [:trace k]) (:trace-path b) [:form (:trace-form b) k]]))

(defn- coverage-text [b]
  (let [[live] (coverage-source b :live-c-coverage)
        [holes] (coverage-source b :mission-hole-coverage)
        source (:source-tokens live) outcomes (:projected-outcome-tokens live)]
    (str (if (and (= :source-token (:unit source))
                  (= :target-qualified-outcome-token (:unit outcomes))
                  (every? number? [(:reached source) (:total source) (:count outcomes)]))
           (str "C reached " (:reached source) " of " (:total source)
                " source tokens, with " (:count outcomes) " projected outcome tokens")
           (str "not recorded in this run: live C coverage"
                (when live (str " (" (shown (or (:reason live) :counts-or-units-unavailable)) ")"))))
         "; "
         (if (every? number? [(:holes-retained holes) (:holes-projected holes)])
           (str "mission-hole census retained " (:holes-retained holes) " holes and projected " (:holes-projected holes))
           "not recorded in this run: mission-hole coverage") ".\n")))

(defn- scan-account [b]
  (let [ref (get-in b [:record :scan-report])
        content (retained-text b ref)]
    (if content
      (str "\n[Retained scan account](<" (:path ref) ">), SHA-256 `" (:sha256 ref) "`. "
           "This is the saved perceive-stage account, not a fresh scan.\n"
           (cite (:record-path b) [:scan-report]))
      (str "\nNot recorded in this run: scan account"
           (when ref (str " (" (shown (or (:reason ref) :retained-file-unavailable)) ")")) ".\n"
           (cite (:record-path b) [:scan-report])))))

(defn figure-paths
  "The three outputs share a basename and parent directory."
  [output]
  (let [stem (subs (str output) 0 (- (count (str output)) 3))]
    {:selection (str stem ".selection.svg") :cascade (str stem ".cascade.svg")}))

(defn figure-data
  "Adapt the same retained selection and outcome evidence used by the prose."
  [b]
  (let [d (decision b) a (selected-action b) c (construction b)
        [receipt] (outcome-receipt b)
        old (when-not receipt (historical-outcomes b))
        target (:target a)
        _ (when (and (get-in receipt [:prediction :target])
                     (not= target (get-in receipt [:prediction :target])))
            (throw (ex-info "Token comparison identifies a different selected target" {})))
        wanted (some #(when (= target (:target %)) (get-in % [:declaration :want]))
                     (get-in d [:selection-certificate :token-belief-stage :domain-inputs]))
        produces (into #{} (mapcat :produces (:precedence a)))
        outcomes (or (seq (if receipt (:tokens receipt) (:tokens old)))
                     (for [w wanted] {:token [target w] :predicted (if receipt
                                                      (some #(when (= [target w] (:token %)) (:predicted %))
                                                            (get-in receipt [:prediction :wanted]))
                                                      (contains? produces [target w]))
                                      :observed {:status :missing}}))
        declines (or (get-in d [:selection-certificate :dropped-candidates]) (:dropped-candidates d)
                     (get-in b [:record :dropped-candidates])
                     (get-in b [:trace :dropped-candidates])
                     (get-in b [:trace :cascade-problems :dropped-candidates]))]
    {:selection {:rows (vec (candidate-rows b)) :chosen a
                 :decided-by (get-in d [:selection-law :action-comparison :decided-by])
                 :declines (filterv #(or (:candidate %) (= :candidate-admission (:stage %))) declines)}
     :cascade {:target target :patterns (:precedence c)
               :need-edges (or (:need-edges c) (:need-edges a))
               :wires (get-in c [:wiring :wires])
               :shape (or (get-in c [:cascade-structure :shape]) (get-in c [:order-structure :shape]) (:shape c))
               :shape-caption (when (:cascade-structure c) (structure/description (:cascade-structure c)))
               :semilattice (:semilattice c)
               :prediction-source (if receipt "retained comparison receipt" "declared produces (reconstructed)")
               :outcomes (filter #(= target (first (:token %))) outcomes)}}))

(defn- figure-link [b kind]
  (if-let [path (get-in b [:figure-refs kind])]
    (str "\n![" (if (= kind :selection) "Selection: relative G and posterior" "Cascade: precedence and wanted-token outcomes")
         "](<" (.toASCIIString (java.net.URI. nil nil path nil nil)) ">)\n")
    (str "\n" (name kind) " figure not emitted; use render-run! to write standalone SVGs.\n")))

(defn preference-audit-text [d]
  (let [audit (get-in d [:selection-certificate :preference-audit])
        rows (get-in audit [:selected-target-odds :rows])
        computed (filter #(= :computed (:status %)) rows)
        held (remove #(= :computed (:status %)) rows)]
    (str "\nC preference audit (source budget: "
         (shown (get-in audit [:source-budget :domain])) ", "
         (shown (get-in audit [:source-budget :count])) " source entries): "
         (if (seq computed)
           (str "C prefers " (str/join "; " (map #(str (shown (:token %)) " present over absent by "
                                                       (String/format java.util.Locale/ROOT "%.6f" (to-array [(double (:present-to-absent %))]))
                                                       " : 1") computed)))
           "wanted-token odds are held or not recorded")
         (when (seq held) (str "; held " (shown (mapv #(select-keys % [:token :reason]) held))))
         ".\n")))

(defn novelty-text [d]
  (let [receipts (get-in d [:selection-certificate :parameter-novelty])]
    (str "Expected parameter information gain (record-only, not in G): "
         (if (seq receipts)
           (str/join "; "
             (for [r receipts]
               (str (get-in r [:id :target]) "/" (shown (get-in r [:id :id])) ": "
                    (if (= :computed (get-in r [:expected-kl :status]))
                      (str (get-in r [:expected-kl :nats]) " nats")
                      (str "unavailable (" (shown (get-in r [:expected-kl :reason])) ")"))
                    " under " (if (seq (:endpoints r))
                                (str/join ", " (distinct (map #(shown (get-in % [:prior :kind])) (:endpoints r))))
                                "no eligible prior"))))
           "not recorded under an undeclared prior") ".\n")))

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
                         ", near-tie " (shown (:near-tie? policy)) ", threshold " (shown (:near-tie-threshold policy)) "; ")
             "Not recorded in this run: policy comparison and near-tie threshold; no near-tie verdict is inferred; ")
         (if action (str "The action comparison records decided-by " (shown (:decided-by action)) ".\n")
             "Not recorded in this run: action comparison.\n")
         (coverage-text b)
         (preference-audit-text d)
         (novelty-text d)
         "\n| Target | Cascade | G (nats) | Posterior | Habit | F consumed |\n|---|---|---:|---:|---:|---:|\n"
         (apply str (for [r rows] (str "| " (str/join " | " (map #(shown (get r %)) [:target :cascade-id :G :posterior :habit :F])) " |\n")))
         (figure-link b :selection)
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
             (if-let [receipt (:cascade-structure c)]
               (str (structure/description receipt) ".\n\n")
               (if (= [] (:semilattice c)) "The semilattice field is not computed (literal []).\n\n"
                   (str "The semilattice field records " (shown (:semilattice c)) "; this is not a proof of structure.\n\n")))
             (figure-link b :cascade)
             (plan-quote b)))
      :dispatch (str "The runner dispatched agent " (shown (:agent j)) " as job " (shown (:job-id j)) ". "
                     "Its prompt reference is " (shown (:prompt-ref j)) ", with availability recorded as " (shown (:availability j)) ".\n")
      :build (str "The author produced commit(s) " (shown (:commits j)) ". "
                  "The recorded changed artifacts are " (shown (:artifacts j)) ". "
                  "Author execution evidence records " (shown (get-in j [:validation :author :command-events]))
                  " command events; reviewer execution records " (shown (get-in j [:validation :reviewer :command-events])) ".\n"
                  (review-text b))
      :adjudication (str "The recorded build match says review-approved " (shown (get-in j [:build-match :review-approved?]))
                         " for commit " (shown (get-in j [:build-match :commit])) ". "
                         "Grounding returned implementation " (shown (get-in j [:witness :implementation-id]))
                         " and discharge " (shown (get-in j [:witness :discharge-id]))
                         "; its dial-moved claim is " (shown (get-in j [:dial :moved?])) ".\n")
      :closed (str "The attempt closed with outcome " (shown (:outcome j)) " after " (shown (:duration-ms j)) " ms. "
                   "The recorded entity state at close has status " (shown (get-in j [:entity-state-at-close :status]))
                   " and reason " (shown (get-in j [:entity-state-at-close :reason])) ". "
                   "Its Morning Brief reference is " (shown (:morning-brief-ref j)) ".\n\n"
                   (route-attestation/paragraph (:route-attestation j))))))

(defn learning-trial-text [b]
  (apply str
         (for [trial (:trials (:learning-trial-receipt (judgment b :closed)))]
           (str (if (:selected-cascade trial)
                  (str "Attempt learning trial for cascade " (get-in trial [:selected-cascade :id]))
                  (str "Learning trial for " (:pattern trial)))
                " → " (pr-str (:effect trial)) ": " (name (:status trial))
                (when-let [reason (:reason trial)] (str " (" (name reason) ")"))
                (if-let [delivery (get-in trial [:attempt-beta :delivery-mean])]
                  (str "; record-only illustrative delivery mean " delivery " for this single attempt")
                  (when-let [theta (get-in trial [:shadow :if-counted-rollout :theta])]
                    (str (if (:selected-cascade trial) "; per-firing sensitivity shadow theta would be "
                             "; shadow theta would be ") theta " under the illustrative prior")))
                ".\n"))))

(defn narrative-text [b]
  (str "# Run " (:run-id b) "\n\nA narrative of retained evidence; no selection, observation, or actuation was rerun.\n\n"
       (apply str
              (for [stage checkpoint-order
                    :let [{:keys [path judgment]} (get-in b [:checkpoints stage])]]
                (str "## " (name stage) "\n\n"
                     (if (or judgment (and (= stage :selection) (seq (decision b))))
                       (body b stage)
                       (str "Not recorded in this run: checkpoint. The remaining evidence does not establish this stage's outcome.\n"
                            (when (= stage :selection) (coverage-text b))))
                     (when (= stage :time-step) (str (perceive-text b) (scan-account b)))
                     (when (= stage :closed) (str (outcome-text b) (learning-trial-text b)))
                     "\nCited facts:\n"
                     (cite (or path (:record-path b)) (if path [:payload :judgment] [:cohort-attempt]))
                     (when (= stage :time-step) (cite (:record-path b) [:startedAt]))
                     (when (= stage :selection)
                       (let [[p k] (decision-source b)]
                         (str (cite p (conj k :selection-certificate :candidates))
                            (cite p (conj k :selection-law))
                            (cite (:trace-path b) [:form (:trace-form b) :cascade-problems])
                            (apply str (for [key [:live-c-coverage :mission-hole-coverage]
                                             :let [[_ path keys] (coverage-source b key)]]
                                         (cite path keys)))
                            "\nDocstring correspondence, not runtime Lean execution: `src/futon2/aif/cascade_selection.clj` namespace docstring cites `PolicySelection.lean` at `a434947c63`, `selectionPosterior`.\n")))
                     (timing b stage) "\n")))))

(defn render-run! [root run-id output]
  (when-not (str/ends-with? (str output) ".md")
    (throw (ex-info "Narrative output must be a Markdown path" {:output output})))
  (let [outputs (figure-paths output)
        bundle (assoc (load-run root run-id) :figure-refs
                      (into {} (for [[k p] outputs] [k (.getName (io/file p))])))
        paths (concat [(:record-path bundle) (:trace-path bundle) (:phase-path bundle) (:binding-path bundle)]
                      (keep :path (vals (:checkpoints bundle)))
                      (when-let [p (:path (d-task-source bundle))]
                        [(if (.isAbsolute (io/file p)) p (str (io/file root p)))])
                      (when-let [p (get-in bundle [:record :scan-report :path])]
                        [(if (.isAbsolute (io/file p)) p (str (io/file root p)))])
                      (for [job (jobs bundle) kind [:prompt :reply]
                            :let [p (get-in job [kind :path])] :when p]
                        (if (.isAbsolute (io/file p)) p (str (io/file root p)))))
        _ (when (some (set (map #(.getCanonicalPath (io/file %)) (cons output (vals outputs))))
                      (map #(.getCanonicalPath (io/file %)) paths))
            (throw (ex-info "Output would overwrite retained evidence" {:output output})))
        _ (when-not (= 3 (count (set (map #(.getCanonicalPath (io/file %)) (cons output (vals outputs))))))
            (throw (ex-info "Narrative output paths alias each other" {:output output :figures outputs})))
        content (narrative-text bundle)
        data (figure-data bundle)
        selection (figures/selection-svg (:selection data))
        cascade (figures/cascade-svg (:cascade data))]
    ;; Resolve and validate all evidence before writing any of the three outputs.
    ;; Parents must already exist; no caches, directories, or other side effects.
    (spit (:selection outputs) selection)
    (spit (:cascade outputs) cascade)
    (spit output content)
    (str output)))

(defn -main [& [run-id output & extra]]
  (when (or (nil? run-id) (seq extra))
    (throw (ex-info "Usage: run-narrative <run-id> [out.md]" {})))
  (println (render-run! (or (System/getenv "FUTON2_NARRATIVE_ROOT") ".")
                        run-id (or output (str run-id "-narrative.md")))))
