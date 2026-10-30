(ns futon2.aif.outer-state
  "Durable carrier for the learned fields of `R20Step.State`.

  This is deliberately separate from outer-learning-manifest, which pins
  completed run publications rather than mutable state between clicks."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.outer-score :as outer-score]
            [futon2.aif.action-identity :as identity])
  (:import [java.nio.file Files StandardCopyOption]
           [java.security MessageDigest]
           [java.util UUID]))

(def makeup-schema :wm/outer-makeup-v1)
(def state-schema :wm/outer-state-v1)
;; The serving JVM runs from futon3c; live stores belong to canonical futon2.
(def default-makeup-path "/home/joe/code/futon2/data/wm-learning/outer-makeup.edn")
(def default-state-path "/home/joe/code/futon2/data/wm-learning/outer-state.edn")

(def ^:dynamic *before-state-move*
  "Fault-injection observer called after the temp file is complete, before rename."
  (fn [_temp _target] nil))

(defn- refuse! [kind data]
  (throw (ex-info "Outer state refused" (assoc data :kind kind))))

(defn- sha256 [^bytes bytes]
  (let [digest (.digest (doto (MessageDigest/getInstance "SHA-256")
                          (.update bytes)))]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(defn- read-one [bytes kind path]
  (try
    (let [value (edn/read-string {:eof ::eof} (String. ^bytes bytes "UTF-8"))]
      (when (= ::eof value) (refuse! kind {:path path :reason :empty}))
      value)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Exception e
      (refuse! kind {:path path :reason :malformed :message (.getMessage e)}))))

(defn- positive-number? [x]
  (and (number? x) (pos? x)))

(defn- valid-rate? [x]
  (positive-number? x))

(defn- valid-retention? [x]
  (and (positive-number? x) (<= x 1)))

(defn- require-keys! [m expected-keys path]
  (when-not (= (set expected-keys) (set (keys m)))
    (refuse! :invalid-makeup {:path path :reason :wrong-keys
                              :expected (set expected-keys) :actual (set (keys m))})))

(defn- validate-makeup! [makeup path]
  (when-not (= makeup-schema (:schema makeup))
    (refuse! :invalid-makeup {:path path :reason :schema :value (:schema makeup)}))
  (require-keys! (:c-kit makeup)
                 [:focused :related :unrelated :stop-the-line :not-yet-evaluated]
                 [:c-kit])
  (when-not (and (every? positive-number?
                         (map (:c-kit makeup) [:focused :related :unrelated :stop-the-line]))
                 (number? (get-in makeup [:c-kit :not-yet-evaluated]))
                 (not (neg? (get-in makeup [:c-kit :not-yet-evaluated]))))
    (refuse! :invalid-makeup {:path path :reason :invalid-c-kit}))
  (when-not (positive-number? (:kappa makeup))
    (refuse! :invalid-makeup {:path path :reason :invalid-kappa}))
  (require-keys! (:state-kit makeup) [:ready :not-ready :undetermined] [:state-kit])
  (doseq [[status row] (:state-kit makeup)]
    (require-keys! row [:doable :blocked] [:state-kit status])
    (when-not (every? positive-number? (vals row))
      (refuse! :invalid-makeup {:path path :reason :invalid-state-kit :status status})))
  (require-keys! (:gain-kit makeup) [:none :one :several] [:gain-kit])
  (when-not (every? positive-number? (vals (:gain-kit makeup)))
    (refuse! :invalid-makeup {:path path :reason :invalid-gain-kit}))
  (doseq [rate [:eta-c :eta-state :eta-gain]]
    (when-not (valid-rate? (rate makeup))
      (refuse! :invalid-makeup {:path path :reason :invalid-learning-rate :rate rate})))
  (doseq [rate [:omega-c :omega-state :omega-gain]]
    (when-not (valid-retention? (rate makeup))
      (refuse! :invalid-makeup {:path path :reason :invalid-forgetting-rate :rate rate})))
  (when-not (and (number? (:default-reach makeup))
                 (<= 0 (:default-reach makeup) 1))
    (refuse! :invalid-makeup {:path path :reason :invalid-default-reach}))
  (when (contains? makeup :cascades)
    (when-not (and (vector? (:cascades makeup))
                   (seq (:cascades makeup))
                   (every? #(and (keyword? (:id %))
                                 (vector? (:patterns %))
                                 (seq (:patterns %))
                                 (every? string? (:patterns %))
                                 (vector? (:steps %)))
                           (:cascades makeup))
                   (= (count (:cascades makeup))
                      (count (distinct (map :id (:cascades makeup))))))
      (refuse! :invalid-makeup {:path path :reason :invalid-cascades})))
  makeup)

