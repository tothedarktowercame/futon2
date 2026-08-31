#!/usr/bin/env bb
(ns r9-independence-test
  (:require [checks.r9-independence :as r9]
            [clojure.test :refer [deftest is run-tests testing]]))

(deftest verdicts-consult-the-checker
  (let [claim {:producer "author" :producing-part #{"author"}}
        outside {:producer "reader" :producing-part #{"author"}}
        good (fn [p s] (contains? s p))
        wrong (fn [_ _] false)]
    (is (= :self (r9/independence-verdict claim "author" good)))
    (is (= :independent (r9/independence-verdict outside "reader" good)))
    (is (r9/checker-sound? good [claim outside]))
    (testing "a false-only checker exposes the self-producer falsifier"
      (is (= :independent (r9/independence-verdict claim "author" wrong)))
      (is (false? (r9/checker-sound? wrong [claim]))))))

(deftest pinned-corpus-census
  (let [{:keys [report]} (r9/run-check {})]
    (is (= {:total 22 :fixed 13 :open 7 :unmarked 2
            :closed-ids r9/closed-ids} (:sections report)))
    (is (= {:unknown 13} (get-in report [:runs :ledger-alone :tally])))
    (is (= {:self 13} (get-in report [:runs :declared :tally])))
    (is (= 8 (get-in report [:prose-attribution :count])))
    (is (every? true? (vals (:checks report))))))

(when (= *file* (System/getProperty "babashka.file"))
  (let [{:keys [fail error]} (run-tests)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
