(ns futon2.aif.meta-acceptance-injury-e43a348c-test
  "Acceptance proposal for injury/e43a348c8b9fa28a0e95636d90b01af1a732bc56.

  Injury: run 2026-10-29-4438ee79-44ec-4982-aba3-4107c477902e selected a
  real reading-family cascade (:selectorSeam \"live:validated-selection\",
  target M-futonzero-prelim-practice), whose compile then held as a typed,
  repairable :wm/open-cascade-execution-error (:held-phase :compile). The
  cascade-resume fall-through relabeled that held machine condition as the
  tick's abstention (\"War Machine abstained or selected no addressable
  action\"), and the close routed RUNNER -> FULL_LOOP_CLOSE via :incomplete.

  Repair contract pinned here, exercised ONLY through the three production
  functions named in the diagnosis (no stubs of them; the hermetic scan
  redefs below follow the repo convention of
  futon2.report.selection-nil-receipt-test and stub scan *inputs*, never
  the functions under test):

  1. The selection entrypoint (futon2.report.war-machine/generate-war-machine)
     drives the judgement whose decision -- the pinned run's own retained
     decision, used as the real input -- measures COMPLETE loop-node
     exercise through the required-loop-node-evaluator
     (futon2.aif.wm.loop-node-exercise/receipt), with legitimate bypasses
     only (:R6 singleton candidate, :R13 single-step policy). Selection ran;
     abstention was therefore never available to this tick.
  2. The terminal-routing-entrypoint
     (futon2.aif.wm.terminal-receipt/terminal-receipt) on the pinned record
     routes the failure as the typed machine condition
     :wm/open-cascade-execution-error naming the selected target, with
     :abstention-status nil, and -- the repaired behavior this acceptance
     requires -- SURFACES the held phase (:held-phase :compile) on the
     failure receipt, so a held open-cascade condition can never again be
     certified at the routing seam under the abstention label. On injured
     code the receipt carries no held-phase marker and this test FAILS for
     the injury's reason; after repair it passes."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.calibration-cycle :as calibration]
            [futon2.aif.wm.loop-node-exercise :as lne]
            [futon2.aif.wm.terminal-receipt :as tr]
            [futon2.report.war-machine :as wm]))

(def ^:const injury-id "injury/e43a348c8b9fa28a0e95636d90b01af1a732bc56")

(def ^:const record-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-29-4438ee79-44ec-4982-aba3-4107c477902e.edn")

(def ^:const record-sha256
  "a687da20d9a8fa3441b718ba006741643f1581954f718fbcf08b43d66e00a39e")

(defn- sha256 [path]
  (let [bytes (java.nio.file.Files/readAllBytes
               (.toPath (io/file path)))
        digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest bytes)
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn- pinned-record []
  (edn/read-string (slurp record-path)))

(def scan-vars
  "The scan/input producers generate-war-machine fans out to before its
  judgement layer. Redefined to constant nil (and the R12 calibration
  admission to a non-writing receipt) so the SELECTION ENTRYPOINT itself
  runs hermetically and offline, per selection-nil-receipt-test."
  '[fetch-evidence-result fetch-missions scan-self-watch scan-loop-health
    scan-support-attack scan-mission-triage scan-graph scan-sessions scan-portfolio
    scan-mission-detail scan-patterns scan-frames scan-metabolic-balance
    summarize-working-tree-hygiene scan-blocks scan-window scan-annotation-graph
    scan-strategic-vocabulary scan-r-criteria scan-vsatarcs-status capability-star-map])

(deftest t1-pinned-record-is-the-injured-run
  (testing "the pinned run record is byte-identical and shows the relabel"
    (is (= record-sha256 (sha256 record-path)))
    (let [record (pinned-record)
          failure (:failure record)]
      (is (= "live:validated-selection" (:selectorSeam record)))
      (is (= :wm/open-cascade-execution-error (:kind failure)))
      (is (= :cascade-resume (:stage failure)))
      (is (= :compile (get-in failure [:detail :held-phase])))
      (is (true? (get-in failure [:detail :repairable])))
      ;; The injury itself: an abstention message for a tick that selected.
      (is (str/includes? (:error failure) "abstained")))))

(deftest t2-loop-machinery-exercised-not-the-cause
  (testing "the real evaluator over the pinned decision: complete, no refusals"
    (let [r (lne/receipt (:decision (pinned-record)))]
      (is (= :wm/loop-node-exercise-v1 (:schema r)))
      (is (= :complete (:status r)))
      (is (= 0 (:refused (:counts r))))
      (is (= :bypassed (:status (get-in r [:by-node :R6]))))
      (is (= :singleton-admissible-candidate
             (:because (get-in r [:by-node :R6]))))
      (is (= :bypassed (:status (get-in r [:by-node :R13]))))
      (is (= :single-step-policy (:because (get-in r [:by-node :R13])))))))

(deftest t3-selection-entrypoint-reproduces-complete-exercise
  (testing "generate-war-machine (hermetic inputs) yields the exercised decision"
    (let [decision (:decision (pinned-record))
          stubs (->> (for [sym scan-vars]
                       (let [v (ns-resolve 'futon2.report.war-machine sym)]
                         (when v [v (constantly nil)])))
                     (keep identity)
                     (into {}))]
      (with-redefs-fn
        (merge stubs
               {#'wm/scan-r12-apparatus
                (constantly {:available? true :class-count 0})
                #'calibration/admit-apparatus!
                (fn [& _] {:ok true :returned {:return/id "fixture/r12"}})
               #'wm/judge (fn [& _] {:decision decision})})
        (fn []
          (let [generated (wm/generate-war-machine
                           14 {:defer-render? true :trace? false})
                judgement (:judgement generated)]
            (is (map? judgement))
            (is (= decision (:decision judgement)))
            (let [r (lne/receipt (:decision judgement))]
              (is (= :complete (:status r)))
              (is (= 0 (:refused (:counts r)))))))))))

(deftest t4-terminal-routing-must-not-certify-the-abstention-relabel
  (testing "the pinned record routes as the typed held machine condition"
    (let [record (pinned-record)
          receipt (tr/terminal-receipt record)]
      (is (= :failure (:kind receipt)))
      (is (= :wm/open-cascade-execution-error (:failure-kind receipt)))
      (is (not= :abstained (:failure-kind receipt)))
      (is (nil? (:abstention-status receipt)))
      (is (= "M-futonzero-prelim-practice" (:target receipt)))
      (is (= :mission (:target-kind receipt)))))
  (testing "REPAIRED: the failure receipt surfaces the held phase, so a held
            open-cascade compile condition is distinguishable from abstention
            at the terminal-routing seam (red on injured code)"
    (let [receipt (tr/terminal-receipt (pinned-record))]
      (is (= :compile (:held-phase receipt)))
      (is (= :wm/open-cascade-execution-error (:failure-kind receipt))))))
