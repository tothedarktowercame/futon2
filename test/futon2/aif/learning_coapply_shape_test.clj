(ns futon2.aif.learning-coapply-shape-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.attempt-learning :as attempt]
            [futon2.aif.learning-trial :as trial]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.token-outcome :as outcome]))

(def fixture-path "test/resources/m2w/pattern-prediction-90ca.edn")
(def fixture-sha "894ef46746d877abbcc7f3156f494c5006e30e0c9e58274ca362ff3857872d0f")

(defn- entry [] (edn/read-string (slurp fixture-path)))

(defn- prediction [e]
  (let [sealed (selected/seal
                {:entries [e]} {}
                {:status :selected
                 :action {:target (:target-id e)
                          :policy-id (:policy-id e)
                          :cascade-digest (:cascade-digest e)}})]
    (outcome/freeze-prediction
     {:action (:action e)
      :selection-certificate
      {:selection-route :wm/selected-outer-cascade-policy-v1
       :pattern-prediction-input (:pattern-prediction-input sealed)}})))

(defn- occurrence [action]
  {:schema :wm/action-transition-occurrence-v2
   :action/id "synthetic-coapply-shape"
   :action/value action
   :action/value-sha256 "synthetic-action-sha"
   :transition/id "synthetic-transition"})

(deftest committed-coapply-prediction-yields-held-trial-candidates
  (let [e (entry)
        p (prediction e)
        comparison (outcome/compare-outcomes p [] "synthetic-artifact-sha")
        receipt (attempt/receipt
                 {:comparison comparison
                  :occurrence (occurrence (:action p))
                  :route :synthetic-test
                  :source-record nil :expected nil :read-job nil})
        patterns (get-in p [:action :precedence :co-apply :patterns])
        units (get-in p [:action :precedence :co-apply :units])
        intended (vec (mapcat (fn [unit]
                                (for [effect (sort-by pr-str (:produces (patterns unit)))]
                                  [(:id (patterns unit)) effect]))
                              units))
        actual (mapv (juxt :pattern :effect) (:trials receipt))
        wanted (set (map :token (:wanted p)))]
    (is (= fixture-sha (identity/sha256 (.getBytes (slurp fixture-path) "UTF-8"))))
    (is (= :frozen (:status p)))
    (is (= intended actual) "every declared output follows declared unit order")
    (is (= 27 (count actual)))
    (is (every? (set (map second actual)) wanted) "both wanted effects remain candidates")
    (is (every? #(= :held (:status %)) (:trials receipt)))
    (is (every? #(= :observation-missing (:reason %)) (:trials receipt)))
    (is (not-any? true? (map :counted? (:trials receipt))))))

(deftest legacy-vector-enumeration-is-unchanged
  (let [patterns [{:id :p :guard {:status :interpreted :clauses []} :produces #{[:t :b] [:t :a]}}
                  {:id :q :guard {:status :interpreted :clauses []} :produces #{[:t :c]}}]
        prediction {:status :frozen :initial-belief {#{} 1} :horizon 1
                    :action {:kind :cascade-candidate :target :t :precedence patterns}}
        receipt (attempt/receipt
                 {:comparison {:prediction prediction :tokens []}
                  :occurrence nil :route :synthetic-test :source-record nil
                  :expected nil :read-job nil})]
    (is (= [[:p [:t :a]] [:p [:t :b]] [:q [:t :c]]]
           (mapv (juxt :pattern :effect) (:trials receipt))))))

(deftest malformed-coapply-is-typed-not-an-empty-success
  (let [prediction {:status :frozen :initial-belief {#{} 1} :horizon 1
                    :action {:kind :cascade-candidate :target :t
                             :precedence {:co-apply {:units [:missing]
                                                    :descent [] :patterns {}}}}}
        receipt (trial/receipt
                 {:comparison {:prediction prediction :tokens []}
                  :occurrence nil :route :synthetic-test :source-record nil})]
    (is (= :held (:status receipt)))
    (is (= :malformed-co-apply-step (:reason receipt)))
    (is (= :patterns (get-in receipt [:precedence-finding :detail])))
    (is (empty? (:trials receipt)))))

(deftest coapply-shadow-preserves-frontier-semantics
  (let [patterns {:p {:id :p :guard {:status :interpreted :clauses [{:present #{} :absent #{}}]}
                      :produces #{[:t :a]}}
                  :q {:id :q :guard {:status :interpreted :clauses [{:present #{} :absent #{}}]}
                      :produces #{[:t :b]}}}
        step {:co-apply {:units [:p :q] :descent [] :patterns patterns}}
        prediction {:status :frozen :initial-belief {#{} 1} :horizon 1
                    :action {:kind :cascade-candidate :target :t :precedence step}}
        comparison {:prediction prediction
                    :tokens [{:token [:t :a] :observed false :measurement nil}
                             {:token [:t :b] :observed false :measurement nil}]}
        receipt (trial/receipt
                 {:comparison comparison :occurrence nil
                  :route :synthetic-test :source-record nil})
        p-shadow (:shadow (first (:trials receipt)))
        prior-belief (get-in p-shadow [:prior-rollout :rollout :belief])
        counted-belief (get-in p-shadow [:if-counted-rollout :rollout :belief])
        marginal (fn [belief token]
                   (reduce-kv (fn [n state mass]
                                (+ n (if (contains? state token) mass 0))) 0 belief))]
    (testing "the other simultaneously enabled pattern remains in both rollouts"
      (is (pos? (marginal prior-belief [:t :b])))
      (is (= (marginal prior-belief [:t :b]) (marginal counted-belief [:t :b]))))
    (testing "only the selected pattern's theta changes"
      (is (not= (marginal prior-belief [:t :a]) (marginal counted-belief [:t :a]))))))
