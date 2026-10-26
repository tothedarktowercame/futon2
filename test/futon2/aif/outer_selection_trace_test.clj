(ns futon2.aif.outer-selection-trace-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.outer-selection-trace :as ost]))

;; Live pin: the projection fixture is extracted verbatim (values) from
;; data/wm-full-loop-machinery-150/wm-contract-machinery-150-v1/attempt-002/002-selection.edn
;; by test/resources/outer-selection-trace/extract.clj. The expected trace
;; below is written from that projection.

(def projection
  (edn/read-string (slurp "test/resources/outer-selection-trace/attempt-002-projection.edn")))

(deftest attempt-002-trace-pinned
  (is (= {:schema :wm/outer-selection-trace-v1,
          :stages
          [{:stage :s0-registry-field,
            :where "[:support] + [:excluded]",
            :in 786,
            :out 786,
            :dropped {}}
           {:stage :s1-standing-map-ownership,
            :where "[:support], :ineligible-reason over [:excluded]",
            :in 109,
            :out 109,
            :dropped
            {:repair-finding/root-missing 102,
             :repair-finding/attached-to-root 25,
             :repair-finding/root-not-current 13,
             :mission-standing-conflict 1,
             :task-document-source-unavailable 1,
             :ownership/actively-held 3,
             :ownership/ambiguous-active-owner 6,
             :pipeline/not-on-current-map 526}}
           {:stage :s2-target-readings,
            :where "[:policy-field :families] by :status, :kind over :failures",
            :in 109,
            :out 7,
            :dropped
            {:no-current-target-reading 101,
             :occurrence-squares-not-authored 1}}
           {:stage :s3-policies,
            :where "(count :policies) over computed [:policy-field :families]",
            :in 7,
            :out 26,
            :dropped {},
            :per-family
            {"M-formal-war-machine" 4,
             "M-digital-nomad-patterns" 4,
             "M-self-improvement-loop" 4,
             "M-stack-geometry" 4,
             "M-war-machine-wiring" 4,
             "M-pattern-posteriors" 2,
             "M-recommendation-bindings" 4}}
           {:stage :s4-canonical-binding,
            :where
            "[:policy :selected-outer-cascade-policy :sealed-menu :entries] vs :refusals/:failures",
            :in 26,
            :out 4,
            :dropped
            {:occurrence-identity-absent 9,
             :seed-not-in-graph 2,
             :target-class-unknown 10,
             :isolated-seed 2},
            :refused-policy-ids
            {:occurrence-identity-absent
             ["M-stack-geometry/policy-3510d20984850ce7cb77"
              "M-stack-geometry/policy-5e99e0fdafc6d8843142"
              "M-stack-geometry/policy-3d3fd6f3858ef13e9d4b"
              "M-war-machine-wiring/policy-45024a469a5a3aafbed2"
              "M-war-machine-wiring/policy-4f3d6579dd4a1d00ff53"
              "M-war-machine-wiring/policy-920e64ceb4c9f6df9eaa"
              "M-recommendation-bindings/policy-cefd4ef77de4a606eaff"
              "M-recommendation-bindings/policy-33e76e8f9c231964a56c"
              "M-recommendation-bindings/policy-4dae6d79c4d1989f2b42"]},
            :unbound-targets
            ["M-formal-war-machine"
             "M-digital-nomad-patterns"
             "M-pattern-posteriors"]}
           {:stage :s5-canonical-g-frontier,
            :where
            "[:policy :canonical-g-frontier :entries] by :status, :reason over :refused",
            :in 4,
            :out 4,
            :dropped {:absent :refused-rows-not-retained},
            :horizon 10,
            :rows
            [{:target-id "M-self-improvement-loop",
              :G -3.9443039235640867,
              :rank 2,
              :status :scored}
             {:target-id "M-stack-geometry",
              :G -2.004267726446009,
              :rank 3,
              :status :scored}
             {:target-id "M-war-machine-wiring",
              :G -3.9560226735640867,
              :rank 1,
              :status :scored}
             {:target-id "M-recommendation-bindings",
              :G -3.9560226735640867,
              :rank 1,
              :status :scored}]}
           {:stage :s6-selection,
            :where
            "[:policy :canonical-policy-selection :status/:selected/:draw]",
            :in 4,
            :out 1,
            :dropped {},
            :basis :canonical-G-min-over-scored-frontier,
            :selected-target "M-war-machine-wiring",
            :tie
            {:law :uniform-over-equal-minima,
             :u 0.22534045104810763,
             :candidates
             ["3a5228e7942579f3e0095ee4a902b1b08a0a9d0bf7a11c966c52bddbbb4e4475"
              "13c0068ddbc12266370e59d82812d9b10472a38831e7e61f0e9aef6a9e690e11"]}}]}
         (ost/trace projection))))
