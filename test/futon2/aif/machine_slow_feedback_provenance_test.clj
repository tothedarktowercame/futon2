(ns futon2.aif.machine-slow-feedback-provenance-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-slow-feedback-provenance :as provenance]
            [futon2.aif.machine-slow-state-carrier :as carrier]
            [futon2.aif.machine-slow-state-carrier-test :as carrier-test])
  (:import (java.nio.file Files)
           (java.security MessageDigest)
           (java.util Base64)))

(def capture-root
  "holes/labs/wm-contract/runs/row-22-e6b-canonical-fixture-2026-09-13/captured")
(def replay-root "holes/labs/wm-contract/runs/row-22-e6b-canonical-replay-2026-09-13")
(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- descriptor [path]
  (let [bs (Files/readAllBytes (.toPath (io/file path)))
        value (edn/read-string (String. bs "UTF-8"))]
    {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
     :source-sha256 (sha256 bs)
     :value-sha256 (sha256 (.getBytes (pr-str value) "UTF-8"))}))
(def closure-paths
  {:e3/pending "e3/pending.edn" :e3/verdict "e3/verdict.edn"
   :e3/review "e3/review.edn" :r9/input "config/r9-input.edn"
   :config/e1 "config/e1-config.edn" :config/e2b "config/e2b-config.edn"
   :config/e3 "config/e3-config.edn" :config/canonical "config/canonical-config.edn"
   :e1/ranked-support "../../row-22-e1-authority-resolution-2026-09-13/fixtures/ranked-support.edn"
   :e1/field-membership "../../row-22-e1-authority-resolution-2026-09-13/fixtures/field-membership.edn"
   :e1/costs "../../row-22-e1-authority-resolution-2026-09-13/fixtures/costs.edn"
   :e1/utilities "../../row-22-e1-authority-resolution-2026-09-13/fixtures/utilities.edn"
   :e1/budgets "../../row-22-e1-authority-resolution-2026-09-13/fixtures/budgets.edn"
   :e2b/context "../../row-22-e2b-correspondence-2026-09-13/fixtures/context.edn"
   :e2b/selection "../../row-22-e2b-correspondence-2026-09-13/fixtures/selection.edn"
   :e2b/enactment "../../row-22-e2b-correspondence-2026-09-13/fixtures/enactment.edn"})
(defn- input []
  (let [bundle (#'carrier-test/bundle)
        projection (carrier/project-transition bundle)]
    {:proposal-evidence (:proposal-evidence bundle)
     :original-sources (:original-sources bundle)
     :carrier-projection projection
     :canonical-closure
     (into {} (map (fn [[role relative]]
                     [role (descriptor (str capture-root "/" relative))]) closure-paths))
     :canonical-outputs
     {:e3 (descriptor (str replay-root "/e3-output.edn"))
      :e2b (descriptor (str replay-root "/e2b-output.edn"))}
     :expected-head {:store/id "isolated-store-1" :generation 0
                     :transaction-sha256 (apply str (repeat 64 "a"))
                     :state/revision (get-in projection [:prior :carrier :state/revision])
                     :state-sha256 (get-in projection [:prior :sha256])}}))
(defn- refusal [x]
  (try (provenance/construct x) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest deterministic-complete-provenance-envelope
  (let [a (provenance/construct (input)) b (provenance/construct (input))]
    (is (= a b))
    (is (= :structural-artifact (:status a)))
    (is (= :none (:authority/status a)))
    (is (= #{:context :prior :e2b :outcome}
           (set (keys (get-in a [:record :retrospective-application-view :input/digests])))))
    (is (= (get-in a [:record :carrier-projection :digest-roles :complete-next-record])
           (get-in a [:record :retrospective-application-view :output/digest])))
    (is (= (set provenance/closure-roles)
           (set (keys (get-in a [:record :canonical-closure :inputs])))))))

(deftest missing-corrupt-and-borrowed-closure-refusals
  (is (= :e6b-provenance/closure-incomplete
         (refusal (update (input) :canonical-closure dissoc :r9/input))))
  (is (= :e6b-provenance/digest-mismatch
         (refusal (assoc-in (input) [:canonical-closure :e3/pending :source-sha256]
                            (apply str (repeat 64 "0"))))))
  (let [i (input) e3 (get-in i [:canonical-outputs :e3])
        value (assoc (edn/read-string
                      (String. (.decode (Base64/getDecoder) (:bytes/base64 e3)) "UTF-8"))
                     :identity {:model/id :borrowed})
        bs (.getBytes (pr-str value) "UTF-8")]
    (is (= :e6b-provenance/closure-join-mismatch
           (refusal (assoc-in i [:canonical-outputs :e3]
                              {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
                               :source-sha256 (sha256 bs) :value-sha256 (sha256 bs)}))))))

(deftest carrier-head-and-interface-refusals
  (doseq [[label changed expected]
          [[:carrier #(assoc-in % [:carrier-projection :prior :sha256]
                               (apply str (repeat 64 "0")))
            :e6b-provenance/carrier-projection-mismatch]
           [:head-revision #(assoc-in % [:expected-head :state/revision] "borrowed")
            :e6b-provenance/expected-head-mismatch]
           [:head-pin #(assoc-in % [:expected-head :state-sha256]
                                (apply str (repeat 64 "0")))
            :e6b-provenance/expected-head-mismatch]
           [:candidate-bool #(assoc % :verified? true) :e6b-provenance/schema-invalid]]]
    (testing (name label) (is (= expected (refusal (changed (input))))))))
