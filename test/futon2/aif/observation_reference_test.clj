(ns futon2.aif.observation-reference-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.observation-checks :as oc]))

(defn git! [repo & args]
  (let [r (apply shell/sh "git" "-C" (str repo) args)]
    (assert (zero? (:exit r)) (pr-str r))
    (str/trim (:out r))))

(defn commit! [repo text]
  (spit (io/file repo "source.clj") text)
  (git! repo "add" "source.clj" "bundle.json")
  (git! repo "-c" "user.name=Fixture" "-c" "user.email=fixture@example.invalid"
        "commit" "-qm" "fixture")
  (git! repo "rev-parse" "HEAD"))

(defn with-repo [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                      "observation-reference" (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (doto (io/file root "fixture") .mkdir)]
    (try
      (git! repo "init" "-q")
      (spit (io/file repo "bundle.json")
            "{\"contracts\":[{\"contract-id\":\"fixture\",\"declarations\":[{\"clojure-locus\":\"fixture/source.clj:1\"}]}]}")
      (with-redefs [oc/repo-root (str root)]
        (f repo (commit! repo "(defn old-head [] 1)\n")))
      (finally (doseq [p (reverse (file-seq root))] (io/delete-file p true))))))

(def c3 {:repo "fixture" :sha "HEAD" :path "source.clj"})
(def c4 (assoc c3 :decl "(defn old-head"))
(def c5 {:repo "fixture" :sha "HEAD" :bundle-path "bundle.json" :entry "fixture"})

(deftest moving-head-and-pinned-references
  (with-repo
   (fn [repo first-sha]
     (let [before (mapv #(%1 %2) [oc/check-path-exists oc/check-decl-in-file oc/check-registry-entry] [c3 c4 c5])
           second-sha (commit! repo "(defn new-head [] 2)\n")
           after (mapv #(%1 %2) [oc/check-path-exists oc/check-decl-in-file oc/check-registry-entry] [c3 c4 c5])]
       (is (not= first-sha second-sha))
       (doseq [[check locator b a] (map vector [oc/check-path-exists oc/check-decl-in-file oc/check-registry-entry] [c3 c4 c5] before after)]
         (is (= "HEAD" (get-in a [:evidence :sha])))
         (is (= first-sha (get-in b [:evidence :resolved-sha])))
         (is (= second-sha (get-in a [:evidence :resolved-sha])))
         (is (re-matches #"[0-9a-f]{40}" (get-in a [:evidence :resolved-sha])))
         (is (not= (:evidence a) (:evidence b)))
         (let [pinned (check (assoc locator :sha first-sha))]
           (is (= first-sha (get-in pinned [:evidence :sha]) (get-in pinned [:evidence :resolved-sha])))))
       (is (true? (:observed (second before))))
       (is (false? (:observed (second after))))
       (let [pinned-bundle (oc/check-registry-entry (assoc c5 :sha first-sha))]
         (is (= first-sha (get-in pinned-bundle [:evidence :resolved-sha])))
         (is (= second-sha (get-in pinned-bundle [:evidence :locus-evidence "fixture/source.clj:1" :evidence :resolved-sha]))))
       (println "OBSERVATION-REFERENCE-RECEIPTS" (pr-str {:before before :after after}))))))

(deftest reference-moves-after-resolution-before-content-check
  (with-repo
   (fn [repo first-sha]
     (let [original @#'oc/git
           moved (atom false)
           result (with-redefs-fn
                    {#'oc/git (fn [r & args]
                                (let [result (apply original r args)]
                                  (when (and (= "rev-parse" (first args)) (compare-and-set! moved false true))
                                    (commit! repo "(defn new-head [] 2)\n"))
                                  result))}
                    #(oc/check-decl-in-file c4))]
       (is @moved)
       (is (not= first-sha (git! repo "rev-parse" "HEAD")))
       (is (= first-sha (get-in result [:evidence :resolved-sha])))
       (is (true? (:observed result)))
       (is (false? (:observed (oc/check-decl-in-file c4))))))))

(deftest unknown-references-refuse
  (with-repo
   (fn [repo _]
     (doseq [[check locator] [[oc/check-path-exists c3] [oc/check-decl-in-file c4] [oc/check-registry-entry c5]]]
       (let [r (check (assoc locator :sha "missing-ref"))]
         (is (= :unknown-sha (:kind r)))
         (is (= "missing-ref" (get-in r [:data :sha])))
         (is (not (contains? r :observed)))))
     (spit (io/file repo "bundle.json") "{\"contracts\":[{\"contract-id\":\"fixture\",\"declarations\":[{\"clojure-locus\":\"missing/source.clj:1\"}]}]}")
     (commit! repo "(defn third-head [] 3)\n")
     (let [r (oc/check-registry-entry c5)]
       (is (= :unknown-sha (:kind r)))
       (is (= "HEAD" (get-in r [:data :sha])))
       (is (= "missing/source.clj:1" (get-in r [:data :locus])))
       (is (not (contains? r :observed)))))))
