(ns futon2.aif.selection-timing
  "Monotonic, non-overlapping timing receipts for the WM selection body.")

(def schema :wm/selection-subphase-timing-v1)

(defn new-state []
  (atom {:schema schema :attempts [] :current nil :errors []}))

(defn- clock [nano-time-fn]
  ((or nano-time-fn #(System/nanoTime))))

(defn begin!
  [state nano-time-fn subphase]
  (when (instance? clojure.lang.IAtom state)
    (let [at (clock nano-time-fn)]
      (swap! state
             (fn [{:keys [current] :as s}]
               (cond-> s
                 current (update :attempts conj
                                 (assoc current :status :typed-missing
                                                :reason :superseded-before-finish
                                                :ended-at-monotonic-ns at))
                 true (assoc :current {:status :running
                                       :started-at-monotonic-ns at
                                       :cursor-at-monotonic-ns at
                                       :subphase subphase
                                       :measurements []}))))))
  nil)

(defn checkpoint!
  "Close the current subphase and begin NEXT. The timestamp is sampled once,
   so adjacent measurements neither overlap nor leave an instrumentation gap."
  [state nano-time-fn next]
  (when (instance? clojure.lang.IAtom state)
    (let [at (clock nano-time-fn)]
      (swap! state
             (fn [s]
               (if-let [{:keys [cursor-at-monotonic-ns subphase]} (:current s)]
                 (if (< at cursor-at-monotonic-ns)
                   (update s :errors conj
                           {:reason :monotonic-clock-regressed
                            :subphase subphase
                            :previous-ns cursor-at-monotonic-ns
                            :observed-ns at})
                   (-> s
                       (update-in [:current :measurements] conj
                                  {:subphase subphase
                                   :started-at-monotonic-ns cursor-at-monotonic-ns
                                   :ended-at-monotonic-ns at
                                   :duration-ns (- at cursor-at-monotonic-ns)})
                       (assoc-in [:current :cursor-at-monotonic-ns] at)
                       (assoc-in [:current :subphase] next)))
                 s)))))
  nil)

(defn finish!
  [state nano-time-fn]
  (when (instance? clojure.lang.IAtom state)
    (checkpoint! state nano-time-fn :receipt-complete)
    (swap! state
           (fn [{:keys [current] :as s}]
             (if current
               (-> s
                   (update :attempts conj
                           (-> current
                               (assoc :status :complete
                                      :ended-at-monotonic-ns
                                      (:cursor-at-monotonic-ns current))
                               (dissoc :cursor-at-monotonic-ns :subphase)))
                   (assoc :current nil))
               s))))
  nil)

(defn abort!
  [state nano-time-fn reason]
  (when (instance? clojure.lang.IAtom state)
    (let [at (clock nano-time-fn)]
      (swap! state
             (fn [{:keys [current] :as s}]
               (if current
                 (-> s
                     (update :attempts conj
                             (-> current
                                 (update :measurements conj
                                         {:subphase (:subphase current)
                                          :started-at-monotonic-ns
                                          (:cursor-at-monotonic-ns current)
                                          :ended-at-monotonic-ns at
                                          :duration-ns
                                          (max 0 (- at (:cursor-at-monotonic-ns current)))})
                                 (assoc :status :typed-missing :reason reason
                                        :ended-at-monotonic-ns at)
                                 (dissoc :cursor-at-monotonic-ns :subphase)))
                     (assoc :current nil))
                 s)))))
  nil)

(defn receipt
  "Reconcile nested measurements with debugger-adjusted parent selection MS.
   Missing/incomplete attempts remain typed; no duration is invented."
  [state parent-active-ms]
  (let [{:keys [attempts current errors]} (when (instance? clojure.lang.IAtom state) @state)
        complete? (and (seq attempts) (nil? current)
                       (empty? errors)
                       (every? #(= :complete (:status %)) attempts))
        measurements (vec (mapcat :measurements attempts))
        measured-ns (reduce + 0 (map :duration-ns measurements))
        by-subphase-ns
        (reduce (fn [m {:keys [subphase duration-ns]}]
                  (update m subphase (fnil + 0) duration-ns))
                {} measurements)
        by-subphase-ms (reduce-kv (fn [m subphase duration-ns]
                                    (assoc m subphase (quot duration-ns 1000000)))
                                  {} by-subphase-ns)
        ;; Reconcile the values actually serialized, not a higher precision
        ;; total whose independently rounded children would not add back up.
        measured-ms (reduce + 0 (vals by-subphase-ms))
        parent? (and (integer? parent-active-ms) (<= 0 parent-active-ms))
        reconciles? (and complete? parent? (<= measured-ms parent-active-ms))]
    (cond-> {:schema schema
             :status (if reconciles? :complete :typed-missing)
             :clock :monotonic
             :unit :millisecond
             :parent-active-selection-ms (when parent? parent-active-ms)
             :measured-ms measured-ms
             :measured-ns measured-ns
             :subphase-timings-ms by-subphase-ms
             :errors (vec errors)
             :attempts (mapv #(select-keys % [:status :reason
                                               :started-at-monotonic-ns
                                               :ended-at-monotonic-ns]) attempts)}
      reconciles? (assoc :gap-ms (- parent-active-ms measured-ms))
      current (assoc :current-status :running)
      (seq errors) (assoc :reason :monotonic-clock-invalid)
      (and parent? (> measured-ms parent-active-ms))
      (assoc :reason :nested-duration-exceeds-parent)
      (not parent?) (assoc :reason :parent-selection-timing-missing)
      (and parent? (empty? errors) (not complete?)
           (<= measured-ms parent-active-ms))
      (assoc :reason :nested-measurement-incomplete))))
