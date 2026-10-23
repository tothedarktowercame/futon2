(ns futon2.aif.flight-driver-budget-preflight-test
  "R1(a): a flight refuses before its first click when its whole requested
  click count is not covered by a readable ordinary-click availability."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [cheshire.core :as json]
            [futon2.aif.flight-driver :as driver]
            [futon2.aif.flight-runner :as fr]
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

;; GET /api/alpha/wm/click, :ordinary-click-budget, captured 2026-10-23T14:3xZ
;; from the serving JVM (other response keys dropped; values verbatim).
(def live-click-get-body
  "{\"ordinary-click-budget\":{\"schema\":\"wm/ordinary-click-availability-v1\",\"authorization\":{\"path\":\"futon2/holes/labs/wm-contract/AUTH-ordinary-click-budget-renewal-7-2026-09-24.md\",\"sha\":\"326826ac\"},\"allocated\":168,\"consumed\":167,\"available\":1,\"unit\":\"ordinary-click\",\"ledger-source\":{\"path\":\"/home/joe/code/futon2/data/wm-ordinary-clicks/consumption.jsonl\",\"sha256\":\"57b2be80e74c2ff7fe29724e4d214948be41d600c04fe772590b3cc97485e69a\"}}}")

(defn- live-availability-fn []
  (fr/http-click-availability-fn
   {:get! (fn [] {:status 200
                  :body (json/parse-string live-click-get-body true)})}))

(deftest live-route-shape-is-readable
  (testing "the live JSON budget reads as available 1, not unreadable"
    (let [{:keys [calls result]} (run 2 (live-availability-fn))]
      (is (zero? calls))
      (is (= :click-budget-insufficient (:reason result)))
      (is (= 1 (:available result)))))
  (testing "the live JSON budget admits a one-click flight"
    (let [{:keys [calls result]} (run 1 (live-availability-fn))]
      (is (pos? calls))
      (is (not= :refused (:status result))))))
