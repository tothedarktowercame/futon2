(ns futon2.aif.m2y4d-effect-target-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.aif.wm.cascade-continuation :as continuation]
            [futon2.aif.wm.on-prod-history :as history]))

(defn capture-request [target]
  (let [seen (atom {}) calls (atom 0)]
    ;; Stub all I/O boundaries, not the target transport: real resume,
    ;; continuation wrapper, executor registry, executor and request builder.
    (with-redefs [history/load-checkpoint
                  (fn [& _] (swap! calls inc)
                    {:selected-entry {:action {:target target}}})
                  runner/run-phase! (fn [_ _ _ f _] (f))
                  decision/execute-selected-cascade! (fn [_ _] {:status :cascade-ready})
                  runtime/designated-agent-answer-fn
                  (fn [_ seat prompt-fn _]
                    (fn [request]
                      (when (= seat "author")
                        (swap! seen assoc :request request :prompt (prompt-fn request)))
                      {}))
                  continuation/continue-certified-cascade!
                  (fn [{:keys [executors]}]
                    ((:wm/pattern-apply executors)
                     {:cascade-digest "digest" :node {:occurrence-id ["pattern" 0]}}))]
      (runtime/continue-on-prod-cascade!
       {:source-run-id "source" :source-attempt-id "attempt"
        :author "author" :reviewer "reviewer" :target "wrong-caller-target"
        :cascade-close-fn identity}))
    (assoc @seen :checkpoint-reads @calls)))

(deftest sealed-mission-target-reaches-author-once
  (let [target (:target (edn/read-string (slurp "test/resources/m2w/f016ed51-target.edn")))
        {:keys [request prompt checkpoint-reads]} (capture-request target)]
    (is (= "M-self-improvement-loop" target))
    (is (= target (:target request)) "Pre-change request loses the selected mission")
    (is (= 1 checkpoint-reads))
    (is (= (:request-id request)
           (evidence/value-digest (dissoc request :schema :request-id))))
    (is (not= (:request-id request)
              (evidence/value-digest (assoc (dissoc request :schema :request-id)
                                           :target "foreign-mission"))))
    (is (.contains prompt "do not contact the reviewer"))
    (is (.contains prompt "runner sends your answer"))))

(deftest missing-target-is-explicit-not-invented
  (doseq [target [nil "" "  "]]
    (is (= {:status :absent :reason :selected-target-unavailable}
           (get-in (capture-request target) [:request :target])))))

(deftest legacy-four-argument-request-is-unchanged
  (let [node {:occurrence-id ["p" 0] :pattern "p" :clause {}}
        body {:cascade-digest "d" :occurrence-id ["p" 0]
              :pattern "p" :clause {} :author "a" :reviewer "r"}]
    (is (= (assoc body :schema :wm/cascade-effect-request-v1
                       :request-id (evidence/value-digest body))
           (continuation/effect-request "d" node "a" "r")))))
