(ns futon2.aif.click2-replay-fixture-test
  "E-cascade-real D4: the CLICK2-D Part 2 replay inputs as test fixtures.

  test/fixtures/click2-replay/ holds, per admitted target of CLICK2-D
  Part 2 (2026-09-24), the EXACT input map
  cascade-problems/constructed-from-interpretations hands to
  interpretation-construction/construct once :construction is wired in:
  :target, :want, :observation (C4-observed facts at the pinned futon2 sha
  in sources-manifest.edn, :unknown read as not established per
  cascade-problems' own caller reading), :interpretations
  (select-keys [:guard :produces]) with :interpretation-receipts,
  declared :horizon 4, :move-cost 0 (the tick's value since 891b4af6; with the lane's real G a cost of 1 declines M-aif-eig's plan, gain 0.33, so the stand-in G below is why the fixture would construct at either cost), and a
  fixture-declared :budget (no :construction exists upstream at the pinned
  sha, so no upstream budget exists). :evaluate-g is a function and cannot
  live in EDN; the test injects a stand-in that prefers any non-empty
  cascade (G=10 for the empty precedence, 0 otherwise) — it is NOT the
  judge's G, and the assertion is about construction, not ranking.

  Pin 1: every fixture loads and reproduces the pre-D4 behaviour — WITHOUT
  :construction, assemble + admit still refuses each of the four targets
  :no-constructed-candidate (the same replay shape as
  futon2.aif.abstention-record-test).

  Pin 2: the constructor called directly on the
  M-aif-policy-conditioned-eig fixture returns :status :constructed (D15,
  89e1b475) and every candidate's construction receipt NAMES
  :unreached-wants (present, never a substituted value; the one absent
  want, :hole/h42fceb4ad48b, is produced by :aif/two-layer-calibration
  within horizon 4, so the vector is honestly empty).

  Falsifier: editing the observation so the false want reads true must
  make the direct construction refuse — there is nothing new to produce
  (:want-already-observed), so a fixture that always constructs fails."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as cascade-sources]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.report.war-machine :as wm]))

(def fixture-dir "test/fixtures/click2-replay")

(def click2-targets
  "The four admitted targets of CLICK2-D Part 2, in manifest order."
  ["M-f11-find-production-successor"
   "M-aif-policy-conditioned-eig"
   "M-wm-08-external-f2"
   "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade"])

(defn- read-edn [name]
  (edn/read-string (slurp (io/file fixture-dir name))))

(defn- load-fixture [target]
  (read-edn (str target ".edn")))

(def ^:private evaluate-g-stand-in
  "G stand-in: 10 for the empty cascade, 0 for any non-empty precedence.
  Makes the construction move worth taking (value = 10 - move-cost > 0)
  without pretending to be the judge's G over policies."
  (fn [candidate] (if (seq (:precedence candidate)) 0 10)))

(deftest fixtures-load-with-manifest-and-pinned-shape
  (let [manifest (read-edn "sources-manifest.edn")]
    (is (= :wm/click2-replay-manifest-v1 (:fixture/schema manifest)))
    (is (string? (:futon2-sha manifest)))
    (is (= 5 (count (:declared-files manifest)))
        "all five declared cascade-source files are hashed")
    (is (= (set click2-targets) (set (keys (:fixtures manifest))))))
  (doseq [target click2-targets]
    (let [m (load-fixture target)]
      (is (= :wm/click2-replay-v1 (:fixture/schema m)))
      (is (= target (:target m)))
      (is (and (vector? (:want m)) (seq (:want m)))
          (str target " carries a non-empty want"))
      (is (every? boolean? (vals (:observation m)))
          (str target " observation is fully boolean (:unknown already read as not established)"))
      (is (every? (set (keys (:observation m))) (:want m))
          (str target " every want token is observed"))
      (is (pos-int? (:horizon m)))
      (is (number? (:move-cost m)))
      (is (and (integer? (get-in m [:budget :max-moves]))
               (pos-int? (get-in m [:budget :max-expansions]))))
      (is (= (set (keys (:interpretations m)))
             (set (keys (:interpretation-receipts m))))
          (str target " every interpretation carries a receipt")))))

(deftest without-construction-all-four-targets-still-refuse
  ;; The pre-D4 behaviour, pinned: with the declared sources as they stand
  ;; (no :construction injection), assembly + admission refuses each target
  ;; :no-constructed-candidate on :new-wanted-token-within-horizon.
  (let [sources (cascade-sources/with-context-fn (cascade-sources/load-declared))
        horizon (or (:horizon-steps sources) 2)
        assembled (wm/assemble-cascade-problems
                   {:targets (vec (keys (:universes sources)))
                    :sources (assoc sources :horizon-steps horizon)})
        admissions (mapv #'wm/admit-cascade-problem (:problems assembled))
        refusals (into (vec (:refusals assembled)) (keep :refusal admissions))
        by-target (into {} (map (juxt :target identity)) refusals)]
    (doseq [target click2-targets]
      (is (= :no-constructed-candidate (get-in by-target [target :kind]))
          (str target " still refuses :no-constructed-candidate")))))

(deftest direct-construction-on-eig-fixture-constructs
  (let [fixture (load-fixture "M-aif-policy-conditioned-eig")
        result (construction/construct
                (assoc (dissoc fixture :fixture/schema)
                       :evaluate-g evaluate-g-stand-in))]
    (is (= :constructed (:status result)))
    (is (seq (:candidates result)))
    (doseq [candidate (:candidates result)]
      (is (= :machine-constructed (get-in candidate [:construction-receipt :kind])))
      (is (contains? (:construction-receipt candidate) :unreached-wants)
          "the receipt NAMES :unreached-wants (D15), never an untyped absence")
      (is (vector? (get-in candidate [:construction-receipt :unreached-wants]))))
    (is (some #(= [:aif/two-layer-calibration] (:precedence %))
              (:candidates result))
        "the absent want :hole/h42fceb4ad48b is produced by :aif/two-layer-calibration")))

(deftest falsifier-false-want-read-true-refuses-construction
  (let [fixture (load-fixture "M-aif-policy-conditioned-eig")
        edited (assoc-in fixture [:observation :hole/h42fceb4ad48b] true)
        result (construction/construct
                (assoc (dissoc edited :fixture/schema)
                       :evaluate-g evaluate-g-stand-in))]
    (is (= :refused (:status result))
        "with every want already observed there is nothing new to produce")
    (is (= :want-already-observed (:kind result)))
    (is (empty? (:candidates result)))))
