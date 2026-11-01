(ns futon2.aif.inner-abstention-test
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.inner-abstention :as stop]
            [futon2.aif.policy :as policy]
            [futon2.aif.decision-gate :as gate]
            [futon2.aif.full-loop-runner :as runner]
            [wm-run-facts :as facts]))
(defn read-edn [p] (edn/read-string {:default tagged-literal} (slurp p)))
(def fixture (read-edn "test/fixtures/wm-run-facts/376d76ff-candidates.edn"))
(defn fake-stop [g]
  {:G g :terms {:risk g :ambiguity 0.0 :information-gain 0.0 :fit-ambiguity 0.0}
   :prediction {:progress [{[0 false] 1}] :class [{:stop-the-line 1}]}
   :prediction-digest (identity/digest [[{[0 false] 1}] [{:stop-the-line 1}]])
   :horizon 1 :no-action-rule-version 1})
(deftest no-class-beats-stop
  (let [rows (mapv #(assoc % :abstention (fake-stop 0.1)) (:ranked fixture))
        d (policy/select-action-cascades rows {:beta (:beta fixture)})
        a (get-in d [:selection-certificate :abstention])]
    (is (= :abstained (:status d)))
    (is (= :abstain-on-task (get-in d [:abstention :case])))
    (is (.endsWith (:chosen-reference a) "policy-34c85cf68e52022bd8ae"))
    (is (true? (:cross-lens a)))
    (is (= d (gate/emit! d)))
    (is (= :abstain-on-task (:case (runner/abstention-carrier d []))))
    (let [wrong (assoc-in d [:selection-certificate :abstention :chosen-reference]
                          (get-in rows [0 :action :id]))]
      (is (thrown? clojure.lang.ExceptionInfo (gate/emit! wrong))))))
(deftest minimum-cannot-be-replaced-by-convenient-stop-reference
  (let [rows (mapv #(assoc % :abstention
                          (fake-stop (if (< (:controller-score %) 1.5) 0.1 10.0)))
                   (:ranked fixture))]
    (is (= :pick-not-below-abstention
           (try (policy/select-action-cascades rows {:beta (:beta fixture)}) nil
                (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))
(deftest carrier-validation-and-historical-projection
  (let [d (policy/select-action-cascades
            (mapv #(assoc % :abstention (fake-stop 10.0)) (:ranked fixture))
            {:beta (:beta fixture)})
        r {:decision d}
        a (get-in d [:selection-certificate :abstention])
        ref (:chosen-reference a)]
    (is (= 10000000 (get-in (facts/inner-abstention-facts r) ["abstentionG" "numerator"])))
    (is (string? (get (facts/inner-abstention-facts r) "abstentionPrediction")))
    (doseq [[path v] [[[:chosen-reference] "missing"]
                     [[:per-policy ref :G] 11.0]
                     [[:per-policy ref :prediction-digest] "wrong"]]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (stop/validate! (assoc-in a path v)
                                   (get-in d [:selection-certificate :candidates])))))
    (is (map? (get (facts/inner-abstention-facts {}) "abstentionG")))))

(def live-pins [{:file "tick-run-record-2026-10-29-376d76ff-75dd-4a2b-a42b-058883554cb6.edn", :sha256 "e4dfcd74b5ade524f9d0edc53ed31dc38f549ef11f03a3365414530485461afa", :chosen "M-reachable-from-boot/policy-7d91338b4da14feb2f06", :stop-G 4.531889161983763} {:file "tick-run-record-2026-10-30-3415b671-88f6-4662-a2c1-48e3630f6116.edn", :sha256 "0af126fec89e95980508f64ed381e5694d172d6dc37f7d7d8b6f384ce363aa36", :chosen "M-peradam-grounding/policy-d5c397705c67abc90ee0", :stop-G 2.729834749881413} {:file "tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn", :sha256 "7dbf1614668d9f8c565562447c5d336b9424e6bb93d42335fde1ba1c32e7a9ac", :chosen "E-comprehension-foundation/policy-78fb2aa6ddcc719988e1", :stop-G 2.8636216006595965}])
(deftest three-retained-models-match-prototype
  (doseq [{:keys [file sha256 chosen stop-G]} live-pins]
    (testing file
      (let [text (slurp (io/file "/home/joe/code/futon2/data/wm-runs" file))
            _ (is (= sha256 (identity/sha256 text)))
            r (edn/read-string {:default tagged-literal} text)
            sc (get-in r [:decision :selection-certificate])
            c (first (filter #(= chosen (get-in % [:id :id])) (:candidates sc)))
            s (first (filter #(= chosen (get-in % [:id :id])) (vals (:scoring sc))))
            result (stop/score c s)]
        (is (< (abs (- stop-G (:G result))) 1.0e-9))
        (is (every? #(= {[0 false] 1} %) (get-in result [:prediction :progress])))
        (is (= :empty-want-stop
               (try (stop/score c (assoc-in s [:observation-model :want] #{})) nil
                    (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))))
