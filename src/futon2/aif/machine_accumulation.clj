(ns futon2.aif.machine-accumulation)

(def schema :wm/declared-accumulation-v1)
(defn- refuse [kind path] {:ok false :refusal {:kind kind :path path}})
(defn- finite-nonnegative? [x]
  (and (number? x) (Double/isFinite (double x)) (not (neg? x))))
(defn initialize
  "Explicit declared initialization; never inferred by `step`."
  [observation-support state-support prior]
  (if (and (vector? observation-support) (seq observation-support)
           (= (count observation-support) (count (set observation-support)))
           (vector? state-support) (seq state-support)
           (= (count state-support) (count (set state-support)))
           (finite-nonnegative? prior))
    {:ok true :schema schema :support {:observation observation-support :state state-support}
     :concentrations (into {} (for [o observation-support]
                                [o (zipmap state-support (repeat prior))]))
     :last-tick nil :initialization {:authority :declared :prior prior}}
    (refuse :invalid-declared-initialization [:initialization])))
(defn step [carried {:keys [id previous-id observation belief]}]
  (let [os (get-in carried [:support :observation]) ss (get-in carried [:support :state])]
    (cond
      (not (:ok carried)) (refuse :missing-carried-state [:carried])
      (not= previous-id (:last-tick carried)) (refuse :carry-chain-gap [:previous-id])
      (not= (set os) (set (keys observation))) (refuse :support-mismatch [:observation])
      (not= (set ss) (set (keys belief))) (refuse :support-mismatch [:belief])
      (not-every? finite-nonnegative? (concat (vals observation) (vals belief)))
      (refuse :invalid-increment [:tick])
      :else
      {:ok true :schema schema :support (:support carried) :last-tick id
       :previous-tick previous-id
       :concentrations
       (into {} (for [o os]
                  [o (into {} (for [s ss]
                                [s (+ (get-in carried [:concentrations o s])
                                      (* (observation o) (belief s)))]))]))})))
(defn recurrence-valid? [before tick after]
  (= after (step before tick)))
