(ns futon2.aif.multi-repo-artifact-binding-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.task-execution-evidence :as evidence]))

(defn- git [repo & args]
  (let [r (apply shell/sh "git" "-C" (str repo) args)]
    (assert (zero? (:exit r)) (str (:err r) (:out r)))
    (str/trim (:out r))))

(defn- repo! [root name]
  (let [repo (io/file root name)]
    (.mkdirs repo)
    (git repo "init" "-q")
    (git repo "config" "user.email" "wm@example.invalid")
    (git repo "config" "user.name" "War Machine test")
    (spit (io/file repo "artifact.txt") "base\n")
    (git repo "add" "artifact.txt")
    (git repo "commit" "-q" "-m" "base")
    (.getCanonicalPath repo)))

(defn- commit! [repo text]
  (spit (io/file repo "artifact.txt") text)
  (git repo "add" "artifact.txt")
  (git repo "commit" "-q" "-m" text)
  (git repo "rev-parse" "HEAD"))

(defn- plan [repos bases]
  {:schema :wm/repository-plan-v1 :required []
   :allowed (mapv (fn [repo] {:repo repo :base-head (get bases repo)}) repos)
   :digest "plan-digest"})

(deftest declared-repositories-are-corroborated-independently
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-multirepo-" (make-array java.nio.file.attribute.FileAttribute 0)))
        a (repo! root "repo-a") b (repo! root "repo-b")
        bases {a (git a "rev-parse" "HEAD") b (git b "rev-parse" "HEAD")}
        started (System/currentTimeMillis)
        ca (commit! a "change-a\n") cb (commit! b "change-b\n")
        members [{:repo a :commit ca} {:repo b :commit cb}]
        declaration {:status :present :members members :plan-digest "plan-digest"}
        job {:state "done" :artifact-ref ca
             :result (str "FULL_LOOP_ARTIFACTS: " (pr-str members) "\n"
                          "FULL_LOOP_AUTHOR: DONE " ca)}
        receipt (evidence/fresh-artifact-binding-set
                 {} (plan [a b] bases) started job declaration)]
    (is (= :wm/artifact-binding-set-v1 (:schema receipt)))
    (is (= [a b] (mapv :repo (:bindings receipt))))
    (is (every? :corroborates? (:bindings receipt)))
    (testing "declaring one repository while two changed is a typed defect"
      (let [ex (try
                 (evidence/fresh-artifact-binding-set
                  {} (plan [a b] bases) started job
                  (assoc declaration :members [(first members)]))
                 nil (catch clojure.lang.ExceptionInfo e e))]
        (is (= :artifact-declaration/repository-set-mismatch
               (get-in (ex-data ex) [:author-response-defect :kind])))))
    (testing "a declared commit outside its member base ancestry is typed"
      (let [c (repo! root "repo-c") base (git c "rev-parse" "HEAD")]
        (git c "checkout" "-q" "--orphan" "unrelated")
        (git c "rm" "-q" "-rf" ".")
        (spit (io/file c "other.txt") "unrelated\n")
        (git c "add" "other.txt")
        (git c "commit" "-q" "-m" "unrelated")
        (let [unrelated (git c "rev-parse" "HEAD")
              ex (try
                   (evidence/fresh-artifact-binding-set
                    {} (plan [c] {c base}) started
                    {:state "done" :result (str "FULL_LOOP_AUTHOR: DONE " unrelated)}
                    {:status :present :members [{:repo c :commit unrelated}]})
                   nil (catch clojure.lang.ExceptionInfo e e))]
          (is (= :artifact-declaration/member-not-corroborated
                 (get-in (ex-data ex) [:author-response-defect :kind]))))))))
