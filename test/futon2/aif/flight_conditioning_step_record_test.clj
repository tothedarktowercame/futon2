(ns futon2.aif.flight-conditioning-step-record-test
  "Real ten-subject label store -> decision -> persisted tick -> conditioning.
  Negative controls change the persisted record before the step reads it."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.report.observation-labels-consume-test :as fixture])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest real-persisted-declaration-produces-a-conditioning-step
  (let [root (.toFile (Files/createTempDirectory "conditioning-record-" (make-array FileAttribute 0)))]
    (try
      (binding [fixture/*dir* root]
        (#'fixture/fill! 5)
        (let [decision (:decision (#'fixture/decision {:observation-labels-path (#'fixture/path)}))
              saved (#'runner/persist-run-record!
                     {:run-record-dir (str (io/file root "records"))}
                     "offline-conditioning" "2026-09-26T00:00:00Z"
                     {:outcome :offline-no-selection
                      :checkpoints {:selection {:judgment {:controller-decision decision}}}})
              file (:run-record saved)
              record (edn/read-string (slurp file))
              chosen (get-in record [:decision :chosen])
              target (:target chosen) precedence (:precedence chosen)
              rates (get-in record [:decision :measured-a :rates])
              local-rates (into {} (for [[[t token] value] rates :when (= t target)] [token value]))
              checked (set (keys local-rates))
              run-step (fn [change]
                         (spit file (pr-str (change record)))
                         (flight/conditioning-step
                          {:run-record (edn/read-string (slurp file))
                           :target target :flight-id "offline" :click-id "offline-conditioning"
                           :policy-key [target precedence {}] :precedence precedence :enactments []
                           :observation {:status :observed :o checked :checked checked
                                         :channel (zipmap checked (repeat :C3))}}))
              step (run-step identity)]
          (testing "real rates and measurement survive the real persistence boundary"
            (is (= (:measured-a decision) (get-in record [:decision :measured-a])))
            (is (seq local-rates))
            (is (every? #(= {:false-neg 1/12 :false-pos 1/12} %) (vals local-rates)))
            (is (every? #(= {:false-neg {:numerator 0 :denominator 5}
                            :false-pos {:numerator 0 :denominator 5}} %)
                        (vals (get-in record [:decision :measured-a :measurement])))))
          (testing "the step accepts declarations that the tick actually wrote"
            (is (= :present (:status step)) (pr-str step))
            (is (= local-rates (get-in step [:measured-a :rates])))
            (is (= precedence (get-in step [:b :precedence])))
            (is (number? (:f step)))
            (is (pos? (:p-o step)))
            (println "conditioning record regression" (select-keys step [:p-o :f])))
          (testing "removing measured-A from disk is a typed absence"
            (is (= :no-measured-a
                   (:reason (run-step #(update % :decision dissoc :measured-a))))))
          (testing "malformed stored pattern refuses before rollout"
            (let [id (first precedence)
                  inputs (get-in record [:decision :selection-certificate :token-belief-stage :domain-inputs])
                  i (first (keep-indexed (fn [n x] (when (= target (:target x)) n)) inputs))
                  path [:decision :selection-certificate :token-belief-stage :domain-inputs i
                        :declaration :interpretations id :produces]
                  bad (run-step #(update-in % path vec))]
              (is (= :no-interpretation (:reason bad)))
              (is (= [id] (get-in bad [:inputs :patterns])))
              (is (= :missing-pattern-interpretation (get-in bad [:inputs :refusals 0 :kind])))))
          (testing "different rates on disk reach the step"
            (let [replacement (zipmap (keys rates) (repeat {:false-neg 1/3 :false-pos 1/4}))
                  changed (run-step #(assoc-in % [:decision :measured-a :rates] replacement))]
              (is (= :present (:status changed)))
              (is (= (zipmap checked (repeat {:false-neg 1/3 :false-pos 1/4}))
                     (get-in changed [:measured-a :rates])))
              (is (not= (:p-o step) (:p-o changed)))))))
      (finally (doseq [f (reverse (file-seq root))] (io/delete-file f))))))
