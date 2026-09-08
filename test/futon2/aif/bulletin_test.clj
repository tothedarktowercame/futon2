(ns futon2.aif.bulletin-test
  "Tests for the morning bulletin producer.

  Fixture rule (futon2/AGENTS.md, 2026-09-01): at least one fixture has the
  shape of the data the code will see. `real-day-facts` runs the collector
  against the live repositories for a date they actually hold work on, so the
  renderer is exercised on an ~80-commit day across two repos, a 900 KB board,
  and the run-era ledger -- not on a two-commit invention."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.bulletin :as bulletin]
            [futon2.aif.morning-brief :as brief]))

(defn- temp-dir [prefix]
  (.toFile (java.nio.file.Files/createTempDirectory
            prefix (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- sh!
  "Run a command in `dir`. `at` (an ISO instant) is applied to BOTH git dates:
  `git log --since/--until` filters on the COMMITTER date, and `--date=` sets
  only the author date -- a fixture that set just the author date produced
  commits the collector could not see."
  [dir at & args]
  (let [env (cond-> (into {} (System/getenv))
              at (assoc "GIT_AUTHOR_DATE" at "GIT_COMMITTER_DATE" at))
        {:keys [exit err]} (apply shell/sh (concat args [:dir dir :env env]))]
    (when-not (zero? exit)
      (throw (ex-info "command failed" {:args args :err err})))))

(defn- board-edn [rows]
  (pr-str {:items (mapv (fn [[id status class owner statement]]
                          {:id id :status status :class class :owner owner
                           :statement statement})
                        rows)}))

(defn- fixture-repo
  "A two-commit repository whose board moved during the day under test: the
  day-boundary commit is dated the day before, the day's commit the day after
  it, so `board-delta` has a real base to diff against."
  []
  (let [dir (temp-dir "bulletin-fixture-repo")
        board "holes/labs/demo/worklist.edn"
        write (fn [rel text]
                (let [f (io/file dir rel)]
                  (io/make-parents f)
                  (spit f text)))]
    (sh! dir nil "git" "init" "-q")
    (sh! dir nil "git" "config" "user.email" "t@example.invalid")
    (sh! dir nil "git" "config" "user.name" "fixture")
    (write board (board-edn [[:X1 :open :D "any" "the first row"]
                             [:J9 :needs-joe :J "joe" "a ruling only Joe makes"]]))
    (sh! dir nil "git" "add" "-A")
    (sh! dir "2026-03-01T09:00:00" "git" "commit" "-q" "-m" "board: first rows")
    (write board (board-edn [[:X1 :done-unreviewed :D "any" "the first row"]
                             [:J9 :needs-joe :J "joe" "a ruling only Joe makes"]
                             [:X2 :open :D "any" "a row added during the day"]]))
    (write "holes/labs/demo/runs/DEMO-run/README.md"
           "# DEMO-run\n\nThe demo run found two of three arms distinguishable.\n")
    (sh! dir nil "git" "add" "-A")
    (sh! dir "2026-03-02T10:00:00" "git" "commit" "-q" "-m" "demo: work the X1 row")
    {:dir (.getPath dir) :board board}))

(defn- fixture-config [{:keys [dir board]} out-dir brief-root]
  {:repos [{:name "demo" :path dir}]
   :boards [{:name "demo" :repo dir :rel board}]
   :registry (str (.getPath (io/file dir "registry.edn")))
   :run-era-ledger (str (.getPath (io/file dir "ledger.edn")))
   :runs-repo dir
   :runs-rel "holes/labs/demo/runs"
   :trace-root (str dir "/data/wm-trace")
   :out-dir out-dir
   :bulletin-rel-dir "holes/labs/demo/bulletins"
   :brief-root brief-root
   :date "2026-03-02"})

(defn- write-registry-and-ledger! [{:keys [dir]}]
  (spit (io/file dir "registry.edn")
        (pr-str {:choices
                 {:demo-choice
                  {:status :adopted-by-machine
                   :at "2026-03-02"
                   :adoption {:by :claude-demo :at "2026-03-02"
                              :grounds :measurement
                              :decision "Read the check the strict way."}
                   :reversal "Re-record the entry with the other reading."
                   :row :AD1}
                  :other-choice
                  {:status :adopted-by-machine
                   :at "2026-02-28"
                   :adoption {:by :claude-demo :at "2026-02-28"}
                   :reversal "not this day"}
                  :a-ruling {:status :decided :at "2026-03-02"}}}))
  (spit (io/file dir "ledger.edn")
        (pr-str {:rows [#:row{:seq 1 :run-id "demo-run-a" :check-id :demo-check
                              :verdict :green :at "2026-03-02T07:00:00Z"
                              :artifact "holes/labs/demo/runs/DEMO-run/README.md"}
                        #:row{:seq 2 :run-id "demo-run-b" :check-id :demo-check
                              :verdict :typed-absence :at "2026-03-02T08:00:00Z"
                              :artifact "holes/labs/demo/runs/DEMO-run/README.md"
                              :notes "the run store held nothing for this check"}
                        #:row{:seq 3 :run-id "demo-run-c" :check-id :demo-check
                              :verdict :green :at "2026-03-01T08:00:00Z"
                              :artifact "holes/labs/demo/runs/DEMO-run/README.md"}]})))

