(ns futon2.aif.evidence-manifest-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.evidence-manifest :as manifest])
  (:import (java.security MessageDigest)))

(defn- utf8-bytes [s] (.getBytes ^String s "UTF-8"))

(defn- sha256 [bs]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bs)]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(def source-a (utf8-bytes "literal evidence A\n"))
(def source-b (utf8-bytes "literal evidence B\n"))
(def sources {"/evidence/a.edn" source-a "/evidence/b.edn" source-b})

(defn- input-entry
  ([id path at] {:evidence/id id :source-path path :admitted-at at})
  ([id path at expected]
   {:evidence/id id :source-path path :expected-sha256 expected :admitted-at at}))

(defn- build [entries]
  (manifest/build-manifest {:entries entries :read-bytes #(get sources %)}))

(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e
                 (:evidence-manifest/refusal (ex-data e)))))

(deftest manifest-admits-literal-bytes-in-order
  (let [entries [(input-entry "evidence-b" "/evidence/b.edn"
                              "2026-09-14T12:00:01Z" (sha256 source-b))
                 (input-entry "evidence-a" "/evidence/a.edn"
                              "2026-09-14T12:00:02Z")]
        first-build (build entries)
        second-build (build entries)]
    (is (= manifest/manifest-schema (:schema first-build)))
    (is (= ["evidence-b" "evidence-a"]
           (mapv :evidence/id (:entries first-build))))
    (is (= [(sha256 source-b) (sha256 source-a)]
           (mapv :sha256 (:entries first-build))))
    (is (= first-build second-build))
    (is (= (:manifest-sha256 first-build)
           (:manifest-sha256 second-build)))
    (is (= first-build (manifest/validate-manifest first-build)))))

(deftest admission-refusals-are-typed
  (let [at "2026-09-14T12:00:00Z"
        good (input-entry "evidence-a" "/evidence/a.edn" at)]
    (testing "missing source"
      (is (= :source-unavailable
             (refusal #(build [(assoc good :source-path "/evidence/missing.edn")])))))
    (testing "duplicate and blank identities"
      (is (= :duplicate-evidence-id (refusal #(build [good good]))))
      (is (= :identity-invalid
             (refusal #(build [(assoc good :evidence/id " ")])))))
    (testing "caller-supplied digest"
      (is (= :source-sha256-mismatch
             (refusal #(build [(assoc good :expected-sha256 (apply str (repeat 64 "0")))])))))
    (testing "instant and entry shape"
      (is (= :timestamp-invalid
             (refusal #(build [(assoc good :admitted-at "yesterday")]))))
      (is (= :shape-invalid
             (refusal #(build [(assoc good :extra :not-allowed)])))))))

(deftest retention-agreement-is-exact-and-ordered
  (let [m (build [(input-entry "evidence-a" "/evidence/a.edn" "2026-09-14T12:00:00Z")
                  (input-entry "evidence-b" "/evidence/b.edn" "2026-09-14T12:00:01Z")])]
    (is (true? (manifest/verify-retention-agreement
                m {:admitted-evidence ["evidence-a" "evidence-b"]})))
    (doseq [ids [["evidence-a"]
                 ["evidence-a" "evidence-b" "evidence-c"]
                 ["evidence-b" "evidence-a"]]]
      (is (= :retention-evidence-mismatch
             (refusal #(manifest/verify-retention-agreement
                        m {:admitted-evidence ids})))))))

(deftest empty-manifest-agrees-with-empty-retention
  (let [m (build [])]
    (is (= [] (:entries m)))
    (is (true? (manifest/verify-retention-agreement
                m {:admitted-evidence []})))
    (is (= :retention-evidence-mismatch
           (refusal #(manifest/verify-retention-agreement
                      m {:admitted-evidence ["unresolved-id"]}))))))
