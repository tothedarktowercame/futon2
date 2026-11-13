(ns futon2.aif.full-loop-giraffe-pin-test
  "WM hardening package 1 (H-wm-ladder-lucy-handoff-2026-11-13 section 4,
  'Pin the field's scope').

  Bad case: a loop run pinned to a scoped `--only` Giraffe run faces a newer
  verified wider run in the giraffe directory.  With the widened lookup of
  repair 8a0cb58c2 — or any lookup that prefers the wider run — selection
  must be refused with :giraffe-run-not-pinned, and the wider run must not
  be read.  On the base commit find-giraffe-run ignored :outer/giraffe-pin
  entirely and returned the wider run; this test watched that defect."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime]))

(def ^:private find-giraffe-run
  (var-get (find-var 'futon2.aif.full-loop-runtime/find-giraffe-run)))

(defonce ^:private giraffe-loader
  ;; The production serving JVM already has futon3c's source root.  The
  ;; isolated test process adds it explicitly so verified-giraffe-run?
  ;; exercises Giraffe's own verifier (same device as
  ;; futon2.aif.outer-observation-test/actual-var).
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn- sha256 [f]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (java.io.BufferedInputStream. (io/input-stream f))]
      (let [buf (make-array Byte/TYPE 8192)]
        (loop []
          (let [n (.read in buf 0 (alength buf))]
            (when (pos? n)
              (.update digest buf 0 n)
              (recur))))))
    (apply str (map #(format "%02x" %) (.digest digest)))))

(def ^:private sealed-files
  ;; verify-run's current baseline (the manifest's :git-sha below resolves in
  ;; no repository, so the format fails closed to it).
  ["input.edn" "rejected.edn" "pass.edn" "work-clusters.json" "report.md"])

(defn- write-run!
  "One verified missions-field run directory: the sealed file set with a
  manifest that sha-pins every file, exactly as futon3c.giraffe.run/write-run!
  seals its own."
  [root run-id only-ids]
  (let [dir (io/file root run-id)]
    (.mkdirs dir)
    (doseq [f sealed-files]
      (spit (io/file dir f) (str "fixture payload for " run-id " " f "\n")))
    (let [manifest {:files (mapv (fn [f]
                                   {:file f :sha256 (sha256 (io/file dir f))})
                                 sealed-files)
                    :git-sha "0000000000000000000000000000000000000000"
                    :invocation {:edges "/fixture/holes/missions/giraffe-mission-edges.json"
                                 :only-ids only-ids}
                    :sources {:edges {:path "/fixture/holes/missions/giraffe-mission-edges.json"}}}]
      (spit (io/file dir "manifest.edn") (pr-str manifest))
      dir)))

(defn- fixture-root []
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "giraffe-pin-test"
                       (make-array java.nio.file.attribute.FileAttribute 0)))]
    ;; The pinned scoped run is OLDER by directory-name order; the wider run
    ;; is newer, so every newest-first lookup prefers the wider one.
    (write-run! root "20260101T000000.000Z-scoped" ["T-ladder-08" "T-ladder-09"])
    (write-run! root "20260202T000000.000Z-wide" nil)
    root))

(def ^:private scoped-pin
  {:run-id "20260101T000000.000Z-scoped"
   :only-ids ["T-ladder-08" "T-ladder-09"]})

(deftest pinned-scope-refuses-a-newer-wider-run
  (let [root (fixture-root)]
    (testing "the widened lookup prefers the wider run, and the pin refuses it"
      (let [e (try
                (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
                  (find-giraffe-run {:outer/giraffe-root (str root)
                                     :outer/giraffe-pin scoped-pin})
                nil)
                (catch clojure.lang.ExceptionInfo e e))]
        (is (some? e) "selection is refused, not silently widened")
        (is (= :giraffe-run-not-pinned (:kind (ex-data e))))
        (is (= "20260101T000000.000Z-scoped" (:pinned-id (ex-data e))))
        (is (= "20260202T000000.000Z-wide" (:found-id (ex-data e))))
        (is (= ["T-ladder-08" "T-ladder-09"] (:pinned-only-ids (ex-data e))))
        (is (= "giraffe run not pinned: pinned 20260101T000000.000Z-scoped, found 20260202T000000.000Z-wide"
               (ex-message e)))))
    (testing "a pin read from the loop's state file refuses the same way"
      (let [pin-file (io/file root "loop-state" "giraffe-pin.edn")]
        (.mkdirs (.getParentFile pin-file))
        (spit pin-file (pr-str scoped-pin))
        (let [e (try
                  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
                    (find-giraffe-run {:outer/giraffe-root (str root)
                                       :outer/giraffe-pin-path (str pin-file)})
                  nil)
                  (catch clojure.lang.ExceptionInfo e e))]
          (is (= :giraffe-run-not-pinned (:kind (ex-data e)))))))
    (testing "a pin on the run the lookup finds is admitted unchanged"
      (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
        (is (= "20260202T000000.000Z-wide"
               (.getName ^java.io.File
                         (find-giraffe-run {:outer/giraffe-root (str root)
                                            :outer/giraffe-pin
                                            {:run-id "20260202T000000.000Z-wide"
                                             :only-ids nil}}))))))
    (testing "with no pin the lookup behaves as before (the wider run)"
      (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
        (is (= "20260202T000000.000Z-wide"
               (.getName ^java.io.File
                         (find-giraffe-run {:outer/giraffe-root (str root)}))))))
    (testing "a pin against an empty directory refuses nothing, widens nothing"
      (let [empty-root (.toFile (java.nio.file.Files/createTempDirectory
                                 "giraffe-pin-test-empty"
                                 (make-array java.nio.file.attribute.FileAttribute 0)))]
        (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
          (is (nil? (find-giraffe-run {:outer/giraffe-root (str empty-root)
                                       :outer/giraffe-pin scoped-pin}))))))))
