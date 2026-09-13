(ns futon2.aif.machine-budget-mapping
  "Pure E1 boundary from the complete ordered R6 candidate support to the
   canonical R11 ranked-field arbiter.  Occurrence identity, not semantic
   action equality, is the mapping key.  Cost, utility, membership and budget
   values are accepted only with pinned independent authority records."
  (:require [clojure.string :as str]
            [futon2.aif.hierarchical-budget-adapter :as r11]))

(def schema-version :wm/r6-r11-mapping-v1)

(def ^:private sha256-pattern #"[0-9a-f]{64}")
(def ^:private authority-kinds #{:declared-source :supported-transformation})

(defn- refuse! [kind message data]
  (throw (ex-info message (assoc data :refusal kind))))

(defn- nonblank? [x]
  (and (string? x) (not (str/blank? x))))

(defn- pin? [x]
  (and (map? x)
       (nonblank? (:artifact/path x))
       (string? (:artifact/sha256 x))
       (re-matches sha256-pattern (:artifact/sha256 x))
       (some? (:source/id x))
       (nonblank? (:source/revision x))))

(defn- authority! [label record expected-binding]
  (when-not (map? record)
    (refuse! :r6-r11/authority-missing "Mapping authority is missing"
             {:authority-label label}))
  (when-not (contains? authority-kinds (:authority record))
    (refuse! :r6-r11/unknown-authority "Unknown mapping authority"
             {:authority-label label :authority (:authority record)}))
  (when-not (pin? record)
    (refuse! :r6-r11/authority-unpinned "Mapping authority lacks an artifact pin"
             {:authority-label label :authority record}))
  (when-not (= expected-binding (:binding record))
    (refuse! :r6-r11/cross-run-authority
             "Authority is not bound to this model/run/tick"
             {:authority-label label :expected-binding expected-binding
              :actual-binding (:binding record)}))
  (when (= :supported-transformation (:authority record))
    (let [t (:transformation record)]
      (when-not (and (map? t) (some? (:id t)) (nonblank? (:revision t))
                     (vector? (:input-pins t)) (seq (:input-pins t))
                     (every? pin? (:input-pins t)))
        (refuse! :r6-r11/transformation-unsupported
                 "A supported transformation must name its revision and pinned inputs"
                 {:authority-label label :authority record}))))
  record)

(defn- finite-number? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn- positive-number? [x]
  (and (finite-number? x) (pos? (double x))))

(defn- nonnegative-number? [x]
  (and (finite-number? x) (not (neg? (double x)))))

(defn- identity! [input]
  (when-not (and (some? (:model/id input)) (some? (:model/revision input))
                 (some? (:run/id input)) (integer? (:tick/index input))
                 (not (neg? (:tick/index input))))
    (refuse! :r6-r11/identity-missing "Model/run/tick identity is incomplete"
             {:identity (select-keys input [:model/id :model/revision :run/id :tick/index])})))

(defn- candidates! [ranked-support]
  (when-not (and (vector? ranked-support) (seq ranked-support))
    (refuse! :r6-r11/support-absent "R6 ranked support must be a non-empty vector"
             {:ranked-support ranked-support}))
  (doseq [[idx candidate] (map-indexed vector ranked-support)]
    (when-not (map? candidate)
      (refuse! :r6-r11/candidate-malformed "Candidate occurrence must be a map"
               {:source-index idx :candidate candidate}))
    (when-not (contains? candidate :candidate/id)
      (refuse! :r6-r11/candidate-identity-missing "Candidate occurrence id is missing"
               {:source-index idx :candidate candidate}))
    (when-not (contains? candidate :action)
      (refuse! :r6-r11/action-missing "Candidate action bytes are missing"
               {:source-index idx :candidate/id (:candidate/id candidate)}))
    (when-not (= (inc idx) (:rank candidate))
      (refuse! :r6-r11/support-reordered "Rank must equal ordered support position"
               {:source-index idx :candidate/id (:candidate/id candidate)
                :expected-rank (inc idx) :actual-rank (:rank candidate)})))
  (let [ids (mapv :candidate/id ranked-support)]
    (when-not (= (count ids) (count (distinct ids)))
      (refuse! :r6-r11/duplicate-occurrence-id
               "Distinct occurrences require distinct candidate ids" {:candidate/ids ids})))
  ranked-support)

(defn- exact-keyset! [label expected actual]
  (when-not (= (set expected) (set (keys actual)))
    (refuse! :r6-r11/support-mismatch "Authority values do not cover exactly the R6 support"
             {:authority-label label :expected-ids (vec expected)
              :actual-ids (vec (keys actual))})))

(defn- values! [label authority binding ids predicate value-description]
  (authority! label authority binding)
  (let [values (:values authority)]
    (when-not (map? values)
      (refuse! :r6-r11/authority-values-missing "Authority has no value map"
               {:authority-label label}))
    (exact-keyset! label ids values)
    (doseq [[id value] values]
      (when-not (predicate value)
        (refuse! :r6-r11/authority-value-invalid "Authority value has the wrong shape"
                 {:authority-label label :candidate/id id :value value
                  :expected value-description})))
    values))

(defn- budgets! [authority binding field-ids]
  (authority! :budgets authority binding)
  (let [{:keys [root fields]} (:values authority)]
    (when-not (positive-number? root)
      (refuse! :r6-r11/unknown-budget "Root budget must be finite and positive"
               {:budget root}))
    (when-not (map? fields)
      (refuse! :r6-r11/unknown-budget "Field budgets are missing" {:fields fields}))
    (when-not (= (set field-ids) (set (keys fields)))
      (refuse! :r6-r11/unknown-budget
               "Field budgets do not cover exactly the mapped fields"
               {:expected-field-ids field-ids :actual-field-ids (vec (keys fields))}))
    (doseq [[field-id budget] fields]
      (when-not (positive-number? budget)
        (refuse! :r6-r11/unknown-budget "Field budget must be finite and positive"
                 {:field/id field-id :budget budget})))
    {:root root :fields fields}))

(defn- checked-output [{:keys [ranked-support authorities] :as input}]
  (when-not (= schema-version (:schema/version input))
    (refuse! :r6-r11/schema-unsupported "Unsupported R6/R11 mapping schema"
             {:schema/version (:schema/version input) :supported schema-version}))
  (identity! input)
  (candidates! ranked-support)
  (when (or (contains? input :Q-pi) (contains? input :policy-posterior)
            (contains? authorities :policy-posterior))
    (refuse! :r6-r11/posterior-substitution
             "Q(pi) is not an R11 utility authority" {}))
  (let [ids (mapv :candidate/id ranked-support)
        binding (select-keys input [:model/id :model/revision :run/id :tick/index])
        memberships (values! :field-membership (:field-membership authorities) binding
                             ids keyword? :keyword-field-id)
        field-ids (vec (distinct (map memberships ids)))
        costs (values! :costs (:costs authorities) binding ids nonnegative-number?
                       :finite-nonnegative-number)
        utilities (values! :utilities (:utilities authorities) binding ids finite-number?
                           :finite-number)
        budgets (budgets! (:budgets authorities) binding field-ids)
        by-id (into {} (map (juxt :candidate/id identity) ranked-support))
        fields (mapv (fn [field-id]
                       {:id field-id
                        :budget (get-in budgets [:fields field-id])
                        :proposals
                        (->> ids
                             (filter #(= field-id (memberships %)))
                             (mapv (fn [id]
                                     (let [candidate (by-id id)]
                                       {:id id
                                        :rank (:rank candidate)
                                        :action (:action candidate)
                                        :cost (costs id)
                                        :utility (utilities id)}))))})
                     field-ids)
        request {:shared-budget (:root budgets)
                 :root-id :r11/shared
                 :context {:model/id (:model/id input)
                           :model/revision (:model/revision input)
                           :run/id (:run/id input)
                           :tick/index (:tick/index input)
                           :producer/declaration :machinePolicySet
                           :consumer/declaration :hierarchical-budget/arbitrate}
                 :fields fields}
        response (r11/select-ranked-proposal-fields request)
        selected-ids (:selected-ids response)
        accounting (mapv (fn [candidate]
                           (let [id (:candidate/id candidate)]
                             {:candidate/id id
                              :rank (:rank candidate)
                              :action (:action candidate)
                              :proposal/id id
                              :field/id (memberships id)
                              :disposition (if (contains? selected-ids id)
                                             :selected :rejected)}))
                         ranked-support)]
    (when-not (= (set ids) (set (map :candidate/id accounting)))
      (refuse! :r6-r11/accounting-incomplete "Candidate accounting is incomplete" {}))
    {:schema/version schema-version
     :scope (:scope input)
     :identity (select-keys input [:model/id :model/revision :run/id :tick/index])
     :producer {:declaration :machinePolicySet :field :pi}
     :consumer {:function 'futon2.aif.hierarchical-budget-adapter/select-ranked-proposal-fields
                :declaration :hierarchical-budget/arbitrate}
     :ordered-support ranked-support
     :authorities authorities
     :bijection accounting
     :request request
     :response response
     :accounting accounting
     :complete-accounting? (= (count ids) (count accounting))}))

(defn map-ranked-support
  "Map one complete, pinned R6 ranked support into the canonical R11 request.
   Returns request, response, bijection, full accounting, and a replay receipt."
  [input]
  (let [output (checked-output input)]
    (assoc output :replay/receipt {:schema/version schema-version
                                   :input input
                                   :output output})))

(defn replay
  "Recompute an E1 receipt and compare every retained field."
  [{receipt-version :schema/version :keys [input output]}]
  (when-not (= receipt-version schema-version)
    (refuse! :r6-r11/replay-schema-unsupported "Unsupported replay schema"
             {:schema/version receipt-version}))
  (let [actual (checked-output input)]
    {:replay/identical? (= output actual)
     :replay/expected output
     :replay/actual actual}))
