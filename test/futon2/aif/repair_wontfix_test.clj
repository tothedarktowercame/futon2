(ns futon2.aif.repair-wontfix-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [futon2.aif.repair-obligation :as r]))

(defn- root []
  (str (java.nio.file.Files/createTempDirectory
        "wontfix-test-" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- record! [root dir record]
  (let [f (io/file root dir (str (:repair/id record) ".edn"))]
    (io/make-parents f)
    (spit f (pr-str record))
    f))

(def finding {:repair/id "test-permanent" :repair/status :open
              :attempt-id "test-attempt" :opened-at "2026-09-21T00:00:00Z"})
(def disposition
  {:authority "Operator fixture ruling: permanently retire synthetic instrument"
   :reason "Fixture instrument's sole measurement target was permanently destroyed; no replacement is permitted by the fixture ruling."
   :actor "test-reviewer"})

(defn- refused [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e
                 (:repair-dismissal/refusal (ex-data e)))))

(deftest append-only-visible-dismissal
  (let [root (root) f (record! root "findings" finding) before (slurp f)
        d (r/dismiss-wontfix! root (:repair/id finding) disposition)]
    (is (= :dismissed-wontfix (:repair/status d)))
    (is (= :wontfix (:dismissal/kind d)))
    (is (= disposition (select-keys d [:authority :reason :actor])))
    (is (empty? (r/open-obligations root)))
    (is (= d (:repair/dismissal (first (r/obligation-history root "test-attempt")))))
    (is (= before (slurp f)))
    (is (= :already-dismissed
           (refused #(r/dismiss-wontfix! root (:repair/id finding) disposition))))
    (is (= before (slurp f)))))

(deftest missing-and-blank-dispositions-refuse-without-writing
  (doseq [k [:authority :reason :actor]
          bad [(dissoc disposition k) (assoc disposition k "") (assoc disposition k "  ")]]
    (let [root (root) f (record! root "findings" finding) before (slurp f)]
      (is (= :disposition-invalid
             (refused #(r/dismiss-wontfix! root (:repair/id finding) bad))))
      (is (= 1 (count (r/open-obligations root))))
      (is (not (.exists (io/file root "dismissals"))))
      (is (= before (slurp f))))))

(deftest prior-dispositions-and-implementation-refuse
  (doseq [[dir expected] [["resolutions" :already-resolved]
                         ["dismissals" :already-dismissed]
                         ["implementations" :finding-not-open]]]
    (let [root (root) f (record! root "findings" finding)
          existing (record! root dir {:repair/id (:repair/id finding)})
          before (slurp existing)]
      (is (= expected (refused #(r/dismiss-wontfix! root (:repair/id finding) disposition))))
      (is (= before (slurp existing)))
      (is (= (pr-str finding) (slurp f))))))

(deftest unknown-id-and-extra-keys-refuse
  (let [root (root)]
    (is (= :finding-not-found (refused #(r/dismiss-wontfix! root "absent" disposition))))
    (is (= :finding-id-invalid (refused #(r/dismiss-wontfix! root "../escape" disposition))))
    (record! root "findings" finding)
    (is (= :disposition-invalid
           (refused #(r/dismiss-wontfix! root (:repair/id finding)
                                       (assoc disposition :override true)))))))
