(ns futon2.aif.pattern-outcome-retention-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.pattern-observation :as pattern-observation]
            [futon2.aif.token-outcome :as token-outcome]
            [futon2.aif.token-outcome-test :as token-fixture])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def fixture
  (edn/read-string
   (slurp "test/resources/wm/retained-25e24-pattern-fold-projection.edn")))
(def after (:after fixture))
(def before "68120423443eab3243c400563cc9c66f45fea6e7")
(def source {:record-path "003-construction.edn"
             :record-sha256 (apply str (repeat 64 "a"))})

(defn supplied []
  (pattern-observation/supply
   {:prediction (:prediction fixture)
    :fold-conditions (:fold-conditions fixture)
    :revision {:repo "futon5a" :sha after}
    :source source}))

(deftest compares-supplied-attempt-observations
  (let [supplied (supplied)
        comparison (token-outcome/compare-supplied-outcomes
                    (:prediction fixture) supplied after)]
    (is (= :compared (:status comparison)))
    (is (= 2 (count (:tokens comparison))))
    (is (every? #(= [:observation-missing
                     :completion-check-not-executable
                     {:status :missing :kind :completion-check-not-executable}]
                    ((juxt :verdict :reason :observed) %))
                (:tokens comparison)))
    (is (= (:rows supplied) (mapv :measurement (:tokens comparison))))
    (is (not-any? #(and (vector? (:token %))
                        (keyword? (second (:token %)))
                        (.startsWith (name (second (:token %))) "hole/"))
                  (:tokens comparison)))))

(deftest supplied-revision-must-match-comparison-revision
  (let [comparison (token-outcome/compare-supplied-outcomes
                    (:prediction fixture) (supplied) before)]
    (is (every? #(= [:observation-missing :artifact-revision-mismatch]
                    ((juxt :verdict :reason) %))
                (:tokens comparison)))))

(deftest boolean-supplied-row-produces-a-verdict
  (let [s (supplied)
        zero ["gauntlet/world-is-hypergraph" 0]
        s (update s :rows
                  (fn [rows]
                    (mapv #(if (= zero (:occurrence %))
                             (assoc % :observed true)
                             %)
                          rows)))
        comparison (token-outcome/compare-supplied-outcomes
                    (:prediction fixture) s after)]
    (is (= :predicted-and-observed
           (:verdict (first (:tokens comparison)))))
    (is (= :observation-missing
           (:verdict (second (:tokens comparison)))))))

(deftest mission-prediction-is-refused-in-pattern-domain
  (let [prediction {:schema :wm/token-outcome-prediction-v1 :status :frozen
                    :target "M-war-machine-wiring"
                    :wanted [{:token ["M-war-machine-wiring" :hole/h3b6587f68a09]
                              :predicted 1}]}
        supplied (pattern-observation/supply
                  {:prediction prediction :fold-conditions []
                   :revision {:repo "futon5a" :sha after} :source source})
        comparison (token-outcome/compare-supplied-outcomes prediction supplied after)]
    (is (= :not-applicable (:status supplied)))
    (is (= [:refused :prediction-not-pattern-domain]
           ((juxt :status :reason) comparison)))
    (is (= supplied (:supplied comparison)))))

(defn retain! [root attempt prediction d-result sha context]
  (#'runner/retain-token-outcome!
   root :cohort attempt prediction d-result sha context))

(deftest retention-carries-pattern-domain-beside-unchanged-learning
  (token-fixture/with-artifact
   (fn [sha]
     (let [root (.toFile (Files/createTempDirectory
                          "pattern-comparison-retention-"
                          (make-array FileAttribute 0)))
           record (io/file root "d-task.edn")]
       (try
         (spit record (pr-str {:after-token-evidence
                               (token-fixture/measurements sha)}))
         (let [d-result {:source {:path (.getPath record)
                                  :sha256 (evidence/sha256 (slurp record))}}
               base-context {:ledger-root (io/file root "ledger")}
               baseline (retain! root "baseline" (:prediction fixture)
                                 d-result sha base-context)
               retained (retain! root "with-pattern" (:prediction fixture)
                                 d-result sha
                                 (assoc base-context
                                        :fold-conditions (:fold-conditions fixture)
                                        :repo "futon5a"
                                        :construction-source source))
               disk (edn/read-string (slurp (get-in retained [:entry :source-path])))]
           (is (= :supplied (get-in disk [:pattern-observations :status])))
           (is (= 2 (count (get-in disk [:pattern-observations :rows]))))
           (is (= :compared (get-in disk [:pattern-comparison :status])))
           (is (every? #(contains? disk %)
                       [:tokens :token-outcome-pairs :learning-trial-receipt]))
           (is (= (get-in baseline [:receipt :learning-trial-receipt])
                  (get-in retained [:receipt :learning-trial-receipt]))))
         (finally
           (doseq [file (reverse (file-seq root))] (.delete file))))))))

(deftest retention-types-an-absent-fold
  (token-fixture/with-artifact
   (fn [sha]
     (let [root (.toFile (Files/createTempDirectory
                          "pattern-fold-absent-"
                          (make-array FileAttribute 0)))
           record (io/file root "d-task.edn")]
       (try
         (spit record (pr-str {:after-token-evidence
                               (token-fixture/measurements sha)}))
         (let [d-result {:source {:path (.getPath record)
                                  :sha256 (evidence/sha256 (slurp record))}}
               retained (retain! root "attempt" (:prediction fixture)
                                 d-result sha {:ledger-root (io/file root "ledger")})
               disk (edn/read-string (slurp (get-in retained [:entry :source-path])))]
           (is (= {:status :absent :reason :fold-conditions-not-supplied}
                  (:pattern-observations disk)))
           (is (= [:refused :fold-conditions-not-supplied]
                  ((juxt :status :reason) (:pattern-comparison disk)))))
         (finally
           (doseq [file (reverse (file-seq root))] (.delete file))))))))
