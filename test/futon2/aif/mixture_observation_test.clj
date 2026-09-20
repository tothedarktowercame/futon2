(ns futon2.aif.mixture-observation-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-model-manifest :as m]))

(defn- rates [tokens fn-rate fp-rate]
  (zipmap tokens (repeat {:false-neg fn-rate :false-pos fp-rate})))

(defn- marginal [row tokens]
  (reduce-kv (fn [acc obs mass]
               (update acc (set/intersection obs tokens) (fnil + 0) mass)) {} row))

(deftest exact-mixture-colsum-and-pointwise-law
  (let [components [{:weight 2/3 :rates (rates [:a :b] 1/4 1/5)}
                    {:weight 1/3 :rates (rates [:a :b] 2/3 3/4)}]]
    (doseq [s [#{} #{:a} #{:b} #{:a :b}]]
      (let [row (m/mixture-observation-distribution components s)]
        (is (= 1 (reduce + (vals row))))
        (is (every? #(and (rational? %) (pos? %)) (vals row)))
        (doseq [o [#{} #{:a} #{:b} #{:a :b}]]
          (is (= (reduce + (for [{:keys [weight rates]} components]
                            (* weight (m/token-likelihood rates s o))))
                 (get row o 0))))))
    (is (= :mixture-weights-not-normalized
           (:kind (m/mixture-observation-distribution
                   (assoc-in components [0 :weight] 1/3) #{}))))))

(deftest invalid-components-refuse
  (let [c {:weight 1 :rates (rates [:a] 0 0)}]
    (doseq [w [-1 2 0.5 nil]]
      (is (= :invalid-mixture-weight
             (:kind (m/mixture-observation-distribution [(assoc c :weight w)] #{})))))
    (is (= :invalid-mixture-components (:kind (m/mixture-observation-distribution [] #{}))))
    (is (= :mixture-universe-mismatch
           (:kind (m/mixture-observation-distribution
                   [c {:weight 0 :rates (rates [:b] 0 0)}] #{}))))
    (is (= :invalid-adjudication-rate
           (:kind (m/mixture-observation-distribution
                   [c {:weight 0 :rates (rates [:a] 2 0)}] #{}))))
    (is (= :invalid-adjudication-rate
           (:kind (m/mixture-observation-distribution [c] #{:outside}))))
    (is (= {#{} 1} (m/mixture-observation-distribution [{:weight 1 :rates {}}] #{})))))

(deftest checkable-marginal-under-maximal-judgement-coupling
  ;; One latent component reports every judgement present; the other reports
  ;; none. Both checkable tokens (one true, one false) remain deterministic.
  (let [checks (rates [:git-present :git-absent] 0 0)
        components [{:weight 1/2 :rates (merge checks (rates [:j1 :j2] 0 1))}
                    {:weight 1/2 :rates (merge checks (rates [:j1 :j2] 1 0))}]
        state #{:git-present :j1}
        row (m/mixture-observation-distribution components state)
        checkable #{:git-present :git-absent}]
    (is (= {#{:git-present :j1 :j2} 1/2 #{:git-present} 1/2} row))
    (is (= {#{:git-present} 1} (marginal row checkable)))
    (is (= {#{:j1 :j2} 1/2 #{} 1/2} (marginal row #{:j1 :j2})))
    (testing "negative control: a nonzero checkable error actually breaks exactness"
      (doseq [[token field] [[:git-present :false-neg] [:git-absent :false-pos]]]
        (let [bad (m/mixture-observation-distribution
                   (assoc-in components [1 :rates token field] 1) state)]
          (is (not= {#{:git-present} 1} (marginal bad checkable)))
          (is (= 1/2 (get (marginal bad checkable) #{:git-present} 0))))))))

(deftest sidebyside-output-is-byte-identical
  (let [dir "holes/labs/wm-contract/runs/coupling-sidebyside-2026-09-18/"
        output (with-out-str
                 (binding [*ns* (create-ns 'mixture-sidebyside-oracle)]
                   (refer 'clojure.core)
                   (load-file (str dir "sidebyside.clj"))))]
    (is (= (slurp (str dir "output.txt")) output))))
