(ns futon2.aif.wm.cascade-decision-isolation-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]))

(deftest cascade-decision-does-not-load-the-report
  (let [form (pr-str '(do
                        (require 'futon2.aif.wm.cascade-decision)
                        (assert (nil? (find-ns 'futon2.report.war-machine)))
                        (println :isolated)))
        {:keys [exit out err]}
        (shell/sh "timeout" "90s"
                  (str (io/file (System/getProperty "java.home") "bin" "java"))
                  "-cp" (System/getProperty "java.class.path")
                  "clojure.main" "-e" form)]
    (is (= 0 exit) (str out err))))
