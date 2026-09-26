(ns futon2.aif.flight-step-observation-test
  "F1a-1 (PROOF-2a-PLAN <2>2d F1, M-wm-wiring): the observation that followed
  an enacted step is written on the flight's :enactments entry, on the token
  carrier O, through observation-admission/admit. First-layer wire test: a
  hermetic flight through run! with one enacted click."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- fly [after-obs check-result]
  (let [dir (str (.toFile (Files/createTempDirectory "f1a1" (make-array FileAttribute 0))))
        enact (fr/enact-fn {:dispatch-step! (fn [_] {:commit "c1" :produced :t :check {:class :fixture}})
                            :check-fn (constantly check-result)
                            :interpretations (constantly {:p/a {:produces #{:t}}})
                            :record-dir dir})
        f (flight/run! (flight/start {:target "M-f1a1" :chosen-because {:kind :requested}}
                                     {:kind :operator-declared :wants [:t :u] :declared-by "test"}
                                     {:id "flight-f1a1"})
                       {:click-fn (constantly {:click-id "run-1" :chosen {:candidate :cand/a :precedence [:p/a]}})
                        :enact-fn enact
                        :observe-fn (fn [_ _] after-obs)
                        :sources-fn (constantly {})
                        :max-clicks 1})]
    (:observation (first (:enactments f)))))

(deftest the-observation-is-on-the-enactment-entry
  (let [o (fly {:t true} {:observed true})]
    (testing "present, on the entry, not a typed absence"
      (is (= :wm/admitted-observation-v1 (:schema o)))
      (is (contains? (get-in o [:candidate-labels :present]) :t) "the produced token, observed after the step")
      (is (contains? (get-in o [:candidate-labels :unobserved]) :u) "a universe token the step did not touch")
      (is (= "run-1" (:click-id o))))
    (testing "the finding: admit needs a review the flight cannot supply, and none is fabricated"
      (is (= :not-admitted (:status o)))
      (is (= :review-required (:reason o)))
      (is (= :invalid-verdict (get-in o [:admission :t :kind])) "admit's own refusal, recorded")
      (is (not (contains? o :present)) "no admitted label stands in"))))

(deftest nothing-observed-is-unobserved-never-absent
  ;; the bad case: an after-observation with no tokens, and a check that refused
  (let [o (fly {} {:status :refused :reason :no-mechanical-check})]
    (is (= #{} (set (get-in o [:candidate-labels :present]))))
    (is (= #{} (set (get-in o [:candidate-labels :absent-observed]))) "no token is absent")
    (is (= #{:t :u} (set (get-in o [:candidate-labels :unobserved]))) "every universe token unobserved")
    (is (= :not-admitted (:status o)))
    (is (= :nothing-observed (:reason o)))))

(deftest a-refused-check-is-unobserved
  (let [o (flight/step-observation [:a :b] {:a :unknown :b false} [] "c")]
    (is (= #{:a} (set (get-in o [:candidate-labels :unobserved]))) "a non-boolean reading is not an observation")
    (is (= #{:b} (set (get-in o [:candidate-labels :absent-observed]))) "an observed false is a candidate :absent, still unadmitted")
    (is (= :review-required (:reason o)))))
