(ns futon2.aif.load-identity-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]))

(deftest real-load-edit-and-reload
  (let [f (java.io.File/createTempFile "wm-load-identity-" ".clj")
        n (symbol (str "wm-load-fixture-" (System/nanoTime)))
        source (fn [value]
                 (str "(ns " n " (:require [futon2.aif.load-identity :as identity]))\n"
                      "(identity/register! *ns* *file*)\n(def value " value ")\n"))]
    (try
      (with-redefs [identity/registry (atom {})]
        (spit f (source 1))
        (load-file (.getPath f))
        (let [check #(get (identity/report {n (.getPath f)} identity/read-bytes) n)]
          (is (= :current (:status (check))))
          (spit f (source 2))
          (is (= 1 (var-get (ns-resolve n 'value))) "compiled function/value has not reloaded")
          ;; This is the old guard's erroneous disk-to-disk comparison.
          (is (= (identity/sha256 (identity/read-bytes f))
                 (identity/sha256 (identity/read-bytes f))))
          (is (= :stale (:status (check))))
          (load-file (.getPath f))
          (is (= 2 (var-get (ns-resolve n 'value))))
          (is (= :current (:status (check))))
          (is (= :unregistered
                 (get-in (identity/report {'never-loaded (.getPath f)} identity/read-bytes)
                         ['never-loaded :status])))))
      (finally (remove-ns n) (.delete f)))))

(deftest equal-bytes-missing-file-and-required-coverage
  (let [bytes (.getBytes "source" "UTF-8")
        entry {:sha256 (identity/sha256 bytes)}]
    (is (= :current (:status (identity/check entry "ignored" (constantly (.getBytes "source" "UTF-8"))))))
    (is (= :unavailable (:status (identity/check entry "absent" (constantly nil)))))
    (with-redefs [identity/registry (atom {})]
      (is (every? #(= :unregistered (:status %))
                  (vals (identity/report identity/required-sources (constantly bytes)))))))
  (is (.isFile (io/file "src/futon2/aif/load_identity.clj"))))

(deftest unscoped-registration-cannot-use-its-own-file-as-canonical
  (with-redefs [identity/registry (atom {'outside-scope {:sha256 "captured" :source-path "/worktree/file"}})]
    (is (= :unavailable (get-in (identity/report {} identity/read-bytes) ['outside-scope :status])))))
