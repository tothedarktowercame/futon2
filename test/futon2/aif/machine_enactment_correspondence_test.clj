(ns futon2.aif.machine-enactment-correspondence-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-budget-authority :as authority]
            [futon2.aif.machine-enactment-correspondence :as correspondence]
            [futon2.aif.machine-portfolio-restriction :as e2a])
  (:import (java.nio.file Files StandardCopyOption)
           (java.security MessageDigest)))

(def e1-root "holes/labs/wm-contract/runs/row-22-e1-authority-resolution-2026-09-13/fixtures")
(def witness-root "holes/labs/wm-contract/runs/row-22-e2b-correspondence-2026-09-13/fixtures")
(def e1-files {:ranked-support "ranked-support.edn" :field-membership "field-membership.edn"
               :costs "costs.edn" :utilities "utilities.edn" :budgets "budgets.edn"})

(defn- hex [bytes] (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))
(defn- digest [file]
  (hex (.digest (doto (MessageDigest/getInstance "SHA-256")
                  (.update (Files/readAllBytes (.toPath (io/file file))))))))
(defn- e1-config []
  {:resolver/version authority/resolver-version :mode :isolated-test :root e1-root
   :sources (into {} (map (fn [[label filename]]
                            [label {:relative-path filename :sha256 (digest (io/file e1-root filename))}]))
                  e1-files)})
(defn- config
  ([] (config witness-root))
  ([root]
   {:mode :isolated-test :e2a-resolver (e1-config) :witness-root (str root)
    :witnesses {:context {:relative-path "context.edn"
                          :sha256 (digest (io/file root "context.edn"))}
                :selection {:relative-path "selection.edn"
                            :sha256 (digest (io/file root "selection.edn"))}
                :enactment {:relative-path "enactment.edn"
                            :sha256 (digest (io/file root "enactment.edn"))}}}))
