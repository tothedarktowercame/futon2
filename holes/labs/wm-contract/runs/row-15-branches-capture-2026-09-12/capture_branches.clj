(ns row-15-branches-capture.capture-branches)

(require '[clojure.java.io :as io]
         '[clojure.edn :as edn]
         '[futon2.aif.policy :as policy]
         '[futon2.aif.trace :as trace])
(load-file "scripts/futon2/report/war_machine.clj")

(def output-dir
  "holes/labs/wm-contract/runs/row-15-branches-capture-2026-09-12")

(defn write-edn! [filename value]
  (let [path (io/file output-dir filename)]
    (spit path (str (pr-str value) "\n"))
    (.getPath path)))

(defn sha256-file [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [buffer (byte-array 8192)]
        (loop []
          (let [n (.read in buffer)]
            (when (pos? n)
              (.update digest buffer 0 n)
              (recur))))))
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest)))))

(defn machinery-selector [{:keys [controller-ranking]}]
  (let [mission-id (some #(get-in % [:action :target]) controller-ranking)]
    {:status :verified-live-selection
     :selected-mission-ids [mission-id]
     :consulted-ranking :machinery-test-controller-ranking
     :actuation {:status :machine-authorized-bounded-autonomy
                 :authorized? true
                 :executed? false}
     :provenance {:selector-seam :explicit-row15-branches-machinery-test}}))

(defn direct-record [branch ranked opts]
  {:schema :wm/machine-action-direct-invocation-v1
   :scope :direct-invocation-of-production-fn
   :production-fn 'futon2.aif.policy/select-action
   :branch branch
   :input {:ranked-actions ranked :options opts}
   :output (policy/select-action ranked opts)})

(io/make-parents (io/file output-dir "placeholder"))

(def result
  (binding [trace/*persist-policy-trace-details?* true]
    ((requiring-resolve 'futon2.report.war-machine/generate-war-machine)
     1 {:trace? false
        :step-portfolio? false
        :step-mission-detail-portfolio? false
        :strategic-selection-fn machinery-selector})))
(def judgement (:judgement result))
(def ranked (:ranked-actions judgement))
(def decision (:decision judgement))
(when-not (and (seq ranked)
               (= 0.01 (:abstain-epsilon decision))
               (:tau decision)
               (:selection-law decision)
               (:selection-boundary decision)
               (:habit-adjusted-ranking decision)
               (:softmax-weights decision))
  (throw (ex-info "machinery capture lacks a required internal-branch input"
                  {:decision-keys (sort (keys decision))
                   :ranked-count (count ranked)})))

(def base-opts
  {:abstain-epsilon (:abstain-epsilon decision)
   :selection-gain (double (or (:selection-gain decision) 1.0))
   :habit-prior-stats (:habit-prior-state judgement)})
(def strategic-opts
  (assoc base-opts
         :selection-boundary :strategic-recommendation
         :selection-law :controller-head))
(def actuation-opts
  (assoc base-opts :selection-boundary :actuation :selection-law :controller-head))
(def f-zeroes (vec (repeat (count ranked) 0.0)))
(def f-present-opts
  (assoc strategic-opts
         :selection-law :full-score-posterior
         :f-pi-opts {:f-pi-policy-posterior? true
                     :f-pi-values f-zeroes
                     :f-pi-scaling :unscaled
                     :f-pi-posterior
                     {:status :commissioned-zero-control
                      :basis :real-candidate-domain-f-pi-option-only}}))
(def f-absent-opts
  (assoc strategic-opts
         :selection-law :full-score-posterior
         :f-pi-opts {:f-pi-policy-posterior? false
                     :f-pi-posterior {:status :absent
                                      :reason :captured-f-pi-absent}}))
(def no-op-entry
  (first (filter #(= :no-op (get-in % [:action :type])) ranked)))
(def non-no-op-entry
  (first (remove #(= :no-op (get-in % [:action :type])) ranked)))
(when-not (and no-op-entry non-no-op-entry)
  (throw (ex-info "real ranked input lacks no-op/non-no-op members" {})))
(def abstain-epsilon
  (inc (Math/abs (- (double (:controller-score no-op-entry))
                    (double (:controller-score non-no-op-entry))))))
(def tie-ranked
  (if (< (count ranked) 2)
    (throw (ex-info "tie control requires two real candidates" {}))
    (assoc-in ranked [1 :controller-score] (:controller-score (first ranked)))))

(def artifacts
  [{:file "machinery-capture.edn"
    :branch :natural-machinery-branch
    :value {:schema :wm/machine-action-branches-capture-v1
            :scope :redirected-machinery-tick
            :daily-trace-written? false
            :cohort-written? false
            :policy-details-bound? true
            :judgement {:decision decision
                        :ranked-actions ranked
                        :habit-prior-state (:habit-prior-state judgement)
                        :f-pi-by-candidate-id (:f-pi-by-candidate-id judgement)
                        :selection-gain (:selection-gain judgement)}}}
   {:file "controller-head.edn" :branch :controller-head
    :value (direct-record :controller-head ranked strategic-opts)}
   {:file "full-score-first-max.edn" :branch :full-score-first-max
    :value (direct-record :full-score-first-max ranked f-present-opts)}
   {:file "habit-last-max.edn" :branch :habit-last-max
    :value (direct-record :habit-last-max ranked actuation-opts)}
   {:file "no-op-abstain.edn" :branch :no-op-abstain
    :value (direct-record :no-op-abstain ranked
                          (assoc actuation-opts :abstain-epsilon abstain-epsilon))}
   {:file "requested-posterior-f-pi-absent.edn"
    :branch :requested-posterior-with-f-pi-absent
    :value (direct-record :requested-posterior-with-f-pi-absent ranked f-absent-opts)}
   {:file "first-max-tie-control.edn" :branch :commissioned-first-max-tie-control
    :value (assoc (direct-record :commissioned-first-max-tie-control
                                 tie-ranked f-present-opts)
                  :mutation {:kind :duplicate-one-real-controller-score
                             :source-index 0 :target-index 1
                             :score (:controller-score (first ranked))})}])

(def written
  (mapv (fn [{:keys [file branch value]}]
          (let [path (write-edn! file value)]
            {:file file :branch branch :sha256 (sha256-file path)}))
        artifacts))
(def manifest
  {:schema :wm/machine-action-branches-manifest-v1
   :generated-by "capture_branches.clj"
   :production-function "futon2.aif.policy/select-action"
   :natural-capture :redirected-machinery-tick
   :direct-record-scope :direct-invocation-of-production-fn
   :artifacts written})
(write-edn! "manifest.edn" manifest)
(prn {:capture :complete :artifacts (count written)
      :branches (mapv :branch written)})
