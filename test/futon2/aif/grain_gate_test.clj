(ns futon2.aif.grain-gate-test
  "Tests for futon2.aif.grain-gate, live-pinned to the click-001 records
  (H-GRAIN-D, futon2 80541ebc): grain maps are READ from the live EDN
  records at test time, never authored as constants.

  Pins:
  * candidate :grain   <- futon3c holes/labs/M-futon-seams/exemplar/click-001-enactment.edn
                          (:keyed-by :role, evidence sha256 over
                           src/futon3c/agency/roles.clj)
  * first-attempt :grain <- .../click-001-outcome.edn (:keyed-by :agent-id)
  * evidence file: futon3c src/futon3c/agency/roles.clj as introduced by
    commit eafd07b7 (\"M-futon-seams instance 4, as chosen\"), the accepted
    enactment and the successor of the first attempt 8e5c431e; the file is
    byte-unchanged since eafd07b7, so the record's pinned :sha256
    (617f7fe8…b0ffe3) still matches it."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.grain-gate :as grain-gate]))

(def ^:private futon3c-root "/home/joe/code/futon3c")

(def ^:private exemplar-dir
  (str futon3c-root "/holes/labs/M-futon-seams/exemplar"))

(defn- read-edn-file [path]
  (edn/read-string (slurp path)))

(def ^:private enactment-record
  (delay (read-edn-file (str exemplar-dir "/click-001-enactment.edn"))))

(def ^:private outcome-record
  (delay (read-edn-file (str exemplar-dir "/click-001-outcome.edn"))))

(def ^:private candidate-grain (:grain @enactment-record))
(def ^:private first-attempt-grain (:grain @outcome-record))

(deftest first-attempt-refuses-grain-mismatch-test
  (testing "live pin: candidate :keyed-by :role (enactment record) against
    the first attempt's :keyed-by :agent-id (outcome record) refuses"
    (is (= :role (:keyed-by candidate-grain)))
    (is (= :agent-id (:keyed-by first-attempt-grain)))
    (let [verdict (grain-gate/grain-gate {:grain candidate-grain}
                                         {:grain first-attempt-grain}
                                         futon3c-root)]
      (is (= :refuse (:status verdict)))
      (is (= :grain-mismatch (:reason verdict))))))

(deftest accepted-enactment-passes-test
  (testing "live pin: the accepted enactment's grain passes; its evidence
    sha256 is checked against the real file at futon3c eafd07b7's path
    src/futon3c/agency/roles.clj (byte-unchanged since that commit)"
    (is (= "src/futon3c/agency/roles.clj" (get-in candidate-grain [:evidence :path])))
    (is (some? (get-in candidate-grain [:evidence :sha256])))
    (is (= {:status :pass}
           (grain-gate/grain-gate {:grain candidate-grain}
                                  {:grain candidate-grain}
                                  futon3c-root)))))

(deftest no-grain-refuses-grain-not-declared-test
  (testing "an attempt with no :grain map refuses :grain-not-declared"
    (is (= :grain-not-declared
           (:reason (grain-gate/grain-gate {:grain candidate-grain}
                                           {}
                                           futon3c-root)))))
  (testing "a :grain map without :keyed-by is also not declared"
    (is (= :grain-not-declared
           (:reason (grain-gate/grain-gate {:grain candidate-grain}
                                           {:grain {:statement "no key"}}
                                           futon3c-root))))))

(deftest changed-evidence-byte-refuses-sha-mismatch-test
  (testing "the accepted attempt with one byte of its evidence file changed
    in a temp copy refuses :sha-mismatch"
    (let [tmp (java.nio.file.Files/createTempDirectory "grain-gate-test"
                                                       (make-array java.nio.file.attribute.FileAttribute 0))
          rel (get-in candidate-grain [:evidence :path])
          target (.resolve tmp rel)]
      (java.nio.file.Files/createDirectories (.getParent target)
                                             (make-array java.nio.file.attribute.FileAttribute 0))
      ;; one byte changed: append a newline to the real file's bytes
      (let [original (java.nio.file.Files/readAllBytes
                      (.toPath (io/file futon3c-root rel)))
            changed (java.util.Arrays/copyOf original (inc (alength original)))]
        (aset changed (alength original) (byte \newline))
        (java.nio.file.Files/write target changed
                                   (make-array java.nio.file.OpenOption 0)))
      (let [verdict (grain-gate/grain-gate {:grain candidate-grain}
                                           {:grain candidate-grain}
                                           (.toString tmp))]
        (is (= :refuse (:status verdict)))
        (is (= :sha-mismatch (:reason verdict)))))))

(deftest scope-mismatch-test
  (testing ":scope-mismatch — specified but unwitnessed in the live records
    (H-GRAIN-D open edge: no record carries :scope; that absence stays
    typed). This witness is constructed from the mission's own scope
    sentence, M-futon-seams.md line 161: \"Pick one instance and declare
    its interface — not all eight.\" The candidate's scope is one instance;
    the attempt touches two."
    (let [candidate {:grain (assoc candidate-grain :scope {:instances 1})}
          attempt {:grain (assoc candidate-grain :scope {:instances 2})}
          verdict (grain-gate/grain-gate candidate attempt futon3c-root)]
      (is (= :refuse (:status verdict)))
      (is (= :scope-mismatch (:reason verdict))))
    (testing "scope is checked only when both grains declare it; an absent
      :scope is a typed absence, not a substituted value"
      (is (= {:status :pass}
             (grain-gate/grain-gate {:grain candidate-grain}
                                    {:grain candidate-grain}
                                    futon3c-root))))))
