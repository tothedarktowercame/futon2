(ns futon2.aif.outer-state
  "Durable carrier for the learned fields of `R20Step.State`.

  This is deliberately separate from outer-learning-manifest, which pins
  completed run publications rather than mutable state between clicks."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.outer-score :as outer-score])
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

(defn next-state
  "The learned-state part of Lean `R20Step.step`.

  Builds the chosen cascade's exact `outer-score/outer-model`, advances C,
  gain and readiness-state counts with makeup rates, and increments the click
  count.  It performs no I/O."
  [makeup state selection-result observation {:keys [ending-class observed-gain]}]
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
         (assoc state :source :durable))))))

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
       (spit temp (str (pr-str (dissoc state :source)) "\n"))
       (*before-state-move* temp target)
       (Files/move (.toPath temp) (.toPath target)
                   (into-array StandardCopyOption
                               [StandardCopyOption/ATOMIC_MOVE
                                StandardCopyOption/REPLACE_EXISTING]))
       {:status :written :path (.getCanonicalPath target)
        :click-count (:click-count state) :makeup/sha256 (:makeup/sha256 state)}
       (finally
         (when (.exists temp) (Files/deleteIfExists (.toPath temp))))))))
