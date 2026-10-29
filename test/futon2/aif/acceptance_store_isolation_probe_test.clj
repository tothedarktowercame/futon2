(ns futon2.aif.acceptance-store-isolation-probe-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(deftest strategic-selection-uses-process-store-root
  (let [root (System/getenv "FUTON_WM_PHASE_REPLAY_INPUT_ROOT")]
    (is (some? root) "this probe may run only through the isolated acceptance boundary")
    (when root
      (let [selection (runner/strategic-selection!
                       {:strategic-selection-invoke-fn
                        (fn [_]
                          {:ok true
                           :selection {:status :verified-live-selection
                                       :selected-mission-ids ["M-isolated"]}})}
                       {:scheduler-habit-ranking ["M-isolated"]})
            files (filter #(.isFile ^java.io.File %)
                          (file-seq (io/file root)))]
        (is (= :verified-live-selection (:status selection)))
        (is (= 2 (count files)))
        (is (every? #(.startsWith (.getCanonicalPath ^java.io.File %)
                                  (.getCanonicalPath (io/file root)))
                    files))))))
