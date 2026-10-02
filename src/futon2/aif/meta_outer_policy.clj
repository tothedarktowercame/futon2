(ns futon2.aif.meta-outer-policy
  "Pure validation and evaluation of the META outer-policy contract.

  This boundary consumes only M/E/T/A field rows and explicit prediction,
  preference, and information-gain models. Tactical cascade material is not an
  accepted input."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.core-efe :as core-efe]
            [futon2.aif.epistemic-value :as eig]))

(def required-slots
  #{:task-kind :target :next-move :resource-envelope
    :evidence-channel :stopping-rule})

(def task-kinds #{:mission :excursion :ticket :algorithm})
(def next-moves
  {:mission #{:advance :unblock :close}
   :excursion #{:advance :unblock :close}
   :ticket #{:advance :unblock :close}
   :algorithm #{:advance :unblock :close :repair :run-algorithm}})
(def stopping-rules
  #{:closed :grounded-progress :typed-blocker :budget-exhausted :debugger-stop})
(def checkable-locator-classes #{:C3 :C4 :C5 :C6 :C8})

(defn- sha256? [x]
  (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))

(defn- source-pin? [source]
  (and (map? source) (string? (:path source)) (not (str/blank? (:path source)))
       (sha256? (:sha256 source))))

(defn- locator? [locator]
  (and (map? locator)
       (or (contains? checkable-locator-classes (:class locator))
           (and (keyword? (:kind locator))
                (or (pos-int? (:line locator)) (some? (:id locator)))))))

(defn- candidate-errors [candidate injured?]
  (let [slots (:slots candidate)
        kind (:task-kind slots)
        envelope (:resource-envelope slots)
        evidence (:evidence-channel slots)]
    (cond-> []
      (not (map? candidate)) (conj :candidate-not-map)
      (nil? (:id candidate)) (conj :id-missing)
      (not= required-slots (set/intersection required-slots (set (keys slots))))
      (conj :required-slot-missing)
      (not (contains? task-kinds kind)) (conj :task-kind-invalid)
      (not (and (string? (:target slots)) (not (str/blank? (:target slots)))))
      (conj :target-invalid)
      (not (contains? (get next-moves kind #{}) (:next-move slots)))
      (conj :next-move-incompatible)
      (not (and (pos-int? (:time-budget-ms envelope))
                (pos-int? (:token-budget envelope))
                (string? (:author-seat envelope))
                (string? (:reviewer-seat envelope))))
      (conj :resource-envelope-invalid)
      (not (source-pin? (:source evidence))) (conj :source-pin-invalid)
      (not (locator? (:locator evidence))) (conj :locator-invalid)
      (not (contains? stopping-rules (:stopping-rule slots)))
      (conj :stopping-rule-invalid)
      (and injured? (= :algorithm kind)
           (not (and (map? (:rearm-observation candidate))
                     (seq (:requires (:rearm-observation candidate))))))
      (conj :rearm-evidence-missing))))

(defn- g-input-errors [candidate]
  (let [{:keys [means variances preference-means preference-variances
                weights information-model source-pin]} (:g-input candidate)
        n (count means)]
    (cond-> []
      (not (and (pos? n) (every? number? means)
                (= n (count variances) (count preference-means)
                   (count preference-variances))
                (every? #(and (number? %) (pos? (double %)))
                        (concat variances preference-variances))
                (or (nil? weights)
                    (and (= n (count weights)) (every? number? weights)))))
      (conj :gaussian-model-incomplete)
      (not (map? information-model)) (conj :information-model-missing)
      (not (source-pin? source-pin)) (conj :g-source-pin-invalid))))

(defn- typed-refusal [reason details]
  {:schema :wm/meta-outer-policy-receipt-v1
   :status :refused :reason reason :details details})

(defn- contract-errors [contract]
  (let [patterns (:patterns contract)
        slots (:slots contract)
        precedence (:precedence contract)
        allowed-slot-types
        {:task-kind :enum :target :current-meta-item-id :next-move :enum
         :resource-envelope :resource-envelope
         :evidence-channel :closure-or-progress-observer :stopping-rule :enum}]
    (cond-> []
      (not (and (keyword? (:id contract)) (vector? patterns) (seq patterns)
                (= (count patterns) (count (set patterns)))
                (every? keyword? patterns))) (conj :pattern-identities-invalid)
      (not= required-slots (set (keys slots))) (conj :slot-declarations-incomplete)
      (some (fn [[slot expected]]
              (let [spec (get slots slot)]
                (or (not= true (:required spec)) (not= expected (:type spec)))))
            allowed-slot-types) (conj :slot-types-invalid)
      (not (and (= #{:time-budget-ms :token-budget :author-seat :reviewer-seat}
                   (set (keys (get-in slots [:resource-envelope :fields]))))
                (= #{:source :locator}
                   (set (keys (get-in slots [:evidence-channel :fields]))))))
      (conj :slot-fields-invalid)
      (not (and (vector? precedence)
                (every? #(and (vector? %) (= 2 (count %))
                              (every? (set patterns) %)) precedence)))
      (conj :precedence-invalid)
      (not= :argmin-G (get-in contract [:selection :law]))
      (conj :selection-law-invalid)
      (not= :refuse-not-zero (get-in contract [:generative-model :missing-term-policy]))
      (conj :missing-term-policy-invalid))))

(defn evaluate
  "Validate and evaluate an explicit, replayable META field.

  Healthy support admits ordinary M/E/T/A work. Injury restricts support to an
  exact capability-matching repair algorithm. G uses the canonical Gaussian
  EFE core plus the canonical Bayes-coherent EIG kernel."
  [{:keys [contract contract-source observation candidates] :as input}]
  (cond
    (not= :meta/outer-policy-cascade-v1 (:schema contract))
    (typed-refusal :contract-schema-invalid {:schema (:schema contract)})

    (not (source-pin? contract-source))
    (typed-refusal :contract-source-unpinned {:contract-source contract-source})

    (seq (contract-errors contract))
    (typed-refusal :contract-invalid {:errors (contract-errors contract)})

    (some #(contains? input %) [:tactical-patterns :tactical-candidates
                                :tactical-precedence :tactical-G])
    (typed-refusal :outer-boundary-violated
                   {:forbidden-present (vec (filter #(contains? input %)
                                                   [:tactical-patterns :tactical-candidates
                                                    :tactical-precedence :tactical-G]))})

    (not (vector? candidates))
    (typed-refusal :candidate-field-invalid {:expected :vector})

    (not= (count candidates) (count (set (map :id candidates))))
    (typed-refusal :candidate-ids-not-unique {:ids (mapv :id candidates)})

    :else
    (let [injured? (= :active (:injury-observation observation))
          malformed (into {} (keep (fn [c] (when-let [es (seq (candidate-errors c injured?))]
                                             [(:id c) (vec es)]))) candidates)
          support (if injured?
                    (filterv #(and (= :algorithm (get-in % [:slots :task-kind]))
                                   (= (:injured-capability observation)
                                      (:repairs-capability %))) candidates)
                    candidates)]
      (cond
        (seq malformed)
        (typed-refusal :candidate-invalid {:candidate-errors malformed})

        (and injured? (not (source-pin? (:source-pin observation))))
        (typed-refusal :injury-source-unpinned {:source-pin (:source-pin observation)})

        (empty? support)
        (typed-refusal (if injured? :no-matching-self-heal-algorithm
                           :no-admitted-meta-policy)
                       {:injured-capability (:injured-capability observation)
                        :field-census (frequencies (map #(get-in % [:slots :task-kind]) candidates))})

        :else
        (let [g-errors (into {} (keep (fn [c] (when-let [es (seq (g-input-errors c))]
                                                [(:id c) (vec es)]))) support)]
          (if (seq g-errors)
            (typed-refusal :g-inputs-incomplete {:candidate-errors g-errors})
            (try
              (let [scored
                    (mapv (fn [candidate]
                            (let [{:keys [means variances preference-means
                                          preference-variances weights information-model]}
                                  (:g-input candidate)
                                  efe (core-efe/g-efe means variances preference-means
                                                      preference-variances {:weights weights})
                                  information (eig/expected-information-gain information-model)]
                              (assoc candidate :g-terms {:risk (:risk efe)
                                                        :ambiguity (:ambiguity efe)
                                                        :epistemic-value information}
                                               :g (- (:g-efe efe) information))))
                          support)
                    ordered (vec (sort-by (juxt :g (comp str :id)) scored))
                    selected (first ordered)]
                {:schema :wm/meta-outer-policy-receipt-v1
                 :status :selected
                 :arm (if injured? :self-heal :healthy)
                 :contract-source contract-source
                 :field-census (frequencies (map #(get-in % [:slots :task-kind]) candidates))
                 :typed-exclusions
                 (into {} (for [c candidates :when (not (some #{c} support))]
                            [(:id c) :machine-injury-active-or-capability-mismatch]))
                 :candidate-slot-fillings (mapv #(select-keys % [:id :slots]) candidates)
                 :source-pins (into {} (map (juxt :id #(get-in % [:slots :evidence-channel :source]))) candidates)
                 :g-terms (into {} (map (juxt :id :g-terms)) scored)
                 :g (into {} (map (juxt :id :g)) scored)
                 :selected-policy (:id selected)
                 :nearest-alternative (:id (second ordered))
                 :selection-reason (if (= 1 (count ordered))
                                     :singleton-admitted-support :minimum-canonical-G)})
              (catch clojure.lang.ExceptionInfo throwable
                (typed-refusal :g-model-refused (ex-data throwable))))))))))
