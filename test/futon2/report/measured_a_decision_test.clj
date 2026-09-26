(ns futon2.report.measured-a-decision-test
  "F1a-2-I (PROOF-2a-PLAN ⟨2⟩2d F1, packet of 2026-09-26): the tick's
  admitted decision carries the measured-A version the step's likelihood
  will use, at [:decision :measured-a]. Write only: the decision still
  scores with the class model.

  Wire tests (per futon3c wm-wire-ledger-test's definition — a value sent
  over the wire, checked at both ends):

  1. hermetic tick WITH sourced rates: the decision's :measured-a carries a
     :rates-sha equal to the digest of what observation-rates/sourced-rates
     returns for the same records (recomputed here, independently of the
     decision);
  2. no records: the typed absence {:status :absent :reason
     :no-measured-rates} — never a digest of the identity/zero kernel;
  3. the bad case: force the no-records path to digest the zero kernel and
     show that test 2 then fails (an absence reads as a value);
  4. the existing decision score is byte-identical with and without the
     write."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.aif.observation-rates :as observation-rates]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.war-machine :as wm]))

;; A real measurement for the fixture's only class (:C3, checkable): three
;; admitted-:present labels, one recorded false (false-neg 1/3); one
;; admitted-:absent label recorded false (false-pos 0/1). The rates are the
;; ratios of these counts, so sourced-rates assembles measured cells.
(def ^:private c3-labels
  [{:token-class :C3 :admitted :present :recorded true}
   {:token-class :C3 :admitted :present :recorded true}
   {:token-class :C3 :admitted :present :recorded false}
   {:token-class :C3 :admitted :absent :recorded false}])

(def ^:private c3-subjects {:C3 12})

(defn- assembled []
  (cp/assemble {:targets [fixture/tick-1-target]
                :sources (locfix/locate-all fixture/tick-1-sources)}))

(defn- opts [extra]
  (merge fixture/live-c-opts extra))

(defn- decision [extra-opts]
  (:decision (wm/cascade-decision (assembled) (opts extra-opts))))

(def ^:private labels-opt
  {:observation-labels {fixture/tick-1-target {:labels c3-labels
                                               :subjects c3-subjects}}})

(defn- contract []
  (edn/read-string (slurp (io/resource "wm/observation-contract.edn"))))

(deftest sourced-rates-reach-the-decision-as-a-version
  (let [d (decision labels-opt)
        ma (:measured-a d)
        ;; the writer's claim, recomputed: sourced-rates over the same
        ;; records and the same locators, target-qualified the decision's
        ;; way, digested the same canonical way
        sourced (observation-rates/sourced-rates
                 c3-labels c3-subjects nil
                 (get-in (assembled) [:problems 0 :cascade-problem :locators])
                 (contract))
        qualified (into {} (map (fn [[tok v]] [[fixture/tick-1-target tok] v]))
                        (:rates sourced))]
    (is (= :sourced (:status sourced)) "the fixture's records source rates")
    (is (= :wm/measured-a-v1 (:schema ma)))
    (is (= (wm/sha256-hex (wm/canonical-pr qualified)) (:rates-sha ma))
        "the decision's :rates-sha is the digest of what sourced-rates
         returns for the same records")
    (is (= :futon2.aif.observation-rates/sourced-rates (:source ma))
        "sourced-rates' provenance, verbatim")
    (is (= [:C3] (:classes ma)))))

(deftest no-records-is-a-typed-absence-never-a-digested-zero-kernel
  (let [ma (:measured-a (decision {}))]
    (is (= {:status :absent :reason :no-measured-rates} ma)
        "no admitted labels: nothing measured is sourced, so the absence is
         written — the zero kernel is never digested")
    (is (not (contains? ma :rates-sha))
        "an absence must not read as a value")))

(deftest bad-case-digesting-the-zero-kernel-fails-the-absence
  ;; The bad case, demonstrated: if the no-records path digested the
  ;; identity/zero kernel, the decision would carry a :rates-sha with
  ;; nothing measured — an absence reading as a value. Drive exactly that
  ;; regression (by redefinition) over the same no-records tick and show
  ;; that the absence test above then fails: the written map carries a
  ;; :rates-sha and is not the typed absence.
  (let [zero-kernel-digesting
        (fn [problems _labels]
          (let [sourced (observation-rates/sourced-rates
                         [] {} nil
                         (get-in (first problems) [:cascade-problem :locators])
                         (contract))]
            {:schema :wm/measured-a-v1
             :rates-sha (wm/sha256-hex (wm/canonical-pr (:rates sourced)))
             :source (:source sourced)
             :classes [:C3]}))
        d (with-redefs [wm/measured-a-version zero-kernel-digesting]
            (decision {}))
        ma (:measured-a d)]
    (is (some? (:rates-sha ma))
        "BAD CASE: the no-records path digested the zero kernel…")
    (is (not= {:status :absent :reason :no-measured-rates}
              (select-keys ma [:status :reason]))
        "…so no-records-is-a-typed-absence-never-a-digested-zero-kernel
         fails: an absence reads as a value")))

(deftest the-write-does-not-touch-the-score
  ;; The decision's score — the selected action and the selection
  ;; posterior — is byte-identical with and without the write's inputs.
  (let [with-write (decision labels-opt)
        without-write (decision {})
        score (fn [d] (pr-str (select-keys d [:action :softmax-weights
                                              :selection-law])))]
    (is (= :wm/measured-a-v1 (get-in with-write [:measured-a :schema])))
    (is (= :absent (get-in without-write [:measured-a :status])))
    (is (= (score without-write) (score with-write))
        "the existing decision score is byte-identical with and without
         the write")))
