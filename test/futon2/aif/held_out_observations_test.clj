(ns futon2.aif.held-out-observations-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.held-out-observations :as observations]))

(def declaration
  (edn/read-string (slurp (io/resource "wm/eig/held-out-split-v2.edn"))))

(defn row [id outcome]
  {:run-id id
   :target (:ticket/id declaration)
   :recorded-at "2026-09-24T09:00:00Z"
   :close-sha256 (apply str (repeat 64 "a"))
   :outcome-class outcome})

(deftest open-window-retains-every-row-without-claiming-disposition
  (let [result (observations/collect-window
                declaration
                [(row "run-1" :failure)
                 (assoc (row "run-wrong" :result) :target "another-ticket")])]
    (is (= :open (:status result)))
    (is (= 2 (count (:observations result))) "failed hygiene is retained")
    (is (= [:valid :invalid] (mapv :hygiene (:observations result))))
    (is (= :different-target (get-in result [:observations 1 :hygiene-reason])))
    (is (nil? (:disposition result)))
    (is (= 1 (:missing-count result)))))

(deftest window-closes-only-on-two-distinct-valid-prospective-rows
  (let [closed (observations/collect-window declaration
                                             [(row "run-1" :no-result)
                                              (row "run-2" :timeout)])
        duplicate (observations/collect-window declaration
                                                [(row "run-1" :result)
                                                 (row "run-1" :result)])]
    (is (= :closed (:status closed)))
    (is (= observations/disposition (:disposition closed)))
    (is (= :open (:status duplicate)))
    (is (= :duplicate-run-id
           (get-in duplicate [:observations 1 :hygiene-reason])))))

(deftest retrospective-and-untyped-outcomes-do-not-close
  (testing "the rows stay inspectable even though neither counts"
    (let [result (observations/collect-window
                  declaration
                  [(assoc (row "old" :result) :recorded-at "2026-09-23T11:00:00Z")
                   (row "novel" :invented)])]
      (is (= :open (:status result)))
      (is (= [:before-registration :unknown-outcome-class]
             (mapv :hygiene-reason (:observations result)))))))

(deftest malformed-instants-are-retained-but-cannot-close-window
  (let [result (observations/collect-window
                declaration
                [(assoc (row "bad-1" :result) :recorded-at "zzzz")
                 (assoc (row "bad-2" :failure) :recorded-at "not-an-instant")])]
    (is (= :open (:status result)))
    (is (= 0 (:valid-count result)))
    (is (= 2 (:missing-count result)))
    (is (nil? (:disposition result)))
    (is (= [:invalid :invalid] (mapv :hygiene (:observations result))))
    (is (= [:malformed-recorded-at :malformed-recorded-at]
           (mapv :hygiene-reason (:observations result))))))
