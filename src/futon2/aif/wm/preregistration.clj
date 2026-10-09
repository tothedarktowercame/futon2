(ns futon2.aif.wm.preregistration
  "Validate and evaluate falsifiable predictions registered against WM cards."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(def predicates '#{= not= > >= < <= contains present absent})
(def decisive-statuses #{:confirmed :refuted})

(defn sha256-file [path]
  (with-open [in (io/input-stream path)]
    (let [digest (java.security.MessageDigest/getInstance "SHA-256")
          buffer (byte-array 8192)]
      (loop []
        (let [n (.read in buffer)]
          (when (pos? n) (.update digest buffer 0 n) (recur))))
      (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest))))))

(defn read-edn [path]
  (edn/read-string {:default tagged-literal} (slurp path)))

(defn- predicate-form? [x]
  (and (map? x) (vector? (:path x)) (contains? predicates (:expect x))
       (if (contains? '#{present absent} (:expect x))
         (not (contains? x :value))
         (contains? x :value))))

(defn validate
  "Validate PREREG against CARDS-BY-RUN, whose values are report-card paths.
  Throws rather than allowing an ungrounded or open-vocabulary prediction."
  [prereg cards-by-run]
  (let [required [:id :against :defect :change :predictions :applies-when
                  :refuted-by :author :registered-for]
        missing (remove #(contains? prereg %) required)
        against (:against prereg)
        card-path (get cards-by-run (:run-id against))
        commits (get-in prereg [:change :commits])]
    (when (seq missing)
      (throw (ex-info "preregistration fields missing" {:missing (vec missing)})))
    (when-not (and (or (keyword? (:id prereg)) (string? (:id prereg)))
                   (= "the next run after these commits" (:registered-for prereg)))
      (throw (ex-info "invalid preregistration identity or registration horizon" {})))
    (when-not (and card-path (.isFile (io/file card-path)))
      (throw (ex-info "against run has no existing report card"
                      {:run-id (:run-id against)})))
    (let [actual (sha256-file card-path)]
      (when-not (= actual (:report-card-sha256 against))
        (throw (ex-info "against report card digest mismatch"
                        {:expected (:report-card-sha256 against) :actual actual}))))
    (when-not (and (map? (:defect prereg))
                   (contains? (:defect prereg) :path)
                   (contains? (:defect prereg) :facts))
      (throw (ex-info "defect must cite a card path and quoted facts" {})))
    (when-not (or (= :pending commits)
                  (and (vector? commits) (seq commits)
                       (every? #(and (string? %) (re-matches #"[0-9a-f]{7,40}" %)) commits)))
      (throw (ex-info "change commits must be :pending or nonempty git shas" {})))
    (when-not (and (vector? (:predictions prereg)) (seq (:predictions prereg))
                   (every? predicate-form? (:predictions prereg)))
      (throw (ex-info "predictions use an unknown or malformed predicate"
                      {:allowed predicates})))
    (when-not (predicate-form? (:applies-when prereg))
      (throw (ex-info "applies-when uses an unknown or malformed predicate"
                      {:allowed predicates})))
    prereg))

(defn default-ancestor? [commit source-revision]
  (zero? (:exit (shell/sh "git" "-C" "/home/joe/code/futon2"
                          "merge-base" "--is-ancestor" commit source-revision))))

(defn observed [card path]
  (get-in card path ::absent))

(defn predicate-holds? [card {:keys [path expect value]}]
  (let [x (observed card path)]
    (case expect
      = (= x value)
      not= (not= x value)
      > (and (number? x) (number? value) (> x value))
      >= (and (number? x) (number? value) (>= x value))
      < (and (number? x) (number? value) (< x value))
      <= (and (number? x) (number? value) (<= x value))
      contains (cond (map? x) (contains? x value)
                     (set? x) (contains? x value)
                     (sequential? x) (boolean (some #(= value %) x))
                     (string? x) (and (string? value) (str/includes? x value))
                     :else false)
      present (and (not= ::absent x)
                   (not (and (map? x) (contains? #{:absent :missing :typed-missing}
                                                  (:status x)))))
      absent (or (= ::absent x)
                 (and (map? x) (contains? #{:absent :missing :typed-missing}
                                           (:status x)))))))

(defn- prior-close [id prior-cards]
  (->> prior-cards
       (mapcat #(get-in % [:preregistrations :entries]))
       (filter #(and (= id (:id %)) (decisive-statuses (:status %))))
       first))

(defn evaluate-one
  [prereg card record {:keys [ancestor? prior-cards]
                       :or {ancestor? default-ancestor? prior-cards []}}]
  (if-let [closed (prior-close (:id prereg) prior-cards)]
    (let [run (or (:deciding-run closed) (:run-id closed))]
      {:id (:id prereg) :status :closed
       :deciding-run run
       :deciding-card {:run-id run :reference (str "../" run "/report-card.html")}
       :deciding-status (or (:deciding-status closed) (:status closed))})
    (let [commits (get-in prereg [:change :commits])
          revisions (get-in record [:registered-run/chronology :source-revisions-before])
          source-revision (or (get revisions "futon2") (get revisions :futon2))
          incorporated? (and (vector? commits) source-revision
                             (every? #(ancestor? % source-revision) commits))]
      (cond
        (not incorporated?)
        {:id (:id prereg) :status :not-yet-applicable
         :reason (cond (= :pending commits) :change-pending
                       (nil? source-revision) :source-revision-absent
                       :else :change-not-ancestor)
         :change-commits commits :source-revision source-revision}

        (not (predicate-holds? card (:applies-when prereg)))
        {:id (:id prereg) :status :not-exercised
         :applies-when (:applies-when prereg)
         :observed (let [p (get-in prereg [:applies-when :path])]
                     {:path p :value (observed card p)})}

        :else
        (let [checks (mapv (fn [prediction]
                             (let [v (observed card (:path prediction))]
                               {:prediction prediction :holds? (predicate-holds? card prediction)
                                :observed (if (= ::absent v) {:status :absent
                                                              :reason :path-not-present} v)}))
                           (:predictions prereg))
              status (if (every? :holds? checks) :confirmed :refuted)]
          {:id (:id prereg) :status status :run-id (:run-id card)
           :deciding-run (:run-id card) :checks checks})))))

(defn evaluate
  [preregs card record opts]
  (let [entries (mapv #(evaluate-one % card record opts) preregs)
        counts (merge {:confirmed 0 :refuted 0 :not-exercised 0
                       :not-yet-applicable 0 :closed 0}
                      (frequencies (map :status entries)))]
    {:counts counts :entries entries}))

(defn discover-card-paths [run-dir]
  (into {}
        (for [f (file-seq (io/file run-dir))
              :when (and (.isFile ^java.io.File f) (= "report-card.edn" (.getName f)))
              :let [card (try (read-edn f) (catch Exception _ nil))]
              :when (:run-id card)]
          [(:run-id card) (.getPath ^java.io.File f)])))

(defn load-registry
  ([root cards-by-run] (load-registry root cards-by-run {}))
  ([root cards-by-run _opts]
   (if-not (.isDirectory (io/file root)) []
     (mapv (fn [f] (validate (read-edn f) cards-by-run))
           (->> (.listFiles (io/file root))
                (filter #(.isFile ^java.io.File %))
                (filter #(str/ends-with? (.getName ^java.io.File %) ".edn"))
                (sort-by #(.getName ^java.io.File %)))))))
