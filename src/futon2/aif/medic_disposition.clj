(ns futon2.aif.medic-disposition
  "Pure correspondence for the Lean medic disposition model.

  `latest-entries`, `qualify`, `process`, `sweep`, and `effective-active`
  correspond respectively to HistoricalMisclassificationCorrection's
  latestEntries, medicQualified, medicProcess, medicSweep, and
  medicEffectiveActiveOccurrences at mathlib4 revision 18bb69d802d5766a7.
  `certified-sweep` corresponds to MedicSweepProvenance.AdmittedSweep with an
  empty initial correction log. Caller-supplied authority remains external;
  this namespace neither authenticates pins nor mutates the META ledger."
  (:require [clojure.string :as str]))

(def schema :wm/medic-disposition-v1)
(def correction-kinds #{:historical-misclassification :ordinary-author-refusal
                        :genuine-repair :cause-reattribution})
(def replay-results #{:producer-qualified-non-injury
                      :unsupported-author-refusal :done-at-base-revision :injury
                      :successful-close-non-injury
                      :ordinary-author-refusal-non-injury})
(def non-injury-replays
  #{:producer-qualified-non-injury :successful-close-non-injury
    :ordinary-author-refusal-non-injury})

(declare payload-valid?)

(def ^:private common-fields
  #{:kind :injury-id :entry-id :predecessor-entry-id :patch-id
    :reviewer :evaluator :evidence-pin})
(def ^:private historical-fields
  (into common-fields [:original-record-id :original-pin :classifier-revision :replay]))
(def ^:private author-refusal-fields
  (into common-fields [:original-record-id :original-pin :author-response-id
                       :author-response-pin :classifier-revision :replay]))
(def ^:private genuine-fields
  (into common-fields [:cause-record-id :cause-pin :acceptance-contract
                       :pre-replay-receipt :post-replay-receipt
                       :negative-control-receipt :source-revision
                       :original-acceptance-repaired?]))
(def ^:private reattribution-fields
  (into common-fields [:original-record-id :original-pin :rejected-cause-id
                       :replacement-injury-id :replacement-cause-id
                       :failure-kind :failure-stage]))

(defn occurrence [x] [(:injury-id x) (:entry-id x)])