(defn- copy-witnesses []
  (let [dir (.toFile (Files/createTempDirectory "e2b-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (doseq [name ["context.edn" "selection.edn" "enactment.edn"]]
      (Files/copy (.toPath (io/file witness-root name)) (.toPath (io/file dir name))
                  (into-array StandardCopyOption [StandardCopyOption/REPLACE_EXISTING])))
    dir))
(defn- replace-in! [root name old new]
  (let [file (io/file root name)] (spit file (str/replace (slurp file) old new))))
(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest exact-approved-selection-enactment-correspondence
  (let [out (correspondence/verify-correspondence (config))]
    (is (= :exact-occurrence-and-action (:correspondence out)))
    (is (= (:selected out) (:enacted out)))
    (is (= 2 (count (:approved-domain out))))
    (is (= :required-external-dependency
           (get-in out [:r9-pre-enact-authorization :status])))
    (is (= :external-dependency (get-in out [:selection-proof :status])))
    (is (= 3 (count (get-in out [:sources :witnesses]))))
    (is (= "e2b-selection-enactment-event-1" (:event/id out)))))

(deftest caller-shaped-selection-and-verified-labels-are-ignored
  (let [out (correspondence/verify-correspondence
             (assoc (config) :selected/occurrence-id [:forged 0]
                    :verified true :scope :production))]
    (is (= [:e1-authority-run 4 0] (get-in out [:selected :candidate/id])))
    (is (= :isolated-test (:scope out)))))

(deftest missing-forged-and-stale-witnesses-refuse
  (testing "missing witness pin"
    (is (= :e2b/witness-set-incomplete
           (refusal #(correspondence/verify-correspondence
                      (update (config) :witnesses dissoc :selection))))))
  (testing "changed bytes with unchanged pin"
    (let [root (copy-witnesses) cfg (config root)]
      (replace-in! root "selection.edn" "M-alpha" "M-forged")
      (is (= :e2b/witness-pin-mismatch
             (refusal #(correspondence/verify-correspondence cfg))))))
  (testing "cross-run selection witness"
    (let [root (copy-witnesses)]
      (replace-in! root "selection.edn" "e1-authority-run" "stale-run")
      (is (= :e2b/cross-run-witness
             (refusal #(correspondence/verify-correspondence (config root))))))))

(deftest independent-cohort-event-and-subject-controls
  (testing "missing context cohort"
    (let [root (copy-witnesses)]
      (replace-in! root "context.edn" ":cohort/id \"e2b-isolated-cohort-1\""
                   ":cohort/id nil")
      (is (= :e2b/context-identity-missing
             (refusal #(correspondence/verify-correspondence (config root)))))))
  (testing "borrowed cohort agreed by both witnesses does not override context"
    (let [root (copy-witnesses)]
      (doseq [name ["selection.edn" "enactment.edn"]]
        (replace-in! root name "e2b-isolated-cohort-1" "borrowed-cohort"))
      (is (= :e2b/cross-cohort-witness
             (refusal #(correspondence/verify-correspondence (config root)))))))
  (testing "changed event agreed by both witnesses does not override context"
    (let [root (copy-witnesses)]
      (doseq [name ["selection.edn" "enactment.edn"]]
        (replace-in! root name "e2b-selection-enactment-event-1" "other-event"))
      (is (= :e2b/cross-event-witness
             (refusal #(correspondence/verify-correspondence (config root)))))))
  (testing "same run/tick but replacement E1 bytes make witnesses stale"
    (let [restricted (e2a/restrict-portfolio (e1-config))
          stale (assoc-in restricted [:source :e1-verification :sources 0 :sha256]
                          (apply str (repeat 64 "0")))]
      (is (= :e2b/context-subject-mismatch
             (refusal #(with-redefs [e2a/restrict-portfolio (fn [_] stale)]
                         (correspondence/verify-correspondence (config)))))))))

(deftest domain-and-occurrence-identity-controls
  (testing "approved domain is complete and ordered"
    (let [root (copy-witnesses)]
      (replace-in! root "selection.edn"
                   ":approved-domain/occurrence-ids\n [[:e1-authority-run 4 0] [:e1-authority-run 4 2]]"
                   ":approved-domain/occurrence-ids\n [[:e1-authority-run 4 2] [:e1-authority-run 4 0]]")
      (is (= :e2b/approved-domain-mismatch
             (refusal #(correspondence/verify-correspondence (config root)))))))
  (testing "equal action at rejected occurrence is not approved"
    (let [root (copy-witnesses)]
      (replace-in! root "selection.edn"
                   ":selected/occurrence-id [:e1-authority-run 4 0]"
                   ":selected/occurrence-id [:e1-authority-run 4 1]")
      (replace-in! root "enactment.edn"
                   ":selected/occurrence-id [:e1-authority-run 4 0]"
                   ":selected/occurrence-id [:e1-authority-run 4 1]")
      (replace-in! root "enactment.edn"
                   ":enacted/occurrence-id [:e1-authority-run 4 0]"
                   ":enacted/occurrence-id [:e1-authority-run 4 1]")
      (is (= :e2b/selected-occurrence-not-approved
             (refusal #(correspondence/verify-correspondence (config root)))))))
  (testing "first-passing substitution with equal action refuses by occurrence"
    (let [root (copy-witnesses)]
      (replace-in! root "enactment.edn"
                   ":enacted/occurrence-id [:e1-authority-run 4 0]"
                   ":enacted/occurrence-id [:e1-authority-run 4 1]")
      (is (= :e2b/selection-enactment-mismatch
             (refusal #(correspondence/verify-correspondence (config root))))))))

(deftest action-proof-divergence-and-production-controls
  (testing "selected action bytes must come from approved occurrence"
    (let [root (copy-witnesses)]
      (replace-in! root "selection.edn"
                   ":selected/action {:type :advance-mission :target \"M-alpha\"}"
                   ":selected/action {:type :advance-mission :target \"M-tampered\"}")
      (is (= :e2b/selected-action-mutation
             (refusal #(correspondence/verify-correspondence (config root)))))))
  (testing "selection proof remains an external dependency"
    (let [root (copy-witnesses)]
      (replace-in! root "selection.edn" ":status :external-dependency" ":status :verified")
      (is (= :e2b/selection-proof-dependency-missing
             (refusal #(correspondence/verify-correspondence (config root)))))))
  (testing "generic typed divergence is unsupported"
    (let [root (copy-witnesses)]
      (replace-in! root "enactment.edn" ":status :enacted" ":status :typed-divergence")
      (is (= :e2b/divergence-unsupported
             (refusal #(correspondence/verify-correspondence (config root)))))))
  (testing "isolated files cannot authorize production"
    (is (= :e2b/production-authority-unavailable
           (refusal #(correspondence/verify-correspondence
                      (assoc (config) :mode :production)))))))
