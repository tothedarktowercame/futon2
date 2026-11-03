(ns futon2.aif.cascade-proposals-withhold-test
  "PROOF-wm-works ⟨1⟩5: the repair withhold is narrowed to what it is for —
  generated proposals and sourceless repair targets stay withheld; a repair
  target with an ADMITTED DECLARED cascade source is not withheld and is
  recorded supplied-by-declaration. All through the tick's own assembly
  path (load-declared + assemble + load-supply + record-supply)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-proposals :as cprop]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.repair-obligation :as repair]))

(def declared-id
  "repair-ea1-19929445d661380bc279f9e439dd2808eec422ce3bf96f7efd0c168da7e3b153--attempt-002-author-delivery-wrong-repository")
(def sourceless-id
  "repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-revision-unchanged")
(def t (str "T-" declared-id))
(def source-template-target
  "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(def repair-root
  (.getPath (io/file (io/resource "fixtures/repair-obligations/withhold"))))

(defn- tick-assembly [target]
  ;; the tick's own path: declared loader, assemble, real supply scan,
  ;; record-supply — exactly what withheld the ticket in run 1790110142.
  (let [loaded (cs/load-declared)
        declared-candidate (first (get-in loaded [:candidates source-template-target]))
        declared (reduce-kv
                  (fn [sources k section]
                    (if (and (map? section) (contains? section source-template-target))
                      (assoc-in sources [k t] (get section source-template-target))
                      sources))
                  loaded
                  loaded)
        sources (-> declared
                    (assoc-in [:candidates t] [declared-candidate])
                    cs/with-context-fn)
        assembled (cp/assemble {:targets [target]
                                :sources (assoc sources :horizon-steps 4)})
        supply (cprop/load-supply {:repair-root repair-root})
        recorded (cprop/record-supply assembled sources supply)]
    {:assembled assembled
     :supply supply
     :recorded recorded}))

(deftest reference-ticket-with-declared-source-is-not-withheld
  (let [{:keys [assembled supply recorded]} (tick-assembly t)]
    ;; 444fb018 no longer witnesses this rule: its finding now says
    ;; `:repair/class :environmental-hold`. Use a pinned actionable finding
    ;; and a test-owned declaration to isolate the withhold distinction.
    (is (some #{declared-id} (get-in supply [:repair-scan :actionable-ids]))
        "precondition: the ticket's finding is an open repair obligation")
    ;; the ticket HAS a declared source with candidates
    (is (seq (get-in (first (:problems assembled)) [:constructed-candidates])))
    ;; NOT withheld: the problem survives record-supply
    (is (some #(= t (:target %)) (:problems recorded))
        "the declared-source repair target reaches the ranked candidates")
    ;; the old declines no longer fire for it
    (is (not-any? #(and (= t (:target %))
                        (= :universe-not-admitted (:kind %))
                        (= :repair-closure-observation-unavailable (:reason %)))
                  (:refusals recorded))
        ":universe-not-admitted no longer fires for the declared-source ticket")
    (is (not-any? #(and (= t (:target %)) (= :repair-closure-observation-unavailable (:reason %)))
                  (:dropped-candidates recorded))
        ":proposal-supply decline no longer fires for the declared-source ticket")
    ;; the distinction is recorded
    (let [supplied (get-in recorded [:repair-withhold-distinction :supplied-by-declaration])]
      (is (some #(= t (:target %)) supplied) (pr-str supplied))
      (is (some seq (map :source-paths supplied))
          "the rule and the source path are recorded"))))

(deftest sourceless-repair-target-is-still-withheld
  ;; A repair target with an open finding and NO declared source: withheld
  ;; with the unchanged reason. (The withhold fires on targets IN the
  ;; assembled problems; a sourceless target reaches problems through the
  ;; mission-hole path or a hand-built problem, so we supply the problem
  ;; directly and run the REAL record-supply with the REAL scan.)
  (let [sources (cs/with-context-fn (cs/load-declared))
        supply (cprop/load-supply {:repair-root repair-root})
        actionable-ids (get-in supply [:repair-scan :actionable-ids])
        sourceless (str "T-" sourceless-id)]
    ;; `:actionable-ids` replaced `:open-finding-ids` in 072dc0404.
    (is (some #{sourceless-id} actionable-ids)
        "the pinned finding is actionable-open")
    (is (empty? (get-in sources [:candidates sourceless]))
        "the pinned actionable target has no declared source")
    (let [assembled {:problems [{:target sourceless
                                 :cascade-problem {:facts {} :want [:x]
                                                   :interpretations {}
                                                   :constructed-candidates []}}]
                     :refusals [] :dropped-candidates []}
          recorded (cprop/record-supply assembled sources supply)]
      (is (not-any? #(= sourceless (:target %)) (:problems recorded))
          "the sourceless repair target is withheld")
      (is (some #(and (= sourceless (:target %))
                      (= :repair-closure-observation-unavailable (:reason %)))
                (:dropped-candidates recorded))
          "with the unchanged reason")
      (is (some #(and (= sourceless (:target %))
                      (= :universe-not-admitted (:kind %)))
                (:refusals recorded))))))

(deftest published-interpretation-with-checkable-closure-is-not-withheld
  (let [sources (cs/with-context-fn (cs/load-declared))
        supply (cprop/load-supply {:repair-root repair-root})
        id declared-id
        target (str "T-" id)
        token :ticket-closure/hfixture
        problem {:target target
                 :cascade-problem {:facts {token false} :want [token]
                                   :locators {token {:class :C4}}
                                   :interpretations {:p {:produces #{token}}}}
                 :constructed-candidates [{:id :C1 :precedence [:p]}]}
        recorded (cprop/record-supply
                  {:problems [problem] :refusals [] :dropped-candidates []}
                  sources supply)]
    (is (some #(= target (:target %)) (:problems recorded)))
    (is (not-any? #(= :repair-closure-observation-unavailable (:reason %))
                  (:dropped-candidates recorded)))
    (is (= :checkable-closure-and-constructed-candidate
           (get-in recorded [:repair-withhold-distinction
                             :supplied-by-observable-construction 0 :rule])))))

(deftest generated-repair-proposal-is-still-withheld
  ;; A :repair-finding-proposed proposal whose target has NO declared source
  ;; keeps the withhold; the generated proposal itself stays out of
  ;; admissions (no admitted receipt).
  (let [sources (cs/with-context-fn (cs/load-declared))
        supply (assoc (cprop/load-supply {:repair-root repair-root})
                      :proposals [{:proposal-id "gen-1"
                                   :target "T-no-source-generated"
                                   :origin :repair-finding-proposed
                                   :pattern "some-pattern"}])
        assembled {:problems [{:target "T-no-source-generated"
                               :cascade-problem {:facts {} :want [:x]
                                                 :interpretations {}
                                                 :constructed-candidates []}}]
                   :refusals [] :dropped-candidates []}
        recorded (cprop/record-supply assembled sources supply)]
    (is (not-any? #(= "T-no-source-generated" (:target %)) (:problems recorded)))
    (is (some #(and (= "T-no-source-generated" (:target %))
                    (= :repair-closure-observation-unavailable (:reason %)))
              (:dropped-candidates recorded))
        "generated repair proposals with no declared source keep the withhold")
    (is (some #(= "T-no-source-generated" (:target %))
              (get-in recorded [:repair-withhold-distinction :withheld-generated-or-sourceless])))))

(deftest pinned-findings-classify-as-actionable-open
  (let [ids (set (get-in (repair/lifecycle-census repair-root)
                         [:actionable-open :ids]))]
    (is (= #{declared-id sourceless-id} ids))))
