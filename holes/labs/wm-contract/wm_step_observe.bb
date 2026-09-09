#!/usr/bin/env bb
;; WM-STEP OBSERVE -- the post-accept realized-outcome observation pass (:U59).
;;
;;   bb holes/labs/wm-contract/wm_step_observe.bb <work-dir> <run-id> [--print]
;;
;; WHAT IT IS FOR. A decision's realized outcome does not exist when the
;; decision is written, so it cannot be a key on the record
;; (war_machine.clj:2284-2306 writes the record and the RE4 rationale together,
;; at decision time). It exists one step later. This pass runs when step N+1 is
;; ACCEPTED and observes the outcome of step N -- the previous accepted step --
;; into step N+1's run store.
;;
;; That is also what dissolves the one-tick-no-pair absence: `wm_step.sh` runs
;; exactly one tick per step, so a run's own records never contain a pair, and
;; `:rationale-regret` deposited `:typed-absence` by construction
;; (C511-repair-or-elaborate.md section 3). The observation names the previous
;; run, and that naming is the pair.
;;
;; WHAT IT WRITES: one file, `runs/<N+1>/observation/realized-outcome-<N>.edn`,
;; in the one schema `:wm/realized-outcome-v1`
;; (src/futon2/aif/realized_outcome.clj). Into N+1's store and never into N's:
;; N's store is already committed and its deposit receipts list what it holds,
;; so a file added there would turn a replayed deposit from :already-present
;; into the append-only ledger's divergence refusal (run_era_ledger.bb:235-245).
;;
;; THE TWO LEGS ARE ONE QUANTITY MEASURED TWICE, which is the scale-match pin
;; the gamma contract requires (selection_gain.clj:83-97): `:expected-score` is
;; the chosen action's `:G-core` in step N's own ranking, `:realized-score` is
;; the SAME action's `:G-core` in step N+1's ranking. Same scorer, same units,
;; two times. Nothing is recomputed here; both numbers are read off records.
;;
;; THE CATEGORICAL OUTCOME IS MISSION-SCOPED AND NOT REPO-SCOPED, deliberately.
;; It is decided by the chosen mission's own `:open-hole-count` on the two
;; records: strictly down => :grounded-change, otherwise :grounded-no-change
;; (the vocabulary is full_loop_cohort.clj:31). The step's world record
;; (`:world/commit-census`, `:world/files`, `:world/mana-age`) is recorded
;; alongside as CONTEXT and is NOT the basis: a repo-wide census moves when
;; anyone commits anything, so reading a mission's outcome off it would
;; attribute an operator's commit to the machine's chosen action.
;;
;; A refusal is written, not omitted: when a leg cannot be observed the record
;; carries `:observation/status :typed-absence` and the reason, so the store
;; never merely lacks an observation.
;;
;; READ-ONLY except for that one file. No tick, no run lock, no substrate call,
;; no network, nothing under data/.