(defn load-makeup
  "Load and validate Lean `R20Step.Makeup` kits and η/ω bounds.

  Returns the decoded makeup plus `:makeup/sha256`, the hash of its exact
  source bytes."
  ([] (load-makeup default-makeup-path))
  ([path]
   (let [file (io/file path)]
     (when-not (.isFile file)
       (refuse! :makeup-unavailable {:path (str path)}))
     (let [bytes (Files/readAllBytes (.toPath file))
           makeup (validate-makeup! (read-one bytes :invalid-makeup (str path)) (str path))]
       (assoc makeup :makeup/sha256 (sha256 bytes) :makeup/path (.getCanonicalPath file))))))

(defn init-state
  "Lean `R20Step.init`: initialize c to κ·kit and copy state/gain kits."
  [makeup]
  {:schema state-schema
   :makeup/sha256 (:makeup/sha256 makeup)
   :click-count 0
   :c (update-vals (:c-kit makeup) #(* (:kappa makeup) %))
   :state-counts (:state-kit makeup)
   :gain-counts (:gain-kit makeup)})

(defn advance-c
  "Lean `R20Step.advanceC`: decay every ending count, then learn a terminal ending.

  `:not-yet-evaluated` is not terminal, so observing it performs decay only."
  [eta omega before ending]
  (let [decayed (update-vals before #(* omega %))]
    (if (= :not-yet-evaluated ending)
      decayed
      (update decayed ending (fnil + 0) eta))))

(defn advance-gain-counts
  "Lean `R20Step.advanceGainCounts`: decay all gain counts and learn the
  observed gain exactly when a task was selected."
  [eta omega before observed selected?]
  (cond-> (update-vals before #(* omega %))
    selected? (update observed (fnil + 0) eta)))

(defn outer-state-likelihood
  "Lean `R20Step.outerStateLikelihood`, read from `outer-score/outer-model`'s
  scoring kernel so scoring and learning use the identical bounded reach and
  landing construction."
  [model observed task-state]
  (get-in model [:kernel task-state observed] 0.0))

(defn outer-state-posterior
  "Lean `R20Step.outerStatePosterior`: Bayes posterior over doable/blocked.

  An observation with zero total likelihood contributes zero evidence."
  [model observed task-state]
  (let [states [:doable :blocked]
        weight (fn [state]
                 (* (double (get-in model [:counts state] 0))
                    (outer-state-likelihood model observed state)))
        total (reduce + 0.0 (map weight states))]
    (if (zero? total) 0.0 (/ (weight task-state) total))))

(defn advance-state-counts
  "Lean `R20Step.advanceStateCounts`: decay every readiness row, then add
  the picked task's posterior to the row for its current Giraffe status."
  [eta omega before model observed]
  (let [decayed (update-vals before
                             (fn [row]
                               (update-vals row #(* omega %))))]
    (if (:pick model)
      (let [status (:status model)]
        (reduce (fn [counts task-state]
                  (update-in counts [status task-state] (fnil + 0)
                             (* eta (outer-state-posterior model observed task-state))))
                decayed
                [:doable :blocked]))
      decayed)))

(defn- chosen-cascade [makeup selection-result]
  (let [index (:cascade-index selection-result)
        entry (when (nat-int? index) (nth (:cascades makeup) index nil))]
    (when-not (and entry (= (:id entry) (:cascade-id selection-result)))
      (refuse! :invalid-selection-result
               {:reason :cascade-identity
                :cascade-index index
                :cascade-id (:cascade-id selection-result)}))
    (:steps entry)))

(declare valid-state? check-pending-makeup!)

(defn next-state
  "The learned-state part of Lean `R20Step.step`.

  Builds the chosen cascade's exact `outer-score/outer-model`, advances C,
  gain and readiness-state counts with makeup rates, and increments the click
  count.  It performs no I/O."
  [makeup state selection-result observation {:keys [ending-class observed-gain]}]
  (check-pending-makeup! makeup state selection-result)
  (let [cascade (chosen-cascade makeup selection-result)
        model (outer-score/outer-model
               (assoc observation
                      :state-counts (:state-counts state)
                      :cascade cascade))
        selected? (some? (:selection selection-result))
        observed [ending-class observed-gain]]
    (-> state
        (dissoc :source)
        (assoc :c (advance-c (:eta-c makeup) (:omega-c makeup)
                             (:c state) ending-class)
               :gain-counts (advance-gain-counts
                             (:eta-gain makeup) (:omega-gain makeup)
                             (:gain-counts state) observed-gain selected?)
               :state-counts (advance-state-counts
                              (:eta-state makeup) (:omega-state makeup)
                              (:state-counts state) model observed))
        (update :click-count inc))))

(defn consume-observation
  "Apply one pinned pending observation once. The receipt travels inside the
  durable state, so counts, consumption identity and replacement pending can
  be committed together by write-state!. R20Step.step supplies the update."
  [makeup state pending observation ending]
  (let [run-id (:run-id pending)
        input {:run-id run-id :selection (:selection pending) :ending ending
               :observation (dissoc observation :relation)}
        input-pin (identity/digest input)
        prior (get-in state [:outer/consumptions run-id])]
    (cond
      (and prior (not= (:sha256 prior) (identity/digest (dissoc prior :sha256))))
      (refuse! :consumption-receipt-tampered {:run-id run-id})
      prior
      (if (= input-pin (:input-sha256 prior))
        {:status :already-consumed :state state :receipt prior}
        (refuse! :consumed-observation-conflict {:run-id run-id}))
      (not= pending (:pending state))
      (refuse! :pending-observation-mismatch {:run-id run-id})
      (not (and (string? run-id) (= :ready (:status ending))
                (= run-id (:run-id ending)) (:record-pin ending)))
      (refuse! :outer-observation-unverified {:run-id run-id})
      :else
      (let [advanced (next-state makeup state (:selection pending) observation ending)
            receipt {:schema :wm/outer-consumption-v1 :run-id run-id
                     :makeup-sha256 (:makeup/sha256 makeup)
                     :input-sha256 input-pin
                     :selection-sha256 (identity/digest (:selection pending))
                     :observation-sha256 (identity/digest (:observation input))
                     :ending ending :selected-relation (:selected-relation (:selection pending))
                     :click-count-before (:click-count state)
                     :click-count-after (:click-count advanced)}
            receipt (assoc receipt :sha256 (identity/digest receipt))]
        {:status :applied :receipt receipt
         :state (-> advanced (dissoc :pending)
                    (assoc-in [:outer/consumptions run-id] receipt))}))))

(defn replace-pending
  "Stage replacement in the same value as the consumed counts and receipt.
  An unconsumed different run may not be silently overwritten. A click-local
  reselection can replace its own selection before that run completes."
  [state pending]
  (when (and (:pending state)
             (not= (get-in state [:pending :run-id]) (:run-id pending))
             (not (get-in state [:outer/consumptions (get-in state [:pending :run-id])])))
    (refuse! :pending-observation-unconsumed {:run-id (get-in state [:pending :run-id])}))
  (assoc state :pending pending))

(defn- valid-state? [state]
  (and (= state-schema (:schema state))
       (string? (:makeup/sha256 state))
       (boolean (re-matches #"[0-9a-f]{64}" (:makeup/sha256 state)))
       (int? (:click-count state))
       (not (neg? (:click-count state)))
       (= #{:focused :related :unrelated :stop-the-line :not-yet-evaluated}
          (set (keys (:c state))))
       (every? #(and (number? %) (not (neg? %))) (vals (:c state)))
       (= #{:ready :not-ready :undetermined} (set (keys (:state-counts state))))
       (every? #(and (= #{:doable :blocked} (set (keys %)))
                     (every? positive-number? (vals %)))
               (vals (:state-counts state)))
       (= #{:none :one :several} (set (keys (:gain-counts state))))
       (every? positive-number? (vals (:gain-counts state)))))

(defn- state-bytes [state]
  (.getBytes (str (pr-str (dissoc state :source)) "\n") "UTF-8"))

(defn- makeup-values [makeup]
  (dissoc makeup :makeup/sha256 :makeup/path))

(defn- check-makeup-extension! [old new]
  (validate-makeup! old :old-makeup)
  (validate-makeup! new :new-makeup)
  (when-not (and (not (contains? old :precision-iterations))
                 (nat-int? (:precision-iterations new))
                 (= (makeup-values old)
                    (dissoc (makeup-values new) :precision-iterations)))
    (refuse! :makeup-migration-not-precision-only
             {:old (makeup-values old) :new (makeup-values new)})))

(defn migrate-makeup-pin
  "Pure, narrowly admitted Makeup extension. All state values except the pin
  are retained unchanged. State SHAs use write-state!'s encoding; when the
  supported reader supplied exact original bytes, the old SHA pins those bytes.
  The receipt is stored separately, never inserted into the learned state."
  [state old-makeup new-makeup]
  (check-makeup-extension! old-makeup new-makeup)
  (when-not (and (valid-state? state)
                 (= (:makeup/sha256 state) (:makeup/sha256 old-makeup))
                 (string? (:makeup/sha256 new-makeup)))
    (refuse! :makeup-migration-state-mismatch {}))
  (let [migrated (assoc state :makeup/sha256 (:makeup/sha256 new-makeup))
        receipt {:schema :wm/makeup-pin-migration-v1
                 :old-makeup old-makeup :new-makeup new-makeup
                 :old-makeup-sha256 (:makeup/sha256 old-makeup)
                 :new-makeup-sha256 (:makeup/sha256 new-makeup)
                 :old-state-sha256 (or (::bytes-sha256 (meta state))
                                       (sha256 (state-bytes state)))
                 :new-state-sha256 (sha256 (state-bytes migrated))
                 :learning-parameters-equal true}]
    {:state (with-meta migrated {::migration receipt}) :receipt receipt}))

(defn- checked-migration? [state makeup receipt]
  (and (= :wm/makeup-pin-migration-v1 (:schema receipt))
       (true? (:learning-parameters-equal receipt))
       (= makeup (:new-makeup receipt))
       (= (:makeup/sha256 state) (:makeup/sha256 makeup)
          (:new-makeup-sha256 receipt))
       (= (:old-makeup-sha256 receipt) (get-in receipt [:old-makeup :makeup/sha256]))
       (= (:new-state-sha256 receipt) (sha256 (state-bytes state)))
       (do (check-makeup-extension! (:old-makeup receipt) (:new-makeup receipt)) true)))

(defn- check-pending-makeup! [makeup state selection]
  (when (:pending state)
    (let [pin (:makeup/sha256 selection)
          receipt (::migration (meta state))]
      (when-not (or (= pin (:makeup/sha256 makeup))
                    (and (= pin (:old-makeup-sha256 receipt))
                         (checked-migration? state makeup receipt)))
        (refuse! :pending-makeup-migration-required
                 {:selection-makeup pin :current-makeup (:makeup/sha256 makeup)})))))

(defn read-state
  "Read the durable `R20Step.State` carrier, or return init marked `:source :init`.

  A durable state pinned to different makeup bytes refuses `:makeup-changed`."
  ([makeup] (read-state makeup default-state-path))
  ([makeup path]
   (let [file (io/file path)]
     (if-not (.isFile file)
       (assoc (init-state makeup) :source :init)
       (let [bytes (Files/readAllBytes (.toPath file))
             state (read-one bytes :invalid-state (str path))]
         (when-not (valid-state? state)
           (refuse! :invalid-state {:path (str path)}))
         (when-not (= (:makeup/sha256 makeup) (:makeup/sha256 state))
           (refuse! :makeup-changed {:path (str path)
                                     :expected (:makeup/sha256 makeup)
                                     :actual (:makeup/sha256 state)}))
         (let [digest (sha256 bytes)
               receipt-file (io/file (str path ".makeup-migration-" digest ".edn"))
               receipt (when (.isFile receipt-file)
                         (read-one (Files/readAllBytes (.toPath receipt-file))
                                   :invalid-migration-receipt (str receipt-file)))
               result (assoc state :source :durable)]
           (when (and receipt (not (checked-migration? result makeup receipt)))
             (refuse! :invalid-migration-receipt {:path (str receipt-file)}))
           (with-meta result {::bytes-sha256 digest ::migration receipt})))))))

(defn write-state!
  "Atomically replace the durable `R20Step.State` carrier in one directory."
  ([state] (write-state! default-state-path state))
  ([path state]
   (when-not (valid-state? state)
     (refuse! :invalid-state {:path (str path)}))
   (let [target (io/file path)
         parent (.getParentFile target)
         _ (io/make-parents target)
         temp (io/file parent (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
     (try
       (Files/write (.toPath temp) (state-bytes state) (make-array java.nio.file.OpenOption 0))
       (*before-state-move* temp target)
       (Files/move (.toPath temp) (.toPath target)
                   (into-array StandardCopyOption
                               [StandardCopyOption/ATOMIC_MOVE
                                StandardCopyOption/REPLACE_EXISTING]))
       {:status :written :path (.getCanonicalPath target)
        :click-count (:click-count state) :makeup/sha256 (:makeup/sha256 state)}
       (finally
         (when (.exists temp) (Files/deleteIfExists (.toPath temp))))))))

(defn write-makeup-migration!
  "Apply an already computed migration, retaining exact old bytes and its
  separate receipt. Refuses a changed source file before any write."
  [path {:keys [state receipt]}]
  (let [target (io/file path)
        bytes (Files/readAllBytes (.toPath target))
        old-sha (sha256 bytes)
        backup (io/file (str path ".pre-r20-" (subs old-sha 0 8)))
        receipt-file (io/file (str path ".makeup-migration-"
                                   (:new-state-sha256 receipt) ".edn"))]
    (when-not (and (= old-sha (:old-state-sha256 receipt))
                   (checked-migration? state (:new-makeup receipt) receipt)
                   (= (dissoc (read-one bytes :invalid-state (str path)) :makeup/sha256 :source)
                      (dissoc state :makeup/sha256 :source)))
      (refuse! :makeup-migration-source-changed {:path (str path)}))
    (when (and (.exists backup)
               (not= old-sha (sha256 (Files/readAllBytes (.toPath backup)))))
      (refuse! :makeup-migration-backup-conflict {:path (str backup)}))
    (when-not (.exists backup)
      (Files/write (.toPath backup) bytes (make-array java.nio.file.OpenOption 0)))
    (spit receipt-file (str (pr-str receipt) "\n"))
    (write-state! path state)
    {:status :migrated :receipt receipt :backup (.getCanonicalPath backup)
     :receipt-path (.getCanonicalPath receipt-file)}))
