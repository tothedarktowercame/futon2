(ns futon2.aif.giraffe-staleness-mint-test
  "End-to-end witness for the staleness-gated Giraffe mint in
  scripts/wm_click.sh (deficits row Q1-GIRAFFE-FIELD-STALENESS).

  Lives in test-slow, outside the default suite (the futon2 convention):
  the mint shells out to the real supported writer
  (clojure -M -m futon3c.giraffe.run) and reads the canonical mission
  repos, so each JVM launch costs about a minute.  All writes go to a
  temp root, never the canonical storage root.
  Run with: clojure -M:test-slow -n futon2.aif.giraffe-staleness-mint-test

  WM_GIRAFFE_F3C selects the futon3c checkout that owns the writer and
  the --check-fresh probe (it must contain the probe code; the canonical
  checkout does once the futon3c branch lands)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime]) ;; loaded so find-var can see its privates
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def f3c (or (System/getenv "WM_GIRAFFE_F3C") "/home/joe/code/futon3c"))

(defn- temp-root []
  (str (Files/createTempDirectory "giraffe-mint-e2e"
                                  (make-array FileAttribute 0))))

(defn- run-writer!
  "Run the supported -main in the futon3c checkout. ARG-VECs are the CLI
  words after futon3c.giraffe.run."
  [args]
  (let [pb (doto (ProcessBuilder.
                  (concat ["/usr/bin/env" "clojure" "-M" "-m"
                           "futon3c.giraffe.run"]
                          args))
             (.directory (io/file f3c)))
        p (.start pb)
        out (slurp (.getInputStream p))
        err (slurp (.getErrorStream p))]
    {:exit (.waitFor p) :out out :err err}))

(defn- probe [root]
  (run-writer! ["--edges" (str (io/file f3c "holes/missions/giraffe-mission-edges.json"))
                "--source" "missions" "--root" root "--check-fresh"]))

(defn- mint [root]
  (run-writer! ["--edges" (str (io/file f3c "holes/missions/giraffe-mission-edges.json"))
                "--source" "missions" "--root" root]))

(defn- line-of [prefix text]
  (some #(when (str/starts-with? % prefix) (str/trim (subs % (count prefix))))
        (str/split-lines text)))

(defn- run-dirs [root]
  (sort (map #(.getName ^java.io.File %)
             (filter #(.isDirectory ^java.io.File %)
                     (.listFiles (io/file root))))))

(deftest stale-root-mints-one-verified-run-the-field-reader-uses
  (let [root (temp-root)]
    (testing "an empty root (changed digest) is reported stale by the probe"
      (let [{:keys [exit out]} (probe root)]
        (is (zero? exit) out)
        (is (= "false" (line-of "fresh: " out)))
        (is (= "-" (line-of "newest-verified-run-id: " out)))))
    (testing "the gate's mint writes exactly one run through the supported -main"
      (let [{:keys [exit out err]} (mint root)]
        (is (zero? exit) (str out err))
        (is (= 1 (count (run-dirs root))))))
    (let [minted (first (run-dirs root))]
      (testing "the unchanged digest writes nothing: the probe now reports fresh"
        (let [{:keys [exit out]} (probe root)]
          (is (zero? exit) out)
          (is (= "true" (line-of "fresh: " out)))
          (is (= minted (line-of "newest-verified-run-id: " out)))))
      (testing "the field reader (full_loop_runtime find-giraffe-run) serves the minted run"
        ;; find-giraffe-run serves-resolves futon3c's verify-run; give the
        ;; test JVM the futon3c source root the way outer-observation-test
        ;; does, then point the reader's root override at the temp root.
        (let [loader (doto (clojure.lang.DynamicClassLoader.
                            (.getContextClassLoader (Thread/currentThread)))
                       (.addURL (.toURL (.toURI (io/file f3c "src")))))
              finder (var-get (find-var 'futon2.aif.full-loop-runtime/find-giraffe-run))]
          (is (fn? finder))
          (let [dir (with-bindings* {clojure.lang.Compiler/LOADER loader}
                      finder {:outer/giraffe-root root})]
            (is (some? dir) "find-giraffe-run served nothing from the temp root")
            (is (= minted (.getName ^java.io.File dir)))
            (is (.isFile (io/file dir "input.edn"))))))
      (testing "nothing else was written under the temp root"
        (is (= [minted] (run-dirs root)))))))
