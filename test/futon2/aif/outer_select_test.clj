(ns futon2.aif.outer-select-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-state :as outer-state]))

(def base-makeup
  {:cascades [{:id :first :patterns ["test"] :steps [{:gate-status :ready}]}
              {:id :second :patterns ["test"] :steps [{:gate-status :undetermined}]}]})
(def base-state
  {:c {:focused 55 :related 35 :unrelated 5 :stop-the-line 5
       :not-yet-evaluated 0}
   :state-counts {:ready {:doable 3 :blocked 1}
                  :not-ready {:doable 1 :blocked 3}
                  :undetermined {:doable 1 :blocked 1}}
   :gain-counts {:none 1 :one 2 :several 3}})
(def base-observation
  {:focus "M-focus"
   :relation (fn [_ target] (if (= target "M-ready") :focus :associated))
   :field [{:target "M-ready" :reach 1/2}
           {:target "M-unknown" :reach 1/2}]
   :facts {"M-ready" {:status :ready :unblocks 1 :cone 2}
           "M-unknown" {:status :undetermined :unblocks 0 :cone 1}}})

(deftest chosen-score-is-minimal
  (testing "Lean chosenCascade_score_le"
    (let [result (outer-select/select base-makeup base-state base-observation)
          chosen (nth (:cascade-scores result) (:cascade-index result))]
      (is (every? #(<= chosen %) (:cascade-scores result))))))

(deftest selection-is-from-field-or-nil
  (testing "Lean step_selection_some"
    (let [result (outer-select/select base-makeup base-state base-observation)]
      (is (some #{(get-in result [:selection :selected])} (:field base-observation)))
      (is (= (:field base-observation) (get-in result [:selection :candidates])))))
  (testing "the chosen cascade can gate everything out"
    (let [makeup {:cascades [{:id :none :patterns ["test"]
                              :steps [{:gate-status :not-ready}]}]}
          result (outer-select/select makeup base-state base-observation)]
      (is (nil? (:selection result))))))

(defonce ^:private giraffe-loader
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn actual-var [symbol]
  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
    (requiring-resolve symbol)))

(deftest real-giraffe-selection-demonstration
  (let [dir (io/file "/home/joe/code/storage/giraffe/20261028T154151.285Z-b916cd4e")]
    (if-not (.isDirectory dir)
      (is true "skipped: retained Giraffe run directory is absent")
      (let [makeup (outer-state/load-makeup)
            state (outer-state/init-state makeup)
            field (binding [outer-observation/*resolve-var* actual-var]
                    (outer-observation/giraffe-field dir makeup))
            observation (assoc field
                               :focus "M-wm-outer-selection"
                               ;; Relation evidence is packet 4b's integration
                               ;; input.  This demonstration holds it constant
                               ;; so the real Giraffe field isolates the menu.
                               :relation (fn [_ _] :focus))
            result (outer-select/select makeup state observation)]
        (println "outer-select real run demonstration"
                 (pr-str {:cascade-id (:cascade-id result)
                          :mission (get-in result [:selection :selected :target])
                          :scores (:cascade-scores result)
                          :tie? (:tie? result)}))
        (is (some? (:selection result)))
        (is (every? #(and (number? %) (Double/isFinite (double %)))
                    (:cascade-scores result)))))))
