(ns futon2.aif.load-identity
  "Source digests sampled as participating namespaces load. Not bytecode hashes.
   Edits between compiler read and registration, failed/partial loads, load-string
   and later Var mutation cannot be certified by this mechanism."
  (:require [clojure.java.io :as io])
  (:import [java.security MessageDigest]))

(defonce registry (atom {}))

(def required-sources
  "Explicit decision/close scope; absent registrations are never current.
  Click-path coverage (codex-20 review, 2026-09-22): the declared set includes
  every namespace the click path actually loads, found by reading
  futon3c.wm.runner-service -> futon2.aif.full-loop-runner requires (not by
  guessing), so namespaces that never register themselves (e.g.
  full-loop-cohort, tripwire, the futon3c runner service) show as
  :unregistered instead of silently disappearing."
  {'futon2.aif.parameter-novelty "/home/joe/code/futon2/src/futon2/aif/parameter_novelty.clj"
   'futon2.aif.surprise "/home/joe/code/futon2/src/futon2/aif/surprise.clj"
   'futon2.aif.attempt-learning "/home/joe/code/futon2/src/futon2/aif/attempt_learning.clj"
   'futon2.aif.learning-trial-ledger "/home/joe/code/futon2/src/futon2/aif/learning_trial_ledger.clj"
   'futon2.aif.learning-trial "/home/joe/code/futon2/src/futon2/aif/learning_trial.clj"
   'futon2.aif.route-attestation "/home/joe/code/futon2/src/futon2/aif/route_attestation.clj"
   'futon2.aif.run-ending-classification "/home/joe/code/futon2/src/futon2/aif/run_ending_classification.clj"
   'futon2.aif.cascade-selection "/home/joe/code/futon2/src/futon2/aif/cascade_selection.clj"
   'futon2.aif.cascade-model-manifest "/home/joe/code/futon2/src/futon2/aif/cascade_model_manifest.clj"
   'futon2.aif.finding-ticket "/home/joe/code/futon2/src/futon2/aif/finding_ticket.clj"
   'futon2.aif.ticket-publication-io "/home/joe/code/futon2/src/futon2/aif/ticket_publication_io.clj"
   'futon2.aif.ticket-queue "/home/joe/code/futon2/src/futon2/aif/ticket_queue.clj"
   'futon2.aif.policy "/home/joe/code/futon2/src/futon2/aif/policy.clj"
   'futon2.aif.live-c "/home/joe/code/futon2/src/futon2/aif/live_c.clj"
   'futon2.aif.d-predecessor-task-authority "/home/joe/code/futon2/src/futon2/aif/d_predecessor_task_authority.clj"
   'futon2.aif.cascade-habit-store "/home/joe/code/futon2/src/futon2/aif/cascade_habit_store.clj"
   'futon2.aif.cascade-habit-reinforcement "/home/joe/code/futon2/src/futon2/aif/cascade_habit_reinforcement.clj"
   'futon2.aif.cascade-plan "/home/joe/code/futon2/src/futon2/aif/cascade_plan.clj"
   'futon2.aif.job-text-retention "/home/joe/code/futon2/src/futon2/aif/job_text_retention.clj"
   'futon2.aif.scan-report "/home/joe/code/futon2/src/futon2/aif/scan_report.clj"
   'futon2.aif.kernel-example "/home/joe/code/futon2/src/futon2/aif/kernel_example.clj"
   'futon2.aif.token-outcome "/home/joe/code/futon2/src/futon2/aif/token_outcome.clj"
   'futon2.aif.trace "/home/joe/code/futon2/src/futon2/aif/trace.clj"
   'futon2.aif.mission-hole-wants "/home/joe/code/futon2/src/futon2/aif/mission_hole_wants.clj"
   'futon2.report.war-machine "/home/joe/code/futon2/scripts/futon2/report/war_machine.clj"
   'futon2.aif.full-loop-runner "/home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj"
   'futon2.aif.interpretation-construction "/home/joe/code/futon2/src/futon2/aif/interpretation_construction.clj"
   'futon2.aif.run-narrative "/home/joe/code/futon2/src/futon2/aif/run_narrative.clj"
   'futon2.aif.efe "/home/joe/code/futon2/src/futon2/aif/efe.clj"
   'futon2.aif.close-loop "/home/joe/code/futon2/src/futon2/aif/close_loop.clj"
   'futon2.aif.close-retention "/home/joe/code/futon2/src/futon2/aif/close_retention.clj"
   'futon2.aif.action-identity "/home/joe/code/futon2/src/futon2/aif/action_identity.clj"
   'futon2.aif.focus-receipt "/home/joe/code/futon2/src/futon2/aif/focus_receipt.clj"
   'futon2.aif.preference-audit "/home/joe/code/futon2/src/futon2/aif/preference_audit.clj"
   'futon2.aif.token-initialization-policy "/home/joe/code/futon2/src/futon2/aif/token_initialization_policy.clj"
   'futon2.aif.token-belief-carry "/home/joe/code/futon2/src/futon2/aif/token_belief_carry.clj"
   'futon2.aif.token-belief-predecessor "/home/joe/code/futon2/src/futon2/aif/token_belief_predecessor.clj"
   'futon2.aif.scoring-input-receipts "/home/joe/code/futon2/src/futon2/aif/scoring_input_receipts.clj"
   'futon2.aif.eig-shadow "/home/joe/code/futon2/src/futon2/aif/eig_shadow.clj"
   'futon2.aif.cascade-sources "/home/joe/code/futon2/src/futon2/aif/cascade_sources.clj"
   'futon2.aif.cascade-problems "/home/joe/code/futon2/src/futon2/aif/cascade_problems.clj"
   'futon2.aif.cascade-structure "/home/joe/code/futon2/src/futon2/aif/cascade_structure.clj"
   ;; --- click-path additions (runner-service -> full-loop-runner requires) ---
   'futon3c.wm.runner-service "/home/joe/code/futon3c/src/futon3c/wm/runner_service.clj"
   'futon2.aif.c-fold-config "/home/joe/code/futon2/src/futon2/aif/c_fold_config.clj"
   'futon2.aif.c-vector "/home/joe/code/futon2/src/futon2/aif/c_vector.clj"
   'futon2.aif.evidence-manifest "/home/joe/code/futon2/src/futon2/aif/evidence_manifest.clj"
   'futon2.aif.fold "/home/joe/code/futon2/src/futon2/aif/fold.clj"
   'futon2.aif.fold-classical "/home/joe/code/futon2/src/futon2/aif/fold_classical.clj"
   'futon2.aif.fold-cascade "/home/joe/code/futon2/src/futon2/aif/fold_cascade.clj"
   'futon2.aif.delivery-qa "/home/joe/code/futon2/src/futon2/aif/delivery_qa.clj"
   'futon2.aif.full-loop-cohort "/home/joe/code/futon2/src/futon2/aif/full_loop_cohort.clj"
   'futon2.aif.g-term-decomposition "/home/joe/code/futon2/src/futon2/aif/g_term_decomposition.clj"
   'futon2.aif.limb-evidence "/home/joe/code/futon2/src/futon2/aif/limb_evidence.clj"
   'futon2.aif.interpretation-evidence "/home/joe/code/futon2/src/futon2/aif/interpretation_evidence.clj"
   'futon2.aif.interpretation-job "/home/joe/code/futon2/src/futon2/aif/interpretation_job.clj"
   'futon2.aif.fact-measurement "/home/joe/code/futon2/src/futon2/aif/fact_measurement.clj"
   'futon2.aif.task-execution-evidence "/home/joe/code/futon2/src/futon2/aif/task_execution_evidence.clj"
   'futon2.aif.receipt-construction "/home/joe/code/futon2/src/futon2/aif/receipt_construction.clj"
   'futon2.aif.mission-registry "/home/joe/code/futon2/src/futon2/aif/mission_registry.clj"
   'futon2.aif.morning-brief "/home/joe/code/futon2/src/futon2/aif/morning_brief.clj"
   'futon2.aif.pattern-registry "/home/joe/code/futon2/src/futon2/aif/pattern_registry.clj"
   'futon2.aif.run-participants "/home/joe/code/futon2/src/futon2/aif/run_participants.clj"
   'futon2.aif.repair-obligation "/home/joe/code/futon2/src/futon2/aif/repair_obligation.clj"
   'futon2.aif.repair-discharge "/home/joe/code/futon2/src/futon2/aif/repair_discharge.clj"
   'futon2.aif.repair-discharge-receipt "/home/joe/code/futon2/src/futon2/aif/repair_discharge_receipt.clj"
   'futon2.aif.repair-evaluators "/home/joe/code/futon2/src/futon2/aif/repair_evaluators.clj"
   'futon2.aif.substrate "/home/joe/code/futon2/src/futon2/aif/substrate.clj"
   'futon2.aif.tripwire "/home/joe/code/futon2/src/futon2/aif/tripwire.clj"
   'futon2.report.cascade-lane "/home/joe/code/futon2/scripts/futon2/report/cascade_lane.clj"})

(defn sha256 [bytes]
  (when bytes
    (apply str (map #(format "%02x" (bit-and 255 %))
                    (.digest (MessageDigest/getInstance "SHA-256") bytes)))))

(defn read-bytes [source]
  (try
    (when source
      (with-open [in (io/input-stream source)] (.readAllBytes in)))
    (catch Exception _ nil)))

(defn register!
  "Overwrite this namespace's entry on every load, preserving other entries.
   Call with *ns* and *file*, at the top of the participating source. Captures
   the resolved resource at that instant, not later at drift-check time."
  [namespace source-file]
  (let [n (if (symbol? namespace) namespace (ns-name namespace))
        file (io/file source-file)
        resource (if (.isAbsolute file) (.toURL (.toURI file))
                     (or (io/resource source-file)
                         (when (.isFile file) (.toURL (.toURI file)))))
        bytes (read-bytes resource)
        entry {:namespace n :source-file source-file
               :source-url (some-> resource str)
               :source-path (when (and resource (= "file" (.getProtocol resource)))
                              (.getCanonicalPath (io/file (.toURI resource))))
               :captured-at (str (java.time.Instant/now))
               :status (if bytes :captured :unavailable)
               :sha256 (sha256 bytes)}]
    (swap! registry assoc n entry)
    entry))

(defn check
  "Compare one captured digest with a fresh disk read; never re-read the loaded
   resource to manufacture the left-hand side. Missing registration is typed."
  [entry canonical-path canonical-read]
  (let [disk (when canonical-path (sha256 (canonical-read canonical-path)))]
    {:status (cond (nil? entry) :unregistered
                   (or (nil? (:sha256 entry)) (nil? disk)) :unavailable
                   (= (:sha256 entry) disk) :current
                   :else :stale)
     :canonical-path canonical-path :loaded-source entry :disk-sha256 disk}))

(defn report
  "Report all registrations plus required names. An undeclared canonical path
   remains unavailable; a captured worktree path must not stand in for canonical."
  ([] (report required-sources read-bytes))
  ([required canonical-read]
   (let [entries @registry
         sources (merge (zipmap (keys entries) (repeat nil)) required)]
     (into (sorted-map)
           (map (fn [[n path]] [n (check (get entries n) path canonical-read)])) sources))))
