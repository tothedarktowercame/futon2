(ns futon2.aif.eig-shadow-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.a4a :as a4a]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.eig-shadow :as shadow]))

(def model
  {:prior {:left 0.5 :right 0.5}
   :predicted-observations {:left 0.5 :right 0.5}
   :posteriors {:left {:left 1.0 :right 0.0}
                :right {:left 0.0 :right 1.0}}})

(def base
  {:model model :model-id "binary-fixture"
   :model-source-sha256 (apply str (repeat 64 "a"))
   :posterior-state (a4a/corpus->concentration
                     {:capabilities ["inspect"]
                      :edges [["inspect" "left"] ["inspect" "right"]]
                      :discharges []})
   :observation {:schema a4a/observation-schema
                 :capability "inspect" :outcome "left"
                 :evidence/id "shadow-observation-1"}
   :selection-before {:winner :inspect :abstain? false :scale 1.0}
   :selection-after {:winner :inspect :abstain? false :scale 1.0}})

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(deftest default-off-shadow-separates-consistency-from-calibration
  (let [held (shadow/collect base)
        observed (shadow/collect
                  (assoc base :held-out
                         {:predicted-probability 0.8 :realised true
                          :prior {:left 0.5 :right 0.5}
                          :posterior {:left 0.9 :right 0.1}
                          :evidence/id "held-out-1"}))]
    (is (= :model-relative (get-in held [:model-relative :status])))
    (is (< (get-in held [:model-relative :consistency-error]) 1.0e-12))
    (is (= {:status :held :reason :held-out-evidence-unavailable}
           (:empirical held)))
    (is (= :observed (get-in observed [:empirical :status])))
    (is (< (Math/abs (- 0.04 (get-in observed [:empirical :brier]))) 1.0e-12))
    (is (= :none (:controller-effect observed)))
    (is (= :shared (get-in observed [:shared-update :status])))
    (is (= a4a/updater-id (get-in observed [:model :updater-id])))
    (is (= (get-in observed [:shared-update :hypothetical-receipt :after])
           (get-in observed [:shared-update :observed-receipt :after])))
    (is (= [:hypothetical :observed]
           [(get-in observed [:shared-update :hypothetical-receipt
                              :observation/source])
            (get-in observed [:shared-update :observed-receipt
                              :observation/source])]))
    (is (= (:packet-sha256 observed)
           (identity/digest (dissoc observed :packet-sha256))))))

(deftest off-mode-replay-fails-closed
  (is (= :off-replay-selection-drift
         (:eig-shadow/refusal
          (refusal #(shadow/collect (assoc base :selection-after {:winner :wait})))))))
