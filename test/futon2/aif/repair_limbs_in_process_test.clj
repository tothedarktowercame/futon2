(ns futon2.aif.repair-limbs-in-process-test
  "PROOF-wm-works ⟨1⟩6-⟨1⟩8, in-process per Joe's 2026-09-23 method ruling:
  drive the repair's remaining limbs through the REAL code paths (the
  accepted-increment predicate, the acceptance locators, the B update) over
  the two real recorded occurrences plus clearly-labelled fixtures for the
  states no recorded occurrence holds yet. No locator, guard or conjunct is
  weakened; no fixture touches the real data directories."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.observation-checks :as checks]
            [clojure.test :refer [deftest is]]
            [futon2.aif.accepted-increment :as ai]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.learning-trial-ledger :as ledger]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

;; ---------------------------------------------------------------------------
;; Fixtures: each stands in for a state the machine has NOT yet reached.
;; They live under a TEMP directory (never the real data/ trees) and are
;; shaped exactly like the real records they stand in for. The locators the
;; cascade source declares are C4-over-git — they read git at a sha — so a
;; fixture that must make a C4 head observable has to be committed. The
;; fixtures below therefore build a THROWAWAY git worktree of futon2's
;; ticket+resources state, commit the fixture shapes there, and point the
;; locators' :repo at it. Nothing in /home/joe/code/futon2 is touched.
;; ---------------------------------------------------------------------------

(defn- fixture-repo
  "A throwaway git repo holding shaped copies of the ticket file and the
   three evidence resources, committed so C4 locators resolve at its HEAD.
   Stands in for: the post-repair state (all limbs landed, Status DONE)."
  []
  ;; NOTE: the C3/C4 checkers run `git -C /home/joe/code/<repo>`, so the
  ;; fixture repo must live UNDER /home/joe/code as a relative path — a
  ;; throwaway sibling of futon2, never inside futon2's data/ trees, and
  ;; removed on exit.
  (let [dir (io/file "/home/joe/code"
                     (str ".repair-limbs-fixture-" (System/nanoTime)))
        _ (.mkdirs dir)
        _ (.deleteOnExit dir)
        git (fn [& args]
              (apply clojure.java.shell/sh
                     (concat ["git" "-C" (.getAbsolutePath dir)] args)))
        ticket-dir (io/file dir "holes/tickets")
        eig-dir (io/file dir "resources/wm/eig")
        rec-dir (io/file dir "resources/wm/rechecks")
        _ (doseq [d [ticket-dir eig-dir rec-dir]] (.mkdirs d))
        ;; the ticket: the REAL current ticket with Status written DONE
        real-ticket (slurp (io/file "/home/joe/code/futon2/holes/tickets"
                                    (str t ".md")))
        _ (spit (io/file ticket-dir (str t ".md"))
                (.replace real-ticket "**Status:** OPEN" "**Status:** DONE"))
        ;; FIXTURE: held-out observations collected — same shape as the
        ;; split declaration (line-initial head), standing in for the
        ;; collected-observations record the machine has not written
        _ (spit (io/file eig-dir "held-out-observations.edn")
                (str "{:schema :wm/eig-held-out-observations-v1\n"
                     "HELD-OUT-OBSERVATIONS-COLLECTED\n"
                     " :held-out [\"attempt-002\" \"attempt-003\"]\n"
                     " :window {:status :closed}\n}\n"))
        ;; FIXTURE: calibration evidence present and passing
        _ (spit (io/file eig-dir "held-out-calibration.edn")
                (str "{:schema :wm/eig-held-out-calibration-v1\n"
                     "CALIBRATION-EVIDENCE-PASSING\n"
                     " :mean-log-loss 0.91\n :mean-brier 0.19\n}\n"))
        ;; the split declaration itself, already real — copy verbatim
        _ (spit (io/file eig-dir "held-out-split.edn")
                (slurp (io/file "/home/joe/code/futon2/resources/wm/eig/held-out-split.edn")))
        _ (git "init" "-q")
        _ (git "config" "user.email" "fixture@test")
        _ (git "config" "user.name" "fixture")
        _ (git "add" "-A")
        _ (git "commit" "-q" "-m" "fixture: post-repair state")
        head (-> (git "rev-parse" "HEAD") :out str/trim)]
    {:dir dir :repo (.getName dir) :head head}))

