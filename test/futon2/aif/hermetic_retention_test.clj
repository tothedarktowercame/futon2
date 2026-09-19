(ns futon2.aif.hermetic-retention-test
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.repair-obligation :as repair]))

(defn- files [root]
  (set (for [f (file-seq (io/file root)) :when (.isFile f)] (.getCanonicalPath f))))

(deftest direct-retention-debug-call-cannot-leak-its-binding-refusal
  ;; Deliberately do NOT install with-hermetic-stores: reproduce direct helper
  ;; use, with the real production default still bound, as in the leaked calls.
  (let [before (files repair/default-root)]
    (fixture/with-hermetic-traces
     (fn []
       (doseq [prefix ["debug-standing-readback" "debug-baseline"]]
         (let [{:keys [root] :as cohort} (#'fixture/retention-cohort prefix)
               opts (assoc (#'fixture/retention-success-opts cohort)
                           :tripwire/cohort-history []
                           :tripwire/a-matrix-events []
                           :tripwire/grounding-witnesses [])
               temp-store (:repair-root opts)]
           (try
             (is (not= repair/default-root temp-store))
             (let [result (binding [runner/*wm-status-reporting?* false]
                            (runner/run-opportunity! opts))
                   finding (get-in result [:data :repair-obligation])
                   path (io/file temp-store "findings" (str (:repair/id finding) ".edn"))]
               (is (= :artifact-binding-mismatch (:failure-kind finding)))
               (is (= "retention-author" (get-in finding [:failure-data :author-job :job-id])))
               (is (.isFile path))
               (is (= (:repair/id finding) (:repair/id (edn/read-string (slurp path))))))
             (finally
               (doseq [dir [root temp-store]
                       f (reverse (file-seq (io/file dir)))] (io/delete-file f true))))))))
    (is (= before (files repair/default-root)) "No production repair file added")))
