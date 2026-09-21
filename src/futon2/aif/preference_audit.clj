(ns futon2.aif.preference-audit
  "Record-only interpretation of retained preference inputs; never a scoring input."
  (:require [clojure.set :as set]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.action-identity :as identity]))

(load-identity/register! *ns* *file*)

(defn- held [reason] {:status :held :reason reason})
(defn- total [weights] (reduce + 0 (vals weights)))
(defn- normalizer [{:keys [tau distribution]}]
  (let [{:keys [universe weights zeroed]} distribution
        softplus (fn [x] (+ (max 0.0 (double x)) (Math/log1p (Math/exp (- (Math/abs (double x)))))))
        log-z0 (reduce + 0.0 (map #(softplus (get weights % 0)) universe))
        excluded (reduce + 0.0 (map #(Math/exp (- (double (reduce + 0 (map (fn [t] (get weights t 0)) %))) log-z0)) zeroed))
        log-z (+ log-z0 (Math/log1p (- excluded)))
        z (Math/exp log-z)]
    (if (Double/isFinite log-z)
      {:tau tau :status :computed :log-z log-z
       :z (if (Double/isFinite z) z (held :normalizer-overflows-double))
       :outcome-count (reduce *' 1 (repeat (count universe) 2))
       :excluded-outcome-count (count zeroed) :domain :full-token-powerset}
      (assoc (held :normalizer-not-finite) :tau tau))))

(defn- odds [target wants terminal locators]
  (if (or (nil? target) (not (seq wants)))
    (held :selected-target-wants-not-retained)
    {:status :recorded :target target
     :comparison :present-versus-absent-all-other-tokens-fixed
     :rows (mapv (fn [w]
                   (let [token [target w] weight (get (:weights terminal) token 0)]
                     (merge {:token token :locator (get locators token)}
                            (cond
                              (not (contains? (:universe terminal) token)) (held :token-outside-outcome-domain)
                              (seq (:zeroed terminal)) (held :other-token-context-required-for-ruled-zeros)
                              (not (contains? #{:C3 :C4 :C5 :C6} (get-in locators [token :class]))) (held :token-meaning-locator-not-retained)
                              (not (Double/isFinite (Math/exp (double weight)))) (held :odds-ratio-overflows-double)
                              :else {:status :computed :log-odds weight
                                     :present-to-absent (Math/exp (double weight))})))) wants)}))

(defn build
  "Audit actual shared C, preserving all source/policy/observation domain units.
   Missing meaning/provenance is held; no current store or corpus is consulted."
  [decision]
  (try
    (let [certificate (:selection-certificate decision)
          policies (get-in certificate [:g-term-decomposition :policies]
                           (get-in decision [:g-term-decomposition :policies]))
          c (get-in (first policies) [:terms :C :value])
          provenance (get-in certificate [:scoring 0 :c])
          live (:live-c provenance)
          steps (:steps c) terminal (:distribution (last steps))
          domains (get-in certificate [:token-belief-stage :domain-inputs])
          want (set (for [{:keys [target declaration]} domains w (:want declaration)] [target w]))
          spec (get-in certificate [:precision-family :model :preference-spec])
          lam (or (get-in live [:preference-scales :lam]) (:lam spec))
          mu (or (get-in live [:preference-scales :mu]) (:mu spec))
          weights (:weights terminal) echo (:weights-echo provenance)
          action (or (:action decision) (get-in certificate [:precision-family :selected-action]))
          target (:target action)
          selected (filter #(= target (:target %)) domains)
          projected (:projected-from live)
          common? (and (seq policies) (seq steps)
                       (every? #(= c (get-in % [:terms :C :value])) policies)
                       (every? #(= provenance (:c %)) (vals (:scoring certificate))))]
      (if-not common?
        {:schema :wm/preference-audit-v1 :status :held :reason :shared-consumed-preference-not-retained}
        (let [fallback-tokens (set/difference want (set (keys echo)))
              fallback (if (and (seq want) (number? lam) (= 0 mu) (map? echo))
                         {:status :computed
                          :rows (mapv (fn [token]
                                        {:token token :origin :unnamed-want-lam-over-want-count
                                         :lam lam :want-count (count want) :weight (/ lam (count want))
                                         :consumed-weight (get weights token 0)})
                                      (sort-by pr-str fallback-tokens))}
                         (held :fallback-derivation-inputs-not-retained))
              expected (when (= :computed (:status fallback))
                         (merge echo (into {} (map (juxt :token :weight)) (:rows fallback))))
              gs (map :g (:candidates certificate))]
          {:schema :wm/preference-audit-v1 :status :recorded
           :outcome-domain {:id (identity/digest {:kind :target-qualified-token-powerset :tokens (:universe terminal)})
                            :kind :target-qualified-token-powerset :token-count (count (:universe terminal))
                            :tokens (:universe terminal)}
           :source-budget {:domain (if (number? (:n-entries live)) :global-deduplicated-live-source-inventory :not-retained)
                           :unit :source-entry :count (:n-entries live)
                           :law (if (number? (:n-entries live)) :raw-source-weights-normalized-to-lam :not-retained)
                           :before-projection (if (and (number? lam) (number? (:n-entries live)))
                                                {:status :derived :value lam :basis :live-c-normalise-weights-law}
                                                (held :source-budget-not-retained))
                           :raw-weight-total (held :raw-source-weights-not-retained)
                           :projected-source-count (when (map? projected) (count projected))
                           :projected-outcome-count (when (map? echo) (count echo))}
           :utility-totals {:after-source-projection (if (and (map? projected) (number? lam))
                                                       (* lam (reduce + 0 (map :source-weight (vals projected))))
                                                       (held :projection-not-retained))
                            :retained-live (if (map? echo) (total echo) (held :live-weights-not-retained))
                            :all-consumed (total weights)}
           :fallbacks fallback
           :weight-accounting (cond (nil? expected) (held :fallback-derivation-inputs-not-retained)
                                    (= expected weights) {:status :matched}
                                    :else (held :consumed-weights-do-not-match-origin-account))
           :schedule (:schedule c) :normalizers (mapv normalizer steps)
           :selected-target-odds (odds target (when (= 1 (count selected)) (get-in (first selected) [:declaration :want]))
                                       terminal (:observation-locators action))
           :G-spread (if (and (seq gs) (every? number? gs)) (- (apply max gs) (apply min gs))
                         (held :candidate-scores-not-retained))})))
    (catch Exception _ {:schema :wm/preference-audit-v1 :status :held :reason :incompatible-preference-inputs})))

(defn attach [decision]
  (assoc-in decision [:selection-certificate :preference-audit] (build decision)))

(defn valid? [decision receipt]
  (= receipt (build decision)))
