(require '[clojure.java.io :as io] '[clojure.pprint :as pp]
         '[clojure.string :as str] '[futon2.aif.revision-scanner :as scanner]
         '[futon2.aif.action-identity :as identity]
         '[futon2.aif.surprise :as surprise] '[futon2.aif.token-outcome :as outcome])
(def output (first *command-line-args*))
(def as-of (second *command-line-args*))
(def repo "/home/joe/code/futon2")
(def run-path (str repo "/data/wm-runs/tick-run-record-2026-09-21-1789964661.edn"))
(def selection-path (str repo "/data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-001/002-selection.edn"))
(def task-path "/home/joe/code/futon3c/data/wm-d-task-enactment/action-a5d2326d-a73a-447c-8bc7-7f77c8bbc187.edn")
(def run (scanner/read-record run-path))
(def task (scanner/read-record task-path))
(def terms (get-in run [:decision :g-term-decomposition :policies 0 :terms]))
(def prediction
  (outcome/freeze-prediction
   {:action (get-in task [:dispatch :occurrence :action/value])
    :selection-certificate
    {:precision-family {:model-id "historical-1789964661"
                        :model {:q0 (get-in terms [:D :value])
                                :horizon (count (get-in terms [:Q :value :steps]))}}
     :token-belief-stage {:domain-inputs
                         (mapv #(hash-map :target (get-in % [:snapshot :target])
                                          :declaration (:snapshot %))
                               (get-in task [:dispatch :declarations]))}}}))
(def rows (surprise/records {:comparison (outcome/compare-outcomes prediction (:after-token-evidence task)
                                                                  (get-in task [:revision-pair :after]))
                            :occurrence (get-in task [:dispatch :occurrence])
                            :declared-at (:recorded-at (scanner/read-record selection-path))}))
(assert (= 1 (count rows)))
(def replay-path (.getAbsolutePath (io/file output "historical-surprises.edn")))
(io/make-parents replay-path)
(spit replay-path (pr-str rows))
(def live-files (vec (sort (for [d (.listFiles (io/file repo "data"))
                               :when (str/starts-with? (.getName d) "wm-full-loop")
                               f (file-seq d)
                               :when (and (.isFile f) (= "surprises.edn" (.getName f))
                                          (= "retained" (.getName (.getParentFile f))))] (.getPath f)))))
(def config {:repos [repo] :surprise-files live-files
             :run-files (vec (sort (map str (filter #(str/ends-with? (str %) ".edn")
                                                     (.listFiles (io/file repo "data/wm-runs"))))))
             :as-of as-of :unanswered-after-seconds 21600
             :interval-reason "Six hours: within-working-day follow-up signal, not a claim that learning must occur on this deadline."
             :bindings [{:repo repo :path "src/futon2/aif/cascade_model_manifest.clj"
                         :namespace 'futon2.aif.cascade-model-manifest :model-part :B-effect}
                        {:repo repo :path "resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn"
                         :target "M-aif-policy-conditioned-eig" :model-part :B-effect
                         :loaded-evidence :unavailable-for-declarations}]})
(spit (io/file output "live-config.edn") (pr-str config))
(spit (io/file output "historical-config.edn") (pr-str (assoc config :surprise-files [replay-path])))
(def result {:provenance {:historical :reconstructed-not-live-close
                          :inputs (mapv #(hash-map :path % :sha256 (identity/sha256 (slurp %)))
                                        [run-path selection-path task-path])}
             :live (scanner/scan config)
             :historical-replay (scanner/scan (assoc config :surprise-files [replay-path]))})
(with-open [w (io/writer (io/file output "real-results.edn"))] (pp/pprint result w))
(prn {:live-surprises (get-in result [:live :surprise-count])
      :historical-surprises (get-in result [:historical-replay :surprise-count])
      :historical-friction (get-in result [:historical-replay :friction-candidates])})
