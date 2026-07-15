(ns futon2.aif.pattern-registry-test
  "Tests for the context-retrieval-backed pattern substrate adapter that flips
   `forward-model/can-propose? :fire-pattern` to true under live substrate."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.pattern-registry :as pr]))

(def sample-entries
  [{:evidence/id "ctx-1"
    :evidence/body {:event "context-retrieval"
                    :at "2026-05-20T10:00:00Z"
                    :turn 11
                    :query "mission drift and adapter gap"
                    :results [{:id "coordination/capability-gate"
                               :title "Capability Gate"
                               :score 0.9
                               :pattern-path "/home/joe/code/futon3/library/coordination/capability-gate.flexiarg"
                               :retrieval-rationale "hotword overlap on capability + gate"
                               :hotwords ["capability" "gate"]
                               :sigils ["⚖/衡" "🧭/引"]
                               :then "Make the capability boundary explicit"
                               :because "Honest action selection requires real substrate"
                               :next-steps ["enumerate targets" "record receipts"]}
                              {:id "agent/evidence-over-assertion"
                               :title "Evidence Over Assertion"
                               :score 0.6}]}}
   {:evidence/id "ctx-2"
    :evidence/body {:event "context-retrieval"
                    :at "2026-05-20T10:05:00Z"
                    :turn 12
                    :query "pattern retrieval quality"
                    :results [{:id "coordination/capability-gate"
                               :title "Capability Gate"
                               :score 0.8}
                              {:id "coordination/candidate-set-hygiene"
                               :title "Candidate Set Hygiene"
                               :score 0.7}]}}
   {:evidence/id "chat-1"
    :evidence/body {:event "chat-turn"
                    :text "not a retrieval entry"}}])

(deftest aggregate-pattern-candidates-builds-bounded-ranking-test
  (let [patterns (pr/open-patterns sample-entries)]
    (is (= 3 (count patterns)))
    (is (= "coordination/capability-gate" (:id (first patterns))))
    (is (= 2 (:mentions (first patterns))))
    (is (= ["ctx-1" "ctx-2"] (:evidence-ids (first patterns))))
    (is (= [11 12] (:turns (first patterns))))
    (is (> (:weighted-score (first patterns))
           (:weighted-score (second patterns))))))

(deftest can-propose-fire-pattern-when-state-has-patterns-test
  (is (false? (fm/can-propose? {:patterns []} :fire-pattern)))
  (is (false? (fm/can-propose? {} :fire-pattern)))
  (is (false? (fm/can-propose? {:patterns [{:id "coordination/capability-gate"}]}
                               :fire-pattern))
      "an id without a retrieval receipt is not an addressable substrate")
  (is (true? (fm/can-propose? {:patterns [{:id "coordination/capability-gate"
                                            :evidence-ids ["ctx-1"]}]}
                              :fire-pattern))))

(deftest can-execute-fire-pattern-requires-target-in-state-test
  (let [state {:patterns [{:id "coordination/capability-gate"
                           :evidence-ids ["ctx-1"]}
                          {:id "agent/evidence-over-assertion"
                           :evidence-ids ["ctx-2"]}]}]
    (is (true? (fm/can-execute? state {:type :fire-pattern
                                       :proposer-id :pattern-enumerator
                                       :target "coordination/capability-gate"
                                       :evidence-ids ["ctx-1"]})))
    (is (true? (fm/can-execute? state {:type :fire-pattern
                                       :proposer-id :pattern-enumerator
                                       :target "agent/evidence-over-assertion"
                                       :evidence-ids ["ctx-2"]})))
    (is (false? (fm/can-execute? state {:type :fire-pattern
                                        :proposer-id :pattern-enumerator
                                        :target "missing/pattern"
                                        :evidence-ids ["ctx-1"]})))
    (is (false? (fm/can-execute? state {:type :fire-pattern
                                        :proposer-id :pattern-enumerator
                                        :target "coordination/capability-gate"
                                        :evidence-ids []})))
    (is (false? (fm/can-execute? state {:type :fire-pattern
                                        :proposer-id :pattern-enumerator
                                        :target "coordination/capability-gate"
                                        :evidence-ids ["spoofed"]}))
        "execution provenance must match the current substrate")))

(deftest pattern-enumerator-proposer-emits-candidates-test
  (let [state {:patterns (pr/open-patterns sample-entries)}
        candidates (ap/propose pr/pattern-enumerator-proposer state)]
    (is (= 3 (count candidates)))
    (is (every? #(= :fire-pattern (:type %)) candidates))
    (is (= #{"coordination/capability-gate"
             "coordination/candidate-set-hygiene"
             "agent/evidence-over-assertion"}
           (set (map :target candidates))))
    (is (every? :evidence-ids candidates))
    (is (every? #(fm/can-execute? state %) candidates))
    (is (false? (fm/can-execute?
                 state (assoc (first candidates)
                              :pattern-summary "caller-controlled rewrite")))
        "execution rejects a payload that does not match the substrate")
    (is (every? :retrieval-score candidates))
    (is (every? #(string? (:rationale %)) candidates))
    (let [first-candidate (first candidates)]
      (is (= "/home/joe/code/futon3/library/coordination/capability-gate.flexiarg"
             (:pattern-path first-candidate)))
      (is (= ["⚖/衡" "🧭/引"] (:sigils first-candidate)))
      (is (= "Make the capability boundary explicit"
             (:pattern-summary first-candidate)))
      (is (= ["enumerate targets" "record receipts"]
             (:next-steps first-candidate))))))

(deftest malformed-retrievals-never-become-candidates-test
  (let [entries [{:evidence/body {:event "context-retrieval"
                                  :results [{:id "missing/receipt"
                                             :title "No receipt"
                                             :score 1.0}]}}
                 {:evidence/id "ctx-invalid"
                  :evidence/body {:event "context-retrieval"
                                  :results [{:id "" :title "No id"
                                             :score 1.0}]}}]
        state {:patterns (pr/open-patterns entries)}]
    (is (empty? (:patterns state)))
    (is (false? (fm/can-propose? state :fire-pattern)))
    (is (empty? (ap/propose pr/pattern-enumerator-proposer state)))))

(deftest fire-pattern-construction-is-typed-and-fail-closed-test
  (let [action (first (ap/propose pr/pattern-enumerator-proposer
                                  {:patterns (pr/open-patterns sample-entries)}))
        construction (pr/actuation-construction action)]
    (is (= :fire-pattern-actuation (:construction-kind construction)))
    (is (= (:target action) (get-in construction [:actuation-contract :target])))
    (is (= (:evidence-ids action)
           (get-in construction [:actuation-contract :evidence-ids])))
    (is (= [:author-dispatch :independent-review :grounded-implementation]
           (get-in construction [:actuation-contract :production-route])))
    (is (nil? (pr/actuation-construction
               (dissoc action :evidence-ids))))
    (is (nil? (pr/actuation-construction
               (dissoc action :proposer-id))))))

(deftest integration-bootstrap-no-longer-surfaces-fire-pattern-gap-test
  (let [state-with-patterns {:patterns (pr/open-patterns sample-entries)
                             :observation {}
                             :belief {}}
        proposed (ap/propose ap/bootstrap-proposer state-with-patterns)
        learn-targets (->> proposed
                           (filter #(= :learn-action-class (:type %)))
                           (map :target-class)
                           set)]
    (is (not (contains? learn-targets :fire-pattern)))
    (is (contains? learn-targets :address-sorry))
    (is (contains? learn-targets :open-mission))))
