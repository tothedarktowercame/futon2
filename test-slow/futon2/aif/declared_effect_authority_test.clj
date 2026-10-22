(ns futon2.aif.declared-effect-authority-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.declared-effect-record :as record]
            [futon2.aif.declared-effect-authority :as authority]
            [futon2.aif.close-retention :as retention]))

(deftest ^:slow retained-source-survives-worktree-edit-and-refuses-forgeries
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                     "effect-authority-" (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file dir "source.edn")]
    (try
      (spit source (slurp "resources/wm/cascade-sources/T-repair-occ-444fb018.edn"))
      (let [target (:target (edn/read-string (slurp source)))
            inputs {:target target :declaration {:path (str source)}
                    :base-revision {:repo "futon2" :sha "ae69f5e7b1a1d9012742fddc6d14313ac9292e3d"}}
            r (record/build-record inputs)
            pin (authority/retain-declaration! r (str (io/file dir "retained")))
            occurrence (retention/mint-occurrence
                        {:run-id "offline-authority" :cohort-id "fixture" :attempt-id "fixture"
                         :selected-action {:kind :cascade-candidate :target target :id :fixture}
                         :now #(java.time.Instant/parse "2026-10-22T08:00:00Z")
                         :uuid-fn #(java.util.UUID/fromString "00000000-0000-0000-0000-000000000001")})
            common {:record r :occurrence occurrence :selected-mission target :declaration-pin pin}
            b (authority/build-binding (assoc common :recorded-at "2026-10-22T08:00:00Z"
                                              :bound-at "2026-10-22T08:00:01Z"))
            verify-input (assoc common :binding b)]
        (is (true? (get-in r [:admission :eligible])))
        (is (= :verified (:status (authority/verify-binding verify-input))))
        (spit source "{:edited true}")
        (is (false? (:verified (record/reconstruct inputs r))))
        (is (:verified (authority/reconstruct-retained r pin)))
        (is (= :verified (:status (authority/verify-binding verify-input))))
        (is (= :refused (:status (authority/verify-binding
                                 (assoc verify-input :record-verifier (constantly {:verified true}))))))
        (let [forged (assoc r :horizon {:status :declared :value 999 :authority :declaration/horizon-steps})
              forged (assoc forged :record/digest (record/record-digest forged))]
          (is (= :refused (:status (authority/verify-binding (assoc verify-input :record forged))))))
        (spit (:path pin) "{:corrupted true}")
        (is (false? (:verified (authority/reconstruct-retained r pin))))
        (is (= :refused (:status (authority/verify-binding verify-input)))))
      (finally
        (doseq [file (reverse (file-seq dir))] (.delete file))))))
