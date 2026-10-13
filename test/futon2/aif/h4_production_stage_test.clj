(ns futon2.aif.h4-production-stage-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.cascade-habit-read-test :as stores]
            [futon2.aif.wm.cascade-decision :as wm-cd]))

(use-fixtures :once hermetic/with-hermetic-stores)

(deftest actual-production-decision-refuses-missing-prefix
  (stores/with-store
   (fn [path]
     (with-redefs [habit/default-path path task/default-root (str path "-missing-task-store")]
       (let [unadmitted (problems/assemble
                         {:targets [fixture/tick-1-target]
                          :sources (locators/locate-all fixture/tick-1-sources)})
             first-admission (#'wm-cd/admit-cascade-problem
                              (first (:problems unadmitted)))
             ;; The shared production-decision fixture attaches the pinned
             ;; three-halves square to every interpretation receipt. This
             ;; test withholds only Prefix-F, its stated boundary.
             assembled (@#'fixture/assemble*
                        {:targets [fixture/tick-1-target]
                         :sources fixture/tick-1-sources})
             refusal (try
                       (wm-cd/cascade-decision
                        assembled (assoc fixture/live-c-opts :cascade-habit-path path))
                       nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))
             candidates (get-in refusal [:refusal :detail :candidate-ids])
             steps (mapv (fn [i candidate]
                           {:step {:status :present
                                   :policy-key (prefix-admission/candidate-key candidate)
                                   :occurrence {:flight "h4-production-control"
                                                :click (str "candidate-" i)}
                                   :s-prev {:value {#{} 1}}
                                   :q {#{} 1}
                                   :f 0.0}
                            :path "h4-production-stage-test"
                            :sha256 "fixture"})
                         (range) candidates)
             selected (wm-cd/cascade-decision
                       assembled
                       (assoc fixture/live-c-opts
                              :cascade-habit-path path
                              :conditioning-steps
                              {:steps steps :read [] :unread []}))]
         (is (= :three-halves-square-invalid
                (get-in first-admission [:declines 0 :reason]))
             "missing construction evidence remains an earlier typed boundary")
         (is (= :free-energy-not-supplied
                (get-in refusal [:refusal :kind])))
         (is (= 3 (get-in refusal [:refusal :detail :n-candidates])))
         (is (= 3 (get-in refusal [:refusal :detail :n-without-f])))
         (is (= 3 (count candidates)))
         (is (some? (get-in selected [:decision :action]))
             "the exact admitted Prefix-F population proceeds to selection"))))))
