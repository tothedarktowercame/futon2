(ns futon2.data-paths-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.data-paths :as data-paths]))

(deftest local-path-relocates-only-canonical-component-prefixes
  (testing "canonical production roots preserve today's paths"
    (is (= "/home/joe/code/futon2/data/x.edn"
           (data-paths/local-path "/home/joe/code/futon2/data/x.edn"
                                  "/home/joe/code" nil))))
  (testing "code and notes roots relocate independently"
    (is (= "/tmp/wm/code/futon2/data/x.edn"
           (data-paths/local-path "/home/joe/code/futon2/data/x.edn"
                                  "/tmp/wm/code" nil)))
    (is (= "/tmp/wm/notes/a/b.edn"
           (data-paths/local-path "/home/joe/notes/a/b.edn"
                                  "/tmp/wm/code" "/tmp/wm/notes"))))
  (testing "matching is component-aware and absolute-only"
    (is (= "/home/joe/codex/x"
           (data-paths/local-path "/home/joe/codex/x" "/tmp/wm/code" nil)))
    (is (= "relative/x"
           (data-paths/local-path "relative/x" "/tmp/wm/code" "/tmp/wm/notes")))))
