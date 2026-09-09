(ns futon2.aif.preference-module-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.preference-module :as c]
            [futon2.aif.efe :as efe]
            [futon2.aif.ruled-outcome-c :as ruled]))

(def profile (edn/read-string (slurp "resources/c-modules/current-work-v1.edn")))
(def entry (first (:entries profile)))
(defn reading [v] {:version 1 :criterion "claim-warrant/v1"
                  :evidence "p4ng:f036a91:sec-recapitulation.tex:16"
                  :status :observed :value v})
(defn tagged-seed []
  {:support (mapv #(vector :organization %) (sort (:support ruled/seeded-c)))
   :mass (into {} (map (fn [[k v]] [[:organization k] v]) (:mass ruled/seeded-c)))})

(deftest exact-distribution-contract
  (let [seed (tagged-seed)]
    (is (= seed (c/validate-distribution seed)))
    (is (= 7 (count (filter zero? (vals (:mass seed)))))))
  (doseq [bad [{:support [[:organization :yes]] :mass {[:organization :yes] 0.9999999999999}}
               {:support [[:organization :yes] [:organization :yes]] :mass {[:organization :yes] 1}}
               {:support [[:organization :yes]] :mass {[:organization :no] 1}}]]
    (is (thrown? clojure.lang.ExceptionInfo (c/validate-distribution bad)))))

(deftest seeded-risk-and-refusal
  (let [seed (tagged-seed)
        q (fn [d] {:support [[:organization d]] :mass {[:organization d] 1}})]
    (is (< (Math/abs (- (Math/log 2) (:risk-nats (c/risk seed (q :grounded-change))))) 1e-12))
    (is (= :refused (:status (c/risk seed (q :cancelled)))))
    (is (= :computed (:status (c/risk seed seed))))))

(deftest soft-family-is-not-a-selected-mass
  (is (= :symbolic (get-in (c/assess profile {:claim-warrant (reading false)}) [:entries 0 :status])))
  (is (= :unknown (get-in (c/assess profile {}) [:entries 0 :status])))
  (doseq [p [1/2 1 0.75]]
    (is (thrown? clojure.lang.ExceptionInfo (c/instantiate entry p "test-only"))))
  (let [d (:distribution (c/instantiate entry 3/4 "synthetic-test-only"))]
    (is (= 3/4 (get-in d [:mass [:organization :satisfied]]))))
  (is (thrown? clojure.lang.ExceptionInfo
               (c/assess profile {:claim-warrant (assoc (reading true) :version 2)}))))

(deftest comparison-does-not-manufacture-priority
  (is (= :improves (c/compare-satisfaction profile {:claim-warrant (reading false)}
                                          {:claim-warrant (reading true)})))
  (is (= :unresolved-measurement (c/compare-satisfaction profile {} {:claim-warrant (reading true)})))
  (let [other #(assoc (reading %) :criterion "inspectable-reasons/v1")]
    (is (= :tradeoff-unranked
           (c/compare-satisfaction profile
            {:claim-warrant (reading false) :inspectable-reasons (other true)}
            {:claim-warrant (reading true) :inspectable-reasons (other false)})))))

(deftest wm-consumes-module-without-relabeling-its-score
  (testing "Actual WM scorer; module adds warranted diagnostics, not an invented aggregation"
    (let [state {:observation {:loop-health 0.8}}
          actions [{:id :local :type :no-op :preference-readings {:claim-warrant (reading false)}}]
          baseline (efe/rank-actions state actions {})
          integrated (efe/rank-actions state actions {:preference-module profile})]
      (is (= baseline (mapv #(dissoc % :preference-module-diagnostic) integrated)))
      (is (= :symbolic (get-in integrated [0 :preference-module-diagnostic :entries 0 :status])))))
  (is (thrown? clojure.lang.ExceptionInfo
               (efe/rank-actions {} [] {:preference-module (assoc profile :schema :v2)}))))

(deftest local-ranking-uses-supplied-predictions
  (let [module (assoc profile :entries [(assoc entry :kind :finite :distribution (tagged-seed))])
        candidate (fn [id outcome status]
                    {:id id :readings {:claim-warrant
                     {:version 1 :criterion "claim-warrant/v1" :status status
                      :evidence "synthetic-control-not-policy-calibration"
                      :distribution {:support [[:organization outcome]]
                                     :mass {[:organization outcome] 1}}}}})
        good (candidate :good :grounded-change :predicted)
        poor (candidate :poor :build-failed :predicted)]
    (is (= [:good :poor] (mapv :id (:candidates (c/rank-local-risk module :claim-warrant [poor good])))))
    (is (= :unresolved (:status (c/rank-local-risk module :claim-warrant
                            [good (candidate :zero :cancelled :predicted)]))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (c/rank-local-risk module :claim-warrant
                   [(candidate :actual :grounded-change :observed)])))))

(deftest candidate-preferences-cannot-be-presented-as-accepted
  (is (thrown? clojure.lang.ExceptionInfo (c/validate-module (assoc profile :mode :accepted)))))

(deftest wm-local-risk-adapter-does-not-pretend-to-be-total-efe
  (let [finite (assoc entry :kind :finite :distribution (tagged-seed))
        module (assoc profile :entries [finite])
        result (efe/rank-local-preference-actions
                {:observation {:loop-health 0.8}}
                [{:id :unknown :type :no-op}]
                {:preference-module module} :claim-warrant)]
    (is (= :unresolved (get-in result [:local-preference-ranking :status])))
    (is (= 1 (count (:wm-rankings result))))))
