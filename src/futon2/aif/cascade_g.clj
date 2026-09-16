(ns futon2.aif.cascade-g
  "Canonical finite categorical G on supplied model inputs. No model discovery,
   authority minting, enactment or selection. Rational log bounds describe the
   represented inputs, not their empirical accuracy or normalization repair."
  (:require [futon2.aif.categorical-ambiguity :as ambiguity]
            [futon2.aif.machine-model :as model]))

(defn- refuse! [kind path]
  (throw (ex-info "Cascade G refused" {:refusal {:kind kind :path path}})))

(defn- plus [[a b] [c d]] [(+' a c) (+' b d)])
(defn- minus [[a b] [c d]] [(-' a d) (-' b c)])
(defn- scale [x [a b]]
  (if (neg? x) [(*' x b) (*' x a)] [(*' x a) (*' x b)]))
(defn- sum-bounds [xs] (reduce plus [0 0] xs))

(defn- log-unit
  "For 1<=x<=2, z=(x-1)/(x+1). After n positive atanh terms,
   tail <= 2*z^(2n+1)/((2n+1)*(1-z^2)). All arithmetic is rational."
  [x]
  (let [z (/ (-' x 1) (+' x 1)) z2 (*' z z) n 24]
    (loop [i 0 power z acc 0]
      (if (= i n)
        [acc (+' acc (/ (*' 2 power) (*' (inc (* 2 n)) (-' 1 z2))))]
        (recur (inc i) (*' power z2)
               (+' acc (/ (*' 2 power) (inc (* 2 i)))))))))

(def ^:private log-two (log-unit 2))

(defn log-bounds
  "Enclose ln(x) for a positive rational x. Range reduction x=2^k*m,
   1<=m<=2. Resource limit is a typed refusal, never a truncated answer."
  [x]
  (when-not (and (or (integer? x) (ratio? x)) (pos? x))
    (refuse! :invalid-log-input [:log]))
  (loop [m x k 0]
    (when (> (abs k) 4096) (refuse! :log-range-limit [:log]))
    (cond
      (> m 2) (recur (/ m 2) (inc k))
      (< m 1) (recur (*' m 2) (dec k))
      :else (plus (log-unit m) (scale k log-two)))))

(defn- rational-value [x]
  (cond
    (or (integer? x) (ratio? x)) x
    (instance? BigDecimal x) (rationalize x)
    :else (rationalize (BigDecimal. (double x)))))

(defn- row! [mass support path]
  (let [admission (model/distribution-admission mass support)]
    (when-not (:ok admission)
      (refuse! (get-in admission [:refusal :kind]) path))
    {:admission admission :mass (update-vals mass rational-value)}))

(defn step-g
  "Score one observation point. q and a use categorical-ambiguity's unchanged
   contracts; c is {:model :support :mass :provenance}. Context explicitly
   binds :policy/id, :occurrence/id and :point. Authority is checked, not created.
   Returns both decompositions with rational enclosures and unchanged inputs."
  [{:keys [q a c context] :as input}]
  (doseq [k [:policy/id :occurrence/id :point]]
    (when (nil? (get context k)) (refuse! :missing-context [:context k])))
  ;; Reuse the existing authority, row, support and model checks. Its binary64
  ;; result is not used as the numeric reference for this scorer.
  (ambiguity/ambiguity q a)
  (when-not (= (:model q) (:model c))
    (refuse! :model-revision-mismatch [:c :model]))
  (when-not (= (:outcome-support a) (:support c))
    (refuse! :support-mismatch [:c :support]))
  (when-not (and (map? (:provenance c)) (seq (:provenance c)))
    (refuse! :missing-provenance [:c :provenance]))
  (let [states (:state-support q) outcomes (:outcome-support a)
        qr (row! (:mass q) states [:q :mass])
        ar (into {} (for [s states]
                      [s (row! (get-in a [:rows s :mass]) outcomes [:a :rows s])]))
        cr (row! (:mass c) outcomes [:c :mass])
        joint (into {} (for [s states]
                         [s (into {} (for [o outcomes]
                                       [o (*' (get-in qr [:mass s])
                                              (get-in ar [s :mass o]))]))]))
        marginal (into {} (for [o outcomes]
                            [o (reduce +' 0 (map #(get-in joint [% o]) states))]))
        ln (memoize log-bounds)]
    (doseq [o outcomes]
      (when (and (pos? (get marginal o)) (zero? (get-in cr [:mass o])))
        (refuse! :infinite-risk [:c :mass o])))
    (let [risk (sum-bounds (for [o outcomes :let [p (get marginal o)] :when (pos? p)]
                                  (scale p (minus (ln p) (ln (get-in cr [:mass o]))))))
          amb (sum-bounds (for [s states o outcomes
                                :let [j (get-in joint [s o])] :when (pos? j)]
                            (scale (- j) (ln (get-in ar [s :mass o])))))
          cross (sum-bounds (for [o outcomes :let [p (get marginal o)] :when (pos? p)]
                              (scale (- p) (ln (get-in cr [:mass o])))))
          mi (sum-bounds (for [s states o outcomes
                               :let [j (get-in joint [s o])] :when (pos? j)]
                           (scale j (minus (ln j)
                                           (plus (ln (get-in qr [:mass s]))
                                                 (ln (get marginal o)))))))
          admissions {:q (:admission qr) :a (update-vals ar :admission) :c (:admission cr)}]
      {:context context :model (:model q) :inputs input
       :joint joint :outcome-mass marginal :admissions admissions
       :exactly-normalized-inputs?
       (every? :exactly-normalized? (concat [(:q admissions) (:c admissions)]
                                           (vals (:a admissions))))
       :risk risk :ambiguity amb :preference-cross-entropy cross :mutual-information mi
       :g (plus risk amb) :equivalent-g (minus cross mi)
       :numeric-contract :rational-atanh-24-v1})))

(defn total-g
  "Sum a nonempty declared schedule, with a supplied prediction/C at each point.
   The schedule is not inferred from policy length. Empty designs still need a
   prediction on this schedule. Does not construct B or its rollout."
  [{:keys [schedule steps]}]
  (when-not (and (vector? schedule) (seq schedule)
                 (= (count schedule) (count (set schedule)))
                 (vector? steps) (= (count schedule) (count steps)))
    (refuse! :invalid-schedule [:schedule]))
  (when-not (= schedule (mapv #(get-in % [:context :point]) steps))
    (refuse! :schedule-mismatch [:steps]))
  (let [identity-of (fn [s] [(get-in s [:q :model])
                            (get-in s [:q :state-support])
                            (get-in s [:a :outcome-support])
                            (select-keys (:context s) [:policy/id :occurrence/id])])]
    (when-not (apply = (map identity-of steps))
      (refuse! :comparison-context-mismatch [:steps])))
  (let [results (mapv step-g steps)]
    {:schedule schedule :steps results
     :g (sum-bounds (map :g results))
     :equivalent-g (sum-bounds (map :equivalent-g results))
     :numeric-contract :rational-atanh-24-v1}))
