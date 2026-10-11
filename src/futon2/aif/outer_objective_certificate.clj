(ns futon2.aif.outer-objective-certificate
  "Pure, unwired certificate model for an externally registered outer objective.

  This is not cascade G and does not replace the live pairwise selector. An
  evaluator can only be called through a sealed registry created by `registry`;
  objective requests and certificates contain no executable code."
  (:require [clojure.set :as set]))

(def registration-schema :wm/outer-objective-registration-v1)
(def certificate-schema :wm/outer-objective-certificate-v1)

(defrecord ^:private SealedRegistry [entries])

(defn- registration-key [x]
  [(:objective/identity x) (:objective/version x)])

(defn registry [registrations]
  (let [required [:objective/identity :objective/version :registration/token
                  :registration/authority :implementation/pin :evaluate]
        valid? (fn [r]
                 (and (= registration-schema (:schema r))
                      (every? #(some? (get r %)) required)
                      (pos-int? (:objective/version r))
                      (not (zero? (:registration/authority r)))
                      (ifn? (:evaluate r))))
        keys (map registration-key registrations)]
    (when (and (every? valid? registrations)
               (= (count keys) (count (distinct keys))))
      (->SealedRegistry (into {} (map (juxt registration-key identity))
                              registrations)))))

(defn public-registration [registration]
  (dissoc registration :evaluate))

(defn- registered [sealed objective]
  (when (instance? SealedRegistry sealed)
    (let [r (get (:entries sealed) (registration-key objective))]
      (when (= (public-registration r) objective) r))))

(defn channel-dependencies [{:channel/keys [id observation evidence value-digest]}]
  #{id observation evidence value-digest})

(defn required-dependencies [objective input]
  (reduce set/union #{(:registration/token objective)}
          (map channel-dependencies (:task/channels input))))

(defn- unique-by? [f xs]
  (= (count xs) (count (distinct (map f xs)))))

(defn task-input-errors [objective input]
  (let [channels (:task/channels input)
        channel-complete? (fn [channel]
                            (every? #(some? (get channel %))
                                    [:channel/id :channel/observation
                                     :channel/evidence :channel/value-digest]))]
    (cond-> []
    (not (some? (:task/id input))) (conj :task-id-missing)
    (not (vector? channels)) (conj :channels-not-vector)
    (and (vector? channels) (not-every? channel-complete? channels))
    (conj :channel-carrier-incomplete)
    (and (vector? channels) (not (unique-by? :channel/id channels)))
    (conj :duplicate-channel)
    (not= (:task/dependencies input) (required-dependencies objective input))
    (conj :dependency-closure-mismatch)
    (not (nat-int? (:task/freshness-epoch input)))
    (conj :freshness-epoch-invalid))))

(defn evaluate-task
  "Invoke the matched registered evaluator exactly once and retain its inputs."
  [sealed selector-authority objective input]
  (let [r (registered sealed objective)
        errors (cond-> (task-input-errors objective input)
                 (nil? r) (conj :objective-not-authorized)
                 (or (zero? selector-authority)
                     (= selector-authority (:registration/authority objective)))
                 (conj :selector-authority-not-separated))]
    (if (seq errors)
      {:status :refused :errors errors}
      (let [key ((:evaluate r) (:task/channels input))]
        {:status :valid
         :schema certificate-schema
         :input input
         :objective (dissoc objective :schema)
         :selector/authority selector-authority
         :entry {:task/id (:task/id input)
                 :observation/digest
                 (mapv (juxt :channel/observation :channel/value-digest)
                       (:task/channels input))
                 :evidence/digest (mapv :channel/evidence (:task/channels input))
                 :ranking-law (:objective/identity objective)
                 :ranking-version (:objective/version objective)
                 :dependencies (:task/dependencies input)
                 :comparison-key key
                 :freshness-epoch (:task/freshness-epoch input)}}))))

(defn evaluation-valid? [sealed selector-authority objective certificate]
  (and (= :valid (:status certificate))
       (= certificate (evaluate-task sealed selector-authority objective
                                     (:input certificate)))))

(defn- task-map [evaluations]
  (when (unique-by? #(get-in % [:entry :task/id]) evaluations)
    (into {} (map (juxt #(get-in % [:entry :task/id]) identity)) evaluations)))

(defn affected? [changed evaluation]
  (boolean (seq (set/intersection changed
                                  (get-in evaluation [:entry :dependencies])))))

(defn refresh-errors
  [{:keys [prior current retained invalidated recomputed removed delta
           current-open-tasks]}]
  (let [pm (task-map prior) cm (task-map current) rm (task-map retained)
        im (task-map invalidated) xm (task-map recomputed)
        prior-tasks (set (keys pm)) current-tasks (set (keys cm))
        retained-tasks (set (keys rm)) invalidated-tasks (set (keys im))
        recomputed-tasks (set (keys xm))
        changed (set (:changed-observations delta))
        added (set (:added-tasks delta)) removed* (set (:removed-tasks delta))
        actual-removed (set/intersection removed* prior-tasks)
        expected-invalidated (set (for [[task e] pm
                                        :when (and (not (contains? actual-removed task))
                                                   (affected? changed e))]
                                    task))
        expected-retained (set/difference prior-tasks actual-removed
                                          expected-invalidated)]
    (cond-> []
      (some nil? [pm cm rm im xm]) (conj :duplicate-task)
      (not= (set removed) actual-removed) (conj :removed-partition-mismatch)
      (not= retained-tasks expected-retained) (conj :retained-partition-mismatch)
      (not= invalidated-tasks expected-invalidated) (conj :invalidated-partition-mismatch)
      (not (every? #(= (get pm %) (get rm %)) retained-tasks))
      (conj :retained-entry-changed)
      (not= recomputed-tasks (set/union invalidated-tasks added))
      (conj :recomputed-population-mismatch)
      (not= current-tasks (set/union retained-tasks recomputed-tasks))
      (conj :current-population-mismatch)
      (not (every? #(= (get xm %) (get cm %)) recomputed-tasks))
      (conj :recomputed-current-mismatch)
      (not= current-tasks (set current-open-tasks))
      (conj :open-task-population-mismatch)
      (seq (set/intersection actual-removed current-tasks))
      (conj :removed-task-current)
      (seq (set/intersection added prior-tasks)) (conj :added-task-was-prior)
      (seq (set/intersection added removed*)) (conj :added-task-also-removed))))

(defn refresh-valid? [refresh] (empty? (refresh-errors refresh)))

(defn produce-refresh
  "Produce the strict refresh partition from prior receipts, a world delta,
  and current inputs keyed by task. Only invalidated and added tasks are
  evaluated; unchanged disjoint receipts are retained byte/field-identically."
  [sealed selector-authority objective prior delta current-inputs current-open-tasks]
  (let [prior-map (task-map prior)
        prior-tasks (set (keys prior-map))
        removed (set/intersection prior-tasks (set (:removed-tasks delta)))
        survivors (remove #(contains? removed (get-in % [:entry :task/id])) prior)
        invalidated (filterv #(affected? (set (:changed-observations delta)) %) survivors)
        invalidated-tasks (set (map #(get-in % [:entry :task/id]) invalidated))
        retained (filterv #(not (contains? invalidated-tasks
                                           (get-in % [:entry :task/id]))) survivors)
        recompute-tasks (set/union invalidated-tasks (set (:added-tasks delta)))
        recomputed (mapv #(evaluate-task sealed selector-authority objective
                                       (get current-inputs %))
                         (sort recompute-tasks))]
    (if (or (nil? prior-map)
            (some #(not= :valid (:status %)) recomputed))
      {:status :refused :reason :outer-refresh-input-invalid
       :evaluations recomputed}
      (let [refresh {:prior prior :current (into retained recomputed)
                     :retained retained :invalidated invalidated
                     :recomputed recomputed :removed removed :delta delta
                     :current-open-tasks (set current-open-tasks)}
            errors (refresh-errors refresh)]
        (if (seq errors)
          {:status :refused :reason :outer-refresh-certificate-invalid
           :errors errors :refresh refresh}
          {:status :valid :refresh refresh
           :evaluations (:current refresh)})))))

(defn frontier-valid? [sealed selector-authority objective frontier]
  (and (refresh-valid? (:refresh frontier))
       (= (:evaluations frontier) (get-in frontier [:refresh :current]))
       (every? #(evaluation-valid? sealed selector-authority objective %)
               (:evaluations frontier))))

(defn select-minimum
  "Select the fully refreshed minimum by comparison key then stable task id."
  [sealed selector-authority objective frontier]
  (if-not (frontier-valid? sealed selector-authority objective frontier)
    {:status :refused :reason :outer-frontier-certificate-invalid}
    (if-let [selected (first (sort-by (juxt #(get-in % [:entry :comparison-key])
                                             #(get-in % [:entry :task/id]))
                                       (:evaluations frontier)))]
      {:status :selected :selected (get-in selected [:entry :task/id])
       :comparison-key (get-in selected [:entry :comparison-key])
       :tie-law :comparison-key-then-stable-task-id :evaluation selected}
      {:status :refused :reason :empty-current-frontier})))
