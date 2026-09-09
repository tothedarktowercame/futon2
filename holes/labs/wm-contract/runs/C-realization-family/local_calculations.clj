;; Run from futon3c: clojure -M ../futon2/holes/labs/wm-contract/runs/C-realization-family/local_calculations.clj
;; Standalone read-only probes. Synthetic controls are explicitly labelled.
(require '[clojure.edn :as edn]
         '[clojure.pprint :as pp])

(def artifact-root "../futon2/holes/labs/wm-contract/runs/C-realization-family/")
(def pins (edn/read-string (slurp (str artifact-root "source-pins.edn"))))
(defn sha256 [path]
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (java.io.File. path)))
        digest (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and (int %) 255)) digest))))
(defn check-pins! []
  (doseq [[path expected] pins]
    (when-not (= expected (sha256 path))
      (throw (ex-info "Source changed; review pins" {:path path})))))
(check-pins!)
(require '[futon2.aif.efe :as efe]
         '[futon2.aif.epistemic-value :as eig]
         '[futon3c.peripheral.mission-shapes :as shapes])

(def graph (edn/read-string
            (slurp "../futon0/holes/missions/M-capability-star-map.graph.edn")))
(def target "M-self-representing-stack")
(def intact-applicable (efe/mission-applicable? graph target))
(def blocked-graph (assoc-in graph [:capabilities :evidence-persistence :status] :held))
(def blocked-applicable (efe/mission-applicable? blocked-graph target))
(assert (seq (get-in graph [:missions target :scope])))
(assert intact-applicable)
(assert (false? blocked-applicable))

;; Probe the completed M-aif-head's cross-phase comparison mechanism, not
;; a claim that a participant learned. These are synthetic semantic controls.
(def proposal {:predicted-artifacts ["report"] :success-criteria ["feedback delivered"]})
(def delivered {:artifacts ["report"] :validation-artifacts ["feedback delivered"]})
(def denied {:artifacts ["report"] :validation-artifacts ["feedback delivered: false"]})
(def absent {:artifacts [] :validation-artifacts []})
(def feedback-results
  {:delivered (shapes/compute-prediction-divergence proposal delivered)
   :explicit-denial (shapes/compute-prediction-divergence proposal denied)
   :absent (shapes/compute-prediction-divergence proposal absent)
   :no-declared-prediction (shapes/compute-prediction-divergence {} {})})
(assert (= 0.0 (:delivered feedback-results) (:explicit-denial feedback-results)))
(assert (= 1.0 (:absent feedback-results)))
(assert (nil? (:no-declared-prediction feedback-results)))

;; Existing epistemic-value-test reduction examples, not empirical priors.
(def prior {:left 0.5 :right 0.5})
(def informative {:prior prior :predicted-observations {:l 0.5 :r 0.5}
                  :posteriors {:l {:left 1.0 :right 0.0}
                               :r {:left 0.0 :right 1.0}}})
(def uninformative {:prior prior :predicted-observations {:same 1.0}
                    :posteriors {:same prior}})
(def gains (eig/policy-information-gains {:inspect informative :wait uninformative}))
(def incoherent-refused?
  (try
    (eig/expected-information-gain
     {:prior prior :predicted-observations {:only 1.0}
      :posteriors {:only {:left 0.9 :right 0.1}}})
    false
    (catch clojure.lang.ExceptionInfo e
      (if (contains? (ex-data e) :max-mismatch) true (throw e)))))
(assert (< (Math/abs (- (Math/log 2.0) (:inspect gains))) 1.0e-12))
(assert (< (Math/abs (:wait gains)) 1.0e-12))
(assert incoherent-refused?)
(check-pins!)
(pp/pprint
 {:schema :c-realization/family-local-calculations-v1
  :B {:evidence-kind :recorded-capability-graph :mission target
      :requires (get-in graph [:missions target :scope])
      :recorded-applicable intact-applicable
      :control {:historical? false :change :evidence-persistence-held
                :applicable blocked-applicable}
      :does-not-certify :live-cross-store-navigation}
  :C {:evidence-kind :synthetic-semantic-probe-of-existing-code
      :mission "M-aif-head" :inputs {:propose proposal :delivered delivered :denied denied :absent absent}
      :divergence feedback-results
      :finding :substring-match-does-not-establish-feedback-satisfaction
      :human-learning :not-established}
  :D {:evidence-kind :existing-reduction-fixtures-recomputed
      :mission "M-aif-policy-conditioned-eig" :gains-nats gains
      :incoherent-posterior-refused? incoherent-refused?
      :does-not-certify :live-policy-observation-model}
  :preference-masses :not-assigned
  :cluster-completion :not-inferred})
