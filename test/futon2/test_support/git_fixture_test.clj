(ns futon2.test-support.git-fixture-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.test-support.git-fixture :as git-fixture]))

(deftest temporary-repository-lifecycle
  (testing "a normal body leaves no fixture behind"
    (let [seen (atom nil)
          result (git-fixture/with-temp-repo
                  "git-fixture-normal-"
                  (fn [dir]
                    (reset! seen dir)
                    (spit (io/file dir "witness") "present")
                    :complete))]
      (is (= :complete result))
      (is (false? (.exists ^java.io.File @seen)))))

  (testing "a throwing body leaves no fixture behind"
    (let [seen (atom nil)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo #"fixture failure"
           (git-fixture/with-temp-repo
            "git-fixture-throwing-"
            (fn [dir]
              (reset! seen dir)
              (spit (io/file dir "witness") "present")
              (throw (ex-info "fixture failure" {}))))))
      (is (false? (.exists ^java.io.File @seen))))))