(deftest render-is-a-pure-function-of-the-facts
  (testing "the same facts render byte-identically, and no clock leaks in"
    (let [repo (fixture-repo)
          _ (write-registry-and-ledger! repo)
          cfg (fixture-config repo (.getPath (temp-dir "bulletin-out"))
                              (.getPath (temp-dir "bulletin-brief")))
          facts (bulletin/collect-facts cfg "2026-03-02")]
      (is (= (bulletin/render facts) (bulletin/render facts)))
      (is (= (bulletin/render facts)
             (bulletin/render (bulletin/collect-facts cfg "2026-03-02")))))))

(deftest recorded-r20-discharge-enters-the-existing-bulletin-channel
  ;; Live record 0a18c4f7-R20.edn pins these values verbatim: :status :absent,
  ;; :reason :no-record-field.  This fixture proves the replacement is recorded.
  (let [root (temp-dir "bulletin-tripwire")
        _ (spit (io/file root "trip-live-pin.edn")
                (pr-str {:node :R20 :tripwire/check :refused
                         :trip/id "trip-live-pin" :status :needs-joe :class :J
                         :statement "R20 tripwire T1 refused the transition"}))
        rows (bulletin/tripwire-discharges (.getPath root))]
    (is (= [{:board "R20-tripwire" :id "trip-live-pin"
             :status :needs-joe :class :J
             :statement "R20 tripwire T1 refused the transition"
             :record (.getPath (io/file root "trip-live-pin.edn"))}]
           rows))
    (is (= :needs-joe (:status (first rows)))
        "the discharge is re-read from its append-only record, not emitted")))

(deftest recorded-trace-enters-the-existing-bulletin-channel
  ;; Live record wm-trace-2026-09-07.edn, run id
  ;; 36820e88-3d68-499d-b359-2d8dbe9743de pins verbatim its :timestamp and
  ;; final :wm/route hop below. The fixture is self-contained because campaign
  ;; data is not present in every worktree.
  (let [root (temp-dir "bulletin-trace")
        path (io/file root "wm-trace-2026-09-07.edn")
        record {:timestamp "2026-09-07T23:12:22.838749091Z"
                :run/id "36820e88-3d68-499d-b359-2d8dbe9743de"
                :wm/route [{:node :TRACE
                            :via "futon2.aif.trace/write-trace!"
                            :at "2026-09-07T23:12:22.837095336Z"}]}
        _ (spit path (str (pr-str record) "\n"))]
    (is (= [{:board "TRACE" :id (:run/id record)
             :status :needs-joe :class :J
             :statement "TRACE record surfaced for operator review"
             :record (.getPath path)}]
           (bulletin/trace-discharges (.getPath root))))))

(deftest a-day-with-no-new-facts-regenerates-to-the-same-bytes
  (testing "generate! twice: identical file, and the second run does not rewrite"
    (let [repo (fixture-repo)
          _ (write-registry-and-ledger! repo)
          cfg (fixture-config repo (.getPath (temp-dir "bulletin-out"))
                              (.getPath (temp-dir "bulletin-brief")))
          first-run (bulletin/generate! cfg)
          bytes-1 (slurp (:file first-run))
          mtime-1 (.lastModified (io/file (:file first-run)))
          second-run (bulletin/generate! cfg)]
      (is (true? (:rewritten? first-run)))
      (is (false? (:rewritten? second-run))
          "an unchanged day must not rewrite the file, or its age is a lie")
      (is (= bytes-1 (slurp (:file second-run))))
      (is (= mtime-1 (.lastModified (io/file (:file second-run)))))
      (testing "the item is minted once, not once per regeneration"
        (is (= :queued (get-in first-run [:item :status])))
        (is (= :already-queued (get-in second-run [:item :status])))
        (is (= 1 (count (brief/items (:brief-root cfg)))))))))

