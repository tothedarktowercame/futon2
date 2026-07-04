(ns futon2.aif.evidence-emit-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [futon2.aif.evidence-emit :as evidence-emit]))

(def ^:private sample-tick
  {:timestamp "2026-07-04T12:34:56Z"
   :mode :multiplied
   ;; rank-1 action carries the policy-grain EFE decomposition + the :risk-mode regime
   :ranked-actions [{:action {:type :address-sorry :target :sorry/x
                              :rationale "clear the blocking sorry"}
                     :G-total 0.25 :risk-mode :kl
                     :G-core 0.4 :G-risk 1.2 :G-ambiguity -1.0 :G-info 0.3
                     :G-goal-outcome 0.5 :G-survival 0.1 :G-augmentation -0.2 :G-gap 0.0}]
   :decision {:action {:type :address-sorry :target :sorry/x
                       :rationale "clear the blocking sorry"}
              :G-total 0.25}
   ;; cascade-lane verdicts: per candidate mission, the cascade's pass/fail + ΔG + source
   :act-gate-verdicts [{:mission "M-x" :verdict :pass :delta-G -0.2 :delta-G-source :fold}
                       {:mission "M-y" :verdict :fail :delta-G -0.25 :delta-G-source :rollout}
                       {:mission "M-z" :verdict :fail}]
   :belly 3
   :enactment {:mission "M-x"}
   :realized-outcome {:realized-G 0.1
                      :expected-G 0.25}
   :wm-version {:trigger :duree-click-regulated}})

(deftest evidence-entry-shape-test
  (testing "WM ticks become compact coordination step evidence"
    (let [entry (evidence-emit/evidence-entry sample-tick)
          body (:body entry)]
      (is (= "coordination" (:type entry)))
      (is (= "step" (:claim-type entry)))
      (is (= "war-machine" (:author entry)))
      (is (= {:ref/type "agent" :ref/id "war-machine"} (:subject entry)))
      (is (= ["wm-tick" "wm-click"] (:tags entry)))
      (testing "action-grain fields (back-compat)"
        (is (= :multiplied (:mode body)))
        (is (= :address-sorry (:decision body)))
        (is (= :sorry/x (:target body)))
        (is (= 0.25 (:G body)))
        (is (= {:pass 1 :fail 2} (:gates body)))
        (is (= "M-x" (:enacted body)))
        (is (= 0.1 (:realized-G body)))
        (is (= 0.25 (:expected-G body)))
        (is (= :duree-click-regulated (:trigger body)))
        (is (= 1 (:candidates body)))
        (is (= "2026-07-04T12:34:56Z" (:at body))))
      (testing "policy/cascade grain (claude-16 emitter-upgrade)"
        (is (= :kl (:risk-mode body)))
        (is (= {:core 0.4 :risk 1.2 :ambiguity -1.0 :info 0.3
                :goal-outcome 0.5 :survival 0.1 :augmentation -0.2 :gap 0.0}
               (:G-breakdown body)))
        (is (= [{:mission "M-x" :verdict :pass :delta-G -0.2 :source :fold}
                {:mission "M-y" :verdict :fail :delta-G -0.25 :source :rollout}
                {:mission "M-z" :verdict :fail :delta-G nil :source nil}]
               (:cascade-lane body))))
      (testing "readable :text carries decision, risk-mode regime, cascade verdicts, outcome"
        (is (string? (:text body)))
        (is (str/includes? (:text body) "address-sorry"))
        (is (str/includes? (:text body) "risk-mode kl"))
        (is (str/includes? (:text body) "M-x ✓"))
        (is (str/includes? (:text body) "Realized G 0.1 vs expected 0.25"))))))

(deftest evidence-entry-cron-tag-test
  (testing "wallclock ticks carry the cron basis tag"
    (is (= ["wm-tick" "wm-cron"]
           (:tags (evidence-emit/evidence-entry
                   (assoc-in sample-tick [:wm-version :trigger] :wallclock-cron))))))
  (testing "unspecified triggers do not invent a basis tag"
    (is (= ["wm-tick"]
           (:tags (evidence-emit/evidence-entry
                   (assoc-in sample-tick [:wm-version :trigger] :unspecified)))))))

(deftest emit-noop-when-flag-off-test
  (testing "flag off means no POST is attempted"
    (with-redefs [evidence-emit/enabled? (constantly false)
                  evidence-emit/post-evidence! (fn [_]
                                                 (throw (ex-info "should not post" {})))]
      (is (nil? (evidence-emit/emit! sample-tick))))))

(deftest emit-no-throw-on-post-failure-test
  (testing "POST failures are best-effort and do not escape the tick"
    (with-redefs [evidence-emit/enabled? (constantly true)
                  evidence-emit/post-evidence! (fn [_]
                                                 (throw (ex-info "bus down" {})))]
      (is (nil? (evidence-emit/emit! sample-tick))))))
