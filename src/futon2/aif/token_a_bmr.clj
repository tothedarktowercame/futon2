(ns futon2.aif.token-a-bmr
  "Prototype model reduction over the counted token likelihood A (item 6,
   alternative (c)); recorded, not applied.

   For each token class c and error kind k in #{:false-neg :false-pos}, the
   full model is an independent rate theta[c,k] ~ Beta(alpha,beta), updated by
   the exact counts `errors/compared` from observation-rates/rates-by-class.
   Its log evidence is ln B(alpha+errors,beta+compared-errors)-ln B(alpha,beta).

   Two reductions are fixed before seeing the data.  :error-free fixes one
   cell's rate to zero, with evidence 1 when errors=0 and impossible otherwise.
   :pooled shares one rate, separately for each error kind, across all observed
   classes.  Unobserved cells are excluded from both reductions and retained
   with a typed reason.

   Sign convention (aif-equations.edn :model-reduction): delta-F is
   log-evidence(full) - log-evidence(reduced); a reduction is favoured at
   delta-F <= -3.  This function only records scores: :applied is always false."
  (:require [clojure.string :as str]
            [futon2.aif.bmr :as bmr]))

(def schema :wm/token-a-bmr-v1)

(def label
  "prototype: model reduction over the counted token A (item 6 alternative (c)); recorded, not applied")

(def ^:private cell-kinds [:false-neg :false-pos])

(defn- valid-prior?
  [{:keys [alpha beta authority]}]
  (and (or (integer? alpha) (ratio? alpha))
       (pos? alpha)
       (or (integer? beta) (ratio? beta))
       (pos? beta)
       (string? authority)
       (not (str/blank? authority))))

(defn- observed-cell?
  [cell]
  (and (map? cell)
       (integer? (:numerator cell))
       (integer? (:denominator cell))
       (pos? (:denominator cell))
       (<= 0 (:numerator cell) (:denominator cell))))

(defn- log-beta
  [x y]
  (bmr/log-multivariate-beta [x y]))

(defn- cell-log-evidence
  [prior errors compared]
  (- (log-beta (+ (:alpha prior) errors)
               (+ (:beta prior) (- compared errors)))
     (log-beta (:alpha prior) (:beta prior))))

(defn- cells-and-exclusions
  [rates]
  (reduce-kv
   (fn [{:keys [cells excluded]} class entry]
     (if (= :unobserved (:status entry))
       {:cells cells
        :excluded (conj excluded {:class class :reason :class-unobserved})}
       (reduce
        (fn [acc kind]
          (let [cell (get entry kind)]
            (if (observed-cell? cell)
              (update acc :cells conj {:class class :kind kind
                                       :errors (:numerator cell)
                                       :compared (:denominator cell)})
              (update acc :excluded conj
                      {:class class :kind kind
                       :reason (if (= :unobserved (:status cell))
                                 :cell-unobserved
                                 :cell-counts-invalid)}))))
        {:cells cells :excluded excluded}
        cell-kinds)))
   {:cells [] :excluded []}
   rates))

(defn score
  "Score fixed error-free and pooled reductions of rates-by-class-shaped RATES.

   PRIOR must satisfy observation-rates' prior rule: positive integer/ratio
   :alpha and :beta and a non-blank string :authority.  Invalid priors return
   {:status :missing :kind :invalid-prior}; no implicit prior is supplied."
  [rates prior]
  (if-not (valid-prior? prior)
    {:status :missing :kind :invalid-prior :prior prior}
    (let [{:keys [cells excluded]} (cells-and-exclusions rates)
          cells (sort-by (juxt (comp str :class) (comp str :kind)) cells)
          cell-evidence (mapv #(assoc % :log-evidence-full
                                     (cell-log-evidence prior (:errors %) (:compared %)))
                              cells)
          error-free
          (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                (map (fn [{:keys [class kind errors compared log-evidence-full]}]
                       [[class kind]
                        (cond-> {:counts {:numerator errors :denominator compared}
                                 :log-evidence-full log-evidence-full}
                          (zero? errors)
                          (assoc :log-evidence-reduced 0.0
                                 :delta-f log-evidence-full)
                          (pos? errors)
                          (assoc :impossible-under-reduced true))])
                     cell-evidence))
          pooled
          (into {}
                (for [kind cell-kinds
                      :let [xs (filter #(= kind (:kind %)) cell-evidence)]
                      :when (seq xs)
                      :let [errors (reduce + (map :errors xs))
                            compared (reduce + (map :compared xs))
                            full (reduce + 0.0 (map :log-evidence-full xs))
                            reduced (cell-log-evidence prior errors compared)]]
                  [kind {:delta-f (- full reduced)
                         :log-evidence-full full
                         :log-evidence-reduced reduced
                         :counts {:numerator errors :denominator compared}
                         :classes (mapv :class xs)}]))]
      {:schema schema
       :label label
       :prior prior
       :threshold bmr/acceptance-threshold
       :applied false
       :error-free error-free
       :pooled pooled
       :excluded (vec (sort-by (juxt (comp str :class) (comp str :kind)) excluded))})))
