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

(defn- commit!
  ([repo text] (commit! repo text nil))
  ([repo text agency-job]
   (spit (io/file repo "artifact.txt") text)
   (git repo "add" "artifact.txt")
   (if agency-job
     (git repo "commit" "-q" "-m" text "-m" (str "Agency-Job: " agency-job))
     (git repo "commit" "-q" "-m" text))
   (git repo "rev-parse" "HEAD")))

(defn- plan [repos bases]
  {:schema :wm/repository-plan-v1 :required []
   :allowed (mapv (fn [repo] {:repo repo :base-head (get bases repo)}) repos)
   :digest "plan-digest"})

(deftest primary-binding-is-selected-without-a-selected-locator
  ;; Held run 2026-11-06-24328734 (repair wm-click-loop-repair-c76bc5b9):
  ;; the selected action's observation-locators were empty, so no repository
  ;; was primary, and a present corroborated single-repository declaration was
  ;; thrown back at the author as :primary-binding-absent with
  ;; :primary-repository nil.
  (let [binding {:repo "/repos/only" :commit "abc1234" :corroborates? true}]
    (testing "no selected locator and one corroborated repository binds"
      (is (= binding (evidence/select-primary-artifact-binding
                      {:schema :wm/artifact-binding-set-v1 :bindings [binding]}
                      nil))))
    (testing "a selected primary repository still requires its declared binding"
      (is (= binding (evidence/select-primary-artifact-binding
                      {:schema :wm/artifact-binding-set-v1 :bindings [binding]}
                      "/repos/only")))
      (let [ex (try (evidence/select-primary-artifact-binding
                     {:schema :wm/artifact-binding-set-v1 :bindings [binding]}
                     "/repos/other")
                    nil (catch clojure.lang.ExceptionInfo e e))]
        (is (= :artifact-declaration/primary-binding-absent
               (get-in (ex-data ex) [:author-response-defect :kind])))
        (is (= "/repos/other"
               (get-in (ex-data ex) [:author-response-defect :primary-repository])))))
    (testing "no locator and several corroborated repositories is an apparatus condition, not an author defect"
      (let [two {:repo "/repos/two" :commit "def5678" :corroborates? true}
            ex (try (evidence/select-primary-artifact-binding
                     {:schema :wm/artifact-binding-set-v1 :bindings [binding two]}
                     nil)
                    nil (catch clojure.lang.ExceptionInfo e e))]
        (is (= :primary-locator-absent (:failure-kind (ex-data ex))))
        (is (nil? (:author-response-defect (ex-data ex))))
        (is (= ["/repos/only" "/repos/two"] (:changed-repositories (ex-data ex))))))))

(deftest declared-repositories-are-corroborated-independently
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-multirepo-" (make-array java.nio.file.attribute.FileAttribute 0)))
        a (repo! root "repo-a") b (repo! root "repo-b")
        bases {a (git a "rev-parse" "HEAD") b (git b "rev-parse" "HEAD")}
        started (System/currentTimeMillis)
        ca (commit! a "change-a\n" "job-author") cb (commit! b "change-b\n" "job-author")
        members [{:repo a :commit ca} {:repo b :commit cb}]
        declaration {:status :present :members members :plan-digest "plan-digest"}
        job {:state "done" :job-id "job-author" :artifact-ref ca
             :result (str "FULL_LOOP_ARTIFACTS: " (pr-str members) "\n"
                          "FULL_LOOP_AUTHOR: DONE " ca)}
        receipt (evidence/fresh-artifact-binding-set
                 {} (plan [a b] bases) started job declaration)]
    (is (= :wm/artifact-binding-set-v1 (:schema receipt)))
    (is (= [a b] (mapv :repo (:bindings receipt))))
    (is (every? :corroborates? (:bindings receipt)))
    (testing "declaring one repository while the author's job signed commits in two is a typed defect"
      (let [ex (try
                 (evidence/fresh-artifact-binding-set
                  {} (plan [a b] bases) started job
                  (assoc declaration :members [(first members)]))
                 nil (catch clojure.lang.ExceptionInfo e e))]
        (is (= :artifact-declaration/repository-set-mismatch
               (get-in (ex-data ex) [:author-response-defect :kind])))))
    (testing "another agent's commit in an undeclared shared repository is not the author's change"
      (let [d (repo! root "repo-d") base-d (git d "rev-parse" "HEAD")]
        (commit! d "other agent\n")
        (let [receipt (evidence/fresh-artifact-binding-set
                       {} (plan [a b d] (assoc bases d base-d)) started
                       job declaration)]
          (is (= [a b] (mapv :repo (:bindings receipt)))))))
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
