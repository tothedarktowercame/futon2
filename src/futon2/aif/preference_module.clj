(ns futon2.aif.preference-module
  "Versioned local C modules. Exact rational distributions mirror Lean's
   ProbabilityKernel Unit (Outcome Obs). Diagnostics never invent Q or masses."
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(def contract :preference-module/v1)
(def vertices #{:nouns :verbs :organization :evidence})

(defn- require! [ok reason data]
  (when-not ok (throw (ex-info (name reason) (assoc data :reason reason)))))

(defn- named? [x] (or (keyword? x) (and (string? x) (not (str/blank? x)))))
(defn- exact? [x] (or (integer? x) (ratio? x)))

(defn validate-distribution
  "No rounding, renormalising, inferred support or smoothing. Zero mass entries
   remain named. Outside declared support the represented mass function is zero."
  [{:keys [support mass] :as distribution}]
  (require! (and (vector? support) (seq support)
                 (= (count support) (count (set support))))
            :invalid-support {})
  (require! (every? #(and (vector? %) (= 2 (count %))
                          (contains? vertices (first %)) (named? (second %))) support)
            :invalid-tagged-outcome {:support support})
  (require! (and (map? mass) (= (set support) (set (keys mass))))
            :mass-support-mismatch {})
  (require! (every? #(and (exact? %) (<= 0 %)) (vals mass))
            :invalid-exact-mass {})
  (require! (= 1 (reduce + (vals mass))) :not-normalised {})
  distribution)

(defn validate-module
  [{:keys [schema id version context entries] :as module}]
  (require! (= contract schema) :unsupported-module-version {:schema schema})
  (require! (and (named? id) (pos-int? version) (named? context))
            :missing-module-identity {})
  (require! (and (vector? entries) (seq entries)
                 (= (count entries) (count (set (map :id entries)))))
            :invalid-entries {})
  (require! (contains? #{:candidate :accepted} (get module :mode :candidate))
            :invalid-review-mode {})
  (doseq [{:keys [id version criterion source kind distribution] :as entry} entries]
    (require! (and (named? id) (pos-int? version) (named? criterion)
                   (map? source) (every? named? (map source [:ref :basis :status])))
              :missing-entry-provenance {:entry id})
    (when (= :accepted (:mode module))
      (require! (= :accepted (:status source)) :unaccepted-preference {:entry id}))
    (case kind
      :finite (validate-distribution distribution)
      :soft-binary (require! (and (named? (:parameter entry))
                                  (contains? (disj vertices :evidence) (:vertex entry)))
                             :invalid-soft-family {:entry id})
      (require! false :unsupported-outcome-type {:entry id :kind kind})))
  module)

(defn instantiate
  "Choose an explicitly supplied exact parameter for one symbolic entry.
   The caller supplies its authority; this operation never picks a value."
  [entry p authority]
  (require! (= :soft-binary (:kind entry)) :not-soft-binary {})
  (require! (and (exact? p) (< 1/2 p 1) (named? authority))
            :unlicensed-parameter {:p p})
  (let [yes [(:vertex entry) :satisfied] no [(:vertex entry) :unsatisfied]]
    (-> entry
        (assoc :kind :finite :distribution
               (validate-distribution {:support [yes no] :mass {yes p no (- 1 p)}}))
        (assoc-in [:source :parameter-authority] authority))))

(defn risk
  "KL(Q||C), after exact validation. Returns refusal where Lean requires
   positive C on predictive support; named zero Q entries contribute nothing."
  [c q]
  (validate-distribution c)
  (validate-distribution q)
  (require! (set/subset? (set (:support q)) (set (:support c)))
            :prediction-domain-mismatch {})
  (let [positive (filter #(pos? (get (:mass q) %)) (:support q))
        forbidden (filter #(zero? (get (:mass c) %)) positive)]
    (if (seq forbidden)
      {:status :refused :reason :positive-prediction-at-zero-preference
       :outcomes (vec forbidden)}
      {:status :computed
       :risk-nats
       (reduce (fn [r o]
                 (let [qv (get (:mass q) o) cv (get (:mass c) o)
                       term (* (double qv) (- (Math/log (double qv))
                                               (Math/log (double cv))))]
                   (require! (Double/isFinite term) :numeric-range-exceeded {:outcome o})
                   (+ r term))) 0.0 positive)})))

(defn assess
  "Readings keyed by entry id pin :version and :criterion. Status is :observed,
   :predicted, :unknown, :inadmissible or :not-applicable. Unknowns are not mass."
  [module readings]
  (validate-module module)
  (require! (map? readings) :invalid-readings {})
  (require! (set/subset? (set (keys readings)) (set (map :id (:entries module))))
            :unknown-preference-id {})
  {:module (select-keys module [:schema :id :version :context])
   :aggregation :not-declared
   :entries
   (mapv
    (fn [{:keys [id version criterion kind distribution parameter] :as entry}]
      (if-let [reading (get readings id)]
        (do
          (require! (and (= version (:version reading)) (= criterion (:criterion reading))
                         (named? (:evidence reading)))
                    :incompatible-assessment {:entry id})
          (case (:status reading)
            (:unknown :inadmissible :not-applicable)
            (do (require! (and (named? (:reason reading)) (not (contains? reading :value))
                               (not (contains? reading :distribution)))
                          :invalid-unobserved-reading {:entry id})
                (assoc reading :id id))
            (:observed :predicted)
            (let [result
                  (if (= :soft-binary kind)
                    (do (require! (and (= :observed (:status reading))
                                       (boolean? (:value reading)))
                                  :symbolic-family-needs-observed-boolean {:entry id})
                        {:status :symbolic :parameter parameter :domain "1/2 < p < 1"
                         :risk-expression (if (:value reading) "-log(p)" "-log(1-p)")})
                    (risk distribution (:distribution reading)))]
              (merge {:id id :version version :criterion criterion
                      :reading reading :source (:source entry)} result))
            (require! false :invalid-assessment-status {:entry id})))
        {:id id :version version :status :unknown :reason :no-reading}))
    (:entries module))})

(defn compare-satisfaction
  "Compare matching versioned boolean readings; never resolve cross-entry tradeoffs."
  [module before after]
  (require! (every? #(= :soft-binary (:kind %)) (:entries module))
            :boolean-comparison-requires-soft-family {})
  (assess module before)
  (assess module after)
  (let [ids (map :id (:entries module))
        changed-unknown? (some (fn [id]
                                (and (not= (get before id) (get after id))
                                     (not (and (= :observed (get-in before [id :status]))
                                               (= :observed (get-in after [id :status])))))) ids)
        pairs (for [id ids
                    :let [b (get-in before [id :value]) a (get-in after [id :value])]
                    :when (and (boolean? b) (boolean? a))] [b a])
        up (some #(= [false true] %) pairs)
        down (some #(= [true false] %) pairs)]
    (cond changed-unknown? :unresolved-measurement
          (and up down) :tradeoff-unranked
          up :improves down :worsens :else :unchanged)))

(defn rank-local-risk
  "Rank candidates on ONE finite preference entry. Unknown/refused candidates
   leave selection unresolved; no aggregation, ambiguity or silent exclusion."
  [module entry-id candidates]
  (validate-module module)
  (let [entry (first (filter #(= entry-id (:id %)) (:entries module)))]
    (require! (= :finite (:kind entry)) :finite-preference-required {:entry entry-id})
    (require! (and (vector? candidates)
                   (every? #(named? (:id %)) candidates)
                   (= (count candidates) (count (set (map :id candidates)))))
              :invalid-candidates {})
    (let [scored (mapv (fn [{:keys [id readings]}]
                         (let [result (first (filter #(= entry-id (:id %))
                                                    (:entries (assess module readings))))]
                           (require! (or (not= :computed (:status result))
                                         (= :predicted (get-in result [:reading :status])))
                                     :ranking-requires-prediction {:candidate id})
                           {:id id :diagnostic result})) candidates)]
      (if (or (empty? scored) (some #(not= :computed (get-in % [:diagnostic :status])) scored))
        {:status :unresolved :candidates scored}
        {:status :ranked :criterion entry-id :quantity :local-kl-risk
         :module (select-keys module [:id :version :context])
         :candidates (vec (sort-by #(get-in % [:diagnostic :risk-nats]) scored))}))))
