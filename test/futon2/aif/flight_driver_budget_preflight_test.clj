(ns futon2.aif.flight-driver-budget-preflight-test
  "R1(a): a flight refuses before its first click when its whole requested
  click count is not covered by a readable ordinary-click availability."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight-driver :as driver]
            [futon2.aif.observation-checks :as checks])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def mission-text
  (slurp "test/fixtures/mission-criteria/M-futon-seams@futon3c-20959e4f.md"))

(def lifecycle-text
  (slurp "test/fixtures/mission-criteria/M-futon-seams-lifecycle@futon3c-d74a7c5a.edn"))

(defn- temp-dir []
  (.getCanonicalPath
   (.toFile (Files/createTempDirectory "flight-budget-preflight"
                                       (make-array FileAttribute 0)))))

(defn- availability [n]
  {:schema :wm/ordinary-click-availability-v1
   :unit :ordinary-click
   :allocated 10
   :consumed (- 10 n)
   :available n
   :ledger-source {:path "/fixture/consumption.jsonl" :sha256 "fixture"}})

(defn- run [max-clicks available]
  (let [calls (atom 0)
        root (temp-dir)
        opts {:target "M-futon-seams"
              :seat "fixture"
              :repo "futon3c"
              :path "holes/missions/M-futon-seams.md"
              :lifecycle-path "holes/labs/M-futon-seams/lifecycle.edn"
              :store root
              :run-record-dir root
              :id "flight-budget-preflight"
              :max-clicks max-clicks
              :read-text (fn [_ _ path]
                           (if (str/ends-with? path ".edn")
                             lifecycle-text
                             mission-text))
              :observe #(checks/decl-present? mission-text (:decl %))
              :sources {:beta-by-context {:WM {:beta 1}}}
              :answer-fn (fn [_]
                           {:seat "fixture" :job-id "none" :state "failed"})
              :availability-fn (if (fn? available)
                                 available
                                 (constantly available))
              :click-fn (fn [_]
                          (let [n (swap! calls inc)]
                            {:click-id (str "fixture-click-" n)
                             :unreached-wants []
                             :abstention {:kind :fixture-no-progress}}))}
        result (driver/run-flight! opts {:flight-id "flight-budget-preflight"})]
    {:calls @calls :result result :root root}))

(deftest whole-flight-click-budget-is-preflighted
  (testing "available 1 cannot admit a two-click flight and posts no click"
    (let [{:keys [calls result]} (run 2 (availability 1))]
      (is (zero? calls))
      (is (= :refused (:status result)))
      (is (= :click-budget-insufficient (:reason result)))
      (is (= 2 (:requested-clicks result)))
      (is (= 1 (:available result)))
      (is (empty? (:clicks result)))))

  (testing "available 2 admits a two-click flight and preserves flight behaviour"
    (let [{:keys [calls result root]} (run 2 (availability 2))]
      (is (pos? calls))
      (is (not= :refused (:status result)))
      (is (string? (:record-path result)))
      (is (.isFile (io/file (:record-path result))))
      (is (str/starts-with? (:record-path result) root))))

  (testing "unreadable availability is a refusal, never assumed sufficient"
    (let [{:keys [calls result]}
          (run 2 (fn [] (throw (ex-info "budget endpoint unreadable" {}))))]
      (is (zero? calls))
      (is (= :refused (:status result)))
      (is (= :click-budget-unreadable (:reason result)))
      (is (= :click-budget-read-failed
             (get-in result [:availability :reason]))))))