(deftest the-bulletins-own-commit-is-not-the-days-work
  (testing "generate, commit only the bulletin, regenerate -> identical bytes"
    (let [repo (fixture-repo)
          _ (write-registry-and-ledger! repo)
          out (str (:dir repo) "/holes/labs/demo/bulletins")
          cfg (assoc (fixture-config repo out
                                     (.getPath (temp-dir "bulletin-brief")))
                     :out-dir out)
          first-run (bulletin/generate! cfg)
          text-1 (slurp (:file first-run))]
      (sh! (:dir repo) nil "git" "add" "--" "holes/labs/demo/bulletins")
      (sh! (:dir repo) "2026-03-02T23:00:00" "git" "commit" "-q"
           "-m" "bulletin: the day's digest" "--" "holes/labs/demo/bulletins")
      (is (= 2 (count (bulletin/day-commits (:dir repo) "2026-03-02")))
          "the day really does now hold the bulletin's own commit")
      (let [second-run (bulletin/generate! cfg)]
        (is (false? (:rewritten? second-run))
            "the digest must not chase its own commit")
        (is (= text-1 (slurp (:file second-run))))
        (is (= 1 (get-in (bulletin/collect-facts cfg "2026-03-02")
                         [:counts :commits]))
            "one commit of work, the bulletin's own dropped")))))

(deftest the-facts-are-the-ones-the-sources-carry
  (let [repo (fixture-repo)
        _ (write-registry-and-ledger! repo)
        cfg (fixture-config repo (.getPath (temp-dir "bulletin-out"))
                            (.getPath (temp-dir "bulletin-brief")))
        facts (bulletin/collect-facts cfg "2026-03-02")
        text (bulletin/render facts)]
    (testing "only the day's commit, not the day-boundary commit"
      (is (= 1 (get-in facts [:counts :commits])))
      (is (= ["demo: work the X1 row"]
             (mapv :subject (:commits (first (:repo-commits facts)))))))
    (testing "board deltas are moves and additions against the previous commit"
      (let [d (first (:deltas facts))]
        (is (some? (:base d)))
        (is (= [{:id :X1 :from :open :to :done-unreviewed}] (:moved d)))
        (is (= [{:id :X2 :status :open}] (:added d)))
        (is (= [] (:gone d)))))
    (testing "only this day's ledger deposits"
      (is (= [1 2] (mapv :row/seq (:deposits facts)))))
    (testing "only choices the machine adopted on this day, with the reversal"
      (is (= [:demo-choice] (mapv :choice (:adopted facts))))
      (is (str/includes? text "Re-record the entry with the other reading.")))
    (testing "a run directory the day's commits wrote into, with its own outcome line"
      (is (= [{:run "DEMO-run"
               :commits (mapv :sha (:commits (first (:repo-commits facts))))
               :outcome "The demo run found two of three arms distinguishable."}]
             (:experiments facts))))
    (testing "what waits on Joe is the J row, not the open D row"
      (is (= [:J9] (mapv :id (:waits-on-joe facts))))
      (is (not (str/includes? text "`:X2` (demo, class D"))))))

(deftest an-unreadable-source-is-reported-not-silently-empty
  (testing "a repository that is not a repository reads as an absence"
    (let [dir (.getPath (temp-dir "bulletin-not-a-repo"))]
      (is (nil? (bulletin/day-commits dir "2026-03-02")))
      (is (str/includes?
           (bulletin/render {:date "2026-03-02"
                             :repo-commits [{:repo "gone" :path dir :commits nil}]
                             :deltas [] :deposits [] :adopted []
                             :experiments [] :waits-on-joe [] :counts {}})
           "repository not readable")))))

