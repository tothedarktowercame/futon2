(ns futon2.aif.selection-always-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(def obligations
  [{:repair/id "repair-selection-open" :repair/status :open
    :repair/class :machine-failure :attempt-id "prior-attempt"}
   {:repair/id "repair-selection-other" :repair/status :open
    :repair/class :independent-review-failure :attempt-id "prior-other"}])

(defn selection-run [lines]
  (let [selected (atom nil) transforms (atom 0)
        result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:repair-open-fn (constantly lines)
                        :judgement-transform-fn (fn [j] (swap! transforms inc) j)
                        ;; Stop after real selection/discrimination; no agent dispatch.
                        :construct-fn (fn [entry]
                                        (reset! selected entry)
                                        (throw (ex-info "Fixture ends after selection"
                                                        {:outcome :incomplete
                                                         :failure-stage :construction
                                                         :failure-kind :fixture-end})))
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))]
    {:result result :selected @selected :transforms @transforms
     :record (edn/read-string (slurp (:run-record result)))}))

(deftest open-repairs-are-evidence-not-selection-preemption
  (let [{:keys [result selected transforms record]} (selection-run obligations)
        cell (get-in result [:checkpoints :selection :judgment])
        evidence {:count 2 :ids (mapv :repair/id obligations)}]
    (is (= 1 transforms))
    (is (= fixture/selected-action (:action selected)))
    (is (= :fixture-end (get-in result [:data :failure-kind])))
    (is (= (:decision fixture/judgement) (:controller-decision cell)))
    (is (true? (get-in cell [:selection-reasons :discrimination :passes?])))
    (is (= 2 (count (:ranked-candidates cell))))
    (is (= evidence (:open-stop-lines cell)))
    (is (= evidence (:open-stop-lines record)))
    (is (not= :stop-the-line (:selection-source selected)))
    (is (not= :stop-the-line (get-in cell [:selection-reasons :source])))
    (is (= (get-in fixture/judgement [:decision :selection-law])
           (get-in record [:decision :selection-law])))))

(deftest no-open-repairs-preserves-ordinary-selection
  (let [{:keys [result selected transforms record]} (selection-run [])
        cell (get-in result [:checkpoints :selection :judgment])]
    (is (= 1 transforms))
    (is (= fixture/selected-action (:action selected)))
    (is (= (:decision fixture/judgement) (:controller-decision cell)))
    (is (true? (get-in cell [:selection-reasons :discrimination :passes?])))
    (is (= {:count 0 :ids []} (:open-stop-lines record)))
    (is (= :fixture-end (get-in result [:data :failure-kind])))))

(deftest recoverable-memory-cannot-divert-after-construction
  (let [reads (atom []) reached (atom false)
        line {:repair/id "unrelated-recovery" :repair/status :open
              :repair/class :incomplete-recoverable :attempt-id "old-attempt"
              :failure-stage :author-wait :failure-data {:job-id "old-running-job"}}
        result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:repair-open-fn (constantly [line])
                        :trace-fn (constantly nil)
                        :read-job-fn (fn [& args] (swap! reads conj args)
                                       {:job-id "old-running-job" :state "running"})
                        :target-repo-fn (fn [& _] "/tmp")
                        :dispatch-fn (fn [& _]
                                       (reset! reached true)
                                       (throw (ex-info "Ordinary dispatch reached"
                                                       {:outcome :incomplete :failure-stage :author-dispatch
                                                        :failure-kind :fixture-author-boundary})))}))]
    (is @reached)
    (is (empty? @reads))
    (is (= :fixture-author-boundary (get-in result [:data :failure-kind])))
    (is (= {:count 1 :ids ["unrelated-recovery"]}
           (:open-stop-lines (edn/read-string (slurp (:run-record result))))))))

(deftest explicit-historical-action-binds-its-selected-obligation
  (doseq [mismatch? [false true]]
    (let [target (assoc (second obligations) :repair/class :machine-failure)
          lines [(first obligations) target]
          admission {:schema :wm/historical-repair-admission-v1
                     :repair/status :awaiting-validation
                     :repair/id (:repair/id target) :verification-id "selected-verification"
                     :verification-artifact {:path "/fixture/source" :sha256 (apply str (repeat 64 "a"))}
                     :actors {:author "zai-5" :reviewer "codex-1"}}
          action {:type :revalidate-historical-repair :target (:repair/id target)
                  :repair-obligation target :admission admission}
          seen (atom nil) executed (atom [])
          result (runner/run-opportunity!
                  (merge (fixture/isolated-runner-opts)
                         {:repair-open-fn (constantly lines)
                          :judge-fn (constantly {:judgement
                                                {:decision {:action action :controller-score 1.0
                                                            :selection-law {:applied :cascade-selection-posterior
                                                                            :posterior [[action 1.0]]}}}})
                          :historical-verification-candidate-fn
                          (fn [obligation]
                            (reset! seen obligation)
                            (cond-> admission mismatch? (assoc :repair/id "wrong-obligation")))
                          :historical-verification-execute-fn
                          (fn [request]
                            (swap! executed conj request)
                            (assoc admission :verification-attempt (:execution-identity request)
                                   :verification-source (:verification-artifact admission)
                                   :verification-artifact {:path "/fixture/admitted" :sha256 (apply str (repeat 64 "b"))}))
                          :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))]
      (is (= target @seen))
      (is (= action (get-in result [:checkpoints :selection :judgment :selected-action])))
      (if mismatch?
        (do (is (empty? @executed))
            (is (= :historical-verification-admission-invalid (get-in result [:data :failure-kind]))))
        (do (is (= [target] (mapv :obligation @executed)))
            (is (= :historical-verification-awaiting-validation (:outcome result))))))))
