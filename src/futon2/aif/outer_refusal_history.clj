(ns futon2.aif.outer-refusal-history
  "Read-only cross-run prevention for unchanged repeats of refused tasks.

  Terminal run records are the authority.  Missing identity fields are not
  invented; a prior refusal is comparable only on the identities it actually
  retained."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def ^:private terminal-record-name
  #"^tick-run-record-\d{4}-\d{2}-\d{2}-.+\.edn$")

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

(defn prior-refusals
  "Read terminal records in DIR and return prior refusals for TARGET.

  Malformed terminal EDN is a typed refusal: silently omitting history would
  turn corrupt evidence into permission to dispatch."
  [dir target]
  (let [root (io/file dir)
        files (if (.isDirectory root)
                (->> (.listFiles root)
                     (filter #(.isFile ^java.io.File %))
                     (filter #(re-matches terminal-record-name (.getName ^java.io.File %)))
                     (sort-by #(.getName ^java.io.File %)))
                [])]
    (->> files
         (keep (fn [file]
                 (let [record (try
                                (edn/read-string (slurp file))
                                (catch Exception e
                                  (throw (ex-info "Terminal refusal history is unreadable"
                                                  {:kind :outer/refusal-history-unreadable
                                                   :path (.getPath file)} e))))]
                   (when (and (refused-record? record)
                              (= target (record-target record)))
                     {:path (.getPath file)
                      :run-id (:run/id record)
                      :failure (select-keys (:failure record) [:kind :stage])
                      :inputs (record-inputs record)}))))
         vec)))

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
  (compare-refusals current (prior-refusals dir (:target current))))

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
