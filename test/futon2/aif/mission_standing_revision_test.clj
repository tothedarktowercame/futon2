(ns futon2.aif.mission-standing-revision-test
  "T-wmq-q7: the reviewer falsifier must read the mission document at the
  reviewed git revision, so an author's legitimate mid-run progress (dated
  update blocks, checkbox ticks) cannot trip a source mismatch."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.mission-registry :as registry]))

(defn- run-git [repo & args]
  (let [{:keys [exit out err]}
        (apply shell/sh "git" "-C" repo args)]
    (assert (zero? exit) (str "git " args " failed: " err))
    (clojure.string/trim-newline out)))

(defn- with-temp-repo [f]
  (let [path (str (System/getProperty "java.io.tmpdir")
                  "/wmq-q7-standing-" (java.util.UUID/randomUUID))]
    (try
      (.mkdirs (java.io.File. (str path "/holes")))
      (run-git path "init" "-q")
      (run-git path "config" "user.email" "test@example.com")
      (run-git path "config" "user.name" "test")
      (f path)
      (finally (shell/sh "rm" "-rf" path)))))

(def base-doc
  "# M-q7-standing

Status: open

## Acceptance walk-through

- [ ] criterion one
")

(def progressed-doc
  "# M-q7-standing

Status: open

Update 2026-10-09 (author): progress note appended mid-run.

## Acceptance walk-through

- [ ] criterion one
")

(defn sha256-of [s]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %)
                    (.digest digest (.getBytes ^String s "UTF-8"))))))

(deftest git-authority-reads-pinned-revision-despite-live-drift
  (with-temp-repo
  (fn [repo]
    (let [doc-path (str repo "/holes/M-q7-standing.md")]
      (spit doc-path base-doc)
      (run-git repo "add" ".")
      (run-git repo "commit" "-q" "-m" "base")
      ;; The author advances the mission file mid-run: dated update block.
      (spit doc-path progressed-doc)
      (run-git repo "add" ".")
      (run-git repo "commit" "-q" "-m" "author progress")
      (let [author-commit (run-git repo "rev-parse" "HEAD")
            task {:kind :mission
                  :source {:path doc-path
                           ;; Pin from selection-time scan: the OLD content.
                           :sha256 (sha256-of base-doc)}}
            live (registry/mission-standing-observation task)
            pinned (registry/mission-standing-observation
                    (assoc task :repo repo :commit author-commit))]
        (testing "without git authority the stale pin still mismatches (old behavior)"
          (is (= :mission-standing-source-mismatch (:reason live))))
        (testing "with git authority the document is read at the reviewed commit"
          (is (= :consistent (:status pinned)))
          (is (= {:repo repo :commit author-commit}
                 (get-in pinned [:source :read-at])))
          (is (= (sha256-of progressed-doc)
                 (get-in pinned [:source :content-sha256-at-commit])))))))))

(deftest git-authority-conflict-is-still-detected-at-the-revision
  (with-temp-repo
  (fn [repo]
    (let [doc-path (str repo "/holes/M-q7-standing.md")
          conflicting (str/replace base-doc "Status: open"
                                    "Final status: complete")]
      (spit doc-path conflicting)
      (run-git repo "add" ".")
      (run-git repo "commit" "-q" "-m" "conflict")
      (let [commit (run-git repo "rev-parse" "HEAD")
            obs (registry/mission-standing-observation
                 {:kind :mission
                  :source {:path doc-path :sha256 (sha256-of conflicting)}
                  :repo repo :commit commit})]
        (is (= :conflict (:status obs))))))))

(deftest unreadable-git-authority-is-a-typed-refusal
  (with-temp-repo
  (fn [repo]
    (let [doc-path (str repo "/holes/M-q7-standing.md")]
      (spit doc-path base-doc)
      (run-git repo "add" ".")
      (run-git repo "commit" "-q" "-m" "base")
      (let [obs (registry/mission-standing-observation
                 {:kind :mission
                  :source {:path doc-path :sha256 (sha256-of base-doc)}
                  :repo repo
                  ;; A commit that cannot produce the document.
                  :commit "0000000000000000000000000000000000000000"})]
        (is (= :unknown (:status obs)))
        (is (= :mission-standing-source-unavailable (:reason obs))))))))

;; --- review round 2: self-certification guard (claude-12 review of 2d9f83abf) ---

(defn- commit-doc! [repo doc msg]
  (spit (str repo "/holes/M-q7-standing.md") doc)
  (run-git repo "add" ".")
  (run-git repo "commit" "-q" "-m" msg))

(defn- standing-at-head [repo pinned-sha]
  (registry/mission-standing-observation
   {:kind :mission
    :source {:path (str repo "/holes/M-q7-standing.md") :sha256 pinned-sha}
    :repo repo :commit (run-git repo "rev-parse" "HEAD")}))

(deftest review-tick-own-criterion-checkbox-is-refused-with-changed-line
  (with-temp-repo
    (fn [repo]
      (let [pinned-sha (do (commit-doc! repo base-doc "base")
                           (sha256-of base-doc))
            ;; The author ticks its own criterion checkbox mid-run (on top
            ;; of an otherwise-legitimate dated prose update).
            ]
        (commit-doc! repo (str/replace progressed-doc "- [ ] criterion one"
                                       "- [x] criterion one")
                     "progress plus tick")
        (let [obs (standing-at-head repo pinned-sha)]
          (is (= :refused (:status obs)))
          (is (= :mission-standing-edited-under-review (:reason obs)))
          (is (= ["- [ ] criterion one" "- [x] criterion one"]
                 (:changed-lines obs))))))))

(deftest review-reworded-criterion-is-refused
  (with-temp-repo
    (fn [repo]
      (let [pinned-sha (do (commit-doc! repo base-doc "base")
                           (sha256-of base-doc))
            reworded (str/replace base-doc "criterion one" "criterion one, tightened")]
        (commit-doc! repo reworded "reword")
        (let [obs (standing-at-head repo pinned-sha)]
          (is (= :refused (:status obs)))
          (is (= :mission-standing-edited-under-review (:reason obs)))
          (is (= 2 (count (:changed-lines obs)))))))))

(deftest review-unrecoverable-pin-is-a-typed-refusal
  (with-temp-repo
    (fn [repo]
      (commit-doc! repo base-doc "base")
      (commit-doc! repo progressed-doc "progress")
      ;; A pin that matches no reachable version of the file.
      (let [obs (standing-at-head repo (sha256-of "never committed"))]
        (is (= :refused (:status obs)))
        (is (= :mission-standing-pin-unrecoverable (:reason obs)))))))
