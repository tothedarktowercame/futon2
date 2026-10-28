(ns futon2.aif.t3-held-resume-stop-line-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.tripwire :as tripwire]
            [futon2.aif.wm.cascade-runner-seam-test :as seam]))

(use-fixtures :once hermetic/with-hermetic-stores)

(deftest held-resume-stop-line-clears-t3-before-opportunity-end
  (let [opts (hermetic/runner-repair-options)
        root (:repair-root opts)
        attempt-id "ea1-held-resume--attempt-002"
        observation {:phase :opportunity
                     :transition :end
                     :outcome :incomplete
                     :external-attempt-id attempt-id
                     :repair-root root
                     :tripwire/force? true}
        witnesses-at-end (atom nil)
        finding {:attempt-id attempt-id
                 :observation {:phase :held-open-cascade-resume}
                 :repair-class :incomplete-recoverable
                 :machine-repo "/home/joe/code/futon2"
                 :target "M-held"
                 :failure-stage :cascade-resume
                 :outcome :incomplete
                 :failure-kind :open-cascade-review-answer-invalid
                 :error "Selected cascade held in open-cascade phase"
                 :failure-data {:held-phase {:open-cascade true
                                             :repairable true}}
                 :discharge-contract
                 {:requires [:complete-existing-author-artifact
                             :independent-review
                             :grounded-existing-commit]
                  :artifact-shape :code-commit}}]
    (testing "the historical producer ordering is red against the real T3"
      (is (= [:missing-durable-stop-line]
             (mapv :kind (tripwire/evaluate-wire :T3 observation))))
      (is (empty? (filter #(and (.isFile %)
                                (.endsWith (.getName %) ".edn"))
                          (file-seq (io/file root "findings"))))))
    (#'runner/record-held-resume-stop-line!
     opts finding
     #(reset! witnesses-at-end (tripwire/evaluate-wire :T3 observation)))
    (let [files (vec (filter #(and (.isFile %)
                                   (.endsWith (.getName %) ".edn"))
                             (file-seq (io/file root "findings"))))
          retained (edn/read-string (slurp (first files)))]
      (testing "the runner uses the ordinary repair producer for one open finding"
        (is (= 1 (count files)))
        (is (= :open (:repair/status retained)))
        (is (= :incomplete-recoverable (:repair/class retained)))
        (is (= attempt-id (:attempt-id retained))))
      (testing "T3 rereads that finding before the end event"
        (is (empty? @witnesses-at-end))
        (is (empty? (tripwire/evaluate-wire :T3 observation)))))))

(def ^:private resume-error
  {:status :error
   :kind :open-cascade-review-answer-invalid
   :phase :cascade-execution
   :repairable true
   :review-answer {:status :invalid}})

(defn- held-resume-click [store producer phases at-end]
  (let [root (:repair-root store)
        result (#'seam/click!
                [seam/px seam/py]
                (merge store
                       {:repair-system-record-fn producer
                        :phase-log-fn
                        (fn [event]
                          (swap! phases conj event)
                          (when (and (= :opportunity (:phase event))
                                     (= :end (:transition event)))
                            (let [observation (assoc event
                                                     :repair-root root
                                                     :tripwire/force? true)
                                  files (vec (filter
                                              #(and (.isFile %)
                                                    (.endsWith (.getName %) ".edn"))
                                              (file-seq (io/file root "findings"))))]
                              (reset! at-end
                                      {:event event
                                       :repair-root root
                                       :files files
                                       :witnesses
                                       (tripwire/evaluate-wire :T3 observation)}))))
                        :cascade-reading-pins
                        {"M-current"
                         {:excerpt-digest (#'seam/hex [:excerpt])
                          :analysis-digest (#'seam/hex [:analysis])}}
                        :cascade-occurrence-readings
                        (fn [_] seam/square-readings)
                        :open-cascade-resume-fn (constantly resume-error)})
                {:task {:id "M-current"}
                 :bind-cascade-identity
                 {:readings (fn [_] seam/square-readings)}})]
    (assoc result :captured-phases @phases
                  :at-end @at-end)))

(defn- opportunity-end [{:keys [captured-phases]}]
  (first (filter #(and (= :opportunity (:phase %))
                       (= :end (:transition %)))
                 captured-phases)))

(deftest real-held-resume-catch-publishes-only-after-durable-stop-line
  (testing "negative: removing only the producer leaves the real T3 witness"
    (let [store (hermetic/runner-repair-options)
          phases (atom [])
          at-end (atom nil)
          click (held-resume-click store (constantly {:not-persisted true})
                                   phases at-end)
          event (assoc (opportunity-end click)
                       :repair-root (:repair-root store)
                       :tripwire/force? true)]
      (is (= :incomplete (get-in click [:result :outcome])))
      (is (map? (get-in click [:result :checkpoints :selection])))
      (is (= :open-cascade-review-answer-invalid
             (get-in click [:result :checkpoints :selection :sorry
                            :downstream-refusal :kind])))
      (is (empty? (:files @at-end)))
      (is (= [:missing-durable-stop-line]
             (mapv :kind (:witnesses @at-end))))
      (is (= [:missing-durable-stop-line]
             (mapv :kind (tripwire/evaluate-wire :T3 event))))))

  (testing "positive: the ordinary producer is visible when the real end event publishes"
    (let [store (hermetic/runner-repair-options)
          root (:repair-root store)
          phases (atom [])
          at-end (atom nil)
          producer (:repair-system-record-fn store)
          click (held-resume-click store producer phases at-end)
          event (assoc (opportunity-end click)
                       :repair-root root
                       :tripwire/force? true)
          files (:files @at-end)
          finding (edn/read-string (slurp (first files)))]
      ;; The runner's own phase sink snapshots the store and evaluates the
      ;; real T3 synchronously while the opportunity-end event is published.
      (is (= 1 (count files)))
      (is (= (opportunity-end click) (:event @at-end)))
      (is (= root (:repair-root @at-end)))
      (is (= (:external-attempt-id event) (:attempt-id finding)))
      (is (= :open (:repair/status finding)))
      (is (= :incomplete-recoverable (:repair/class finding)))
      (is (= :incomplete (get-in click [:result :outcome])))
      (is (map? (get-in click [:result :checkpoints :selection])))
      (is (empty? (:witnesses @at-end)))))

  (testing "producer failure cannot publish a clear end or return a held success"
    (let [store (hermetic/runner-repair-options)
          phases (atom [])
          at-end (atom nil)
          failure (try
                    (held-resume-click
                     store
                     (fn [_] (throw (ex-info "repair store unavailable"
                                             {:kind :repair-store-unavailable})))
                     phases at-end)
                    nil
                    (catch Throwable e e))]
      (is (some? failure))
      (is (= :repair-store-unavailable (:kind (ex-data failure))))
      (is (nil? @at-end))
      (is (nil? (first (filter #(and (= :opportunity (:phase %))
                                     (= :end (:transition %)))
                               @phases)))))))
