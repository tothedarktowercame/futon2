(ns futon2.aif.occurrence-square-registry-library-root-test
  "Repair wm-click-loop-repair-d2550457 (held run
  2026-11-12-87084895-ad71-414f-bd88-721664ce5936): selection stopped at
  :occurrence-square-redecision with :wm/selection-terminal-abstention after
  occurrence-square authoring voided EVERY citation of the retained reading
  (:pattern-part-missing, all four parts) and the policy family came back
  empty. Root cause: the reading cites patterns pinned against the SERVING
  code root's library (the 象 validator session_turn_analysis.py locates its
  library at parents[2] of its own file, so under an isolated
  FUTON_WM_CODE_ROOT it stamps sha256 pins from that copy), while
  occurrence-square-registry/default-library-root was the hard-coded
  canonical /home/joe/code/futon3/library. The resolver returned nil for each
  such pattern, square/pattern-parts saw no IF/HOWEVER/BECAUSE/THEN at all,
  and the citations became voids. These seams must resolve the library
  through data-paths, the same authority as the validator."
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [futon2.aif.occurrence-square-registry :as squares]
            [futon2.aif.selected-reading :as selected-reading]
            [futon2.data-paths :as data-paths]))

(defn temp-dir [prefix]
  (.toFile (java.nio.file.Files/createTempDirectory
            prefix (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest default-library-roots-follow-the-serving-code-root
  (with-redefs [data-paths/production-code-root "/isolated-code-root"]
    (is (= "/isolated-code-root/futon3/library"
           (squares/default-library-root)))
    (is (= "/isolated-code-root/futon3/library"
           (selected-reading/default-library-root)))))

(deftest resolver-reads-patterns-from-the-served-library-copy
  (let [code-root (temp-dir "wm-code-root-")
        library (io/file code-root "futon3" "library")
        family (io/file library "test-registry")
        pattern (io/file family "register-the-run.flexiarg")]
    (.mkdirs family)
    (spit pattern "@flexiarg test-registry/register-the-run\n  + IF:\n    keep\n")
    (with-redefs [data-paths/production-code-root (.getPath code-root)]
      (let [resolve (squares/library-resolver)]
        (is (= "@flexiarg test-registry/register-the-run\n  + IF:\n    keep\n"
               (resolve "test-registry/register-the-run"))
            "a pattern present only in the served library copy resolves")
        (is (nil? (resolve "test-registry/absent-everywhere")))))))
