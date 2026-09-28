(ns futon2.report.measured-a-decision-test
  "Measured-A's value and digest agree with the rates sourced from a real
   entry label snapshot. The joint decision still scores its class model."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.observation-labels :as labels]
            [futon2.aif.observation-label-store :as store]
            [futon2.aif.observation-label-reader :as reader]
            [futon2.aif.observation-label-reader-test :as population]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.aif.observation-rates :as observation-rates]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.war-machine :as wm] [futon2.aif.wm.cascade-decision :as wm-cd]))

(def ^:dynamic ^:private c3-labels nil)
(def ^:private c3-subjects {:C3 10})
(def ^:dynamic ^:private labels-opt nil)

(use-fixtures :each
  (fn [f]
    (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                       "measured-a-labels-" (make-array java.nio.file.attribute.FileAttribute 0)))
          path (io/file dir "labels.edn")
          ids (labels/loaded-identities)]
      (try
        (store/init! path)
        (store/record! path
                       (mapv #(checks/check-path-exists {:repo "futon2" :sha population/pin :path %})
                             (concat population/present-paths population/absent-paths)) ids {})
        (binding [c3-labels (:labels (reader/read-rates-inputs path ids))
                  labels-opt {:observation-labels-path (str path)}]
          (f))
        (finally (doseq [file (reverse (file-seq dir))] (io/delete-file file true)))))))

(defn- assembled []
  (cp/assemble {:targets [fixture/tick-1-target]
                :sources (locfix/locate-all fixture/tick-1-sources)}))

(defn- opts [extra]
  (merge fixture/live-c-opts extra))

(defn- decision [extra-opts]
  (:decision (wm-cd/cascade-decision (assembled) (opts extra-opts))))

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
                 c3-labels c3-subjects reader/prior
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

(deftest without-a-store-the-lane-is-unmeasured
  ;; No configured store preserves the historical unmeasured default.
  (let [problem (get-in (assembled) [:problems 0 :cascade-problem])
        lane (wm-cd/cascade-lane problem)
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
        d (with-redefs [wm-cd/measured-a-version zero-kernel-digesting]
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
