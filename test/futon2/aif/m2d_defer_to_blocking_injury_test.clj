(ns futon2.aif.m2d-defer-to-blocking-injury-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-self-heal-production :as p]))

(def fixture-root "test/futon2/aif/fixtures/m2d/")
(def old-id "injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d")
(def blocker "injury/cfbcbad062bde07d539c7e2abd97a6a189f910f4")
(defn fixture [name] (edn/read-string (slurp (str fixture-root name ".edn"))))
(defn sha [path] (identity/sha256 (java.nio.file.Files/readAllBytes (.toPath (io/file path)))))
(defn env [history]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "m2d-test-" (make-array java.nio.file.attribute.FileAttribute 0)))
        lp (str dir "/ledger.edn") tp (str dir "/transaction.edn")
        order (:work-order (fixture "transaction"))]
    (spit lp (pr-str history))
    (spit tp (slurp (str fixture-root "transaction.edn")))
    {:meta-injury-ledger-path lp :meta-repair-transaction-path tp
     :diagnosis-evidence-pin (:diagnosis-evidence-pin order)
     :acceptance-test (:acceptance-test order)
     :meta-acceptance-observation {:status :present}
     :meta-acceptance-run-fn identity :meta-repair-ports {:author identity}
     :author "fixture-author" :reviewer "fixture-reviewer" :repair-reviewer "fixture-repair-reviewer"
     :author-session "fixture-author-session" :reviewer-session "fixture-reviewer-session"
     :repair-reviewer-session "fixture-repair-reviewer-session"
     :target-repository "/fixture" :target-worktree "/fixture"}))
(defn route [opts]
  (let [seen (atom nil)
        result (with-redefs [coordinator/coordinate
                            (fn [_ transaction _]
                              (reset! seen (:work-order transaction))
                              {:status :refused :reason :fixture-stop :phase :claim})]
                 (p/route! opts))]
    {:result result :order @seen}))

