(ns futon2.aif.evidence-emit-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [futon2.aif.evidence-emit :as evidence-emit]
            [futon2.aif.policy :as policy]))

(def ^:private cascade-candidate
  {:kind :cascade-candidate :cascade-id "M-x" :id "M-x"
   :precedence [:aif/placeholder-is-load-bearing]
   :construction-receipt {:cascade/id "M-x" :moves 1
                          :family-searched :unit :coverage 1}
   :interpretation-receipts [{:pattern :aif/placeholder-is-load-bearing
                              :admitted-by :test-suite}]})

(def ^:private sample-tick
  "A tick whose decision is a REAL cascade decision (SPEC flat-removal H4,
  2026-09-17); no :ranked-actions field can exist."
  {:timestamp "2026-09-17T12:34:56Z"
   :mode :multiplied
   :decision (policy/select-action-cascades
              [{:action cascade-candidate :controller-score 0.25 :rank 1}]
              {:beta 2.0})
   :cascade-problems {:problems [] :refusals []}
   :act-gate-verdicts [{:mission "M-x" :verdict :pass :coverage-score-delta -0.2 :coverage-score-source :fold}
                       {:mission "M-y" :verdict :fail :coverage-score-delta -0.25 :coverage-score-source :rollout}
                       {:mission "M-z" :verdict :fail}]
   :belly 3
   :enactment {:mission "M-x"}
   :realized-outcome {:realized-score 0.1
                      :expected-score 0.25}
   :wm-version {:trigger :duree-click-regulated}})

(def ^:private abstaining-tick
  (-> sample-tick
      (assoc :decision
             {:status :abstained
              :refusals [{:target "M-a" :kind :beta-not-declared :missing :beta-by-context}
                         {:target "M-b" :kind :universe-not-admitted :missing :universes}]})
      (assoc :cascade-problems
             {:problems []
              :refusals [{:target "M-c" :kind :want-not-declared :missing :wants}]})))

(deftest emit-base-keeps-its-override-and-shares-read-fallback-order-test
  (is (= "https://emit.example"
         (evidence-emit/evidence-base
          {"FUTON2_WM_EMIT_BASE" "https://emit.example"
           "FUTON3C_EVIDENCE_BASE" "https://read.example"
           "FUTON3C_SERVER" "https://server.example"
           "FUTON3C_PORT" "7099"})))
  (is (= "https://read.example"
         (evidence-emit/evidence-base
          {"FUTON3C_EVIDENCE_BASE" "https://read.example"
           "FUTON3C_SERVER" "https://server.example"
           "FUTON3C_PORT" "7099"})))
  (is (= "https://server.example"
         (evidence-emit/evidence-base
          {"FUTON3C_SERVER" "https://server.example"
           "FUTON3C_PORT" "7099"})))
  (is (= "http://127.0.0.1:7099"
         (evidence-emit/evidence-base {"FUTON3C_PORT" "7099"}))))

(deftest evidence-entry-shape-test
  (testing "WM ticks become compact coordination step evidence"
    (let [entry (evidence-emit/evidence-entry sample-tick)
          body (:body entry)]
      (is (= "coordination" (:type entry)))
      (is (= "step" (:claim-type entry)))
      (is (= "war-machine" (:author entry)))
      (is (= {:ref/type "agent" :ref/id "war-machine"} (:subject entry)))
      (is (= ["wm-tick" "wm-click"] (:tags entry)))
      (testing "cascade-decision grain (SPEC flat-removal H4): no back-compat flat fields"
        (is (= :multiplied (:mode body)))
        (is (= "M-x" (:decision body)))
        (is (= :cascade-candidate (:kind body)))
        (is (= ":aif/placeholder-is-load-bearing" (:enacted-step body)))
        (is (= 1.0 (:posterior-mass body)))
        (is (= 2.0 (:beta body)))
        (is (= :declared (:beta-status body)))
        (is (= 0.25 (:G body)))
        (is (= {:pass 1 :fail 2} (:gates body)))
        (is (= "M-x" (:enacted body)))
        (is (= 0.1 (:realized-score body)))
        (is (= 0.25 (:expected-score body)))
        (is (= :duree-click-regulated (:trigger body)))
        (is (= 1 (:candidates body)))
        (is (= "2026-09-17T12:34:56Z" (:at body)))
        (is (not (contains? body :risk-mode)) "flat rank-1 readback removed")
        (is (not (contains? body :G-breakdown)) "flat EFE decomposition removed")
        (is (not (contains? body :target)) "flat action target removed"))
      (testing "cascade lane verdicts survive"
        (is (= [{:mission "M-x" :verdict :pass :coverage-score-delta -0.2 :source :fold}
                {:mission "M-y" :verdict :fail :coverage-score-delta -0.25 :source :rollout}
                {:mission "M-z" :verdict :fail :coverage-score-delta nil :source nil}]
               (:cascade-lane body))))
      (testing "readable :text carries the cascade, its enacted step, verdicts, outcome"
        (is (string? (:text body)))
        (is (str/includes? (:text body) "M-x"))
        (is (str/includes? (:text body) "enacts :aif/placeholder-is-load-bearing"))
        (is (str/includes? (:text body) "M-x ✓"))
        (is (str/includes? (:text body) "Realized G 0.1 vs expected 0.25"))))))

(deftest abstaining-tick-emits-readiness-test
  (let [body (:body (evidence-emit/evidence-entry abstaining-tick))]
    (is (= :abstained (:decision body)))
    (is (= {:beta-not-declared 1 :universe-not-admitted 1}
           (:refusals-by-kind body)))
    (is (= {:want-not-declared 1}
           (:cascade-problems-refusals-by-kind body)))
    (is (str/includes? (:text body) "readiness, not an error"))
    (is (str/includes? (:text body) "beta-not-declared"))))

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
