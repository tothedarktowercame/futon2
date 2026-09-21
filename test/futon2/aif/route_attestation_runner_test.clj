(ns futon2.aif.route-attestation-runner-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.route-attestation-test :as route-fixture]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(deftest grounded-close-retains-account-without-changing-decision
  ;; Canonical owner runs this; never bypass the source-authority guard.
  (let [base (:result (#'fixture/run-feature-card-attempt {:author-card fixture/feature-card-claim}))
        result (:result (#'fixture/run-feature-card-attempt
                         {:author-card fixture/feature-card-claim
                          :runner-options {:route-attestation (:declarations route-fixture/input)}}))
        r (:route-attestation result) ref (:route-attestation-ref result)]
    (is (= :grounded-change (:outcome base) (:outcome result)))
    (is (= (pr-str (get-in base [:checkpoints :selection :judgment :controller-decision]))
           (pr-str (get-in result [:checkpoints :selection :judgment :controller-decision]))))
    (is (= :wm/route-attestation-v1 (:schema r)))
    (is (empty? (:increments r)))
    (is (= 11 (count (:iad-profile r))))
    (is (str/ends-with? (:path ref) "/retained/route-attestation.edn"))
    (is (= (:sha256 ref) (identity/sha256 (java.nio.file.Files/readAllBytes (.toPath (io/file (:path ref)))))))
    (is (= r (edn/read-string (slurp (:path ref)))))))
