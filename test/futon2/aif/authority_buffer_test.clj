(ns futon2.aif.authority-buffer-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.authority-buffer :as subject])
  (:import (java.nio.charset StandardCharsets)
           (java.nio.file Files)))

(defn temp-file [text]
  (let [p (Files/createTempFile "authority-buffer-" ".edn" (make-array java.nio.file.attribute.FileAttribute 0))]
    (Files/write p (.getBytes text StandardCharsets/UTF_8) (make-array java.nio.file.OpenOption 0))
    p))

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest strict-single-buffer-authority
  (let [p (temp-file "{:payload {:x 7}}")
        digest (subject/sha256 (Files/readAllBytes p))
        captured (subject/capture! {:path (str p) :expected-sha256 digest :format :edn})
        resolved (subject/resolve-pointer! captured [:payload :x])]
    (is (= 7 (:value resolved)))
    (is (= digest (:source-sha256 resolved)))
    (is (not= (:source-sha256 resolved) (:value-sha256 resolved)))
    ;; Mutation after capture cannot alter retained text/value; recapture refuses.
    (Files/write p (.getBytes "{:payload {:x 9}}" StandardCharsets/UTF_8)
                 (make-array java.nio.file.OpenOption 0))
    (is (= 7 (:value (subject/resolve-pointer! captured [:payload :x]))))
    (is (= :authority-pin-mismatch
           (refusal #(subject/capture! {:path (str p) :expected-sha256 digest :format :edn}))))))

(deftest refusing-shapes
  (testing "trailing and malformed"
    (doseq [[text expected] [["{:a 1} {:b 2}" :authority-trailing-form]
                             ["{" :authority-malformed]]]
      (let [p (temp-file text) d (subject/sha256 (Files/readAllBytes p))]
        (is (= expected (refusal #(subject/capture! {:path (str p) :expected-sha256 d :format :edn})))))))
  (testing "pointer refusal"
    (let [p (temp-file "{:a 1}") d (subject/sha256 (Files/readAllBytes p))
          c (subject/capture! {:path (str p) :expected-sha256 d :format :edn})]
      (is (= :authority-pointer-missing (refusal #(subject/resolve-pointer! c [:b]))))
      (is (= :authority-pointer-ambiguous (refusal #(subject/resolve-pointer! c [])))))))
