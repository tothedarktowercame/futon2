(ns futon2.aif.m2l-outer-selection-ask-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.wm.outer-action-declared-wants-test :as m2h]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
;; In this isolated process, source freshness is relative to the checkout
;; under test, not the different canonical serving checkout.
(use-fixtures :each
  (fn [f]
    (with-redefs-fn {#'runner/canonical-runner-bytes
                     #(java.nio.file.Files/readAllBytes
                       (java.nio.file.Paths/get "src/futon2/aif/full_loop_runner.clj"
                                                 (make-array String 0)))}
      #(binding [runner/*wm-status-reporting?* false] (f)))))

(defn selected-decision []
  {:status :selected
   :selection-certificate {:selection-route :wm/selected-outer-cascade-policy-v1}
   :action (get-in (m2h/menu m2h/mission-sha (slurp "test/resources/m2h/mission.md"))
                   [:entries 0 :action])})

(defn run-case [decision]
  (let [calls (atom [])
        result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:judge-fn (fn [_] {:judgement {:decision decision :belief {} :belief-pre {}
                                                      :observation {} :free-energy {} :prediction-errors {}
                                                      :precision-state {} :micro-step-trace [] :mode :maintain}})
                        :interpretation-ask-classify-fn (constantly :focused)
                        :interpretation-ask-fn (fn [_ r] (swap! calls conj r)
                                                 {:status :absent :published false :reason :test-no-publication})
                        :repair-system-record-fn (fn [m] {:repair/id "m2l-test" :repair/class (:repair-class m)})
                        :dispatch-fn (fn [& _] (throw (ex-info "Test stops at dispatch" {})))}))]
    {:calls @calls :record (edn/read-string (slurp (:run-record result)))}))

(deftest selected-outer-wants-ask-once-and-retain-result
  (let [decision (selected-decision)
        pin (edn/read-string (slurp "test/resources/m2l/chosen.edn"))
        {:keys [calls record]} (run-case decision)]
    (is (= (:target pin) (get-in decision [:action :target])))
    (is (= [{:target "M-stack-geometry" :stage :outer-policy-effects
             :kind :no-admitted-interpretation}] calls))
    (is (= {:status :absent :published false :reason :test-no-publication}
           (:interpretation-ask record)))))

(deftest unavailable-declaration-does-not-ask
  (let [{:keys [calls record]} (run-case (assoc-in (selected-decision)
                                                  [:action :declared-wants] {:status :unavailable}))]
    (is (empty? calls))
    (is (= :selection-complete-no-ask-required (get-in record [:interpretation-ask :reason])))))

(deftest abstained-interpretation-still-asks
  (let [refusal {:target "M-stack-geometry" :kind :no-admitted-interpretation}
        {:keys [calls record]} (run-case {:status :abstained :refusals [refusal]})]
    (is (= [refusal] calls))
    (is (= :test-no-publication (get-in record [:interpretation-ask :reason])))))
