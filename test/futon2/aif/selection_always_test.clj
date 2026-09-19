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
