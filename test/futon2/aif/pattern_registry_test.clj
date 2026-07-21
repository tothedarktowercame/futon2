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

(def live-wire-entry
  {:evidence/id "ctx-live-wire"
   :evidence/at "2026-07-15T21:42:07Z"
   :evidence/body
   (pr-str {"event" "context-retrieval"
            "at" "2026-07-15T21:42:07Z"
            "turn" 20
            "query" "content-bound fire pattern"
            "results" [{:id "agent/evidence-over-assertion"
                         :title "Evidence Over Assertion"
                         :score 0.8
                         :rank 1
                         :retrieval-source "futon3a"}]})})

(deftest evidence-base-configuration-pins-stack-wide-precedence-test
  (is (= "http://127.0.0.1:7070"
         (pr/configured-evidence-base {}))
      "the default must reach the IPv4-bound production Evidence Landscape")
  (is (= "https://evidence.example"
         (pr/configured-evidence-base
          {"FUTON3C_PORT" "7099"
           "FUTON3C_SERVER" "https://server.example"
           "FUTON3C_EVIDENCE_BASE" "https://evidence.example"}))
      "the historical evidence-base override wins every fallback")
  (is (= "https://server.example"
         (pr/configured-evidence-base
          {"FUTON3C_SERVER" "https://server.example"
           "FUTON3C_PORT" "7099"})))
  (is (= "http://127.0.0.1:7099"
         (pr/configured-evidence-base
          {"FUTON3C_PORT" "7099"}))))

(deftest aggregate-pattern-candidates-builds-bounded-ranking-test
  (let [patterns (pr/open-patterns sample-entries)]
    (is (= 2 (count patterns))
        "retrieval ids without resolvable pattern artifacts are not targets")
    (is (= "coordination/capability-gate" (:id (first patterns))))
    (is (= 2 (:mentions (first patterns))))
    (is (= ["ctx-1" "ctx-2"] (:evidence-ids (first patterns))))
    (is (= [11 12] (:turns (first patterns))))
    (is (re-matches #"[0-9a-f]{64}" (:pattern-sha256 (first patterns))))))

(deftest live-edn-string-body-resolves-to-content-bound-candidate-test
  (let [pattern (first (pr/open-patterns [live-wire-entry]))]
    (is (= "agent/evidence-over-assertion" (:id pattern)))
    (is (= "/home/joe/code/futon3/library/agent/evidence-over-assertion.flexiarg"
           (:pattern-path pattern)))
    (is (= ["ctx-live-wire"] (:evidence-ids pattern)))
    (is (re-matches #"[0-9a-f]{64}" (:pattern-sha256 pattern)))
    (is (pr/addressable-pattern? pattern))))

(deftest unreadable-wire-body-is-not-authority-test
  (is (empty? (pr/open-patterns
               [{:evidence/id "ctx-bad" :evidence/body "{not edn"}]))))

(deftest can-propose-fire-pattern-when-state-has-patterns-test
  (is (false? (fm/can-propose? {:patterns []} :fire-pattern)))
  (is (false? (fm/can-propose? {} :fire-pattern)))
  (is (false? (fm/can-propose? {:patterns [{:id "coordination/capability-gate"}]}
                               :fire-pattern))
      "an id without a retrieval receipt is not an addressable substrate")
  (is (false? (fm/can-propose? {:patterns [{:id "coordination/capability-gate"
                                             :evidence-ids ["ctx-1"]}]}
                               :fire-pattern))
      "a receipt without a content-bound artifact remains non-addressable")
  (is (true? (fm/can-propose? {:patterns (pr/open-patterns sample-entries)}
                              :fire-pattern))))

(deftest can-execute-fire-pattern-requires-current-target-in-state-test
  (let [state {:patterns (pr/open-patterns sample-entries)}
        action (first (ap/propose pr/pattern-enumerator-proposer state))]
    (is (true? (fm/can-execute? state action)))
    (is (false? (fm/can-execute? state (assoc action :target "missing/pattern"))))
    (is (false? (fm/can-execute? state (assoc action :evidence-ids []))))
    (is (false? (fm/can-execute? state (assoc action :evidence-ids ["spoofed"])))
        "execution provenance must match the current substrate")))

(deftest pattern-enumerator-proposer-emits-candidates-test
  (let [state {:patterns (pr/open-patterns sample-entries)}
        candidates (ap/propose pr/pattern-enumerator-proposer state)]
    (is (= 2 (count candidates)))
    (is (every? #(= :fire-pattern (:type %)) candidates))
    (is (= #{"coordination/capability-gate"
             "agent/evidence-over-assertion"}
           (set (map :target candidates))))
    (is (every? :evidence-ids candidates))
    (is (every? #(fm/can-execute? state %) candidates))
    (is (false? (fm/can-execute?
                 state (assoc (first candidates)
                              :pattern-summary "caller-controlled rewrite")))
        "execution rejects a payload that does not match the substrate")
    (is (every? :retrieval-score candidates))
    (is (every? :pattern-path candidates))
    (is (every? #(re-matches #"[0-9a-f]{64}" (:pattern-sha256 %)) candidates))
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
    (is (= (:pattern-sha256 action)
           (get-in construction [:actuation-contract :pattern-sha256])))
    (is (= [:author-dispatch :independent-review :grounded-implementation]
           (get-in construction [:actuation-contract :production-route])))
    (is (nil? (pr/actuation-construction
               (dissoc action :evidence-ids))))
    (is (nil? (pr/actuation-construction
               (dissoc action :proposer-id))))
    (is (nil? (pr/actuation-construction
               (assoc action :pattern-sha256 (apply str (repeat 64 "0")))))
        "construction rejects a stale or substituted artifact digest")))

(deftest artifact-address-rejects-id-path-substitution-test
  (let [path "/home/joe/code/futon3/library/coordination/capability-gate.flexiarg"]
    (is (some? (pr/pattern-artifact-receipt
                "coordination/capability-gate" path)))
    (is (nil? (pr/pattern-artifact-receipt
               "agent/evidence-over-assertion" path)))
    (is (nil? (pr/pattern-artifact-receipt
               "coordination/capability-gate" "/etc/passwd")))))

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
    (is (seq learn-targets)
        "other action classes are still gated, still surfaced as gaps")))
