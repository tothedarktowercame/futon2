(ns futon2.aif.habit-store-retired-test
  "WM-HABIT-STORE-RETIRE-I: the runner's close no longer writes the legacy
  cascade habit store (data/wm-habit/cascade-prior.edn).

  That call — habit-reinforcement/close! into cascade-habit-store's path — was
  the LAST production writer of that file, and nothing read it: E comes from
  the enactment records now (WM-HABIT-FOLD-CALL-I), counted by a different
  rule (one increment per enactment record, gated on W_c) over a history the
  legacy store's does not match, since the legacy rule counted per successful
  close with no W_c gate and its history includes the withdrawn M-f11
  (WM-HABIT-FOLD-WIRE-D, futon2 655bbced).

  What these tests measure is the CALL SITE. `close!` is instrumented to
  record the path it is handed and to write there, so a call is visible
  whether or not the legacy rule would have incremented on this fixture's
  decision; what `close!` itself writes, and when, is
  cascade-habit-reinforcement-test's subject and is not restated here.

  Nothing is flown; the store is a temp file; no shared JVM is touched."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.cascade-habit-reinforcement :as reinforcement]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.report.cascade-habit-read-test :as stores]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(defn- close-through-the-runner
  "One close through the runner seam, with the legacy store at PATH."
  [path]
  (#'fixture/run-feature-card-attempt
   {:author-card fixture/feature-card-claim
    :runner-options {:cascade-habit-path path
                     :run-record-dir (str (.getParentFile (io/file path)))}}))

(defn- watching-close!
  "Run F with habit-reinforcement/close! recording the path it is given and
  writing there. Returns [result calls]."
  [f]
  (let [calls (atom [])
        result (atom nil)]
    (with-redefs-fn {#'reinforcement/close!
                     (fn [path _decision _outcome _comparison]
                       (swap! calls conj path)
                       (spit path (pr-str {:wrote :legacy-habit-store}))
                       {:rule/id :instrumented-in-habit-store-retired-test})}
      #(reset! result (f)))
    [@result @calls]))

;; ---------------------------------------------------------------------------
;; 1. The runner at HEAD: no call, no write, a typed absence on the record.

(deftest the-close-does-not-write-the-legacy-store
  (stores/with-store
   (fn [path]
     (spit path (pr-str (habit/read-state path)))
     (let [before (slurp path)
           [{:keys [result]} calls] (watching-close! #(close-through-the-runner path))
           record (edn/read-string (slurp (:run-record result)))]
       (is (= :grounded-change (:outcome result)) "the close ran")
       (is (= [] calls) "habit-reinforcement/close! is not called at all")
       (is (= before (slurp path)) "the store is byte-unchanged")
       (testing "and the absence is typed, on the result and on the run record"
         (is (= {:absent :legacy-habit-store-retired} (:habit-reinforcement result)))
         (is (= {:absent :legacy-habit-store-retired} (:habit-reinforcement record)))
         (is (not (contains? (:habit-reinforcement result) :delta))
             "not a receipt with a zero delta, which would read as the rule running and declining"))))))

;; ---------------------------------------------------------------------------
;; 2. The bad case: the runner one commit earlier does write it.

(def pre-fix-ns
  "futon2.aif.full-loop-runner at f69f103d — the commit before this change —
  loaded as its OWN namespace, never over the real one. Same technique as
  missions-cache-stamp-test; Compiler/load takes a source path because
  load-identity/register! reads *file* and a StringReader leaves it nil."
  (delay
    (let [sha "f69f103d"
          {:keys [exit out err]} (sh/sh "git" "show" (str sha ":src/futon2/aif/full_loop_runner.clj"))
          renamed 'futon2.aif.full-loop-runner-at-f69f103d]
      (when-not (zero? exit) (throw (ex-info "git show failed" {:sha sha :err err})))
      (when-not (str/includes? out "habit-reinforcement/close!")
        (throw (ex-info "the pinned source does not call close!: the bad case would be vacuous"
                        {:sha sha})))
      (with-open [r (java.io.StringReader.
                     (str/replace-first out "(ns futon2.aif.full-loop-runner"
                                        (str "(ns " renamed)))]
        (clojure.lang.Compiler/load r "full_loop_runner_at_f69f103d.clj"
                                    "full_loop_runner_at_f69f103d.clj"))
      (the-ns renamed))))

(deftest the-pre-fix-runner-does-write-the-legacy-store
  (stores/with-store
   (fn [path]
     (spit path (pr-str (habit/read-state path)))
     (let [before (slurp path)
           pre-fix (ns-resolve @pre-fix-ns 'run-opportunity!)
           [_ calls] (watching-close!
                      (fn []
                        (with-redefs-fn {#'runner/run-opportunity! pre-fix}
                          #(close-through-the-runner path))))]
       (is (some? pre-fix) "the pinned runner loaded")
       (is (= [path] calls)
           "the runner at f69f103d calls close! once, with the configured store path")
       (is (not= before (slurp path)) "and the store is written")
       (is (= {:wrote :legacy-habit-store} (edn/read-string (slurp path))))))))
