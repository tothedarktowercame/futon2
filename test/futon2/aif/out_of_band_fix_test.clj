(ns futon2.aif.out-of-band-fix-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.out-of-band-fix :as fix]
            [futon2.aif.meta-self-heal-production :as production]))

(deftest checked-three-evidence-rule
  (let [green {:tests 1 :failures 0 :errors 0}
        red {:tests 1 :failures 7 :errors 0}]
    (is (fix/qualifies? true green red))
    (is (not (fix/qualifies? false green red)))
    (is (not (fix/qualifies? true green green)))
    (is (not (fix/qualifies? true green (assoc red :errors 1))))
    (is (not (fix/qualifies? true (assoc green :tests 0) red)))))

(deftest absent-artifact-preserves-ordinary-replay
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "oob-empty" (make-array java.nio.file.attribute.FileAttribute 0)))
        calls (atom 0)
        run (fn [_] (swap! calls inc))
        request {:phase :retry :work-order {:injury-id "injury/absent"
                                          :repair-id "repair/absent" :attempt 1}}]
    (is (nil? (fix/compute! root (:work-order request) run)))
    (is (= ((production/ordinary-acceptance-replay-port run root) request)
           ((production/acceptance-replay-port run root) request)))
    (is (zero? @calls))))

(deftest artifact-refusal-does-not-fall-through-to-ordinary-discharge
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "oob-refusal" (make-array java.nio.file.attribute.FileAttribute 0)))
        calls (atom 0)
        refused {:status :refused :reason :out-of-band/counterfactual-not-causal}
        result (with-redefs [fix/compute! (fn [& _] refused)]
                 ((production/acceptance-replay-port (fn [_] (swap! calls inc)) root)
                  {:work-order {}}))]
    (is (= :refused (:status result)))
    (is (= refused (clojure.edn/read-string
                    (fix/read-pin (:out-of-band-receipt-pin result)))))
    (is (zero? @calls))))
