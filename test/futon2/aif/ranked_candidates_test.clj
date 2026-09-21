(ns futon2.aif.ranked-candidates-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-cli :as cli]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.policy :as policy]))

(def recorded-decision
  (:decision (edn/read-string
              (slurp (io/resource "fixtures/narrative-trace/ranking-1789964661.edn")))))

(defn checkpoint-ranking [decision]
  (runner/ranked-candidates {:decision decision}))

(deftest recorded-certificate-keeps-quantities-and-identities
  ;; Deliberately reverse insertion order. All three real actions use :id :C1;
  ;; joining on that label would silently attach the wrong G to two targets.
  (let [decision (update-in recorded-decision [:selection-law :posterior]
                            #(into (array-map) (reverse %)))
        rows (checkpoint-ranking decision)
        cs (get-in recorded-decision [:selection-certificate :candidates])]
    (is (= 3 (count rows)))
    ;; 002-selection.edn records :passes? true, epsilon 1e-6, top-k 5.
    (is (= {:passes? true :epsilon 1.0e-6 :top-k 5}
           (select-keys (runner/selection-discrimination rows) [:passes? :epsilon :top-k])))
    (is (= (mapv :g cs) (mapv :G rows)))
    (is (= [0.3336188600826242 0.333197695627431 0.3331834442899445]
           (mapv :posterior rows)))
    (is (= [1 2 3] (mapv :rank rows)))
    (is (= (mapv #(get-in % [:id :target]) cs) (mapv :target rows)))
    (is (= [:C1 :C1 :C1] (mapv :cascade-id rows)))
    (is (= (mapv :habit cs) (mapv :habit rows)))
    (is (= (mapv :f cs) (mapv :F rows)))
    (is (= (mapv :f-status cs) (mapv :f-status rows)))
    (doseq [row rows]
      (is (not= (:G row) (:posterior row)))
      (is (= (:G row) (:G-efe row)))
      (is (not (contains? row :controller-score)))
      ;; The existing CLI consumer still prints G, never posterior.
      (is (.contains (#'cli/candidate-label row) (str "G=" (:G row)))))))

(deftest ranking-retains-more-than-ten-and-uses-action-tie-rule
  (let [actions (mapv (fn [n] {:kind :cascade-candidate :id (keyword (str "C" n))
                               :target (str "M" n)
                               :precedence [(keyword (format "pattern/p%02d" (- 12 n)))]})
                      (range 12))
        decision (policy/select-action-cascades
                  (mapv #(hash-map :action % :controller-score 15.0) actions) {:beta 1})
        rows (checkpoint-ranking decision)]
    (is (= :action-name-ascending (get-in decision [:selection-law :tie-break-rule])))
    (is (= 12 (count rows)))
    (is (= (mapv :target (reverse actions)) (mapv :target rows)))
    (is (= (vec (range 1 13)) (mapv :rank rows)))))

(deftest discrimination-requires-finite-posterior
  (doseq [p [nil ##NaN ##Inf ##-Inf]]
    (is (false? (:passes? (runner/selection-discrimination [{:posterior p}])))))
  (is (true? (:passes? (runner/selection-discrimination [{:posterior 1.0}])))))

(deftest discrimination-compares-posterior-not-g
  (is (true? (:passes? (runner/selection-discrimination
                        [{:G 15.0 :posterior 0.7} {:G 15.0 :posterior 0.3}]))))
  (is (false? (:passes? (runner/selection-discrimination
                         [{:G 10.0 :posterior 0.5} {:G 20.0 :posterior 0.5}]))))
  (is (false? (:passes? (runner/selection-discrimination
                         [{:posterior 0.5000001} {:posterior 0.4999999}])))))

(deftest absent-certificate-never-turns-posterior-into-g
  (let [rows (runner/ranked-candidates
              {:decision {:selection-law {:posterior [[{:type :no-op} 1.0]]}}})]
    (is (= [{:rank 1 :action {:type :no-op} :target nil :cascade-id nil
             :G nil :G-efe nil :habit nil :F nil :f-status :not-recorded
             :posterior 1.0}]
           rows))))

(deftest unknown-tie-rule-is-not-invented
  (is (= :unsupported-ranking-tie-rule
         (try
           (runner/ranked-candidates
            {:decision {:selection-law {:posterior {:a 0.5 :b 0.5}
                                         :tie-break-rule :unknown}}})
           nil
           (catch clojure.lang.ExceptionInfo e (:failure-kind (ex-data e)))))))
