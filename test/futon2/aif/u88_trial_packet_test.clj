(ns futon2.aif.u88-trial-packet-test
  "Validation of the activated, exactly pinned U88 trial-1 task. The canonical
   OPEN mission resolves through production discovery and only the exact
   :advance-mission action targeting M-u88-contextual-preferences validates.
   Validation remains non-dispatching and returns launch permission false."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.c-fold-config :as digest]
            [futon2.aif.mission-registry :as missions]
            [futon2.aif.run4-task-pin :as task-pin]))


(def packet-dir
  "holes/labs/wm-contract/runs/RUN4-preparation-2026-09-10/u88-trial-1")
(def task-pin-path (str packet-dir "/task-pin.edn"))
(def run-config-path (str packet-dir "/run-config.edn"))
(def series-pin-path (str packet-dir "/series-pin.edn"))
(def mission-path "holes/missions/M-u88-contextual-preferences.md")
(def mission-id "M-u88-contextual-preferences")

(defn- read-text [path] (slurp path))

(defn- stub-ports [resolve-mission]
  {:read-text read-text
   :resolve-mission resolve-mission
   ;; DELIBERATE FIXTURE STUB, not the serving boundary's admissibility rule.
   :action-admissible? (fn [mission action]
                         (and (= :open (:status-class mission))
                              (= :advance-mission (:type action))))})

(defn- refusal-reason [thunk]
  (try
    (thunk)
    (catch clojure.lang.ExceptionInfo e
      (:reason (ex-data e)))))

(deftest production-open-mission-validates-the-exact-packet-and-action
  (let [open-by-id (into {} (map (juxt :id identity) (missions/open-missions)))
        mission (get open-by-id mission-id)
        envelope (task-pin/validate (read-text task-pin-path)
                                    (stub-ports (fn [id] (get open-by-id id))))]
    (is (= :open (:status-class mission)))
    (is (:valid? envelope))
    (is (= "9cf34ffcff3a78bbe2b60e5c8cbfa674887ff3c34de5a41c49ea57315b63b717"
           (get-in envelope [:task-pin :sha256])))
    (is (= mission-path (-> envelope :source-pins first :path)))
    (is (= mission-id (get-in envelope [:task-pin :selected-task-id])))
    (is (= {:type :advance-mission :target mission-id}
           (get-in envelope [:mission-action :action])))
    (is (= "zai-2" (get-in envelope [:casting :author])))
    (is (= false (:permitted? (:launch envelope))))))

(deftest production-pin-refuses-any-nonexact-action-target
  (let [open-by-id (into {} (map (juxt :id identity) (missions/open-missions)))
        text (read-text task-pin-path)
        aliased (str/replace text
                  ":target \"M-u88-contextual-preferences\""
                  ":target \"futon2-d/mission/u88-contextual-preferences\"")]
    (is (= :action-mission-mismatch
           (refusal-reason #(task-pin/validate
                             aliased
                             (stub-ports (fn [id] (get open-by-id id)))))))))

(deftest series-pin-consistency-fixture-scope
  ;; Structural consistency of the frozen series manifest with the actual
  ;; task-pin bytes. The full futon3c controller preflight runs at the serving
  ;; boundary; this check pins the packet's internal identity only.
  (let [series (edn/read-string (read-text series-pin-path))
        trial (first (:trials series))
        pin-sha (digest/sha256 (read-text task-pin-path))]
    (is (= :wm/run4-series-pin-v1 (:schema series)))
    (is (= :frozen (:status series)))
    (is (= :attempt-each-once-even-after-fail-or-block (:stop-rule series)))
    (is (= [1] (mapv :ordinal (:trials series))))
    (is (= pin-sha (:pin-sha256 trial)))
    (is (= pin-sha (:sha256 (:packet trial))))
    (is (not= (:author (:casting series)) (:reviewer (:casting series))))
    (is (pos? (count (:source-pins series))))))

(deftest serving-and-recording-requirements-are-declared-not-attested
  (let [sheet (edn/read-string (read-text run-config-path))
        declaration (:serving-declaration sheet)]
    (is (= {"FUTON_WM_FPI_DARK" "1"
            "FUTON_WM_BETA_DARK" "1"
            "FUTON_WM_TRACE_POLICY_DETAILS" "1"}
           (:required-environment declaration)))
    (is (= {:model :single-level :scope :RUN4} (:hierarchy declaration)))
    (is (= :wm/realized-recording-v1
           (get-in declaration [:recording-requirement :contract])))
    (is (nil? (:effective-environment-attestation sheet)))))
