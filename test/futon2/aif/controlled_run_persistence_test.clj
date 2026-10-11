(ns futon2.aif.controlled-run-persistence-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.controlled-report-card :as report-card]
            [futon2.aif.controlled-run-persistence :as sut]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.run-record-io :as run-record-io])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn temp-root []
  (.getCanonicalPath (.toFile (Files/createTempDirectory
                               "controlled-persistence-"
                               (make-array FileAttribute 0)))))

(defn paths [root]
  {:trace-dir (str (io/file root "trace"))
   :checkpoint-dir (str (io/file root "checkpoints"))
   :run-record-dir (str (io/file root "runs"))
   :morning-brief-root (str (io/file root "brief"))})

(def result
  {:outcome :offline-no-selection
   :trace-path "controlled-trace"
   :backtrace {:decision {:selection-law {:posterior {:a 1}}
                          :c {:status :derived}}}})

(def trace-output
  {:belief {:schema :controlled-test-belief}
   :observation {:controlled true}
   :free-energy {:controller-score 0}
   :cascade-problems {:problems [] :refusals []}
   :decision {:status :abstained :refusals []}
   :mode :controlled-sut})

(defn writer-opts [_root]
  {:scan-render-fn (fn [& _] nil)
   :nano-time-fn (constantly 10)
   :run-timing/start-nanos 10})

(defn closed [checkpoint-write]
  {:schema :wm/closed-run-v1 :run-id "controlled-a0"
   :started-at "2026-10-11T00:00:00Z" :result result
   :trace-output trace-output :trace-date "2026-10-11"
   :brief-item {:attempt-id "controlled-a0" :outcome :controlled-sut
                :achievement {:summary "controlled persistence"}}
   :checkpoint-write checkpoint-write})

(defn strip-volatile [record]
  (-> record
      (dissoc :route :registered-run/critical-parameters :observation-labels)
      (update :registered-run/timing dissoc :finished-at)))

(deftest controlled-seam-delegates-to-production-record-builder-and-hydrates
  (let [root (temp-root)
        bundle (paths root)
        checkpoint-write
        (fn [dir _]
          (let [path (io/file dir "selection.edn") value {:type :selection :status :closed}]
            (io/make-parents path) (spit path (pr-str value))
            {:path (.getCanonicalPath path) :value (edn/read-string (slurp path))}))
        persisted (sut/persist-closed-run!
                   (closed checkpoint-write)
                   {:sut-root root :paths bundle :writer-opts (writer-opts root)})
        legacy-dir (str (io/file root "legacy"))
        legacy (runner/persist-run-record!
                (assoc (writer-opts root) :run-record-dir legacy-dir
                       :observation-labels-path (str (io/file root "legacy-labels.edn")))
                "controlled-a0" "2026-10-11T00:00:00Z" result)
        legacy-record (run-record-io/hydrate-run-record (:run-record legacy))]
    (is (= :persisted (:status persisted)))
    (is (= (strip-volatile legacy-record)
           (strip-volatile (get-in persisted [:hydrated :run-record]))))
    (is (= (get-in persisted [:trace :record])
           (get-in persisted [:hydrated :trace])))
    (is (= "controlled-a0" (get-in persisted [:hydrated :morning-brief :attempt-id])))
    (is (= {:status :absent :reason :controlled-checker-authority-unconfigured}
           (:report-card persisted)))
    (is (= (:report-card persisted)
           (get-in persisted [:hydrated :morning-brief :report-card])))
    (is (= {:type :selection :status :closed}
           (get-in persisted [:checkpoint :value])))))

(deftest controlled-paths-refuse-escape-and-production-default
  (let [root (temp-root)
        noop (fn [& _] nil)]
    (testing "one escaped path refuses before a writer runs"
      (let [e (try (sut/persist-closed-run!
                    (closed noop)
                    {:sut-root root :paths (assoc (paths root) :trace-dir "/tmp/escape")
                     :writer-opts (writer-opts root)}) nil
                   (catch clojure.lang.ExceptionInfo e e))]
        (is (= :controlled-path-isolation-failed (:kind (ex-data e))))))
    (testing "a production default refuses even when it is named explicitly"
      (let [e (try (sut/persist-closed-run!
                    (closed noop)
                    {:sut-root "/home/joe/code/futon2/data"
                     :paths (assoc (paths "/home/joe/code/futon2/data/sut")
                                   :run-record-dir runner/default-run-record-dir)
                     :writer-opts (writer-opts root)}) nil
                   (catch clojure.lang.ExceptionInfo e e))]
        (is (contains? #{:controlled-path-isolation-failed
                         :production-default-path-refused}
                       (:kind (ex-data e))))))))

(def checker-descriptor
  {:identity :sut/lean-requirements-reader :version 1 :authority 901
   :implementation-pin "test-only:wm-check-run-facts+RequirementsReader:v1"})

(def all-unverifiable
  (into {} (map #(vector (keyword (str "q" %)) :unverifiable) (range 1 11))))

(defn terminal [request output & [alter]]
  (let [base {:schema :wm/requirements-checker-terminal-v1
              :checker (:checker request)
              :request-digest (:request-digest request)
              :source-digest (:source-digest request)
              :exit 0 :output output
              :output-digest (evidence/value-digest output)}
        value (if alter (alter base) base)]
    (assoc value :terminal-digest (evidence/value-digest value))))

(defn report-config [executor]
  {:checker checker-descriptor :executor executor
   :snapshot {:open-missions #{} :open-excursions #{} :open-tickets #{}
              :patterns #{} :seats #{} :pins {:controlled true}}})

(deftest controlled-report-card-binds-request-source-checker-and-parsed-outcomes
  (let [output (pr-str {:schema :wm/lean-requirements-result-v1
                        :outcomes all-unverifiable})
        good #(terminal % output)
        card (report-card/produce result (report-config good))]
    (is (= :verified (:status card)))
    (is (= all-unverifiable (:outcomes card)))
    (is (= checker-descriptor (:checker card)))
    (doseq [bad [(fn [request]
                   (terminal request output #(assoc % :request-digest "copied")))
                 (fn [request]
                   (terminal request output #(assoc % :source-digest "altered-source")))
                 (fn [request]
                   (terminal request
                             (pr-str {:schema :wm/lean-requirements-result-v1
                                      :outcomes (dissoc all-unverifiable :q10)})))
                 (fn [request]
                   (terminal request output
                             #(assoc-in % [:checker :implementation-pin]
                                        "substituted-checker")))]]
      (is (= :checker-terminal-invalid
             (:reason (report-card/produce result (report-config bad))))))))
