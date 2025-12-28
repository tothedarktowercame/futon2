(ns ants.aif.microtrace-regression-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ants.aif.core :as core]))

(def default-opts {:max-steps 4 :alpha 0.45 :beta 0.28})

(defn- base-world []
  {:grid {:size [7 7]
          :max-food 5.0
          :max-pher 4.0
          :cells {[2 2] {:food 4.5 :pher 1.5}
                  [2 3] {:food 2.8 :pher 1.0}
                  [3 2] {:food 1.2 :pher 2.0}
                  [1 2] {:food 0.4 :pher 0.3}
                  [2 1] {:food 0.3 :pher 0.1}
                  [4 2] {:food 0.6 :pher 1.8}
                  [4 3] {:food 4.2 :pher 2.2}
                  [5 3] {:food 3.8 :pher 0.9}
                  [5 4] {:food 0.5 :pher 0.7}
                  [0 0] {:home :aif :food 0.0 :pher 0.0}
                  [6 6] {:home :classic :food 0.0 :pher 0.0}}}
   :homes {:aif [0 0]
           :classic [6 6]}
   :colonies {:aif {:reserves 2.5}
              :classic {:reserves 2.0}}
   :config {:hunger {:queen {:initial 4.0}}}})

(defn- base-ant []
  {:species :aif
   :loc [2 2]
   :cargo 0.35
   :ingest 0.18
   :recent-gather 0.22
   :visit-counts {[2 2] 5
                  [2 3] 2
                  [3 2] 1}
   :mu {:h 0.62
        :pos [2 2]
        :goal [5 5]}
   :prec {:tau 1.1}})

(def scenarios
  [{:name :baseline
    :world (base-world)
    :ant (base-ant)}
   {:name :hungry-near-enemy
    :world (assoc-in (base-world) [:colonies :aif :reserves] 0.4)
    :ant (-> (base-ant)
             (assoc :loc [4 4])
             (assoc-in [:mu :h] 0.92))}
   {:name :sated-on-trail
    :world (update-in (base-world) [:grid :cells [2 2]] assoc :pher 3.6 :food 4.8)
    :ant (-> (base-ant)
             (assoc-in [:mu :h] 0.18)
             (assoc :ingest 0.65))}
   {:name :homebound-low-pher
    :world (update-in (base-world) [:grid :cells [2 2]] assoc :pher 0.0 :food 0.2)
    :ant (-> (base-ant)
             (assoc :cargo 0.8)
             (assoc-in [:mu :h] 0.55)
             (assoc :loc [1 1]))}])

(def microtrace-goldens
  (-> (io/file "test" "resources" "goldens" "microtraces.edn")
      slurp
      edn/read-string))

(defn- evaluate [{:keys [name world ant]}]
  (let [{:keys [perception]} (core/aif-step world ant default-opts)]
    {:name name
     :trace (:trace perception)}))

(deftest microtraces-stable
  (doseq [scenario scenarios
          :let [expected (some #(when (= (:name scenario) (:name %)) %) microtrace-goldens)]]
    (testing (name (:name scenario))
      (is expected)
      (is (= expected (evaluate scenario))))))
