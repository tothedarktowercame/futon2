(ns futon2.aif.f42a-receipt-menu-once-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-policy-runtime-selection-test]
            [futon2.aif.selected-outer-cascade-policy :as selected]))

(defn- fixture-var [sym]
  (var-get (ns-resolve 'futon2.aif.outer-policy-runtime-selection-test sym)))

(deftest receipt-retains-one-complete-sealed-menu
  (let [run-live (fixture-var 'run-live)
        explicit-provider (fixture-var 'explicit-provider)]
    (testing "a selected receipt points to the carrier's verified menu"
      (let [receipt (run-live (explicit-provider [0.0 1.0] []))
            carrier (get-in receipt [:policy :selected-outer-cascade-policy])
            pointer (get-in receipt [:policy :sealed-menu])]
        (is (= :carried-on-carrier (:status pointer)))
        (is (= (:menu-pin carrier) (:menu-pin pointer)))
        (is (= [:selected-outer-cascade-policy :sealed-menu] (:at pointer)))
        (is (nil? (selected/verify carrier)))
        (is (seq (get-in carrier [:sealed-menu :entries])))))

    (testing "without an activated carrier the receipt remains the menu authority"
      (let [receipt (run-live nil)
            menu (get-in receipt [:policy :sealed-menu])]
        (is (= :wm/sealed-outer-policy-menu-v1 (:schema menu)))
        (is (seq (:entries menu)))
        (is (not= :carried-on-carrier (:status menu)))))))