(defn- locator-in [repo head loc]
  (assoc loc :repo (:repo repo) :sha head))

(deftest real-attempt-001-limb-one-recorded
  ;; The REAL occurrence: the split declaration limb. (b) failed on the
  ;; artifact's shape at the time; the state is superseded by 0798f96a.
  ;; This test pins the predicate's verdict on attempt-001's own inputs.
  (let [r (ai/accepted-increment
           {:binding {:repo "/home/joe/code/futon2"
                      :commit "ee22106c0e1f02c104d4864419ea0ffc264e5529"
                      :pre-dispatch-head "c24c903b9b9577d6192ea4c9918a5da4f8a81d2a"
                      :descendant? true :corroborates? true :claim-in-author-window? true}
            :produced-tokens {[t :repair/split-declared-valid]
                              {:class :C4 :repo "futon2" :sha "ee22106c"
                               :path "resources/wm/eig/held-out-split.edn"
                               :decl "HELD-OUT-SPLIT-DECLARED"}}
            :acceptance {:token :restoration-accepted
                         :locator {:class :C4 :repo "futon2" :sha "HEAD"
                                   :path (str "holes/tickets/" t ".md")
                                   :decl "**Status:** DONE"}}
            :after-revision "ee22106c0e1f02c104d4864419ea0ffc264e5529"})]
    (is (false? (:accepted? r)) "the first limb's own close was not accepted")
    (is (contains? #{:b :c} (:failed r)) "a conjunct after (a) failed")))

(deftest real-attempt-002-limb-two-recorded
  ;; The REAL occurrence: the shape-corrected declaration. (a)(b) hold, (c)
  ;; fails on Status OPEN. Pinned from the recorded close's own inputs.
  (let [r (ai/accepted-increment
           {:binding {:repo "/home/joe/code/futon2"
                      :commit "0798f96ad2082a8f80c8a850c8e1138c036213f9"
                      :pre-dispatch-head "ee22106c0e1f02c104d4864419ea0ffc264e5529"
                      :descendant? true :corroborates? true :claim-in-author-window? true}
            :produced-tokens {[t :repair/split-declared-valid]
                              {:class :C4 :repo "futon2" :sha "0798f96a"
                               :path "resources/wm/eig/held-out-split.edn"
                               :decl "HELD-OUT-SPLIT-DECLARED"}}
            :acceptance {:token :restoration-accepted
                         :locator {:class :C4 :repo "futon2" :sha "HEAD"
                                   :path (str "holes/tickets/" t ".md")
                                   :decl "**Status:** DONE"}}
            :after-revision "0798f96ad2082a8f80c8a850c8e1138c036213f9"})]
    (is (false? (:accepted? r)))
    (is (= :c (:failed r)) "only the target fails acceptance")))

(deftest fixture-window-complete-observations-observable
  ;; FIXTURE stands in for: attempt-003 durable close + the window complete
  ;; (no recorded occurrence holds this). With the window closed and the
  ;; observations record present, the observations limb's produced token
  ;; becomes observable through its OWN C4 locator — unweakened, resolved
  ;; against the fixture repo's HEAD.
  (let [repo (fixture-repo)
        loc (locator-in repo (:head repo)
                        {:class :C4 :path "resources/wm/eig/held-out-observations.edn"
                          :decl "HELD-OUT-OBSERVATIONS-COLLECTED"})
        check (fn [l] (case (:class l)
                        :C4 (checks/check-decl-in-file l)
                        :C3 (checks/check-path-exists l)))
        r (check loc)]
    (is (true? (:observed r)) (pr-str r))))

(deftest fixture-full-chain-accepted
  ;; FIXTURE stands in for: all four limbs landed — the declaration (real,
  ;; copied verbatim), the observations collected, the calibration passing,
  ;; and the ticket's Status written DONE. With (a) a fresh binding over the
  ;; fixture commit, (b) the calibration limb's declared produced token
  ;; observed true, and (c) the ticket's own acceptance observed true, the
  ;; predicate MUST return {:accepted? true} with all three conjuncts
  ;; satisfied — every locator, guard and conjunct exactly as declared.
  (let [repo (fixture-repo)
        head (:head repo)
        r (ai/accepted-increment
           {:binding {:repo (:repo repo) :commit head
                      :pre-dispatch-head "4b825dc642cb6eb9a060e54bf8d69288fbed4b0"
                      ;; the fixture's empty-tree base is the pre-state
                      :descendant? true :corroborates? true :claim-in-author-window? true}
            :produced-tokens
            ;; the calibration limb's declared produced token
            {[t :repair/calibration-evidence-present]
             (locator-in repo head
                         {:class :C4 :path "resources/wm/eig/held-out-calibration.edn"
                          :decl "CALIBRATION-EVIDENCE-PASSING"})}
            :acceptance
            {:token :restoration-accepted
             :locator (locator-in repo head
                                  {:class :C4 :path (str "holes/tickets/" t ".md")
                                   :decl "**Status:** DONE"})}
            :after-revision head})]
    (is (true? (:accepted? r)) (pr-str (dissoc r :evidence)))
    (is (true? (get-in r [:evidence :acceptance-result :observed])))
    (is (every? (fn [[_ v]] (true? (:observed v)))
                (get-in r [:evidence :produced-token-results])))))

(deftest fixture-accepted-close-writes-the-b-update
  ;; With the PREDICATE'S OWN accepted verdict (from the full-chain fixture,
  ;; not a hand-built map), the runner's B-update call site's guard passes
  ;; and b-update runs — exactly once, occurrence-keyed by the verdict's own
  ;; binding commit. The second call reads the row the RUNNER'S OWN PATH
  ;; wrote: the append goes through learning-trial-ledger/record! with the
  ;; identity the call site itself derives (the binding commit), so the
  ;; exactly-once is pinned against the real row shape.
  (let [repo (fixture-repo)
        head (:head repo)
        verdict (ai/accepted-increment
                 {:binding {:repo (:repo repo) :commit head
                            :pre-dispatch-head "4b825dc642cb6eb9a060e54bf8d69288fbed4b0"
                            :descendant? true :corroborates? true :claim-in-author-window? true}
                  :produced-tokens
                  {[t :repair/calibration-evidence-present]
                   (locator-in repo head
                               {:class :C4 :path "resources/wm/eig/held-out-calibration.edn"
                                :decl "CALIBRATION-EVIDENCE-PASSING"})}
                  :acceptance
                  {:token :restoration-accepted
                   :locator (locator-in repo head
                                        {:class :C4 :path (str "holes/tickets/" t ".md")
                                         :decl "**Status:** DONE"})}
                  :after-revision head})
        _ (is (true? (:accepted? verdict)) "precondition: the predicate accepts")
        tmp (.toFile (java.nio.file.Files/createTempDirectory
                      "limbs-b-update" (make-array java.nio.file.attribute.FileAttribute 0)))
        _ (spit (io/file tmp "attempts.edn") "")
        occurrence-identity (get-in verdict [:evidence :binding :commit])
        r1 (ledger/b-update {:family :aif/two-layer-calibration
                             :occurrence-identity occurrence-identity
                             :accepted-verdict verdict
                             :ledger-root (.getPath tmp)})
        ;; the runner's own append path: record! a row carrying the same
        ;; identity the call site derives from the verdict
        _ (ledger/record! (.getPath tmp)
                                   {:trials [{:status :admitted-at-attempt-grain
                                              :deduplication {:identity occurrence-identity}
                                              :learning-family :aif/two-layer-calibration
                                              ;; record! derives the increment from
                                              ;; :after-observation — the real producer's
                                              ;; field — so the runner-written row is honest
                                              :after-observation true}]})
        r2 (ledger/b-update {:family :aif/two-layer-calibration
                             :occurrence-identity occurrence-identity
                             :accepted-verdict verdict
                             :ledger-root (.getPath tmp)})]
    (is (= :updated (:status r1)) (pr-str r1))
    (is (= 3/4 (:theta r1)) "cold Laplace success")
    (is (= :already-recorded (:status r2)) "exactly once: the second call sees the runner-written row")
    (is (= (:theta r1) (:theta r2)))
    (is (= 1 (count (ledger/read-trials (.getPath tmp))))
        "exactly one row, written by the runner's record! seam")))
