(ns futon2.aif.cascade-sources-spelling-test
  "D17: one canonical pattern-id form (the namespaced keyword) in the
  cascade-sources loader. The E-cascade-real probe had two seats write the
  same pattern id in different spellings; a naive comparison reported zero
  agreement. The loader now canonicalises at load and counts the rewrites
  on the per-file occurrence as :id-normalization."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as cs]))

(def seat-B-dir
  "The seat-B probe declarations (read-only input pinned by the packet)."
  "holes/labs/wm-contract/E-cascade-real/probe-interp/seat-B")

(defn- copy-into-tmp [source-path]
  (let [dir (doto (.toFile (java.nio.file.Files/createTempDirectory
                            "cascade-sources-spelling"
                            (make-array java.nio.file.attribute.FileAttribute 0)))
              (.deleteOnExit))
        target (io/file dir (.getName (io/file source-path)))]
    (io/copy (io/file source-path) target)
    dir))

(defn- occurrence-of [sources target]
  (first (filter #(= target (:target %)) (:read-occurrences sources))))

(deftest declared-file-is-already-canonical
  ;; Pin: the production declared file uses keyword ids throughout, so it
  ;; loads keyword-keyed and rewrites nothing.
  (let [dir (copy-into-tmp "resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn")
        s (cs/load-declared (.getPath dir))
        patterns (get-in s [:interpretations "M-aif-policy-conditioned-eig" :patterns])]
    (is (seq patterns))
    (is (every? keyword? (keys patterns)))
    (is (= 0 (:id-normalization (occurrence-of s "M-aif-policy-conditioned-eig"))))))

(deftest seat-B-file-loads-keyword-keyed-with-rewrites-counted
  ;; Pin: the seat-B probe file spells its pattern ids as namespaced
  ;; strings (the record on disk; the D17 note remembers symbols). It must
  ;; load keyword-keyed, with a nonzero :id-normalization on its occurrence.
  (let [dir (copy-into-tmp (str seat-B-dir "/M-canon-fingerprint-store.edn"))
        s (cs/load-declared (.getPath dir))
        patterns (get-in s [:interpretations "M-canon-fingerprint-store" :patterns])
        receipts (get-in s [:interpretations "M-canon-fingerprint-store" :receipts])]
    (is (seq patterns))
    (is (every? keyword? (keys patterns)))
    (is (every? keyword? (keys receipts)))
    (is (= (set (keys patterns)) (set (keys receipts))))
    (is (pos? (:id-normalization (occurrence-of s "M-canon-fingerprint-store"))))))

(deftest mixed-spellings-load-as-one-pattern-the-precedence-resolves-to
  ;; The D17 falsifier: one pattern named as a symbol in :patterns and as a
  ;; keyword in a candidate's :precedence. Before canonicalisation the
  ;; keyword precedence did not resolve against the symbol pattern key.
  (let [s (cs/load-declared "test/fixtures/cascade-sources-spelling/mixed")
        patterns (get-in s [:interpretations "M-spelling-mix" :patterns])
        precedence (get-in s [:candidates "M-spelling-mix" 0 :precedence])]
    (is (= #{:spelling/one-pattern} (set (keys patterns))))
    (is (= :spelling/one-pattern (:id (first (vals patterns)))))
    (is (= #{:spelling/one-pattern}
           (set (keys (get-in s [:interpretations "M-spelling-mix" :receipts])))))
    (is (every? (fn [entry]
                  (let [id (if (map? entry) (:id entry) entry)]
                    (and (keyword? id) (contains? patterns id))))
                precedence)
        "every precedence id is a keyword resolving to the one pattern")
    ;; Rewrites: symbol pattern key, symbol :id, symbol receipt key, string
    ;; :id in the map entry -- 4; the keyword precedence entry is untouched.
    (is (= 4 (:id-normalization (occurrence-of s "M-spelling-mix"))))))

(deftest un-namespaced-and-non-id-values-refuse-with-the-typed-reason
  (doseq [dir ["test/fixtures/cascade-sources-spelling/un-namespaced-keyword"
               "test/fixtures/cascade-sources-spelling/un-namespaced-string"
               "test/fixtures/cascade-sources-spelling/non-id"]]
    (let [failure (try (cs/load-declared dir) nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :invalid-cascade-source (:error failure)) dir)
      (is (= :invalid-pattern-id (:reason failure)) dir)
      (is (contains? failure :path) dir)
      (is (contains? failure :value) dir))))
