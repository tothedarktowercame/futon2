(ns futon2.aif.experiment-registration-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.experiment-registration :as reg]))

(def fixture-registration
  {:clean/experiment :slice5-confirmation
   :clean/title "Do any AIF terms carry a measurable explore/exploit effect?"
   :clean/status :registered-not-run
   :clean/schema-version 1
   :clean/experiment-design
   {:arms [{:id :aif-full :ablates []}
           {:id :no-risk :ablates [:kl-risk]}]
    :scenarios [:patchy :sparse]
    :axes [{:id :kl-risk :score-varies? true :justification "action-dependent"}]
    :seeds {:runs-per-cell 2 :food-fn "b + 100000*s + 2*i"}
    :stop-rules [{:id :positive-control-violated :action :abandon-run}]
    :decision {:control-violated :abandoned}}})

(deftest fixture-registration-validates
  (is (:ok (reg/validate-registration fixture-registration))))

(deftest registration-without-schema-version-refused
  (let [result (reg/validate-registration (dissoc fixture-registration :clean/schema-version))]
    (is (false? (:ok result)))
    (is (some (fn [e] (re-find #"schema-version" e)) (:errors result)))))

(deftest unknown-schema-version-refused
  (is (false? (:ok (reg/validate-registration
                    (assoc fixture-registration :clean/schema-version 99))))))

(deftest in-flight-registration-validates-as-of-its-own-version
  (testing "a v2 tightening (:clean/amendments required) cannot invalidate a
            live v1 registration: it validates as-of the version it carries"
    (let [result (reg/validate-registration fixture-registration {:as-of 1})]
      (is (:ok result))
      (is (= 1 (:validated-against-version result))))))

(deftest derived-schema-refuses-unregistered-arm-before-run
  (testing "the exact bad case the gate is named for, against the real schema"
    (let [ok-config {:arm :aif-full :scenario :patchy
                     :seeds {:food-seed 202609110 :move-seed 202609111}}
          refused-config (assoc ok-config :arm :no-canonical-ambiguity)]
      (is (:ok (reg/validate-run-config fixture-registration ok-config)))
      (let [refusal (reg/validate-run-config fixture-registration refused-config)]
        (is (false? (:ok refusal)))
        (is (= :before-run (:stage refusal)))
        (is (= :unregistered-run-config (:refusal refusal)))
        (is (some (fn [e] (re-find #":no-canonical-ambiguity" e)) (:errors refusal)))))))

(deftest derived-schema-refuses-unregistered-scenario
  (is (false? (:ok (reg/validate-run-config
                    fixture-registration
                    {:arm :aif-full :scenario :desert :seeds {:food-seed 1}})))))

(deftest version-change-recorded-without-mutating-in-flight-registration
  (let [original fixture-registration
        {:keys [event successor]} (reg/amendment original 2
                                                  {:note "amendments slot added"})]
    (testing "the original registration is untouched by the amendment"
      (is (= fixture-registration original))
      (is (nil? (:clean/amendments original)))
      (is (= 1 (:clean/schema-version original))))
    (testing "the change is recorded as an amendment event"
      (is (= :experiment-registration/amendment-v1 (:schema event)))
      (is (= 1 (:from-schema-version event)))
      (is (= 2 (:to-schema-version event)))
      (is (= :slice5-confirmation (:amends event))))
    (testing "the successor is a derived registration, not an edit"
      (is (= 2 (:clean/schema-version successor)))
      (is (= 1 (count (:clean/amendments successor))))
      (is (= :slice5-confirmation
             (get (meta successor) :derived-from))))
    (testing "the in-flight original still validates under v1 after the bump"
      (is (:ok (reg/validate-registration original {:as-of 1}))))))

(deftest load-registration-refuses-a-file-without-schema-version
  (testing "against a real file on disk, not a stub: the shared fixture"
    (let [tmp (java.io.File/createTempFile "reg" ".clean.edn")]
      (try
        (spit tmp (pr-str (dissoc fixture-registration :clean/schema-version)))
        (let [result (reg/load-registration (str tmp))]
          (is (false? (:ok result)))
          (is (:registration result)))
        (finally (.delete tmp))))))
