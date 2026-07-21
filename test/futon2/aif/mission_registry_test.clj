(ns futon2.aif.mission-registry-test
  "Tests for the mission-doc substrate adapter that flips
   `forward-model/can-propose? :open-mission` to true."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.mission-registry :as mr])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def ^:dynamic *tmpdir* nil)

(defn- with-tmpdir [f]
  (let [dir (Files/createTempDirectory "wm-mission-registry-test"
                                       (into-array FileAttribute []))]
    (binding [*tmpdir* (str dir)]
      (try (f)
           (finally
             (doseq [^File child (reverse (file-seq (io/file (str dir))))]
               (.delete child)))))))

(use-fixtures :each with-tmpdir)

(defn- write-mission!
  [relative-path body]
  (let [path (io/file *tmpdir* relative-path)]
    (io/make-parents path)
    (spit path body)
    (.getAbsolutePath path)))

(deftest load-missions-finds-top-level-mission-docs-test
  (write-mission! "futon0/holes/missions/M-alpha.md"
                  (str "**Status:** OPEN\n"
                       "# Mission Alpha\n"))
  (write-mission! "futon3/holes/missions/M-beta.md"
                  (str "**Status:** COMPLETE\n"
                       "# Mission Beta\n"))
  (write-mission! "futon3/holes/missions/M-beta.journal/hinge-log.md"
                  "# Not a mission doc\n")
  (write-mission! "futon5a/holes/missions/M-handoffs/README.md"
                  "# Not a top-level mission doc\n")
  (let [doc (mr/load-missions *tmpdir*)
        ids (set (map :id (:missions doc)))]
    (is (= #{"M-alpha" "M-beta"} ids))
    (is (not (contains? ids "hinge-log")))
    (is (not (contains? ids "README")))))

(deftest load-missions-excludes-sandbox-and-derived-docs-test
  (write-mission! "futon0/holes/missions/M-alpha.md"
                  (str "Status: OPEN\n"
                       "# Mission Alpha\n"))
  (write-mission! "futon0/holes/missions/M-alpha.v1.md"
                  (str "Status: IDENTIFY (draft 1)\n"
                       "# Mission Alpha v1\n"))
  (write-mission! "futon0/holes/missions/M-alpha.aif-wiring.md"
                  (str "Status: OPEN\n"
                       "# Mission Alpha wiring support\n"))
  (write-mission! "futon3c/.state/night-shift-frames/frame/checkout/holes/missions/M-sandbox.md"
                  (str "Status: OPEN\n"
                       "# Sandbox Mission\n"))
  (let [doc (mr/load-missions *tmpdir*)
        ids (set (map :id (:missions doc)))]
    (is (= #{"M-alpha"} ids))))

(deftest open-missions-filters-closed-inactive-and-draft-test
  (let [doc {:missions [{:id "M-open" :status-class :open}
                        {:id "M-active" :status-class :active}
                        {:id "M-partial" :status-class :partial}
                        {:id "M-unknown" :status-class :unknown}
                        {:id "M-complete" :status-class :complete}
                        {:id "M-inactive" :status-class :inactive}
                        {:id "M-draft" :status-class :draft}]}
        openes (mr/open-missions doc)]
    (is (= #{"M-open" "M-active" "M-partial" "M-unknown"}
           (set (map :id openes))))))

(deftest status-line-variants-are-classified-test
  (write-mission! "futon0/holes/missions/M-plain.md"
                  (str "Status: DONE\n"
                       "# Plain Status\n"))
  (write-mission! "futon0/holes/missions/M-heading.md"
                  (str "## Status: archived\n"
                       "# Heading Status\n"))
  (write-mission! "futon0/holes/missions/M-bulleted.md"
                  (str "- **Status:** **CLOSED 2026-06-04**\n"
                       "# Bulleted Status\n"))
  (write-mission! "futon0/holes/missions/M-draft.md"
                  (str "**Status:** SPECIFIED, NOT YET IMPLEMENTED\n"
                       "# Draft Status\n"))
  (let [by-id (->> (:missions (mr/load-missions *tmpdir*))
                   (map (juxt :id :status-class))
                   (into {}))]
    (is (= :complete (get by-id "M-plain")))
    (is (= :inactive (get by-id "M-heading")))
    (is (= :complete (get by-id "M-bulleted")))
    (is (= :draft (get by-id "M-draft")))))

(deftest open-hole-count-uses-representative-mission-work-signals-test
  (write-mission! "futon0/holes/missions/M-live-work.md"
                  (str "**Status:** HEAD complete; DERIVE pending; INSTANTIATE next\n"
                       "# Live Work\n"
                       "- [ ] unchecked task\n"
                       "- [x] finished task\n"
                       "TODO: tighten the witness\n"
                       "hole: unresolved derivation case\n"
                       "Parent sorry: `sorry/example-open`\n"
                       "## Open questions\n"
                       "1. Which substrate owns the new edge?\n"
                       "2. How should the UI display it?\n"))
  (let [mission (->> (:missions (mr/load-missions *tmpdir*))
                     (filter #(= "M-live-work" (:id %)))
                     first)]
    (is (= :open (:status-class mission)))
    (is (= 7 (:open-hole-count mission)))))

(deftest open-hole-count-is-zero-for-closed-missions-test
  (write-mission! "futon0/holes/missions/M-done.md"
                  (str "**Status:** DONE\n"
                       "# Done\n"
                       "- [ ] stale unchecked task from old plan\n"
                       "TODO: stale note\n"
                       "## Open questions\n"
                       "1. Historical question already closed\n"))
  (let [mission (->> (:missions (mr/load-missions *tmpdir*))
                     (filter #(= "M-done" (:id %)))
                     first)]
    (is (= :complete (:status-class mission)))
    (is (zero? (:open-hole-count mission)))))

(deftest default-open-missions-includes-at-least-one-addressable-mission-test
  (let [missions (mr/open-missions)
        ids (set (map :id missions))]
    (is (seq missions))
    (is (contains? ids "M-the-futon-stack"))))

(deftest can-propose-open-mission-when-state-has-missions-test
  (is (false? (fm/can-propose? {:missions []} :open-mission)))
  (is (false? (fm/can-propose? {} :open-mission)))
  (is (true? (fm/can-propose? {:missions [{:id "M-alpha"}]} :open-mission))))

(deftest can-execute-open-mission-requires-target-in-state-test
  (let [state {:missions [{:id "M-alpha"} {:id "M-beta"}]}]
    (is (true? (fm/can-execute? state {:type :open-mission :target "M-alpha"})))
    (is (true? (fm/can-execute? state {:type :open-mission :target "M-beta"})))
    (is (true? (fm/can-execute? state {:type :open-mission
                                       :target "futon4-d/mission/alpha"})))
    (is (false? (fm/can-execute? state {:type :open-mission :target "M-missing"})))))

(deftest live-mission-target-normalizes-substrate-2-mission-endpoints-test
  (let [missions [{:id "M-alpha" :status-class :open}
                  {:id "M-beta" :status-class :complete}
                  {:id "M-gamma" :status-class :draft}]]
    (is (= "M-alpha" (mr/mission-target-id "futon4-d/mission/alpha")))
    (is (= "M-alpha" (mr/mission-target-id "M-alpha")))
    (is (true? (mr/live-mission-target? missions "futon4-d/mission/alpha")))
    (is (false? (mr/live-mission-target? missions "futon4-d/mission/beta")))
    (is (false? (mr/live-mission-target? missions "futon4-d/mission/gamma")))
    (is (false? (mr/live-mission-target? missions "futon4-d/mission/missing")))))

(deftest mission-status-reports-open-and-hole-count-test
  (write-mission! "futon0/holes/missions/M-alpha.md"
                  (str "Status: OPEN\n"
                       "# Alpha\n"
                       "- [ ] remaining task\n"))
  (let [load-missions mr/load-missions]
    (with-redefs [mr/load-missions (fn
                                     ([] (load-missions *tmpdir*))
                                     ([root] (load-missions root)))]
      (is (= {:open? true :open-hole-count 1}
             (mr/mission-status "M-alpha")))
      (is (= {:open? true :open-hole-count 1}
             (mr/mission-status "futon4-d/mission/alpha")))
      (is (= {:open? false :open-hole-count 0}
             (mr/mission-status "M-missing"))))))

(deftest mission-enumerator-proposer-emits-candidates-test
  (let [state {:missions [{:id "M-alpha" :title "Mission Alpha" :status-class :open :path "/tmp/a"}
                          {:id "M-beta" :title "Mission Beta" :status-class :partial :path "/tmp/b"}]}
        candidates (ap/propose mr/mission-enumerator-proposer state)]
    (is (= 2 (count candidates)))
    ;; live mission docs are ALREADY-OPEN missions: the enumerator proposes
    ;; advancing them, never re-opening them (pilot cycle #1, 2026-06-10)
    (is (every? #(= :advance-mission (:type %)) candidates))
    (is (= #{"M-alpha" "M-beta"} (set (map :target candidates))))
    (is (every? :mission-path candidates))
    (is (every? #(string? (:rationale %)) candidates))))

(deftest proposer-id-test
  (is (= :mission-enumerator (ap/proposer-id mr/mission-enumerator-proposer))))

(deftest integration-bootstrap-no-longer-surfaces-open-mission-gap-test
  (let [state-with-missions {:missions [{:id "M-alpha" :title "Mission Alpha" :status-class :open}]
                             :observation {}
                             :belief {}}
        proposed (ap/propose ap/bootstrap-proposer state-with-missions)
        learn-targets (->> proposed
                           (filter #(= :learn-action-class (:type %)))
                           (map :target-class)
                           set)]
    (is (not (contains? learn-targets :open-mission)))
    (is (seq learn-targets)
        "other action classes are still gated, still surfaced as gaps")))

(deftest sub-phase-keywords-do-not-terminally-classify-test
  ;; Mission statuses describe per-phase progress; a mid-line terminal keyword
  ;; (complete/done/deferred/draft for a SUB-phase) must NOT exclude a still-live
  ;; mission. Only the LEADING state token classifies. Regression guard for the
  ;; over-exclusion fix (claude-1 review of a3f8702): "HEAD complete; ... pending",
  ;; "INSTANTIATE (... MAP completed ...)", "PARTIAL (... deferred)", and a
  ;; "MAP ... revised-draft" lead all stay live; a leading COMPLETE is excluded.
  (write-mission! "futon0/holes/missions/M-subphase-complete.md"
                  "**Status:** HEAD complete; IDENTIFY drafted; MAP pilot run; DERIVE pending\n# X\n")
  (write-mission! "futon0/holes/missions/M-instantiate-active.md"
                  "**Status:** INSTANTIATE (INSTANTIATE-0 active; MAP completed; VERIFY accepted)\n# X\n")
  (write-mission! "futon0/holes/missions/M-partial-deferred.md"
                  "**Status:** PARTIAL (Phase 1 shipped; Phases 2-4 deferred)\n# X\n")
  (write-mission! "futon0/holes/missions/M-map-lead-draft.md"
                  "**Status:** MAP / DERIVE all complete. INSTANTIATE in progress; revised-draft landed\n# X\n")
  (write-mission! "futon0/holes/missions/M-really-complete.md"
                  "**Status:** COMPLETE (2026-01-01)\n# X\n")
  (let [open-ids (set (map :id (mr/open-missions (mr/load-missions *tmpdir*))))]
    (is (contains? open-ids "M-subphase-complete"))
    (is (contains? open-ids "M-instantiate-active"))
    (is (contains? open-ids "M-partial-deferred"))
    (is (contains? open-ids "M-map-lead-draft"))
    (is (not (contains? open-ids "M-really-complete")))))

(deftest finding-2-compound-superseded-status-classifies-inactive
  "Finding-2 (E-live-loop-3): SUPERSEDED-AS-MISSION must classify as :inactive.
   The old exact-match missed compound forms; prefix matching catches them."
  (write-mission! "futon0/holes/missions/M-superseded-compound.md"
                  (str "**Status:** SUPERSEDED-AS-MISSION toward Campaign-bayesian\n"
                       "# Superseded Compound\n"))
  (write-mission! "futon0/holes/missions/M-superseded-plain.md"
                  (str "**Status:** SUPERSEDED\n"
                       "# Superseded Plain\n"))
  (write-mission! "futon0/holes/missions/M-archived-compound.md"
                  (str "**Status:** ARCHIVED-LEGACY\n"
                       "# Archived Legacy\n"))
  (let [by-id (->> (:missions (mr/load-missions *tmpdir*))
                   (map (juxt :id :status-class))
                   (into {}))]
    (is (= :inactive (get by-id "M-superseded-compound"))
        "SUPERSEDED-AS-MISSION classifies :inactive (the Finding-2 bug)")
    (is (= :inactive (get by-id "M-superseded-plain"))
        "SUPERSEDED still classifies :inactive (unchanged)")
    (is (= :inactive (get by-id "M-archived-compound"))
        "ARCHIVED-LEGACY classifies :inactive (prefix match catches compounds)")))

(deftest finding-2-bayesian-structure-learning-filtered-from-live-missions
  "The real mission that triggered Finding-2: M-bayesian-structure-learning
   has SUPERSEDED-AS-MISSION status and must be filtered from the live set."
  (let [missions (mr/open-missions)
        ids (set (map :id missions))]
    (is (not (contains? ids "M-bayesian-structure-learning"))
        "M-bayesian-structure-learning is NOT in the live mission set")))

(deftest duplicate-scan-dedupes-worktree-copies
  "Worktrees and directory copies under the code root produce duplicate
   mission-doc hits with identical ids. The scan must dedupe by id,
   keeping the primary checkout (shortest path)."
  (write-mission! "futon3c/holes/missions/M-dup-test.md"
                  (str "**Status:** OPEN\n"
                       "# Dup Test\n"))
  (write-mission! "futon3c-index-check/holes/missions/M-dup-test.md"
                  (str "**Status:** OPEN\n"
                       "# Dup Test (copy)\n"))
  (write-mission! ".worktrees/futon5-x/holes/missions/M-dup-test.md"
                  (str "**Status:** OPEN\n"
                       "# Dup Test (worktree)\n"))
  (let [doc (mr/load-missions *tmpdir*)
        matches (filter #(= "M-dup-test" (:id %)) (:missions doc))]
    (is (= 1 (count matches)) "duplicate ids deduped to one entry")
    (is (= "OPEN" (:status-line (first matches))))
    ;; Primary checkout (shortest path) is kept
    (is (re-find #"futon3c/holes" (:path (first matches)))
        "primary checkout (shortest path) kept over worktree/copy")))

(deftest scan-root-fence-excludes-non-primary-checkouts
  "W-candidate-drift-fence: the scan-root fence excludes non-primary checkouts
   (worktrees, directory copies, cross-repo duplicates) BEFORE dedupe, so they
   never enter the candidate pool. This is structural, not heuristic."
  (write-mission! "futon3c/holes/missions/M-fence-test.md"
                  (str "**Status:** OPEN\n"
                       "# Fence Test\n"))
  (write-mission! "futon3c-index-check/holes/missions/M-fence-test.md"
                  (str "**Status:** OPEN\n"
                       "# Fence Test (index-check copy)\n"))
  (write-mission! "futon5-health-main/holes/missions/M-fence-test.md"
                  (str "**Status:** OPEN\n"
                       "# Fence Test (health-main worktree)\n"))
  (write-mission! ".worktrees/futon5-x/holes/missions/M-fence-test.md"
                  (str "**Status:** OPEN\n"
                       "# Fence Test (git worktree)\n"))
  (let [doc (mr/load-missions *tmpdir*)
        matches (filter #(= "M-fence-test" (:id %)) (:missions doc))]
    (is (= 1 (count matches)) "only the primary checkout appears")
    (is (re-find #"futon3c/holes" (:path (first matches)))
        "the primary checkout path is kept, not a worktree or copy")))
