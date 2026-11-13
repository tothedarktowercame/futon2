(ns futon2.aif.task-document-entry-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.mission-registry :as registry])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- fixture []
  (let [root (.toFile (Files/createTempDirectory "task-entry-test-" (make-array FileAttribute 0)))
        repo (io/file root "invented-repo")]
    (.mkdirs (io/file repo ".git"))
    {:root root :repo repo}))

(defn- doc [repo dir id text]
  (let [f (io/file repo "holes" dir (str id ".md"))]
    (.mkdirs (.getParentFile f)) (spit f text) (.getAbsolutePath f)))

(deftest single-document-is-the-same-record-as-census
  (let [{:keys [root repo]} (fixture)
        m (doc repo "missions" "M-invented" "# Invented\nStatus: OPEN\n\n## Wants\n- [ ] An invented outcome\n")
        e (doc repo "excursions" "E-invented" "# Invented\nStatus: OPEN\n")
        t (doc repo "tickets" "T-invented" "# Invented\nStatus: OPEN\n\n## What would close this\nAn invented outcome.\n")]
    (doseq [[path entry] [[m (first (:missions (registry/load-missions-from-repos [repo])))]
                          [e (first (:excursions (registry/load-excursions root)))]
                          [t (first (:tickets (registry/load-tickets root)))]]]
      (is (some? entry))
      (is (= (dissoc entry :kind) (dissoc (registry/task-document-entry path) :kind))))
    (spit m "# Invented\nStatus: COMPLETE\n")
    (is (not (registry/live-mission? (registry/task-document-entry m))))
    (spit t "# Invented\nStatus: OPEN\nGate: awaits Joe\n")
    (is (not (registry/live-ticket? (registry/task-document-entry t))))))

(deftest rejects-worktrees-nested-copies-and-derived-identities
  (let [{:keys [repo]} (fixture)
        m (doc repo "missions" "M-invented" "# Invented\nStatus: OPEN\n")]
    (is (nil? (registry/task-document-entry (doc repo "missions" "M-invented.copy" "# Copy"))))
    (is (nil? (registry/task-document-entry (doc repo "labs/copy/holes/missions" "M-copy" "# Copy"))))
    (.delete (io/file repo ".git"))
    (spit (io/file repo ".git") "gitdir: /invented/worktree")
    (is (nil? (registry/task-document-entry m)))))
