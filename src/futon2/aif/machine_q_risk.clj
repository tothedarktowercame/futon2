(ns futon2.aif.machine-q-risk
  "KL-risk adapter over admitted machine-Q and machine-C rows."
  (:require [futon2.aif.machine-model :as machine-model]))

(defn- refuse! [kind path & [detail]]
  (throw (ex-info "Machine Q risk refused"
                  (cond-> {:refusal {:kind kind :path path}
                           :machine-model/refusal kind :path path}
                    detail (assoc :detail detail)))))

(defn- identity? [x]
  (and (map? x) (string? (:id x)) (seq (:id x))
       (string? (:revision x)) (seq (:revision x))))

(defn- validate-row! [x kind pin-key]
  (when-not (map? x) (refuse! :malformed-map [kind]))
  (doseq [field [:model :support :mass]]
    (when-not (some? (get x field)) (refuse! :missing-field [kind field])))
  (when-not (identity? (:model x)) (refuse! :model-revision-mismatch [kind :model]))
  (when-not (and (vector? (:support x)) (= 12 (count (:support x)))
                 (= 12 (count (set (:support x)))))
    (refuse! :support-mismatch [kind :support]))
  (when-not (and (map? (get x pin-key)) (seq (get x pin-key)))
    (refuse! :missing-pins [kind pin-key]))
  (let [mass (:mass x)]
    (when-not (= (set (:support x)) (set (keys mass)))
      (refuse! :support-mismatch [kind :mass]))
    (when-not (every? #(and (number? %) (Double/isFinite (double %))
                            (not (neg? %))) (vals mass))
      (refuse! :invalid-mass [kind :mass]))
    (or (machine-model/row-sum-admission mass)
        (refuse! :unnormalized-input [kind :mass]))))

(defn risk
  "Return D_KL[Q||C]. Inputs are validated and are never renormalized."
  [q c]
  (validate-row! q :Q :pins)
  (validate-row! c :C :provenance)
  (when-not (and (contains? q :policy/id) (some? (:authority q)))
    (refuse! :missing-field [:Q :policy/id-or-authority]))
  (when-not (= (:model q) (:model c))
    (refuse! :model-revision-mismatch [:model]
             {:Q (:model q) :C (:model c)}))
  (when-not (= (:support q) (:support c))
    (refuse! :support-mismatch [:support]))
  (let [offending (vec (for [o (:support q)
                             :when (and (pos? (get-in q [:mass o]))
                                        (zero? (get-in c [:mass o])))] o))]
    (when (seq offending)
      (refuse! :infinite-risk [:mass] {:offending-outcomes offending}))
    {:ok true
     :policy/id (:policy/id q)
     :model (:model q)
     :support (:support q)
     :admission {:Q (machine-model/row-sum-admission (:mass q))
                 :C (machine-model/row-sum-admission (:mass c))}
     :risk (reduce + 0.0
                   (for [o (:support q)
                         :let [qm (double (get-in q [:mass o]))
                               cm (double (get-in c [:mass o]))]
                         :when (pos? qm)]
                     (* qm (Math/log (/ qm cm)))))}))
