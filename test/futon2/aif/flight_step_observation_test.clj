(ns futon2.aif.flight-step-observation-test
  "F1a-1 / F1b-I (PROOF-2a-PLAN <2>2d F1, M-wm-wiring): the observation that
  followed an enacted step is written on the flight's :enactments entry as the
  check channel's RECORDED VERDICTS (TokenObservation: o is what the channel
  reported), with no admission applied to it (F1b-D, futon3c d8b6bf7f).
  First-layer wire test: a hermetic flight through run! with one enacted
  click; bad cases on the verdicts."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- fly [after-obs check-result]
  (let [dir (str (.toFile (Files/createTempDirectory "f1b" (make-array FileAttribute 0))))
        enact (fr/enact-fn {:dispatch-step! (fn [_] {:commit "c1" :produced :t :check {:class :C4}})
                            :check-fn (constantly check-result)
                            :interpretations (constantly {:p/a {:produces #{:t}}})
                            :record-dir dir})
        f (flight/run! (flight/start {:target "M-f1b" :chosen-because {:kind :requested}}
                                     {:kind :operator-declared :wants [:t :u] :declared-by "test"}
                                     {:id "flight-f1b"})
                       {:click-fn (constantly {:click-id "run-1" :chosen {:candidate :cand/a :precedence [:p/a]}})
                        :enact-fn enact
                        :observe-fn (fn [_ _] after-obs)
                        :sources-fn (constantly {})
                        :max-clicks 1})]
    (:observation (first (:enactments f)))))

(deftest the-recorded-observation-is-on-the-enactment-entry
  (let [o (fly {:t true} {:observed true})]
    (is (= :wm/step-observation-v2 (:schema o)))
    (is (= :observed (:status o)))
    (is (contains? (:o o) :t) "the produced token, reported after the step")
    (is (contains? (:checked o) :t))
    (is (contains? (:unobserved o) :u) "a universe token the step did not check")
    (is (= :C4 (get-in o [:channel :t])) "the channel is the check's class")
    (is (= "run-1" (:click-id o)))
    (testing "no admission applied to the observation"
      (is (not (contains? o :admission)))
      (is (not (contains? o :candidate-labels)))
      (is (not= :review-required (:reason o))))))

(deftest a-false-verdict-is-checked-not-reported
  ;; bad case: a check reporting false is a scored observation (reported
  ;; absent), not an unobserved token
  (let [o (fly {:t false} {:observed false})]
    (is (= :observed (:status o)))
    (is (contains? (:checked o) :t))
    (is (not (contains? (:o o) :t)))
    (is (not (contains? (:unobserved o) :t)))))

(deftest nothing-checked-is-unobserved-never-absent
  ;; bad case: no reading, and a check that refused
  (let [o (fly {} {:status :refused :reason :no-mechanical-check})]
    (is (= :nothing-observed (:status o)))
    (is (= #{} (set (:checked o))))
    (is (= #{} (set (:o o))))
    (is (= #{:t :u} (set (:unobserved o))) "every universe token unobserved")))

(deftest a-non-boolean-reading-is-unchecked-not-false
  (let [o (flight/step-observation [:a :b] {:a :unknown :b false} [] {:b {:class :C3}} "c")]
    (is (= #{:b} (set (:checked o))) ":unknown is a refused check")
    (is (= #{} (set (:o o))) "false is reported absent: in checked, not in o")
    (is (= #{:a} (set (:unobserved o))))
    (is (= {:b :C3} (:channel o)) "the after-observation's channel is the locator's class")))
