(ns futon2.aif.eig-shadow-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
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

(deftest passing-held-out-calibration-joins-the-default-off-shadow
  (let [shadow-packet (shadow/collect base)
        calibration (edn/read-string
                     (slurp (io/resource "wm/eig/held-out-calibration.edn")))
        packet (shadow/calibrated-packet shadow-packet calibration)]
    (is (= :wm/eig-shadow-calibration-packet-v1 (:schema packet)))
    (is (= :observed (get-in packet [:empirical :status])))
    (is (= (:metrics calibration) (get-in packet [:empirical :metrics])))
    (is (= {:winner :unchanged :abstain :unchanged :scale :unchanged}
           (select-keys (:selection-effects packet) [:winner :abstain :scale])))
    (is (= (get-in shadow-packet [:model-relative :prior-entropy])
           (get-in packet [:model-relative :prior-entropy])))
    (is (= (:packet-sha256 packet)
           (identity/digest (dissoc packet :packet-sha256))))))

(deftest calibration-join-fails-closed
  (let [shadow-packet (shadow/collect base)
        calibration (edn/read-string
                     (slurp (io/resource "wm/eig/held-out-calibration.edn")))]
    (is (= :shadow-digest-mismatch
           (:eig-shadow/refusal
            (refusal #(shadow/calibrated-packet
                       (assoc-in shadow-packet [:model :id] "tampered") calibration)))))
    (is (= :held-out-calibration-not-passing
           (:eig-shadow/refusal
            (refusal #(shadow/calibrated-packet
                       shadow-packet
                       (assoc calibration :status :failing
                              :failing-reasons [:mean-brier-outside-bounds]))))))
    (is (= :calibration-model-mismatch
           (:eig-shadow/refusal
            (refusal #(shadow/calibrated-packet
                       shadow-packet
                       (assoc-in calibration [:split :model :theta] :other))))))))
