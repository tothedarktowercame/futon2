(ns futon2.aif.abstention-record-test
  "E-cascade-real D8 / PROOF-2a AR-16: the abstained tick record carries its
  typed declines.

  Before this change the tick run record's :decision kept only
  :g-term-decomposition on the abstention path: the runner's
  persist-run-record! read the decision from
  [:checkpoints :selection :judgment], but an abstained tick throws before
  a judgment cell is written (its decision travels on the :no-selection
  sorry cell), and the record's select-keys then dropped :status and
  :refusals. CLICK2-D Part 2 (2026-09-24, click 2, cohort
  wm-contract-machinery-77 attempt-001) found the per-target declines only
  in the cohort's 002-selection.edn, never on the tick record.

  Pin 1 replays admission over the four admitted targets' declared sources
  at HEAD (resources/wm/cascade-sources/, read-only) and asserts the
  record carrier names each with :no-constructed-candidate and its
  :no-new-wanted-token declines. Test 2 is the falsifier: a decision whose
  refusals are non-empty but whose record carrier is missing or
  :not-abstained fails abstention-record-ok?. Test 3: an abstention with
  no refusal list is a typed absence, never an empty vector."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as cascade-sources]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.report.war-machine :as wm]))

(def click2-targets
  "The four admitted targets of CLICK2-D Part 2, each declined
  :no-constructed-candidate on :new-wanted-token-within-horizon."
  #{"M-f11-find-production-successor"
    "M-aif-policy-conditioned-eig"
    "M-wm-08-external-f2"
    "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade"})

(defn- replay-click2-admission
  "The smallest path that yields the judge's :refusals: assemble the
  declared sources exactly as the war machine does (common declared
  horizon), then admit-cascade-problem per problem — the same function
  cascade-decision calls, with the same dropped-candidates shape
  cascade-decision builds."
  []
  (let [sources (cascade-sources/with-context-fn (cascade-sources/load-declared))
        horizon (or (:horizon-steps sources) 2)
        assembled (wm/assemble-cascade-problems
                   {:targets (vec (keys (:universes sources)))
                    :sources (assoc sources :horizon-steps horizon)})
        admissions (mapv #'wm/admit-cascade-problem (:problems assembled))
        refusals (into (vec (:refusals assembled)) (keep :refusal admissions))
        dropped (vec (concat (:dropped-candidates assembled)
                             (map (fn [r] {:target (:target r) :stage :assembly
                                           :reason (:kind r)
                                           :missing-evidence [(:missing r)]})
                                  (:refusals assembled))
                             (mapcat :declines admissions)))]
    {:decision {:status :abstained :refusals refusals}
     :dropped-candidates dropped}))

(deftest click2-pin-carrier-names-four-targets-typed-declines
  (let [{:keys [decision dropped-candidates]} (replay-click2-admission)
        carrier (runner/abstention-carrier decision dropped-candidates)
        by-target (into {} (map (juxt :target identity)) (:targets carrier))]
    (is (= :abstained (:status carrier)))
    (is (= :abstained (:status decision)))
    (doseq [target click2-targets]
      (is (= :no-constructed-candidate (get-in by-target [target :kind]))
          (str target " refused :no-constructed-candidate"))
      (is (= [:new-wanted-token-within-horizon] (get-in by-target [target :missing]))
          (str target " missing :new-wanted-token-within-horizon"))
      (is (some #(= :no-new-wanted-token (:reason %))
                (get-in by-target [target :declines]))
          (str target " carries a :no-new-wanted-token decline"))
      (is (every? #(and (:candidate %) (:reason %) (vector? (:missing-evidence %)))
                  (get-in by-target [target :declines]))
          (str target " declines are typed, not substituted values")))))

(deftest record-ok-falsifies-missing-or-contradicting-carrier
  (let [decision {:status :abstained
                  :refusals [{:target "T-a" :kind :no-constructed-candidate
                              :missing [:new-wanted-token-within-horizon]}]}
        dropped [{:target "T-a" :stage :candidate-admission :candidate "c1"
                  :reason :no-new-wanted-token
                  :missing-evidence [:new-wanted-token-within-horizon]}]
        good-record {:decision {:abstention (runner/abstention-carrier decision dropped)}}
        missing-carrier {:decision {}}
        wrong-carrier {:decision {:abstention {:status :not-abstained}}}]
    (is (runner/abstention-record-ok? decision good-record))
    (is (not (runner/abstention-record-ok? decision missing-carrier))
        "non-empty refusals with no carrier on the record must fail")
    (is (not (runner/abstention-record-ok? decision wrong-carrier))
        "non-empty refusals read as :not-abstained must fail")))

(deftest abstention-without-refusal-list-is-typed-absence
  (let [carrier (runner/abstention-carrier {:status :abstained} [])]
    (is (= :absent (:status carrier)))
    (is (= :judge-recorded-no-refusal-list (:reason carrier)))
    (is (not (contains? carrier :targets))
        "never an empty :targets vector read as \"nothing declined\""))
  (is (= {:status :not-abstained}
         (runner/abstention-carrier {:status :selected} [])))
  (is (= :absent (:status (runner/abstention-carrier nil nil)))
      "no recorded decision is a typed absence, never :not-abstained"))
