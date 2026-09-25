(ns futon2.aif.registry-orthogonality-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.declaration-reads-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.observation-checks :as checks]))

(use-fixtures :once hermetic/with-hermetic-stores)

(deftest removed-checks-refuse-before-observation
  (fixture/with-dir
   (fn [dir]
     (let [path (io/file dir "source.edn")
           base (assoc fixture/declaration :facts [] :locators {})]
       (spit path (pr-str base))
       (is (map? (sources/load-declared (str dir))))
       (doseq [removed [:C1 :C2] field [:class :check]]
         (let [bad (assoc-in base [:locators :unused-token] {field removed})
               calls (atom 0)]
           (is (not= base bad))
           (spit path (pr-str bad))
           (let [r (with-redefs [checks/observe (fn [_] (swap! calls inc))]
                     (try (sources/load-declared (str dir)) nil
                          (catch clojure.lang.ExceptionInfo e (ex-data e))))]
             (is (= :invalid-cascade-source (:error r)))
             (is (= :removed-observation-check (:reason r)))
             (is (= removed (:check r)))
             (is (= :unused-token (:token r)))
             (is (= (str path) (:path r)))
             (is (zero? @calls)))))))))

(deftest reduced-contract-and-loaded-run-record
  (let [contract (edn/read-string (slurp (io/resource "wm/observation-contract.edn")))
        classes #{:C3 :C4 :C5 :C6 :C8}
        loaded (sources/load-declared)
        record (fixture/record-run #(sources/load-declared))]
    (is (= classes (get-in contract [:production-path :classes])))
    (is (= classes (set (keys (get-in contract [:production-path :checks-implemented])))))
    (is (= classes (set (map :id (filter #(= :checkable (:kind %)) (:classes contract))))))
    (is (= classes (set (keys checks/checks)) problems/checkable-classes))
    (doseq [[class fn-name] (get-in contract [:production-path :checks-implemented])]
      (is (= (get checks/checks class)
             (some-> (ns-resolve 'futon2.aif.observation-checks fn-name) deref))
          (str class " names " fn-name ", which is not the check observe runs")))
    (doseq [{:keys [id check-fn]} (filter #(= :checkable (:kind %)) (:classes contract))]
      (is (= check-fn (get-in contract [:production-path :checks-implemented id]))
          (str id " :check-fn disagrees with :checks-implemented")))
    (is (not (contains? (:universes loaded) "M-wm-aif-policy-grain-compliance")))
    (is (nil? (io/resource "wm/cascade-sources/M-wm-aif-policy-grain-compliance.edn")))
    (is (seq (:read-occurrences loaded)))
    (is (= :present (get-in record [:declaration-reads :status])))
    (is (every? classes (for [[_ locators] (:locators loaded) [_ locator] locators] (:class locator))))
    (println "REGISTRY-ORTHOGONALITY-TEST-RECORD" (pr-str record))))
