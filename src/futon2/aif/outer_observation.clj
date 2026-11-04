(ns futon2.aif.outer-observation
  "Build the task-level `R20Step.Observation` from verified Giraffe evidence.

  The serving JVM supplies futon3c's verifier dynamically.  This namespace
  does not duplicate its manifest law and refuses when that authority is not
  available."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [futon2.aif.area-focus :as area-focus]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.outer-score :as outer-score]
            [futon2.aif.task-requisition :as task-requisition]
            [futon2.aif.wm.construction-inputs :as construction-inputs])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def ^:dynamic *resolve-var* requiring-resolve)

(defn- refuse! [kind data]
  (throw (ex-info "Outer observation refused" (assoc data :kind kind))))

(defn- sha256 [^bytes bytes]
  (let [digest (.digest (doto (MessageDigest/getInstance "SHA-256")
                          (.update bytes)))]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(defn- read-edn-file [file kind]
  (try
    (edn/read-string (slurp file))
    (catch Exception e
      (refuse! kind {:path (str file) :message (.getMessage e)}))))

(defn verified-input-pin
  "Read the input pin from the run manifest and verify the actual bytes.
  Missing evidence remains a typed absence; a mismatched pin refuses."
  [run-dir]
  (let [manifest-file (io/file run-dir "manifest.edn")
        input-file (io/file run-dir "input.edn")]
    (cond
      (not (.isFile manifest-file)) {:status :absent :reason :giraffe-manifest-missing}
      (not (.isFile input-file)) {:status :absent :reason :giraffe-input-missing}
      :else
      (let [manifest (read-edn-file manifest-file :giraffe-manifest-unreadable)
            pin (some #(when (= "input.edn" (:file %)) (:sha256 %)) (:files manifest))
            actual (sha256 (Files/readAllBytes (.toPath input-file)))]
        (cond
          (nil? pin) {:status :absent :reason :giraffe-input-pin-unavailable}
          (= pin actual) actual
          :else (refuse! :giraffe-input-pin-mismatch {:expected pin :actual actual}))))))

(defn- verifier []
  (try
    (or (*resolve-var* 'futon3c.giraffe.run/verify-run)
        (refuse! :giraffe-verifier-unavailable {}))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Exception e
      (refuse! :giraffe-verifier-unavailable {:message (.getMessage e)}))))

(defn- target-authority []
  (let [loaded-tickets (mission-registry/load-tickets)
        loaded-excursions (mission-registry/load-excursions)]
    {:registry-source :substrate
     :loaded-missions (mission-registry/load-missions)
     :loaded-tickets loaded-tickets
     :loaded-excursions loaded-excursions
     :ticket-targets (mapv :id (:tickets loaded-tickets))
     :excursion-targets (mapv :id (:excursions loaded-excursions))}))

(defn- registry-entries [{:keys [loaded-missions loaded-tickets loaded-excursions]}]
  (into {}
        (map (juxt :id identity))
        (concat (:missions loaded-missions)
                (:tickets loaded-tickets)
                (:excursions loaded-excursions))))

(def task-exclusion-reasons
  "Typed task-admission exclusions. Evidence gaps have no in-flight producer;
  requisition states are ineligible by the shared task-requisition authority.
  Registry statuses and inner-policy satisfaction are not exclusions."
  (into #{:target-source-path-absent :target-source-conflict :target-item-line-absent
          :unread-new :reading-source-changed}
        (keep (comp task-requisition/ineligible-reason #(hash-map :state %)))
        (vals task-requisition/states)))

(defn validate-exclusion! [{:keys [target reason pin] :as row}]
  (when-not (and (string? target) (task-exclusion-reasons reason)
                 (string? pin) (seq pin))
    (refuse! :invalid-task-exclusion {:exclusion row}))
  row)

(defn validate-field!
  "Check exact open-set partition, not just matching cardinalities."
  [enumerated admitted excluded]
  (doseq [row excluded] (validate-exclusion! row))
  (let [ids (concat admitted (map :target excluded))]
    (when-not (and (= (count enumerated) (count (set enumerated)))
                   (= (count ids) (count (set ids)))
                   (= (set enumerated) (set ids)))
      (refuse! :task-field-partition-invalid
               {:missing (set/difference (set enumerated) (set ids))
                :extra (set/difference (set ids) (set enumerated))
                :duplicate-ids (vec (for [[id n] (frequencies ids) :when (> n 1)] id))})))
  true)

(defn task-reach
  "Version 1 readiness prior: ready 3/4, undetermined 1/2, not-ready 1/4.
  Status is measured in the pinned pass; the mapping is an empirical kit Joe
  may replace, NOT a calibrated probability. Reads no policy, makeup or prior
  run. Unknown status or missing row evidence stays explicitly absent."
  [row]
  (let [pin (get-in row [:dependency-reading :record-pin :sha256])
        value (get (:readiness-reach-prior outer-score/kernel-definition) (:status row))
        inputs {:status (:status row) :pin pin}]
    (if (and value (string? pin) (seq pin))
      {:value value :kind :prior :source :giraffe-status-readiness-prior-v1
       :inputs inputs :kit-version 1}
      {:status :absent :reason (if value :row-pin-unavailable :status-unavailable)
       :inputs inputs})))

(defn- open-entries [authority]
  (concat (filter mission-registry/live-mission? (get-in authority [:loaded-missions :missions]))
          (filter mission-registry/live-excursion? (get-in authority [:loaded-excursions :excursions]))
          (filter mission-registry/live-ticket? (get-in authority [:loaded-tickets :tickets]))))

(defn- data-pin [x] (sha256 (.getBytes (pr-str x) "UTF-8")))

(defn- open-roster [authority entries]
  (let [ids (set (map :id entries))
        groups (mapv (fn [[loaded kind]]
                       (vec (sort (filter ids (map :id (get-in authority [loaded kind]))))))
                     [[:loaded-missions :missions] [:loaded-excursions :excursions]
                      [:loaded-tickets :tickets]])]
    (assoc (zipmap [:missions :excursions :tickets] groups)
           ;; Explicit authorities may come from tooling; do not invent provenance.
           :source (or (:registry-source authority) :unspecified)
           :registry-pin (data-pin groups))))

(defn giraffe-field
  "The registry-open task population, partitioned into pinned pass facts and
  typed task exclusions. No inner-policy or makeup reach participates."
  ([run-dir] (giraffe-field run-dir (outer-state/load-makeup)))
  ([run-dir makeup] (giraffe-field run-dir makeup (target-authority)))
  ([run-dir makeup authority]
   (let [dir (io/file run-dir) verification ((verifier) dir)]
     (when-not (:ok? verification)
       (refuse! :giraffe-run-unverified {:run-dir (.getPath dir) :verification verification}))
     (let [manifest-file (io/file dir "manifest.edn") pass-file (io/file dir "pass.edn")]
       (when-not (and (.isFile manifest-file) (.isFile pass-file))
         (refuse! :giraffe-run-incomplete {:run-dir (.getPath dir)}))
       (let [manifest (read-edn-file manifest-file :giraffe-manifest-malformed)
             pass (read-edn-file pass-file :giraffe-pass-malformed)
             manifest-sha (sha256 (Files/readAllBytes (.toPath manifest-file)))
             pass-sha (sha256 (Files/readAllBytes (.toPath pass-file)))
             rows (:order pass)
             _ (when-not (and (vector? rows) (every? #(and (:id %) (contains? % :status)) rows)
                              (= (count rows) (count (set (map :id rows)))))
                 (refuse! :giraffe-pass-invalid {:run-dir (.getPath dir)}))
             by-id (into {} (map (juxt (comp name :id) identity)) rows)
             registry (registry-entries authority)
             open (vec (open-entries authority))
             roster (open-roster authority open)
             open-ids (mapv :id open)
             _ (when-not (= (count open-ids) (count (set open-ids)))
                 (refuse! :registry-duplicate-open-task {}))
             ;; Preserve the pass's order (including stable-sort ties), then
             ;; append newly open tasks that have no pass row.
             enumerated (into (filterv (set open-ids) (mapv (comp name :id) rows))
                              (remove #(contains? by-id %) open-ids))
             declarations (construction-inputs/target-source-declarations enumerated authority)
             changed (set (map name (get-in manifest [:reading-drift :drift :reading-source-changed])))
             entries
             (mapv (fn [target declaration]
                     (let [row (get by-id target)
                           registry-entry (get registry target)
                           requisition-reason
                           (task-requisition/ineligible-reason
                            (:requisition registry-entry))
                           reason (cond requisition-reason requisition-reason
                                        (contains? changed target) :reading-source-changed
                                        (nil? row) :unread-new
                                        :else (:source-absent declaration))
                           pin (get-in row [:dependency-reading :record-pin :sha256])]
                       (if reason
                         {:excluded (validate-exclusion!
                                     {:target target :reason reason
                                      :reach (if row (task-reach row)
                                               {:status :absent :reason :pass-row-unavailable})
                                      :pin (case reason
                                             :reading-source-changed manifest-sha
                                             :unread-new pass-sha
                                             (data-pin registry-entry))
                                      :evidence (if (contains? #{:unread-new :reading-source-changed} reason)
                                                  {:pass-sha256 pass-sha :manifest-sha256 manifest-sha
                                                   :registry-entry registry-entry}
                                                  {:registry-entry registry-entry :declaration declaration})})}
                         (do
                           (when-not (and (string? pin) (seq pin))
                             (refuse! :giraffe-row-pin-unavailable {:target target :pass-sha256 pass-sha}))
                           (let [reach (task-reach row)]
                             {:candidate {:target target :reach (:value reach) :reach-source (:source reach)}
                              :target target
                              :fact (assoc (select-keys row [:status :cone :unblocks :rank :rank-basis :graph-terms
                                                            :completion :dependency-reading])
                                           :pin pin :reach reach :reach-source (:source reach))})))) )
                   enumerated declarations)
             admitted (filterv :candidate entries)
             excluded (mapv :excluded (filter :excluded entries))
             facts (into {} (map (juxt :target :fact)) admitted)]
         (validate-field! enumerated (mapv :target admitted) excluded)
         {:field (mapv :candidate admitted) :facts facts
          :enumerated-open-tasks enumerated
          :field-facts {:open-tasks roster :enumerated enumerated :admitted facts :excluded excluded
                        :giraffe/pass-sha256 pass-sha}
          :outer-field/excluded excluded
          :registry/non-open (mapv #(select-keys % [:id :status-class
                                                    :mission/unselectable-reason])
                                  (remove #(contains? (set enumerated) (:id %)) (vals registry)))
          :default-reach-ignored (contains? makeup :default-reach)
          :giraffe/run-id (:run-id manifest)
          :giraffe/pass-sha256 pass-sha
          :giraffe/input-sha256 (verified-input-pin run-dir)
          :giraffe/manifest-sha256 manifest-sha})))))

(defn outer-observation
  "Build the input read by `outer-score`, aligned with Lean `R20Step.Observation`.

  Required keys are `:run-dir`, `:makeup`, `:focus-inputs`, and
  `:decision-as-of`.  Optional `:previous-focus` and `:relation-context` are
  passed to the shared focus producer.  Unknown relations remain `:unknown`."
  [{:keys [run-dir makeup focus-inputs decision-as-of previous-focus relation-context
           area-corpus focus-manifest focus-manifest-sha256 focus-manifest-source]}]
  (let [giraffe (giraffe-field run-dir makeup)
        area-mode? (and area-corpus focus-manifest)
        focus-result (when-not area-mode? (focus-receipt/decision-focus focus-inputs decision-as-of previous-focus))
        focus-info (when focus-result (:info focus-result))
        receipts (into {} (map (fn [target]
                                 [target (if area-mode?
                                           (area-focus/classify-target
                                            (:rows area-corpus) (:sha256 area-corpus) focus-manifest
                                            focus-manifest-sha256 target)
                                           (focus-receipt/classify-target
                                            focus-inputs focus-info decision-as-of target relation-context))])
                               (distinct (map :target (:field giraffe)))))
        ;; One retained receipt supplies both the scoring class and its evidence.
        relation (fn [focus target]
                   (if (or area-mode? (= focus (:focus focus-info)))
                     (get-in receipts [target (if area-mode? :relation-key :class)] :unknown) :unknown))
        giraffe (update giraffe :facts
                        (fn [facts]
                          (reduce-kv (fn [m target receipt]
                                       (assoc-in m [target :relation]
                                                 {:class (:class receipt) :receipt receipt}))
                                     facts receipts)))]
    (merge giraffe
           {:focus (if area-mode? (area-focus/focus-areas focus-manifest) (:focus focus-info))
            :relation relation
            :focus/source (if area-mode?
                            {:status (get-in focus-manifest [:result :status])
                             :as-of (:as-of focus-manifest)
                             :manifest (merge focus-manifest-source
                                              {:sha256 focus-manifest-sha256})
                             :corpus-sha256 (:sha256 area-corpus)}
                            (select-keys focus-info
                                         [:status :focus-origin :as-of
                                          :retained-evidence-as-of]))})))
