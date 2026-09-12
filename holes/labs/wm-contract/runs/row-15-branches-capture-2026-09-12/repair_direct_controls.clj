(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.policy :as policy])

(def output-dir
  "holes/labs/wm-contract/runs/row-15-branches-capture-2026-09-12")
(defn path [name] (.getPath (io/file output-dir name)))
(defn read-edn [name] (edn/read-string (slurp (path name))))
(defn write-edn! [name value] (spit (path name) (str (pr-str value) "\n")))
(defn sha256-file [p]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream p)]
      (let [buffer (byte-array 8192)]
        (loop []
          (let [n (.read in buffer)]
            (when (pos? n) (.update digest buffer 0 n) (recur))))))
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest)))))

(def capture (read-edn "machinery-capture.edn"))
(def ranked (get-in capture [:judgement :ranked-actions]))
(def decision (get-in capture [:judgement :decision]))
(def base-opts {:abstain-epsilon 1.0e12
                :selection-gain (double (or (:selection-gain decision) 1.0))
                :habit-prior-stats (get-in capture [:judgement :habit-prior-state])
                :selection-boundary :actuation
                :selection-law :controller-head})
(def no-op-output (policy/select-action ranked base-opts))
(when-not (= :abstain (:action no-op-output))
  (throw (ex-info "large finite epsilon did not exercise abstain" {:output no-op-output})))
(write-edn! "no-op-abstain.edn"
            {:schema :wm/machine-action-direct-invocation-v1
             :scope :direct-invocation-of-production-fn
             :production-fn 'futon2.aif.policy/select-action
             :branch :no-op-abstain
             :input {:ranked-actions ranked :options base-opts}
             :output no-op-output})

;; Project two real candidates and duplicate the first real score. Zero habit
;; bias is the option-local neutral prior; the candidate/action payloads remain
;; the captured values. With equal G and equal ln E, first-max must pick index 0.
(def tie-domain
  (-> (subvec ranked 0 2)
      (assoc-in [0 :habit-prior-bias] 0.0)
      (assoc-in [1 :habit-prior-bias] 0.0)
      (assoc-in [1 :controller-score] (:controller-score (first ranked)))))
(def tie-opts {:abstain-epsilon 0.01
               :selection-gain (double (or (:selection-gain decision) 1.0))
               :habit-prior-stats (get-in capture [:judgement :habit-prior-state])
               :selection-boundary :strategic-recommendation
               :selection-law :full-score-posterior
               :f-pi-opts {:f-pi-policy-posterior? true
                           :f-pi-values [0.0 0.0]
                           :f-pi-scaling :unscaled
                           :f-pi-posterior {:status :commissioned-zero-control}}})
(def tie-output (policy/select-action tie-domain tie-opts))
(when-not (= (:action (first tie-domain)) (:action tie-output))
  (throw (ex-info "first-max tie control did not choose first candidate"
                  {:output tie-output})))
(write-edn! "first-max-tie-control.edn"
            {:schema :wm/machine-action-direct-invocation-v1
             :scope :direct-invocation-of-production-fn
             :production-fn 'futon2.aif.policy/select-action
             :branch :commissioned-first-max-tie-control
             :input {:ranked-actions tie-domain :options tie-opts}
             :output tie-output
             :mutation {:kind :duplicate-one-real-controller-score
                        :source-index 0 :target-index 1
                        :score (:controller-score (first ranked))
                        :domain-projection :first-two-real-ranked-candidates
                        :habit-bias :neutral-control}})

(def artifact-branches
  [["machinery-capture.edn" :natural-machinery-branch]
   ["controller-head.edn" :controller-head]
   ["full-score-first-max.edn" :full-score-first-max]
   ["habit-last-max.edn" :habit-last-max]
   ["no-op-abstain.edn" :no-op-abstain]
   ["requested-posterior-f-pi-absent.edn" :requested-posterior-with-f-pi-absent]
   ["first-max-tie-control.edn" :commissioned-first-max-tie-control]])
(def manifest
  {:schema :wm/machine-action-branches-manifest-v1
   :generated-by ["capture_branches.clj" "repair_direct_controls.clj"]
   :production-function "futon2.aif.policy/select-action"
   :natural-capture :redirected-machinery-tick
   :direct-record-scope :direct-invocation-of-production-fn
   :artifacts
   (mapv (fn [[file branch]]
           {:file file :branch branch :sha256 (sha256-file (path file))})
         artifact-branches)})
(write-edn! "manifest.edn" manifest)
(prn {:direct-controls :complete :no-op (:action no-op-output)
      :tie-selected-first? (= (:action (first tie-domain)) (:action tie-output))})
