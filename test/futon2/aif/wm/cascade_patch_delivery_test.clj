(ns futon2.aif.wm.cascade-patch-delivery-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [cheshire.core :as json]
            [futon2.aif.wm.cascade-patch-delivery :as delivery]
            [futon2.aif.wm.open-cascade-resume :as resume]
            [futon2.aif.full-loop-runtime :as runtime]))

(defn git! [dir & args]
  (let [r (apply shell/sh "git" "-C" (str dir) args)]
    (assert (zero? (:exit r)) r)
    (str/trim (:out r))))

(deftest retained-response-must-arrive-before-review
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "cascade-delivery-" (make-array java.nio.file.attribute.FileAttribute 0)))
        root (io/file dir "canonical") sibling (io/file dir "sibling")
        relative "src/futon2/aif/wm/authored/req_aab759a8.clj"
        source (slurp (io/file "test/resources/wm/cascade-author-delivery/source.clj"))
        answer (json/parse-string (slurp (io/file "test/resources/wm/cascade-author-delivery/response.json")) true)
        checks delivery/check-delivery
        reviews (atom 0) resumes (atom 0)
        error {:kind :wm/open-cascade-execution-error :phase :compile
               :cascade-digest (:cascade-digest answer)
               :holes [{:hole-id ["agent/evidence-over-assertion" 0] :owner :wm/enactment-compiler :question :operational-clause-owed}]
               :resume-port {:retry identity}}]
    (try
      (.mkdirs root)
      (git! root "init" "-q")
      (git! root "config" "user.email" "test@example.invalid")
      (git! root "config" "user.name" "Delivery test")
      (git! root "commit" "--allow-empty" "-qm" "base")
      (git! root "worktree" "add" "-qb" "author" (str sibling))
      (io/make-parents (io/file sibling relative))
      (spit (io/file sibling relative) source)
      (git! sibling "add" relative)
      (git! sibling "commit" "-qm" "retained 7675 source")
      (let [sha (git! sibling "rev-parse" "HEAD")
            resource (fn [name] (let [f (io/file root "src" name)] (when (.isFile f) (.toURL (.toURI f)))))
            handler (runtime/open-cascade-resume-handler
                     {:author "wm-author" :reviewer "wm-reviewer"
                      :open-cascade-author-answer-fn (fn [_] {:state "done" :text (json/generate-string answer)})
                      :open-cascade-reviewer-answer-fn (fn [_] (swap! reviews inc) {:state "done" :text (json/generate-string (assoc answer :accepted-by "wm-reviewer"))})})]
        (with-redefs [delivery/check-delivery (fn [a] (checks (str root) resource a))
                      resume/handle-error! (fn [& _] (swap! resumes inc) {:status :delivery-reached-reviewed-handler})]
          (let [rejected (handler error)]
            (is (= :author-delivery-wrong-repository (:kind rejected)))
            (is (= :source-not-in-canonical-repository (:reason rejected)))
            (is (= (str sibling) (get-in rejected [:found-locations 0 :repository])))
            (is (= sha (get-in rejected [:found-locations 0 :source-commit])))
            (is (zero? @reviews))
            (is (zero? @resumes)))
          (git! root "merge" "--ff-only" sha)
          (is (= :delivered (:status (checks (str root) resource answer))))
          (is (= :delivery-reached-reviewed-handler (:status (handler error))))
          (is (= 1 @reviews @resumes))
          (let [wrong-resource (fn [_] (.toURL (.toURI (io/file sibling relative))))]
            (is (= :serving-resource-not-canonical (:reason (checks (str root) wrong-resource answer))))))
        ;; A supplied unrelated commit cannot borrow the valid source's ancestry.
        (is (= :committed-source-mismatch
               (:reason (checks (str root) resource (assoc-in answer [:patch :commit] (git! root "rev-parse" "HEAD^"))))))
        (spit (io/file root relative) (str source "\n;; drift\n"))
        (is (= :author-delivery-wrong-repository (:kind (checks (str root) resource answer)))))
      (finally
        (doseq [f (reverse (file-seq dir))] (io/delete-file f))))))

(deftest prompt-names-canonical-delivery
  (let [prompt (#'runtime/open-cascade-author-prompt {})]
    (is (str/includes? prompt "/home/joe/code/futon2"))
    (is (str/includes? prompt "patch.commit"))
    (is (str/includes? prompt "canonical HEAD"))))
