(ns futon2.aif.f40a-selected-entry-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]))

(def fixture-path
  "test/futon2/aif/fixtures/trace-decision-2026-10-20-881a8839.edn")

(deftest sealed-outer-policy-route-produces-an-addressable-entry
  ;; The fixture is the exact :decision map retained for run
  ;; 2026-10-20-881a8839-d725-493e-95cf-eb06dd71b92c, sha256
  ;; 9a643f9c373f07334ca97218a021e93115662011cb5c55538acc281b9b1ccc03.
  (let [decision (edn/read-string (slurp fixture-path))
        selected-entry #'runner/selected-entry
        entry (selected-entry {:decision decision})]
    (is (= "M-formal-war-machine/policy-95a29538723f94edfa51"
           (get-in entry [:action :id])))
    (is (some? (:canonical-cascade entry)))

    (testing "only the sealed selected route is admitted"
      (is (nil? (selected-entry {:decision (dissoc decision :action)})))
      (is (nil? (selected-entry {:decision (assoc decision :status :abstained)})))
      (is (nil? (selected-entry
                 {:decision (update decision :selection-certificate
                                    dissoc :selection-route)}))))))