(require '[babashka.classpath :as classpath]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
(classpath/add-classpath (str repo-root "/src"))
(require '[futon2.aif.realized-recording :as recording])

;; The :wm/realized-recording-v1 envelope is OPT-IN (A4 adoption is a
;; separate reviewed step; DRAFT-realized-outcome-recording-19b s. legacy
;; clause). Absent the flag, the legacy record is emitted byte-identically
;; -- the replay control in negative_controls.sh holds this invariant.
(def recording-contract-active?
  (= "1" (System/getenv "FUTON_WM_RECORDING_CONTRACT")))
(def runs-dir (io/file repo-root "holes/labs/wm-contract/runs"))

(def read-opts {:default (fn [t v] {:unread-tag t :value v})})

(defn read-edn [f]
  (when (and f (.isFile ^java.io.File (io/file f)))
    (edn/read-string read-opts (slurp (io/file f)))))

(defn store [run-id] (io/file runs-dir run-id))

(defn trace-records
  "The run's own records: the trace file(s) in its store, filtered to the tick
   ids its receipts name -- the same selection `u39_selection_retrospective.bb`
   makes (run-records), so the two readers cannot disagree about what a run's
   records are."
  [run-id]
  (let [d (store run-id)
        files (when (.isDirectory d) (vec (sort (map #(.getName ^java.io.File %)
                                                     (filter #(.isFile ^java.io.File %) (.listFiles d))))))
        ids (set (keep #(second (re-matches #"tick-run-record-\d{4}-\d{2}-\d{2}-(.+)\.edn" %)) files))
        traces (filter #(re-matches #"wm-trace.*\.edn" %) files)]
    (->> traces
         (mapcat (fn [t] (with-open [r (io/reader (io/file d t))]
                           (mapv #(edn/read-string read-opts %) (line-seq r)))))
         (filter #(contains? ids (:run/id %)))
         (sort-by :timestamp)
         vec)))

(defn chosen-action [record] (get-in record [:decision :action]))

(defn g-core-for
  "The `:G-core` this record's ranking gives ACTION, matched on type+target --
   the identity `u39_selection_retrospective.bb` (action-key) matches on."
  [record action]
  (let [k (juxt :type :target)
        want (k action)]
    (some (fn [entry] (when (= want (k (:action entry))) (:G-core entry)))
          (:ranked-actions record))))

(defn open-hole-count-for
  [record action]
  (let [k (juxt :type :target)
        want (k action)]
    (some (fn [entry] (when (= want (k (:action entry))) (:open-hole-count (:action entry))))
          (:ranked-actions record))))

(defn world-delta
  "Context only. Which keys of the two step world records differ, `:world/at`
   excluded because it is the reading's own clock and always differs."
  [before after]
  (when (and (map? before) (map? after))
    {:keys-moved (vec (sort (for [k (disj (into (set (keys before)) (keys after)) :world/at)
                                  :when (not= (get before k) (get after k))]
                              k)))
     :from (:world/at before)
     :to (:world/at after)}))

(defn previous-accepted-run
  "The run accepted BEFORE run-id, from the pin's own ordered list
   (`:pin/accepted-steps`, appended by wm_step.sh cmd_accept). The pin is the
   authority on the order because it is what advances; nothing here infers an
   order from file names or timestamps.

   TWO CALL TIMES, ONE ANSWER. `cmd_accept` runs this pass BEFORE it advances
   the pin, so the run being accepted is not yet in the list and the previous
   accepted run is its last entry. Re-run later (`wm_step.sh observe`) the run
   IS in the list, and the previous one is the entry before it. Both are the
   same run, which is why the record this pass writes is byte-identical whether
   it was taken at accept time or replayed afterwards."
  [work run-id]
  (let [pin (read-edn (io/file work "pin/pin.edn"))
        accepted (vec (:pin/accepted-steps pin))
        idx (first (keep-indexed (fn [i e] (when (= run-id (:run-id e)) i)) accepted))]
    (cond
      (nil? idx) (last accepted)
      (pos? idx) (get accepted (dec idx))
      :else nil)))

(defn absence [run-id prev-run-id reason detail]
  (array-map
   :observation/schema :wm/step-observation-v1
   :observation/status :typed-absence
   :observation/reason reason
   :observation/detail detail
   :observation/observed-at-run run-id
   :observation/observed-for-run prev-run-id
   :observation/produced-by "holes/labs/wm-contract/wm_step_observe.bb"))

(defn observe [work run-id]
  (let [prev (previous-accepted-run work run-id)]
    (if-not prev
      (absence run-id nil :no-previous-accepted-step
               (str "runs/" run-id " is the first accepted step of this pin, so there is no "
                    "earlier decision to observe the outcome of. The pin's :pin/accepted-steps "
                    "is the order; a second accepted step produces the first observation."))
      (let [prev-run-id (:run-id prev)
            prev-recs (trace-records prev-run-id)
            recs (trace-records run-id)
            claim-rec (last (filter #(seq (get-in % [:decision :controller-ranking])) prev-recs))
            later-rec (last (filter #(seq (get-in % [:decision :controller-ranking])) recs))]
        (cond
          (nil? claim-rec)
          (absence run-id prev-run-id :previous-run-carries-no-ranked-decision
                   (str "runs/" prev-run-id " holds " (count prev-recs)
                        " record(s) of its own and none carries a [:decision :controller-ranking], "
                        "so there is no claim whose outcome this pass could observe."))

          (nil? later-rec)
          (absence run-id prev-run-id :this-run-carries-no-ranked-decision
                   (str "runs/" run-id " holds " (count recs)
                        " record(s) of its own and none carries a [:decision :controller-ranking], "
                        "so the realized leg -- the same action re-scored at this step -- has "
                        "nowhere to be read from."))

          :else
          (let [action (chosen-action claim-rec)
                expected (g-core-for claim-rec action)
                realized (g-core-for later-rec action)
                holes-before (open-hole-count-for claim-rec action)
                holes-after (open-hole-count-for later-rec action)
                wb-prev (read-edn (io/file (store prev-run-id) "world-before.edn"))
                wb-this (read-edn (io/file (store run-id) "world-before.edn"))
                outcome (cond
                          (not (and (number? holes-before) (number? holes-after))) nil
                          (< holes-after holes-before) :grounded-change
                          :else :grounded-no-change)]
            ((if recording-contract-active?
               #(recording/step-envelope % %2)
               (fn [r _] r))
             (cond-> (array-map
                     :schema :wm/realized-outcome-v1
                     :observation/schema :wm/step-observation-v1
                     :observation/status (if (and (number? expected) (number? realized) outcome)
                                           :observed :partial)
                     :observation/observed-at-run run-id
                     :observation/observed-for-run prev-run-id
                     :observation/observed-for-tick (:run/id claim-rec)
                     :observation/evaluated-against-tick (:run/id later-rec)
                     :observation/produced-by "holes/labs/wm-contract/wm_step_observe.bb"
                     :observation/basis
                     ["[:decision :action] of the observed run's record"
                      "[:ranked-actions] :G-core of that action on BOTH records (same scorer, two times)"
                      "[:ranked-actions] :action :open-hole-count of that action on both records"]
                     :policy (:target action)
                     :action (select-keys action [:type :target])
                     :tick (:timestamp claim-rec)
                     :scale :g-core
                     :expected-score expected
                     :realized-score realized
                     :outcome outcome
                     :outcome/basis
                     {:dial :open-hole-count
                      :target (:target action)
                      :at-decision holes-before
                      :at-observation holes-after
                      :vocabulary "futon2.aif.full-loop-cohort/outcome vocabulary (full_loop_cohort.clj:31)"
                      :rule (str "strictly fewer open holes on the chosen mission at the next accepted "
                                 "step => :grounded-change; otherwise :grounded-no-change. The dial is "
                                 "MISSION-SCOPED on purpose: the step world record is repo-wide and "
                                 "moves when anyone commits anything, so it cannot attribute a change "
                                 "to the action that was chosen.")}
                     :observation/world-context-not-the-basis (world-delta wb-prev wb-this))
              (not (number? expected))
              (assoc :observation/expected-leg-absent :chosen-action-not-in-its-own-ranking)
              (not (number? realized))
              (assoc :observation/realized-leg-absent :chosen-action-not-ranked-at-the-next-step)
              (nil? outcome)
              (assoc :observation/outcome-leg-absent :open-hole-count-not-recorded-on-both-records))
             {:run-id prev-run-id
              :step-index (inc (.indexOf (vec (:pin/accepted-steps
                                                (read-edn (io/file work "pin/pin.edn")))) prev))
              :before claim-rec :after later-rec
              :evidence {:digest (format "%064x" (BigInteger. 1
                                        (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                                 (.getBytes (pr-str [claim-rec later-rec]) "UTF-8"))))
                         :locator {:before-run prev-run-id :after-run run-id
                                   :before-tick (:run/id claim-rec) :after-tick (:run/id later-rec)}
                         :adapter :wm-step-observe-recording-v1 :actor :supervised-stepper}})))))))

(let [args (vec *command-line-args*)
      [work run-id] (filterv #(not (str/starts-with? % "--")) args)
      print-only? (some #{"--print"} args)]
  (when-not (and work run-id)
    (println "usage: wm_step_observe.bb <work-dir> <run-id> [--print]")
    (System/exit 2))
  (let [r (observe work run-id)
        rel (str "holes/labs/wm-contract/runs/" run-id "/observation/realized-outcome-"
                 (or (:observation/observed-for-run r) "none") ".edn")
        out (io/file repo-root rel)]
    (if print-only?
      (pp/pprint r)
      (do (io/make-parents out)
          (recording/persist! out r)
          (println "wm_step_observe: wrote" rel)
          (println "  status" (:observation/status r)
                   "for-run" (:observation/observed-for-run r)
                   "outcome" (:outcome r)
                   "expected" (:expected-score r)
                   "realized" (:realized-score r))))
    (System/exit 0)))
