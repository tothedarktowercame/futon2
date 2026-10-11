(ns futon2.aif.integrated-controlled-sut
  "Production-shaped controlled composition of the seven accepted SUT seams.

  Authorities, fixtures, and the external Agency executor are injected.  This
  namespace has no production defaults and is not referenced by the live JVM."
  (:require [clojure.string :as str]
            [futon2.aif.cascade-efe-sut :as efe]
            [futon2.aif.controlled-run-persistence :as persistence]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.open-cascade-relatedness-sut :as relatedness]
            [futon2.aif.outer-objective-sut :as outer]
            [futon2.aif.wm.cascade-decision :as decision]))

(def schema :wm/integrated-controlled-sut-v1)

(defn- refuse [phase reason & [detail]]
  {:schema schema :status :refused :phase phase :reason reason :detail detail})

(defn- terminal-job? [job authority]
  (and (= "done" (:state job)) (= authority (:agent-id job))
       (= (:result-digest job) (evidence/value-digest (dissoc job :result-digest)))))

(defn- request-bound-job? [job authority request]
  (and (terminal-job? job authority)
       (= (evidence/value-digest request) (get-in job [:result :request-digest]))))

(defn- rss-bytes []
  (try
    (let [line (first (filter #(.startsWith ^String % "VmHWM:")
                              (str/split-lines (slurp "/proc/self/status"))))]
      (when line (* 1024 (Long/parseLong (second (re-find #"(\d+)" line))))))
    (catch Exception _ nil)))

(defn run-controlled!
  [{:keys [mode phase-hook outer-input efe-input
           dispatch-fn dispatch-authority revision-input-fn grounding-fn
           persistence-input]}]
  (if-not (= :controlled-sut mode)
    (refuse :selection :production-authorities-unconfigured)
    (let [started (System/nanoTime) peak (atom (or (rss-bytes) 0)) timings (atom {})
          phase! (fn [phase thunk]
                  (let [t (System/nanoTime)
                        value (thunk)]
                    (swap! peak max (or (rss-bytes) 0))
                    (swap! timings assoc phase (/ (- (System/nanoTime) t) 1e6))
                    (when phase-hook (phase-hook {:phase phase :value value}))
                    value))
          selected (phase! :selection #(outer/run-controlled outer-input))]
      (if-not (= :selected (:status selected))
        (refuse :selection (:reason selected) selected)
        (let [target (:selected-task selected)
              problem (:problem efe-input)
              constructed-target (:target problem)
              construction (phase! :construction
                                   #(decision/controlled-application-order-run
                                     (:order-control efe-input) problem))]
          (if-not (and (= target constructed-target)
                       (= :prepared-for-enactment (:status construction))
                       (= :ready-for-g (get-in construction [:composition :status])))
            (refuse :construction :selected-target-construction-mismatch
                    {:selected target :constructed constructed-target
                     :receipt construction})
            (let [_ (phase! :admission #(get-in construction [:composition :certificate]))
                  scored (phase! :scoring #(efe/compose! (assoc efe-input
                                                                :ordered construction)))]
              (if-not (= :prepared-for-enactment (:status scored))
                (refuse (if (= :application-order-refused (:reason scored))
                          :admission :scoring)
                        (:reason scored) scored)
                (let [selected-key (get-in scored [:identity-chain :selected])
                      original (get-in scored [:posterior :action])
                      request-1 {:schema :wm/controlled-dispatch-request-v1 :attempt 1
                                 :policy-key selected-key :action original}
                      first-dispatch (phase! :dispatch #(dispatch-fn request-1))]
                  (if-not (and (request-bound-job? first-dispatch dispatch-authority request-1)
                               (= selected-key (get-in first-dispatch [:result :policy-key])))
                    (refuse :dispatch :dispatch-authority-or-identity-mismatch)
                    (let [revision-input (revision-input-fn original first-dispatch)
                          revised (phase! :revision
                                          #(relatedness/run-controlled! revision-input))]
                        (if-not (= :ready-for-external-dispatch (:status revised))
                          (refuse :revision (or (:kind revised) (:reason revised)) revised)
                          (let [revised-digest (get-in revised [:dispatch :dispatched-action-sha256])
                                request-2 {:schema :wm/controlled-dispatch-request-v1 :attempt 2
                                           :action-sha256 revised-digest
                                           :revision (:revision revised)}
                                second-dispatch (phase! :dispatch #(dispatch-fn request-2))]
                            (if-not (and (request-bound-job? second-dispatch dispatch-authority request-2)
                                         (= revised-digest
                                            (get-in second-dispatch [:result :action-sha256]))
                                         (= :verified-pattern-use
                                            (get-in second-dispatch [:result :observation :status])))
                              (refuse :dispatch :revised-dispatch-evidence-mismatch)
                              (let [certificates {:outer-objective selected
                                                    :three-halves (get-in scored [:order :composition :certificate])
                                                    :application-order (get-in scored [:order :application-order])
                                                    :predictive (:predictive scored)
                                                    :prefix-f (:prefix-f scored)
                                                    :factorized-preference (:preference scored)
                                                    :relatedness (:search revised)}
                                      grounding-request {:schema :wm/controlled-grounding-request-v1
                                                         :original-identity selected-key
                                                         :revised-identity revised-digest
                                                         :dispatch-jobs [first-dispatch second-dispatch]}
                                      grounded (phase! :grounding
                                                       #(grounding-fn grounding-request))]
                                  (if-not (and (= :wm/controlled-grounding-v1 (:schema grounded))
                                               (= :verified (:status grounded))
                                               (= (evidence/value-digest grounding-request)
                                                  (:request-digest grounded))
                                               (= selected-key (:original-identity grounded))
                                               (= revised-digest (:revised-identity grounded))
                                               (seq (:artifact-commit grounded)))
                                    (refuse :grounding :grounding-evidence-mismatch grounded)
                                    (let [closed {:schema :wm/closed-run-v1
                                                  :status :closed :grounding grounded
                                                  :certificates certificates}
                                      result (assoc (:result persistence-input)
                                                    :outcome :controlled-sut-closed
                                                    :controlled-sut closed)
                                      result (assoc-in result
                                                       [:checkpoints :selection :judgment
                                                        :controller-decision]
                                                       (:posterior scored))
                                      closed-run (-> (:closed-run persistence-input)
                                                     (assoc :result result)
                                                     (update :brief-item assoc
                                                             :controlled-sut
                                                             {:certificates certificates}))
                                      persisted (phase! :close
                                                        #(persistence/persist-closed-run!
                                                          closed-run
                                                          (:options persistence-input)))
                                      elapsed (/ (- (System/nanoTime) started) 1e6)
                                      artifact-paths [(get-in persisted [:trace :path])
                                                      (get-in persisted [:checkpoint :path])
                                                      (get-in persisted [:run-record :run-record])
                                                      (:morning-brief persisted)]
                                      artifact-bytes
                                      (into {} (keep (fn [path]
                                                       (when path [path (.length (java.io.File. path))])))
                                            artifact-paths)]
                                  {:schema schema :status :persisted
                                   :selection selected :construction construction
                                   :scoring scored :revision revised :closed closed
                                   :persistence persisted
                                   :telemetry {:phase-ms @timings :total-ms elapsed
                                               :peak-rss-bytes @peak
                                               :artifact-bytes artifact-bytes}})))))))))))))))))
