(ns futon2.report.scan-report-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.calibration-cycle :as calibration]
            [futon2.report.selection-nil-receipt-test :as generation]
            [futon2.aif.scan-report :as scan]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(def ^:dynamic *root* nil)
(defn file-count [dir] (count (filter #(.isFile %) (file-seq (io/file dir)))))
(use-fixtures :each
  (fn [f]
    (let [before (file-count "data")
          root (.toFile (Files/createTempDirectory "scan-report-" (make-array FileAttribute 0)))]
      (try (binding [*root* root] (f))
           (finally
             (is (= before (file-count "data")) "no writes to real data stores")
             (doseq [file (reverse (file-seq root))] (io/delete-file file true)))))))

(def input
  {:now "2026-09-21" :days 14
   :judgement {:mode :multiplied
               :active-mission {:endpoint "M-sentinel-λ" :mission-id "M-sentinel-λ"
                                :clocked-at-ms 0 :witness-rule :fixture}
               :free-energy {:controller-score 0.05 :preference-gap-score 0.05
                             :coverage-uncertainty-pressure 0.1}
               :decision {:status :abstained
                          :refusals [{:target "M-sentinel-λ" :kind :beta-not-declared
                                      :missing :beta-by-context}]}}})
(defn sha [path]
  (format "%064x" (java.math.BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256")
                                                  (Files/readAllBytes (.toPath (io/file path)))))))

(deftest retains-supplied-judgement-and-digest
  (let [calls (atom [])
        render (fn [data] (swap! calls conj data) (wm/render-war-machine data))
        path (io/file *root* "tick-run-record-fixture.edn")
        ref (scan/retain! path input render)]
    (is (= :present (:status ref)))
    (is (= [input] @calls))
    (is (= "tick-run-record-fixture.scan.md" (.getName (io/file (:path ref)))))
    (is (.contains (slurp (:path ref) :encoding "UTF-8") "M-sentinel-λ"))
    (is (= (:sha256 ref) (sha (:path ref))))
    (is (= ref (scan/retain! path input render)))))

(deftest failures-are-typed-absences
  (let [path (io/file *root* "fixture.edn")]
    (is (= {:status :absent :reason :render-failed
            :error {:class "java.lang.IllegalStateException" :message "renderer broke"}}
           (scan/retain! path input (fn [_] (throw (IllegalStateException. "renderer broke"))))))
    (is (= {:status :absent :reason :scan-input-unavailable}
           (scan/retain! path nil (fn [_] (throw (Exception. "must not render"))))))
    (is (= :rendered-text-empty (:reason (scan/retain! path input (constantly "")))))
    (is (= 0 (file-count *root*)))
    (spit (io/file *root* "not-a-directory") "occupied")
    (is (= :write-failed
           (:reason (scan/retain! (io/file *root* "not-a-directory" "fixture.edn") input (constantly "text")))))))

(deftest run-record-retains-reference-and-survives-render-failure
  (doseq [[id renderer status] [["success" wm/render-war-machine :present]
                               ["failure" (fn [_] (throw (IllegalStateException. "renderer broke"))) :absent]]]
    (let [saved (#'runner/persist-run-record!
                 {:run-record-dir (.getPath *root*) :scan-report/state (atom input)
                  :scan-render-fn renderer}
                 id "2026-09-21T00:00:00Z"
                 {:outcome :incomplete :data {:failure-stage :construction}
                  :checkpoints {:selection {:judgment {:controller-decision (:decision (:judgement input))}}}})
          record (edn/read-string (slurp (:run-record saved)))
          ref (:scan-report record)]
      (is (= :present (:run-record-status saved)))
      (is (= status (:status ref)))
      (when (= :present (:status ref))
        (is (= (sha (:path ref)) (:sha256 ref)))
        (is (.contains (slurp (:path ref) :encoding "UTF-8") "M-sentinel-λ")))
      (when (= :absent (:status ref))
        (is (= :render-failed (:reason ref)))
        (is (= "renderer broke" (get-in ref [:error :message])))))))

(deftest generation-defers-only-when-requested
  (let [calls (atom 0)
        stubs (into {} (for [sym generation/scan-vars]
                         [(ns-resolve 'futon2.report.war-machine sym) (constantly nil)]))]
    (with-redefs-fn
      (merge stubs
             {#'wm/scan-r12-apparatus (constantly {:available? true :class-count 0})
              #'calibration/admit-apparatus! (fn [& _] {:ok true})
              #'wm/judge (fn [& _] (swap! calls inc) (:judgement input))
              #'wm/render-war-machine (fn [_] (throw (IllegalStateException. "renderer broke")))})
      (fn []
        (let [generated (wm/generate-war-machine 14 {:defer-render? true})]
          (is (= 1 @calls))
          (is (= (:judgement generated) (get-in generated [:render-data :judgement])))
          (is (not (contains? generated :markdown)))
          (is (= :render-failed
                 (:reason (scan/retain! (io/file *root* "deferred.edn")
                                        (:render-data generated) wm/render-war-machine)))))
        ;; Existing callers still render by default; exceptions still propagate there.
        (is (thrown-with-msg? IllegalStateException #"renderer broke"
                             (wm/generate-war-machine 14)))))))
