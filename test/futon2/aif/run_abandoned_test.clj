(ns futon2.aif.run-abandoned-test
  "A pending learning step whose run was started and never closed can still
  be in flight, or can be provably dead: the serving JVM restarted after the
  run started, or a later run has since started and closed in the same run
  stream. The dead case is typed :run-abandoned and discharges with the same
  backup + receipt discipline as the other unrecoverable kinds. Elapsed time
  alone is never evidence (clicks frozen on run d6886778 since 2026-11-06
  21:07; the JVM restarted at 21:09:29 and killed that click)."
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [futon2.aif.outer-state :as state]
            [futon2.aif.outer-pending-observation :as pending]
            [futon2.aif.makeup-pin-migration-test :as migration]))

(def run-id "2026-11-06-d6886778-4db3-4ce1-b036-ffe475c85b02")
;; Production shape, copied from data/wm-runs/run-stream.edn line 363.
(def started-event {:event :started :run/id run-id
                    :startedAt "2026-11-06T21:07:12.360123542Z"})
(def later-run-id "2026-11-07-ebafc353-4705-4166-bc0a-df99c6f5910d")
(def later-started-event {:event :started :run/id later-run-id
                          :startedAt "2026-11-07T04:35:18.628007606Z"})
(def later-closed-event
  {:event :closed :run/id later-run-id
   :record-file "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-07-ebafc353-4705-4166-bc0a-df99c6f5910d.edn"
   :q6 {:schema :wm/q6-terminal-v1 :run/id later-run-id
        :startedAt "2026-11-07T04:35:18.628007606Z"
        :choice {:status :absent :reason :chose-nothing}
        :outcome {:status :present :outcome :changed :raw :grounded-progress}
        :input-digest nil :sha256 "666f836d013148138562f003d43aaaec27e83f39a1c9229cb77e99bf593981ec"}})

(defn- stream-dir [& events]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "run-abandoned-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (spit (io/file dir "run-stream.edn")
          (str/join "\n" (map pr-str events)))
    dir))

(defn- planted-state [run-id]
  (let [{:keys [old path before]} (migration/fixture)
        planted (assoc (dissoc before :source)
                       :pending {:run-id run-id :giraffe/run-id "giraffe-legacy"
                                 :selection {:cascade-id :what-others-wait-on-first
                                             :cascade-index 0
                                             :selection {:selected {:target "M-legacy"}}
                                             :steps [:ready-gate :by-rank]}})]
    (state/write-state! path planted)
    {:old old :path path :bytes (slurp path) :planted planted}))

(defn- receipt-files [path]
  (filter #(re-find #"pending-discharge" (.getName %))
          (.listFiles (.getParentFile (io/file path)))))

(deftest superseded-run-is-typed-run-abandoned-and-discharges
  (let [dir (stream-dir started-event later-started-event later-closed-event)
        ending (pending/observe dir run-id)]
    (is (= :deferred (:status ending)))
    (is (= :run-abandoned (:reason ending)))
    (is (= run-id (:run-id ending)) "the defer map keeps the run id")
    (is (= :superseded-by-later-closed-run
           (get-in ending [:abandonment-evidence :kind])))
    (is (= later-run-id (get-in ending [:abandonment-evidence :later-run-id])))
    (let [{:keys [old path bytes planted]} (planted-state run-id)
          result (state/discharge-unrecoverable-pending!
                  path old :run-abandoned ending)
          after (state/read-state old path)
          receipt (edn/read-string (slurp (:receipt-path result)))]
      (is (= :discharged (:status result)))
      (is (nil? (:pending after)))
      (is (= (dissoc planted :pending) (dissoc after :source))
          "learned counts untouched")
      (is (= bytes (slurp (:backup result))) "exact old bytes retained")
      (is (= :run-abandoned (get-in receipt [:refusal :kind])))
      (is (= (:abandonment-evidence ending)
             (get-in receipt [:refusal :abandonment-evidence])))
      (is (= :durable (:source after)) "read-state accepts the receipt"))))

(deftest jvm-restart-after-start-is-run-abandoned
  ;; The run started 2026-11-06 21:07; this test JVM started later, so the
  ;; process that fired the run is gone. No later run is needed.
  (let [dir (stream-dir started-event)
        ending (pending/observe dir run-id)]
    (is (= :run-abandoned (:reason ending)))
    (is (= :jvm-restart (get-in ending [:abandonment-evidence :kind])))
    (is (= run-id (:run-id ending)))
    (let [{:keys [old path bytes]} (planted-state run-id)
          result (state/discharge-unrecoverable-pending!
                  path old :run-abandoned ending)
          after (state/read-state old path)
          receipt (edn/read-string (slurp (:receipt-path result)))]
      (is (= :discharged (:status result)))
      (is (nil? (:pending after)))
      (is (= bytes (slurp (:backup result))))
      (is (= :jvm-restart (get-in receipt [:refusal :abandonment-evidence :kind]))))))

(deftest started-not-closed-without-evidence-stays-in-flight
  ;; started after this JVM started (a run this JVM fired), nothing else in
  ;; the stream: no evidence it cannot still be running.
  (let [now-run "2026-11-07-live-run-00000000000000000000000000"
        dir (stream-dir {:event :started :run/id now-run
                         :startedAt (str (java.time.Instant/now))})
        ending (pending/observe dir now-run)]
    (is (= :run-in-flight (:reason ending)))
    (is (= now-run (:run-id ending)) "the defer map keeps the run id")
    (let [{:keys [old path bytes]} (planted-state now-run)
          failure (try
                    (state/discharge-unrecoverable-pending!
                     path old :run-abandoned ending)
                    nil
                    (catch Throwable e e))]
      (is (= :pending-step-recoverable (:kind (ex-data failure))))
      (is (= bytes (slurp path)) "nothing written")
      (is (empty? (receipt-files path))))))

(deftest receipt-without-abandonment-evidence-refuses-the-read
  (let [dir (stream-dir started-event)
        ending (pending/observe dir run-id)
        {:keys [old path]} (planted-state run-id)
        result (state/discharge-unrecoverable-pending! path old :run-abandoned ending)
        receipt-path (:receipt-path result)
        receipt (edn/read-string (slurp receipt-path))]
    (is (= :durable (:source (state/read-state old path))))
    (spit receipt-path
          (pr-str (update-in receipt [:refusal] dissoc :abandonment-evidence)))
    (is (= :invalid-discharge-receipt
           (:kind (ex-data (try (state/read-state old path)
                                (catch clojure.lang.ExceptionInfo e e))))))))
