(ns futon2.aif.apparatus-certificates-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-observation-scoring :as scoring]
            [futon2.aif.cascade-policy :as cascade-policy]
            [futon2.aif.focus-receipt :as focus]
            [futon2.aif.run-participants :as participants]
            [futon2.aif.wm.apparatus-certificates :as apparatus]))

(defn participant-record [author reviewer]
  (let [state (atom nil)
        opts {:author author :reviewer reviewer :repair-reviewer "repair"
              :participants/state state}]
    (participants/observe! opts)
    (participants/record-value opts)))

(def terminal-preference
  {:focused 55/100 :related 35/100 :unrelated 5/100 :stop-the-line 5/100})

(defn class-scoring-decision []
  (let [target "M-test"
        start [target :start] done [target :done]
        step (assoc (cascade-policy/token-interpretation
                     :finish {:guard {:needs #{start} :forbids #{done}}
                              :produces #{done}}) :target target)
        candidate {:kind :cascade-candidate :id :C1 :target target :precedence [step]}
        model {:schema :wm/observation-model-v1 :backend :exact-enumeration
               :kind :class-emission :universe #{start done} :horizon 2
               :class-universe [:focused :related :unrelated :stop-the-line
                                :ending/not-yet-evaluated]
               :acceptance #{done} :target-class {target :focused}
               :class-preference {1 {:ending/not-yet-evaluated 1}
                                  2 terminal-preference}
               :provenance {:status :synthetic :calibrated false :source "r20-test"}}
        ranked (scoring/rank-cascade-actions
                {:cascade-belief {#{start} 1}} [candidate]
                {:observation-model model :horizon-steps 2
                 :prediction-context {:occurrence-id "r20-test" :tau 2}
                 :cascade-spec {:want #{done} :evidence #{} :zeroed #{}}})
        entry (first ranked)
        certificate (assoc (select-keys (:certificate entry)
                                        [:observation-model :steps :g-terms])
                           :id (:action entry))]
    {:action (:action entry)
     :selection-certificate {:scoring {0 certificate}}}))

(defn focus-decision []
  (let [inputs (focus/read-inputs)
        bare {:selection-certificate
              {:candidates [{:id {:target "M-wm-08-external-f2" :id :C1}}]}}
        context {:as-of "2026-09-21T18:00:00Z"
                 :previous-focus {:focus "WM" :as-of "2026-09-20T00:00:00Z"}}]
    (assoc-in bare [:selection-certificate :focus-receipt]
              (focus/build bare inputs context))))

(defn row [receipt node]
  (first (filter #(= node (:node %)) (:rows receipt))))

(deftest independence-uses-the-real-participant-producer
  (let [good (apparatus/receipt {:decision {} :participants (participant-record "author" "reviewer")})
        bad (apparatus/receipt {:decision {} :participants (participant-record "same" "same")})
        missing (apparatus/receipt {:decision {} :participants (participant-record nil "reviewer")})]
    (is (= [:agrees :author-is-not-reviewer]
           ((juxt :verdict :case) (row good :independence))))
    (is (= [:disagrees :same-agent]
           ((juxt :verdict :case) (row bad :independence))))
    (is (= :not-recorded (:verdict (row missing :independence))))))

(deftest class-preference-checks-real-scorer-output
  (let [decision (class-scoring-decision)
        good (apparatus/receipt {:decision decision :participants {}})
        bad-decision (update-in decision [:selection-certificate :scoring 0 :steps 1 :risk] + 0.01)
        bad (apparatus/receipt {:decision bad-decision :participants {}})
        missing (apparatus/receipt {:decision {} :participants {}})]
    (is (= [:agrees :at-horizon-supported]
           ((juxt :verdict :case) (row good :class-preference))))
    (is (= :disagrees (:verdict (row bad :class-preference))))
    (is (= :not-recorded (:verdict (row missing :class-preference))))))

(deftest class-preference-refuses-a-changed-preference-and-an-unsupported-prediction
  (let [decision (class-scoring-decision)
        other-preference (assoc-in decision
                                   [:selection-certificate :scoring 0 :observation-model
                                    :class-preference 2]
                                   {:focused 1})
        ;; before the horizon all preference mass is on not-yet-evaluated, so a
        ;; prediction of :focused there has infinite risk; the record still
        ;; reports the scorer's finite number
        unsupported (assoc-in decision
                              [:selection-certificate :scoring 0 :steps 0 :prediction]
                              {:focused 1})]
    (is (number? (get-in decision [:selection-certificate :scoring 0 :steps 0 :risk])))
    (is (= :disagrees
           (:verdict (row (apparatus/receipt {:decision other-preference :participants {}})
                          :class-preference))))
    (is (= :disagrees
           (:verdict (row (apparatus/receipt {:decision unsupported :participants {}})
                          :class-preference))))))

(def focus-target "M-wm-08-external-f2")

;; CTauClassPreference.scorerClass, written out here so the fixture does not
;; borrow the table under test
(def lean-scorer-class
  {:focus :focused :associated :related :useful-elsewhere :unrelated})

(defn focus-decision-with-scoring
  "The real focus receipt, plus a real class score whose model holds
   CONSUMED-CLASS for the focus candidate's target."
  [consumed-class]
  (let [focus (focus-decision)
        scoring (assoc-in (class-scoring-decision)
                          [:selection-certificate :scoring 0 :observation-model :target-class]
                          {focus-target consumed-class})]
    (update focus :selection-certificate merge (:selection-certificate scoring))))

(deftest strategic-focus-checks-real-focus-receipt
  (let [recorded (get-in (focus-decision)
                         [:selection-certificate :focus-receipt :candidates 0 :class])
        expected (get lean-scorer-class recorded :unknown)
        decision (focus-decision-with-scoring expected)
        good (apparatus/receipt {:decision decision :participants {}})
        bad-decision (assoc-in decision
                               [:selection-certificate :focus-receipt :candidates 0 :class]
                               :associated)
        bad (apparatus/receipt {:decision bad-decision :participants {}})
        other (first (remove #{expected} [:focused :related :unrelated]))
        scorer-consumed-other (apparatus/receipt
                               {:decision (focus-decision-with-scoring other)
                                :participants {}})
        unscored (apparatus/receipt {:decision (focus-decision) :participants {}})
        missing (apparatus/receipt {:decision {} :participants {}})]
    (is (not= :associated recorded))
    (is (= [:agrees :focus-kept]
           ((juxt :verdict :case) (row good :strategic-focus))))
    (is (= :disagrees (:verdict (row bad :strategic-focus))))
    (testing "the scorer consumed a different class from the one the relation gives"
      (is (= :disagrees (:verdict (row scorer-consumed-other :strategic-focus)))))
    (testing "a focus receipt with no class score has no scored class to check"
      (is (= :not-recorded (:verdict (row unscored :strategic-focus))))
      (is (some #{:scored-class} (:missing (row unscored :strategic-focus)))))
    (is (= :not-recorded (:verdict (row missing :strategic-focus))))))

(deftest certificate-is-total-and-fail-closed
  (let [r (apparatus/receipt {:decision {} :participants {}})]
    (is (= apparatus/node-order (mapv :node (:rows r))))
    (is (= (repeat 8 :not-recorded) (map :verdict (:rows r))))
    (is (false? (:certified? r))))
  (testing "one missing row prevents certification even when implemented rows agree"
    (let [r (apparatus/receipt {:decision (merge (class-scoring-decision)
                                                 (focus-decision-with-scoring :focused))
                                :participants (participant-record "author" "reviewer")})]
      (is (some #(= :agrees (:verdict %)) (:rows r)))
      (is (false? (:certified? r))))))
