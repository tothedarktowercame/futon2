(ns futon2.aif.wm.on-prod-history-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.wm.on-prod-history :as sut])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-root []
  (.getPath (.toFile (Files/createTempDirectory
                      "on-prod-history-" (make-array FileAttribute 0)))))

(def entry {:target "M-live" :action {:id :policy/live :precedence {:co-apply []}}})

(deftest checkpoint-is-sealed-append-only-and-loadable
  (let [root (temp-root)
        input {:run-id "run-live" :attempt-id "attempt-002"
               :selected-entry entry
               :selection-receipt {:schema :wm/outer-task-selection-v1}
               :world-at-selection {:schema :wm/world-at-selection-v1}}
        first-write (sut/checkpoint! root input)
        second-write (sut/checkpoint! root input)
        loaded (sut/load-checkpoint root "run-live" "attempt-002")]
    (is (= first-write second-write))
    (is (sut/valid? loaded))
    (is (= entry (:selected-entry loaded)))
    (is (= :on-prod-history-checkpoint-collision
           (try
             (sut/checkpoint! root (assoc input :selected-entry
                                          (assoc entry :target "M-other")))
             nil
             (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))
    (let [path (:path first-write)
          tampered (assoc (edn/read-string (slurp path)) :phase :selection)]
      (spit (io/file path) (pr-str tampered))
      (is (= :on-prod-history-checkpoint-seal-broken
             (try
               (sut/load-checkpoint root "run-live" "attempt-002")
               nil
               (catch clojure.lang.ExceptionInfo e (:kind (ex-data e)))))))))
