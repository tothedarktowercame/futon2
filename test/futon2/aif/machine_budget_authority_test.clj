(ns futon2.aif.machine-budget-authority-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-budget-authority :as authority])
  (:import (java.nio.file Files StandardCopyOption)
           (java.security MessageDigest)))

(def fixture-root
  "holes/labs/wm-contract/runs/row-22-e1-authority-resolution-2026-09-13/fixtures")

(def files
  {:ranked-support "ranked-support.edn"
   :field-membership "field-membership.edn"
   :costs "costs.edn"
   :utilities "utilities.edn"
   :budgets "budgets.edn"})

(defn- hex [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))

(defn- digest [file]
  (hex (.digest (doto (MessageDigest/getInstance "SHA-256")
                  (.update (Files/readAllBytes (.toPath (io/file file))))))))

(defn- config [root]
  {:resolver/version authority/resolver-version
   :mode :isolated-test
   :root (str root)
   :sources (into {} (map (fn [[label filename]]
                            [label {:relative-path filename
                                    :sha256 (digest (io/file root filename))}]))
                  files)})

(defn- copy-fixtures []
  (let [dir (.toFile (Files/createTempDirectory "e1-authority-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (doseq [[_ filename] files]
      (Files/copy (.toPath (io/file fixture-root filename))
                  (.toPath (io/file dir filename))
                  (into-array StandardCopyOption [StandardCopyOption/REPLACE_EXISTING])))
    dir))

(defn- replace-in! [root filename old new]
  (let [file (io/file root filename)]
    (spit file (str/replace (slurp file) old new))))

(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest resolved-files-authorize-complete-occurrence-mapping
  (let [out (authority/resolve-and-map (config fixture-root))
        support (:ordered-support out)]
    (is (= :isolated-test (:scope out)))
    (is (= {:model/id :wm-e1-fixture :model/revision "model-v3"
            :run/id "e1-authority-run" :tick/index 4}
           (:identity out)))
    (is (= 3 (count support)))
    (is (= (:action (support 0)) (:action (support 1))))
    (is (not= (:candidate/id (support 0)) (:candidate/id (support 1))))
    (is (= 5 (count (get-in out [:verification :sources]))))
    (is (= #{[:e1-authority-run 4 0] [:e1-authority-run 4 2]}
           (get-in out [:response :selected-ids])))
    (is (:replay/identical?
         ((requiring-resolve 'futon2.aif.machine-budget-mapping/replay)
          (:replay/receipt out))))))

(deftest changed-value-with-unchanged-pin-refuses
  (let [root (copy-fixtures)
        before (config root)]
    (replace-in! root "utilities.edn" " 8\n" " 9\n")
    (is (= :r6-r11/source-pin-mismatch
           (refusal #(authority/resolve-and-map before))))))

(deftest simultaneous-support-and-value-deletion-cannot-shrink-authority
  (let [root (copy-fixtures)
        before (config root)]
    (doseq [filename ["ranked-support.edn" "field-membership.edn"
                      "costs.edn" "utilities.edn"]]
      (replace-in! root filename
                   (case filename
                     "ranked-support.edn"
                     "  {:candidate/id [:e1-authority-run 4 2] :rank 3\n   :action {:type :no-op :target nil}}"
                     "field-membership.edn"
                     "\n          [:e1-authority-run 4 2] :idle-field"
                     "costs.edn" "\n          [:e1-authority-run 4 2] 1"
                     "utilities.edn" "\n          [:e1-authority-run 4 2] 1")
                   ""))
    (is (= :r6-r11/source-pin-mismatch
           (refusal #(authority/resolve-and-map before))))))

(deftest resolved-scope-cannot-be-laundered-by-config-or-source
  (testing "candidate-like config metadata is ignored"
    (let [out (authority/resolve-and-map
               (assoc (config fixture-root) :scope :production
                      :run/id "caller-run"))]
      (is (= :isolated-test (:scope out)))
      (is (= "e1-authority-run" (get-in out [:identity :run/id])))))
  (testing "consistently rehashed test sources still cannot claim production"
    (let [root (copy-fixtures)]
      (doseq [[_ filename] files]
        (replace-in! root filename ":scope :isolated-test" ":scope :production"))
      (is (= :r6-r11/scope-laundering
             (refusal #(authority/resolve-and-map (config root))))))))

(deftest isolated-files-cannot-enable-production-mode
  (is (= :r6-r11/production-authority-unavailable
         (refusal #(authority/resolve-and-map
                    (assoc (config fixture-root) :mode :production))))))

(deftest cross-run-and-unsupported-transformation-refuse-after-valid-hashing
  (testing "source-derived identities must agree"
    (let [root (copy-fixtures)]
      (replace-in! root "costs.edn" "e1-authority-run" "other-run")
      (is (= :r6-r11/cross-run-authority
             (refusal #(authority/resolve-and-map (config root)))))))
  (testing "a transformation label has no authority without an implementation"
    (let [root (copy-fixtures)]
      (replace-in! root "costs.edn" ":authority :declared-source"
                   ":authority :supported-transformation")
      (is (= :r6-r11/transformation-unsupported
             (refusal #(authority/resolve-and-map (config root))))))))

(deftest strict-reader-refuses-trailing-form-and-missing-source
  (testing "two EDN forms are not one authority record"
    (let [root (copy-fixtures)
          file (io/file root "costs.edn")]
      (spit file (str (slurp file) "\n{:second :form}\n"))
      (is (= :r6-r11/source-trailing-form
             (refusal #(authority/resolve-and-map (config root)))))))
  (testing "a configured but vanished source is not empty authority"
    (let [root (copy-fixtures)
          cfg (config root)]
      (Files/delete (.toPath (io/file root "budgets.edn")))
      (is (= :r6-r11/source-unreadable
             (refusal #(authority/resolve-and-map cfg)))))))
