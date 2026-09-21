(ns futon2.aif.scan-report-runner-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.report.scan-report-test :as scan-test]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(deftest click-retains-the-selected-scan-even-if-renderer-fails
  ;; Canonical checkout required: preserve the source-authority guard.
  (let [before (scan-test/file-count "data")
        root (.toFile (Files/createTempDirectory "click-scan-" (make-array FileAttribute 0)))]
    (try
      (doseq [[id fail?] [["scan-success" false] ["scan-failure" true]]]
        (let [calls (atom 0) rendered (atom nil)
              opts (-> (fixture/isolated-runner-opts)
                       (dissoc :judge-fn)
                       (assoc :run-id id :run-record-dir (.getPath root)
                              :repair-open-fn (constantly [])
                              :judgement-transform-fn
                              #(assoc % :active-mission (get-in scan-test/input [:judgement :active-mission]))
                              :scan-render-fn
                              (fn [data]
                                (reset! rendered data)
                                (if fail? (throw (IllegalStateException. "renderer broke"))
                                    (wm/render-war-machine data)))
                              :construct-fn (fn [& _] (throw (ex-info "stop after selection" {:outcome :incomplete})))))
              result (with-redefs [wm/generate-war-machine
                                  (fn [_ options]
                                    (is (true? (:defer-render? options)))
                                    (swap! calls inc)
                                    ;; The renderer needs a full judgement (e.g. the
                                    ;; free-energy table); the runner fixture's is
                                    ;; minimal. Take the scan fixture's judgement and
                                    ;; the runner fixture's belief keys and decision.
                                    (let [judgement (assoc (merge fixture/judgement
                                                                  (:judgement scan-test/input))
                                                           :decision (:decision fixture/judgement))]
                                      {:render-data (assoc scan-test/input :judgement judgement)
                                       :judgement judgement}))]
                       (runner/run-opportunity! opts))
              record (edn/read-string (slurp (:run-record result)))
              ref (:scan-report record)]
          (is (= 1 @calls) "selection uses one generation")
          (is (= (:decision fixture/judgement)
                 (get-in @rendered [:judgement :decision])
                 (get-in result [:checkpoints :selection :judgment :controller-decision])))
          (is (not (contains? record :render-data)))
          (is (not (contains? ref :judgement)))
          (if fail?
            (do (is (= :absent (:status ref)))
                (is (= :render-failed (:reason ref)))
                (is (= "renderer broke" (get-in ref [:error :message]))))
            (do (is (= :present (:status ref)))
                (is (= (.getCanonicalPath root) (.getCanonicalPath (.getParentFile (io/file (:path ref))))))
                (is (= (:sha256 ref) (scan-test/sha (:path ref))))
                (is (.contains (slurp (:path ref) :encoding "UTF-8") "M-sentinel-λ"))))))
      (finally
        (is (= before (scan-test/file-count "data")))
        (doseq [file (reverse (file-seq root))] (io/delete-file file true))))))
