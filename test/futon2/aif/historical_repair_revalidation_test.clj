(ns futon2.aif.historical-repair-revalidation-test
  (:require [clojure.test :refer [deftest is]] [clojure.java.io :as io]
            [futon2.aif.c-fold-config :as digest]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.repair-obligation :as repair]))
(defn- tmp [] (.toFile (java.nio.file.Files/createTempDirectory "hist-repair" (make-array java.nio.file.attribute.FileAttribute 0))))
(deftest selectable-construction-and-store-transition
  (let [root (tmp) evidence-root (doto (io/file root "e") .mkdir)
        store (doto (io/file root "store") .mkdir)
        obligation {:repair/id "repair-057" :repair/status :open :repair/class :machine-failure
                    :attempt-id "attempt-057"}
        verification {:schema :wm/historical-repair-verification-v1
                      :verification-id "verify-1" :repair-id "repair-057"
                      :state :awaiting-validation :repair-resolved? false
                      :actors {:author "zai-2" :reviewer "codex-10"}
                      :review {:job-id "review-1" :verdict :approve
                               :execution {:executed true :tool-events 1}}
                      :qualification {:path "/authority/q.edn" :sha256 (apply str (repeat 64 "a"))
                                      :check-ids [:recovery :exhaustion]}
                      :finding {:path "/authority/f.edn" :sha256 (apply str (repeat 64 "b"))}
                      :implementation {:first "a" :last "b" :source-head "c"}}
        file (io/file evidence-root "verify.edn")
        _ (spit file (str (pr-str verification) "\n"))
        _ (doto (io/file store "findings") .mkdir)
        _ (spit (io/file store "findings/repair-057.edn") (str (pr-str obligation) "\n"))]
    (doseq [bad [(assoc-in verification [:review :job-id] "")
                 (assoc-in verification [:review :execution :executed] false)
                 (assoc-in verification [:actors :reviewer] "zai-2")
                 (assoc-in verification [:qualification :check-ids] [:recovery :recovery])]]
      (spit file (str (pr-str bad) "\n"))
      (is (thrown? clojure.lang.ExceptionInfo
                   (repair/record-historical-verification!
                    (.getPath store) obligation
                    {:verification-root (.getPath evidence-root) :path (.getPath file)
                     :sha256 (digest/sha256 (slurp file))}))))
    (spit file (str (pr-str verification) "\n"))
    (let [record (repair/record-historical-verification!
                  (.getPath store) obligation
                  {:verification-root (.getPath evidence-root) :path (.getPath file)
                   :sha256 (digest/sha256 (slurp file))})]
      (let [entry (runner/historical-revalidation-entry
                   obligation record {:author "zai-2" :repair-reviewer "codex-10"})]
        (is (= :historical-repair-revalidation
               (:construction-kind (runner/construct-for-decision entry)))))
      (is (nil? (runner/historical-revalidation-entry
                 obligation record {:author "zai-2" :repair-reviewer "zai-2"})))
      (is (= :awaiting-validation (:repair/status record)))
      (is (= :awaiting-validation (:repair/status (first (repair/open-obligations (.getPath store)))))))
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (repair/record-historical-verification!
                  (.getPath store) obligation
                  {:verification-root (.getPath evidence-root) :path (.getPath file)
                   :sha256 (digest/sha256 (slurp file))})))))
