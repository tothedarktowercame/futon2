(ns futon2.aif.m2y4e-contact-absence-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight-runner :as flight-runner]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]))

(def cascade-digest
  "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")

(def node
  {:occurrence-id ["agent/trail-enables-return" 1]
   :pattern "agent/trail-enables-return"
   :clause {:operation :apply :capability :wm/pattern-apply}})

(defn- receipt [roster answer-of]
  (with-redefs [runner/agent-roster (constantly roster)
                flight-runner/agency-answer-fn
                (fn [{:keys [seat]}]
                  (fn [_] (answer-of seat)))]
    ((:wm/pattern-apply
      (runtime/production-cascade-executors
       {:author "zai-6" :reviewer "zai-2" :target "M-self-improvement-loop"}))
     {:cascade-digest cascade-digest :node node})))

(deftest effect-contact-absence-is-retained
  (testing "a designated seat that is not idle is a typed non-contact"
    (let [r (receipt {"zai-6" {:status "running" :invoke-ready? false}
                      "zai-2" {:status "running" :invoke-ready? false}}
                     (fn [_] (throw (ex-info "unavailable seat was contacted" {}))))]
      (is (= :reviewed-effect-invalid (:kind r)))
      (is (= {:status :absent :reason :not-contacted :seat "zai-6"}
             (:author-answer r)))
      (is (= {:status :absent :reason :not-contacted :seat "zai-2"}
             (:review-answer r)))))

  (testing "a done Agency answer with no JSON has an explicit parse reason"
    (let [r (receipt {"zai-6" {:status "idle" :invoke-ready? true}
                      "zai-2" {:status "idle" :invoke-ready? true}}
                     (fn [seat]
                       {:state "done" :job-id (str seat "-job")
                        :result "completed, but no JSON object"}))]
      (is (= :reviewed-effect-invalid (:kind r)))
      (is (= {:status :absent :reason :unparseable-answer
              :seat "zai-6" :job-id "zai-6-job"}
             (:author-answer r)))
      (is (= {:status :absent :reason :unparseable-answer
              :seat "zai-2" :job-id "zai-2-job"}
             (:review-answer r)))))

  (testing "a good answer is decoded exactly as before"
    (let [r (receipt {"zai-6" {:status "idle" :invoke-ready? true}
                      "zai-2" {:status "idle" :invoke-ready? true}}
                     (fn [seat]
                       {:state "done" :job-id (str seat "-job")
                        :result (if (= seat "zai-6")
                                  "{\"answer\":\"unchanged\"}"
                                  "review prose without JSON")}))]
      (is (= :reviewed-effect-invalid (:kind r)))
      (is (= {:answer "unchanged"} (:author-answer r))))))
