(ns ants.clj-ants-aif-test
  (:require [clojure.test :refer [deftest is]]
            [ants.clj-ants-aif :as entry]))

(deftest run!-returns-nil
  ;; The production one-shot entrypoint shuts down Clojure's global executors
  ;; so its JVM can exit promptly. A suite-hosted call must not poison futures
  ;; and clojure.java.shell for every namespace that follows it.
  (with-redefs [clojure.core/shutdown-agents (fn [])]
    (is (nil? (entry/run! 0)))))