(defn latest-entries
  "Latest entry per injury, retaining the order of each injury's last entry."
  [history]
  (reduce (fn [entries entry]
            (conj (filterv #(not= (:injury-id %) (:injury-id entry)) entries) entry))
          [] history))

(defn latest-for [history injury-id]
  (first (filter #(= injury-id (:injury-id %)) (latest-entries history))))

(defn- exact-keys? [m expected] (= expected (set (keys m))))
(defn- identity-scalar? [x] (and (string? x) (not (str/blank? x))))
(defn- present-scalars? [m ks] (every? #(identity-scalar? (get m %)) ks))

(defn- entry-valid? [entry]
  (and (map? entry)
       (exact-keys? entry #{:injury-id :entry-id :predecessor-entry-id
                            :cause-record-id :cause-pin :active?})
       (present-scalars? entry [:injury-id :entry-id :cause-record-id :cause-pin])
       (or (nil? (:predecessor-entry-id entry))
           (identity-scalar? (:predecessor-entry-id entry)))
       (boolean? (:active? entry))))

(defn validate-state
  "Validate the supplied model domain. Entry IDs must be globally unique and
  every non-nil predecessor must name an earlier entry for the same injury."
  [state]
  (let [history (:history state)
        corrections (:corrections state)]
    (cond
      (not (and (map? state) (exact-keys? state #{:history :corrections})))
      {:status :refused :reason :malformed-state}
      (not (and (vector? history) (vector? corrections)))
      {:status :refused :reason :malformed-state}
      (not-every? entry-valid? history)
      {:status :refused :reason :malformed-history-entry}
      (not= (count history) (count (set (map :entry-id history))))
      {:status :refused :reason :duplicate-entry-id}
      (not-every? payload-valid? corrections)
      {:status :refused :reason :malformed-correction}
      :else
      (loop [prior {} remaining history]
        (if-let [entry (first remaining)]
          (let [pred (:predecessor-entry-id entry)]
            (if (and (some? pred) (not= pred (get prior (:injury-id entry))))
              {:status :refused :reason :invalid-predecessor :entry entry}
              (recur (assoc prior (:injury-id entry) (:entry-id entry))
                     (next remaining))))
          {:status :valid})))))

(defn- payload-fields [kind]
  (case kind
    :historical-misclassification historical-fields
    :ordinary-author-refusal author-refusal-fields
    :genuine-repair genuine-fields
    :cause-reattribution reattribution-fields
    nil))

(defn payload-valid?
  "Structural model-domain validation; this does not authenticate any field."
  [payload]
  (let [kind (:kind payload)
        fields (payload-fields kind)]
    (and (map? payload) fields (exact-keys? payload fields)
         (present-scalars? payload (disj fields :kind :predecessor-entry-id
                                          :original-acceptance-repaired? :replay
                                          :failure-kind :failure-stage))
         (not= (:reviewer payload) (:evaluator payload))
         (case kind
           :historical-misclassification (contains? replay-results (:replay payload))
           :ordinary-author-refusal
           (= :ordinary-author-refusal-non-injury (:replay payload))
           :genuine-repair (boolean? (:original-acceptance-repaired? payload))
           :cause-reattribution
           (and (keyword? (:failure-kind payload))
                (keyword? (:failure-stage payload))
                (not= (:injury-id payload) (:replacement-injury-id payload))
                (not= (:rejected-cause-id payload) (:replacement-cause-id payload)))
           false))))

(defn- local-binding? [current candidate evidence]
  (and (= candidate evidence)
       (= (occurrence current) (occurrence candidate))
       (= (:predecessor-entry-id current) (:predecessor-entry-id candidate))
       (:active? current)
       (case (:kind candidate)
         :historical-misclassification
         (and (= (:cause-record-id current) (:original-record-id candidate))
              (= (:cause-pin current) (:original-pin candidate))
              (contains? non-injury-replays (:replay candidate)))
         :ordinary-author-refusal
         (and (= (:cause-record-id current) (:original-record-id candidate))
              (= (:cause-pin current) (:original-pin candidate))
              (= :ordinary-author-refusal-non-injury (:replay candidate)))
         :genuine-repair
         (and (= (:cause-record-id current) (:cause-record-id candidate))
              (= (:cause-pin current) (:cause-pin candidate))
              (true? (:original-acceptance-repaired? candidate)))
         :cause-reattribution
         (and (= (:cause-record-id current) (:original-record-id candidate))
              (= (:cause-pin current) (:original-pin candidate))
              (not= (:injury-id current) (:replacement-injury-id candidate))
              (not= (:rejected-cause-id candidate) (:replacement-cause-id candidate)))
         false)))

(defn qualify
  "Check structural binding before invoking supplied `authority?`. A false
  repaired verdict is present evidence that refuses; nil evidence is missing."
  [authority? current candidate evidence]
  (cond
    (nil? evidence) {:qualified? false :reason :missing-evidence}
    (not (and (payload-valid? candidate) (payload-valid? evidence)))
    {:qualified? false :reason :malformed-payload}
    (not (local-binding? current candidate evidence))
    {:qualified? false :reason :binding-mismatch}
    :else
    (try
      (if (true? (authority? current candidate evidence))
        {:qualified? true}
        {:qualified? false :reason :authority-rejected})
      (catch Throwable t
        {:qualified? false :reason :authority-error :detail (ex-message t)}))))

(defn process
  "Pure medicProcess correspondence. Returns retained request/evidence and the
  unchanged or appended state; it never rewrites history or prior corrections."
  [authority? state {:keys [candidate evidence] :as request}]
  (let [valid (validate-state state)]
    (if-not (= :valid (:status valid))
      (assoc valid :schema schema :request request :state state)
      (if-not (and (map? request) (= #{:candidate :evidence} (set (keys request))))
        {:schema schema :status :refused :reason :malformed-request
         :request request :state state}
        (if-let [current (latest-for (:history state) (:injury-id candidate))]
          (let [qualification (qualify authority? current candidate evidence)
                base {:schema schema :request request :current current
                      :qualification qualification}]
            (cond
              (not (:qualified? qualification))
              (assoc base :status :refused :reason (:reason qualification) :state state)
              (some #(= candidate %) (:corrections state))
              (assoc base :status :duplicate :state state)
              (some #(= (occurrence candidate) (occurrence %)) (:corrections state))
              (assoc base :status :conflict :state state)
              :else
              (assoc base :status :appended
                     :state (update state :corrections conj candidate))))
          {:schema schema :status :refused :reason :latest-occurrence-missing
           :request request :state state})))))

(defn sweep
  "Ordinary computed projection; results may include refusals/duplicates/conflicts."
  [authority? state requests]
  (reduce (fn [{:keys [state results]} request]
            (let [result (process authority? state request)]
              {:state (:state result) :results (conj results result)}))
          {:state state :results []} requests))

(defn certified-sweep
  "Empty-origin verified fold corresponding to AdmittedSweep. Certification is
  withheld unless state is valid, corrections start empty, and every request
  was actually appended by `process`."
  [authority? state requests]
  (let [request-sequence? (or (vector? requests) (list? requests))
        valid (validate-state state)]
    (cond
      (not request-sequence?)
      {:schema schema :status :uncertified :reason :request-sequence-required
       :certified? false :state state :results []}
      (not= :valid (:status valid)) (assoc valid :schema schema :certified? false)
      (seq (:corrections state))
      {:schema schema :status :uncertified :reason :preloaded-corrections
       :certified? false :state state :results []}
      :else
      (let [{:keys [results] :as folded} (sweep authority? state requests)
            all-appended? (every? #(= :appended (:status %)) results)]
        (assoc folded :schema schema :status (if all-appended? :certified :uncertified)
               :certified? all-appended?
               :reason (when-not all-appended? :non-appended-step))))))

(defn effective-active
  "Ordinary exact projection; this computation alone is not provenance."
  [state]
  (let [corrected (set (map occurrence (:corrections state)))]
    (->> (latest-entries (:history state))
         (filter :active?)
         (remove #(contains? corrected (occurrence %)))
         (mapv occurrence))))

(defn certified-clear-all?
  "Recompute from explicit authority, origin, and requests. The one-argument
  form rejects untrusted result maps retained by callers of the earlier API."
  ([_untrusted-result] false)
  ([authority? origin requests]
   (let [result (certified-sweep authority? origin requests)]
     (and (:certified? result)
          (empty? (effective-active (:state result)))))))
