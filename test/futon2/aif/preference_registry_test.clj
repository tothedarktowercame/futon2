(ns futon2.aif.preference-registry-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.factorized-preference-sut :as preference]
            [futon2.aif.preference-registry :as registry]))

(defn- refusal-kind [f]
  (try
    (f)
    nil
    (catch clojure.lang.ExceptionInfo e
      (:kind (ex-data e)))))

(defn- with-registry [registry f]
  (let [file (java.io.File/createTempFile "preference-registry-" ".edn")]
    (try
      (spit file (pr-str registry))
      (f (.getPath file))
      (finally
        (.delete file)))))

(defn- shipped-registry []
  (edn/read-string (slurp (io/resource registry/default-resource))))

(deftest shipped-registry-loads-validates-and-builds-authorized-kernel
  (let [carrier (registry/load!)
        calibration (get-in carrier [:certificate :calibration])]
    (is (= :wm/factorized-preference-v1 (:schema carrier)))
    (is (identical? (:calibration-authorizations carrier)
                    (get-in carrier [:registries :calibration])))
    (is (identical? (:factorization-authorizations carrier)
                    (get-in carrier [:registries :factorization])))
    (is (= carrier (preference/validate-certificate! carrier)))
    (is (= {:costs {:focused 0 :related 1 :unrelated 2 :stop-the-line 3}
            :probabilities {:focused 12/25 :related 6/25
                            :unrelated 4/25 :stop-the-line 3/25}
            :law :completion-then-conditional-relevance}
           (preference/kernel calibration)))
    (is (= 3001 (get-in carrier [:certificate :calibration :authority])))
    (is (= 3001 (get-in carrier [:certificate :factorization-authority])))
    (is (= 1001 (get-in carrier [:certificate :selector-authority])))
    (is (not= (get-in carrier [:certificate :selector-authority])
              (get-in carrier [:certificate :calibration :authority])))
    (is (not= (get-in carrier [:certificate :selector-authority])
              (get-in carrier [:certificate :factorization-authority])))))

(deftest certificate-refusals-preserve-existing-kinds
  (let [carrier (registry/load!)
        invalid-costs (-> carrier
                          (assoc-in [:certificate :calibration :focused-cost] 2)
                          (assoc-in [:calibration-authorizations 0
                                     :calibration :focused-cost] 2))
        bad-cost-registry
        (assoc-in (shipped-registry)
                  [:calibration-authorizations 0 :calibration :focused-cost] 2)
        missing-receipt-registry
        (update-in (shipped-registry) [:calibration-authorizations 0]
                   dissoc :receipt)
        self-authorized-registry
        (assoc (shipped-registry) :selector-authority 3001)
        substituted-registry
        (assoc-in (shipped-registry)
                  [:factorization-authorizations 0 :implementation]
                  :substituted/implementation)
        load-kind (fn [value]
                    (with-registry value
                      (fn [path]
                        (refusal-kind #(registry/load! path)))))]
    (testing "cost order"
      (is (= :certificate-invalid
             (refusal-kind
              #(preference/validate-certificate! invalid-costs))))
      (is (= :certificate-invalid (load-kind bad-cost-registry))))
    (testing "missing receipt"
      (is (= :calibration-authorization-mismatch
             (refusal-kind
              #(preference/validate-certificate!
                (assoc-in carrier [:certificate :calibration-authorization]
                          999999)))))
      (is (= :preference-registry-invalid
             (load-kind missing-receipt-registry))))
    (testing "selector cannot authorize its own calibration"
      (is (= :certificate-invalid
             (refusal-kind
              #(preference/validate-certificate!
                (assoc-in carrier [:certificate :selector-authority] 3001)))))
      (is (= :certificate-invalid (load-kind self-authorized-registry))))
    (testing "implementation pin drift"
      (is (= :factorization-authorization-mismatch
             (refusal-kind
              #(preference/validate-certificate!
                (assoc-in carrier [:factorization-authorizations 0
                                   :implementation-pin]
                          "changed")))))
      (is (= :factorization-authorization-mismatch
             (load-kind substituted-registry))))))

(deftest loader-refuses-absence-and-malformation
  (let [missing (str (System/getProperty "java.io.tmpdir")
                     "/missing-preference-registry-" (random-uuid) ".edn")
        malformed (java.io.File/createTempFile "preference-registry-" ".edn")]
    (try
      (spit malformed "{:schema")
      (is (= :preference-registry-unconfigured
             (refusal-kind #(registry/load! missing))))
      (is (= :preference-registry-invalid
             (refusal-kind #(registry/load! (.getPath malformed)))))
      (finally
        (.delete malformed)))))
