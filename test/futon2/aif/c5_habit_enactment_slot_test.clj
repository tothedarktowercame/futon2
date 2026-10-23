(ns futon2.aif.c5-habit-enactment-slot-test
  "C5 habit: Q6 retains the ordinary route's missing enactment as a sealed,
   typed absence rather than treating selected precedence as an enactment."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.policy-prefix-admission :as prefix]
            [futon2.aif.previous-run :as previous-run])
  (:import [java.nio.file Files]))

(def absent-enactment
  {:status :absent :reason :ordinary-pattern-enactment-not-observed})

(defn- temp-dir []
  (str (Files/createTempDirectory "c5-habit-enactment-"
                                  (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- coapply-decision []
  (let [occurrence [:fixture 1]
        action0 {:target "M-fixture"
                 :id "M-fixture/policy-c5-habit"
                 :precedence {:co-apply {:units [occurrence]
                                         :patterns {occurrence {:pattern-id "fixture/pattern"}}}}
                 :arrangement {:nodes [{:occurrence-id occurrence
                                        :pattern "fixture/pattern"}]
                               :edges []
                               :precedence [occurrence]}}
        action (assoc action0 :policy-key (prefix/reading-policy-key action0))]
    {:action action}))

(defn- terminal
  ([id] (terminal id nil))
  ([id enactment]
   (cond-> {:run-id id
            :started-at "2026-10-23T00:00:00Z"
            :decision (coapply-decision)}
     enactment (assoc :enactment enactment))))

(deftest ordinary-coapply-crosses-the-real-q6-close-as-typed-absence
  (let [dir (temp-dir)
        value (previous-run/terminal (terminal "one"))
        _ (previous-run/start! dir "one" "2026-10-23T00:00:00Z")
        _ (previous-run/close! dir value "run-one.edn")
        predecessor (previous-run/start! dir "two" "2026-10-24T00:00:00Z")]
    (is (= absent-enactment (:enactment value)))
    (is (= absent-enactment (get-in predecessor [:previous :enactment])))
    (is (= :present (get-in value [:choice :status])))
    ;; The selected precedence names what could be enacted; it cannot occupy
    ;; the enactment slot before an executor has observed attempts.
    (is (nil? (get-in value [:enactment :precedence])))
    (is (not= (get-in (coapply-decision) [:action :precedence])
              (:enactment value)))))

(deftest supplied-enactment-is-sealed-without-reinterpretation
  (let [enactment {:schema :wm/enactment-v1
                   :click "click-1"
                   :candidate "M-fixture/policy-c5-habit"
                   :attempts [{:n 1 :pattern "fixture/pattern" :success true}]
                   :conformance {:deviations []}}
        value (previous-run/terminal (terminal "one" enactment))]
    (is (= enactment (:enactment value)))
    (is (previous-run/intact? value))
    (is (not (previous-run/intact?
              (assoc-in value [:enactment :attempts 0 :success] false))))))

(deftest old-sealed-terminal-without-enactment-remains-a-valid-predecessor
  (let [dir (temp-dir)
        current (previous-run/terminal (terminal "one"))
        old (previous-run/seal (dissoc current :enactment :sha256))
        _ (previous-run/start! dir "one" "2026-10-23T00:00:00Z")
        _ (previous-run/close! dir old "run-one.edn")
        predecessor (previous-run/start! dir "two" "2026-10-24T00:00:00Z")]
    (is (previous-run/intact? old))
    (is (= :present (:status predecessor)))
    (is (= old (:previous predecessor)))
    (is (not (contains? (:previous predecessor) :enactment)))))
