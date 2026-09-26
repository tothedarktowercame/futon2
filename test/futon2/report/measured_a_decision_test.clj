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
     write.

  F1a-2b (2026-09-26, F1c-D §3 and §5): the record carries the rates VALUE
  beside its digest, and :rates-sha is the digest of that same value
  (rates-match-digest?); a record whose :rates is not the digested value
  fails that check. Live-shaped: the decision's lanes call cascade-lane with
  no :observation-labels, exactly as production does, and no production
  caller of cascade-lane passes any (war_machine.clj's three calls,
  evidence_emit's three; only click_measurement_test supplies labels), so
  the R5 lane's own certificate records every token :measurement :absent
  and the decision writes the typed absence. The labels stop before the
  lane: nothing in production admits them."
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

(defn- rates-match-digest?
  "F1a-2b: the record's :rates-sha is the digest of the record's own :rates."
  [ma]
  (and (contains? ma :rates)
       (= (:rates-sha ma) (wm/sha256-hex (wm/canonical-pr (:rates ma))))))

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
    (is (= [:C3] (:classes ma)))
    (is (= qualified (:rates ma))
        "F1a-2b: the rates VALUE is on the record, equal to sourced-rates'
         output target-qualified")
    (is (rates-match-digest? ma)
        "F1a-2b: :rates-sha is the digest of the record's own :rates")))

(deftest bad-case-rates-not-the-digested-value
  ;; F1a-2b bad case: a record carrying a :rates value other than the one
  ;; its :rates-sha digests (the sourced value with one cell moved) fails
  ;; the value-digest check the sourced test relies on.
  (let [ma (:measured-a (decision labels-opt))
        [k cell] (first (:rates ma))
        tampered (assoc-in ma [:rates k] (update cell :false-neg #(+ 0.25 (double %))))]
    (is (rates-match-digest? ma))
    (is (not (rates-match-digest? tampered))
        "BAD CASE: :rates is not the digested value, and the check fails")))

(deftest live-shaped-the-labels-stop-before-the-lane
  ;; F1a-2b live-shaped case. The decision's lanes call
  ;; (cascade-lane (:cascade-problem problem)) with no opts, as production
  ;; does; the R5 step's certificate therefore records every located token
  ;; unmeasured, and the decision on the production path (no
  ;; :observation-labels) writes the typed absence. This pins the finding:
  ;; closing it needs a production source of admitted labels, which does
  ;; not exist at this sha.
  (let [problem (get-in (assembled) [:problems 0 :cascade-problem])
        lane (wm/cascade-lane problem)
        prov (get-in (first (:ranked lane)) [:certificate :rates-provenance])
        ma (:measured-a (decision {}))]
    (is (nil? (:stopped-at lane)) (pr-str (:refusal lane)))
    (is (seq (:measurement prov)) "the R5 lane records a measurement per token")
    (is (every? #{:absent} (vals (:measurement prov)))
        "production's R5 lane: no labels, every token unmeasured")
    (is (= {:status :absent :reason :no-measured-rates} ma)
        "so the decision writes the absence, not a value")
    (is (not (contains? ma :rates)))))

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
