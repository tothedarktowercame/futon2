(ns futon2.aif.flight-runner-test
  "Production adapters for a flight, over a stubbed run-opportunity!:
  the flight reaches the runner, the chosen plan's unreached wants and the
  target's own abstention come back, and a refused check is :unknown."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]))

(def target "M-test")

(defn- selected [unreached]
  {:checkpoints
   {:selection
    {:judgment
     {:decision {:selection-law {:applied :cascade-selection-posterior}
                 :action {:kind :cascade-candidate :id :C1 :target target
                          :precedence [{:id :p/one} {:id :p/two}]
                          :construction-receipt {:kind :machine-constructed
                                                 :unreached-wants unreached}}}}}}})

(defn- abstained [refusals]
  {:checkpoints
   {:selection
    {:sorry {:decision {:status :abstained :refusals refusals}
             :dropped-candidates [{:target target :candidate :C1 :reason :no-new-wanted-token
                                   :missing-evidence [:x]}]}}}})

(deftest summary-of-a-selected-click
  (let [s (fr/click-summary target "run-1" (selected [{:token :b :reason :beyond-horizon}]))]
    (is (= "run-1" (:click-id s)))
    (is (= {:candidate :C1 :precedence [:p/one :p/two]} (:chosen s)))
    (is (= [{:token :b :reason :beyond-horizon}] (:unreached-wants s)))
    (is (nil? (:abstention s)))))

(deftest summary-of-an-abstained-click-names-the-targets-decline
  (let [s (fr/click-summary target "run-2"
                            (abstained [{:target "M-other" :kind :want-not-declared :missing :wants}
                                        {:target target :kind :no-constructed-candidate
                                         :missing :construction}]))]
    (is (= :no-constructed-candidate (get-in s [:abstention :kind])))
    (is (= :construction (get-in s [:abstention :missing])))
    (is (= [{:candidate :C1 :reason :no-new-wanted-token :missing-evidence [:x]}]
           (get-in s [:abstention :declines])))
    (is (nil? (:chosen s)))))

(deftest abstention-without-the-target-is-not-silent
  ;; bad case: the tick abstained but recorded no refusal for the flight's
  ;; target; the flight must still see an abstention, not a clean click
  (let [s (fr/click-summary target "run-3"
                            (abstained [{:target "M-other" :kind :want-not-declared :missing :wants}]))]
    (is (= :target-not-in-refusals (get-in s [:abstention :kind])))))

(deftest a-plan-for-another-target-is-not-this-flights
  (let [r (assoc-in (selected [{:token :b}]) [:checkpoints :selection :judgment :decision :action :target]
                    "M-other")
        s (fr/click-summary target "run-4" r)]
    (is (nil? (:chosen s)))
    (is (= [] (:unreached-wants s)))))

(deftest click-fn-puts-the-flight-on-the-run
  (let [calls (atom [])
        cf (fr/click-fn {:base-opts {:mode :test}
                         :run! (fn [opts] (swap! calls conj opts) (selected []))})
        s (cf {:flight {:flight/id "flight-x" :target target :click 3 :wants [:a]}})]
    (is (= "flight-x-click-3" (:click-id s)))
    (is (= {:mode :test :run-id "flight-x-click-3"
            :flight {:flight/id "flight-x" :target target :click 3 :wants [:a]}}
           (first @calls)))))

(deftest observe-fn-reads-refusals-as-unknown
  (let [of (fr/observe-fn (fn [locs]
                            (is (= #{:a :b :c} (set (keys locs))))
                            {:observed #{:a} :results {:a {} :b {}} :refused {:c {:status :missing}}}))]
    (is (= {:a true :b false :c :unknown}
           (of target {:a {:class :C4} :b {:class :C4} :c {:class :C9}})))))

(deftest a-flight-runs-over-the-adapters
  ;; the loop with production adapters over a stubbed runner and checks:
  ;; click 1 abstains (no progress: the flight stops and records the need)
  (let [runs (atom 0)
        cf (fr/click-fn {:base-opts {}
                         :run! (fn [_] (swap! runs inc)
                                 (abstained [{:target target :kind :no-admitted-interpretation
                                              :missing :interpretations}]))})
        of (fr/observe-fn (fn [_] {:observed #{} :results {:a {}} :refused {}}))
        f (flight/run! (flight/start {:target target :chosen-because {:kind :requested}}
                                     {:kind :checkbox} {:id "flight-y"})
                       {:click-fn cf :observe-fn of :max-clicks 5
                        :sources-fn (constantly {:wants {target [:a]}
                                                 :locators {target {:a {:class :C4}}}})})]
    (is (= 1 @runs))
    (is (= :no-progress (:status f)))
    (testing "the missing input is recorded as the flight's need"
      (is (= :interpretations (:missing (first (:needs f))))))))
