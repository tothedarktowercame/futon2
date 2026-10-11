(ns futon2.aif.prefix-free-energy-sut
  "Controlled, unwired Prefix-F composition ending immediately before the
  production posterior selector."
  (:require [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.policy-prefix-evidence :as prefix-evidence]
            [futon2.aif.prefix-free-energy-cold-start :as cold]))

(def schema :wm/prefix-f-controlled-menu-v1)

(defn- refuse [reason & [details]]
  {:schema schema :status :refused :reason reason :details details})

(defn- records-for [policy records]
  (filterv #(= policy (get-in % [:step :policy-key])) records))

(defn- terminal-records [route request context]
  (when (= :bootstrap (:route route))
    (let [prior (:bootstrap-prior request)
          law (get-in context [:law-registry (:calibration-law-id prior)])
          executable (get-in context [:executable-registry (:evaluator-id law)])
          refs [(:authority prior) (:registry-authority law) (:authority executable)]]
      (mapv #(get-in context [:authority-jobs (:job-id %)]) refs))))

(defn compose-menu!
  "Run the real prefix admission/evidence path and produce the exact menu that
  would next cross `policy/select-action-cascades`. No posterior is computed."
  [{:keys [candidates ranked conditioning-steps bootstrap-priors
           scorer-authority]} context]
  (if-not (and (map? (:law-registry context))
               (map? (:executable-registry context))
               (map? (:authority-jobs context))
               (map? bootstrap-priors))
    (refuse :production-prefix-f-configuration-absent)
    (let [keys (mapv admission/candidate-key candidates)
          ranked-keys (mapv #(admission/candidate-key (:action %)) ranked)]
      (cond
        (some nil? keys) (refuse :incomplete-policy-key)
        (not= (count keys) (count (distinct keys)))
        (refuse :duplicate-policy-key)
        (not= keys ranked-keys)
        (refuse :ranked-policy-population-mismatch
                {:candidate-keys keys :ranked-keys ranked-keys})
        :else
        (let [prefixes (admission/prefixes candidates conditioning-steps)
              production (prefix-evidence/production-ranked ranked nil prefixes)
              records (:steps conditioning-steps)
              routes
              (mapv (fn [candidate policy]
                      (let [request {:policy policy :candidate candidate
                                     :raw-records (records-for policy records)
                                     :bootstrap-prior (get bootstrap-priors policy)
                                     :scorer-authority scorer-authority}
                            route (cold/evaluate! request context)]
                        (assoc route
                               :authority-terminal-records
                               (terminal-records route request context))))
                    candidates keys)
              by-key (into {} (map (juxt :policy identity)) routes)
              posterior-input
              (mapv (fn [entry prepared]
                      (let [key (admission/candidate-key (:action entry))
                            route (get by-key key)
                            empirical-prefix (:f-prefix prepared)]
                        (when (and (= :empirical (:route route))
                                   (not= (:f route) (:f empirical-prefix)))
                          (throw (ex-info "empirical prefix path disagrees"
                                          {:prefix-f-sut/refusal
                                           :empirical-path-disagreement
                                           :policy key})))
                        (assoc (dissoc entry :f)
                               :f-prefix {:schema :wm/policy-prefix-evidence-v1
                                          :policy (:action entry)
                                          :status :computed :f (:f route)
                                          :route (:route route)
                                          :route-receipt route})))
                    ranked production)]
          {:schema schema :status :ready-for-posterior
           :boundary :before-policy/select-action-cascades
           :policy-keys keys :prefixes prefixes :routes routes
           :posterior-input posterior-input
           :population-correspondence
           {:candidates keys
            :routes (mapv :policy routes)
            :posterior-input (mapv #(admission/candidate-key (:action %))
                                   posterior-input)}})))))

(defn transition-to-empirical!
  "Validate the externally authorized post-enactment transition, then route
  the newly coherent history. Bootstrap data may remain retained for audit but
  is not consulted by `evaluate!` once empirical evidence is coherent."
  [transition transition-context request route-context]
  (let [validated (cold/validate-transition! transition transition-context)
        route (cold/evaluate! request route-context)]
    (if (= :empirical (:route route))
      {:schema :wm/prefix-f-post-enactment-v1 :status :empirical
       :transition validated :route route :bootstrap-retired true}
      (refuse :bootstrap-not-retired {:route route}))))