(deftest live-pair-defers-and-intake-selects-the-blocker
  (let [history (fixture "ledger") opts (env history)
        context (p/diagnosis-context opts)
        carried (p/carried-bindings-observation opts context)
        observation (with-redefs [p/produce-diagnosis!
                                 (fn [_ _ entry]
                                   {:status :available :injury-id (:injury-id entry)
                                    :source-record-pin (:cause-record-pin entry)
                                    :diagnosis-evidence-pin (:diagnosis-evidence-pin opts)})]
                      (p/prepare-diagnosis (dissoc opts :diagnosis-evidence-pin) context))
        seen (atom nil)
        result (p/route! (assoc opts :meta-diagnosis-context context
                              :meta-repair-ports
                              {:author (fn [request]
                                         (reset! seen (:work-order request))
                                         (throw (ex-info "fixture stops at author" {})))}))
        order @seen
        deferred (get-in result [:transaction-deferred :path])]
    (is (= blocker (:injury-id observation)))
    (is (= :transaction-deferred (:reason carried)))
    (is (= blocker (:blocking-injury carried)))
    (is (= blocker (:injury-id order)))
    (is (= 1 (:attempt order)))
    (is (= :valid (:status (#'p/diagnosis-transaction-binding
                           {:meta-diagnosis-observation observation} {:work-order order}))))
    (is (and deferred (.isFile (io/file deferred))))
    (when deferred
      (is (= (sha deferred) (sha (str fixture-root "transaction.edn")))))
    (let [after (edn/read-string (slurp (:meta-injury-ledger-path opts)))]
      (is (= (filter #(= old-id (:injury-id %)) (:entries history))
             (filter #(= old-id (:injury-id %)) (:entries after)))))
    (is (= :coordinator/port-threw (:reason result)))))

(defn appended [history entry]
  (let [r (ledger/append-transition history entry)]
    (assert (= :appended (:status r)) (pr-str r))
    (:ledger r)))
(defn inactive-blocker []
  (-> (fixture "ledger")
      (appended {:state :claimed :injury-id blocker :attempt 1 :repair-id "repair/fixture"
                 :algorithm "fixture" :author-session "fixture-claim-author"
                 :reviewer-session "fixture-claim-reviewer"})
      (appended {:state :abandoned :injury-id blocker :attempt 1 :repair-id "repair/fixture"
                 :reason :retry-budget-spent :requeues 3
                 :port-receipt-pin {:path "fixture-only" :sha256 (apply str (repeat 64 "a"))}})))
(defn defer-copy! [opts]
  (let [tp (:meta-repair-transaction-path opts)
        dp (str tp ".deferred-injury_04e14314a0eec3aab88d8e7044be26f3ae5dfe6d.edn")]
    (java.nio.file.Files/move (.toPath (io/file tp)) (.toPath (io/file dp))
                              (make-array java.nio.file.CopyOption 0))
    dp))
(defn readiness [opts]
  ;; Run the actual adapter in its own dependency/classpath environment. No
  ;; serving JVM, seat invoke, reset, click or live store write is involved.
  (let [dir (.getParent (io/file (:meta-repair-transaction-path opts)))
        script (str dir "/readiness.clj")
        input (select-keys opts [:target-repository :meta-injury-ledger-path
                                :meta-repair-transaction-path :author :reviewer :repair-reviewer])]
    (spit script
          (str "(require '[futon3c.agency.registry :as reg] '[futon3c.wm.meta-self-heal-adapters :as a])\n"
               "(with-redefs [reg/get-agent (fn [_] {:agent/status :idle :agent/invoke-ready? true})]"
               " (prn (select-keys (a/readiness (assoc " (pr-str input)
               " :queue-retry-fn identity)) [:status :defects :observations])))\n(shutdown-agents)\n"))
    (let [r (shell/sh "clojure" "-Sdeps" "{:paths [\"src\" \"resources\" \"library\" \"dev\"]}"
                      "-M" script :dir "/home/joe/code/futon3c")]
      (assert (zero? (:exit r)) (pr-str r))
      (edn/read-string (:out r)))))
(defn git! [dir & args]
  (let [r (apply shell/sh "git" "-C" dir args)]
    (assert (zero? (:exit r)) (pr-str r))))
(defn tracked-fixture [opts]
  (let [dir (.getParent (io/file (:meta-repair-transaction-path opts)))]
    (git! dir "init" "-q")
    (spit (str dir "/tracked.txt") "clean\n")
    (git! dir "add" "tracked.txt")
    (git! dir "-c" "user.name=fixture" "-c" "user.email=fixture@example.invalid" "commit" "-qm" "fixture")
    (assoc opts :target-repository dir)))

(deftest live-byte-pins
  (is (= "72f376254b0cbd9b3e3e08beaacb5b37200c0df4d69ac851b54d9d503b80f67e"
         (sha (str fixture-root "ledger.edn"))))
  (is (= "6f271b0961784652df18bde420581673dba6bf867fd6e5c576563145d8135cc9"
         (sha (str fixture-root "transaction.edn")))))

(deftest clean-restore-through-intake-and-route
  (let [opts (tracked-fixture (env (inactive-blocker)))
        dp (defer-copy! opts) pin (sha dp)
        context (p/diagnosis-context opts)
        carried (p/carried-bindings-observation opts context)
        seen (atom nil)
        _ (with-redefs [p/produce-diagnosis! (fn [_ _ entry] (reset! seen (:injury-id entry)))]
            (p/prepare-diagnosis (dissoc opts :diagnosis-evidence-pin) context))
        ready (readiness opts)
        result (when (= :ready (:status ready))
                 (route (assoc opts :meta-diagnosis-context context)))]
    (is (= :restore (get-in context [:injury-selection :mode])))
    (is (= old-id @seen))
    (is (= (:diagnosis-evidence-pin opts) (:diagnosis-evidence-pin carried)))
    (is (= (:acceptance-test opts) (:acceptance-test carried)))
    (is (= :ready (:status ready)))
    (is (= old-id (get-in result [:order :injury-id])))
    (is (= (:meta-repair-transaction-path opts) (get-in result [:result :transaction-restored :path])))
    (is (not (.exists (io/file dp))))
    (is (= pin (sha (:meta-repair-transaction-path opts))))))

(deftest dirty-restore-stops-at-readiness-without-moving-files
  (let [opts (tracked-fixture (env (inactive-blocker)))
        dp (defer-copy! opts) pin (sha dp)
        _ (spit (str (:target-repository opts) "/tracked.txt") "dirty\n")
        ready (readiness opts)]
    ;; The existing adapter refusal is the boundary, not a new route guard.
    ;; We assert physical positions/pins; this is not a full runtime invocation.
    (is (= :refused (:status ready)))
    (is (= [:tracked-worktree-dirty] (:defects ready)))
    (is (not (.exists (io/file (:meta-repair-transaction-path opts)))))
    (is (= pin (sha dp)))))

(deftest inactive-and-absent-causes-resume
  (doseq [history [(inactive-blocker)
                   ;; Rebuild a valid prefix via the real append functions.
                   (:ledger (reduce (fn [r entry]
                                      (let [entry (dissoc entry :ordinal :entry-id :entry-pin
                                                         :predecessor :injury-predecessor)]
                                        (ledger/append-transition (:ledger r) entry)))
                                    {:ledger ledger/empty-ledger}
                                    (take 5 (:entries (fixture "ledger")))))]]
    (let [opts (env history) context (p/diagnosis-context opts)
          result (route (assoc opts :meta-diagnosis-context context))]
      (is (= :resume (get-in context [:injury-selection :mode])))
      (is (= old-id (get-in result [:order :injury-id])))
      (is (nil? (get-in result [:result :transaction-deferred]))))))

(deftest self-cause-does-not-defer
  ;; Deliberately malformed self-reference: test selection, not ledger admission.
  (let [history (assoc-in (fixture "ledger") [:entries 7 :requeue-cause :injury-id] old-id)
        opts (env history)
        selection (p/injury-selection (:meta-repair-transaction-path opts)
                                     {:ledger history}
                                     {:injuries {old-id {:state :enacted}}})]
    (is (= :resume (:mode selection)))))

(deftest changed-selected-file-refuses-before-coordination
  (let [opts (env (fixture "ledger")) context (p/diagnosis-context opts)
        path (:meta-repair-transaction-path opts)
        _ (spit path "\n" :append true)
        result (route (assoc opts :meta-diagnosis-context context))]
    (is (= :meta-self-heal/selected-transaction-changed (get-in result [:result :reason])))
    (is (nil? (:order result)))
    (is (.isFile (io/file path)))
    (is (not (.exists (io/file (get-in context [:injury-selection :deferred-path])))))))

(deftest fresh-selection-yields-to-an-active-blocker
  ;; Defer moved the old transaction aside, then the blocker's prepare wrote
  ;; nothing: the next intake must still select the blocker, not 04e14314.
  (let [history (fixture "ledger") opts (env history)
        _ (defer-copy! opts)
        context (p/diagnosis-context opts)]
    (is (= :fresh (get-in context [:injury-selection :mode])))
    (is (= blocker (get-in context [:injury-selection :injury-id])))))

(deftest discharged-canonical-restores-retained-2aef-transaction
  (let [root "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22"
        path (str root "/transaction.edn")
        deferred (str path ".deferred-injury_2aef7e0c0f48a851a781b41642bf30e52e3fa246.edn")
        injury-id "injury/2aef7e0c0f48a851a781b41642bf30e52e3fa246"
        history (edn/read-string (slurp (str root "/ledger.edn")))
        selection (p/injury-selection path {:ledger history}
                                      {:injuries {injury-id {:state :enacted}}})]
    (is (= "injury/9c6220388033cbd1fbc888cdf6bb3a5b9a3082c0"
           (get-in selection [:stale-transaction :injury-id]))
        "the retained canonical slot is the discharged iteration-25 repair")
    (is (= :restore (:mode selection)) (pr-str selection))
    (is (= injury-id (:injury-id selection)))
    (is (= deferred (:transaction-path selection)))
    (is (= path (:restore-to selection)))))
