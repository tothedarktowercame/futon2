(ns futon2.aif.selection-authoring-coupling-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.mana-gate :as mana]
            [futon2.aif.selection-authoring-coupling :as coupling])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def trace
  {:act-gate-verdicts
   [{:mission "M-learning-loop"
     :verdict :abstain-missing-leg
     :cascade-score -0.461
     :coverage-score-delta nil}
    {:mission "M-canon-fingerprint-store"
     :verdict :abstain-missing-leg
     :cascade-score -0.454
     :coverage-score-delta nil}]})

(deftest decision-fires-when-top-abstain-has-mana-and-no-deposit
  (let [d (coupling/decision {:trace-record trace
                              :deposit-keys #{}
                              :mana-balance 2
                              :in-flight? false
                              :dry-run? false})]
    (is (= :fired (:status d)))
    (is (= "M-learning-loop" (:mission d)))
    (is (= "M-learning-loop" (:mission-key d)))))

(deftest decision-dry-run-would-fire-without-firing
  (let [d (coupling/decision {:trace-record trace
                              :deposit-keys #{}
                              :mana-balance 1
                              :in-flight? false
                              :dry-run? true})]
    (is (= :would-fire (:status d)))
    (is (= "M-learning-loop" (:mission d)))))

(deftest decision-skips-when-top-abstain-already-has-deposit
  (testing "canonical escrow ids normalize to the WM M-id"
    (let [d (coupling/decision {:trace-record trace
                                :deposit-keys #{"M-learning-loop"}
                                :mana-balance 2
                                :in-flight? false
                                :dry-run? false})]
      (is (= :skipped-has-deposit (:status d)))
      (is (= "M-learning-loop" (:mission d))))))

(deftest decision-defers-when-mana-is-zero
  (let [d (coupling/decision {:trace-record trace
                              :deposit-keys #{}
                              :mana-balance 0
                              :in-flight? false
                              :dry-run? false})]
    (is (= :deferred-no-mana (:status d)))
    (is (= 0 (:mana-balance d)))))

(deftest decision-skips-when-authoring-lock-exists
  (let [d (coupling/decision {:trace-record trace
                              :deposit-keys #{}
                              :mana-balance 3
                              :in-flight? true
                              :dry-run? false})]
    (is (= :skipped-in-flight (:status d)))
    (is (= "authoring lock already exists" (:reason d)))))

(deftest deposited-mission-keys-normalizes-canonical-deposit-missions
  (is (= #{"M-learning-loop" "M-first-flights"}
         (coupling/deposited-mission-keys
          [{:mission "futon5a-d/mission/learning-loop"}
           {:mission "futon3c-d/mission/first-flights"}]))))

(defn- tmp-dir []
  (.toFile (Files/createTempDirectory "selection-authoring-test"
                                      (make-array FileAttribute 0))))

(defn- write-trace! [dir record]
  (let [f (io/file dir "wm-trace-2099-01-01.edn")]
    (spit f (str (pr-str record) "\n"))
    (.getPath f)))

(defn- fake-author-script! [dir]
  (let [f (io/file dir "fake-author.sh")
        calls (io/file dir "calls.edn")]
    (spit f (str "#!/usr/bin/env bash\n"
                 "echo \"$@\" >> " (.getPath calls) "\n"
                 "echo fake-author-ok\n"))
    (.setExecutable f true)
    [(.getPath f) (.getPath calls)]))

(deftest run-once-fires-once-then-skips-by-lock
  (let [root (tmp-dir)
        trace-dir (io/file root "trace")
        deposit-dir (io/file root "deposits")
        mana-dir (io/file root "mana")
        lock-dir (io/file root "locks")
        log-file (io/file root "coupling.log")
        _ (.mkdirs trace-dir)
        _ (.mkdirs deposit-dir)
        _ (write-trace! trace-dir trace)
        [author-script calls] (fake-author-script! root)]
    (mana/award! coupling/fold-authoring-gate 1 "test award" (.getPath mana-dir))
    (let [first-run (coupling/run-once! {:trace-dir (.getPath trace-dir)
                                         :deposit-dir (.getPath deposit-dir)
                                         :mana-dir (.getPath mana-dir)
                                         :lock-dir (.getPath lock-dir)
                                         :log-file (.getPath log-file)
                                         :author-script author-script
                                         :reviewer "claude-4"})]
      (is (= :fired (:status first-run)))
      (is (= 0 (:invoke-exit first-run)))
      (is (= ["M-learning-loop --from claude-4"]
             (str/split-lines (slurp calls)))))
    (let [second-run (coupling/run-once! {:trace-dir (.getPath trace-dir)
                                          :deposit-dir (.getPath deposit-dir)
                                          :mana-dir (.getPath mana-dir)
                                          :lock-dir (.getPath lock-dir)
                                          :log-file (.getPath log-file)
                                          :author-script author-script
                                          :reviewer "claude-4"})]
      (is (= :skipped-in-flight (:status second-run)))
      (is (= ["M-learning-loop --from claude-4"]
             (str/split-lines (slurp calls))))
      (is (re-find #"fired" (slurp log-file)))
      (is (re-find #"skipped-in-flight" (slurp log-file))))))
