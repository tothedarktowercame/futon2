(ns futon2.aif.clock-store-busy-close-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.terminal-receipt :as terminal])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(def prereg-path
  "/home/joe/code/futon2/holes/labs/M-aif-full-loop-40/cohort.edn")

(def injured-record-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-06-152a1490-23bd-4b2b-a7d3-0c4b86e1ec75.edn")

(def injured-record-sha
  "4ee5e588d10d56973c8adc308c26e67681ce07c48d54bd86b6dd442e0c6aebcf")

(defn- sha256 [text]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes text "UTF-8"))
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn- term [judgment]
  {:judgment judgment :ground {:kind :clock-store-busy-regression}})

(defn- opened-attempt []
  (let [root (.getPath
              (.toFile (Files/createTempDirectory
                        "clock-store-busy-close" (make-array FileAttribute 0))))
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id
                 (cohort/start-attempt!
                  prereg-path root
                  (term {:opportunity-id "clock-store-busy-close"
                         :trigger :wallclock-cron :machine-state {:tick 1}
                         :agent-roster []
                         :code-state {:git-sha "test" :git-dirty? false
                                      :resolved-mode-flags {}
                                      :configuration-digest "test"}
                         :semantic-epoch :epoch-1})))]
    (doseq [checkpoint [:selection :construction :dispatch :build :adjudication]]
      (cohort/append-checkpoint!
       prereg-path root attempt checkpoint
       {:sorry {:kind (keyword (str "test-" (name checkpoint)))}}))
    {:root root :attempt attempt}))

(deftest pinned-injury-maps-to-existing-substrate-outcome-and-keeps-cause
  (let [text (slurp injured-record-path)
        injured (edn/read-string text)
        failure (ex-info "Agency clock store busy; dispatch not created"
                         {:outcome :clock-store-busy
                          :failure-kind :clock/store-busy
                          :dispatch/not-created? true})
        outcome (@#'runner/outcome-from failure)
        {:keys [root attempt]} (opened-attempt)
        closed (cohort/close-attempt!
                prereg-path root attempt
                (term {:outcome outcome :grounded? false :artifact-only? false
                       :duration-ms 1 :resource-use {:agent-turns 0}
                       :failure-kind (:failure-kind (ex-data failure))
                       :failure-stage :reviewer-dispatch}))
        receipt (terminal/terminal-receipt
                 {:run/id "clock-store-busy-repaired"
                  :click/id "wm-click-clock-store-busy"
                  :startedAt "2026-11-06T07:23:42Z"
                  :failure {:kind (:failure-kind (ex-data failure))
                            :stage :reviewer-dispatch
                            :error (.getMessage failure)}})]
    (is (= injured-record-sha (sha256 text)))
    (is (= :invalid-close-outcome
           (get-in injured [:terminal-receipt :failure-kind])))
    (is (= :clock/store-busy (get-in injured [:failure :kind])))
    (is (= :substrate-unavailable outcome))
    (is (= :substrate-unavailable
           (get-in closed [:payload :judgment :outcome])))
    (is (= :clock/store-busy
           (get-in closed [:payload :judgment :failure-kind])))
    (is (= :clock/store-busy (:failure-kind receipt)))))

(deftest unknown-outcome-still-refuses-without-a-catch-all
  (let [failure (ex-info "new outcome" {:outcome :something-new})
        outcome (@#'runner/outcome-from failure)
        {:keys [root attempt]} (opened-attempt)
        data (try
               (cohort/close-attempt!
                prereg-path root attempt
                (term {:outcome outcome :grounded? false :artifact-only? false
                       :duration-ms 1 :resource-use {:agent-turns 0}}))
               nil
               (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :something-new outcome))
    (is (= :invalid-close-outcome (:failure-kind data)))
    (is (= [:unknown-outcome] (:errors data)))))
