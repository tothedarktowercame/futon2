(ns futon2.aif.outer-refusal-history
  "Read-only cross-run prevention for unchanged repeats of refused tasks.

  The compact terminal-record index is the click-path authority. Missing
  identity fields are not invented; a prior refusal is comparable only on
  the identities it actually retained. This namespace never opens a terminal
  run record."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.meta-terminal-injury :as terminal-injury]))

(def index-name "outer-refusal-index.ednl")

(def ^:private outer-identity-keys
  [:cascade-id :cascade-index :giraffe/run-id :giraffe/manifest-sha256
   :makeup/sha256 :focus :selected-relation])

(defn record-target
  "Return the selected task identity retained by a terminal record."
  [record]
  (or (get-in record [:outer-repeat-comparison :inputs :target])
      (get-in record [:outer/selection :selection :selected :target])
      (get-in record [:selection-event :target])
      (get-in record [:failure :target])
      (get-in record [:failure :detail :target])))

(defn record-inputs
  "Project the relevant identities a terminal record actually carries."
  [record]
  (or (get-in record [:outer-repeat-comparison :inputs])
      (cond-> {:schema :wm/outer-repeat-inputs-v1
               :target (record-target record)}
        (get-in record [:world-at-selection :selection-input-digest])
        (assoc :selection-input-digest
               (get-in record [:world-at-selection :selection-input-digest]))

        (seq (select-keys (:outer/selection record) outer-identity-keys))
        (assoc :outer-selection
               (select-keys (:outer/selection record) outer-identity-keys)))))

(defn refused-record?
  "True for a terminal record that retained a typed failure/refusal."
  [record]
  (and (map? (:failure record))
       (keyword? (get-in record [:failure :kind]))))

(defn index-entry
  "Project one already-read terminal RECORD into its compact index row."
  [record record-path record-sha256]
  {:schema :wm/outer-refusal-index-entry-v1
   :status :indexed
   :run-id (:run/id record)
   :record-path record-path
   :record-sha256 record-sha256
   :target (record-target record)
   :failure (select-keys (:failure record) [:kind :stage])
   :finished-work? (terminal-injury/finished-work? record)
   :satisfaction (terminal-injury/satisfaction-evidence record)
   :inputs (record-inputs record)
   :effective-run-identity (:effective-run-identity record)})

(defn reading-index-entries
  "Project each intermediate reading refusal once, even if the run succeeds.
  Reading rows deliberately lack terminal :failure and :finished-work? fields."
  [record record-path record-sha]
  (let [reading (:selected-target-reading record)
        attempts (or (seq (:reading-attempts reading)) [reading])]
    (->> attempts
         (filter #(and (= :refused (:status %)) (keyword? (:kind %))
                       (not (contains? #{:selected-target-reading-unchanged-refusal
                                         :selected-target-reading-history-invalid} (:kind %)))))
         distinct
         (mapv (fn [attempt]
                 {:schema :wm/reading-refusal-index-entry-v1 :status :indexed
                  :run-id (:run/id record) :record-path record-path :record-sha256 record-sha
                  :target (:target attempt) :refusal (:kind attempt)
                  :reading-inputs (:reading-inputs attempt)
                  :identity-status (if (:reading-inputs attempt) :retained :legacy-unavailable)
                  :reader-job-ids (get-in attempt [:detail :reader-job-ids])
                  :response-pins (get-in attempt [:detail :response-pins])})))))

(defn read-index
  "Read the compact index under DIR. A malformed index is a typed refusal;
  terminal records are never opened here."
  [dir]
  (let [file (io/file dir index-name)]
    (if-not (.isFile file)
      {:schema :wm/outer-repeat-comparison-v1
       :status :history-index-absent
       :index-path (.getPath file)}
      (with-open [reader (io/reader file)]
        (->> (line-seq reader)
             (map-indexed
              (fn [line-number line]
                (try
                  (edn/read-string line)
                  (catch Exception e
                    (throw (ex-info "Terminal refusal index is unreadable"
                                    {:kind :outer/refusal-index-unreadable
                                     :path (.getPath file)
                                     :line (inc line-number)} e))))))
             doall
             vec)))))

(defn prior-refusals
  "Read only the compact index in DIR and return prior refusals for TARGET.
  Legacy unreadable rows remain visible in the index but cannot authorize a
  refusal because they carry no target or inputs."
  [dir target]
  (let [index (read-index dir)]
    (if (map? index)
      index
      (->> index
           (filter #(and (= :indexed (:status %))
                         (keyword? (get-in % [:failure :kind]))
                         (= target (:target %))))
           (mapv (fn [row]
                   {:path (:record-path row)
                    :record-sha256 (:record-sha256 row)
                    :run-id (:run-id row)
                    :failure (:failure row)
                    :inputs (:inputs row)}))))))

(defn compare-refusals
  "Compare CURRENT inputs with prior refusals.

  Each prior record is compared on every identity it retained.  A legacy row
  must at least retain target and selection-input-digest; otherwise it is
  reported as incomparable, never treated as unchanged."
  [current priors]
  (let [rows
        (mapv
         (fn [{:keys [inputs] :as prior}]
           (let [identity-keys (-> (keys inputs) set (disj :schema))
                 comparable? (and (contains? identity-keys :target)
                                  (or (contains? identity-keys :selection-input-digest)
                                      (contains? identity-keys :outer-selection)))
                 differing (->> identity-keys
                                (filter #(not= (get inputs %) (get current %)))
                                sort vec)]
             (assoc prior
                    :comparable? comparable?
                    :differing-identities differing
                    :unchanged? (and comparable? (empty? differing)))))
         priors)
        unchanged (first (filter :unchanged? rows))
        comparable (filter :comparable? rows)]
    {:schema :wm/outer-repeat-comparison-v1
     :status (cond unchanged :unchanged-refused-repeat
                   (seq comparable) :inputs-changed
                   (seq rows) :prior-inputs-incomparable
                   :else :no-prior-refusal)
     :inputs current
     :matched-refusal unchanged
     :comparisons rows}))

(defn compare-history
  "Read and compare the terminal refusal history for CURRENT inputs."
  [dir current]
  (let [priors (prior-refusals dir (:target current))]
    (if (map? priors)
      (assoc priors :inputs current :comparisons [])
      (compare-refusals current priors))))

(defn require-new-inputs!
  "Return COMPARISON, or refuse an unchanged repeat with both identities."
  [comparison]
  (when (= :unchanged-refused-repeat (:status comparison))
    (throw (ex-info "Selected task repeats a refusal on unchanged inputs"
                    {:outcome :refused
                     :failure-kind :outer/unchanged-refused-repeat
                     :failure-stage :outer-repeat-comparison
                     :outer-repeat-comparison comparison})))
  comparison)
