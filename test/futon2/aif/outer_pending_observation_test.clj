(ns futon2.aif.outer-pending-observation-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-pending-observation :as pending]
            [futon2.aif.outer-refusal-index :as durable]))

(def run-id "2026-10-29-f74df9cc-c27b-420b-91ec-7a32db708c41")

(defn- copy-fixture []
  (let [source (.getParentFile (io/file (io/resource "fixtures/outer-consumption/README.md")))
        root (.toFile (java.nio.file.Files/createTempDirectory
                       "outer-pending-observation-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))]
    (doseq [file (file-seq source) :when (.isFile file)]
      (let [target (io/file root (str (.relativize (.toPath source) (.toPath file))))]
        (io/make-parents target)
        (io/copy file target)))
    root))

(deftest retained-unknown-grounded-ending-is-derived-on-read
  (let [root (copy-fixture)
        file (io/file root (str "tick-run-record-" run-id ".edn"))
        index (io/file root "reading-policy-class-history-index.ednl")
        target "M-selected"
        action-value {:target target}
        occurrence {:schema :wm/action-transition-occurrence-v2
                    :run/id run-id :cohort/id "cohort" :attempt/id "attempt-001"
                    :transition/id "transition-00000000-0000-0000-0000-000000000001"
                    :action/id "action-00000000-0000-0000-0000-000000000002"
                    :action/value action-value
                    :action/value-sha256 (identity/digest action-value)
                    :action-at "2026-09-21T18:00:00Z"}
        close {:outcome :grounded-progress :grounded? true
               :grounded-commit {:repo "futon2" :sha "abc"}
               :artifact-only? false :occurrence occurrence}
        record (-> (edn/read-string (slurp file))
                   (assoc :run-ending-classification
                          {:schema :wm/run-ending-classification-receipt-v1
                           :status :recorded :class :unknown
                           :missing [:attested-increment]
                           :close-projection close})
                   (assoc :outer/selection
                          {:selection {:selected {:target target}}
                           :selected-relation :useful-elsewhere}))]
    (spit file (pr-str record))
    (spit index
          (str (pr-str (assoc (edn/read-string (slurp index))
                              :record-sha256 (durable/sha256-file file))) "\n"))
    (let [ending (pending/observe root run-id)]
      (is (= :ready (:status ending)) (pr-str ending))
      (is (= :unrelated (:ending-class ending)))
      (is (= :derived-on-read
             (get-in ending [:classification-provenance :method])))
      (is (= :unknown
             (get-in ending [:classification-provenance :stored-class])))
      (is (= (durable/sha256-file file)
             (get-in ending [:classification-provenance :record-pin :sha256]))))))
