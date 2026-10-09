(ns futon2.aif.observation-model-q4-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.observation-model :as om]))

(defn- model [prior]
  {:schema :wm/observation-model-v1
   :backend :exact-enumeration
   :kind :class-emission
   :universe #{[:t :done]}
   :class-universe [:progress-0 :progress-1 :focused]
   :progress-classes {0 :progress-0 1 :progress-1}
   :horizon 2
   :acceptance #{[:t :done]}
   :target-class {:t :focused}
   :class-preference {1 {:progress-0 1/3 :progress-1 1/3 :focused 1/3}
                      2 {:progress-0 1/3 :progress-1 1/3 :focused 1/3}}
   :dirichlet-prior prior
   :provenance {:status :synthetic :calibrated false :source "q4 test"}})

(def prior
  {:progress-0 {:progress-0 3/2 :progress-1 1/2 :focused 1/2}
   :progress-1 {:progress-0 1/2 :progress-1 3/2 :focused 1/2}
   :focused {:progress-0 1/2 :progress-1 1/2 :focused 3/2}})

(deftest declared-dirichlet-a-produces-ambiguity-and-state-eig
  (let [r (om/query (model prior)
                    {:op :score
                     :belief {#{} 1/2 #{[:t :done]} 1/2}
                     :tau 1 :target :t
                     :preference {:progress-0 1/3 :progress-1 1/3 :focused 1/3}})]
    (is (= :computed (:status r)))
    (is (pos? (:ambiguity r)))
    (is (pos? (:information-gain r)))
    (is (= 1 (:tau r)))))

(deftest deterministic-a-remains-zero
  (let [r (om/query (dissoc (model prior) :dirichlet-prior)
                    {:op :score
                     :belief {#{} 1/2 #{[:t :done]} 1/2}
                     :tau 1 :target :t
                     :preference {:progress-0 1/3 :progress-1 1/3 :focused 1/3}})]
    (is (= :computed (:status r)))
    (is (zero? (:ambiguity r)))
    (is (pos? (:information-gain r)))))

(deftest equivalent-belief-order-does-not-break-g-ties
  (let [request {:op :score :tau 1 :target :t
                 :preference {:progress-0 1/3 :progress-1 1/3 :focused 1/3}}
        a (om/query (model prior)
                    (assoc request :belief {#{} 1/2 #{[:t :done]} 1/2}))
        b (om/query (model prior)
                    (assoc request :belief {#{[:t :done]} 1/2 #{} 1/2}))]
    (is (= (:g a) (:g b)))
    (is (= (:ambiguity a) (:ambiguity b)))
    (is (= (:information-gain a) (:information-gain b)))))