(deftest empty-sections-say-so
  (let [text (bulletin/render {:date "2026-03-02" :repo-commits []
                               :deltas [] :deposits [] :adopted []
                               :experiments [] :waits-on-joe [] :counts {}})]
    (is (str/includes? text "No commits in any watched repository"))
    (is (str/includes? text "No row was deposited"))
    (is (str/includes? text "No choice was adopted by the machine"))
    (is (str/includes? text "Nothing on the watched boards is waiting"))))

(deftest the-item-asks-exactly-the-two-questions-that-apply
  (testing "no :outcome and no :failure, so :machine-response does not fire"
    (let [item (bulletin/item-for {:date "2026-03-02" :counts {:commits 1}
                                   :waits-on-joe [{:id :J9}] :adopted []
                                   :experiments []}
                                  "holes/labs/demo/bulletins/BULLETIN-2026-03-02.md"
                                  nil)]
      (is (= [:selection-quality :substantive-achievement]
             (brief/item-objectives item)))
      (is (nil? (:outcome item)))
      (is (= [:J9] (:bulletin/waits-on-joe-at-queue item)))))
  (testing "a bound grounded entity target restores the third objective's target"
    (let [item (bulletin/item-for {:date "2026-03-02" :counts {} :waits-on-joe []
                                   :adopted [] :experiments []}
                                  "x.md" "arxana/stack/futon-v1/leaf/2")]
      (is (= "arxana/stack/futon-v1/leaf/2"
             (get-in item [:qa-targets :achievement :entity-id]))))))

(deftest the-queued-item-round-trips-through-the-morning-brief-read-path
  (let [repo (fixture-repo)
        _ (write-registry-and-ledger! repo)
        root (.getPath (temp-dir "bulletin-brief"))
        cfg (fixture-config repo (.getPath (temp-dir "bulletin-out")) root)
        result (bulletin/generate! cfg)
        attempt-id (get-in result [:item :attempt-id])
        read-back (first (brief/items root))]
    (is (= "bulletin-2026-03-02" attempt-id))
    (is (= attempt-id (:attempt-id read-back)))
    (is (= 2 (:morning-brief/schema-version read-back))
        "queued through morning-brief/queue-item!, so it carries the v2 carrier")
    (is (= [:selection-quality :substantive-achievement]
           (:pending-objectives (first (brief/pending-items root)))))
    (testing "reviewing it records a verdict and mints no A-matrix event"
      (let [review (brief/review! root attempt-id :substantive-achievement
                                  :partial "half the day landed" "joe")]
        (is (= :partial (:answer review)))
        (is (nil? (:belief-event review))
            "no grounded entity target, so no claim on A -- the spec's own rule")
        (is (empty? (brief/unseen-belief-events root #{})))
        (is (= [:selection-quality]
               (:pending-objectives (first (brief/pending-items root)))))))))

;; The fixture that looks like the data: the live repositories, on a date they
;; hold real work for. This is the shape the renderer will actually see -- two
;; repos, ~80 commits, a 900 KB board, the real ledger and choices registry.
(def ^:private real-day "2026-09-05")

(defn- by-area-of [{:keys [commits]}]
  (if (seq commits) (bulletin/by-area commits) []))

(defn- live-repo? []
  (.isDirectory (io/file (:runs-repo bulletin/default-config) ".git")))

(deftest the-live-day-collects-and-renders
  (if-not (live-repo?)
    (is true "no live futon2 checkout here; the fixture tests carry the logic")
    (let [facts (bulletin/collect-facts bulletin/default-config real-day)
          text (bulletin/render facts)]
      (is (pos? (get-in facts [:counts :commits]))
          "the demonstration day has commits, or the collector is not reading them")
      (is (str/starts-with? text (str "# Morning bulletin -- " real-day)))
      (is (= (get-in facts [:counts :commits])
             (reduce + 0 (map (comp count :commits)
                              (filter :commits (:repo-commits facts)))))
          "the per-repo counts sum to the reported count")
      (is (= (get-in facts [:counts :commits])
             (reduce + 0 (map (comp count :commits)
                              (mapcat #(by-area-of %) (:repo-commits facts)))))
          "each commit lands in exactly one area bucket")
      (is (every? #(str/starts-with? (str (:row/at %)) real-day) (:deposits facts)))
      (is (every? #(= :adopted-by-machine
                      (:status (get-in (edn/read-string
                                        (slurp (:registry bulletin/default-config)))
                                       [:choices (:choice %)])))
                  (:adopted facts))))))
