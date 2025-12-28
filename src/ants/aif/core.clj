(ns ants.aif.core
  "Top-level orchestration of the observe → perceive → policy cycle."
  (:require [ants.aif.affect :as affect]
            [ants.aif.observe :as observe]
            [ants.aif.perceive :as perceive]
            [ants.aif.policy :as policy]))

(def default-aif-config
  {:preferences {:hunger {:mean 0.40 :sd 0.08}
                 :ingest {:mean 0.70 :sd 0.20}}
   :precision {:tau-floor 0.08
               :tau-cap 1.5
               :need-gain 0.6
               :dhdt-gain 0.8
               :hunger-thresh 0.45
               :ingest-thresh 0.60
               :tau-reserve-gain 0.6
               :tau-survival-gain 0.5}
   :actions {:pheromone {:base-cost 0.01
                         :hunger-mult 0.1
                         :no-ingest-pen 0.3
                         :ingest-thresh 0.2}
             :forage {:friendly-home-pen 1.2}
             :return {:empty-home-pen 0.9
                      :cargo-thresh 0.05
                      :home-thresh 0.8}}
   :efe {:lambda {:pragmatic 1.0
                  :ambiguity 0.5
                  :info 0.4
                  :colony 0.4
                  :survival 1.2}
         :colony {:reserve-thresh 1.0
                  :non-return-pen 0.6
                  :return-pen 0.0}
         :survival {:hunger-thresh 0.55
                    :hunger-weight 1.5
                    :dist-weight 0.5
                    :ingest-buffer 0.30
                    :return-reduction 0.40}}
   :trend {:window 5}

   :modes {:cargo-high 0.60      ; enter HOMEBOUND when cargo ≥ this
           :cargo-low  0.10      ; leave HOMEBOUND when cargo ≤ this
           :home-high  0.80      ; “on home” threshold
           :home-low   0.50      ; “near home” (hysteresis)
           :reserve-low 0.20     ; colony low reserve → MAINTAIN when near-home
           :trail-min  0.20      ; below this, trails are considered weak
           :food-eps   0.02}}    ; “basically no food” epsilon
  )



(defn- merge-deep
  [& ms]
  (letfn [(merge* [a b]
            (reduce (fn [acc [k v]]
                      (if (and (map? (get acc k)) (map? v))
                        (assoc acc k (merge* (get acc k) v))
                        (assoc acc k v)))
                    (or a {})
                    b))]
    (reduce merge* {} (remove nil? ms))))

(defn- aif-config
  [world ant]
  (let [base (merge-deep default-aif-config (get-in world [:config :aif]))]
    (if-let [override (:aif-config ant)]
      (merge-deep base override)
      base)))

(defn dhdt
  "Return hunger trend (last - first) over the stored recent window."
  [{:keys [recent]}]
  (let [hs (->> (or recent [])
                (keep #(or (get-in % [:obs :hunger])
                           (get-in % [:obs :h])
                           (get % :h)
                           0.0))
                (map double))
        cnt (count hs)]
    (if (>= cnt 2)
      (- (nth hs (dec cnt)) (first hs))
      0.0)))

(defn- append-recent
  [recent observation window tau]
  (let [entry {:obs {:hunger (double (or (:hunger observation)
                                         (:h observation)
                                         0.0))
                     :ingest (double (or (:ingest observation) 0.0))}
               :tau tau}
        window (max 1 (int window))]
    (->> (conj (vec (or recent [])) entry)
         (take-last window)
         vec)))

(defn- ensure-aif-baseline
  [ant]
  (-> ant
      (update :mu #(or % {}))
      (update :prec #(or % {}))
      (update :recent #(vec (or % [])))
      (update :ingest (fn [ing]
                        (if (number? ing)
                          (observe/clamp01 ing)
                          0.0)))
      (update :mode #(or % :outbound))))

;; helper: trailing true-like run length for a boolean-ish key in :recent
(defn- trailing-streak
  [recent key pred?]
  (loop [xs (rseq (vec (or recent []))) n 0]
    (if-let [x (first xs)]
      (if (pred? (get-in x [:obs key]))
        (recur (next xs) (inc n))
        n)
      n)))

(defn aif-step
  "Perform a single active inference update for an ant. Pure – no world mutation.

  Returns a map containing the updated ant plus rich diagnostics:
  {:ant … :action … :observation … :policy … :perception … :G … :P …}."
  ([world ant]
   (aif-step world ant nil))
  ([world ant {:keys [actions] :as opts}]
   (let [ant (ensure-aif-baseline ant)
         cfg (aif-config world ant)
         precision-cfg (:precision cfg)
         trend-window (get-in cfg [:trend :window]
                              (get-in default-aif-config [:trend :window]))
         observation (observe/g-observe world ant)
         recent (append-recent (:recent ant)
                               observation
                               trend-window
                               (get-in ant [:prec :tau]))
         white-streak (trailing-streak recent :white? #(>= (double (or % 0.0)) 0.5))
         since-ingest (trailing-streak recent :ingest #(< (double (or % 0.0)) 0.20))
         observation (assoc observation
                            :white-streak white-streak
                            :since-ingest since-ingest)

         modes-cfg (get-in cfg [:modes])
         next-mode (affect/next-mode (:mode ant) observation modes-cfg)
         ant (assoc ant :mode next-mode)
         observation (assoc observation :mode next-mode)

         ant (assoc ant :recent recent)
         
         precision-opts (merge precision-cfg (:precision-options opts))
         perceive-opts (-> (select-keys opts [:max-steps :alpha :beta])
                           (assoc :hunger-options (:hunger-options opts))
                           (assoc :precision-options precision-opts))
         perception (perceive/perceive world ant observation perceive-opts)
         mu (:mu perception)
         prec (:prec perception)
         need (affect/need-error {:obs observation} precision-opts)
         hunger-trend (dhdt {:recent recent})
         prec (affect/update-tau prec {:obs observation
                                       :dhdt hunger-trend}
                                 precision-opts)
         perception-with-prec (assoc perception :prec prec)
         action-list (vec (or actions policy/default-actions))
         policy (policy/choose-action mu
                                      prec
                                      observation
                                      {:actions action-list
                                       :preferences (:preferences cfg)
                                       :action-costs (:actions cfg)
                                       :efe (:efe cfg)
                                       :precision precision-opts})
         tau (:tau policy)
         prec' (assoc prec :tau tau)
         perception (assoc perception-with-prec :prec prec')
         chosen (:action policy)
         action-stats (get-in policy [:policies chosen] {:G 0.0 :p 1.0})
         updated-ant (-> ant
                         (assoc :mu mu
                                :prec prec'
                                :recent recent
                                :last-observation observation
                                :last-trace (:trace perception)
                                :last-action chosen
                                ;; ---- NEW persisted telemetry:
                                :white? (>= (double (or (:white? observation) 0.0)) 0.5)
                                :white-streak white-streak
                                :since-ingest since-ingest
                                ;; (rest unchanged...)
                                :last-G (:G action-stats)
                                :last-policy policy
                                :need-error need
                                :dhdt hunger-trend))]
     {:ant updated-ant
      :action chosen
      :observation observation
      :policy policy
      :perception perception
      :G (:G action-stats)
      :P (:p action-stats)
      :diagnostics {:need need
                    :dhdt hunger-trend
                    :tau tau
                    :risk (:risk action-stats)
                    :ambiguity (:ambiguity action-stats)
                    :info (:info action-stats)
                    :colony (:colony action-stats)
                    :survival (:survival action-stats)
                    :action-cost (:action-cost action-stats)}})))
