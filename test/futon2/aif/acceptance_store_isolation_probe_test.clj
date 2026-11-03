(ns futon2.aif.acceptance-store-isolation-probe-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(deftest strategic-selection-uses-process-store-root
  ;; The acceptance boundary names the replay root through
  ;; FUTON_WM_PHASE_REPLAY_INPUT_ROOT; outside it (an ordinary warrant run,
  ;; where b13461d2e already makes the data root hermetic) the probe owns a
  ;; temp root and passes it explicitly, so the claim (the replay inputs
  ;; land under the supplied root and nowhere else) is tested either way.
  (let [root (or (System/getenv "FUTON_WM_PHASE_REPLAY_INPUT_ROOT")
                 (str (java.nio.file.Files/createTempDirectory
                       "acceptance-store-isolation-probe"
                       (make-array java.nio.file.attribute.FileAttribute 0))))]
    (let [selection (runner/strategic-selection!
                     {:phase-replay-input-root root
                      :strategic-selection-invoke-fn
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
                  files)))))
