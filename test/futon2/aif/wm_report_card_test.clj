(ns futon2.aif.wm-report-card-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [wm-report-card :as card]))

(def snap {:open-missions #{"M-x"} :open-excursions #{} :open-tickets #{}
           :patterns #{"p1" "p2"} :seats #{"author"} :pins {:test true}})

(def full-record
  {:run/id "fixture-full" :startedAt "2026-10-09T00:00:00Z"
   :world-at-selection {:open-tasks {:missions {:ids ["M-x"]}
                                     :excursions {:ids []} :tickets {:ids []}}
                        :enumerated-tasks {:ids ["M-x"]} :failures []
                        :selection-ended-at "2026-10-09T00:00:01Z"
                        :selection-input-digest "d" :seat-roster {:codex {:ids ["author"]}}}
   :participants {:roles {:author {:status :present :identity "author"}}}
   :registered-run/timing {:wall-clock-ms 12 :phase-timings-ms {:selection 5}
                           :debugger-stopped-ms 0 :debugger-dwell-receipts []}
   :registered-run/model-usage {:jobs [{:job-id "j" :role {:agent "author"}
                                        :total-tokens 9 :phase :author-dispatch}]}
   :decision {:chosen {:target "M-x" :id :c1 :precedence [:p1 :p2]}
              :selection-certificate
              {:candidates [{:id :c1 :target "M-x" :g 1.0 :g-terms {:risk 1 :ambiguity 1
                                                                     :expected-information-gain 1}
                             :precedence [:p1 :p2]
                             :observation-locators {["M-x" :done]
                                                    {:repo "futon2" :path "holes/M-x.md"}}}]
               :policies [{:id :c1}] :target-construction
               [{:target "M-x" :slice [:p1 :p2] :pool [:p1 :p2]
                 :slice-from-whole-library true :policy-count 2}]
               :library-pin {:size 2}}}
   :d-task-enactment {:ask "Build the recorded target."}
   :grounded-commit "abc1234"
   :run-output {:status :present :summary "Built it" :things-to-try ["run demo"]}
   :terminal-receipt {:outcome :changed :id "terminal"}})

(deftest full-card-has-five-ordered-sections-and-three-artifacts
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-report-card" (make-array java.nio.file.attribute.FileAttribute 0)))
        record (io/file root "tick-run-record-fixture-full.edn")
        out (io/file root "out")]
    (spit record (pr-str full-record))
    (let [paths (card/generate! (.getPath record) {:output-dir (.getPath out) :snap snap})
          result (edn/read-string (slurp (:edn paths)))]
      (is (= [:verdict :run :cascade :outcome :example-text]
             (mapv :id (:sections result))))
      (is (every? #(.isFile (io/file %)) (vals paths)))
      (is (= 10 (count (get-in result [:sections 0 :rows]))))
      (is (= "M-x" (get-in result [:sections 2 :data :chosen-target])))
      (is (re-find #"Build the recorded target" (slurp (:html paths)))))))

(deftest failed-at-close-still-renders-visible-typed-absences
  (let [r {:run/id "failed" :startedAt "now"
           :failure {:kind :required-checkpoints-missing}
           :terminal-receipt {:kind :failure :failure-kind :required-checkpoints-missing}}
        result (card/build-card r "fixture.edn" snap)]
    (is (= :not-recomputable (get-in result [:sections 0 :rows 0 :status])))
    (is (= :absent (get-in result [:sections 2 :data :chosen-target :status])))
    (is (= :absent (get-in result [:sections 3 :data :run-output :status])))
    (is (re-find #":status :absent" (card/markdown result)))))

(deftest public-html-removes-host-paths-and-secret-url-components
  (let [r (-> full-record
              (assoc :run/id "public")
              (assoc-in [:run-output :summary]
                        "/home/joe/code/futon2/x https://me:password@example.test/x?token=secret"))
        html (card/html (card/build-card r "/home/joe/code/futon2/data/run.edn" snap))]
    (is (not (re-find #"/home/joe" html)))
    (is (not (re-find #"password|token=secret" html)))
    (is (re-find #"futon2/data/run.edn" html))))
