(ns futon2.aif.wm.cascade-capability-dispatch-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.wm.cascade-capability-dispatch :as sut]))

(def digest (apply str (repeat 64 "a")))
(def node {:occurrence-id ["p/x" 0]
           :clause {:operation :apply-pattern
                    :capability :wm/pattern-apply}})
(def cascade {:digest digest :frontier [node]})

(defn receipt [request]
  (sut/seal-receipt
   {:schema sut/receipt-schema
    :cascade-digest (:cascade-digest request)
    :occurrence-id (get-in request [:node :occurrence-id])
    :operation (get-in request [:node :clause :operation])
    :capability (get-in request [:node :clause :capability])
    :evidence-kind :verified-effect
    :effect {:changed :test-substrate}}))

(deftest missing-executor-is-a-typed-fault
  (is (= :capability-executor-missing
         (:kind (sut/dispatch-frontier {} cascade)))))

(deftest production-routing-does-not-claim-an-effect
  (is (= :capability-route-missing
         (:kind (sut/route-frontier {} cascade))))
  (let [r (sut/route-frontier
           {:wm/pattern-apply :full-loop-build-review-close} cascade)]
    (is (= :routed (:status r)))
    (is (= :full-loop-build-review-close (get-in r [:steps 0 :route])))
    (is (not (contains? r :receipts)))))

(deftest receipt-must-bind-the-exact-cascade-and-occurrence
  (is (= :enacted
         (:status (sut/dispatch-frontier {:wm/pattern-apply receipt} cascade))))
  (is (= :capability-effect-receipt-invalid
         (:kind (sut/dispatch-frontier
                 {:wm/pattern-apply
                  (fn [request]
                    (assoc (receipt request) :cascade-digest (apply str (repeat 64 "b"))))}
                 cascade))))
  (is (= :capability-effect-receipt-invalid
         (:kind (sut/dispatch-frontier
                 {:wm/pattern-apply (fn [_] {:status :ok})} cascade)))))

(deftest recording-executor-produces-an-append-only-verifiable-effect
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "pattern-effects-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        executor (sut/recording-pattern-executor (.getPath root))
        first-result (sut/dispatch-frontier {:wm/pattern-apply executor} cascade)
        second-result (sut/dispatch-frontier {:wm/pattern-apply executor} cascade)
        receipt (first (:receipts first-result))]
    (is (= :enacted (:status first-result)))
    (is (= (:receipts first-result) (:receipts second-result)))
    (is (.isFile (io/file (get-in receipt [:effect :path]))))))
