(ns futon2.aif.cascade-want-projection-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-want-projection :as projection])
  (:import [java.math BigInteger]
           [java.nio.file Files Paths]
           [java.security MessageDigest]))

(def tagged-action
  {:kind :cascade-candidate
   :target "M-fixture"
   :precedence
   [[:co-apply
     {:patterns
      {[:p 0] {:id [:p 0]
               :produces #{["M-fixture" :pattern-done [:p 0]]}}}}]]})

(deftest tagged-co-apply-projects-produced-wants
  (is (= [["M-fixture" [:pattern-done [:p 0]]]]
         (:want (projection/attach tagged-action)))))

(deftest tagged-output-free-cascade-stays-empty
  (is (= []
         (:want (projection/attach
                 (assoc-in tagged-action [:precedence 0 1 :patterns [:p 0] :produces]
                           #{}))))))

(deftest existing-wants-remain-authoritative
  (let [action (assoc tagged-action :want [:already-declared])]
    (is (= action (projection/attach action)))))

(def retained-record
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-04-bfa01c36-c6e9-4b2d-b979-788dc5038960.edn")

(def retained-record-sha256
  "168124e864a4c96a1f1f83ca4c04555d25631f2902aa96bd5db5a53fda0e7e5d")

(defn- sha256 [bytes]
  (format "%064x"
          (BigInteger. 1 (.digest (doto (MessageDigest/getInstance "SHA-256")
                                   (.update bytes))))))

(deftest retained-selected-action-projects-all-eight-wants
  (testing "the retained failing action is read by pinned path and digest"
    (let [path (Paths/get retained-record (make-array String 0))
          bytes (Files/readAllBytes path)
          record (edn/read-string {:default (fn [_ value] value)} (String. bytes "UTF-8"))
          action (->> (tree-seq coll? seq record)
                      (filter map?)
                      (filter #(= "M-typed-holes-mathlib-handoff/policy-182bb89516e5bc51e5ed"
                                  (:id %)))
                      (filter #(sequential? (:precedence %)))
                      first)
          wants (:want (projection/attach action))]
      (is (= retained-record-sha256 (sha256 bytes)))
      (is (map? action))
      (is (<= 8 (count wants)))
      (is (every? #(= "M-typed-holes-mathlib-handoff" (first %)) wants)))))
