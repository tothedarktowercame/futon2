(ns futon2.aif.outer-refusal-history-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-refusal-history :as history]))

(def retained-refusal
  ;; Real refused M-recommendation-bindings terminal record.
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-24-d0608eec-cca3-45de-9c19-14d6db26577d.edn")

(def retained-repick
  ;; The later real re-pick changed its selection input digest.
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-27-39eb74f5-9db6-4f1d-8d28-03694c6f1fba.edn")

(defn- temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "outer-refusal-history" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- copy-record! [dir source]
  (let [target (io/file dir (.getName (io/file source)))]
    (spit target (slurp source))
    target))

(deftest retained-repick-has-changed-inputs
  (testing "the real recommendation-bindings re-pick is not stopped"
    (let [dir (temp-dir)
          _ (copy-record! dir retained-refusal)
          current (history/record-inputs (edn/read-string (slurp retained-repick)))
          comparison (history/compare-history dir current)]
      (is (= "M-recommendation-bindings" (:target current)))
      (is (= :inputs-changed (:status comparison)))
      (is (= [:selection-input-digest]
             (get-in comparison [:comparisons 0 :differing-identities])))
      (is (nil? (:matched-refusal comparison))))))

(deftest cloned-retained-inputs-are-stopped
  (testing "changing only run id cannot authorize unchanged refused inputs"
    (let [dir (temp-dir)
          source (edn/read-string (slurp retained-refusal))
          _ (copy-record! dir retained-refusal)
          cloned (assoc source :run/id "2026-10-29-cloned-inputs")
          comparison (history/compare-history dir (history/record-inputs cloned))
          refusal (try
                    (history/require-new-inputs! comparison)
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (= :unchanged-refused-repeat (:status comparison)))
      (is (= "2026-10-24-d0608eec-cca3-45de-9c19-14d6db26577d"
             (get-in comparison [:matched-refusal :run-id])))
      (is (empty? (get-in comparison
                          [:matched-refusal :differing-identities])))
      (is (= :outer/unchanged-refused-repeat
             (:failure-kind (ex-data refusal))))
      (is (= (history/record-inputs cloned)
             (get-in (ex-data refusal)
                     [:outer-repeat-comparison :inputs]))))))
