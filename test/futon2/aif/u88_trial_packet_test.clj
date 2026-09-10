(ns futon2.aif.u88-trial-packet-test
  "Fixture-scope validation of the prepared U88 trial-1 task pin
   (runs/RUN4-preparation-2026-09-10/u88-trial-1/): the pin refuses against
   PRODUCTION because the mission is DRAFT (real eligibility must refuse
   until explicit reviewed activation); a disposable OPEN copy only validates
   the exact packet with the exact action :advance-mission target
   M-u88-contextual-preferences; stale bytes refuse.  Pinned-config loading
   and full series preflight are tested at futon3c, where those namespaces
   live.  Nothing here is a live-valid pin, a dispatch, or an activation."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.c-fold-config :as digest]
            [futon2.aif.mission-registry :as missions]
            [futon2.aif.run4-task-pin :as task-pin])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def packet-dir
  "holes/labs/wm-contract/runs/RUN4-preparation-2026-09-10/u88-trial-1")
(def task-pin-path (str packet-dir "/task-pin.edn"))
(def run-config-path (str packet-dir "/run-config.edn"))
(def series-pin-path (str packet-dir "/series-pin.edn"))
(def mission-path
  "holes/labs/wm-contract/runs/RUN4-preparation-2026-09-10/draft-missions/M-u88-contextual-preferences.md")
(def mission-id "M-u88-contextual-preferences")

(defn- read-text [path] (slurp path))

(defn- stub-ports [resolve-mission]
  {:read-text read-text
   :resolve-mission resolve-mission
   ;; DELIBERATE FIXTURE STUB, not the serving boundary's admissibility rule.
   :action-admissible? (fn [mission action]
                         (and (= :open (:status-class mission))
                              (= :advance-mission (:type action))))})

(defn- with-temp-code-root [f]
  (let [root (Files/createTempDirectory "u88-trial-packet"
                                        (into-array FileAttribute []))]
    (try
      (f (str root))
      (finally
        (doseq [^File child (reverse (file-seq (io/file (str root))))]
          (.delete child))))))

(defn- refusal-reason [thunk]
  (try
    (thunk)
    (catch clojure.lang.ExceptionInfo e
      (:reason (ex-data e)))))

(deftest production-draft-mission-refuses-the-task-pin
  ;; Real eligibility validation: the mission is DRAFT, production
  ;; open-missions does not carry it, so the pin must refuse.
  (is (not (contains? (set (map :id (missions/open-missions))) mission-id)))
  (let [open-by-id (into {} (map (juxt :id identity) (missions/open-missions)))]
    (is (= :unknown-mission-mapping
           (refusal-reason
            #(task-pin/validate (read-text task-pin-path)
                                (stub-ports (fn [id] (get open-by-id id)))))))))

(deftest disposable-open-copy-validates-the-exact-packet-and-action
  (with-temp-code-root
    (fn [root]
      (let [target (io/file root "futon2/holes/missions"
                            (str mission-id ".md"))]
        (io/make-parents target)
        (spit target (str/replace
                      (read-text mission-path)
                      "DRAFT — NON-LIVE; independent review and explicit activation required"
                      "OPEN — fixture episode milestone pending"))
        (let [open (into {} (map (juxt :id identity)
                                 (missions/open-missions
                                  {:missions (:missions (missions/load-missions root))})))
              envelope (task-pin/validate (read-text task-pin-path)
                                         (stub-ports (fn [id] (get open id))))]
          (is (:valid? envelope))
          (is (= "ec0555250f116814370eb77810eeb946302b1d50c277a2f30f9cbc3a7e64c9d6"
                 (get-in envelope [:task-pin :sha256])))
          (is (= mission-id (get-in envelope [:task-pin :selected-task-id])))
          (is (= :advance-mission
                 (get-in envelope [:mission-action :action :type])))
          (is (= mission-id
                 (missions/mission-target-id
                  (get-in envelope [:mission-action :action :target]))))
          (is (= "zai-2" (get-in envelope [:casting :author])))
          (is (= false (:permitted? (:launch envelope)))))))))

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
